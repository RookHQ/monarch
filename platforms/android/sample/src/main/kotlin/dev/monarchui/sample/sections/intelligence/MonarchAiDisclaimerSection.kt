package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import dev.monarchui.components.intelligence.aidisclaimer.MonarchAiDisclaimer
import dev.monarchui.components.intelligence.aidisclaimer.MonarchAiDisclaimerRisk
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

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
