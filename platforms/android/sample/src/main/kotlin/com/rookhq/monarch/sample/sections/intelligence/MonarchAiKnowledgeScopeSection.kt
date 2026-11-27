package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.intelligence.aiknowledgescope.MonarchAiKnowledgeScope
import com.rookhq.monarch.components.intelligence.aiknowledgescope.MonarchAiKnowledgeSource
import com.rookhq.monarch.components.intelligence.aiknowledgescope.MonarchAiKnowledgeSourceKind
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of AiKnowledgeScope. */
object MonarchAiKnowledgeScopeSection : CataloguePage {
    override val title = "AiKnowledgeScope"

    /** A chevron that points to the end; it mirrors in right-to-left layouts. */
    private val Chevron: ImageVector by lazy {
        ImageVector.Builder(
            name = "Chevron",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = true,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(8.6f, 7.4f)
                lineTo(10f, 6f)
                lineTo(16f, 12f)
                lineTo(10f, 18f)
                lineTo(8.6f, 16.6f)
                lineTo(13.2f, 12f)
                close()
            }
        }.build()
    }

    private val sources = listOf(
        MonarchAiKnowledgeSource(
            id = "handbook",
            label = "Handbook",
            kind = MonarchAiKnowledgeSourceKind.Space,
            description = "3 documents",
            children = listOf(
                MonarchAiKnowledgeSource("onboarding", "Onboarding", MonarchAiKnowledgeSourceKind.Document),
                MonarchAiKnowledgeSource("expenses", "Expenses policy", MonarchAiKnowledgeSourceKind.Document),
                MonarchAiKnowledgeSource("travel", "Travel policy", MonarchAiKnowledgeSourceKind.Document),
            ),
        ),
        MonarchAiKnowledgeSource(
            id = "support",
            label = "Support",
            kind = MonarchAiKnowledgeSourceKind.Space,
            children = listOf(
                MonarchAiKnowledgeSource("macros", "Macros", MonarchAiKnowledgeSourceKind.Document),
                MonarchAiKnowledgeSource("helpdesk", "Help desk", MonarchAiKnowledgeSourceKind.App),
            ),
        ),
        MonarchAiKnowledgeSource("crm", "CRM", MonarchAiKnowledgeSourceKind.App, description = "Accounts and deals"),
    )

    @Composable
    override fun Content() {
        var selection by rememberSaveable { mutableStateOf(listOf("onboarding", "crm")) }
        CatalogueCaption("Pick what grounds the answers")
        MonarchAiKnowledgeScope(
            sources = sources,
            selection = selection,
            onSelectionChange = { selection = ArrayList(it) },
            expandIcon = { MonarchIcon(Chevron, contentDescription = null, size = MonarchIconSize.Sm) },
            initiallyExpanded = setOf("handbook"),
        )
        MonarchText(
            text = if (selection.isEmpty()) "Nothing selected." else "Selected: ${selection.joinToString()}",
            color = MonarchTheme.colors.textMuted,
        )
        CatalogueCaption("Disabled")
        MonarchAiKnowledgeScope(
            sources = sources,
            selection = listOf("support"),
            onSelectionChange = {},
            expandIcon = { MonarchIcon(Chevron, contentDescription = null, size = MonarchIconSize.Sm) },
            enabled = false,
            showSummary = false,
        )
    }
}
