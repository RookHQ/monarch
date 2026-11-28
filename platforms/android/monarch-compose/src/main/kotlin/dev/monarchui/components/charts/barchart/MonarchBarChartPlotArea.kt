package dev.monarchui.components.charts.barchart

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
import dev.monarchui.theme.MonarchTheme
import kotlin.math.roundToInt

/**
 * The plot of a bar chart and, while a category is inspected, the row of its values under it.
 *
 * The canvas is one TalkBack element named by the title with the summary as its state. Over it lie one
 * invisible element per category band, in category order, each read as the category and its values, with a
 * "Details" action when [onSelect] is set. A tap, or a drag along columns, inspects the category under the
 * finger.
 */
@Composable
internal fun MonarchBarChartPlotArea(
    model: MonarchBarChartModel,
    paint: MonarchBarChartPaint,
    text: MonarchBarChartPlotText,
    inspected: Int?,
    onInspect: (Int?) -> Unit,
    onSelect: ((Int) -> Unit)?,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PlotCanvas(model, paint, text, inspected, onInspect, onSelect, Modifier.weight(1f))
        if (inspected != null && inspected in model.categories.indices) {
            InspectionRow(
                values = text.describe(inspected),
                details = text.details.takeIf { onSelect != null },
                clear = text.clear,
                onDetails = { onSelect?.invoke(inspected) },
                onClear = { onInspect(null) },
            )
        }
    }
}

@Composable
private fun PlotCanvas(
    model: MonarchBarChartModel,
    paint: MonarchBarChartPaint,
    text: MonarchBarChartPlotText,
    inspected: Int?,
    onInspect: (Int?) -> Unit,
    onSelect: ((Int) -> Unit)?,
    modifier: Modifier,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // The category elements are placed by absolute x, already mirrored, so the box must not mirror them again.
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().semantics { isTraversalGroup = true },
        contentAlignment = AbsoluteAlignment.TopLeft,
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val plot = remember(model, paint, width, height, density) {
            MonarchBarChartMeasure.plot(model, width, height, measurer, paint, density)
        }
        val currentPlot by rememberUpdatedState(plot)
        val inspect: (Offset) -> Unit = { offset ->
            val along = if (currentPlot.horizontal) offset.y else if (rtl) width - offset.x else offset.x
            currentPlot.indexAt(along, model.categories.size)?.let(onInspect)
        }
        val gestures = Modifier
            .pointerInput(model, rtl) { detectTapGestures { inspect(it) } }
            .then(
                if (model.horizontal) {
                    Modifier
                } else {
                    Modifier.pointerInput(model, rtl) {
                        detectHorizontalDragGestures(onDragStart = { inspect(it) }) { change, _ ->
                            inspect(change.position)
                        }
                    }
                },
            )
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(gestures)
                .clearAndSetSemantics {
                    contentDescription = text.title
                    stateDescription = text.summary
                    role = Role.Image
                    traversalIndex = -1f
                },
        ) { drawMonarchBarChart(model, plot, paint, measurer, inspected, rtl) }
        model.categories.indices.forEach { index ->
            val band = plot.categoryRect(index)
            val left = if (rtl) width - band.right else band.left
            Box(
                modifier = Modifier
                    .absoluteOffset { IntOffset(left.roundToInt(), band.top.roundToInt()) }
                    .size(with(density) { band.width.toDp() }, with(density) { band.height.toDp() })
                    .clearAndSetSemantics {
                        contentDescription = text.describe(index)
                        traversalIndex = index.toFloat()
                        if (model.highlightedIndex == index) stateDescription = text.highlighted
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

/** The values of the inspected category, announced politely, with Details and Clear. */
@Composable
private fun InspectionRow(
    values: String,
    details: String?,
    clear: String,
    onDetails: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = MonarchBarChartTokens.InspectionGap),
        horizontalArrangement = Arrangement.spacedBy(MonarchBarChartTokens.InspectionGap),
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
