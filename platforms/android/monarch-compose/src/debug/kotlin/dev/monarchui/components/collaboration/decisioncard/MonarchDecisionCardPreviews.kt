package dev.monarchui.components.collaboration.decisioncard

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val previewOptions = listOf(
    MonarchDecisionOption(label = "Keep", value = "keep", tone = MonarchTone.Success),
    MonarchDecisionOption(
        label = "Let lapse",
        value = "lapse",
        description = "The domain is released after it expires.",
        note = "Email for this domain stops working.",
        tone = MonarchTone.Danger,
        reasonRequired = true,
    ),
)

@MonarchPreviews
@Composable
internal fun MonarchDecisionCardPreview() {
    PreviewFrame {
        MonarchDecisionCard(
            options = previewOptions,
            value = "keep",
            title = "Renewal decision",
            decidedBy = "Ana",
            onDecide = {},
        )
        MonarchDecisionCard(
            options = previewOptions,
            value = "lapse",
            reason = "Replaced by the new brand.",
            decidedBy = "Ana",
            readOnly = true,
        )
    }
}
