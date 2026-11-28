package com.rookhq.monarch.components.charts.stackedproportionbar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.R
import com.rookhq.monarch.components.charts.chartlegend.ChartLegendMarkerShape
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegendMarker
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegendTokens
import com.rookhq.monarch.components.charts.donutchart.MonarchDonutChartSlices
import com.rookhq.monarch.components.charts.donutchart.MonarchDonutChartWords
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchChartPercentFormat
import com.rookhq.monarch.internal.rememberMonarchChartValueFormat
import com.rookhq.monarch.theme.MonarchTheme

/**
 * One horizontal bar split into proportions, with a legend that lists each part's value and share.
 *
 * Built by Monarch: Material 3 has no proportion bar. The bar is a Compose `Canvas` of the parts in order from
 * the start edge, each at least 4dp wide with 2dp between them, in a rounded track; a total larger than the sum
 * leaves empty track (a capacity, such as storage). Parts take the theme's data colours in order, or, when every
 * part has a tone, the solid colour of their status. The legend reuses the chart legend's markers.
 *
 * For TalkBack, while the legend is shown the bar is hidden and the legend is a group named by [label] whose
 * entries are read as the part's name, value and share ("Replied 182 · 9%"). Without the legend the bar is one
 * element named by [label] whose state lists every part. With no data, one element reads [label] and
 * "No data yet". The bar mirrors in right to left. It does not animate.
 *
 * @param label names the bar and its legend for TalkBack, such as "Lead pipeline". Already localised.
 * @param segments the parts, in display order.
 * @param modifier the modifier of the bar and legend.
 * @param total the size of the whole, for a capacity. The sum of the parts when `null` or smaller than the sum.
 * @param remainderLabel the legend name of the empty rest of the whole, such as "Free". Without it the rest is
 * drawn but not listed.
 * @param legendContent what each legend entry shows after its label.
 * @param size the thickness of the bar: 4, 8 or 12dp.
 * @param showLegend whether the legend is drawn. Without it TalkBack still reads every part from the bar.
 * @param hideZero whether parts with a value of 0 are left out of the bar and the legend.
 * @param percentDigits the decimal places of shares. A share too small to write reads "<1%".
 * @param emptyLabel replaces "No data yet", shown when no part has a value.
 * @param formatValue writes a value. `null` writes numbers in the current locale.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchStackedProportionBar(
    label: String,
    segments: List<MonarchStackedProportionBarSegment>,
    modifier: Modifier = Modifier,
    total: Double? = null,
    remainderLabel: String? = null,
    legendContent: MonarchStackedProportionBarLegendContent = MonarchStackedProportionBarLegendContent.Both,
    size: MonarchStackedProportionBarSize = MonarchStackedProportionBarSize.Md,
    showLegend: Boolean = true,
    hideZero: Boolean = true,
    percentDigits: Int = 0,
    emptyLabel: String? = null,
    formatValue: ((Double) -> String)? = null,
) {
    val model = remember(segments, total, hideZero) {
        MonarchStackedProportionBarGeometry.model(segments, total, hideZero)
    }
    val format = rememberMonarchChartValueFormat(formatValue)
    val percentFormat = rememberMonarchChartPercentFormat(percentDigits)
    val words = shareWords()
    val entryPattern = stringResource(R.string.monarch_stackedproportionbar_entry)
    val colors = MonarchTheme.colors
    val entries = remember(model, remainderLabel) {
        model.parts + listOfNotNull(
            remainderLabel?.takeIf { model.remainder > 0 }?.let {
                MonarchStackedProportionBarPart(
                    MonarchStackedProportionBarGeometry.REMAINDER_KEY, it, model.remainder, MonarchChartColor.Neutral,
                    null, remainder = true,
                )
            },
        )
    }
    val share: (Double) -> String = {
        MonarchDonutChartSlices.share(it, model.whole, percentDigits, percentFormat, words)
    }
    val both: (MonarchStackedProportionBarPart) -> String = {
        words.valueAndShare.format(format(it.value), share(it.value))
    }
    val spoken: (MonarchStackedProportionBarPart) -> String = { entryPattern.format(it.label, both(it)) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MonarchStackedProportionBarTokens.Gap),
    ) {
        if (model.empty) {
            val empty = monarchString(emptyLabel, R.string.monarch_stackedproportionbar_empty)
            Column(
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics {
                    contentDescription = label
                    stateDescription = empty
                },
                verticalArrangement = Arrangement.spacedBy(MonarchStackedProportionBarTokens.Gap),
            ) {
                Bar(model, size, MonarchStackedProportionBarTokens.track(colors), emptyList())
                Text(
                    text = empty,
                    style = MonarchTheme.typography.caption,
                    color = MonarchStackedProportionBarTokens.emptyText(colors),
                )
            }
            return@Column
        }
        val partColors = model.parts.map { MonarchStackedProportionBarTokens.partColor(it.slot, it.tone, colors) }
        val barSemantics = if (showLegend) {
            Modifier.clearAndSetSemantics { }
        } else {
            Modifier.clearAndSetSemantics {
                contentDescription = label
                stateDescription = entries.joinToString(Separator, transform = spoken)
            }
        }
        Bar(model, size, MonarchStackedProportionBarTokens.track(colors), partColors, barSemantics)
        if (showLegend) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().semantics {
                    isTraversalGroup = true
                    contentDescription = label
                },
                horizontalArrangement = Arrangement.spacedBy(MonarchChartLegendTokens.ItemSpacing),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                entries.forEach { part ->
                    val value = when (legendContent) {
                        MonarchStackedProportionBarLegendContent.Both -> both(part)
                        MonarchStackedProportionBarLegendContent.Value -> format(part.value)
                        MonarchStackedProportionBarLegendContent.Percent -> share(part.value)
                        MonarchStackedProportionBarLegendContent.None -> null
                    }
                    val marker = if (part.remainder) {
                        MonarchStackedProportionBarTokens.remainderMarker(colors)
                    } else {
                        MonarchStackedProportionBarTokens.partColor(part.slot, part.tone, colors)
                    }
                    LegendEntry(part.label, value, marker, part.remainder, spoken(part))
                }
            }
        }
    }
}

/** The bar: the rounded track and each part, drawn from the start edge. [modifier] carries its semantics. */
@Composable
private fun Bar(
    model: MonarchStackedProportionBarModel,
    size: MonarchStackedProportionBarSize,
    track: Color,
    partColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(MonarchStackedProportionBarTokens.height(size)),
    ) {
        val radius = CornerRadius(this.size.height / 2)
        val shape = Path().apply {
            addRoundRect(RoundRect(0f, 0f, this@Canvas.size.width, this@Canvas.size.height, radius))
        }
        clipPath(shape) {
            drawRect(track)
            if (partColors.isEmpty()) return@clipPath
            val spans = MonarchStackedProportionBarGeometry.spans(
                values = model.parts.map { it.value },
                whole = model.whole,
                width = this.size.width,
                minPart = MonarchStackedProportionBarTokens.MinPart.toPx(),
                gap = MonarchStackedProportionBarTokens.PartGap.toPx(),
            )
            withTransform({ if (rtl) scale(-1f, 1f, Offset(this@clipPath.size.width / 2, 0f)) }) {
                spans.forEachIndexed { index, (start, length) ->
                    if (length > 0f) drawRect(partColors[index], Offset(start, 0f), Size(length, this.size.height))
                }
            }
        }
    }
}

