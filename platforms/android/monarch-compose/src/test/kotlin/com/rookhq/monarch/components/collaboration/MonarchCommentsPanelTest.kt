@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.collaboration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.collaboration.commentspanel.MonarchCommentsPanel
import com.rookhq.monarch.components.collaboration.commentspanel.MonarchCommentsPanelFilter
import com.rookhq.monarch.components.collaboration.commentspanel.MonarchCommentsPanelLabels
import com.rookhq.monarch.components.collaboration.commentspanel.MonarchCommentsPanelThread
import com.rookhq.monarch.components.collaboration.commentspanel.MonarchCommentsPanelTokens
import com.rookhq.monarch.components.collaboration.commentthread.MonarchCommentThreadComment
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
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
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 14 November 2026, 14:00 UTC. */
private const val Now = 1_794_664_800_000L
private const val Minute = 60_000L

private fun comment(id: String, author: String, body: String, minutesAgo: Long, canDelete: Boolean = false) =
    MonarchCommentThreadComment(id, author, body, Now - minutesAgo * Minute, canDelete = canDelete)

private val Pricing = MonarchCommentsPanelThread(
    id = "t1",
    comments = listOf(
        comment("c1", "Priya Raman", "Should this say billed every quarter?", 50),
        comment("c2", "Tom Becker", "Yes. Monthly billing was retired.", 20, canDelete = true),
    ),
    quote = "Billed monthly, cancel any time.",
)

private val General = MonarchCommentsPanelThread(
    id = "t2",
    comments = listOf(comment("c3", "Lee Park", "Can we add a summary at the top?", 180)),
)

private val Closed = MonarchCommentsPanelThread(
    id = "t3",
    comments = listOf(comment("c4", "Ana Ruiz", "Typo in the heading.", 3 * 24 * 60)),
    quote = "Our pricinng",
    resolved = true,
)

private val Threads = listOf(Pricing, General, Closed)

@Composable
private fun Panel(
    threads: List<MonarchCommentsPanelThread>? = Threads,
    filter: MonarchCommentsPanelFilter = MonarchCommentsPanelFilter.Open,
    selected: String? = null,
    errorMessage: String? = null,
    labels: MonarchCommentsPanelLabels = MonarchCommentsPanelLabels(),
    onFilterChange: (MonarchCommentsPanelFilter) -> Unit = {},
    onSelectedThreadChange: (String?) -> Unit = {},
    onRetry: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    onReply: (suspend (String, String) -> Unit)? = null,
    onResolvedChange: (suspend (String, Boolean) -> Unit)? = null,
) {
    MonarchCommentsPanel(
        threads = threads,
        filter = filter,
        onFilterChange = onFilterChange,
        selectedThreadId = selected,
        onSelectedThreadChange = onSelectedThreadChange,
        errorMessage = errorMessage,
        now = Now,
        locale = Locale.US,
        labels = labels,
        onRetry = onRetry,
        onClose = onClose,
        onReply = onReply,
        onResolvedChange = onResolvedChange,
    )
}

