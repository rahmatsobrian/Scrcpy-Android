@file:kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.rahmatsobrian.scrcpy.ui.screens

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.TextureView
import android.view.KeyEvent as AndroidKeyEvent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.isForwardPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rahmatsobrian.scrcpy.scrcpy.ControlProtocol
import com.rahmatsobrian.scrcpy.scrcpy.ScrcpyController
import com.rahmatsobrian.scrcpy.scrcpy.VideoSource
import com.rahmatsobrian.scrcpy.ui.AppViewModel
import com.rahmatsobrian.scrcpy.ui.i18n.t
import com.rahmatsobrian.scrcpy.util.AppLog
import java.text.SimpleDateFormat
import kotlin.math.roundToInt
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.delay

private const val TAG = "MirrorScreen"

private const val POINTER_FINGER = -2L
private const val POINTER_MOUSE = -1L
private const val ACTION_DOWN = 0
private const val ACTION_UP = 1
private const val ACTION_MOVE = 2
private const val ACTION_CANCEL = 3
private const val ACTION_HOVER_MOVE = 7

// bitmask tombol gaya Android (MotionEvent.BUTTON_*)
private const val BUTTON_PRIMARY = 1
private const val BUTTON_SECONDARY = 2
private const val BUTTON_TERTIARY = 4
private const val BUTTON_BACK = 8
private const val BUTTON_FORWARD = 16

/** Warna header — status bar sengaja dibuat sama persis. */
private val HEADER_BG = Color(0xFF0E0E0E)
private val HEADER_BG_ARGB = 0xFF0E0E0E.toInt()

