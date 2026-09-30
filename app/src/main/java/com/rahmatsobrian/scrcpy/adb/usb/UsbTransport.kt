package com.rahmatsobrian.scrcpy.adb.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import com.flyfishxu.kadb.transport.TransportChannel
import java.io.EOFException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

/**
 * TransportChannel di atas bulk endpoint USB (ADB interface class 0xFF / subclass 0x42 / protocol 0x01).
 *
 * Framing identik dengan ADB over TCP (header 24 byte little-endian) sehingga AdbConnection
 * bisa dipakai apa adanya.
 */
class UsbTransport private constructor(
    private val connection: UsbDeviceConnection,
    private val iface: UsbInterface,
    private val epIn: UsbEndpoint,
    private val epOut: UsbEndpoint,
) : TransportChannel {

    @Volatile
    private var closed = false

    override val localAddress: InetSocketAddress = InetSocketAddress("127.0.0.1", 0)
    override val remoteAddress: InetSocketAddress = InetSocketAddress("127.0.0.1", 0)

    override val isOpen: Boolean
        get() = !closed

    override suspend fun read(dst: ByteBuffer, timeout: Long, unit: TimeUnit): Int {
        if (dst.remaining() == 0) return 0
        val deadline = if (timeout > 0) System.nanoTime() + unit.toNanos(timeout) else 0L
        while (!closed) {
            val chunk = dst.remaining().coerceAtMost(MAX_TRANSFER)
            val tmp = ByteArray(chunk)
            val transferred = connection.bulkTransfer(epIn, tmp, chunk, remainingTimeout(deadline))
            if (transferred > 0) {
                dst.put(tmp, 0, transferred)
                return transferred
            }
            if (closed) return -1
            if (deadline != 0L && System.nanoTime() >= deadline) {
                throw InterruptedIOException("USB read timeout")
            }
        }
        return -1
    }

    override suspend fun write(src: ByteBuffer, timeout: Long, unit: TimeUnit): Int {
        val deadline = if (timeout > 0) System.nanoTime() + unit.toNanos(timeout) else 0L
        var total = 0
        while (src.hasRemaining()) {
            if (closed) throw IOException("USB transport closed")
            val chunk = src.remaining().coerceAtMost(MAX_TRANSFER)
            val tmp = ByteArray(chunk)
            src.get(tmp)
            val transferred = connection.bulkTransfer(epOut, tmp, chunk, remainingTimeout(deadline))
            if (transferred <= 0) {
                if (closed) throw IOException("USB transport closed")
                if (deadline != 0L && System.nanoTime() >= deadline) {
                    throw InterruptedIOException("USB write timeout")
                }
                // Kembalikan posisi agar tidak ada data hilang.
                src.position(src.position() - chunk)
                throw IOException("USB bulkTransfer write failed: $transferred")
            }
            total += transferred
            if (transferred != chunk) {
                src.position(src.position() - (chunk - transferred))
                throw IOException("USB short write: $transferred/$chunk")
            }
        }
        return total
    }

    override suspend fun readExactly(dst: ByteBuffer, timeout: Long, unit: TimeUnit) {
        val deadline = if (timeout > 0) System.nanoTime() + unit.toNanos(timeout) else 0L
        while (dst.hasRemaining()) {
            val remaining = if (deadline == 0L) 0L else deadline - System.nanoTime()
            if (deadline != 0L && remaining <= 0) throw InterruptedIOException("USB readExactly timeout")
            val n = read(dst, if (deadline == 0L) 0 else remaining, TimeUnit.NANOSECONDS)
            if (n < 0) throw EOFException("EOF while readExactly")
        }
    }

    override suspend fun writeExactly(src: ByteBuffer, timeout: Long, unit: TimeUnit) {
        val deadline = if (timeout > 0) System.nanoTime() + unit.toNanos(timeout) else 0L
        while (src.hasRemaining()) {
            val remaining = if (deadline == 0L) 0L else deadline - System.nanoTime()
            if (deadline != 0L && remaining <= 0) throw InterruptedIOException("USB writeExactly timeout")
            write(src, if (deadline == 0L) 0 else remaining, TimeUnit.NANOSECONDS)
        }
    }

    override suspend fun shutdownInput() {
        // Bulk endpoint tidak punya separuh-tutup; abaikan.
    }

    override suspend fun shutdownOutput() {
        // Bulk endpoint tidak punya separuh-tutup; abaikan.
    }

    override fun close() {
        if (closed) return
        closed = true
        try {
            connection.releaseInterface(iface)
        } catch (_: Throwable) {
        }
        try {
            connection.close()
        } catch (_: Throwable) {
        }
    }

    private fun remainingTimeout(deadlineNanos: Long): Int {
        if (deadlineNanos == 0L) return 0
        val left = TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime())
        return left.coerceIn(1, Int.MAX_VALUE.toLong()).toInt()
    }

    companion object {
        private const val MAX_TRANSFER = 16 * 1024

        /** ADB USB interface: class 0xFF, subclass 0x42, protocol 0x01. */
        fun findAdbInterface(device: UsbDevice): UsbInterface? {
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                if (iface.interfaceClass == 0xFF &&
                    iface.interfaceSubclass == 0x42 &&
                    iface.interfaceProtocol == 0x01
                ) {
                    return iface
                }
            }
            return null
        }

        fun isAdbDevice(device: UsbDevice): Boolean = findAdbInterface(device) != null

        /**
         * Buka [device] dan klaim interface ADB. Melempar [IOException] bila perangkat
         * tidak punya interface ADB, izin ditolak, atau endpoint tidak lengkap.
         */
        @Throws(IOException::class)
        fun open(manager: UsbManager, device: UsbDevice): UsbTransport {
            val iface = findAdbInterface(device)
                ?: throw IOException("Perangkat ${device.deviceName} tidak memiliki interface ADB")

            var epIn: UsbEndpoint? = null
            var epOut: UsbEndpoint? = null
            for (i in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(i)
                if (ep.type != UsbConstants.USB_ENDPOINT_XFER_BULK) continue
                if (ep.direction == UsbConstants.USB_DIR_IN) epIn = ep else epOut = ep
            }
            val inEp = epIn ?: throw IOException("Endpoint bulk IN tidak ditemukan")
            val outEp = epOut ?: throw IOException("Endpoint bulk OUT tidak ditemukan")

            if (!manager.hasPermission(device)) {
                throw IOException("Izin USB untuk ${device.deviceName} belum diberikan")
            }

            val conn = manager.openDevice(device)
                ?: throw IOException("Gagal membuka perangkat USB ${device.deviceName}")

            if (!conn.claimInterface(iface, true)) {
                conn.close()
                throw IOException("Gagal klaim interface ADB (mungkin sedang dipakai adb host)")
            }

            return UsbTransport(conn, iface, inEp, outEp)
        }
    }
}
