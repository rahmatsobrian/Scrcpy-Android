package com.rahmatsobrian.scrcpy.adb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.flyfishxu.kadb.DelayedAckMode
import com.flyfishxu.kadb.Kadb
import com.flyfishxu.kadb.KadbOptions
import com.flyfishxu.kadb.transport.TransportChannel
import com.rahmatsobrian.scrcpy.adb.usb.UsbTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Menemukan perangkat ADB (USB OTG) dan mempertahankan receiver izin USB.
 *
 * Perangkat nirkabel (TCP/TLS + pairing) ditangani terpisah oleh
 * [WirelessAdbRepository].
 */
class UsbAdbRepository(private val context: Context) {

    data class UsbEntry(
        val device: UsbDevice,
        val hasPermission: Boolean,
    )

    private val _devices = MutableStateFlow<List<UsbEntry>>(emptyList())
    val devices: StateFlow<List<UsbEntry>> = _devices.asStateFlow()

    private val _permissionDenied = MutableStateFlow<UsbDevice?>(null)
    val permissionDenied: StateFlow<UsbDevice?> = _permissionDenied.asStateFlow()

    private val manager: UsbManager =
        context.getSystemService(Context.USB_SERVICE) as UsbManager

    private var receiver: BroadcastReceiver? = null
    private var registered = false

    private var hotplugReceiver: BroadcastReceiver? = null

    /**
     * Pantau colok/keluar kabel USB supaya daftar perangkat ikut ter-update
     * tanpa perlu menekan tombol Segarkan secara manual.
     */
    fun start() {
        if (hotplugReceiver != null) return
        val r = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) = refresh()
        }
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(r, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(r, filter)
            }
            hotplugReceiver = r
        }
    }

    /** Daftar perangkat yang punya interface ADB (class 0xFF/0x42/0x01). */
    fun refresh() {
        val list = manager.deviceList.values
            .filter { UsbTransport.isAdbDevice(it) }
            .map { UsbEntry(it, manager.hasPermission(it)) }
            .sortedBy { it.device.deviceName }
        _devices.value = list
    }

    suspend fun requestPermission(device: UsbDevice) {
        ensureReceiver()
        val intent = Intent(ACTION_USB_PERMISSION).setPackage(context.packageName)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val pi = PendingIntent.getBroadcast(context, 0, intent, flags)
        manager.requestPermission(device, pi)
    }

    private fun ensureReceiver() {
        if (registered) return
        val r = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action != ACTION_USB_PERMISSION) return
                @Suppress("DEPRECATION")
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                device ?: return
                if (granted) {
                    _permissionDenied.value = null
                } else {
                    _permissionDenied.value = device
                }
                refresh()
            }
        }
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(r, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(r, filter)
        }
        receiver = r
        registered = true
    }

    /** Buka transport USB untuk [device]; lempar [IOException] bila gagal. */
    fun openChannel(device: UsbDevice): TransportChannel = UsbTransport.open(manager, device)

    /** `Kadb` yang (re)koneksi ulang lewat USB tiap kali dibutuhkan. */
    fun kadbFor(device: UsbDevice): Kadb = Kadb.fromChannel(
        channelProvider = { withContext(Dispatchers.IO) { openChannel(device).also { refresh() } } },
        socketTimeout = 15_000,
        options = KadbOptions(DelayedAckMode.ENABLED),
    )

    fun close() {
        receiver?.let { runCatching { context.unregisterReceiver(it) } }
        receiver = null
        registered = false
        hotplugReceiver?.let { runCatching { context.unregisterReceiver(it) } }
        hotplugReceiver = null
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.rahmatsobrian.scrcpy.USB_PERMISSION"
    }
}
