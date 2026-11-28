package dev.monarchui.components.layout.marketingsection

import androidx.compose.runtime.Composable
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchMarketingSectionPreview() {
    PreviewFrame {
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
        MonarchStatStrip(
            listOf(MonarchStatStripItem("3", "Export formats"), MonarchStatStripItem("13", "Interface languages")),
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchMarketingBlocksPreview() {
    PreviewFrame {
        MonarchProductFrame(label = "Share dialog with a copyable link", title = "example.rookhq.com/share") {
            MonarchText(text = "Anyone with the link can view")
        }
        MonarchFaq(listOf(MonarchFaqItem("Which formats can I export?", "HTML, Markdown and PDF.")))
        MonarchCtaBand(title = "Start with a blank page.")
    }
}
