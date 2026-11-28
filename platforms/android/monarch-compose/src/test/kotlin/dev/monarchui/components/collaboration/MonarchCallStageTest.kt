@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.collaboration

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.collaboration.callcontrols.MonarchCallControlsLayout
import dev.monarchui.components.collaboration.callstage.CallStageTool
import dev.monarchui.components.collaboration.callstage.MonarchCallStage
import dev.monarchui.components.collaboration.callstage.MonarchCallStageLabels
import dev.monarchui.components.collaboration.callstage.MonarchCallStageLayout
import dev.monarchui.components.collaboration.callstage.MonarchCallStageParticipant
import dev.monarchui.components.collaboration.callstage.MonarchCallStageSurface
import dev.monarchui.components.collaboration.callstage.MonarchCallStageTokens
import dev.monarchui.components.collaboration.calltile.MonarchCallQuality
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val Start = 1_767_225_600_000L
private const val FourMinutes = 252_000L

private val me = MonarchCallStageParticipant("me", "Sam Lee", self = true, micOff = true)
private val robin =
    MonarchCallStageParticipant("robin", "Robin Park", speaking = true, quality = MonarchCallQuality.Good)
private val alex = MonarchCallStageParticipant("alex", "Alex Chen", micOff = true, quality = MonarchCallQuality.Fair)

