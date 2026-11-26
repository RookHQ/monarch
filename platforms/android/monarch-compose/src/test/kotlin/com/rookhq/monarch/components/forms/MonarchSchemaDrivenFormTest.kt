package com.rookhq.monarch.components.forms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.schemadrivenform.MonarchSchemaDrivenForm
import com.rookhq.monarch.components.forms.schemadrivenform.MonarchSchemaDrivenFormDefaults
import com.rookhq.monarch.components.forms.schemadrivenform.MonarchSchemaDrivenFormLabels
import com.rookhq.monarch.components.forms.schemadrivenform.MonarchSchemaDrivenFormTokens
import com.rookhq.monarch.components.forms.schemadrivenform.MonarchSchemaDrivenFormValidateOn
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchCondition
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchConditionOperator
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldDefinition
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldFile
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldOption
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldProblem
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldType
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldValue
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val Name = MonarchFormFieldDefinition("name", MonarchFormFieldType.ShortText, "Full name", required = true)
private val Email = MonarchFormFieldDefinition("email", MonarchFormFieldType.Email, "Work email")
private val Remote = MonarchFormFieldDefinition("remote", MonarchFormFieldType.YesNo, "Remote?")
private val City = MonarchFormFieldDefinition(
    "city",
    MonarchFormFieldType.ShortText,
    "City",
    required = true,
    conditions = listOf(MonarchCondition("c1", "remote", MonarchConditionOperator.IsFalse)),
)
private val Billing = MonarchFormFieldDefinition(
    "billing",
    MonarchFormFieldType.Section,
    "Billing",
    conditions = listOf(MonarchCondition("c2", "remote", MonarchConditionOperator.IsTrue)),
)
private val Vat = MonarchFormFieldDefinition("vat", MonarchFormFieldType.ShortText, "VAT number", required = true)
private val Contact = MonarchFormFieldDefinition("contact", MonarchFormFieldType.Section, "Contact")

private val Schema = listOf(Name, Email, Remote, City, Billing, Vat)

@Composable
private fun Form(
    fields: List<MonarchFormFieldDefinition> = listOf(Name, Email),
    start: Map<String, MonarchFormFieldValue?> = emptyMap(),
    onSubmit: (suspend (Map<String, MonarchFormFieldValue?>) -> Unit)? = {},
    validateOn: MonarchSchemaDrivenFormValidateOn = MonarchSchemaDrivenFormValidateOn.Blur,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    dirty: Boolean? = null,
    errors: Map<String, String> = emptyMap(),
    labels: MonarchSchemaDrivenFormLabels = MonarchSchemaDrivenFormLabels(),
    secondary: Boolean = false,
) {
    val answers = remember { mutableStateMapOf<String, MonarchFormFieldValue?>().apply { putAll(start) } }
    MonarchSchemaDrivenForm(
        fields = fields,
        values = answers,
        onValuesChange = { all, _ ->
            answers.clear()
            answers.putAll(all)
        },
        onSubmit = onSubmit,
        validateOn = validateOn,
        enabled = enabled,
        readOnly = readOnly,
        dirty = dirty,
        errors = errors,
        locale = Locale.US,
        labels = labels,
        secondaryActions = if (secondary) {
            { MonarchButton(text = "Cancel", onClick = {}, variant = MonarchButtonVariant.Ghost) }
        } else {
            null
        },
    )
}

