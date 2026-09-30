@file:kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.rahmatsobrian.scrcpy.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.FlipCameraAndroid
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Height
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShortText
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.SurroundSound
import androidx.compose.material.icons.outlined.SwitchVideo
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoSettings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Window
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rahmatsobrian.scrcpy.scrcpy.AudioCodec
import com.rahmatsobrian.scrcpy.scrcpy.LogLevel
import com.rahmatsobrian.scrcpy.scrcpy.ScrcpyOptions
import com.rahmatsobrian.scrcpy.scrcpy.VideoCodec
import com.rahmatsobrian.scrcpy.scrcpy.VideoSource
import com.rahmatsobrian.scrcpy.ui.AppViewModel
import com.rahmatsobrian.scrcpy.ui.ListKind
import com.rahmatsobrian.scrcpy.ui.i18n.AppLang
import com.rahmatsobrian.scrcpy.ui.i18n.Lang
import com.rahmatsobrian.scrcpy.ui.i18n.t
import com.rahmatsobrian.scrcpy.ui.theme.ThemeMode
import com.rahmatsobrian.scrcpy.ui.theme.ThemeState
import com.rahmatsobrian.scrcpy.util.AppLog
import kotlin.math.abs

/**
 * Layar pengaturan: seluruh 111 opsi scrcpy 4.1 dikelompokkan per kategori.
 * Nilai disimpan pada [ScrcpyOptions] di ViewModel dan diterapkan pada sesi
 * berikutnya.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val options by vm.options.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()

    // Setiap perubahan langsung disimpan ke SharedPreferences oleh AppViewModel,
    // jadi tidak ada tombol simpan dan nilai bertahan setelah aplikasi ditutup.
    fun update(block: (ScrcpyOptions) -> ScrcpyOptions) {
        vm.setOptions(block(options))
    }

    fun reset() {
        vm.setOptions(ScrcpyOptions(scid = options.scid))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("scrcpy Settings")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Back"))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { vm.reloadOptions() },
                        enabled = !refreshing,
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = t("Reload"))
                    }
                    TextButton(onClick = { reset() }) { Text(t("Reset")) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
        // Indikator reload: meluncur turun keluar dari dalam header,
        // sehingga seluruh daftar di bawahnya ikut terdorong ke bawah.
        AnimatedVisibility(
            visible = refreshing,
            enter = expandVertically(tween(260)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(140)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                ContainedLoadingIndicator(Modifier.size(48.dp))
            }
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionHeader("Appearance", "Theme, accent color and interface language.") }
            item {
                SettingCard {
                    EnumChoice(
                        icon = Icons.Outlined.Palette,
                        label = "Theme",
                        description = "Light, dark, true-black AMOLED, or follow the system setting.",
                        value = ThemeState.mode,
                        choices = ThemeMode.entries,
                        toString = {
                            t(
                                when (it) {
                                    ThemeMode.SYSTEM -> "System"
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                    ThemeMode.AMOLED -> "AMOLED"
                                },
                            )
                        },
                        onSelect = { ThemeState.setMode(context, it) },
                    )
                    SwitchRow(
                        "Dynamic color",
                        "Build the palette from your wallpaper, like Material You (Android 12+).",
                        ThemeState.dynamicColor,
                        icon = Icons.Outlined.AutoAwesome,
                    ) {
                        ThemeState.setDynamicColor(context, it)
                    }
                    SeedColorRow(enabled = !ThemeState.dynamicColor)
                    EnumChoice(
                        icon = Icons.Outlined.Language,
                        label = "Language",
                        description = "Language used across the whole app.",
                        value = Lang.current,
                        choices = AppLang.entries,
                        toString = { it.endonym },
                        onSelect = { Lang.set(context, it) },
                    )
                }
            }

            item { SectionHeader("Video", "Source, codec and quality of the video stream from the target device.") }
            item {
                SettingCard {
                    SwitchRow(
                        "Capture video",
                        "Turn off to connect without a video stream; audio and control keep working.",
                        options.video,
                        icon = Icons.Outlined.Videocam,
                    ) {
                        update { o -> o.copy(video = it) }
                    }
                    EnumChoice(
                        icon = Icons.Outlined.VideoSettings,
                        label = "Video codec",
                        description = "Video encoder codec used by the scrcpy server.",
                        value = options.videoCodec,
                        choices = VideoCodec.entries,
                        toString = { it.wire.uppercase() },
                        onSelect = { update { o -> o.copy(videoCodec = it) } },
                    )
                    PickerField(
                        icon = Icons.Outlined.DataUsage,
                        label = "Video bit rate (bps)",
                        description = "Target bitrate of the video encoder; 0 means automatic.",
                        options = listOf(
                            "Automatic" to "0",
                            "1 Mbps" to "1000000",
                            "2 Mbps" to "2000000",
                            "4 Mbps" to "4000000",
                            "8 Mbps" to "8000000",
                            "16 Mbps" to "16000000",
                        ),
                        value = options.videoBitRate.toString(),
                        onChange = {
                            update { o -> o.copy(videoBitRate = it.toIntOrNull() ?: o.videoBitRate) }
                        },
                    )
                    SliderField(
                        icon = Icons.Outlined.CropFree,
                        label = "Maximum size (px, 0 = unlimited)",
                        description = "The longest side of a frame never exceeds this value.",
                        value = options.maxSize.toFloat(),
                        range = 0f..4096f,
                        steps = 15,
                        display = { if (it.toInt() == 0) "Unlimited" else "${it.toInt()} px" },
                        onChange = { update { o -> o.copy(maxSize = it.toInt()) } },
                    )
                    PickerField(
                        icon = Icons.Outlined.Speed,
                        label = "Maximum FPS",
                        description = "Cap the frame rate of the stream, for example 60.",
                        options = listOf(
                            "Unlimited" to "",
                            "24" to "24",
                            "30" to "30",
                            "60" to "60",
                            "90" to "90",
                            "120" to "120",
                        ),
                        value = options.maxFps.orEmpty(),
                        numeric = true,
                        onChange = { update { o -> o.copy(maxFps = it.ifBlank { null }) } },
                    )
                    EnumChoice(
                        icon = Icons.Outlined.SwitchVideo,
                        label = "Video source",
                        description = "Capture the device screen or its physical camera.",
                        value = options.videoSource,
                        choices = VideoSource.entries,
                        toString = { if (it == VideoSource.DISPLAY) "Display" else "Camera" },
                        onSelect = { update { o -> o.copy(videoSource = it) } },
                    )
                    TextFieldRow(
                        icon = Icons.Outlined.Crop,
                        label = "Crop (W:H:X:Y)",
                        description = "Cut the captured area using width:height:x:y.",
                        value = options.crop.orEmpty(),
                        placeholder = "empty = no crop",
                        onChange = { update { o -> o.copy(crop = it.ifBlank { null }) } },
                    )
                    PickerField(
                        icon = Icons.Outlined.Memory,
                        label = "Video encoder",
                        description = "Force a specific video encoder; empty for automatic.",
                        options = listOf(
                            "Automatic" to "",
                            "c2.android.avc.encoder" to "c2.android.avc.encoder",
                            "c2.android.hevc.encoder" to "c2.android.hevc.encoder",
                            "c2.android.av1.encoder" to "c2.android.av1.encoder",
                            "OMX.google.h264.encoder" to "OMX.google.h264.encoder",
                            "OMX.google.h265.encoder" to "OMX.google.h265.encoder",
                        ),
                        value = options.videoEncoder.orEmpty(),
                        onChange = { update { o -> o.copy(videoEncoder = it.ifBlank { null }) } },
                    )
                    TextFieldRow(
                        icon = Icons.Outlined.Tune,
                        label = "Video codec options",
                        description = "Extra options for the video encoder, key=value pairs separated by commas.",
                        value = options.videoCodecOptions.orEmpty(),
                        placeholder = "key=value,…",
                        onChange = { update { o -> o.copy(videoCodecOptions = it.ifBlank { null }) } },
                    )
                    PickerField(
                        icon = Icons.Outlined.ScreenRotation,
                        label = "Capture orientation",
                        description = "Rotate or mirror the capture, for example 90 or 0x1000.",
                        options = listOf(
                            "Automatic" to "",
                            "0" to "0",
                            "90" to "90",
                            "180" to "180",
                            "270" to "270",
                            "Mirror (0x1000)" to "0x1000",
                            "Mirror + 90 (0x1090)" to "0x1090",
                            "Mirror + 180 (0x10e0)" to "0x10e0",
                            "Mirror + 270 (0x1110)" to "0x1110",
                        ),
                        value = options.captureOrientation.orEmpty(),
                        onChange = {
                            update { o -> o.copy(captureOrientation = it.ifBlank { null }) }
                        },
                    )
                    PickerField(
                        icon = Icons.Outlined.Science,
                        label = "ANGLE backend (Android 10+)",
                        description = "ANGLE render backend used for video processing, for example sw.",
                        options = listOf(
                            "Automatic" to "",
                            "Software (sw)" to "sw",
                            "ANGLE (angle)" to "angle",
                        ),
                        value = options.angle.orEmpty(),
                        onChange = { update { o -> o.copy(angle = it.ifBlank { null }) } },
                    )
                    PickerField(
                        icon = Icons.Outlined.Height,
                        label = "Minimum size alignment",
                        description = "Round the frame width and height to a multiple of this value.",
                        options = listOf(
                            "1" to "1",
                            "2" to "2",
                            "4" to "4",
                            "8" to "8",
                            "16" to "16",
                        ),
                        value = options.minSizeAlignment.toString(),
                        numeric = true,
                        onChange = {
                            update { o -> o.copy(minSizeAlignment = it.toIntOrNull() ?: o.minSizeAlignment) }
                        },
                    )
                    SwitchRow(
                        "Ignore encoder constraints",
                        "Force the encoder to use the requested profile or level even if the target does not support it.",
                        options.ignoreVideoEncoderConstraints,
                        icon = Icons.Outlined.Construction,
                    ) { update { o -> o.copy(ignoreVideoEncoderConstraints = it) } }
                    SwitchRow(
                        "Downsize on error",
                        "Lower the resolution automatically and retry when the encoder fails.",
                        options.downsizeOnError,
                        icon = Icons.Outlined.SettingsSuggest,
                    ) { update { o -> o.copy(downsizeOnError = it) } }
                }
            }

            item { SectionHeader("Audio", "Source, codec and quality of the audio stream from the target device.") }
            item {
                SettingCard {
                    SwitchRow(
                        "Capture audio",
                        "Turn off to stop the audio stream coming from the target device.",
                        options.audio,
                        icon = Icons.Outlined.VolumeUp,
                    ) {
                        update { o -> o.copy(audio = it) }
                    }
                    EnumChoice(
                        icon = Icons.Outlined.GraphicEq,
                        label = "Audio codec",
                        description = "Server audio encoder. AAC is the default and works on almost every device.",
                        value = options.audioCodec,
                        choices = AudioCodec.entries,
                        toString = { it.wire.uppercase() },
                        onSelect = { update { o -> o.copy(audioCodec = it) } },
                    )
                    PickerField(
                        icon = Icons.Outlined.SignalCellularAlt,
                        label = "Audio bit rate (bps)",
                        description = "Target bitrate of the audio encoder.",
                        options = listOf(
                            "32 kbps" to "32000",
                            "64 kbps" to "64000",
                            "96 kbps" to "96000",
                            "128 kbps" to "128000",
                            "192 kbps" to "192000",
                            "256 kbps" to "256000",
                        ),
                        value = options.audioBitRate.toString(),
                        onChange = {
                            update { o -> o.copy(audioBitRate = it.toIntOrNull() ?: o.audioBitRate) }
                        },
                    )
                    PickerField(
                        icon = Icons.Outlined.SurroundSound,
                        label = "Audio source",
                        description = "Audio endpoint on the target device, for example output.",
                        options = listOf(
                            "Output (output)" to "output",
                            "Playback (playback)" to "playback",
                            "Microphone (mic)" to "mic",
                        ),
                        value = options.audioSource,
                        onChange = { update { o -> o.copy(audioSource = it.ifBlank { "output" }) } },
                    )
                    SwitchRow(
                        "Keep target audio playing",
                        "The target device keeps making sound while mirroring. Turn off only if you want it silent.",
                        options.audioDup,
                        icon = Icons.Outlined.Headphones,
                    ) {
                        update { o -> o.copy(audioDup = it) }
                    }
                    PickerField(
                        icon = Icons.Outlined.AudioFile,
                        label = "Audio encoder",
                        description = "Force a specific audio encoder; empty for automatic.",
                        options = listOf(
                            "Automatic" to "",
                            "c2.android.aac.encoder" to "c2.android.aac.encoder",
                            "OMX.google.aac.encoder" to "OMX.google.aac.encoder",
                            "c2.android.opus.encoder" to "c2.android.opus.encoder",
                            "c2.android.flac.encoder" to "c2.android.flac.encoder",
                        ),
                        value = options.audioEncoder.orEmpty(),
                        onChange = { update { o -> o.copy(audioEncoder = it.ifBlank { null }) } },
                    )
                    TextFieldRow(
                        icon = Icons.Outlined.Equalizer,
                        label = "Audio codec options",
                        description = "Extra options for the audio encoder, key=value pairs separated by commas.",
                        value = options.audioCodecOptions.orEmpty(),
                        placeholder = "key=value,…",
                        onChange = { update { o -> o.copy(audioCodecOptions = it.ifBlank { null }) } },
                    )
                }
            }

            item { SectionHeader("Display & control", "Screen behaviour, clipboard and input permissions on the target device.") }
            item {
                SettingCard {
                    PickerField(
                        icon = Icons.Outlined.Devices,
                        label = "Target display ID",
                        description = "Pick a physical display on the target device by its ID.",
                        options = listOf(
                            "0" to "0",
                            "1" to "1",
                            "2" to "2",
                        ),
                        value = options.displayId.toString(),
                        numeric = true,
                        onChange = { update { o -> o.copy(displayId = it.toIntOrNull() ?: o.displayId) } },
                    )
                    SwitchRow(
                        "Control (touch and type)",
                        "Turn off for view-only mode; gestures and keyboard are not sent to the device.",
                        options.control,
                        icon = Icons.Outlined.TouchApp,
                    ) {
                        update { o -> o.copy(control = it) }
                    }
                    SwitchRow(
                        "Show touches",
                        "Draw touch points and trails on the target device screen.",
                        options.showTouches,
                        icon = Icons.Outlined.Gesture,
                    ) {
                        update { o -> o.copy(showTouches = it) }
                    }
                    SwitchRow(
                        "Stay awake",
                        "Keep the target screen from sleeping while the session runs.",
                        options.stayAwake,
                        icon = Icons.Outlined.Visibility,
                    ) {
                        update { o -> o.copy(stayAwake = it) }
                    }
                    PickerField(
                        icon = Icons.Outlined.Schedule,
                        label = "Screen off timeout (ms)",
                        description = "Screen timeout of the target in milliseconds; -1 keeps the device default.",
                        options = listOf(
                            "Device default" to "",
                            "-1" to "-1",
                            "30 seconds" to "30000",
                            "60 seconds" to "60000",
                            "5 minutes" to "300000",
                        ),
                        value = options.screenOffTimeoutMs?.toString().orEmpty(),
                        numeric = true,
                        onChange = {
                            update { o -> o.copy(screenOffTimeoutMs = it.ifBlank { null }?.toLongOrNull()) }
                        },
                    )
                    SwitchRow(
                        "Turn screen off when closed",
                        "Switch off the target screen as soon as the scrcpy session ends.",
                        options.powerOffOnClose,
                        icon = Icons.Outlined.PowerSettingsNew,
                    ) {
                        update { o -> o.copy(powerOffOnClose = it) }
                    }
                    SwitchRow(
                        "Turn screen on at start",
                        "Wake the target screen when the connection begins.",
                        options.powerOn,
                        icon = Icons.Outlined.WbSunny,
                    ) {
                        update { o -> o.copy(powerOn = it) }
                    }
                    SwitchRow(
                        "Automatic clipboard sync",
                        "Two-way copy and paste between this device and the target.",
                        options.clipboardAutosync,
                        icon = Icons.Outlined.ContentPaste,
                    ) {
                        update { o -> o.copy(clipboardAutosync = it) }
                    }
                    SwitchRow(
                        "Clean up state when finished",
                        "Remove the extra state scrcpy created once the session closes.",
                        options.cleanup,
                        icon = Icons.Outlined.CleaningServices,
                    ) {
                        update { o -> o.copy(cleanup = it) }
                    }
                    SwitchRow(
                        "Keep session active",
                        "Keep the session alive even while the target screen is off.",
                        options.keepActive,
                        icon = Icons.Outlined.ToggleOn,
                    ) {
                        update { o -> o.copy(keepActive = it) }
                    }
                }
            }

            item { SectionHeader("Camera", "Physical camera of the target device instead of the screen.") }
            item {
                SettingCard {
                    PickerField(
                        icon = Icons.Outlined.CameraAlt,
                        label = "Camera ID",
                        description = "Pick a physical camera by its ID, for example 0.",
                        options = listOf(
                            "Automatic" to "",
                            "0" to "0",
                            "1" to "1",
                            "2" to "2",
                        ),
                        value = options.cameraId.orEmpty(),
                        numeric = true,
                        onChange = { update { o -> o.copy(cameraId = it.ifBlank { null }) } },
                    )
                    TextFieldRow(
                        icon = Icons.Outlined.CropSquare,
                        label = "Camera size (WxH)",
                        description = "Force the camera resolution; empty for automatic size.",
                        value = options.cameraSize.orEmpty(),
                        placeholder = "automatic",
                        onChange = { update { o -> o.copy(cameraSize = it.ifBlank { null }) } },
                    )
                    EnumChoice(
                        icon = Icons.Outlined.FlipCameraAndroid,
                        label = "Camera facing",
                        description = "Which camera to use: front, back, external or automatic.",
                        value = options.cameraFacing ?: "",
                        choices = listOf("front", "back", "external", ""),
                        toString = { it.ifEmpty { "Automatic" } },
                        onSelect = { update { o -> o.copy(cameraFacing = it.ifBlank { null }) } },
                    )
                    TextFieldRow(
                        icon = Icons.Outlined.AspectRatio,
                        label = "Camera aspect ratio",
                        description = "Aspect ratio of the camera, for example 4:3 or 16:9.",
                        value = options.cameraAr.orEmpty(),
                        placeholder = "e.g. 4:3 / 16:9",
                        onChange = { update { o -> o.copy(cameraAr = it.ifBlank { null }) } },
                    )
                    PickerField(
                        icon = Icons.Outlined.Speed,
                        label = "Camera FPS",
                        description = "Force the camera frame rate; empty or 0 means automatic.",
                        options = listOf(
                            "Automatic" to "0",
                            "24" to "24",
                            "30" to "30",
                            "60" to "60",
                            "120" to "120",
                        ),
                        value = options.cameraFps.toString(),
                        numeric = true,
                        onChange = { update { o -> o.copy(cameraFps = it.toIntOrNull() ?: o.cameraFps) } },
                    )
                    SwitchRow(
                        "High-speed camera",
                        "Allow the high-speed mode for camera frame rates above 60 fps.",
                        options.cameraHighSpeed,
                        icon = Icons.Outlined.Timer,
                    ) {
                        update { o -> o.copy(cameraHighSpeed = it) }
                    }
                    SwitchRow(
                        "Camera torch",
                        "Turn on the camera flashlight while the camera stream runs.",
                        options.cameraTorch,
                        icon = Icons.Outlined.FlashOn,
                    ) {
                        update { o -> o.copy(cameraTorch = it) }
                    }
                    TextFieldRow(
                        icon = Icons.Outlined.ZoomIn,
                        label = "Camera zoom",
                        description = "Digital camera zoom as a ratio, for example 2.0.",
                        value = options.cameraZoom.orEmpty(),
                        placeholder = "ratio, e.g. 2.0",
                        onChange = { update { o -> o.copy(cameraZoom = it.ifBlank { null }) } },
                    )
                }
            }

            item { SectionHeader("Virtual display", "Create an extra virtual display on the target device.") }
            item {
                SettingCard {
                    PickerField(
                        icon = Icons.Outlined.Dashboard,
                        label = "New virtual display",
                        description = "Create a new virtual display; empty, WxH, WxH/dpi or /dpi.",
                        options = listOf(
                            "Automatic" to "",
                            "1920x1080" to "1920x1080",
                            "1600x900" to "1600x900",
                            "1280x720" to "1280x720",
                        ),
                        value = options.newDisplay.orEmpty(),
                        onChange = { update { o -> o.copy(newDisplay = it.ifBlank { null }) } },
                    )
                    SwitchRow(
                        "Flexible display",
                        "Let the display follow orientation and window size changes.",
                        options.flexDisplay,
                        icon = Icons.Outlined.OpenInFull,
                    ) {
                        update { o -> o.copy(flexDisplay = it) }
                    }
                    SwitchRow(
                        "Clear the source display",
                        "Empty the content of the original display while the new virtual display is used.",
                        options.vdDestroyContent,
                        icon = Icons.Outlined.AutoDelete,
                    ) {
                        update { o -> o.copy(vdDestroyContent = it) }
                    }
                    SwitchRow(
                        "Show system bars",
                        "Show the status bar and navigation bar on the new virtual display.",
                        options.vdSystemDecorations,
                        icon = Icons.Outlined.Window,
                    ) {
                        update { o -> o.copy(vdSystemDecorations = it) }
                    }
                    PickerField(
                        icon = Icons.Outlined.Keyboard,
                        label = "Keyboard policy",
                        description = "Where the keyboard appears on the new display: 0, 1 or 2.",
                        options = listOf(
                            "0" to "0",
                            "1" to "1",
                            "2" to "2",
                        ),
                        value = options.displayImePolicy.orEmpty(),
                        numeric = true,
                        onChange = {
                            update { o -> o.copy(displayImePolicy = it.ifBlank { null }) }
                        },
                    )
                }
            }

            item { SectionHeader("Client (input & behaviour)", "Input handling done by this app instead of the server.") }
            item {
                SettingCard {
                    SwitchRow(
                        "Forward mouse hover",
                        "Send the cursor position while no mouse button is pressed.",
                        options.mouseHover,
                        icon = Icons.Outlined.Mouse,
                    ) {
                        update { o -> o.copy(mouseHover = it) }
                    }
                    SwitchRow(
                        "Send characters as text",
                        "The whole keyboard is sent as text instead of key events.",
                        options.preferText,
                        icon = Icons.Outlined.ShortText,
                    ) {
                        update { o ->
                            o.copy(preferText = it, rawKeyEvents = if (it) false else o.rawKeyEvents)
                        }
                    }
                    SwitchRow(
                        "Raw key events",
                        "The whole keyboard is sent as raw key events; text input is turned off.",
                        options.rawKeyEvents,
                        icon = Icons.Outlined.Code,
                    ) {
                        update { o ->
                            o.copy(rawKeyEvents = it, preferText = if (it) false else o.preferText)
                        }
                    }
                    SwitchRow(
                        "Repeat keys while held",
                        "Forward repeated key events so key repeat keeps working.",
                        options.keyRepeat,
                        icon = Icons.Outlined.SyncAlt,
                    ) {
                        update { o -> o.copy(keyRepeat = it) }
                    }
                    SwitchRow(
                        "Turn screen off at start",
                        "Switch the target screen off as soon as the session starts.",
                        options.turnScreenOff,
                        icon = Icons.Outlined.PowerSettingsNew,
                    ) {
                        update { o -> o.copy(turnScreenOff = it) }
                    }
                    TextFieldRow(
                        icon = Icons.Outlined.PlayCircle,
                        label = "Launch an app at start",
                        description = "Open an app when the session starts; a + prefix forces it, ? only if already running.",
                        value = options.startApp.orEmpty(),
                        placeholder = "e.g. +com.android.settings",
                        onChange = { update { o -> o.copy(startApp = it.ifBlank { null }) } },
                    )
                }
            }

            item { SectionHeader("Server & log", "scrcpy server logging, state cleanup and device information.") }
            item {
                SettingCard {
                    EnumChoice(
                        icon = Icons.Outlined.Terminal,
                        label = "Log level",
                        description = "Server log verbosity, from verbose down to error.",
                        value = options.logLevel,
                        choices = LogLevel.entries,
                        toString = { it.name.lowercase() },
                        onSelect = { update { o -> o.copy(logLevel = it) } },
                    )
                    SwitchRow(
                        "Clean up temporary files",
                        "Delete the temporary files the scrcpy server created once it finishes.",
                        options.cleanup,
                        icon = Icons.Outlined.AutoDelete,
                    ) {
                        update { o -> o.copy(cleanup = it) }
                    }
                    SwitchRow(
                        "Write activity log",
                        "Save every action to scrcpy.log so it can be inspected later. Crash reports are always written.",
                        AppLog.enabled,
                        icon = Icons.Outlined.Description,
                    ) { AppLog.setEnabled(context, it) }
                }

                DeviceInfoCard(vm)
            }

            item {
                Text(
                    t("Settings apply to the next scrcpy session. A random scid is generated every time for socket security."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        }
    }
}
/** Layar sementara ketika berpindah dari beranda ke Pengaturan. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsLoadingScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("scrcpy Settings")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Back"))
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ContainedLoadingIndicator(modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(18.dp))
                Text(
                    t("Loading settings…"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- widgets

@Composable
private fun SectionHeader(text: String, description: String) {
    Column(Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp)) {
        Text(
            t(text),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            t(description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Jarak antar baris dalam satu grup menu. Sengaja sangat kecil (~2dp) supaya
 * baris terbaca sebagai satu blok *Segmented/Connected List* ala M3 Expressive.
 */
