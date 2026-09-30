package com.rahmatsobrian.scrcpy.scrcpy

import com.flyfishxu.kadb.Kadb
import com.flyfishxu.kadb.shell.AdbShellPacket
import com.flyfishxu.kadb.shell.AdbShellStream
import com.flyfishxu.kadb.stream.AdbStream
import com.rahmatsobrian.scrcpy.scrcpy.Wire.readCodecId
import com.rahmatsobrian.scrcpy.scrcpy.Wire.readDeviceName
import com.rahmatsobrian.scrcpy.scrcpy.Wire.readPacketHeader
import com.rahmatsobrian.scrcpy.util.AppLog
import okio.Buffer
import okio.source
import java.io.InputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Orkestrasi satu sesi scrcpy 4.1: push server → jalankan `app_process` →
 * buka 3 socket `localabstract:scrcpy_<scid>` (video, audio, control) →
 * baca handshake (dummy byte + device meta) → loop demux.
 *
 * Semua callback dipanggil dari thread background (bukan main thread).
 *
 * Model koneksi: klien Android ini berperan seperti `adb forward` yang selalu
 * mode *forward* (`tunnel_forward=true`), sehingga device yang membuat
 * `LocalServerSocket` dan kita cukup membuka service `localabstract:<nama>`.
 * Tidak perlu `adb reverse`.
 */
