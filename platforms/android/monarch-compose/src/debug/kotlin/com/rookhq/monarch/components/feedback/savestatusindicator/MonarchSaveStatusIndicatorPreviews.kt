package com.rookhq.monarch.components.feedback.savestatusindicator

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
