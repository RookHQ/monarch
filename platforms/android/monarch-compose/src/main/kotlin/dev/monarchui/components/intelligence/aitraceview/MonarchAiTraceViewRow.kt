package dev.monarchui.components.intelligence.aitraceview

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.res.stringResource
import dev.monarchui.R
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchTheme

/** One span: the open and close button (or its space), then the button that opens its details. */
@Suppress("LongParameterList", "LongMethod")
@Composable
internal fun MonarchAiTraceViewRow(
    line: MonarchAiTraceLine,
    totals: MonarchAiTraceTotals,
    selected: Boolean,
    expandIcon: @Composable () -> Unit,
    texts: MonarchAiTraceViewTexts,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    compact: Boolean = false,
) {
    val tokens = MonarchAiTraceViewTokens
    val colors = MonarchTheme.colors
    val span = line.span
    val running = span.end == null
    val durationText = if (running) null else texts.duration(tokens.duration(span, totals.end))
    val statusText = texts.status(span.status).takeIf { span.status != MonarchAiTraceSpanStatus.Ok }
    val tokenText = tokenCount(span)?.let { texts.tokens(it) }
    val costText = texts.cost(span)
    val kind = texts.kind(span.kind)
    val timing = texts.timing(texts.offset(span.start - totals.start), durationText)
    val description = listOfNotNull(span.name, statusText, kind, tokenText, costText, timing).joinToString(", ")
    val numberStyle = MonarchTheme.typography.caption.copy(fontFeatureSettings = "tnum")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) MonarchAiTraceViewColors.selectedColor(colors) else Color.Transparent,
                MonarchShapeDefaults.Row,
            ),
    ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = (if (compact) Modifier.weight(1f) else Modifier.width(tokens.SpanWidth))
                .padding(start = tokens.Indent * line.depth),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (line.hasChildren) {
                Toggle(span.name, line.open, expandIcon, texts, onToggle)
            } else {
                Spacer(Modifier.width(MonarchTouchTarget.Minimum))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MonarchTouchTarget.Minimum)
                    .monarchFocusRing(MonarchShapeDefaults.Row)
                    .clickable(indication = ripple(), interactionSource = null, role = Role.Button, onClick = onOpen)
                    .clearAndSetSemantics {
                        contentDescription = description
                        this.selected = selected
                    }
                    .padding(vertical = tokens.RowPadding)
                    .padding(end = tokens.RowPadding),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = span.name, style = MonarchTheme.typography.label, color = colors.textDefault)
                statusText?.let { MonarchBadge(text = it, tone = tone(span.status)) }
            }
        }
        Row(
            modifier = Modifier.then(if (compact) Modifier else Modifier.clickable(null, null, onClick = onOpen))
                .clearAndSetSemantics {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Cell(kind, tokens.KindWidth, numberStyle, colors.textDefault, end = false)
            Cell(
                durationText ?: texts.status(MonarchAiTraceSpanStatus.Running),
                tokens.NumberWidth,
                numberStyle,
                colors.textDefault,
                end = true,
            )
            if (!compact) {
                WideCells(
                    listOf(tokenText ?: texts.notAvailable, costText ?: texts.notAvailable),
                    tokens.bar(span, totals),
                    MonarchAiTraceViewColors.barColor(span, colors),
                    numberStyle,
                )
            }
        }
    }
    if (compact) {
        CompactTimeline(
            line.depth,
            tokens.bar(span, totals),
            MonarchAiTraceViewColors.barColor(span, colors),
            listOfNotNull(tokenText, costText),
            numberStyle,
        )
    }
    }
}

@Composable
private fun Cell(
    text: String,
    width: Dp,
    style: TextStyle,
    color: Color,
    end: Boolean,
) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(horizontal = MonarchAiTraceViewTokens.RowPadding),
        style = style,
        color = color,
        textAlign = if (end) TextAlign.End else TextAlign.Start,
    )
}

