package com.rookhq.monarch.components.layout.pricingsection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.rookhq.monarch.R
import com.rookhq.monarch.components.billing.plancard.MonarchPlanCard
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingFeature
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingPeriod
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingPlan
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingTable
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingTableLabels
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingTableLayout
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.layout.marketingsection.MarketingHeader
import com.rookhq.monarch.components.layout.marketingsection.MonarchMarketingAlign
import com.rookhq.monarch.components.layout.marketingsection.MonarchMarketingSectionTone
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A pricing band: a header, a billing period picker, one card per plan with its price, features and action, a
 * note, and an optional feature comparison below.
 *
 * Built from `MonarchSegmentedControl`, `MonarchPlanCard` and, for the comparison, `MonarchPricingTable` in its
 * grid layout without prices. Cards are stacked, so each plan stays readable on a phone and at large font sizes.
 * Prices are shown exactly as given; a plan without a price for the chosen period shows its fallback price
 * without the period's suffix and note.
 *
 * @param plans the plans, in display order.
 * @param modifier the modifier of the band.
 * @param title the title of the band, read as a heading.
 * @param eyebrow a short line above the title, in the accent.
 * @param description the muted line under the title.
 * @param periods billing periods; with two or more the period picker is shown.
 * @param period the chosen period id. `null` lets the section keep its own choice, starting with the first.
 * @param onPeriodChange called with the id of the period the user chooses.
 * @param comparison features compared across the plans; empty shows no comparison.
 * @param note a muted line under the cards, such as "Prices shown before taxes."
 * @param align the header centred (the default) or at the start edge.
 * @param tone no fill, or the subtle background fill.
 * @param comparisonTitle the heading of the comparison. `null` uses "Compare plans".
 * @param billingPeriodLabel the spoken name of the period picker. `null` uses "Billing period".
 * @param action the call to action of each plan, normally one full-width `MonarchButton`.
 */
@Composable
public fun MonarchPricingSection(
    plans: List<MonarchPricingSectionPlan>,
    modifier: Modifier = Modifier,
    title: String? = null,
    eyebrow: String? = null,
    description: String? = null,
    periods: List<MonarchPricingPeriod> = emptyList(),
    period: String? = null,
    onPeriodChange: ((String) -> Unit)? = null,
    comparison: List<MonarchPricingFeature> = emptyList(),
    note: String? = null,
    align: MonarchMarketingAlign = MonarchMarketingAlign.Center,
    tone: MonarchMarketingSectionTone = MonarchMarketingSectionTone.Default,
    comparisonTitle: String? = null,
    billingPeriodLabel: String? = null,
    action: (@Composable (plan: MonarchPricingSectionPlan) -> Unit)? = null,
) {
    val colors = MonarchTheme.colors
    var ownPeriod by rememberSaveable { mutableStateOf(periods.firstOrNull()?.id) }
    val chosen = periods.firstOrNull { it.id == (period ?: ownPeriod) } ?: periods.firstOrNull()
    val pickerName = monarchString(billingPeriodLabel, R.string.monarch_pricingsection_billing_period)
    val compareTitle = monarchString(comparisonTitle, R.string.monarch_pricingsection_comparison_title)
    val textAlign = if (align == MonarchMarketingAlign.Center) TextAlign.Center else TextAlign.Start
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true }
            .then(
                if (tone == MonarchMarketingSectionTone.Subtle) {
                    Modifier
                        .background(colors.bgSubtle, MonarchTheme.shapes.lg)
                        .padding(MonarchPricingSectionTokens.CardGap)
                } else {
                    Modifier
                },
            ),
        verticalArrangement = Arrangement.spacedBy(MonarchPricingSectionTokens.Gap),
        horizontalAlignment = if (align == MonarchMarketingAlign.Center) {
            Alignment.CenterHorizontally
        } else {
            Alignment.Start
        },
    ) {
        if (title != null) {
            MarketingHeader(title = title, eyebrow = eyebrow, description = description, align = align)
        }
        if (periods.size > 1 && chosen != null) {
            MonarchSegmentedControl(
                options = periods.map { it.label },
                selectedIndex = periods.indexOf(chosen),
                onSelect = { index ->
                    val id = periods[index].id
                    ownPeriod = id
                    onPeriodChange?.invoke(id)
                },
                modifier = Modifier.semantics { contentDescription = pickerName },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(MonarchPricingSectionTokens.CardGap)) {
            plans.forEach { plan -> PlanCard(plan, chosen, action) }
        }
        if (note != null) {
            MonarchText(
                text = note,
                modifier = Modifier.fillMaxWidth(),
                style = MonarchTextStyle.Label,
                color = MonarchPricingSectionTokens.note(colors),
                textAlign = textAlign,
            )
        }
        if (comparison.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MonarchPricingSectionTokens.ComparisonGap),
            ) {
                MonarchText(text = compareTitle, style = MonarchTextStyle.Subheading, heading = true)
                ComparisonScroller(columns = plans.size + 1) {
                    MonarchPricingTable(
                        plans = plans.map {
                            MonarchPricingPlan(id = it.id, name = it.name, highlighted = it.highlighted)
                        },
                        features = comparison,
                        layout = MonarchPricingTableLayout.Grid,
                        showPrices = false,
                        showPeriodPicker = false,
                        labels = MonarchPricingTableLabels(caption = compareTitle),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: MonarchPricingSectionPlan,
    period: MonarchPricingPeriod?,
    action: (@Composable (plan: MonarchPricingSectionPlan) -> Unit)?,
) {
    val periodPrice = period?.let { plan.prices[it.id] }
    MonarchPlanCard(
        name = plan.name,
        modifier = Modifier.fillMaxWidth(),
        price = periodPrice ?: plan.price,
        period = if (periodPrice != null) period.priceSuffix else null,
        priceNote = if (periodPrice != null) period.priceNote else null,
        description = plan.description,
        featuresLabel = plan.featuresLabel,
        features = plan.features,
        badge = plan.badge,
        highlighted = plan.highlighted,
        action = action?.let { { it(plan) } },
    )
}
