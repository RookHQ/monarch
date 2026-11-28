package dev.monarchui.components.billing.pricingtable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString

/**
 * Plans compared feature by feature, with a billing period picker.
 *
 * Built by Monarch: Material 3 has no pricing table. On a phone each plan is a
 * page of a `HorizontalPager` (a `MonarchPlanCard` with its price, badges and
 * action, then the plan's features grouped below) with a plan picker above, so
 * the comparison stays readable at large font sizes. From 640dp wide the plans
 * are columns and the features rows. The period picker is a Material 3
 * single-choice segmented button row. Prices are shown exactly as given.
 *
 * @param plans the plans, in display order.
 * @param features the features compared, in display order. Empty shows only the plans.
 * @param modifier the modifier of the table.
 * @param periods the billing periods. With two or more, the period picker is shown.
 * @param period the id of the chosen period. `null` lets the table keep its own choice, starting with the
 * first period.
 * @param onPeriodChange called with the id of the period the user chooses.
 * @param layout `Automatic` (the grid from 640dp wide below font scale 1.3, pages otherwise), `Paged` or
 * `Grid`.
 * @param showPrices shows the prices; `false` for comparisons without prices.
 * @param showPeriodPicker shows the period picker; `false` when the screen shows its own.
 * @param includedIcon a mark for an included feature, for example a check `MonarchIcon` with a `null`
 * content description. Without it the word "Included" is shown. Monarch bundles no icons.
 * @param labels replacements for the built-in strings.
 * @param action the call to action of a plan, normally one `MonarchButton`.
 */
@Composable
public fun MonarchPricingTable(
    plans: List<MonarchPricingPlan>,
    features: List<MonarchPricingFeature>,
    modifier: Modifier = Modifier,
    periods: List<MonarchPricingPeriod> = emptyList(),
    period: String? = null,
    onPeriodChange: ((String) -> Unit)? = null,
    layout: MonarchPricingTableLayout = MonarchPricingTableLayout.Automatic,
    showPrices: Boolean = true,
    showPeriodPicker: Boolean = true,
    includedIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Check,
    labels: MonarchPricingTableLabels = MonarchPricingTableLabels(),
    action: (@Composable (plan: MonarchPricingPlan) -> Unit)? = null,
) {
    var ownPeriod by rememberSaveable { mutableStateOf(periods.firstOrNull()?.id) }
    val chosen = MonarchPricingTableTokens.selectedPeriod(period ?: ownPeriod, periods)
    val caption = monarchString(labels.caption, R.string.monarch_pricingtable_caption)
    val strings = PricingStrings(
        included = monarchString(labels.included, R.string.monarch_pricingtable_included),
        notIncluded = monarchString(labels.notIncluded, R.string.monarch_pricingtable_not_included),
        currentPlan = monarchString(labels.currentPlan, R.string.monarch_pricingtable_current_plan),
        billingPeriod = monarchString(labels.billingPeriod, R.string.monarch_pricingtable_billing_period),
        plan = monarchString(labels.plan, R.string.monarch_pricingtable_plan),
    )
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(
        modifier = modifier.semantics {
            isTraversalGroup = true
            contentDescription = caption
        },
    ) {
        val effective = MonarchPricingTableTokens.layout(layout, maxWidth.value, fontScale)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MonarchPricingTableTokens.SectionGap),
        ) {
            if (MonarchPricingTableTokens.showsPeriodPicker(showPeriodPicker, periods) && chosen != null) {
                PricingPeriodPicker(
                    periods = periods,
                    selected = chosen,
                    name = strings.billingPeriod,
                    onSelect = { id ->
                        ownPeriod = id
                        onPeriodChange?.invoke(id)
                    },
                )
            }
            val content = PricingContent(plans, features, chosen, showPrices, includedIcon, strings, action)
            if (effective == MonarchPricingTableLayout.Grid) PricingGrid(content) else PricingPages(content)
        }
    }
}
