package dev.monarchui.components.collaboration.mentionpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.R
import dev.monarchui.components.datadisplay.listrow.MonarchListRow
import dev.monarchui.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import dev.monarchui.components.feedback.skeleton.MonarchSkeleton
import dev.monarchui.components.feedback.skeleton.MonarchSkeletonShape
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarShape
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * Suggestions for an @mention or #channel: a strip of people and channels
 * that the app places directly above the message field, so it sits above
 * the keyboard, with loading, empty and error states.
 *
 * Built by Monarch from `MonarchListRow` rows in a `LazyColumn`,
 * `MonarchAvatar`, `MonarchSkeleton` and `MonarchInlineErrorRetry`. Compose
 * gives no caret rectangle to anchor a popup to, so the strip spans the
 * width of the composer. Find the query with [MonarchMentionQuery.find] as
 * the text changes, search in the app (for example in a `LaunchedEffect`
 * keyed by the query, which cancels the previous search), and complete the
 * mention with [MonarchMentionQuery.replacing] in [onSelect]. Focus stays in
 * the text field; the number of suggestions, "Searching…", "No matches" and
 * the error are announced through a polite live region.
 *
 * Nothing is drawn without a query, or when a query of two words finds
 * nothing, which is ordinary prose rather than a mention.
 *
 * @param query the mention being typed, or `null` to close the picker.
 * @param items the suggestions for [query]. People and channels are grouped under headings when both are present.
 * @param onSelect called with the chosen suggestion.
 * @param modifier the modifier of the strip.
 * @param status loading, ready or failed.
 * @param onRetry repeats a failed search. Adds a Retry button.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchMentionPicker(
    query: MonarchMentionQuery?,
    items: List<MonarchMentionItem>,
    onSelect: (MonarchMentionItem) -> Unit,
    modifier: Modifier = Modifier,
    status: MonarchMentionPickerStatus = MonarchMentionPickerStatus.Ready,
    onRetry: (() -> Unit)? = null,
    labels: MonarchMentionPickerLabels = MonarchMentionPickerLabels(),
) {
    val tokens = MonarchMentionPickerTokens
    if (!tokens.isOpen(query, status, items.size)) return
    val colors = MonarchTheme.colors
    val loading = monarchString(labels.loading, R.string.monarch_mentionpicker_loading)
    val empty = monarchString(labels.empty, R.string.monarch_mentionpicker_empty)
    val error = monarchString(labels.error, R.string.monarch_mentionpicker_error)
    val announcement = when {
        status == MonarchMentionPickerStatus.Loading -> loading
        status == MonarchMentionPickerStatus.Failed -> error
        items.isEmpty() -> empty
        else -> labels.count?.invoke(items.size)
            ?: pluralStringResource(R.plurals.monarch_mentionpicker_count, items.size, items.size)
    }
    val name = monarchString(labels.label, R.string.monarch_mentionpicker_label)
    val border = tokens.border(colors)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(tokens.background(colors))
            .drawBehind {
                val width = tokens.BorderWidth.toPx()
                drawLine(border, Offset(0f, width / 2), Offset(size.width, width / 2), width)
            }
            .semantics {
                contentDescription = name
                stateDescription = announcement
                liveRegion = LiveRegionMode.Polite
                isTraversalGroup = true
            },
    ) {
        when {
            status == MonarchMentionPickerStatus.Loading -> Placeholder(loading)
            status == MonarchMentionPickerStatus.Failed -> Failure(error, onRetry, labels)
            items.isEmpty() -> Note(empty)
            else -> Suggestions(items, onSelect, labels)
        }
    }
}

@Composable
private fun Suggestions(
    items: List<MonarchMentionItem>,
    onSelect: (MonarchMentionItem) -> Unit,
    labels: MonarchMentionPickerLabels,
) {
    val grouped = MonarchMentionPickerTokens.grouped(items)
    val people = items.filter { it.kind == MonarchMentionKind.Person }
    val channels = items.filter { it.kind == MonarchMentionKind.Channel }
    val peopleHeading = monarchString(labels.people, R.string.monarch_mentionpicker_people)
    val channelsHeading = monarchString(labels.channels, R.string.monarch_mentionpicker_channels)
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = MonarchMentionPickerTokens.MaxListHeight)) {
        group(if (grouped) peopleHeading else null, people, onSelect)
        group(if (grouped) channelsHeading else null, channels, onSelect)
    }
}

private fun LazyListScope.group(
    heading: String?,
    items: List<MonarchMentionItem>,
    onSelect: (MonarchMentionItem) -> Unit,
) {
    if (items.isEmpty()) return
    if (heading != null) {
        item(key = "heading-$heading") {
            Text(
                text = heading,
                modifier = Modifier
                    .padding(
                        horizontal = MonarchMentionPickerTokens.Padding,
                        vertical = MonarchMentionPickerTokens.Gap,
                    )
                    .semantics { heading() },
                style = MonarchTheme.typography.caption,
                color = MonarchMentionPickerTokens.noteColor(MonarchTheme.colors),
            )
        }
    }
    items.forEach { item ->
        item(key = "${item.kind}-${item.id}") { Suggestion(item = item, onSelect = onSelect) }
    }
}

@Composable
private fun Suggestion(item: MonarchMentionItem, onSelect: (MonarchMentionItem) -> Unit) {
    MonarchListRow(
        headlineContent = { Text(text = item.label, style = MonarchTheme.typography.body) },
        onClick = { onSelect(item) },
        enabled = item.enabled,
        supportingContent = item.detail?.let { detail ->
            { Text(text = detail, style = MonarchTheme.typography.caption) }
        },
        leadingContent = {
            if (item.kind == MonarchMentionKind.Channel) {
                MonarchAvatar(
                    image = null,
                    name = item.label,
                    shape = MonarchAvatarShape.Square,
                    size = MonarchAvatarSize.Md,
                    seed = item.id,
                    decorative = true,
                    icon = {
                        Text(text = MonarchMentionPickerTokens.CHANNEL_MARK, style = MonarchTheme.typography.label)
                    },
                )
            } else {
                MonarchAvatar(
                    image = item.image,
                    name = item.label,
                    size = MonarchAvatarSize.Md,
                    seed = item.id,
                    decorative = true,
                )
            }
        },
    )
}

@Composable
private fun Placeholder(loading: String) {
    val tokens = MonarchMentionPickerTokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(tokens.Padding)
            .clearAndSetSemantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(tokens.Padding),
    ) {
        repeat(tokens.PLACEHOLDER_ROWS) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchSkeleton(shape = MonarchSkeletonShape.Circle)
                MonarchSkeleton(modifier = Modifier.width(tokens.PlaceholderWidth), shape = MonarchSkeletonShape.Text)
            }
        }
    }
}

@Composable
private fun Failure(error: String, onRetry: (() -> Unit)?, labels: MonarchMentionPickerLabels) {
    if (onRetry != null) {
        MonarchInlineErrorRetry(
            message = error,
            onRetry = onRetry,
            modifier = Modifier.padding(MonarchMentionPickerTokens.Padding),
            retryLabel = labels.retry,
        )
    } else {
        Note(error)
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(MonarchMentionPickerTokens.Padding),
        style = MonarchTheme.typography.body,
        color = MonarchMentionPickerTokens.noteColor(MonarchTheme.colors),
    )
}
