package com.example.podlingo.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Mp3FrameScannerTest {

    // MPEG1 Layer III, 128kbps, 44100Hz, no padding, no CRC -> 417-byte frames.
    private val frameHeader = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x00)
    private fun frame(): ByteArray = frameHeader + ByteArray(413)

    @Test
    fun `scans consecutive frames and computes their offsets and duration`() {
        val bytes = frame() + frame() + frame()

        val frames = Mp3FrameScanner.scan(bytes)

        assertEquals(3, frames.size)
        assertEquals(417, frames[0].length)
        assertEquals(0, frames[0].offset)
        assertEquals(417, frames[1].offset)
        assertEquals(834, frames[2].offset)
        assertEquals(1152.0 / 44100.0, frames[0].durationSec, 1e-9)
    }

    @Test
    fun `skips a leading ID3v2 tag before scanning for frames`() {
        // "ID3", version 3.0, flags 0, syncsafe size = 10 (10 header bytes + 10 payload bytes).
        val id3 = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 3, 0, 0, 0, 0, 0, 10) +
            ByteArray(10)
        val bytes = id3 + frame()

        val frames = Mp3FrameScanner.scan(bytes)

        assertEquals(1, frames.size)
        assertEquals(id3.size, frames[0].offset)
    }

    @Test
    fun `ignores garbage bytes before the first valid sync`() {
        val bytes = byteArrayOf(1, 2, 3) + frame()

        val frames = Mp3FrameScanner.scan(bytes)

        assertEquals(1, frames.size)
        assertEquals(3, frames[0].offset)
    }

    @Test
    fun `non-mp3 bytes yield no frames`() {
        assertTrue(Mp3FrameScanner.scan(ByteArray(20)).isEmpty())
        assertTrue(Mp3FrameScanner.scan(ByteArray(0)).isEmpty())
    }
}