private val SegmentedGap = 2.dp

/** Radius luar (item pertama/terakhir) dan radius dalam (sisi yang berdempetan). */
private val SegmentedLargeRadius = 16.dp
private val SegmentedSmallRadius = 4.dp

/** Radius kontainer dropdown dan pill item terpilih (lihat referensi MIUI). */
private val MenuRadius = 16.dp
private val MenuItemRadius = 14.dp

/** Spes animasi bentuk (spatial) dan warna (effects) ala Material 3 Expressive. */
private val GroupSpatialSpec = spring<Dp>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
)
private val GroupEffectsSpec = tween<Color>(durationMillis = 120)

/**
 * Satu baris pada *Segmented List*.
 *
 * - Item pertama : pojok atas besar, pojok bawah kecil
 * - Item tengah  : semua pojok kecil
 * - Item terakhir: pojok atas kecil, pojok bawah besar
 * - Saat ditekan : seluruh pojok morph menjadi bulat penuh lalu kembali
 *
 * [morphOnPress] dimatikan untuk baris yang sudah punya interaksi sendiri
 * (pill segmented control dan slider) agar tidak ada dua morph sekaligus.
 */
@Composable
private fun GroupRow(
    modifier: Modifier = Modifier,
    morphOnPress: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressDetector = if (morphOnPress) {
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                var down: Offset? = null
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val active = event.changes.firstOrNull { it.pressed }
                    if (active == null) {
                        pressed = false
                        down = null
                    } else {
                        val start = down ?: active.position.also { down = it }
                        val slop = viewConfiguration.touchSlop
                        val moved = abs(active.position.x - start.x) > slop ||
                            abs(active.position.y - start.y) > slop
                        // Jangan tampil "tertekan" ketika gesture berubah jadi scroll.
                        pressed = !moved
                    }
                }
            }
        }
    } else {
        Modifier
    }

    // Pojok luar grup sudah dipotong oleh [SettingCard] (clip 16dp), sehingga
    // tiap baris cukup memakai radius kecil; saat ditekan seluruh sisi morph
    // menjadi bulat penuh (shape morphing M3 Expressive).
    val topTarget = SegmentedSmallRadius
    val bottomTarget = SegmentedSmallRadius
    val morphTarget = if (pressed) SegmentedLargeRadius else null

    val topStart by animateDpAsState(
        morphTarget ?: topTarget, GroupSpatialSpec, label = "segTs",
    )
    val topEnd by animateDpAsState(
        morphTarget ?: topTarget, GroupSpatialSpec, label = "segTe",
    )
    val bottomStart by animateDpAsState(
        morphTarget ?: bottomTarget, GroupSpatialSpec, label = "segBs",
    )
    val bottomEnd by animateDpAsState(
        morphTarget ?: bottomTarget, GroupSpatialSpec, label = "segBe",
    )
    val container by animateColorAsState(
        targetValue = if (pressed) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = GroupEffectsSpec,
        label = "segContainer",
    )

    val clickModifier = if (onClick != null) {
        // Tanpa indikator: umpan balik datang dari warna/shape baris di atas,
        // supaya tidak ada kotak highlight membentuk blok saat disentuh.
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    Surface(
        shape = RoundedCornerShape(
            topStart = topStart,
            topEnd = topEnd,
            bottomStart = bottomStart,
            bottomEnd = bottomEnd,
        ),
        color = container,
        modifier = modifier
            .fillMaxWidth()
            .then(pressDetector)
            .then(clickModifier),
    ) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) { content() }
    }
}