class ScrcpySession(
    private val kadb: Kadb,
    private val options: ScrcpyOptions,
    private val openServerAsset: () -> InputStream,
    private val listener: Listener,
) {

    interface Listener {
        fun onDeviceName(name: String) {}
        fun onServerLog(isError: Boolean, line: String) {}
        fun onVideoCodec(codecId: Int) {}
        fun onVideoSession(width: Int, height: Int, clientResized: Boolean) {}
        fun onVideoConfig(data: ByteArray) {}
        fun onVideoPacket(header: PacketHeader, payload: ByteArray) {}
        fun onVideoStreamDisabled() {}
        fun onAudioCodec(codecId: Int) {}
        fun onAudioConfig(data: ByteArray) {}
        fun onAudioPacket(header: PacketHeader, payload: ByteArray) {}
        fun onAudioStreamDisabled() {}
        fun onDeviceMessage(message: DeviceMessage) {}
        fun onClosed(error: Throwable?) {}
    }

    private val running = AtomicBoolean(false)
    private val shellExited = AtomicBoolean(false)
    private val terminated = AtomicBoolean(false)
    private val killed = AtomicBoolean(false)

    @Volatile private var videoStream: AdbStream? = null
    @Volatile private var audioStream: AdbStream? = null
    @Volatile private var controlStream: AdbStream? = null
    @Volatile private var shell: AdbShellStream? = null

    private val controlOut = LinkedBlockingQueue<ByteArray>()
    private val threads = mutableListOf<Thread>()

    @Volatile
    var deviceName: String = ""
        private set

    val isRunning: Boolean get() = running.get()

    // ---------------------------------------------------------------- lifecycle

    /** Jalankan sesi penuh. Blocking; panggil dari thread sendiri. */
    @Throws(Throwable::class)
    fun start() {
        check(running.compareAndSet(false, true)) { "Sesi sudah berjalan" }
        try {
            ensureServerPushed()
            startServerProcess()
            openStreams()
            readHandshake()
            startLoops()
        } catch (t: Throwable) {
            stop()
            throw t
        }
    }

    /** Mode `--list-*`: jalankan server sekali, kembalikan output stdout. */
    fun listOutput(): String {
        ensureServerPushed()
        val cmd = "CLASSPATH=${ScrcpyOptions.SERVER_PATH} ${options.serverCommandLine()}"
        return kadb.shell(cmd).allOutput
    }

    /**
     * Tutup semua stream, hentikan thread, dan bunuh proses server.
     * Aman dipanggil berkali-kali dan dari thread mana pun.
     */
    fun stop() {
        running.set(false)
        controlOut.clear()
        controlOut.offer(POISON)
        closeQuietly(videoStream); videoStream = null
        closeQuietly(audioStream); audioStream = null
        closeQuietly(controlStream); controlStream = null
        closeQuietly(shell); shell = null
        killServerOnce()
        // JANGAN interrupt. SocketChannel (kadb) adalah InterruptibleChannel:
        // interrupt menutup socket bersama di tengah tulis/baca, memicu alert dari
        // adbd sehingga SSLEngine masuk status CLOSED sementara isOpen tetap true.
        // Akibatnya sesi berikutnya selalu gagal dengan "TLS write returned -1".
        // Menutup stream di atas sudah melepas reader (AdbStreamClosed) dan POISON
        // di atas melepas writer.
        threads.clear()
    }

    /**
     * Thread yang membunuh proses scrcpy-server. Sesuai berikutnya WAJIB menunggu
     * thread ini selesai, karena `pkill -f com.genymobile.scrcpy.Server` juga cocok
     * dengan cmdline shell milik sesi baru dan bisa membunuh server yang baru mulai.
     */
    @Volatile
    var killThread: Thread? = null
        private set

    private fun killServerOnce() {
        if (!killed.compareAndSet(false, true)) return
        // Thread terpisah agar stop() tidak menunggu roundtrip ADB.
        val t = Thread({
            runCatching { kadb.shell("pkill -f com.genymobile.scrcpy.Server >/dev/null 2>&1") }
        }, "scrcpy-kill").apply { isDaemon = true; start() }
        killThread = t
    }

    // ---------------------------------------------------------------- control

    /** Kirim control message mentah (byte sudah terserialisasi). */
    fun sendControl(bytes: ByteArray) {
        controlOut.offer(bytes)
    }

    // ---------------------------------------------------------------- setup

    private fun ensureServerPushed() {
        val bytes = openServerAsset().use { it.readBytes() }
        val existing = runCatching {
            kadb.shell("if [ -f ${ScrcpyOptions.SERVER_PATH} ]; then wc -c < ${ScrcpyOptions.SERVER_PATH}; fi")
                .output.trim().toInt()
        }.getOrNull()
        if (existing == bytes.size) return
        bytes.inputStream().source().use { src ->
            kadb.push(
                source = src,
                remotePath = ScrcpyOptions.SERVER_PATH,
                mode = 0x1ED, // 0755
                lastModifiedMs = System.currentTimeMillis(),
            )
        }
    }

    private fun startServerProcess() {
        val cmd = "CLASSPATH=${ScrcpyOptions.SERVER_PATH} ${options.serverCommandLine()}"
        AppLog.log("server args: ${options.serverCommandLine()}")
        val stream = kadb.openShell(cmd)
        shell = stream
        spawn("scrcpy-shell") {
            drainShell(stream)
        }
    }

    /** Akhiri sesi dan beri tahu listener tepat satu kali. */
    private fun terminate(cause: Throwable?) {
        if (!terminated.compareAndSet(false, true)) return
        try {
            listener.onClosed(cause)
        } finally {
            stop()
        }
    }

    private fun drainShell(stream: AdbShellStream) {
        try {
            while (running.get()) {
                when (val packet = stream.read()) {
                    is AdbShellPacket.StdOut ->
                        packet.payload.toString(Charsets.UTF_8).lineSequence()
                            .filter { it.isNotBlank() }
                            .forEach { listener.onServerLog(false, it) }

                    is AdbShellPacket.StdError ->
                        packet.payload.toString(Charsets.UTF_8).lineSequence()
                            .filter { it.isNotBlank() }
                            .forEach { listener.onServerLog(true, it) }

                    is AdbShellPacket.Exit -> {
                        shellExited.set(true)
                        val code = packet.payload[0].toUByte().toInt()
                        if (running.get()) {
                            // Keluar sendiri saat masih Running = kegagalan.
                            // Sengaja tidak `terminate(null)` agar UI tidak kembali
                            // ke Ready lalu langsung menyalakan sesi baru berulang-ulang.
                            listener.onServerLog(code != 0, "server keluar dengan kode $code")
                            terminate(
                                IllegalStateException(
                                    if (code != 0) "scrcpy-server berhenti (kode $code)"
                                    else "scrcpy-server berhenti lebih awal",
                                ),
                            )
                        }
                        return
                    }
                }
            }
        } catch (t: Throwable) {
            shellExited.set(true)
            if (running.get()) {
                listener.onServerLog(true, "stream shell terputus")
                terminate(t)
            }
        }
    }

    private fun openStreams() {
        check(options.video || options.audio || options.control) {
            "Minimal satu stream (video/audio/control) harus aktif"
        }
        // Urutan wajib: video → audio → control (server accept berurutan).
        if (options.video) videoStream = openWithRetry()
        if (options.audio) audioStream = openWithRetry()
        if (options.control) controlStream = openWithRetry()
    }

    private fun openWithRetry(): AdbStream {
        val destination = "localabstract:${options.socketName}"
        var lastError: Throwable? = null
        // Sama seperti klien desktop: 100 percobaan × 100 ms.
        repeat(100) {
            if (!running.get()) throw IllegalStateException("Sesi dihentikan")
            if (shellExited.get()) {
                throw IllegalStateException("Server keluar sebelum socket siap", lastError)
            }
            try {
                return kadb.open(destination)
            } catch (t: Throwable) {
                lastError = t
                Thread.sleep(100)
            }
        }
        throw IllegalStateException(
            "Gagal terhubung ke ${options.socketName} setelah 10 detik", lastError
        )
    }

    private fun readHandshake() {
        // Dummy byte hanya pada socket pertama (tunnel_forward=true).
        val first = videoStream ?: audioStream ?: controlStream
            ?: throw IllegalStateException("Tidak ada stream terbuka")
        val source = first.source
        source.readByte() // 0x00 dummy
        deviceName = source.readDeviceName()
        listener.onDeviceName(deviceName)
    }

    // ---------------------------------------------------------------- loops

    private fun startLoops() {
        videoStream?.let { s ->
            spawn("scrcpy-video") {
                runCatching { videoLoop(s) }
                    .onFailure { e ->
                        if (running.get()) {
                            listener.onServerLog(true, "video: $e")
                            terminate(e)
                        }
                    }
                if (running.get()) listener.onVideoStreamDisabled()
            }
        }
        audioStream?.let { s ->
            spawn("scrcpy-audio") {
                runCatching { audioLoop(s) }
                    .onFailure { e ->
                        // Audio bersifat opsional: stream mati bukan alasan sesi berhenti.
                        if (running.get()) listener.onServerLog(true, "audio: $e")
                    }
                if (running.get()) listener.onAudioStreamDisabled()
            }
        }
        controlStream?.let { s ->
            spawn("scrcpy-control-read") {
                runCatching { controlReadLoop(s) }
                    .onFailure { if (running.get()) listener.onServerLog(true, "control: $it") }
            }
            spawn("scrcpy-control-write") { controlWriteLoop(s) }
        }
    }

    private fun videoLoop(stream: AdbStream) {
        val src = stream.source
        val codecId = src.readCodecId()
        listener.onVideoCodec(codecId)
        when (codecId) {
            Wire.CODEC_ID_DISABLED -> return
            Wire.CODEC_ID_ERROR -> throw IllegalStateException("Konfigurasi encoder video gagal di device")
        }
        if (codecId !in VIDEO_CODEC_IDS) {
            throw IllegalStateException("Codec video tidak didukung: 0x${codecId.toString(16)}")
        }

        // Urutan paket pertama tidak dijamin: boleh session, config, atau
        // media. Terima apa pun selama `running` — hanya session yang divalidasi.
        while (running.get()) {
            val header = PacketHeader.parse(src.readPacketHeader())
            if (header.isSession) {
                if (header.width == 0 || header.height == 0) {
                    throw IllegalStateException(
                        "Ukuran video tidak valid: ${header.width}x${header.height}",
                    )
                }
                listener.onVideoSession(header.width, header.height, header.clientResized)
                continue
            }
            val payload = src.readByteArray(header.size.toLong())
            if (header.isConfig) {
                listener.onVideoConfig(payload)
            } else {
                listener.onVideoPacket(header, payload)
            }
        }
    }

    private fun audioLoop(stream: AdbStream) {
        val src = stream.source
        val codecId = src.readCodecId()
        listener.onAudioCodec(codecId)
        when (codecId) {
            Wire.CODEC_ID_DISABLED -> return
            Wire.CODEC_ID_ERROR -> throw IllegalStateException("Konfigurasi encoder audio gagal di device")
        }
        if (codecId !in AUDIO_CODEC_IDS) {
            throw IllegalStateException("Codec audio tidak didukung: 0x${codecId.toString(16)}")
        }

        while (running.get()) {
            val header = PacketHeader.parse(src.readPacketHeader())
            if (header.isSession) continue
            val payload = src.readByteArray(header.size.toLong())
            if (header.isConfig) {
                listener.onAudioConfig(payload)
            } else {
                listener.onAudioPacket(header, payload)
            }
        }
    }

    private fun controlReadLoop(stream: AdbStream) {
        val src = stream.source
        val reader = DeviceMessageReader()
        val chunk = Buffer()
        while (running.get()) {
            chunk.clear()
            val read = src.read(chunk, CONTROL_CHUNK.toLong())
            if (read <= 0) break
            reader.feed(chunk.readByteArray())
            while (true) {
                val message = reader.next() ?: break
                listener.onDeviceMessage(message)
            }
        }
    }

    private fun controlWriteLoop(stream: AdbStream) {
        val sink = stream.sink
        try {
            while (running.get()) {
                val bytes = controlOut.take()
                if (bytes === POISON) break
                sink.write(Buffer().write(bytes), bytes.size.toLong())
                sink.flush()
            }
        } catch (_: Throwable) {
            // stream ditutup saat stop
        }
    }

    private fun spawn(name: String, block: () -> Unit) {
        val t = Thread({
            try {
                block()
            } catch (_: InterruptedException) {
            } catch (t: Throwable) {
                // Exception tak tertangkap di thread Android = proses dibunuh.
                listener.onServerLog(true, "$name: $t")
                if (running.get()) terminate(t)
            }
        }, name)
        t.isDaemon = true
        threads += t
        t.start()
    }

    private fun closeQuietly(stream: AdbStream?) {
        runCatching { stream?.close() }
    }

    private fun closeQuietly(stream: AdbShellStream?) {
        runCatching { stream?.close() }
    }

    companion object {
        private val POISON = ByteArray(0)
        private const val CONTROL_CHUNK = 8192

        private val VIDEO_CODEC_IDS = intArrayOf(
            VideoCodec.H264.fourcc, VideoCodec.H265.fourcc,
            VideoCodec.AV1.fourcc, VideoCodec.VP8.fourcc, VideoCodec.VP9.fourcc,
        )

        private val AUDIO_CODEC_IDS = intArrayOf(
            AudioCodec.OPUS.fourcc, AudioCodec.AAC.fourcc,
            AudioCodec.FLAC.fourcc, AudioCodec.RAW.fourcc,
        )
    }
}
