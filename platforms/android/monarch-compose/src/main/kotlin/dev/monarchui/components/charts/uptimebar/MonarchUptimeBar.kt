package dev.monarchui.components.charts.uptimebar

import android.icu.text.NumberFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.R
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.overlays.popover.MonarchPopover
import dev.monarchui.components.overlays.popover.MonarchPopoverSide
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme
import kotlin.math.roundToInt

/**
 * A row of per-period status segments, usually one per day, with the details of a period one tap away and the
 * overall uptime under it.
 *
 * Built by Monarch: Material 3 has no status bar of this kind. The segments are drawn on a Compose `Canvas` from
 * the start edge, coloured by state; a tap on the bar (a touch area at least 48dp high) inspects the nearest
 * segment, rings it and opens its details in a `MonarchPopover` anchored over the
 * segment. A tap outside the details closes them. A period's state is its status, or comes from its uptime and
 * [thresholds].
 *
 * For TalkBack the bar is one element with the image role named by [label] (or "Uptime") whose state is the
 * summary ("Periods: 30. Operational 28, Down 2. Uptime 99.83%"), followed by one element per period in order,
 * read as the period and its details; with [onSegmentSelect] each has an "Open" action. The label, start, end
 * and overall lines are hidden, because the bar and the summary already say them. It does not animate.
 *
 * @param segments the periods, oldest first.
 * @param modifier the modifier of the whole bar.
 * @param label what is monitored, such as a service name: shown above the bar and the bar's spoken name.
 * @param description a short line under the label, such as "Last 30 days".
 * @param overallUptime the overall uptime in percent. `null` takes the mean of the measured periods.
 * @param showOverallUptime whether the overall uptime is written under the bar.
 * @param startLabel text under the oldest period, such as "30 days ago".
 * @param endLabel text under the newest period, such as "Today".
 * @param thresholds the cut points for periods with an uptime but no status.
 * @param size the height of the segments: 32dp, or 20dp for tiles and rows.
 * @param showLegend lists the names of the colours under the bar. With several bars on a screen, show it on one.
 * @param uptimeDigits the decimal places of uptime percentages.
 * @param activeIndex the inspected period, used when [onActiveIndexChange] is set. Give several bars one state to
 * inspect the same period on each.
 * @param onActiveIndexChange makes the inspection controlled: called with the period the user taps, or `null`
 * when the details close. `null` lets the bar keep its own inspection.
 * @param summary replaces the generated spoken summary.
 * @param labels replacements for the built-in strings: state names, "Uptime", the overall line, "Legend",
 * "Periods", "Open" and the summary.
 * @param onSegmentSelect opens a period in the app, such as that day's incidents: from the Open button in the
 * details, or from the period's TalkBack action.
 */
@Composable
public fun MonarchUptimeBar(
    segments: List<MonarchUptimeBarSegment>,
    modifier: Modifier = Modifier,
    label: String? = null,
    description: String? = null,
    overallUptime: Double? = null,
    showOverallUptime: Boolean = true,
    startLabel: String? = null,
    endLabel: String? = null,
    thresholds: MonarchUptimeBarThresholds = MonarchUptimeBarThresholds(),
    size: MonarchUptimeBarSize = MonarchUptimeBarSize.Md,
    showLegend: Boolean = false,
    uptimeDigits: Int = 2,
    activeIndex: Int? = null,
    onActiveIndexChange: ((Int?) -> Unit)? = null,
    summary: String? = null,
    labels: MonarchUptimeBarLabels = MonarchUptimeBarLabels(),
    onSegmentSelect: ((MonarchUptimeBarSegment) -> Unit)? = null,
) {
    val words = uptimeWords(labels)
    val percent = rememberPercent(uptimeDigits)
    val statuses = remember(segments, thresholds) { segments.map { MonarchUptimeBarText.status(it, thresholds) } }
    val overall = MonarchUptimeBarText.overall(segments, overallUptime)?.let(percent)
    val counts = MonarchUptimeBarText.counts(statuses)
    val spoken = summary ?: labels.summary?.invoke(segments.size, counts, overall)
        ?: MonarchUptimeBarText.summary(segments.size, counts, overall, words)
    var ownActive by rememberSaveable { mutableStateOf<Int?>(null) }
    val active = (if (onActiveIndexChange != null) activeIndex else ownActive)?.takeIf { it in segments.indices }
    val setActive: (Int?) -> Unit = { next ->
        if (onActiveIndexChange != null) onActiveIndexChange(next) else ownActive = next
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MonarchUptimeBarTokens.Gap),
    ) {
        if (label != null || description != null) Header(label, description)
        UptimeSegments(
            segments = segments,
            statuses = statuses,
            name = label ?: words.uptime,
            summary = spoken,
            size = size,
            active = active,
            onActiveChange = setActive,
            describe = { MonarchUptimeBarText.describe(segments[it], statuses[it], percent, words) },
            details = { index ->
                DetailsContent(segments[index], statuses[index], percent, words, labels, onSegmentSelect)
            },
            openAction = openAction(segments, labels, onSegmentSelect),
        )
        val overallLine = overallLine(overall.takeIf { showOverallUptime }, labels)
        if (startLabel != null || endLabel != null || overallLine != null) AxisLine(startLabel, overallLine, endLabel)
        if (showLegend) Legend(words, monarchString(labels.legend, R.string.monarch_uptimebar_legend))
    }
}

