@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiactionlog.MonarchAiActionLog
import dev.monarchui.components.intelligence.aiactionlog.MonarchAiActionLogColors
import dev.monarchui.components.intelligence.aiactionlog.MonarchAiActionLogEntry
import dev.monarchui.components.intelligence.aiactionlog.MonarchAiActionLogLabels
import dev.monarchui.components.intelligence.aiactionlog.MonarchAiActionLogTokens
import dev.monarchui.components.intelligence.aiactionlog.MonarchAiActionLogUndo
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val Now = 1_793_750_400_000L
private const val Minute = 60_000L
private const val Hour = 3_600_000L
private const val Day = 24 * Hour
private val Agent = MonarchAiAgentRef(name = "Research agent", id = "research")
private val Summary = MonarchAiActionLogEntry(
    id = "1",
    date = Now - 3 * Hour,
    agent = Agent,
    action = "added a summary to",
    target = "Quarterly report",
    url = "https://example.com/report",
    undoable = true,
)
private val Closed =
    MonarchAiActionLogEntry(id = "2", date = Now - Day, agent = Agent, action = "closed", target = "Ticket 4821")

internal object MonarchAiActionLogFixtures : ComponentFixtures {
    override val id = "ai-action-log"
    override val fixtures = listOf(
        fixture("an undoable entry with a link") {
            MonarchAiActionLog(entries = listOf(Summary), now = Now, onUndo = {})
        },
        fixture("plain and undone entries") {
            MonarchAiActionLog(entries = listOf(Closed, Summary.copy(id = "3", undone = true)), now = Now, onUndo = {})
        },
        fixture("paged") {
            MonarchAiActionLog(entries = listOf(Summary, Closed), now = Now, pageSize = 1)
        },
        fixture("empty") { MonarchAiActionLog(entries = emptyList(), now = Now) },
        fixture("long action") {
            MonarchAiActionLog(
                entries = listOf(
                    Closed.copy(action = "moved every overdue invoice of the northern region to the review queue of"),
                ),
                now = Now,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiActionLogTest {
    @get:Rule
    val rule = createComposeRule()

    private val live = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    private fun announced(): String? = rule.onNode(live, useUnmergedTree = true).fetchSemanticsNode()
        .config.getOrNull(SemanticsProperties.ContentDescription)?.single()

    @Test
    fun eachEntryIsOneElementThatMarksTheActorAsAnAiAgent() {
        rule.setMonarchContent { MonarchAiActionLog(entries = listOf(Summary), now = Now) }
        val entry = rule.onNodeWithContentDescription(
            "Research agent (AI agent) added a summary to Quarterly report",
        )
        assertEquals("3 hours ago", entry.stateDescription())
        rule.onNodeWithContentDescription("AI activity").assertExists()
    }

    @Test
    fun undoRunsShowsUndoneAndAnnounces() {
        val undone = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiActionLog(entries = listOf(Summary), now = Now, onUndo = { undone += it })
        }
        assertNull(announced())
        val name = "Undo: Research agent added a summary to Quarterly report"
        rule.onNode(hasContentDescription(name) and hasClickAction())
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitForIdle()
        assertEquals(listOf("1"), undone)
        assertEquals("Undone: Research agent added a summary to Quarterly report", announced())
        rule.onNode(hasContentDescription("Undo:", substring = true) and hasClickAction()).assertDoesNotExist()
        val entry = rule.onNodeWithContentDescription("Research agent (AI agent)", substring = true)
        assertEquals("3 hours ago · Undone", entry.stateDescription())
    }

    @Test
    fun aFailedUndoIsShownAnnouncedAndUndoStays() {
        rule.setMonarchContent {
            MonarchAiActionLog(entries = listOf(Summary), now = Now, onUndo = { error("offline") })
        }
        rule.onNode(hasContentDescription("Undo:", substring = true) and hasClickAction()).performClick()
        rule.waitForIdle()
        val failure = "Could not undo: Research agent added a summary to Quarterly report"
        assertEquals(failure, announced())
        val entry = rule.onNodeWithContentDescription("Research agent (AI agent)", substring = true)
        assertEquals("3 hours ago, $failure", entry.stateDescription())
        rule.onNode(hasContentDescription("Undo:", substring = true) and hasClickAction()).assertExists()
    }

    @Test
    fun aSecondPressWhileUndoingDoesNothing() {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        rule.setMonarchContent {
            MonarchAiActionLog(
                entries = listOf(Summary),
                now = Now,
                onUndo = {
                    calls++
                    gate.await()
                },
            )
        }
        rule.mainClock.autoAdvance = false
        val undo = rule.onNode(hasContentDescription("Undo:", substring = true) and hasClickAction())
        undo.performClick()
        rule.mainClock.advanceTimeByFrame()
        undo.performClick()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(1, calls)
        gate.complete(Unit)
    }

    @Test
    fun entriesWithoutUndoableOrHandlerHaveNoUndo() {
        rule.setMonarchContent { MonarchAiActionLog(entries = listOf(Summary, Closed), now = Now) }
        rule.onNode(hasText("Undo")).assertDoesNotExist()
    }

    @Test
    fun aSafeTargetIsANamedActionThatOpens() {
        val opened = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiActionLog(entries = listOf(Summary), now = Now, onOpenTarget = { opened += it.id })
        }
        val entry = rule.onNodeWithContentDescription("Research agent (AI agent)", substring = true)
        val actions = entry.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        val open = actions.single { it.label == "Open Quarterly report" }
        rule.runOnIdle { open.action() }
        assertEquals(listOf("1"), opened)
    }

    @Test
    fun anUnsafeUrlIsNotALink() {
        rule.setMonarchContent {
            MonarchAiActionLog(entries = listOf(Summary.copy(url = "javascript:alert(1)")), now = Now)
        }
        val entry = rule.onNodeWithContentDescription("Research agent (AI agent)", substring = true)
        assertNull(entry.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions))
    }

    @Test
    fun showMoreAddsAPage() {
        rule.setMonarchContent { MonarchAiActionLog(entries = listOf(Summary, Closed), now = Now, pageSize = 1) }
        rule.onNodeWithContentDescription("Research agent (AI agent) closed Ticket 4821").assertDoesNotExist()
        rule.onNodeWithText("Show 1 more action").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("Research agent (AI agent) closed Ticket 4821").assertExists()
        rule.onNodeWithText("Show 1 more action").assertDoesNotExist()
    }

    @Test
    fun anEmptyLogSaysSo() {
        rule.setMonarchContent { MonarchAiActionLog(entries = emptyList(), now = Now) }
        rule.onNodeWithText("No AI activity yet.").assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiActionLog(
                entries = listOf(Summary, Closed),
                now = Now,
                pageSize = 1,
                onUndo = {},
                labels = MonarchAiActionLogLabels(
                    label = "Activité IA",
                    actorKind = "agent IA",
                    undo = "Annuler",
                    undoLabel = { "Annuler : $it" },
                    showMore = { "Afficher $it de plus" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Activité IA").assertExists()
        rule.onNodeWithContentDescription("Research agent (agent IA)", substring = true).assertExists()
        rule.onNode(hasContentDescription("Annuler : Research agent", substring = true) and hasClickAction())
            .assertExists()
        rule.onNodeWithText("Afficher 1 de plus").assertExists()
    }

    @Test
    fun undoIsAtTheEndInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiActionLog(entries = listOf(Summary), now = Now, onUndo = {})
        }
        val undo = rule.onNode(hasContentDescription("Undo:", substring = true) and hasClickAction())
        rule.onNodeWithContentDescription("Research agent (AI agent)", substring = true)
            .assertIsBefore(undo, LayoutDirection.Rtl)
    }

    @Test
    fun anEntryGrowsAtTheLargestFontSizeInsteadOfCuttingText() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchAiActionLog(entries = listOf(Closed.copy(action = "moved every overdue invoice to")), now = Now)
        }
        val entry = rule.onNodeWithContentDescription("Research agent (AI agent)", substring = true)
        val regular = entry.fetchSemanticsNode().size.height
        environment.fontScale = LargestFontScale
        rule.waitForIdle()
        assertTrue(entry.fetchSemanticsNode().size.height > regular)
    }

