package com.example.podlingo.data.remote.whisper

import com.example.podlingo.data.local.Mp3ChunkSplitter
import java.io.File
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhisperChunkedTranscriberTest {

    // MPEG1 Layer III, 128kbps, 44100Hz, no padding, no CRC -> 417-byte frames.
    private val frame = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x00) + ByteArray(413)
    private val frameDurationSec = 1152.0 / 44100.0

    @Test
    fun `file within the limit is sent as a single request`() = runTest {
        var callCount = 0
        val api = FakeWhisperApi { callCount++; WhisperTranscriptionResponse(text = "hi") }
        val file = File.createTempFile("episode", ".mp3").apply { writeBytes(frame) }
        val transcriber = WhisperChunkedTranscriber(api, Mp3ChunkSplitter())
        val progress = mutableListOf<Pair<Int, Int>>()

        val result = transcriber.transcribe(file, maxRequestBytes = 1_000_000) { i, c -> progress += i to c }

        assertEquals(1, callCount)
        assertEquals("hi", result.text)
        assertEquals(listOf(1 to 1), progress)
        file.delete()
    }

    @Test
    fun `oversized file is split into chunks with time-offset timestamps merged in order`() = runTest {
        val bytes = frame + frame + frame + frame // 4 frames
        val file = File.createTempFile("episode", ".mp3").apply { writeBytes(bytes) }
        var callIndex = 0
        val api = FakeWhisperApi {
            callIndex++
            WhisperTranscriptionResponse(
                text = "part$callIndex ",
                segments = listOf(WhisperSegment(start = 0.0, end = 0.5, text = "part$callIndex")),
                words = listOf(WhisperWord("part$callIndex", 0.0, 0.5)),
            )
        }
        val transcriber = WhisperChunkedTranscriber(api, Mp3ChunkSplitter())
        val progress = mutableListOf<Pair<Int, Int>>()

        // Cap of 2 frames per chunk -> 2 chunks.
        val result = transcriber.transcribe(file, maxRequestBytes = 417L * 2) { i, c -> progress += i to c }

        assertEquals(2, callIndex)
        assertEquals(listOf(1 to 2, 2 to 2), progress)
        assertEquals("part1 part2 ", result.text)
        assertEquals(listOf(0.0, 2 * frameDurationSec), result.segments.map { it.start })
        assertEquals(listOf(0.0, 2 * frameDurationSec), result.words.map { it.start })
        file.delete()
    }

    @Test
    fun `an unsplittable oversized file surfaces a clear error`() = runTest {
        val api = FakeWhisperApi { WhisperTranscriptionResponse() }
        // Not a valid MP3 frame structure - the splitter can't find a boundary to cut on.
        val file = File.createTempFile("episode", ".mp3").apply { writeBytes(ByteArray(10)) }
        val transcriber = WhisperChunkedTranscriber(api, Mp3ChunkSplitter())

        val error = runCatching { transcriber.transcribe(file, maxRequestBytes = 1) { _, _ -> } }.exceptionOrNull()

        assertTrue(error is IOException)
        file.delete()
    }

    private class FakeWhisperApi(private val respond: () -> WhisperTranscriptionResponse) : WhisperApi {
        override suspend fun transcribe(
            file: MultipartBody.Part,
            model: RequestBody,
            responseFormat: RequestBody,
            wordGranularity: RequestBody,
            segmentGranularity: RequestBody,
        ): WhisperTranscriptionResponse = respond()
    }
}
