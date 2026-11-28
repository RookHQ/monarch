package dev.monarchui.components.intelligence.aiattachmenttray

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.files.attachmentchip.MonarchAttachmentChip
import dev.monarchui.components.files.attachmentchip.MonarchUploadStatus
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchRemovalFocus
import dev.monarchui.theme.MonarchTheme

/**
 * The files attached to a message for an assistant, in a row that wraps onto
 * further lines, each with its type, name and size, upload progress, failure and
 * retry, and a remove button.
 *
 * Built by Monarch from a `FlowRow` of `MonarchAttachmentChip`s, each at most
 * 320dp wide before the font scale. A file that finishes uploading ("photo.png
 * uploaded") and a file removed with its button ("photo.png removed") are
 * announced through a polite live region; a failure is announced by its chip.
 * With no files nothing is drawn.
 *
 * @param files the attached files in the order they were added.
 * @param onRemove removes a file by id; take it out of [files].
 * @param modifier the modifier of the tray.
 * @param enabled disables the retry and remove buttons, for example while the message is sending.
 * @param onRetry retries a failed upload by id. Without it failed files have no retry button.
 * @param retryIcon the icon of each retry button; without it the button shows "Retry". Monarch bundles no icons.
 * @param removeIcon the icon of each remove button; without it the button shows "Remove".
 * @param labels replacements for the built-in strings.
 * @param fallbackFocusRequester where focus goes when the last file is removed, typically the prompt field (the
 * web `fallbackFocusRef`). Otherwise focus moves to the next file's buttons, else the previous file's.
 */
@Composable
public fun MonarchAiAttachmentTray(
    files: List<MonarchAiAttachmentTrayFile>,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onRetry: ((String) -> Unit)? = null,
    retryIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Retry,
    removeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    labels: MonarchAiAttachmentTrayLabels = MonarchAiAttachmentTrayLabels(),
    fallbackFocusRequester: FocusRequester? = null,
) {
    val tokens = MonarchAiAttachmentTrayTokens
    val removalFocus = rememberMonarchRemovalFocus(files.map { it.id }, fallbackFocusRequester)
    val resources = LocalResources.current
    val label = monarchString(labels.label, R.string.monarch_aiattachmenttray_label)
    val ready by rememberUpdatedState(
        labels.ready ?: { name: String -> resources.getString(R.string.monarch_aiattachmenttray_ready, name) },
    )
    val removed by rememberUpdatedState(
        labels.removed ?: { name: String -> resources.getString(R.string.monarch_aiattachmenttray_removed, name) },
    )
    var previous by remember { mutableStateOf(files) }
    var removedIds by remember { mutableStateOf(emptySet<String>()) }
    var announcement by remember { mutableStateOf("") }
    LaunchedEffect(files) {
        val message = tokens.announcement(previous, files, removedIds, ready, removed)
        if (message.isNotEmpty()) announcement = message
        previous = files
        removedIds = removedIds.filterTo(mutableSetOf()) { id -> files.any { it.id == id } }
    }
    val maxWidth = tokens.MaxTileWidth * LocalDensity.current.fontScale
    Column(modifier = modifier) {
        if (files.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.semantics {
                    contentDescription = label
                    isTraversalGroup = true
                },
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                verticalArrangement = Arrangement.spacedBy(tokens.Gap),
            ) {
                files.forEach { file ->
                    key(file.id) {
                        val status = tokens.status(file)
                        MonarchAttachmentChip(
                            name = file.name,
                            // On the chip, so focus goes to its first button (retry or remove) after a removal.
                            modifier = Modifier
                                .focusRequester(removalFocus.requester(file.id))
                                .widthIn(max = maxWidth),
                            status = status,
                            enabled = enabled,
                            sizeBytes = file.sizeBytes,
                            contentType = file.contentType,
                            progress = file.progress,
                            errorMessage = file.errorMessage,
                            onRetry = if (onRetry != null && status == MonarchUploadStatus.Failed) {
                                { onRetry(file.id) }
                            } else {
                                null
                            },
                            onRemove = {
                                removedIds = removedIds + file.id
                                removalFocus.removing(files.map { it.id }, file.id)
                                onRemove(file.id)
                            },
                            icon = { AiAttachmentTrayTypeBadge(file.name) },
                            retryIcon = retryIcon,
                            removeIcon = removeIcon,
                            labels = labels.chip,
                        )
                    }
                }
            }
        }
        // Always present, so uploads and removals are announced as changes of this region.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** The file type as the web draws it in the tray: its letters on a muted tile. Decorative: the name says the rest. */
@Composable
private fun AiAttachmentTrayTypeBadge(name: String) {
    val tokens = MonarchAiAttachmentTrayTokens
    val colors = MonarchTheme.colors
    Box(
        modifier = Modifier
            .size(tokens.BadgeSize)
            .background(colors.bgMuted, MonarchShapeDefaults.Row),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tokens.badgeText(name),
            style = MonarchTheme.typography.caption,
            color = colors.textMuted,
            maxLines = 1,
        )
    }
}
