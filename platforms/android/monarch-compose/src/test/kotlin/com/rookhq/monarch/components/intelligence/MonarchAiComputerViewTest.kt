@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.intelligence.aicomputerview.AiComputerViewContent
import com.rookhq.monarch.components.intelligence.aicomputerview.AiComputerViewNews
import com.rookhq.monarch.components.intelligence.aicomputerview.AiComputerViewRules
import com.rookhq.monarch.components.intelligence.aicomputerview.MonarchAiComputerAction
import com.rookhq.monarch.components.intelligence.aicomputerview.MonarchAiComputerActionKind
import com.rookhq.monarch.components.intelligence.aicomputerview.MonarchAiComputerFrame
import com.rookhq.monarch.components.intelligence.aicomputerview.MonarchAiComputerPoint
import com.rookhq.monarch.components.intelligence.aicomputerview.MonarchAiComputerView
import com.rookhq.monarch.components.intelligence.aicomputerview.MonarchAiComputerViewLabels
import com.rookhq.monarch.components.intelligence.aicomputerview.MonarchAiComputerViewStatus
import com.rookhq.monarch.components.intelligence.aicomputerview.MonarchAiComputerViewSurface
import com.rookhq.monarch.components.intelligence.aicomputerview.MonarchAiComputerViewTokens
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
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 15 November 2026, 10:02 UTC. */
private const val At = 1_794_736_920_000L
private val Utc: TimeZone = TimeZone.getTimeZone("UTC")

private val Frame = MonarchAiComputerFrame(
    image = ColorPainter(Color.Transparent),
    width = 1280,
    height = 800,
    capturedAt = At,
    alt = "The sign-in page",
)
private val Opened =
    MonarchAiComputerAction("1", MonarchAiComputerActionKind.Navigate, "Opened the portal", At - 9_000L)
private val Clicked = MonarchAiComputerAction(
    id = "2",
    kind = MonarchAiComputerActionKind.Click,
    label = "Clicked Sign in",
    at = At,
    point = MonarchAiComputerPoint(640, 400),
)
private val Typed = MonarchAiComputerAction(
    id = "3",
    kind = MonarchAiComputerActionKind.Type,
    label = "Typed the email address",
    at = At + 1_000L,
    detail = "sam@example.com",
)
private val Private = MonarchAiComputerAction(
    id = "4",
    kind = MonarchAiComputerActionKind.Type,
    label = "Typed the password",
    at = At + 2_000L,
    detail = "hunter2",
    sensitive = true,
)

internal object MonarchAiComputerViewFixtures : ComponentFixtures {
    override val id = "ai-computer-view"
    override val fixtures = listOf(
        fixture("done, no screenshot, compact log") {
            MonarchAiComputerView(status = MonarchAiComputerViewStatus.Done, compact = true, locale = Locale.US)
        },
        fixture("working with controls") {
            MonarchAiComputerView(
                status = MonarchAiComputerViewStatus.Working,
                onPauseChange = {},
                onTakeOver = {},
                onStop = {},
                locale = Locale.US,
            )
        },
        fixture("waiting for you") {
            MonarchAiComputerView(
                status = MonarchAiComputerViewStatus.WaitingForYou,
                waitingReason = "Sign in to continue",
                onTakeOver = {},
                locale = Locale.US,
            )
        },
        fixture("taken over, computer") {
            MonarchAiComputerView(
                status = MonarchAiComputerViewStatus.TakenOver,
                surface = MonarchAiComputerViewSurface.Computer,
                onHandBack = {},
                locale = Locale.US,
            )
        },
        fixture("paused, screenshot with markers") {
            MonarchAiComputerView(
                status = MonarchAiComputerViewStatus.Paused,
                frame = Frame,
                actions = listOf(Opened, Clicked),
                pointer = MonarchAiComputerPoint(10, 10),
                selectedActionId = "2",
                onSelectAction = {},
                onPauseChange = {},
                compact = true,
                locale = Locale.US,
                timeZone = Utc.id,
            )
        },
        fixture("failed, log with private and typed actions") {
            MonarchAiComputerView(
                status = MonarchAiComputerViewStatus.Failed,
                actions = listOf(Typed, Private),
                currentUrl = "https://user:secret@billing.example.com/sign-in",
                windowTitle = "Billing portal",
                locale = Locale.US,
                timeZone = Utc.id,
            )
        },
        fixture("live picture") {
            MonarchAiComputerView(
                status = MonarchAiComputerViewStatus.Stopped,
                live = { Box(Modifier.fillMaxSize()) },
                compact = true,
                locale = Locale.US,
            )
        },
    )
}

