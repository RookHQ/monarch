package com.rookhq.monarch.components.forms.durationinput

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchDurationInputPreview() {
    PreviewFrame {
        MonarchDurationInput(minutes = 90, onMinutesChange = {}, label = "Estimate", helperText = "Rounded to minutes.")
        MonarchDurationInput(
            minutes = 1560,
            onMinutesChange = {},
            label = "Time off",
            largestUnit = MonarchDurationInputUnit.Days,
            minutesPerDay = 480,
            showSteppers = false,
        )
        MonarchDurationInput(minutes = null, onMinutesChange = {}, label = "Break", errorText = "Enter a break.")
        MonarchDurationInput(minutes = 30, onMinutesChange = {}, label = "Buffer", enabled = false)
    }
}
