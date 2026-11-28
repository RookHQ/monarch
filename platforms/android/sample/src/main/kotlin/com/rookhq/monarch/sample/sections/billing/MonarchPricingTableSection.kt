package com.rookhq.monarch.sample.sections.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingFeature
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingFeatureValue
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingPeriod
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingPlan
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingTable
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingTableLayout
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of PricingTable. */
object MonarchPricingTableSection : CataloguePage {
    override val title = "PricingTable"

    private val plans = listOf(
        MonarchPricingPlan(
            "starter",
            "Starter",
            description = "For one person",
            prices = mapOf("month" to "$0", "year" to "$0"),
        ),
        MonarchPricingPlan(
            "team",
            "Team",
            description = "For growing teams",
            prices = mapOf("month" to "$12", "year" to "$120"),
            badge = "Recommended",
            highlighted = true,
        ),
        MonarchPricingPlan("enterprise", "Enterprise", description = "For large organizations", price = "Contact us"),
    )
    private val features = listOf(
        MonarchPricingFeature(
            "seats",
            "Seats",
            values = mapOf(
                "starter" to MonarchPricingFeatureValue.Text("1"),
                "team" to MonarchPricingFeatureValue.Text("Up to 50"),
                "enterprise" to MonarchPricingFeatureValue.Text("Unlimited"),
            ),
        ),
        MonarchPricingFeature(
            "projects",
            "Projects",
            values = mapOf(
                "starter" to MonarchPricingFeatureValue.Text("3"),
                "team" to MonarchPricingFeatureValue.Text("Unlimited"),
                "enterprise" to MonarchPricingFeatureValue.Text("Unlimited"),
            ),
        ),
        MonarchPricingFeature(
            "sso",
            "Single sign-on",
            description = "SAML and OIDC",
            group = "Security",
            values = mapOf(
                "team" to MonarchPricingFeatureValue.Included,
                "enterprise" to MonarchPricingFeatureValue.Included,
            ),
        ),
        MonarchPricingFeature(
            "audit",
            "Audit log",
            group = "Security",
            values = mapOf("enterprise" to MonarchPricingFeatureValue.Included),
        ),
    )
    private val periods = listOf(
        MonarchPricingPeriod("month", "Monthly", priceSuffix = "/ month"),
        MonarchPricingPeriod("year", "Yearly", priceSuffix = "/ year", priceNote = "Billed yearly"),
    )

    @Composable
    override fun Content() {
        var period by rememberSaveable { mutableStateOf("month") }
        CatalogueCaption("Automatic: pages on a phone, a grid on a wide screen")
        MonarchPricingTable(
            plans = plans,
            features = features,
            periods = periods,
            period = period,
            onPeriodChange = { period = it },
        ) { plan ->
            MonarchButton(
                text = if (plan.price != null) "Contact sales" else "Choose ${plan.name}",
                onClick = {},
                variant = if (plan.highlighted) MonarchButtonVariant.Primary else MonarchButtonVariant.Secondary,
            )
        }
        CatalogueCaption("Grid, forced, without prices")
        MonarchPricingTable(
            plans = plans,
            features = features,
            layout = MonarchPricingTableLayout.Grid,
            showPrices = false,
        )
    }
}
