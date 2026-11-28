package dev.monarchui.sample.sections.datadisplay

import androidx.compose.runtime.Composable
import dev.monarchui.components.datadisplay.truncatedtext.MonarchTruncatedText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of TruncatedText. */
object MonarchTruncatedTextSection : CataloguePage {
    override val title = "TruncatedText"

    private const val NOTE =
        "The customer asked for the invoice to be split across two cost centers, with the travel costs on the " +
            "second one and the license fees on the first. Finance agreed on the condition that both parts " +
            "reference the original purchase order number."

    @Composable
    override fun Content() {
        CatalogueCaption("Three lines")
        MonarchTruncatedText(text = NOTE)
        CatalogueCaption("One line")
        MonarchTruncatedText(text = NOTE, maxLines = 1)
        CatalogueCaption("Fits")
        MonarchTruncatedText(text = "A short note has no button.")
    }
}
