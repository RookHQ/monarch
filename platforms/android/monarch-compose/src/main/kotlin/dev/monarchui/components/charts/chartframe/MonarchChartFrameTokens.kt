package dev.monarchui.components.charts.chartframe

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonTokens
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure rules of MonarchChartFrame. */
internal object MonarchChartFrameTokens {
    /** The height kept for the body in every status, as on the web: 240dp. */
    val DefaultHeight: Dp = (MonarchTokens.Space.space24 * HEIGHT_STEPS).dp

    /** The space between the header, the legend, the body and the footer. */
    val SectionGap: Dp = MonarchTokens.Space.space3.dp

    /** The space between the title and the description. */
    val TitleGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The space around the header actions. */
    val ActionsGap: Dp = MonarchTokens.Space.space2.dp

    /** The vertical padding of a table row. */
    val CellPadding: Dp = MonarchTokens.Space.space2.dp

    /** The text of a gap in the table. */
    const val GAP_TEXT: String = "—"

    private const val HEIGHT_STEPS = 2.5f

    /** Whether the legend is shown: only with the chart itself, ready and not replaced by the table. */
    fun showsLegend(status: MonarchChartFrameStatus, showTable: Boolean, legendCount: Int): Boolean =
        status == MonarchChartFrameStatus.Ready && !showTable && legendCount > 0

    /** Whether the table toggle is offered: the chart is ready, it has a table and the toggle is wanted. */
    fun showsTableToggle(status: MonarchChartFrameStatus, table: MonarchChartTable?, tableToggle: Boolean): Boolean =
        status == MonarchChartFrameStatus.Ready && table != null && tableToggle

    /** The shown text of one cell: the app's formatted value, the locale's format of the number, or a dash. */
    fun cellText(series: MonarchChartTableSeries, row: Int, format: (Double) -> String): String {
        val value = series.values.getOrNull(row) ?: return GAP_TEXT
        return series.formattedValues?.getOrNull(row) ?: format(value)
    }

    /** The spoken text of one cell: like [cellText], with [noValue] for a gap. */
    fun spokenCell(series: MonarchChartTableSeries, row: Int, noValue: String, format: (Double) -> String): String =
        if (series.values.getOrNull(row) == null) noValue else cellText(series, row, format)

    /** How far a small ghost button's text sits inside its edge: the button is moved out by it. */
    fun ghostInset(): Dp =
        MonarchButtonTokens.contentPadding(MonarchButtonSize.Sm).calculateRightPadding(LayoutDirection.Ltr)
}
