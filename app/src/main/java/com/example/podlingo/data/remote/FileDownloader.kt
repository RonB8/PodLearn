package com.example.podlingo.data.remote

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class FileDownloader @Inject constructor(private val okHttpClient: OkHttpClient) {

    /** Streams [url] to [destination], reporting progress via [onProgress] (bytes so far, total bytes or -1 if unknown). */
    suspend fun download(
        url: String,
        destination: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            destination.parentFile?.mkdirs()
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Download failed: HTTP ${response.code}"))
                }
                val body = response.body
                    ?: return@withContext Result.failure(IOException("Empty response body"))
                val total = body.contentLength()
                body.byteStream().use { input ->
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE_BYTES)
                        var downloaded = 0L
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            onProgress(downloaded, total)
                        }
                    }
                }
            }
            Result.success(destination)
        } catch (e: Exception) {
            destination.delete()
            Result.failure(e)
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE_BYTES = 8 * 1024
    }
}
