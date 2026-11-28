@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.scheduling.timeoffpanel

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme
import java.util.TimeZone

private const val PreviewNow = 1_793_793_600_000L
private const val PreviewHeightSteps = 5

@MonarchPreviews
@Composable
internal fun MonarchTimeOffPanelPreview() {
    PreviewFrame {
        MonarchTimeOffPanel(
            entries = listOf(
                MonarchTimeOffEntry(
                    id = "1",
                    personId = "p1",
                    allDay = true,
                    start = 1_794_182_400_000L,
                    end = 1_794_528_000_000L,
                    personName = "Alex Kim",
                    type = "Annual leave",
                ),
                MonarchTimeOffEntry(
                    id = "2",
                    personId = null,
                    allDay = true,
                    start = 1_798_156_800_000L,
                    end = 1_798_243_200_000L,
                    reason = "Office closed",
                ),
            ),
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * PreviewHeightSteps),
            onAdd = {},
            onRemove = {},
            timeZone = "UTC",
            now = PreviewNow,
            is24Hour = false,
        )
    }
}
