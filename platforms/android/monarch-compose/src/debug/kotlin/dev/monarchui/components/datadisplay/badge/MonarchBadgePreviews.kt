package dev.monarchui.components.datadisplay.badge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

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
