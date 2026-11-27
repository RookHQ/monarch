package com.rookhq.monarch.components.intelligence.airelatedentities

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val previewEntities = listOf(
    MonarchAiRelatedEntity("jordan", "Jordan Lee", MonarchAiRelatedEntityKind.Person, weight = 0.9),
    MonarchAiRelatedEntity("acme", "Acme", MonarchAiRelatedEntityKind.Org, weight = 0.8),
    MonarchAiRelatedEntity("renewal", "Renewal 2026", MonarchAiRelatedEntityKind.Record, weight = 0.6),
    MonarchAiRelatedEntity("berlin", "Berlin office", MonarchAiRelatedEntityKind.Place),
)

private val previewRelations = listOf(
    MonarchAiEntityRelation("jordan", "acme", "works at"),
    MonarchAiEntityRelation("renewal", "acme"),
)

@MonarchPreviews
@Composable
internal fun MonarchAiRelatedEntitiesListPreview() {
    PreviewFrame {
        MonarchAiRelatedEntities(entities = previewEntities, relations = previewRelations, onSelect = {})
    }
}

@MonarchPreviews
@Composable
internal fun MonarchAiRelatedEntitiesGraphPreview() {
    PreviewFrame {
        MonarchAiRelatedEntities(
            entities = previewEntities,
            variant = MonarchAiRelatedEntitiesVariant.Graph,
            relations = previewRelations,
        )
        MonarchAiRelatedEntities(entities = emptyList())
    }
}
