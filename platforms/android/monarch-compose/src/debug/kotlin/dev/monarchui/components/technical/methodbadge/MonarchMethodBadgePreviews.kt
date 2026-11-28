package dev.monarchui.components.technical.methodbadge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

@OptIn(ExperimentalLayoutApi::class)
@MonarchPreviews
@Composable
internal fun MonarchMethodBadgePreview() {
    PreviewFrame {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
        ) {
            listOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD").forEach { MonarchMethodBadge(method = it) }
            MonarchMethodBadge(method = "purge", tone = MonarchTone.Danger)
        }
    }
}
