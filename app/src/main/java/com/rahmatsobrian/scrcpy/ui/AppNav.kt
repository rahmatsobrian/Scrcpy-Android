package com.rahmatsobrian.scrcpy.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import com.rahmatsobrian.scrcpy.scrcpy.ScrcpyController
import com.rahmatsobrian.scrcpy.ui.screens.HomeScreen
import com.rahmatsobrian.scrcpy.ui.screens.MirrorScreen
import com.rahmatsobrian.scrcpy.ui.screens.SettingsLoadingScreen
import com.rahmatsobrian.scrcpy.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val MIRROR = "mirror"
    const val SETTINGS = "settings"
}

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val vm: AppViewModel = viewModel()
    val state by vm.sessionState.collectAsStateWithLifecycle()
    val connecting by vm.connecting.collectAsStateWithLifecycle()

    val enter: AnimatedContentTransitionScope<*>.() -> androidx.compose.animation.EnterTransition = {
        fadeIn(tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220))
    }
    val exit: AnimatedContentTransitionScope<*>.() -> androidx.compose.animation.ExitTransition = {
        fadeOut(tween(180)) + scaleOut(targetScale = 0.98f, animationSpec = tween(180))
    }

    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        enterTransition = enter,
        exitTransition = exit,
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(180)) },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                vm = vm,
                onOpenMirror = { nav.navigate(Routes.MIRROR) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            Routes.MIRROR,
            enterTransition = { fadeIn(tween(260)) },
            exitTransition = { fadeOut(tween(200)) },
        ) {
            MirrorScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.SETTINGS,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(260),
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(260),
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(260),
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(260),
                )
            },
        ) {
            // Muat opsi dari penyimpanan dulu, tampilkan ContainedLoadingIndicator
            // Expressive sejenak, lalu isi halaman pengaturan.
            var settingsReady by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                vm.reloadOptions()
                vm.refreshing.first { !it }
                settingsReady = true
            }
            Crossfade(
                targetState = settingsReady,
                animationSpec = tween(180),
                label = "settings",
            ) { ready ->
                if (ready) {
                    SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
                } else {
                    SettingsLoadingScreen(onBack = { nav.popBackStack() })
                }
            }
        }
    }

    // Konteks dipakai layar-layar di bawah untuk Toast.
    @Suppress("UNUSED_EXPRESSION")
    LocalContext.current

    // Izin notifikasi dibutuhkan agar notifikasi layanan latar depan
    // (sesi mirror) benar-benar tampil di Android 13+.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) {}
        LaunchedEffect(Unit) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
