package dev.monarchui.components.billing.pricingtable

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The price of a plan in a period, as shown: the price, the text after it and the line under it. */
internal data class PricingPrice(val price: String?, val suffix: String?, val note: String?)

/** Component tokens and the pure rules of MonarchPricingTable. */
internal object MonarchPricingTableTokens {
    /** The space between the pickers, the pages and the sections of a page. */
    val SectionGap: Dp = MonarchTokens.Space.space4.dp

    /** The space between features and between grid rows. */
    val RowGap: Dp = MonarchTokens.Space.space2.dp

    /** The space between a feature's label and its value, and between grid columns. */
    val ColumnGap: Dp = MonarchTokens.Space.space3.dp

    /** The space between lines inside a plan header. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The padding inside a grid plan header. */
    val HeaderPadding: Dp = MonarchTokens.Space.space3.dp

    /** The border of a highlighted plan header in the grid. */
    val HighlightBorder: Dp = MonarchTokens.Space.space0_5.dp

    /** From this width Automatic uses the grid, as the web's small breakpoint. */
    val GridMinWidth: Dp = MonarchTokens.Breakpoint.sm.dp

    /** From this font scale Automatic uses pages, so grid columns are not squeezed by large text. */
    const val GRID_MAX_FONT_SCALE: Float = 1.3f

    /** The share of the grid width the feature column takes, against one per plan. */
    const val FEATURE_COLUMN_WEIGHT: Float = 1f

    /** The text shown for a feature a plan does not have. */
    const val DASH: String = "—"

    /** The layout at [widthDp] and [fontScale]. */
    fun layout(requested: MonarchPricingTableLayout, widthDp: Float, fontScale: Float): MonarchPricingTableLayout =
        when (requested) {
            MonarchPricingTableLayout.Automatic ->
                if (widthDp >= GridMinWidth.value && fontScale < GRID_MAX_FONT_SCALE) {
                    MonarchPricingTableLayout.Grid
                } else {
                    MonarchPricingTableLayout.Paged
                }
            else -> requested
        }

    /** The chosen period: [period] when it is one of [periods], else the first period, else `null`. */
    fun selectedPeriod(period: String?, periods: List<MonarchPricingPeriod>): MonarchPricingPeriod? =
        periods.firstOrNull { it.id == period } ?: periods.firstOrNull()

    /**
     * The price of [plan] in [period]. A price for the period carries the plan's or the period's suffix and
     * the period's note; the fallback price stands alone.
     */
    fun price(plan: MonarchPricingPlan, period: MonarchPricingPeriod?): PricingPrice {
        val forPeriod = period?.let { plan.prices[it.id] }
        return when {
            forPeriod != null -> PricingPrice(forPeriod, plan.priceSuffix ?: period.priceSuffix, period.priceNote)
            plan.price != null -> PricingPrice(plan.price, null, null)
            period == null && plan.prices.size == 1 -> PricingPrice(plan.prices.values.first(), plan.priceSuffix, null)
            else -> PricingPrice(null, null, null)
        }
    }

    /** The value of [feature] for [plan]; a plan missing from the values does not have the feature. */
    fun value(feature: MonarchPricingFeature, plan: MonarchPricingPlan): MonarchPricingFeatureValue =
        feature.values[plan.id] ?: MonarchPricingFeatureValue.NotIncluded

    /** The spoken text of a value. */
    fun spoken(value: MonarchPricingFeatureValue, included: String, notIncluded: String): String = when (value) {
        MonarchPricingFeatureValue.Included -> included
        MonarchPricingFeatureValue.NotIncluded -> notIncluded
        is MonarchPricingFeatureValue.Text -> value.text
    }

    /** The features grouped by their group, groups and features in first-seen order. */
    fun groups(features: List<MonarchPricingFeature>): List<Pair<String?, List<MonarchPricingFeature>>> =
        features.groupBy { it.group }.toList()

    /** Whether the period picker is shown: wanted, and at least two periods to choose from. */
    fun showsPeriodPicker(show: Boolean, periods: List<MonarchPricingPeriod>): Boolean = show && periods.size >= 2

    /** The colour of a value: muted for a dash, default otherwise. */
    fun valueColor(value: MonarchPricingFeatureValue, colors: MonarchColors): Color =
        if (value == MonarchPricingFeatureValue.NotIncluded) colors.textSubtle else colors.textDefault

    /** The border of a grid plan header: the accent for the highlighted plan, the default border otherwise. */
    fun headerBorder(highlighted: Boolean, colors: MonarchColors): Color =
        if (highlighted) colors.accentBorder else colors.borderDefault
}
