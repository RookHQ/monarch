@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.files.documentrelations

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val relations = listOf(
    MonarchDocumentRelation(
        id = "r1",
        type = "parent-of",
        target = MonarchDocumentRelationTarget("d1", "Q3 plan", description = "Finance", fileName = "q3-plan.pdf"),
    ),
    MonarchDocumentRelation(
        id = "r2",
        type = "supersedes",
        target = MonarchDocumentRelationTarget("d2", "Pricing 2025"),
        direction = MonarchDocumentRelationDirection.Incoming,
    ),
)

@MonarchPreviews
@Composable
internal fun MonarchDocumentRelationsPreview() {
    PreviewFrame {
        MonarchDocumentRelations(relations = relations, onOpen = {}, onAdd = { _, _ -> }, onRemove = {})
        MonarchDocumentRelations(relations = emptyList(), showEmpty = true, onAdd = { _, _ -> })
    }
}