@Composable
fun MirrorScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by vm.sessionState.collectAsStateWithLifecycle()
    val videoSize by vm.videoSize.collectAsStateWithLifecycle()
    val options by vm.options.collectAsStateWithLifecycle()

    var toolbarVisible by remember { mutableStateOf(true) }
    var fullscreen by remember { mutableStateOf(false) }

    // Posisi tombol keluar fullscreen, digeser dengan drag namun selalu
    // dibatasi di dalam layar dan tidak boleh menabrak bar bawaan Android.
    var fsDragX by remember { mutableStateOf(0f) }
    var fsDragY by remember { mutableStateOf(0f) }
    var fsAreaSize by remember { mutableStateOf(IntSize.Zero) }
    var fsBtnSize by remember { mutableStateOf(IntSize.Zero) }
    // Tinggi status bar & navigation bar bawaan Android. Direkam selagi bar
    // masih terlihat (sebelum fullscreen), karena saat bar disembunyikan
    // WindowInsets melapor 0 dan tidak bisa lagi dipakai sebagai batas.
    var fsBarTopPx by remember { mutableStateOf(0f) }
    var fsBarBottomPx by remember { mutableStateOf(0f) }
    var showMenu by remember { mutableStateOf(false) }
    var showTextInput by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var torchOn by remember { mutableStateOf(false) }

    val view = LocalView.current
    var surfaceReady by remember { mutableStateOf(false) }
    var textureView by remember { mutableStateOf<TextureView?>(null) }

    val kbFocus = remember { FocusRequester() }

    val (deviceW, deviceH) = videoSize
    val ready = state is ScrcpyController.State.Ready
    val running = state is ScrcpyController.State.Running
    val active = ready || running
    val showPlaceholder = !running || deviceW == 0

    /** Jejak aktivitas ke `Android/media/<package>/scrcpy.log`. */
    fun mark(action: String) = AppLog.log("mirror: $action")

    LaunchedEffect(state) { AppLog.log("state: $state") }
    LaunchedEffect(deviceW, deviceH) {
        if (deviceW > 0) AppLog.log("layar target: ${deviceW}x$deviceH")
    }

    // Denyut: menandai sampai kapan proses masih hidup sebelum force-close,
    // supaya kelompok waktu kematiannya bisa ditekan dari log.
    LaunchedEffect(running) {
        while (running) {
            delay(2_000)
            AppLog.log("heartbeat: mirror aktif")
        }
    }

    // ------------------------------------------------------ system bar
    // Status bar & navigation bar sengaja **ditampilkan** agar:
    //  - header berada tepat di bawah status bar host (bukan menembusnya),
    //  - footer berada di bawah panel navigasi,
    //  - panel video berada di antara keduanya (tidak tertimpa header/footer).
    // Warna status bar disamakan dengan header dan ikonnya dipaksa putih.
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val insets = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        @Suppress("DEPRECATION")
        val prevColor = window?.statusBarColor
        val prevLight = insets?.isAppearanceLightStatusBars

        if (window != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            @Suppress("DEPRECATION")
            window.statusBarColor = HEADER_BG_ARGB
        }
        // API 35+ selalu edge-to-edge (scrim system dibuang), jadi cukup
        // pastikan ikon status bar terang.
        insets?.isAppearanceLightStatusBars = false

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (window != null && prevColor != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = prevColor
            }
            if (prevLight != null) insets?.isAppearanceLightStatusBars = prevLight
        }
    }
    val density = LocalDensity.current
    val statusTopPx = with(density) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
    }
    val navBottomPx = with(density) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().toPx()
    }
    LaunchedEffect(statusTopPx, navBottomPx, fullscreen) {
        if (!fullscreen) {
            if (statusTopPx > fsBarTopPx) fsBarTopPx = statusTopPx
            if (navBottomPx > fsBarBottomPx) fsBarBottomPx = navBottomPx
        }
    }

    // --------------------------------------------------------- mode fullscreen
    // Menyembunyikan status bar & navigation bar supaya mirror memakai seluruh
    // layar; geser dari tepi menampilkan bar sebentar (behaviour transient).
    DisposableEffect(fullscreen) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        if (fullscreen && controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else if (controller != null) {
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }

    LaunchedEffect(Unit) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setFormat(PixelFormat.RGBA_8888)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Edge-to-edge; inset status/navigation bar tetap dikirim ke Compose.
            window.setDecorFitsSystemWindows(false)
        }
    }

    // ---------------------------------------------------------- mulai sesi
    LaunchedEffect(surfaceReady, ready) {
        if (surfaceReady && ready) vm.startSession()
    }

    DisposableEffect(Unit) {
        onDispose {
            vm.controller.setSurface(null)
            // Keluar dari layar mirror = hentikan sesi (koneksi ADB tetap hidup).
            vm.stopSession()
        }
    }

    BackHandler { onBack() }
    // Dipasang setelahnya supaya saat fullscreen tombol Back keluar dari
    // fullscreen dulu, baru keluar dari layar mirror.
    BackHandler(enabled = fullscreen) { fullscreen = false }

    // Keyboard fisik hanya aktif bila mirror sedang fokus.
    LaunchedEffect(running) {
        if (running) runCatching { kbFocus.requestFocus() }
    }

    // Senter kamera hanya berlaku selama sesi berjalan.
    LaunchedEffect(running) {
        if (!running) torchOn = false
    }

    LaunchedEffect(notice) {
        notice?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            notice = null
        }
    }

    // Header → layar target → footer, disusun vertikal sehingga:
    //  * header selalu di bawah status bar (tidak menembusnya),
    //  * footer selalu di bawah panel video (tidak menembus layar target).
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Sama dengan warna header sehingga area status bar menyatu.
            .background(HEADER_BG)
            .focusRequester(kbFocus)
            .focusable()
            .onKeyEvent { vm.hardwareKeyboard(it) },
    ) {
        // ------------------------------------------------------------ header
        AnimatedVisibility(
            visible = toolbarVisible && !fullscreen,
            enter = fadeIn(tween(220)) + expandVertically(),
            exit = fadeOut(tween(180)) + shrinkVertically(),
        ) {
            TopToolbar(
                deviceName = (state as? ScrcpyController.State.Running)?.deviceName
                    ?: (state as? ScrcpyController.State.Ready)?.deviceName.orEmpty(),
                onBack = onBack,
                onTextInput = { showTextInput = true },
                onMenu = {
                    mark("menu dibuka")
                    showMenu = true
                },
                menu = {
                    if (showMenu) {
                        OverflowMenu(
                            onDismiss = {
                                mark("menu ditutup")
                                showMenu = false
                            },
                            onTextInput = {
                                mark("menu: kirim teks")
                                showMenu = false
                                showTextInput = true
                            },
                            onCopyClipboard = {
                                mark("menu: clipboard")
                                vm.controller.getClipboard()
                                showMenu = false
                                notice = t("Clipboard requested")
                            },
                            onScreenshot = {
                                mark("menu: screenshot")
                                showMenu = false
                                notice = if (captureScreenshot(context, textureView) != null) {
                                    t("Screenshot saved to Pictures/Scrcpy")
                                } else {
                                    t("Screenshot failed")
                                }
                            },
                            onResetVideo = {
                                mark("menu: reset video")
                                vm.controller.resetVideo()
                                showMenu = false
                            },
                            showCameraControls = running &&
                                options.videoSource == VideoSource.CAMERA,
                            torchOn = torchOn,
                            onToggleTorch = {
                                val next = !torchOn
                                torchOn = next
                                showMenu = false
                                mark("menu: senter=$next")
                                vm.controller.setCameraTorch(next)
                                notice = if (next) t("Camera torch on") else t("Camera torch off")
                            },
                            onZoomIn = {
                                mark("menu: zoom in")
                                vm.controller.zoomCameraIn()
                                showMenu = false
                                notice = t("Camera zoomed in")
                            },
                            onZoomOut = {
                                mark("menu: zoom out")
                                vm.controller.zoomCameraOut()
                                showMenu = false
                                notice = t("Camera zoomed out")
                            },
                            onSwitchCamera = {
                                mark("menu: ganti kamera")
                                showMenu = false
                                vm.switchCameraFacing()
                                notice = t("Switching camera…")
                            },
                            onDisconnect = {
                                mark("menu: putus sesi")
                                showMenu = false
                                vm.stopSession()
                                vm.disconnect()
                                onBack()
                            },
                            fullscreen = fullscreen,
                            onToggleFullscreen = {
                                fullscreen = !fullscreen
                                showMenu = false
                                mark("menu: fullscreen=" + fullscreen)
                            },
                        )
                    }
                },
            )
        }

        // ----------------------------------------------------- layar target
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
                .onSizeChanged { fsAreaSize = it },
            contentAlignment = Alignment.Center,
        ) {
            val videoModifier = if (deviceW > 0 && deviceH > 0) {
                Modifier.aspectRatio(deviceW.toFloat() / deviceH.toFloat())
            } else {
                Modifier.fillMaxSize()
            }

            VideoSurface(
                modifier = videoModifier.then(
                    if (active && deviceW > 0) {
                        Modifier.scrcpyGestures(vm, deviceW, deviceH)
                    } else {
                        Modifier
                    },
                ),
                onSurfaceAvailable = { surface ->
                    surfaceReady = true
                    vm.controller.setSurface(surface)
                },
                onSurfaceDestroyed = {
                    surfaceReady = false
                    vm.controller.setSurface(null)
                },
                onViewCreated = { textureView = it },
            )

            // Tombol keluar fullscreen: satu-satunya kontrol yang tersisa
            // ketika header dan footer disembunyikan. Di dalam Box (BoxScope)
            // kita sengaja memakai AnimatedVisibility tingkat atas, bukan milik
            // ColumnScope. Overlaynya selebar area video supaya tombol yang
            // digeser tetap bisa di-tap / di-drag; Box tanpa pointerInput tidak
            // ikut dalam hit test, jadi sentuhan di area video lain tetap
            // diteruskan ke surface.
            androidx.compose.animation.AnimatedVisibility(
                visible = fullscreen,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(150)),
            ) {
                Box(Modifier.fillMaxSize()) {
                    FilledTonalIconButton(
                        onClick = {
                            mark("keluar fullscreen")
                            fullscreen = false
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .onSizeChanged { fsBtnSize = it }
                            // `offset` lambda: posisi digeser lebih dulu, baru
                            // di-clamp supaya tidak keluar layar dan tidak
                            // menabrak status bar / navigation bar bawaan Android.
                            .offset {
                                val padPx = 8.dp.toPx()
                                val minX = (fsBtnSize.width + 2 * padPx - fsAreaSize.width)
                                    .coerceAtMost(0f)
                                val maxX = 0f
                                val minY = fsBarTopPx
                                val maxY = (fsAreaSize.height - fsBtnSize.height -
                                    fsBarBottomPx - 2 * padPx)
                                IntOffset(
                                    fsDragX.coerceIn(minOf(minX, maxX), maxOf(minX, maxX))
                                        .roundToInt(),
                                    fsDragY.coerceIn(minOf(minY, maxY), maxOf(minY, maxY))
                                        .roundToInt(),
                                )
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val padPx = 8.dp.toPx()
                                    val minX = (fsBtnSize.width + 2 * padPx - fsAreaSize.width)
                                        .coerceAtMost(0f)
                                    val maxX = 0f
                                    val minY = fsBarTopPx
                                    val maxY = (fsAreaSize.height - fsBtnSize.height -
                                        fsBarBottomPx - 2 * padPx)
                                    fsDragX = (fsDragX + dragAmount.x)
                                        .coerceIn(minOf(minX, maxX), maxOf(minX, maxX))
                                    fsDragY = (fsDragY + dragAmount.y)
                                        .coerceIn(minOf(minY, maxY), maxOf(minY, maxY))
                                }
                            },
                    ) {
                        Icon(
                            Icons.Filled.FullscreenExit,
                            contentDescription = t("Exit fullscreen"),
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showPlaceholder,
                enter = fadeIn(tween(240)),
                exit = fadeOut(tween(240)),
            ) {
                PlaceholderOverlay(state = state, onBack = onBack, onRetry = vm::retrySession)
            }
        }

        // ------------------------------------------------------------ footer
        AnimatedVisibility(
            visible = toolbarVisible && active && !fullscreen,
            enter = fadeIn(tween(220)) + expandVertically(),
            exit = fadeOut(tween(180)) + shrinkVertically(),
        ) {
            BottomNav(
                onBack = { mark("back"); vm.controller.back() },
                onHome = { mark("home"); vm.controller.home() },
                onRecents = { mark("recents"); vm.controller.recentApps() },
                onRotate = { mark("rotate"); vm.controller.rotateDevice() },
                onNotifications = { mark("panel notifikasi"); vm.controller.expandNotifications() },
                onResetVideo = {
                    mark("reset video")
                    vm.controller.resetVideo()
                    notice = t("Video reset")
                },
                onScreenOff = {
                    mark("layar mati")
                    vm.controller.setDisplayPower(false)
                    notice = t("Screen turned off")
                },
            )
        }
    }

    if (showTextInput) {
        TextInputDialog(
            onDismiss = { showTextInput = false },
            onSubmit = {
                vm.controller.sendText(it)
                showTextInput = false
                notice = t("Text sent")
            },
        )
    }
}

