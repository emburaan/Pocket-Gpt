package com.sumit.pocketgpt.domain.inference

import kotlinx.coroutines.flow.StateFlow

/**
 * Fetches the on-device model checkpoint from its remote source into local
 * storage, so the app doesn't depend on the file being sideloaded via
 * `adb push`.
 */
interface ModelDownloader {
    val downloadState: StateFlow<DownloadState>

    /**
     * Downloads the model if it isn't already present at the destination
     * path. Safe to call repeatedly — once [DownloadState.Completed] is
     * reached, later calls are a no-op.
     *
     * There is no partial-content resume: a failed or cancelled download
     * deletes whatever bytes it had written, so the next call always starts
     * a fresh download rather than continuing a `.part` file.
     *
     * Thrown at call time:
     * - Whatever [java.io.IOException] the connection or transfer raised,
     *   after [downloadState] has been set to [DownloadState.Error].
     */
    suspend fun ensureModelDownloaded()
}
