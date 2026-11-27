package com.rookhq.monarch.components.workflow

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchCondition
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchConditionOperator
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchConditionValue
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFieldRenderer
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFieldRendererLabels
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFieldRendererTokens as Tokens
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldDefinition as Field
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldFile
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldOption as Option
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldProblem as Problem
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldType as Type
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldValue as Value
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Sizes = listOf(Option("s", "Small"), Option("m", "Medium"), Option("l", "Large"))

internal object MonarchFieldRendererFixtures : ComponentFixtures {
    override val id = "field-renderer"

    private fun one(field: Field, value: Value?, problem: Problem? = null, confidence: Float? = null) =
        fixture("${field.type}${if (problem != null) ", with an error" else ""}") {
            MonarchFieldRenderer(field = field, value = value, onValueChange = {}, problem = problem,
                confidence = confidence)
        }

    override val fixtures = listOf(
        one(Field("a", Type.Section, "About you", description = "We use this to set up your account."), null),
        one(Field("a", Type.ShortText, "Full name", required = true), null, Problem.Required),
        one(Field("a", Type.LongText, "Anything else?", placeholder = "Optional"), Value.Text("Long answer")),
        one(Field("a", Type.Email, "Work email"), Value.Text("asha@example.com"), confidence = 0.9f),
        one(Field("a", Type.Phone, "Phone"), Value.Text("+44 20 7946 0000"), confidence = 0.3f),
        one(Field("a", Type.Url, "Website"), null),
        one(Field("a", Type.Number, "Team size", min = 1.0, max = 500.0), Value.Number(12.0)),
        one(Field("a", Type.Date, "Start date"), Value.Date(1_794_614_400_000L)),
        one(Field("a", Type.Time, "Call time"), Value.Text("14:30")),
        one(Field("a", Type.Select, "Size", options = Sizes), Value.Text("m")),
        one(Field("a", Type.MultiSelect, "Sizes", options = Sizes), Value.Values(listOf("s"))),
        one(Field("a", Type.Radio, "Size", options = Sizes), Value.Text("l")),
        one(Field("a", Type.Checkboxes, "Sizes", options = Sizes, minSelections = 2), Value.Values(listOf("s")),
            Problem.MinSelections(2)),
        one(Field("a", Type.YesNo, "Remote role?"), Value.Flag(false)),
        one(Field("a", Type.Consent, "Terms", required = true, consentText = "I agree to the terms."), null,
            Problem.MustAgree),
        one(Field("a", Type.Rating, "How was it?"), Value.Number(4.0)),
        one(Field("a", Type.File, "CV", maxFiles = 2), Value.Files(listOf(MonarchFormFieldFile("cv.pdf")))),
        fixture("read-only text and disabled choice") {
            Column {
                MonarchFieldRenderer(field = Field("a", Type.Email, "Email", readOnly = true),
                    value = Value.Text("asha@example.com"), onValueChange = {})
                MonarchFieldRenderer(field = Field("b", Type.YesNo, "Remote?"), value = Value.Flag(true),
                    onValueChange = {}, enabled = false)
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchFieldRendererTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(field: Field, start: Value?, onChange: (Value?) -> Unit = {}, problem: Problem? = null) {
        rule.setMonarchContent {
            var value by remember { mutableStateOf(start) }
            MonarchFieldRenderer(
                field = field,
                value = value,
                onValueChange = {
                    value = it
                    onChange(it)
                },
                problem = problem,
            )
        }
    }

    @Test
    fun textIsNamedByTheLabelAndTypingStoresText() {
        var result: Value? = null
        show(Field("name", Type.ShortText, "Full name", maxLength = 5), null, { result = it })
        rule.onNodeWithContentDescription("Full name").performTextInput("Asha Rao")
        assertEquals(Value.Text("Asha "), result)
    }

    @Test
    fun aRequiredQuestionSaysRequiredAndShowsTheProblem() {
        show(Field("name", Type.ShortText, "Full name", required = true), null, problem = Problem.Required)
        rule.onNodeWithContentDescription("Full name", substring = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "This field is required."))
    }

    @Test
    fun errorTextWinsOverTheProblem() {
        rule.setMonarchContent {
            MonarchFieldRenderer(field = Field("e", Type.Email, "Email"), value = Value.Text("x"), onValueChange = {},
                problem = Problem.Email, errorText = "This address is taken.")
        }
        rule.onNodeWithText("This address is taken.", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Enter an email address", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun aSectionIsAHeading() {
        show(Field("s", Type.Section, "About you"), null)
        rule.onNode(hasText("About you") and isHeading()).assertExists()
    }

    @Test
    fun radioOptionsAreSelectableRows() {
        var result: Value? = null
        show(Field("size", Type.Radio, "Size", options = Sizes), null, { result = it })
        rule.onNodeWithText("Medium").assertTouchTargetAtLeast48dp(rule.density).performClick().assertIsSelected()
        assertEquals(Value.Text("m"), result)
    }

    @Test
    fun yesOrNoStoresAFlag() {
        var result: Value? = null
        show(Field("r", Type.YesNo, "Remote role?"), null, { result = it })
        rule.onNodeWithText("No").performClick()
        assertEquals(Value.Flag(false), result)
    }

    @Test
    fun yesComesBeforeNoRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchFieldRenderer(field = Field("r", Type.YesNo, "Remote role?"), value = null, onValueChange = {})
        }
        rule.onNodeWithText("Yes").assertIsBefore(rule.onNodeWithText("No"), LayoutDirection.Rtl)
    }

    @Test
    fun checkboxesKeepTheOrderOfTheOptions() {
        var result: Value? = null
        show(Field("c", Type.Checkboxes, "Sizes", options = Sizes), Value.Values(listOf("l")), { result = it })
        rule.onNodeWithText("Small").performClick()
        assertEquals(Value.Values(listOf("s", "l")), result)
    }

    @Test
    fun aConsentIsACheckboxWithItsStatement() {
        var result: Value? = null
        show(Field("t", Type.Consent, "Terms", consentText = "I agree to the terms."), null, { result = it })
        rule.onNode(hasText("I agree to the terms.") and
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .performClick()
            .assertIsOn()
        assertEquals(Value.Flag(true), result)
    }

    @Test
    fun aConsentErrorIsAnnouncedPolitely() {
        show(Field("t", Type.Consent, "Terms", required = true), null, problem = Problem.MustAgree)
        rule.onNodeWithText("Tick this to continue.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun aDateOpensThePickerFromAButton() {
        show(Field("d", Type.Date, "Start date"), null)
        rule.onNodeWithText("Choose a date").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("OK").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("OK").assertDoesNotExist()
    }

    @Test
    fun aDateAnswerIsShownInTheLocale() {
        show(Field("d", Type.Date, "Start date"), Value.Date(1_794_614_400_000L))
        rule.onNodeWithText("Nov 14, 2026").assertExists()
    }

    @Test
    fun aTimeAnswerIsShownAndTheDialogOpens() {
        show(Field("t", Type.Time, "Call time"), Value.Text("14:30"))
        rule.onNodeWithText("2:30\u202FPM").performClick()
        rule.onNodeWithText("OK").assertExists()
    }

    @Test
    fun aChosenFileCanBeRemoved() {
        var result: Value? = null
        show(Field("f", Type.File, "CV", maxFiles = 1), Value.Files(listOf(MonarchFormFieldFile("cv.pdf"))),
            { result = it })
        rule.onNodeWithText("cv.pdf").assertExists()
        rule.onNodeWithContentDescription("Remove cv.pdf").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(Value.Files(emptyList()), result)
    }

    @Test
    fun aReadOnlyChoiceIsDisabled() {
        show(Field("s", Type.Radio, "Size", options = Sizes, readOnly = true), Value.Text("s"))
        rule.onNodeWithText("Small").assertIsNotEnabled()
    }

    @Test
    fun theAiNoteAsksForACheckBelowTheThreshold() {
        rule.setMonarchContent {
            MonarchFieldRenderer(field = Field("c", Type.ShortText, "Company"), value = Value.Text("Rook"),
                onValueChange = {}, confidence = 0.4f)
        }
        rule.onNodeWithText("Filled in automatically, but unsure. Please check.").assertExists()
    }

    @Test
    fun theLabelIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchFieldRenderer(field = Field("s", Type.Section, "Your experience"), value = null, onValueChange = {})
        }
        rule.onNodeWithText("Your experience").assertTextFullyVisible()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchFieldRenderer(
                field = Field("r", Type.YesNo, "Remote?", required = true),
                value = null,
                onValueChange = {},
                problem = Problem.Required,
                labels = MonarchFieldRendererLabels(yes = "Oui", required = "Answer this."),
            )
        }
        rule.onNodeWithText("Oui").assertExists()
        rule.onNodeWithText("Answer this.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun problemFollowsTheWebRules() {
        assertEquals(Problem.Required, Field("a", Type.ShortText, "A", required = true).problem(Value.Text("  ")))
        assertNull(Field("a", Type.ShortText, "A").problem(null))
        assertEquals(Problem.Email, Field("a", Type.Email, "A").problem(Value.Text("asha@")))
        assertNull(Field("a", Type.Email, "A").problem(Value.Text(" asha@example.com ")))
        assertEquals(Problem.Url, Field("a", Type.Url, "A").problem(Value.Text("ftp://example.com")))
        assertNull(Field("a", Type.Url, "A").problem(Value.Text("https://example.com/a")))
        assertEquals(Problem.MinLength(3), Field("a", Type.ShortText, "A", minLength = 3).problem(Value.Text("ab")))
        assertEquals(Problem.Max(10.0), Field("a", Type.Number, "A", max = 10.0).problem(Value.Number(11.0)))
        assertEquals(Problem.NotANumber, Field("a", Type.Number, "A").problem(Value.Number(Double.NaN)))
        assertEquals(Problem.Required, Field("a", Type.YesNo, "A", required = true).problem(Value.Text("yes")))
        assertEquals(Problem.MustAgree, Field("a", Type.Consent, "A", required = true).problem(Value.Flag(false)))
        assertEquals(
            Problem.MaxSelections(1),
            Field("a", Type.Checkboxes, "A", maxSelections = 1).problem(Value.Values(listOf("s", "m"))),
        )
        assertNull(Field("a", Type.Section, "A", required = true).problem(null))
    }

    @Test
    fun conditionsDecideVisibility() {
        val field = Field(
            "why",
            Type.LongText,
            "Why?",
            conditions = listOf(MonarchCondition("c", "remote", MonarchConditionOperator.IsTrue)),
        )
        assertTrue(field.isVisible(mapOf("remote" to Value.Flag(true))))
        assertFalse(field.isVisible(mapOf("remote" to Value.Flag(false))))
        val sized = Field(
            "x",
            Type.ShortText,
            "X",
            conditions = listOf(
                MonarchCondition("c", "size", MonarchConditionOperator.Eq, MonarchConditionValue.Text("m")),
            ),
        )
        assertTrue(sized.isVisible(mapOf("size" to Value.Text("m"))))
        assertTrue(Field("y", Type.ShortText, "Y").isVisible(emptyMap()))
    }

    @Test
    fun tokensParseTimesAndPickKeyboards() {
        assertEquals(870, Tokens.minutesOf("14:30"))
        assertNull(Tokens.minutesOf("25:00"))
        assertEquals("09:05", Tokens.timeAnswer(9, 5))
        assertTrue(Tokens.keepsReadOnlyInput(Type.Number))
        assertFalse(Tokens.keepsReadOnlyInput(Type.Radio))
    }

    @Test
    fun notesTakeTheirRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.aiStrong, Tokens.aiNoteColor(colors, unsure = false))
            assertEquals(colors.warningStrong, Tokens.aiNoteColor(colors, unsure = true))
            assertEquals(colors.dangerStrong, Tokens.errorColor(colors))
        }
    }
}
