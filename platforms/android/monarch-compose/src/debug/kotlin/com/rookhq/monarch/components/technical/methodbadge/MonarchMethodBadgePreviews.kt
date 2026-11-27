package com.rookhq.monarch.components.technical.methodbadge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

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
