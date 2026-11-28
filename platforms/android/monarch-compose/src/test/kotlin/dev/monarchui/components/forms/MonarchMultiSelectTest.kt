package dev.monarchui.components.forms

import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.forms.combobox.MonarchComboboxOption
import dev.monarchui.components.forms.multiselect.MonarchMultiSelect
import dev.monarchui.components.forms.multiselect.MonarchMultiSelectLabels
import dev.monarchui.components.forms.multiselect.MonarchMultiSelectTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.onNodeWithTextOrDescription
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val teams = listOf(
    MonarchComboboxOption("design", "Design", group = "Product"),
    MonarchComboboxOption("eng", "Engineering", description = "Web and mobile", group = "Product"),
    MonarchComboboxOption("legal", "Legal", enabled = false, group = "Company"),
    MonarchComboboxOption("sales", "Sales", hint = "12", group = "Company"),
)

internal object MonarchMultiSelectFixtures : ComponentFixtures {
    override val id = "multi-select"
    override val fixtures = listOf(
        fixture("empty") {
            MonarchMultiSelect(label = "Teams", options = teams, selected = emptyList(), onSelectedChange = {})
        },
        fixture("chosen with remove buttons and a limit") {
            MonarchMultiSelect(
                label = "Teams",
                options = teams,
                selected = listOf("design", "sales"),
                onSelectedChange = {},
                maxSelections = 3,
                removeIcon = { Icon(TestIcon, contentDescription = null) },
            )
        },
        fixture("chosen without remove buttons") {
            MonarchMultiSelect(label = "Teams", options = teams, selected = listOf("eng"), onSelectedChange = {})
        },
        fixture("required with error") {
            MonarchMultiSelect(
                label = "Teams",
                options = teams,
                selected = emptyList(),
                onSelectedChange = {},
                required = true,
                errorText = "Choose at least one team.",
            )
        },
        fixture("disabled") {
            MonarchMultiSelect(
                label = "Teams",
                options = teams,
                selected = listOf("design"),
                onSelectedChange = {},
                enabled = false,
                removeIcon = { Icon(TestIcon, contentDescription = null) },
            )
        },
        fixture("long labels") {
            MonarchMultiSelect(
                label = "The teams that review every change before it is released",
                options = listOf(MonarchComboboxOption("x", "A team with a name far longer than the field")),
                selected = listOf("x"),
                onSelectedChange = {},
                helperText = "They are notified when a change is ready.",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchMultiSelectTest {
    @get:Rule
    val rule = createComposeRule()

    /** A row of the sheet: it has a toggle state, unlike a tag with the same text. */
    private fun row(text: String) =
        rule.onNode(hasText(text) and SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState))

    @Test
    fun theButtonIsNamedByTheLabelAndReadsTheChosenValues() {
        rule.setMonarchContent {
            MonarchMultiSelect(
                label = "Teams",
                options = teams,
                selected = listOf("sales", "design"),
                onSelectedChange = {},
            )
        }
        val button = rule.onNodeWithContentDescription("Teams")
        assertEquals("Sales, Design", button.stateDescription())
        button.assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun nothingChosenReadsNoneAndShowsThePrompt() {
        rule.setMonarchContent {
            MonarchMultiSelect(label = "Teams", options = teams, selected = emptyList(), onSelectedChange = {})
        }
        assertEquals("None", rule.onNodeWithContentDescription("Teams").stateDescription())
        rule.onNodeWithText("Select…").assertExists()
    }

    @Test
    fun theSheetTogglesValuesInPickOrderAndStaysOpen() {
        var chosen by mutableStateOf(listOf("sales"))
        rule.setMonarchContent {
            MonarchMultiSelect(label = "Teams", options = teams, selected = chosen, onSelectedChange = { chosen = it })
        }
        rule.onNodeWithContentDescription("Teams").performClick()
        rule.onNode(hasText("Product") and isHeading()).assertExists()
        row("Sales").assertIsOn()
        row("Legal").assertIsNotEnabled()
        row("Design").assertIsOff().performClick()
        rule.waitForIdle()
        assertEquals(listOf("sales", "design"), chosen)
        row("Sales").performClick()
        rule.waitForIdle()
        assertEquals(listOf("design"), chosen)
        row("Design").assertIsOn().assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Done").performClick()
        rule.waitForIdle()
        row("Design").assertDoesNotExist()
    }

    @Test
    fun searchingFiltersTheList() {
        rule.setMonarchContent {
            MonarchMultiSelect(label = "Teams", options = teams, selected = emptyList(), onSelectedChange = {})
        }
        rule.onNodeWithContentDescription("Teams").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("eng")
        rule.waitForIdle()
        row("Engineering").assertExists()
        row("Design").assertDoesNotExist()
    }

    @Test
    fun theLimitDisablesOtherRowsAndShowsTheCount() {
        var chosen by mutableStateOf(listOf("design", "sales"))
        rule.setMonarchContent {
            MonarchMultiSelect(
                label = "Teams",
                options = teams,
                selected = chosen,
                onSelectedChange = { chosen = it },
                maxSelections = 2,
            )
        }
        rule.onNodeWithText("2 of 2 selected").assertExists()
        // The field shows the chosen values as tags with their own remove buttons: open it by its action, not by a
        // tap in its middle.
        rule.onNodeWithContentDescription("Teams").performSemanticsAction(SemanticsActions.OnClick)
        row("Engineering").assertIsNotEnabled()
        row("Design").performClick()
        rule.waitForIdle()
        assertEquals(listOf("sales"), chosen)
        row("Engineering").performClick()
        rule.waitForIdle()
        assertEquals(listOf("sales", "eng"), chosen)
    }

    @Test
    fun createAddsTheNewOption() {
        var chosen by mutableStateOf(emptyList<String>())
        rule.setMonarchContent {
            MonarchMultiSelect(
                label = "Teams",
                options = teams,
                selected = chosen,
                onSelectedChange = { chosen = it },
                onCreate = { MonarchComboboxOption("new-$it", it) },
            )
        }
        rule.onNodeWithContentDescription("Teams").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("Support")
        rule.waitForIdle()
        rule.onNodeWithText("Create “Support”").performClick()
        rule.waitForIdle()
        assertEquals(listOf("new-Support"), chosen)
    }

    @Test
    fun tagsRemoveTheirValueAndAreNamed() {
        var chosen by mutableStateOf(listOf("design", "sales"))
        rule.setMonarchContent {
            MonarchMultiSelect(
                label = "Teams",
                options = teams,
                selected = chosen,
                onSelectedChange = { chosen = it },
                removeIcon = { Icon(TestIcon, contentDescription = null) },
            )
        }
        rule.onNodeWithContentDescription("Remove Design").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(listOf("sales"), chosen)
    }

    @Test
    fun disabledDoesNothing() {
        var changes = 0
        rule.setMonarchContent {
            MonarchMultiSelect(
                label = "Teams",
                options = teams,
                selected = listOf("design"),
                onSelectedChange = { changes++ },
                enabled = false,
                removeIcon = { Icon(TestIcon, contentDescription = null) },
            )
        }
        rule.onNodeWithContentDescription("Teams").assertIsNotEnabled().performClick()
        rule.onNodeWithContentDescription("Remove Design").assertIsNotEnabled()
        rule.waitForIdle()
        row("Design").assertDoesNotExist()
        assertEquals(0, changes)
    }

    @Test
    fun theErrorIsSetOnTheButton() {
        rule.setMonarchContent {
            MonarchMultiSelect(
                label = "Teams",
                options = teams,
                selected = emptyList(),
                onSelectedChange = {},
                errorText = "Choose a team.",
            )
        }
        val error = rule.onNodeWithContentDescription("Teams").fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.Error)
        assertEquals("Choose a team.", error)
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchMultiSelect(
                label = "Teams",
                options = teams,
                selected = listOf("design"),
                onSelectedChange = {},
                maxSelections = 3,
                removeIcon = { Icon(TestIcon, contentDescription = null) },
                labels = MonarchMultiSelectLabels(
                    change = "Change",
                    count = { count, max -> "$count/$max" },
                    remove = { "Drop $it" },
                ),
            )
        }
        rule.onNodeWithText("1/3").assertExists()
        rule.onNodeWithContentDescription("Drop Design").assertExists()
    }

    @Test
    fun tagsFollowTheLayoutDirection() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchMultiSelect(
                label = "Teams",
                options = teams,
                selected = listOf("design", "sales"),
                onSelectedChange = {},
            )
        }
        rule.onNodeWithText("Design").assertIsBefore(rule.onNodeWithText("Sales"), LayoutDirection.Rtl)
    }

