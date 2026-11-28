@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceStatus
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSession
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSessionActivity
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSessionAnswering
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSessionKind
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSessionLabels
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSessionLogic
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSessionMove
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSessionParticipant
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSessionPresence
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSessionRequest
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSessionRole
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSessionStatusTexts
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSessionTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
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
import kotlin.test.assertNull

private fun person(
    id: String,
    name: String,
    role: MonarchAiSharedSessionRole = MonarchAiSharedSessionRole.Editor,
    activity: MonarchAiSharedSessionActivity? = null,
    you: Boolean = false,
) = MonarchAiSharedSessionParticipant(id = id, name = name, role = role, activity = activity, you = you)

private val Maya = person("maya", "Maya", MonarchAiSharedSessionRole.Owner, you = true)
private val Sam = person("sam", "Sam Lee", activity = MonarchAiSharedSessionActivity.Typing)
private val Agent =
    MonarchAiSharedSessionParticipant(id = "ra", name = "Research agent", kind = MonarchAiSharedSessionKind.Agent)
private val Everyone = listOf(Maya, Sam, Agent)
private val Queue = listOf(
    MonarchAiSharedSessionRequest("q1", "sam", "Summarize the Q3 notes"),
    MonarchAiSharedSessionRequest("q2", "maya", "Draft the reply"),
)

internal object MonarchAiSharedSessionFixtures : ComponentFixtures {
    override val id = "ai-shared-session"
    override val fixtures = listOf(
        fixture("answering with activity") {
            MonarchAiSharedSession(
                participants = Everyone,
                answering = MonarchAiSharedSessionAnswering("ra", "sam", "Q3 notes"),
                onInvite = {},
            )
        },
        fixture("owner with an open queue, own request") {
            MonarchAiSharedSession(
                participants = listOf(Maya),
                queue = Queue.drop(1),
                queueExpanded = true,
                onWithdraw = {},
                onReorder = { _, _ -> },
            )
        },
        fixture("owner with an open queue, another's request") {
            MonarchAiSharedSession(
                participants = listOf(Maya),
                queue = Queue.take(1),
                queueExpanded = true,
                onRemove = {},
            )
        },
        fixture("viewer") {
            MonarchAiSharedSession(
                participants = listOf(person("v", "Viewer one", MonarchAiSharedSessionRole.Viewer, you = true), Agent),
                onRequestAccess = {},
            )
        },
        fixture("viewer, access requested, overflow") {
            MonarchAiSharedSession(
                participants = listOf(person("v", "Viewer one", MonarchAiSharedSessionRole.Viewer, you = true)) +
                    (1..5).map { person("p$it", "Person $it") },
                maxVisible = 2,
                accessRequested = true,
            )
        },
        fixture("quiet and empty") { MonarchAiSharedSession(participants = emptyList()) },
    )
}

