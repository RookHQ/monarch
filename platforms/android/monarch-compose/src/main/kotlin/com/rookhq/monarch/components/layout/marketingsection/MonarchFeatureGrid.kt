package com.rookhq.monarch.components.layout.marketingsection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Features as outlined cards in a grid: a title, a description and an optional icon on each.
 *
 * Built on `MonarchCard`. The grid shows [columns] cards per row while each card can stay at least
 * 160dp wide, and fewer on a narrow screen, down to one. Cards in a row share the height of the tallest.
 *
 * @param items the features, in reading order.
 * @param modifier the modifier of the grid.
 * @param columns the most cards per row.
 */
@Composable
public fun MonarchFeatureGrid(
    items: List<MonarchFeatureGridItem>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
) {
    val gap = MonarchMarketingSectionTokens.GridGap
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val fit = ((maxWidth + gap) / (MonarchMarketingSectionTokens.MinCardWidth + gap)).toInt()
        val perRow = columns.coerceAtMost(fit).coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            items.chunked(perRow).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    row.forEach { item ->
                        FeatureCard(item, Modifier.weight(1f).fillMaxHeight())
                    }
                    repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(item: MonarchFeatureGridItem, modifier: Modifier) {
    val colors = MonarchTheme.colors
    MonarchCard(
        modifier = modifier.semantics(mergeDescendants = true) {},
        contentPadding = PaddingValues(MonarchMarketingSectionTokens.CardPadding),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchMarketingSectionTokens.CardGap)) {
            item.icon?.let { icon ->
                CompositionLocalProvider(LocalContentColor provides colors.accentStrong, content = icon)
            }
            MonarchText(text = item.title, style = MonarchTextStyle.BodyStrong, heading = true)
            if (item.description != null) {
                MonarchText(text = item.description, style = MonarchTextStyle.Label, color = colors.textMuted)
            }
        }
    }
}
