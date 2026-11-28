package com.rookhq.monarch.components.charts.heatmap

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegendTokens
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

/**
 * The four-step sequential scale of a heatmap: three cut points and the level
 * of any value. Level `null` is no data, 0 is zero or less, 1 to 4 run from
 * the lightest to the darkest step.
 */
internal class HeatmapScale(val thresholds: List<Double>) {
    /** The level of [value]. */
    fun level(value: Double?): Int? {
        if (value == null || !value.isFinite()) return null
        val (a, b, c) = thresholds
        return when {
            value <= 0 -> 0
            c > 0 && value >= c -> HeatmapLevels.TOP
            b > 0 && value >= b -> HeatmapLevels.TOP - 1
            a > 0 && value >= a -> 2
            else -> 1
        }
    }

    companion object {
        /** Quarters of the largest value, or the first three of [thresholds] when given. */
        fun of(values: List<Double?>, thresholds: List<Double>?): HeatmapScale {
            if (thresholds != null && thresholds.size >= HeatmapLevels.CUTS) {
                return HeatmapScale(thresholds.take(HeatmapLevels.CUTS))
            }
            val max = values.filterNotNull().filter { it.isFinite() }.fold(0.0) { best, value -> maxOf(best, value) }
            return HeatmapScale(List(HeatmapLevels.CUTS) { max * (it + 1) / (HeatmapLevels.CUTS + 1) })
        }
    }
}

/** The levels of the scale. */
internal object HeatmapLevels {
    /** The darkest level. */
    const val TOP = 4

    /** The number of cut points. */
    const val CUTS = 3

    /** The level from which values sit on a surface chip. */
    const val CHIP = 3
}

/** The resolved strings and number format of a heatmap. Templates use `%1$s`-style placeholders. */
@Suppress("LongParameterList")
internal class HeatmapWords(
    val locale: Locale,
    val format: (Double) -> String,
    val noData: String,
    val rangeBelow: String,
    val above: String,
    val range: String,
    val rangeAbove: String,
    val rowCount: (Int) -> String,
    val columnCount: (Int) -> String,
    val size: String,
    val highest: String,
    val cell: String,
    val withNote: String,
)

/** The cells of a heatmap by position, built once from its rows, columns and cells. */
internal class HeatmapModel(
    val rows: List<MonarchHeatmapAxisItem>,
    val columns: List<MonarchHeatmapAxisItem>,
    cells: List<MonarchHeatmapCell>,
    thresholds: List<Double>?,
) {
    private val byPosition: Map<Pair<String, String>, MonarchHeatmapCell> =
        cells.associateBy { it.row to it.column }

    /** The cell at a row and column index; a missing one has no value. */
    fun cellAt(row: Int, column: Int): MonarchHeatmapCell {
        val rowKey = rows[row].key
        val columnKey = columns[column].key
        return byPosition[rowKey to columnKey] ?: MonarchHeatmapCell(rowKey, columnKey, null)
    }

    /** The finite value at a position, or `null`. */
    fun valueAt(row: Int, column: Int): Double? = cellAt(row, column).value?.takeIf { it.isFinite() }

    val scale: HeatmapScale = HeatmapScale.of(
        rows.indices.flatMap { r -> columns.indices.map { c -> valueAt(r, c) } },
        thresholds,
    )

    /** Whether any position has a finite value. */
    val hasData: Boolean = rows.indices.any { r -> columns.indices.any { c -> valueAt(r, c) != null } }

    /** Whether any position has no value, which adds the No data step to the legend. */
    val hasGaps: Boolean = rows.indices.any { r -> columns.indices.any { c -> valueAt(r, c) == null } }

    /** The position of the highest value; the first one on a tie. */
    val highest: Pair<Int, Int>? = run {
        var best: Pair<Int, Int>? = null
        var bestValue = Double.NEGATIVE_INFINITY
        for (r in rows.indices) {
            for (c in columns.indices) {
                val value = valueAt(r, c) ?: continue
                if (best == null || value > bestValue) {
                    best = r to c
                    bestValue = value
                }
            }
        }
        best
    }
}

/** Component tokens and the pure rules of MonarchHeatmap. */
internal object MonarchHeatmapTokens {
    /** The largest side of a cell: 36dp, as on the web. */
    val MaxCell: Dp = MonarchTokens.Control.md.dp

    /** The gap between cells. */
    val Gap: Dp = MonarchTokens.Space.space0_5.dp

    /** The smallest step of a cell and its gap. */
    val MinStep: Dp = MonarchTokens.Space.space1.dp

    /** The space between the row labels and the cells, and the column labels and the cells. */
    val LabelGap: Dp = MonarchTokens.Space.space2.dp

    /** The corner radius of a cell. */
    val CellRadius: Dp = MonarchTokens.Radius.xs.dp

    /** The outline of a cell without data, and of the selected cell. */
    val EmptyStroke: Dp = MonarchTokens.Space.px.dp
    val SelectedStroke: Dp = MonarchTokens.Space.space0_5.dp

    /** The inset of a value's surface chip from the cell's sides, and its padding around the text. */
    val ChipInset: Dp = MonarchTokens.Space.space0_5.dp

    /** The side of a legend swatch, the gap between swatches and between the swatches and the words. */
    val Swatch: Dp = MonarchTokens.Space.space3.dp
    val SwatchGap: Dp = MonarchTokens.Space.space0_5.dp
    val LegendGap: Dp = MonarchTokens.Space.space1_5.dp

