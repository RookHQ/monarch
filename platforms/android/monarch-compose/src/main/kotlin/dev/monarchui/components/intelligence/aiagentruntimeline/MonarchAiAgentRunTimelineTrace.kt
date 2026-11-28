package dev.monarchui.components.intelligence.aiagentruntimeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchTheme

/** The time axis and one opening row per event. */
@Composable
internal fun MonarchAiAgentRunTimelineTrace(
    events: List<MonarchAiRunEvent>,
    now: Long?,
    currency: String,
    onOpenPart: ((MonarchAiRunEvent) -> Unit)?,
    texts: MonarchAiAgentRunTimelineTexts,
) {
    val tokens = MonarchAiAgentRunTimelineTokens
    val start = tokens.runStart(events)
    val end = tokens.runEnd(events, now)
    val open = remember { mutableStateListOf<String>() }
    Column(
        modifier = Modifier.semantics { contentDescription = texts.events },
        verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
    ) {
        Axis(end - start, texts)
        events.forEach { event ->
            EventRow(
                event = event,
                span = tokens.span(event, start, end),
                durationText = if (event.endedAt == null) {
                    texts.running
                } else {
                    texts.formatDuration(tokens.duration(event, end))
                },
                offsetText = texts.formatOffset(event.startedAt - start),
                expanded = event.id in open,
                onExpandedChange = { if (it) open += event.id else open -= event.id },
                onOpenPart = onOpenPart?.takeIf { tokens.opensPart(event, true) },
                texts = texts,
                currency = currency,
            )
        }
    }
}

/** The axis marks the start, the quarters and the end of the run. */
private const val AxisSteps = 4

/** Start, quarters and end of the run, hidden from TalkBack. */
@Composable
private fun Axis(total: Long, texts: MonarchAiAgentRunTimelineTexts) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        List(AxisSteps + 1) { total * it / AxisSteps }.forEach {
            Text(
                text = texts.formatDuration(it),
                style = MonarchTheme.typography.caption,
                color = MonarchTheme.colors.textMuted,
            )
        }
    }
}

@Composable
private fun EventRow(
    event: MonarchAiRunEvent,
    span: MonarchAiAgentRunTimelineSpan,
    durationText: String,
    offsetText: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpenPart: ((MonarchAiRunEvent) -> Unit)?,
    texts: MonarchAiAgentRunTimelineTexts,
    currency: String,
) {
    val tokens = MonarchAiAgentRunTimelineTokens
    val colors = MonarchTheme.colors
    val kind = texts.kind(event.kind)
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MonarchTouchTarget.Minimum)
                .monarchFocusRing(MonarchShapeDefaults.Row)
                .clickable(indication = ripple(), interactionSource = null, role = Role.Button) {
                    onExpandedChange(!expanded)
                }
                .clearAndSetSemantics {
                    contentDescription = listOf(kind, event.name, durationText).joinToString(", ")
                    stateDescription = if (expanded) texts.expanded else texts.collapsed
                    if (expanded) {
                        collapse {
                            onExpandedChange(false)
                            true
                        }
                    } else {
                        expand {
                            onExpandedChange(true)
                            true
                        }
                    }
                },
            verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.SmallGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalContentColor provides colors.textMuted) {
                    if (expanded) MonarchDefaultIcons.ChevronDown() else MonarchDefaultIcons.ChevronRight()
                }
                MonarchBadge(
                    text = kind,
                    tone = if (event.kind == MonarchAiRunEventKind.Error) MonarchTone.Danger else MonarchTone.Neutral,
                )
                Text(
                    text = event.name,
                    modifier = Modifier.weight(1f),
                    style = MonarchTheme.typography.label,
                    color = colors.textDefault,
                )
                Text(text = durationText, style = MonarchTheme.typography.caption, color = colors.textMuted)
            }
            Bar(
                span = span,
                color = tokens.barColor(event.kind, colors),
                fadeTo = if (event.endedAt == null) tokens.runningColor(colors) else null,
                track = tokens.trackColor(colors),
            )
        }
        if (expanded) {
            Details(event, offsetText, durationText, onOpenPart, texts, currency)
        }
    }
}

/** The bar of an event on its track; start and end follow the layout direction. */
@Composable
private fun Bar(span: MonarchAiAgentRunTimelineSpan, color: Color, fadeTo: Color?, track: Color) {
    val tokens = MonarchAiAgentRunTimelineTokens
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(tokens.BarHeight),
    ) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(color = track, cornerRadius = radius)
        val width = maxOf(span.length * size.width, tokens.MinimumBar.toPx())
        val ltr = layoutDirection == LayoutDirection.Ltr
        val startX = span.start * size.width
        val left = (if (ltr) startX else size.width - startX - width).coerceIn(0f, size.width - width)
        val colors = if (fadeTo == null) listOf(color, color) else listOf(color, fadeTo)
        val brush = Brush.horizontalGradient(
            colors = if (ltr) colors else colors.reversed(),
            startX = left,
            endX = left + width,
        )
        drawRoundRect(
            brush = brush,
            topLeft = Offset(left, 0f),
            size = Size(width, size.height),
            cornerRadius = radius,
        )
    }
}

/** The facts of an open event, each one element, then the detail and Show in conversation. */
@Composable
private fun Details(
    event: MonarchAiRunEvent,
    offsetText: String,
    durationText: String,
    onOpenPart: ((MonarchAiRunEvent) -> Unit)?,
    texts: MonarchAiAgentRunTimelineTexts,
    currency: String,
) {
    val tokens = MonarchAiAgentRunTimelineTokens
    val format = MonarchAiAgentRunTimelineFormat
    val facts = buildList {
        add(texts.start to offsetText)
        add(texts.duration to durationText)
        event.inputTokens?.let { add(texts.inputTokens to format.formatCount(it)) }
        event.outputTokens?.let { add(texts.outputTokens to format.formatCount(it)) }
        event.costMicros?.let { add(texts.cost to format.formatCost(it, currency)) }
    }
    Column(
        modifier = Modifier.padding(vertical = tokens.SmallGap),
        verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
    ) {
        facts.forEach { (name, value) ->
            Text(
                text = texts.fact(name, value),
                style = MonarchTheme.typography.caption,
                color = MonarchTheme.colors.textMuted,
            )
        }
        event.detail?.let {
            Text(text = it, style = MonarchTheme.typography.code, color = MonarchTheme.colors.textDefault)
        }
        if (onOpenPart != null) {
            MonarchButton(
                text = texts.openPart,
                onClick = { onOpenPart(event) },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}
