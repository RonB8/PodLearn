package com.example.podlingo.core

/**
 * Locates MPEG audio frame boundaries directly from raw bytes, without decoding. A frame is the
 * smallest independently-decodable unit of an MP3 stream, so cutting a file exactly on frame
 * boundaries (see [Mp3Chunker]) yields sub-files that are still valid, playable MP3s - no
 * re-encoding needed to split a large episode for upload.
 */
object Mp3FrameScanner {

    data class Frame(val offset: Int, val length: Int, val durationSec: Double)

    /** Scans [bytes] for consecutive valid frames, skipping any leading ID3v2 tag. */
    fun scan(bytes: ByteArray): List<Frame> {
        val frames = mutableListOf<Frame>()
        var pos = leadingId3v2TagSize(bytes)
        while (pos + 4 <= bytes.size) {
            val header = parseHeader(bytes, pos)
            if (header == null) {
                pos++
                continue
            }
            if (pos + header.length > bytes.size) break
            frames += Frame(pos, header.length, header.durationSec)
            pos += header.length
        }
        return frames
    }

    private fun leadingId3v2TagSize(bytes: ByteArray): Int {
        if (bytes.size < 10 ||
            bytes[0] != 'I'.code.toByte() ||
            bytes[1] != 'D'.code.toByte() ||
            bytes[2] != '3'.code.toByte()
        ) {
            return 0
        }
        // ID3v2 tag size is a 4-byte syncsafe integer (7 usable bits per byte) at offset 6.
        val size = ((bytes[6].toInt() and 0x7F) shl 21) or
            ((bytes[7].toInt() and 0x7F) shl 14) or
            ((bytes[8].toInt() and 0x7F) shl 7) or
            (bytes[9].toInt() and 0x7F)
        return 10 + size
    }

    private class Header(val length: Int, val durationSec: Double)

    private fun parseHeader(bytes: ByteArray, pos: Int): Header? {
        val b0 = bytes[pos].toInt() and 0xFF
        val b1 = bytes[pos + 1].toInt() and 0xFF
        val b2 = bytes[pos + 2].toInt() and 0xFF

        if (b0 != 0xFF || (b1 and 0xE0) != 0xE0) return null

        val versionBits = (b1 shr 3) and 0x03
        val layerBits = (b1 shr 1) and 0x03
        if (versionBits == 1 || layerBits == 0) return null // reserved

        val bitrateIndex = (b2 shr 4) and 0x0F
        val sampleRateIndex = (b2 shr 2) and 0x03
        val padding = (b2 shr 1) and 0x01
        if (bitrateIndex == 0 || bitrateIndex == 15 || sampleRateIndex == 3) return null // free/bad/reserved

        val version = when (versionBits) {
            3 -> Version.V1
            2 -> Version.V2
            else -> Version.V2_5
        }
        val layer = when (layerBits) {
            3 -> Layer.I
            2 -> Layer.II
            else -> Layer.III
        }

        val bitrateKbps = bitrateTable(version)[layer]?.getOrNull(bitrateIndex) ?: return null
        val sampleRate = SAMPLE_RATE_TABLE[version]?.getOrNull(sampleRateIndex) ?: return null

        val samplesPerFrame = when (layer) {
            Layer.I -> 384
            Layer.II -> 1152
            Layer.III -> if (version == Version.V1) 1152 else 576
        }

        val length = when (layer) {
            Layer.I -> (12 * bitrateKbps * 1000 / sampleRate + padding) * 4
            else -> samplesPerFrame / 8 * bitrateKbps * 1000 / sampleRate + padding
        }
        if (length <= 0) return null

        return Header(length, samplesPerFrame.toDouble() / sampleRate)
    }

    private enum class Version { V1, V2, V2_5 }
    private enum class Layer { I, II, III }

    private fun bitrateTable(version: Version): Map<Layer, IntArray> =
        if (version == Version.V1) BITRATE_TABLE_V1 else BITRATE_TABLE_V2

    private val SAMPLE_RATE_TABLE: Map<Version, IntArray> = mapOf(
        Version.V1 to intArrayOf(44100, 48000, 32000),
        Version.V2 to intArrayOf(22050, 24000, 16000),
        Version.V2_5 to intArrayOf(11025, 12000, 8000),
    )

    // MPEG2 and MPEG2.5 share the same bitrate tables (ISO/IEC 11172-3 / 13818-3).
    private val BITRATE_TABLE_V1: Map<Layer, IntArray> = mapOf(
        Layer.I to intArrayOf(0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448),
        Layer.II to intArrayOf(0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384),
        Layer.III to intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320),
    )
    private val BITRATE_TABLE_V2: Map<Layer, IntArray> = mapOf(
        Layer.I to intArrayOf(0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256),
        Layer.II to intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160),
        Layer.III to intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160),
    )
}
