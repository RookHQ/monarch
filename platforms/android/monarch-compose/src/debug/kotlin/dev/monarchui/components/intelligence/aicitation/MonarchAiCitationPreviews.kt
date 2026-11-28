package dev.monarchui.components.intelligence.aicitation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@OptIn(ExperimentalLayoutApi::class)
@MonarchPreviews
@Composable
internal fun MonarchAiCitationPreview() {
    PreviewFrame {
        FlowRow(itemVerticalAlignment = Alignment.CenterVertically) {
            MonarchText(text = "Seats are billed monthly.")
            MonarchAiCitation(
                index = 1,
                source = MonarchAiSourcePart(
                    id = "s1",
                    title = "Pricing",
                    url = "https://www.example.com/pricing",
                    kind = MonarchAiSourceKind.Web,
                ),
            )
        }
        Box {
            MonarchAiCitation(
                index = 2,
                source = MonarchAiSourcePart(id = "s2", title = "Q3 plan", kind = MonarchAiSourceKind.Doc),
                quote = "Seats are billed monthly, prorated to the day.",
                expanded = true,
                onOpen = {},
            )
        }
    }
}