// ---------------------------------------------------------------- gestures

/**
 * Input untuk mirror.
 *
 * **Sentuh** (jari / stylus):
 * - satu jari diam → tap (klik)
 * - satu jari gerak → drag (gerakkan pointer)
 * - dua jari vertikal → scroll
 *
 * **Mouse** — mengikuti `mouse_sdk.c` + binding `bhsn` (scrcpy 4.1):
 * - gerak tanpa tombol → `ACTION_HOVER_MOVE` dengan `POINTER_ID_MOUSE` (-1)
 * - tombol kiri ditekan → `ACTION_DOWN`/`ACTION_MOVE`/`ACTION_UP` dengan
 *   `action_button` dan `buttons` = `BUTTON_PRIMARY`
 * - klik kanan → shortcut BACK, klik tengah → HOME, tombol 4 → APP_SWITCH,
 *   tombol 5 → panel notifikasi
 * - roda scroll → `INJECT_SCROLL_EVENT`
 */
private fun Modifier.scrcpyGestures(
    vm: AppViewModel,
    deviceW: Int,
    deviceH: Int,
): Modifier = this.then(
    Modifier.pointerInput(deviceW, deviceH) {
        if (deviceW <= 0 || deviceH <= 0) return@pointerInput
        val ctrl = vm.controller
        val slop = viewConfiguration.touchSlop
        val scrollThreshold = 10f * density

        // Server menolak koordinat yang bukan dalam ruang frame video, jadi
        // piksel view dipetakan ke ukuran video (`sc_screen_convert_window_to_frame_coords`).
        val viewW = size.width.coerceAtLeast(1).toFloat()
        val viewH = size.height.coerceAtLeast(1).toFloat()
        fun vx(px: Float) = (px * deviceW / viewW).coerceIn(0f, deviceW.toFloat())
        fun vy(py: Float) = (py * deviceH / viewH).coerceIn(0f, deviceH.toFloat())

        // ---- state sentuh
        var active = false
        var start = Offset.Zero
        var last = Offset.Zero
        var dragging = false
        var scrolling = false

        // ---- state mouse: bitmask tombol yang ditekan sebelumnya
        var prevButtons = 0

        fun mask(b: PointerButtons): Int =
            (if (b.isPrimaryPressed) BUTTON_PRIMARY else 0) or
                (if (b.isSecondaryPressed) BUTTON_SECONDARY else 0) or
                (if (b.isTertiaryPressed) BUTTON_TERTIARY else 0) or
                (if (b.isBackPressed) BUTTON_BACK else 0) or
                (if (b.isForwardPressed) BUTTON_FORWARD else 0)

        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)

                // ---------------------------------------------------------- mouse
                if (event.changes.any { it.type == PointerType.Mouse }) {
                    val cur = event.changes.first { it.type == PointerType.Mouse }
                    val now = mask(event.buttons)
                    val pressed = now and prevButtons.inv()
                    val released = prevButtons and now.inv()
                    prevButtons = now

                    if (pressed and BUTTON_PRIMARY != 0) {
                        ctrl.sendTouch(
                            ACTION_DOWN, POINTER_MOUSE,
                            vx(cur.position.x), vy(cur.position.y), deviceW, deviceH, 1f,
                            actionButton = BUTTON_PRIMARY, buttons = BUTTON_PRIMARY,
                        )
                    }
                    if (released and BUTTON_PRIMARY != 0) {
                        ctrl.sendTouch(
                            ACTION_UP, POINTER_MOUSE,
                            vx(cur.position.x), vy(cur.position.y), deviceW, deviceH, 0f,
                            actionButton = BUTTON_PRIMARY, buttons = 0,
                        )
                    }
                    if (pressed and BUTTON_SECONDARY != 0) ctrl.back()
                    if (pressed and BUTTON_TERTIARY != 0) ctrl.home()
                    if (pressed and BUTTON_BACK != 0) ctrl.recentApps()
                    if (pressed and BUTTON_FORWARD != 0) ctrl.expandNotifications()

                    when (event.type) {
                        PointerEventType.Move -> {
                            if (now and BUTTON_PRIMARY != 0) {
                                ctrl.sendTouch(
                                    ACTION_MOVE, POINTER_MOUSE,
                                    vx(cur.position.x), vy(cur.position.y),
                                    deviceW, deviceH, 1f, buttons = BUTTON_PRIMARY,
                                )
                            } else if (vm.options.value.mouseHover) {
                                ctrl.sendTouch(
                                    ACTION_HOVER_MOVE, POINTER_MOUSE,
                                    vx(cur.position.x), vy(cur.position.y),
                                    deviceW, deviceH, 1f,
                                )
                            }
                        }
                        PointerEventType.Scroll -> {
                            val delta = cur.scrollDelta
                            // `PointerEvent.scrollDelta.y` sudah dibalik Compose
                            // dibanding `AXIS_VSCROLL` / SDL `wheel.y`.
                            // Tombol sekunder terikat shortcut sehingga tidak
                            // masuk `mouse_buttons_state` scrcpy.
                            ctrl.sendScroll(
                                vx(cur.position.x), vy(cur.position.y),
                                deviceW, deviceH,
                                hScroll = delta.x,
                                vScroll = -delta.y,
                                buttons = now and BUTTON_PRIMARY,
                            )
                        }
                        else -> Unit
                    }
                }

                // --------------------------------------------------------- sentuh
                val nonMouse = event.changes.filter { it.type != PointerType.Mouse }
                if (nonMouse.isEmpty()) continue
                val pressed = nonMouse.filter { it.pressed }

                if (!active) {
                    val down = pressed.firstOrNull() ?: continue
                    active = true
                    start = down.position
                    last = start
                    dragging = false
                    scrolling = false

                    ctrl.sendTouch(
                        ACTION_DOWN, POINTER_FINGER,
                        vx(start.x), vy(start.y), deviceW, deviceH, 1f,
                    )
                    continue
                }

                if (pressed.isEmpty()) {
                    ctrl.sendTouch(
                        ACTION_UP, POINTER_FINGER,
                        vx(last.x), vy(last.y), deviceW, deviceH, 0f,
                    )
                    active = false
                    continue
                }

                if (pressed.size >= 2 && !scrolling) {
                    // Dua jari: akhiri sentuh lalu mulai scroll.
                    ctrl.sendTouch(
                        ACTION_UP, POINTER_FINGER,
                        vx(last.x), vy(last.y), deviceW, deviceH, 0f,
                    )
                    scrolling = true
                    last = pressed.first().position
                    continue
                }

                val pos = pressed.first().position
                if (scrolling) {
                    val dy = pos.y - last.y
                    if (abs(dy) >= scrollThreshold) {
                        ctrl.sendScroll(
                            vx(pos.x), vy(pos.y), deviceW, deviceH,
                            hScroll = 0f,
                            vScroll = if (dy < 0) 8f else -8f,
                        )
                        last = pos
                    }
                    continue
                }

                if (!dragging &&
                    (abs(pos.x - start.x) > slop || abs(pos.y - start.y) > slop)
                ) {
                    dragging = true
                }
                if (dragging) {
                    ctrl.sendTouch(
                        ACTION_MOVE, POINTER_FINGER,
                        vx(pos.x), vy(pos.y), deviceW, deviceH, 1f,
                    )
                    last = pos
                }
            }
        }
    },
)

