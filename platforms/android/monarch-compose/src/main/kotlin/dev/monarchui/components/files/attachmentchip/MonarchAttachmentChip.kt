package dev.monarchui.components.files.attachmentchip

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.files.filetypeicon.MonarchFileTypeIcon
import dev.monarchui.components.files.filetypeicon.MonarchFileTypeIconSize
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchFormatBytes
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme

/**
 * A compact chip for one attached file: the type icon, the name, the size,
 * upload progress or failure, and remove and retry buttons.
 *
 * Built by Monarch from a bordered `Row` with `MonarchFileTypeIcon`,
 * `MonarchProgress` and `MonarchIconButton`s, not `InputChip`: an input
 * chip is one touch target, and this chip has up to three (open, retry and
 * remove) and a second line of text. The name and the line under it are one
 * TalkBack element; each button is its own element named after the file,
 * "Remove report.pdf". A failure is announced through a polite live region.
 * Long names are shortened in the middle; TalkBack reads the whole name.
 *
 * @param name the file name.
 * @param modifier the modifier of the chip.
 * @param status where the upload stands.
 * @param enabled disables the retry and remove buttons, for example while the message is sending.
 * @param sizeBytes the size in bytes, shown in the user's units once uploaded.
 * @param sizeText the size as written by the caller, such as "46.9 kB"; it replaces the one built from [sizeBytes].
 * @param contentType the MIME type; with the name it picks the type icon.
 * @param progress the uploaded fraction from 0 to 1 while uploading. `null` shows a moving bar and no percentage.
 * @param errorMessage why the upload failed, shown instead of "Upload failed".
 * @param onOpen opens the file once uploaded; the name becomes a button. Monarch does not open files.
 * @param onRetry adds a retry button to a failed upload.
 * @param onRemove adds a remove button.
 * @param icon replaces the type icon, for example with a thumbnail. Decorative.
 * @param retryIcon the icon of the retry button. Drawn by default; without it (`null`) the button shows the
 * word "Retry".
 * @param removeIcon the icon of the remove button. Drawn by default; without it (`null`) the button shows the
 * word "Remove".
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchAttachmentChip(
    name: String,
    modifier: Modifier = Modifier,
    status: MonarchUploadStatus = MonarchUploadStatus.Done,
    enabled: Boolean = true,
    sizeBytes: Long? = null,
    contentType: String? = null,
    progress: Float? = null,
    errorMessage: String? = null,
    onOpen: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    retryIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Retry,
    removeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    labels: MonarchAttachmentChipLabels = MonarchAttachmentChipLabels(),
    sizeText: String? = null,
) {
    val themeColors = MonarchTheme.colors
    val colors = MonarchAttachmentChipTokens.colors(status, themeColors)
    val fraction = MonarchAttachmentChipTokens.fraction(progress)
    val meta = chipMeta(status, fraction, sizeText ?: sizeBytes?.let { monarchFormatBytes(it) }, errorMessage, labels)
    val opens = MonarchAttachmentChipTokens.opens(status, onOpen != null)
    Row(
        modifier = modifier
            .monarchSurface(colors, MonarchShapeDefaults.Control)
            .padding(MonarchAttachmentChipTokens.Padding),
        horizontalArrangement = Arrangement.spacedBy(MonarchAttachmentChipTokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.clearAndSetSemantics { }) {
            icon?.invoke() ?: MonarchFileTypeIcon(
                fileName = name,
                contentType = contentType,
                size = MonarchFileTypeIconSize.Sm,
                decorative = true,
            )
        }
        Column(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(MonarchAttachmentChipTokens.LineGap),
        ) {
            ChipBody(name, meta, status, if (opens) onOpen else null)
            if (status == MonarchUploadStatus.Uploading) {
                MonarchProgress(
                    modifier = Modifier.fillMaxWidth(),
                    progress = fraction,
                    label = labels.uploading?.invoke(name)
                        ?: stringResource(R.string.monarch_attachmentchip_uploading, name),
                )
            }
        }
        if (status == MonarchUploadStatus.Failed && onRetry != null) {
            ChipAction(
                spoken = labels.retry?.invoke(name) ?: stringResource(R.string.monarch_attachmentchip_retry, name),
                text = monarchString(labels.retryText, R.string.monarch_attachmentchip_retry_text),
                icon = retryIcon,
                enabled = enabled,
                onClick = onRetry,
                showText = true,
            )
        }
        if (onRemove != null) {
            ChipAction(
                spoken = labels.remove?.invoke(name) ?: stringResource(R.string.monarch_attachmentchip_remove, name),
                text = monarchString(labels.removeText, R.string.monarch_attachmentchip_remove_text),
                icon = removeIcon,
                enabled = enabled,
                onClick = onRemove,
            )
        }
    }
}

/** The line under the name: the percentage, the waiting or failed words, or the size once uploaded. */
@Composable
private fun chipMeta(
    status: MonarchUploadStatus,
    fraction: Float?,
    sizeBytes: String?,
    errorMessage: String?,
    labels: MonarchAttachmentChipLabels,
): String? {
    return when (status) {
        // The size and the progress together, as the web chip: "375 KB  Uploading 25%".
        MonarchUploadStatus.Uploading -> listOfNotNull(
            sizeBytes,
            fraction?.let {
                val percent = MonarchAttachmentChipTokens.percent(it)
                stringResource(R.string.monarch_attachmentchip_uploading_percent, percent)
            },
        ).joinToString("  ").ifEmpty { null }
        MonarchUploadStatus.Queued -> monarchString(labels.queued, R.string.monarch_attachmentchip_queued)
        MonarchUploadStatus.Failed ->
            errorMessage ?: monarchString(labels.failed, R.string.monarch_attachmentchip_failed)
        MonarchUploadStatus.Cancelled -> monarchString(labels.cancelled, R.string.monarch_attachmentchip_cancelled)
        MonarchUploadStatus.Done -> sizeBytes
    }
}

