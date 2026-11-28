package com.rookhq.monarch.components.geo.worldmap

import android.icu.text.NumberFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.R
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrame
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.heatmap.HeatmapLegend
import com.rookhq.monarch.components.charts.heatmap.HeatmapLegendWords
import com.rookhq.monarch.components.charts.heatmap.HeatmapScale
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Locale

/**
 * Values by country on a simplified world map: a choropleth in one data
 * colour with a Less to More legend, or bubbles sized by value.
 *
 * Built by Monarch inside `MonarchChartFrame`, drawn on a Compose `Canvas`
 * from a bundled, simplified Natural Earth outline (1:110m, Antarctica left
 * out), not a map SDK: Google Maps does not fill countries from data.
 * Countries too small for an outline (Singapore, Malta) get a dot. Values
 * fall into the heatmap's five steps; codes the map cannot place are listed
 * under it, and values that belong to no country ("Global", "EU") are tiles
 * under the map and rows of the table. A tap on a country with a value
 * outlines it and writes its name, value and details under the map,
 * announced politely; a tap that misses a small country finds the nearest
 * country with a value within 24dp.
 *
 * The map is a TalkBack traversal group named by the generated summary ("5
 * countries. Highest: United States 1,200, Germany 300, India 150."), with
 * one element per country with a value, read largest first as "Germany: 300,
 * Users 90"; with [onSelect] each has a Select action. The legend reads the
 * range of each step, and "View as table" lists every value. The map never
 * mirrors right to left.
 *
 * @param title the chart's name, shown as the frame's heading.
 * @param data one value per ISO 3166-1 alpha-2 code. Codes are trimmed and upper-cased; a later entry for the
 * same code wins; values that are not finite are left out.
 * @param modifier the modifier of the frame.
 * @param description one line under the title, such as the period.
 * @param status what the frame shows. A ready map without values or extras shows the empty state.
 * @param variant filled countries, or bubbles whose area grows with the value on a neutral map.
 * @param color the data colour of the steps or bubbles.
 * @param thresholds three ascending values where levels 2, 3 and 4 start. `null` uses quarters of the largest
 * value.
 * @param extras values that belong to no country, shown as tiles under the map and as table rows.
 * @param showLegend shows the Less to More legend under a choropleth.
 * @param summary replaces the generated summary that names the map for TalkBack.
 * @param emptyMessage the message of the empty state.
 * @param footer notes under the chart, such as the source.
 * @param countryName names a country from its code. `null` uses the region name in the current locale, then the
 * code.
 * @param formatValue writes a value. `null` writes a number in the current locale.
 * @param onRetry shows Try again in the error status.
 * @param onSelect called with a country's datum when it is tapped, or chosen with the TalkBack Select action, for
 * drill-in.
 * @param labels replacements for the built-in strings, including the frame's.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
@Suppress("LongParameterList", "LongMethod")
public fun MonarchWorldMap(
    title: String,
    data: List<MonarchWorldMapDatum>,
    modifier: Modifier = Modifier,
    description: String? = null,
    status: MonarchChartFrameStatus = MonarchChartFrameStatus.Ready,
    variant: MonarchWorldMapVariant = MonarchWorldMapVariant.Choropleth,
    color: MonarchChartColor = MonarchChartColor.Data1,
    thresholds: List<Double>? = null,
    extras: List<MonarchWorldMapExtra> = emptyList(),
    showLegend: Boolean = true,
    summary: String? = null,
    emptyMessage: String? = null,
    footer: String? = null,
    countryName: ((String) -> String)? = null,
    formatValue: ((Double) -> String)? = null,
    onRetry: (() -> Unit)? = null,
    onSelect: ((MonarchWorldMapDatum) -> Unit)? = null,
    labels: MonarchWorldMapLabels = MonarchWorldMapLabels(),
) {
    val tokens = MonarchWorldMapTokens
    val locale = LocalConfiguration.current.locales[0]
    val words = worldMapWords(labels, locale, formatValue, countryName)
    val rows = remember(data) { WorldMapData.rows(data) }
    val placed = remember(rows) { WorldMapData.placed(rows) }
    val scale = remember(rows, thresholds) { WorldMapData.scale(rows, thresholds) }
    val byCode = remember(rows) { rows.associateBy { it.country } }
    var active by rememberSaveable { mutableStateOf<String?>(null) }
    val current = active?.takeIf { code -> placed.any { it.country == code } }
    val density = LocalDensity.current
    var measured by remember { mutableStateOf<Dp?>(null) }
    MonarchChartFrame(
        title = title,
        modifier = modifier,
        description = description,
        status = WorldMapData.status(status, rows, extras),
        table = WorldMapText.table(
            rows = rows,
            extras = extras,
            words = words,
            countryTitle = monarchString(labels.country, R.string.monarch_worldmap_country),
            valueTitle = monarchString(labels.value, R.string.monarch_worldmap_value),
        ),
        emptyMessage = emptyMessage,
        footer = footer,
        height = maxOf(tokens.MinHeight, measured ?: tokens.MinHeight),
        onRetry = onRetry,
        labels = labels.frame,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .onSizeChanged { measured = with(density) { it.height.toDp() } },
            verticalArrangement = Arrangement.spacedBy(tokens.SectionGap),
        ) {
            if (rows.isNotEmpty()) {
                val select: (String) -> Unit = { code ->
                    active = code
                    byCode[code]?.let { onSelect?.invoke(it) }
                }
                WorldMapPlot(
                    model = WorldMapPlotModel(
                        byCode = byCode,
                        placed = placed,
                        variant = variant,
                        slot = color,
                        scale = scale,
                        maximum = WorldMapData.maximum(rows),
                        active = current,
                    ),
                    words = words,
                    summary = summary ?: worldMapSummary(rows, words, labels, locale),
                    selectLabel = monarchString(labels.select, R.string.monarch_heatmap_select),
                    onTap = { code -> if (code == null) active = null else select(code) },
                    onSelect = select.takeIf { onSelect != null },
                )
                WorldMapDetail(current?.let(byCode::get)?.let { WorldMapText.describe(it, words) })
                if (showLegend && variant == MonarchWorldMapVariant.Choropleth) {
                    WorldMapLegend(scale, color, labels, words)
                }
            }
            if (extras.isNotEmpty()) {
                WorldMapTiles(extras, words, monarchString(labels.extras, R.string.monarch_worldmap_extras))
            }
            WorldMapUnplaced(WorldMapData.unplaced(rows), words, labels)
        }
    }
}

/** The details of the inspected country, in a polite live region that is always present. */
@Composable
private fun WorldMapDetail(detail: String?) {
    Box(
        Modifier.clearAndSetSemantics {
            liveRegion = LiveRegionMode.Polite
            if (detail != null) contentDescription = detail
        },
    ) {
        if (detail != null) {
            Text(
                text = detail,
                style = MonarchTheme.typography.caption,
                color = MonarchWorldMapTokens.detailColor(MonarchTheme.colors),
            )
        }
    }
}

