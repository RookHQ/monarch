package dev.monarchui.sample.sections.forms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.forms.marketingconsentcheckbox.MonarchMarketingChannel
import dev.monarchui.components.forms.marketingconsentcheckbox.MonarchMarketingConsentCheckbox
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of MarketingConsentCheckbox. */
object MonarchMarketingConsentCheckboxSection : CataloguePage {
    override val title = "MarketingConsentCheckbox"

    @Composable
    override fun Content() {
        var email by rememberSaveable { mutableStateOf(emptySet<MonarchMarketingChannel>()) }
        var several by rememberSaveable { mutableStateOf(emptySet<MonarchMarketingChannel>()) }
        var proof by rememberSaveable { mutableStateOf("") }
        var policyOpened by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Email, with a description and the policy")
        MonarchMarketingConsentCheckbox(
            selection = email,
            onConsentChange = {
                email = it.granted
                proof = it.text
            },
            description = "Product updates and offers, about twice a month.",
            policyTitle = "Privacy policy",
            onPolicyClick = { policyOpened = true },
        )
        CatalogueCaption("One checkbox per channel")
        MonarchMarketingConsentCheckbox(
            selection = several,
            onConsentChange = {
                several = it.granted
                proof = it.text
            },
            channels = listOf(MonarchMarketingChannel.Email, MonarchMarketingChannel.Sms, MonarchMarketingChannel.Post),
        )
        CatalogueCaption("Disabled")
        MonarchMarketingConsentCheckbox(selection = emptySet(), onConsentChange = {}, enabled = false)
        MonarchText(
            text = "Wording recorded: ${proof.ifEmpty { "nothing yet" }}" +
                if (policyOpened) ". The policy link was used." else "",
            style = MonarchTextStyle.Caption,
            color = MonarchTheme.colors.textMuted,
        )
    }
}
