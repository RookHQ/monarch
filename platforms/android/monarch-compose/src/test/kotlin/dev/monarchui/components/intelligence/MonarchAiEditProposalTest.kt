@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.files.diffviewer.MonarchDiffViewerLineKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiEditProposalPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiEditProposalState
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.components.intelligence.aieditproposal.MonarchAiEditHunk
import dev.monarchui.components.intelligence.aieditproposal.MonarchAiEditHunkStatus
import dev.monarchui.components.intelligence.aieditproposal.MonarchAiEditProposal
import dev.monarchui.components.intelligence.aieditproposal.MonarchAiEditProposalEvent
import dev.monarchui.components.intelligence.aieditproposal.MonarchAiEditProposalLabels
import dev.monarchui.components.intelligence.aieditproposal.MonarchAiEditProposalLogic
import dev.monarchui.components.intelligence.aieditproposal.MonarchAiEditProposalTokens
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

private fun part(state: MonarchAiEditProposalState = MonarchAiEditProposalState.Proposed, id: String = "p1") =
    MonarchAiEditProposalPart(id = id, target = "doc-1", diff = MonarchAiJSON.Null, state = state)

private val Date = MonarchAiEditHunk("1", "Ships on Friday.", "Ships on Monday.", label = "Release date")
private val Fee = MonarchAiEditHunk("2", "The fee is 2%.", "The fee is 3%.")
private val Closing = MonarchAiEditHunk("3", "", "A new closing paragraph.")

internal object MonarchAiEditProposalFixtures : ComponentFixtures {
    override val id = "ai-edit-proposal"
    override val fixtures = listOf(
        fixture("proposed with every button") {
            MonarchAiEditProposal(
                part = part(),
                hunks = listOf(Date),
                onAcceptAll = {},
                onRejectAll = {},
                onAcceptHunk = {},
                onRejectHunk = {},
            )
        },
        fixture("accepted and rejected changes") {
            MonarchAiEditProposal(
                part = part(),
                hunks = listOf(
                    MonarchAiEditHunk("1", "Friday", "Monday", status = MonarchAiEditHunkStatus.Accepted),
                    MonarchAiEditHunk("2", "2%", "3%", status = MonarchAiEditHunkStatus.Rejected),
                ),
                statusIcon = { MonarchIcon(TestIcon, contentDescription = null, size = MonarchIconSize.Sm) },
            )
        },
        fixture("applied with revert") {
            MonarchAiEditProposal(part = part(MonarchAiEditProposalState.Applied), hunks = listOf(Fee), onRevert = {})
        },
        fixture("rejected") {
            MonarchAiEditProposal(part = part(MonarchAiEditProposalState.Rejected), hunks = listOf(Closing))
        },
        fixture("reverted, no change") {
            MonarchAiEditProposal(
                part = part(MonarchAiEditProposalState.Reverted),
                hunks = listOf(MonarchAiEditHunk("4", "Same text", "Same text")),
                title = "Edits to the contract",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiEditProposalTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTitleIsAHeadingAndEachChangeIsOneElement() {
        rule.setMonarchContent { MonarchAiEditProposal(part = part(), hunks = listOf(Date, Closing)) }
        rule.onNodeWithText("Suggested edits").assert(isHeading())
        rule.onNodeWithText("2 changes").assertExists()
        rule.onNodeWithContentDescription("Release date, Ships on Deleted: Friday Inserted: Monday.").assertExists()
        rule.onNodeWithContentDescription("Change 2 of 2, Inserted: A new closing paragraph.").assertExists()
    }

    @Test
    fun acceptingAChangeCallsBackAndAnnounces() {
        val accepted = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiEditProposal(
                part = part(),
                hunks = listOf(Date, Fee),
                onAcceptHunk = { accepted += it },
                onRejectHunk = {},
            )
        }
        rule.onNodeWithContentDescription("Accept, Release date")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(listOf("1"), accepted)
        rule.onNodeWithContentDescription("Change accepted. 1 left.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithText("1 of 2 changes left").assertExists()
        rule.onNodeWithContentDescription("Release date, Accepted", substring = true).assertExists()
        rule.onAllNodes(hasContentDescription("Accept, Release date")).assertCountEquals(0)
        rule.onNodeWithContentDescription("Accept, Change 2 of 2").assertExists()
    }

    @Test
    fun rejectAllDecidesEveryUndecidedChange() {
        var rejectedAll = 0
        rule.setMonarchContent {
            MonarchAiEditProposal(
                part = part(),
                hunks = listOf(Date, Fee),
                onAcceptAll = {},
                onRejectAll = { rejectedAll++ },
            )
        }
        rule.onNodeWithText("Reject all").performClick()
        assertEquals(1, rejectedAll)
        rule.onNodeWithContentDescription("All changes rejected.").assertExists()
        rule.onAllNodesWithText("Reject all").assertCountEquals(0)
        rule.onNodeWithText("0 of 2 changes left").assertExists()
    }

    @Test
    fun aDecidedProposalIsReadOnlyAndAppliedCanRevert() {
        var reverted = false
        rule.setMonarchContent {
            MonarchAiEditProposal(
                part = part(MonarchAiEditProposalState.Applied),
                hunks = listOf(Date),
                onAcceptAll = {},
                onAcceptHunk = {},
                onRevert = { reverted = true },
            )
        }
        rule.onNodeWithText("These edits were applied.").assertExists()
        rule.onNodeWithText("Applied").assertExists()
        rule.onAllNodesWithText("Accept all").assertCountEquals(0)
        rule.onAllNodes(hasContentDescription("Accept, Release date")).assertCountEquals(0)
        rule.onNodeWithText("Revert").performClick()
        assertEquals(true, reverted)
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchAiEditProposal(
                part = part(),
                hunks = listOf(Fee),
                title = "Vorschläge",
                onAcceptHunk = {},
                labels = MonarchAiEditProposalLabels(
                    accept = "Annehmen",
                    hunkName = { i, n -> "Änderung $i/$n" },
                    count = { total, _ -> "$total Änderung" },
                    deleted = "Gelöscht:",
                    inserted = "Eingefügt:",
                    announcement = { event, left -> "$event $left" },
                ),
            )
        }
        rule.onNodeWithText("Vorschläge").assertExists()
        rule.onNodeWithText("1 Änderung").assertExists()
        rule.onNodeWithContentDescription("Änderung 1/1, The fee is Gelöscht: 2 Eingefügt: 3%.").assertExists()
        rule.onNodeWithContentDescription("Annehmen, Änderung 1/1").performClick()
        rule.onNodeWithContentDescription("Accepted 0").assertExists()
    }

    @Test
    fun decisionsBelongToOneProposal() {
        val decided = mapOf("1" to MonarchAiEditHunkStatus.Accepted)
        val logic = MonarchAiEditProposalLogic
        val accepted = MonarchAiEditHunkStatus.Accepted
        val rejectedStatus = MonarchAiEditHunkStatus.Rejected
        assertEquals(accepted, logic.statusOf(Date, decided, MonarchAiEditProposalState.Proposed))
        val reverted = logic.statusOf(Date, decided, MonarchAiEditProposalState.Reverted)
        assertEquals(MonarchAiEditHunkStatus.Proposed, reverted)
        assertEquals(rejectedStatus, logic.statusOf(Date, decided, MonarchAiEditProposalState.Rejected))
        assertEquals(accepted, logic.statusOf(Fee, emptyMap(), MonarchAiEditProposalState.Applied))
        val rejected = MonarchAiEditHunk("5", "a", "b", status = rejectedStatus)
        assertEquals(rejectedStatus, logic.statusOf(rejected, emptyMap(), MonarchAiEditProposalState.Applied))
        val statuses = listOf(accepted, MonarchAiEditHunkStatus.Proposed)
        assertEquals(
            mapOf("1" to accepted, "2" to rejectedStatus),
            logic.decideAll(listOf(Date, Fee), statuses, decided, rejectedStatus),
        )
        assertEquals(1, logic.nextUndecided(statuses, 0))
        assertEquals(null, logic.nextUndecided(statuses, 1))
        assertEquals(MonarchTone.Warning, logic.stateTone(MonarchAiEditProposalState.Reverted))
        assertEquals(4, MonarchAiEditProposalEvent.entries.size)
    }

    @Test
    fun aChangeThatChangesNothingSaysSo() {
        rule.setMonarchContent {
            MonarchAiEditProposal(part = part(), hunks = listOf(MonarchAiEditHunk("1", "Same", "Same")))
        }
        rule.onNodeWithContentDescription("Change 1 of 1, Same (No change)").assertExists()
    }

    @Test
    fun theButtonsRunFromTheStartInBothDirections() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiEditProposal(part = part(), hunks = listOf(Fee), onAcceptHunk = {}, onRejectHunk = {})
        }
        rule.onNodeWithContentDescription("Accept, Change 1 of 1")
            .assertIsBefore(rule.onNodeWithContentDescription("Reject, Change 1 of 1"), LayoutDirection.Rtl)
    }

