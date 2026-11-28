package dev.monarchui.components.actions

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
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

/**
 * The web ToggleButton is the Material 3 icon toggle buttons and FilterChip,
 * styled by MonarchTheme. Monarch ships no type for it.
 */
internal object ToggleButtonFixtures : ComponentFixtures {
    override val id = "toggle-button"
    override val fixtures = listOf(true, false).flatMap { checked ->
        val state = if (checked) "on" else "off"
        listOf(
            fixture("tonal icon toggle, $state") {
                FilledTonalIconToggleButton(checked = checked, onCheckedChange = {}) {
                    Icon(imageVector = TestIcon, contentDescription = "Bold")
                }
            },
            fixture("outlined icon toggle, $state") {
                OutlinedIconToggleButton(checked = checked, onCheckedChange = {}) {
                    Icon(imageVector = TestIcon, contentDescription = "Bold")
                }
            },
            fixture("filter chip, $state") {
                FilterChip(selected = checked, onClick = {}, label = { Text("Show archived") })
            },
            fixture("disabled, $state") {
                Row {
                    FilledTonalIconToggleButton(checked = checked, onCheckedChange = {}, enabled = false) {
                        Icon(imageVector = TestIcon, contentDescription = "Bold")
                    }
                    FilterChip(selected = checked, onClick = {}, label = { Text("Show archived") }, enabled = false)
                }
            },
        )
    } + listOf(
        fixture("filter chip with an icon and a long label") {
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text("Show archived and deleted conversations") },
                leadingIcon = { Icon(imageVector = TestIcon, contentDescription = null) },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class ToggleButtonTest {
    @get:Rule
    val rule = createComposeRule()

    private val isCheckbox = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)

    @Test
    fun anIconToggleButtonExposesItsOnAndOffStateAndToggles() {
        rule.setMonarchContent {
            var bold by remember { mutableStateOf(false) }
            FilledTonalIconToggleButton(checked = bold, onCheckedChange = { bold = it }) {
                Icon(imageVector = TestIcon, contentDescription = "Bold")
            }
        }
        val button = rule.onNodeWithContentDescription("Bold")
        button.assert(isCheckbox).assertIsOff().performClick()
        button.assertIsOn().performClick()
        button.assertIsOff()
    }

    @Test
    fun theOutlinedIconToggleButtonExposesTheSameState() {
        rule.setMonarchContent {
            var bold by remember { mutableStateOf(true) }
            OutlinedIconToggleButton(checked = bold, onCheckedChange = { bold = it }) {
                Icon(imageVector = TestIcon, contentDescription = "Bold")
            }
        }
        rule.onNodeWithContentDescription("Bold").assert(isCheckbox).assertIsOn().performClick().assertIsOff()
    }

    @Test
    fun aFilterChipIsSelectedAndKeepsItsLabel() {
        rule.setMonarchContent {
            var archived by remember { mutableStateOf(false) }
            FilterChip(selected = archived, onClick = { archived = !archived }, label = { Text("Show archived") })
        }
        val chip = rule.onNodeWithText("Show archived")
        chip.assertIsNotSelected().performClick()
        chip.assertIsSelected()
        // The label names the thing that is switched and does not change with the state.
        rule.onNodeWithText("Show archived").assertExists()
    }

    @Test
    fun aDisabledToggleIsAnnouncedAsDisabledAndDoesNotChange() {
        var changes = 0
        rule.setMonarchContent {
            FilledTonalIconToggleButton(checked = false, onCheckedChange = { changes++ }, enabled = false) {
                Icon(imageVector = TestIcon, contentDescription = "Bold")
            }
        }
        rule.onNodeWithContentDescription("Bold").assertIsNotEnabled().performClick()
        assertEquals(0, changes)
    }

    @Test
    fun everyFormHasATouchTargetOfAtLeast48dp() {
        rule.setMonarchContent {
            Row {
                FilledTonalIconToggleButton(checked = false, onCheckedChange = {}, Modifier.testTag("tonal")) {
                    Icon(imageVector = TestIcon, contentDescription = "Bold")
                }
                OutlinedIconToggleButton(checked = true, onCheckedChange = {}, Modifier.testTag("outlined")) {
                    Icon(imageVector = TestIcon, contentDescription = "Italic")
                }
                FilterChip(selected = false, onClick = {}, label = { Text("A") }, Modifier.testTag("chip"))
            }
        }
        listOf("tonal", "outlined", "chip").forEach { tag ->
            rule.onNodeWithTag(tag).assertTouchTargetAtLeast48dp(rule.density)
        }
    }

    @Test
    fun theChipIconIsAtTheStartInBothLayoutDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text("Show archived") },
                leadingIcon = { Icon(TestIcon, contentDescription = null, Modifier.testTag("icon")) },
            )
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithTag("icon", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Show archived", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun theChipLabelIsNotCutOffAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            FilterChip(selected = true, onClick = {}, label = { Text("Show archived") })
        }
        rule.onNodeWithText("Show archived", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun monarchThemeGivesTheTogglesTheirColoursFromTheThemeRoles() {
        var tonal: IconToggleButtonColors? = null
        var outlined: IconToggleButtonColors? = null
        var chipSelected = Color.Unspecified
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            tonal = IconButtonDefaults.filledTonalIconToggleButtonColors()
            outlined = IconButtonDefaults.outlinedIconToggleButtonColors()
            // FilterChip fills a selected chip with secondaryContainer.
            chipSelected = MaterialTheme.colorScheme.secondaryContainer
        }
        AllThemes.forEach { theme ->
            environment.tokens = theme
            rule.waitForIdle()
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.accentStrong, tonal?.checkedContainerColor, theme.id)
            assertEquals(colors.accentOnStrong, tonal?.checkedContentColor, theme.id)
            assertEquals(colors.accentSubtle, tonal?.containerColor, theme.id)
            assertEquals(colors.accentStrong, tonal?.contentColor, theme.id)
            assertEquals(colors.bgInverse, outlined?.checkedContainerColor, theme.id)
            assertEquals(colors.textInverse, outlined?.checkedContentColor, theme.id)
            assertEquals(colors.accentSubtle, chipSelected, theme.id)
            // On is not colour alone: the fill and the content colour change together.
            assertTrue(tonal?.checkedContainerColor != tonal?.containerColor, theme.id)
        }
    }
}
