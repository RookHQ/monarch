package dev.monarchui.components.billing.pricingtable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import dev.monarchui.components.billing.plancard.MonarchPlanCard
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.launch

/**
 * The phone layout: a plan picker and one page per plan, a `MonarchPlanCard`
 * with the plan's features grouped below. The picker follows the visible page.
 */
@Composable
internal fun PricingPages(content: PricingContent) {
    val plans = content.plans
    val pager = rememberPagerState { plans.size }
    val scope = rememberCoroutineScope()
    val motion = MonarchTheme.motion
    Column(verticalArrangement = Arrangement.spacedBy(MonarchPricingTableTokens.SectionGap)) {
        if (plans.size >= 2) {
            PricingSegments(
                options = plans.map { it.name },
                selected = pager.currentPage,
                name = content.strings.plan,
                onSelect = { page -> scope.launch { pager.animateScrollToPage(page, animationSpec = motion.spec()) } },
            )
        }
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = MonarchPricingTableTokens.ColumnGap,
            verticalAlignment = Alignment.Top,
        ) { page -> PlanPage(plans[page], content) }
    }
}

@Composable
private fun PlanPage(plan: MonarchPricingPlan, content: PricingContent) {
    val price = if (content.showPrices) MonarchPricingTableTokens.price(plan, content.period) else null
    Column(verticalArrangement = Arrangement.spacedBy(MonarchPricingTableTokens.SectionGap)) {
        MonarchPlanCard(
            name = plan.name,
            modifier = Modifier.fillMaxWidth(),
            price = price?.price,
            period = price?.suffix,
            priceNote = price?.note,
            description = plan.description,
            badge = plan.badge,
            highlighted = plan.highlighted,
            current = plan.current,
            currentPlanLabel = content.strings.currentPlan,
            action = content.action?.let { action -> { action(plan) } },
        )
        MonarchPricingTableTokens.groups(content.features).forEach { (group, features) ->
            Column(verticalArrangement = Arrangement.spacedBy(MonarchPricingTableTokens.RowGap)) {
                if (group != null) MonarchText(text = group, style = MonarchTextStyle.Label, heading = true)
                features.forEach { feature -> FeatureLine(feature, plan, content) }
            }
        }
    }
}

/** One feature on a page, read as one element: the feature, its value and its description. */
@Composable
private fun FeatureLine(feature: MonarchPricingFeature, plan: MonarchPricingPlan, content: PricingContent) {
    val value = MonarchPricingTableTokens.value(feature, plan)
    val spoken = listOfNotNull(
        feature.label,
        MonarchPricingTableTokens.spoken(value, content.strings.included, content.strings.notIncluded),
        feature.description,
    ).joinToString(", ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(MonarchPricingTableTokens.ColumnGap),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MonarchText(text = feature.label)
            if (feature.description != null) {
                MonarchText(
                    text = feature.description,
                    style = MonarchTextStyle.Caption,
                    color = MonarchTheme.colors.textMuted,
                )
            }
        }
        PricingValue(value, content)
    }
}
