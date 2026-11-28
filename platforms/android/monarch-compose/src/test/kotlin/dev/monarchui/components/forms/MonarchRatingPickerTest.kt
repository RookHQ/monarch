package dev.monarchui.components.forms

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.forms.ratingpicker.MonarchRatingOption
import dev.monarchui.components.forms.ratingpicker.MonarchRatingPicker
import dev.monarchui.components.forms.ratingpicker.MonarchRatingPickerLabels
import dev.monarchui.components.forms.ratingpicker.MonarchRatingPickerSize
import dev.monarchui.components.forms.ratingpicker.MonarchRatingPickerTokens
import dev.monarchui.components.forms.ratingpicker.MonarchRatingPickerVariant
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

private val labelled = listOf(
    MonarchRatingOption(1, "Below"),
    MonarchRatingOption(2, "Meets"),
    MonarchRatingOption(3, "Exceeds"),
)

internal object MonarchRatingPickerFixtures : ComponentFixtures {
    override val id = "rating-picker"
    override val fixtures = listOf(
        fixture("scale, no rating") { MonarchRatingPicker(value = null, onValueChange = {}, label = "How was it?") },
        fixture("scale, rated, small") {
            MonarchRatingPicker(value = 3, onValueChange = {}, size = MonarchRatingPickerSize.Sm)
        },
        fixture("labelled scale") { MonarchRatingPicker(value = 2, onValueChange = {}, options = labelled) },
        fixture("stars, rated with label") {
            MonarchRatingPicker(
                value = 2,
                onValueChange = {},
                options = labelled,
                variant = MonarchRatingPickerVariant.Stars,
            )
        },
        fixture("stars, small, empty") {
            MonarchRatingPicker(
                value = null,
                onValueChange = {},
                variant = MonarchRatingPickerVariant.Stars,
                size = MonarchRatingPickerSize.Sm,
            )
        },
        fixture("disabled") { MonarchRatingPicker(value = 4, onValueChange = {}, enabled = false) },
        fixture("read only stars") {
            MonarchRatingPicker(
                value = 4,
                onValueChange = {},
                label = "Average",
                variant = MonarchRatingPickerVariant.Stars,
                readOnly = true,
            )
        },
        fixture("ten-point scale") {
            MonarchRatingPicker(value = 7, onValueChange = {}, options = MonarchRatingOption.range(1, 10))
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchRatingPickerTest {
    @get:Rule
    val rule = createComposeRule()

    private val isRadio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    @Test
    fun eachValueIsARadioButtonAndPicks() {
        var value by mutableStateOf<Int?>(null)
        rule.setMonarchContent {
            MonarchRatingPicker(value = value, onValueChange = { value = it }, label = "How was it?")
        }
        rule.onNodeWithText("3").assert(isRadio).assertIsNotSelected().performClick()
        assertEquals(3, value)
        rule.onNodeWithText("3").assertIsSelected()
        rule.onNodeWithText("4").assertIsNotSelected()
        // Without clearable, choosing the chosen value keeps it.
        rule.onNodeWithText("3").performClick()
        assertEquals(3, value)
    }

    @Test
    fun aLabelledTileIsNamedByTheNumberAndTheLabel() {
        rule.setMonarchContent { MonarchRatingPicker(value = 2, onValueChange = {}, options = labelled) }
        rule.onNodeWithText("Meets", substring = true).assertIsSelected()
        rule.onNodeWithText("2", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun aClearableRatingClearsByTappingAgainOrByTheAction() {
        var value by mutableStateOf<Int?>(4)
        rule.setMonarchContent {
            Box(Modifier.testTag("frame")) {
                MonarchRatingPicker(value = value, onValueChange = { value = it }, clearable = true)
            }
        }
        rule.onNodeWithText("4").performClick()
        assertNull(value)
        value = 2
        rule.waitForIdle()
        val group = rule.onNodeWithTag("frame").fetchSemanticsNode().children
            .flatMap { listOf(it) + it.children }
            .first { it.config.contains(SemanticsActions.CustomActions) }
        val clear = group.config.getOrNull(SemanticsActions.CustomActions).orEmpty().single()
        assertEquals("Clear rating", clear.label)
        clear.action()
        assertNull(value)
    }

    @Test
    fun starsAreNamedOfTheScaleAndFillUpToTheValue() {
        var value by mutableStateOf<Int?>(null)
        rule.setMonarchContent {
            MonarchRatingPicker(
                value = value,
                onValueChange = { value = it },
                options = labelled,
                variant = MonarchRatingPickerVariant.Stars,
            )
        }
        rule.onNodeWithContentDescription("2 of 3, Meets").assert(isRadio).performClick()
        assertEquals(2, value)
        rule.onNodeWithContentDescription("2 of 3, Meets").assertIsSelected()
        rule.onNodeWithText("Meets", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aDisabledPickerDoesNotChange() {
        var changes = 0
        rule.setMonarchContent { MonarchRatingPicker(value = 2, onValueChange = { changes++ }, enabled = false) }
        rule.onNodeWithText("3").assertIsNotEnabled().performClick()
        assertEquals(0, changes)
    }

    @Test
    fun aReadOnlyPickerIsOneValue() {
        rule.setMonarchContent {
            MonarchRatingPicker(
                value = 4,
                onValueChange = {},
                label = "Average",
                variant = MonarchRatingPickerVariant.Stars,
                readOnly = true,
            )
        }
        val node = rule.onNodeWithContentDescription("Average")
        assertEquals("4 of 5", node.stateDescription())
        assertEquals(false, node.fetchSemanticsNode().config.contains(SemanticsActions.OnClick))
    }

    @Test
    fun targetsAre48dpAndTheScaleRunsFromTheStart() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchRatingPicker(value = null, onValueChange = {}, variant = MonarchRatingPickerVariant.Stars)
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithContentDescription("1 of 5").assertTouchTargetAtLeast48dp(rule.density)
                .assertIsBefore(rule.onNodeWithContentDescription("2 of 5"), direction)
        }
    }

    @Test
    fun tileTextIsNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchRatingPicker(value = null, onValueChange = {}, options = labelled, size = MonarchRatingPickerSize.Sm)
        }
        rule.onNodeWithText("Exceeds", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Exceeds", substring = true).assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchRatingPicker(
                value = null,
                onValueChange = {},
                variant = MonarchRatingPickerVariant.Stars,
                labels = MonarchRatingPickerLabels(valueText = { v, max, _ -> "$v stars out of $max" }),
            )
        }
        rule.onNodeWithContentDescription("3 stars out of 5").assertExists()
    }

    @Test
    fun aCustomStarReplacesTheDrawnOne() {
        val drawn = mutableListOf<Boolean>()
        rule.setMonarchContent {
            MonarchRatingPicker(
                value = 2,
                onValueChange = {},
                variant = MonarchRatingPickerVariant.Stars,
                star = { filled -> drawn += filled },
            )
        }
        assertEquals(listOf(true, true, false, false, false), drawn.take(5))
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val t = MonarchRatingPickerTokens
            assertEquals(colors.bgSurface, t.tile(false, true, colors).container, theme.id)
            assertEquals(colors.accentSubtle, t.tile(true, true, colors).container, theme.id)
            assertEquals(colors.accentBorder, t.tile(true, true, colors).border, theme.id)
            assertEquals(colors.textDisabled, t.tile(true, false, colors).content, theme.id)
            assertEquals(colors.warningStrong, t.star(true, true, colors), theme.id)
            assertEquals(colors.borderStrong, t.star(false, true, colors), theme.id)
            assertEquals(colors.textDisabled, t.star(true, false, colors), theme.id)
            assertNotEquals(t.tileBorderWidth(true), t.tileBorderWidth(false))
        }
    }
}