private val Texts = MonarchAiSharedSessionStatusTexts(
    answering = { agent, requester, label -> "$agent answers $requester${label?.let { ": $it" }.orEmpty()}" },
    answeringYou = { agent, _ -> "$agent answers you" },
    activityLine = { activity, names, count -> "$names $count ${activity.name}" },
    twoNames = { a, b -> "$a and $b" },
    several = { "$it people" },
    queueCount = { "$it waiting" },
    assistant = "Assistant",
    someone = "Someone",
)

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiSharedSessionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theStackIsOneButtonThatOpensTheList() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchAiSharedSession(participants = Everyone) }
        rule.mainClock.advanceTimeByFrame()
        val stack = "2 people and 1 agent here: Maya (you), Owner, Active; Sam Lee, Editor, Active, Typing; " +
            "Research agent, AI agent, Active"
        rule.onNodeWithContentDescription(stack)
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .assert(
                SemanticsMatcher("labelled") { it.config[SemanticsActions.OnClick].label == "Show all 3 participants" },
            )
            .performClick()
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithText("In this session").assertExists()
        rule.onNodeWithText("People").assertExists()
        rule.onNodeWithText("Agents").assertExists()
    }

    @Test
    fun theStatusSaysWhoIsAnsweredAndWhatOthersDo() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiSharedSession(
                participants = Everyone,
                answering = MonarchAiSharedSessionAnswering("ra", "maya", "Q3 notes"),
                queue = Queue,
            )
        }
        rule.mainClock.advanceTimeByFrame()
        val line = "Research agent is answering your request: Q3 notes · Sam Lee is typing… · 2 requests waiting"
        rule.onNodeWithText(line).assertExists()
    }

    @Test
    fun aQuietSessionSaysSo() {
        rule.setMonarchContent { MonarchAiSharedSession(participants = listOf(Maya)) }
        rule.onNodeWithText("No requests in progress").assertExists()
    }

    @Test
    fun statusChangesAreAnnouncedButNotTypingOrTheFirstStatus() {
        var answering by mutableStateOf<MonarchAiSharedSessionAnswering?>(null)
        var queue by mutableStateOf(emptyList<MonarchAiSharedSessionRequest>())
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchAiSharedSession(participants = Everyone, answering = answering, queue = queue) }
        rule.mainClock.advanceTimeBy(100)
        rule.onAllNodes(hasContentDescription("Sam Lee is typing…")).assertCountEquals(0)
        answering = MonarchAiSharedSessionAnswering("ra", "sam")
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithContentDescription("Research agent is answering Sam Lee’s request")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        queue = Queue.take(1)
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithContentDescription("Research agent is answering Sam Lee’s request. 1 request waiting")
            .assertExists()
    }

    @Test
    fun theOwnerMovesRemovesAndWithdrawsRequests() {
        val moves = mutableListOf<Pair<List<String>, MonarchAiSharedSessionMove>>()
        val removed = mutableListOf<String>()
        val withdrawn = mutableListOf<String>()
        var queue by mutableStateOf(Queue)
        rule.setMonarchContent {
            MonarchAiSharedSession(
                participants = Everyone,
                queue = queue,
                onReorder = { ids, move ->
                    moves += ids to move
                    queue = ids.map { id -> Queue.first { it.id == id } }
                },
                onRemove = { removed += it },
                onWithdraw = { withdrawn += it },
            )
        }
        rule.onNodeWithText("Queue (2)").performClick()
        rule.onNodeWithContentDescription("Move up: request 1, from Sam Lee").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Remove: request 1, from Sam Lee").performClick()
        rule.onNodeWithContentDescription("Withdraw: request 2, from Maya").performClick()
        assertEquals(listOf("q1"), removed)
        assertEquals(listOf("q2"), withdrawn)
        rule.onNodeWithContentDescription("Move down: request 1, from Sam Lee").performClick()
        assertEquals(listOf("q2", "q1") to MonarchAiSharedSessionMove("q1", 0, 1), moves.single())
        rule.onNodeWithContentDescription("Sam Lee’s request moved to position 2 of 2").assertExists()
    }

    @Test
    fun anEditorWithoutCallbacksSeesNoQueueButtons() {
        rule.setMonarchContent {
            MonarchAiSharedSession(
                participants = listOf(person("e", "Ed", you = true), Sam),
                queue = Queue,
                queueExpanded = true,
                onRemove = {},
                onReorder = { _, _ -> },
            )
        }
        rule.onNodeWithText("Summarize the Q3 notes").assertExists()
        rule.onAllNodesWithText("Remove").assertCountEquals(0)
        rule.onAllNodesWithText("Move up").assertCountEquals(0)
    }

    @Test
    fun theQueueButtonSaysWhetherItIsExpanded() {
        val expandedCalls = mutableListOf<Boolean>()
        rule.setMonarchContent {
            MonarchAiSharedSession(
                participants = Everyone,
                queue = Queue,
                queueExpanded = false,
                onQueueExpandedChange = { expandedCalls += it },
            )
        }
        rule.onNodeWithText("Queue (2)").assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand)).performClick()
        assertEquals(listOf(true), expandedCalls)
        rule.onAllNodesWithText("Summarize the Q3 notes").assertCountEquals(0)
    }

    @Test
    fun aViewerCanAskForAccess() {
        var asked = false
        var requested by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchAiSharedSession(
                participants = listOf(person("v", "Vic", MonarchAiSharedSessionRole.Viewer, you = true)),
                accessRequested = requested,
                onRequestAccess = { asked = true },
            )
        }
        rule.onNodeWithText("You can watch but not send").assertExists()
        rule.onNodeWithText("Request access").performClick()
        assertEquals(true, asked)
        requested = true
        rule.onNodeWithText("Access requested").assertExists()
    }

    @Test
    fun followTogglesAreNamedAndSelected() {
        var following by mutableStateOf(setOf<String>())
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiSharedSession(
                participants = Everyone,
                following = following,
                onFollowingChange = { following = it },
            )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("2 people", substring = true).performClick()
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithContentDescription("Follow Sam Lee").performClick()
        rule.mainClock.advanceTimeBy(100)
        assertEquals(setOf("sam"), following)
        rule.onNodeWithContentDescription("Follow Sam Lee").assertIsSelected()
        rule.onAllNodes(hasContentDescription("Follow Maya")).assertCountEquals(0)
    }

    @Test
    fun theLabelsReplaceTheStrings() {
        rule.setMonarchContent {
            MonarchAiSharedSession(
                participants = listOf(Maya),
                onInvite = {},
                labels = MonarchAiSharedSessionLabels(quiet = "Ruhig", invite = "Einladen"),
            )
        }
        rule.onNodeWithText("Ruhig").assertExists()
        rule.onNodeWithText("Einladen").assertExists()
    }

    @Test
    fun theStatusLogicNamesOneTwoOrSeveralAndLeavesYouOut() {
        val logic = MonarchAiSharedSessionLogic
        val typing = MonarchAiSharedSessionActivity.Typing
        val people = listOf(
            person("a", "Ana", activity = typing, you = true),
            person("b", "Ben", activity = typing),
            person("c", "Cy", activity = typing),
        )
        assertEquals(listOf("Ben and Cy 2 Typing"), logic.status(people, null, 0, Texts).map { it.text })
        val three = people + person("d", "Di", activity = typing)
        assertEquals(listOf("3 people 3 Typing"), logic.status(three, null, 0, Texts).map { it.text })
        val clauses = logic.status(
            listOf(person("r", "Rae", activity = MonarchAiSharedSessionActivity.Reviewing)),
            MonarchAiSharedSessionAnswering("x", "y", "Plan"),
            2,
            Texts,
        )
        assertEquals("Assistant answers Someone: Plan. Rae 1 Reviewing. 2 waiting", logic.announcement(clauses, ". "))
        assertEquals(listOf("b", "a"), logic.reorder(listOf("a", "b"), 0, 1))
        assertNull(logic.reorder(listOf("a", "b"), 1, 2))
        assertEquals(listOf("maya", "sam"), logic.groups(listOf(Sam, Agent, Maya)).people.map { it.id })
        assertEquals(MonarchPresenceStatus.Offline, logic.presenceStatus(MonarchAiSharedSessionPresence.Away))
    }

    @Test
    fun theStatusWrapsAtTheLargestFontScale() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiSharedSession(participants = listOf(Maya, Sam))
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Sam Lee is typing…", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theStackComesBeforeTheStatusRightToLeft() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiSharedSession(participants = listOf(Maya, Sam))
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("2 people here", substring = true)
            .assertIsBefore(rule.onNodeWithText("Sam Lee is typing…"), LayoutDirection.Rtl)
    }

    @Test
    fun coloursComeFromRolesInEveryTheme() {
        val tokens = MonarchAiSharedSessionTokens
        for (theme in AllThemes) {
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.aiSubtle, tokens.requestColors(own = true, colors = colors).container)
            assertEquals(colors.bgSubtle, tokens.requestColors(own = false, colors = colors).container)
            assertEquals(colors.bgMuted, tokens.noticeColors(colors).container)
            assertEquals(colors.textSubtle, tokens.quietColor(colors))
        }
    }
}
