package com.rookhq.monarch.components.datadisplay.statusbadge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon
import com.rookhq.monarch.theme.MonarchTheme

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
