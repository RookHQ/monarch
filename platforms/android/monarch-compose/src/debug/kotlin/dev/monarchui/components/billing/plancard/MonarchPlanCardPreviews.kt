package dev.monarchui.components.billing.plancard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchPlanCardPreview() {
    PreviewFrame {
        MonarchPlanCard(
            name = "Team",
            price = "$12",
            period = "per seat / month",
            priceNote = "Billed yearly",
            description = "For teams that ship together.",
            features = listOf(
                MonarchPlanCardFeature("Unlimited projects"),
                MonarchPlanCardFeature("Seats", hint = "up to 10"),
                MonarchPlanCardFeature("Single sign-on", included = false),
            ),
            badge = "Recommended",
            highlighted = true,
            includedIcon = {
                MonarchIcon(imageVector = PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm)
            },
        ) {
            MonarchButton(
                text = "Choose Team",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = MonarchButtonVariant.Primary,
            )
        }
        MonarchPlanCard(name = "Starter", price = "$0", period = "per month", current = true, footer = "Taxes extra.")
    }
}
