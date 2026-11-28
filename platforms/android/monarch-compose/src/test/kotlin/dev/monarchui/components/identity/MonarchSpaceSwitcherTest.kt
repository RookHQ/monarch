package dev.monarchui.components.identity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.identity.spaceswitcher.MonarchSpaceOption
import dev.monarchui.components.identity.spaceswitcher.MonarchSpaceSwitcher
import dev.monarchui.components.identity.spaceswitcher.MonarchSpaceSwitcherLabels
import dev.monarchui.components.identity.spaceswitcher.MonarchSpaceSwitcherRules
import dev.monarchui.components.identity.spaceswitcher.MonarchSpaceSwitcherTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Spaces = listOf(
    MonarchSpaceOption("eng", "Engineering", "18 members"),
    MonarchSpaceOption("ops", "Operações", "4 members"),
)

internal object MonarchSpaceSwitcherFixtures : ComponentFixtures {
    override val id = "space-switcher"
    override val fixtures = listOf(
        fixture("all spaces") { MonarchSpaceSwitcher(spaces = Spaces, selection = null, onSelectionChange = {}) },
        fixture("one space, small") {
            MonarchSpaceSwitcher(
                spaces = Spaces,
                selection = "eng",
                onSelectionChange = {},
                size = MonarchButtonSize.Sm,
            )
        },
        fixture("no choice, icon only") {
            MonarchSpaceSwitcher(
                spaces = Spaces,
                selection = null,
                onSelectionChange = {},
                allowsAll = false,
                iconOnly = true,
            )
        },
        fixture("disabled, loading") {
            MonarchSpaceSwitcher(
                spaces = emptyList(),
                selection = null,
                onSelectionChange = {},
                enabled = false,
                loading = true,
            )
        },
        fixture("long name") {
            MonarchSpaceSwitcher(
                spaces = listOf(MonarchSpaceOption("x", "Customer success and support for every region")),
                selection = "x",
                onSelectionChange = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchSpaceSwitcherTest {
    @get:Rule
    val rule = createComposeRule()

    private val radio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    @Test
    fun theButtonIsNamedSpaceWithTheCurrentSpaceAsItsValue() {
        rule.setMonarchContent {
            MonarchSpaceSwitcher(spaces = Spaces, selection = "eng", onSelectionChange = {}, iconOnly = true)
        }
        val button = rule.onNodeWithContentDescription("Space")
        button.assertTouchTargetAtLeast48dp(rule.density)
        assertEquals("Engineering", button.stateDescription())
        rule.onNodeWithText("Engineering").assertDoesNotExist()
    }

    @Test
    fun anUnknownIdReadsAsAllSpacesAndWithoutAllAsChooseASpace() {
        var allowsAll by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchSpaceSwitcher(spaces = Spaces, selection = "gone", onSelectionChange = {}, allowsAll = allowsAll)
        }
        assertEquals("All spaces", rule.onNodeWithContentDescription("Space").stateDescription())
        allowsAll = false
        rule.waitForIdle()
        assertEquals("Choose a Space", rule.onNodeWithContentDescription("Space").stateDescription())
    }

    @Test
    fun theSheetListsAllSpacesFirstAndMarksTheChosenRow() {
        var selection by mutableStateOf<String?>(null)
        rule.setMonarchContent {
            MonarchSpaceSwitcher(spaces = Spaces, selection = selection, onSelectionChange = { selection = it })
        }
        rule.onNodeWithContentDescription("Space").performClick()
        rule.onNode(radio and hasContentDescription("All spaces")).assertIsSelected()
        rule.onNode(radio and hasContentDescription("Engineering, 18 members"))
            .assertIsNotSelected()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("eng", selection)
        rule.onNode(radio and hasContentDescription("All spaces")).assertDoesNotExist()
        rule.onNodeWithContentDescription("Space").performClick()
        rule.onNode(radio and hasContentDescription("All spaces")).performClick()
        assertNull(selection)
    }

    @Test
    fun searchIgnoresCaseAndAccentsAndSaysWhenNothingMatches() {
        rule.setMonarchContent {
            MonarchSpaceSwitcher(spaces = Spaces, selection = null, onSelectionChange = {})
        }
        rule.onNodeWithContentDescription("Space").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("OPERA")
        rule.onNode(radio and hasContentDescription("Operações, 4 members")).assertExists()
        rule.onNode(radio and hasContentDescription("Engineering, 18 members")).assertDoesNotExist()
        rule.onNode(radio and hasContentDescription("All spaces")).assertDoesNotExist()
        rule.onNode(hasSetTextAction()).performTextInput("zzz")
        rule.onNodeWithText("No spaces match.").assertExists()
    }

    @Test
    fun newSpaceAndManageCloseTheSheetAndCallBack() {
        val calls = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchSpaceSwitcher(
                spaces = Spaces,
                selection = null,
                onSelectionChange = {},
                onCreate = { calls += "create" },
                onManage = { calls += "manage" },
            )
        }
        rule.onNodeWithContentDescription("Space").performClick()
        rule.onNodeWithText("New Space").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("New Space").assertDoesNotExist()
        rule.onNodeWithContentDescription("Space").performClick()
        rule.onNodeWithText("Manage Spaces").performClick()
        assertEquals(listOf("create", "manage"), calls)
    }

    @Test
    fun loadingIsSaidInTheSheetAndCancelChangesNothing() {
        var changes = 0
        rule.setMonarchContent {
            MonarchSpaceSwitcher(
                spaces = emptyList(),
                selection = null,
                onSelectionChange = { changes++ },
                loading = true,
            )
        }
        rule.onNodeWithContentDescription("Space").performClick()
        rule.onNodeWithText("Loading spaces…").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Loading spaces…").assertDoesNotExist()
        assertEquals(0, changes)
    }

    @Test
    fun nothingIsDrawnWhenEmptyAndADisabledButtonDoesNotOpen() {
        var hides by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchSpaceSwitcher(
                spaces = emptyList(),
                selection = null,
                onSelectionChange = {},
                enabled = false,
                hidesWhenEmpty = hides,
            )
        }
        rule.onNodeWithContentDescription("Space").assertDoesNotExist()
        hides = false
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Space").assertIsNotEnabled().performClick()
        rule.onNodeWithText("Cancel").assertDoesNotExist()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchSpaceSwitcher(
                spaces = Spaces,
                selection = null,
                onSelectionChange = {},
                labels = MonarchSpaceSwitcherLabels(label = "Bereich", all = "Alle Bereiche", cancel = "Abbrechen"),
            )
        }
        assertEquals("Alle Bereiche", rule.onNodeWithContentDescription("Bereich").stateDescription())
        rule.onNodeWithContentDescription("Bereich").performClick()
        rule.onNodeWithText("Abbrechen").assertExists()
    }

