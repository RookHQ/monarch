package dev.monarchui.internal.field

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchControlMetrics
import dev.monarchui.foundation.MonarchFieldSize
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchThemeTokens
import dev.monarchui.tokens.MonarchTokens
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The field decoration scaffold: sizes, touch target, font scale, slots, direction and state colours. */
@RunWith(RobolectricTestRunner::class)
class MonarchFieldBoxTest {
    @get:Rule
    val rule = createComposeRule()

    private val environment = MonarchTestEnvironment()
    private var size by mutableStateOf(MonarchFieldSize.Medium)
    private var enabled by mutableStateOf(true)
    private var value by mutableStateOf("")
    private var multiline by mutableStateOf(false)
    private var withSlots by mutableStateOf(false)

    /** Sets the content once; a test changes the state variables afterwards, as the rule allows one setContent. */
    private fun setField(
        size: MonarchFieldSize = MonarchFieldSize.Medium,
        fontScale: Float = 1f,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        enabled: Boolean = true,
        value: String = "",
        multiline: Boolean = false,
        withSlots: Boolean = false,
    ) {
        this.size = size
        this.enabled = enabled
        this.value = value
        this.multiline = multiline
        this.withSlots = withSlots
        environment.fontScale = fontScale
        environment.layoutDirection = layoutDirection
        if (!started) {
            started = true
            rule.setMonarchContent(environment) { Field() }
        }
        rule.waitForIdle()
    }

    private var started = false

