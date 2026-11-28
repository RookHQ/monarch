package com.rookhq.monarch.components.intelligence

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.intelligence.airelatedentities.MonarchAiEntityRelation
import com.rookhq.monarch.components.intelligence.airelatedentities.MonarchAiRelatedEntities
import com.rookhq.monarch.components.intelligence.airelatedentities.MonarchAiRelatedEntitiesLabels
import com.rookhq.monarch.components.intelligence.airelatedentities.MonarchAiRelatedEntitiesLogic
import com.rookhq.monarch.components.intelligence.airelatedentities.MonarchAiRelatedEntitiesTokens
import com.rookhq.monarch.components.intelligence.airelatedentities.MonarchAiRelatedEntitiesVariant
import com.rookhq.monarch.components.intelligence.airelatedentities.MonarchAiRelatedEntity
import com.rookhq.monarch.components.intelligence.airelatedentities.MonarchAiRelatedEntityKind
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val relatedEntities = listOf(
    MonarchAiRelatedEntity("berlin", "Berlin office", MonarchAiRelatedEntityKind.Place),
    MonarchAiRelatedEntity("acme", "Acme", MonarchAiRelatedEntityKind.Org, weight = 0.8),
    MonarchAiRelatedEntity("jordan", "Jordan Lee", MonarchAiRelatedEntityKind.Person, weight = 0.9),
    MonarchAiRelatedEntity("renewal", "Renewal", MonarchAiRelatedEntityKind.Record, weight = 0.6),
)

private val relatedRelations = listOf(
    MonarchAiEntityRelation("jordan", "acme", "works at"),
    MonarchAiEntityRelation("renewal", "acme"),
    MonarchAiEntityRelation("renewal", "unknown"),
)

private const val Jordan = "Jordan Lee. Person. Relevance 90%. Related to: works at Acme"
private const val Acme = "Acme. Organization. Relevance 80%. Related to: works at Jordan Lee, Renewal"

