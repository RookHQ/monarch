@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.collaboration

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.collaboration.commentthread.MonarchCommentThread
import dev.monarchui.components.collaboration.commentthread.MonarchCommentThreadComment
import dev.monarchui.components.collaboration.commentthread.MonarchCommentThreadLabels
import dev.monarchui.components.collaboration.commentthread.MonarchCommentThreadTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
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
import kotlin.test.assertTrue

/** 14 November 2026, 14:00 UTC. */
private const val Now = 1_794_664_800_000L
private const val Minute = 60_000L

private fun comment(id: String, author: String, body: String, minutesAgo: Long, canDelete: Boolean = false) =
    MonarchCommentThreadComment(id, author, body, Now - minutesAgo * Minute, canDelete = canDelete)

private val Start = listOf(
    comment("c1", "Priya Raman", "Should this say billed every quarter instead?", 50),
    comment("c2", "Tom Becker", "Yes. Monthly billing was retired last year.", 20, canDelete = true),
)

private val LongThread = listOf(comment("c1", "Lee Park", "Should this say billed every quarter?", 90)) +
    listOf("Agreed.", "Updated the copy.", "Checked the pricing table.", "Legal is fine.", "Shipping tomorrow.")
        .mapIndexed { i, body -> comment("r$i", if (i % 2 == 0) "Tom Becker" else "Lee Park", body, 60L - i * 5) }