/** The column headings of the table of spans. */
@Composable
internal fun MonarchAiTraceViewHeader(texts: MonarchAiTraceViewTexts, compact: Boolean = false) {
    val tokens = MonarchAiTraceViewTokens
    val colors = MonarchTheme.colors
    val style = MonarchTheme.typography.caption
    Column(modifier = Modifier.clearAndSetSemantics {}) {
        Row(modifier = Modifier.padding(vertical = tokens.RowPadding), verticalAlignment = Alignment.CenterVertically) {
            val span = stringResource(R.string.monarch_aitraceview_column_span)
            if (compact) {
                // The bar sits under the name, so its heading shares the first column.
                Row(
                    modifier = Modifier.weight(1f).padding(horizontal = tokens.RowPadding),
                    horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                ) {
                    Text(text = span, style = style, color = colors.textMuted)
                    Text(
                        text = stringResource(R.string.monarch_aitraceview_column_timeline),
                        style = style,
                        color = colors.textMuted,
                    )
                }
            } else {
                Cell(span, tokens.SpanWidth, style, colors.textMuted, end = false)
            }
            Cell(texts.kindName, tokens.KindWidth, style, colors.textMuted, end = false)
            Cell(texts.duration, tokens.NumberWidth, style, colors.textMuted, end = true)
            if (compact) return@Row
            Cell(texts.totalTokens, tokens.NumberWidth, style, colors.textMuted, end = true)
            Cell(texts.totalCost, tokens.CostWidth, style, colors.textMuted, end = true)
            Cell(
                stringResource(R.string.monarch_aitraceview_column_timeline),
                tokens.TimelineWidth,
                style,
                colors.textMuted,
                end = false,
            )
        }
        MonarchDivider(variant = MonarchDividerVariant.Subtle)
    }
}

private fun tone(status: MonarchAiTraceSpanStatus): MonarchTone = when (status) {
    MonarchAiTraceSpanStatus.Error -> MonarchTone.Danger
    MonarchAiTraceSpanStatus.Running -> MonarchTone.Ai
    else -> MonarchTone.Neutral
}

/** The bar of a span on its track; start and end follow the layout direction. */
@Composable
private fun Bar(bar: Pair<Float, Float>, color: Color, track: Color) {
    val tokens = MonarchAiTraceViewTokens
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(tokens.BarHeight),
    ) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(color = track, cornerRadius = radius)
        val width = maxOf(bar.second * size.width, tokens.MinimumBar.toPx()).coerceAtMost(size.width)
        val startX = bar.first * size.width
        val left = if (layoutDirection == LayoutDirection.Ltr) startX else size.width - startX - width
        drawRoundRect(
            color = color,
            topLeft = Offset(left.coerceIn(0f, size.width - width), 0f),
            size = Size(width, size.height),
            cornerRadius = radius,
        )
    }
}

/** The open and close button of a span with children; the chevron turns a quarter when open. */
@Composable
private fun Toggle(
    name: String,
    open: Boolean,
    expandIcon: @Composable () -> Unit,
    texts: MonarchAiTraceViewTexts,
    onToggle: () -> Unit,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val turn = MonarchAiTraceViewTokens.EXPANDED_ROTATION
    val rotation by animateFloatAsState(
        targetValue = if (open) (if (rtl) -turn else turn) else 0f,
        animationSpec = MonarchTheme.motion.spec(MonarchTheme.motion.fast),
        label = "trace view chevron",
    )
    val label = texts.toggle(name, open)
    Box(
        modifier = Modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(role = Role.Button, interactionSource = null, indication = ripple(), onClick = onToggle)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides MonarchTheme.colors.textMuted) {
            Box(Modifier.graphicsLayer { rotationZ = rotation }.clearAndSetSemantics { }) { expandIcon() }
        }
    }
}

/** The second line of a row on a phone: the bar on its track, then the tokens and the cost. */
@Composable
private fun CompactTimeline(
    depth: Int,
    bar: Pair<Float, Float>,
    barColor: Color,
    figures: List<String>,
    style: TextStyle,
) {
    val tokens = MonarchAiTraceViewTokens
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier
            .padding(start = MonarchTouchTarget.Minimum + tokens.Indent * depth, end = tokens.RowPadding)
            .padding(bottom = tokens.RowPadding)
            .clearAndSetSemantics {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        Box(modifier = Modifier.weight(1f)) { Bar(bar, barColor, MonarchAiTraceViewColors.trackColor(colors)) }
        figures.forEach { Text(text = it, style = style, color = colors.textMuted) }
    }
}

/** The tokens, cost and timeline columns of a row in the full table. */
@Composable
private fun WideCells(figures: List<String>, bar: Pair<Float, Float>, barColor: Color, style: TextStyle) {
    val tokens = MonarchAiTraceViewTokens
    val colors = MonarchTheme.colors
    Cell(figures[0], tokens.NumberWidth, style, colors.textDefault, end = true)
    Cell(figures[1], tokens.CostWidth, style, colors.textDefault, end = true)
    Box(modifier = Modifier.width(tokens.TimelineWidth).padding(horizontal = tokens.RowPadding)) {
        Bar(bar, barColor, MonarchAiTraceViewColors.trackColor(colors))
    }
}

/** The tokens of a span, `null` when neither side is known. */
private fun tokenCount(span: MonarchAiTraceSpan): Long? {
    if (span.inputTokens == null && span.outputTokens == null) return null
    return (span.inputTokens ?: 0L) + (span.outputTokens ?: 0L)
}