// ---------------------------------------------------------------- keyboard

/**
 * Teruskan event keyboard fisik ke perangkat (KEYCODE_* Android dipakai apa adanya
 * oleh scrcpy). Tombol [KEYCODE_BACK][android.view.KeyEvent.KEYCODE_BACK] sengaja
 * tidak diteruskan agar back bawaan tetap menutup layar mirror.
 *
 * Mode injeksi mengikuti `keyboard_sdk.c`:
 * - `rawKeyEvents` (`--raw-key-events`) → selalu key event
 * - `preferText` (`--prefer-text`) → karakter printable dikirim sebagai `INJECT_TEXT`
 * - campuran (default) → huruf & spasi key event, karakter lain teks
 *
 * `keyRepeat` (`--no-key-repeat`) menahan key event berulang saat tombol ditahan.
 */
private fun AppViewModel.hardwareKeyboard(event: KeyEvent): Boolean {
    val android = event.nativeKeyEvent
    if (android.keyCode == AndroidKeyEvent.KEYCODE_BACK) return false
    val down = when (event.type) {
        KeyEventType.KeyDown -> true
        KeyEventType.KeyUp -> false
        else -> return false
    }

    val opts = options.value

    // Tombol volume: diteruskan ke HP target DAN event di-consume supaya host
    // tidak ikut menggeser stream-nya sendiri. Tanpa consume, host memakai dua
    // slider berbeda (Media selama mirror, Ringtone setelah putus) sehingga
    // angkanya terlihat "berubah sendiri" 80 → 50 → 80.
    val volKey = android.keyCode == AndroidKeyEvent.KEYCODE_VOLUME_UP ||
        android.keyCode == AndroidKeyEvent.KEYCODE_VOLUME_DOWN ||
        android.keyCode == AndroidKeyEvent.KEYCODE_VOLUME_MUTE
    if (volKey) {
        if (controller.state.value !is ScrcpyController.State.Running) return false
        if (down && android.repeatCount > 0 && !opts.keyRepeat) return true
        controller.sendKey(
            action = if (down) ControlProtocol.KEY_ACTION_DOWN else ControlProtocol.KEY_ACTION_UP,
            keycode = android.keyCode,
            repeat = android.repeatCount,
            metastate = android.metaState,
        )
        if (down) controller.logTargetVolume("volkey")
        return true
    }

    if (!opts.keyRepeat && down && android.repeatCount > 0) return false

    // Ctrl/Alt/Meta aktif → shortcut, selalu key event (scrcpy andalkan SDL
    // yang tidak menghasilkan event teks untuk shortcut).
    val shortcutHeld =
        android.metaState and (AndroidKeyEvent.META_CTRL_ON or
            AndroidKeyEvent.META_ALT_ON or AndroidKeyEvent.META_META_ON) != 0

    val unicode = android.getUnicodeChar(android.metaState)
    val printable = unicode != 0 && !Character.isISOControl(unicode)
    val letterOrSpace = printable && (Character.isLetter(unicode) || unicode == ' '.code)

    val asText = printable && !shortcutHeld && when {
        opts.rawKeyEvents -> false
        opts.preferText -> true
        else -> !letterOrSpace
    }

    if (asText) {
        // Event teks hanya dikirim saat ditekan (bukan saat diulang/dilepas).
        if (down && android.repeatCount == 0) {
            runCatching { controller.sendText(String(Character.toChars(unicode))) }
        }
        return false
    }

    controller.sendKey(
        action = if (down) ControlProtocol.KEY_ACTION_DOWN else ControlProtocol.KEY_ACTION_UP,
        keycode = android.keyCode,
        repeat = android.repeatCount,
        metastate = android.metaState,
    )
    // Jangan konsumsi agar Compose tetap bisa menangani arrow/enter miliknya sendiri.
    return false
}

