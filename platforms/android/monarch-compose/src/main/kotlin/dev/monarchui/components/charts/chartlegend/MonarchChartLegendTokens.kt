package dev.monarchui.components.charts.chartlegend

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure rules of MonarchChartLegend. */
internal object MonarchChartLegendTokens {
    /** The length of the line marker. */
    val LineLength: Dp = MonarchTokens.Space.space4.dp

    /** The thickness of the line marker and the outline of a dashed square. */
    val Stroke: Dp = MonarchTokens.Space.space0_5.dp

    /** The length of a dash of a dashed line marker. */
    val Dash: Dp = MonarchTokens.Space.space1.dp

    /** The side of the square marker. */
    val Square: Dp = MonarchTokens.Space.space2_5.dp

    /** The diameter of the dot marker. */
    val Dot: Dp = MonarchTokens.Space.space2.dp

    /** The space between the marker, the label and the value. */
    val Gap: Dp = MonarchTokens.Space.space1_5.dp

    /** The space between entries. */
    val ItemSpacing: Dp = MonarchTokens.Space.space3.dp

    /** The visible height of an entry, the height of a filter chip; the touch target is 48dp. */
    val EntryHeight: Dp = MonarchTokens.Control.sm.dp

    /** The space at the start and end of an entry, inside its highlight. */
    val EntryPadding: Dp = MonarchTokens.Space.space2.dp

    /** The number of data slots before entries fall back to the neutral colour. */
    private const val DATA_SLOTS = 5

    /** The palette slot of an entry: its own, or the one of its position. */
    fun slot(color: MonarchChartColor?, index: Int): MonarchChartColor =
        color ?: if (index in 0 until DATA_SLOTS) MonarchChartColor.entries[index] else MonarchChartColor.Neutral

    /** The colour of a palette slot in the current theme. */
    fun color(slot: MonarchChartColor, colors: MonarchColors): Color = when (slot) {
        MonarchChartColor.Data1 -> colors.data1
        MonarchChartColor.Data2 -> colors.data2
        MonarchChartColor.Data3 -> colors.data3
        MonarchChartColor.Data4 -> colors.data4
        MonarchChartColor.Data5 -> colors.data5
        MonarchChartColor.Neutral -> colors.textSubtle
    }

    /** The colour of a marker: the series colour, or the disabled text colour while hidden or disabled. */
    fun markerColor(slot: MonarchChartColor, shown: Boolean, enabled: Boolean, colors: MonarchColors): Color =
        if (shown && enabled) color(slot, colors) else colors.textDisabled

    /** The colour of a label: default while shown, muted while hidden, disabled when disabled. */
    fun labelColor(shown: Boolean, enabled: Boolean, colors: MonarchColors): Color = when {
        !enabled -> colors.textDisabled
        shown -> colors.textDefault
        else -> colors.textMuted
    }

    /**
     * The hidden set after the user toggles [key]. With [keepOneVisible] the last shown entry of [keys] stays
     * shown. Keys of [hidden] that the legend does not list are kept.
     */
    fun toggle(hidden: Set<String>, key: String, keys: List<String>, keepOneVisible: Boolean): Set<String> = when {
        key in hidden -> hidden - key
        locked(hidden, key, keys, keepOneVisible) -> hidden
        else -> hidden + key
    }

    /** Whether [key] is the last shown entry and may not be hidden. */
    fun locked(hidden: Set<String>, key: String, keys: List<String>, keepOneVisible: Boolean): Boolean =
        keepOneVisible && key !in hidden && keys.count { it !in hidden } <= 1

    /** The hidden set after Show all: the legend's own keys are shown, other keys stay hidden. */
    fun showAll(hidden: Set<String>, keys: List<String>): Set<String> = hidden - keys.toSet()

    /** Whether the Show all button is shown: when it is wanted and one of the legend's series is hidden. */
    fun showsShowAll(hidden: Set<String>, keys: List<String>, showAllButton: Boolean): Boolean =
        showAllButton && keys.any { it in hidden }
}
