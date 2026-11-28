package dev.monarchui.sample.sections.feedback

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.feedback.errorstate.MonarchErrorState
import dev.monarchui.components.feedback.errorstate.MonarchErrorStateKind
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import kotlinx.coroutines.delay

/** The catalogue page of ErrorState. */
object MonarchErrorStateSection : CataloguePage {
    override val title = "ErrorState"

    private const val RETRY_MILLIS = 1500L

    @Composable
    override fun Content() {
        var retrying by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(retrying) {
            if (retrying) {
                delay(RETRY_MILLIS)
                retrying = false
            }
        }
        CatalogueCaption("Error, with a request id")
        MonarchCard(modifier = Modifier.fillMaxWidth()) {
            MonarchErrorState(
                modifier = Modifier.fillMaxWidth(),
                title = "Invoices did not load",
                retrying = retrying,
                message = "The billing service did not answer. Try again in a moment.",
                icon = { MonarchIcon(SampleIcons.Error, contentDescription = null) },
                requestId = "req_8f2a91",
                onRetry = { retrying = true },
            )
        }
        CatalogueCaption("Forbidden: no retry")
        MonarchErrorState(
            modifier = Modifier.fillMaxWidth(),
            title = "You do not have access to this report",
            kind = MonarchErrorStateKind.Forbidden,
            message = "Ask an admin of the workspace for access.",
            onRetry = {},
        )
    }
}