    @Test
    fun longTextIsFullyVisibleAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchMultiSelect(
                label = "Teams",
                options = listOf(MonarchComboboxOption("x", "A team with a long name")),
                selected = listOf("x"),
                onSelectedChange = {},
            )
        }
        rule.onNodeWithText("A team with a long name").assertTextFullyVisible()
    }

    @Test
    fun toggleKeepsPickOrderAndTheLimit() {
        val tokens = MonarchMultiSelectTokens
        assertEquals(listOf("a", "b"), tokens.toggle(listOf("a"), "b", null))
        assertEquals(listOf("b"), tokens.toggle(listOf("a", "b"), "a", null))
        assertEquals(listOf("a"), tokens.toggle(listOf("a"), "b", 1))
        assertTrue(tokens.atLimit(listOf("a"), 1))
        assertFalse(tokens.atLimit(listOf("a"), null))
        val option = MonarchComboboxOption("a", "A")
        assertTrue(tokens.rowEnabled(option, chosen = true, atLimit = true, enabled = true))
        assertFalse(tokens.rowEnabled(option, chosen = false, atLimit = true, enabled = true))
        assertEquals(listOf("a" to "A", "zz" to "zz"), tokens.chosenLabels(listOf("a", "zz"), listOf(option)))
    }

    @Test
    fun coloursComeFromRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchMultiSelectTokens
            assertEquals(colors.textDefault, tokens.labelColor(true, colors))
            assertEquals(colors.textDisabled, tokens.labelColor(false, colors))
            assertEquals(colors.textMuted, tokens.secondaryColor(true, colors))
            assertEquals(colors.bgSurface, tokens.sheetColor(colors))
        }
    }
}
