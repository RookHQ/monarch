package com.rookhq.monarch.components.forms.passwordstrengthmeter

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchPasswordStrengthMeterPreview() {
    PreviewFrame {
        MonarchPasswordStrengthMeter(password = "")
        MonarchPasswordStrengthMeter(password = "abc", showRules = false)
        MonarchPasswordStrengthMeter(password = "Abcdefg1", showRules = false)
        MonarchPasswordStrengthMeter(password = "Correct horse 9")
    }
}
