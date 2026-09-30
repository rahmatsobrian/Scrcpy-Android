package com.rahmatsobrian.scrcpy.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AmoledColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF004494),
    onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF263141),
    secondaryContainer = Color(0xFF3C4758),
    onSecondaryContainer = Color(0xFFD7E3F8),
    tertiary = Color(0xFFD6BEE4),
    onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF523F5F),
    onTertiaryContainer = Color(0xFFF3DAFF),
    background = Color.Black,
    onBackground = Color(0xFFE2E2E6),
    surface = Color.Black,
    onSurface = Color(0xFFE2E2E6),
    surfaceContainer = Color(0xFF0A0A0A),
    surfaceContainerHigh = Color(0xFF141414),
    surfaceContainerHighest = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6CF),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    outline = Color(0xFF8E9099),
)

private val DarkColors = darkColorScheme(
    background = Color(0xFF0B0B0F),
    surface = Color(0xFF111116),
    surfaceContainer = Color(0xFF1A1A21),
)

private val LightColors = lightColorScheme()

private fun mix(a: Color, b: Color, ratio: Float): Color {
    val r = ratio.coerceIn(0f, 1f)
    return Color(
        red = a.red * (1f - r) + b.red * r,
        green = a.green * (1f - r) + b.green * r,
        blue = a.blue * (1f - r) + b.blue * r,
        alpha = 1f,
    )
}

private fun inkOf(color: Color): Color =
    if (color.luminance() > 0.45f) Color(0xFF101014) else Color(0xFFFFFFFF)

/**
 * Susun color scheme dari satu warna benih (seed) — dipakai saat *dynamic
 * color* dimatikan dan pengguna memilih warna aksen sendiri.
 */
private fun seedScheme(seed: Color, dark: Boolean, amoled: Boolean): ColorScheme {
    val base = when {
        amoled -> AmoledColors
        dark -> DarkColors
        else -> LightColors
    }
    val onPrimary = inkOf(seed)
    val primaryContainer = if (dark) mix(seed, Color.Black, 0.62f) else mix(seed, Color.White, 0.82f)
    val onPrimaryContainer = if (dark) mix(seed, Color.White, 0.80f) else mix(seed, Color.Black, 0.62f)
    val secondary = if (dark) mix(seed, Color.White, 0.35f) else mix(seed, Color.Black, 0.30f)
    val secondaryContainer = if (dark) mix(seed, Color.Black, 0.74f) else mix(seed, Color.White, 0.86f)
    val onSecondaryContainer = if (dark) mix(seed, Color.White, 0.74f) else mix(seed, Color.Black, 0.68f)
    val tertiary = if (dark) mix(seed, Color(0xFFB7A0FF), 0.45f) else mix(seed, Color(0xFF6750A4), 0.45f)
    val tertiaryContainer = if (dark) mix(tertiary, Color.Black, 0.62f) else mix(tertiary, Color.White, 0.82f)
    val onTertiaryContainer = if (dark) mix(tertiary, Color.White, 0.78f) else mix(tertiary, Color.Black, 0.62f)

    return base.copy(
        primary = seed,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = if (dark) Color(0xFF101014) else Color.White,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = inkOf(tertiary),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        inversePrimary = if (dark) mix(seed, Color.Black, 0.4f) else mix(seed, Color.White, 0.6f),
        outlineVariant = if (dark) mix(base.onSurface, Color.Black, 0.72f) else mix(base.onSurface, Color.White, 0.82f),
    )
}

/** Surface benar-benar hitam murni khas AMOLED. */
private fun ColorScheme.withAmoledSurfaces(): ColorScheme = copy(
    background = Color.Black,
    onBackground = Color(0xFFE2E2E6),
    surface = Color.Black,
    onSurface = Color(0xFFE2E2E6),
    surfaceContainer = Color(0xFF0A0A0A),
    surfaceContainerHigh = Color(0xFF141414),
    surfaceContainerHighest = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2A2A30),
    onSurfaceVariant = Color(0xFFC4C6CF),
)

@Composable
fun ScrcpyTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    val mode = ThemeState.mode
    val dark = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val amoled = mode == ThemeMode.AMOLED
    val dynamic = ThemeState.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val base = when {
        dynamic && dark -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        else -> seedScheme(ThemeState.seed, dark, amoled)
    }
    val colorScheme = if (amoled) base.withAmoledSurfaces() else base

    val view = LocalView.current
    if (!view.isInEditMode) {
        // enableEdgeToEdge() mengikuti mode gelap *sistem*; kita paksa ikut
        // tema aplikasi supaya ikon status bar & navigasi tetap kontras.
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
