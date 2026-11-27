package com.rookhq.monarch.components.identity.recoverycodes

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Download: the system file picker (`CreateDocument`, Storage Access
 * Framework) suggests [fileName], then the codes are written to the chosen
 * file as UTF-8 text. Success is announced; a failure is shown under the
 * button and announced. Closing the picker does nothing.
 */
@Composable
internal fun RecoveryCodesDownload(
    text: String,
    fileName: String,
    enabled: Boolean,
    labels: MonarchRecoveryCodesLabels,
    onAnnounce: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val content by rememberUpdatedState(text)
    val announce by rememberUpdatedState(onAnnounce)
    val downloaded = monarchString(labels.downloaded, R.string.monarch_recoverycodes_downloaded)
    val failedText = monarchString(labels.downloadFailed, R.string.monarch_recoverycodes_download_failed)
    var failed by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(MonarchRecoveryCodesTokens.FILE_TYPE),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val error = writeRecoveryCodes(context, uri, content)
            failed = error != null
            announce(if (error == null) downloaded else failedText)
        }
    }
    Column {
        MonarchButton(
            text = monarchString(labels.download, R.string.monarch_recoverycodes_download),
            onClick = {
                failed = false
                launcher.launch(fileName)
            },
            size = MonarchButtonSize.Sm,
            leadingIcon = MonarchDefaultIcons.Download,
            enabled = enabled,
        )
        if (failed) {
            Text(text = failedText, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.dangerStrong)
        }
    }
}

/** Writes [text] to [uri]; returns the failure, or `null`. A cancellation is passed on. */
internal suspend fun writeRecoveryCodes(context: Context, uri: Uri, text: String): Throwable? {
    val outcome = runCatching {
        withContext(Dispatchers.IO) {
            val stream = checkNotNull(context.contentResolver.openOutputStream(uri)) { "No stream for $uri" }
            stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }
    }
    (outcome.exceptionOrNull() as? CancellationException)?.let { throw it }
    return outcome.exceptionOrNull()
}
