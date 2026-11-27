package com.rookhq.monarch.components.feedback.calloutbanner

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchCalloutBannerTonesPreview() {
    PreviewFrame {
        MonarchTone.entries.forEach { tone ->
            MonarchCalloutBanner(
                title = "${tone.name} callout",
                message = "A message in the ${tone.name.lowercase()} tone.",
                tone = tone,
                icon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
                onDismiss = {},
            )
        }
    }
}

@MonarchPreviews
@Composable
internal fun MonarchCalloutBannerPlacementsPreview() {
    PreviewFrame {
        MonarchCalloutBanner(
            message = "Your trial ends in 3 days.",
            tone = MonarchTone.Warning,
            placement = MonarchCalloutBannerPlacement.Page,
            action = { MonarchButton(text = "Upgrade", onClick = {}, size = MonarchButtonSize.Sm) },
        )
        MonarchCalloutBanner(
            message = "Showing tickets in Support",
            tone = MonarchTone.Neutral,
            variant = MonarchCalloutBannerVariant.Scope,
            action = { MonarchButton(text = "Show all", onClick = {}, size = MonarchButtonSize.Sm) },
        )
    }
}
