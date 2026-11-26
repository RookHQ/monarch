package com.rookhq.monarch.components.intelligence.aicontextdivider

import android.icu.text.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.collapsible.MonarchCollapsible
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Date
import java.util.TimeZone

/**
 * A labelled rule between messages in a thread: a calendar day, a context reset, or a point where earlier
 * messages were summarized.
 *
 * Built by Monarch from a row of lines and a label; Material 3 has no
 * labelled divider. A context reset has dashed lines and the line "The
 * assistant does not see the messages above this line." A summarized
 * divider can show the summary the assistant now works from behind a
 * `MonarchCollapsible`, and a button that shows the original messages.
 * Without controls the divider is one TalkBack element read as its label and
 * description; with them, the label, the disclosure and the button are
 * separate elements.
 *
 * @param kind what the divider marks.
 * @param modifier the modifier of the divider.
 * @param dateMillis the day of a `Day` divider, in epoch milliseconds (UTC). Labelled "Today", "Yesterday",
 * a weekday and date in this year, or a full date, in the device time zone. Without it a day divider has an
 * empty label and shows only the lines.
 * @param nowMillis the reference time for "Today" and "Yesterday". `null` is the current time.
 * @param summary for `Summarized`: the summary the assistant now works from, shown behind a disclosure.
 * @param onExpand for `Summarized`: shows a button that calls this to reveal the original messages.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchAiContextDivider(
    kind: MonarchAiContextDividerKind,
    modifier: Modifier = Modifier,
    dateMillis: Long? = null,
    nowMillis: Long? = null,
    summary: String? = null,
    onExpand: (() -> Unit)? = null,
    labels: MonarchAiContextDividerLabels = MonarchAiContextDividerLabels(),
) {
    val colors = MonarchTheme.colors
    val label = labels.label ?: kindLabel(kind, dateMillis, nowMillis, labels)
    val description = if (kind == MonarchAiContextDividerKind.ContextReset) {
        monarchString(labels.detail, R.string.monarch_aicontextdivider_context_reset_description)
    } else {
        null
    }
    val interactive = MonarchAiContextDividerTokens.interactive(kind, summary != null, onExpand != null)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MonarchAiContextDividerTokens.VerticalMargin)
            .then(
                if (interactive) {
                    Modifier
                } else {
                    val spoken = if (description == null) {
                        label
                    } else {
                        stringResource(R.string.monarch_aicontextdivider_described, label, description)
                    }
                    Modifier.clearAndSetSemantics { contentDescription = spoken }
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MonarchAiContextDividerTokens.VerticalGap),
    ) {
        Rule(kind, label, colors)
        if (description != null) {
            Text(
                text = description,
                style = MonarchTheme.typography.caption,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
            )
        }
        if (interactive) Controls(summary, onExpand, labels, colors)
    }
}

@Composable
private fun kindLabel(
    kind: MonarchAiContextDividerKind,
    dateMillis: Long?,
    nowMillis: Long?,
    labels: MonarchAiContextDividerLabels,
): String = when (kind) {
    MonarchAiContextDividerKind.Day -> if (dateMillis == null) "" else dayLabel(dateMillis, nowMillis, labels)
    MonarchAiContextDividerKind.ContextReset -> stringResource(R.string.monarch_aicontextdivider_context_reset)
    MonarchAiContextDividerKind.Summarized -> stringResource(R.string.monarch_aicontextdivider_summarized)
}

@Composable
private fun dayLabel(dateMillis: Long, nowMillis: Long?, labels: MonarchAiContextDividerLabels): String {
    val locale = LocalConfiguration.current.locales[0]
    val now = nowMillis ?: remember { System.currentTimeMillis() }
    val relation = MonarchAiContextDividerTokens.relation(dateMillis, now, TimeZone.getDefault())
    return when (relation) {
        MonarchAiDayRelation.Today -> monarchString(labels.today, R.string.monarch_aicontextdivider_today)
        MonarchAiDayRelation.Yesterday -> monarchString(labels.yesterday, R.string.monarch_aicontextdivider_yesterday)
        MonarchAiDayRelation.ThisYear, MonarchAiDayRelation.OtherYear -> remember(dateMillis, relation, locale) {
            val skeleton = if (relation == MonarchAiDayRelation.ThisYear) {
                MonarchAiContextDividerTokens.THIS_YEAR_SKELETON
            } else {
                MonarchAiContextDividerTokens.OTHER_YEAR_SKELETON
            }
            DateFormat.getInstanceForSkeleton(skeleton, locale).format(Date(dateMillis))
        }
    }
}

@Composable
private fun Rule(kind: MonarchAiContextDividerKind, label: String, colors: MonarchColors) {
    val lineColor = MonarchAiContextDividerTokens.lineColor(kind, colors)
    val dashed = MonarchAiContextDividerTokens.dashed(kind)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MonarchAiContextDividerTokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Line(lineColor, dashed)
        if (label.isNotEmpty()) {
            Text(
                text = label,
                modifier = Modifier.weight(1f, fill = false),
                style = MonarchTheme.typography.caption,
                fontWeight = FontWeight(MonarchAiContextDividerTokens.FONT_WEIGHT),
                color = MonarchAiContextDividerTokens.labelColor(kind, colors),
                textAlign = TextAlign.Center,
            )
        }
        Line(lineColor, dashed)
    }
}

/** One hairline, at least 16dp long, solid or dashed. */
@Composable
private fun RowScope.Line(color: Color, dashed: Boolean) {
    Box(
        modifier = Modifier
            .weight(1f)
            .widthIn(min = MonarchAiContextDividerTokens.LineMinLength)
            .height(MonarchAiContextDividerTokens.LineWidth)
            .drawBehind {
                val dash = MonarchAiContextDividerTokens.Dash.toPx()
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = size.height,
                    pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(dash, dash)) else null,
                )
            },
    )
}

@Composable
private fun Controls(
    summary: String?,
    onExpand: (() -> Unit)?,
    labels: MonarchAiContextDividerLabels,
    colors: MonarchColors,
) {
    if (summary != null) {
        var expanded by rememberSaveable { mutableStateOf(false) }
        val prefix = monarchString(labels.summaryPrefix, R.string.monarch_aicontextdivider_summary_prefix)
        val spoken = stringResource(R.string.monarch_aicontextdivider_summary_read, prefix, summary)
        MonarchCollapsible(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            label = monarchString(labels.showSummary, R.string.monarch_aicontextdivider_show_summary),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = summary,
                modifier = Modifier
                    .fillMaxWidth()
                    .monarchSurface(MonarchAiContextDividerTokens.summaryColors(colors), MonarchShapeDefaults.Row)
                    .padding(MonarchAiContextDividerTokens.SummaryPadding)
                    .clearAndSetSemantics { contentDescription = spoken },
                style = MonarchTheme.typography.body,
                color = colors.textDefault,
            )
        }
    }
    if (onExpand != null) {
        MonarchButton(
            text = monarchString(labels.expand, R.string.monarch_aicontextdivider_expand),
            onClick = onExpand,
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
        )
    }
}
