package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.intelligence.aidisclaimer.MonarchAiDisclaimer
import com.rookhq.monarch.components.intelligence.aidisclaimer.MonarchAiDisclaimerRisk
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

/** The catalogue page of AiDisclaimer. */
object MonarchAiDisclaimerSection : CataloguePage {
    override val title = "AiDisclaimer"

    @Composable
    override fun Content() {
        CatalogueCaption("Low risk, under a composer")
        MonarchAiDisclaimer()
        CatalogueCaption("Medium risk, with an icon")
        MonarchAiDisclaimer(
            risk = MonarchAiDisclaimerRisk.Medium,
            icon = { MonarchIcon(SampleIcons.Info, contentDescription = null, size = MonarchIconSize.Sm) },
        )
        CatalogueCaption("High risk, for answers people act on")
        MonarchAiDisclaimer(risk = MonarchAiDisclaimerRisk.High)
        CatalogueCaption("Custom wording")
        MonarchAiDisclaimer(text = "Summaries can leave out details. Open the thread for the full discussion.")
    }
}
