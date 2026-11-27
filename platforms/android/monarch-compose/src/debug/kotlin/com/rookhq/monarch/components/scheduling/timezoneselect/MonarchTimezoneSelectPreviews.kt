package com.rookhq.monarch.components.scheduling.timezoneselect

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

/** 4 November 2026, 12:00 UTC. */
private const val PreviewNow = 1_793_793_600_000L

@MonarchPreviews
@Composable
internal fun MonarchTimezoneSelectPreview() {
    PreviewFrame {
        MonarchTimezoneSelect(
            label = "Time zone",
            selected = "Asia/Calcutta",
            onSelectedChange = {},
            helperText = "Meeting times are shown in this zone.",
            pinned = listOf("Europe/London", "America/New_York"),
            now = PreviewNow,
        )
        MonarchTimezoneSelect(
            label = "Meeting time zone",
            selected = null,
            onSelectedChange = {},
            placeholder = "Choose a time zone",
            errorText = "Choose a time zone.",
            zones = listOf("UTC", "Europe/Paris", "Asia/Kolkata"),
            now = PreviewNow,
        )
        MonarchTimezoneSelect(
            label = "Disabled",
            selected = "UTC",
            onSelectedChange = {},
            enabled = false,
            now = PreviewNow,
        )
    }
}
