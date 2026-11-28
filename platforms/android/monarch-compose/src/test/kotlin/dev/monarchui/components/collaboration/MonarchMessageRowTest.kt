package dev.monarchui.components.collaboration

import android.icu.util.TimeZone
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.collaboration.messagerow.MonarchMessageRow
import dev.monarchui.components.collaboration.messagerow.MonarchMessageRowAction
import dev.monarchui.components.collaboration.messagerow.MonarchMessageRowAuthor
import dev.monarchui.components.collaboration.messagerow.MonarchMessageRowLabels
import dev.monarchui.components.collaboration.messagerow.MonarchMessageRowStatus
import dev.monarchui.components.collaboration.messagerow.MonarchMessageRowTimeStyle
import dev.monarchui.components.collaboration.messagerow.MonarchMessageRowTokens
import dev.monarchui.components.collaboration.messagerow.MonarchMessageRowVariant
import dev.monarchui.components.collaboration.reactionbar.MonarchReactionBarItem
import dev.monarchui.components.typography.text.MonarchText
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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 14 November 2026, 14:00 UTC. */
private const val Now = 1_794_664_800_000L
private const val Minute = 60_000L
private const val Hour = 60 * Minute
private const val Day = 24 * Hour

private val Ana = MonarchMessageRowAuthor("Ana Ruiz")
private val ThumbsUp = MonarchReactionBarItem("👍", 3, reacted = true, label = "thumbs up")