/**
 * Tombol *pill* terhubung (connected button group) milik Material 3:
 * sudut luar penuh membulat, sudut dalam 8dp, jarak antar pill kecil, dan
 * seluruh sudut morph membulat ketika pill ditekan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PillGroup(
    count: Int,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    content: @Composable (index: Int, selected: Boolean) -> Unit,
) {
    if (count <= 0) return
    val outer = 20.dp
    val inner = 8.dp
    val morph = 20.dp
    val colors = MaterialTheme.colorScheme
    val spatial = GroupSpatialSpec
    val effects = GroupEffectsSpec

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(count) { i ->
            val selected = i == selectedIndex
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()

            val startTarget = if (i == 0) outer else inner
            val endTarget = if (i == count - 1) outer else inner

            val topStart by animateDpAsState(
                if (pressed) morph else startTarget, spatial, label = "pillTs",
            )
            val topEnd by animateDpAsState(
                if (pressed) morph else endTarget, spatial, label = "pillTe",
            )
            val bottomStart by animateDpAsState(
                if (pressed) morph else startTarget, spatial, label = "pillBs",
            )
            val bottomEnd by animateDpAsState(
                if (pressed) morph else endTarget, spatial, label = "pillBe",
            )
            val container by animateColorAsState(
                when {
                    pressed -> colors.secondaryContainer
                    selected -> colors.primaryContainer
                    else -> colors.surfaceContainerHighest
                },
                effects,
                label = "pillContainer",
            )
            val contentColor = when {
                pressed -> colors.onSecondaryContainer
                selected -> colors.onPrimaryContainer
                else -> colors.onSurfaceVariant
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = topStart,
                    topEnd = topEnd,
                    bottomStart = bottomStart,
                    bottomEnd = bottomEnd,
                ),
                color = container,
                contentColor = contentColor,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onSelect(i) },
                    ),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    content(i, selected)
                }
            }
        }
    }
}

/** Wadah satu seksi: *segmented list* tanpa kartu pembungkus. */
@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    // Clip di sini (bukan per-baris) supaya radius luar grup selalu 16dp dan
    // tidak bergantung urutan registrasi baris saat komposisi pertama.
    Column(
        verticalArrangement = Arrangement.spacedBy(SegmentedGap),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SegmentedLargeRadius)),
    ) { content() }
}