internal object MonarchCallStageFixtures : ComponentFixtures {
    override val id = "call-stage"
    override val fixtures = listOf(
        fixture("grid of two with pins, timer and low data") {
            MonarchCallStage(
                participants = listOf(me, robin),
                title = "Weekly planning",
                startedAt = Start,
                now = Start + FourMinutes,
                mode = MonarchCallControlsLayout.Grid,
                onPinnedIdChange = {},
                lowData = true,
            )
        },
        fixture("one-to-one spotlight") {
            MonarchCallStage(participants = listOf(me, robin), now = Start)
        },
        fixture("alone and docked, with Expand and Minimize") {
            MonarchCallStage(
                participants = listOf(me),
                now = Start,
                surface = MonarchCallStageSurface.Docked,
                onSurfaceChange = {},
                onMinimize = {},
            )
        },
        fixture("picture in picture, reconnecting, error") {
            MonarchCallStage(
                participants = listOf(me, robin),
                now = Start,
                mode = MonarchCallControlsLayout.Grid,
                surface = MonarchCallStageSurface.Pip,
                reconnecting = true,
                error = "The call could not reach Robin.",
            )
        },
        fixture("muted hint and controls") {
            MonarchCallStage(
                participants = listOf(me, alex),
                now = Start,
                mode = MonarchCallControlsLayout.Grid,
                surface = MonarchCallStageSurface.Pip,
                speakingWhileMuted = true,
                controls = { MonarchButton(text = "Leave", onClick = {}) },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchCallStageTest {
    @get:Rule
    val rule = createComposeRule()

    private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
    private val assertive = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive)

    @Test
    fun theHeaderHasTheTitleTheTimerAndTheHeadCount() {
        rule.setMonarchContent {
            MonarchCallStage(
                participants = listOf(me, robin, alex),
                title = "Weekly planning",
                startedAt = Start,
                now = Start + FourMinutes,
                mode = MonarchCallControlsLayout.Grid,
            )
        }
        rule.onNode(hasText("Weekly planning") and isHeading()).assertExists()
        rule.onNodeWithContentDescription("Call duration, 04:12").assertExists()
        rule.onNodeWithText("3 in call").assert(polite)
    }

    @Test
    fun aloneTheHeaderSaysWaitingAndTheStageIsNamedCall() {
        rule.setMonarchContent { MonarchCallStage(participants = listOf(me), now = Start) }
        rule.onNodeWithText("Waiting for others…").assert(polite)
        rule.onNodeWithContentDescription("Call").assertExists()
        rule.onNodeWithContentDescription("Sam Lee (you)").assertExists()
    }

    @Test
    fun pinButtonsReportTheNextPin() {
        val pins = mutableListOf<String?>()
        rule.setMonarchContent {
            MonarchCallStage(
                participants = listOf(me, robin, alex),
                now = Start,
                // A pin would put Alex in the spotlight; a fixed grid keeps every tile at the size with buttons.
                layout = MonarchCallStageLayout.Grid(listOf("me", "robin", "alex"), 2),
                pinnedId = "alex",
                onPinnedIdChange = { pins += it },
            )
        }
        rule.onNodeWithContentDescription("Pin Robin Park").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("Unpin Alex Chen").performClick()
        assertEquals(listOf("robin", null), pins)
    }

    @Test
    fun compactSurfacesHaveNoTileButtons() {
        rule.setMonarchContent {
            MonarchCallStage(
                participants = listOf(me, robin),
                now = Start,
                mode = MonarchCallControlsLayout.Grid,
                surface = MonarchCallStageSurface.Docked,
                onPinnedIdChange = {},
                onFullScreen = {},
            )
        }
        rule.onNodeWithContentDescription("Pin Robin Park").assertDoesNotExist()
        rule.onNodeWithContentDescription("Show Robin Park full screen").assertDoesNotExist()
    }

    @Test
    fun fullScreenReceivesTheParticipant() {
        val opened = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchCallStage(
                participants = listOf(me, robin),
                now = Start,
                mode = MonarchCallControlsLayout.Grid,
                onFullScreen = { opened += it },
            )
        }
        rule.onNodeWithContentDescription("Show Robin Park full screen").performClick()
        assertEquals(listOf("robin"), opened)
    }

    @Test
    fun headerButtonsChangeTheSurface() {
        val surfaces = mutableListOf<MonarchCallStageSurface>()
        var pip = 0
        rule.setMonarchContent {
            MonarchCallStage(
                participants = listOf(me, robin),
                now = Start,
                onSurfaceChange = { surfaces += it },
                onPictureInPicture = { pip++ },
                onMinimize = {},
            )
        }
        rule.onNode(hasText("Shrink call") and hasClickAction())
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNode(hasText("Picture in picture") and hasClickAction()).performClick()
        rule.onNodeWithText("Minimize call").assertDoesNotExist()
        assertEquals(listOf(MonarchCallStageSurface.Docked), surfaces)
        assertEquals(1, pip)
    }

    @Test
    fun aDockedStageOffersExpandAndMinimize() {
        val surfaces = mutableListOf<MonarchCallStageSurface>()
        var minimized = 0
        rule.setMonarchContent {
            MonarchCallStage(
                participants = listOf(me, robin),
                now = Start,
                surface = MonarchCallStageSurface.Docked,
                onSurfaceChange = { surfaces += it },
                onMinimize = { minimized++ },
            )
        }
        rule.onNode(hasText("Expand call") and hasClickAction()).performClick()
        rule.onNode(hasText("Minimize call") and hasClickAction()).performClick()
        assertEquals(listOf(MonarchCallStageSurface.Stage), surfaces)
        assertEquals(1, minimized)
    }

    @Test
    fun noticesAreAnnounced() {
        rule.setMonarchContent {
            MonarchCallStage(
                participants = listOf(me, robin),
                now = Start,
                reconnecting = true,
                error = "The call could not reach Robin.",
                speakingWhileMuted = true,
            )
        }
        rule.onNode(hasText("You’re offline. Trying to reconnect…", substring = true) and polite).assertExists()
        rule.onNode(hasText("The call could not reach Robin.", substring = true) and assertive).assertExists()
        rule.onNode(hasText("You’re muted.", substring = true) and polite).assertExists()
    }

    @Test
    fun theVideoSlotGetsEachParticipant() {
        rule.setMonarchContent {
            MonarchCallStage(
                participants = listOf(me, robin),
                now = Start,
                mode = MonarchCallControlsLayout.Grid,
                video = { person -> Box(Modifier.fillMaxSize().testTag("video-${person.id}")) },
            )
        }
        rule.onNodeWithTag("video-me", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("video-robin", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theSpeakerIsLargeAboveTheFilmstrip() {
        rule.setMonarchContent {
            MonarchCallStage(
                participants = listOf(me, robin, alex),
                now = Start,
                mode = MonarchCallControlsLayout.Speaker,
                activeSpeakerId = "alex",
            )
        }
        val main = rule.onNodeWithContentDescription("Alex Chen").fetchSemanticsNode().boundsInRoot
        val strip = rule.onNodeWithContentDescription("Robin Park").fetchSemanticsNode().boundsInRoot
        assertTrue(main.top < strip.top, "$main above $strip")
    }

    @Test
    fun theGridStartsAtTheStartEdgeInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchCallStage(participants = listOf(me, robin), now = Start, mode = MonarchCallControlsLayout.Grid)
        }
        rule.onNodeWithContentDescription("Sam Lee (you)")
            .assertIsBefore(rule.onNodeWithContentDescription("Robin Park"), LayoutDirection.Rtl)
    }

    @Test
    fun theTitleIsFullyVisibleAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchCallStage(
                participants = listOf(me),
                title = "Weekly planning with the design team",
                now = Start,
                surface = MonarchCallStageSurface.Docked,
            )
        }
        rule.onNodeWithText("Weekly planning with the design team").assertTextFullyVisible()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchCallStage(
                participants = listOf(me, robin),
                startedAt = Start,
                now = Start + FourMinutes,
                lowData = true,
                onSurfaceChange = {},
                labels = MonarchCallStageLabels(
                    region = "Anruf",
                    duration = "Dauer",
                    inCall = { "$it im Anruf" },
                    lowData = "Wenig Daten",
                    shrink = "Verkleinern",
                ),
            )
        }
        rule.onNodeWithContentDescription("Anruf").assertExists()
        rule.onNodeWithContentDescription("Dauer, 04:12").assertExists()
        rule.onNodeWithText("2 im Anruf").assertExists()
        rule.onNodeWithText("Wenig Daten").assertExists()
        rule.onNode(hasText("Verkleinern") and hasClickAction()).assertExists()
    }

