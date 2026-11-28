package com.rookhq.monarch.components.forms.radiocardgroup

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