/** The note that lists the codes the map cannot draw, with their total. */
@Composable
private fun WorldMapUnplaced(
    unplaced: List<MonarchWorldMapDatum>,
    words: WorldMapWords,
    labels: MonarchWorldMapLabels,
) {
    val template = stringResource(R.string.monarch_worldmap_unplaced)
    val note = WorldMapText.unplacedNote(unplaced, words) { names, total ->
        labels.unplaced?.invoke(names, total) ?: String.format(words.locale, template, names, total)
    } ?: return
    Text(
        text = note,
        style = MonarchTheme.typography.caption,
        color = MonarchWorldMapTokens.mutedColor(MonarchTheme.colors),
    )
}

/** The built-in strings and formats, or their replacements. */
@Composable
private fun worldMapWords(
    labels: MonarchWorldMapLabels,
    locale: Locale,
    formatValue: ((Double) -> String)?,
    countryName: ((String) -> String)?,
): WorldMapWords {
    val format = formatValue ?: remember(locale) {
        val number = NumberFormat.getInstance(locale)
        val writer: (Double) -> String = { number.format(it) }
        writer
    }
    return WorldMapWords(
        locale = locale,
        format = format,
        name = { WorldMapText.countryName(it, locale, countryName) },
        countryValue = stringResource(R.string.monarch_worldmap_country_value),
        noValue = monarchString(labels.frame.noValue, R.string.monarch_chartframe_no_value),
    )
}

