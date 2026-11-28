package com.rookhq.monarch.components.overlays

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.overlays.lightbox.MonarchLightbox
import com.rookhq.monarch.components.overlays.lightbox.MonarchLightboxContent
import com.rookhq.monarch.components.overlays.lightbox.MonarchLightboxItem
import com.rookhq.monarch.components.overlays.lightbox.MonarchLightboxLabels
import com.rookhq.monarch.components.overlays.lightbox.MonarchLightboxTokens
import com.rookhq.monarch.components.overlays.lightbox.handleKey
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.onNodeWithTextOrDescription
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Composable
private fun photos(count: Int = 3, caption: String? = "Sketch from the planning day"): List<MonarchLightboxItem> {
    val painter = ColorPainter(MonarchTheme.colors.bgMuted)
    return List(count) { index ->
        MonarchLightboxItem(image = painter, alt = "Photo ${index + 1}", caption = caption)
    }
}

/** A viewer that ignores its callbacks, for the fixtures. */
@Composable
private fun Viewer(
    items: List<MonarchLightboxItem>,
    selectedIndex: Int = 0,
    looping: Boolean = false,
    maximumZoom: Float = 4f,
    onShare: ((Int) -> Unit)? = null,
) {
    MonarchLightbox(
        items = items,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = {},
        onDismissRequest = {},
        looping = looping,
        maximumZoom = maximumZoom,
        onShare = onShare,
    )
}

