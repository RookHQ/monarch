package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.intelligence.airelatedentities.MonarchAiEntityRelation
import dev.monarchui.components.intelligence.airelatedentities.MonarchAiRelatedEntities
import dev.monarchui.components.intelligence.airelatedentities.MonarchAiRelatedEntitiesVariant
import dev.monarchui.components.intelligence.airelatedentities.MonarchAiRelatedEntity
import dev.monarchui.components.intelligence.airelatedentities.MonarchAiRelatedEntityKind
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

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
