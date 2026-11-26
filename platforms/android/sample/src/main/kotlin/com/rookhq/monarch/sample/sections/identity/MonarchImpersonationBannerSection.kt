package com.rookhq.monarch.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.identity.impersonationbanner.MonarchImpersonationBanner
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import kotlinx.coroutines.delay

private const val SimulatedStopMillis = 800L

/** The catalogue page of ImpersonationBanner. */
object MonarchImpersonationBannerSection : CataloguePage {
    override val title = "ImpersonationBanner"

    @Composable
    override fun Content() {
        var viewing by rememberSaveable { mutableStateOf(true) }
        var startedAt by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
        var failNext by rememberSaveable { mutableStateOf(true) }
        CatalogueCaption("Under the top app bar. The first Stop fails; the second ends the session.")
        if (viewing) {
            MonarchImpersonationBanner(
                name = "Sam Lee",
                onStop = {
                    delay(SimulatedStopMillis)
                    if (failNext) {
                        failNext = false
                        error("The session service did not answer")
                    }
                    viewing = false
                },
                detail = "sam@example.com",
                reason = "Ticket 4821",
                startedAtMillis = startedAt,
                icon = { MonarchIcon(SampleIcons.Person, contentDescription = null) },
            )
        } else {
            MonarchButton(
                text = "View as Sam Lee again",
                onClick = {
                    startedAt = System.currentTimeMillis()
                    failNext = true
                    viewing = true
                },
            )
        }
        CatalogueCaption("Stopping, tracked by the app")
        MonarchImpersonationBanner(name = "Ana Silva", onStop = {}, stopping = true)
    }
}
