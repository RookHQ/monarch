package com.rookhq.monarch.components.intelligence.aimentioncontext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.components.collaboration.mentionpicker.MonarchMentionPickerStatus
import com.rookhq.monarch.components.collaboration.mentionpicker.MonarchMentionQuery
import com.rookhq.monarch.components.datadisplay.listrow.MonarchListRow
import com.rookhq.monarch.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeleton
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeletonShape
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarShape
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.intelligence.aicontextchips.MonarchAiContextChipsLogic
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The @ context menu of an AI prompt: documents, people, records and apps
 * that can be added to what the AI sees, grouped by kind, with loading,
 * empty and error states. The app places it directly above the prompt field,
 * so it sits above the keyboard.
 *
 * Built by Monarch with the layout of `MonarchMentionPicker`:
 * `MonarchListRow` rows in a `LazyColumn`, `MonarchAvatar`, `MonarchSkeleton` and
 * `MonarchInlineErrorRetry`. Find the query with [MonarchMentionQuery.find],
 * run the product's search (for example in a `LaunchedEffect` keyed by the
 * query), leave out ids already in the context, and in [onSelect] add
 * [MonarchAiMentionItem.contextItem] to `MonarchAiContextChips` and remove
 * the query with [MonarchMentionQuery.replacing]. Focus stays in the prompt;
 * "Searching…", the number of results, the empty text and the error are
 * announced through a polite live region.
 *
 * @param query the @ query being typed, or `null` to close the menu. Nothing is drawn without it, or when a query
 * with a space found nothing.
 * @param items the results for [query]. Grouped under headings when more than one kind is present.
 * @param onSelect called with the chosen result.
 * @param modifier the modifier of the strip.
 * @param status loading, ready or failed.
 * @param onRetry repeats a failed search. Adds a Retry button.
 * @param kindIcon an icon for documents, records and apps, decorative. Monarch bundles no icons; without it the
 * row shows the first letter on a tinted tile. People always show their avatar.
 * @param labels replacements for the built-in strings.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchAiMentionContext(
    query: MonarchMentionQuery?,
    items: List<MonarchAiMentionItem>,
    onSelect: (MonarchAiMentionItem) -> Unit,
    modifier: Modifier = Modifier,
    status: MonarchMentionPickerStatus = MonarchMentionPickerStatus.Ready,
    onRetry: (() -> Unit)? = null,
    kindIcon: (@Composable (MonarchAiMentionKind) -> Unit)? = MonarchDefaultIcons.mentionKind,
    labels: MonarchAiMentionContextLabels = MonarchAiMentionContextLabels(),
) {
    val tokens = MonarchAiMentionContextTokens
    if (query == null || !MonarchAiMentionContextRules.isOpen(query, status, items.size)) return
    val colors = MonarchTheme.colors
    val loading = monarchString(labels.loading, R.string.monarch_aimentioncontext_loading)
    val error = monarchString(labels.error, R.string.monarch_aimentioncontext_error)
    val empty = aiMentionContextEmpty(query, labels)
    val announcement = aiMentionContextAnnouncement(status, items.size, loading, error, empty, labels)
    val name = monarchString(labels.label, R.string.monarch_aimentioncontext_label)
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
            status == MonarchMentionPickerStatus.Loading -> AiMentionContextPlaceholder(loading)
            status == MonarchMentionPickerStatus.Failed -> if (onRetry != null) {
                MonarchInlineErrorRetry(
                    message = error,
                    onRetry = onRetry,
                    modifier = Modifier.padding(tokens.Padding),
                    retryLabel = monarchString(labels.retry, R.string.monarch_aimentioncontext_retry),
                )
            } else {
                AiMentionContextNote(error)
            }
            items.isEmpty() -> AiMentionContextNote(empty)
            else -> AiMentionContextResults(items, onSelect, kindIcon, labels)
        }
    }
}

/** "Type to search" for an empty query, else "Nothing matches “q3”". */
@Composable
private fun aiMentionContextEmpty(query: MonarchMentionQuery, labels: MonarchAiMentionContextLabels): String {
    val emptyFormat = stringResource(R.string.monarch_aimentioncontext_empty)
    val typeToSearch = stringResource(R.string.monarch_aimentioncontext_type_to_search)
    val locale = LocalConfiguration.current.locales[0]
    return labels.empty?.invoke(query.text)
        ?: if (query.text.isEmpty()) typeToSearch else String.format(locale, emptyFormat, query.text)
}

/** What the live region reads: searching, the error, the empty text or the number of results. */
@Composable
@Suppress("LongParameterList")
private fun aiMentionContextAnnouncement(
    status: MonarchMentionPickerStatus,
    count: Int,
    loading: String,
    error: String,
    empty: String,
    labels: MonarchAiMentionContextLabels,
): String = when {
    status == MonarchMentionPickerStatus.Loading -> loading
    status == MonarchMentionPickerStatus.Failed -> error
    count == 0 -> empty
    else -> labels.count?.invoke(count)
        ?: pluralStringResource(R.plurals.monarch_aimentioncontext_count, count, count)
}

