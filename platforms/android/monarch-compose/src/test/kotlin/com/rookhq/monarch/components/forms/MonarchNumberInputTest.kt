package com.rookhq.monarch.components.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.forms.numberinput.MonarchNumberInput
import com.rookhq.monarch.components.forms.numberinput.MonarchNumberInputLabels
import com.rookhq.monarch.components.forms.numberinput.MonarchNumberInputTokens
import com.rookhq.monarch.components.forms.numberinput.NumberInputMath
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal object MonarchNumberInputFixtures : ComponentFixtures {
    override val id = "number-input"
    override val fixtures = listOf(
        fixture("bounded with helper") {
            MonarchNumberInput(
                value = 1.0,
                onValueChange = {},
                label = "Quantity",
                helperText = "Up to 99 per order.",
                minimum = 0.0,
                maximum = 99.0,
            )
        },
        fixture("at the maximum") {
            MonarchNumberInput(value = 99.0, onValueChange = {}, label = "Quantity", minimum = 0.0, maximum = 99.0)
        },
        fixture("empty with placeholder") {
            MonarchNumberInput(value = null, onValueChange = {}, label = "Price", placeholder = "0", step = 0.1)
        },
        fixture("error from the app") {
            MonarchNumberInput(
                value = 0.0,
                onValueChange = {},
                label = "Seats",
                errorText = "Choose at least one seat.",
            )
        },
        fixture("without steppers") {
            MonarchNumberInput(value = 1234.5, onValueChange = {}, label = "Amount", showSteppers = false)
        },
        fixture("disabled") { MonarchNumberInput(value = 4.0, onValueChange = {}, label = "Guests", enabled = false) },
        fixture("read only") { MonarchNumberInput(value = 4.0, onValueChange = {}, label = "Guests", readOnly = true) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchNumberInputTest {
    @get:Rule
    val rule = createComposeRule()

    private val field get() = rule.onNode(hasSetTextAction())
    private val increase get() = rule.onNodeWithContentDescription("Increase")
    private val decrease get() = rule.onNodeWithContentDescription("Decrease")

    @Test
    fun stepsWithoutDrift() {
        assertEquals(0.3, NumberInputMath.stepValue(0.2, 0.1, 1))
        assertEquals(0.3, NumberInputMath.roundTo(0.1 + 0.2, 3))
        assertEquals(1.0, NumberInputMath.stepValue(0.2, 1.0, 1))
        assertEquals(0.0, NumberInputMath.stepValue(0.2, 1.0, -1))
        assertEquals(5.5, NumberInputMath.stepValue(5.0, 0.5, 1, base = 0.5))
        assertEquals(7, NumberInputMath.decimalPlaces(1e-7))
        assertEquals(0, NumberInputMath.decimalPlaces(100.0))
        assertEquals(0.0, NumberInputMath.roundTo(-0.0001, 3))
    }

    @Test
    fun readsNumbersInTheConventionsOfTheLocale() {
        assertEquals(1234.5, NumberInputMath.parse("1,234.5", Locale.US))
        assertEquals(1234.5, NumberInputMath.parse("1.234,5", Locale.GERMANY))
        assertEquals(1234.5, NumberInputMath.parse("1 234,5", Locale.FRANCE))
        assertEquals(1234.5, NumberInputMath.parse("1 234,5", Locale.FRANCE))
        assertEquals(123.0, NumberInputMath.parse("١٢٣", Locale.forLanguageTag("ar-EG")))
        assertEquals(-12.0, NumberInputMath.parse("(12)", Locale.US))
        assertEquals(-12.0, NumberInputMath.parse("− 12", Locale.US))
        assertEquals(12.0, NumberInputMath.parse("€ 12", Locale.US))
        assertNull(NumberInputMath.parse("12abc3", Locale.US))
        assertNull(NumberInputMath.parse("abc", Locale.US))
        assertNull(NumberInputMath.parse("1.2.3", Locale.US))
    }

    @Test
    fun aTypedNumberIsSentAtOnceAndShownInTheLocaleFormatOnDone() {
        var value by mutableStateOf<Double?>(null)
        rule.setMonarchContent {
            MonarchNumberInput(value = value, onValueChange = { value = it }, label = "Amount")
        }
        field.performTextInput("1234.5")
        assertEquals(1234.5, value)
        assertEquals("1234.5", field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text)
        field.performImeAction()
        assertEquals(1234.5, value)
        assertEquals("1,234.5", field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text)
        field.performTextClearance()
        field.performImeAction()
        assertNull(value)
    }

    @Test
    fun aButtonTappedRightAfterTypingReadsTheTypedNumber() {
        // Regression (C-021): the number was only sent on Done or when the
        // field lost focus, so a Save button read the old value.
        var value by mutableStateOf<Double?>(null)
        var saved: Double? = null
        rule.setMonarchContent {
            Column {
                MonarchNumberInput(value = value, onValueChange = { value = it }, label = "Quantity")
                MonarchButton(text = "Save", onClick = { saved = value })
            }
        }
        field.performTextInput("1")
        field.performTextInput("2")
        rule.onNodeWithText("Save").performClick()
        assertEquals(12.0, saved)
    }

    @Test
    fun typedTextStaysAsTypedWhileTheValueIsSent() {
        var value by mutableStateOf<Double?>(null)
        rule.setMonarchContent {
            MonarchNumberInput(value = value, onValueChange = { value = it }, label = "Price", step = 0.01)
        }
        field.performTextInput("1234.")
        assertEquals(1234.0, value)
        field.performTextInput("5")
        assertEquals(1234.5, value)
        assertEquals("1234.5", field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text)
        field.performImeAction()
        assertEquals("1,234.5", field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text)
    }

    @Test
    fun clearingTheFieldSendsNullAtOnce() {
        var value by mutableStateOf<Double?>(7.0)
        rule.setMonarchContent {
            MonarchNumberInput(value = value, onValueChange = { value = it }, label = "Quantity")
        }
        field.performTextClearance()
        assertNull(value)
    }

    @Test
    fun aNumberOutsideTheBoundsIsSentWhileTypingAndExplainedOnDone() {
        val sent = mutableListOf<Double?>()
        var value by mutableStateOf<Double?>(null)
        rule.setMonarchContent {
            MonarchNumberInput(
                value = value,
                onValueChange = { sent += it; value = it },
                label = "Qty",
                maximum = 99.0,
            )
        }
        field.performTextInput("9")
        field.performTextInput("9")
        field.performTextInput("9")
        // C-021: every number that reads is sent, in range or not, so a Save tapped now reads 999.
        assertEquals(listOf<Double?>(9.0, 99.0, 999.0), sent)
        assertNull(field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error), "Not flagged while typing")
        field.performImeAction()
        assertEquals(
            "Enter 99 or less.",
            field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error),
        )
    }

    @Test
    fun aValueChangedByTheAppReplacesTheTypedText() {
        var value by mutableStateOf<Double?>(null)
        rule.setMonarchContent {
            MonarchNumberInput(value = value, onValueChange = { value = it }, label = "Quantity")
        }
        field.performTextInput("4")
        value = 10.0
        rule.waitForIdle()
        assertEquals("10", field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text)
    }

    @Test
    fun unreadableTextIsFlaggedAndNotSaved() {
        var changes = 0
        rule.setMonarchContent {
            MonarchNumberInput(value = 3.0, onValueChange = { changes++ }, label = "Amount")
        }
        field.performTextReplacement("abc")
        assertNull(field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error), "Not flagged while typing")
        field.performImeAction()
        assertEquals(0, changes)
        assertEquals("Enter a number.", field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error))
    }

    @Test
    fun aNumberOutsideTheBoundsIsSavedAsTypedAndFlaggedWhenLeft() {
        val sent = mutableListOf<Double?>()
        var value by mutableStateOf<Double?>(3.0)
        rule.setMonarchContent {
            MonarchNumberInput(
                value = value,
                onValueChange = { sent += it; value = it },
                label = "Qty",
                minimum = 0.0,
                maximum = 99.0,
            )
        }
        field.performTextReplacement("120")
        assertEquals(listOf<Double?>(120.0), sent)
        field.performImeAction()
        assertEquals(listOf<Double?>(120.0), sent, "Leaving the field does not clamp or send again")
        assertEquals("120", field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text)
        assertEquals(
            "Enter a number from 0 to 99.",
            field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error),
        )
    }

    @Test
    fun theButtonsStepAndStopAtTheBounds() {
        var value by mutableStateOf<Double?>(97.0)
        rule.setMonarchContent {
            MonarchNumberInput(value = value, onValueChange = { value = it }, label = "Qty", maximum = 99.0)
        }
        increase.performClick()
        increase.performClick()
        assertEquals(99.0, value)
        increase.assertIsNotEnabled()
        decrease.assertIsEnabled().performClick()
        assertEquals(98.0, value)
    }

    @Test
    fun decimalStepsDoNotDrift() {
        var value by mutableStateOf<Double?>(0.1)
        rule.setMonarchContent {
            MonarchNumberInput(value = value, onValueChange = { value = it }, label = "Price", step = 0.1)
        }
        increase.performClick()
        increase.performClick()
        assertEquals(0.3, value)
    }

    @Test
    fun anEmptyFieldStartsFromTheMinimum() {
        var value by mutableStateOf<Double?>(null)
        rule.setMonarchContent {
            MonarchNumberInput(value = value, onValueChange = { value = it }, label = "Seats", minimum = 2.0)
        }
        increase.performClick()
        assertEquals(2.0, value)
    }

    @Test
    fun theFieldHasIncreaseAndDecreaseActions() {
        var value by mutableStateOf<Double?>(5.0)
        rule.setMonarchContent {
            MonarchNumberInput(value = value, onValueChange = { value = it }, label = "Qty")
        }
        val actions = field.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions).orEmpty()
        assertEquals(listOf("Decrease", "Increase"), actions.map { it.label })
        actions.last().action()
        assertEquals(6.0, value)
    }

    @Test
    fun disabledAndReadOnlyFieldsDoNotStep() {
        var changes = 0
        rule.setMonarchContent {
            MonarchNumberInput(value = 1.0, onValueChange = { changes++ }, label = "Guests", readOnly = true)
        }
        increase.assertIsNotEnabled().performClick()
        decrease.assertIsNotEnabled()
        assertEquals(0, changes)
    }

    @Test
    fun decreaseIsAtTheStartAndBothHave48dpTargets() {
        val environment = MonarchTestEnvironment(fontScale = LargestFontScale)
        rule.setMonarchContent(environment) {
            MonarchNumberInput(value = 1.0, onValueChange = {}, label = "Qty")
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            decrease.assertIsBefore(increase, direction)
            decrease.assertTouchTargetAtLeast48dp(rule.density)
            increase.assertTouchTargetAtLeast48dp(rule.density)
        }
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchNumberInput(
                value = 3.0,
                onValueChange = {},
                label = "Qty",
                minimum = 1.0,
                labels = MonarchNumberInputLabels(
                    increment = "More",
                    decrement = "Fewer",
                    range = { min, max -> "From $min to $max" },
                ),
            )
        }
        rule.onNodeWithContentDescription("More").assertExists()
        rule.onNodeWithContentDescription("Fewer").assertExists()
        field.performTextClearance()
        field.performTextInput("0")
        field.performImeAction()
        assertEquals("From 1 to null", field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error))
    }

    @Test
    fun theSignColourComesFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchNumberInputTokens.stepper(true, colors), theme.id)
            assertEquals(colors.textDisabled, MonarchNumberInputTokens.stepper(false, colors), theme.id)
        }
    }
}