internal object MonarchCommentsPanelFixtures : ComponentFixtures {
    override val id = "comments-panel"
    override val fixtures = listOf(
        fixture("open threads, with Close") { Panel(onClose = {}) },
        fixture("resolved threads") { Panel(filter = MonarchCommentsPanelFilter.Resolved) },
        fixture("one thread open, reply and resolve") {
            Panel(selected = "t1", onReply = { _, _ -> }, onResolvedChange = { _, _ -> })
        },
        fixture("a resolved thread, reopen") {
            Panel(
                filter = MonarchCommentsPanelFilter.Resolved,
                selected = "t3",
                onReply = { _, _ -> },
                onResolvedChange = { _, _ -> },
            )
        },
        fixture("loading") { Panel(threads = null) },
        fixture("failed, with Try again") { Panel(errorMessage = "Couldn’t load the comments.", onRetry = {}) },
        fixture("failed, no retry") { Panel(errorMessage = "Couldn’t load the comments.") },
        fixture("no open threads") { Panel(threads = listOf(Closed)) },
        fixture("no resolved threads") {
            Panel(threads = listOf(Pricing), filter = MonarchCommentsPanelFilter.Resolved)
        },
        fixture("long text") {
            Panel(
                threads = listOf(
                    MonarchCommentsPanelThread(
                        id = "t9",
                        comments = listOf(
                            comment(
                                "c9",
                                "Maria Fernanda de los Santos Oliveira",
                                "We reviewed the second draft and have questions about the terms.",
                                5,
                            ),
                        ),
                        quote = "The supplier may change the price with thirty days of written notice to the customer.",
                    ),
                ),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchCommentsPanelTest {
    @get:Rule
    val rule = createComposeRule()

    private fun announces(text: String) = SemanticsMatcher("announces $text") { node ->
        node.config.getOrNull(SemanticsProperties.LiveRegion) == LiveRegionMode.Polite &&
            node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().contains(text)
    }

    @Test
    fun theListShowsTheOpenThreadsWithTheirStarterQuoteCountAndTime() {
        rule.setMonarchContent { Panel() }
        rule.onNodeWithText("Comments", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Open").assertIsSelected()
        rule.onNodeWithText("Priya Raman", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("“Billed monthly, cancel any time.”", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("2 comments · 20 minutes ago", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("1 comment · 3 hours ago", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithText("Ana Ruiz", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun theTitleIsAHeading() {
        rule.setMonarchContent { Panel() }
        rule.onAllNodes(isHeading()).fetchSemanticsNodes().let { nodes ->
            assertEquals(1, nodes.size)
            assertEquals("Comments", nodes.single().config[SemanticsProperties.Text].joinToString { it.text })
        }
    }

    @Test
    fun switchingTheFilterCallsBackAndClosesTheThread() {
        val filters = mutableListOf<MonarchCommentsPanelFilter>()
        val selections = mutableListOf<String?>()
        rule.setMonarchContent {
            Panel(
                selected = "t1",
                onFilterChange = { filters += it },
                onSelectedThreadChange = { selections += it },
            )
        }
        rule.onNodeWithText("Resolved").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf(MonarchCommentsPanelFilter.Resolved), filters)
        assertEquals(listOf<String?>(null), selections)
    }

    @Test
    fun openingARowSelectsItsThreadAndBackReturns() {
        lateinit var inputMode: InputModeManager
        rule.setMonarchContent {
            inputMode = LocalInputModeManager.current
            var selected by remember { mutableStateOf<String?>(null) }
            Panel(selected = selected, onSelectedThreadChange = { selected = it })
        }
        // Buttons take focus with a keyboard, as on a tablet with a keyboard or with switch access.
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithText("Priya Raman")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        rule.onNodeWithText("All comments").assertTouchTargetAtLeast48dp(rule.density).assertIsFocused()
        rule.onNodeWithContentDescription("Thread started by Priya Raman").assertExists()
        rule.onNodeWithText("All comments").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        rule.onAllNodesWithText("All comments").assertCountEquals(0)
        rule.onNodeWithText("Priya Raman").assertIsFocused()
    }

    @Test
    fun aSelectedThreadOutsideTheFilterShowsTheList() {
        rule.setMonarchContent { Panel(selected = "t3") }
        rule.onAllNodesWithText("All comments").assertCountEquals(0)
        rule.onNodeWithText("Priya Raman").assertExists()
    }

    @Test
    fun repliesAndResolvingCarryTheThreadIdAndResolvingReturnsToTheList() {
        val replies = mutableListOf<Pair<String, String>>()
        val resolved = mutableListOf<Pair<String, Boolean>>()
        var selected: String? = "t1"
        lateinit var inputMode: InputModeManager
        rule.setMonarchContent {
            inputMode = LocalInputModeManager.current
            var shown by remember { mutableStateOf(selected) }
            Panel(
                selected = shown,
                onSelectedThreadChange = {
                    shown = it
                    selected = it
                },
                onReply = { id, body -> replies += id to body },
                onResolvedChange = { id, value -> resolved += id to value },
            )
        }
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithText("Resolve").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        assertEquals(listOf("t1" to true), resolved)
        assertNull(selected)
        rule.onNode(announces("Thread resolved")).assertExists()
        rule.onNodeWithText("Priya Raman").assertIsFocused()
        assertEquals(emptyList(), replies)
    }

    @Test
    fun aReplyIsSentWithTheThreadId() {
        val replies = mutableListOf<Pair<String, String>>()
        rule.setMonarchContent { Panel(selected = "t2", onReply = { id, body -> replies += id to body }) }
        rule.onNodeWithContentDescription("Reply").performTextInput("Thanks")
        rule.onNodeWithText("Send reply").performClick()
        rule.waitForIdle()
        assertEquals(listOf("t2" to "Thanks"), replies)
    }

    @Test
    fun aThreadThatTakesNoRepliesHasNoReplyField() {
        val readOnly = MonarchCommentsPanelThread("t5", General.comments, canReply = false, canResolve = false)
        rule.setMonarchContent {
            Panel(threads = listOf(readOnly), selected = "t5", onReply = { _, _ -> }, onResolvedChange = { _, _ -> })
        }
        rule.onAllNodesWithText("Send reply").assertCountEquals(0)
        rule.onAllNodesWithText("Resolve").assertCountEquals(0)
    }

    @Test
    fun loadingIsReadOnce() {
        rule.setMonarchContent { Panel(threads = null) }
        rule.onNodeWithContentDescription("Loading comments…", substring = true).assertExists()
    }

    @Test
    fun anErrorOffersTryAgain() {
        var retried = 0
        rule.setMonarchContent { Panel(errorMessage = "Couldn’t load the comments.", onRetry = { retried++ }) }
        rule.onNodeWithText("Couldn’t load the comments.", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Try again").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, retried)
    }

    @Test
    fun emptyFiltersSaySo() {
        rule.setMonarchContent { Panel(threads = listOf(Closed)) }
        rule.onNodeWithText("No open comments", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Select text to start a discussion.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun closeCallsBackAndComesAfterTheTitleInBothDirections() {
        var closed = 0
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { Panel(onClose = { closed++ }) }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Comments", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithContentDescription("Close"), direction)
        }
        rule.onNodeWithContentDescription("Close").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, closed)
    }

    @Test
    fun rowsWrapAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) { Panel(threads = listOf(Pricing)) }
        rule.onNodeWithText("Priya Raman", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("2 comments · 20 minutes ago", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Priya Raman").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            Panel(
                threads = listOf(Pricing),
                labels = MonarchCommentsPanelLabels(
                    title = "Comentarios",
                    open = "Abiertos",
                    resolved = "Resueltos",
                    commentCount = { "$it comentarios" },
                    quote = { "« $it »" },
                ),
            )
        }
        rule.onNodeWithText("Comentarios", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Abiertos").assertExists()
        rule.onNodeWithText("Resueltos").assertExists()
        rule.onNodeWithText("« Billed monthly, cancel any time. »", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("2 comentarios · 20 minutes ago", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theListLogic() {
        val t = MonarchCommentsPanelTokens
        assertEquals(listOf(Pricing, General), t.visible(Threads, MonarchCommentsPanelFilter.Open))
        assertEquals(listOf(Closed), t.visible(Threads, MonarchCommentsPanelFilter.Resolved))
        assertEquals(Pricing, t.selected("t1", listOf(Pricing, General)))
        assertNull(t.selected("t3", listOf(Pricing, General)))
        assertNull(t.selected(null, Threads))
        assertEquals(Now - 20 * Minute, t.lastActivity(Pricing))
        assertEquals(5L, t.lastActivity(MonarchCommentsPanelThread("x", Pricing.comments, updatedAt = 5L)))
        assertNull(t.lastActivity(MonarchCommentsPanelThread("x", emptyList())))
        assertEquals("t2", t.focusRow("t2", listOf(Pricing, General)))
        assertEquals("t1", t.focusRow("t3", listOf(Pricing, General)))
        assertNull(t.focusRow(t.BACK_TARGET, listOf(Pricing)))
        assertNull(t.focusRow(null, listOf(Pricing)))
        assertEquals("1 comment", t.meta("1 comment", null))
        assertEquals("now", t.relativeTime(Now, Now, Locale.US))
        // The shared web rule: a future time reads as such, as on the web (MONARCH-COMPAT).
        assertEquals("in 1 minute", t.relativeTime(Now + Minute, Now, Locale.US))
        assertEquals("2 days ago", t.relativeTime(Now - 2 * 24 * 60 * Minute, Now, Locale.US))
    }

    @Test
    fun theColoursComeFromTheirRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchCommentsPanelTokens
            assertEquals(c.textMuted, t.mutedColor(c), theme.id)
            assertEquals(c.textDefault, t.titleColor(c), theme.id)
            assertEquals(c.dangerStrong, t.errorColor(c), theme.id)
        }
    }
}
