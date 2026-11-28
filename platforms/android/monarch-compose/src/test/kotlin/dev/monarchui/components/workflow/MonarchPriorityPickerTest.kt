package dev.monarchui.components.workflow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.workflow.prioritypicker.MonarchPriorityGlyph
import dev.monarchui.components.workflow.prioritypicker.MonarchPriorityLevel
import dev.monarchui.components.workflow.prioritypicker.MonarchPriorityPicker
import dev.monarchui.components.workflow.prioritypicker.MonarchPriorityPickerLabels
import dev.monarchui.components.workflow.prioritypicker.MonarchPriorityPickerSize
import dev.monarchui.components.workflow.prioritypicker.MonarchPriorityPickerTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object MonarchPriorityPickerFixtures : ComponentFixtures {
    override val id = "priority-picker"
    override val fixtures = listOf(
        fixture("every level, md") {
            Column {
                MonarchPriorityLevel.entries.forEach { MonarchPriorityPicker(value = it, onValueChange = {}) }
            }
        },
        fixture("sm, shape only, disabled, unset") {
            Column {
                MonarchPriorityPicker(
                    value = MonarchPriorityLevel.Medium,
                    onValueChange = {},
                    size = MonarchPriorityPickerSize.Sm,
                )
                MonarchPriorityPicker(value = MonarchPriorityLevel.Urgent, onValueChange = {}, showLabel = false)
                MonarchPriorityPicker(value = MonarchPriorityLevel.Low, onValueChange = {}, enabled = false)
                MonarchPriorityPicker(value = null, onValueChange = {})
            }
        },
        fixture("glyphs") {
            Row { MonarchPriorityLevel.entries.forEach { MonarchPriorityGlyph(it) } }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchPriorityPickerTest {
    @get:Rule
    val rule = createComposeRule()

    private val isDropdown = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList)

    @Test
    fun theTriggerIsADropdownNamedWithTheLevel() {
        rule.setMonarchContent { MonarchPriorityPicker(value = MonarchPriorityLevel.High, onValueChange = {}) }
        rule.onNodeWithContentDescription("Priority: High")
            .assert(isDropdown)
            .assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun choosingALevelCallsBackAndTheCurrentOneIsSelected() {
        var chosen: MonarchPriorityLevel? = null
        rule.setMonarchContent {
            MonarchPriorityPicker(value = MonarchPriorityLevel.Low, onValueChange = { chosen = it })
        }
        rule.onNodeWithContentDescription("Priority: Low").performClick()
        rule.onNode(hasText("Low") and hasAnyAncestor(isPopup())).assertIsSelected()
        rule.onNode(hasText("Urgent") and hasAnyAncestor(isPopup())).assertIsNotSelected().performClick()
        assertEquals(MonarchPriorityLevel.Urgent, chosen)
    }

    @Test
    fun aDisabledPickerDoesNotOpen() {
        rule.setMonarchContent { MonarchPriorityPicker(value = null, onValueChange = {}, enabled = false) }
        rule.onNodeWithContentDescription("Priority: No priority").assertIsNotEnabled().performClick()
        rule.onNode(hasAnyAncestor(isPopup())).assertDoesNotExist()
    }

    @Test
    fun theShapeOnlyTriggerIsStillNamedAndHas48dp() {
        rule.setMonarchContent {
            MonarchPriorityPicker(value = MonarchPriorityLevel.Urgent, onValueChange = {}, showLabel = false)
        }
        rule.onNodeWithContentDescription("Priority: Urgent")
            .assert(isDropdown)
            .assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun onlyTheOfferedLevelsAreListed() {
        rule.setMonarchContent {
            MonarchPriorityPicker(
                value = MonarchPriorityLevel.High,
                onValueChange = {},
                levels = listOf(MonarchPriorityLevel.High, MonarchPriorityLevel.Low),
            )
        }
        rule.onNodeWithContentDescription("Priority: High").performClick()
        rule.onNode(hasText("Urgent") and hasAnyAncestor(isPopup())).assertDoesNotExist()
        rule.onNode(hasText("Low") and hasAnyAncestor(isPopup())).assertExists()
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchPriorityPicker(
                value = MonarchPriorityLevel.High,
                onValueChange = {},
                labels = MonarchPriorityPickerLabels(trigger = { "Prioridad: $it" }, high = "Alta"),
            )
        }
        rule.onNodeWithContentDescription("Prioridad: Alta").assertExists()
    }

    @Test
    fun theGlyphComesBeforeTheNameRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchPriorityPicker(value = MonarchPriorityLevel.Medium, onValueChange = {})
        }
        val name = rule.onNode(hasText("Medium"), useUnmergedTree = true).getUnclippedBoundsInRoot()
        val trigger = rule.onNodeWithContentDescription("Priority: Medium").getUnclippedBoundsInRoot()
        // The glyph is at the start, the right edge right to left, so the name is nearer the left edge.
        assertTrue(trigger.right - name.right > name.left - trigger.left)
    }

    @Test
    fun theNameIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchPriorityPicker(value = MonarchPriorityLevel.None, onValueChange = {})
        }
        rule.onNode(hasText("No priority"), useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun levelsMapToAndFromTrackerNumbers() {
        assertEquals(MonarchPriorityLevel.Urgent, MonarchPriorityLevel.fromNumber(1))
        assertEquals(MonarchPriorityLevel.Low, MonarchPriorityLevel.fromNumber(4))
        assertEquals(MonarchPriorityLevel.None, MonarchPriorityLevel.fromNumber(null))
        assertEquals(MonarchPriorityLevel.None, MonarchPriorityLevel.fromNumber(9))
        assertEquals(listOf(1, 2, 3, 4, 0), MonarchPriorityLevel.entries.map { it.number })
    }

    @Test
    fun eachLevelHasItsOwnShape() {
        val tokens = MonarchPriorityPickerTokens
        assertEquals(listOf(0, 3, 2, 1, 0), MonarchPriorityLevel.entries.map { tokens.filledBars(it) })
    }

    @Test
    fun glyphColoursComeFromTheThemeWithoutFading() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchPriorityPickerTokens
            assertEquals(colors.dangerStrong, tokens.glyphColors(colors, MonarchPriorityLevel.Urgent).mark)
            assertEquals(colors.warningStrong, tokens.glyphColors(colors, MonarchPriorityLevel.High).mark)
            assertEquals(colors.textDefault, tokens.glyphColors(colors, MonarchPriorityLevel.Medium).mark)
            assertEquals(colors.textDefault, tokens.glyphColors(colors, MonarchPriorityLevel.Low).mark)
            assertEquals(colors.textMuted, tokens.glyphColors(colors, MonarchPriorityLevel.None).mark)
            assertEquals(colors.borderStrong, tokens.glyphColors(colors, MonarchPriorityLevel.Low).empty)
            assertEquals(colors.bgSurface, tokens.glyphColors(colors, MonarchPriorityLevel.Urgent).cutout)
        }
    }
}
