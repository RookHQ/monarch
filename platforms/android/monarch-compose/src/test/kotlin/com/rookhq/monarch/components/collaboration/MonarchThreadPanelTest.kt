@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.collaboration

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.collaboration.messagerow.MonarchMessageRow
import com.rookhq.monarch.components.collaboration.messagerow.MonarchMessageRowAuthor
import com.rookhq.monarch.components.collaboration.threadpanel.MonarchThreadPanel
import com.rookhq.monarch.components.collaboration.threadpanel.MonarchThreadPanelLabels
import com.rookhq.monarch.components.collaboration.threadpanel.MonarchThreadPanelStatus
import com.rookhq.monarch.components.collaboration.threadpanel.MonarchThreadPanelTokens
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.onNodeWithTextOrDescription
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

/** 14 November 2026, 14:00 UTC. */
private const val Now = 1_794_664_800_000L
private const val Minute = 60_000L

private class ThreadReply(val id: String, val author: String, val text: String, val time: Long)

private val Replies = listOf(
    ThreadReply("r1", "Ben Okafor", "I can take it.", Now - 30 * Minute),
    ThreadReply("r2", "Ana Ruiz", "Thanks, Ben.", Now - 10 * Minute),
)

@Composable
private fun ThreadUnderTest(
    replies: List<ThreadReply> = Replies,
    replyCount: Int = replies.size,
    status: MonarchThreadPanelStatus = MonarchThreadPanelStatus.Ready,
    following: Boolean = false,
    notice: String? = null,
    labels: MonarchThreadPanelLabels = MonarchThreadPanelLabels(),
    onClose: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onFollowingChange: (suspend (Boolean) -> Unit)? = null,
    onRepliesSeen: ((Int) -> Unit)? = null,
    composer: (@Composable () -> Unit)? = null,
) {
    MonarchThreadPanel(
        replies = replies,
        key = { it.id },
        time = { it.time },
        replyCount = replyCount,
        status = status,
        following = following,
        authorId = { it.author },
        notice = notice,
        now = Now,
        locale = Locale.US,
        timeZone = "UTC",
        labels = labels,
        onClose = onClose,
        onRetry = onRetry,
        onFollowingChange = onFollowingChange,
        onRepliesSeen = onRepliesSeen,
        root = {
            MonarchMessageRow(author = MonarchMessageRowAuthor("Lee Park"), time = Now - 60 * Minute, now = Now) {
                MonarchText("Who can review the release notes?")
            }
        },
        composer = composer,
    ) { reply, context ->
        MonarchMessageRow(
            author = MonarchMessageRowAuthor(reply.author),
            time = reply.time,
            now = Now,
            grouped = context.grouped,
        ) { MonarchText(reply.text) }
    }
}