/** One legend entry: the marker (outlined for the remainder), the label and the value, read as one element. */
@Composable
private fun LegendEntry(label: String, value: String?, marker: Color, outlined: Boolean, spoken: String) {
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(MonarchChartLegendTokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChartLegendMarkerShape(MonarchChartLegendMarker.Square, marker, dashed = outlined)
        Text(
            text = label,
            modifier = Modifier.weight(1f, fill = false),
            style = MonarchTheme.typography.caption,
            color = MonarchTheme.colors.textDefault,
        )
        if (value != null) {
            Text(
                text = value,
                style = MonarchTheme.typography.caption,
                fontWeight = FontWeight.Medium,
                color = MonarchTheme.colors.textDefault,
            )
        }
    }
}

/** The share patterns, in the donut chart's word holder whose share rule this bar reuses. */
@Composable
private fun shareWords(): MonarchDonutChartWords {
    val valueAndShare = stringResource(R.string.monarch_stackedproportionbar_value_and_share)
    val entry = stringResource(R.string.monarch_stackedproportionbar_entry)
    return MonarchDonutChartWords(
        entry = entry,
        namedValue = entry,
        valueAndShare = valueAndShare,
        below = stringResource(R.string.monarch_stackedproportionbar_share_below),
        above = stringResource(R.string.monarch_stackedproportionbar_share_above),
        total = "",
    )
}

private const val Separator = ", "
