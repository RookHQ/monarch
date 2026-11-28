package dev.monarchui.components.workflow.orgchart

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * The list layout, the phone form: every visible person as a card, indented
 * under their manager, with the team chip at the start. It fills the height
 * its parent gives it and scrolls.
 */
@Composable
internal fun MonarchOrgChartListView(context: MonarchOrgChartContext, modifier: Modifier = Modifier) {
    val lines = context.tree.visible(context.open)
    val state = rememberLazyListState()
    LaunchedEffect(context.focusId) {
        val index = lines.indexOfFirst { it.first.id == context.focusId }
        if (index >= 0) state.animateScrollToItem(index)
    }
    val rail = MonarchOrgChartTokens.lineColor(MonarchTheme.colors)
    LazyColumn(
        modifier = modifier.onGloballyPositioned { context.positions.area = monarchOrgChartWindowBounds(it) },
        state = state,
        contentPadding = PaddingValues(vertical = MonarchOrgChartTokens.ListGap),
    ) {
        items(lines, key = { it.first.id }) { (node, depth) ->
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Box(
                    modifier = Modifier
                        .widthIn(max = MonarchOrgChartTokens.ListWidth)
                        .fillMaxWidth()
                        .drawBehind {
                            for (level in 1..depth.coerceAtMost(MonarchOrgChartTokens.LIST_MAX_INDENT_LEVELS)) {
                                val indent = MonarchOrgChartTokens.listIndent(level - 1)
                                val x = (indent + MonarchOrgChartTokens.RailOffset).toPx()
                                drawLine(
                                    rail,
                                    Offset(x, 0f),
                                    Offset(x, size.height),
                                    strokeWidth = MonarchTokens.Space.px.dp.toPx(),
                                )
                            }
                        },
                ) {
                    Column(
                        modifier = Modifier
                            .padding(start = MonarchOrgChartTokens.listIndent(depth))
                            .padding(
                                bottom = if (context.tree.reportCount(node) > 0) {
                                    MonarchTokens.Space.space0.dp
                                } else {
                                    MonarchOrgChartTokens.ListGap
                                },
                            ),
                    ) {
                        MonarchOrgChartCard(node, context, Modifier.fillMaxWidth())
                        if (context.tree.reportCount(node) > 0) {
                            MonarchOrgChartChip(
                                node,
                                context,
                                Modifier
                                    .padding(start = MonarchOrgChartTokens.ChipInset)
                                    .offset(y = -MonarchOrgChartTokens.ChipOverlap),
                            )
                        }
                    }
                }
            }
        }
    }
}
