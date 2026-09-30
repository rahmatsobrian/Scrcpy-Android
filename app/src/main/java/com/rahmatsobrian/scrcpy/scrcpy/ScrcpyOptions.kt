package com.rahmatsobrian.scrcpy.scrcpy

/**
 * Opsi sesi scrcpy 4.1.
 *
 * [toServerArgs] membangun argumen `key=value` yang dikirim ke scrcpy-server,
 * dengan urutan persis seperti `execute_server()` di `app/src/server.c:269-457`
 * agar server menerimanya dengan predikabel.
 */
data class ScrcpyOptions(
    // --- scrcpy core ---
    val scid: Int = randomScid(),
    val logLevel: LogLevel = LogLevel.INFO,

    // --- video (SERVER) ---
    val video: Boolean = true,
    val videoBitRate: Int = 8_000_000,
    val audio: Boolean = true,
    val audioBitRate: Int = 128_000,
    val videoCodec: VideoCodec = VideoCodec.H264,
    // AAC (bukan Opus): encoder+decoder AAC-LC diwajibkan CDD sehingga tersedia
    // di hampir semua perangkat; c2.android.opus.decoder di beberapa device
    // langsung gagal ("work failed to complete: 14") dan memicu badai restart → FC.
    val audioCodec: AudioCodec = AudioCodec.AAC,
    val videoSource: VideoSource = VideoSource.DISPLAY,
    val audioSource: String = "output",
    // true = ROUTE_FLAG_LOOP_BACK_RENDER: target tetap mengeluarkan suara, jadi
    // device aktif STREAM_MUSIC tetap "speaker". Dengan false scrcpy pakai
    // ROUTE_FLAG_LOOP_BACK saja → audio diarahkan ke remote_submix → tombol
    // volume menulis ke volume_music_remote_submix, dan setelah putus slider
    // kembali membaca volume_music_speaker yang tidak berubah ("kembali sendiri").
    val audioDup: Boolean = true,
    val maxSize: Int = 0,
    val maxFps: String? = null,
    val minSizeAlignment: Int = 1,
    val angle: String? = null,
    val captureOrientation: String? = null,
    val crop: String? = null,
    val control: Boolean = true,

    // --- display / virtual display (SERVER) ---
    val displayId: Int = 0,
    val newDisplay: String? = null,
    val flexDisplay: Boolean = false,
    val vdDestroyContent: Boolean = true,
    val vdSystemDecorations: Boolean = true,
    val displayImePolicy: String? = null,
    val ignoreVideoEncoderConstraints: Boolean = false,
    val downsizeOnError: Boolean = true,

    // --- kamera (SERVER) ---
    val cameraId: String? = null,
    val cameraSize: String? = null,
    val cameraFacing: String? = null,
    val cameraAr: String? = null,
    val cameraFps: Int = 0,
    val cameraHighSpeed: Boolean = false,
    val cameraTorch: Boolean = false,
    val cameraZoom: String? = null,

    // --- perilaku device (SERVER) ---
    val showTouches: Boolean = false,
    val stayAwake: Boolean = false,
    val screenOffTimeoutMs: Long? = null,
    val powerOffOnClose: Boolean = false,
    val powerOn: Boolean = true,
    val keepActive: Boolean = false,
    val clipboardAutosync: Boolean = true,
    val cleanup: Boolean = true,

    // --- encoder (SERVER) ---
    val videoCodecOptions: String? = null,
    val audioCodecOptions: String? = null,
    val videoEncoder: String? = null,
    val audioEncoder: String? = null,

    // --- list_* (SERVER, keluar setelah print) ---
    val listEncoders: Boolean = false,
    val listDisplays: Boolean = false,
    val listCameras: Boolean = false,
    val listCameraSizes: Boolean = false,
    val listApps: Boolean = false,

    // --- klien Android ini (tidak pernah masuk keServerArgs) ---
    /** `--no-mouse-hover`: forward gerak mouse tanpa tombol (`ACTION_HOVER_MOVE`). */
    val mouseHover: Boolean = true,
    /** `--prefer-text`: huruf/spasi dikirim sebagai event teks (mutlak eksklusif `rawKeyEvents`). */
    val preferText: Boolean = false,
    /** `--raw-key-events`: setiap tombol dikirim sebagai key event. */
    val rawKeyEvents: Boolean = false,
    /** `--no-key-repeat` = false: teruskan event tombol yang berulang saat ditahan. */
    val keyRepeat: Boolean = true,
    /** `-S/--turn-screen-off`: matikan layar device setelah sesi siap (kirim `SET_DISPLAY_POWER` off). */
    val turnScreenOff: Boolean = false,
    /** `--start-app`: jalankan aplikasi setelah sesi siap (prefix `?` dan `+` didukung server). */
    val startApp: String? = null,

    // --- tunnel: selalu forward pada klien Android ini ---
    val tunnelForward: Boolean = true,
) {

    /** Nama socket abstrak di device: `scrcpy_<scid>` (8 digit hex). */
    val socketName: String get() = "scrcpy_%08x".format(scid)

    val listRequested: Boolean
        get() = listEncoders || listDisplays || listCameras || listCameraSizes || listApps

    /**
     * Argumen server setelah `<VERSION>` (`/` dan `com.genymobile.scrcpy.Server`
     * ditambahkan oleh pemanggil). Urutan identik `execute_server()`.
     */
    fun toServerArgs(): List<String> {
        val args = mutableListOf<String>()
        args += "scid=%08x".format(scid)
        args += "log_level=${logLevel.wire}"

        if (!video) args += "video=false"
        if (video && videoBitRate != 0) args += "video_bit_rate=$videoBitRate"
        if (!audio) args += "audio=false"
        if (audio && audioBitRate != 0) args += "audio_bit_rate=$audioBitRate"
        if (videoCodec != VideoCodec.H264) args += "video_codec=${videoCodec.wire}"
        if (audioCodec != AudioCodec.OPUS) args += "audio_codec=${audioCodec.wire}"
        if (videoSource == VideoSource.CAMERA) args += "video_source=camera"
        if (audio && audioSource != "output") args += "audio_source=$audioSource"
        if (audioDup) args += "audio_dup=true"
        if (maxSize != 0) args += "max_size=$maxSize"
        if (maxFps != null) args += "max_fps=$maxFps"
        if (minSizeAlignment != 1) args += "min_size_alignment=$minSizeAlignment"
        if (angle != null) args += "angle=$angle"
        if (captureOrientation != null) args += "capture_orientation=$captureOrientation"
        if (tunnelForward) args += "tunnel_forward=true"
        if (crop != null) args += "crop=$crop"
        if (!control) args += "control=false"
        if (displayId != 0) args += "display_id=$displayId"

        cameraId?.let { args += "camera_id=$it" }
        cameraSize?.let { args += "camera_size=$it" }
        cameraFacing?.let { args += "camera_facing=$it" }
        cameraAr?.let { args += "camera_ar=$it" }
        if (cameraFps != 0) args += "camera_fps=$cameraFps"
        if (cameraHighSpeed) args += "camera_high_speed=true"
        if (cameraTorch) args += "camera_torch=true"
        cameraZoom?.let { args += "camera_zoom=$it" }

        if (showTouches) args += "show_touches=true"
        if (stayAwake) args += "stay_awake=true"
        screenOffTimeoutMs?.let { args += "screen_off_timeout=$it" }

        videoCodecOptions?.let { args += "video_codec_options=$it" }
        audioCodecOptions?.let { args += "audio_codec_options=$it" }
        videoEncoder?.let { args += "video_encoder=$it" }
        audioEncoder?.let { args += "audio_encoder=$it" }

        if (powerOffOnClose) args += "power_off_on_close=true"
        if (!clipboardAutosync) args += "clipboard_autosync=false"
        if (!downsizeOnError) args += "downsize_on_error=false"
        if (!cleanup) args += "cleanup=false"
        if (!powerOn) args += "power_on=false"

        newDisplay?.let { args += "new_display=$it" }
        if (flexDisplay) args += "flex_display=true"
        if (ignoreVideoEncoderConstraints) args += "ignore_video_encoder_constraints=true"
        displayImePolicy?.let { args += "display_ime_policy=$it" }
        if (!vdDestroyContent) args += "vd_destroy_content=false"
        if (!vdSystemDecorations) args += "vd_system_decorations=false"
        if (keepActive) args += "keep_active=true"

        if (listEncoders) args += "list_encoders=true"
        if (listDisplays) args += "list_displays=true"
        if (listCameras) args += "list_cameras=true"
        if (listCameraSizes) args += "list_camera_sizes=true"
        if (listApps) args += "list_apps=true"

        return args
    }

    /**
     * Perintah shell penuh untuk menjalankan server (tanpa `CLASSPATH=...` yang
     * jadi prefix). `args[0]` wajib versi server sesuai `Options.java:328`.
     */
    fun serverCommandLine(): String =
        (listOf("app_process", "/", "com.genymobile.scrcpy.Server", SERVER_VERSION) + toServerArgs())
            .joinToString(" ")

    companion object {
        const val SERVER_VERSION = "4.1"
        const val SERVER_PATH = "/data/local/tmp/scrcpy-server.jar"

        /** 31-bit acak, sama seperti `scrcpy.c:278-283`. */
        fun randomScid(): Int = (System.nanoTime() xor System.nanoTime() shl 16).toInt() and 0x7FFFFFFF
    }
}

enum class LogLevel(val wire: String) {
    VERBOSE("verbose"), DEBUG("debug"), INFO("info"), WARN("warn"), ERROR("error");
}

enum class VideoCodec(val wire: String, val fourcc: Int) {
    H264("h264", 0x68323634),
    H265("h265", 0x68323635),
    AV1("av1", 0x00617631),
    VP8("vp8", 0x00767038),
    VP9("vp9", 0x00767039);

    val mime: String
        get() = when (this) {
            H264 -> "video/avc"
            H265 -> "video/hevc"
            AV1 -> "video/av01"
            VP8 -> "video/x-vnd.on2.vp8"
            VP9 -> "video/x-vnd.on2.vp9"
        }
}

enum class AudioCodec(val wire: String, val fourcc: Int) {
    OPUS("opus", 0x6F707573),
    AAC("aac", 0x00616163),
    FLAC("flac", 0x666C6163),
    RAW("raw", 0x00726177);

    val mime: String?
        get() = when (this) {
            OPUS -> "audio/ogg"
            AAC -> "audio/mp4a-latm"
            FLAC -> "audio/flac"
            RAW -> "audio/raw"
        }
}

enum class VideoSource { DISPLAY, CAMERA }
