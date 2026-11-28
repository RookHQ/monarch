package dev.monarchui.sample.sections.files

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.monarchui.components.files.calloutblock.MonarchCalloutBlock
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of CalloutBlock. */
object MonarchCalloutBlockSection : CataloguePage {
    override val title = "CalloutBlock"

    @Composable
    override fun Content() {
        CatalogueCaption("Tones")
        MonarchTone.entries.forEach { tone ->
            MonarchCalloutBlock(
                text = "A paragraph inside a ${tone.name.lowercase()} callout block.",
                tone = tone,
                icon = { MonarchIcon(imageVector = SampleIcons.Info, contentDescription = null) },
            )
        }
        CatalogueCaption("Several paragraphs")
        MonarchCalloutBlock(tone = MonarchTone.Info) {
            Text("Tip", style = MonarchTheme.typography.bodyStrong)
            Text("Callouts can hold more than one paragraph, a list or a link.")
        }
    }
}
