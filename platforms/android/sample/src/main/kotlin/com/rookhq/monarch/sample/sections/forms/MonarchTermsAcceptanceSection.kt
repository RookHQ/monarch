package com.rookhq.monarch.sample.sections.forms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.termsacceptance.MonarchTermsAcceptance
import com.rookhq.monarch.components.forms.termsacceptance.MonarchTermsDocument
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

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