// ---------------------------------------------------------------- video view

@Composable
private fun VideoSurface(
    modifier: Modifier,
    onSurfaceAvailable: (android.view.Surface) -> Unit,
    onSurfaceDestroyed: () -> Unit,
    onViewCreated: (TextureView) -> Unit = {},
) {
    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                onViewCreated(this)
                isOpaque = false
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(
                        st: android.graphics.SurfaceTexture, width: Int, height: Int,
                    ) = onSurfaceAvailable(android.view.Surface(st))

                    override fun onSurfaceTextureSizeChanged(
                        st: android.graphics.SurfaceTexture, width: Int, height: Int,
                    ) = Unit

                    override fun onSurfaceTextureDestroyed(
                        st: android.graphics.SurfaceTexture,
                    ): Boolean {
                        onSurfaceDestroyed()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(st: android.graphics.SurfaceTexture) = Unit
                }
            }
        },
        modifier = modifier,
    )
}

// ---------------------------------------------------------------- placeholder

@Composable
private fun PlaceholderOverlay(
    state: ScrcpyController.State,
    onBack: () -> Unit,
    onRetry: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is ScrcpyController.State.Ready -> PlaceholderMessage(
                title = state.deviceName,
                subtitle = t("Preparing scrcpy-server…"),
            ) { BackIconButton(onBack) }

            is ScrcpyController.State.Connecting -> PlaceholderLoading(state.label) {
                t("Opening scrcpy-server on the device…")
            }

            is ScrcpyController.State.Running -> PlaceholderMessage(
                title = t("Waiting for video frames…"),
                subtitle = t("Decoder is preparing the surface"),
            )

            is ScrcpyController.State.Failed -> PlaceholderMessage(
                title = t("Session failed"),
                subtitle = state.message,
                titleColor = MaterialTheme.colorScheme.error,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalIconButton(onClick = onRetry) {
                        Icon(Icons.Filled.Refresh, contentDescription = t("Try again"))
                    }
                    BackIconButton(onBack)
                }
            }

            else -> PlaceholderMessage(
                title = t("No active session"),
                subtitle = t("Go back to the home screen and pick a device"),
            ) { BackIconButton(onBack) }
        }
    }
}

