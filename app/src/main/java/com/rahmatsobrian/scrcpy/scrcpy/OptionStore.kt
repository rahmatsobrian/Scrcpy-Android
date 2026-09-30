package com.rahmatsobrian.scrcpy.scrcpy

import android.content.Context
import android.content.SharedPreferences

/**
 * Penyimpanan otomatis [ScrcpyOptions] di SharedPreferences.
 *
 * Setiap perubahan dari layar Pengaturan langsung ditulis, sehingga pengguna
 * tidak perlu menekan tombol simpan dan nilai bertahan setelah aplikasi
 * ditutup. `scid` sengaja tidak disimpan karena dibuat acak tiap sesi.
 */
object OptionStore {

    private const val PREFS = "scrcpy_options"

    fun load(context: Context): ScrcpyOptions {
        val d = ScrcpyOptions()
        val p = prefs(context)
        if (!p.contains(KEY_PRESENT)) return d
        return ScrcpyOptions(
            scid = d.scid,
            logLevel = enumOf(p, "logLevel", LogLevel.entries, d.logLevel),
            video = p.getBoolean("video", d.video),
            videoBitRate = p.getInt("videoBitRate", d.videoBitRate),
            audio = p.getBoolean("audio", d.audio),
            audioBitRate = p.getInt("audioBitRate", d.audioBitRate),
            videoCodec = enumOf(p, "videoCodec", VideoCodec.entries, d.videoCodec),
            audioCodec = enumOf(p, "audioCodec", AudioCodec.entries, d.audioCodec),
            videoSource = enumOf(p, "videoSource", VideoSource.entries, d.videoSource),
            audioSource = p.getString("audioSource", d.audioSource) ?: d.audioSource,
            audioDup = p.getBoolean("audioDup", d.audioDup),
            maxSize = p.getInt("maxSize", d.maxSize),
            maxFps = p.getString("maxFps", d.maxFps),
            minSizeAlignment = p.getInt("minSizeAlignment", d.minSizeAlignment),
            angle = p.getString("angle", d.angle),
            captureOrientation = p.getString("captureOrientation", d.captureOrientation),
            crop = p.getString("crop", d.crop),
            control = p.getBoolean("control", d.control),
            displayId = p.getInt("displayId", d.displayId),
            newDisplay = p.getString("newDisplay", d.newDisplay),
            flexDisplay = p.getBoolean("flexDisplay", d.flexDisplay),
            vdDestroyContent = p.getBoolean("vdDestroyContent", d.vdDestroyContent),
            vdSystemDecorations = p.getBoolean("vdSystemDecorations", d.vdSystemDecorations),
            displayImePolicy = p.getString("displayImePolicy", d.displayImePolicy),
            ignoreVideoEncoderConstraints = p.getBoolean(
                "ignoreVideoEncoderConstraints",
                d.ignoreVideoEncoderConstraints,
            ),
            downsizeOnError = p.getBoolean("downsizeOnError", d.downsizeOnError),
            cameraId = p.getString("cameraId", d.cameraId),
            cameraSize = p.getString("cameraSize", d.cameraSize),
            cameraFacing = p.getString("cameraFacing", d.cameraFacing),
            cameraAr = p.getString("cameraAr", d.cameraAr),
            cameraFps = p.getInt("cameraFps", d.cameraFps),
            cameraHighSpeed = p.getBoolean("cameraHighSpeed", d.cameraHighSpeed),
            cameraTorch = p.getBoolean("cameraTorch", d.cameraTorch),
            cameraZoom = p.getString("cameraZoom", d.cameraZoom),
            showTouches = p.getBoolean("showTouches", d.showTouches),
            stayAwake = p.getBoolean("stayAwake", d.stayAwake),
            screenOffTimeoutMs = if (p.contains("screenOffTimeoutMs")) {
                p.getLong("screenOffTimeoutMs", d.screenOffTimeoutMs ?: -1L)
            } else {
                d.screenOffTimeoutMs
            },
            powerOffOnClose = p.getBoolean("powerOffOnClose", d.powerOffOnClose),
            powerOn = p.getBoolean("powerOn", d.powerOn),
            keepActive = p.getBoolean("keepActive", d.keepActive),
            clipboardAutosync = p.getBoolean("clipboardAutosync", d.clipboardAutosync),
            cleanup = p.getBoolean("cleanup", d.cleanup),
            videoCodecOptions = p.getString("videoCodecOptions", d.videoCodecOptions),
            audioCodecOptions = p.getString("audioCodecOptions", d.audioCodecOptions),
            videoEncoder = p.getString("videoEncoder", d.videoEncoder),
            audioEncoder = p.getString("audioEncoder", d.audioEncoder),
            mouseHover = p.getBoolean("mouseHover", d.mouseHover),
            preferText = p.getBoolean("preferText", d.preferText),
            rawKeyEvents = p.getBoolean("rawKeyEvents", d.rawKeyEvents),
            keyRepeat = p.getBoolean("keyRepeat", d.keyRepeat),
            turnScreenOff = p.getBoolean("turnScreenOff", d.turnScreenOff),
            startApp = p.getString("startApp", d.startApp),
            tunnelForward = p.getBoolean("tunnelForward", d.tunnelForward),
        )
    }