internal object MonarchSchemaDrivenFormFixtures : ComponentFixtures {
    override val id = "schema-driven-form"
    override val fixtures = listOf(
        fixture("text questions and Save") { Form() },
        fixture("a section and a choice") {
            Form(fields = listOf(Contact, Remote), secondary = true)
        },
        fixture("a conditional question shown") {
            Form(fields = listOf(Remote, City), start = mapOf("remote" to MonarchFormFieldValue.Flag(false)))
        },
        fixture("server errors") { Form(errors = mapOf("email" to "This email is already in use.")) },
        fixture("disabled") { Form(enabled = false) },
        fixture("without a submit button") { Form(onSubmit = null) },
        fixture("edit form with the actions bar") {
            Form(fields = listOf(Name), start = mapOf("name" to MonarchFormFieldValue.Text("Asha")), dirty = true)
        },
        fixture("read only") {
            Form(
                fields = Schema,
                start = mapOf(
                    "name" to MonarchFormFieldValue.Text("Asha Rao"),
                    "remote" to MonarchFormFieldValue.Flag(true),
                    "vat" to MonarchFormFieldValue.Text("GB123"),
                ),
                readOnly = true,
            )
        },
        fixture("long label") {
            Form(
                fields = listOf(
                    MonarchFormFieldDefinition(
                        "reason",
                        MonarchFormFieldType.LongText,
                        "Tell us in a few sentences why you would like to join the early access programme",
                        description = "We read every answer and reply within two working days.",
                    ),
                ),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchSchemaDrivenFormTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun everyQuestionIsRenderedWithItsInput() {
        rule.setMonarchContent { Form(fields = listOf(Contact, Name, Remote)) }
        rule.onNode(isHeading()).assertExists()
        rule.onNodeWithText("Contact", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Full name", substring = true).assertExists()
        rule.onNodeWithText("Yes").assertExists()
        rule.onNodeWithText("Save").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun answersAreReportedWithTheQuestionId() {
        val changes = mutableListOf<Pair<String, MonarchFormFieldValue?>>()
        rule.setMonarchContent {
            MonarchSchemaDrivenForm(
                fields = listOf(Name),
                values = emptyMap(),
                onValuesChange = { all, id -> changes += id to all[id] },
            )
        }
        rule.onNodeWithContentDescription("Full name", substring = true).performTextInput("Asha")
        val expected: Pair<String, MonarchFormFieldValue?> = "name" to MonarchFormFieldValue.Text("Asha")
        assertEquals(listOf(expected), changes)
    }

    @Test
    fun anInvalidSubmitShowsTheErrorsAndFocusesTheFirstInvalidQuestion() {
        val sent = mutableListOf<Map<String, MonarchFormFieldValue?>>()
        rule.setMonarchContent { Form(onSubmit = { sent += it }) }
        rule.onAllNodesWithText("This field is required.", useUnmergedTree = true).assertCountEquals(0)
        rule.onNodeWithText("Save").performClick()
        rule.waitForIdle()
        assertEquals(emptyList(), sent)
        rule.onNodeWithText("This field is required.", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Full name", substring = true).assertIsFocused()
    }

    @Test
    fun aValidSubmitSendsOnlyTheShownAnswers() {
        val sent = mutableListOf<Map<String, MonarchFormFieldValue?>>()
        rule.setMonarchContent {
            Form(
                fields = listOf(Remote, City, Billing, Vat),
                start = mapOf(
                    "remote" to MonarchFormFieldValue.Flag(false),
                    "city" to MonarchFormFieldValue.Text("Leeds"),
                    "vat" to MonarchFormFieldValue.Text("GB1"),
                ),
                onSubmit = { sent += it },
            )
        }
        rule.onAllNodesWithText("VAT number", useUnmergedTree = true).assertCountEquals(0)
        rule.onNodeWithText("Save").performClick()
        rule.waitForIdle()
        assertEquals(
            listOf(
                mapOf<String, MonarchFormFieldValue?>(
                    "remote" to MonarchFormFieldValue.Flag(false),
                    "city" to MonarchFormFieldValue.Text("Leeds"),
                ),
            ),
            sent,
        )
    }

    @Test
    fun aFailedSubmitShowsTheErrorsMessage() {
        rule.setMonarchContent {
            Form(start = mapOf("name" to MonarchFormFieldValue.Text("Asha")), onSubmit = { error("Server is busy.") })
        }
        rule.onNodeWithText("Save").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Server is busy.", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun aFailureWithoutAMessageUsesTheBuiltInOne() {
        rule.setMonarchContent {
            Form(
                start = mapOf("name" to MonarchFormFieldValue.Text("Asha")),
                onSubmit = { throw IllegalStateException() },
            )
        }
        rule.onNodeWithText("Save").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("The form could not be saved. Try again.", substring = true, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun withBlurAnErrorShowsOnceTheQuestionIsLeft() {
        rule.setMonarchContent { Form() }
        rule.onNodeWithContentDescription("Full name", substring = true).requestFocus()
        rule.waitForIdle()
        rule.onAllNodesWithText("This field is required.", useUnmergedTree = true).assertCountEquals(0)
        rule.onNodeWithContentDescription("Work email").requestFocus()
        rule.waitForIdle()
        rule.onNodeWithText("This field is required.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun withSubmitNothingShowsBeforeASubmitAttempt() {
        rule.setMonarchContent { Form(validateOn = MonarchSchemaDrivenFormValidateOn.Submit) }
        rule.onNodeWithContentDescription("Full name", substring = true).requestFocus()
        rule.onNodeWithContentDescription("Work email").requestFocus()
        rule.waitForIdle()
        rule.onAllNodesWithText("This field is required.", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun conditionalQuestionsAndSectionsFollowTheAnswers() {
        rule.setMonarchContent { Form(fields = listOf(Remote, City, Billing, Vat)) }
        rule.onAllNodesWithText("City", useUnmergedTree = true).assertCountEquals(0)
        rule.onAllNodesWithText("Billing", useUnmergedTree = true).assertCountEquals(0)
        rule.onNodeWithText("No").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("City", substring = true).assertExists()
        rule.onNodeWithText("Yes").performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("City", useUnmergedTree = true).assertCountEquals(0)
        rule.onNodeWithText("Billing", useUnmergedTree = true).assertExists()
    }

    @Test
    fun appErrorsShowAtOnce() {
        rule.setMonarchContent { Form(errors = mapOf("email" to "This email is already in use.")) }
        rule.onNodeWithText("This email is already in use.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aDisabledFormCannotBeSubmitted() {
        var sent = 0
        rule.setMonarchContent { Form(enabled = false, onSubmit = { sent++ }) }
        rule.onNodeWithText("Save").assertIsNotEnabled().performClick()
        assertEquals(0, sent)
    }

    @Test
    fun theActionsBarSavesThroughOnSubmit() {
        val sent = mutableListOf<Map<String, MonarchFormFieldValue?>>()
        rule.setMonarchContent {
            Form(
                fields = listOf(Name),
                start = mapOf("name" to MonarchFormFieldValue.Text("Asha")),
                onSubmit = { sent += it },
                dirty = true,
            )
        }
        rule.onNodeWithText("Save changes").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(listOf(mapOf<String, MonarchFormFieldValue?>("name" to MonarchFormFieldValue.Text("Asha"))), sent)
    }

    @Test
    fun theReadOnlyViewShowsTheAnswersAsText() {
        rule.setMonarchContent {
            Form(
                fields = Schema,
                start = mapOf(
                    "name" to MonarchFormFieldValue.Text("Asha Rao"),
                    "remote" to MonarchFormFieldValue.Flag(true),
                ),
                readOnly = true,
            )
        }
        rule.onNodeWithText("Asha Rao", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Yes", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithText("City", useUnmergedTree = true).assertCountEquals(0)
        rule.onAllNodesWithText("Save").assertCountEquals(0)
    }

    @Test
    fun submitComesBeforeTheSecondaryActionsInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { Form(secondary = true) }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Save").assertIsBefore(rule.onNodeWithText("Cancel"), direction)
        }
    }

    @Test
    fun labelsWrapAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) { Form(fields = listOf(Name)) }
        rule.onNodeWithText("Save").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Save", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            Form(
                start = mapOf("name" to MonarchFormFieldValue.Text("Asha")),
                onSubmit = { throw IllegalStateException() },
                labels = MonarchSchemaDrivenFormLabels(submit = "Guardar", submitFailed = "No se pudo guardar."),
            )
        }
        rule.onNodeWithText("Guardar").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("No se pudo guardar.", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun theRulesHideSectionsAndCollectProblems() {
        val d = MonarchSchemaDrivenFormDefaults
        val remote = mapOf<String, MonarchFormFieldValue?>("remote" to MonarchFormFieldValue.Flag(true))
        assertEquals(listOf(Name, Email, Remote, Billing, Vat), d.visibleFields(Schema, remote))
        val office = mapOf<String, MonarchFormFieldValue?>("remote" to MonarchFormFieldValue.Flag(false))
        assertEquals(listOf(Name, Email, Remote, City), d.visibleFields(Schema, office))
        assertEquals(
            mapOf("name" to MonarchFormFieldProblem.Required, "city" to MonarchFormFieldProblem.Required),
            d.problems(Schema, office),
        )
        assertEquals(
            mapOf<String, MonarchFormFieldValue?>(
                "name" to null,
                "email" to null,
                "remote" to MonarchFormFieldValue.Flag(true),
                "vat" to null,
            ),
            d.submittedValues(Schema, remote),
        )
    }

    @Test
    fun theDisplayOfEachKindOfAnswer() {
        val t = MonarchSchemaDrivenFormTokens
        val size = MonarchFormFieldDefinition(
            "size",
            MonarchFormFieldType.Select,
            "Size",
            options = listOf(MonarchFormFieldOption("s", "Small"), MonarchFormFieldOption("m", "Medium")),
        )
        val us = Locale.US
        assertEquals(null, t.display(Name, null, us, "Yes", "No"))
        assertEquals(null, t.display(Name, MonarchFormFieldValue.Text(" "), us, "Yes", "No"))
        assertEquals("No", t.display(Remote, MonarchFormFieldValue.Flag(false), us, "Yes", "No"))
        assertEquals("Medium", t.display(size, MonarchFormFieldValue.Text("m"), us, "Yes", "No"))
        assertEquals("Small, x", t.display(size, MonarchFormFieldValue.Values(listOf("s", "x")), us, "Yes", "No"))
        assertEquals("1,234.5", t.display(Name, MonarchFormFieldValue.Number(1234.5), us, "Yes", "No"))
        // 14 November 2026, UTC midnight.
        assertEquals("Nov 14, 2026", t.display(Name, MonarchFormFieldValue.Date(1_794_614_400_000L), us, "Yes", "No"))
        assertEquals(
            "cv.pdf, photo.png",
            t.display(
                Name,
                MonarchFormFieldValue.Files(listOf(MonarchFormFieldFile("cv.pdf"), MonarchFormFieldFile("photo.png"))),
                us,
                "Yes",
                "No",
            ),
        )
    }

    @Test
    fun whenErrorsShow() {
        val t = MonarchSchemaDrivenFormTokens
        val blur = MonarchSchemaDrivenFormValidateOn.Blur
        val submit = MonarchSchemaDrivenFormValidateOn.Submit
        assertTrue(t.showsProblem(blur, submitted = false, visited = true))
        assertTrue(!t.showsProblem(blur, submitted = false, visited = false))
        assertTrue(!t.showsProblem(submit, submitted = false, visited = true))
        assertTrue(t.showsProblem(submit, submitted = true, visited = false))
        assertTrue(t.visitedOnFocusLoss(MonarchFormFieldType.Email))
        assertTrue(!t.visitedOnFocusLoss(MonarchFormFieldType.YesNo))
        assertEquals("fallback", t.failureText(IllegalStateException(" "), "fallback"))
        assertEquals("busy", t.failureText(IllegalStateException("busy"), "fallback"))
    }
}
