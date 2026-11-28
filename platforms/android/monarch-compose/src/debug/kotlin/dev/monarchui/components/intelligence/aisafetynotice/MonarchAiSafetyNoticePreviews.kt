package dev.monarchui.components.intelligence.aisafetynotice

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiSafetyNoticePreview() {
    PreviewFrame {
        MonarchAiSafetyNotice(
            kind = MonarchAiSafetyNoticeKind.Blocked,
            reason = "Personal data",
            nextSteps = "Remove the phone number and ask again.",
            learnMoreUrl = "https://example.com/guidelines",
            onEditPrompt = {},
        )
        MonarchAiSafetyNotice(kind = MonarchAiSafetyNoticeKind.Modified)
        MonarchAiSafetyNotice(kind = MonarchAiSafetyNoticeKind.Filtered, onEditPrompt = {})
    }
}
