package dev.monarchui.components.collaboration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.collaboration.screenshareview.MonarchScreenShareView
import dev.monarchui.components.collaboration.screenshareview.MonarchScreenShareViewLabels
import dev.monarchui.components.collaboration.screenshareview.MonarchScreenShareViewTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.onNodeWithTextOrDescription
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Composable
private fun TestVideo() {
    Box(modifier = Modifier.fillMaxSize().testTag("video").background(MonarchTheme.colors.bgMuted))
}

// The test window is 320 by 470dp and fixtures do not scroll, so each fixture fits at font scale 2.0.
internal object MonarchScreenShareViewFixtures : ComponentFixtures {
    override val id = "screen-share-view"
    override val fixtures = listOf(
        fixture("zoom controls") {
            MonarchScreenShareView(presenterName = "Ana", zoom = 1.5f, onZoomChange = {}) { TestVideo() }
        },
        fixture("picture in picture and full screen, active") {
            MonarchScreenShareView(
                presenterName = "Ana",
                pictureInPictureActive = true,
                fullScreen = true,
                showControls = true,
                onPictureInPicture = {},
                onFullScreen = {},
                labels = MonarchScreenShareViewLabels(zoomIn = "+", zoomOut = "-", fit = "Fit"),
            ) { TestVideo() }
        },
        fixture("local, preview hidden, stop sharing") {
            MonarchScreenShareView(presenterName = "Ana", local = true, onStopSharing = {}) { TestVideo() }
        },
        fixture("waiting") { MonarchScreenShareView(presenterName = "Ana", waiting = true) { TestVideo() } },
        fixture("controls hidden, long name") {
            MonarchScreenShareView(presenterName = "Maria Fernanda de los Santos", showControls = false) {
                TestVideo()
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchScreenShareViewTest {
    @get:Rule
    val rule = createComposeRule()

    private var zoom by mutableFloatStateOf(1f)

    private fun show(
        local: Boolean = false,
        waiting: Boolean = false,
        pictureInPictureActive: Boolean = false,
        onPictureInPicture: (() -> Unit)? = null,
        onStopSharing: (() -> Unit)? = null,
        showControls: Boolean = true,
    ) {
        rule.setMonarchContent {
            MonarchScreenShareView(
                presenterName = "Ana",
                zoom = zoom,
                onZoomChange = { zoom = it },
                local = local,
                waiting = waiting,
                pictureInPictureActive = pictureInPictureActive,
                showControls = showControls,
                onPictureInPicture = onPictureInPicture,
                onStopSharing = onStopSharing,
            ) { TestVideo() }
        }
    }

    private fun click(text: String) =
        rule.onNodeWithTextOrDescription(text).performSemanticsAction(SemanticsActions.OnClick)

    @Test
    fun thePictureIsOneImageNamedAfterThePresenterWithTheZoomAsItsState() {
        show()
        rule.onNodeWithText("Ana is presenting").assertExists()
        val picture = rule.onNodeWithContentDescription("Screen shared by Ana")
        picture.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
        assertEquals("100%", picture.stateDescription())
    }

    @Test
    fun theButtonsStepThroughTheZoomLevels() {
        show()
        rule.onNodeWithTextOrDescription("Zoom out").assertIsNotEnabled()
        rule.onNodeWithTextOrDescription("Fit to view").assertIsNotEnabled()
        val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        rule.onNodeWithText("100%").assert(polite)
        click("Zoom in")
        assertEquals(1.5f, zoom)
        rule.onNodeWithText("150%").assertExists()
        click("Zoom in")
        click("Zoom in")
        assertEquals(3f, zoom)
        rule.onNodeWithTextOrDescription("Zoom in").assertIsNotEnabled()
        click("Zoom out")
        assertEquals(2f, zoom)
        click("Fit to view")
        assertEquals(1f, zoom)
    }

    @Test
    fun talkBackCanZoomThePictureWithItsActions() {
        show()
        val picture = rule.onNodeWithContentDescription("Screen shared by Ana")
        val actions = picture.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions).orEmpty()
        assertEquals(listOf("Zoom in"), actions.map { it.label })
        actions.single().action()
        rule.waitForIdle()
        assertEquals(1.5f, zoom)
        val next = picture.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions).orEmpty()
        assertEquals(listOf("Zoom in", "Zoom out"), next.map { it.label })
    }

    @Test
    fun aDoubleTapZoomsInOneStep() {
        show()
        rule.onNodeWithContentDescription("Screen shared by Ana").performTouchInput { doubleClick() }
        rule.waitForIdle()
        assertEquals(1.5f, zoom)
    }

    @Test
    fun aZoomOutsideTheRangeIsClamped() {
        zoom = 5f
        show()
        assertEquals("300%", rule.onNodeWithContentDescription("Screen shared by Ana").stateDescription())
    }

    @Test
    fun yourOwnShareIsHiddenUntilAskedForAndCanBeStopped() {
        var stops = 0
        show(local = true, onStopSharing = { stops++ })
        rule.onNodeWithText("You are presenting").assertExists()
        rule.onAllNodesWithTag("video", useUnmergedTree = true).assertCountEquals(0)
        rule.onAllNodes(hasText("Zoom in") or hasContentDescription("Zoom in")).assertCountEquals(0)
        click("Stop sharing")
        assertEquals(1, stops)
        click("Show preview")
        rule.onNodeWithTag("video", useUnmergedTree = true).assertExists()
    }

    @Test
    fun yourOwnShareOffersNoPreviewWhileWaiting() {
        show(local = true, waiting = true)
        rule.onNodeWithText("Your screen is being shared. The preview is hidden to avoid a mirror effect.")
            .assertExists()
        rule.onAllNodesWithText("Show preview").assertCountEquals(0)
    }

    @Test
    fun theWaitingTextReplacesThePicture() {
        show(waiting = true)
        rule.onNodeWithText("Waiting for the shared screen…").assertExists()
        rule.onAllNodesWithTag("video", useUnmergedTree = true).assertCountEquals(0)
        rule.onAllNodes(hasText("Zoom in") or hasContentDescription("Zoom in")).assertCountEquals(0)
    }

    @Test
    fun thePictureInPictureButtonShowsItsState() {
        var calls = 0
        show(pictureInPictureActive = true, onPictureInPicture = { calls++ })
        rule.onNodeWithText("Exit picture in picture")
            .assertIsSelected()
            .assertTouchTargetAtLeast48dp(rule.density)
        click("Exit picture in picture")
        assertEquals(1, calls)
    }

    @Test
    fun theControlsCanBeHidden() {
        show(showControls = false, onPictureInPicture = {})
        rule.onAllNodes(hasText("Zoom in") or hasContentDescription("Zoom in")).assertCountEquals(0)
        rule.onAllNodesWithText("Picture in picture").assertCountEquals(0)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchScreenShareView(
                presenterName = "Ana",
                labels = MonarchScreenShareViewLabels(
                    presenting = { "$it präsentiert" },
                    region = { "Bildschirm von $it" },
                    zoomIn = "Vergrößern",
                    zoomLevel = { "$it Prozent" },
                ),
            ) { TestVideo() }
        }
        rule.onNodeWithText("Ana präsentiert").assertExists()
        rule.onNodeWithTextOrDescription("Vergrößern").assertExists()
        rule.onNodeWithText("100 Prozent").assertExists()
        rule.onNodeWithContentDescription("Bildschirm von Ana").assertExists()
    }

    @Test
    fun thePresenterComesBeforeTheControlsInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchScreenShareView(presenterName = "Ana") { TestVideo() }
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Ana is presenting", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithTextOrDescription("Zoom out"), direction)
        }
    }

    @Test
    fun thePresenterWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchScreenShareView(presenterName = "Maria Fernanda de los Santos", showControls = false) {
                TestVideo()
            }
        }
        rule.onNodeWithText("Maria Fernanda de los Santos is presenting", useUnmergedTree = true)
            .assertTextFullyVisible()
    }

    @Test
    fun theZoomRulesMatchTheWebAndIos() {
        val t = MonarchScreenShareViewTokens
        assertEquals(1.5f, t.zoomIn(1f))
        assertEquals(2f, t.zoomIn(1.6f))
        assertNull(t.zoomIn(3f))
        assertEquals(2f, t.zoomOut(3f))
        assertNull(t.zoomOut(1f))
        assertEquals(1f, t.doubleTap(3f))
        assertEquals(3f, t.clamp(7f))
        assertEquals(1f, t.clamp(0.2f))
        assertEquals("150%", t.percentText(1.5f, Locale.US))
        val size = Size(200f, 100f)
        assertEquals(Offset(50f, -25f), t.clampOffset(Offset(80f, -90f), size, zoom = 1.5f))
        assertEquals(Offset.Zero, t.clampOffset(Offset(80f, -90f), size, zoom = 1f))
    }

    @Test
    fun theFrameThePictureAndTheNoticeUseTheirRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val frame = MonarchScreenShareViewTokens.frameColors(colors)
            assertEquals(colors.bgSurface, frame.container, theme.id)
            assertEquals(colors.borderDefault, frame.border, theme.id)
            val viewport = MonarchScreenShareViewTokens.viewportColors(colors)
            assertEquals(colors.bgInverse, viewport.container, theme.id)
            assertEquals(colors.textInverseMuted, viewport.content, theme.id)
            val notice = MonarchScreenShareViewTokens.noticeColors(colors)
            assertEquals(colors.bgSubtle, notice.container, theme.id)
            assertEquals(colors.textMuted, notice.content, theme.id)
        }
    }
}
