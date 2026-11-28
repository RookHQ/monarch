package dev.monarchui.components.intelligence.aitoolresultcard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValue
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValueItem
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValueLayout
import dev.monarchui.components.navigation.link.MonarchLink
import dev.monarchui.components.navigation.link.MonarchLinkVariant
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * A preview of what an AI tool did: a record created, an email sent, an
 * event booked, a ticket filed. It shows the kind, the title, a line of
 * detail, key facts, a link to open the result and an optional Undo.
 *
 * Built by Monarch on [MonarchCard], with [MonarchKeyValue] for the facts,
 * [MonarchLink] for Open and a ghost [MonarchButton] for Undo. Show it for a
 * `MonarchAiToolCallPart` whose state is `Succeeded`, with the title and facts
 * the app reads from the tool's output.
 *
 * TalkBack reads the kind, the title and the description as one element.
 * Open and Undo are separate elements. Undo runs [onUndo] in a coroutine of
 * the card: the button is busy while it runs and ignores taps; when it
 * returns, Open and Undo go away and "Undone" is shown and announced; when it
 * throws, "Couldn’t undo. Try again." is shown and announced and Undo stays.
 *
 * @param title what the tool made or changed: the record's name, the email's subject.
 * @param modifier the modifier of the card.
 * @param kind what the tool did; picks the label above the title.
 * @param description one line of detail, such as "To 3 people".
 * @param meta short facts, shown with [MonarchKeyValue] in the stacked layout.
 * @param icon a glyph for the kind in a tile at the start, for example a `MonarchIcon` with a `null` content
 * description. It takes the success colour, muted once undone. Monarch bundles no icons, so without it there
 * is no tile.
 * @param onOpen shows an Open link that calls it, for example to open the record. Hidden once undone.
 * @param onUndo shows Undo. Runs in a coroutine of the card; throw to report a failure.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchAiToolResultCard(
    title: String,
    modifier: Modifier = Modifier,
    kind: MonarchAiToolResultKind = MonarchAiToolResultKind.Generic,
    description: String? = null,
    meta: List<MonarchKeyValueItem> = emptyList(),
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.toolResult(kind),
    onOpen: (() -> Unit)? = null,
    onUndo: (suspend () -> Unit)? = null,
    labels: MonarchAiToolResultCardLabels = MonarchAiToolResultCardLabels(),
) {
    val tokens = MonarchAiToolResultCardTokens
    val colors = MonarchTheme.colors
    val scope = rememberCoroutineScope()
    val currentOnUndo by rememberUpdatedState(onUndo)
    var undo by remember { mutableStateOf(MonarchAiToolResultCardUndo.Idle) }
    val runUndo: () -> Unit = {
        val action = currentOnUndo
        if (action != null && tokens.canUndo(undo)) {
            undo = MonarchAiToolResultCardUndo.Pending
            scope.launch {
                val outcome = runCatching { action() }
                // An undo cancelled with the card is not a failure.
                (outcome.exceptionOrNull() as? CancellationException)?.let { throw it }
                undo = if (outcome.isSuccess) MonarchAiToolResultCardUndo.Done else MonarchAiToolResultCardUndo.Failed
            }
        }
    }
    val kindLabel = labels.kind?.invoke(kind) ?: stringResource(tokens.kindLabel(kind))
    val undone = undo == MonarchAiToolResultCardUndo.Done
    MonarchCard(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.ContentGap)) {
            if (icon != null) ToolResultTile(undone, icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.ContentGap)) {
                Column(
                    modifier = Modifier.semantics(mergeDescendants = true) { },
                    verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
                ) {
                    Text(text = kindLabel, style = MonarchTheme.typography.caption, color = colors.textMuted)
                    Text(text = title, style = MonarchTheme.typography.bodyStrong, color = colors.textDefault)
                    if (!description.isNullOrBlank()) {
                        Text(text = description, style = MonarchTheme.typography.body, color = colors.textMuted)
                    }
                }
                if (meta.isNotEmpty()) MonarchKeyValue(items = meta, layout = MonarchKeyValueLayout.Stack)
                if (tokens.showsActions(undo) && (onOpen != null || onUndo != null)) {
                    ToolResultActions(title, onOpen, onUndo?.let { runUndo }, undo, labels)
                }
                ToolResultStatus(undo, labels)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToolResultActions(
    title: String,
    onOpen: (() -> Unit)?,
    onUndo: (() -> Unit)?,
    undo: MonarchAiToolResultCardUndo,
    labels: MonarchAiToolResultCardLabels,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MonarchAiToolResultCardTokens.ActionGap),
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (onOpen != null) {
            val openLabel = labels.openLabel?.invoke(title)
                ?: stringResource(R.string.monarch_aitoolresultcard_open_label, title)
            MonarchLink(
                text = monarchString(labels.open, R.string.monarch_aitoolresultcard_open),
                onClick = onOpen,
                modifier = Modifier.semantics { contentDescription = openLabel },
                variant = MonarchLinkVariant.Standalone,
            )
        }
        if (onUndo != null) {
            MonarchButton(
                text = monarchString(labels.undo, R.string.monarch_aitoolresultcard_undo),
                onClick = onUndo,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                loading = undo == MonarchAiToolResultCardUndo.Pending,
            )
        }
    }
}

@Composable
private fun ToolResultTile(undone: Boolean, icon: @Composable () -> Unit) {
    val tile = MonarchAiToolResultCardTokens.tileColors(undone, MonarchTheme.colors)
    Box(
        modifier = Modifier
            .size(MonarchAiToolResultCardTokens.TileSize)
            .background(tile.container, MonarchTheme.shapes.md)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides tile.content, content = icon)
    }
}

/** The status line: always present as a polite live region, so "Undone" and the failure are announced. */
@Composable
private fun ToolResultStatus(undo: MonarchAiToolResultCardUndo, labels: MonarchAiToolResultCardLabels) {
    val status = when (undo) {
        MonarchAiToolResultCardUndo.Done -> monarchString(labels.undone, R.string.monarch_aitoolresultcard_undone)
        MonarchAiToolResultCardUndo.Failed ->
            monarchString(labels.undoFailed, R.string.monarch_aitoolresultcard_undo_failed)
        else -> null
    }
    Box(
        Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            if (status != null) contentDescription = status
        },
    ) {
        if (status != null) {
            Text(
                text = status,
                modifier = Modifier.clearAndSetSemantics { },
                style = MonarchTheme.typography.caption,
                color = MonarchAiToolResultCardTokens.statusColor(undo, MonarchTheme.colors),
            )
        }
    }
}