@Composable
private fun PlaceholderLoading(label: String, subtitle: () -> String) {
    // Tanpa glow radial-gradient, tanpa bayangan: hanya indikator berwadah.
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ContainedLoadingIndicator(modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(22.dp))
        Text(
            label,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PlaceholderMessage(
    title: String,
    subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onBackground,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = titleColor, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 36.dp),
        )
        trailing?.let {
            Spacer(Modifier.height(18.dp))
            it()
        }
    }
}

@Composable
private fun BackIconButton(onBack: () -> Unit) {
    FilledTonalIconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Back"))
    }
}

// ---------------------------------------------------------------- toolbar

@Composable
private fun TopToolbar(
    deviceName: String,
    onBack: () -> Unit,
    onTextInput: () -> Unit,
    onMenu: () -> Unit,
    menu: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Dipasang sebelum background: area status bar tetap hitam polos,
            // isi header mulai tepat di bawah status bar.
            .statusBarsPadding()
            .background(HEADER_BG)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = t("Back"),
                tint = Color.White,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                deviceName.ifEmpty { "scrcpy" },
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                t("Mirror active"),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        IconButton(onClick = onTextInput) {
            Icon(Icons.Filled.Keyboard, contentDescription = t("Send text"), tint = Color.White)
        }
        // Box pembungkus = jangkar DropdownMenu supaya menu muncul menempel
        // pada tombol ini, bukan di sudut layar penuh.
        Box {
            IconButton(onClick = onMenu) {
                Icon(Icons.Filled.MoreVert, contentDescription = t("More options"), tint = Color.White)
            }
            menu()
        }
    }
}

