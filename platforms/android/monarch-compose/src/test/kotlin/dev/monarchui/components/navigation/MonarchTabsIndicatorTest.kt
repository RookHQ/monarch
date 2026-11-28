package dev.monarchui.components.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.monarchui.components.navigation.tabs.MonarchTabs
import dev.monarchui.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The indicator of `MonarchTabs` slides from the old tab to the new one instead of jumping (PREMIUM rule 13). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MonarchTabsIndicatorTest {
    @get:Rule
    val rule = createComposeRule()

    /** The left edge, in px, of the accent bar along the bottom row of the tabs, or `null` when none is drawn. */
    private fun indicatorLeft(): Int? {
        val pixels = rule.onNodeWithTag("tabs").captureToImage().toPixelMap()
        val y = pixels.height - 1
        return (0 until pixels.width).firstOrNull { x ->
            val c = pixels[x, y]
            c.red - c.green > ACCENT_MARGIN && c.red - c.blue > ACCENT_MARGIN
        }
    }

    @Test
    fun theIndicatorSlidesToTheNewTabAndSettlesUnderIt() {
        rule.setMonarchContent {
            var selected by remember { mutableIntStateOf(0) }
            MonarchTabs(
                selectedIndex = selected,
                onSelectedIndexChange = { selected = it },
                modifier = Modifier.testTag("tabs"),
            ) {
                tab("Overview")
                tab("Activity")
                tab("Members")
            }
        }
        val start = assertNotNull(indicatorLeft(), "indicator under the first tab")
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Members").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(FRAME_STEP_MS)
        val moving = assertNotNull(indicatorLeft(), "indicator while moving")
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        val end = assertNotNull(indicatorLeft(), "indicator under the third tab")
        assertTrue(end > start, "the indicator ends further right: $start -> $end")
        assertTrue(moving in (start + 1) until end, "mid-way it is between the tabs: $start, $moving, $end")
    }

    private companion object {
        const val ACCENT_MARGIN = 0.25f
        const val FRAME_STEP_MS = 64L
    }
}
