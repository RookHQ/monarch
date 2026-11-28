package dev.monarchui.components.files.filetypeicon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

@OptIn(ExperimentalLayoutApi::class)
@MonarchPreviews
@Composable
internal fun MonarchFileTypeIconPreview() {
    PreviewFrame {
        MonarchFileTypeIconSize.entries.forEach { size ->
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchFileTypeFamily.entries.forEach { family -> MonarchFileTypeIcon(family = family, size = size) }
            }
        }
    }
}
