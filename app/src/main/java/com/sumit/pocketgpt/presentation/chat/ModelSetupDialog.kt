package com.sumit.pocketgpt.presentation.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sumit.pocketgpt.domain.inference.DownloadState
import com.sumit.pocketgpt.domain.inference.ModelState
import com.sumit.pocketgpt.ui.theme.PocketGPTTheme

/**
 * Blocks the chat screen while the model is downloading or loading, so a
 * disabled send button doesn't read as broken. No dismiss action — setup is
 * a precondition for using the screen, not something to skip past.
 */
@Composable
internal fun ModelSetupDialog(
    modelState: ModelState,
    downloadState: DownloadState,
    onRetry: () -> Unit,
) {
    val isError = modelState is ModelState.Error || downloadState is DownloadState.Error
    val message = when {
        modelState is ModelState.Error ->
            "Setup failed: ${modelState.cause.message ?: "unknown error"}"
        downloadState is DownloadState.Error ->
            "Download failed: ${downloadState.cause.message ?: "unknown error"}"
        downloadState is DownloadState.Downloading && downloadState.totalBytes > 0 ->
            "Downloading model… ${downloadProgress(downloadState)}%"
        downloadState is DownloadState.Downloading ->
            "Downloading model…"
        else ->
            "Setting up model…"
    }
    val determinateProgress =
        (downloadState as? DownloadState.Downloading)
            ?.takeIf { it.totalBytes > 0 }
            ?.let { it.bytesDownloaded.toFloat() / it.totalBytes }

    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            if (isError) {
                TextButton(onClick = onRetry) { Text("Retry") }
            }
        },
        title = { Text("Setting up PocketGPT") },
        text = {
            Column {
                Text(message)
                if (!isError) {
                    Spacer(Modifier.height(12.dp))
                    if (determinateProgress != null) {
                        LinearProgressIndicator(
                            progress = { determinateProgress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
    )
}

private fun downloadProgress(state: DownloadState.Downloading): Int =
    (state.bytesDownloaded * 100 / state.totalBytes).toInt()

@Preview(showBackground = true)
@Composable
private fun ModelSetupDialogDownloadingPreview() {
    PocketGPTTheme {
        ModelSetupDialog(
            modelState = ModelState.Unloaded,
            downloadState = DownloadState.Downloading(bytesDownloaded = 512_000_000L, totalBytes = 2_588_147_712L),
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ModelSetupDialogErrorPreview() {
    PocketGPTTheme {
        ModelSetupDialog(
            modelState = ModelState.Unloaded,
            downloadState = DownloadState.Error(RuntimeException("Network unreachable")),
            onRetry = {},
        )
    }
}
