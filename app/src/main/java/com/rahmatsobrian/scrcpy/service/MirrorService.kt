package com.rahmatsobrian.scrcpy.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rahmatsobrian.scrcpy.ui.i18n.t
import com.rahmatsobrian.scrcpy.MainActivity
import com.rahmatsobrian.scrcpy.R
import com.rahmatsobrian.scrcpy.scrcpy.ScrcpyController
import com.rahmatsobrian.scrcpy.util.AppLog

/**
 * Layanan latar depan yang menjaga proses tetap hidup selama sesi mirror
 * berjalan (USB/wireless), sekaligus menampilkan notifikasi persisten
 * dengan aksi "Berhenti".
 *
 * Tipe `connectedDevice` dipilih karena aplikasi memang berinteraksi dengan
 * perangkat ADB yang tersambung (OTG maupun TCP). Tipe ini tidak dibatasi
 * kuota waktu seperti `dataSync`.
 */
class MirrorService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                active.set(false)
                runCatching { sessionStopper?.stop() }
                dismiss()
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val name = intent?.getStringExtra(EXTRA_DEVICE_NAME).orEmpty()
        createChannel()
        val notification = buildNotification(name.ifBlank { "Perangkat ADB" })

        // `startForeground()` dipanggil **paling awal**: sistem hanya memberi
        // jendela 5 detik sejak `startForegroundService()`, dan bila lewat
        // aplikasi dihancurkan dengan
        // `ForegroundServiceDidNotStartInTimeException`.
        val fgOk = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
                )
            } else {
                @Suppress("DEPRECATION")
                startForeground(NOTIFICATION_ID, notification)
            }
            AppLog.log("MirrorService: startForeground OK")
            true
        } catch (t: Throwable) {
            AppLog.log("MirrorService: startForeground GAGAL: $t")
            false
        }

        if (!fgOk) {
            // Penuhi kewajiban dengan berhenti secepat mungkin agar sistem
            // tidak melempar exception ±5 detik kemudian.
            runCatching {
                getSystemService(NotificationManager::class.java)
                    ?.notify(NOTIFICATION_ID, notification)
            }
            dismiss()
            stopSelf()
            return START_NOT_STICKY
        }

        // Sesi sudah dimatikan dari sisi lain, jangan biarkan FGS menggantung.
        if (!active.get()) {
            AppLog.log("MirrorService: sesi sudah berhenti → stopSelf")
            dismiss()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        runCatching { getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID) }
        super.onDestroy()
    }

    private fun dismiss() {
        @Suppress("DEPRECATION")
        runCatching { stopForeground(true) }
        runCatching { getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID) }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            t("Active mirror session"),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = t("Tells you that a scrcpy session is running")
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(deviceName: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MirrorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("scrcpy aktif")
            .setContentText(deviceName)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, "Berhenti", stopIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "scrcpy_session"
        const val NOTIFICATION_ID = 4101
        const val ACTION_STOP = "com.rahmatsobrian.scrcpy.action.STOP_SESSION"
        private const val EXTRA_DEVICE_NAME = "device_name"

        /** Dibagikan agar aksi "Berhenti" di notifikasi bisa menghentikan sesi. */
        @Volatile
        internal var sessionStopper: ScrcpyController? = null

        private val active = java.util.concurrent.atomic.AtomicBoolean(false)

        /**
         * Mulai (atau perbarui) notifikasi sesi. Idempotent — panggil berulang
         * saat nama perangkat berubah tidak akan membuat layanan ganda.
         */
        fun start(context: Context, deviceName: String) {
            val alreadyActive = active.getAndSet(true)
            val intent = Intent(context, MirrorService::class.java)
                .putExtra(EXTRA_DEVICE_NAME, deviceName)
            val ok = runCatching {
                if (alreadyActive) {
                    context.startService(intent)
                } else {
                    context.startForegroundService(intent)
                }
                true
            }.getOrElse {
                AppLog.log(
                    "MirrorService.start gagal (alreadyActive=$alreadyActive): $it",
                )
                false
            }
            // Kalau gagal (mis. app sudah di latar belakang di Android 12+),
            // jangan tandai aktif agar stop() nanti tidak mengirim intent mati.
            if (!ok) active.set(false)
        }

        fun stop(context: Context) {
            if (!active.compareAndSet(true, false)) return
            runCatching { context.stopService(Intent(context, MirrorService::class.java)) }
        }
    }
}
