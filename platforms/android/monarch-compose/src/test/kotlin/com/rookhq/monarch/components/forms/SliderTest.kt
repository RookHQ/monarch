package com.rookhq.monarch.components.forms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.forms.slider.MonarchSlider
import com.rookhq.monarch.components.forms.slider.MonarchSliderTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.MonarchTestEnvironment
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

internal object SliderFixtures : ComponentFixtures {
    override val id = "slider"
    override val fixtures = listOf(
        fixture("continuous") { MonarchSlider(value = 0.4f, onValueChange = {}) },
        fixture("stepped, at the end") {
            MonarchSlider(value = 100f, onValueChange = {}, valueRange = 0f..100f, steps = 9)
        },
        fixture("disabled") { MonarchSlider(value = 0.6f, onValueChange = {}, enabled = false) },
        fixture("invalid") { MonarchSlider(value = 0.6f, onValueChange = {}, invalid = true) },
        fixture("with a label and the value shown") {
            MonarchSlider(
                value = 40f,
                onValueChange = {},
                valueRange = 0f..100f,
                label = "Volume",
                valueLabel = { "${it.toInt()}%" },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class SliderTest {
    @get:Rule
    val rule = createComposeRule()

    private val slider get() = rule.onNodeWithTag("slider")

    @Test
    fun exposesItsRangeAndChangesThroughTheProgressAction() {
        var value by mutableFloatStateOf(20f)
        rule.setMonarchContent {
            MonarchSlider(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.testTag("slider"),
                valueRange = 0f..100f,
                steps = 9,
                label = "Volume",
                valueLabel = { "${it.toInt()} percent" },
            )
        }
        val thumb = rule.onNodeWithContentDescription("Volume")
        assertEquals(
            ProgressBarRangeInfo(20f, 0f..100f, 9),
            thumb.fetchSemanticsNode().config.getOrNull(SemanticsProperties.ProgressBarRangeInfo),
        )
        assertEquals("20 percent", thumb.stateDescription())
        thumb.performSemanticsAction(SemanticsActions.SetProgress) { it(43f) }
        // Nine steps make ten intervals of 10, so 43 snaps to 40.
        assertEquals(40f, value)
        assertEquals("40 percent", thumb.stateDescription())
    }

    @Test
    fun theValueIsShownNextToTheLabel() {
        rule.setMonarchContent {
            MonarchSlider(
                value = 40f,
                onValueChange = {},
                valueRange = 0f..100f,
                label = "Volume",
                valueLabel = { "${it.toInt()}%" },
            )
        }
        rule.onNodeWithText("Volume", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("40%", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aDisabledSliderIsReadAsDisabled() {
        rule.setMonarchContent {
            MonarchSlider(value = 0.5f, onValueChange = {}, modifier = Modifier.testTag("slider"), enabled = false)
        }
        slider.assertIsNotEnabled()
    }

    @Test
    fun theSliderHasA48dpTouchTargetInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchSlider(value = 0.3f, onValueChange = {}, modifier = Modifier.testTag("slider"))
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            slider.assertTouchTargetAtLeast48dp(rule.density)
        }
    }

    @Test
    fun theColoursComeFromTheThemeRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val normal = MonarchSliderTokens.colors(enabled = true, invalid = false, colors = c)
            assertEquals(c.accentSolid, normal.range, theme.id)
            assertEquals(c.bgEmphasis, normal.track, theme.id)
            assertEquals(c.bgSurface, normal.thumb, theme.id)
            assertEquals(c.dangerSolid, MonarchSliderTokens.colors(true, true, c).range, theme.id)
            assertEquals(c.textDisabled, MonarchSliderTokens.colors(false, false, c).range, theme.id)
        }
    }

    @Test
    fun theFractionIsClampedToTheRange() {
        assertEquals(0.25f, MonarchSliderTokens.fraction(25f, 0f..100f))
        assertEquals(1f, MonarchSliderTokens.fraction(500f, 0f..100f))
        assertEquals(0f, MonarchSliderTokens.fraction(5f, 5f..5f))
    }
}
