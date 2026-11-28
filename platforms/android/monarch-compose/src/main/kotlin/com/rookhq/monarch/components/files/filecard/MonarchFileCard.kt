package com.rookhq.monarch.components.files.filecard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.files.filetypeicon.MonarchFileTypeFamily
import com.rookhq.monarch.components.files.filetypeicon.MonarchFileTypeIcon
import com.rookhq.monarch.components.files.filetypeicon.MonarchFileTypeIconSize
import com.rookhq.monarch.components.files.filetypeicon.MonarchFileTypeIconTokens
import com.rookhq.monarch.components.forms.checkbox.MonarchCheckbox
import com.rookhq.monarch.internal.MonarchImage
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.monarchFormatBytes
import com.rookhq.monarch.theme.MonarchTheme

/**
 * One file as a card or a row: a picture or the type icon, the name, the type
 * and size, optional badges, an optional selection checkbox and an optional
 * place for actions.
 *
 * Built by Monarch on a Material 3 `Surface` with a `MonarchCheckbox`,
 * `MonarchFileTypeIcon` and `MonarchBadge`. `MonarchCard` has no selected
 * state, so the card draws its own selected fill and thicker accent border.
 * The picture, name and meta line are one TalkBack element; with [onOpen] it
 * is a button with a pressed state. The checkbox and the actions are
 * separate elements.
 *
 * @param name the file name with its extension.
 * @param modifier the modifier of the card.
 * @param sizeBytes the size in bytes, shown in the locale's file units, such as "2.4 MB".
 * @param contentType the MIME type. With the name it decides the type icon and the type in the meta line.
 * @param meta more text after the size, already localised, such as "Edited yesterday" or an owner.
 * @param image a preview the app has loaded, or `null` (no preview, loading or failed) for the type icon on a
 * tinted tile.
 * @param badges short states shown as neutral `MonarchBadge`s under the name, such as "Shared".
 * @param layout [MonarchFileCardLayout.Card] for grids, [MonarchFileCardLayout.Row] for lists.
 * @param selected whether the file is selected. Shown only with [onSelectedChange].
 * @param onSelectedChange shows a selection checkbox: at the top start corner of a card, at the start of a row.
 * @param enabled whether the file can be opened and selected. Disable the app's [actions] too.
 * @param onOpen opens the file. Makes the picture, name and meta one button.
 * @param labels replacements for the built-in strings.
 * @param interactionSource the source of the press and focus interactions of the open area.
 * @param actions the file's actions, such as an `IconButton` that opens a `DropdownMenu`, at the top end corner
 * of a card or the end of a row. Name an icon-only trigger after the file.
 */
@Composable
public fun MonarchFileCard(
    name: String,
    modifier: Modifier = Modifier,
    sizeBytes: Long? = null,
    contentType: String? = null,
    meta: String? = null,
    image: Painter? = null,
    badges: List<String> = emptyList(),
    layout: MonarchFileCardLayout = MonarchFileCardLayout.Card,
    selected: Boolean = false,
    onSelectedChange: ((Boolean) -> Unit)? = null,
    enabled: Boolean = true,
    onOpen: (() -> Unit)? = null,
    labels: MonarchFileCardLabels = MonarchFileCardLabels(),
    interactionSource: MutableInteractionSource? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    val tokens = MonarchFileCardTokens
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val selectedShown = selected && onSelectedChange != null
    val colors = tokens.colors(selectedShown, enabled, pressed && onOpen != null, MonarchTheme.colors)
    val shape = if (layout == MonarchFileCardLayout.Card) MonarchTheme.shapes.lg else MonarchTheme.shapes.md
    val parts = FileCardParts(
        name = name,
        metaLine = fileCardMeta(name, contentType, sizeBytes, meta, labels),
        badges = badges,
        layout = layout,
        enabled = enabled,
        colors = colors,
    )
    val open = Modifier.fileCardOpenArea(onOpen, enabled, source, shape)
    Surface(
        modifier = modifier,
        shape = shape,
        color = colors.container,
        contentColor = colors.content,
        border = colors.border?.let { BorderStroke(tokens.borderWidth(selectedShown, enabled), it) },
    ) {
        val checkbox: (@Composable () -> Unit)? = onSelectedChange?.let { change ->
            {
                val backdrop = layout == MonarchFileCardLayout.Card
                FileCardCheckbox(name, selected, change, enabled, labels, backdrop)
            }
        }
        if (layout == MonarchFileCardLayout.Card) {
            FileCardGrid(parts, open, image, contentType, checkbox, actions)
        } else {
            FileCardRow(parts, open, image, contentType, checkbox, actions)
        }
    }
}

/** What both layouts show. */
private class FileCardParts(
    val name: String,
    val metaLine: String,
    val badges: List<String>,
    val layout: MonarchFileCardLayout,
    val enabled: Boolean,
    val colors: MonarchSurfaceColors,
)

