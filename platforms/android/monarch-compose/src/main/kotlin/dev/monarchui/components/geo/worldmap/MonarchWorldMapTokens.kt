package dev.monarchui.components.geo.worldmap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.components.charts.chartframe.MonarchChartFrameStatus
import dev.monarchui.components.charts.chartframe.MonarchChartTable
import dev.monarchui.components.charts.chartframe.MonarchChartTableSeries
import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.components.charts.chartlegend.MonarchChartLegendTokens
import dev.monarchui.components.charts.heatmap.HeatmapScale
import dev.monarchui.components.charts.heatmap.MonarchHeatmapTokens
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale
import kotlin.math.sqrt

/** The resolved strings and formats of a world map. Templates use `%1$s`-style placeholders. */
internal class WorldMapWords(
    val locale: Locale,
    val format: (Double) -> String,
    val name: (String) -> String,
    val countryValue: String,
    val noValue: String,
)

/** Component tokens and the pure rules of MonarchWorldMap. */
internal object MonarchWorldMapTokens {
    /** The least height kept for the map in every status. */
    val MinHeight: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space6).dp

    /** The gap between the map, the country details, the legend, the tiles and the note. */
    val SectionGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between tiles, and between a tile's label and value. */
    val TileGap: Dp = MonarchTokens.Space.space2.dp
    val TileContentGap: Dp = MonarchTokens.Space.space1_5.dp

    /** The padding of a tile. */
    val TileHorizontalPadding: Dp = MonarchTokens.Space.space2_5.dp
    val TileVerticalPadding: Dp = MonarchTokens.Space.space1.dp

    /** The line between countries: half the border width. */
    val CountryBorder: Dp = (MonarchTokens.Space.px / 2).dp

    /** The outline of the inspected country or mark. */
    val ActiveBorder: Dp = MonarchTokens.Space.space0_5.dp

    /** The outline of a dot or bubble. */
    val MarkBorder: Dp = MonarchTokens.Space.px.dp

    /** The side of the invisible TalkBack element of each country. */
    val CountryNode: Dp = MonarchTokens.Space.space2.dp

    /** How far from a country's centre a tap still finds it: half the minimum touch target. */
    val TapRadius: Dp = MonarchTouchTarget.Minimum / 2

    /** The radius of the dot of a country too small for its outline, in map units. */
    const val DOT_RADIUS: Float = 3.5f

    /** The radius of the smallest bubble, and how much larger the largest one is, in map units. */
    const val BUBBLE_MIN_RADIUS: Float = 4f
    const val BUBBLE_RADIUS_RANGE: Float = 22f

    /** How much of the data colour a bubble mixes into the surface, and the inspected bubble. */
    const val BUBBLE_MIX: Float = 0.25f
    const val ACTIVE_BUBBLE_MIX: Float = 0.5f

    /** How many countries the summary names. */
    const val TOP_COUNT: Int = 3

    /** Width over height of the map. */
    const val ASPECT_RATIO: Float = WorldMapGeometry.WIDTH / WorldMapGeometry.HEIGHT


    /** The fill of a country or dot: the muted background without a level, else the heatmap's step. */
    fun fill(level: Int?, slot: MonarchChartColor, colors: MonarchColors): Color =
        if (level == null) colors.bgMuted else MonarchHeatmapTokens.levelColor(level, slot, colors)

    /** The fill of a bubble: the data colour mixed into the surface, stronger when inspected. */
    fun bubbleFill(active: Boolean, slot: MonarchChartColor, colors: MonarchColors): Color = lerp(
        colors.bgSurface,
        MonarchChartLegendTokens.color(slot, colors),
        if (active) ACTIVE_BUBBLE_MIX else BUBBLE_MIX,
    )

    /** The line between countries and around dots: the surface, so neighbours read apart. */
    fun border(colors: MonarchColors): Color = colors.bgSurface

    /** The outline of a bubble: the data colour. */
    fun bubbleBorder(slot: MonarchChartColor, colors: MonarchColors): Color =
        MonarchChartLegendTokens.color(slot, colors)

    /** The outline of the inspected country or mark. */
    fun activeBorder(colors: MonarchColors): Color = colors.textDefault

    /** The details text, and the tile and note texts. */
    fun detailColor(colors: MonarchColors): Color = colors.textDefault
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The fill and border of a tile. */
    fun tileFill(colors: MonarchColors): Color = colors.bgSubtle
    fun tileBorder(colors: MonarchColors): Color = colors.borderSubtle
}

