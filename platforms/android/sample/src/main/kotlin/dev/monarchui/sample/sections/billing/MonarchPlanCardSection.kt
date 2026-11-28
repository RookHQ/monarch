package dev.monarchui.sample.sections.billing

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.billing.plancard.MonarchPlanCard
import dev.monarchui.components.billing.plancard.MonarchPlanCardFeature
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of PlanCard. Choosing a plan only moves the "Current plan" badge. */
object MonarchPlanCardSection : CataloguePage {
    override val title = "PlanCard"

    private val starterFeatures = listOf(
        MonarchPlanCardFeature("Three projects"),
        MonarchPlanCardFeature("Single sign-on", included = false),
    )
    private val teamFeatures = listOf(
        MonarchPlanCardFeature("Unlimited projects"),
        MonarchPlanCardFeature("Seats", hint = "up to 10"),
        MonarchPlanCardFeature("Single sign-on", included = false),
    )

    @Composable
    override fun Content() {
        var current by rememberSaveable { mutableStateOf("Starter") }
        MonarchPlanCard(
            name = "Starter",
            price = "$0",
            period = "per month",
            description = "For trying things out.",
            features = starterFeatures,
            current = current == "Starter",
        ) {
            MonarchButton(
                text = "Choose Starter",
                onClick = { current = "Starter" },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        MonarchPlanCard(
            name = "Team",
            price = "$12",
            period = "per seat / month",
            priceNote = "Billed yearly",
            description = "For teams that ship together.",
            featuresLabel = "Everything in Starter, plus",
            features = teamFeatures,
            badge = "Recommended",
            footer = "Taxes extra.",
            highlighted = true,
            current = current == "Team",
            includedIcon = {
                MonarchIcon(imageVector = SampleIcons.Check, contentDescription = null, size = MonarchIconSize.Sm)
            },
        ) {
            MonarchButton(
                text = "Choose Team",
                onClick = { current = "Team" },
                modifier = Modifier.fillMaxWidth(),
                variant = MonarchButtonVariant.Primary,
            )
        }
    }
}
