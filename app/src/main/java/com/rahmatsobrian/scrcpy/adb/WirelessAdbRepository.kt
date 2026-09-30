package com.rahmatsobrian.scrcpy.adb

import android.content.Context
import android.net.wifi.WifiManager
import com.flyfishxu.kadb.DelayedAckMode
import com.flyfishxu.kadb.Kadb
import com.flyfishxu.kadb.KadbOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Koneksi ADB nirkabel: pairing (SPAKE2, port 37xxx) + connect (TLS, port 5555).
 *
 * Sumber: `<scrcpy>/doc/develop.md`, `adb pair` / `adb connect` konvensional.
 * - Pairing port: **37000 + (5555 & 0xFFF)** pada Android ≥ 11, tetapi scrcpy
 *   dan adb klien memakai port yang diberikan pengguna (default 37400).
 * - Connect port: **5555** (tcpip) setelah `adb tcpip` / wireless debugging aktif.
 */
class WirelessAdbRepository(private val context: Context) {

    /** IP lokal perangkat ini (klien) untuk ditampilkan ke pengguna. */
    fun localIpAddress(): String {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val ip = wifi.connectionInfo?.ipAddress ?: 0
        if (ip != 0) {
            return String.format(
                "%d.%d.%d.%d",
                ip and 0xFF, (ip shr 8) and 0xFF, (ip shr 16) and 0xFF, (ip shr 24) and 0xFF,
            )
        }
        return firstIpv4() ?: "0.0.0.0"
    }

    private fun firstIpv4(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces()?.toList()
            ?.flatMap { it.inetAddresses?.toList() ?: emptyList() }
            ?.filterIsInstance<Inet4Address>()
            ?.firstOrNull { !it.isLoopbackAddress && it.isReachable(200) }
            ?.hostAddress
    }.getOrNull()

    /**
     * Pasangkan perangkat dengan kode 6 digit (dialog Wireless Debugging).
     * Melempar [IOException] bila gagal.
     */
    suspend fun pair(host: String, port: Int, pairingCode: String): Unit =
        withContext(Dispatchers.IO) {
            val code = pairingCode.filter { it.isDigit() }
            require(code.length == 6) { "Kode pairing harus 6 digit" }
            Kadb.pair(host, port, code)
        }

    /**
     * Buka sesi ADB ke perangkat yang sudah dipasangkan / `adb tcpip`.
     * Melempar [IOException] bila tidak bisa tersambung.
     */
    suspend fun connect(host: String, port: Int = 5555): Kadb =
        withContext(Dispatchers.IO) {
            val kadb = Kadb.create(
                host = host,
                port = port,
                connectTimeout = 5_000,
                socketTimeout = 15_000,
                options = KadbOptions(DelayedAckMode.ENABLED),
            )
            // Paksa handshake & verifikasi benar-benar hidup.
            runCatching {
                val out = kadb.shell("echo ok").output
                if (!out.contains("ok")) throw IOException("Balasan shell tidak sesuai: $out")
            }.onFailure { t ->
                runCatching { kadb.close() }
                throw if (t is IOException) t else IOException(t.message, t)
            }
            kadb
        }

    companion object {
        const val DEFAULT_CONNECT_PORT = 5555
        const val DEFAULT_PAIR_PORT = 37400

        /**
         * Terjemahkan kegagalan koneksi nirkabel ke pesan yang bisa dipahami.
         *
         * Kegagalan TLS (SSL handshake / protocol error) hampir selalu berarti
         * kunci aplikasi ini belum terdaftar di perangkat target, yaitu belum
         * melakukan **pairing**.
         */
        fun describeError(t: Throwable, host: String = "", port: Int = 0): String {
            val chain = generateSequence(t) { it.cause }.toList()
            val text = chain.joinToString("\n") { it.message.orEmpty() }.lowercase()
            val target = if (host.isBlank()) "" else "$host:$port".trimEnd(':')

            val tls = chain.any { it is javax.net.ssl.SSLException } ||
                text.contains("ssl") ||
                text.contains("handshake") ||
                text.contains("certificate") ||
                text.contains("protocol error") ||
                text.contains("connection reset")

            val unreachable = text.contains("connection refused") ||
                text.contains("failed to connect") ||
                text.contains("timed out") ||
                text.contains("no route to host") ||
                text.contains("host is unreachable") ||
                text.contains("network is unreachable")

            return when {
                tls ->
                    com.rahmatsobrian.scrcpy.ui.i18n.t(
                        "TLS handshake failed — this app has not paired with the device yet. " +
                            "Open Settings → Wireless debugging → Pair device with pairing " +
                            "code, enter the pairing port and the 6-digit code, then tap Pair. " +
                            "After that tap Connect using the port shown on the Wireless " +
                            "debugging screen.",
                    )

                unreachable ->
                    com.rahmatsobrian.scrcpy.ui.i18n.tf(
                        "Cannot reach {}. Make sure Wireless debugging is still enabled and " +
                            "both devices are on the same Wi-Fi network. Note: the Wireless " +
                            "debugging port changes every time it is turned off — check the " +
                            "port again on the Wireless debugging screen.",
                        target,
                    )

                else -> t.message?.takeIf { it.isNotBlank() } ?: t.toString()
            }
        }
    }
}
