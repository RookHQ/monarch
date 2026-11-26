package com.rookhq.monarch.components.datadisplay.badge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchBadgePreview() {
    PreviewFrame {
        MonarchBadgeVariant.entries.forEach { variant ->
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            ) {
                MonarchTone.entries.forEach { tone ->
                    MonarchBadge(text = tone.name, tone = tone, variant = variant)
                }
            }
        }
    }
}
