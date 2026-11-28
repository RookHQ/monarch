package dev.monarchui.components.datadisplay.freshnessindicator

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchFreshnessIndicatorPreview() {
    val now = System.currentTimeMillis()
    PreviewFrame {
        MonarchFreshnessIndicatorVariant.entries.forEach { variant ->
            MonarchFreshnessIndicator(
                updatedAt = now - 3 * DateUtils.MINUTE_IN_MILLIS,
                variant = variant,
                source = "Directory",
                onRefresh = {},
            )
        }
        MonarchFreshnessIndicator(
            updatedAt = now - 2 * DateUtils.HOUR_IN_MILLIS,
            staleAfterMillis = DateUtils.HOUR_IN_MILLIS,
            details = listOf("Events on"),
        )
        MonarchFreshnessIndicator(
            updatedAt = now - 2 * DateUtils.DAY_IN_MILLIS,
            staleAfterMillis = DateUtils.HOUR_IN_MILLIS,
            expiredAfterMillis = DateUtils.DAY_IN_MILLIS,
        )
        MonarchFreshnessIndicator(updatedAt = null)
        MonarchFreshnessIndicator(updatedAt = null, status = MonarchFreshnessStatus.Checking)
        MonarchFreshnessIndicator(updatedAt = now, status = MonarchFreshnessStatus.Error, error = "Sync failed")
    }
}
