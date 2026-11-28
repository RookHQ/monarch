package dev.monarchui.components.intelligence.aidisclaimer

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiDisclaimerPreview() {
    PreviewFrame {
        MonarchAiDisclaimer()
        MonarchAiDisclaimer(risk = MonarchAiDisclaimerRisk.Medium)
        MonarchAiDisclaimer(risk = MonarchAiDisclaimerRisk.High)
    }
}
