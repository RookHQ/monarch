@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiinlinediff.MonarchAiDiffSegment
import com.rookhq.monarch.components.intelligence.aiinlinediff.MonarchAiDiffSegmentType
import com.rookhq.monarch.components.intelligence.aiinlinediff.MonarchAiInlineDiff
import com.rookhq.monarch.components.intelligence.aiinlinediff.MonarchAiInlineDiffLabels
import com.rookhq.monarch.components.intelligence.aiinlinediff.MonarchAiInlineDiffTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private fun eq(text: String, id: String? = null) = MonarchAiDiffSegment(MonarchAiDiffSegmentType.Equal, text, id)
private fun ins(text: String, id: String? = null) = MonarchAiDiffSegment(MonarchAiDiffSegmentType.Insert, text, id)
private fun del(text: String, id: String? = null) = MonarchAiDiffSegment(MonarchAiDiffSegmentType.Delete, text, id)

private val Meeting = listOf(
    eq("We meet every "),
    del("week"),
    ins("Monday"),
    eq(" to review the "),
    del("plan"),
    ins("roadmap"),
    eq("."),
)

internal object MonarchAiInlineDiffFixtures : ComponentFixtures {
    override val id = "ai-inline-diff"
    override val fixtures = listOf(
        fixture("two changes with accept and reject") {
            MonarchAiInlineDiff(segments = Meeting, onAccept = {}, onReject = {})
        },
        fixture("read only, without navigation") {
            MonarchAiInlineDiff(segments = Meeting, showNavigation = false)
        },
        fixture("one change, accept only") {
            MonarchAiInlineDiff(segments = listOf(eq("Ships on "), del("Friday"), ins("Monday")), onAccept = {})
        },
        fixture("no changes") {
            MonarchAiInlineDiff(segments = listOf(eq("Nothing to change here.")), onAccept = {}, onReject = {})
        },
        fixture("long text") {
            MonarchAiInlineDiff(
                segments = listOf(
                    eq("The quarterly review covers revenue, churn, hiring and the "),
                    del("product plan for the coming year"),
                    ins("roadmap for the next two quarters, with dates"),
                ),
                onAccept = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiInlineDiffTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun changesGroupRunsOfRemovedAndAddedText() {
        val changes = MonarchAiDiffSegment.changes(Meeting)
        assertEquals(listOf("1", "4"), changes.map { it.id })
        assertEquals("week", changes[0].deleted)
        assertEquals("Monday", changes[0].inserted)
        assertEquals(listOf(4, 5), changes[1].segmentIndexes)
        val ids = MonarchAiDiffSegment.changes(listOf(del("a", "x"), ins("b", "x"), ins("c", "y")))
        assertEquals(listOf("x", "y"), ids.map { it.id })
    }

    @Test
    fun resolveKeepsTheChosenSideAndJoinsUnchangedText() {
        val accepted = MonarchAiDiffSegment.resolve(Meeting, "1", accept = true)
        assertEquals(eq("We meet every Monday to review the "), accepted.first())
        val left = MonarchAiDiffSegment.changes(accepted).single()
        // Ids without an explicit id follow the position of the change's first segment.
        assertEquals("1", left.id)
        val rejected = MonarchAiDiffSegment.resolve(accepted, left.id, accept = false)
        assertEquals(listOf(eq("We meet every Monday to review the plan.")), rejected)
        assertEquals(Meeting, MonarchAiDiffSegment.resolve(Meeting, "missing", accept = true))
    }

    @Test
    fun diffWordsMarksTheChangedWords() {
        val segments = MonarchAiDiffSegment.diffWords("Ships on Friday.", "Ships on Monday.")
        assertEquals("Ships on ", segments.first().text)
        assertEquals(MonarchAiDiffSegmentType.Equal, segments.first().type)
        val change = MonarchAiDiffSegment.changes(segments).single()
        assertEquals("Friday", change.deleted)
        assertEquals("Monday", change.inserted)
    }

    @Test
    fun theTextIsOneElementReadWithDeletedAndInserted() {
        rule.setMonarchContent { MonarchAiInlineDiff(segments = Meeting) }
        val text = rule.onNodeWithContentDescription(
            "We meet every Deleted: week Inserted: Monday to review the Deleted: plan Inserted: roadmap .",
        )
        assertEquals("Change 1 of 2", text.stateDescription())
        rule.onNodeWithText("Change 1 of 2").assertExists()
        rule.onNodeWithContentDescription("AI suggested changes").assertExists()
    }

    @Test
    fun nextAndPreviousMoveAndWrap() {
        rule.setMonarchContent { MonarchAiInlineDiff(segments = Meeting) }
        rule.onNodeWithContentDescription("Next").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Change 2 of 2").assertExists()
        rule.onNodeWithContentDescription("Next").performClick()
        rule.onNodeWithText("Change 1 of 2").assertExists()
        rule.onNodeWithContentDescription("Previous").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Change 2 of 2").assertExists()
    }

    @Test
    fun oneChangeDisablesNavigation() {
        rule.setMonarchContent { MonarchAiInlineDiff(segments = Meeting.take(3)) }
        rule.onNodeWithText("Change 1 of 1").assertExists()
        rule.onNodeWithContentDescription("Next").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Previous").assertIsNotEnabled()
    }

    @Test
    fun acceptCallsBackWithTheActiveChangeAndAnnounces() {
        var segments by mutableStateOf(Meeting)
        val accepted = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiInlineDiff(
                segments = segments,
                onAccept = { id ->
                    accepted += id
                    segments = MonarchAiDiffSegment.resolve(segments, id, accept = true)
                },
                onReject = {},
            )
        }
        rule.onNodeWithContentDescription("Next").performClick()
        rule.onNodeWithContentDescription("Accept change 2 of 2")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(listOf("4"), accepted)
        rule.onNodeWithContentDescription("Change accepted. 1 left.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithText("Change 1 of 1").assertExists()
    }

    @Test
    fun rejectingTheLastChangeAnnouncesNoneLeft() {
        var segments by mutableStateOf(Meeting.take(3))
        val rejected = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiInlineDiff(
                segments = segments,
                onReject = { id ->
                    rejected += id
                    segments = MonarchAiDiffSegment.resolve(segments, id, accept = false)
                },
            )
        }
        rule.onNodeWithContentDescription("Accept change 1 of 1").assertDoesNotExist()
        rule.onNodeWithContentDescription("Reject change 1 of 1").performClick()
        assertEquals(listOf("1"), rejected)
        rule.onNodeWithContentDescription("Change rejected. No changes left.").assertExists()
        rule.onNodeWithText("No suggested changes").assertExists()
        rule.onNodeWithContentDescription("Next").assertDoesNotExist()
    }

    @Test
    fun customActionsMoveAndDecideWithoutATap() {
        val accepted = mutableListOf<String>()
        rule.setMonarchContent { MonarchAiInlineDiff(segments = Meeting, onAccept = { accepted += it }) }
        val text = rule.onNode(hasContentDescription("We meet every", substring = true))
        val actions = text.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(listOf("Next", "Previous", "Accept change 1 of 2"), actions.map { it.label })
        rule.runOnIdle { actions.first { it.label == "Next" }.action() }
        rule.onNodeWithText("Change 2 of 2").assertExists()
        val after = text.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        rule.runOnIdle { after.first { it.label == "Accept change 2 of 2" }.action() }
        assertEquals(listOf("4"), accepted)
    }

    @Test
    fun readOnlyHasNoDecisionsAndNoChangesHasNoActions() {
        rule.setMonarchContent { MonarchAiInlineDiff(segments = listOf(eq("Plain text."))) }
        rule.onNodeWithText("No suggested changes").assertExists()
        val text = rule.onNodeWithContentDescription("Plain text.")
        assertEquals(emptyList(), text.fetchSemanticsNode().config[SemanticsActions.CustomActions])
        assertEquals(null, text.stateDescription())
    }

    @Test
    fun withoutNavigationTheBarIsHidden() {
        rule.setMonarchContent { MonarchAiInlineDiff(segments = Meeting, showNavigation = false, onAccept = {}) }
        rule.onNodeWithText("Change 1 of 2").assertDoesNotExist()
        rule.onNodeWithContentDescription("Accept change 1 of 2").assertIsEnabled()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchAiInlineDiff(
                segments = Meeting,
                onAccept = {},
                labels = MonarchAiInlineDiffLabels(
                    position = { index, total -> "Vorschlag $index von $total" },
                    next = "Weiter",
                    accept = "Annehmen",
                    acceptChange = { index, _ -> "Vorschlag $index annehmen" },
                    deleted = "Entfernt:",
                    inserted = "Neu:",
                ),
            )
        }
        rule.onNodeWithText("Vorschlag 1 von 2").assertExists()
        rule.onNodeWithContentDescription("Weiter").assertExists()
        rule.onNode(hasText("Annehmen") or hasContentDescription("Vorschlag 1 annehmen")).assertExists()
        rule.onNodeWithContentDescription("Entfernt: week", substring = true).assertExists()
    }

    @Test
    fun theButtonsFollowTheLayoutDirection() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiInlineDiff(segments = Meeting, onAccept = {}, onReject = {})
        }
        rule.onNodeWithContentDescription("Previous")
            .assertIsBefore(rule.onNodeWithContentDescription("Next"), LayoutDirection.Rtl)
        rule.onNodeWithContentDescription("Accept change 1 of 2")
            .assertIsBefore(rule.onNodeWithContentDescription("Reject change 1 of 2"), LayoutDirection.Rtl)
    }

    @Test
    fun theTextWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchAiInlineDiff(segments = Meeting) }
        rule.onNodeWithContentDescription("We meet every", substring = true).assertTextFullyVisible()
        rule.onNodeWithText("Change 1 of 2").assertTextFullyVisible()
    }

    @Test
    fun removedAndAddedTextIsMarkedBeyondColourInEveryTheme() {
        AllThemes.forEach { tokens ->
            val colors = MonarchColors(tokens.colors)
            val removed = MonarchAiInlineDiffTokens.span(MonarchAiDiffSegmentType.Delete, colors)
            val added = MonarchAiInlineDiffTokens.span(MonarchAiDiffSegmentType.Insert, colors)
            assertEquals(TextDecoration.LineThrough, removed.textDecoration, tokens.id)
            assertEquals(TextDecoration.Underline, added.textDecoration, tokens.id)
            assertEquals(colors.aiDiffRemoveStrong, removed.color, tokens.id)
            assertEquals(colors.aiDiffAdd, added.background, tokens.id)
            assertNotEquals(removed.background, added.background, tokens.id)
            assertEquals(colors.aiBorder, MonarchAiInlineDiffTokens.activeOutline(colors), tokens.id)
        }
    }
}
