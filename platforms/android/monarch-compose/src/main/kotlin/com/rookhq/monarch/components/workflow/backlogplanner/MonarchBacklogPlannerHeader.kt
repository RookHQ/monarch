package com.rookhq.monarch.components.workflow.backlogplanner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.theme.MonarchTheme

private typealias HeaderTokens = MonarchBacklogPlannerTokens

/** What a bucket header shows besides the bucket itself. */
internal class MonarchBacklogPlannerHeaderState(
    val expanded: Boolean,
    val points: Int?,
    val unestimated: Int,
)

/**
 * A bucket's header: a button that opens and closes the bucket (a heading
 * with the title and subtitle), the badge and actions, the summary, and the
 * capacity meter with its over-capacity note.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MonarchBacklogPlannerHeader(
    bucket: MonarchBacklogBucket<*>,
    state: MonarchBacklogPlannerHeaderState,
    onToggle: () -> Unit,
    text: MonarchBacklogPlannerText,
    collapseIcon: (@Composable () -> Unit)?,
) {
    val colors = MonarchTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = HeaderTokens.BucketGap, bottom = HeaderTokens.LineGap),
        verticalArrangement = Arrangement.spacedBy(HeaderTokens.LineGap),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(HeaderTokens.PartGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MonarchTouchTarget.Minimum)
                    .clickable(role = Role.Button, onClick = onToggle)
                    .semantics {
                        heading()
                        stateDescription = if (state.expanded) text.expanded else text.collapsed
                        if (state.expanded) {
                            collapse {
                                onToggle()
                                true
                            }
                        } else {
                            expand {
                                onToggle()
                                true
                            }
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(HeaderTokens.PartGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (collapseIcon != null) {
                    Box(Modifier.rotate(if (state.expanded) HeaderTokens.EXPANDED_ROTATION else 0f)) { collapseIcon() }
                }
                // One line, as on the web: the title, then the dates in the quiet colour.
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(HeaderTokens.PartGap),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    MonarchText(text = bucket.title, style = MonarchTextStyle.BodyStrong)
                    bucket.subtitle?.let {
                        MonarchText(text = it, style = MonarchTextStyle.Caption, color = HeaderTokens.meta(colors))
                    }
                }
            }
            bucket.badge?.invoke()
            bucket.actions?.invoke(this)
        }
        val capacity = bucket.capacity
        val points = state.points
        val summary = text.summary(bucket.items.size, points, state.unestimated)
        if (capacity != null && points != null) {
            val label = text.capacity(points, capacity)
            // The summary, the capacity bar and its figure on one compact line, as on the web.
            Row(
                horizontalArrangement = Arrangement.spacedBy(HeaderTokens.PartGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchText(text = summary, style = MonarchTextStyle.Caption, color = HeaderTokens.meta(colors))
                CapacityBar(HeaderTokens.fill(points, capacity), label, Modifier.weight(1f))
                MonarchText(
                    text = label,
                    modifier = Modifier.clearAndSetSemantics { },
                    style = MonarchTextStyle.Caption,
                    color = HeaderTokens.meta(colors),
                )
            }
            if (points > capacity) {
                MonarchText(
                    text = text.overCapacity(points - capacity),
                    style = MonarchTextStyle.Caption,
                    color = HeaderTokens.overCapacity(colors),
                )
            }
        } else {
            MonarchText(text = summary, style = MonarchTextStyle.Caption, color = HeaderTokens.meta(colors))
        }
    }
}

/** The capacity meter: a thin bar in the dark neutral, on the muted track, read as a progress bar. */
@Composable
private fun CapacityBar(fraction: Float, label: String, modifier: Modifier) {
    val colors = MonarchTheme.colors
    Box(
        modifier = modifier
            .height(HeaderTokens.BarHeight)
            .clip(CircleShape)
            .background(colors.bgEmphasis)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            },
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(colors.textMuted, CircleShape),
        )
    }
}
