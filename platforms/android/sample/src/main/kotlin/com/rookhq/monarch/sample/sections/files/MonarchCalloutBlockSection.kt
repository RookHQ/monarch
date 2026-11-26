package com.rookhq.monarch.sample.sections.files

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.files.calloutblock.MonarchCalloutBlock
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

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