/** The name and the line under it, one TalkBack element; a button when [onOpen] is given. */
@Composable
private fun ChipBody(name: String, meta: String?, status: MonarchUploadStatus, onOpen: (() -> Unit)?) {
    val colors = MonarchTheme.colors
    val interaction = if (onOpen != null) {
        Modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(role = Role.Button, interactionSource = null, indication = ripple(), onClick = onOpen)
    } else {
        Modifier
    }
    Column(
        modifier = interaction.semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(MonarchAttachmentChipTokens.LineGap),
    ) {
        Text(
            text = name,
            style = MonarchTheme.typography.label,
            color = MonarchAttachmentChipTokens.nameColor(onOpen != null, colors),
            textDecoration = if (onOpen != null) TextDecoration.Underline else null,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
        )
        if (meta != null) {
            val failed = status == MonarchUploadStatus.Failed
            val metaColor = MonarchAttachmentChipTokens.metaColor(status, colors)
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchAttachmentChipTokens.LineGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (failed) {
                    // The web marks the failure with the error mark before the words; the words say it too.
                    Box(Modifier.clearAndSetSemantics { }) {
                        CompositionLocalProvider(
                            LocalContentColor provides metaColor,
                            content = MonarchDefaultIcons.Error,
                        )
                    }
                }
                Text(
                    text = meta,
                    modifier = if (failed) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier,
                    style = MonarchTheme.typography.caption,
                    color = metaColor,
                )
            }
        }
    }
}

/** A retry or remove button: an icon button when the app gives an icon, else a small ghost button with a word. */
@Composable
private fun ChipAction(
    spoken: String,
    text: String,
    icon: (@Composable () -> Unit)?,
    enabled: Boolean,
    onClick: () -> Unit,
    showText: Boolean = false,
) {
    val named = Modifier.semantics { contentDescription = spoken }
    if (icon != null && !showText) {
        val colors = MonarchTheme.colors
        MonarchIconButton(label = spoken, onClick = onClick, enabled = enabled) {
            val tint = if (enabled) colors.textMuted else colors.textDisabled
            Box(Modifier.clearAndSetSemantics { }) {
                CompositionLocalProvider(LocalContentColor provides tint, content = icon)
            }
        }
    } else {
        MonarchButton(
            text = text,
            onClick = onClick,
            modifier = named,
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
            leadingIcon = icon,
        )
    }
}
