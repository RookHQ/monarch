package com.rookhq.monarch.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.feedback.calloutbanner.MonarchCalloutBannerPlacement
import com.rookhq.monarch.components.identity.emailverificationbanner.MonarchEmailVerificationBanner
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import kotlinx.coroutines.delay

private const val SimulatedSendMillis = 600L
private const val ShortCooldownSeconds = 10

/** The catalogue page of EmailVerificationBanner. */
object MonarchEmailVerificationBannerSection : CataloguePage {
    override val title = "EmailVerificationBanner"

    @Composable
    override fun Content() {
        var shown by rememberSaveable { mutableStateOf(true) }
        var sends by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Every third send fails. The cooldown is 10 seconds here.")
        if (shown) {
            MonarchEmailVerificationBanner(
                email = "ana@example.com",
                onResend = {
                    delay(SimulatedSendMillis)
                    sends++
                    if (sends % 3 == 0) error("The mail service did not answer")
                },
                cooldownSeconds = ShortCooldownSeconds,
                onChangeEmail = {},
                onDismiss = { shown = false },
            )
        } else {
            MonarchButton(text = "Show the banner again", onClick = { shown = true })
        }
        CatalogueCaption("Info tone as a page strip, right after sign-up")
        MonarchEmailVerificationBanner(
            email = "ana@example.com",
            onResend = {},
            tone = MonarchTone.Info,
            placement = MonarchCalloutBannerPlacement.Page,
            initialCooldownSeconds = 60,
        )
    }
}
