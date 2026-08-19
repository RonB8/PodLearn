package com.example.podlingo.data.local

import com.example.podlingo.core.Mp3Chunker
import com.example.podlingo.core.Mp3FrameScanner
import java.io.File
import javax.inject.Inject

/**
 * Splits a downloaded mp3 into playable sub-files no larger than [maxChunkBytes], cutting only on
 * MPEG frame boundaries (no re-encoding) so each piece stays independently decodable. If the
 * source's frame structure can't be parsed at all, returns the source file unsplit as the sole
 * chunk - the caller decides how to treat that. Returned chunk files other than the source are
 * temporary; deleting them once done is the caller's responsibility.
 */
class Mp3ChunkSplitter @Inject constructor() {

    data class Chunk(val file: File, val startTimeSec: Double)

    fun split(sourceFile: File, maxChunkBytes: Long): List<Chunk> {
        val bytes = sourceFile.readBytes()
        val frames = Mp3FrameScanner.scan(bytes)
        val plan = Mp3Chunker.planChunks(frames, maxChunkBytes)
        if (plan.size <= 1) return listOf(Chunk(sourceFile, 0.0))

        return plan.mapIndexed { index, chunk ->
            val chunkFile = File(sourceFile.parentFile, "${sourceFile.nameWithoutExtension}.part$index.mp3")
            chunkFile.outputStream().use { it.write(bytes, chunk.startByteOffset, chunk.byteLength) }
            Chunk(chunkFile, chunk.startTimeSec)
        }
    }
}
