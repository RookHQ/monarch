package dev.monarchui.sample.sections.datadisplay

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.datadisplay.freshnessindicator.MonarchFreshnessIndicator
import dev.monarchui.components.datadisplay.freshnessindicator.MonarchFreshnessIndicatorVariant
import dev.monarchui.components.datadisplay.freshnessindicator.MonarchFreshnessStatus
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import kotlinx.coroutines.delay

/** The catalogue page of FreshnessIndicator. */
object MonarchFreshnessIndicatorSection : CataloguePage {
    override val title = "FreshnessIndicator"

    private const val REFRESH_MILLIS = 1500L
    private const val STALE_HOURS = 2L
    private const val OLD_DAYS = 3L

    @Composable
    override fun Content() {
        var updatedAt by rememberSaveable {
            mutableLongStateOf(System.currentTimeMillis() - STALE_HOURS * DateUtils.HOUR_IN_MILLIS)
        }
        var refreshing by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(refreshing) {
            if (refreshing) {
                delay(REFRESH_MILLIS)
                updatedAt = System.currentTimeMillis()
                refreshing = false
            }
        }
        CatalogueCaption("Stale after an hour, with refresh")
        MonarchFreshnessIndicator(
            updatedAt = updatedAt,
            refreshing = refreshing,
            staleAfterMillis = DateUtils.HOUR_IN_MILLIS,
            expiredAfterMillis = DateUtils.DAY_IN_MILLIS,
            source = "Directory",
            onRefresh = { refreshing = true },
        )
        CatalogueCaption("Banner, expired")
        MonarchFreshnessIndicator(
            updatedAt = System.currentTimeMillis() - OLD_DAYS * DateUtils.DAY_IN_MILLIS,
            variant = MonarchFreshnessIndicatorVariant.Banner,
            staleAfterMillis = DateUtils.HOUR_IN_MILLIS,
            expiredAfterMillis = DateUtils.DAY_IN_MILLIS,
            details = listOf("Events on"),
        )
        CatalogueCaption("Never, checking and error")
        MonarchFreshnessIndicator(updatedAt = null)
        MonarchFreshnessIndicator(updatedAt = null, status = MonarchFreshnessStatus.Checking)
        MonarchFreshnessIndicator(updatedAt = updatedAt, status = MonarchFreshnessStatus.Error, error = "Sync failed")
    }
}
