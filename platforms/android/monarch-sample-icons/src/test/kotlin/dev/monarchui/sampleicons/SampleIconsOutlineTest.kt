package dev.monarchui.sampleicons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Every sample icon is an outline glyph (PREMIUM.md rule 9: never a filled glyph next to an outlined one).
 *
 * Each icon is drawn black on white at 96px (4px per unit of its 24-unit grid), and the test measures the side of
 * the largest square of solid ink in it. Strokes are 2 units wide; the widest outline details (the stripe of the
 * card, the ears of the robot) reach 4 units. Solid glyphs reach 5 to 7 units (the solid mic, warning triangle,
 * error disc and star). An icon fails above [MAX_SOLID_UNITS].
 */
@RunWith(RobolectricTestRunner::class)
class SampleIconsOutlineTest {
    @get:Rule
    val rule = createComposeRule()

    private var icon by mutableStateOf<ImageVector?>(null)
    private var started = false

    @Test
    fun everySampleIconIsAnOutlineGlyph() {
        val icons = SampleIcons::class.java.declaredMethods
            .filter {
                it.returnType == ImageVector::class.java && it.parameterCount == 0 &&
                    java.lang.reflect.Modifier.isPublic(it.modifiers)
            }
            .associate { it.name.removePrefix("get") to it.invoke(SampleIcons) as ImageVector }
            .filterKeys { it !in SolidByDesign }
        assertTrue(icons.size > 50, "found ${icons.size} icons")
        val solid = icons.mapValues { (_, vector) -> largestSolidSquare(vector) }.filterValues { it > MAX_SOLID_UNITS }
        assertTrue(solid.isEmpty(), "filled glyphs (largest solid square, in icon units): $solid")
    }

    @Test
    fun theCheckFindsTheSolidGlyphsOfTheOutlinedSet() {
        // These four Material "Outlined" icons are solid shapes; the check must catch each, or it proves nothing.
        listOf(Icons.Outlined.Star, Icons.Outlined.Error, Icons.Outlined.Warning, Icons.Outlined.Mic).forEach {
            val units = largestSolidSquare(it)
            assertTrue(units > MAX_SOLID_UNITS, "${it.name}: $units")
        }
    }

    /** The side of the largest all-ink square, in icon units (1 unit = 4px). */
    private fun largestSolidSquare(vector: ImageVector): Float {
        if (!started) {
            started = true
            icon = vector
            rule.setContent {
                icon?.let {
                    Layout(
                        modifier = Modifier
                            .testTag("icon")
                            .drawBehind { drawRect(Color.White) }
                            .paint(
                                rememberVectorPainter(it),
                                contentScale = ContentScale.Fit,
                                colorFilter = ColorFilter.tint(Color.Black),
                            ),
                    ) { _, _ -> layout(SIZE_PX, SIZE_PX) {} }
                }
            }
        } else {
            rule.runOnIdle { icon = vector }
        }
        // Robolectric draws nothing for a first frame that follows another test: the first capture draws the
        // pending frame, the second one is the frame itself. If it is still blank, recompose and try again.
        repeat(FRAME_ATTEMPTS) {
            rule.waitForIdle()
            rule.onNodeWithTag("icon").captureToImage()
            val side = rule.onNodeWithTag("icon").captureToImage().toPixelMap().largestInkSquare()
            if (side > 0) return side.toFloat() * GRID_UNITS / SIZE_PX
            rule.runOnIdle { icon = null }
            rule.waitForIdle()
            rule.runOnIdle { icon = vector }
        }
        fail("${vector.name} drew nothing")
    }

    private fun PixelMap.largestInkSquare(): Int {
        // current[x + 1] is the side of the largest ink square whose bottom-right corner is (x, y).
        var previous = IntArray(width + 1)
        var best = 0
        for (y in 0 until height) {
            val current = IntArray(width + 1)
            for (x in 0 until width) {
                if (this[x, y].red < INK_LEVEL) {
                    current[x + 1] = 1 + minOf(previous[x], previous[x + 1], current[x])
                    best = maxOf(best, current[x + 1])
                }
            }
            previous = current
        }
        return best
    }

    private companion object {
        const val SIZE_PX = 96
        const val GRID_UNITS = 24
        const val INK_LEVEL = 0.5f
        const val FRAME_ATTEMPTS = 3

        /** Measured with this renderer: outline glyphs reach 4 units, solid ones start at 5. */
        const val MAX_SOLID_UNITS = 4.5f

        /** Glyphs whose solid part is the drawing itself, not a filled style: the arrowhead of Undo. */
        val SolidByDesign = setOf("Undo", "Partial", "Shrink")
    }
}
