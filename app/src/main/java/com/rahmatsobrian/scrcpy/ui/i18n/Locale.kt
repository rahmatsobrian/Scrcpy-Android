package com.rahmatsobrian.scrcpy.ui.i18n

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Bahasa antarmuka. Inggris adalah kunci kanonik di seluruh kode sumber. */
enum class AppLang(val tag: String, val endonym: String) {
    EN("en", "English"),
    ID("id", "Bahasa Indonesia"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLang =
            entries.firstOrNull { it.tag == tag } ?: EN
    }
}

/**
 * Status bahasa aplikasi.
 *
 * [current] adalah [androidx.compose.runtime.MutableState] agar pembacaan di
 * dalam composable ikut ter-recompose begitu pengguna berganti bahasa.
 */
object Lang {
    private const val PREFS = "appearance"
    private const val KEY_LANG = "lang"

    var current by mutableStateOf(AppLang.EN)
        private set

    /** Isi dari preferensi: bahasa perangkat bila belum pernah dipilih. */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANG, null)
        current = if (saved == null) systemLang() else AppLang.fromTag(saved)
    }

    fun set(context: Context, lang: AppLang) {
        current = lang
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, lang.tag)
            .apply()
    }

    private fun systemLang(): AppLang = runCatching {
        val tag = Resources.getSystem().configuration.locales[0].language
        if (tag == "id") AppLang.ID else AppLang.EN
    }.getOrDefault(AppLang.EN)
}

/**
 * Terjemahkan teks UI: [key] berisi teks Inggris (satu-satunya teks yang ada
 * di kode sumber). Saat bahasa Indonesia aktif dicari padanannya di [Indonesian];
 * bila belum diterjemahkan, teks Inggris dipakai apa adanya.
 */
fun t(key: String): String =
    if (Lang.current == AppLang.ID) Indonesian[key] ?: key else key

/**
 * Terjemah dengan tempat duduk `{}`: urutan [args] mengisi tiap `{}` pada teks
 * hasil terjemahan.
 */
fun tf(key: String, vararg args: Any): String {
    var out = t(key)
    for (a in args) out = out.replaceFirst("{}", a.toString())
    return out
}

/** Bahasa Indonesia hanya dipakai untuk menampilkan [key] Inggris. */
val Indonesian: Map<String, String> = StringsId
