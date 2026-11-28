package dev.monarchui.components.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import dev.monarchui.components.feedback.skeleton.MonarchSkeleton
import dev.monarchui.components.feedback.skeleton.MonarchSkeletonShape
import dev.monarchui.components.feedback.skeleton.MonarchSkeletonTokens
import dev.monarchui.foundation.MonarchMotion
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.LocalMonarchMotion
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal object MonarchSkeletonFixtures : ComponentFixtures {
    override val id = "skeleton"
    override val fixtures = listOf(
        fixture("every shape, default sizes") {
            Column {
                MonarchSkeletonShape.entries.forEach { shape -> MonarchSkeleton(shape = shape) }
            }
        },
        fixture("sized by the modifier") {
            Column {
                MonarchSkeleton(modifier = Modifier.width(120.dp), shape = MonarchSkeletonShape.Text)
                MonarchSkeleton(modifier = Modifier.size(24.dp), shape = MonarchSkeletonShape.Circle)
                MonarchSkeleton(modifier = Modifier.size(width = 200.dp, height = 80.dp))
            }
        },
        fixture("still, animations off") {
            CompositionLocalProvider(LocalMonarchMotion provides MonarchMotion(reducedMotion = true)) {
                MonarchSkeleton(shape = MonarchSkeletonShape.Text)
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSkeletonTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aSkeletonIsHiddenFromTalkBack() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            Box(Modifier.testTag("frame")) { MonarchSkeleton(modifier = Modifier.width(100.dp)) }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("frame").onChildren().assertAll(
            SemanticsMatcher.keyNotDefined(SemanticsProperties.Text) and
                SemanticsMatcher.keyNotDefined(SemanticsProperties.Role) and
                SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription),
        )
        val described = rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
        assertEquals(0, described.fetchSemanticsNodes().size)
    }

    @Test
    fun theDefaultSizesFollowTheShape() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            Column {
                Box(Modifier.testTag("circle")) { MonarchSkeleton(shape = MonarchSkeletonShape.Circle) }
                Box(Modifier.testTag("text").width(100.dp)) { MonarchSkeleton(shape = MonarchSkeletonShape.Text) }
                Box(Modifier.testTag("rect").width(100.dp)) { MonarchSkeleton() }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("circle").assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(40.dp)
        // Three quarters of the 16sp body text at font scale 1.
        rule.onNodeWithTag("text").assertWidthIsEqualTo(100.dp).assertHeightIsEqualTo(12.dp)
        rule.onNodeWithTag("rect").assertWidthIsEqualTo(100.dp).assertHeightIsEqualTo(40.dp)
    }

    @Test
    fun aTextLineGrowsWithTheFontSize() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(fontScale = 2f) {
            Box(Modifier.testTag("text").width(100.dp)) { MonarchSkeleton(shape = MonarchSkeletonShape.Text) }
        }
        rule.mainClock.advanceTimeByFrame()
        // Larger than the 12dp at font scale 1; Android scales large text less than linearly.
        assertTrue(rule.onNodeWithTag("text").getUnclippedBoundsInRoot().height > 18.dp)
    }

    @Test
    fun theModifierSetsTheSize() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            Box(Modifier.testTag("circle")) {
                MonarchSkeleton(modifier = Modifier.size(24.dp), shape = MonarchSkeletonShape.Circle)
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("circle").assertWidthIsEqualTo(24.dp).assertHeightIsEqualTo(24.dp)
    }

    /**
     * With animations turned off the skeleton runs no animation, so the test
     * clock can run until the composition is idle. An endless shimmer would
     * never let it become idle.
     */
    @Test
    fun theShimmerStandsStillWhenAnimationsAreOff() {
        rule.setMonarchContent {
            CompositionLocalProvider(LocalMonarchMotion provides MonarchMotion(reducedMotion = true)) {
                MonarchSkeleton(shape = MonarchSkeletonShape.Text)
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun thePulseIsAFullWebCycleOf1600ms() {
        assertEquals(1600, MonarchSkeletonTokens.PULSE_MILLIS * 2)
    }

    @Test
    fun coloursAreTheMutedAndSubtleBackgroundsInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val (base, highlight) = MonarchSkeletonTokens.colors(colors)
            assertEquals(colors.bgEmphasis, base, theme.id)
            val expected = lerp(colors.bgEmphasis, colors.bgMuted, MonarchSkeletonTokens.PULSE_DEPTH)
            assertEquals(expected, highlight, theme.id)
        }
    }

    @Test
    fun thePulseNeverReachesTheCanvasLooksInEveryTheme() {
        // At the top of the pulse the placeholder must still differ from the canvas it sits on, or a picture taken
        // at that moment shows an empty region (loading-state and ai-response-skeleton in the light theme).
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val (_, highlight) = MonarchSkeletonTokens.colors(colors)
            assertNotEquals(colors.bgCanvas, highlight, theme.id)
            assertTrue(contrast(highlight, colors.bgCanvas) > contrast(colors.bgMuted, colors.bgCanvas), theme.id)
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val l1 = a.luminance().toDouble()
        val l2 = b.luminance().toDouble()
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }
}
