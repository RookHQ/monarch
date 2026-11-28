package dev.monarchui.components.datadisplay.statusbadge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon
import dev.monarchui.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchStatusBadgePreview() {
    PreviewFrame {
        MonarchStatusBadgeVariant.entries.forEach { variant ->
            Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                listOf("operational", "partial", "down", "maintenance").forEach { key ->
                    MonarchStatusBadge(status = key, variant = variant)
                }
            }
        }
        MonarchStatusBadge(status = "pending_review", size = MonarchStatusBadgeSize.Sm)
        MonarchStatusBadge(
            definition = MonarchStatusDefinition(
                label = "Waiting on customer",
                tone = MonarchTone.Warning,
                icon = { MonarchIcon(PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm) },
            ),
        )
    }
}
