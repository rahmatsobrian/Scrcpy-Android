package com.rahmatsobrian.scrcpy.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Mode tampilan aplikasi. */
enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

/**
 * Preferensi tampilan yang tersimpan di SharedPreferences sehingga langsung
 * terpakai pada peluncuran berikutnya, tanpa tombol simpan.
 */
object ThemeState {

    private const val PREFS = "appearance"
    private const val KEY_MODE = "theme_mode"
    private const val KEY_DYNAMIC = "dynamic_color"
    private const val KEY_SEED = "seed_color"

    /** Warna aksen bawaan (biru Material 3). */
    val DefaultSeed = Color(0xFF4E7BFF)

    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    var dynamicColor by mutableStateOf(true)
        private set

    var seed by mutableStateOf(DefaultSeed)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        mode = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_MODE, ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM)
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC, true)
        seed = Color(prefs.getInt(KEY_SEED, DefaultSeed.toArgbInt()))
    }

    fun setMode(context: Context, value: ThemeMode) {
        mode = value
        save(context) { putString(KEY_MODE, value.name) }
    }

    fun setDynamicColor(context: Context, value: Boolean) {
        dynamicColor = value
        save(context) { putBoolean(KEY_DYNAMIC, value) }
    }

    fun setSeed(context: Context, value: Color) {
        seed = value
        save(context) { putInt(KEY_SEED, value.toArgbInt()) }
    }

    private inline fun save(context: Context, block: android.content.SharedPreferences.Editor.() -> Unit) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            block()
            apply()
        }
    }

    private fun Color.toArgbInt(): Int = android.graphics.Color.argb(
        (alpha * 255).toInt().coerceIn(0, 255),
        (red * 255).toInt().coerceIn(0, 255),
        (green * 255).toInt().coerceIn(0, 255),
        (blue * 255).toInt().coerceIn(0, 255),
    )
}
