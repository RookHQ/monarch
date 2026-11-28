package com.rookhq.monarch.components.datadisplay.downloadreportbutton

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgress
import com.rookhq.monarch.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Downloads a report in one or several formats, with progress while the file
 * is prepared, a note when it is prepared in the background, the system save
 * dialog for a ready file, and Try again on failure.
 *
 * Built from `MonarchButton`, a `MonarchMenu` for several formats,
 * `MonarchProgress`, `MonarchInlineErrorRetry`, and the system save dialog
 * through `ActivityResultContracts.CreateDocument`. Only one download runs at a
 * time. Preparing, saved and queued are announced through a polite live
 * region; a failure is announced by `MonarchInlineErrorRetry`.
 *
 * @param onDownload prepares the file in the chosen format. Return `File` to open the system save dialog,
 * `Downloaded` when the app saved or shared it, `Queued` when it is prepared in the background; throw on failure.
 * @param modifier the modifier of the button and its notes.
 * @param formats the formats offered. One shows a plain button "Download CSV"; several open a menu from "Download".
 * @param progress how far the preparation is, 0 to 1, while it runs, drawn as a bar under the button.
 * @param variant the button's visual weight.
 * @param size the button's height.
 * @param enabled whether the button can be used.
 * @param initiallyOpen whether the format menu is open at first (the docs preview shows it open, as the web).
 * @param icon a download icon before the text, the web's by default; `null` draws none.
 * @param menuIcon a chevron after the text when there are several formats.
 * @param labels replacements for the built-in strings.
 * @param errorMessage turns a thrown exception into the text shown. `null` shows "The CSV download failed."
 */
