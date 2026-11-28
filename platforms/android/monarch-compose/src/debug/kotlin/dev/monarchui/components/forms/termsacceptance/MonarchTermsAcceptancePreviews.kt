package dev.monarchui.components.forms.termsacceptance

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewDocuments = listOf(
    MonarchTermsDocument("Terms of Service", "https://example.com/terms", version = "2026-11-01"),
    MonarchTermsDocument("Privacy Policy", "https://example.com/privacy", version = "2026-09-15"),
)

@MonarchPreviews
@Composable
internal fun MonarchTermsAcceptancePreview() {
    PreviewFrame {
        MonarchTermsAcceptance(accepted = false, onAcceptedChange = { _, _ -> }, documents = PreviewDocuments)
        MonarchTermsAcceptance(
            accepted = false,
            onAcceptedChange = { _, _ -> },
            documents = PreviewDocuments,
            showRequiredError = true,
        )
        MonarchTermsAcceptance(accepted = true, onAcceptedChange = { _, _ -> }, documents = PreviewDocuments)
        MonarchTermsAcceptance(
            accepted = true,
            onAcceptedChange = { _, _ -> },
            documents = PreviewDocuments.take(1),
            required = false,
            enabled = false,
        )
    }
}
