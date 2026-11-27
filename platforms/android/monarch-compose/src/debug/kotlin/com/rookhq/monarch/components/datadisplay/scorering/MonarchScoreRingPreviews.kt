package com.rookhq.monarch.components.datadisplay.scorering

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchScoreRingPreview() {
    PreviewFrame {
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3)) {
            listOf(92.0, 71.0, 45.0, 12.0).forEach { MonarchScoreRing(value = it, size = MonarchScoreRingSize.Sm) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3)) {
            MonarchScoreRing(value = 78.0, label = "Health", description = "Updated today")
            MonarchScoreRing(value = null, label = "Quality")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3)) {
            MonarchScoreRing(
                value = 3.5,
                max = 5.0,
                color = MonarchScoreRingColor.Accent,
                format = { "%.1f".format(it) },
            )
            MonarchScoreRing(value = 64.0, color = MonarchScoreRingColor.Tone(MonarchTone.Neutral))
        }
        MonarchScoreRing(value = 88.0, size = MonarchScoreRingSize.Lg, label = "Overall")
    }
}
