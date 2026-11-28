package dev.monarchui.components.navigation.tableofcontents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import dev.monarchui.R
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

/**
 * In-page navigation built from the page's headings: a list of entries that
 * marks the section being read and asks the app to scroll to the one chosen.
 *
 * Built by Monarch: Compose has no contents list. On a phone, put it in a
 * `ModalBottomSheet` opened from the top app bar; on a large screen, beside
 * the content. Keep [selectedId] in step with scrolling with
 * [rememberMonarchTableOfContentsSpy].
 *
 * Each entry is a link-like element with a 48dp touch target, the current one
 * marked by an accent line and a heavier weight as well as by colour, and by
 * the state "Current section" for TalkBack.
 *
 * @param items the entries in page order.
 * @param selectedId the id of the section being read, or `null` before the first heading.
 * @param onSelect called with the id of the chosen entry. Scroll to it, for example with
 * `LazyListState.animateScrollToItem`, set it as the selection and close the sheet.
 * @param modifier the modifier of the list.
 * @param enabled whether the entries can be chosen. When not, they use the disabled text colour.
 * @param showTitle whether the title is shown. When hidden it still names the list for TalkBack.
 * @param title replaces the built-in title "On this page" (`monarch_tableofcontents_title`).
 * @param currentLabel replaces the built-in "Current section" (`monarch_tableofcontents_current`), read as
 * the state of the current entry.
 */
@Composable
public fun MonarchTableOfContents(
    items: List<MonarchTableOfContentsItem>,
    selectedId: String?,
    onSelect: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showTitle: Boolean = true,
    title: String? = null,
    currentLabel: String? = null,
) {
    val tokens = MonarchTableOfContentsTokens
    val heading = monarchString(title, R.string.monarch_tableofcontents_title)
    val current = monarchString(currentLabel, R.string.monarch_tableofcontents_current)
    Column(
        modifier = modifier.semantics {
            isTraversalGroup = true
            if (!showTitle) contentDescription = heading
        },
    ) {
        if (showTitle) {
            MonarchText(
                text = heading,
                modifier = Modifier.padding(bottom = tokens.TitleGap),
                style = MonarchTextStyle.Label,
                color = MonarchTheme.colors.textMuted,
                heading = true,
            )
        }
        items.forEach { item ->
            TableOfContentsEntry(
                item = item,
                depth = tokens.depth(item.level, items),
                current = item.id == selectedId,
                enabled = enabled,
                currentLabel = current,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun TableOfContentsEntry(
    item: MonarchTableOfContentsItem,
    depth: Int,
    current: Boolean,
    enabled: Boolean,
    currentLabel: String,
    onSelect: (String) -> Unit,
) {
    val tokens = MonarchTableOfContentsTokens
    val colors = tokens.entryColors(current, enabled, MonarchTheme.colors)
    val shape = MonarchTheme.shapes.xs
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = MonarchTouchTarget.Minimum)
            .monarchFocusRing(shape)
            .clickable(enabled = enabled) { onSelect(item.id) }
            .semantics { if (current) stateDescription = currentLabel },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .padding(start = tokens.IndentStep * depth)
                .width(tokens.LineWidth)
                .fillMaxHeight()
                .background(colors.line),
        )
        Text(
            text = item.title,
            modifier = Modifier
                .weight(1f)
                .padding(start = tokens.TextPadding, top = tokens.VerticalPadding, bottom = tokens.VerticalPadding),
            style = MonarchTheme.typography.body,
            color = colors.text,
            fontWeight = if (current) FontWeight(MonarchTokens.FontWeight.semibold) else null,
        )
    }
}
