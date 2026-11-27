package com.rookhq.monarch.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.identity.cookieconsentbanner.MonarchCookieConsentBanner
import com.rookhq.monarch.components.identity.cookieconsentbanner.MonarchCookieConsentBannerLabels
import com.rookhq.monarch.components.identity.cookieconsentbanner.MonarchCookieConsentRegime
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of CookieConsentBanner. A decision hides the card until it is shown again. */
object MonarchCookieConsentBannerSection : CataloguePage {
    override val title = "CookieConsentBanner"

    @Composable
    override fun Content() {
        var decision by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Opt-in (ask first)")
        if (decision == null) {
            MonarchCookieConsentBanner(
                regime = MonarchCookieConsentRegime.OptIn,
                onDecide = { decision = it.name },
                onCustomize = { decision = "Customize" },
                onPolicyClick = {},
            )
        } else {
            MonarchText(text = "Decision: $decision")
            MonarchButton(text = "Show the banner again", onClick = { decision = null })
        }
        CatalogueCaption("Opt-out (sale or sharing)")
        MonarchCookieConsentBanner(
            regime = MonarchCookieConsentRegime.OptOut,
            onDecide = {},
            labels = MonarchCookieConsentBannerLabels(title = "Your privacy choices"),
        )
        CatalogueCaption("Notice")
        MonarchCookieConsentBanner(
            regime = MonarchCookieConsentRegime.Notice,
            onDecide = {},
            labels = MonarchCookieConsentBannerLabels(title = "About cookies"),
        )
    }
}
