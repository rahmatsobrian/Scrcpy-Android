package com.rahmatsobrian.scrcpy.scrcpy

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Surface
import com.rahmatsobrian.scrcpy.util.AppLog
import java.nio.ByteBuffer
import java.util.ArrayDeque

/**
 * Decode paket video scrcpy (H.264/H.265/AV1/VP8/VP9) menuju [Surface] lewat
 * [MediaCodec] mode async.
 *
 * Perilaku diambil dari klien desktop (`decoder.c` + `packet_merger.c`):
 * - paket **config** (CSD/SPS-PPS) tidak didecode, disimpan lalu **disisipkan
 *   ke depan paket media berikutnya** (hanya untuk H.26x) — di sisi Android
 *   CSD juga dipasang di `MediaFormat` saat `configure()`;
 * - paket session hanya memberi ukuran video (rotasi/resize) dan diumumkan
 *   ke UI lewat [Listener].
 */
class VideoDecoder(
    private val listener: Listener,
) {

    interface Listener {
        /** Ukuran video baru (mis. setelah rotasi). */
        fun onVideoSize(width: Int, height: Int, clientResized: Boolean) {}
        fun onDecoderError(cause: Throwable) {}
    }

    private data class Frame(val data: ByteArray, val ptsUs: Long, val flags: Int)

    private val lock = Object()
    /** Thread luar untuk memanggil stop()/release() — tidak boleh dari callback. */
    private val disposeHandler = Handler(Looper.getMainLooper())
    private var codec: MediaCodec? = null
    private var codecThread: HandlerThread? = null
    private var mime: String? = null
    private var width = 0
    private var height = 0
    private var surface: Surface? = null

    /** Config terakhir (mis. SPS+PPS) untuk CSD dan merger. */
    private var config: ByteArray? = null
    /** Config yang menunggu disisipkan ke paket media berikutnya. */
    private var pendingConfig: ByteArray? = null

    private val freeInputs = ArrayDeque<Int>()
    private val queued = ArrayDeque<Frame>()

    private var started = false
    private var closed = false

    /**
     * Sama seperti di [AudioPlayer]: `onError` → `started=false` → paket
     * berikutnya langsung bikin codec baru = **badai restart** yang berakhir
     * native use-after-free → SIGSEGV. Terapkan jeda & batas percobaan.
     */
    private var failCount = 0
    private var nextRetryAtMs = 0L
    private var disabled = false

    /**
     * `true` setelah ukuran video berubah (rotasi layar target) dan menunggu
     * **CSD/SPS baru** dari server.
     *
     * scrcpy-server selalu membuat ulang encoder saat resolusi berubah
     * (`SurfaceEncoder`: `capture.prepare()` → `mediaCodec.configure()` →
     * `streamer.writeSessionMeta()` → `encode()`), sehingga selalu ada paket
     * config baru tepat setelah paket session. Selama menunggu, paket media
     * dibuang agar tidak masuk ke codec yang berukuran lama.
     */
    private var awaitingConfig = false

    /** Dipertahankan agar UI bisa tahu codec aktif. */
    val videoCodecId: Int
        get() = synchronized(lock) { currentCodecId }

    private var currentCodecId = 0

    fun setCodec(codecId: Int) = synchronized(lock) {
        if (closed) return
        currentCodecId = codecId
        mime = codecIdToMime(codecId)
    }

    fun setSurface(newSurface: Surface?) {
        synchronized(lock) {
            if (closed) return
            val changed = surface !== newSurface
            surface = newSurface
            if (changed) {
                if (started) {
                    // Reconfigure: CSD harus dikirim ulang lewat merger.
                    releaseCodecLocked()
                    pendingConfig = config ?: pendingConfig
                }
                tryStartLocked()
            }
        }
    }

    fun onSession(width: Int, height: Int, clientResized: Boolean) {
        synchronized(lock) {
            if (closed) return
            val resized = this.width > 0 && (this.width != width || this.height != height)
            this.width = width
            this.height = height
            listener.onVideoSize(width, height, clientResized)
            if (resized) {
                // Resolusi berubah (rotasi layar target). Codec lama sudah pasti
                // dikonfigurasi untuk ukuran lama, jadi dilepas dulu dan tunggu
                // CSD baru — sambil buang antrian frame lama.
                releaseCodecLocked()
                queued.clear()
                pendingConfig = null
                awaitingConfig = true
            }
            if (config != null) tryStartLocked()
        }
    }

    fun onConfig(data: ByteArray) {
        synchronized(lock) {
            if (closed) return
            config = data
            pendingConfig = data
            awaitingConfig = false
            tryStartLocked()
        }
    }

    fun onPacket(header: PacketHeader, payload: ByteArray) {
        synchronized(lock) {
            if (closed) return
            if (!tryStartLocked()) return

            var data = payload
            var flags = if (header.isKeyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
            pendingConfig?.let { cfg ->
                // Merger scrcpy: config di depan paket media pertama sesudahnya.
                data = cfg + payload
                pendingConfig = null
                flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
            }
            enqueueLocked(Frame(data, header.ptsUs, flags))
        }
    }

    fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            releaseCodecLocked()
            queued.clear()
            freeInputs.clear()
            surface = null
        }
    }

    // ------------------------------------------------------------------ impl

    /** @return true bila decoder sudah/siap menerima paket. */
    private fun tryStartLocked(): Boolean {
        if (started) return true
        if (disabled) return false
        if (awaitingConfig) return false // menunggu CSD baru setelah rotasi
        if (System.currentTimeMillis() < nextRetryAtMs) return false
        val s = surface ?: return false
        val m = mime ?: return false
        if (width <= 0 || height <= 0) return false

        // Buang codec lama (mis. sisa onError) sebelum bikin yang baru,
        // kalau tidak: referensi lama tertimpa & tidak pernah di-release.
        releaseCodecLocked()

        return try {
            val thread = HandlerThread("scrcpy-decoder").apply { start() }
            codecThread = thread
            val handler = Handler(thread.looper)

            // KEY_MAX_INPUT_SIZE sengaja tidak disetel agar decoder memakai
            // ukuran bawaan berdasarkan dimensi (hemat memori).
            val format = MediaFormat.createVideoFormat(m, width, height).apply {
                config?.let { setByteBuffer("csd-0", ByteBuffer.wrap(it)) }
            }

            val c = if (m == MediaFormat.MIMETYPE_VIDEO_AVC ||
                m == MediaFormat.MIMETYPE_VIDEO_HEVC
            ) {
                MediaCodec.createDecoderByType(m)
            } else {
                MediaCodec.createDecoderByType(m)
            }
            c.setCallback(codecCallback, handler)
            c.configure(format, s, null, 0)
            c.start()
            codec = c
            started = true
            AppLog.log(
                "video decoder: mime=$m ${width}x$height name=" +
                    (runCatching { c.name }.getOrNull() ?: "?"),
            )
            // Paket pertama setelah (re)configure harus membawa CSD kembali.
            pendingConfig = config ?: pendingConfig
            true
        } catch (t: Throwable) {
            started = false
            releaseCodecLocked()
            recordFailureLocked("gagal mulai: $t")
            listener.onDecoderError(t)
            false
        }
    }

    private fun enqueueLocked(frame: Frame) {
        if (freeInputs.isEmpty() && queued.size >= MAX_QUEUED) {
            queued.pollFirst() // buang frame terlama, stream tetap real-time
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
                    // Input buffer terlalu kecil untuk access unit ini; buang
                    // dan tunggu keyframe berikutnya.
                    runCatching { c.queueInputBuffer(index, 0, 0, 0, 0) }
                    continue
                }
                buf.put(frame.data)
                c.queueInputBuffer(index, 0, frame.data.size, frame.ptsUs, frame.flags)
            } catch (_: Throwable) {
                // Codec bisa sudah di-release/di-reset, atau index tidak valid
                // saat resolusi berubah (rotasi). Exception yang lolos dari
                // callback MediaCodec = proses dibunuh Android.
            }
        }
    }

    private val codecCallback = object : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            try {
                synchronized(lock) {
                    if (closed || this@VideoDecoder.codec !== codec) return
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
                    if (closed || this@VideoDecoder.codec !== codec) return
                    // Rilis buffer **di dalam lock**: bila di luar, releaseCodecLocked()
                    // bisa berjalan di antara pengecekan dan pemanggilan →
                    // releaseOutputBuffer() pada codec yang sudah di-release = SIGSEGV.
                    if (info.size > 0) {
                        // Berhasil decode → reset penghitung kegagalan.
                        failCount = 0
                        nextRetryAtMs = 0
                    }
                    codec.releaseOutputBuffer(index, info.size > 0)
                }
            } catch (_: Throwable) {
                // IllegalStateException bila codec sudah di-release; bila
                // merender ke surface yang baru saja berukuran ulang (rotasi
                // layar target) index bisa tidak valid → tangkap semuanya.
            }
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            try {
                synchronized(lock) {
                    if (closed || this@VideoDecoder.codec !== codec) return
                    pendingConfig = config
                    started = false
                    AppLog.log(
                        "video error: code=0x${e.errorCode.toString(16)}" +
                            " rec=${e.isRecoverable} trans=${e.isTransient}" +
                            " diag=${e.diagnosticInfo.take(240)}",
                    )
                    recordFailureLocked(null)
                }
                // PENTING: stop()/release() DILARANG dipanggil dari thread
                // callback MediaCodec (di sinilah onError berjalan) — Android
                // akan membunuh proses dengan SIGSEGV/SIGABRT. Jadwalkan ke
                // thread lain dan pastikan codec belum diganti.
                disposeHandler.post {
                    synchronized(lock) {
                        if (this@VideoDecoder.codec === codec) releaseCodecLocked()
                    }
                }
                listener.onDecoderError(e)
            } catch (_: Throwable) {
            }
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) = Unit
    }

    /**
     * Catat kegagalan decoder & terapkan jeda sebelum mencoba lagi.
     * Setelah [MAX_FAILS] kegagalan berturut-turut video dihentikan — lebih
     * baik layar beku daripada badai restart yang membuat force-close.
     */
    private fun recordFailureLocked(detail: String?) {
        failCount++
        if (failCount >= MAX_FAILS) {
            disabled = true
            started = false
            AppLog.log(
                "video dihentikan setelah $failCount kegagalan" +
                    (detail?.let { " ($it)" } ?: ""),
            )
            return
        }
        val backoff = RETRY_BACKOFF_MS * failCount
        nextRetryAtMs = System.currentTimeMillis() + backoff
        AppLog.log(
            "video: coba lagi ke-${failCount + 1}/$MAX_FAILS dalam ${backoff}ms" +
                (detail?.let { " ($it)" } ?: ""),
        )
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
        private const val MAX_QUEUED = 12
        private const val MAX_FAILS = 5
        private const val RETRY_BACKOFF_MS = 500L

        fun codecIdToMime(codecId: Int): String? = when (codecId) {
            VideoCodec.H264.fourcc -> MediaFormat.MIMETYPE_VIDEO_AVC
            VideoCodec.H265.fourcc -> MediaFormat.MIMETYPE_VIDEO_HEVC
            VideoCodec.AV1.fourcc -> MediaFormat.MIMETYPE_VIDEO_AV1
            VideoCodec.VP8.fourcc -> MediaFormat.MIMETYPE_VIDEO_VP8
            VideoCodec.VP9.fourcc -> MediaFormat.MIMETYPE_VIDEO_VP9
            else -> null
        }
    }
}
