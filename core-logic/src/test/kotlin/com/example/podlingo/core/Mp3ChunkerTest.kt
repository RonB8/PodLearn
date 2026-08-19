package com.example.podlingo.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Mp3ChunkerTest {

    private val frameDurationSec = 1152.0 / 44100.0
    private fun frames(count: Int, length: Int = 417) =
        (0 until count).map { Mp3FrameScanner.Frame(offset = it * length, length = length, durationSec = frameDurationSec) }

    @Test
    fun `packs whole frames into chunks under the byte cap`() {
        // 4 frames fit (4*417=1668), a 5th would exceed 2000.
        val chunks = Mp3Chunker.planChunks(frames(10), maxChunkBytes = 2000)

        assertEquals(3, chunks.size) // 4 + 4 + 2 frames
        assertEquals(0, chunks[0].startByteOffset)
        assertEquals(4 * 417, chunks[0].byteLength)
        assertEquals(4 * 417, chunks[1].startByteOffset)
        assertEquals(8 * 417, chunks[2].startByteOffset)
        assertEquals(2 * 417, chunks[2].byteLength)
    }

    @Test
    fun `each chunk's start time is the sum of prior frame durations`() {
        val chunks = Mp3Chunker.planChunks(frames(8), maxChunkBytes = 2000) // 4 frames per chunk

        assertEquals(0.0, chunks[0].startTimeSec, 1e-9)
        assertEquals(4 * frameDurationSec, chunks[1].startTimeSec, 1e-9)
    }

    @Test
    fun `a single frame larger than the cap still becomes its own chunk`() {
        val oversizedFrame = listOf(Mp3FrameScanner.Frame(offset = 0, length = 5000, durationSec = frameDurationSec))

        val chunks = Mp3Chunker.planChunks(oversizedFrame, maxChunkBytes = 2000)

        assertEquals(1, chunks.size)
        assertEquals(5000, chunks[0].byteLength)
    }

    @Test
    fun `an empty frame list yields no chunks`() {
        assertTrue(Mp3Chunker.planChunks(emptyList(), maxChunkBytes = 2000).isEmpty())
    }

    @Test
    fun `all frames fitting under the cap yields a single chunk`() {
        val chunks = Mp3Chunker.planChunks(frames(3), maxChunkBytes = 1_000_000)

        assertEquals(1, chunks.size)
        assertEquals(3 * 417, chunks[0].byteLength)
    }
}