// ---------------------------------------------------------------- bottom nav

@Composable
private fun BottomNav(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onRecents: () -> Unit,
    onRotate: () -> Unit,
    onNotifications: () -> Unit,
    onResetVideo: () -> Unit,
    onScreenOff: () -> Unit,
) {
    Surface(
        color = Color(0xFF161616),
        // Sengaja persegi: panel navigasi tidak lagi membulat di pinggirnya.
        shape = RectangleShape,
        modifier = Modifier
            .fillMaxWidth()
            // Dipasang di Surface: panel navigasi tetap hitam, footer di atasnya.
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavButton(Icons.Filled.Home, "Home", onHome)
            NavButton(Icons.Filled.Apps, "Recent apps", onRecents)
            NavButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
            NavButton(Icons.Filled.Notifications, "Notification panel", onNotifications)
            NavButton(Icons.Filled.ScreenRotation, "Rotate screen", onRotate)
            NavButton(Icons.Filled.Refresh, "Reset video", onResetVideo)
            NavButton(Icons.Filled.Lock, "Turn screen off", onScreenOff)
        }
    }
}

@Composable
private fun NavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
        modifier = Modifier.size(44.dp),
    ) {
        Icon(icon, contentDescription = t(label), modifier = Modifier.size(22.dp))
    }
}