/** The TalkBack action that opens a period, "Open, Mar 12", or `null` without [onSegmentSelect]. */
@Composable
private fun openAction(
    segments: List<MonarchUptimeBarSegment>,
    labels: MonarchUptimeBarLabels,
    onSegmentSelect: ((MonarchUptimeBarSegment) -> Unit)?,
): ((Int) -> CustomAccessibilityAction)? {
    val select = onSegmentSelect ?: return null
    val open = monarchString(labels.open, R.string.monarch_uptimebar_open)
    val pattern = stringResource(R.string.monarch_uptimebar_open_action)
    return { index ->
        CustomAccessibilityAction(pattern.format(open, segments[index].label)) {
            select(segments[index])
            true
        }
    }
}

/** The overall line under the bar, "99.83% uptime", or `null` when it is not shown. */
@Composable
private fun overallLine(overall: String?, labels: MonarchUptimeBarLabels): String? {
    if (overall == null) return null
    return labels.overall?.invoke(overall) ?: stringResource(R.string.monarch_uptimebar_overall, overall)
}

/** The label and description. The label is hidden from TalkBack: it names the bar. */
@Composable
private fun Header(label: String?, description: String?) {
    Column {
        if (label != null) {
            MonarchText(text = label, modifier = Modifier.clearAndSetSemantics { }, style = MonarchTextStyle.BodyStrong)
        }
        if (description != null) {
            MonarchText(
                text = description,
                style = MonarchTextStyle.Caption,
                color = MonarchUptimeBarTokens.mutedText(MonarchTheme.colors),
            )
        }
    }
}

