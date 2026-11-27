package com.rookhq.monarch.components.layout.marketingsection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A few figures side by side, each a large value over a muted label, wrapping onto more lines when the screen is
 * narrow. Each figure is one TalkBack element, read as the value then the label.
 *
 * @param items the figures, in reading order.
 * @param modifier the modifier of the strip.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchStatStrip(
    items: List<MonarchStatStripItem>,
    modifier: Modifier = Modifier,
) {
    val colors = MonarchTheme.colors
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MonarchMarketingSectionTokens.StatGap),
        verticalArrangement = Arrangement.spacedBy(MonarchMarketingSectionTokens.GridGap),
    ) {
        items.forEach { item ->
            Column(Modifier.semantics(mergeDescendants = true) {}) {
                MonarchText(
                    text = item.value,
                    style = MonarchTextStyle.Title,
                    color = MonarchMarketingSectionTokens.statValue(colors),
                    heading = false,
                )
                MonarchText(text = item.label, style = MonarchTextStyle.Label, color = colors.textMuted)
            }
        }
    }
}
