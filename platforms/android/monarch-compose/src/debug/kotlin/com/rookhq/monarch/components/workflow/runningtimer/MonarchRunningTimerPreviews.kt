package com.rookhq.monarch.components.workflow.runningtimer

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private const val PreviewStart = 1_793_784_600_000L
private const val PreviewNow = PreviewStart + 249_000L

@MonarchPreviews
@Composable
internal fun MonarchRunningTimerPreview() {
    PreviewFrame {
        MonarchRunningTimer(
            startedAt = PreviewStart,
            label = "ENG-142",
            now = PreviewNow,
            onStop = {},
            onStopAt = {},
            onDiscard = {},
        )
        MonarchRunningTimer(startedAt = null, now = PreviewNow, onStart = {})
    }
}

@MonarchPreviews
@Composable
internal fun MonarchRunningTimerIndicatorPreview() {
    PreviewFrame {
        MonarchRunningTimer(
            startedAt = PreviewStart,
            variant = MonarchRunningTimerVariant.Indicator,
            label = "ENG-142",
            now = PreviewNow,
            onStop = {},
            onDiscard = {},
            onOpen = {},
        )
        MonarchRunningTimer(
            startedAt = PreviewStart,
            variant = MonarchRunningTimerVariant.Indicator,
            warnAfterMillis = 60_000L,
            now = PreviewNow,
            onStop = {},
        )
        MonarchRunningTimer(
            startedAt = null,
            variant = MonarchRunningTimerVariant.Indicator,
            showWhenStopped = true,
            now = PreviewNow,
        )
    }
}
