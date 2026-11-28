package dev.monarchui.components.forms

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/** The segmented control has one indicator that slides to the chosen segment, while the layout stays put. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SegmentedIndicatorTest {
    @get:Rule
    val rule = createComposeRule()

    private val colors = MonarchColors(MonarchTokens.light.colors)

    private fun show() {
        rule.setMonarchContent {
            var chosen by remember { mutableIntStateOf(0) }
            MonarchSegmentedControl(listOf("List", "Board", "Grid"), selectedIndex = chosen, onSelect = { chosen = it })
        }
        rule.waitForIdle()
    }

    /** The colour inside the pill of [label], between the pill's start and the text. */
    private fun colourBeside(label: String): Int {
        val text = rule.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val inset = with(rule.density) { 6.dp.toPx() }
        val point = Offset(text.left - inset, text.center.y)
        val image = rule.onRoot().captureToImage()
        val pixels = IntArray(1)
        image.readPixels(pixels, point.x.toInt(), point.y.toInt(), 1, 1)
        return pixels[0]
    }

    private fun Color.argb() = toArgb()

    @Test
    fun theIndicatorStartsUnderTheChosenSegment() {
        show()
        assertEquals(colors.bgSurface.argb(), colourBeside("List"))
        assertEquals(colors.bgMuted.argb(), colourBeside("Board"))
    }

    @Test
    fun choosingASegmentSlidesTheIndicatorThere() {
        show()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Grid").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        // Two frames in, the indicator is on its way: not yet under "Grid".
        assertNotEquals(colors.bgSurface.argb(), colourBeside("Grid"))
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertEquals(colors.bgSurface.argb(), colourBeside("Grid"))
        assertEquals(colors.bgMuted.argb(), colourBeside("List"))
    }

    @Test
    fun choosingASegmentDoesNotMoveTheLayout() {
        show()
        val before = listOf("List", "Board", "Grid").map {
            rule.onNodeWithText(it, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        }
        rule.onNodeWithText("Board").performClick()
        rule.waitForIdle()
        val after = listOf("List", "Board", "Grid").map {
            rule.onNodeWithText(it, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        }
        assertEquals(before, after)
    }

    @Test
    fun withFillWidthTheIndicatorFillsTheChosenSegmentsShare() {
        rule.setMonarchContent {
            MonarchSegmentedControl(
                options = listOf("Open", "Resolved"),
                selectedIndex = 0,
                onSelect = {},
                modifier = Modifier.width(320.dp).testTag("control"),
                fillWidth = true,
            )
        }
        rule.waitForIdle()
        val control = rule.onNodeWithTag("control").fetchSemanticsNode().boundsInRoot
        val text = rule.onNodeWithText("Open", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        // Halfway between the start of the control and the text: inside the half the first segment owns.
        val point = Offset((control.left + text.left) / 2, text.center.y)
        val pixels = IntArray(1)
        rule.onRoot().captureToImage().readPixels(pixels, point.x.toInt(), point.y.toInt(), 1, 1)
        assertEquals(colors.bgSurface.argb(), pixels[0])
    }
}
