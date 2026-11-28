package com.rookhq.monarch.sample.sections.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import com.rookhq.monarch.components.feedback.inlineerrorretry.MonarchInlineErrorRetryVariant
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import kotlinx.coroutines.delay

/** The catalogue page of InlineErrorRetry. */
object MonarchInlineErrorRetrySection : CataloguePage {
    override val title = "InlineErrorRetry"

    private const val RETRY_MILLIS = 1500L

    @Composable
    override fun Content() {
        var retrying by rememberSaveable { mutableStateOf(false) }
        var attempts by rememberSaveable { mutableIntStateOf(0) }
        LaunchedEffect(retrying) {
            if (retrying) {
                delay(RETRY_MILLIS)
                attempts++
                retrying = false
            }
        }
        CatalogueCaption("Inline")
        MonarchInlineErrorRetry(
            message = if (attempts == 0) "Your earnings did not load." else "Still not loaded after $attempts tries.",
            onRetry = { retrying = true },
            retrying = retrying,
        )
        CatalogueCaption("Banner, over stale data")
        MonarchInlineErrorRetry(
            message = "Showing figures from yesterday.",
            onRetry = {},
            tone = MonarchTone.Warning,
            variant = MonarchInlineErrorRetryVariant.Banner,
        )
    }
}
