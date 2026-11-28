package dev.monarchui.components.datadisplay.truncatedtext

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private const val LongText =
    "The customer asked for the invoice to be split across two cost centers, with the travel costs on the " +
        "second one and the license fees on the first. Finance agreed on the condition that both parts " +
        "reference the original purchase order number."

@MonarchPreviews
@Composable
internal fun MonarchTruncatedTextPreview() {
    PreviewFrame {
        MonarchTruncatedText(text = LongText)
        MonarchTruncatedText(text = LongText, expanded = true, onExpandedChange = {})
        MonarchTruncatedText(text = LongText, maxLines = 1)
        MonarchTruncatedText(text = "Short text fits, so there is no button.")
    }
}
