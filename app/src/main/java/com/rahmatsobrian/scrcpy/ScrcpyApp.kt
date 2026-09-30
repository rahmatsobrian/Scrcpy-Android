package com.rahmatsobrian.scrcpy

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.flyfishxu.kadb.cert.KadbCert
import com.flyfishxu.kadb.cert.OkioFilePrivateKeyStore
import com.rahmatsobrian.scrcpy.ui.i18n.Lang
import com.rahmatsobrian.scrcpy.ui.i18n.t
import com.rahmatsobrian.scrcpy.ui.theme.ThemeState
import com.rahmatsobrian.scrcpy.util.AppLog
import okio.Path.Companion.toPath
import java.io.File

class ScrcpyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        ThemeState.init(this)
        Lang.init(this)
        val prevExit = reportPreviousExit()
        AppLog.dumpLogcat("kematian proses sebelumnya: $prevExit")
        configureAdbIdentity()
        installCrashLogger()
        createNotificationChannels()
    }

    /**
     * Laporkan penyebab matinya proses **sebelumnya** (Android 11+/API 30).
     *
     * Handler uncaught-exception tidak berfungsi untuk kematian native
     * (SIGSEGV/SIGABRT) atau pembunuhan oleh sistem (lmkd) — hanya
     * [ApplicationExitInfo] yang bisa memberi tahu alasannya.
     */
    private fun reportPreviousExit(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return "sdk<30 (tanpa riwayat ApplicationExitInfo)"
        }
        return runCatching {
            val am = getSystemService(ActivityManager::class.java)
                ?: return@runCatching "ActivityManager tidak tersedia"
            val exits = am.getHistoricalProcessExitReasons(packageName, 0, 4)
            if (exits.isEmpty()) {
                AppLog.log("exit: belum ada riwayat kematian proses")
                return@runCatching "belum ada riwayat kematian proses"
            }
            // Diurutkan terbaru dulu; dicetak dari yang paling lama agar
            // kematian terakhir terbaca paling bawah.
            for (i in exits.indices.reversed()) {
                val e = exits[i]
                val ageMs = (System.currentTimeMillis() - e.timestamp).coerceAtLeast(0L)
                AppLog.log(
                    "exit: ${reasonName(e.reason)}(${e.reason}) | status=${e.status} | " +
                        "importance=${e.importance} | pid=${e.pid} | " +
                        "age=${ageMs}ms | ${e.description ?: "-"}",
                )
            }
            // Tombstone/stack untuk kematian yang paling relevan:
            // CRASH_NATIVE (SIGSEGV) > CRASH (exception java) > ANR.
            // `exits` sudah urut terbaru-dulu, jadi take(2) = yang terbaru.
            val interesting = exits.filter {
                it.reason == ApplicationExitInfo.REASON_CRASH_NATIVE ||
                    it.reason == ApplicationExitInfo.REASON_CRASH ||
                    it.reason == ApplicationExitInfo.REASON_ANR
            }
            for (e in interesting.take(2)) {
                runCatching {
                    val bytes = e.traceInputStream?.use { it.readBytes() } ?: ByteArray(0)
                    if (bytes.isNotEmpty()) {
                        AppLog.crashNote(
                            "exit trace (pid=${e.pid}, ${reasonName(e.reason)}):\n" +
                                readableTrace(bytes, minOf(bytes.size, 150_000)),
                        )
                    }
                }
            }
            val newest = interesting.firstOrNull() ?: exits.first()
            val ageMs = (System.currentTimeMillis() - newest.timestamp).coerceAtLeast(0L)
            "${reasonName(newest.reason)}(${newest.reason}) pid=${newest.pid} age=${ageMs}ms"
        }.getOrElse { "gagal membaca riwayat exit: $it" }
    }

    private fun reasonName(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_UNKNOWN -> "UNKNOWN"
        ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF (aplikasi sendiri berhenti)"
        ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED (sinyal OS, cek status=sinyal)"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY (lmkd)"
        ApplicationExitInfo.REASON_CRASH -> "CRASH (exception java)"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE (sinyal native)"
        ApplicationExitInfo.REASON_ANR -> "ANR"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INIT_FAILURE"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "PERMISSION_CHANGE"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
        ApplicationExitInfo.REASON_USER_STOPPED -> "USER_STOPPED"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "DEPENDENCY_DIED"
        ApplicationExitInfo.REASON_OTHER -> "OTHER (dibunuh sistem)"
        ApplicationExitInfo.REASON_FREEZER -> "FREEZER"
        ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE -> "PACKAGE_STATE_CHANGE"
        ApplicationExitInfo.REASON_PACKAGE_UPDATED -> "PACKAGE_UPDATED"
        else -> "LAINNYA($reason)"
    }

    /**
     * Tombstone berbentuk protobuf/biner sehingga sulit dibaca. Ekstrak semua
     * potongan teks ASCII (≥4 huruf) — di antaranya ada **nama thread** dan
     * **nama frame/fungsi**, sehingga susunan thread + stack-nya terbaca.
     */
    private fun readableTrace(raw: ByteArray, limit: Int): String {
        val out = StringBuilder(limit / 2)
        val cur = StringBuilder()
        val end = minOf(raw.size, limit)
        for (i in 0 until end) {
            val c = raw[i].toInt() and 0xFF
            if (c in 32..126) {
                cur.append(c.toChar())
            } else {
                if (cur.length >= 4) out.append(cur).append('\n')
                cur.setLength(0)
            }
        }
        if (cur.length >= 4) out.append(cur).append('\n')
        return out.toString().ifBlank { "(tombstone tanpa teks terbaca)" }
    }

    /**
     * Tulis stack trace force-close ke `Android/media/<package>/crash.log`
     * sebelum proses mati. Folder itu bisa dibuka dari file manager tanpa
     * root, sehingga penyebab FC gampang diperiksa.
     */
    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { AppLog.crash(thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Simpan identitas ADB (kunci RSA) ke disk.
     *
     * Default kadb memakai [com.flyfishxu.kadb.cert.InMemoryPrivateKeyStore],
     * sehingga identitas hilang tiap kali proses berhenti. Akibatnya setiap
     * buka aplikasi harus **pairing ulang** dan hp target menumpuk entri baru
     * di `adb_keys`. Dengan store berbasis file, pairing cukup sekali.
     */
    private fun configureAdbIdentity() {
        runCatching {
            KadbCert.configure(
                OkioFilePrivateKeyStore(File(filesDir, "adbkey").absolutePath.toPath()),
            )
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SESSION,
                    t("Mirror session"),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = t("Shown while a scrcpy session is running") },
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ADB,
                    t("ADB service"),
                    NotificationManager.IMPORTANCE_MIN,
                ).apply { description = t("Shown by the ADB connection service") },
            )
        }
    }

    companion object {
        const val CHANNEL_SESSION = "session"
        const val CHANNEL_ADB = "adb"
    }
}
