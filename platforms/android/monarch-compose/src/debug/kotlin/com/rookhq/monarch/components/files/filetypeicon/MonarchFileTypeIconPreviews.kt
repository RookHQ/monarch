package com.rookhq.monarch.components.files.filetypeicon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

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
