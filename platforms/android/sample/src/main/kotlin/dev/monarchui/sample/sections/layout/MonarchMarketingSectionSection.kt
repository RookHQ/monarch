package dev.monarchui.sample.sections.layout

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.layout.marketingsection.MonarchCtaBand
import dev.monarchui.components.layout.marketingsection.MonarchFaq
import dev.monarchui.components.layout.marketingsection.MonarchFaqItem
import dev.monarchui.components.layout.marketingsection.MonarchFeatureGrid
import dev.monarchui.components.layout.marketingsection.MonarchFeatureGridItem
import dev.monarchui.components.layout.marketingsection.MonarchMarketingSection
import dev.monarchui.components.layout.marketingsection.MonarchProductFrame
import dev.monarchui.components.layout.marketingsection.MonarchStatStrip
import dev.monarchui.components.layout.marketingsection.MonarchStatStripItem
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

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