private val isLiveRegion = SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)

private fun hasLiveRegion(mode: LiveRegionMode) = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, mode)

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiComputerViewTest {
    @get:Rule
    val rule = createComposeRule()

    private fun time(millis: Long) = MonarchAiComputerViewTokens.time(millis, Locale.US, Utc)

    @Test
    fun theStatusAndWhoIsInControlAreWritten() {
        var status by mutableStateOf(MonarchAiComputerViewStatus.Working)
        rule.setMonarchContent { MonarchAiComputerView(status = status, locale = Locale.US) }
        rule.onNodeWithText("Working").assertExists()
        rule.onNodeWithText("The assistant is controlling this browser").assertExists()
        status = MonarchAiComputerViewStatus.TakenOver
        rule.onNodeWithText("You are controlling this browser").assertExists()
        status = MonarchAiComputerViewStatus.WaitingForYou
        rule.onNodeWithText("The assistant needs you to continue").assertExists()
        rule.onNodeWithText("This step is one the assistant must not do itself.").assertExists()
        status = MonarchAiComputerViewStatus.Done
        rule.onNodeWithText("Done").assertExists()
        rule.onNodeWithText("You are controlling this browser").assertDoesNotExist()
        rule.onNodeWithContentDescription("Assistant browser view").assertExists()
    }

    @Test
    fun theControlsFollowTheStatus() {
        var status by mutableStateOf(MonarchAiComputerViewStatus.Working)
        val paused = mutableListOf<Boolean>()
        var tookOver = 0
        var handedBack = 0
        rule.setMonarchContent {
            MonarchAiComputerView(
                status = status,
                onPauseChange = { paused += it },
                onTakeOver = { tookOver++ },
                onHandBack = { handedBack++ },
                onStop = {},
            )
        }
        rule.onNode(hasText("Pause") and hasClickAction()).assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNode(hasText("Take over") and hasClickAction()).performClick()
        rule.onNode(hasText("Hand back")).assertDoesNotExist()
        status = MonarchAiComputerViewStatus.Paused
        rule.onNode(hasText("Resume") and hasClickAction()).performClick()
        status = MonarchAiComputerViewStatus.TakenOver
        rule.onNode(hasText("Take over")).assertDoesNotExist()
        rule.onNode(hasText("Hand back") and hasClickAction()).performClick()
        status = MonarchAiComputerViewStatus.Done
        rule.onNode(hasText("Stop")).assertDoesNotExist()
        assertEquals(listOf(true, false), paused)
        assertEquals(1, tookOver)
        assertEquals(1, handedBack)
    }

    @Test
    fun stopAsksFirst() {
        var stopped = 0
        rule.setMonarchContent {
            MonarchAiComputerView(status = MonarchAiComputerViewStatus.Working, onStop = { stopped++ })
        }
        rule.onNode(hasText("Stop") and hasClickAction()).performClick()
        rule.onNodeWithText("Stop the assistant? It will not finish the task.").assertExists()
        rule.onNode(hasText("Keep going") and hasClickAction()).performClick()
        assertEquals(0, stopped)
        rule.onNode(hasText("Stop") and hasClickAction()).performClick()
        rule.onNode(hasText("Stop now") and hasClickAction()).assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, stopped)
        rule.onNodeWithText("Stop the assistant? It will not finish the task.").assertDoesNotExist()
    }

    @Test
    fun theScreenIsDescribedAndOpensLarger() {
        rule.setMonarchContent {
            MonarchAiComputerView(
                status = MonarchAiComputerViewStatus.Paused,
                frame = Frame,
                actions = listOf(Clicked),
                locale = Locale.US,
                timeZone = Utc.id,
            )
        }
        val screen = rule.onNodeWithContentDescription("The sign-in page. Latest action: Clicked Sign in")
        val action = screen.fetchSemanticsNode().config[SemanticsActions.OnClick]
        assertEquals("View larger", action.label)
        rule.onNodeWithText("Screenshot from ${time(At)}").assertExists()
        screen.performClick()
        rule.onAllNodes(hasContentDescription("The sign-in page", substring = true)).fetchSemanticsNodes().let {
            assertTrue(it.size >= 2, "the lightbox shows the screenshot too")
        }
    }

    @Test
    fun aPrivateScreenshotIsNeverShown() {
        rule.setMonarchContent {
            MonarchAiComputerView(
                status = MonarchAiComputerViewStatus.Working,
                frame = Frame,
                actions = listOf(Clicked, Private),
                locale = Locale.US,
            )
        }
        val hidden = "Screenshot hidden: it may show private information. Latest action: Private action"
        rule.onNodeWithContentDescription(hidden).assertExists()
        rule.onNodeWithText("Screenshot from", substring = true).assertDoesNotExist()
    }

    @Test
    fun beforeTheFirstScreenshotTheBoxSaysSo() {
        rule.setMonarchContent { MonarchAiComputerView(status = MonarchAiComputerViewStatus.Starting) }
        rule.onNodeWithContentDescription("No screenshot yet. Screenshot of the browser the assistant is using")
            .assertExists()
    }

    @Test
    fun theAddressNeverShowsCredentials() {
        rule.setMonarchContent {
            MonarchAiComputerView(
                status = MonarchAiComputerViewStatus.Working,
                currentUrl = "https://sam:secret@billing.example.com/a",
                windowTitle = "Billing portal",
            )
        }
        rule.onNodeWithText("https://billing.example.com/a").assertExists()
        rule.onNodeWithContentDescription("Address: https://billing.example.com/a").assertExists()
        rule.onNodeWithText("secret", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Billing portal").assertExists()
    }

    @Test
    fun eachActionIsReadWithItsKindAndPrivateDetailsStayHidden() {
        rule.setMonarchContent {
            MonarchAiComputerView(
                status = MonarchAiComputerViewStatus.Done,
                actions = listOf(Clicked, Typed, Private),
                locale = Locale.US,
                timeZone = Utc.id,
            )
        }
        rule.onNodeWithContentDescription("Click: Clicked Sign in, ${time(At)}").assertExists()
        rule.onNodeWithContentDescription("Type: Typed the email address, Hidden, ${time(At + 1_000L)}").assertExists()
        rule.onNodeWithContentDescription("Type: Private action, Hidden, ${time(At + 2_000L)}").assertExists()
        rule.onNodeWithText("sam@example.com", substring = true, useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("hunter2", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun typedTextShowsOnlyWhenAsked() {
        rule.setMonarchContent {
            MonarchAiComputerView(
                status = MonarchAiComputerViewStatus.Done,
                actions = listOf(Typed, Private),
                showTypedText = true,
                locale = Locale.US,
                timeZone = Utc.id,
            )
        }
        rule.onNodeWithText("sam@example.com", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("hunter2", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun selectingAnActionReportsIt() {
        var selected by mutableStateOf<String?>(null)
        rule.setMonarchContent {
            MonarchAiComputerView(
                status = MonarchAiComputerViewStatus.Done,
                actions = listOf(Opened, Clicked),
                selectedActionId = selected,
                onSelectAction = { selected = it },
                locale = Locale.US,
                timeZone = Utc.id,
            )
        }
        rule.onNodeWithContentDescription("Click: Clicked Sign in, ${time(At)}")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("2", selected)
        rule.onNodeWithContentDescription("Click: Clicked Sign in, ${time(At)}").assertIsSelected()
    }

    @Test
    fun theCompactLogOpensFromItsButton() {
        rule.setMonarchContent {
            MonarchAiComputerView(
                status = MonarchAiComputerViewStatus.Done,
                actions = listOf(Opened, Clicked),
                compact = true,
                locale = Locale.US,
                timeZone = Utc.id,
            )
        }
        val toggle = rule.onNode(hasText("Actions (2)") and hasClickAction())
        assertEquals("Collapsed", toggle.stateDescription())
        rule.onNodeWithContentDescription("Click: Clicked Sign in, ${time(At)}").assertDoesNotExist()
        toggle.performClick()
        assertEquals("Expanded", rule.onNode(hasText("Actions (2)") and hasClickAction()).stateDescription())
        rule.onNodeWithContentDescription("Click: Clicked Sign in, ${time(At)}").assertExists()
    }

    @Test
    fun olderActionsWaitBehindShowEarlier() {
        rule.setMonarchContent {
            MonarchAiComputerView(
                status = MonarchAiComputerViewStatus.Done,
                actions = listOf(Opened, Clicked, Typed),
                visibleActions = 1,
                locale = Locale.US,
                timeZone = Utc.id,
            )
        }
        rule.onNodeWithContentDescription("Click: Clicked Sign in, ${time(At)}").assertDoesNotExist()
        rule.onNode(hasText("Show 2 earlier actions") and hasClickAction()).performClick()
        rule.onNodeWithContentDescription("Click: Clicked Sign in, ${time(At)}").assertExists()
    }

    @Test
    fun statusChangesAreAnnouncedOnceAndUrgentOnesAssertively() {
        var status by mutableStateOf(MonarchAiComputerViewStatus.Working)
        rule.setMonarchContent { MonarchAiComputerView(status = status) }
        val spoken = rule.onAllNodes(isLiveRegion and hasContentDescription("", substring = true))
        assertTrue(spoken.fetchSemanticsNodes().isEmpty(), "the first status is not news")
        status = MonarchAiComputerViewStatus.TakenOver
        rule.onNode(
            hasLiveRegion(LiveRegionMode.Polite) and
                hasContentDescription("You are in control of this browser. The assistant has stopped acting."),
        ).assertExists()
        status = MonarchAiComputerViewStatus.Working
        rule.onNode(hasContentDescription("The assistant is in control of this browser again.")).assertExists()
        status = MonarchAiComputerViewStatus.Failed
        rule.onNode(hasLiveRegion(LiveRegionMode.Assertive) and hasContentDescription("Assistant status: Failed"))
            .assertExists()
    }

    @Test
    fun newActionsAreAnnouncedPolitelyAndThrottled() {
        rule.mainClock.autoAdvance = false
        var actions by mutableStateOf(listOf(Opened))
        rule.setMonarchContent {
            MonarchAiComputerView(status = MonarchAiComputerViewStatus.Working, actions = actions)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNode(hasContentDescription("Opened the portal") and isLiveRegion).assertDoesNotExist()
        actions = listOf(Opened, Clicked)
        Snapshot.sendApplyNotifications()
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        rule.onNode(hasLiveRegion(LiveRegionMode.Polite) and hasContentDescription("Clicked Sign in")).assertExists()
        actions = listOf(Opened, Clicked, Typed, Private)
        Snapshot.sendApplyNotifications()
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        rule.onNode(hasLiveRegion(LiveRegionMode.Polite) and hasContentDescription("Clicked Sign in")).assertExists()
        rule.mainClock.advanceTimeBy(MonarchAiComputerViewTokens.DEFAULT_ANNOUNCE_INTERVAL_MILLIS)
        rule.onNode(hasContentDescription("2 new actions. Latest: Private action") and isLiveRegion).assertExists()
    }

    @Test
    fun theControlsFollowTheLayoutDirection() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiComputerView(status = MonarchAiComputerViewStatus.Working, onTakeOver = {}, onPauseChange = {})
        }
        rule.onNode(hasText("Take over") and hasClickAction())
            .assertIsBefore(rule.onNode(hasText("Pause") and hasClickAction()), LayoutDirection.Rtl)
    }

    @Test
    fun aLongWaitingReasonWrapsAtTheLargestFontSize() {
        val reason = "Sign in with your company account to continue"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiComputerView(status = MonarchAiComputerViewStatus.WaitingForYou, waitingReason = reason)
        }
        rule.onNodeWithText(reason, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiComputerView(
                status = MonarchAiComputerViewStatus.Working,
                onStop = {},
                compact = true,
                labels = MonarchAiComputerViewLabels(
                    label = { "Vue de l’assistant" },
                    status = { "En cours" },
                    agentInControl = { "L’assistant contrôle ce navigateur" },
                    stop = "Arrêter",
                    logToggle = { "Actions : $it" },
                    collapsed = "Replié",
                    noFrame = "Pas encore de capture",
                ),
            )
        }
        rule.onNodeWithContentDescription("Vue de l’assistant").assertExists()
        rule.onNodeWithText("En cours").assertExists()
        rule.onNodeWithText("L’assistant contrôle ce navigateur").assertExists()
        rule.onNodeWithText("Arrêter").assertExists()
        assertEquals("Replié", rule.onNode(hasText("Actions : 0") and hasClickAction()).stateDescription())
        rule.onNodeWithContentDescription("Pas encore de capture", substring = true).assertExists()
    }

    @Test
    fun theRulesOfControlAndPrivacy() {
        val t = AiComputerViewRules
        val c = AiComputerViewContent
        val s = MonarchAiComputerViewStatus.entries
        val acting = listOf(MonarchAiComputerViewStatus.Starting, MonarchAiComputerViewStatus.Working)
        assertEquals(acting, s.filter { t.canPause(it, true) })
        assertFalse(t.canPause(MonarchAiComputerViewStatus.Working, false))
        assertEquals(listOf(MonarchAiComputerViewStatus.Paused), s.filter { t.canResume(it, true) })
        assertEquals(4, s.count { t.canTakeOver(it, true) })
        assertEquals(listOf(MonarchAiComputerViewStatus.TakenOver), s.filter { t.canHandBack(it, true) })
        assertEquals(5, s.count { t.canStop(it, true) })
        assertEquals(MonarchButtonVariant.Primary, t.takeOverVariant(MonarchAiComputerViewStatus.WaitingForYou))
        assertEquals(MonarchButtonVariant.Secondary, t.takeOverVariant(MonarchAiComputerViewStatus.Working))
        val working = MonarchAiComputerViewStatus.Working
        val takenOver = MonarchAiComputerViewStatus.TakenOver
        assertEquals(AiComputerViewNews.TakenOver, t.news(working, takenOver))
        assertEquals(AiComputerViewNews.Released, t.news(takenOver, working))
        assertEquals(AiComputerViewNews.Status, t.news(takenOver, MonarchAiComputerViewStatus.Done))
        assertTrue(t.urgent(MonarchAiComputerViewStatus.Failed) && t.urgent(MonarchAiComputerViewStatus.WaitingForYou))
        assertFalse(t.urgent(MonarchAiComputerViewStatus.Done))
        assertEquals("Private", c.actionLabel(Private, "Private"))
        assertEquals("Clicked Sign in", c.actionLabel(Clicked, "Private"))
        assertTrue(c.detail(Private, showTypedText = true).hidden)
        assertTrue(c.detail(Typed, showTypedText = false).hidden)
        assertEquals("sam@example.com", c.detail(Typed, showTypedText = true).text)
        assertFalse(c.detail(Clicked, showTypedText = false).hidden)
        assertTrue(c.pictureHidden(Frame, Private, live = false, liveSensitive = false))
        assertFalse(c.pictureHidden(Frame, Clicked, live = false, liveSensitive = false))
        assertTrue(c.pictureHidden(null, null, live = true, liveSensitive = true))
        assertFalse(c.pictureHidden(null, Private, live = false, liveSensitive = false))
    }

    @Test
    fun theGeometryRules() {
        val t = AiComputerViewContent
        assertEquals(Offset(0.5f, 0.5f), t.position(MonarchAiComputerPoint(640, 400), 1280, 800))
        assertEquals(Offset(1f, 0f), t.position(MonarchAiComputerPoint(5000, -20), 1280, 800))
        assertNull(t.position(MonarchAiComputerPoint(1, 1), 0, 800))
        assertNull(t.position(null, 1280, 800))
        assertEquals(1.6f, t.aspectRatio(Frame))
        assertEquals(1.6f, t.aspectRatio(null))
        assertEquals("https://x.test/a?b=1", t.displayUrl("https://user:pw@x.test/a?b=1"))
        assertEquals("https://x.test/a@b", t.displayUrl("https://x.test/a@b"))
        assertEquals(0, t.hiddenBefore(3, 200, showAll = false))
        assertEquals(2, t.hiddenBefore(3, 1, showAll = false))
        assertEquals(0, t.hiddenBefore(3, 1, showAll = true))
        assertEquals(Clicked, t.lastPointAction(listOf(Opened, Clicked, Typed)))
    }

    @Test
    fun theColoursComeFromTheThemeRolesInEveryTheme() {
        val t = MonarchAiComputerViewTokens
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.dangerStrong, t.statusColor(MonarchAiComputerViewStatus.Failed, c), theme.id)
            assertEquals(c.successStrong, t.statusColor(MonarchAiComputerViewStatus.Done, c), theme.id)
            assertEquals(c.warningStrong, t.statusColor(MonarchAiComputerViewStatus.WaitingForYou, c), theme.id)
            assertEquals(c.textDefault, t.statusColor(MonarchAiComputerViewStatus.Working, c), theme.id)
            val marks = t.markerColors(c)
            assertEquals(c.aiSolid, marks.last, theme.id)
            assertEquals(c.accentSolid, marks.selected, theme.id)
            assertEquals(c.textDefault, marks.pointer, theme.id)
            assertEquals(c.bgSurface, marks.pointerOutline, theme.id)
            assertEquals(c.accentSubtle, t.rowColor(true, c), theme.id)
            assertEquals(c.bgSurface, t.rowColor(false, c), theme.id)
            assertEquals(c.accentSolid, t.barColor(true, c), theme.id)
            val bar = t.banner(c)
            assertEquals(c.aiSolid, bar.container, theme.id)
            assertEquals(c.aiOnSolid, bar.content, theme.id)
            assertEquals(c.aiBorder, t.frameBorder(true, c), theme.id)
            assertEquals(c.borderDefault, t.frameBorder(false, c), theme.id)
        }
    }
}