/** The generated summary, which names the map for TalkBack. */
@Composable
private fun worldMapSummary(
    rows: List<MonarchWorldMapDatum>,
    words: WorldMapWords,
    labels: MonarchWorldMapLabels,
    locale: Locale,
): String {
    val top = WorldMapText.top(rows, words)
    labels.summary?.let { return it(rows.size, top) }
    if (rows.isEmpty()) return words.noValue
    val count = NumberFormat.getInstance(locale).format(rows.size.toLong())
    return LocalResources.current.getQuantityString(R.plurals.monarch_worldmap_summary, rows.size, count, top)
}

/** The Less to More legend of a choropleth, the heatmap's, read as the range of each step. */
@Composable
private fun WorldMapLegend(
    scale: HeatmapScale,
    color: MonarchChartColor,
    labels: MonarchWorldMapLabels,
    words: WorldMapWords,
) {
    val (a, b, c) = scale.thresholds.map(words.format)
    val zero = words.format(0.0)
    val range = monarchString(labels.range, R.string.monarch_heatmap_range)
    val lightest = if (scale.thresholds[0] > 0) {
        String.format(words.locale, monarchString(labels.rangeBelow, R.string.monarch_heatmap_range_below), a)
    } else {
        String.format(words.locale, stringResource(R.string.monarch_heatmap_above), zero)
    }
    HeatmapLegend(
        slot = color,
        showEmpty = false,
        words = HeatmapLegendWords(
            name = monarchString(labels.frame.legend, R.string.monarch_chartlegend_legend),
            less = monarchString(labels.less, R.string.monarch_heatmap_less),
            more = monarchString(labels.more, R.string.monarch_heatmap_more),
            steps = listOf(
                zero,
                lightest,
                String.format(words.locale, range, a, b),
                String.format(words.locale, range, b, c),
                String.format(
                    words.locale,
                    monarchString(labels.rangeAbove, R.string.monarch_heatmap_range_above),
                    c,
                ),
            ),
        ),
    )
}

/** The tiles under the map for values that belong to no country: a group named [name], one element per tile. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorldMapTiles(extras: List<MonarchWorldMapExtra>, words: WorldMapWords, name: String) {
    val colors = MonarchTheme.colors
    val tokens = MonarchWorldMapTokens
    FlowRow(
        modifier = Modifier.semantics {
            isTraversalGroup = true
            contentDescription = name
        },
        horizontalArrangement = Arrangement.spacedBy(tokens.TileGap),
        verticalArrangement = Arrangement.spacedBy(tokens.TileGap),
    ) {
        extras.forEach { extra ->
            Row(
                modifier = Modifier
                    .monarchSurface(
                        MonarchSurfaceColors(tokens.tileFill(colors), colors.textDefault, tokens.tileBorder(colors)),
                        MonarchShapeDefaults.Control,
                    )
                    .padding(horizontal = tokens.TileHorizontalPadding, vertical = tokens.TileVerticalPadding)
                    .semantics(mergeDescendants = true) { },
                horizontalArrangement = Arrangement.spacedBy(tokens.TileContentGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(extra.label, style = MonarchTheme.typography.caption, color = tokens.mutedColor(colors))
                Text(
                    words.format(extra.value),
                    style = MonarchTheme.typography.label,
                    color = tokens.detailColor(colors),
                )
            }
        }
    }
}
