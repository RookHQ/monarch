package dev.monarchui.components.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.monarchui.components.forms.checkbox.MonarchCheckbox
import dev.monarchui.components.forms.checkbox.MonarchCheckboxTokens
import dev.monarchui.components.forms.radio.MonarchRadio
import dev.monarchui.components.forms.radio.radioContainer
import dev.monarchui.components.forms.radio.radioRing
import dev.monarchui.components.forms.radiogroup.MonarchRadioGroup
import dev.monarchui.components.forms.switch.MonarchSwitch
import dev.monarchui.components.forms.switch.MonarchSwitchTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.contrastRatio
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The Monarch checkbox, radio and switch primitives: semantics, touch target, text at font scale 2.0, colours. */
@RunWith(RobolectricTestRunner::class)
class MonarchSelectionControlsTest {
    @get:Rule
    val rule = createComposeRule()

    // Checkbox.

    @Test
    fun aBareCheckboxIsACheckboxWithA48dpTargetAndTheBoxStaysSmall() {
        var checked by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchCheckbox(checked = checked, onCheckedChange = { checked = it }, modifier = Modifier.testTag("box"))
        }
        val box = rule.onNodeWithTag("box")
        box.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).assertIsOff()
        box.assertTouchTargetAtLeast48dp(rule.density)
        box.performClick()
        assertTrue(checked)
        box.assertIsOn()
    }

    @Test
    fun theTriStateCheckboxReportsPartlyChecked() {
        rule.setMonarchContent {
            MonarchCheckbox(state = ToggleableState.Indeterminate, onClick = {}, modifier = Modifier.testTag("box"))
        }
        assertEquals(
            ToggleableState.Indeterminate,
            rule.onNodeWithTag("box").fetchSemanticsNode().config.getOrNull(SemanticsProperties.ToggleableState),
        )
    }

    @Test
    fun aCheckboxWithALabelIsOneElementThatReadsTheLabelAndWraps() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchCheckbox(
                checked = true,
                onCheckedChange = {},
                label = "A label that is long enough to wrap onto a second line at this size",
                description = "And a description under it.",
            )
        }
        rule.onNodeWithText("A label that is long enough", substring = true)
            .assertIsOn()
            .assertHeightIsAtLeast(64.dp)
            .assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("A label that is long enough", substring = true, useUnmergedTree = true)
            .assertTextFullyVisible()
    }

    @Test
    fun aCheckboxWithoutAHandlerIsOnlyDrawn() {
        rule.setMonarchContent {
            MonarchCheckbox(checked = true, onCheckedChange = null, modifier = Modifier.testTag("box"))
        }
        assertNull(rule.onNodeWithTag("box").fetchSemanticsNode().config.getOrNull(SemanticsProperties.Role))
    }

    @Test
    fun theCheckboxColoursFollowTheWebInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val tokens = MonarchCheckboxTokens
            val off = tokens.colors(on = false, invalid = false, pressed = false, colors = c)
            assertEquals(c.bgSurface, off.container, theme.id)
            assertEquals(c.borderInput, off.border, theme.id)
            val on = tokens.colors(on = true, invalid = false, pressed = false, colors = c)
            assertEquals(c.accentStrong, on.container, theme.id)
            assertEquals(c.accentStrong, on.border, theme.id)
            assertEquals(c.accentOnStrong, on.mark, theme.id)
            assertEquals(c.dangerStrong, tokens.colors(false, true, false, c).border, theme.id)
            assertEquals(c.accentSolidActive, tokens.colors(true, false, true, c).container, theme.id)
            assertEquals(c.bgMuted, tokens.colors(false, false, true, c).container, theme.id)
        }
    }

    // Radio.

    @Test
    fun radiosInAGroupAreRadioButtonsThatSelectOne() {
        var selected by mutableStateOf("a")
        rule.setMonarchContent {
            MonarchRadioGroup(label = "Plan") {
                listOf("a", "b").forEach { id ->
                    MonarchRadio(selected = selected == id, onClick = { selected = id }, label = "Option $id")
                }
            }
        }
        rule.onNodeWithText("Option a")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsSelected()
            .assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Option b").assertIsNotSelected().performClick()
        assertEquals("b", selected)
        rule.onNodeWithText("Option b").assertIsSelected()
    }

    @Test
    fun aBareRadioHasA48dpTarget() {
        rule.setMonarchContent { MonarchRadio(selected = false, onClick = {}, modifier = Modifier.testTag("radio")) }
        rule.onNodeWithTag("radio").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theRadioRingColoursFollowTheWebInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.borderInput, radioRing(false, false, false, c), theme.id)
            assertEquals(c.accentStrong, radioRing(true, false, false, c), theme.id)
            assertEquals(c.dangerStrong, radioRing(false, true, false, c), theme.id)
        }
    }

    // Switch.

    @Test
    fun aBareSwitchIsASwitchWithA48dpTargetAndTheMaterialTrack() {
        var on by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchSwitch(checked = on, onCheckedChange = { on = it }, modifier = Modifier.testTag("switch"))
        }
        val node = rule.onNodeWithTag("switch")
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)).assertIsOff()
        node.assertTouchTargetAtLeast48dp(rule.density)
        node.assertWidthIsEqualTo(52.dp)
        node.performClick()
        node.assertIsOn()
    }

    @Test
    fun aSwitchWithALabelReadsTheLabelAndWraps() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchSwitch(
                checked = true,
                onCheckedChange = {},
                label = "A setting name that is long enough to wrap onto a second line",
            )
        }
        rule.onNodeWithText("A setting name", substring = true, useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("A setting name", substring = true).assertIsOn()
    }

    @Test
    fun theSwitchColoursFollowTheWebInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val on = MonarchSwitchTokens.colors(true, c)
            assertEquals(c.accentStrong, on.track, theme.id)
            assertEquals(MonarchSwitchTokens.onThumb(c), on.thumb, theme.id)
            // White or near white in every theme, like iOS; never the dark on-accent colour.
            assertTrue(on.thumb.luminance() > 0.9f, "${theme.id}: on thumb ${on.thumb}")
            val off = MonarchSwitchTokens.colors(false, c)
            assertEquals(c.bgEmphasis, off.track, theme.id)
            assertEquals(c.borderInput, off.border, theme.id)
            assertEquals(c.textSubtle, off.thumb, theme.id)
        }
    }

    @Test
    fun theOffThumbHasAtLeast3To1ContrastAgainstTheTrackInEveryTheme() {
        // The off thumb is the only mark of the off state, so it keeps 3:1. The on state is also shown by the
        // accent track and its hairline against the canvas; its thumb is white in every theme, as on iOS.
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val colors = MonarchSwitchTokens.colors(false, c)
            val ratio = contrastRatio(colors.thumb, colors.track)
            assertTrue(ratio >= 3.0, "${theme.id}: off thumb against track is $ratio")
        }
    }

    @Test
    fun theThumbIs16Off24OnAnd28Pressed() {
        assertEquals(16f, MonarchSwitchTokens.thumbDiameter(0f, 0f).value)
        assertEquals(24f, MonarchSwitchTokens.thumbDiameter(1f, 0f).value)
        assertEquals(28f, MonarchSwitchTokens.thumbDiameter(1f, 1f).value)
    }

    @Test
    fun aColumnOfControlsKeepsEachTargetSeparate() {
        rule.setMonarchContent {
            Column {
                MonarchCheckbox(checked = false, onCheckedChange = {}, modifier = Modifier.testTag("a"))
                MonarchCheckbox(checked = false, onCheckedChange = {}, modifier = Modifier.testTag("b"))
            }
        }
        val a = rule.onNodeWithTag("a").fetchSemanticsNode().boundsInRoot
        val b = rule.onNodeWithTag("b").fetchSemanticsNode().boundsInRoot
        assertTrue(a.bottom <= b.top + 0.5f, "the reserved 48dp targets do not overlap: $a and $b")
    }

    // Disabled controls are muted by colour, never by opacity (PREMIUM rule 10).

    @Test
    fun aDisabledControlIsMutedByColourInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val boxOff = MonarchCheckboxTokens.colors(false, false, false, c, enabled = false)
            assertEquals(c.bgSubtle, boxOff.container, theme.id)
            assertEquals(c.borderDefault, boxOff.border, theme.id)
            val boxOn = MonarchCheckboxTokens.colors(true, false, false, c, enabled = false)
            assertEquals(c.bgEmphasis, boxOn.container, theme.id)
            assertEquals(c.textDisabled, boxOn.mark, theme.id)
            assertEquals(c.borderDefault, radioRing(true, false, false, c, enabled = false), theme.id)
            assertEquals(c.bgSubtle, radioContainer(false, false, c), theme.id)
            val switchOn = MonarchSwitchTokens.colors(true, c, enabled = false)
            val half = lerp(c.bgSurface, c.accentStrong, 0.5f)
            assertEquals(half, switchOn.track, theme.id)
            assertEquals(c.bgSurface, switchOn.thumb, theme.id)
            val switchOff = MonarchSwitchTokens.colors(false, c, enabled = false)
            assertEquals(c.bgMuted, switchOff.track, theme.id)
            assertEquals(c.textDisabled, switchOff.thumb, theme.id)
        }
    }

    // Haptics: a toggle gives the platform's on or off haptic, as the system switch does.

    private class RecordingHaptics : HapticFeedback {
        val performed = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performed += hapticFeedbackType
        }
    }

    @Test
    fun togglingASwitchGivesTheOnThenTheOffHaptic() {
        val haptics = RecordingHaptics()
        var on by mutableStateOf(false)
        rule.setMonarchContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                MonarchSwitch(checked = on, onCheckedChange = { on = it }, label = "Alerts")
            }
        }
        rule.onNodeWithText("Alerts").performClick()
        rule.onNodeWithText("Alerts").performClick()
        assertEquals(listOf(HapticFeedbackType.ToggleOn, HapticFeedbackType.ToggleOff), haptics.performed)
    }

    @Test
    fun aDisabledSwitchGivesNoHaptic() {
        val haptics = RecordingHaptics()
        rule.setMonarchContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                MonarchSwitch(checked = false, onCheckedChange = {}, label = "Alerts", enabled = false)
            }
        }
        rule.onNodeWithText("Alerts").performClick()
        assertTrue(haptics.performed.isEmpty())
    }
}
