package com.rookhq.monarch.sample.sections.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.calloutbanner.MonarchCalloutBanner
import com.rookhq.monarch.components.feedback.calloutbanner.MonarchCalloutBannerPlacement
import com.rookhq.monarch.components.feedback.calloutbanner.MonarchCalloutBannerVariant
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

/** The catalogue page of CalloutBanner. */
object MonarchCalloutBannerSection : CataloguePage {
    override val title = "CalloutBanner"

    @Composable
    override fun Content() {
        var trialShown by rememberSaveable { mutableStateOf(true) }
        var scoped by rememberSaveable { mutableStateOf(true) }
        CatalogueCaption("Page")
        if (trialShown) {
            MonarchCalloutBanner(
                message = "Your trial ends in 3 days.",
                tone = MonarchTone.Warning,
                placement = MonarchCalloutBannerPlacement.Page,
                action = { MonarchButton(text = "Upgrade", onClick = {}, size = MonarchButtonSize.Sm) },
                onDismiss = { trialShown = false },
            )
        } else {
            MonarchButton(text = "Show the banner again", onClick = { trialShown = true }, size = MonarchButtonSize.Sm)
        }
        CatalogueCaption("Section, every tone")
        MonarchTone.entries.forEach { tone ->
            MonarchCalloutBanner(
                title = "${tone.name} callout",
                message = "A message in the ${tone.name.lowercase()} tone.",
                tone = tone,
                icon = { MonarchIcon(imageVector = SampleIcons.Info, contentDescription = null) },
            )
        }
        CatalogueCaption("Scope")
        MonarchCalloutBanner(
            message = if (scoped) "Showing tickets in Support" else "Showing all tickets",
            tone = MonarchTone.Neutral,
            variant = MonarchCalloutBannerVariant.Scope,
            action = {
                MonarchButton(
                    text = if (scoped) "Show all" else "Only Support",
                    onClick = { scoped = !scoped },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            },
        )
    }
}
