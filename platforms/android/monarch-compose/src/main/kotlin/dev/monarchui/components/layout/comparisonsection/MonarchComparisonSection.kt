package dev.monarchui.components.layout.comparisonsection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.billing.pricingtable.MonarchPricingFeature
import dev.monarchui.components.billing.pricingtable.MonarchPricingPlan
import dev.monarchui.components.billing.pricingtable.MonarchPricingTable
import dev.monarchui.components.billing.pricingtable.MonarchPricingTableLabels
import dev.monarchui.components.billing.pricingtable.MonarchPricingTableLayout
import dev.monarchui.components.layout.marketingsection.MarketingHeader
import dev.monarchui.components.layout.marketingsection.MonarchMarketingAlign
import dev.monarchui.components.layout.marketingsection.MonarchMarketingSectionTone
import dev.monarchui.components.layout.pricingsection.ComparisonScroller
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchTheme

/**
 * A band that compares the product with alternatives: a header, a table of capabilities by option, cards that say
 * whom each option suits, a note on sources and the actions.
 *
 * The table is `MonarchPricingTable` in its grid layout without prices: options as columns (the highlighted one in
 * the accent, with its badge), capabilities as rows under group headings, and each cell read by TalkBack as the
 * option, the capability and the value. Values are a check, a dash or text, never colour alone.
 *
 * @param columns the options, in display order; normally the product first.
 * @param rows the capabilities, in display order.
 * @param modifier the modifier of the band.
 * @param title the title of the band, read as a heading.
 * @param eyebrow a short line above the title, in the accent.
 * @param description the muted line under the title.
 * @param bestFor cards under the table that say whom each option suits.
 * @param note a muted note under the cards: when the comparison was checked and against which sources.
 * @param align the header at the start edge (the default) or centred.
 * @param tone no fill, or the subtle background fill.
 * @param caption the spoken name of the table. `null` uses [title], then "Plan comparison".
 * @param actions the buttons at the end, such as one primary `MonarchButton`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchComparisonSection(
    columns: List<MonarchComparisonColumn>,
    rows: List<MonarchComparisonRow>,
    modifier: Modifier = Modifier,
    title: String? = null,
    eyebrow: String? = null,
    description: String? = null,
    bestFor: List<MonarchComparisonBestFor> = emptyList(),
    note: String? = null,
    align: MonarchMarketingAlign = MonarchMarketingAlign.Start,
    tone: MonarchMarketingSectionTone = MonarchMarketingSectionTone.Default,
    caption: String? = null,
    actions: (@Composable FlowRowScope.() -> Unit)? = null,
) {
    val colors = MonarchTheme.colors
    val centred = align == MonarchMarketingAlign.Center
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true }
            .then(
                if (tone == MonarchMarketingSectionTone.Subtle) {
                    Modifier
                        .background(colors.bgSubtle, MonarchTheme.shapes.lg)
                        .padding(MonarchComparisonSectionTokens.NotePadding)
                } else {
                    Modifier
                },
            ),
        verticalArrangement = Arrangement.spacedBy(MonarchComparisonSectionTokens.Gap),
        horizontalAlignment = if (centred) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        if (title != null) {
            MarketingHeader(title = title, eyebrow = eyebrow, description = description, align = align)
        }
        ComparisonScroller(columns = columns.size + 1) {
            MonarchPricingTable(
                plans = columns.map {
                    MonarchPricingPlan(id = it.id, name = it.name, badge = it.badge, highlighted = it.highlighted)
                },
                features = rows.map {
                    MonarchPricingFeature(
                        id = it.id,
                        label = it.label,
                        description = it.description,
                        group = it.group,
                        values = it.values,
                    )
                },
                layout = MonarchPricingTableLayout.Grid,
                showPrices = false,
                showPeriodPicker = false,
                labels = MonarchPricingTableLabels(caption = caption ?: title),
            )
        }
        if (bestFor.isNotEmpty()) BestForCards(bestFor)
        if (note != null) {
            MonarchText(
                text = note,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MonarchComparisonSectionTokens.noteFill(colors), MonarchShapeDefaults.Control)
                    .padding(MonarchComparisonSectionTokens.NotePadding),
                style = MonarchTextStyle.Label,
                color = MonarchComparisonSectionTokens.noteText(colors),
            )
        }
        if (actions != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MonarchComparisonSectionTokens.CardGap),
                verticalArrangement = Arrangement.spacedBy(MonarchComparisonSectionTokens.CardGap),
                content = actions,
            )
        }
    }
}

@Composable
private fun BestForCards(cards: List<MonarchComparisonBestFor>) {
    val gap = MonarchComparisonSectionTokens.CardGap
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val perRow = ((maxWidth + gap) / (MonarchComparisonSectionTokens.MinCardWidth + gap)).toInt()
            .coerceIn(1, 2)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            cards.chunked(perRow).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    row.forEach { card -> BestForCard(card, Modifier.weight(1f).fillMaxHeight()) }
                    repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun BestForCard(card: MonarchComparisonBestFor, modifier: Modifier) {
    val colors = MonarchTheme.colors
    val shape = MonarchShapeDefaults.Container
    Column(
        modifier = modifier
            .background(MonarchComparisonSectionTokens.cardFill(card.highlighted, colors), shape)
            .border(
                MonarchShapeDefaults.BorderWidth,
                MonarchComparisonSectionTokens.cardBorder(card.highlighted, colors),
                shape,
            )
            .semantics(mergeDescendants = true) {}
            .padding(MonarchComparisonSectionTokens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(MonarchComparisonSectionTokens.TextGap),
    ) {
        MonarchText(text = card.title, style = MonarchTextStyle.BodyStrong, heading = true)
        MonarchText(text = card.description, style = MonarchTextStyle.Label, color = colors.textMuted)
    }
}