/** One element for TalkBack; a button with a ripple and the focus ring when the file can be opened. */
private fun Modifier.fileCardOpenArea(
    onOpen: (() -> Unit)?,
    enabled: Boolean,
    source: MutableInteractionSource,
    shape: Shape,
): Modifier {
    val merged = semantics(mergeDescendants = true) { }
    return if (onOpen == null) {
        merged
    } else {
        merged.monarchFocusRing(shape).clickable(
            interactionSource = source,
            indication = ripple(),
            enabled = enabled,
            role = Role.Button,
            onClick = onOpen,
        )
    }
}

@Composable
private fun FileCardGrid(
    parts: FileCardParts,
    modifier: Modifier,
    image: Painter?,
    contentType: String?,
    checkbox: (@Composable () -> Unit)?,
    actions: (@Composable () -> Unit)?,
) {
    val tokens = MonarchFileCardTokens
    Box {
        Column(modifier = modifier.fillMaxWidth()) {
            MonarchImage(
                painter = image,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(tokens.PREVIEW_RATIO),
                shape = MonarchTheme.shapes.none,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(tokens.placeholder(MonarchTheme.colors)),
                    contentAlignment = Alignment.Center,
                ) {
                    MonarchFileTypeIcon(
                        fileName = parts.name,
                        contentType = contentType,
                        size = MonarchFileTypeIconSize.Lg,
                        decorative = true,
                    )
                }
            }
            FileCardText(parts, Modifier.padding(tokens.Padding))
        }
        if (checkbox != null) {
            Box(Modifier.align(Alignment.TopStart).padding(tokens.CornerInset)) { checkbox() }
        }
        if (actions != null) {
            Box(Modifier.align(Alignment.TopEnd).padding(tokens.CornerInset)) { actions() }
        }
    }
}

@Composable
private fun FileCardRow(
    parts: FileCardParts,
    modifier: Modifier,
    image: Painter?,
    contentType: String?,
    checkbox: (@Composable () -> Unit)?,
    actions: (@Composable () -> Unit)?,
) {
    val tokens = MonarchFileCardTokens
    Row(
        modifier = Modifier.heightIn(min = tokens.RowMinHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        checkbox?.invoke()
        Row(
            modifier = modifier
                .weight(1f)
                .heightIn(min = tokens.RowMinHeight)
                .padding(horizontal = tokens.Padding, vertical = tokens.Gap),
            horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchImage(
                painter = image,
                contentDescription = null,
                modifier = Modifier.size(tokens.RowThumbnail),
            ) {
                MonarchFileTypeIcon(
                    fileName = parts.name,
                    contentType = contentType,
                    size = MonarchFileTypeIconSize.Lg,
                    decorative = true,
                )
            }
            FileCardText(parts, Modifier.weight(1f))
        }
        actions?.invoke()
    }
}

/** The name, the meta line and the badges. */
@Composable
private fun FileCardText(parts: FileCardParts, modifier: Modifier) {
    val tokens = MonarchFileCardTokens
    val colors = MonarchTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        val lines = tokens.nameLines(parts.layout)
        Text(
            text = parts.name,
            style = MonarchTheme.typography.bodyStrong,
            color = parts.colors.content,
            maxLines = lines,
            overflow = if (lines == 1) TextOverflow.MiddleEllipsis else TextOverflow.Ellipsis,
        )
        Text(
            text = parts.metaLine,
            style = MonarchTheme.typography.caption,
            color = tokens.metaColor(parts.enabled, colors),
        )
        if (parts.badges.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                verticalArrangement = Arrangement.spacedBy(tokens.Gap),
            ) {
                parts.badges.forEach { MonarchBadge(text = it) }
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun FileCardCheckbox(
    name: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    enabled: Boolean,
    labels: MonarchFileCardLabels,
    backdrop: Boolean,
) {
    val label = labels.select?.invoke(name) ?: stringResource(R.string.monarch_filecard_select, name)
    val colors = MonarchTheme.colors
    Box(
        modifier = if (backdrop) {
            Modifier.background(MonarchFileCardTokens.checkboxBackdrop(colors), MonarchTheme.shapes.full)
        } else {
            Modifier
        },
    ) {
        MonarchCheckbox(
            checked = selected,
            onCheckedChange = onSelectedChange,
            modifier = Modifier.semantics { contentDescription = label },
            enabled = enabled,
        )
    }
}

@Composable
private fun fileCardMeta(
    name: String,
    contentType: String?,
    sizeBytes: Long?,
    meta: String?,
    labels: MonarchFileCardLabels,
): String {
    val family = remember(name, contentType) { MonarchFileTypeFamily.of(name, contentType) }
    val type = labels.type ?: stringResource(MonarchFileTypeIconTokens.label(family))
    val size = sizeBytes?.let { monarchFormatBytes(it) }
    return MonarchFileCardTokens.meta(type, size, meta, stringResource(R.string.monarch_filecard_separator))
}
