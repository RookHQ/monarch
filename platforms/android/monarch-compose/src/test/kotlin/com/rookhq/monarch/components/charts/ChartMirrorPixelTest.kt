package com.rookhq.monarch.components.charts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.gauge.MonarchGauge
import com.rookhq.monarch.components.charts.sparkline.MonarchSparkline
import com.rookhq.monarch.components.charts.sparkline.MonarchSparklineVariant
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The Canvas charts draw from the start edge: right to left, the drawing is
 * mirrored. Checked on the pixels, because the drawing is not in the
 * semantics tree.
 */
@RunWith(RobolectricTestRunner::class)
class ChartMirrorPixelTest {
    @get:Rule
    val rule = createComposeRule()

    private val colors = MonarchColors(MonarchTokens.light.colors)

    private fun capture(environment: MonarchTestEnvironment, direction: LayoutDirection): PixelMap {
        environment.layoutDirection = direction
        rule.waitForIdle()
        return rule.onNodeWithTag(Tag).captureToImage().toPixelMap()
    }

    private fun near(a: Color, b: Color) =
        abs(a.red - b.red) < Tolerance && abs(a.green - b.green) < Tolerance && abs(a.blue - b.blue) < Tolerance

    /** The share of pixels in the start half (left half) of [image] that are [color], over both halves. */
    private fun leftShare(image: PixelMap, color: Color): Float {
        var left = 0
        var right = 0
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                if (near(image[x, y], color)) if (x < image.width / 2) left++ else right++
            }
        }
        assertTrue(left + right > 0, "The chart is drawn in the expected color")
        return left.toFloat() / (left + right)
    }

    @Test
    fun theSparklineMirrorsItsDrawingRightToLeft() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            Box(Modifier.testTag(Tag).size(Width, Height)) {
                // Low on the start side, high on the end side; the area fill makes the halves easy to count.
                MonarchSparkline(
                    values = listOf(1.0, 1.0, 1.0, 9.0, 10.0, 10.0),
                    tone = MonarchTone.Success,
                    variant = MonarchSparklineVariant.Area,
                    fullWidth = true,
                    showLastPoint = false,
                )
            }
        }
        val ltr = capture(environment, LayoutDirection.Ltr)
        val rtl = capture(environment, LayoutDirection.Rtl)
        // The top half of each image: only the high end of the line and its fill reach it.
        val ltrLeft = differentFromBackground(ltr, topRows(ltr))
        val rtlLeft = differentFromBackground(rtl, topRows(rtl))
        assertTrue(ltrLeft < Half, "Left to right, the high values are on the right: $ltrLeft")
        assertTrue(rtlLeft > Half, "Right to left, the high values are on the left: $rtlLeft")
    }

    @Test
    fun theGaugeFillsFromTheStartEdge() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            Box(Modifier.testTag(Tag).size(Height * 2)) {
                MonarchGauge(value = 25.0, tone = MonarchTone.Success, showRange = false)
            }
        }
        val fill = colors.successSolid
        val ltr = leftShare(capture(environment, LayoutDirection.Ltr), fill)
        val rtl = leftShare(capture(environment, LayoutDirection.Rtl), fill)
        assertTrue(ltr > Half, "Left to right, a quarter-full dial is filled on the left: $ltr")
        assertTrue(rtl < Half, "Right to left, it is filled on the right: $rtl")
    }

    /** The rows of the top half. */
    private fun topRows(image: PixelMap): IntRange = 0 until image.height / 2

    /** The share of non-background pixels of [rows] that are in the left half. */
    private fun differentFromBackground(image: PixelMap, rows: IntRange): Float {
        // The top of the middle column: between the low and the high values, nothing is drawn there.
        val background = image[image.width / 2, 0]
        var left = 0
        var right = 0
        for (x in 0 until image.width) {
            for (y in rows) {
                if (!near(image[x, y], background)) if (x < image.width / 2) left++ else right++
            }
        }
        assertTrue(left + right > 0, "The high end of the sparkline reaches the top half")
        return left.toFloat() / (left + right)
    }

    private companion object {
        const val Tag = "chart"
        const val Tolerance = 0.06f
        const val Half = 0.5f
        val Width = 200.dp
        val Height = 80.dp
    }
}
