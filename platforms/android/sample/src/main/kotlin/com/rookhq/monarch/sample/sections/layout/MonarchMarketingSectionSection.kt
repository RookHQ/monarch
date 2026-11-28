package com.rookhq.monarch.sample.sections.layout

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.layout.marketingsection.MonarchCtaBand
import com.rookhq.monarch.components.layout.marketingsection.MonarchFaq
import com.rookhq.monarch.components.layout.marketingsection.MonarchFaqItem
import com.rookhq.monarch.components.layout.marketingsection.MonarchFeatureGrid
import com.rookhq.monarch.components.layout.marketingsection.MonarchFeatureGridItem
import com.rookhq.monarch.components.layout.marketingsection.MonarchMarketingSection
import com.rookhq.monarch.components.layout.marketingsection.MonarchProductFrame
import com.rookhq.monarch.components.layout.marketingsection.MonarchStatStrip
import com.rookhq.monarch.components.layout.marketingsection.MonarchStatStripItem
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of the marketing sections. The FAQ expands. */
object MonarchMarketingSectionSection : CataloguePage {
    override val title = "Marketing sections"

    @Composable
    override fun Content() {
        CatalogueCaption("Section with features")
        MonarchMarketingSection(
            title = "Every idea gets the right shape.",
            eyebrow = "The editor",
            description = "Type a slash and pick a block.",
        ) {
            MonarchFeatureGrid(
                items = listOf(
                    MonarchFeatureGridItem("Slash commands", "Tables, lists and callouts, one keystroke away."),
                    MonarchFeatureGridItem("Version history", "Named snapshots you can restore safely."),
                ),
            )
        }
        CatalogueCaption("Product frame, stats, FAQ and CTA")
        MonarchProductFrame(label = "Share dialog with a copyable link", title = "example.rookhq.com/share") {
            MonarchText(
                text = "Anyone with the link can view",
                modifier = Modifier.padding(MonarchTheme.spacing.space6),
            )
        }
        MonarchStatStrip(
            listOf(MonarchStatStripItem("3", "Export formats"), MonarchStatStripItem("13", "Interface languages")),
        )
        MonarchFaq(listOf(MonarchFaqItem("Which formats can I export?", "HTML, Markdown and PDF.")))
        MonarchCtaBand(title = "Start with a blank page.") {
            MonarchButton(text = "Start writing", onClick = {}, variant = MonarchButtonVariant.Primary)
        }
    }
}
