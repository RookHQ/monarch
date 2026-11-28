@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.files.documentrelations.MonarchDocumentRelation
import dev.monarchui.components.files.documentrelations.MonarchDocumentRelationDirection
import dev.monarchui.components.files.documentrelations.MonarchDocumentRelationTarget
import dev.monarchui.components.files.documentrelations.MonarchDocumentRelations
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of DocumentRelations: relations kept in memory. */
object MonarchDocumentRelationsSection : CataloguePage {
    override val title = "DocumentRelations"

    private const val FIRST_NEW_ID = 3

    private val documents = listOf(
        MonarchDocumentRelationTarget("plan", "Q3 plan", description = "Finance", fileName = "q3-plan.pdf"),
        MonarchDocumentRelationTarget("pricing", "Pricing 2025", description = "Sales", fileName = "pricing.xlsx"),
        MonarchDocumentRelationTarget("brief", "Launch brief", description = "Marketing", fileName = "brief.docx"),
        MonarchDocumentRelationTarget("spec", "Billing spec", description = "Engineering", fileName = "billing.md"),
    )

    @Composable
    override fun Content() {
        var relations by remember {
            mutableStateOf(
                listOf(
                    MonarchDocumentRelation("r1", "parent-of", documents[0]),
                    MonarchDocumentRelation(
                        "r2",
                        "supersedes",
                        documents[1],
                        MonarchDocumentRelationDirection.Incoming,
                    ),
                ),
            )
        }
        var next by remember { mutableStateOf(FIRST_NEW_ID) }
        MonarchDocumentRelations(
            relations = relations,
            candidates = documents,
            showEmpty = true,
            onOpen = {},
            onAdd = { id, type ->
                val target = documents.first { it.id == id }
                relations = relations + MonarchDocumentRelation("r$next", type, target)
                next++
            },
            onRemove = { id -> relations = relations.filterNot { it.id == id } },
        )
        CatalogueCaption("Read only")
        MonarchDocumentRelations(relations = relations.take(1))
    }
}
