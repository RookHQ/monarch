package dev.monarchui.components.feedback.savestatusindicator

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchSaveStatusIndicatorPreview() {
    PreviewFrame {
        MonarchSaveStatusIndicator(status = MonarchSaveStatus.Unsaved)
        MonarchSaveStatusIndicator(status = MonarchSaveStatus.Saving)
        MonarchSaveStatusIndicator(
            status = MonarchSaveStatus.Saved,
            savedAt = System.currentTimeMillis() - 2 * DateUtils.MINUTE_IN_MILLIS,
            icon = { MonarchIcon(PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm) },
        )
        MonarchSaveStatusIndicator(status = MonarchSaveStatus.Error, errorMessage = "You are offline", onRetry = {})
    }
}