internal object MonarchMessageRowFixtures : ComponentFixtures {
    override val id = "message-row"
    override val fixtures = listOf(
        fixture("first of a group, tags, reactions, thread") {
            MonarchMessageRow(
                author = Ana,
                time = Now - 5 * Minute,
                now = Now,
                tags = listOf("Guest"),
                reactions = listOf(ThumbsUp),
                replyCount = 4,
                onReactionToggle = { _, _ -> },
                onOpenThread = {},
            ) { MonarchText("The build is green again.") }
        },
        fixture("grouped follow-up, clock time") {
            MonarchMessageRow(
                author = Ana,
                time = Now,
                now = Now,
                grouped = true,
                timeStyle = MonarchMessageRowTimeStyle.Clock,
            ) { MonarchText("Shipping after lunch.") }
        },
        fixture("pending") {
            MonarchMessageRow(author = Ana, time = Now, now = Now, status = MonarchMessageRowStatus.Pending) {
                MonarchText("On my way")
            }
        },
        fixture("failed with retry and discard") {
            MonarchMessageRow(
                author = Ana,
                time = Now,
                now = Now,
                status = MonarchMessageRowStatus.Failed,
                errorMessage = "No connection",
                onRetry = {},
                onDiscard = {},
            ) { MonarchText("Can you check the invoice?") }
        },
        fixture("edited with history, highlighted, actions") {
            MonarchMessageRow(
                author = Ana,
                time = Now - 2 * Hour,
                now = Now,
                edited = true,
                highlighted = true,
                onShowEdits = {},
                actions = listOf(MonarchMessageRowAction("Reply", {}), MonarchMessageRowAction("Delete", {}, true)),
            ) { MonarchText("Meeting moved to 3 PM.") }
        },
        fixture("deleted, read-only reactions") {
            MonarchMessageRow(
                author = Ana,
                time = Now - 3 * Day,
                now = Now,
                deleted = true,
                reactions = listOf(ThumbsUp),
            ) { MonarchText("Gone") }
        },
        fixture("internal note, one reply") {
            MonarchMessageRow(
                author = MonarchMessageRowAuthor("Ben Okafor"),
                time = Now - 10 * Day,
                now = Now,
                variant = MonarchMessageRowVariant.Internal,
                tags = listOf("Internal"),
                replyCount = 1,
            ) { MonarchText("Customer is on the old plan.") }
        },
        fixture("system line") {
            MonarchMessageRow(author = Ana, time = Now, now = Now, variant = MonarchMessageRowVariant.System) {
                MonarchText("Ana Ruiz joined the channel")
            }
        },
        fixture("long author and body") {
            MonarchMessageRow(
                author = MonarchMessageRowAuthor("Maria Fernanda de los Santos Oliveira"),
                time = Now,
                now = Now,
                tags = listOf("Procurement"),
            ) { MonarchText("We reviewed the second draft of the contract and have a few questions about the terms.") }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchMessageRowTest {
    @get:Rule
    val rule = createComposeRule()

    private var savedZone: java.util.TimeZone? = null

    @Before
    fun pinTheTimeZone() {
        savedZone = java.util.TimeZone.getDefault()
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreTheTimeZone() {
        java.util.TimeZone.setDefault(savedZone)
    }

    private val utc: TimeZone get() = TimeZone.getTimeZone("UTC")

    @Test
    fun theHeaderAndBodyAreOneElementReadWithTheFullDate() {
        rule.setMonarchContent {
            MonarchMessageRow(author = Ana, time = Now - 5 * Minute, now = Now, tags = listOf("Guest")) {
                MonarchText("Hello", Modifier.testTag("body"))
            }
        }
        val full = MonarchMessageRowTokens.fullTime(Now - 5 * Minute, Locale.US, utc)
        val merged = rule.onNodeWithText("Hello").fetchSemanticsNode()
        val description = merged.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        assertEquals(listOf("Ana Ruiz, Guest, $full"), description)
        rule.onNodeWithText("5 minutes ago", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("AR", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aGroupedRowHidesTheHeaderButStillReadsTheAuthor() {
        rule.setMonarchContent {
            MonarchMessageRow(author = Ana, time = Now, now = Now, grouped = true) { MonarchText("Again") }
        }
        val merged = rule.onNodeWithText("Again").fetchSemanticsNode()
        val description = merged.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        assertTrue(description.single().startsWith("Ana Ruiz, "))
        rule.onAllNodesWithText("AR", useUnmergedTree = true).assertCountEquals(0)
        rule.onAllNodesWithText("now", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun aLongPressOpensTheActionsWhichAreAlsoTalkBackActions() {
        var replied = 0
        var deleted = 0
        rule.setMonarchContent {
            MonarchMessageRow(
                author = Ana,
                time = Now,
                now = Now,
                modifier = Modifier.testTag("row"),
                actions = listOf(
                    MonarchMessageRowAction("Reply", { replied++ }),
                    MonarchMessageRowAction("Delete", { deleted++ }, destructive = true),
                ),
            ) { MonarchText("Hello") }
        }
        val config = rule.onNodeWithTag("row").fetchSemanticsNode().config
        val labels = config.getOrNull(SemanticsActions.CustomActions).orEmpty().map { it.label }
        assertEquals(listOf("Reply", "Delete"), labels)
        assertEquals("Message actions", config.getOrNull(SemanticsActions.OnLongClick)?.label)
        config.getOrNull(SemanticsActions.CustomActions)!![1].action()
        assertEquals(1, deleted)

        rule.onNodeWithTag("row").performTouchInput { longClick() }
        rule.onNodeWithText("Reply").performClick()
        assertEquals(1, replied)
        rule.onAllNodesWithText("Reply").assertCountEquals(0)
    }

    @Test
    fun pendingFailedAndDeletedMessagesOfferNoActions() {
        var status by mutableStateOf(MonarchMessageRowStatus.Pending)
        var deleted by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchMessageRow(
                author = Ana,
                time = Now,
                now = Now,
                status = status,
                deleted = deleted,
                modifier = Modifier.testTag("row"),
                actions = listOf(MonarchMessageRowAction("Reply", {})),
            ) { MonarchText("Hello") }
        }
        val noLongPress = SemanticsMatcher.keyNotDefined(SemanticsActions.OnLongClick)
        rule.onNodeWithTag("row").assert(noLongPress)
        rule.onNodeWithText("Sending…", substring = true).assertExists()
        status = MonarchMessageRowStatus.Failed
        rule.onNodeWithTag("row").assert(noLongPress)
        status = MonarchMessageRowStatus.Sent
        rule.onNodeWithTag("row").assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
        deleted = true
        rule.onNodeWithTag("row").assert(noLongPress)
    }

    @Test
    fun aFailureIsWrittenInALiveRegionWithRetryAndDiscard() {
        var retried = 0
        var discarded = 0
        var status by mutableStateOf(MonarchMessageRowStatus.Sent)
        rule.setMonarchContent {
            MonarchMessageRow(
                author = Ana,
                time = Now,
                now = Now,
                status = status,
                onRetry = { retried++ },
                onDiscard = { discarded++ },
            ) { MonarchText("Hello") }
        }
        val live = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        rule.onNode(live).assertExists()
        rule.onAllNodesWithText("Retry").assertCountEquals(0)
        status = MonarchMessageRowStatus.Failed
        rule.onNode(live).assert(SemanticsMatcher("reads Not sent") { node ->
            node.config.getOrNull(SemanticsProperties.Text).orEmpty().any { it.text == "Not sent" }
        })
        rule.onNodeWithText("Retry").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Discard").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, retried)
        assertEquals(1, discarded)
    }

    @Test
    fun theEditedMarkerOpensTheHistory() {
        var opened = 0
        rule.setMonarchContent {
            MonarchMessageRow(author = Ana, time = Now, now = Now, edited = true, onShowEdits = { opened++ }) {
                MonarchText("Hello")
            }
        }
        rule.onNodeWithContentDescription("Edited. Show edit history")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, opened)
    }

    @Test
    fun aDeletedMessageHidesItsBodyReactionsAndReplies() {
        rule.setMonarchContent {
            MonarchMessageRow(
                author = Ana,
                time = Now,
                now = Now,
                deleted = true,
                edited = true,
                reactions = listOf(ThumbsUp),
                replyCount = 3,
            ) { MonarchText("Secret") }
        }
        rule.onNodeWithText("This message was deleted.", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithText("Secret", useUnmergedTree = true).assertCountEquals(0)
        rule.onAllNodesWithText("(edited)", useUnmergedTree = true).assertCountEquals(0)
        rule.onAllNodesWithText("3 replies", useUnmergedTree = true).assertCountEquals(0)
        val reaction = SemanticsMatcher("a thumbs up reaction") { node ->
            node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().any { it.startsWith("thumbs up") }
        }
        rule.onAllNodes(reaction, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun theReplyCountIsAButtonOnlyWithOnOpenThread() {
        var opened = 0
        var withThread by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchMessageRow(
                author = Ana,
                time = Now,
                now = Now,
                replyCount = if (withThread) 4 else 1,
                onOpenThread = if (withThread) ({ opened++ }) else null,
            ) { MonarchText("Hello") }
        }
        rule.onNodeWithText("4 replies").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, opened)
        withThread = false
        rule.onNodeWithText("1 reply", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun aSystemLineHasNoAvatarAndReadsItsTime() {
        rule.setMonarchContent {
            MonarchMessageRow(author = Ana, time = Now, now = Now, variant = MonarchMessageRowVariant.System) {
                MonarchText("Ana joined")
            }
        }
        rule.onAllNodesWithText("AR", useUnmergedTree = true).assertCountEquals(0)
        val description = rule.onNodeWithText("Ana joined").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        assertEquals(listOf(MonarchMessageRowTokens.fullTime(Now, Locale.US, utc)), description)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchMessageRow(
                author = Ana,
                time = Now,
                now = Now,
                status = MonarchMessageRowStatus.Failed,
                edited = true,
                replyCount = 2,
                onRetry = {},
                labels = MonarchMessageRowLabels(
                    failed = "No enviado",
                    retry = "Reintentar",
                    edited = "(editado)",
                    replies = { "$it respuestas" },
                ),
            ) { MonarchText("Hola") }
        }
        rule.onNodeWithText("No enviado", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Reintentar").assertExists()
        rule.onNodeWithText("(editado)", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("2 respuestas", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theAvatarComesBeforeTheAuthorInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchMessageRow(author = Ana, time = Now, now = Now) { MonarchText("Hello") }
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("AR", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Ana Ruiz", useUnmergedTree = true), direction)
            rule.onNodeWithText("Ana Ruiz", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("now", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun textWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchMessageRow(author = MonarchMessageRowAuthor("Maria Fernanda de los Santos"), time = Now, now = Now) {
                MonarchText("We reviewed the second draft of the contract.")
            }
        }
        rule.onNodeWithText("Maria Fernanda de los Santos", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("We reviewed the second draft of the contract.", useUnmergedTree = true)
            .assertTextFullyVisible()
    }

    @Test
    fun theRelativeTimeFollowsTheWebSteps() {
        val t = MonarchMessageRowTokens
        // The web message row uses RelativeTime with no absoluteAfter: seconds, future times and weeks as words
        // (MONARCH-COMPAT: Android wrote "now", and a date from a week on).
        assertEquals("now", t.relativeTime(Now, Now, Locale.US))
        assertEquals("30 seconds ago", t.relativeTime(Now - 30_000L, Now, Locale.US))
        assertEquals("in 1 hour", t.relativeTime(Now + Hour, Now, Locale.US))
        assertEquals("5 minutes ago", t.relativeTime(Now - 5 * Minute, Now, Locale.US))
        assertEquals("3 hours ago", t.relativeTime(Now - 3 * Hour, Now, Locale.US))
        assertEquals("2 days ago", t.relativeTime(Now - 2 * Day, Now, Locale.US))
        assertEquals("last week", t.relativeTime(Now - 10 * Day, Now, Locale.US))
        assertEquals("last year", t.relativeTime(Now - 365 * Day, Now, Locale.US))
        // ICU puts a narrow no-break space before PM.
        assertEquals("2:00 PM", t.clockTime(Now, Locale.US, utc).replace('\u202F', ' '))
        assertTrue(t.fullTime(Now, Locale.US, utc).startsWith("Saturday, November 14, 2026"))
    }

    @Test
    fun onlySentMessagesWithActionsOfferTheMenu() {
        val t = MonarchMessageRowTokens
        assertTrue(t.offersActions(MonarchMessageRowVariant.Standard, MonarchMessageRowStatus.Sent, false, 1))
        assertTrue(t.offersActions(MonarchMessageRowVariant.Internal, MonarchMessageRowStatus.Sent, false, 1))
        assertTrue(!t.offersActions(MonarchMessageRowVariant.System, MonarchMessageRowStatus.Sent, false, 1))
        assertTrue(!t.offersActions(MonarchMessageRowVariant.Standard, MonarchMessageRowStatus.Pending, false, 1))
        assertTrue(!t.offersActions(MonarchMessageRowVariant.Standard, MonarchMessageRowStatus.Sent, true, 1))
        assertTrue(!t.offersActions(MonarchMessageRowVariant.Standard, MonarchMessageRowStatus.Sent, false, 0))
    }

    @Test
    fun theColoursComeFromTheirRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchMessageRowTokens
            assertEquals(c.accentSubtle, t.background(MonarchMessageRowVariant.Internal, true, c), theme.id)
            assertEquals(c.warningSubtle, t.background(MonarchMessageRowVariant.Internal, false, c), theme.id)
            assertEquals(Color.Transparent, t.background(MonarchMessageRowVariant.Standard, false, c), theme.id)
            assertEquals(c.warningBorder, t.bar(MonarchMessageRowVariant.Internal, c), theme.id)
            assertNull(t.bar(MonarchMessageRowVariant.Standard, c), theme.id)
            assertEquals(c.textMuted, t.bodyColor(MonarchMessageRowStatus.Pending, false, c), theme.id)
            assertEquals(c.textMuted, t.bodyColor(MonarchMessageRowStatus.Sent, true, c), theme.id)
            assertEquals(c.textDefault, t.bodyColor(MonarchMessageRowStatus.Failed, false, c), theme.id)
            assertEquals(c.dangerStrong, t.errorColor(c), theme.id)
        }
    }
}