    /** The space between the matrix, the details and the legend. */
    val SectionGap: Dp = MonarchTokens.Space.space2.dp

    /** The smallest kept height of the chart. */
    val MinHeight: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space6).dp

    /** The largest share of the width the row labels may take. */
    const val ROW_LABEL_SHARE: Float = 0.35f

    /** The share of a cell side kept as corner radius at most: a quarter. */
    const val RADIUS_SHARE: Float = 0.25f

    /** The share of the data colour in each level from 1 to 3; level 4 is the data colour itself. */
    private const val LEVEL_STEP = 0.25f

    /**
     * The fill of a level: the muted background, three mixes of the data colour (in Oklab, as the web's
     * `color-mix(in oklab …)`), then the colour itself. No data has no fill.
     */
    fun levelColor(level: Int?, slot: MonarchChartColor, colors: MonarchColors): Color = when (level) {
        null -> Color.Transparent
        0 -> colors.bgMuted
        HeatmapLevels.TOP -> MonarchChartLegendTokens.color(slot, colors)
        else -> lerp(colors.bgMuted, MonarchChartLegendTokens.color(slot, colors), level * LEVEL_STEP)
    }

    /** The outline of a cell or swatch without data. */
    fun emptyOutline(colors: MonarchColors): Color = colors.borderSubtle

    /** The outline of the No data swatch of the legend, stronger than a cell's so it reads at its small size. */
    fun swatchOutline(colors: MonarchColors): Color = colors.borderDefault

    /** The outline of the selected cell. */
    fun selectedOutline(colors: MonarchColors): Color = colors.textDefault

    /** The text of a value in a cell and of the axis labels and legend. */
    fun valueColor(colors: MonarchColors): Color = colors.textDefault
    fun labelColor(colors: MonarchColors): Color = colors.textMuted

    /** The chip behind a value on the two strongest levels, where no text colour reads on every data colour. */
    fun chipColor(colors: MonarchColors): Color = colors.bgSurface

}

/** The geometry of the matrix: cell steps, label thinning, and positions mirrored right to left. */
internal object HeatmapLayout {
    /** The side of a step (a cell and its gap): the width shared out, between the smallest and the largest. */
    fun step(available: Float, columns: Int, minStep: Float, maxStep: Float): Float =
        if (columns <= 0) 0f else (available / columns).coerceIn(minStep, maxOf(minStep, maxStep))

    /** Shows one column label in every this many, so labels never overlap. */
    fun labelEvery(step: Float, labelWidth: Float): Int =
        if (step <= 0f) 1 else maxOf(1, ceil(labelWidth / step).toInt())

    /** The column under [x] in a row of [columns] steps, measured from the left edge; mirrored right to left. */
    fun columnAt(x: Float, step: Float, columns: Int, rtl: Boolean): Int? {
        if (step <= 0f || columns <= 0) return null
        val width = step * columns
        val fromStart = if (rtl) width - x else x
        if (fromStart < 0f || fromStart >= width) return null
        return floor(fromStart / step).toInt().coerceIn(0, columns - 1)
    }

    /** The left edge of column [column] in a row of [columns] steps; mirrored right to left. */
    fun cellLeft(column: Int, step: Float, cell: Float, columns: Int, rtl: Boolean): Float =
        if (rtl) step * columns - column * step - cell else column * step
}

/** The text of the heatmap: values, what TalkBack reads, the summary and the legend's ranges. */
internal object HeatmapText {
    /** The value as written: the cell's label, the formatted value or No data, followed by the note. */
    fun valueText(cell: MonarchHeatmapCell, words: HeatmapWords): String {
        val value = cell.value?.takeIf { it.isFinite() }
        val text = when {
            cell.valueLabel != null -> cell.valueLabel
            value != null -> words.format(value)
            else -> words.noData
        }
        return if (cell.note != null) String.format(words.locale, words.withNote, text, cell.note) else text
    }

    /** What TalkBack reads for a cell: its row, column and value. */
    fun describe(model: HeatmapModel, row: Int, column: Int, words: HeatmapWords): String = String.format(
        words.locale,
        words.cell,
        model.rows[row].label,
        model.columns[column].label,
        valueText(model.cellAt(row, column), words),
    )

    /** The generated summary: the size of the matrix and its highest cell. */
    fun summary(model: HeatmapModel, words: HeatmapWords): String {
        val size = String.format(
            words.locale,
            words.size,
            words.rowCount(model.rows.size),
            words.columnCount(model.columns.size),
        )
        val highest = model.highest?.let { (r, c) ->
            String.format(words.locale, words.highest, describe(model, r, c, words))
        }
        return listOf(size, highest ?: words.noData).joinToString(". ")
    }

    /** The range of each legend step from level 0 to 4, written with the scale's cut points. */
    fun legendSteps(scale: HeatmapScale, words: HeatmapWords): List<String> {
        val (a, b, c) = scale.thresholds.map(words.format)
        val first = scale.thresholds[0]
        return listOf(
            words.format(0.0),
            if (first > 0) {
                String.format(words.locale, words.rangeBelow, a)
            } else {
                String.format(words.locale, words.above, words.format(0.0))
            },
            String.format(words.locale, words.range, a, b),
            String.format(words.locale, words.range, b, c),
            String.format(words.locale, words.rangeAbove, c),
        )
    }
}

/** The words of the Less to More legend. */
internal class HeatmapLegendWords(val name: String, val less: String, val more: String, val steps: List<String>)
