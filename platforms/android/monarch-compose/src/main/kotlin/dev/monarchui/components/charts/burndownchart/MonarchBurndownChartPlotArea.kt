package dev.monarchui.components.charts.burndownchart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.charts.linechart.MonarchLineChartGeometry
import dev.monarchui.components.charts.linechart.MonarchLineChartPaint
import dev.monarchui.components.charts.linechart.MonarchLineChartPlotText
import dev.monarchui.components.charts.linechart.MonarchLineChartTokens
import dev.monarchui.theme.MonarchTheme
import kotlin.math.roundToInt

/**
 * The plot of a burndown and, while a day is inspected, the row of its values under it. It works as the line
 * chart's plot does: the canvas is one TalkBack element named by the title with the summary as its state, over it
 * lies one invisible element per day, and a tap or a drag inspects the nearest day.
 */
@Composable
@Suppress("LongParameterList")
internal fun MonarchBurndownChartPlotArea(
    scene: MonarchBurndownChartScene,
    paint: MonarchLineChartPaint,
    colors: MonarchBurndownChartColors,
    text: MonarchLineChartPlotText,
    active: Int?,
    onActiveChange: (Int?) -> Unit,
    onSelect: ((Int) -> Unit)?,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BurndownChartCanvas(scene, paint, colors, text, active, onActiveChange, onSelect, Modifier.weight(1f))
        if (active != null) {
            BurndownChartInspectionRow(
                values = text.describe(active),
                details = text.details.takeIf { onSelect != null },
                clear = text.clear,
                onDetails = { onSelect?.invoke(active) },
                onClear = { onActiveChange(null) },
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun BurndownChartCanvas(
    scene: MonarchBurndownChartScene,
    paint: MonarchLineChartPaint,
    colors: MonarchBurndownChartColors,
    text: MonarchLineChartPlotText,
    active: Int?,
    onActiveChange: (Int?) -> Unit,
    onSelect: ((Int) -> Unit)?,
    modifier: Modifier,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val change by rememberUpdatedState(onActiveChange)
    // The day elements are placed by absolute x, already mirrored, so the box must not mirror them again.
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().semantics { isTraversalGroup = true },
        contentAlignment = AbsoluteAlignment.TopLeft,
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val plot = remember(scene, paint, width, height, density) {
            MonarchBurndownChartMeasure.plot(scene, width, height, measurer, paint, density)
        }
        val currentPlot by rememberUpdatedState(plot)
        val inspect: (Offset) -> Unit = { offset ->
            currentPlot.indexAt(if (rtl) width - offset.x else offset.x)?.let { change(it) }
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(scene, rtl) { detectTapGestures { inspect(it) } }
                .pointerInput(scene, rtl) {
                    detectHorizontalDragGestures(onDragStart = { inspect(it) }) { pointer, _ ->
                        inspect(pointer.position)
                    }
                }
                .clearAndSetSemantics {
                    contentDescription = text.title
                    stateDescription = text.summary
                    role = Role.Image
                    traversalIndex = -1f
                },
        ) { drawMonarchBurndownChart(scene, plot, paint, colors, measurer, active, rtl) }
        plot.positions.indices.forEach { index ->
            val (from, to) = MonarchLineChartGeometry.strip(plot.positions, index, plot.x0, plot.x1)
            val left = if (rtl) width - to else from
            Box(
                modifier = Modifier
                    .absoluteOffset { IntOffset(left.roundToInt(), plot.y0.roundToInt()) }
                    .size(with(density) { (to - from).toDp() }, with(density) { (plot.y1 - plot.y0).toDp() })
                    .clearAndSetSemantics {
                        contentDescription = text.describe(index)
                        traversalIndex = index.toFloat()
                        if (onSelect != null) {
                            customActions = listOf(
                                CustomAccessibilityAction(text.detailsAction(index)) {
                                    onSelect(index)
                                    true
                                },
                            )
                        }
                    },
            )
        }
    }
}

/** The values of the inspected day, announced politely, with Details and Clear. */
@Composable
private fun BurndownChartInspectionRow(
    values: String,
    details: String?,
    clear: String,
    onDetails: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = MonarchLineChartTokens.InspectionGap),
        horizontalArrangement = Arrangement.spacedBy(MonarchLineChartTokens.InspectionGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = values,
            modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            style = MonarchTheme.typography.caption,
            color = MonarchTheme.colors.textDefault,
        )
        if (details != null) {
            MonarchButton(
                text = details,
                onClick = onDetails,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
        MonarchButton(
            text = clear,
            onClick = onClear,
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
        )
    }
}
