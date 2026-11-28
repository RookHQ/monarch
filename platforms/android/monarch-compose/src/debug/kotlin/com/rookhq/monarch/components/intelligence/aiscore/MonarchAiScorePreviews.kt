package com.rookhq.monarch.components.intelligence.aiscore

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiScoreRingPreview() {
    PreviewFrame {
        MonarchAiScore(
            value = 72.0,
            label = "Lead score",
            rationale = "Engaged with pricing twice this week and matches the ideal customer profile.",
            factors = listOf(
                MonarchAiScoreFactor("Visited pricing", impact = 18.0, detail = "Twice in 7 days"),
                MonarchAiScoreFactor("No reply to last email", impact = -9.0),
            ),
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchAiScoreBarPreview() {
    PreviewFrame {
        MonarchAiScore(value = 34.0, variant = MonarchAiScoreVariant.Bar, label = "Churn risk", showBands = false)
        MonarchAiScore(value = null, size = MonarchAiScoreSize.Sm, label = "Fit")
    }
}
