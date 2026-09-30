package com.rahmatsobrian.scrcpy.adb

import android.content.Context

/** Target nirkabel terakhir yang berhasil tersambung. */
data class LastWirelessTarget(val host: String, val port: Int)

/**
 * Menyimpan target nirkabel terakhir di SharedPreferences, supaya setelah
 * aplikasi berhenti (atau crash) pengguna tidak perlu mengetik ulang IP/port
 * dan bisa langsung menyambungkan ulang.
 */
internal object LastConnectionStore {
    private const val PREFS = "scrcpy_last_connection"
    private const val KEY_HOST = "host"
    private const val KEY_PORT = "port"

    fun load(context: Context): LastWirelessTarget? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val host = prefs.getString(KEY_HOST, null)?.takeIf { it.isNotBlank() } ?: return null
        val port = prefs.getInt(KEY_PORT, 0)
        if (port !in 1..65535) return null
        return LastWirelessTarget(host, port)
    }

    fun save(context: Context, target: LastWirelessTarget) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HOST, target.host)
            .putInt(KEY_PORT, target.port)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