/** Ikon material di sisi kiri tiap baris pengaturan. */
@Composable
private fun LeadingIcon(icon: ImageVector?, modifier: Modifier = Modifier) {
    if (icon != null) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(24.dp),
        )
    }
}

@Composable
private fun SwitchRow(
    label: String,
    description: String,
    value: Boolean,
    icon: ImageVector? = null,
    onChange: (Boolean) -> Unit,
) {
    GroupRow {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LeadingIcon(icon)
            Column(Modifier.weight(1f)) {
                Text(t(label), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    t(description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = value,
                onCheckedChange = onChange,
                thumbContent = {
                    Icon(
                        imageVector = if (value) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                },
            )
        }
    }
}

/**
 * Pilihan bergaya *menu*: satu baris ikon + label + nilai yang membuka dropdown
 * gelap ala referensi — item terpilih tampil sebagai **pill terang** berisi
 * centang, seluruh teks diratakan tengah.
 *
 * Memilih **Custom…** membuat kolom isian baru muncul tepat di bawah baris
 * ([AnimatedVisibility]) untuk memasukkan nilai bebas.
 */
@Composable
private fun PickerField(
    label: String,
    description: String,
    options: List<Pair<String, String>>,
    value: String,
    icon: ImageVector? = null,
    customLabel: String = "Custom…",
    numeric: Boolean = false,
    onChange: (String) -> Unit,
) {
    val matched = options.firstOrNull { it.second == value }
    // Dihitung sekali: mode custom hanya berubah lewat menu, tidak saat mengetik.
    var custom by remember { mutableStateOf(matched == null && value.isNotEmpty()) }
    var expanded by remember { mutableStateOf(false) }
    // Saat memilih preset tampilkan label manusianya; mode custom tetap nilai mentah.
    val shown = if (custom) value else (matched?.first ?: value)
    val colors = MaterialTheme.colorScheme

    Box(Modifier.fillMaxWidth()) {
        GroupRow(onClick = { expanded = true }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LeadingIcon(icon)
                Column(Modifier.weight(1f)) {
                    Text(t(label), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        t(description),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Text(
                    shown,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 110.dp),
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(MenuRadius),
            containerColor = colors.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                options.forEach { (optionLabel, optionValue) ->
                    // Saat mode custom aktif, preset tidak boleh ikut tercentang
                    // walau nilai mentahnya masih sama dengan preset tersebut.
                    DropdownOption(t(optionLabel), !custom && optionValue == value) {
                        custom = false
                        expanded = false
                        onChange(optionValue)
                    }
                }
                DropdownOption(t(customLabel), custom) {
                    custom = true
                    expanded = false
                }
            }
        }
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = custom,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        GroupRow(morphOnPress = false) {
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                label = { Text(t("Custom value")) },
                supportingText = { Text(t(description)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Satu baris isi dropdown: teks rata tengah; yang terpilih diberi pill + centang. */
@Composable
private fun DropdownOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = if (selected) colors.secondaryContainer else Color.Transparent,
        animationSpec = tween(120),
        label = "ddOption",
    )
    val shape = RoundedCornerShape(MenuItemRadius)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) colors.onSecondaryContainer else colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TextFieldRow(
    label: String,
    description: String,
    value: String,
    placeholder: String = "",
    numeric: Boolean = false,
    onChange: (String) -> Unit,
    icon: ImageVector? = null,
) {
    GroupRow {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            LeadingIcon(icon, Modifier.padding(top = 14.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                label = { Text(t(label)) },
                supportingText = { Text(t(description)) },
                placeholder = if (placeholder.isEmpty()) null else ({ Text(t(placeholder)) }),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SliderField(
    label: String,
    description: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    display: (Float) -> String,
    onChange: (Float) -> Unit,
    icon: ImageVector? = null,
) {
    GroupRow(morphOnPress = false) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            LeadingIcon(icon, Modifier.padding(top = 1.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                Text(
                    t(label),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    t(display(value)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Text(
                t(description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = value,
                onValueChange = onChange,
                valueRange = range,
                steps = steps,
            )
            }
        }
    }
}

@Composable
private fun <T> EnumChoice(
    label: String,
    description: String,
    value: T,
    choices: List<T>,
    toString: (T) -> String,
    onSelect: (T) -> Unit,
    icon: ImageVector? = null,
) {
    GroupRow(morphOnPress = false) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            LeadingIcon(icon, Modifier.padding(top = 1.dp))
            Column(Modifier.weight(1f)) {
            Text(t(label), style = MaterialTheme.typography.bodyMedium)
            Text(
                t(description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            PillGroup(
                count = choices.size,
                selectedIndex = choices.indexOf(value),
                onSelect = { onSelect(choices[it]) },
            ) { i, _ ->
                Text(
                    text = toString(choices[i]),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
        }
    }
}

/** Warna benih pilihan pengguna; aktif hanya saat dynamic color dimatikan. */
private val SeedColors = listOf(
    0xFF4E7BFF.toInt(), 0xFFB7261C.toInt(), 0xFF006A60.toInt(), 0xFF7B5200.toInt(),
    0xFF8B4FBB.toInt(), 0xFF9C4146.toInt(), 0xFF386A20.toInt(), 0xFF006E7A.toInt(),
    0xFF9B4093.toInt(), 0xFF575E71.toInt(), 0xFF8A5100.toInt(), 0xFF4A6170.toInt(),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeedColorRow(enabled: Boolean) {
    val context = LocalContext.current
    val selected = ThemeState.seed.toArgb()
    GroupRow(morphOnPress = false) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            LeadingIcon(Icons.Outlined.ColorLens, Modifier.padding(top = 1.dp))
            Column(Modifier.weight(1f)) {
            Text(t("Accent color"), style = MaterialTheme.typography.bodyMedium)
            Text(
                t("Seed color used to build the palette when dynamic color is off."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            // FlowRow supaya 12 swatch membungkus ke baris berikutnya di layar sempit.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SeedColors.forEach { argb ->
                    val color = Color(argb)
                    val isOn = argb == selected
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = 2.dp,
                                color = if (isOn) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = CircleShape,
                            )
                            .then(
                                if (enabled) {
                                    Modifier.clickable { ThemeState.setSeed(context, color) }
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isOn) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = inkOn(color),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
            if (!enabled) {
                Spacer(Modifier.height(6.dp))
                Text(
                    t("Turn off dynamic color to pick an accent color."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        }
    }
}

private fun inkOn(color: Color): Color =
    if (color.luminance() > 0.45f) Color.Black else Color.White

// ------------------------------------------------------- informasi perangkat

/** Ikon khas untuk tiap mode `--list-*` scrcpy. */
private fun kindIcon(kind: ListKind): ImageVector = when (kind) {
    ListKind.ENCODERS -> Icons.Outlined.Memory
    ListKind.DISPLAYS -> Icons.Outlined.Devices
    ListKind.CAMERAS -> Icons.Outlined.CameraAlt
    ListKind.CAMERA_SIZES -> Icons.Outlined.CropSquare
    ListKind.APPS -> Icons.Outlined.Apps
}

/** Deskripsi pendek tiap mode daftar, diselaraskan dengan baris pengaturan lain. */
private val ListKindInfo: List<Pair<ListKind, String>> = listOf(
    ListKind.ENCODERS to "Read the video and audio encoders available on the target device.",
    ListKind.DISPLAYS to "Read the displays available on the target device.",
    ListKind.CAMERAS to "Read the cameras available on the target device.",
    ListKind.CAMERA_SIZES to "Read the resolutions supported by every camera.",
    ListKind.APPS to "Read the applications installed on the target device.",
)

/**
 * Seksi *Device info*: menjalankan server scrcpy sekali lalu keluar untuk membaca
 * encoder, display, kamera, resolusi kamera, dan aplikasi terpasang dari
 * perangkat target. Ditampilkan sebagai daftar baris seperti pengaturan lainnya.
 */
@Composable
private fun DeviceInfoCard(vm: AppViewModel) {
    val loading by vm.listLoading.collectAsState()
    val result by vm.listResult.collectAsState()
    val clipboard = LocalClipboardManager.current

    SectionHeader(
        "Device info",
        "Run the scrcpy server once and exit to read encoders, displays, cameras and installed apps from the target device.",
    )
    SettingCard {
        ListKindInfo.forEach { (kind, description) ->
            val runKind: (() -> Unit)? = if (loading == null) {
                ({ vm.runList(kind) })
            } else {
                null
            }
            GroupRow(onClick = runKind) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LeadingIcon(kindIcon(kind))
                    Column(Modifier.weight(1f)) {
                        Text(t(kind.label), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            t(description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (loading == kind) {
                        LoadingIndicator(Modifier.size(20.dp))
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
    }

    result?.let { r ->
        AlertDialog(
            onDismissRequest = { vm.clearListResult() },
            title = { Text(r.label) },
            text = {
                SelectionContainer {
                    Text(
                        r.text,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.clearListResult() }) { Text(t("Close")) }
            },
            dismissButton = {
                TextButton(onClick = {
                    clipboard.setText(buildAnnotatedString { append(r.text) })
                    vm.clearListResult()
                }) { Text(t("Copy")) }
            },
        )
    }
}
