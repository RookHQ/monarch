package com.rookhq.monarch.components.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.layout.resizablepanels.MonarchResizablePanels
import com.rookhq.monarch.components.layout.resizablepanels.MonarchResizablePanelsAxis
import com.rookhq.monarch.components.layout.resizablepanels.MonarchResizablePanelsTokens
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchResizablePanelsFixtures : ComponentFixtures {
    override val id = "resizable-panels"
    override val fixtures = listOf(
        fixture("horizontal") {
            MonarchResizablePanels(
                fraction = 0.4f,
                onFractionChange = {},
                modifier = Modifier.height(120.dp),
                stacksWhenCompact = false,
                first = { MonarchText(text = "Editor") },
                second = { MonarchText(text = "Preview") },
            )
        },
        fixture("horizontal with grip") {
            MonarchResizablePanels(
                fraction = 0.6f,
                onFractionChange = {},
                modifier = Modifier.height(120.dp),
                showGrip = true,
                stacksWhenCompact = false,
                first = { MonarchText(text = "Editor") },
                second = { MonarchText(text = "Preview") },
            )
        },
        fixture("vertical") {
            MonarchResizablePanels(
                fraction = 0.5f,
                onFractionChange = {},
                modifier = Modifier.height(200.dp),
                axis = MonarchResizablePanelsAxis.Vertical,
                showGrip = true,
                first = { MonarchText(text = "Query") },
                second = { MonarchText(text = "Results") },
            )
        },
        fixture("disabled") {
            MonarchResizablePanels(
                fraction = 0.5f,
                onFractionChange = {},
                modifier = Modifier.height(120.dp),
                enabled = false,
                stacksWhenCompact = false,
                first = { MonarchText(text = "Editor") },
                second = { MonarchText(text = "Preview") },
            )
        },
        fixture("stacked on a compact window") {
            MonarchResizablePanels(
                fraction = 0.5f,
                onFractionChange = {},
                first = { MonarchText(text = "Editor") },
                second = { MonarchText(text = "Preview") },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchResizablePanelsTest {
    @get:Rule
    val rule = createComposeRule()

    private fun handle() = rule.onNodeWithContentDescription("Resize panels")

    private fun setPanels(
        fraction: () -> Float,
        onChange: (Float) -> Unit,
        onFinished: ((Float) -> Unit)? = null,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        enabled: Boolean = true,
    ) {
        rule.setMonarchContent(layoutDirection = layoutDirection) {
            MonarchResizablePanels(
                fraction = fraction(),
                onFractionChange = onChange,
                modifier = Modifier.height(120.dp),
                enabled = enabled,
                stacksWhenCompact = false,
                onResizeFinished = onFinished,
                first = { Box(Modifier.fillMaxSize().testTag("first")) },
                second = { Box(Modifier.fillMaxSize().testTag("second")) },
            )
        }
    }

    @Test
    fun theSeparatorIsNamedAndReadsTheShareAsAPercentage() {
        setPanels(fraction = { 0.4f }, onChange = {})
        val separator = handle()
        assertEquals("40%", separator.stateDescription())
        separator.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(0.4f, 0.1f..0.9f, steps = 15),
            ),
        )
        separator.assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun thePanelsShareTheWidthLeftByTheSeparator() {
        setPanels(fraction = { 0.5f }, onChange = {})
        val first = rule.onNodeWithTag("first").fetchSemanticsNode().size.width
        val second = rule.onNodeWithTag("second").fetchSemanticsNode().size.width
        val separator = handle().fetchSemanticsNode().size.width
        assertTrue(kotlin.math.abs(first - second) <= 1, "$first and $second")
        assertEquals(with(rule.density) { 48.dp.roundToPx() }, separator)
    }

    @Test
    fun theFirstPanelIsAtTheStartInBothDirections() {
        setPanels(fraction = { 0.3f }, onChange = {}, layoutDirection = LayoutDirection.Rtl)
        val first = rule.onNodeWithTag("first").fetchSemanticsNode().boundsInRoot
        val second = rule.onNodeWithTag("second").fetchSemanticsNode().boundsInRoot
        assertTrue(first.left > second.left, "In right to left the first panel is on the right")
    }

    @Test
    fun talkBackAdjustmentsAreClampedAndReported() {
        var fraction by mutableFloatStateOf(0.5f)
        var finished: Float? = null
        setPanels(fraction = { fraction }, onChange = { fraction = it }, onFinished = { finished = it })
        handle().performSemanticsAction(SemanticsActions.SetProgress) { it(0.95f) }
        assertEquals(0.9f, fraction)
        assertEquals(0.9f, finished)
        rule.waitForIdle()
        assertEquals("90%", handle().stateDescription())
    }

    @Test
    fun draggingTowardsTheEndGrowsTheFirstPanel() {
        var ltr by mutableFloatStateOf(0.5f)
        setPanels(fraction = { ltr }, onChange = { ltr = it })
        handle().performTouchInput { swipeRight(startX = centerX, endX = centerX + 60f) }
        assertTrue(ltr > 0.5f, "Dragging towards the end grows the first panel: $ltr")
    }

    @Test
    fun draggingLeftGrowsTheFirstPanelInRightToLeft() {
        var rtl by mutableFloatStateOf(0.5f)
        var finished: Float? = null
        setPanels(
            fraction = { rtl },
            onChange = { rtl = it },
            onFinished = { finished = it },
            layoutDirection = LayoutDirection.Rtl,
        )
        handle().performTouchInput { swipeLeft(startX = centerX, endX = centerX - 60f) }
        assertTrue(rtl > 0.5f, "Dragging left grows the first panel in right to left: $rtl")
        assertEquals(rtl, finished)
    }

    @Test
    fun aDisabledSeparatorCannotBeAdjusted() {
        var changes = 0
        setPanels(fraction = { 0.5f }, onChange = { changes++ }, enabled = false)
        handle().assertIsNotEnabled()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetProgress))
            .performTouchInput { swipeRight() }
        assertEquals(0, changes)
    }

    @Test
    fun onACompactWindowThePanelsAreStackedWithoutASeparator() {
        rule.setMonarchContent {
            MonarchResizablePanels(
                fraction = 0.5f,
                onFractionChange = {},
                first = { MonarchText(text = "Editor") },
                second = { MonarchText(text = "Preview") },
            )
        }
        rule.onAllNodesWithContentDescription("Resize panels").assertCountEquals(0)
        val editor = rule.onNodeWithText("Editor").fetchSemanticsNode().boundsInRoot
        val preview = rule.onNodeWithText("Preview").fetchSemanticsNode().boundsInRoot
        assertTrue(preview.top >= editor.bottom)
    }

    @Test
    fun arrowKeysFollowTheAxisAndTheLayoutDirection() {
        val t = MonarchResizablePanelsTokens
        val h = MonarchResizablePanelsAxis.Horizontal
        val v = MonarchResizablePanelsAxis.Vertical
        val ltr = LayoutDirection.Ltr
        val rtl = LayoutDirection.Rtl
        assertEquals(1, t.keyDirection(Key.DirectionRight, h, ltr))
        assertEquals(-1, t.keyDirection(Key.DirectionLeft, h, ltr))
        assertEquals(-1, t.keyDirection(Key.DirectionRight, h, rtl))
        assertEquals(1, t.keyDirection(Key.DirectionLeft, h, rtl))
        assertEquals(1, t.keyDirection(Key.DirectionDown, v, rtl))
        assertEquals(-1, t.keyDirection(Key.DirectionUp, v, ltr))
        assertNull(t.keyDirection(Key.DirectionDown, h, ltr))
        assertNull(t.keyDirection(Key.DirectionRight, v, ltr))
    }

    @Test
    fun keysMoveByTheStepWithinTheRange() {
        val t = MonarchResizablePanelsTokens
        val range = 0.1f..0.9f
        assertEquals(0.55f, t.keyTarget(Key.DirectionRight, 0.5f, 0.05f, range, 1)!!, 0.0001f)
        assertEquals(0.45f, t.keyTarget(Key.DirectionLeft, 0.5f, 0.05f, range, -1)!!, 0.0001f)
        assertEquals(0.9f, t.keyTarget(Key.DirectionRight, 0.88f, 0.05f, range, 1))
        assertEquals(0.1f, t.keyTarget(Key.MoveHome, 0.5f, 0.05f, range, null))
        assertEquals(0.9f, t.keyTarget(Key.MoveEnd, 0.5f, 0.05f, range, null))
        assertNull(t.keyTarget(Key.Enter, 0.5f, 0.05f, range, null))
    }

    @Test
    fun theLogicClampsSplitsAndMirrors() {
        val t = MonarchResizablePanelsTokens
        assertEquals(0f..1f, t.sanitize(1.5f..-0.5f))
        assertEquals(101 to 151, t.panelSizes(total = 300, handle = 48, fraction = 0.4f))
        assertEquals(0.1f, t.dragChange(30f, 300, MonarchResizablePanelsAxis.Horizontal, LayoutDirection.Ltr))
        assertEquals(-0.1f, t.dragChange(30f, 300, MonarchResizablePanelsAxis.Horizontal, LayoutDirection.Rtl))
        assertEquals(0.1f, t.dragChange(30f, 300, MonarchResizablePanelsAxis.Vertical, LayoutDirection.Rtl))
        assertEquals(0f, t.dragChange(30f, 0, MonarchResizablePanelsAxis.Horizontal, LayoutDirection.Ltr))
        assertEquals(15, t.accessibilitySteps(0.1f..0.9f, 0.05f))
        assertTrue(t.stacks(MonarchResizablePanelsAxis.Horizontal, stacksWhenCompact = true, compactWidth = true))
        assertFalse(t.stacks(MonarchResizablePanelsAxis.Vertical, stacksWhenCompact = true, compactWidth = true))
        assertFalse(t.stacks(MonarchResizablePanelsAxis.Horizontal, stacksWhenCompact = false, compactWidth = true))
        assertFalse(t.stacks(MonarchResizablePanelsAxis.Horizontal, stacksWhenCompact = true, compactWidth = false))
    }

    @Test
    fun theLineIsAccentWhileActiveAndSubtleWhenDisabledInEveryTheme() {
        val t = MonarchResizablePanelsTokens
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val resting = t.colors(active = false, enabled = true, colors = colors)
            assertEquals(colors.borderDefault, resting.line, theme.id)
            assertEquals(colors.borderStrong, resting.grip, theme.id)
            val active = t.colors(active = true, enabled = true, colors = colors)
            assertEquals(colors.accentSolid, active.line, theme.id)
            assertEquals(colors.accentSolid, active.grip, theme.id)
            val disabled = t.colors(active = true, enabled = false, colors = colors)
            assertEquals(colors.borderSubtle, disabled.line, theme.id)
            assertEquals(colors.borderSubtle, disabled.grip, theme.id)
        }
        assertTrue(t.lineWidth(active = true) > t.lineWidth(active = false))
    }
}
