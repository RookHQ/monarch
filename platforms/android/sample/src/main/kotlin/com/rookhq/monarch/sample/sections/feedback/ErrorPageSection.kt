package com.rookhq.monarch.sample.sections.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.components.feedback.errorstate.MonarchErrorState
import com.rookhq.monarch.components.feedback.errorstate.MonarchErrorStateKind
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of ErrorPage: MonarchErrorState filling a screen on the canvas colour. */
object ErrorPageSection : CataloguePage {
    override val title = "ErrorPage"

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
        CatalogueCaption("Not found")
        ErrorScreen {
            MonarchErrorState(
                title = "This project does not exist",
                kind = MonarchErrorStateKind.NotFound,
                size = MonarchEmptyStateSize.Lg,
                message = "It may have been deleted, or the link is wrong.",
                actions = { MonarchButton(text = "Go to projects", onClick = {}) },
            )
        }
        CatalogueCaption("Offline, with retry")
        ErrorScreen {
            MonarchErrorState(
                title = "You are offline",
                kind = MonarchErrorStateKind.Offline,
                size = MonarchEmptyStateSize.Lg,
                retrying = retrying,
                message = "Check your connection. The page loads again when you retry.",
                onRetry = { retrying = true },
            )
        }
    }

    @Composable
    private fun ErrorScreen(content: @Composable () -> Unit) {
        // In an app the error fills the screen: Modifier.fillMaxSize() in place of fillMaxWidth().
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MonarchTheme.colors.bgCanvas)
                .padding(MonarchTheme.spacing.space6),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}
