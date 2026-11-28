package dev.monarchui.components.intelligence.airesponseskeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import dev.monarchui.R
import dev.monarchui.components.feedback.skeleton.MonarchSkeleton
import dev.monarchui.components.feedback.skeleton.MonarchSkeletonShape
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme

/**
 * A placeholder shaped like the answer that is on its way, shown between
 * sending a prompt and the first streamed token: paragraph lines, a column of
 * cards or a table.
 *
 * Built from `MonarchSkeleton` bars in a `Column`: Material 3 has no
 * placeholder. The bars shimmer, and stand still when the user has turned
 * animations off. The whole placeholder is one TalkBack element read as
 * [label] and reported as an indeterminate progress, so it is read as busy;
 * the bars are not read. It announces nothing: the conversation announces its
 * own progress.
 *
 * @param modifier the modifier of the placeholder. It fills the available width.
 * @param shape the shape of the answer expected. Pick the one that matches, so the layout does not jump when
 * the answer arrives.
 * @param lines the number of text lines, cards or table body rows. `null` gives 3 for text and cards and 4 for
 * a table. Values below 1 draw 1.
 * @param columns the number of table columns. Values below 1 draw 1. Only used by the table shape.
 * @param label the text TalkBack reads. `null` uses the built-in string `monarch_airesponseskeleton_label`,
 * "Generating response…".
 */
@Composable
public fun MonarchAiResponseSkeleton(
    modifier: Modifier = Modifier,
    shape: MonarchAiResponseSkeletonShape = MonarchAiResponseSkeletonShape.Text,
    lines: Int? = null,
    columns: Int = 3,
    label: String? = null,
) {
    val spoken = monarchString(label, R.string.monarch_airesponseskeleton_label)
    val tokens = MonarchAiResponseSkeletonTokens
    val count = tokens.count(shape, lines)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = spoken
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            },
    ) {
        when (shape) {
            MonarchAiResponseSkeletonShape.Text -> TextLines(count)
            MonarchAiResponseSkeletonShape.Cards -> Cards(count)
            MonarchAiResponseSkeletonShape.Table -> Table(count, tokens.columns(columns))
        }
    }
}

@Composable
private fun TextLines(count: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(MonarchAiResponseSkeletonTokens.Gap)) {
        repeat(count) { index ->
            Bar(MonarchAiResponseSkeletonTokens.lineWidth(index, count))
        }
    }
}

@Composable
private fun Cards(count: Int) {
    val colors = MonarchAiResponseSkeletonTokens.cardColors(MonarchTheme.colors)
    Column(verticalArrangement = Arrangement.spacedBy(MonarchAiResponseSkeletonTokens.Gap)) {
        repeat(count) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .monarchSurface(colors, MonarchShapeDefaults.Container)
                    .padding(MonarchAiResponseSkeletonTokens.CardPadding),
                verticalArrangement = Arrangement.spacedBy(MonarchAiResponseSkeletonTokens.InnerGap),
            ) {
                MonarchAiResponseSkeletonTokens.CardLines.forEach { width -> Bar(width) }
            }
        }
    }
}

@Composable
private fun Table(rows: Int, columns: Int) {
    Column {
        // The header row first (-1), then the body rows.
        for (row in -1 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = MonarchAiResponseSkeletonTokens.RowPadding),
                horizontalArrangement = Arrangement.spacedBy(MonarchAiResponseSkeletonTokens.InnerGap),
            ) {
                repeat(columns) { column ->
                    val width = if (row < 0) {
                        MonarchAiResponseSkeletonTokens.HEADER_WIDTH
                    } else {
                        MonarchAiResponseSkeletonTokens.cellWidth(row, column)
                    }
                    Box(modifier = Modifier.weight(1f)) { Bar(width) }
                }
            }
            if (row < rows - 1) MonarchDivider(variant = MonarchDividerVariant.Subtle)
        }
    }
}

@Composable
private fun Bar(width: Float) {
    MonarchSkeleton(modifier = Modifier.fillMaxWidth(width), shape = MonarchSkeletonShape.Text)
}

