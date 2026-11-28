package com.rookhq.monarch.testing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** The matrix axes catch what they are meant to catch, and pass what they should. */
@RunWith(RobolectricTestRunner::class)
class MatrixAxesTest {
    @get:Rule
    val rule = createComposeRule()

    private val checker = MatrixChecker(rule, "frame")

    /**
     * Renders [content], then recomposes it once. Robolectric draws nothing for a first frame that follows
     * another test in the same JVM, so the contrast axis would measure an empty frame; a second frame is drawn.
     */
    private fun show(
        environment: MonarchTestEnvironment = MonarchTestEnvironment(),
        content: @Composable () -> Unit,
    ) {
        var tick by mutableIntStateOf(0)
        rule.setMonarchContent(environment) { Box(Modifier.testTag("frame")) { key(tick) { content() } } }
        rule.waitForIdle()
        tick++
        rule.waitForIdle()
    }

    @Test
    fun anEllipsizedLabelFailsTheFontScaleAxis() {
        show(MonarchTestEnvironment(fontScale = LargestFontScale)) {
            Text(
                "A label that cannot fit in so little room",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(60.dp),
            )
        }
        assertFailsWith<AssertionError> { checker.textNotCut("ellipsized") }
    }

    @Test
    fun aLabelThatWrapsPassesTheFontScaleAxis() {
        show(MonarchTestEnvironment(fontScale = LargestFontScale)) {
            Text("A label that wraps onto several lines", modifier = Modifier.width(120.dp))
        }
        checker.textNotCut("wrapped")
    }

    @Test
    fun paleTextOnWhiteFailsTheContrastAxis() {
        show { Text("Pale", color = Color(0xFFDDDDDD)) }
        assertFailsWith<AssertionError> { checker.textContrast("pale") }
    }

    private fun showInfoText(color: Color) = show {
        Box(Modifier.background(Color.White)) {
            Text("Info", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color)
        }
    }

    @Test
    fun smallThinTextOnAKnown4Point5PairPasses() {
        // 767676 on white is 4.54:1: the axis measures the colour the text is drawn in, not an anti-aliased pixel.
        showInfoText(Color(0xFF767676))
        checker.textContrast("12sp at 4.54")
    }

    @Test
    fun smallThinTextJustBelow4Point5Fails() {
        showInfoText(Color(0xFF7A7A7A))
        assertFailsWith<AssertionError> { checker.textContrast("12sp at 4.3") }
    }

    @Test
    fun defaultTextPassesTheContrastAxisInEveryTheme() {
        val environment = MonarchTestEnvironment()
        show(environment) { Text("Default text") }
        for (theme in AllThemes) {
            environment.tokens = theme
            rule.waitForIdle()
            checker.textContrast("default in ${theme.id}")
        }
    }

    @Test
    fun aSmallTapTargetFailsTheTouchAxis() {
        show { Box(Modifier.size(24.dp).clickable { }) }
        assertFailsWith<AssertionError> { checker.touchTargets("small") }
    }

    @Test
    fun theContrastMathMatchesWcag() {
        assertEquals(21.0, contrastRatio(Color.Black, Color.White), 0.01)
        assertEquals(1.0, contrastRatio(Color.White, Color.White), 0.001)
        assertTrue(contrastRatio(Color(0xFF767676), Color.White) in 4.5..4.6)
    }

    @Test
    fun theDefaultAxesAreOnlyTheTouchTargetAndStrictIsAll() {
        assertEquals(MatrixAxes(touchTarget = true, fontScaleNoClipping = false, textContrast = false), MatrixAxes())
        val strict = MatrixAxes.Strict
        assertTrue(strict.fontScaleNoClipping && strict.textContrast && strict.touchTarget)
    }
}