    @Test
    fun theLayoutRulesArePure() {
        val ids = listOf("me", "a", "b")
        val grid = MonarchCallControlsLayout.Grid
        val auto = MonarchCallControlsLayout.Automatic
        val speaker = MonarchCallControlsLayout.Speaker
        assertEquals(listOf(1, 1, 2, 2, 2, 3, 3), (0..6).map(MonarchCallStageLayout::gridColumns))
        assertEquals(MonarchCallStageLayout.Grid(ids, 2), MonarchCallStageLayout.automatic("me", ids, grid))
        assertEquals(
            MonarchCallStageLayout.Spotlight("b", listOf("me", "a")),
            MonarchCallStageLayout.automatic("me", ids, grid, pinnedId = "b"),
        )
        assertEquals(
            MonarchCallStageLayout.Grid(ids, 2),
            MonarchCallStageLayout.automatic("me", ids, grid, pinnedId = "gone"),
        )
        assertEquals(
            MonarchCallStageLayout.Spotlight("a", listOf("me", "b")),
            MonarchCallStageLayout.automatic("me", ids, grid, sharerId = "a"),
        )
        assertEquals(
            MonarchCallStageLayout.Grid(ids, 2),
            MonarchCallStageLayout.automatic("me", ids, grid, sharerId = "me"),
        )
        assertEquals(
            MonarchCallStageLayout.Spotlight("a", listOf("me", "b")),
            MonarchCallStageLayout.automatic("me", ids, speaker, activeSpeakerId = "me"),
        )
        assertEquals(
            MonarchCallStageLayout.Spotlight("b", listOf("me", "a")),
            MonarchCallStageLayout.automatic("me", ids, speaker, activeSpeakerId = "b"),
        )
        assertEquals(
            MonarchCallStageLayout.Spotlight("a", listOf("me")),
            MonarchCallStageLayout.automatic("me", listOf("me", "a"), auto),
        )
        assertEquals(
            MonarchCallStageLayout.Grid(listOf("me"), 1),
            MonarchCallStageLayout.automatic("me", listOf("me"), speaker),
        )
    }

    @Test
    fun theStageRulesArePure() {
        val tokens = MonarchCallStageTokens
        assertEquals(2, tokens.columns(3, compact = true, narrow = false))
        assertEquals(2, tokens.columns(3, compact = false, narrow = true))
        assertEquals(3, tokens.columns(3, compact = false, narrow = false))
        assertEquals(1, tokens.columns(0, compact = false, narrow = false))
        assertEquals(
            listOf(CallStageTool.PictureInPicture, CallStageTool.Shrink),
            tokens.tools(MonarchCallStageSurface.Stage, pictureInPicture = true, surfaceChange = true, minimize = true),
        )
        assertEquals(
            listOf(CallStageTool.PictureInPicture, CallStageTool.Expand, CallStageTool.Minimize),
            tokens.tools(
                MonarchCallStageSurface.Docked,
                pictureInPicture = true, surfaceChange = true, minimize = true,
            ),
        )
        assertEquals(
            emptyList(),
            tokens.tools(MonarchCallStageSurface.Pip, pictureInPicture = true, surfaceChange = true, minimize = true),
        )
        assertTrue(tokens.waiting(listOf(me)))
        assertTrue(!tokens.waiting(listOf(me, robin)))
        assertEquals("04:12", tokens.duration(Start, Start + FourMinutes, Locale.US))
        assertEquals("1:00:05", tokens.duration(Start, Start + 3_605_000L, Locale.US))
        assertEquals("00:00", tokens.duration(Start, Start - 5_000L, Locale.US))
        assertTrue(tokens.gap(compact = true) < tokens.gap(compact = false))
        assertTrue(tokens.compact(MonarchCallStageSurface.Pip))
        assertEquals(0.dp < tokens.StripTile, true)
    }
}
