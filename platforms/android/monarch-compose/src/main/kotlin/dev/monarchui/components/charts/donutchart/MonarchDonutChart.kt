package dev.monarchui.components.charts.donutchart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.R
import dev.monarchui.components.charts.chartframe.MonarchChartFrame
import dev.monarchui.components.charts.chartframe.MonarchChartFrameStatus
import dev.monarchui.components.charts.chartlegend.MonarchChartLegendItem
import dev.monarchui.components.charts.chartlegend.MonarchChartLegendMarker
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchChartPercentFormat
import dev.monarchui.internal.rememberMonarchChartValueFormat
import dev.monarchui.theme.MonarchTheme
import kotlin.math.roundToInt

/**
 * Parts of one whole as a donut inside a `MonarchChartFrame`: the total in the middle, a legend of values
 * and shares, the smallest segments folded into Other, and every segment in the data table.
 *
 * Built by Monarch with a Compose `Canvas` and `drawArc`: Material 3 has no chart. Slices take the theme's
 * data colours in order, clockwise from the top, and Other the neutral colour; a gap separates them.
 *
 * For TalkBack the donut is one element named by [title] whose state is a summary ("Total 874. Laptops 412
 * (47%), …"), followed by one element per slice, in order, read as its name, value and share. The centre
 * text is part of the summary and not read again. The legend entries read the same values; "View as table"
 * lists every segment, folded or not. In right to left the slices run anticlockwise. It does not animate.
 *
 * @param title the chart's name: the frame heading and the donut's spoken name.
 * @param segments the parts. Negative and non-finite values count as 0.
 * @param modifier the modifier of the frame.
 * @param description one line under the title.
 * @param centerValue the large text in the middle. `null` writes the total.
 * @param centerLabel small text under the centre value, such as "assets".
 * @param summary replaces the generated spoken summary.
 * @param sort the largest slice first, or the given order.
 * @param maxSegments the most slices drawn, at least 2; the smallest beyond it fold into a neutral Other slice.
 * @param legendContent what each legend entry shows after its label.
 * @param status ready, loading, empty or error. Ready with a total of 0 shows the empty state.
 * @param size the outer diameter, and the height kept in every status. `null` is 160dp.
 * @param thickness the ring's thickness, kept between 4dp and half the size. `null` is 22% of the size.
 * @param percentDigits the decimal places of shares.
 * @param showCenterValue whether the middle shows the centre value and label.
 * @param showLegend whether the frame shows the legend.
 * @param tableToggle whether "View as table" is offered.
 * @param emptyMessage the message of the empty state.
 * @param footer notes under the chart.
 * @param labels replacements for the built-in strings, including the frame's.
 * @param formatValue writes values in the centre, the legend, the table, TalkBack and the summary. `null`
 * writes numbers in the current locale.
 * @param onRetry shows Try again in the error status.
 */
