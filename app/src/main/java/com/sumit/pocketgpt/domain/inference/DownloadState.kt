package com.sumit.pocketgpt.domain.inference

sealed interface DownloadState {
    data object NotStarted : DownloadState
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : DownloadState
    data object Completed : DownloadState
    data class Error(val cause: Throwable) : DownloadState
}
