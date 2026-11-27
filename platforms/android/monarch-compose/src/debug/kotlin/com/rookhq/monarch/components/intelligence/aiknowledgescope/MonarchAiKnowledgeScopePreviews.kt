package com.rookhq.monarch.components.intelligence.aiknowledgescope

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

private val previewSources = listOf(
    MonarchAiKnowledgeSource(
        id = "handbook",
        label = "Handbook",
        kind = MonarchAiKnowledgeSourceKind.Space,
        description = "3 documents",
        children = listOf(
            MonarchAiKnowledgeSource("onboarding", "Onboarding", MonarchAiKnowledgeSourceKind.Document),
            MonarchAiKnowledgeSource("expenses", "Expenses policy", MonarchAiKnowledgeSourceKind.Document),
        ),
    ),
    MonarchAiKnowledgeSource("crm", "CRM", MonarchAiKnowledgeSourceKind.App, description = "Accounts and deals"),
)

@MonarchPreviews
@Composable
internal fun MonarchAiKnowledgeScopePreview() {
    PreviewFrame {
        MonarchAiKnowledgeScope(
            sources = previewSources,
            selection = listOf("onboarding", "crm"),
            onSelectionChange = {},
            expandIcon = { MonarchIcon(PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm) },
            initiallyExpanded = setOf("handbook"),
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchAiKnowledgeScopeDisabledPreview() {
    PreviewFrame {
        MonarchAiKnowledgeScope(
            sources = previewSources,
            selection = listOf("handbook"),
            onSelectionChange = {},
            expandIcon = { MonarchIcon(PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm) },
            enabled = false,
        )
        MonarchAiKnowledgeScope(
            sources = emptyList(),
            selection = emptyList(),
            onSelectionChange = {},
            expandIcon = { MonarchIcon(PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm) },
        )
    }
}
