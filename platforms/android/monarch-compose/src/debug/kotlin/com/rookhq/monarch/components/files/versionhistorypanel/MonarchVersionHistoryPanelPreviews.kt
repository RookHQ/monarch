package com.rookhq.monarch.components.files.versionhistorypanel

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Locale
import java.util.TimeZone

private const val Now = 1_793_750_400_000L
private const val Hour = 3_600_000L
private const val HeightSteps = 4
private val Versions = listOf(
    MonarchVersionHistoryPanelVersion("v3", Now - Hour, "Ana Ruiz", note = "Ready for review"),
    MonarchVersionHistoryPanelVersion("v2", Now - 26 * Hour, "Ben Okafor", automatic = true),
)

@MonarchPreviews
@Composable
internal fun MonarchVersionHistoryPanelPreview() {
    PreviewFrame {
        val height = Modifier.height(MonarchTheme.spacing.space24 * HeightSteps)
        MonarchVersionHistoryPanel(
            versions = Versions,
            selection = null,
            onSelectionChange = {},
            currentLines = listOf("Ship on Monday.", "Tell support."),
            modifier = height,
            now = Now,
            onSaveVersion = {},
            onClose = {},
        )
        MonarchVersionHistoryPanel(
            versions = Versions,
            selection = "v2",
            onSelectionChange = {},
            currentLines = listOf("Ship on Monday.", "Tell support."),
            modifier = height,
            selectedLines = listOf("Ship on Friday.", "Tell support."),
            now = Now,
            locale = Locale.US,
            timeZone = "UTC",
            onRestore = {},
        )
    }
}
