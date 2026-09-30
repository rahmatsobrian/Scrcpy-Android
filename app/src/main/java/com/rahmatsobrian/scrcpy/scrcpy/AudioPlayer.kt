package com.rahmatsobrian.scrcpy.scrcpy

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.rahmatsobrian.scrcpy.util.AppLog
import java.nio.ByteBuffer
import java.util.ArrayDeque
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Decode & putar paket audio scrcpy (Opus/AAC/FLAC) atau tulis PCM mentah
 * untuk codec `raw`.
 *
 * Parameter keluaran di-hardcode sama seperti klien desktop
 * (`demuxer.c:257-271`): **48000 Hz, stereo, PCM signed 16-bit**.
 * Paket config (OpusHead/AudioSpecificConfig/STREAMINFO) dipasang sebagai
 * `csd-0` `MediaFormat` — tidak pernah didecode langsung.
 */
class AudioPlayer(
    private val codecId: Int,
    private val listener: Listener,
) {

    interface Listener {
        fun onAudioError(cause: Throwable) {}
        fun onFirstFramePlayed() {}
    }

    private data class Frame(val data: ByteArray, val ptsUs: Long, val flags: Int)

    /** Potongan PCM hasil decoder beserta PTS-nya (untuk diag latency). */
    private data class Pcm(val data: ByteArray, val ptsUs: Long)

    private val lock = Object()
    /** Thread luar untuk memanggil stop()/release() — tidak boleh dari callback. */
    private val disposeHandler = Handler(Looper.getMainLooper())
    private var codec: MediaCodec? = null
    private var codecThread: HandlerThread? = null
    private var track: AudioTrack? = null

    private var mime: String? = null
    private var config: ByteArray? = null
    private var pendingConfig: ByteArray? = null

    /**
     * Antrean PCM menuju AudioTrack. Ditulis dari callback MediaCodec,
     * dibaca dari thread penulis khusus — sehingga `AudioTrack.write()` yang
     * memblokir TIDAK PERNAH menahan lock dan tidak menghentikan reader ADB.
     * Antrean dibatasi (drop-oldest) agar latency tidak pernah menumpuk.
     */
    private val writeQueue = ArrayBlockingQueue<Pcm>(MAX_WRITE_CHUNKS)
    @Volatile private var writerThread: Thread? = null

    /** Timestamp untuk diag latency (nanos, System.nanoTime()). */
    private var firstPacketNs = 0L
    private var firstPcmNs = 0L
    private var playbackNs = 0L

    private val freeInputs = ArrayDeque<Int>()
    private val queued = ArrayDeque<Frame>()

    private var started = false
    private var closed = false
    private var announcedFirst = false

    /**
     * Decoder bisa gagal berulang. Tanpa jeda, `onError` → `started=false`
     * → paket berikutnya langsung bikin codec baru → **badai restart**
     * (~30 codec + HandlerThread per detik) yang akhirnya membuat native
     * use-after-free di thread `scrcpy-audio` → SIGSEGV (force-close).
     */
    private var failCount = 0
    private var nextRetryAtMs = 0L
    private var disabled = false
    private var loggedConfig = false
    private var loggedFirstPacket = false

    init {
        if (codecId == AudioCodec.RAW.fourcc) {
            // PCM: tidak butuh decoder, hanya AudioTrack.
            synchronized(lock) { startRawTrackLocked() }
        }
    }

    fun onConfig(data: ByteArray) {
        synchronized(lock) {
            if (closed || codecId == AudioCodec.RAW.fourcc) return
            if (!loggedConfig) {
                loggedConfig = true
                AppLog.log("audio config: ${data.size} byte hex=${hexOf(data)}")
            }
            val csdBerubah = config.contentEquals(data).not()
            config = data
            pendingConfig = data
            // csd-0 baru datang setelah decoder jalan → paksa mulai ulang
            // supaya header Opus benar-benar terpasang.
            if (csdBerubah && started) releaseCodecLocked()
            tryStartLocked()
        }
    }

    fun onPacket(header: PacketHeader, payload: ByteArray) {
        synchronized(lock) {
            if (closed) return
            if (codecId == AudioCodec.RAW.fourcc) {
                writeRawLocked(payload)
                return
            }
            // Beberapa device tidak pernah mengirim paket config audio
            // (encoder tidak menyetel BUFFER_FLAG_CODEC_CONFIG). Tanpa csd-0,
            // decoder langsung gagal ("work failed to complete: 14") → badai
            // restart → force-close. Sintesis CSD dari parameter stream scrcpy
            // (AudioConfig.java: 48000 Hz / stereo).
            if (config == null) {
                val synth = when (codecId) {
                    AudioCodec.OPUS.fourcc -> defaultOpusHead()
                    AudioCodec.AAC.fourcc -> defaultAacConfig()
                    else -> null
                }
                if (synth != null) {
                    config = synth
                    pendingConfig = synth
                    AppLog.log(
                        "audio: server tidak kirim config → sintesis CSD " +
                            "${AudioCodec.entries.firstOrNull { it.fourcc == codecId }?.wire} " +
                            "${SAMPLE_RATE}Hz/${CHANNEL_COUNT}ch",
                    )
                }
            }
            if (!tryStartLocked()) return
            if (!loggedFirstPacket) {
                loggedFirstPacket = true
                firstPacketNs = System.nanoTime()
                AppLog.log(
                    "audio paket pertama: ${payload.size} byte pts=${header.ptsUs}",
                )
            }
            enqueueLocked(Frame(payload, header.ptsUs, header.flagsForInput()))
        }
    }

    /**
     * OpusHead bawaan untuk 48000 Hz stereo (channel mapping family 0).
     * Dipakai hanya bila server tidak mengirim paket config.
     */
    private fun defaultOpusHead(): ByteArray = byteArrayOf(
        'O'.code.toByte(), 'p'.code.toByte(), 'u'.code.toByte(), 's'.code.toByte(),
        'H'.code.toByte(), 'e'.code.toByte(), 'a'.code.toByte(), 'd'.code.toByte(),
        0x01, // versi
        CHANNEL_COUNT.toByte(), // jumlah channel
        0x00, 0x00, // pre-skip
        0x80.toByte(), 0xBB.toByte(), 0x00, 0x00, // sample rate 48000 (LE)
        0x00, 0x00, // output gain
        0x00, // channel mapping family 0
    )

    /**
     * AudioSpecificConfig untuk AAC-LC 48000 Hz stereo (0x11 0x90):
     * object type 2 (AAC-LC), sampling frequency index 3 (48000), channel 2.
     * Dipakai bila encoder tidak menyetel BUFFER_FLAG_CODEC_CONFIG.
     */
    private fun defaultAacConfig(): ByteArray = byteArrayOf(0x11, 0x90.toByte())

    fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            releaseCodecLocked()
            queued.clear()
            writeQueue.clear()
            track?.let {
                runCatching { it.pause() }
                runCatching { it.release() }
            }
            track = null
        }
        writerThread?.interrupt()
        writerThread = null
    }

    /** Masukkan PCM ke antrean penulis; drop yang paling lama bila penuh. */
    private fun offerPcmLocked(pcm: Pcm) {
        while (!writeQueue.offer(pcm)) {
            writeQueue.poll()
        }
    }

    private fun PacketHeader.flagsForInput(): Int =
        if (isKeyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0

    // ------------------------------------------------------------------ impl

    private fun tryStartLocked(): Boolean {
        if (started) return true
        if (disabled) return false
        // CSD (OpusHead) wajib ada; tanpa csd-0 decoder langsung menolak
        // paket pertamanya → error beruntun.
        if (codecId != AudioCodec.RAW.fourcc && config == null) return false
        if (System.currentTimeMillis() < nextRetryAtMs) return false
        val candidates = mimeCandidates(codecId)
        if (candidates.isEmpty()) return false

        // Buang codec lama (mis. sisa onError) sebelum bikin yang baru,
        // kalau tidak: referensi lama tertimpa & tidak pernah di-release.
        releaseCodecLocked()

        var created: MediaCodec? = null
        return try {
            val thread = HandlerThread("scrcpy-audio").apply { start() }
            codecThread = thread
            val handler = Handler(thread.looper)

            // Cari decoder yang benar-benar tersedia; nama MIME Opus berbeda
            // antar perangkat ("audio/ogg" vs "audio/opus").
            var resolved: String? = null
            for (candidate in candidates) {
                val codec = runCatching { MediaCodec.createDecoderByType(candidate) }
                    .getOrNull() ?: continue
                created = codec
                resolved = candidate
                break
            }
            val mimeUsed = resolved
                ?: throw IllegalStateException(
                    "Decoder audio tidak tersedia: ${candidates.joinToString()}",
                )
            mime = mimeUsed

            val format = MediaFormat.createAudioFormat(mimeUsed, SAMPLE_RATE, CHANNEL_COUNT)
                .apply {
                    setInteger(
                        MediaFormat.KEY_MAX_INPUT_SIZE,
                        maxOf(1 shl 16, config?.size ?: 0),
                    )
                    config?.let { setByteBuffer("csd-0", ByteBuffer.wrap(it)) }
                }

            val codec = created
                ?: throw IllegalStateException("Decoder audio tidak tersedia")
            codec.setCallback(audioCallback, handler)
            codec.configure(format, null, null, 0)
            codec.start()
            this.codec = codec
            created = null // sudah dipindah kepemilikannya ke this.codec
            AppLog.log(
                "audio decoder: mime=$mimeUsed csd=${config?.size ?: 0} name=" +
                    (runCatching { codec.name }.getOrNull() ?: "?"),
            )
            startTrackLocked()
            started = true
            true
        } catch (t: Throwable) {
            started = false
            runCatching { created?.release() }
            releaseCodecLocked()
            recordFailureLocked("gagal mulai: $t")
            listener.onAudioError(t)
            false
        }
    }

    private fun startTrackLocked() {
        if (track != null) return
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, encoding)
        // Buffer kecil (≈100 ms): buffer besar membuat audio tertinggal jauh
        // dari video karena AudioTrack menyimpan semua data yang masuk sampai
        // penuh. Dengan thread penulis khusus, write() yang memblokir hanya
        // menahan thread itu sendiri.
        val bufSize = maxOf(minBuf, SAMPLE_RATE * CHANNEL_COUNT * 2 / 10)
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(encoding)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_MASK)
                    .build()
            )
            .setBufferSizeInBytes(bufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = t
        // Mulai playback sesegera mungkin, jangan tunggu buffer penuh.
        val thr = runCatching { t.setStartThresholdInFrames(START_THRESHOLD_FRAMES) }
            .getOrDefault(-1)
        val state = runCatching { t.state }.getOrDefault(-1)
        val playResult = runCatching { t.play() }.fold(
            onSuccess = { "ok" },
            onFailure = { it.toString() },
        )
        startWriterLocked()
        AppLog.log(
            "audiotrack: minBuf=$minBuf bufSize=$bufSize " +
                "threshold=$thr state=$state play=$playResult",
        )
    }

    private fun startWriterLocked() {
        if (writerThread?.isAlive == true) return
        val t = Thread({
            var lastHead = 0L
            while (!closed) {
                val pcm = try {
                    writeQueue.poll(100, TimeUnit.MILLISECONDS)
                } catch (_: InterruptedException) {
                    break
                } ?: continue
                // Tulis TANPA lock: lock hanya dipakai untuk menyalakan/mematikan
                // track, dan AudioTrack.write() bisa memblokir ratusan ms.
                val target = synchronized(lock) {
                    if (closed) null else track
                } ?: break
                try {
                    target.write(pcm.data, 0, pcm.data.size)
                    if (firstPcmNs == 0L) {
                        firstPcmNs = System.nanoTime()
                        AppLog.log(
                            "audio: pcm pertama ditulis " +
                                "dt=${(firstPcmNs - firstPacketNs) / 1_000_000}ms " +
                                "pts=${pcm.ptsUs}",
                        )
                    }
                    if (playbackNs == 0L) {
                        val head = runCatching { target.playbackHeadPosition.toLong() }
                            .getOrDefault(0L)
                        if (head > 0 && head > lastHead) {
                            playbackNs = System.nanoTime()
                            AppLog.log(
                                "audio: audiotrack mulai bunyi " +
                                    "dt=${(playbackNs - firstPacketNs) / 1_000_000}ms head=$head",
                            )
                        }
                        lastHead = head
                    }
                } catch (_: IllegalStateException) {
                } catch (_: IllegalArgumentException) {
                }
            }
        }, "scrcpy-audio-out").apply { isDaemon = true; start() }
        writerThread = t
    }

    private fun startRawTrackLocked() {
        startTrackLocked()
        started = true
    }

    private fun writeRawLocked(payload: ByteArray) {
        startRawTrackLocked()
        val t = track ?: return
        try {
            t.write(payload, 0, payload.size)
            announceFirst()
        } catch (_: IllegalStateException) {
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun enqueueLocked(frame: Frame) {
        if (freeInputs.isEmpty() && queued.size >= MAX_QUEUED) {
            queued.pollFirst()
        }
        queued.addLast(frame)
        fillInputLocked()
    }

    private fun fillInputLocked() {
        val c = codec ?: return
        while (freeInputs.isNotEmpty() && queued.isNotEmpty()) {
            val index = freeInputs.pollFirst()
            val frame = queued.pollFirst()
            try {
                val buf = c.getInputBuffer(index) ?: continue
                buf.clear()
                if (frame.data.size > buf.capacity()) {
                    runCatching { c.queueInputBuffer(index, 0, 0, 0, 0) }
                    continue
                }
                buf.put(frame.data)
                c.queueInputBuffer(index, 0, frame.data.size, frame.ptsUs, frame.flags)
            } catch (_: Throwable) {
                // codec bisa saja baru di-release; jangan sampai lolos
            }
        }
    }

    private val audioCallback = object : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            // Exception yang lolos dari callback MediaCodec = proses dibunuh
            // Android, jadi semua Throwable ditahan.
            try {
                synchronized(lock) {
                    if (closed || this@AudioPlayer.codec !== codec) return
                    freeInputs.addLast(index)
                    fillInputLocked()
                }
            } catch (_: Throwable) {
            }
        }

        override fun onOutputBufferAvailable(
            codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo,
        ) {
            try {
                synchronized(lock) {
                    if (closed || this@AudioPlayer.codec !== codec) return
                    try {
                        // Semua akses codec di dalam lock. PCM tidak ditulis ke
                        // AudioTrack di sini: cukup masuk antrean penulis, supaya
                        // write() yang memblokir tidak menghentikan callback
                        // MediaCodec (yang juga menangani input buffer).
                        if (info.size > 0) {
                            val buf = codec.getOutputBuffer(index)
                            if (buf != null) {
                                buf.position(info.offset)
                                buf.limit(info.offset + info.size)
                                val bytes = ByteArray(info.size)
                                buf.get(bytes)
                                offerPcmLocked(Pcm(bytes, info.presentationTimeUs))
                                // Decoder berhasil → reset penghitung gagal.
                                failCount = 0
                                nextRetryAtMs = 0
                                announceFirst()
                            }
                        }
                        codec.releaseOutputBuffer(index, false)
                    } catch (_: Throwable) {
                    }
                }
            } catch (_: Throwable) {
            }
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            try {
                synchronized(lock) {
                    if (closed || this@AudioPlayer.codec !== codec) return
                    pendingConfig = config
                    started = false
                    AppLog.log(
                        "audio error: code=0x${e.errorCode.toString(16)}" +
                            " rec=${e.isRecoverable} trans=${e.isTransient}" +
                            " diag=${e.diagnosticInfo.take(240)}",
                    )
                    recordFailureLocked(null)
                }
                // stop()/release() DILARANG dari thread callback MediaCodec.
                disposeHandler.post {
                    synchronized(lock) {
                        if (this@AudioPlayer.codec === codec) releaseCodecLocked()
                    }
                }
                listener.onAudioError(e)
            } catch (_: Throwable) {
            }
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) = Unit
    }

    /**
     * Catat kegagalan decoder & terapkan jeda sebelum mencoba lagi.
     * Setelah [MAX_FAILS] kegagalan berturut-turut audio dimatikan total —
     * lebih baik tidak ada suara daripada badai restart → force-close.
     */
    private fun recordFailureLocked(detail: String?) {
        failCount++
        if (failCount >= MAX_FAILS) {
            disabled = true
            started = false
            AppLog.log(
                "audio dimatikan setelah $failCount kegagalan" +
                    (detail?.let { " ($it)" } ?: ""),
            )
            return
        }
        val backoff = RETRY_BACKOFF_MS * failCount
        nextRetryAtMs = System.currentTimeMillis() + backoff
        AppLog.log(
            "audio: coba lagi ke-${failCount + 1}/$MAX_FAILS dalam ${backoff}ms" +
                (detail?.let { " ($it)" } ?: ""),
        )
    }

    private fun announceFirst() {
        if (announcedFirst) return
        announcedFirst = true
        listener.onFirstFramePlayed()
    }

    private fun hexOf(data: ByteArray, max: Int = 64): String {
        val n = minOf(data.size, max)
        val sb = StringBuilder(n * 2 + 12)
        for (i in 0 until n) {
            val b = data[i].toInt() and 0xFF
            sb.append(HEX[(b shr 4) and 0xF]).append(HEX[b and 0xF])
        }
        if (data.size > n) sb.append("(${data.size}B)")
        return sb.toString()
    }

    private fun releaseCodecLocked() {
        started = false
        freeInputs.clear()
        val c = codec
        codec = null
        val t = codecThread
        codecThread = null
        c?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        t?.quitSafely()
    }

    companion object {
        const val SAMPLE_RATE = 48000
        const val CHANNEL_COUNT = 2
        const val CHANNEL_MASK = AudioFormat.CHANNEL_OUT_STEREO
        private const val MAX_QUEUED = 16

        /** Potongan PCM yang boleh menumpuk di antrean penulis (≈340 ms). */
        private const val MAX_WRITE_CHUNKS = 8

        /** ≈10 ms PCM stereo 48 kHz (dalam frame). */
        private const val START_THRESHOLD_FRAMES = 480

        /** Kegagalan berturut-turut sebelum audio dimatikan selamanya. */
        private const val MAX_FAILS = 3
        private const val RETRY_BACKOFF_MS = 750L
        private const val HEX = "0123456789abcdef"

        fun mimeOf(codecId: Int): String? = mimeCandidates(codecId).firstOrNull()

        /** Kandidat MIME per codec, urut: yang umum dulu. */
        fun mimeCandidates(codecId: Int): List<String> = when (codecId) {
            // Utamakan "audio/opus" (elementary stream Opus → c2.android.opus).
            // "audio/ogg" = container; bila dipakai untuk frame Opus mentah,
            // parser-nya bisa korup memori native → SIGSEGV.
            AudioCodec.OPUS.fourcc -> listOf("audio/opus", "audio/ogg; codecs=opus", "audio/ogg")
            AudioCodec.AAC.fourcc -> listOf("audio/mp4a-latm")
            AudioCodec.FLAC.fourcc -> listOf("audio/flac")
            AudioCodec.RAW.fourcc -> listOf("audio/raw")
            else -> emptyList()
        }
    }
}
