package com.rookhq.monarch.components.charts.barlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.typography.tabularnumberstyle.MonarchTabularNumberStyle
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchChartValueFormat
import com.rookhq.monarch.theme.MonarchTheme
import java.text.NumberFormat

/**
 * A ranked list with a quiet bar under each row's label and value: top
 * pages, top referrers, rivals by score.
 *
 * Built by Monarch: Material 3 has no equivalent. Labels and values are real
 * text and each row is one TalkBack element; the bars and rank numbers are
 * decoration, and the order of the rows carries the rank. The bar grows from
 * the start edge, so it is mirrored in right-to-left layouts. The rows are a
 * plain column, not a lazy list, so the list can sit inside a scrolling
 * screen; use [limit] for long data.
 *
 * @param items the rows.
 * @param modifier the modifier of the list.
 * @param sort the order of the rows. Rows without a value go last.
 * @param maximum the value of a full bar. `null` uses the largest value. Pass 1 for ratios of a whole.
 * @param limit shows only the first rows, with a "Top 5 of 12" footer. The scale still uses every item.
 * @param ranked numbers the rows.
 * @param loading shows a progress indicator and the loading text instead of the rows.
 * @param label names the list for TalkBack, such as "Top pages".
 * @param formatValue writes a value. `null` writes a number in the current locale.
 * @param onSelect makes every row a button that is called with its item.
 * @param missingLabel replaces the built-in text "No value" read for a missing value (`monarch_barlist_missing`).
 * @param emptyLabel replaces the built-in text "No data yet" (`monarch_barlist_empty`).
 * @param loadingLabel replaces the built-in text "Loading" (`monarch_barlist_loading`).
 * @param overflowLabel replaces the built-in footer "Top 5 of 12" (`monarch_barlist_overflow`).
 */