internal object MonarchLightboxFixtures : ComponentFixtures {
    override val id = "lightbox"
    override val fixtures = listOf(
        fixture("first of three, with a caption and share") {
            Viewer(photos(), onShare = {})
        },
        fixture("middle item, looping") {
            Viewer(photos(), selectedIndex = 1, looping = true)
        },
        fixture("single item without a caption") {
            Viewer(photos(1, caption = null))
        },
        fixture("unavailable item") {
            MonarchLightbox(
                listOf(MonarchLightboxItem(image = null, alt = "Team photo")),
                selectedIndex = 0,
                onSelectedIndexChange = {},
                onDismissRequest = {},
            )
        },
        fixture("no items") {
            MonarchLightbox(emptyList(), selectedIndex = 0, onSelectedIndexChange = {}, onDismissRequest = {})
        },
        fixture("video slot") {
            MonarchLightbox(
                listOf(MonarchLightboxItem(image = null, alt = "Product demo", video = { MonarchText("Player") })),
                selectedIndex = 0,
                onSelectedIndexChange = {},
                onDismissRequest = {},
            )
        },
        fixture("zoom off") {
            Viewer(photos(2), maximumZoom = 1f)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchLightboxTest {
    @get:Rule
    val rule = createComposeRule()

    private var dismissed = 0
    private var shared = mutableListOf<Int>()

    private fun showViewer(
        count: Int = 3,
        start: Int = 0,
        looping: Boolean = false,
        maximumZoom: Float = 4f,
        labels: MonarchLightboxLabels = MonarchLightboxLabels(),
        environment: MonarchTestEnvironment = MonarchTestEnvironment(),
        caption: String? = "Sketch from the planning day",
    ) {
        rule.setMonarchContent(environment) {
            var index by remember { mutableIntStateOf(start) }
            Box {
                MonarchLightbox(
                    items = photos(count, caption),
                    selectedIndex = index,
                    onSelectedIndexChange = { index = it },
                    onDismissRequest = { dismissed++ },
                    looping = looping,
                    maximumZoom = maximumZoom,
                    onShare = { shared += it },
                    labels = labels,
                )
            }
        }
    }

    @Test
    fun theViewerIsANamedPaneThatAnnouncesThePositionAndTheItem() {
        showViewer()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Media viewer")).assertExists()
        rule.onNodeWithText("1 of 3")
            .assertContentDescriptionEquals("1 of 3: Photo 1")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithContentDescription("Photo 1")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
    }

    @Test
    fun nextAndPreviousStepThroughTheItems() {
        showViewer()
        rule.onNodeWithTextOrDescription("Previous").assertIsNotEnabled()
        rule.onNodeWithTextOrDescription("Next").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("2 of 3").assertContentDescriptionEquals("2 of 3: Photo 2")
        rule.onNodeWithTextOrDescription("Previous").assertIsEnabled().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("1 of 3").assertExists()
    }

    @Test
    fun withoutLoopingTheLastItemHasNoNext() {
        showViewer(start = 2)
        rule.onNodeWithText("3 of 3").assertExists()
        rule.onNodeWithTextOrDescription("Next").assertIsNotEnabled()
    }

    @Test
    fun loopingWrapsFromTheFirstItemToTheLast() {
        showViewer(looping = true)
        rule.onNodeWithTextOrDescription("Previous").assertIsEnabled().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("3 of 3").assertExists()
    }

    @Test
    fun anIndexOutsideTheItemsShowsTheNearestItem() {
        showViewer(start = 9)
        rule.onNodeWithText("3 of 3").assertExists()
    }

    @Test
    fun swipingShowsTheNextItem() {
        showViewer()
        rule.onNodeWithContentDescription("Photo 1").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithText("2 of 3").assertExists()
    }

    /**
     * The viewer's content without its dialog window. The unit-test manifest
     * does not declare supportsRtl, so a dialog window's layout direction and
     * key focus are not those of a real app; the content is tested directly.
     */
    private fun showContent(environment: MonarchTestEnvironment = MonarchTestEnvironment()) {
        rule.setMonarchContent(environment) {
            var index by remember { mutableIntStateOf(0) }
            MonarchLightboxContent(
                items = photos(3),
                selectedIndex = index,
                onSelectedIndexChange = { index = it },
                onDismissRequest = { dismissed++ },
                looping = false,
                maximumZoom = 4f,
                onShare = null,
                labels = MonarchLightboxLabels(),
            )
        }
    }

    private fun keyDown(code: Int) = KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, code))

    @Test
    fun theArrowKeysStepMirroredZoomKeysZoomAndEscapeCloses() {
        val steps = mutableListOf<Int>()
        val zooms = mutableListOf<Float>()
        var closes = 0
        fun press(
            code: Int,
            direction: LayoutDirection = LayoutDirection.Ltr,
            zoom: Float = 1f,
            zoomable: Boolean = true,
        ) = handleKey(keyDown(code), direction, zoomable, zoom, { steps += it }, { zooms += it }, { closes++ })

        assertTrue(press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT))
        assertTrue(press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT, LayoutDirection.Rtl))
        assertTrue(press(AndroidKeyEvent.KEYCODE_DPAD_LEFT))
        assertEquals(listOf(1, -1, -1), steps)
        assertFalse(press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT, zoom = 2f), "while zoomed the arrows pan instead")
        assertTrue(press(AndroidKeyEvent.KEYCODE_EQUALS, zoom = 1.5f))
        assertTrue(press(AndroidKeyEvent.KEYCODE_MINUS, zoom = 1.5f))
        assertTrue(press(AndroidKeyEvent.KEYCODE_0, zoom = 3f))
        assertEquals(listOf(2f, 1f, 1f), zooms)
        assertFalse(press(AndroidKeyEvent.KEYCODE_EQUALS, zoomable = false))
        assertTrue(press(AndroidKeyEvent.KEYCODE_ESCAPE))
        assertEquals(1, closes)
        assertFalse(press(AndroidKeyEvent.KEYCODE_A))
        val up = KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_UP, AndroidKeyEvent.KEYCODE_ESCAPE))
        assertFalse(handleKey(up, LayoutDirection.Ltr, true, 1f, {}, {}, { closes++ }))
    }

    @Test
    fun theZoomButtonsChangeAndResetTheLevel() {
        showViewer()
        val image = rule.onNodeWithContentDescription("Photo 1")
        assertEquals("100%", image.stateDescription())
        rule.onNodeWithContentDescription("Zoom out").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Reset zoom").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Zoom in").performClick()
        rule.waitForIdle()
        assertEquals("150%", rule.onNodeWithContentDescription("Photo 1").stateDescription())
        rule.onNodeWithText("150%").assertIsEnabled()
        rule.onNodeWithContentDescription("Reset zoom").performClick()
        rule.waitForIdle()
        assertEquals("100%", rule.onNodeWithContentDescription("Photo 1").stateDescription())
    }

    @Test
    fun zoomInStopsAtTheMaximum() {
        showViewer(maximumZoom = 1.5f)
        rule.onNodeWithContentDescription("Zoom in").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Zoom in").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Zoom out").assertIsEnabled()
    }

    @Test
    fun aMaximumOfOneTurnsZoomOff() {
        showViewer(maximumZoom = 1f)
        rule.onNodeWithContentDescription("Zoom in").assertDoesNotExist()
        assertNull(rule.onNodeWithContentDescription("Photo 1").stateDescription())
    }

    @Test
    fun closeAndShareCallBack() {
        showViewer(start = 1)
        rule.onNodeWithTextOrDescription("Share").performClick()
        rule.onNodeWithTextOrDescription("Close").performClick()
        assertEquals(listOf(1), shared)
        assertEquals(1, dismissed)
    }

    @Test
    fun aSingleItemHasNoPositionAndNoSteps() {
        showViewer(count = 1)
        rule.onNodeWithText("1 of 1").assertDoesNotExist()
        rule.onNodeWithTextOrDescription("Next").assertDoesNotExist()
        rule.onNodeWithTextOrDescription("Previous").assertDoesNotExist()
    }

    @Test
    fun anItemWithoutAPictureOrVideoSaysItCannotBeShown() {
        rule.setMonarchContent {
            MonarchLightbox(
                listOf(MonarchLightboxItem(image = null, alt = "Team photo")),
                selectedIndex = 0,
                onSelectedIndexChange = {},
                onDismissRequest = {},
            )
        }
        rule.onNodeWithText("This file can’t be shown.").assertExists()
        rule.onNodeWithContentDescription("Zoom in").assertDoesNotExist()
    }

    @Test
    fun aVideoIsTheAppsPlayerNamedByItsAltText() {
        rule.setMonarchContent {
            MonarchLightbox(
                listOf(MonarchLightboxItem(image = null, alt = "Product demo", video = { MonarchText("Player") })),
                selectedIndex = 0,
                onSelectedIndexChange = {},
                onDismissRequest = {},
            )
        }
        rule.onNodeWithText("Player").assertExists()
        rule.onNodeWithContentDescription("Product demo").assertExists()
        rule.onNodeWithContentDescription("Zoom in").assertDoesNotExist()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        showViewer(
            labels = MonarchLightboxLabels(
                title = "Visionneuse",
                next = "Suivant",
                close = "Fermer",
                zoomIn = "Agrandir",
                position = { number, count -> "$number sur $count" },
            ),
        )
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Visionneuse")).assertExists()
        rule.onNodeWithText("1 sur 3").assertExists()
        rule.onNodeWithTextOrDescription("Suivant").assertExists()
        rule.onNodeWithTextOrDescription("Fermer").assertExists()
        rule.onNodeWithContentDescription("Agrandir").assertExists()
    }

    @Test
    fun everyButtonHasA48dpTarget() {
        showViewer(start = 1)
        listOf("Previous", "Next", "Share", "Close").forEach {
            rule.onNodeWithTextOrDescription(it).assertTouchTargetAtLeast48dp(rule.density)
        }
        listOf("Zoom in", "Zoom out", "Reset zoom").forEach {
            rule.onNodeWithContentDescription(it).assertTouchTargetAtLeast48dp(rule.density)
        }
    }

    @Test
    fun previousComesBeforeNextInBothDirections() {
        val environment = MonarchTestEnvironment()
        showContent(environment)
        rule.onNodeWithTextOrDescription("Next").performClick()
        for (direction in LayoutDirection.entries) {
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithTextOrDescription("Previous", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithTextOrDescription("Next", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun theCaptionIsFullyShownAtTheLargestFontScale() {
        showViewer(environment = MonarchTestEnvironment(fontScale = LargestFontScale), caption = "Lisbon")
        rule.onNodeWithText("Lisbon", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun stepsWrapOnlyWhenLooping() {
        assertEquals(1, MonarchLightboxTokens.step(0, 1, 3, looping = false))
        assertNull(MonarchLightboxTokens.step(2, 1, 3, looping = false))
        assertEquals(0, MonarchLightboxTokens.step(2, 1, 3, looping = true))
        assertEquals(2, MonarchLightboxTokens.step(0, -1, 3, looping = true))
        assertNull(MonarchLightboxTokens.step(0, 1, 1, looping = true))
        assertEquals(2, MonarchLightboxTokens.clampIndex(9, 3))
        assertEquals(0, MonarchLightboxTokens.clampIndex(-1, 3))
        assertEquals(0, MonarchLightboxTokens.clampIndex(4, 0))
    }

    @Test
    fun zoomStaysBetweenOneAndTheLimitAndThePanStaysInsideTheImage() {
        assertEquals(1f, MonarchLightboxTokens.zoomLimit(0.5f))
        assertEquals(4f, MonarchLightboxTokens.zoomLimit(4f))
        assertEquals(1f, MonarchLightboxTokens.clampZoom(0.2f, 4f))
        assertEquals(4f, MonarchLightboxTokens.clampZoom(9f, 4f))
        assertEquals(2f, MonarchLightboxTokens.doubleTapZoom(1f, 4f))
        assertEquals(1.5f, MonarchLightboxTokens.doubleTapZoom(1f, 1.5f))
        assertEquals(1f, MonarchLightboxTokens.doubleTapZoom(3f, 4f))
        val size = IntSize(200, 100)
        assertEquals(Offset(100f, -50f), MonarchLightboxTokens.clampOffset(Offset(500f, -500f), 2f, size))
        assertEquals(Offset.Zero, MonarchLightboxTokens.clampOffset(Offset(30f, 30f), 1f, size))
    }
}