    @Test
    fun theDiffWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiEditProposal(part = part(), hunks = listOf(Date))
        }
        rule.onNodeWithText("Ships on FridayMonday.", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Suggested edits", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun removedAndAddedTextDifferByFormAndColourInEveryTheme() {
        val tokens = MonarchAiEditProposalTokens
        val removedKind = MonarchDiffViewerLineKind.Removed
        for (theme in AllThemes) {
            val colors = MonarchColors(theme.colors)
            val removed = tokens.spanColors(MonarchDiffViewerLineKind.Removed, MonarchAiEditHunkStatus.Proposed, colors)
            assertEquals(colors.aiDiffRemoveStrong, removed.text)
            assertEquals(colors.aiDiffRemove, removed.background)
            assertEquals(TextDecoration.LineThrough, removed.decoration)
            val added = tokens.spanColors(MonarchDiffViewerLineKind.Added, MonarchAiEditHunkStatus.Proposed, colors)
            assertEquals(colors.aiDiffAddStrong, added.text)
            assertEquals(TextDecoration.Underline, added.decoration)
            val acceptedRemoved = tokens.spanColors(removedKind, MonarchAiEditHunkStatus.Accepted, colors)
            assertEquals(colors.textSubtle, acceptedRemoved.text)
            assertEquals(Color.Transparent, acceptedRemoved.background)
            val rejectedRemoved = tokens.spanColors(removedKind, MonarchAiEditHunkStatus.Rejected, colors)
            assertEquals(colors.textDefault, rejectedRemoved.text)
            assertEquals(null, rejectedRemoved.decoration)
            val rejectedAdded =
                tokens.spanColors(MonarchDiffViewerLineKind.Added, MonarchAiEditHunkStatus.Rejected, colors)
            assertEquals(TextDecoration.Underline, rejectedAdded.decoration)
            assertEquals(colors.successStrong, tokens.statusColor(MonarchAiEditHunkStatus.Accepted, colors))
            assertEquals(colors.aiBorder, tokens.accent(colors))
        }
    }
}
