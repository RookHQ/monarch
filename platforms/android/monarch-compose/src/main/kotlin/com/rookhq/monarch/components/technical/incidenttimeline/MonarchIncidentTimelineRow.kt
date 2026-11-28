package com.rookhq.monarch.components.technical.incidenttimeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadge
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadgeSize
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.components.datadisplay.tag.MonarchTag
import com.rookhq.monarch.components.datadisplay.tag.MonarchTagSize
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.theme.MonarchTheme

/** One update with what it shows and what TalkBack reads. */
internal class MonarchIncidentTimelineRow(
    val update: MonarchIncidentUpdate,
    val definition: MonarchStatusDefinition,
    val time: String,
    val automated: String?,
    val spoken: String,
)

/** One update: the dot on the line, the stage, time, author and Automated, then the text. Read as one element. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MonarchIncidentTimelineUpdate(
    row: MonarchIncidentTimelineRow,
    density: MonarchIncidentTimelineDensity,
    column: Dp,
    last: Boolean,
) {
    val colors = MonarchTheme.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val line = MonarchIncidentTimelineTokens.line(colors)
    val update = row.update
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (last) return@drawBehind
                val half = column.toPx() / 2
                val x = if (rtl) size.width - half else half
                val top = half + MonarchIncidentTimelineTokens.Dot.toPx() / 2
                drawLine(line, Offset(x, top), Offset(x, size.height), MonarchIncidentTimelineTokens.Line.toPx())
            }
            .padding(bottom = MonarchIncidentTimelineTokens.rowGap(density, last))
            .semantics(mergeDescendants = true) { contentDescription = row.spoken },
        horizontalArrangement = Arrangement.spacedBy(MonarchIncidentTimelineTokens.ColumnGap),
    ) {
        Box(modifier = Modifier.size(column).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(MonarchIncidentTimelineTokens.Dot)
                    .background(MonarchIncidentTimelineTokens.marker(row.definition.tone, colors), CircleShape),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MonarchIncidentTimelineTokens.LineGap),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MonarchIncidentTimelineTokens.MetaGap),
                verticalArrangement = Arrangement.spacedBy(MonarchIncidentTimelineTokens.LineGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchStatusBadge(definition = row.definition, size = MonarchStatusBadgeSize.Sm)
                MonarchText(
                    text = row.time,
                    style = MonarchTextStyle.Caption,
                    color = MonarchIncidentTimelineTokens.muted(colors),
                )
                update.author?.takeIf { it.isNotEmpty() }?.let {
                    MonarchText(
                        text = it,
                        style = MonarchTextStyle.Caption,
                        color = MonarchIncidentTimelineTokens.muted(colors),
                    )
                }
                row.automated?.let { MonarchTag(text = it, size = MonarchTagSize.Sm) }
            }
            if (update.body.isNotEmpty()) MonarchText(text = update.body)
        }
    }
}
