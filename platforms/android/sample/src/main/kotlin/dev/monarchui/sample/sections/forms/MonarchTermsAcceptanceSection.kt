package dev.monarchui.sample.sections.forms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.termsacceptance.MonarchTermsAcceptance
import dev.monarchui.components.forms.termsacceptance.MonarchTermsDocument
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

private val Documents = listOf(
    MonarchTermsDocument("Terms of Service", "https://example.com/terms", version = "2026-11-01"),
    MonarchTermsDocument("Privacy Policy", "https://example.com/privacy", version = "2026-09-15"),
)

/** The catalogue page of TermsAcceptance. */
object MonarchTermsAcceptanceSection : CataloguePage {
    override val title = "TermsAcceptance"

    @Composable
    override fun Content() {
        var accepted by rememberSaveable { mutableStateOf(false) }
        var tried by rememberSaveable { mutableStateOf(false) }
        var created by rememberSaveable { mutableStateOf(false) }
        var record by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Sign-up: press Create account without ticking the box")
        MonarchTermsAcceptance(
            accepted = accepted,
            onAcceptedChange = { value, details ->
                accepted = value
                record = if (value) "Accepted ${details.versions} at ${details.dateMillis}" else ""
            },
            documents = Documents,
            showRequiredError = tried,
        )
        MonarchButton(
            text = "Create account",
            onClick = {
                tried = true
                created = accepted
            },
            variant = MonarchButtonVariant.Primary,
        )
        if (created) MonarchText(text = record, style = MonarchTextStyle.Caption, color = MonarchTheme.colors.textMuted)
        CatalogueCaption("Disabled")
        MonarchTermsAcceptance(
            accepted = true,
            onAcceptedChange = { _, _ -> },
            documents = Documents.take(1),
            enabled = false,
        )
    }
}
