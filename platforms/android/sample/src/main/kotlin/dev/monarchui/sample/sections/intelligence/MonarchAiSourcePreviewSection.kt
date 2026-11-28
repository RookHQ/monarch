package dev.monarchui.sample.sections.intelligence

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.components.intelligence.aisourcepreview.MonarchAiSourcePreview
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private val Report = MonarchAiSourcePart("s1", "Q3 revenue report", "https://example.com/q3", MonarchAiSourceKind.Doc)

private val ReportText = listOf(
    "This report covers July to September.",
    "Revenue grew 12% over the quarter, led by the Payments team and two new enterprise customers.",
    "Costs stayed flat. Hiring resumes in the next quarter.",
).joinToString("\n\n")

/** The catalogue page of AiSourcePreview. */
object MonarchAiSourcePreviewSection : CataloguePage {
    override val title = "AiSourcePreview"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var shown by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("In a bottom sheet, with the source text")
        MonarchButton(text = "Show the cited source", onClick = { shown = "content" })
        CatalogueCaption("In a bottom sheet, a quote without the source text")
        MonarchButton(text = "Show the quote", onClick = { shown = "quote" })
        if (shown.isNotEmpty()) {
            MonarchBottomSheet(onDismissRequest = { shown = "" }) {
                MonarchAiSourcePreview(
                    source = Report,
                    quote = if (shown == "content") "revenue grew 12%" else "Revenue grew 12% over the quarter.",
                    content = if (shown == "content") ReportText else null,
                    onClose = { dismiss() },
                )
            }
        }
    }
}