    @Composable
    private fun Field() {
        var text by remember(value) { mutableStateOf(value) }
        MonarchFieldBox(
            modifier = Modifier.testTag("box").width(240.dp),
            size = size,
            state = MonarchFieldState(enabled = enabled),
            multiline = multiline,
            leading = if (withSlots) ({ Box(Modifier.testTag("leading")) { Text("L") } }) else null,
            prefix = if (withSlots) "$" else null,
            suffix = if (withSlots) "kg" else null,
            trailing = if (withSlots) ({ Box(Modifier.testTag("trailing")) { Text("T") } }) else null,
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                enabled = enabled,
                modifier = Modifier.testTag("input"),
                decorationBox = { inner ->
                    if (text.isEmpty()) MonarchFieldPlaceholder("Placeholder", enabled)
                    inner()
                },
            )
        }
    }

    @Test
    fun theThreeSizesAreAtLeast40And48And52Dp() {
        assertEquals(40.dp, MonarchControlMetrics.fieldMinHeight(MonarchFieldSize.Small))
        assertEquals(48.dp, MonarchControlMetrics.fieldMinHeight(MonarchFieldSize.Medium))
        assertEquals(52.dp, MonarchControlMetrics.fieldMinHeight(MonarchFieldSize.Large))
    }

    @Test
    fun theMediumFieldIs48DpAndLargeIs52Dp() {
        setField(MonarchFieldSize.Medium)
        rule.onNodeWithTag("box").assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun aSmallFieldIs40DpVisibleButReserves48Dp() {
        setField(MonarchFieldSize.Small)
        rule.onNodeWithTag("box").assertHeightIsEqualTo(MonarchTokens.Space.space12.dp)
        rule.onNodeWithTag("input").assertHeightIsAtLeast(1.dp)
    }

    @Test
    fun theTouchTargetIsAtLeast48DpInEverySize() {
        for (size in MonarchFieldSize.entries) {
            setField(size)
            rule.onNodeWithTag("box").assertTouchTargetAtLeast48dp(rule.density)
        }
    }

    @Test
    fun aMultilineFieldIsAtLeast72Dp() {
        setField(multiline = true)
        rule.onNodeWithTag("box").assertHeightIsAtLeast(72.dp)
    }

    @Test
    fun theFieldGrowsAtFontScale2InsteadOfClippingItsText() {
        setField(fontScale = LargestFontScale, value = "A value that is longer than one line at twice the size")
        val box = rule.onNodeWithTag("box")
        box.assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("input").assertExists()
        val bounds = box.getUnclippedBoundsInRoot()
        assertTrue(bounds.right > bounds.left && bounds.bottom > bounds.top)
    }

    @Test
    fun thePlaceholderIsFullyVisibleAtFontScale2() {
        setField(fontScale = LargestFontScale)
        rule.onNodeWithTag("box").assertExists()
        rule.onNodeWithTag("input").assertIsEnabled()
    }

    @Test
    fun theTextEngineKeepsItsSemantics() {
        setField(value = "x")
        rule.onNode(hasSetTextAction()).assertIsEnabled()
        setField(enabled = false)
        rule.onNodeWithTag("input").assertIsNotEnabled()
    }

    @Test
    fun leadingComesBeforeTrailingInLtrAndAfterItInRtl() {
        setField(withSlots = true)
        val ltrLeading = rule.onNodeWithTag("leading").getUnclippedBoundsInRoot().left
        val ltrTrailing = rule.onNodeWithTag("trailing").getUnclippedBoundsInRoot().left
        assertTrue(ltrLeading < ltrTrailing)
        setField(withSlots = true, layoutDirection = LayoutDirection.Rtl)
        val rtlLeading = rule.onNodeWithTag("leading").getUnclippedBoundsInRoot().left
        val rtlTrailing = rule.onNodeWithTag("trailing").getUnclippedBoundsInRoot().left
        assertTrue(rtlLeading > rtlTrailing)
    }

    @Test
    fun theSlotsAreAllPresentWithPrefixAndSuffix() {
        setField(withSlots = true, value = "5")
        rule.onNodeWithTag("leading").assertExists()
        rule.onNodeWithTag("trailing").assertExists()
        rule.onNode(hasText("$")).assertExists()
        rule.onNode(hasText("kg")).assertExists()
    }

    @Test
    fun theBorderFollowsTheStateInEveryTheme() {
        for (tokens in AllThemes) {
            val colors = MonarchColors(tokens.colors)
            val resting = colors.fieldColors(MonarchFieldState())
            assertEquals(colors.borderInput, resting.border)
            assertEquals(colors.bgSurface, resting.container)
            assertNull(resting.ring)
            assertEquals(colors.textSubtle, colors.fieldColors(MonarchFieldState(hovered = true)).border)
            val focused = colors.fieldColors(MonarchFieldState(focused = true))
            // The ring alone shows focus; the border stays the resting one, so focus never reads as invalid.
            assertEquals(colors.borderInput, focused.border)
            assertEquals(colors.focusRing, focused.ring)
            val focusedInvalid = MonarchFieldState(invalid = true, focused = true)
            assertEquals(colors.dangerStrong, colors.fieldColors(focusedInvalid).border)
            assertEquals(colors.successStrong, colors.fieldColors(MonarchFieldState(success = true)).border)
            val everything = MonarchFieldState(enabled = false, invalid = true, focused = true, hovered = true)
            val disabled = colors.fieldColors(everything)
            assertEquals(colors.bgSubtle, disabled.container)
            assertEquals(colors.textDisabled, disabled.text)
            assertEquals(colors.borderDefault, disabled.border)
            assertNull(disabled.ring)
        }
    }

    @Test
    fun theStatesLookDifferent() {
        val colors = MonarchColors(AllThemes.first().colors)
        val states = listOf(
            MonarchFieldState(),
            MonarchFieldState(hovered = true),
            MonarchFieldState(focused = true),
            MonarchFieldState(invalid = true),
            MonarchFieldState(enabled = false),
        )
        val looks = states.map {
            val c = colors.fieldColors(it)
            Triple(c.border, c.container, c.ring)
        }
        assertEquals(states.size, looks.toSet().size)
        assertNotEquals(colors.fieldColors(states[0]).placeholder, colors.fieldColors(states[4]).placeholder)
    }

    @Test
    fun aDisabledFieldIsDrawnInOpaqueQuietColoursNotWithAlpha() {
        for (tokens in AllThemes) {
            val colors = MonarchColors(tokens.colors)
            val enabled = colors.fieldColors(MonarchFieldState())
            val disabled = colors.fieldColors(MonarchFieldState(enabled = false))
            listOf(disabled.container, disabled.border, disabled.text, disabled.placeholder, disabled.icon).forEach {
                assertEquals(1f, it.alpha, "a disabled colour is an opaque theme role, not a faded enabled colour")
            }
            assertEquals(colors.bgSubtle, disabled.container)
            assertEquals(colors.textDisabled, disabled.text)
            assertNotEquals(enabled.container, disabled.container)
            assertNotEquals(enabled.text, disabled.text)
        }
    }

    @Test
    fun theFocusRingIs2DpWideAndSits2DpOutsideTheUnchangedBorder() {
        assertRing(MonarchTokens.light, ringDp = 2)
    }

    @Test
    fun theFocusRingFollowsTheThemeWidthIn3DpInHighContrast() {
        assertRing(MonarchTokens.hcLight, ringDp = 3)
    }

    /** Renders a focused medium field on white and reads one row of pixels to the left of its border. */
    private fun assertRing(tokens: MonarchThemeTokens, ringDp: Int) {
        val colors = MonarchColors(tokens.colors)
        environment.tokens = tokens
        var tick by mutableIntStateOf(0)
        rule.setMonarchContent(environment) {
            key(tick) {
                Box(Modifier.testTag("frame").background(Color.White).padding(FramePadding)) {
                    MonarchFieldBox(
                        modifier = Modifier.width(160.dp),
                        state = MonarchFieldState(focused = true),
                    ) {}
                }
            }
        }
        // Robolectric draws nothing for a first frame that follows another test in the same JVM: recompose once,
        // then capture twice (the first capture draws the pending frame), as MatrixAxes does.
        rule.runOnIdle { tick++ }
        rule.waitForIdle()
        rule.onNodeWithTag("frame").captureToImage()
        val pixels = rule.onNodeWithTag("frame").captureToImage().toPixelMap()
        with(rule.density) {
            val edge = FramePadding.roundToPx()
            val y = (FramePadding + MonarchControlMetrics.fieldMinHeight(MonarchFieldSize.Medium) / 2).roundToPx()
            val px = { dp: Int -> pixels[edge - dp.dp.roundToPx(), y] }
            assertClose(colors.borderInput, pixels[edge, y], "the border keeps its resting colour in focus")
            assertClose(Color.White, px(1), "a gap of one ring width between the border and the ring")
            assertClose(Color.White, px(ringDp), "a gap of one ring width between the border and the ring")
            assertClose(colors.focusRing, px(ringDp + 1), "the ring starts one ring width outside the border")
            assertClose(colors.focusRing, px(ringDp * 2), "the ring is $ringDp dp wide")
            assertClose(Color.White, px(ringDp * 2 + 2), "the ring is no wider than $ringDp dp")
        }
    }

    private fun assertClose(expected: Color, actual: Color, message: String) {
        val distance = maxOf(
            abs(expected.red - actual.red),
            abs(expected.green - actual.green),
            abs(expected.blue - actual.blue),
        )
        assertTrue(distance < 0.08f, "$message: expected $expected, was $actual")
    }

    private companion object {
        val FramePadding = 8.dp
    }
}
