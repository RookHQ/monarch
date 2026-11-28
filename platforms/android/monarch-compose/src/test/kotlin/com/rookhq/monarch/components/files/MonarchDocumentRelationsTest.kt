@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.files

import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.files.documentrelations.MonarchDocumentRelation
import com.rookhq.monarch.components.files.documentrelations.MonarchDocumentRelationDirection
import com.rookhq.monarch.components.files.documentrelations.MonarchDocumentRelationTarget
import com.rookhq.monarch.components.files.documentrelations.MonarchDocumentRelationType
import com.rookhq.monarch.components.files.documentrelations.MonarchDocumentRelations
import com.rookhq.monarch.components.files.documentrelations.MonarchDocumentRelationsLabels
import com.rookhq.monarch.components.files.documentrelations.MonarchDocumentRelationsTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

private val plan = MonarchDocumentRelationTarget("d1", "Q3 plan", description = "Finance", fileName = "q3-plan.pdf")
private val pricing = MonarchDocumentRelationTarget("d2", "Pricing 2025")
private val brief = MonarchDocumentRelationTarget("d3", "Launch brief", description = "Marketing")
private val notes = MonarchDocumentRelationTarget("d4", "Meeting notes")

private val sample = listOf(
    MonarchDocumentRelation("r1", "parent-of", plan),
    MonarchDocumentRelation("r2", "supersedes", pricing, MonarchDocumentRelationDirection.Incoming),
)

private val testTypes = listOf(
    MonarchDocumentRelationType("related-to", "Related to", "related-to"),
    MonarchDocumentRelationType("parent-of", "Parent of", "child-of"),
    MonarchDocumentRelationType("child-of", "Child of", "parent-of"),
)

internal object MonarchDocumentRelationsFixtures : ComponentFixtures {
    override val id = "document-relations"
    override val fixtures = listOf(
        fixture("relations, open, add, remove") {
            MonarchDocumentRelations(relations = sample, onOpen = {}, onAdd = { _, _ -> }, onRemove = {})
        },
        fixture("remove icon") {
            MonarchDocumentRelations(
                relations = sample.take(1),
                onRemove = {},
                removeIcon = { Icon(TestIcon, contentDescription = null) },
            )
        },
        fixture("read only") {
            MonarchDocumentRelations(relations = sample)
        },
        fixture("empty") {
            MonarchDocumentRelations(relations = emptyList(), showEmpty = true, onAdd = { _, _ -> })
        },
        fixture("long title") {
            MonarchDocumentRelations(
                relations = listOf(
                    MonarchDocumentRelation(
                        "r9",
                        "related-to",
                        MonarchDocumentRelationTarget("d9", "A document title far longer than the width of a phone"),
                    ),
                ),
                onRemove = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchDocumentRelationsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachRelationIsOneElementReadInWordsAndOpensItsDocument() {
        val opened = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchDocumentRelations(relations = sample, onOpen = { opened += it.id })
        }
        val row = rule.onNode(hasText("Q3 plan") and hasClickAction())
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        val text = row.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)?.map { it.text }
        assertEquals(listOf("Parent of", "Q3 plan"), text)
        row.assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf("d1"), opened)
    }

    @Test
    fun anIncomingRelationIsReadAsTheInverse() {
        rule.setMonarchContent { MonarchDocumentRelations(relations = sample) }
        rule.onNodeWithText("Superseded by", useUnmergedTree = true).assertExists()
        rule.onNode(hasText("Pricing 2025") and hasClickAction()).assertDoesNotExist()
    }

