package dev.monarchui.components.forms.radiocardgroup

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

private val previewPlans = listOf(
    MonarchRadioCardOption("starter", "Starter", description = "For one person.", meta = "1 seat"),
    MonarchRadioCardOption(
        "team",
        "Team",
        description = "Shared projects and roles.",
        meta = "Up to 50 seats",
        icon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
    ),
    MonarchRadioCardOption("enterprise", "Enterprise", description = "Talk to sales.", enabled = false),
)

@MonarchPreviews
@Composable
internal fun MonarchRadioCardGroupPreview() {
    PreviewFrame {
        MonarchRadioCardGroup(options = previewPlans, selected = "team", onSelect = {}, label = "Plan")
        MonarchRadioCardGroup(options = previewPlans, selected = null, onSelect = {}, label = "Plan", enabled = false)
    }
}