@Composable
public fun MonarchDonutChart(
    title: String,
    segments: List<MonarchDonutChartSegment>,
    modifier: Modifier = Modifier,
    description: String? = null,
    centerValue: String? = null,
    centerLabel: String? = null,
    summary: String? = null,
    sort: MonarchDonutChartSort = MonarchDonutChartSort.Descending,
    maxSegments: Int = 6,
    legendContent: MonarchDonutChartLegendContent = MonarchDonutChartLegendContent.Both,
    status: MonarchChartFrameStatus = MonarchChartFrameStatus.Ready,
    size: Dp? = null,
    thickness: Dp? = null,
    percentDigits: Int = 0,
    showCenterValue: Boolean = true,
    showLegend: Boolean = true,
    tableToggle: Boolean = true,
    emptyMessage: String? = null,
    footer: String? = null,
    labels: MonarchDonutChartLabels = MonarchDonutChartLabels(),
    formatValue: ((Double) -> String)? = null,
    onRetry: (() -> Unit)? = null,
) {
    val format = rememberMonarchChartValueFormat(formatValue)
    val percentFormat = rememberMonarchChartPercentFormat(percentDigits)
    val words = donutWords(labels)
    val ordered = remember(segments, sort) { MonarchDonutChartSlices.ordered(segments, sort) }
    val total = MonarchDonutChartSlices.total(ordered)
    val share: (Double) -> String = { MonarchDonutChartSlices.share(it, total, percentDigits, percentFormat, words) }
    val other = monarchString(labels.other, R.string.monarch_donutchart_other)
    val slices = remember(ordered, maxSegments, other) { MonarchDonutChartSlices.slices(ordered, maxSegments, other) }
    val legend = if (showLegend) {
        slices.map { slice ->
            MonarchChartLegendItem(
                key = slice.key,
                label = slice.label,
                color = slice.slot,
                value = MonarchDonutChartSlices.legendValue(
                    legendContent, format(slice.value), share(slice.value), words,
                ),
            )
        }
    } else {
        emptyList()
    }
    val table = MonarchDonutChartSlices.table(
        ordered = ordered,
        segmentTitle = monarchString(labels.segment, R.string.monarch_donutchart_segment),
        valueTitle = monarchString(labels.value, R.string.monarch_donutchart_value),
        shareTitle = monarchString(labels.share, R.string.monarch_donutchart_share),
        format = format,
        share = share,
        words = words,
    )
    val diameter = size ?: MonarchDonutChartTokens.DefaultSize
    val empty = status == MonarchChartFrameStatus.Ready && total <= 0.0
    val resolved = if (empty) MonarchChartFrameStatus.Empty else status
    MonarchChartFrame(
        title = title,
        modifier = modifier,
        description = description,
        status = resolved,
        table = table,
        legend = legend,
        legendMarker = MonarchChartLegendMarker.Square,
        emptyMessage = emptyMessage,
        footer = footer,
        height = diameter,
        tableToggle = tableToggle,
        onRetry = onRetry,
        labels = labels.frame,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Donut(
                slices = slices,
                diameter = diameter,
                ring = MonarchDonutChartTokens.thickness(diameter, thickness),
                title = title,
                summary = summary ?: MonarchDonutChartSlices.summary(ordered, format, share, words),
                describe = { words.entry.format(it.label, format(it.value), share(it.value)) },
                center = if (showCenterValue) centerValue ?: format(total) else null,
                centerLabel = centerLabel.takeIf { showCenterValue },
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun Donut(
    slices: List<MonarchDonutChartSlice>,
    diameter: Dp,
    ring: Dp,
    title: String,
    summary: String,
    describe: (MonarchDonutChartSlice) -> String,
    center: String?,
    centerLabel: String?,
) {
    val colors = MonarchTheme.colors
    val palette = MonarchDonutChartTokens.colors(colors)
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val sizePx = with(density) { diameter.toPx() }
    val ringPx = with(density) { ring.toPx() }
    val radius = (sizePx - ringPx) / 2
    val circumference = (2 * Math.PI * radius).toFloat().coerceAtLeast(1f)
    val arcs = MonarchDonutChartSlices.arcs(
        slices = slices,
        gap = with(density) { MonarchDonutChartTokens.Gap.toPx() } / circumference * FullTurn,
        minSweep = with(density) { MonarchDonutChartTokens.MinArc.toPx() } / circumference * FullTurn,
    )
    // The slice elements are placed by absolute x, already mirrored, so the box must not mirror them again.
    Box(
        modifier = Modifier.size(diameter).semantics { isTraversalGroup = true },
        contentAlignment = AbsoluteAlignment.TopLeft,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clearAndSetSemantics {
                    contentDescription = title
                    stateDescription = summary
                    role = Role.Image
                    traversalIndex = -1f
                },
        ) {
            val stroke = Stroke(width = ringPx)
            val topLeft = Offset(ringPx / 2, ringPx / 2)
            val arcSize = Size(radius * 2, radius * 2)
            drawCircle(color = palette.track, radius = radius, style = stroke)
            scale(scaleX = if (rtl) -1f else 1f, scaleY = 1f) {
                arcs.forEach { arc ->
                    drawArc(
                        color = MonarchDonutChartTokens.sliceColor(arc.slice.slot, colors),
                        startAngle = arc.start - QuarterTurn,
                        sweepAngle = arc.sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke,
                    )
                }
            }
        }
        if (center != null || centerLabel != null) {
            CenterText(center, centerLabel, palette, Modifier.padding(ring + MonarchDonutChartTokens.CenterInset))
        }
        arcs.forEachIndexed { index, arc ->
            val (x, y) = MonarchDonutChartSlices.pointAt(arc.middle, radius, sizePx / 2)
            val left = (if (rtl) sizePx - x else x) - ringPx / 2
            Box(
                modifier = Modifier
                    .absoluteOffset { IntOffset(left.roundToInt(), (y - ringPx / 2).roundToInt()) }
                    .size(ring)
                    .clearAndSetSemantics {
                        contentDescription = describe(arc.slice)
                        traversalIndex = index.toFloat()
                    },
            )
        }
    }
}

/** The centre value and label: one line each, shrinking to half size before they are cut. Hidden from TalkBack. */
@Composable
private fun CenterText(
    value: String?,
    label: String?,
    palette: MonarchDonutChartColors,
    modifier: Modifier,
) {
    val typography = MonarchTheme.typography
    Column(
        modifier = modifier.fillMaxSize().clearAndSetSemantics { },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (value != null) CenterLine(value, typography.heading.merge(TextStyle(color = palette.centerValue)))
        if (label != null) CenterLine(label, typography.caption.merge(TextStyle(color = palette.centerLabel)))
    }
}

@Composable
private fun CenterLine(text: String, style: TextStyle) {
    BasicText(
        text = text,
        style = style.merge(TextStyle(textAlign = TextAlign.Center)),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(
            minFontSize = style.fontSize * MonarchDonutChartTokens.CENTER_MIN_SCALE,
            maxFontSize = style.fontSize,
        ),
    )
}

/** The built-in patterns of the donut, with the app's replacement for Total. */
@Composable
private fun donutWords(labels: MonarchDonutChartLabels): MonarchDonutChartWords = MonarchDonutChartWords(
    entry = stringResource(R.string.monarch_donutchart_entry),
    namedValue = stringResource(R.string.monarch_donutchart_named_value),
    valueAndShare = stringResource(R.string.monarch_donutchart_value_and_share),
    below = stringResource(R.string.monarch_donutchart_share_below),
    above = stringResource(R.string.monarch_donutchart_share_above),
    total = monarchString(labels.total, R.string.monarch_donutchart_total),
)

private const val FullTurn = 360f
private const val QuarterTurn = 90f
