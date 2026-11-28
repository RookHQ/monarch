package dev.monarchui.components.technical.lifecyclebadge

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
internal fun MonarchLifecycleBadgePreview() {
    PreviewFrame {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
        ) {
            MonarchLifecycle.entries.forEach { MonarchLifecycleBadge(lifecycle = it) }
            MonarchLifecycleBadge(stage = "early_access", tone = MonarchTone.Info)
            MonarchLifecycleBadge(lifecycle = MonarchLifecycle.Stable, showDot = false)
        }
    }
}