// ---------------------------------------------------------------- overflow

@Composable
private fun OverflowMenu(
    onDismiss: () -> Unit,
    onTextInput: () -> Unit,
    onCopyClipboard: () -> Unit,
    onScreenshot: () -> Unit,
    onResetVideo: () -> Unit,
    showCameraControls: Boolean,
    torchOn: Boolean,
    onToggleTorch: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onSwitchCamera: () -> Unit,
    onDisconnect: () -> Unit,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
) {
    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(t("Send text")) },
            leadingIcon = { Icon(Icons.Filled.Keyboard, null) },
            onClick = onTextInput,
        )
        DropdownMenuItem(
            text = { Text(t("Paste clipboard")) },
            leadingIcon = { Icon(Icons.Filled.Description, null) },
            onClick = onCopyClipboard,
        )
        DropdownMenuItem(
            text = { Text(t("Screenshot")) },
            leadingIcon = { Icon(Icons.Filled.CameraAlt, null) },
            onClick = onScreenshot,
        )
        DropdownMenuItem(
            text = { Text(t("Reset video")) },
            leadingIcon = { Icon(Icons.Filled.Refresh, null) },
            onClick = onResetVideo,
        )
        DropdownMenuItem(
            text = { Text(t(if (fullscreen) "Exit fullscreen" else "Fullscreen")) },
            leadingIcon = {
                Icon(
                    if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    null,
                )
            },
            onClick = onToggleFullscreen,
        )

        if (showCameraControls) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(if (torchOn) t("Turn torch off") else t("Turn torch on")) },
                leadingIcon = { Icon(Icons.Filled.FlashOn, null) },
                onClick = onToggleTorch,
            )
            DropdownMenuItem(
                text = { Text(t("Zoom in")) },
                leadingIcon = { Icon(Icons.Filled.ZoomIn, null) },
                onClick = onZoomIn,
            )
            DropdownMenuItem(
                text = { Text(t("Zoom out")) },
                leadingIcon = { Icon(Icons.Filled.ZoomOut, null) },
                onClick = onZoomOut,
            )
            DropdownMenuItem(
                text = { Text(t("Switch camera")) },
                leadingIcon = { Icon(Icons.Filled.FlipCameraAndroid, null) },
                onClick = onSwitchCamera,
            )
        }

        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(t("End session")) },
            leadingIcon = { Icon(Icons.Filled.Lock, null) },
            onClick = onDisconnect,
        )
    }
}

// ---------------------------------------------------------------- screenshot

/**
 * Simpan frame terakhir [view] ke `Pictures/Scrcpy` lewat MediaStore.
 * Min SDK 29 sehingga `RELATIVE_PATH` selalu tersedia (tanpa izin tulis).
 */
private fun captureScreenshot(context: Context, view: TextureView?): String? {
    val bitmap = runCatching { view?.bitmap }.getOrNull() ?: return null
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val name = "scrcpy_$stamp.png"
    val uri = runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Scrcpy")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }.getOrNull() ?: return null

    return try {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } ?: return null
        val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        context.contentResolver.update(uri, done, null, null)
        name
    } catch (_: Throwable) {
        runCatching { context.contentResolver.delete(uri, null, null) }
        null
    }
}

// ---------------------------------------------------------------- dialog

@Composable
private fun TextInputDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t("Send text to the device")) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(300) },
                label = { Text(t("Text (max. 300 characters)")) },
                minLines = 2,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onSubmit(text) },
                enabled = text.isNotBlank(),
            ) { Text(t("Send")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("Cancel")) } },
    )
}
