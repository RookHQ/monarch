package dev.monarchui.components.files.uploadqueue

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.clearAndSetSemantics
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.files.attachmentchip.MonarchUploadStatus
import dev.monarchui.components.files.filetypeicon.MonarchFileTypeIcon
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.monarchFormatBytes
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchRemovalFocus
import dev.monarchui.theme.MonarchTheme

/**
 * The files being uploaded: progress and status per file, cancel, retry and
 * remove, and a summary with the total progress.
 *
 * Built by Monarch from a `Column` of rows with `MonarchFileTypeIcon`,
 * `MonarchProgress` and `MonarchButton`. The app does the transfer (with
 * WorkManager when it must survive the app going to the background) and
 * passes the [items] again when a status or a progress changes; the queue only
 * shows them. Uploads that finish or fail are announced, and so is the end of
 * a batch.
 *
 * The rows are a `Column`, not a lazy list, so the queue can sit in a
 * scrolling screen; keep it to the uploads of one task.
 *
 * @param items the uploads, in display order.
 * @param modifier the modifier of the queue.
 * @param showSummary shows the heading, the summary ("2 of 5 uploaded · 1 failed"), the clear button and the
 * total progress.
 * @param labels replacements for every built-in string and announcement.
 * @param onCancel cancels a queued or uploading file by id. Adds cancel buttons.
 * @param onRetry retries a failed or cancelled, retryable file by id. Adds retry buttons.
 * @param onRemove removes a file that ended from the list by id. Adds remove buttons.
 * @param onClearFinished removes every file that ended. Adds a header button while there is one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchUploadQueue(
    items: List<MonarchUploadQueueItem>,
    modifier: Modifier = Modifier,
    showSummary: Boolean = true,
    labels: MonarchUploadQueueLabels = MonarchUploadQueueLabels(),
    onCancel: ((String) -> Unit)? = null,
    onRetry: ((String) -> Unit)? = null,
    onRemove: ((String) -> Unit)? = null,
    onClearFinished: (() -> Unit)? = null,
) {
    val tokens = MonarchUploadQueueTokens
    val counts = MonarchUploadQueueCounts.of(items)
    var previous by remember { mutableStateOf<Map<String, MonarchUploadStatus>?>(null) }
    var events by remember { mutableStateOf(emptyList<MonarchUploadQueueEvent>()) }
    LaunchedEffect(items) {
        val next = tokens.events(previous, items)
        previous = items.associate { it.id to it.status }
        if (next.isNotEmpty()) events = next
    }
    val announcement = events.map { announcementOf(it, labels) }.joinToString(" ")
    // As on the web: after a removal focus goes to the first button of the next row, else of the previous row,
    // else to the heading.
    val removalFocus = rememberMonarchRemovalFocus(items.map { it.id })
    val remove = onRemove?.let { handler ->
        { id: String ->
            removalFocus.removing(items.map { it.id }, id)
            handler(id)
        }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
        if (showSummary) Header(items, counts, labels, onClearFinished, removalFocus.fallback)
        items.forEach { item ->
            UploadRow(item, labels, onCancel, onRetry, remove, removalFocus.requester(item.id))
        }
        // Always present, so each change is announced once as a change of this live region.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

@Composable
private fun Header(
    items: List<MonarchUploadQueueItem>,
    counts: MonarchUploadQueueCounts,
    labels: MonarchUploadQueueLabels,
    onClearFinished: (() -> Unit)?,
    headingFocus: FocusRequester,
) {
    val tokens = MonarchUploadQueueTokens
    Column(verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MonarchText(
                    text = monarchString(labels.title, R.string.monarch_uploadqueue_title),
                    // Focusable, so it can take focus when the last row is removed, as the web heading does.
                    modifier = Modifier.focusRequester(headingFocus).focusable().semantics { heading() },
                    style = MonarchTextStyle.Subheading,
                )
                if (items.isNotEmpty()) {
                    MonarchText(
                        text = labels.summary?.invoke(counts) ?: defaultSummary(counts),
                        style = MonarchTextStyle.Caption,
                        color = tokens.mutedColor(MonarchTheme.colors),
                    )
                }
            }
            if (onClearFinished != null && counts.finished > 0) {
                MonarchButton(
                    text = monarchString(labels.clearFinished, R.string.monarch_uploadqueue_clear_finished),
                    onClick = onClearFinished,
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            }
        }
        if (counts.active > 0) {
            MonarchProgress(
                modifier = Modifier.fillMaxWidth(),
                progress = tokens.totalProgress(items),
                label = monarchString(labels.totalProgress, R.string.monarch_uploadqueue_total_progress),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UploadRow(
    item: MonarchUploadQueueItem,
    labels: MonarchUploadQueueLabels,
    onCancel: ((String) -> Unit)?,
    onRetry: ((String) -> Unit)?,
    onRemove: ((String) -> Unit)?,
    firstButton: FocusRequester,
) {
    val tokens = MonarchUploadQueueTokens
    val colors = MonarchTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.RowGap)) {
        MonarchFileTypeIcon(fileName = item.name, contentType = item.contentType, decorative = true)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
            Column(modifier = Modifier.semantics(mergeDescendants = true) { }) {
                MonarchText(
                    text = item.name,
                    style = MonarchTextStyle.Label,
                    color = tokens.nameColor(colors),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(tokens.ActionGap)) {
                    if (item.size != null) {
                        MonarchText(
                            text = formatSize(item.size, labels),
                            style = MonarchTextStyle.Caption,
                            color = tokens.mutedColor(colors),
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(tokens.ActionGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val statusIcon = tokens.statusIcon(item.status)
                        if (statusIcon != null) {
                            val statusColor = tokens.statusColor(item.status, colors)
                            CompositionLocalProvider(LocalContentColor provides statusColor) {
                                Box(Modifier.clearAndSetSemantics { }) { statusIcon() }
                            }
                        }
                        MonarchText(
                            text = item.detail ?: statusText(item, labels),
                            style = MonarchTextStyle.Caption,
                            color = tokens.statusColor(item.status, colors),
                        )
                    }
                }
            }
            if (item.status == MonarchUploadStatus.Uploading) {
                MonarchProgress(
                    modifier = Modifier.fillMaxWidth(),
                    progress = tokens.fraction(item.progress),
                    label = labels.progressLabel?.invoke(item.name)
                        ?: stringResource(R.string.monarch_uploadqueue_progress_label, item.name),
                )
            }
        }
        RowActions(item, labels, onCancel, onRetry, onRemove, firstButton)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RowActions(
    item: MonarchUploadQueueItem,
    labels: MonarchUploadQueueLabels,
    onCancel: ((String) -> Unit)?,
    onRetry: ((String) -> Unit)?,
    onRemove: ((String) -> Unit)?,
    firstButton: FocusRequester,
) {
    val retry = onRetry?.takeIf { MonarchUploadQueueTokens.canRetry(item) }
    val cancel = onCancel?.takeIf { item.status.active }
    val remove = onRemove?.takeIf { !item.status.active }
    if (retry == null && cancel == null && remove == null) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(MonarchUploadQueueTokens.ActionGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (retry != null) {
            val retryName = labels.retryLabel?.invoke(item.name)
                ?: stringResource(R.string.monarch_uploadqueue_retry_label, item.name)
            MonarchButton(
                text = monarchString(labels.retry, R.string.monarch_uploadqueue_retry),
                onClick = { retry(item.id) },
                modifier = Modifier
                    .focusRequester(firstButton)
                    .semantics { contentDescription = retryName },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                leadingIcon = MonarchDefaultIcons.Retry,
            )
        }
        if (cancel != null) {
            MonarchIconButton(
                label = labels.cancelLabel?.invoke(item.name)
                    ?: stringResource(R.string.monarch_uploadqueue_cancel_label, item.name),
                onClick = { cancel(item.id) },
                modifier = if (retry == null) Modifier.focusRequester(firstButton) else Modifier,
                icon = MonarchDefaultIcons.Close,
            )
        }
        if (remove != null) {
            MonarchIconButton(
                label = labels.removeLabel?.invoke(item.name)
                    ?: stringResource(R.string.monarch_uploadqueue_remove_label, item.name),
                onClick = { remove(item.id) },
                modifier = if (retry == null && cancel == null) Modifier.focusRequester(firstButton) else Modifier,
                icon = MonarchDefaultIcons.Close,
            )
        }
    }
}

@Composable
private fun statusText(item: MonarchUploadQueueItem, labels: MonarchUploadQueueLabels): String = when (item.status) {
    MonarchUploadStatus.Queued -> monarchString(labels.queued, R.string.monarch_uploadqueue_queued)
    MonarchUploadStatus.Uploading -> {
        val percent = MonarchUploadQueueTokens.percent(item.progress)
        labels.uploading?.invoke(percent)
            ?: if (percent == null) {
                stringResource(R.string.monarch_uploadqueue_uploading)
            } else {
                stringResource(R.string.monarch_uploadqueue_uploading_percent, percent)
            }
    }
    MonarchUploadStatus.Done -> monarchString(labels.done, R.string.monarch_uploadqueue_done)
    MonarchUploadStatus.Failed -> labels.failed?.invoke(item.errorMessage)
        ?: item.errorMessage
        ?: stringResource(R.string.monarch_uploadqueue_failed)
    MonarchUploadStatus.Cancelled -> monarchString(labels.cancelled, R.string.monarch_uploadqueue_cancelled)
}

@Composable
private fun formatSize(bytes: Long, labels: MonarchUploadQueueLabels): String =
    labels.formatSize?.invoke(bytes) ?: monarchFormatBytes(bytes)

@Composable
private fun defaultSummary(counts: MonarchUploadQueueCounts): String {
    val considered = counts.total - counts.cancelled
    val parts = mutableListOf(
        pluralStringResource(R.plurals.monarch_uploadqueue_summary_uploaded, considered, counts.done, considered),
    )
    if (counts.failed > 0) {
        parts += pluralStringResource(R.plurals.monarch_uploadqueue_summary_failed, counts.failed, counts.failed)
    }
    if (counts.cancelled > 0) {
        val cancelled = counts.cancelled
        parts += pluralStringResource(R.plurals.monarch_uploadqueue_summary_cancelled, cancelled, cancelled)
    }
    return parts.joinToString(stringResource(R.string.monarch_uploadqueue_summary_separator))
}

@Composable
private fun announcementOf(event: MonarchUploadQueueEvent, labels: MonarchUploadQueueLabels): String = when (event) {
    is MonarchUploadQueueEvent.Done ->
        labels.announceDone?.invoke(event.name)
            ?: stringResource(R.string.monarch_uploadqueue_announce_done, event.name)
    is MonarchUploadQueueEvent.Failed -> labels.announceFailed?.invoke(event.name, event.error)
        ?: event.error?.let { stringResource(R.string.monarch_uploadqueue_announce_failed_reason, event.name, it) }
        ?: stringResource(R.string.monarch_uploadqueue_announce_failed, event.name)
    is MonarchUploadQueueEvent.Finished -> labels.announceFinished?.invoke(event.counts)
        ?: if (event.counts.failed > 0) {
            pluralStringResource(
                R.plurals.monarch_uploadqueue_announce_finished_failed,
                event.counts.failed,
                event.counts.done,
                event.counts.failed,
            )
        } else {
            pluralStringResource(R.plurals.monarch_uploadqueue_announce_finished, event.counts.done, event.counts.done)
        }
}
