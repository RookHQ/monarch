package com.rookhq.monarch.components.datadisplay.descriptionlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.copybutton.MonarchCopyButton
import com.rookhq.monarch.components.datadisplay.truncatedtext.MonarchTruncatedText
import com.rookhq.monarch.internal.MonarchEmptyValueText
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Key and value facts of a record: owner, stage, created, plan.
 *
 * Built by Monarch from columns and rows: Material 3 has no description list.
 * Each item is one TalkBack element read as the term and then the value
 * ("Owner, Ada Lovelace"); a copy button or a "Show more" button in the value
 * stays a separate element. An empty value shows a dash read as "Not set".
 * With several [columns] the items fill rows of that many; below 288dp every
 * item takes its own row.
 *
 * @param items the facts, in order.
 * @param modifier the modifier of the list.
 * @param layout `Stacked` puts each term above its value; `Inline` puts them side by side and stacks again
 * when the list is narrower than 288dp or the font scale is 1.5 or more.
 * @param columns how many items share a row from 288dp wide. Wide items always take a whole row.
 * @param copyIcon a copy icon. Given, copy buttons are icon buttons; otherwise they are small text buttons.
 * @param emptyLabel replaces the built-in spoken text of an empty value, "Not set"
 * (`monarch_descriptionlist_empty`).
 * @param showMoreLabel replaces the built-in "Show more" of values with a line limit.
 * @param showLessLabel replaces the built-in "Show less" of values with a line limit.
 */
@Composable
public fun MonarchDescriptionList(
    items: List<MonarchDescriptionItem>,
    modifier: Modifier = Modifier,
    layout: MonarchDescriptionListLayout = MonarchDescriptionListLayout.Stacked,
    columns: Int = 1,
    copyIcon: (@Composable () -> Unit)? = null,
    emptyLabel: String? = null,
    showMoreLabel: String? = null,
    showLessLabel: String? = null,
) {
    val empty = monarchString(emptyLabel, R.string.monarch_descriptionlist_empty)
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier = modifier) {
        val width = maxWidth.value
        val effectiveLayout = MonarchDescriptionListTokens.layout(layout, width, fontScale)
        val count = MonarchDescriptionListTokens.columns(columns, width)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MonarchDescriptionListTokens.RowGap),
        ) {
            MonarchDescriptionListTokens.rows(items, count).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MonarchDescriptionListTokens.ColumnGap),
                ) {
                    row.forEach { index ->
                        Item(
                            item = items[index],
                            layout = effectiveLayout,
                            emptyLabel = empty,
                            copyIcon = copyIcon,
                            showMoreLabel = showMoreLabel,
                            showLessLabel = showLessLabel,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    val wide = row.size == 1 && items[row.first()].wide
                    if (!wide) repeat(count - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun Item(
    item: MonarchDescriptionItem,
    layout: MonarchDescriptionListLayout,
    emptyLabel: String,
    copyIcon: (@Composable () -> Unit)?,
    showMoreLabel: String?,
    showLessLabel: String?,
    modifier: Modifier,
) {
    val colors = MonarchTheme.colors
    val term = @Composable { termModifier: Modifier ->
        Text(
            text = item.term,
            modifier = termModifier,
            style = MonarchTheme.typography.caption,
            color = MonarchDescriptionListTokens.termColor(colors),
        )
    }
    val merged = modifier.semantics(mergeDescendants = true) { }
    if (layout == MonarchDescriptionListLayout.Inline) {
        Row(
            modifier = merged,
            horizontalArrangement = Arrangement.spacedBy(MonarchDescriptionListTokens.ColumnGap),
            verticalAlignment = Alignment.Top,
        ) {
            term(Modifier.weight(MonarchDescriptionListTokens.INLINE_TERM_WEIGHT))
            Value(
                item,
                emptyLabel,
                copyIcon,
                showMoreLabel,
                showLessLabel,
                Modifier.weight(MonarchDescriptionListTokens.INLINE_VALUE_WEIGHT),
            )
        }
    } else {
        Column(
            modifier = merged,
            verticalArrangement = Arrangement.spacedBy(MonarchDescriptionListTokens.TermGap),
        ) {
            term(Modifier)
            Value(item, emptyLabel, copyIcon, showMoreLabel, showLessLabel, Modifier.fillMaxWidth())
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun Value(
    item: MonarchDescriptionItem,
    emptyLabel: String,
    copyIcon: (@Composable () -> Unit)?,
    showMoreLabel: String?,
    showLessLabel: String?,
    modifier: Modifier,
) {
    val colors = MonarchTheme.colors
    val empty = MonarchDescriptionListTokens.isEmpty(item)
    val copyText = MonarchDescriptionListTokens.copyText(item)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MonarchDescriptionListTokens.TermGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f, fill = copyText != null)) {
            when {
                // The word is the text of the node, not a content description, so
                // the merged row reads "Label, Not set" and keeps the label.
                empty -> MonarchEmptyValueText(
                    spokenLabel = emptyLabel,
                    style = MonarchTheme.typography.body,
                    color = MonarchDescriptionListTokens.valueColor(empty = true, colors),
                )
                item.content != null -> item.content.invoke()
                item.maxLines != null -> MonarchTruncatedText(
                    text = item.value.orEmpty(),
                    maxLines = item.maxLines,
                    showMoreLabel = showMoreLabel,
                    showLessLabel = showLessLabel,
                )
                else -> Text(
                    text = item.value.orEmpty(),
                    style = MonarchTheme.typography.body,
                    color = MonarchDescriptionListTokens.valueColor(empty = false, colors),
                )
            }
        }
        if (copyText != null) {
            if (copyIcon != null) {
                MonarchCopyButton(value = copyText, icon = copyIcon, target = item.term)
            } else {
                MonarchCopyButton(
                    value = copyText,
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                    target = item.term,
                )
            }
        }
    }
}