@Composable
public fun MonarchBarList(
    items: List<MonarchBarListItem>,
    modifier: Modifier = Modifier,
    sort: MonarchBarListSort = MonarchBarListSort.Descending,
    maximum: Double? = null,
    limit: Int? = null,
    ranked: Boolean = false,
    loading: Boolean = false,
    label: String? = null,
    formatValue: ((Double) -> String)? = null,
    onSelect: ((MonarchBarListItem) -> Unit)? = null,
    missingLabel: String? = null,
    emptyLabel: String? = null,
    loadingLabel: String? = null,
    overflowLabel: String? = null,
) {
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    if (loading) {
        val text = monarchString(loadingLabel, R.string.monarch_barlist_loading)
        Row(
            modifier = modifier.semantics(mergeDescendants = true) { },
            horizontalArrangement = Arrangement.spacedBy(MonarchBarListTokens.IconGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(MonarchBarListTokens.RowGap)
                    .clearAndSetSemantics { },
                color = colors.accentSolid,
                trackColor = colors.bgEmphasis,
            )
            Text(
                text = text,
                style = MonarchBarListTokens.bodyStyle(typography),
                color = MonarchBarListTokens.mutedColor(colors),
            )
        }
        return
    }
    if (items.isEmpty()) {
        Text(
            text = monarchString(emptyLabel, R.string.monarch_barlist_empty),
            modifier = modifier,
            style = MonarchBarListTokens.bodyStyle(typography),
            color = MonarchBarListTokens.mutedColor(colors),
        )
        return
    }
    val write = rememberMonarchChartValueFormat(formatValue)
    val sorted = remember(items, sort) { MonarchBarListLayout.sorted(items, sort) }
    val shown = if (limit != null && limit >= 0) sorted.take(limit) else sorted
    val scale = MonarchBarListLayout.scale(items, maximum)
    val missing = monarchString(missingLabel, R.string.monarch_barlist_missing)
    Column(
        modifier = modifier.semantics {
            collectionInfo = CollectionInfo(rowCount = shown.size, columnCount = 1)
            if (label != null) contentDescription = label
        },
        verticalArrangement = Arrangement.spacedBy(MonarchBarListTokens.RowGap),
    ) {
        shown.forEachIndexed { position, item ->
            BarListRow(
                item = item,
                position = position,
                fraction = MonarchBarListLayout.fraction(MonarchBarListLayout.valueOf(item), scale),
                value = item.displayValue ?: MonarchBarListLayout.valueOf(item)?.let(write),
                missing = missing,
                ranked = ranked,
                onSelect = onSelect,
            )
        }
        if (shown.size < sorted.size) {
            Text(
                text = overflowLabel ?: stringResource(R.string.monarch_barlist_overflow, shown.size, sorted.size),
                style = typography.caption,
                color = MonarchBarListTokens.mutedColor(colors),
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun BarListRow(
    item: MonarchBarListItem,
    position: Int,
    fraction: Float,
    value: String?,
    missing: String,
    ranked: Boolean,
    onSelect: ((MonarchBarListItem) -> Unit)?,
) {
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    val weight = if (item.highlighted) FontWeight.SemiBold else null
    val shape = MonarchShapeDefaults.Row
    val interaction = if (onSelect != null) {
        Modifier
            .heightIn(min = MonarchTouchTarget.Minimum)
            .monarchFocusRing(shape)
            .clickable(role = Role.Button) { onSelect(item) }
    } else {
        Modifier.heightIn(min = MonarchBarListTokens.RowMinHeight)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                collectionItemInfo = CollectionItemInfo(position, 1, 0, 1)
            }
            .then(interaction),
        horizontalArrangement = Arrangement.spacedBy(MonarchBarListTokens.ContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (ranked) {
            Text(
                text = (position + 1).toString(),
                modifier = Modifier
                    .widthIn(min = MonarchBarListTokens.RankMinWidth)
                    .clearAndSetSemantics { },
                style = typography.caption,
                color = MonarchBarListTokens.mutedColor(colors),
                textAlign = TextAlign.End,
            )
        }
        // The label row on top, a quiet full-width track under it: the bar is decoration, it starts at the
        // start edge in both directions and its filled share is the value over the scale.
        Column(
            modifier = Modifier.weight(1f).padding(vertical = MonarchBarListTokens.BarVerticalPadding),
            verticalArrangement = Arrangement.spacedBy(MonarchBarListTokens.TrackGap),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchBarListTokens.IconGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item.icon?.let { icon ->
                    Box(Modifier.clearAndSetSemantics { }) {
                        CompositionLocalProvider(LocalContentColor provides MonarchBarListTokens.mutedColor(colors)) {
                            icon()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.label,
                        style = MonarchBarListTokens.bodyStyle(typography),
                        fontWeight = weight,
                        color = MonarchBarListTokens.labelColor(colors),
                    )
                    item.description?.let {
                        Text(text = it, style = typography.caption, color = MonarchBarListTokens.mutedColor(colors))
                    }
                }
                if (value != null) {
                    Text(
                        text = value,
                        style = MonarchTabularNumberStyle.applyTo(MonarchBarListTokens.bodyStyle(typography)),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textDefault,
                    )
                } else {
                    Text(
                        text = MissingDash,
                        // The dash is replaced by the missing label as text, so it is read in its place in the row.
                        modifier = Modifier.clearAndSetSemantics { text = AnnotatedString(missing) },
                        style = MonarchBarListTokens.bodyStyle(typography),
                        color = MonarchBarListTokens.mutedColor(colors),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MonarchBarListTokens.TrackHeight)
                    .background(MonarchBarListTokens.trackColor(colors), MonarchShapeDefaults.Mark),
            ) {
                if (fraction > 0f) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(MonarchBarListTokens.barColor(colors), MonarchShapeDefaults.Mark),
                    )
                }
            }
        }
    }
}

/** An en dash for a missing value; TalkBack reads the missing label instead. */
private const val MissingDash = "–"
