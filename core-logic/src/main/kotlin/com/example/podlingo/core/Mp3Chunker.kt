package com.example.podlingo.core

/** Groups consecutive [Mp3FrameScanner.Frame]s into runs no larger than a byte cap. */
object Mp3Chunker {

    data class Chunk(val startByteOffset: Int, val byteLength: Int, val startTimeSec: Double)

    /**
     * Greedily packs whole frames into each chunk, never splitting a frame, so every chunk stays
     * a valid, independently-decodable MP3. A single frame larger than [maxChunkBytes] still
     * becomes its own (oversized) chunk rather than being dropped or split mid-frame.
     */
    fun planChunks(frames: List<Mp3FrameScanner.Frame>, maxChunkBytes: Long): List<Chunk> {
        if (frames.isEmpty()) return emptyList()

        val chunks = mutableListOf<Chunk>()
        var chunkStartIndex = 0
        var chunkBytes = 0L
        var chunkStartTime = 0.0
        var cumulativeTime = 0.0

        for ((index, frame) in frames.withIndex()) {
            if (index > chunkStartIndex && chunkBytes + frame.length > maxChunkBytes) {
                val start = frames[chunkStartIndex]
                chunks += Chunk(start.offset, frame.offset - start.offset, chunkStartTime)
                chunkStartIndex = index
                chunkBytes = 0L
                chunkStartTime = cumulativeTime
            }
            chunkBytes += frame.length
            cumulativeTime += frame.durationSec
        }

        val start = frames[chunkStartIndex]
        val last = frames.last()
        chunks += Chunk(start.offset, last.offset + last.length - start.offset, chunkStartTime)
        return chunks
    }
}
