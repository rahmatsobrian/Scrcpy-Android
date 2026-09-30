package com.rahmatsobrian.scrcpy.scrcpy

import okio.Buffer
import okio.BufferedSource
import java.io.IOException

/**
 * Serialisasi control message (client → server) dan deserialisasi device message
 * (server → client) untuk scrcpy 4.1.
 *
 * Sumber: `app/src/control_msg.c`, `app/src/device_msg.c`,
 * `server/.../control/ControlMessageReader.java`, `DeviceMessageWriter.java`.
 *
 * Semua integer multi-byte adalah big-endian; `Buffer.writeInt/Long/Short`
 * okio memang menulis big-endian (network order), jadi tidak perlu helper khusus.
 */
object ControlProtocol {
    const val TYPE_INJECT_KEYCODE = 0
    const val TYPE_INJECT_TEXT = 1
    const val TYPE_INJECT_TOUCH_EVENT = 2
    const val TYPE_INJECT_SCROLL_EVENT = 3
    const val TYPE_BACK_OR_SCREEN_ON = 4
    const val TYPE_EXPAND_NOTIFICATION_PANEL = 5
    const val TYPE_EXPAND_SETTINGS_PANEL = 6
    const val TYPE_COLLAPSE_PANELS = 7
    const val TYPE_GET_CLIPBOARD = 8
    const val TYPE_SET_CLIPBOARD = 9
    const val TYPE_SET_DISPLAY_POWER = 10
    const val TYPE_ROTATE_DEVICE = 11
    const val TYPE_UHID_CREATE = 12
    const val TYPE_UHID_INPUT = 13
    const val TYPE_UHID_DESTROY = 14
    const val TYPE_OPEN_HARD_KEYBOARD_SETTINGS = 15
    const val TYPE_START_APP = 16
    const val TYPE_RESET_VIDEO = 17
    const val TYPE_CAMERA_SET_TORCH = 18
    const val TYPE_CAMERA_ZOOM_IN = 19
    const val TYPE_CAMERA_ZOOM_OUT = 20
    const val TYPE_RESIZE_DISPLAY = 21
    const val TYPE_SCAN_FILE = 22

    const val MAX_CLIPBOARD = 262130
    const val MAX_INJECT_TEXT = 300
    const val MAX_SCAN_PATH = 256

    const val POINTER_ID_MOUSE = -1L
    const val POINTER_ID_FINGER = -2L
    const val POINTER_ID_VIRTUAL_FINGER = -3L

    const val SCROLL_SCALE = 16
    const val SCROLL_MAX = 32767

    const val KEY_ACTION_DOWN = 0
    const val KEY_ACTION_UP = 1
    const val KEY_ACTION_MULTIPLE = 2

    fun keycode(action: Int, keycode: Int, repeat: Int, metastate: Int): ByteArray = Buffer()
        .writeByte(TYPE_INJECT_KEYCODE)
        .writeByte(action)
        .writeInt(keycode)
        .writeInt(repeat)
        .writeInt(metastate)
        .readByteArray()

