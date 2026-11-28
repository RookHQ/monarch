package dev.monarchui.components.billing.pricingtable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.theme.MonarchTheme

/** The built-in strings of a pricing table, resolved once. */
internal class PricingStrings(
    val included: String,
    val notIncluded: String,
    val currentPlan: String,
    val billingPeriod: String,
    val plan: String,
)

/** What both layouts show. */
internal class PricingContent(
    val plans: List<MonarchPricingPlan>,
    val features: List<MonarchPricingFeature>,
    val period: MonarchPricingPeriod?,
    val showPrices: Boolean,
    val includedIcon: (@Composable () -> Unit)?,
    val strings: PricingStrings,
    val action: (@Composable (plan: MonarchPricingPlan) -> Unit)?,
)

/** A single-choice segmented row named for TalkBack: the period picker and the plan picker. */
@Composable
internal fun PricingSegments(
    options: List<String>,
    selected: Int,
    name: String,
    onSelect: (Int) -> Unit,
) {
    MonarchSegmentedControl(
        options = options,
        selectedIndex = selected,
        onSelect = onSelect,
        modifier = Modifier.semantics {
            isTraversalGroup = true
            contentDescription = name
        },
    )
}

/** The billing period picker, with the chosen period's suffix under it. */
@Composable
internal fun PricingPeriodPicker(
    periods: List<MonarchPricingPeriod>,
    selected: MonarchPricingPeriod,
    name: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MonarchPricingTableTokens.LineGap)) {
        PricingSegments(
            options = periods.map { it.label },
            selected = periods.indexOf(selected),
            name = name,
            onSelect = { onSelect(periods[it].id) },
        )
        if (selected.suffix != null) {
            MonarchText(text = selected.suffix, style = MonarchTextStyle.Caption, color = MonarchTheme.colors.textMuted)
        }
    }
}

/** A feature's value as shown: the included icon or word, a dash, or the text. Not read on its own. */
@Composable
internal fun PricingValue(value: MonarchPricingFeatureValue, content: PricingContent, modifier: Modifier = Modifier) {
    val color = MonarchPricingTableTokens.valueColor(value, MonarchTheme.colors)
    when (value) {
        MonarchPricingFeatureValue.Included -> if (content.includedIcon != null) {
            Box(modifier) {
                CompositionLocalProvider(LocalContentColor provides MonarchTheme.colors.accentStrong) {
                    content.includedIcon.invoke()
                }
            }
        } else {
            MonarchText(text = content.strings.included, modifier = modifier, color = color)
        }
        MonarchPricingFeatureValue.NotIncluded ->
            MonarchText(text = MonarchPricingTableTokens.DASH, modifier = modifier, color = color)
        is MonarchPricingFeatureValue.Text -> MonarchText(text = value.text, modifier = modifier, color = color)
    }
}