/** The segments with the touch area, the TalkBack elements and the details of the inspected period. */
@Composable
@Suppress("LongParameterList", "CyclomaticComplexMethod")
private fun UptimeSegments(
    segments: List<MonarchUptimeBarSegment>,
    statuses: List<MonarchUptimeBarStatus>,
    name: String,
    summary: String,
    size: MonarchUptimeBarSize,
    active: Int?,
    onActiveChange: (Int?) -> Unit,
    describe: (Int) -> String,
    details: @Composable ColumnScope.(Int) -> Unit,
    openAction: ((Int) -> CustomAccessibilityAction)?,
) {
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val colors = MonarchTheme.colors
    val barHeight = MonarchUptimeBarTokens.height(size)
    val touchHeight = maxOf(barHeight, MonarchTouchTarget.Minimum)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val width = constraints.maxWidth.toFloat()
        val spans = with(density) {
            MonarchUptimeBarText.spans(
                segments.size, width, MonarchUptimeBarTokens.SegmentGap.toPx(), MonarchUptimeBarTokens.ThinGap.toPx(),
                MonarchUptimeBarTokens.ThinSegment.toPx(),
            )
        }
        fun left(index: Int): Float = spans[index].let { (start, length) -> if (rtl) width - start - length else start }
        val center = active?.let { left(it) + spans[it].second / 2 } ?: (width / 2)
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(touchHeight)
                    .semantics { isTraversalGroup = true }
                    .pointerInput(segments.size, rtl, width) {
                        detectTapGestures { offset ->
                            val along = if (rtl) width - offset.x else offset.x
                            MonarchUptimeBarText.indexAt(along, width, segments.size)?.let { index ->
                                onActiveChange(index)
                            }
                        }
                    },
                contentAlignment = AbsoluteAlignment.CenterLeft,
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clearAndSetSemantics {
                            contentDescription = name
                            stateDescription = summary
                            role = Role.Image
                            traversalIndex = -1f
                        },
                ) { drawSegments(statuses, active, colors, ::left, spans) }
                segments.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .absoluteOffset { IntOffset(left(index).roundToInt(), 0) }
                            .size(with(density) { spans[index].second.toDp() }, touchHeight)
                            .clearAndSetSemantics {
                                contentDescription = describe(index)
                                traversalIndex = index.toFloat()
                                openAction?.let { customActions = listOf(it(index)) }
                            },
                    )
                }
            }
            // The details of the inspected period, centred over its segment, above the bar when there is room.
            MonarchPopover(
                expanded = active != null,
                onDismissRequest = { onActiveChange(null) },
                label = active?.let { segments[it].label } ?: name,
                side = MonarchPopoverSide.Top,
                anchorX = with(density) { center.toDp() },
            ) { if (active != null) details(active) }
        }
    }
}

/** Draws each segment in its state's colours from [left], and the ring around the [active] one. */
private fun DrawScope.drawSegments(
    statuses: List<MonarchUptimeBarStatus>,
    active: Int?,
    colors: MonarchColors,
    left: (Int) -> Float,
    spans: List<Pair<Float, Float>>,
) {
    val radius = CornerRadius(MonarchUptimeBarTokens.SegmentRadius.toPx())
    val hairline = MonarchUptimeBarTokens.Hairline.toPx()
    statuses.forEachIndexed { index, status ->
        val part = MonarchUptimeBarTokens.statusColors(status, colors)
        val topLeft = Offset(left(index), 0f)
        val box = Size(spans[index].second, size.height)
        drawRoundRect(part.fill, topLeft, box, radius)
        part.outline?.let {
            drawRoundRect(
                it, topLeft + Offset(hairline / 2, hairline / 2),
                Size(box.width - hairline, box.height - hairline), radius, Stroke(hairline),
            )
        }
    }
    val index = active ?: return
    val (ring, gapColor) = MonarchUptimeBarTokens.ring(colors)
    val ringWidth = MonarchUptimeBarTokens.Ring.toPx()
    val inset = hairline + ringWidth / 2
    drawRoundRect(
        gapColor, Offset(left(index) - hairline / 2, -hairline / 2),
        Size(spans[index].second + hairline, size.height + hairline), radius, Stroke(hairline),
    )
    drawRoundRect(
        ring, Offset(left(index) - inset, -inset),
        Size(spans[index].second + 2 * inset, size.height + 2 * inset), radius, Stroke(ringWidth),
    )
}

/** The details of one period: its label as the title, then its state, uptime, details and note; Open when set. */
@Composable
@Suppress("LongParameterList")
private fun ColumnScope.DetailsContent(
    segment: MonarchUptimeBarSegment,
    status: MonarchUptimeBarStatus,
    percent: (Double) -> String,
    words: MonarchUptimeBarWords,
    labels: MonarchUptimeBarLabels,
    onSegmentSelect: ((MonarchUptimeBarSegment) -> Unit)?,
) {
    val open = monarchString(labels.open, R.string.monarch_uptimebar_open)
    val uptime = segment.uptime?.takeIf { it.isFinite() }?.let { words.namedValue.format(words.uptime, percent(it)) }
    MonarchText(text = segment.label, style = MonarchTextStyle.BodyStrong)
    MonarchUptimeBarDetailsBody(
        status = words.names.getValue(status),
        colors = MonarchUptimeBarTokens.statusColors(status, MonarchTheme.colors),
        uptime = uptime,
        lines = segment.details.map { words.namedValue.format(it.label, it.value) },
        note = segment.note,
    )
    onSegmentSelect?.let { select ->
        MonarchButton(
            text = open,
            onClick = { select(segment) },
            modifier = Modifier.defaultMinSize(minWidth = MonarchTouchTarget.Minimum),
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
        )
    }
}