internal object MonarchCommentThreadFixtures : ComponentFixtures {
    override val id = "comment-thread"
    override val fixtures = listOf(
        fixture("open, quote, resolve and reply") {
            MonarchCommentThread(
                comments = Start,
                quote = "Billed monthly, cancel any time.",
                now = Now,
                onResolvedChange = {},
                onReply = {},
                onDeleteComment = {},
            )
        },
        fixture("folded replies") {
            MonarchCommentThread(comments = LongThread, visibleReplies = 1, now = Now)
        },
        fixture("resolved, with the hint") {
            MonarchCommentThread(
                comments = Start.take(1),
                resolved = true,
                now = Now,
                onResolvedChange = {},
                onReply = {},
            )
        },
        fixture("read only, long text") {
            MonarchCommentThread(
                comments = listOf(
                    comment(
                        "c1",
                        "Maria Fernanda de los Santos Oliveira",
                        "We reviewed the second draft of the contract and have a few questions about the terms.",
                        5,
                    ),
                ),
                quote = "The supplier may change the price with thirty days of written notice to the customer.",
                now = Now,
            )
        },
        fixture("no comments yet") {
            MonarchCommentThread(comments = emptyList(), now = Now, onReply = {})
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchCommentThreadTest {
    @get:Rule
    val rule = createComposeRule()

    private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    private fun readsText(text: String) = SemanticsMatcher("reads $text") { node ->
        node.config.getOrNull(SemanticsProperties.Text).orEmpty().any { it.text == text }
    }

    private val deleteAction = SemanticsMatcher("offers Delete comment") { node ->
        node.config.getOrNull(SemanticsActions.CustomActions).orEmpty().any { it.label == "Delete comment" }
    }

    @Test
    fun theThreadIsNamedAfterItsFirstAuthorAndReadsTheQuote() {
        rule.setMonarchContent {
            MonarchCommentThread(comments = Start, quote = "Billed monthly.", now = Now)
        }
        rule.onNodeWithContentDescription("Thread started by Priya Raman").assertExists()
        rule.onNodeWithContentDescription("Quoted text: Billed monthly.").assertExists()
        rule.onNodeWithText("Should this say billed every quarter instead?", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("20 minutes ago", useUnmergedTree = true).assertExists()
    }

    @Test
    fun olderRepliesFoldAndUnfold() {
        rule.setMonarchContent { MonarchCommentThread(comments = LongThread, now = Now) }
        rule.onAllNodesWithText("Agreed.", useUnmergedTree = true).assertCountEquals(0)
        rule.onAllNodesWithText("Updated the copy.", useUnmergedTree = true).assertCountEquals(0)
        rule.onNodeWithText("Shipping tomorrow.", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Show 2 earlier replies").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Agreed.", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithText("Show 2 earlier replies").assertCountEquals(0)
    }

    @Test
    fun theFoldingRuleKeepsTheLatestReplies() {
        val t = MonarchCommentThreadTokens
        assertEquals(0, t.folded(replyCount = 4, visibleReplies = 3, expanded = false))
        assertEquals(2, t.folded(replyCount = 5, visibleReplies = 3, expanded = false))
        assertEquals(0, t.folded(replyCount = 5, visibleReplies = 3, expanded = true))
        assertEquals(0, t.folded(replyCount = 1, visibleReplies = 0, expanded = false))
        assertEquals(3, t.folded(replyCount = 3, visibleReplies = -2, expanded = false))
        assertTrue(t.canReply(takesReplies = true, resolved = false))
        assertTrue(!t.canReply(takesReplies = true, resolved = true))
        assertTrue(!t.canReply(takesReplies = false, resolved = false))
    }

    @Test
    fun resolvingCallsBackAndIsAnnounced() {
        val calls = mutableListOf<Boolean>()
        rule.setMonarchContent {
            MonarchCommentThread(comments = Start, now = Now, onResolvedChange = { calls += it })
        }
        rule.onAllNodes(polite and readsText("Thread resolved")).assertCountEquals(0)
        rule.onNodeWithText("Resolve").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(listOf(true), calls)
        rule.onNode(polite and readsText("Thread resolved")).assertExists()
    }

    @Test
    fun aResolvedThreadSaysSoInTextAndOffersReopen() {
        val calls = mutableListOf<Boolean>()
        rule.setMonarchContent {
            MonarchCommentThread(
                comments = Start,
                resolved = true,
                now = Now,
                onResolvedChange = { calls += it },
                onReply = {},
            )
        }
        rule.onNodeWithText("Resolved", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("This thread is resolved. Reopen it to reply.", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithText("Send reply").assertCountEquals(0)
        rule.onNodeWithText("Reopen").performClick()
        rule.waitForIdle()
        assertEquals(listOf(false), calls)
        rule.onNode(polite and readsText("Thread reopened")).assertExists()
    }

    @Test
    fun aFailedActionIsWrittenInTheLiveRegion() {
        rule.setMonarchContent {
            MonarchCommentThread(
                comments = Start,
                now = Now,
                onResolvedChange = { error("offline") },
            )
        }
        rule.onNodeWithText("Resolve").performClick()
        rule.waitForIdle()
        rule.onNode(polite and readsText("That didn’t work. Try again.")).assertExists()
    }

    @Test
    fun aReplyIsSentTrimmedAndClearsTheField() {
        val sent = mutableListOf<String>()
        val draft = TextFieldState("  Looks good  ")
        rule.setMonarchContent {
            MonarchCommentThread(comments = Start, now = Now, replyState = draft, onReply = { sent += it })
        }
        rule.onNodeWithContentDescription("Reply").assertExists()
        rule.onNodeWithText("Send reply").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(listOf("Looks good"), sent)
        assertEquals("", draft.text.toString())
        rule.onNode(polite and readsText("Reply sent")).assertExists()
    }

    @Test
    fun aFailedReplyKeepsTheText() {
        val draft = TextFieldState("Looks good")
        rule.setMonarchContent {
            MonarchCommentThread(comments = Start, now = Now, replyState = draft, onReply = { error("offline") })
        }
        rule.onNodeWithText("Send reply").performClick()
        rule.waitForIdle()
        assertEquals("Looks good", draft.text.toString())
        rule.onNode(polite and readsText("That didn’t work. Try again.")).assertExists()
    }

    @Test
    fun deletingAsksFirstAndIsOfferedOnlyWhereAllowed() {
        val deleted = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchCommentThread(comments = Start, now = Now, onDeleteComment = { deleted += it })
        }
        val offering = rule.onAllNodes(deleteAction).fetchSemanticsNodes()
        assertEquals(1, offering.size)
        val action = offering.single().config[SemanticsActions.CustomActions].single { it.label == "Delete comment" }
        rule.runOnIdle { action.action() }
        rule.onNodeWithText("Delete this comment?").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.waitForIdle()
        assertEquals(emptyList(), deleted)
        rule.runOnIdle { action.action() }
        rule.onNodeWithText("Delete comment").performClick()
        rule.waitForIdle()
        assertEquals(listOf("c2"), deleted)
    }

    @Test
    fun withoutOnDeleteCommentNothingIsDeletable() {
        rule.setMonarchContent { MonarchCommentThread(comments = Start, now = Now) }
        rule.onAllNodes(deleteAction).assertCountEquals(0)
    }

    @Test
    fun theBadgeComesBeforeReopenInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchCommentThread(comments = Start, resolved = true, now = Now, onResolvedChange = {})
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Resolved", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Reopen"), direction)
        }
    }

    @Test
    fun textWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchCommentThread(
                comments = Start.take(1),
                quote = "Billed monthly, cancel any time.",
                now = Now,
                onResolvedChange = {},
            )
        }
        rule.onNodeWithText("Billed monthly, cancel any time.", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Should this say billed every quarter instead?", useUnmergedTree = true)
            .assertTextFullyVisible()
        rule.onNodeWithText("Resolve").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchCommentThread(
                comments = LongThread,
                quote = "Mensual",
                now = Now,
                onResolvedChange = {},
                onReply = {},
                labels = MonarchCommentThreadLabels(
                    label = { "Hilo de $it" },
                    quote = { "Cita: $it" },
                    showEarlier = { "Ver $it respuestas" },
                    resolve = "Resolver",
                    send = "Enviar",
                ),
            )
        }
        rule.onNodeWithContentDescription("Hilo de Lee Park").assertExists()
        rule.onNodeWithContentDescription("Cita: Mensual").assertExists()
        rule.onNodeWithText("Ver 2 respuestas").assertExists()
        rule.onNodeWithText("Resolver").assertExists()
        rule.onNodeWithText("Enviar").assertExists()
    }

    @Test
    fun theColoursComeFromTheirRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchCommentThreadTokens
            assertEquals(c.bgSubtle, t.background(true, c), theme.id)
            assertEquals(Color.Transparent, t.background(false, c), theme.id)
            assertEquals(c.borderStrong, t.quoteBarColor(c), theme.id)
            assertEquals(c.textMuted, t.mutedColor(c), theme.id)
            assertEquals(c.dangerStrong, t.errorColor(c), theme.id)
        }
    }
}