    fun text(value: String): ByteArray {
        val bytes = value.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_INJECT_TEXT) { "Teks terlalu panjang: ${bytes.size}" }
        return Buffer()
            .writeByte(TYPE_INJECT_TEXT)
            .writeInt(bytes.size)
            .write(bytes)
            .readByteArray()
    }

    fun touch(
        action: Int,
        pointerId: Long,
        x: Int,
        y: Int,
        screenWidth: Int,
        screenHeight: Int,
        pressure: Float,
        actionButton: Int,
        buttons: Int,
    ): ByteArray = Buffer()
        .writeByte(TYPE_INJECT_TOUCH_EVENT)
        .writeByte(action)
        .writeLong(pointerId)
        .writeInt(x)
        .writeInt(y)
        .writeShort(screenWidth)
        .writeShort(screenHeight)
        .writeShort(encodePressure(pressure))
        .writeInt(actionButton)
        .writeInt(buttons)
        .readByteArray()

    /** tekanan ∈ [0,1] → u16 fixed point; `0xFFFF` ≈ 1.0. */
    fun encodePressure(pressure: Float): Int {
        val scaled = (pressure.coerceIn(0f, 1f) * 65536f).toLong()
        return (scaled.coerceAtMost(0xFFFFL)).toInt()
    }

    fun scroll(
        x: Int,
        y: Int,
        screenWidth: Int,
        screenHeight: Int,
        hScroll: Float,
        vScroll: Float,
        buttons: Int,
    ): ByteArray = Buffer()
        .writeByte(TYPE_INJECT_SCROLL_EVENT)
        .writeInt(x)
        .writeInt(y)
        .writeShort(screenWidth)
        .writeShort(screenHeight)
        .writeShort(encodeScroll(hScroll))
        .writeShort(encodeScroll(vScroll))
        .writeInt(buttons)
        .readByteArray()

    /** nilai ∈ [-16,16] → i16 big-endian `sc_float_to_i16fp()` (`0x7FFF` = +16). */
    fun encodeScroll(value: Float): Int {
        val normalized = (value / SCROLL_SCALE).coerceIn(-1f, 1f)
        val fixed = (normalized * 32768f).toInt()
        return fixed.coerceIn(-0x8000, 0x7FFF)
    }

    fun backOrScreenOn(action: Int): ByteArray =
        byteArrayOf(TYPE_BACK_OR_SCREEN_ON.toByte(), action.toByte())

    fun expandNotificationPanel(): ByteArray = byteArrayOf(TYPE_EXPAND_NOTIFICATION_PANEL.toByte())
    fun expandSettingsPanel(): ByteArray = byteArrayOf(TYPE_EXPAND_SETTINGS_PANEL.toByte())
    fun collapsePanels(): ByteArray = byteArrayOf(TYPE_COLLAPSE_PANELS.toByte())

    fun getClipboard(copyKey: Int): ByteArray =
        byteArrayOf(TYPE_GET_CLIPBOARD.toByte(), copyKey.toByte())

    fun setClipboard(sequence: Long, paste: Boolean, text: String): ByteArray {
        val bytes = text.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_CLIPBOARD) { "Klipbord terlalu panjang: ${bytes.size}" }
        return Buffer()
            .writeByte(TYPE_SET_CLIPBOARD)
            .writeLong(sequence)
            .writeByte(if (paste) 1 else 0)
            .writeInt(bytes.size)
            .write(bytes)
            .readByteArray()
    }

    fun setDisplayPower(on: Boolean): ByteArray =
        byteArrayOf(TYPE_SET_DISPLAY_POWER.toByte(), if (on) 1 else 0)

    fun rotateDevice(): ByteArray = byteArrayOf(TYPE_ROTATE_DEVICE.toByte())
    fun resetVideo(): ByteArray = byteArrayOf(TYPE_RESET_VIDEO.toByte())
    fun openHardKeyboardSettings(): ByteArray = byteArrayOf(TYPE_OPEN_HARD_KEYBOARD_SETTINGS.toByte())

    fun startApp(packageName: String, forceStop: Boolean = false): ByteArray {
        val prefix = if (forceStop) "+" else ""
        val raw = (prefix + packageName).toByteArray(Charsets.UTF_8)
        require(raw.size <= 255) { "Nama paket terlalu panjang" }
        return Buffer()
            .writeByte(TYPE_START_APP)
            .writeByte(raw.size)
            .write(raw)
            .readByteArray()
    }

    fun cameraTorch(on: Boolean): ByteArray =
        byteArrayOf(TYPE_CAMERA_SET_TORCH.toByte(), if (on) 1 else 0)

    fun cameraZoomIn(): ByteArray = byteArrayOf(TYPE_CAMERA_ZOOM_IN.toByte())
    fun cameraZoomOut(): ByteArray = byteArrayOf(TYPE_CAMERA_ZOOM_OUT.toByte())

    fun resizeDisplay(width: Int, height: Int): ByteArray = Buffer()
        .writeByte(TYPE_RESIZE_DISPLAY)
        .writeShort(width)
        .writeShort(height)
        .readByteArray()

    fun scanFile(path: String): ByteArray {
        val bytes = path.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_SCAN_PATH) { "Path terlalu panjang" }
        return Buffer()
            .writeByte(TYPE_SCAN_FILE)
            .writeInt(bytes.size)
            .write(bytes)
            .readByteArray()
    }
}

/** Pesan yang datang dari server (`DeviceMessage.java`). */
sealed interface DeviceMessage {
    data class Clipboard(val text: String) : DeviceMessage
    data class AckClipboard(val sequence: Long) : DeviceMessage
    data class UhidOutput(val id: Int, val data: ByteArray) : DeviceMessage
}

/**
 * Reader pesan dari server dengan buffering 256 KiB sliding window.
 * Tipe 1 byte pertama menentukan sisa panjang (self-delimiting),
 * sesuai `app/src/device_msg.c`.
 */
class DeviceMessageReader {
    private val buffer = Buffer()

    fun feed(data: ByteArray, count: Int = data.size) {
        buffer.write(data, 0, count)
    }

    /** Ambil satu pesan bila sudah lengkap; `null` bila byte belum cukup. */
    @Throws(IOException::class)
    fun next(): DeviceMessage? {
        if (buffer.size < 1) return null
        return when (buffer[0].toInt() and 0xFF) {
            0 -> readClipboard()
            1 -> readAckClipboard()
            2 -> readUhidOutput()
            else -> throw IOException("Tipe device message tidak dikenal: ${buffer[0]}")
        }
    }

    private fun peekIntBe(index: Long): Int =
        ((buffer[index].toInt() and 0xFF) shl 24) or
            ((buffer[index + 1].toInt() and 0xFF) shl 16) or
            ((buffer[index + 2].toInt() and 0xFF) shl 8) or
            (buffer[index + 3].toInt() and 0xFF)

    private fun peekShortBe(index: Long): Int =
        ((buffer[index].toInt() and 0xFF) shl 8) or
            (buffer[index + 1].toInt() and 0xFF)

    private fun readClipboard(): DeviceMessage? {
        if (buffer.size < 5) return null
        val len = peekIntBe(1)
        if (len < 0 || len > 262139) throw IOException("Panjang clipboard tidak valid: $len")
        if (buffer.size < 5L + len) return null
        buffer.skip(5)
        val bytes = buffer.readByteArray(len.toLong())
        return DeviceMessage.Clipboard(bytes.toString(Charsets.UTF_8))
    }

    private fun readAckClipboard(): DeviceMessage? {
        if (buffer.size < 9) return null
        buffer.skip(1)
        return DeviceMessage.AckClipboard(buffer.readLong())
    }

    private fun readUhidOutput(): DeviceMessage? {
        if (buffer.size < 5) return null
        val id = peekShortBe(1)
        val size = peekShortBe(3)
        if (buffer.size < 5L + size) return null
        buffer.skip(5)
        return DeviceMessage.UhidOutput(id, buffer.readByteArray(size.toLong()))
    }
}
