package com.rahmatsobrian.scrcpy.scrcpy

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.Surface
import com.flyfishxu.kadb.Kadb
import com.rahmatsobrian.scrcpy.ui.i18n.t
import com.rahmatsobrian.scrcpy.util.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * Menjembatani [ScrcpySession] dengan decoder video/audio dan UI.
 *
 * Urutan siklus:
 * 1. [connect] — siapkan [Kadb] (USB/wireless) di luar main thread.
 * 2. [start] — push & jalankan server, buka socket, mulai demux.
 * 3. UI memanggil [setSurface] saat `TextureView` siap (boleh setelah [start]).
 * 4. [stop] — tutup semua.
 */
class ScrcpyController(
    private val appContext: Context,
) {

    sealed interface State {
        data object Idle : State

        /** ADB tersambung, menunggu surface lalu sesi dijalankan. */
        data class Ready(val deviceName: String) : State

        data class Connecting(val label: String) : State
        data class Running(
            val deviceName: String,
            val videoWidth: Int,
            val videoHeight: Int,
            val audioCodecId: Int,
            val videoCodecId: Int,
        ) : State

        data class Failed(val message: String) : State
        data object Stopped : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _clipboard = MutableStateFlow<String?>(null)
    val clipboard: StateFlow<String?> = _clipboard.asStateFlow()

    /** Resolusi video terakhir dari paket session. */
    private val _videoSize = MutableStateFlow(0 to 0)
    val videoSize: StateFlow<Pair<Int, Int>> = _videoSize.asStateFlow()

    @Volatile private var kadb: Kadb? = null
    @Volatile private var session: ScrcpySession? = null
    @Volatile private var videoDecoder: VideoDecoder? = null
    @Volatile private var audioPlayer: AudioPlayer? = null
    @Volatile private var surface: Surface? = null

    /** Thread pembunuh server sesi sebelumnya; sesi baru harus menunggunya. */
    @Volatile private var pendingKill: Thread? = null

    @Volatile private var lastVolumeLogNs = 0L

    /** Device aktif STREAM_MUSIC pernah terlihat berpindah ke remote_submix. */
    @Volatile private var sawSubmix = false

    /** Pengguna menekan tombol volume selama sesi berjalan. */
    @Volatile private var volumeTouched = false

    /** Nama device terakhir, dipertahankan agar state Ready tetap bermakna. */
    @Volatile private var lastName: String = ""

    private val clipboardSeq = AtomicLong(1)

    // ---------------------------------------------------------------- connect

    fun attach(kadb: Kadb, deviceName: String) {
        this.kadb = kadb
        lastName = deviceName.ifBlank { t("ADB device") }
        _state.value = State.Ready(lastName)
    }

    val connectedKadb: Kadb? get() = kadb

    // ---------------------------------------------------------------- start

    fun start(options: ScrcpyOptions) = startInternal(options, allowRetry = true)

    private fun startInternal(options: ScrcpyOptions, allowRetry: Boolean) {
        if (session != null && session?.isRunning == true) return
        val conn = kadb ?: run {
            _state.value = State.Failed(t("No ADB connection yet"))
            return
        }

        _state.value = State.Connecting(t("Starting scrcpy-server…"))

        val decoderListener = object : VideoDecoder.Listener {
            override fun onVideoSize(width: Int, height: Int, clientResized: Boolean) {
                AppLog.log("video size: ${width}x$height (clientResized=$clientResized)")
                _videoSize.value = width to height
                val s = _state.value
                if (s is State.Running) {
                    _state.value = s.copy(videoWidth = width, videoHeight = height)
                }
            }

            override fun onDecoderError(cause: Throwable) {
                Log.w(TAG, "Video decoder error", cause)
                AppLog.log("decoder error: $cause")
            }
        }

        val decoder = VideoDecoder(decoderListener)
        videoDecoder = decoder
        surface?.let { decoder.setSurface(it) }

        val sessionListener = object : ScrcpySession.Listener {
            override fun onDeviceName(name: String) {
                val resolved = name.ifBlank { lastName }
                lastName = resolved
                _state.value = State.Running(
                    deviceName = resolved,
                    videoWidth = 0,
                    videoHeight = 0,
                    audioCodecId = 0,
                    videoCodecId = 0,
                )
                applyPostStartOptions(options)
            }

            override fun onServerLog(isError: Boolean, line: String) {
                if (isError) Log.w(TAG, "server: $line") else Log.d(TAG, "server: $line")
                AppLog.log("server: $line")
            }

            override fun onVideoCodec(codecId: Int) {
                decoder.setCodec(codecId)
                patchState(videoCodecId = codecId)
            }

            override fun onVideoSession(width: Int, height: Int, clientResized: Boolean) {
                decoder.onSession(width, height, clientResized)
            }

            override fun onVideoConfig(data: ByteArray) {
                decoder.onConfig(data)
            }

            override fun onVideoPacket(header: PacketHeader, payload: ByteArray) {
                decoder.onPacket(header, payload)
            }

            override fun onVideoStreamDisabled() {
                Log.w(TAG, "stream video dinonaktifkan")
            }

            override fun onAudioCodec(codecId: Int) {
                patchState(audioCodecId = codecId)
                if (codecId == 0 || codecId == 1) return
                AppLog.log("audio: buat player (codecId=0x${codecId.toString(16)})")
                audioPlayer?.close()
                audioPlayer = AudioPlayer(codecId, object : AudioPlayer.Listener {
                    override fun onAudioError(cause: Throwable) {
                        Log.w(TAG, "Audio error", cause)
                        AppLog.log("audio error: $cause")
                    }

                    override fun onFirstFramePlayed() {
                        AppLog.log("audio: frame pertama diputar")
                    }
                })
            }

            override fun onAudioConfig(data: ByteArray) {
                audioPlayer?.onConfig(data)
            }

            override fun onAudioPacket(header: PacketHeader, payload: ByteArray) {
                audioPlayer?.onPacket(header, payload)
            }

            override fun onDeviceMessage(message: DeviceMessage) {
                when (message) {
                    is DeviceMessage.Clipboard -> _clipboard.value = message.text
                    is DeviceMessage.AckClipboard -> Log.d(TAG, "clipboard ack ${message.sequence}")
                    is DeviceMessage.UhidOutput -> Unit
                }
            }

            override fun onClosed(error: Throwable?) {
                Log.i(TAG, "sesi berakhir", error)
                AppLog.log("sesi berakhir: ${error ?: "normal"}")
                releaseMedia()
                session = null
                _videoSize.value = 0 to 0
                _state.value = error?.let {
                    State.Failed(it.message ?: it.toString())
                } ?: State.Ready(lastName.ifBlank { t("ADB device") })
            }
        }

        val newSession = ScrcpySession(
            kadb = conn,
            options = options,
            openServerAsset = { appContext.assets.open(SERVER_ASSET) },
            listener = sessionListener,
        )
        session = newSession

        Thread({
            // pkill sesi sebelumnya harus selesai dulu: pola "pkill -f
            // com.genymobile.scrcpy.Server" juga cocok dengan cmdline shell milik
            // sesi baru sehingga server yang baru bisa ikut mati.
            runCatching { pendingKill?.join(3_000) }
            pendingKill = null
            runCatching { newSession.start() }
                .onSuccess {
                    logTargetVolume("sesi-start")
                    syncVolumeOnStart()
                }
                .onFailure { handleStartFailure(conn, newSession, options, it, allowRetry) }
        }, "scrcpy-start").apply { isDaemon = true; start() }
    }

    private fun handleStartFailure(
        conn: Kadb,
        failed: ScrcpySession,
        options: ScrcpyOptions,
        t: Throwable,
        allowRetry: Boolean,
    ) {
        Log.e(TAG, "gagal memulai sesi", t)
        AppLog.log("gagal memulai sesi: $t (koneksiOk=${conn.connectionCheck()})")
        runCatching { failed.stop() }
        pendingKill = failed.killThread

        if (allowRetry && isConnectionBroken(t)) {
            AppLog.log("koneksi ADB bermasalah → reset transport & coba ulang sekali")
            runCatching { conn.resetConnection() }
            if (session === failed) session = null
            releaseMedia()
            startInternal(options, allowRetry = false)
            return
        }

        if (session === failed) session = null
        _state.value = State.Failed(t.message ?: t.toString())
    }

    /**
     * Catat volume milik target (read-only) ke log. Dipakai untuk memastikan
     * apakah volume benar-benar berubah balik setelah disconnect, atau hanya
     * perangkat keluaran yang beda.
     *
     * Android menyimpan `volume_music_*` per perangkat keluaran (speaker,
     * headphone, remote submix, …). Tombol volume menulis ke kunci perangkat
     * yang sedang aktif — bisa berbeda dengan yang tampil setelah scrcpy
     * berhenti, sehingga angkanya terlihat "kembali".
     */
    fun logTargetVolume(tag: String) {
        val conn = kadb ?: return
        val now = System.nanoTime()
        // Throttle hanya untuk volkey; baca saat start/stop selalu dicatat.
        if (tag == "volkey" && now - lastVolumeLogNs < VOL_LOG_MIN_INTERVAL_NS) return
        lastVolumeLogNs = now
        if (tag == "volkey") volumeTouched = true
        Thread({
            val resp = runCatching {
                conn.shell(
                    "echo A; settings list system | grep -i volume_music; " +
                        "echo B; dumpsys audio | grep -i -A12 -m1 STREAM_MUSIC; " +
                        "echo C; dumpsys audio | grep -i -m8 streamvolume; echo D",
                )
            }.getOrNull()
            val raw = resp?.allOutput.orEmpty()
            // AudioPlaybackCapture scrcpy memindahkan device aktif STREAM_MUSIC
            // ke remote_submix → slider Settings membaca nilai yang berbeda.
            if (REMOTE_SUBMIX_DEVICES.containsMatchIn(raw)) sawSubmix = true
            val text = if (resp == null) {
                "gagal baca"
            } else {
                raw
                    .trim()
                    .replace(Regex("[ \\t]+"), " ")
                    .replace(Regex("\n+"), " | ")
                    .take(900)
                    .ifEmpty { "(kosong)" }
            }
            AppLog.log("volume[$tag] exit=${resp?.exitCode}: $text")
        }, "vol-log").apply { isDaemon = true; start() }
    }

    /**
     * Saat mulai: device aktif STREAM_MUSIC berpindah ke remote_submix sehingga
     * slider target "meloncat" dari nilai speaker ke nilai submix lama.
     * Set nilai submix = nilai speaker begitu device aktif berpindah.
     */
    fun syncVolumeOnStart() {
        val conn = kadb ?: return
        Thread({
            val speaker = readIntSetting(conn, "volume_music_speaker")
            if (speaker == null) {
                AppLog.log("volume[sync-start]: volume_music_speaker tidak terbaca")
                return@Thread
            }
            repeat(SYNC_START_ATTEMPTS) {
                runCatching { Thread.sleep(SYNC_START_INTERVAL_MS) }
                val dev = musicDevices(conn)
                AppLog.log("volume[sync-start#$it] dev=$dev")
                if (dev != null && dev.contains("remote_submix", ignoreCase = true)) {
                    sawSubmix = true
                    val r = setMusicVolume(conn, speaker, "sync-start", "volume_music_remote_submix")
                    AppLog.log("volume[sync-start]: speaker=$speaker → submix ($r)")
                    return@Thread
                }
            }
            AppLog.log("volume[sync-start]: device aktif tidak berpindah ke remote_submix")
        }, "vol-sync").apply { isDaemon = true; start() }
    }

    /**
     * Saat putus: kembalikan volume yang diatur selama mirror (tersimpan di
     * `volume_music_remote_submix`) ke device speaker, supaya slider tidak
     * "kembali" ke nilai lama setelah sesi berakhir.
     */
    fun syncVolumeOnStop() {
        val conn = kadb ?: return
        if (!volumeTouched || !sawSubmix) {
            AppLog.log(
                "volume[sync-stop]: lewat (volumeTouched=$volumeTouched sawSubmix=$sawSubmix)",
            )
            volumeTouched = false
            sawSubmix = false
            return
        }
        volumeTouched = false
        sawSubmix = false
        Thread({
            // Tunggu server scrcpy mati & AudioPolicy dilepas → device aktif speaker.
            runCatching { Thread.sleep(SYNC_STOP_DELAY_MS) }
            val x = readIntSetting(conn, "volume_music_remote_submix")
            val dev = musicDevices(conn)
            if (x == null || dev == null || !dev.contains("speaker", ignoreCase = true)) {
                AppLog.log("volume[sync-stop]: lewat (x=$x dev=$dev)")
                return@Thread
            }
            val r = setMusicVolume(conn, x, "sync-stop", "volume_music_speaker")
            val after = readIntSetting(conn, "volume_music_speaker")
            AppLog.log("volume[sync-stop]: submix=$x → speaker=$after ($r)")
        }, "vol-sync").apply { isDaemon = true; start() }
    }

    private fun readIntSetting(conn: Kadb, key: String): Int? =
        runCatching { conn.shell("settings get system $key").output.trim().toInt() }
            .getOrNull()

    /**
     * `cmd media_session volume --set` mengikat AudioService secara async, jadi
     * hasilnya diverifikasi ke settings per-device; diulang bila belum cocok.
     */
    private fun setMusicVolume(
        conn: Kadb,
        index: Int,
        tag: String,
        verifyKey: String?,
    ): String {
        var last = ""
        repeat(3) { attempt ->
            last = runCatching {
                conn.shell("cmd media_session volume --stream 3 --set $index")
                    .allOutput.trim().replace(Regex("\\s+"), " ").take(160)
                    .ifEmpty { "ok" }
            }.getOrElse { "gagal: $it" }
            AppLog.log("volume[$tag#${attempt + 1}] set($index): $last")
            if (verifyKey == null) return last
            runCatching { Thread.sleep(600) }
            val cur = readIntSetting(conn, verifyKey)
            if (cur == index) return "ok($verifyKey=$cur)"
        }
        return "belum cocok: $last"
    }

    private fun musicDevices(conn: Kadb): String? = runCatching {
        val out = conn.shell("dumpsys audio | grep -i -A12 -m1 STREAM_MUSIC").output
        MUSIC_DEVICES.find(out)?.groupValues?.get(1)?.trim()
    }.getOrNull()

    /**
     * True bila kegagalan berasal dari transport ADB/TLS yang sudah mati
     * (SSLEngine CLOSED / socket tertutup), bukan dari server scrcpy itu sendiri.
     */
    private fun isConnectionBroken(t: Throwable): Boolean {
        var e: Throwable? = t
        var depth = 0
        while (e != null && depth++ < 6) {
            val m = (e.message ?: "").lowercase()
            if (m.contains("tls write") || m.contains("tls read") ||
                m.contains("eof") || m.contains("closedchannel") ||
                m.contains("channel closed") || m.contains("connection reset") ||
                m.contains("broken pipe") || m.contains("close_notify") ||
                m.contains("handshake") || m.contains("channel is closed")
            ) return true
            e = e.cause
        }
        return false
    }

    /**
     * Aksi klien yang scrcpy jalankan setelah seluruh pipeline siap:
     * `-S/--turn-screen-off` (`scrcpy.c:851`) dan `--start-app` (`scrcpy.c:887`).
     */
    private fun applyPostStartOptions(options: ScrcpyOptions) {
        if (!options.control) return
        val turnOff = options.turnScreenOff
        val app = options.startApp?.takeIf { it.isNotBlank() }
        if (!turnOff && app == null) return
        Thread({
            runCatching { Thread.sleep(300) }
            if (turnOff) runCatching { setDisplayPower(false) }
            if (app != null) runCatching { startApp(app) }
        }, "scrcpy-post-start").apply { isDaemon = true; start() }
    }

    private fun patchState(videoCodecId: Int? = null, audioCodecId: Int? = null) {
        val s = _state.value as? State.Running ?: return
        _state.value = s.copy(
            videoCodecId = videoCodecId ?: s.videoCodecId,
            audioCodecId = audioCodecId ?: s.audioCodecId,
        )
    }

    // ---------------------------------------------------------------- surface

    fun setSurface(newSurface: Surface?) {
        val had = surface != null
        surface = newSurface
        videoDecoder?.setSurface(newSurface)
        // Surface baru setelah sempat hilang (mis. layar host berputar):
        // minta server memulai sesi tangkap baru agar ada keyframe segar.
        if (!had && newSurface != null && session != null) {
            Thread({
                runCatching { Thread.sleep(120) }
                runCatching { session?.sendControl(ControlProtocol.resetVideo()) }
            }, "scrcpy-reset-video").apply { isDaemon = true; start() }
        }
    }

    // ---------------------------------------------------------------- control

    /**
     * Serialisasi control message lalu kirim.
     *
     * [block] dijalankan di thread pemanggil (sering main thread); `require(...)`
     * di `ControlProtocol` boleh saja gagal tapi **tidak boleh mematikan proses**,
     * jadi semua Throwable ditahan dan dicatat ke [AppLog].
     */
    private fun control(block: ControlProtocol.() -> ByteArray) {
        val bytes = try {
            block(ControlProtocol)
        } catch (t: Throwable) {
            Log.e(TAG, "control gagal", t)
            AppLog.log("control gagal: $t")
            return
        }
        session?.sendControl(bytes) ?: Log.w(TAG, "control diabaikan: sesi tidak aktif")
    }

    fun sendTouch(
        action: Int,
        pointerId: Long,
        x: Float,
        y: Float,
        screenWidth: Int,
        screenHeight: Int,
        pressure: Float,
        actionButton: Int = 0,
        buttons: Int = 0,
    ) = control {
        touch(
            action, pointerId, x.toInt(), y.toInt(), screenWidth, screenHeight,
            pressure, actionButton, buttons,
        )
    }

    fun sendScroll(
        x: Float,
        y: Float,
        screenWidth: Int,
        screenHeight: Int,
        hScroll: Float,
        vScroll: Float,
        buttons: Int = 0,
    ) = control {
        scroll(x.toInt(), y.toInt(), screenWidth, screenHeight, hScroll, vScroll, buttons)
    }

    fun sendKey(action: Int, keycode: Int, repeat: Int = 0, metastate: Int = 0) =
        control { keycode(action, keycode, repeat, metastate) }

    fun sendText(text: String) = control { text(text) }
    fun back() = control { backOrScreenOn(ControlProtocol.KEY_ACTION_DOWN) + backOrScreenOn(ControlProtocol.KEY_ACTION_UP) }
    fun home() = sendKey(ControlProtocol.KEY_ACTION_DOWN, KEYCODE_HOME).also {
        sendKey(ControlProtocol.KEY_ACTION_UP, KEYCODE_HOME)
    }

    fun recentApps() {
        sendKey(ControlProtocol.KEY_ACTION_DOWN, KEYCODE_APP_SWITCH)
        sendKey(ControlProtocol.KEY_ACTION_UP, KEYCODE_APP_SWITCH)
    }

    fun expandNotifications() = control { expandNotificationPanel() }
    fun expandSettings() = control { expandSettingsPanel() }
    fun collapsePanels() = control { collapsePanels() }
    fun rotateDevice() = control { rotateDevice() }
    fun resetVideo() = control { resetVideo() }

    fun setClipboard(text: String, paste: Boolean = false) {
        val seq = clipboardSeq.getAndIncrement()
        control { setClipboard(seq, paste, text) }
    }

    fun getClipboard(copyKey: Int = 0) = control { getClipboard(copyKey) }
    fun setDisplayPower(on: Boolean) = control { setDisplayPower(on) }
    fun startApp(packageName: String) = control { startApp(packageName) }
    fun scanFile(path: String) = control { scanFile(path) }

    /** Kontrol kamera runtime (hanya bila `video_source=camera`). */
    fun setCameraTorch(on: Boolean) = control { cameraTorch(on) }
    fun zoomCameraIn() = control { cameraZoomIn() }
    fun zoomCameraOut() = control { cameraZoomOut() }

    // ---------------------------------------------------------------- stop

    private fun releaseMedia() {
        if (audioPlayer != null || videoDecoder != null) AppLog.log("media: release")
        runCatching { audioPlayer?.close() }
        audioPlayer = null
        runCatching { videoDecoder?.close() }
        videoDecoder = null
        _videoSize.value = 0 to 0
    }

    /**
     * Setelah kegagalan: kembalikan state ke [State.Ready] agar sesi bisa
     * dijalankan ulang tanpa menyambung ulang ADB.
     */
    fun acknowledgeFailure(): Boolean {
        if (_state.value !is State.Failed) return false
        if (kadb == null) return false
        _state.value = State.Ready(lastName.ifBlank { t("ADB device") })
        return true
    }

    fun stop() {
        logTargetVolume("sesi-stop")
        syncVolumeOnStop()
        val s = session
        session = null
        runCatching { s?.stop() }
        pendingKill = s?.killThread
        releaseMedia()

        // Koneksi ADB dipertahankan: kembali ke "siap mirror" agar user bisa
        // langsung membuka layar mirror lagi tanpa menyambung ulang.
        val current = _state.value
        if (kadb != null && current !is State.Idle) {
            val name = when (current) {
                is State.Running -> current.deviceName
                is State.Ready -> current.deviceName
                else -> t("ADB device")
            }
            _state.value = State.Ready(name)
        } else if (current != State.Idle) {
            _state.value = State.Stopped
        }
    }

    fun close() {
        stop()
        runCatching { kadb?.close() }
        lastName = ""
        kadb = null
        _state.value = State.Idle
    }

    companion object {
        private const val TAG = "ScrcpyController"

        /** Jeda minimum antar log volume target (shell dumpsys cukup berat). */
        private const val VOL_LOG_MIN_INTERVAL_NS = 1_500_000_000L

        /** Berapa kali menunggu device aktif berpindah ke remote_submix. */
        private const val SYNC_START_ATTEMPTS = 15
        private const val SYNC_START_INTERVAL_MS = 1_000L

        /** Tunggu server & AudioPolicy scrcpy benar-benar dilepas. */
        private const val SYNC_STOP_DELAY_MS = 900L

        /** `Devices: remote_submix(8000)` — device aktif STREAM_MUSIC. */
        private val REMOTE_SUBMIX_DEVICES =
            Regex("""Devices:\s*remote_submix""", RegexOption.IGNORE_CASE)

        private val MUSIC_DEVICES =
            Regex("""Devices:\s*(.+)""", RegexOption.IGNORE_CASE)
        const val SERVER_ASSET = "scrcpy-server"

        const val KEYCODE_HOME = 3
        const val KEYCODE_APP_SWITCH = 187
        const val KEYCODE_BACK = 4
        const val KEYCODE_VOLUME_UP = 24
        const val KEYCODE_VOLUME_DOWN = 25
        const val KEYCODE_POWER = 26
        const val KEYCODE_ENTER = 66
        const val KEYCODE_DEL = 67
        const val KEYCODE_TAB = 61
        const val KEYCODE_ESCAPE = 111
        const val KEYCODE_DPAD_UP = 19
        const val KEYCODE_DPAD_DOWN = 20
        const val KEYCODE_DPAD_LEFT = 21
        const val KEYCODE_DPAD_RIGHT = 22
    }
}

/** Util kecil agar asset scrcpy-server bisa dibuka dari [Uri] bila diperlukan. */
fun Context.openServerAsset(): java.io.InputStream = assets.open(ScrcpyController.SERVER_ASSET)

@Suppress("unused")
private val unusedUri: Uri? = null
