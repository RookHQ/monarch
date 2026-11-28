package com.rookhq.monarch.components.datagrid

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.datagrid.editablecell.EditableCellMessages
import com.rookhq.monarch.components.datagrid.editablecell.EditableCellOutcome
import com.rookhq.monarch.components.datagrid.editablecell.MonarchEditableCell
import com.rookhq.monarch.components.datagrid.editablecell.MonarchEditableCellEditor
import com.rookhq.monarch.components.datagrid.editablecell.MonarchEditableCellLabels
import com.rookhq.monarch.components.datagrid.editablecell.MonarchEditableCellTokens
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private val stages = MonarchEditableCellEditor.Select(
    listOf(
        MonarchSelectOption("open", "Open"),
        MonarchSelectOption("won", "Won"),
        MonarchSelectOption("lost", "Lost", enabled = false),
    ),
)

/** 1 January 2026, midnight UTC. */
private const val NewYear = 1_767_225_600_000L

internal object MonarchEditableCellFixtures : ComponentFixtures {
    override val id = "editable-cell"
    override val fixtures = listOf(
        fixture("text and number") {
            MonarchEditableCell(
                value = "Acme",
                label = "Company",
                editor = MonarchEditableCellEditor.Text,
                onCommit = {},
            )
            MonarchEditableCell(
                value = 1250.5,
                label = "Amount",
                editor = MonarchEditableCellEditor.Number(min = 0.0),
                onCommit = {},
            )
        },
        fixture("select and date") {
            MonarchEditableCell(value = "open", label = "Stage", editor = stages, onCommit = {})
            MonarchEditableCell(
                value = NewYear,
                label = "Close date",
                editor = MonarchEditableCellEditor.Date(),
                onCommit = {},
            )
        },
        fixture("empty with placeholder") {
            MonarchEditableCell<String>(
                value = null,
                label = "Notes",
                editor = MonarchEditableCellEditor.Text,
                onCommit = {},
                placeholder = "Add a note",
            )
        },
        fixture("editing") {
            MonarchEditableCell(
                value = "Ada",
                label = "Owner",
                editor = MonarchEditableCellEditor.Text,
                onCommit = {},
                startEditing = true,
            )
        },
        fixture("disabled") {
            MonarchEditableCell(
                value = "Locked",
                label = "Region",
                editor = MonarchEditableCellEditor.Text,
                onCommit = {},
                enabled = false,
            )
        },
        fixture("long text") {
            MonarchEditableCell(
                value = "A company name that is far longer than the column it sits in",
                label = "Company",
                editor = MonarchEditableCellEditor.Text,
                onCommit = {},
            )
        },
    )
}