    @Test
    fun removingCallsBackAndIsAnnounced() {
        var relations by mutableStateOf(sample)
        val removed = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchDocumentRelations(
                relations = relations,
                onRemove = { id ->
                    removed += id
                    relations = relations.filterNot { it.id == id }
                },
            )
        }
        rule.onNodeWithContentDescription("Remove relation to Q3 plan")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitForIdle()
        assertEquals(listOf("r1"), removed)
        rule.onNodeWithContentDescription("Relation to Q3 plan removed")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
    }

    @Test
    fun aFailedRemovalShowsTheErrorUnderTheList() {
        rule.setMonarchContent {
            MonarchDocumentRelations(relations = sample, onRemove = { error("Read only.") })
        }
        rule.onNodeWithContentDescription("Remove relation to Pricing 2025").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Read only.", substring = true).assertExists()
    }

    @Test
    fun addingSearchesCandidatesAndAddsWithTheChosenType() {
        val added = mutableListOf<Pair<String, String>>()
        rule.setMonarchContent {
            MonarchDocumentRelations(
                relations = sample,
                types = testTypes,
                candidates = listOf(plan, brief, notes),
                onAdd = { id, type -> added += id to type },
            )
        }
        rule.onNodeWithText("Add relation").performClick()
        rule.onNodeWithText("Add a relation").assertExists()
        // Q3 plan is already related, so it is not offered.
        rule.onNode(hasText("Q3 plan") and hasClickAction()).assertDoesNotExist()
        rule.onNode(hasSetTextAction()).performTextInput("launch")
        rule.waitForIdle()
        rule.onNode(hasText("Meeting notes") and hasClickAction()).assertDoesNotExist()
        rule.onNode(hasText("Launch brief") and hasClickAction())
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitForIdle()
        assertEquals(listOf("d3" to "related-to"), added)
        rule.onNodeWithText("Add a relation").assertDoesNotExist()
        rule.onNodeWithContentDescription("Relation to Launch brief added").assertExists()
    }

    @Test
    fun aFailedAddKeepsTheSheetOpenWithTheError() {
        rule.setMonarchContent {
            MonarchDocumentRelations(
                relations = emptyList(),
                candidates = listOf(brief),
                onAdd = { _, _ -> error("Not allowed.") },
            )
        }
        rule.onNodeWithText("Add relation").performClick()
        rule.onNode(hasText("Launch brief") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Add a relation").assertExists()
        rule.onNodeWithText("Not allowed.", substring = true).assertExists()
    }

    @Test
    fun serverSearchGetsTheTrimmedText() {
        val queries = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchDocumentRelations(
                relations = emptyList(),
                candidates = listOf(brief, notes),
                onSearch = { queries += it },
                onAdd = { _, _ -> },
            )
        }
        rule.onNodeWithText("Add relation").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("zz ")
        rule.waitForIdle()
        assertEquals("zz", queries.last())
        // The app filters on its server, so the candidates it passed stay listed.
        rule.onNode(hasText("Meeting notes") and hasClickAction()).assertExists()
    }

    @Test
    fun emptyAndReadOnlyStates() {
        rule.setMonarchContent { MonarchDocumentRelations(relations = emptyList(), showEmpty = true) }
        rule.onNodeWithText("No relations yet.").assertExists()
        rule.onNodeWithText("Add relation").assertDoesNotExist()
    }

    @Test
    fun aRemoveIconMakesAnIconButtonAfterTheRowInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchDocumentRelations(
                relations = sample.take(1),
                onOpen = {},
                onRemove = {},
                removeIcon = { Icon(TestIcon, contentDescription = null) },
            )
        }
        val remove = rule.onNodeWithContentDescription("Remove relation to Q3 plan")
        remove.assertTouchTargetAtLeast48dp(rule.density)
        rule.onNode(hasText("Q3 plan") and hasClickAction()).assertIsBefore(remove, LayoutDirection.Rtl)
        rule.onNodeWithText("Remove").assertDoesNotExist()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchDocumentRelations(
                relations = sample.take(1),
                onAdd = { _, _ -> },
                onRemove = {},
                labels = MonarchDocumentRelationsLabels(
                    add = "Link",
                    removeText = "Unlink",
                    remove = { "Unlink $it" },
                ),
            )
        }
        rule.onNodeWithText("Link").assertExists()
        rule.onNodeWithContentDescription("Unlink Q3 plan").assertExists()
    }

    @Test
    fun longTitlesWrapAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchDocumentRelations(relations = sample, onRemove = {})
        }
        rule.onNodeWithText("Q3 plan", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun tokensReadTypesAndFilterCandidates() {
        val tokens = MonarchDocumentRelationsTokens
        assertEquals("parent-of" to "Parent of", tokens.labelOf(sample[0], testTypes))
        assertEquals(
            "child-of" to "Child of",
            tokens.labelOf(
                MonarchDocumentRelation("x", "parent-of", plan, MonarchDocumentRelationDirection.Incoming),
                testTypes,
            ),
        )
        assertEquals("custom" to "custom", tokens.labelOf(MonarchDocumentRelation("x", "custom", plan), testTypes))
        assertEquals("related-to", tokens.initialType(null, testTypes))
        assertEquals("child-of", tokens.initialType("child-of", testTypes))
        assertEquals("parent-of", tokens.initialType(null, testTypes.drop(1)))
        assertEquals(listOf(brief, notes), tokens.candidates(listOf(plan, brief, notes), sample, "", false))
        assertEquals(listOf(brief), tokens.candidates(listOf(plan, brief, notes), sample, " LAUNCH", false))
        assertEquals(listOf(brief, notes), tokens.candidates(listOf(brief, notes), sample, "zz", true))
        val many = (1..20).map { MonarchDocumentRelationTarget("m$it", "Doc $it") }
        assertEquals(MonarchDocumentRelationsTokens.MAX_RESULTS, tokens.candidates(many, emptyList(), "", false).size)
    }

    @Test
    fun coloursComeFromRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchDocumentRelationsTokens
            assertEquals(colors.textLink, tokens.titleColor(openable = true, enabled = true, colors))
            assertEquals(colors.textDefault, tokens.titleColor(openable = false, enabled = true, colors))
            assertEquals(colors.textDisabled, tokens.titleColor(openable = true, enabled = false, colors))
            assertEquals(colors.textMuted, tokens.typeColor(colors))
            assertEquals(colors.bgSurface, tokens.sheetColor(colors))
        }
    }
}
