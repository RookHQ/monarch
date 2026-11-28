package dev.monarchui.components.workflow

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.forms.select.MonarchSelectOption
import dev.monarchui.components.workflow.conditioneditor.MonarchCondition
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionEditor
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionEditorInput
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionEditorLabels
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionEditorTokens
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionEvaluation
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionField
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionFieldType
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionMatch
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator as Op
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionProblem as Problem
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionValue as Cv
import dev.monarchui.components.workflow.conditioneditor.monarchConditionEditorFormatDay
import dev.monarchui.components.workflow.conditioneditor.monarchConditionEditorInput
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val Day = 86_400_000L
private const val Nov14 = 1_794_614_400_000L // 2026-11-14T00:00:00Z

private val Status = MonarchConditionField(
    key = "status",
    label = "Status",
    type = MonarchConditionFieldType.Select,
    options = listOf(MonarchSelectOption("open", "Open"), MonarchSelectOption("closed", "Closed")),
)
private val Title = MonarchConditionField(key = "title", label = "Title", type = MonarchConditionFieldType.Text)
private val Amount = MonarchConditionField(
    key = "amount",
    label = "Amount",
    type = MonarchConditionFieldType.Number,
    placeholder = "Amount",
)
private val Created = MonarchConditionField(key = "created", label = "Created", type = MonarchConditionFieldType.Date)
private val Labels = MonarchConditionField(
    key = "labels",
    label = "Labels",
    type = MonarchConditionFieldType.Tags,
    options = listOf(MonarchSelectOption("vip", "vip")),
)
private val Flagged =
    MonarchConditionField(key = "flagged", label = "Flagged", type = MonarchConditionFieldType.Boolean)
private val Later = MonarchConditionField(
    key = "later",
    label = "Later question",
    type = MonarchConditionFieldType.Text,
    enabled = false,
    disabledReason = "comes after this question",
    group = "Form",
)
private val Fields = listOf(Status, Title, Amount, Created, Labels, Flagged, Later)

private fun condition(
    field: MonarchConditionField,
    operator: Op,
    value: Cv? = null,
    id: String = "c1",
) = MonarchCondition(id, field.key, operator, value)

