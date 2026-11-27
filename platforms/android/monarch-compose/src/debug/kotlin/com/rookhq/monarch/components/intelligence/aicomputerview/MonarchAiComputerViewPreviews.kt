@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence.aicomputerview

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.ColorPainter
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Locale
import java.util.TimeZone

/** 15 November 2026, 10:02 UTC. */
private const val At = 1_794_736_920_000L

@MonarchPreviews
@Composable
internal fun MonarchAiComputerViewPreview() {
    PreviewFrame {
        MonarchAiComputerView(
            status = MonarchAiComputerViewStatus.Working,
            frame = MonarchAiComputerFrame(
                image = ColorPainter(MonarchTheme.colors.bgSubtle),
                width = 1280,
                height = 800,
                capturedAt = At,
                alt = "The sign-in page of the billing portal",
            ),
            actions = listOf(
                MonarchAiComputerAction(
                    id = "1",
                    kind = MonarchAiComputerActionKind.Navigate,
                    label = "Opened the billing portal",
                    at = At - 4_000L,
                ),
                MonarchAiComputerAction(
                    id = "2",
                    kind = MonarchAiComputerActionKind.Click,
                    label = "Clicked Sign in",
                    at = At,
                    point = MonarchAiComputerPoint(640, 420),
                ),
            ),
            currentUrl = "https://billing.example.com/sign-in",
            windowTitle = "Billing portal",
            pointer = MonarchAiComputerPoint(660, 430),
            onPauseChange = {},
            onTakeOver = {},
            onStop = {},
            locale = Locale.US,
            timeZone = "UTC",
        )
    }
}
