package com.rookhq.monarch.components.forms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.forms.colorpicker.MonarchColorPicker
import com.rookhq.monarch.components.forms.colorpicker.MonarchColorPickerLabels
import com.rookhq.monarch.components.forms.colorpicker.MonarchColorPickerTokens
import com.rookhq.monarch.components.forms.colorpicker.MonarchColorPickerTokens.Commit
import com.rookhq.monarch.components.forms.colorpicker.MonarchColorSwatch
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val FewSwatches = listOfNotNull(
    MonarchColorSwatch.of("#2563eb", "Blue"),
    MonarchColorSwatch.of("dc2626", "Red"),
    MonarchColorSwatch.of("not a color", "Broken"),
)

internal object MonarchColorPickerFixtures : ComponentFixtures {
    override val id = "color-picker"
    override val fixtures = listOf(
        fixture("default swatches, one selected") {
            MonarchColorPicker(value = "#2563eb", onValueChange = {}, label = "Label color", allowCustom = false)
        },
        fixture("hex field only, custom color") {
            MonarchColorPicker(
                value = "#123abc",
                onValueChange = {},
                swatches = emptyList(),
                label = "Brand color",
                helperText = "Used for buttons and links.",
            )
        },
        fixture("few swatches and the field, nothing selected") {
            MonarchColorPicker(value = null, onValueChange = {}, swatches = FewSwatches)
        },
        fixture("disabled") {
            MonarchColorPicker(value = "#dc2626", onValueChange = {}, swatches = FewSwatches, enabled = false)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchColorPickerTest {
    @get:Rule
    val rule = createComposeRule()

    private val isRadio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    @Test
    fun hexTextIsNormalisedAsOnTheWeb() {
        assertEquals("#aabbcc", MonarchColorSwatch.normalizedHex(" #ABC "))
        assertEquals("#2563eb", MonarchColorSwatch.normalizedHex("2563EB"))
        assertNull(MonarchColorSwatch.normalizedHex("#12345"))
        assertNull(MonarchColorSwatch.normalizedHex("blue"))
        assertNull(MonarchColorSwatch.of("#xyz", "Nope"))
        assertEquals(2, FewSwatches.size)
        assertEquals(Color(red = 0x25, green = 0x63, blue = 0xEB), MonarchColorSwatch.color("#2563eb"))
        assertNull(MonarchColorSwatch.color("#12"))
    }

    @Test
    fun aSwatchIsARadioButtonNamedByItsNameAndValue() {
        var value: String? by mutableStateOf(null)
        val picked = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchColorPicker(
                value = value,
                onValueChange = {
                    picked += it
                    value = it
                },
                swatches = FewSwatches,
            )
        }
        val blue = rule.onNodeWithContentDescription("Blue, #2563eb")
        blue.assert(isRadio).assertIsNotSelected().assertTouchTargetAtLeast48dp(rule.density)
        blue.performClick()
        blue.assertIsSelected()
        blue.performClick()
        assertEquals(listOf("#2563eb"), picked)
        rule.onNodeWithContentDescription("Preset colors").assertExists()
    }

    @Test
    fun theDefaultSwatchesAreTheTenSharedPresets() {
        rule.setMonarchContent { MonarchColorPicker(value = "#DB2777", onValueChange = {}) }
        rule.onNodeWithContentDescription("Slate, #64748b").assertIsNotSelected()
        rule.onNodeWithContentDescription("Pink, #db2777").assertIsSelected()
    }

    @Test
    fun aValidHexIsCommittedOnDoneAndAnInvalidOneShowsTheError() {
        val picked = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchColorPicker(value = null, onValueChange = { picked += it }, swatches = emptyList())
        }
        val field = rule.onNode(hasSetTextAction())
        field.performTextReplacement("zzz")
        field.performImeAction()
        // The message is the field's error (the visible line is hidden from TalkBack, which reads it with the field).
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Enter a hex color like #2563eb."))
        field.performTextReplacement("ABC")
        field.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
        field.performImeAction()
        assertEquals(listOf("#aabbcc"), picked)
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchColorPicker(
                value = null,
                onValueChange = {},
                swatches = FewSwatches,
                labels = MonarchColorPickerLabels(
                    swatches = "Couleurs",
                    swatch = { name, hex -> "$name ($hex)" },
                    hex = "Couleur hexadécimale",
                    invalid = "Invalide",
                ),
            )
        }
        rule.onNodeWithContentDescription("Couleurs").assertExists()
        rule.onNodeWithContentDescription("Blue (#2563eb)").assertExists()
        val field = rule.onNode(hasSetTextAction() and hasContentDescription("Couleur hexadécimale"))
        field.performTextReplacement("nope")
        field.performImeAction()
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Invalide"))
    }

    @Test
    fun aDisabledPickerDoesNotChange() {
        val picked = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchColorPicker(value = null, onValueChange = { picked += it }, swatches = FewSwatches, enabled = false)
        }
        rule.onNodeWithContentDescription("Red, #dc2626").assertIsNotEnabled().performClick()
        rule.onNodeWithContentDescription("Hex color").assertIsNotEnabled()
        assertEquals(emptyList(), picked)
    }

    @Test
    fun swatchesFlowFromTheStartAndTheLabelFitsAtTheLargestFontScale() {
        val environment = MonarchTestEnvironment(fontScale = LargestFontScale)
        rule.setMonarchContent(environment) {
            MonarchColorPicker(value = null, onValueChange = {}, swatches = FewSwatches, label = "Label color")
        }
        rule.onNodeWithText("Label color").assertTextFullyVisible()
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.onNodeWithContentDescription("Blue, #2563eb")
                .assertIsBefore(rule.onNodeWithContentDescription("Red, #dc2626"), direction)
        }
    }

    @Test
    fun committingFollowsTheWebRules() {
        assertEquals(Commit(null, "#2563eb", invalid = false), MonarchColorPickerTokens.commit(" ", "#2563eb"))
        assertEquals(Commit(null, "red", invalid = true), MonarchColorPickerTokens.commit("red", null))
        assertEquals(Commit("#ffffff", "#ffffff", invalid = false), MonarchColorPickerTokens.commit("FFF", null))
        assertEquals(Commit(null, "#ffffff", invalid = false), MonarchColorPickerTokens.commit("fff", "#ffffff"))
        assertNull(MonarchColorPickerTokens.selection("blue"))
    }

    @Test
    fun theSelectionIsARingInTheTextColourInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val selected = MonarchColorPickerTokens.swatchColors(selected = true, colors)
            assertEquals(colors.textDefault, selected.ring, theme.id)
            assertEquals(colors.bgSurface, selected.mark, theme.id)
            assertEquals(colors.textDefault, selected.markBorder, theme.id)
            assertEquals(colors.borderDefault, MonarchColorPickerTokens.swatchColors(selected = false, colors).ring)
            assertEquals(colors.textDisabled, MonarchColorPickerTokens.textColor(false, colors, strong = true))
            assertEquals(colors.textMuted, MonarchColorPickerTokens.textColor(true, colors, strong = false))
        }
    }
}