/** The data rules of MonarchWorldMap: cleaning, placing, the scale, the marks and hit testing. */
internal object WorldMapData {

    /** The values to show: trimmed upper-case codes, finite values, the last entry per code; largest first. */
    fun rows(data: List<MonarchWorldMapDatum>): List<MonarchWorldMapDatum> {
        val byCode = LinkedHashMap<String, MonarchWorldMapDatum>()
        data.forEach { datum ->
            val code = datum.country.trim().uppercase(Locale.ROOT)
            if (code.isNotEmpty() && datum.value.isFinite()) {
                byCode.remove(code)
                byCode[code] = MonarchWorldMapDatum(code, datum.value, datum.details)
            }
        }
        return byCode.values.sortedWith(compareByDescending<MonarchWorldMapDatum> { it.value }.thenBy { it.country })
    }

    /** The rows the map can draw, in value order. */
    fun placed(rows: List<MonarchWorldMapDatum>): List<MonarchWorldMapDatum> =
        rows.filter { WorldMapGeometry.isPlaced(it.country) }

    /** The rows with codes the map cannot draw. */
    fun unplaced(rows: List<MonarchWorldMapDatum>): List<MonarchWorldMapDatum> =
        rows.filterNot { WorldMapGeometry.isPlaced(it.country) }

    /** The status shown: empty when the map is ready but has nothing to show. */
    fun status(
        status: MonarchChartFrameStatus,
        rows: List<MonarchWorldMapDatum>,
        extras: List<MonarchWorldMapExtra>,
    ): MonarchChartFrameStatus =
        if (status == MonarchChartFrameStatus.Ready && rows.isEmpty() && extras.isEmpty()) {
            MonarchChartFrameStatus.Empty
        } else {
            status
        }

    /** The largest value, never below zero. */
    fun maximum(rows: List<MonarchWorldMapDatum>): Double = maxOf(0.0, rows.maxOfOrNull { it.value } ?: 0.0)

    /** The colour scale: three given cut points, or quarters of the largest value, as in the heatmap. */
    fun scale(rows: List<MonarchWorldMapDatum>, thresholds: List<Double>?): HeatmapScale =
        HeatmapScale.of(rows.map { it.value }, thresholds)

    /**
     * The countries drawn as circles, largest first so smaller ones sit on
     * top: every placed country for bubbles, and countries without an
     * outline for the choropleth.
     */
    fun marks(placed: List<MonarchWorldMapDatum>, variant: MonarchWorldMapVariant): List<MonarchWorldMapDatum> =
        placed.filter {
            WorldMapGeometry.centroids.containsKey(it.country) &&
                (variant == MonarchWorldMapVariant.Bubble || !WorldMapGeometry.shapes.containsKey(it.country))
        }

    /** The radius of a mark in map units: a dot, or a bubble whose area grows with the value. */
    fun markRadius(value: Double, maximum: Double, variant: MonarchWorldMapVariant): Float = when {
        variant != MonarchWorldMapVariant.Bubble -> MonarchWorldMapTokens.DOT_RADIUS
        maximum <= 0.0 -> MonarchWorldMapTokens.BUBBLE_MIN_RADIUS
        else -> MonarchWorldMapTokens.BUBBLE_MIN_RADIUS +
            sqrt(maxOf(0.0, value) / maximum).toFloat() * MonarchWorldMapTokens.BUBBLE_RADIUS_RANGE
    }

    /** The level of a country's fill: `null` (neutral) without a value or for bubbles. */
    fun level(datum: MonarchWorldMapDatum?, scale: HeatmapScale, variant: MonarchWorldMapVariant): Int? =
        if (variant == MonarchWorldMapVariant.Choropleth && datum != null) scale.level(datum.value) else null


