package dev.monarchui.components.forms.passwordstrengthmeter

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