    fun save(context: Context, o: ScrcpyOptions) {
        prefs(context).edit().apply {
            putBoolean(KEY_PRESENT, true)
            putString("logLevel", o.logLevel.name)
            putBoolean("video", o.video)
            putInt("videoBitRate", o.videoBitRate)
            putBoolean("audio", o.audio)
            putInt("audioBitRate", o.audioBitRate)
            putString("videoCodec", o.videoCodec.name)
            putString("audioCodec", o.audioCodec.name)
            putString("videoSource", o.videoSource.name)
            putString("audioSource", o.audioSource)
            putBoolean("audioDup", o.audioDup)
            putInt("maxSize", o.maxSize)
            putString("maxFps", o.maxFps)
            putInt("minSizeAlignment", o.minSizeAlignment)
            putString("angle", o.angle)
            putString("captureOrientation", o.captureOrientation)
            putString("crop", o.crop)
            putBoolean("control", o.control)
            putInt("displayId", o.displayId)
            putString("newDisplay", o.newDisplay)
            putBoolean("flexDisplay", o.flexDisplay)
            putBoolean("vdDestroyContent", o.vdDestroyContent)
            putBoolean("vdSystemDecorations", o.vdSystemDecorations)
            putString("displayImePolicy", o.displayImePolicy)
            putBoolean("ignoreVideoEncoderConstraints", o.ignoreVideoEncoderConstraints)
            putBoolean("downsizeOnError", o.downsizeOnError)
            putString("cameraId", o.cameraId)
            putString("cameraSize", o.cameraSize)
            putString("cameraFacing", o.cameraFacing)
            putString("cameraAr", o.cameraAr)
            putInt("cameraFps", o.cameraFps)
            putBoolean("cameraHighSpeed", o.cameraHighSpeed)
            putBoolean("cameraTorch", o.cameraTorch)
            putString("cameraZoom", o.cameraZoom)
            putBoolean("showTouches", o.showTouches)
            putBoolean("stayAwake", o.stayAwake)
            if (o.screenOffTimeoutMs != null) {
                putLong("screenOffTimeoutMs", o.screenOffTimeoutMs)
            } else {
                remove("screenOffTimeoutMs")
            }
            putBoolean("powerOffOnClose", o.powerOffOnClose)
            putBoolean("powerOn", o.powerOn)
            putBoolean("keepActive", o.keepActive)
            putBoolean("clipboardAutosync", o.clipboardAutosync)
            putBoolean("cleanup", o.cleanup)
            putString("videoCodecOptions", o.videoCodecOptions)
            putString("audioCodecOptions", o.audioCodecOptions)
            putString("videoEncoder", o.videoEncoder)
            putString("audioEncoder", o.audioEncoder)
            putBoolean("mouseHover", o.mouseHover)
            putBoolean("preferText", o.preferText)
            putBoolean("rawKeyEvents", o.rawKeyEvents)
            putBoolean("keyRepeat", o.keyRepeat)
            putBoolean("turnScreenOff", o.turnScreenOff)
            putString("startApp", o.startApp)
            putBoolean("tunnelForward", o.tunnelForward)
            apply()
        }
    }

    private const val KEY_PRESENT = "present"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun <E : Enum<E>> enumOf(
        p: SharedPreferences,
        key: String,
        all: List<E>,
        default: E,
    ): E {
        val name = p.getString(key, null) ?: return default
        return all.firstOrNull { it.name == name } ?: default
    }
}
