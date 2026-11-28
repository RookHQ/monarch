package dev.monarchui.components.scheduling.calendartoolbar

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchCalendarToolbarPreview() {
    PreviewFrame {
        MonarchCalendarToolbar(
            title = "November 2026",
            view = MonarchCalendarToolbarView.Month,
            onToday = {},
            onPrevious = {},
            onNext = {},
            previousIcon = { Text("‹") },
            nextIcon = { Text("›") },
            onViewChange = {},
            actions = {
                MonarchButton(
                    text = "New event",
                    onClick = {},
                    variant = MonarchButtonVariant.Primary,
                    size = MonarchButtonSize.Sm,
                )
            },
        )
        MonarchCalendarToolbar(
            title = "2 – 8 November 2026",
            view = MonarchCalendarToolbarView.Week,
            onToday = {},
            onPrevious = {},
            onNext = {},
            previousIcon = { Text("‹") },
            nextIcon = { Text("›") },
            busy = true,
            todayEnabled = false,
            previousEnabled = false,
        )
    }
}