@Composable
private fun AiMentionContextResults(
    items: List<MonarchAiMentionItem>,
    onSelect: (MonarchAiMentionItem) -> Unit,
    kindIcon: (@Composable (MonarchAiMentionKind) -> Unit)?,
    labels: MonarchAiMentionContextLabels,
) {
    val rules = MonarchAiMentionContextRules
    val grouped = rules.grouped(items)
    val headings = MonarchAiMentionKind.entries.associateWith {
        labels.group?.invoke(it) ?: stringResource(aiMentionContextGroupName(it))
    }
    // The first result that can be picked is the active one, drawn in the assistant's tone as on the web.
    val activeKey = items.firstOrNull { it.enabled }?.let { "${it.kind}-${it.id}" }
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = MonarchAiMentionContextTokens.MaxListHeight)) {
        rules.groups(items).forEach { (kind, results) ->
            if (grouped) {
                item(key = "heading-$kind") { AiMentionContextHeading(headings.getValue(kind)) }
            }
            results.forEach { result ->
                item(key = "${result.kind}-${result.id}") {
                    AiMentionContextRow(result, onSelect, kindIcon, labels, "${result.kind}-${result.id}" == activeKey)
                }
            }
        }
    }
}

@Composable
private fun AiMentionContextHeading(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .padding(horizontal = MonarchAiMentionContextTokens.Padding, vertical = MonarchAiMentionContextTokens.Gap)
            .semantics { heading() },
        style = MonarchTheme.typography.caption,
        color = MonarchAiMentionContextTokens.noteColor(MonarchTheme.colors),
    )
}

@Composable
private fun AiMentionContextRow(
    item: MonarchAiMentionItem,
    onSelect: (MonarchAiMentionItem) -> Unit,
    kindIcon: (@Composable (MonarchAiMentionKind) -> Unit)?,
    labels: MonarchAiMentionContextLabels,
    active: Boolean,
) {
    val colors = MonarchAiMentionContextTokens.rowColors(item.enabled, MonarchTheme.colors)
    val rowFill = MonarchAiMentionContextTokens.activeFill(MonarchTheme.colors)
    val locale = LocalConfiguration.current.locales[0]
    val tokens = item.tokens?.let { count ->
        val formatted = MonarchAiContextChipsLogic.formatTokens(count, locale)
        labels.tokens?.invoke(formatted, count)
            ?: pluralStringResource(R.plurals.monarch_aimentioncontext_tokens, maxOf(0, count), formatted)
    }
    val spoken = MonarchAiMentionContextRules.spoken(item, tokens)
    MonarchListRow(
        headlineContent = {
            // Kept in the tree (TalkBack reads the row's content description) so the text can be measured.
            Text(text = item.label, style = MonarchTheme.typography.body)
        },
        modifier = Modifier
            .then(if (active) Modifier.background(rowFill) else Modifier)
            .semantics { contentDescription = spoken },
        supportingContent = item.description?.let { description ->
            {
                Text(
                    text = description,
                    modifier = Modifier.clearAndSetSemantics { },
                    style = MonarchTheme.typography.caption,
                )
            }
        },
        leadingContent = { AiMentionContextPicture(item, kindIcon, active) },
        trailingContent = tokens?.let { text ->
            {
                Text(
                    text = text,
                    modifier = Modifier.clearAndSetSemantics { },
                    style = MonarchTheme.typography.caption,
                    color = colors.detail,
                )
            }
        },
        onClick = { onSelect(item) },
        enabled = item.enabled,
    )
}

/** A person's avatar, or the kind's icon (or first letter) on a square tile. Decorative. */
@Composable
private fun AiMentionContextPicture(
    item: MonarchAiMentionItem,
    kindIcon: (@Composable (MonarchAiMentionKind) -> Unit)?,
    active: Boolean,
) {
    if (item.kind == MonarchAiMentionKind.Person) {
        MonarchAvatar(
            image = item.image,
            name = item.label,
            size = MonarchAvatarSize.Sm,
            seed = item.id,
            decorative = true,
        )
    } else {
        // A neutral tile: the web tints only the active row's icon, in the assistant's tone.
        val colors = MonarchTheme.colors
        val tint = if (active) colors.aiStrong else colors.textMuted
        val tile = MonarchAiMentionContextTokens.tileFill(active, colors)
        MonarchAvatar(
            image = {
                Box(
                    modifier = Modifier.fillMaxSize().background(tile),
                    contentAlignment = Alignment.Center,
                ) {
                    CompositionLocalProvider(LocalContentColor provides tint) { kindIcon?.invoke(item.kind) }
                }
            },
            name = item.label,
            shape = MonarchAvatarShape.Square,
            size = MonarchAvatarSize.Sm,
            decorative = true,
        )
    }
}

@Composable
private fun AiMentionContextPlaceholder(loading: String) {
    val tokens = MonarchAiMentionContextTokens
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
private fun AiMentionContextNote(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(MonarchAiMentionContextTokens.Padding),
        style = MonarchTheme.typography.body,
        color = MonarchAiMentionContextTokens.noteColor(MonarchTheme.colors),
    )
}

private fun aiMentionContextGroupName(kind: MonarchAiMentionKind): Int = when (kind) {
    MonarchAiMentionKind.Doc -> R.string.monarch_aimentioncontext_group_doc
    MonarchAiMentionKind.Person -> R.string.monarch_aimentioncontext_group_person
    MonarchAiMentionKind.Record -> R.string.monarch_aimentioncontext_group_record
    MonarchAiMentionKind.App -> R.string.monarch_aimentioncontext_group_app
}
