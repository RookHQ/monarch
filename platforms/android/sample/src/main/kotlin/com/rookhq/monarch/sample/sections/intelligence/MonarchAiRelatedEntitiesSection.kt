package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.intelligence.airelatedentities.MonarchAiEntityRelation
import com.rookhq.monarch.components.intelligence.airelatedentities.MonarchAiRelatedEntities
import com.rookhq.monarch.components.intelligence.airelatedentities.MonarchAiRelatedEntitiesVariant
import com.rookhq.monarch.components.intelligence.airelatedentities.MonarchAiRelatedEntity
import com.rookhq.monarch.components.intelligence.airelatedentities.MonarchAiRelatedEntityKind
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of AiRelatedEntities. */
object MonarchAiRelatedEntitiesSection : CataloguePage {
    override val title = "AiRelatedEntities"

    private const val JORDAN = 0.9
    private const val ACME = 0.8
    private const val RENEWAL = 0.6
    private const val PRIYA = 0.5

    private val entities = listOf(
        MonarchAiRelatedEntity("jordan", "Jordan Lee", MonarchAiRelatedEntityKind.Person, weight = JORDAN),
        MonarchAiRelatedEntity("acme", "Acme", MonarchAiRelatedEntityKind.Org, weight = ACME),
        MonarchAiRelatedEntity("renewal", "Renewal 2026", MonarchAiRelatedEntityKind.Record, weight = RENEWAL),
        MonarchAiRelatedEntity("priya", "Priya Shah", MonarchAiRelatedEntityKind.Person, weight = PRIYA),
        MonarchAiRelatedEntity("berlin", "Berlin office", MonarchAiRelatedEntityKind.Place),
    )

    private val relations = listOf(
        MonarchAiEntityRelation("jordan", "acme", "works at"),
        MonarchAiEntityRelation("priya", "acme", "works at"),
        MonarchAiEntityRelation("renewal", "acme"),
        MonarchAiEntityRelation("acme", "berlin", "based in"),
    )

    @Composable
    override fun Content() {
        var selected by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Ranked list, selectable")
        MonarchAiRelatedEntities(entities = entities, relations = relations, onSelect = { selected = it.label })
        MonarchText(
            text = selected?.let { "Selected: $it" } ?: "Nothing selected.",
            color = MonarchTheme.colors.textMuted,
        )
        CatalogueCaption("Radial graph")
        MonarchAiRelatedEntities(
            entities = entities,
            variant = MonarchAiRelatedEntitiesVariant.Graph,
            relations = relations,
            onSelect = { selected = it.label },
        )
    }
}
