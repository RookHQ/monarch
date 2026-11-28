package com.rookhq.monarch.components.intelligence.aidisclaimer

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiDisclaimerPreview() {
    PreviewFrame {
        MonarchAiDisclaimer()
        MonarchAiDisclaimer(risk = MonarchAiDisclaimerRisk.Medium)
        MonarchAiDisclaimer(risk = MonarchAiDisclaimerRisk.High)
    }
}
