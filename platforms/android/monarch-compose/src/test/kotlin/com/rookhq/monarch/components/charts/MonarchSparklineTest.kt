package com.rookhq.monarch.components.charts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.sparkline.MonarchSparkline
import com.rookhq.monarch.components.charts.sparkline.MonarchSparklineFrame
import com.rookhq.monarch.components.charts.sparkline.MonarchSparklineGeometry
import com.rookhq.monarch.components.charts.sparkline.MonarchSparklineTokens
import com.rookhq.monarch.components.charts.sparkline.MonarchSparklineVariant
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Trend = listOf(3.0, 5.0, 4.0, 8.0, 7.0, 9.0, 6.0, 11.0)

internal object MonarchSparklineFixtures : ComponentFixtures {
    override val id = "sparkline"
    override val fixtures = MonarchSparklineVariant.entries.map { variant ->
        fixture("${variant.name}, labelled") { MonarchSparkline(values = Trend, variant = variant, label = "Sign-ups") }
    } + MonarchTone.entries.map { tone ->
        fixture("tone ${tone.name}, area with gaps") {
            MonarchSparkline(
                values = listOf(120.0, 180.0, null, 260.0, 410.0),
                tone = tone,
                variant = MonarchSparklineVariant.Area,
            )
        }
    } + listOf(
        fixture("negative bars, full width") {
            MonarchSparkline(
                values = listOf(4.0, -2.0, 6.0, 3.0, -1.0, 5.0),
                variant = MonarchSparklineVariant.Bar,
                fullWidth = true,
            )
        },
        fixture("inverted with a fixed range") {
            MonarchSparkline(values = listOf(3.0, 1.0, 2.0, 1.0), range = 1.0..10.0, inverted = true)
        },
        fixture("flat series") { MonarchSparkline(values = listOf(5.0, 5.0, 5.0)) },
        fixture("not enough data") { MonarchSparkline(values = listOf(5.0), label = "Sign-ups") },
        fixture("empty") { MonarchSparkline(values = emptyList()) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSparklineTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aLabelledSparklineIsAnImageWithATextAlternative() {
        rule.setMonarchContent {
            MonarchSparkline(values = Trend, modifier = Modifier.testTag("spark"), label = "Sign-ups, last 8 days")
        }
        val node = rule.onNodeWithTag("spark")
            .assertContentDescriptionEquals("Sign-ups, last 8 days")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
        assertEquals("latest 11, low 3, high 11", node.stateDescription())
    }

    @Test
    fun theWarningAndDangerTonesAreSpokenByName() {
        rule.setMonarchContent {
            MonarchSparkline(
                values = listOf(120.0, 180.0, null, 260.0, 410.0),
                modifier = Modifier.testTag("spark"),
                tone = MonarchTone.Warning,
                label = "p95 latency",
            )
        }
        assertEquals("latest 410, low 120, high 410, Warning", rule.onNodeWithTag("spark").stateDescription())
    }

    @Test
    fun theSummaryAndTheWriterCanBeReplaced() {
        rule.setMonarchContent {
            Box {
                MonarchSparkline(
                    values = listOf(0.1, 0.2),
                    modifier = Modifier.testTag("own"),
                    label = "Rate",
                    formatValue = { "${(it * 100).toInt()} pc" },
                )
                MonarchSparkline(
                    values = Trend,
                    modifier = Modifier.testTag("summary"),
                    label = "A",
                    summary = "Rising",
                )
            }
        }
        assertEquals("latest 20 pc, low 10 pc, high 20 pc", rule.onNodeWithTag("own").stateDescription())
        assertEquals("Rising", rule.onNodeWithTag("summary").stateDescription())
    }

    @Test
    fun withoutALabelTheSparklineIsHidden() {
        rule.setMonarchContent { Box(Modifier.testTag("box")) { MonarchSparkline(values = Trend) } }
        assertTrue(rule.onNodeWithTag("box").fetchSemanticsNode().children.all { it.config.none() })
    }

    @Test
    fun tooFewValuesShowNotEnoughDataAndTheTextCanBeReplaced() {
        rule.setMonarchContent {
            Box {
                MonarchSparkline(
                    values = listOf(1.0, null),
                    modifier = Modifier.testTag("labelled"),
                    label = "Sign-ups",
                )
                MonarchSparkline(values = emptyList(), notEnoughDataLabel = "Pas assez")
            }
        }
        assertEquals("Not enough data", rule.onNodeWithTag("labelled").stateDescription())
        rule.onNodeWithText("Pas assez").assertExists()
    }

    @Test
    fun aSingleBarIsEnoughButASingleLinePointIsNot() {
        val one = MonarchSparklineGeometry.stats(listOf(4.0))
        assertTrue(MonarchSparklineGeometry.hasEnough(one, MonarchSparklineVariant.Bar))
        assertTrue(!MonarchSparklineGeometry.hasEnough(one, MonarchSparklineVariant.Line))
        assertNull(MonarchSparklineGeometry.stats(MonarchSparklineGeometry.clean(listOf(Double.NaN, null))))
    }

    @Test
    fun itIsWordSizedOrFillsTheWidth() {
        rule.setMonarchContent {
            Box(Modifier.width(300.dp)) {
                MonarchSparkline(values = Trend, modifier = Modifier.testTag("word"), label = "A")
            }
        }
        rule.onNodeWithTag("word").assertWidthIsEqualTo(MonarchSparklineTokens.Width)
            .assertHeightIsEqualTo(MonarchSparklineTokens.Height)
    }

    @Test
    fun fullWidthFillsTheContainer() {
        rule.setMonarchContent {
            Box(Modifier.width(300.dp)) {
                MonarchSparkline(values = Trend, modifier = Modifier.testTag("wide"), fullWidth = true, label = "A")
            }
        }
        rule.onNodeWithTag("wide").assertWidthIsEqualTo(300.dp)
    }

    @Test
    fun gapsBreakTheLineAndLonePointsGetADot() {
        val clean = listOf(1.0, 2.0, null, 3.0, null, 4.0, 5.0)
        val frame = MonarchSparklineFrame(70f, 24f, 2f, 1.0 to 5.0, inverted = false)
        val points = MonarchSparklineGeometry.points(clean, frame)
        val segments = MonarchSparklineGeometry.segments(points)
        assertEquals(listOf(2, 1, 2), segments.map { it.size })
        val dots = MonarchSparklineGeometry.dots(points, showLastPoint = true)
        assertEquals(listOf(points[3], points[6]), dots)
        assertEquals(listOf(points[3]), MonarchSparklineGeometry.dots(points, showLastPoint = false))
    }

    @Test
    fun largerValuesAreHigherUnlessInvertedAndValuesOutsideTheRangeStayAtTheEdge() {
        val domain = 0.0 to 10.0
        assertEquals(22f, MonarchSparklineGeometry.y(0.0, domain, 2f, 22f, inverted = false))
        assertEquals(2f, MonarchSparklineGeometry.y(10.0, domain, 2f, 22f, inverted = false))
        assertEquals(22f, MonarchSparklineGeometry.y(10.0, domain, 2f, 22f, inverted = true))
        assertEquals(2f, MonarchSparklineGeometry.y(50.0, domain, 2f, 22f, inverted = false))
        val frame = MonarchSparklineFrame(40f, 24f, 2f, domain, inverted = false)
        val first = MonarchSparklineGeometry.points(listOf(0.0, 10.0), frame)
        assertEquals(Offset(2f, 22f), first[0])
        assertEquals(Offset(38f, 2f), first[1])
    }

    @Test
    fun barsIncludeZeroAndNegativeValuesGoDown() {
        val stats = checkNotNull(MonarchSparklineGeometry.stats(listOf(4.0, -2.0)))
        val domain = MonarchSparklineGeometry.domain(stats, null, MonarchSparklineVariant.Bar)
        assertEquals(-2.0 to 4.0, domain)
        val frame = MonarchSparklineFrame(60f, 26f, 2f, domain, inverted = false)
        val bars = MonarchSparklineGeometry.bars(listOf(4.0, -2.0, null), frame)
        val zero = frame.y(0.0)
        assertEquals(zero, checkNotNull(bars[0]).bottom)
        assertEquals(zero, checkNotNull(bars[1]).top)
        assertNull(bars[2])
        val level = checkNotNull(MonarchSparklineGeometry.stats(listOf(5.0, 5.0)))
        val flat = MonarchSparklineGeometry.domain(level, null, MonarchSparklineVariant.Line)
        assertEquals(4.0 to 6.0, flat)
    }

    @Test
    fun theNotEnoughDataTextIsNotCutOffAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchSparkline(values = emptyList()) }
        rule.onNodeWithText("Not enough data", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheToneOrTheContentColour() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val plain = MonarchSparklineTokens.colors(null, colors, colors.textDefault)
            assertEquals(colors.textDefault, plain.stroke, theme.id)
            assertEquals(colors.bgMuted, plain.fill, theme.id)
            val warning = MonarchSparklineTokens.colors(MonarchTone.Warning, colors, Color.Unspecified)
            assertEquals(colors.warningStrong, warning.stroke, theme.id)
            assertEquals(colors.warningSubtle, warning.fill, theme.id)
            val neutral = MonarchSparklineTokens.colors(MonarchTone.Neutral, colors, Color.Unspecified)
            assertEquals(colors.textMuted, neutral.stroke, theme.id)
        }
    }
}