private val messages = EditableCellMessages(
    required = "required",
    invalidNumber = "nan",
    belowMin = { "below $it" },
    aboveMax = { "above $it" },
)

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchEditableCellTest {
    @get:Rule
    val rule = createComposeRule()

    private val field get() = rule.onNode(hasSetTextAction())
    private fun cell(label: String) = rule.onNodeWithContentDescription(label)
    private fun error() = field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error)

    @Test
    fun inViewModeTheCellIsOneButtonWithTheLabelAndTheValue() {
        rule.setMonarchContent {
            MonarchEditableCell(
                value = "Acme",
                label = "Company",
                editor = MonarchEditableCellEditor.Text,
                onCommit = {},
            )
        }
        cell("Company").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        assertEquals("Acme", cell("Company").stateDescription())
        val click = cell("Company").fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)
        assertEquals("Edit Company", click?.label)
        cell("Company").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun aTextCellSavesTheTrimmedTextAndCloses() {
        var value by mutableStateOf<String?>("Acme")
        val committed = mutableListOf<String?>()
        val editing = mutableListOf<Boolean>()
        rule.setMonarchContent {
            MonarchEditableCell(
                value = value,
                label = "Company",
                editor = MonarchEditableCellEditor.Text,
                onCommit = {
                    committed += it
                    value = it
                },
                onEditingChange = { editing += it },
            )
        }
        cell("Company").performClick()
        field.performTextReplacement("  Globex  ")
        field.performImeAction()
        rule.waitForIdle()
        assertEquals(listOf<String?>("Globex"), committed)
        assertEquals(listOf(true, false), editing)
        field.assertDoesNotExist()
        assertEquals("Globex", cell("Company").stateDescription())
    }

    @Test
    fun anUnchangedValueIsNotCommittedAndCancelDoesNotCommit() {
        var commits = 0
        var cancels = 0
        rule.setMonarchContent {
            MonarchEditableCell(
                value = "Acme",
                label = "Company",
                editor = MonarchEditableCellEditor.Text,
                saveOnFocusLoss = false,
                onCommit = { commits++ },
                onCancel = { cancels++ },
            )
        }
        cell("Company").performClick()
        field.performTextReplacement("Acme ")
        rule.onNodeWithText("Save").performClick()
        rule.waitForIdle()
        field.assertDoesNotExist()
        cell("Company").performClick()
        field.performTextReplacement("Other")
        rule.onNodeWithText("Cancel").performClick()
        rule.waitForIdle()
        assertEquals(0, commits)
        assertEquals(1, cancels)
        field.assertDoesNotExist()
    }

    @Test
    fun aRequiredCellRefusesEmptyAndKeepsTheEditorOpen() {
        var commits = 0
        rule.setMonarchContent {
            MonarchEditableCell(
                value = "Acme",
                label = "Company",
                editor = MonarchEditableCellEditor.Text,
                onCommit = { commits++ },
                required = true,
            )
        }
        cell("Company").performClick()
        field.performTextReplacement("  ")
        field.performImeAction()
        assertEquals("Enter a value.", error())
        assertEquals(0, commits)
    }

    @Test
    fun aNumberCellParsesChecksTheRangeAndFormats() {
        var value by mutableStateOf<Double?>(1250.5)
        val committed = mutableListOf<Double?>()
        rule.setMonarchContent {
            MonarchEditableCell(
                value = value,
                label = "Amount",
                editor = MonarchEditableCellEditor.Number(min = 0.0, max = 10_000.0),
                onCommit = {
                    committed += it
                    value = it
                },
            )
        }
        assertEquals("1,250.5", cell("Amount").stateDescription())
        cell("Amount").performClick()
        assertEquals("1250.5", field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text)
        field.performTextReplacement("abc")
        field.performImeAction()
        assertEquals("Enter a number.", error())
        field.performTextReplacement("-5")
        field.performImeAction()
        assertEquals("Enter 0 or more.", error())
        field.performTextReplacement("20000")
        field.performImeAction()
        assertEquals("Enter 10,000 or less.", error())
        field.performTextReplacement("42")
        field.performImeAction()
        rule.waitForIdle()
        assertEquals(listOf<Double?>(42.0), committed)
        assertEquals("42", cell("Amount").stateDescription())
    }

    @Test
    fun aFailedCommitKeepsTheDraftAndShowsTheMessage() {
        rule.setMonarchContent {
            MonarchEditableCell(
                value = "Acme",
                label = "Company",
                editor = MonarchEditableCellEditor.Text,
                saveOnFocusLoss = false,
                onCommit = { error("The name is taken.") },
            )
        }
        cell("Company").performClick()
        field.performTextReplacement("Globex")
        rule.onNodeWithText("Save").performClick()
        rule.waitForIdle()
        assertEquals("The name is taken.", error())
        assertEquals("Globex", field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text)
    }

    @Test
    fun validateRefusesAValue() {
        var commits = 0
        rule.setMonarchContent {
            MonarchEditableCell(
                value = "Acme",
                label = "Company",
                editor = MonarchEditableCellEditor.Text,
                onCommit = { commits++ },
                validate = { if (it?.contains("!") == true) "No punctuation." else null },
            )
        }
        cell("Company").performClick()
        field.performTextReplacement("Acme!")
        field.performImeAction()
        assertEquals("No punctuation.", error())
        assertEquals(0, commits)
    }

    @Test
    fun aSelectCellOpensAMenuAndCommitsTheChoice() {
        var value by mutableStateOf<String?>("open")
        val committed = mutableListOf<String?>()
        rule.setMonarchContent {
            MonarchEditableCell(
                value = value,
                label = "Stage",
                editor = stages,
                onCommit = {
                    committed += it
                    value = it
                },
            )
        }
        assertEquals("Open", cell("Stage").stateDescription())
        cell("Stage").performClick()
        rule.onNode(
            hasText("Open") and hasClickAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected),
        )
            .assertIsSelected()
        rule.onNode(hasText("Lost") and hasClickAction()).assertIsNotEnabled()
        rule.onNode(hasText("None") and hasClickAction()).assertExists()
        rule.onNode(hasText("Won") and hasClickAction()).performClick()
        rule.waitForIdle()
        assertEquals(listOf<String?>("won"), committed)
        assertEquals("Won", cell("Stage").stateDescription())
    }

    @Test
    fun aRequiredSelectCellHasNoNoneItem() {
        rule.setMonarchContent {
            MonarchEditableCell(value = "open", label = "Stage", editor = stages, onCommit = {}, required = true)
        }
        cell("Stage").performClick()
        rule.onNode(hasText("Won") and hasClickAction()).assertExists()
        rule.onNode(hasText("None") and hasClickAction()).assertDoesNotExist()
    }

    @Test
    fun aFailedSelectCommitShowsTheErrorUnderTheCell() {
        rule.setMonarchContent {
            MonarchEditableCell(value = "open", label = "Stage", editor = stages, onCommit = { error("Locked deal.") })
        }
        cell("Stage").performClick()
        rule.onNode(hasText("Won") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Locked deal.")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        assertEquals("Open", cell("Stage").stateDescription())
    }

    @Test
    fun aDateCellShowsTheMediumDateAndOpensThePicker() {
        rule.setMonarchContent {
            MonarchEditableCell(
                value = NewYear,
                label = "Close date",
                editor = MonarchEditableCellEditor.Date(),
                onCommit = {},
            )
        }
        assertEquals("Jan 1, 2026", cell("Close date").stateDescription())
        cell("Close date").performClick()
        rule.onNodeWithText("Save").assertExists()
        rule.onNodeWithText("None").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Save").assertDoesNotExist()
    }

    @Test
    fun aDisabledCellIsPlainText() {
        rule.setMonarchContent {
            MonarchEditableCell(
                value = "Locked",
                label = "Region",
                editor = MonarchEditableCellEditor.Text,
                onCommit = {},
                enabled = false,
            )
        }
        rule.onNodeWithText("Locked").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        cell("Region").assertDoesNotExist()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchEditableCell(
                value = 3.0,
                label = "Seats",
                editor = MonarchEditableCellEditor.Number(min = 1.0),
                saveOnFocusLoss = false,
                onCommit = {},
                labels = MonarchEditableCellLabels(
                    edit = { "Change $it" },
                    save = "Keep",
                    belowMin = { "At least $it" },
                ),
            )
        }
        val click = cell("Seats").fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)
        assertEquals("Change Seats", click?.label)
        cell("Seats").performClick()
        field.performTextReplacement("0")
        rule.onNodeWithText("Keep").performClick()
        assertEquals("At least 1", error())
    }

    @Test
    fun saveComesBeforeCancelRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchEditableCell(
                value = "Ada",
                label = "Owner",
                editor = MonarchEditableCellEditor.Text,
                saveOnFocusLoss = false,
                onCommit = {},
                startEditing = true,
            )
        }
        rule.onNodeWithText("Save").assertIsBefore(rule.onNodeWithText("Cancel"), LayoutDirection.Rtl)
        rule.onNodeWithText("Save").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun longTextWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchEditableCell(
                value = "A company name that is far longer than the column",
                label = "Company",
                editor = MonarchEditableCellEditor.Text,
                onCommit = {},
                modifier = Modifier.width(200.dp),
                enabled = false,
            )
        }
        // A disabled cell is plain text; an enabled one draws the same Text inside its button.
        rule.onNodeWithText("A company name that is far longer than the column").assertTextFullyVisible()

    }

    @Test
    fun parseNumberReadsDotsCommasAndBlank() {
        val tokens = MonarchEditableCellTokens
        assertEquals(1234.5, tokens.parseNumber(" 1 234.5 ", Locale.US).getOrNull())
        assertEquals(2.5, tokens.parseNumber("2,5", Locale.GERMANY).getOrNull())
        assertEquals(null, tokens.parseNumber("  ", Locale.US).getOrThrow())
        assertTrue(tokens.parseNumber("12a", Locale.US).isFailure)
        assertTrue(tokens.parseNumber("Infinity", Locale.US).isFailure)
        assertEquals("1250.5", tokens.numberDraft(1250.50))
        assertEquals("3", tokens.numberDraft(3.0))
        assertEquals("", tokens.numberDraft(null))
    }

    @Test
    fun decideFollowsTheWebOrder() {
        val tokens = MonarchEditableCellTokens
        assertEquals(EditableCellOutcome.Refused("required"), tokens.decide(null, "a", true, null, null, messages))
        assertEquals(EditableCellOutcome.Unchanged, tokens.decide("a", "a", false, "below 1", null, messages))
        assertEquals(EditableCellOutcome.Refused("below 1"), tokens.decide("b", "a", false, "below 1", null, messages))
        assertEquals(EditableCellOutcome.Refused("no"), tokens.decide("b", "a", false, null, { "no" }, messages))
        assertEquals(EditableCellOutcome.Commit("b"), tokens.decide("b", "a", false, null, { null }, messages))
        assertEquals(
            EditableCellOutcome.Commit<String>(null),
            tokens.decide<String>(null, "a", false, null, null, messages),
        )
    }

    @Test
    fun rangeErrorsUseTheFormattedLimit() {
        val tokens = MonarchEditableCellTokens
        val number = MonarchEditableCellEditor.Number(min = 1000.0, max = 2000.0)
        assertEquals("below 1,000", tokens.rangeError(number, 5.0, Locale.US, messages))
        assertEquals("above 2,000", tokens.rangeError(number, 2500.0, Locale.US, messages))
        assertEquals(null, tokens.rangeError(number, 1500.0, Locale.US, messages))
        val date = MonarchEditableCellEditor.Date(min = NewYear)
        assertEquals("below Jan 1, 2026", tokens.rangeError(date, NewYear - 1, Locale.US, messages))
        assertTrue(tokens.selectable(date, NewYear))
        assertTrue(!tokens.selectable(date, NewYear - 1))
    }

    @Test
    fun theViewColoursComeFromRolesInEveryTheme() {
        for (theme in AllThemes) {
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchEditableCellTokens
            assertEquals(colors.textDefault, tokens.viewText(empty = false, enabled = true, colors))
            assertEquals(colors.textSubtle, tokens.viewText(empty = true, enabled = true, colors))
            assertEquals(colors.textDisabled, tokens.viewText(empty = false, enabled = false, colors))
            assertEquals(colors.dangerStrong, tokens.errorText(colors))
            assertNotEquals(tokens.viewText(true, true, colors), tokens.viewText(false, true, colors))
        }
    }
}
