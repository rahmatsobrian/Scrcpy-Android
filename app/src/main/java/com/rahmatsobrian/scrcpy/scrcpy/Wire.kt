package com.rahmatsobrian.scrcpy.scrcpy

import okio.BufferedSource
import okio.ByteString
import okio.buffer
import java.io.EOFException
import java.io.IOException

/**
 * Pembaca wire-protocol scrcpy 4.1. Semua integer multi-byte di stream adalah
 * big-endian (lihat `doc/develop.md` dan `device/Streamer.java`).
 */
object Wire {
    const val PACKET_HEADER_SIZE = 12
    const val DEVICE_NAME_FIELD_LENGTH = 64

    const val PACKET_FLAG_SESSION = 1L shl 63
    const val PACKET_FLAG_CONFIG = 1L shl 62
    const val PACKET_FLAG_KEY_FRAME = 1L shl 61
    const val PTS_MASK = (1L shl 61) - 1

    /** codec id 0 = stream dimatikan server, 1 = konfigurasi gagal. */
    const val CODEC_ID_DISABLED = 0
    const val CODEC_ID_ERROR = 1

    /** Baca tepat [n] byte; lempar [EOFException] bila stream terputus. */
    fun BufferedSource.readExactly(n: Int, what: String): ByteArray {
        val bytes = try {
            readByteArray(n.toLong())
        } catch (e: EOFException) {
            throw EOFException("Stream berakhir saat membaca $what (${e.message})")
        }
        if (bytes.size != n) throw EOFException("Short read $what: ${bytes.size}/$n")
        return bytes
    }

    fun BufferedSource.readIntBe(what: String): Int =
        readExactly(4, what).let { ((it[0].toInt() and 0xFF) shl 24) or
            ((it[1].toInt() and 0xFF) shl 16) or
            ((it[2].toInt() and 0xFF) shl 8) or
            (it[3].toInt() and 0xFF) }

    fun BufferedSource.readLongBe(what: String): Long =
        readExactly(8, what).let {
            var v = 0L
            for (b in it) v = (v shl 8) or (b.toLong() and 0xFF)
            v
        }

    /** 4 byte codec id (`"h264"`, `"opus"`, …). */
    fun BufferedSource.readCodecId(): Int = readIntBe("codec id")

    /** 12 byte header paket (session / frame). */
    fun BufferedSource.readPacketHeader(): ByteArray = readExactly(PACKET_HEADER_SIZE, "packet header")

    /** 64 byte nama device, di-NUL-terminate di byte terakhir. */
    fun BufferedSource.readDeviceName(): String {
        val raw = readExactly(DEVICE_NAME_FIELD_LENGTH, "device meta")
        raw[raw.size - 1] = 0
        val end = raw.indexOf(0)
        return String(raw, 0, if (end >= 0) end else raw.size, Charsets.UTF_8)
    }
}

/** Hasil parse 12 byte header paket media. */
data class PacketHeader(
    val isSession: Boolean,
    val isConfig: Boolean,
    val isKeyFrame: Boolean,
    val ptsUs: Long,
    val size: Int,
    val width: Int,
    val height: Int,
    val clientResized: Boolean,
) {
    companion object {
        fun parse(header: ByteArray): PacketHeader {
            require(header.size == Wire.PACKET_HEADER_SIZE) { "header harus 12 byte" }
            var ptsFlags = 0L
            for (i in 0..7) ptsFlags = (ptsFlags shl 8) or (header[i].toLong() and 0xFF)

            val isSession = (ptsFlags and Wire.PACKET_FLAG_SESSION) != 0L
            if (isSession) {
                return PacketHeader(
                    isSession = true,
                    isConfig = false,
                    isKeyFrame = false,
                    ptsUs = 0,
                    size = Wire.PACKET_HEADER_SIZE,
                    width = readIntBe(header, 4),
                    height = readIntBe(header, 8),
                    clientResized = (header[3].toInt() and 1) != 0,
                )
            }

            val isConfig = (ptsFlags and Wire.PACKET_FLAG_CONFIG) != 0L
            val isKey = (ptsFlags and Wire.PACKET_FLAG_KEY_FRAME) != 0L
            val size = readIntBe(header, 8)
            if (size <= 0) throw IOException("Panjang paket tidak valid: $size")
            return PacketHeader(
                isSession = false,
                isConfig = isConfig,
                isKeyFrame = isKey,
                ptsUs = if (isConfig) 0 else ptsFlags and Wire.PTS_MASK,
                size = size,
                width = 0,
                height = 0,
                clientResized = false,
            )
        }

        private fun readIntBe(b: ByteArray, off: Int): Int =
            ((b[off].toInt() and 0xFF) shl 24) or
                ((b[off + 1].toInt() and 0xFF) shl 16) or
                ((b[off + 2].toInt() and 0xFF) shl 8) or
                (b[off + 3].toInt() and 0xFF)
    }
}

/** Satu paket media siap dikonsumsi (video/audio). */
data class MediaPacket(
    val header: PacketHeader,
    val payload: ByteString,
)