    /**
     * The country at [point] in map units: a dot or bubble first (the
     * smallest on top), then the outline of a country with a value, then the
     * nearest country with a value within [tolerance] map units.
     */
    fun hit(
        point: Offset,
        placed: List<MonarchWorldMapDatum>,
        variant: MonarchWorldMapVariant,
        maximum: Double,
        tolerance: Float,
    ): String? {
        marks(placed, variant).asReversed().forEach { datum ->
            val centre = WorldMapGeometry.centroids[datum.country] ?: return@forEach
            if ((centre - point).getDistance() <= markRadius(datum.value, maximum, variant)) return datum.country
        }
        placed.firstOrNull { datum ->
            WorldMapGeometry.shapes[datum.country]?.let { WorldMapGeometry.contains(it, point) } == true
        }?.let { return it.country }
        return placed
            .mapNotNull { datum ->
                WorldMapGeometry.anchor(datum.country)?.let { datum.country to (it - point).getDistance() }
            }
            .filter { it.second <= tolerance }
            .minByOrNull { it.second }
            ?.first
    }
}

/** The text of MonarchWorldMap: what TalkBack reads, the summary, the note and the table. */
internal object WorldMapText {

    /** A country as read and written: "Germany: 300, Users 90". */
    fun describe(datum: MonarchWorldMapDatum, words: WorldMapWords): String {
        val parts = listOf(words.format(datum.value)) + datum.details.map { "${it.label} ${it.value}" }
        return String.format(words.locale, words.countryValue, words.name(datum.country), parts.joinToString(", "))
    }

    /** The largest values as text: "United States 1,200, Germany 300, India 150". */
    fun top(rows: List<MonarchWorldMapDatum>, words: WorldMapWords): String =
        rows.take(MonarchWorldMapTokens.TOP_COUNT)
            .joinToString(", ") { "${words.name(it.country)} ${words.format(it.value)}" }

    /** The note for codes the map cannot draw, or `null` when every code is placed. */
    fun unplacedNote(
        unplaced: List<MonarchWorldMapDatum>,
        words: WorldMapWords,
        template: (names: String, total: String) -> String,
    ): String? {
        if (unplaced.isEmpty()) return null
        val names = unplaced.joinToString(", ") { words.name(it.country) }
        return template(names, words.format(unplaced.sumOf { it.value }))
    }

    /** The labels of every detail line, in the order they first appear. */
    fun detailLabels(rows: List<MonarchWorldMapDatum>): List<String> =
        rows.flatMap { row -> row.details.map { it.label } }.distinct()

    /**
     * The data table: one row per country, then one per extra; a value column
     * and one column per detail label. A detail is text, so its column keeps
     * the text and marks a present detail with a number (the text read as a
     * number, or NaN) so the table writes it instead of a gap.
     */
    fun table(
        rows: List<MonarchWorldMapDatum>,
        extras: List<MonarchWorldMapExtra>,
        words: WorldMapWords,
        countryTitle: String,
        valueTitle: String,
    ): MonarchChartTable {
        val values = MonarchChartTableSeries(
            name = valueTitle,
            values = rows.map { it.value } + extras.map { it.value },
            formattedValues = rows.map { words.format(it.value) } + extras.map { words.format(it.value) },
        )
        val details = detailLabels(rows).map { label ->
            val texts = rows.map { row -> row.details.firstOrNull { it.label == label }?.value }
            MonarchChartTableSeries(
                name = label,
                values = texts.map { text -> text?.let { it.toDoubleOrNull() ?: Double.NaN } } + extras.map { null },
                formattedValues = texts.map { it.orEmpty() } + extras.map { "" },
            )
        }
        return MonarchChartTable(
            categoryTitle = countryTitle,
            categories = rows.map { words.name(it.country) } + extras.map { it.label },
            series = listOf(values) + details,
        )
    }

    /** A country's name: the app's, else the locale's name of the region, else the code. */
    fun countryName(code: String, locale: Locale, custom: ((String) -> String)?): String {
        if (custom != null) return custom(code)
        val region = runCatching { Locale.Builder().setRegion(code).build() }.getOrNull() ?: return code
        return region.getDisplayCountry(locale).ifBlank { code }
    }
}