/** The start label, the overall uptime and the end label under the bar. Hidden from TalkBack. */
@Composable
private fun AxisLine(start: String?, overall: String?, end: String?) {
    val color = MonarchUptimeBarTokens.mutedText(MonarchTheme.colors)
    Row(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(MonarchUptimeBarTokens.Gap),
        verticalAlignment = Alignment.Top,
    ) {
        MonarchText(
            text = start.orEmpty(),
            modifier = Modifier.weight(1f),
            style = MonarchTextStyle.Caption,
            color = color,
        )
        if (overall != null) {
            MonarchText(
                text = overall,
                modifier = Modifier.weight(1f),
                style = MonarchTextStyle.Caption,
                color = MonarchTheme.colors.textDefault,
                textAlign = TextAlign.Center,
            )
        }
        MonarchText(
            text = end.orEmpty(),
            modifier = Modifier.weight(1f),
            style = MonarchTextStyle.Caption,
            color = color,
            textAlign = TextAlign.End,
        )
    }
}

/** The names of the colours: a group named [name], one element per state. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(words: MonarchUptimeBarWords, name: String) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().semantics {
            isTraversalGroup = true
            contentDescription = name
        },
        horizontalArrangement = Arrangement.spacedBy(MonarchUptimeBarTokens.Gap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchUptimeBarStatus.entries.forEach { status ->
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) { },
                horizontalArrangement = Arrangement.spacedBy(MonarchUptimeBarTokens.LegendGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchUptimeBarSwatch(MonarchUptimeBarTokens.statusColors(status, MonarchTheme.colors))
                MonarchText(
                    text = words.names.getValue(status),
                    style = MonarchTextStyle.Caption,
                    color = MonarchUptimeBarTokens.mutedText(MonarchTheme.colors),
                )
            }
        }
    }
}

/** Percentages with exactly [digits] decimals in the current locale: "99.83%". */
@Composable
private fun rememberPercent(digits: Int): (Double) -> String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale, digits) {
        val format = NumberFormat.getPercentInstance(locale).apply {
            minimumFractionDigits = digits.coerceAtLeast(0)
            maximumFractionDigits = digits.coerceAtLeast(0)
        }
        val writer: (Double) -> String = { format.format(it / Percent) }
        writer
    }
}

/** The built-in words, with the app's replacements. */
@Composable
private fun uptimeWords(labels: MonarchUptimeBarLabels): MonarchUptimeBarWords {
    val defaults = mapOf(
        MonarchUptimeBarStatus.Operational to stringResource(R.string.monarch_uptimebar_operational),
        MonarchUptimeBarStatus.Degraded to stringResource(R.string.monarch_uptimebar_degraded),
        MonarchUptimeBarStatus.Partial to stringResource(R.string.monarch_uptimebar_partial),
        MonarchUptimeBarStatus.Down to stringResource(R.string.monarch_uptimebar_down),
        MonarchUptimeBarStatus.Maintenance to stringResource(R.string.monarch_uptimebar_maintenance),
        MonarchUptimeBarStatus.NoData to stringResource(R.string.monarch_uptimebar_no_data),
    )
    return MonarchUptimeBarWords(
        names = defaults + labels.statusNames,
        uptime = monarchString(labels.uptime, R.string.monarch_uptimebar_uptime),
        periods = monarchString(labels.periods, R.string.monarch_uptimebar_periods),
        namedValue = stringResource(R.string.monarch_uptimebar_named_value),
        namedCount = stringResource(R.string.monarch_uptimebar_named_count),
        periodDetails = stringResource(R.string.monarch_uptimebar_period_details),
    )
}

private const val Percent = 100.0