internal object MonarchAiRelatedEntitiesFixtures : ComponentFixtures {
    override val id = "ai-related-entities"
    override val fixtures = listOf(
        fixture("list") { MonarchAiRelatedEntities(entities = relatedEntities.take(3), relations = relatedRelations) },
        fixture("list, selectable") {
            MonarchAiRelatedEntities(entities = relatedEntities.take(2), relations = relatedRelations, onSelect = {})
        },
        fixture("graph") {
            MonarchAiRelatedEntities(
                entities = relatedEntities,
                variant = MonarchAiRelatedEntitiesVariant.Graph,
                relations = relatedRelations,
            )
        },
        fixture("graph, selectable, custom center") {
            MonarchAiRelatedEntities(
                entities = relatedEntities,
                variant = MonarchAiRelatedEntitiesVariant.Graph,
                centerId = "acme",
                onSelect = {},
            )
        },
        fixture("long name and custom kind") {
            MonarchAiRelatedEntities(
                entities = listOf(
                    MonarchAiRelatedEntity(
                        id = "long",
                        label = "International Federation of Independent Regional Distributors",
                        kind = MonarchAiRelatedEntityKind("Supplier"),
                        weight = 0.4,
                    ),
                ),
            )
        },
        fixture("empty") { MonarchAiRelatedEntities(entities = emptyList()) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiRelatedEntitiesTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theListIsRankedAndEachEntityIsOneElement() {
        rule.setMonarchContent { MonarchAiRelatedEntities(entities = relatedEntities, relations = relatedRelations) }
        rule.onNodeWithContentDescription("Related entities").assertExists()
        val jordan = rule.onNodeWithContentDescription(Jordan).getUnclippedBoundsInRoot()
        val acme = rule.onNodeWithContentDescription(Acme).getUnclippedBoundsInRoot()
        val berlin = rule.onNodeWithContentDescription("Berlin office. Place").getUnclippedBoundsInRoot()
        assertTrue(jordan.top < acme.top)
        assertTrue(acme.top < berlin.top)
        rule.onNodeWithText("Related to: works at Acme", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Organization", useUnmergedTree = true).assertExists()
    }

    @Test
    fun withOnSelectEachEntityIsAButton() {
        val selected = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiRelatedEntities(
                entities = relatedEntities,
                relations = relatedRelations,
                onSelect = { selected += it.id },
            )
        }
        rule.onNodeWithContentDescription(Jordan)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(listOf("jordan"), selected)
    }

    @Test
    fun graphNodesAreNamedImagesOrButtons() {
        rule.setMonarchContent {
            MonarchAiRelatedEntities(
                entities = relatedEntities,
                modifier = Modifier.width(320.dp),
                variant = MonarchAiRelatedEntitiesVariant.Graph,
                relations = relatedRelations,
            )
        }
        val center = rule.onNodeWithContentDescription(Jordan)
        center.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
        val middle = center.getUnclippedBoundsInRoot()
        assertEquals(160f, ((middle.left + middle.right) / 2).value, 1f)
        // The first ring node is at the top, the second on the right, clockwise.
        val top = rule.onNodeWithContentDescription(Acme).getUnclippedBoundsInRoot()
        val right = rule.onNodeWithContentDescription("Renewal. Record. Relevance 60%. Related to: Acme")
            .getUnclippedBoundsInRoot()
        assertTrue(top.bottom < middle.top)
        assertTrue(right.left > middle.right)
        rule.onNodeWithText("Related to: works at Acme", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun theGraphIsNotMirroredInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiRelatedEntities(
                entities = relatedEntities,
                modifier = Modifier.width(320.dp),
                variant = MonarchAiRelatedEntitiesVariant.Graph,
                onSelect = {},
            )
        }
        val middle = rule.onNodeWithContentDescription("Jordan Lee. Person. Relevance 90%").getUnclippedBoundsInRoot()
        val right = rule.onNodeWithContentDescription("Renewal. Record. Relevance 60%")
            .assertTouchTargetAtLeast48dp(rule.density)
            .getUnclippedBoundsInRoot()
        assertTrue(right.left > middle.right)
    }

    @Test
    fun theGraphFallsBackToTheListWhenItDoesNotFit() {
        rule.setMonarchContent {
            MonarchAiRelatedEntities(
                entities = relatedEntities,
                variant = MonarchAiRelatedEntitiesVariant.Graph,
                relations = relatedRelations,
                maxGraphNodes = 3,
            )
        }
        rule.onNodeWithText("Related to: works at Acme", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theLargestFontScaleShowsTheListWithWrappedNames() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiRelatedEntities(
                entities = relatedEntities.drop(1).take(2),
                variant = MonarchAiRelatedEntitiesVariant.Graph,
                relations = relatedRelations,
            )
        }
        rule.onNodeWithText("Related to: works at Acme", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Jordan Lee", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theLabelsReplaceTheBuiltInStringsAndCustomKindsShowTheirValue() {
        rule.setMonarchContent {
            MonarchAiRelatedEntities(
                entities = listOf(
                    MonarchAiRelatedEntity("a", "Acme", MonarchAiRelatedEntityKind.Org, weight = 0.5),
                    MonarchAiRelatedEntity("s", "Steel Co", MonarchAiRelatedEntityKind("Supplier")),
                ),
                relations = listOf(MonarchAiEntityRelation("a", "s", "buys from")),
                labels = MonarchAiRelatedEntitiesLabels(
                    label = "Linked",
                    kind = { if (it == MonarchAiRelatedEntityKind.Org) "Company" else it.value },
                    weight = { "Score ${(it * 10).toInt()}" },
                    relation = { other, label -> "$other ($label)" },
                    relatedTo = "Links",
                ),
            )
        }
        rule.onNodeWithContentDescription("Linked").assertExists()
        rule.onNodeWithContentDescription("Acme. Company. Score 5. Links: Steel Co (buys from)").assertExists()
        rule.onNodeWithContentDescription("Steel Co. Supplier. Links: Acme (buys from)").assertExists()
    }

    @Test
    fun noEntitiesShowsTheEmptyState() {
        rule.setMonarchContent { MonarchAiRelatedEntities(entities = emptyList()) }
        rule.onNodeWithText("No related entities", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theRulesMatchTheWeb() {
        val logic = MonarchAiRelatedEntitiesLogic
        assertEquals(listOf("jordan", "acme", "renewal", "berlin"), logic.sorted(relatedEntities).map { it.id })
        assertEquals(1.0, logic.weight(1.4))
        assertNull(logic.weight(Double.NaN))
        assertEquals(
            listOf("Jordan Lee" to "works at", "Renewal" to null),
            logic.relationsOf("acme", relatedEntities, relatedRelations),
        )
        assertTrue(logic.showsGraph(MonarchAiRelatedEntitiesVariant.Graph, 4, 12, 1f))
        assertFalse(logic.showsGraph(MonarchAiRelatedEntitiesVariant.Graph, 13, 12, 1f))
        assertFalse(logic.showsGraph(MonarchAiRelatedEntitiesVariant.Graph, 4, 12, LargestFontScale))
        assertFalse(logic.showsGraph(MonarchAiRelatedEntitiesVariant.List, 4, 12, 1f))
        assertEquals("acme", logic.center(logic.sorted(relatedEntities), "acme")?.id)
        assertEquals("jordan", logic.center(logic.sorted(relatedEntities), "missing")?.id)
        val top = logic.ringPoint(0, 4, 100f)
        assertEquals(0f, top.x, 0.01f)
        assertEquals(-100f, top.y, 0.01f)
        assertEquals(Offset(100f, 0f).x, logic.ringPoint(1, 4, 100f).x, 0.01f)
        assertEquals(MonarchAiRelatedEntityKind("person"), MonarchAiRelatedEntityKind.Person)
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchAiRelatedEntitiesTokens
            assertEquals(colors.borderSubtle, tokens.rowBorder(colors), theme.id)
            assertEquals(colors.aiSolid, tokens.weightFill(colors), theme.id)
            assertEquals(colors.bgMuted, tokens.weightTrack(colors), theme.id)
            assertEquals(colors.borderDefault, tokens.edge(colors), theme.id)
        }
    }
}