    @Test
    fun theLogicDescribesDecidesAndFormats() {
        val t = MonarchAiActionLogTokens
        assertEquals("Research agent added a summary to Quarterly report", t.description(Summary))
        assertEquals("Research agent (AI agent) closed", t.spoken(Closed.copy(target = " "), "AI agent"))
        assertEquals("https://example.com", t.safeUrl(" https://example.com"))
        assertNull(t.safeUrl("javascript:alert(1)"))
        assertNull(t.safeUrl(null))
        assertEquals(1, t.step(0))
        assertTrue(t.canUndo(Summary, hasHandler = true, state = null))
        assertFalse(t.canUndo(Summary, hasHandler = false, state = null))
        assertFalse(t.canUndo(Summary, hasHandler = true, state = MonarchAiActionLogUndo.Done))
        assertFalse(t.canUndo(Closed, hasHandler = true, state = null))
        val us = Locale.US
        assertEquals("now", t.relativeTime(Now, Now, us))
        // The shared web rule: a future time reads as such, as on the web (MONARCH-COMPAT).
        assertEquals("in 1 hour", t.relativeTime(Now + Hour, Now, us))
        assertEquals("5 minutes ago", t.relativeTime(Now - 5 * Minute, Now, us))
        assertEquals("3 hours ago", t.relativeTime(Now - 3 * Hour, Now, us))
        assertEquals("2 days ago", t.relativeTime(Now - 2 * Day, Now, us))
        assertEquals("2 weeks ago", t.relativeTime(Now - 15 * Day, Now, us))
        assertEquals("3 months ago", t.relativeTime(Now - 95 * Day, Now, us))
        assertEquals("last year", t.relativeTime(Now - 400 * Day, Now, us))
        assertEquals("2 years ago", t.relativeTime(Now - 800 * Day, Now, us))
    }

    @Test
    fun theColoursComeFromTheThemeRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchAiActionLogColors
            assertEquals(c.textDefault, t.nameColor(c), theme.id)
            assertEquals(c.textDefault, t.actionColor(undone = false, c), theme.id)
            assertEquals(c.textMuted, t.actionColor(undone = true, c), theme.id)
            assertEquals(c.textMuted, t.metaColor(c), theme.id)
            assertEquals(c.textLink, t.linkColor(c), theme.id)
            assertEquals(c.dangerStrong, t.failureColor(c), theme.id)
        }
    }
}
