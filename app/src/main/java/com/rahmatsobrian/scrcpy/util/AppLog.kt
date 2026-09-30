package com.rahmatsobrian.scrcpy.util

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaScannerConnection
import androidx.compose.runtime.mutableStateOf
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Log aplikasi yang **bisa dibuka dari file manager tanpa root/adb**.
 *
 * Folder utama: `Android/media/<package>/`
 * (hasil [Context.getExternalMediaDirs] — tidak butuh izin sama sekali,
 *  dan terlihat oleh aplikasi lain lewat FUSE).
 *
 * Berkas:
 * - [LOG_NAME]    — jejak aktivitas (tiap aksi pengguna, ukuran video, state).
 * - [CRASH_NAME]  — stack trace force-close + ekor [LOG_NAME] + logcat.
 *
 * Fallback: bila media storage belum tersedia, pindah ke `filesDir`.
 */
object AppLog {

    private const val TAG = "ScrcpyLog"
    const val LOG_NAME = "scrcpy.log"
    const val CRASH_NAME = "crash.log"

    private const val MAX_LOG_BYTES = 512_000L
    private const val MAX_CRASH_BYTES = 1_048_576L
    private const val LOGCAT_LINES = 1500
    private const val TAIL_LINES = 40

    private const val PREFS_NAME = "logging"
    private const val KEY_ENABLED = "enabled"

    @Volatile
    private var dir: File? = null

    /**
     * Sakelar catatan aktivitas ([LOG_NAME]). Bisa dimatikan dari Settings.
     * Log crash ([CRASH_NAME]) **tidak** dipengaruhi — itu tetap ditulis.
     *
     * Disimpan sebagai [mutableStateOf] supaya membaca nilainya saat
     * komposisi Compose ikut berlangganan dan switch langsung bergerak.
     */
    private val enabledState = mutableStateOf(true)

    val enabled: Boolean
        get() = enabledState.value

    private val lock = Any()

    fun init(context: Context) {
        val app = context.applicationContext
        enabledState.value = prefs(app).getBoolean(KEY_ENABLED, true)
        if (dir != null) return
        val media = runCatching { app.getExternalMediaDirs() }
            .getOrNull()
            ?.firstOrNull { it != null && it.isDirectory }
        val base = media ?: File(app.filesDir, "logs")
        runCatching { base.mkdirs() }
        dir = base
        runCatching {
            MediaScannerConnection.scanFile(app, arrayOf(base.absolutePath), null, null)
        }
        Log.i(TAG, "folder log=${base.absolutePath}")
        log(
            "proses dimulai (model=${android.os.Build.MODEL}, " +
                "sdk=${android.os.Build.VERSION.SDK_INT})",
        )
    }

    /** Folder log, mis. `/storage/emulated/0/Android/media/<package>`. */
    fun folder(): File? = dir

    fun file(name: String): File? = dir?.let { File(it, name) }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Nyalakan/matikan catatan aktivitas (persisten). */
    fun setEnabled(context: Context, value: Boolean) {
        val app = context.applicationContext
        prefs(app).edit().putBoolean(KEY_ENABLED, value).apply()
        val previous = enabledState.value
        enabledState.value = value
        if (value && !previous) log("catatan log dinyalakan kembali")
    }

    /** Tulis satu baris jejak aktivitas ke [LOG_NAME]. */
    fun log(message: String) {
        if (!enabledState.value) return
        val f = file(LOG_NAME) ?: return
        runCatching {
            synchronized(lock) {
                if (f.length() > MAX_LOG_BYTES) f.delete()
                f.appendText("${stamp()}  $message\n", Charsets.UTF_8)
            }
        }.onFailure { Log.w(TAG, "gagal menulis log: ${it.message}") }
    }

    /**
     * Dipanggil dari uncaught exception handler: tulis stack trace lengkap
     * beserta logcat dan ekor [LOG_NAME] sebelum proses dibunuh sistem.
     */
    fun crash(thread: Thread, error: Throwable) {
        val f = file(CRASH_NAME) ?: return
        runCatching {
            synchronized(lock) {
                if (f.length() > MAX_CRASH_BYTES) f.delete()
                val sb = StringBuilder()
                sb.append("\n===== ").append(stamp()).append(" =====\n")
                    .append("thread: ").append(thread.name).append('\n')
                    .append(Log.getStackTraceString(error))
                    .append("\n----- aktivitas terakhir -----\n")
                    .append(tail())
                    .append("\n----- logcat (").append(LOGCAT_LINES).append(" baris) -----\n")
                    .append(logcat())
                f.appendText(sb.toString(), Charsets.UTF_8)
            }
        }.onFailure { Log.e(TAG, "gagal menulis crash log", it) }
    }

    /** Tulis potongan teks besar (mis. tombstone) ke [CRASH_NAME]. */
    fun crashNote(message: String) {
        val f = file(CRASH_NAME) ?: return
        runCatching {
            synchronized(lock) {
                if (f.length() > MAX_CRASH_BYTES) f.delete()
                f.appendText("\n===== ${stamp()} =====\n$message\n", Charsets.UTF_8)
            }
        }.onFailure { Log.w(TAG, "gagal menulis crash log: ${it.message}") }
    }

    /**
     * Ambil ekor logcat saat **awal proses** dan tulis ke [CRASH_NAME].
     *
     * Ini penting untuk crash **native** (SIGSEGV/SIGABRT): uncaught-exception
     * handler tidak pernah terpanggil, jadi satu-satunya jejak yang tersisa
     * adalah baris `F libc : Fatal signal ... in tid N (NAMA_THREAD)` yang
     * tetap terbaca oleh proses sendiri tanpa izin READ_LOGS.
     */
    fun dumpLogcat(reason: String) {
        val raw = logcat()
        val fatal = raw.lineSequence()
            .filter { it.contains("Fatal signal") || it.contains("F DEBUG") }
            .joinToString("\n")
            .trim()
        if (fatal.isBlank()) {
            log("tidak ada 'Fatal signal' di logcat — kematian kemungkinan bukan SIGSEGV")
            return
        }
        log("sinyal fatal sebelumnya ($reason):\n$fatal")
        val f = file(CRASH_NAME) ?: return
        runCatching {
            synchronized(lock) {
                if (f.length() > MAX_CRASH_BYTES) f.delete()
                val sb = StringBuilder()
                    .append("\n===== ").append(stamp()).append(" (dump saat start) =====\n")
                    .append(reason).append('\n')
                    .append("----- sinyal fatal -----\n").append(fatal).append('\n')
                    .append("----- logcat (").append(LOGCAT_LINES).append(" baris) -----\n")
                    .append(raw)
                f.appendText(sb.toString(), Charsets.UTF_8)
            }
        }.onFailure { Log.e(TAG, "gagal menulis dump logcat", it) }
    }

    private fun tail(): String = runCatching {
        val f = file(LOG_NAME) ?: return@runCatching "(belum ada log)"
        val lines = f.readLines(Charsets.UTF_8)
        lines.takeLast(TAIL_LINES).joinToString("\n").ifBlank { "(log kosong)" }
    }.getOrDefault("(gagal membaca log)")

    private fun logcat(): String = runCatching {
        val process = ProcessBuilder("logcat", "-d", "-v", "threadtime", "-t", "$LOGCAT_LINES")
            .redirectErrorStream(true)
            .start()
        val out = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor()
        out.ifBlank { "(logcat kosong — tidak ada izin READ_LOGS)" }
    }.getOrDefault("(logcat tidak tersedia)")

    private fun stamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
}