@Composable
public fun MonarchDownloadReportButton(
    onDownload: suspend (MonarchDownloadReportFormat) -> MonarchDownloadReportResult,
    modifier: Modifier = Modifier,
    formats: List<MonarchDownloadReportFormat> = listOf(MonarchDownloadReportFormat.Csv),
    progress: Float? = null,
    variant: MonarchButtonVariant = MonarchButtonVariant.Secondary,
    size: MonarchButtonSize = MonarchButtonSize.Sm,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.Download,
    menuIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronDown,
    labels: MonarchDownloadReportButtonLabels = MonarchDownloadReportButtonLabels(),
    errorMessage: ((Throwable, MonarchDownloadReportFormat) -> String)? = null,
    initiallyOpen: Boolean = false,
) {
    val tokens = MonarchDownloadReportButtonTokens
    val context = LocalContext.current
    val texts = DownloadReportTexts(context, labels)
    val queuedText = monarchString(labels.queued, R.string.monarch_downloadreportbutton_queued)
    val download = rememberDownloadReport(onDownload, texts, queuedText)
    val phase = download.phase
    val running = tokens.runningFormat(phase)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        Trigger(
            formats = formats,
            busy = running != null,
            busyLabel = running?.let(texts::preparing),
            variant = variant,
            size = size,
            enabled = enabled,
            icon = icon,
            menuIcon = menuIcon,
            labels = labels,
            initiallyOpen = initiallyOpen,
            onStart = download.start,
        )
        tokens.shownProgress(phase, progress)?.let {
            MonarchProgress(
                modifier = Modifier.fillMaxWidth(),
                progress = it,
                label = monarchString(labels.progress, R.string.monarch_downloadreportbutton_progress),
            )
        }
        if (phase is MonarchDownloadReportPhase.Queued) {
            Text(
                text = phase.message ?: queuedText,
                style = MonarchTheme.typography.caption,
                color = tokens.noteColor(MonarchTheme.colors),
            )
        }
        if (phase is MonarchDownloadReportPhase.Failed) {
            MonarchInlineErrorRetry(
                message = errorMessage?.invoke(phase.error, phase.format) ?: texts.error(phase.format),
                onRetry = { download.start(phase.format) },
            )
        }
        // Always present, so each new announcement is read as a change of this region.
        val announcement = download.announcement
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

@Composable
private fun Trigger(
    formats: List<MonarchDownloadReportFormat>,
    busy: Boolean,
    busyLabel: String?,
    variant: MonarchButtonVariant,
    size: MonarchButtonSize,
    enabled: Boolean,
    icon: (@Composable () -> Unit)?,
    menuIcon: (@Composable () -> Unit)?,
    labels: MonarchDownloadReportButtonLabels,
    initiallyOpen: Boolean,
    onStart: (MonarchDownloadReportFormat) -> Unit,
) {
    val context = LocalContext.current
    if (formats.size <= 1) {
        val format = formats.firstOrNull() ?: MonarchDownloadReportFormat.Csv
        MonarchButton(
            text = DownloadReportTexts(context, labels).single(format),
            onClick = { onStart(format) },
            variant = variant,
            size = size,
            enabled = enabled,
            loading = busy,
            leadingIcon = icon,
            loadingLabel = busyLabel,
        )
        return
    }
    var expanded by remember { mutableStateOf(initiallyOpen) }
    Box {
        MonarchButton(
            text = monarchString(labels.label, R.string.monarch_downloadreportbutton_label),
            onClick = { expanded = true },
            variant = variant,
            size = size,
            enabled = enabled,
            loading = busy,
            leadingIcon = icon,
            trailingIcon = menuIcon,
            loadingLabel = busyLabel,
        )
        MonarchMenu(expanded = expanded && !busy && enabled, onDismissRequest = { expanded = false }) {
            formats.forEach { format ->
                MonarchMenuItem(
                    onClick = {
                        expanded = false
                        onStart(format)
                    },
                ) {
                    Text(format.label)
                    format.description?.let {
                        Text(text = it, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
                    }
                }
            }
        }
    }
}

/** The format-dependent strings, read when a download changes state. */
private class DownloadReportTexts(val context: Context, val labels: MonarchDownloadReportButtonLabels) {
    fun single(format: MonarchDownloadReportFormat): String = labels.single?.invoke(format.label)
        ?: context.getString(R.string.monarch_downloadreportbutton_single, format.label)

    fun preparing(format: MonarchDownloadReportFormat): String = labels.preparing?.invoke(format.label)
        ?: context.getString(R.string.monarch_downloadreportbutton_preparing, format.label)

    fun downloaded(format: MonarchDownloadReportFormat): String = labels.downloaded?.invoke(format.label)
        ?: context.getString(R.string.monarch_downloadreportbutton_downloaded, format.label)

    fun error(format: MonarchDownloadReportFormat): String = labels.error?.invoke(format.label)
        ?: context.getString(R.string.monarch_downloadreportbutton_error, format.label)
}

/** The state of the download, what was last announced, and a way to start one. */
private class DownloadReportState(
    val phase: MonarchDownloadReportPhase,
    val announcement: String,
    val start: (MonarchDownloadReportFormat) -> Unit,
)

@Composable
private fun rememberDownloadReport(
    onDownload: suspend (MonarchDownloadReportFormat) -> MonarchDownloadReportResult,
    texts: DownloadReportTexts,
    queuedText: String,
): DownloadReportState {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val download by rememberUpdatedState(onDownload)
    val currentTexts by rememberUpdatedState(texts)
    val currentQueued by rememberUpdatedState(queuedText)
    var phase by remember { mutableStateOf<MonarchDownloadReportPhase>(MonarchDownloadReportPhase.Idle) }
    var announcement by remember { mutableStateOf("") }
    val contract = remember { MonarchDownloadReportCreateDocument() }
    val launcher = rememberLauncherForActivityResult(contract) { uri ->
        val saving = phase as? MonarchDownloadReportPhase.Saving ?: return@rememberLauncherForActivityResult
        if (uri == null) {
            // The person closed the save dialog: back to idle, nothing to say.
            phase = MonarchDownloadReportPhase.Idle
        } else {
            scope.writeReport(context, uri, saving) { error ->
                if (error == null) {
                    phase = MonarchDownloadReportPhase.Idle
                    announcement = currentTexts.downloaded(saving.format)
                } else {
                    phase = MonarchDownloadReportPhase.Failed(saving.format, error)
                }
            }
        }
    }
    return DownloadReportState(phase, announcement) { format ->
        if (!MonarchDownloadReportButtonTokens.busy(phase)) {
            phase = MonarchDownloadReportPhase.Preparing(format)
            announcement = currentTexts.preparing(format)
            scope.launch {
                val outcome = runCatching { download(format) }
                (outcome.exceptionOrNull() as? CancellationException)?.let { throw it }
                outcome.fold(
                    onSuccess = { result ->
                        phase = MonarchDownloadReportButtonTokens.after(format, result)
                        when (result) {
                            MonarchDownloadReportResult.Downloaded -> announcement = currentTexts.downloaded(format)
                            is MonarchDownloadReportResult.Queued -> announcement = result.message ?: currentQueued
                            is MonarchDownloadReportResult.File -> {
                                contract.nextMimeType = format.mimeType
                                launcher.launch(result.fileName)
                            }
                        }
                    },
                    onFailure = { phase = MonarchDownloadReportPhase.Failed(format, it) },
                )
            }
        }
    }
}

/** Writes the prepared file to the document the person chose, off the main thread. */
private fun CoroutineScope.writeReport(
    context: Context,
    uri: Uri,
    saving: MonarchDownloadReportPhase.Saving,
    onDone: (Throwable?) -> Unit,
) {
    launch {
        val outcome = runCatching {
            withContext(Dispatchers.IO) {
                val stream = checkNotNull(context.contentResolver.openOutputStream(uri)) { "No stream for $uri" }
                stream.use { saving.file.write(it) }
            }
        }
        (outcome.exceptionOrNull() as? CancellationException)?.let { throw it }
        onDone(outcome.exceptionOrNull())
    }
}
