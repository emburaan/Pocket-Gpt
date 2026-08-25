package com.sumit.pocketgpt.data.inference

import com.sumit.pocketgpt.di.ModelPath
import com.sumit.pocketgpt.di.ModelUrl
import com.sumit.pocketgpt.domain.inference.DownloadState
import com.sumit.pocketgpt.domain.inference.ModelDownloader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloaderImpl @Inject constructor(
    @ModelUrl private val modelUrl: String,
    @ModelPath private val modelPath: String,
) : ModelDownloader {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.NotStarted)
    override val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    override suspend fun ensureModelDownloaded() {
        val destination = File(modelPath)
        if (destination.exists()) {
            _downloadState.value = DownloadState.Completed
            return
        }

        withContext(Dispatchers.IO) {
            destination.parentFile?.mkdirs()
            val partial = File(destination.parentFile, "${destination.name}.part")
            // No resume support: a leftover .part is from a prior failed or
            // cancelled attempt and can't be trusted (was the connection cut
            // by us or the server? mid-write?), so it's discarded rather than
            // resumed from — the next attempt always starts at byte zero.
            partial.delete()

            var connection: HttpURLConnection? = null
            try {
                connection = (URL(modelUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                }
                val totalBytes = connection.contentLengthLong
                _downloadState.value = DownloadState.Downloading(0L, totalBytes)

                connection.inputStream.use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var bytesDownloaded = 0L
                        var lastReportedBytes = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            bytesDownloaded += read
                            if (bytesDownloaded - lastReportedBytes >= PROGRESS_REPORT_STEP_BYTES) {
                                _downloadState.value =
                                    DownloadState.Downloading(bytesDownloaded, totalBytes)
                                lastReportedBytes = bytesDownloaded
                            }
                        }
                        _downloadState.value = DownloadState.Downloading(bytesDownloaded, totalBytes)
                    }
                }

                check(partial.renameTo(destination)) {
                    "failed to move downloaded model into place at $modelPath"
                }
                _downloadState.value = DownloadState.Completed
            } catch (e: CancellationException) {
                partial.delete()
                _downloadState.value = DownloadState.NotStarted
                throw e
            } catch (e: Exception) {
                partial.delete()
                _downloadState.value = DownloadState.Error(e)
                throw e
            } finally {
                connection?.disconnect()
            }
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 15_000
        const val PROGRESS_REPORT_STEP_BYTES = 1L shl 20 // 1 MiB
    }
}