internal object MonarchThreadPanelFixtures : ComponentFixtures {
    override val id = "thread-panel"
    override val fixtures = listOf(
        fixture("replies, follow and close") { ThreadUnderTest(onClose = {}, onFollowingChange = {}) },
        fixture("following, with a reply box") {
            var text by remember { mutableStateOf("") }
            ThreadUnderTest(
                following = true,
                onFollowingChange = {},
                composer = {
                    MonarchTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth())
                },
            )
        },
        fixture("no replies yet") { ThreadUnderTest(replies = emptyList()) },
        fixture("more replies than loaded, with a notice") {
            ThreadUnderTest(replyCount = 500, notice = "Showing the latest 2 of 500 replies.")
        },
        fixture("loading") { ThreadUnderTest(status = MonarchThreadPanelStatus.Loading, onClose = {}) },
        fixture("failed, with Try again") { ThreadUnderTest(status = MonarchThreadPanelStatus.Error, onRetry = {}) },
        fixture("failed, no retry") { ThreadUnderTest(status = MonarchThreadPanelStatus.Error) },
        fixture("deleted, with Close") { ThreadUnderTest(status = MonarchThreadPanelStatus.Deleted, onClose = {}) },
        fixture("long text") {
            ThreadUnderTest(
                replies = listOf(
                    ThreadReply(
                        "r9",
                        "Maria Fernanda de los Santos Oliveira",
                        "I read the whole document and left notes on the second and third sections.",
                        Now - 5 * Minute,
                    ),
                ),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchThreadPanelTest {
    @get:Rule
    val rule = createComposeRule()

    private fun announces(text: String) = SemanticsMatcher("announces $text") { node ->
        node.config.getOrNull(SemanticsProperties.LiveRegion) == LiveRegionMode.Polite &&
            node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().contains(text)
    }

    @Test
    fun theRootTheCountAndTheRepliesAreShown() {
        rule.setMonarchContent { ThreadUnderTest() }
        // The heading of the panel; the day separators of the replies are headings too.
        rule.onNode(isHeading() and hasText("Thread")).assertExists()
        rule.onNodeWithText("Who can review the release notes?", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("2 replies", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("I can take it.", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Thanks, Ben.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theListIsNamedReplies() {
        rule.setMonarchContent { ThreadUnderTest() }
        rule.onNodeWithContentDescription("Replies").assertExists()
    }

    @Test
    fun aThreadWithoutRepliesStillShowsTheRoot() {
        rule.setMonarchContent { ThreadUnderTest(replies = emptyList()) }
        rule.onNodeWithText("Who can review the release notes?", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("0 replies", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theCountCanExceedTheLoadedRepliesAndTheNoticeFollows() {
        rule.setMonarchContent { ThreadUnderTest(replyCount = 500, notice = "Showing the latest 2 of 500 replies.") }
        rule.onNodeWithText("500 replies", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Showing the latest 2 of 500 replies.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun followingTogglesAndIsAnnounced() {
        val calls = mutableListOf<Boolean>()
        rule.setMonarchContent {
            var following by remember { mutableStateOf(false) }
            ThreadUnderTest(
                following = following,
                onFollowingChange = {
                    calls += it
                    following = it
                },
            )
        }
        rule.onNodeWithText("Follow").assertIsNotSelected().assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(listOf(true), calls)
        rule.onNodeWithText("Following").assertIsSelected()
        rule.onNode(announces("You’ll be notified about replies")).assertExists()
        rule.onNodeWithText("Following").performClick()
        rule.waitForIdle()
        assertEquals(listOf(true, false), calls)
        rule.onNode(announces("You won’t be notified about replies")).assertExists()
    }

    @Test
    fun aFailedFollowIsAnnouncedAndKeepsTheState() {
        rule.setMonarchContent { ThreadUnderTest(onFollowingChange = { error("offline") }) }
        rule.onNodeWithText("Follow").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Follow").assertIsNotSelected()
        rule.onNode(announces("Thread notifications didn’t change. Try again.")).assertExists()
    }

    @Test
    fun withoutOnFollowingChangeOrWhileLoadingThereIsNoFollowChip() {
        var status by mutableStateOf(MonarchThreadPanelStatus.Loading)
        rule.setMonarchContent { ThreadUnderTest(status = status, onFollowingChange = {}) }
        rule.onAllNodesWithText("Follow").assertCountEquals(0)
        status = MonarchThreadPanelStatus.Ready
        rule.waitForIdle()
        rule.onNodeWithText("Follow").assertExists()
    }

    @Test
    fun repliesAreReportedSeenAfterTheDelay() {
        val seen = mutableListOf<Int>()
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { ThreadUnderTest(onRepliesSeen = { seen += it }) }
        rule.mainClock.advanceTimeBy(MonarchThreadPanelTokens.SEEN_DELAY_MILLIS / 2)
        assertEquals(emptyList(), seen)
        rule.mainClock.advanceTimeBy(MonarchThreadPanelTokens.SEEN_DELAY_MILLIS)
        assertEquals(listOf(2), seen)
    }

    @Test
    fun loadingIsReadOnce() {
        rule.setMonarchContent { ThreadUnderTest(status = MonarchThreadPanelStatus.Loading) }
        rule.onNodeWithContentDescription("Loading thread…", substring = true).assertExists()
        rule.onAllNodesWithText("I can take it.", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun anErrorOffersTryAgain() {
        var retried = 0
        rule.setMonarchContent { ThreadUnderTest(status = MonarchThreadPanelStatus.Error, onRetry = { retried++ }) }
        rule.onNodeWithText("This thread couldn’t be loaded.", substring = true, useUnmergedTree = true)
            .assertExists()
        rule.onNodeWithText("Try again").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, retried)
    }

    @Test
    fun aDeletedThreadSaysSoAndOffersClose() {
        var closed = 0
        rule.setMonarchContent { ThreadUnderTest(status = MonarchThreadPanelStatus.Deleted, onClose = { closed++ }) }
        rule.onNodeWithText("This thread was deleted.").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
        )
        rule.onAllNodes(hasText("Close") or hasContentDescription("Close"))
            .onFirst().assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, closed)
    }

    @Test
    fun theHeadingComesBeforeCloseInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { ThreadUnderTest(onClose = {}, onFollowingChange = {}) }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Thread", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Follow"), direction)
            rule.onNodeWithText("Follow").assertIsBefore(rule.onNodeWithTextOrDescription("Close"), direction)
        }
    }

    @Test
    fun textWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) { ThreadUnderTest(onFollowingChange = {}) }
        rule.onNodeWithText("2 replies", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Follow").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            ThreadUnderTest(
                onClose = {},
                onFollowingChange = {},
                labels = MonarchThreadPanelLabels(
                    title = "Hilo",
                    close = "Cerrar",
                    follow = "Seguir",
                    replyCount = { "$it respuestas" },
                    replies = "Respuestas",
                ),
            )
        }
        rule.onNodeWithText("Hilo", useUnmergedTree = true).assertExists()
        rule.onNodeWithTextOrDescription("Cerrar").assertExists()
        rule.onNodeWithText("Seguir").assertExists()
        rule.onNodeWithText("2 respuestas", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Respuestas").assertExists()
    }

    @Test
    fun theFollowLogic() {
        val t = MonarchThreadPanelTokens
        assertEquals(true, t.showsFollow(true, MonarchThreadPanelStatus.Ready))
        assertEquals(false, t.showsFollow(false, MonarchThreadPanelStatus.Ready))
        MonarchThreadPanelStatus.entries.filter { it != MonarchThreadPanelStatus.Ready }.forEach {
            assertEquals(false, t.showsFollow(true, it))
        }
        assertEquals("on", t.followNotice(succeeded = true, following = true, "on", "off", "failed"))
        assertEquals("off", t.followNotice(succeeded = true, following = false, "on", "off", "failed"))
        assertEquals("failed", t.followNotice(succeeded = false, following = true, "on", "off", "failed"))
    }

    @Test
    fun theColoursComeFromTheirRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchThreadPanelTokens
            assertEquals(c.textMuted, t.mutedColor(c), theme.id)
            assertEquals(c.textDefault, t.titleColor(c), theme.id)
            assertEquals(c.dangerStrong, t.errorColor(c), theme.id)
        }
    }
}