internal object MonarchConditionEditorFixtures : ComponentFixtures {
    override val id = "condition-editor"
    override val fixtures = listOf(
        fixture("empty") { MonarchConditionEditor(value = emptyList(), onValueChange = {}, fields = Fields) },
        fixture("text value at the limit") {
            MonarchConditionEditor(
                value = listOf(condition(Title, Op.Contains, Cv.Text("report"))),
                onValueChange = {},
                fields = Fields,
                maxConditions = 1,
            )
        },
        fixture("one option") {
            MonarchConditionEditor(
                value = listOf(condition(Status, Op.Eq, Cv.Text("open"))),
                onValueChange = {},
                fields = Fields,
                maxConditions = 1,
            )
        },
        fixture("several options") {
            MonarchConditionEditor(
                value = listOf(condition(Status, Op.In, Cv.Values(listOf("open")))),
                onValueChange = {},
                fields = Fields,
                maxConditions = 1,
            )
        },
        fixture("tags") {
            MonarchConditionEditor(
                value = listOf(condition(Labels, Op.Contains, Cv.Values(listOf("vip")))),
                onValueChange = {},
                fields = Fields,
                maxConditions = 1,
            )
        },
        fixture("day") {
            MonarchConditionEditor(
                value = listOf(condition(Created, Op.Before, Cv.Date(Nov14))),
                onValueChange = {},
                fields = Fields,
                maxConditions = 1,
            )
        },
        fixture("no value, disabled") {
            MonarchConditionEditor(
                value = listOf(condition(Flagged, Op.IsTrue)),
                onValueChange = {},
                fields = Fields,
                enabled = false,
            )
        },
        fixture("number with an error") {
            MonarchConditionEditor(
                value = listOf(condition(Amount, Op.Gte, Cv.Number("abc"))),
                onValueChange = {},
                fields = Fields,
                maxConditions = 1,
                showErrors = true,
            )
        },
        fixture("removed field with an error") {
            MonarchConditionEditor(
                value = listOf(MonarchCondition("c1", "gone", Op.IsEmpty)),
                onValueChange = {},
                fields = Fields,
                maxConditions = 1,
                showErrors = true,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchConditionEditorTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun addAppendsAConditionOnTheFirstEnabledFieldWithAFreshId() {
        var result: List<MonarchCondition>? = null
        val existing = listOf(condition(Flagged, Op.IsTrue, id = "condition-1"))
        rule.setMonarchContent {
            MonarchConditionEditor(
                value = existing,
                onValueChange = { result = it },
                fields = Fields,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
        rule.onNodeWithText("Add condition").assertTouchTargetAtLeast48dp(rule.density).performClick()
        val added = result!!.last()
        assertEquals("condition-2", added.id)
        assertEquals("status", added.field)
        assertEquals(Op.Eq, added.operator)
        assertEquals(Cv.Text(""), added.value)
    }

    @Test
    fun defaultFieldAndCreateIdAreUsed() {
        var result: List<MonarchCondition>? = null
        rule.setMonarchContent {
            MonarchConditionEditor(
                value = emptyList(),
                onValueChange = { result = it },
                fields = Fields,
                defaultField = "amount",
                createId = { "custom" },
            )
        }
        rule.onNodeWithText("No conditions. This applies every time.").assertExists()
        rule.onNodeWithText("Add condition").performClick()
        assertEquals(listOf(MonarchCondition("custom", "amount", Op.Eq, Cv.Number(""))), result)
    }

    @Test
    fun eachConditionHasAHeadingAndANamedRemoveButton() {
        var result: List<MonarchCondition>? = null
        val two = listOf(
            condition(Flagged, Op.IsTrue, id = "a"),
            condition(Flagged, Op.IsFalse, id = "b"),
        )
        rule.setMonarchContent {
            MonarchConditionEditor(
                value = two,
                onValueChange = { result = it },
                fields = Fields,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
        rule.onNode(hasText("Condition 2") and isHeading()).assertExists()
        rule.onNodeWithContentDescription("Remove condition 1").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithContentDescription("Remove condition 2").performClick()
        assertEquals(listOf("a"), result!!.map { it.id })
    }

    @Test
    fun theMatchSwitchShowsFromTwoConditionsAndReportsTheChoice() {
        var chosen: MonarchConditionMatch? = null
        val two = listOf(
            condition(Flagged, Op.IsTrue, id = "a"),
            condition(Flagged, Op.IsFalse, id = "b"),
        )
        rule.setMonarchContent {
            MonarchConditionEditor(
                value = two,
                onValueChange = {},
                fields = Fields,
                modifier = Modifier.verticalScroll(rememberScrollState()),
                match = MonarchConditionMatch.All,
                onMatchChange = { chosen = it },
            )
        }
        rule.onNodeWithText("Match").assertExists()
        rule.onNodeWithText("of these conditions").assertExists()
        rule.onNodeWithText("all").assertIsSelected()
        rule.onNodeWithText("any").performClick()
        assertEquals(MonarchConditionMatch.Any, chosen)
    }

    @Test
    fun noMatchSwitchForOneCondition() {
        rule.setMonarchContent {
            MonarchConditionEditor(
                value = listOf(condition(Flagged, Op.IsTrue)),
                onValueChange = {},
                fields = Fields,
                match = MonarchConditionMatch.All,
                onMatchChange = {},
            )
        }
        rule.onNodeWithText("Match").assertDoesNotExist()
    }

    @Test
    fun disabledEditorCannotAddOrRemove() {
        rule.setMonarchContent {
            MonarchConditionEditor(
                value = listOf(condition(Flagged, Op.IsTrue)),
                onValueChange = { error("changed") },
                fields = Fields,
                modifier = Modifier.verticalScroll(rememberScrollState()),
                enabled = false,
            )
        }
        rule.onNodeWithText("Add condition").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Remove condition 1").assertIsNotEnabled()
    }

    @Test
    fun theLimitReplacesTheAddButtonWithANote() {
        rule.setMonarchContent {
            MonarchConditionEditor(
                value = listOf(condition(Flagged, Op.IsTrue)),
                onValueChange = {},
                fields = Fields,
                modifier = Modifier.verticalScroll(rememberScrollState()),
                maxConditions = 1,
            )
        }
        rule.onNodeWithText("Add condition").assertDoesNotExist()
        rule.onNodeWithText("You can add up to 1 condition.").assertExists()
    }

    @Test
    fun showErrorsPutsTheProblemOnTheValue() {
        rule.setMonarchContent {
            MonarchConditionEditor(
                value = listOf(condition(Amount, Op.Gte, Cv.Number("abc"))),
                onValueChange = {},
                fields = Fields,
                modifier = Modifier.verticalScroll(rememberScrollState()),
                showErrors = true,
            )
        }
        rule.onNodeWithText("Enter a number.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun errorsStayHiddenUntilAsked() {
        rule.setMonarchContent {
            MonarchConditionEditor(
                value = listOf(condition(Amount, Op.Gte, Cv.Number(""))),
                onValueChange = {},
                fields = Fields,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
        rule.onNodeWithText("Enter a value.", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchConditionEditor(
                value = listOf(condition(Flagged, Op.IsTrue)),
                onValueChange = {},
                fields = Fields,
                modifier = Modifier.verticalScroll(rememberScrollState()),
                labels = MonarchConditionEditorLabels(
                    condition = { "Rule $it" },
                    removeItem = { "Delete rule $it" },
                    add = "Add rule",
                    operators = mapOf(Op.IsTrue to "is checked"),
                ),
            )
        }
        rule.onNodeWithText("Rule 1").assertExists()
        rule.onNodeWithContentDescription("Delete rule 1").assertExists()
        rule.onNodeWithText("Add rule").assertIsEnabled()
        rule.onNodeWithText("is checked", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theHeadingComesBeforeTheRemoveButtonRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchConditionEditor(
                value = listOf(condition(Flagged, Op.IsTrue)),
                onValueChange = {},
                fields = Fields,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
        rule.onNodeWithText("Condition 1").assertIsBefore(
            rule.onNodeWithContentDescription("Remove condition 1"),
            LayoutDirection.Rtl,
        )
    }

    @Test
    fun theHeadingIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchConditionEditor(
                value = listOf(condition(Flagged, Op.IsTrue)),
                onValueChange = {},
                fields = Fields,
                modifier = Modifier.verticalScroll(rememberScrollState()),
                labels = MonarchConditionEditorLabels(
                    condition = { "Condition $it of the routing rule for new tickets" },
                ),
            )
        }
        rule.onNodeWithText("Condition 1 of the routing rule for new tickets").assertTextFullyVisible()
    }

    @Test
    fun problemsFollowTheWebRules() {
        assertEquals(Problem.Field, MonarchCondition("x", "gone", Op.Eq).problem(Fields))
        assertNull(condition(Title, Op.IsEmpty).problem(Fields))
        assertEquals(Problem.Value, condition(Title, Op.Eq, Cv.Text(" ")).problem(Fields))
        assertEquals(Problem.Number, condition(Amount, Op.Gt, Cv.Number("1x")).problem(Fields))
        assertEquals(Problem.Option, condition(Status, Op.Eq, Cv.Text("gone")).problem(Fields))
        assertNull(condition(Labels, Op.Contains, Cv.Values(listOf("free"))).problem(Fields))
        val all = listOf(
            condition(Title, Op.Eq, Cv.Text("a"), id = "ok"),
            condition(Amount, Op.Eq, Cv.Number(""), id = "bad"),
        )
        assertEquals(mapOf("bad" to Problem.Value), MonarchCondition.problems(all, Fields))
    }

    @Test
    fun evaluationFollowsTheWebRules() {
        assertTrue(condition(Title, Op.Eq, Cv.Text("Report")).matches("report"))
        assertTrue(condition(Title, Op.Contains, Cv.Text("port")).matches("Report"))
        assertTrue(condition(Title, Op.NotContains, Cv.Text("x")).matches("Report"))
        assertTrue(condition(Status, Op.In, Cv.Values(listOf("open", "new"))).matches("OPEN"))
        assertFalse(condition(Status, Op.Nin, Cv.Values(listOf("open"))).matches("open"))
        assertTrue(condition(Labels, Op.Contains, Cv.Values(listOf("vip"))).matches(listOf("VIP", "new")))
        assertTrue(condition(Amount, Op.Gte, Cv.Number(500.0)).matches(500))
        assertFalse(condition(Amount, Op.Gt, Cv.Number("500")).matches(null))
        assertTrue(condition(Amount, Op.Eq, Cv.Number("120")).matches("120.0"))
        assertTrue(condition(Created, Op.Eq, Cv.Date(Nov14)).matches(Nov14 + 5 * 3_600_000L))
        assertTrue(condition(Created, Op.Before, Cv.Date(Nov14)).matches(Nov14 - Day))
        assertTrue(condition(Created, Op.After, Cv.Date(Nov14)).matches(Nov14 + Day))
        assertTrue(condition(Flagged, Op.IsTrue).matches(true))
        assertTrue(condition(Flagged, Op.IsFalse).matches("false"))
        assertTrue(condition(Title, Op.IsEmpty).matches(emptyList<String>()))
        assertTrue(condition(Title, Op.IsNotEmpty).matches("a"))
        val rules = listOf(
            condition(Status, Op.Eq, Cv.Text("open"), id = "a"),
            condition(Amount, Op.Gt, Cv.Number("500"), id = "b"),
        )
        val record = mapOf("status" to "open", "amount" to 120)
        assertFalse(MonarchCondition.evaluate(rules, record))
        assertTrue(MonarchCondition.evaluate(rules, record, MonarchConditionMatch.Any))
        assertTrue(MonarchCondition.evaluate(emptyList(), record))
    }

    @Test
    fun changingTheOperatorKeepsOrConvertsTheValue() {
        val one = condition(Status, Op.Eq, Cv.Text("open"))
        val many = MonarchConditionEvaluation.withOperator(one, Op.In, Status)
        assertEquals(Cv.Values(listOf("open")), many.value)
        val back = MonarchConditionEvaluation.withOperator(many, Op.Neq, Status)
        assertEquals(Cv.Text("open"), back.value)
        val kept = MonarchConditionEvaluation.withOperator(one, Op.Neq, Status)
        assertEquals(one.value, kept.value)
        val none = MonarchConditionEvaluation.withOperator(one, Op.IsEmpty, Status)
        assertNull(none.value)
        val again = MonarchConditionEvaluation.withOperator(none, Op.Eq, Amount)
        assertEquals(Cv.Number(""), again.value)
    }

    @Test
    fun operatorsAndInputsFollowTheFieldType() {
        assertEquals(listOf(Op.IsTrue, Op.IsFalse), Op.forField(Flagged))
        assertEquals(listOf(Op.Eq), Op.forField(null))
        val custom = MonarchConditionField("k", "K", MonarchConditionFieldType.Text, operators = listOf(Op.Contains))
        assertEquals(listOf(Op.Contains), Op.forField(custom))
        assertEquals(MonarchConditionEditorInput.Many, monarchConditionEditorInput(Status, Op.In))
        assertEquals(MonarchConditionEditorInput.One, monarchConditionEditorInput(Status, Op.Eq))
        assertEquals(MonarchConditionEditorInput.Many, monarchConditionEditorInput(Labels, Op.Contains))
        assertEquals(MonarchConditionEditorInput.Day, monarchConditionEditorInput(Created, Op.Before))
        assertEquals(MonarchConditionEditorInput.Number, monarchConditionEditorInput(Amount, Op.Gt))
        assertEquals(MonarchConditionEditorInput.Text, monarchConditionEditorInput(Title, Op.Eq))
        assertEquals(
            listOf(Op.Gt) + Op.forField(Title),
            MonarchConditionEditorTokens.operatorChoices(Title, Op.Gt),
        )
    }

    @Test
    fun fieldOptionsListUngroupedFirstAndARemovedFieldOnTop() {
        val options = MonarchConditionEditorTokens.fieldOptions(Fields, "gone") { "$it (removed)" }
        assertEquals("gone (removed)", options.first().label)
        assertEquals("later", options.last().value)
        assertFalse(options.last().enabled)
        assertEquals("comes after this question", options.last().description)
        val taken = listOf("condition-1", "condition-2")
        assertEquals("condition-3", MonarchConditionEditorTokens.freshId("condition", taken))
        assertEquals(Status, MonarchConditionEditorTokens.startField(Fields, null))
        assertEquals(Amount, MonarchConditionEditorTokens.startField(Fields, "amount"))
    }

    @Test
    fun aNumberKeepsTheTypedTextAndReadsTheNumber() {
        assertEquals("500", Cv.Number(500.0).text)
        assertEquals(1.5, Cv.Number(" 1.5 ").parsed)
        assertNull(Cv.Number("1x").parsed)
        assertEquals("Nov 14, 2026", monarchConditionEditorFormatDay(Nov14, Locale.US))
    }

    @Test
    fun anInvalidConditionTakesTheDangerBorderInEveryTheme() {
        AllThemes.forEach { tokens ->
            val colors = MonarchColors(tokens.colors)
            val resting = MonarchConditionEditorTokens.rowColors(colors, invalid = false)
            val invalid = MonarchConditionEditorTokens.rowColors(colors, invalid = true)
            assertEquals(colors.borderSubtle, resting.border)
            assertEquals(colors.dangerBorder, invalid.border)
            assertNotEquals(resting.border, invalid.border)
            assertEquals(colors.bgSurface, invalid.container)
        }
    }
}