    @Test
    fun theNameComesAfterTheIconInRightToLeftAndFitsAtTheLargestScale() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl, fontScale = LargestFontScale) {
            MonarchSpaceSwitcher(spaces = Spaces, selection = "eng", onSelectionChange = {})
        }
        rule.onNodeWithContentDescription("Space").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theRulesMatchChooseAndHide() {
        val rules = MonarchSpaceSwitcherRules
        assertTrue(rules.matches(" opera ", "Operações"))
        assertTrue(rules.matches("", "x"))
        assertTrue(rules.matches("18", "Engineering", "18 members"))
        assertFalse(rules.matches("x", null))
        assertNull(rules.current(Spaces, "gone"))
        assertFalse(rules.accepts(null, allowsAll = false))
        assertTrue(rules.hidden(emptyList(), loading = false, hidesWhenEmpty = true))
        assertFalse(rules.hidden(emptyList(), loading = true, hidesWhenEmpty = true))
        assertEquals("Ops", rules.spoken("Ops", " "))
    }

    @Test
    fun theColoursAreRolesInEveryTheme() {
        AllThemes.forEach { tokens ->
            val colors = MonarchColors(tokens.colors)
            assertEquals(colors.bgRaised, MonarchSpaceSwitcherTokens.sheetColor(colors))
            assertEquals(colors.textDefault, MonarchSpaceSwitcherTokens.nameColor(colors))
            assertEquals(colors.textMuted, MonarchSpaceSwitcherTokens.detailColor(colors))
        }
    }
}
