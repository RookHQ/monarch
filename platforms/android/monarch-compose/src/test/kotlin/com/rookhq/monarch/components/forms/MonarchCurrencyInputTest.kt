package com.rookhq.monarch.components.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
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
import com.rookhq.monarch.components.billing.money.MonarchMoneyCurrencyDisplay
import com.rookhq.monarch.components.forms.currencyinput.CurrencyInputMath
import com.rookhq.monarch.components.forms.currencyinput.MonarchCurrencyInput
import com.rookhq.monarch.components.forms.numberinput.MonarchNumberInputLabels
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal object MonarchCurrencyInputFixtures : ComponentFixtures {
    override val id = "currency-input"
    override val fixtures = listOf(
        fixture("dollars with helper") {
            MonarchCurrencyInput(
                value = 19.99,
                currencyCode = "USD",
                onValueChange = {},
                label = "Amount",
                helperText = "Charged today.",
            )
        },
        fixture("yen in minor units, code, steppers") {
            MonarchCurrencyInput(
                minorUnits = 1500L,
                currencyCode = "JPY",
                onMinorUnitsChange = {},
                label = "Price",
                currencyDisplay = MonarchMoneyCurrencyDisplay.Code,
                showSteppers = true,
            )
        },
        fixture("negative allowed") {
            MonarchCurrencyInput(
                value = -5.0,
                currencyCode = "EUR",
                onValueChange = {},
                label = "Refund",
                allowNegative = true,
            )
        },
        fixture("empty with placeholder") {
            MonarchCurrencyInput(
                value = null,
                currencyCode = "KWD",
                onValueChange = {},
                label = "Fee",
                placeholder = "0.000",
            )
        },
        fixture("long currency name and error") {
            MonarchCurrencyInput(
                value = 1234567.5,
                currencyCode = "USD",
                onValueChange = {},
                label = "Budget",
                currencyDisplay = MonarchMoneyCurrencyDisplay.Name,
                errorText = "The budget is above what this workspace can approve.",
            )
        },
        fixture("disabled") {
            MonarchCurrencyInput(value = 10.0, currencyCode = "GBP", onValueChange = {}, label = "Tip", enabled = false)
        },
        fixture("read only") {
            MonarchCurrencyInput(value = 10.0, currencyCode = "GBP", onValueChange = {}, label = "Tip", readOnly = true)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchCurrencyInputTest {
    @get:Rule
    val rule = createComposeRule()

    private val field get() = rule.onNode(hasSetTextAction())

    private fun SemanticsNodeInteraction.text(): String? =
        fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text

    private fun SemanticsNodeInteraction.error(): String? =
        fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error)

    @Test
    fun convertsExactlyBetweenMajorAndMinorUnits() {
        assertEquals(1.01, CurrencyInputMath.rounded(1.005, 2))
        assertEquals(20.0, CurrencyInputMath.rounded(19.999, 2))
        assertEquals(1500.0, CurrencyInputMath.rounded(1499.6, 0))
        assertEquals(0.0, CurrencyInputMath.rounded(-0.001, 2))
        assertEquals(1999L, CurrencyInputMath.minorUnits(19.99, 2))
        assertEquals(1235L, CurrencyInputMath.minorUnits(1.2345, 3))
        assertNull(CurrencyInputMath.minorUnits(Double.NaN, 2))
        assertEquals(19.99, CurrencyInputMath.majorUnits(1999L, 2))
        assertEquals(-0.5, CurrencyInputMath.majorUnits(-500L, 3))
        assertEquals(0.0, CurrencyInputMath.lowest(null, allowNegative = false))
        assertNull(CurrencyInputMath.lowest(null, allowNegative = true))
        assertEquals(-10.0, CurrencyInputMath.lowest(-10.0, allowNegative = false))
    }

    @Test
    fun aTypedAmountIsRoundedToTheCurrencyAndShownWithItsSymbol() {
        var amount by mutableStateOf<Double?>(null)
        rule.setMonarchContent {
            MonarchCurrencyInput(
                value = amount,
                currencyCode = "USD",
                onValueChange = { amount = it },
                label = "Amount",
            )
        }
        field.performTextInput("1234.567")
        assertEquals(1234.57, amount)
        assertEquals("1234.567", field.text())
        field.performImeAction()
        assertEquals(1234.57, amount)
        assertEquals("$1,234.57", field.text())
        field.performTextClearance()
        field.performTextInput("$12.50")
        field.performImeAction()
        assertEquals(12.5, amount)
        field.performTextClearance()
        field.performImeAction()
        assertNull(amount)
    }

    @Test
    fun aButtonTappedRightAfterTypingReadsTheTypedAmount() {
        // Regression (C-021): the amount was only sent on Done or when the
        // field lost focus, so a Save button read the old amount.
        var cents by mutableStateOf<Long?>(null)
        var saved: Long? = null
        rule.setMonarchContent {
            Column {
                MonarchCurrencyInput(
                    minorUnits = cents,
                    currencyCode = "USD",
                    onMinorUnitsChange = { cents = it },
                    label = "Price",
                )
                MonarchButton(text = "Save", onClick = { saved = cents })
            }
        }
        "12.5".forEach { field.performTextInput(it.toString()) }
        assertEquals("12.5", field.text())
        rule.onNodeWithText("Save").performClick()
        assertEquals(1250L, saved)
    }

    @Test
    fun minorUnitsAreStoredAsWholeCents() {
        var cents by mutableStateOf<Long?>(1999L)
        rule.setMonarchContent {
            MonarchCurrencyInput(
                minorUnits = cents,
                currencyCode = "EUR",
                onMinorUnitsChange = { cents = it },
                label = "Price",
            )
        }
        assertEquals("€19.99", field.text())
        field.performTextClearance()
        field.performTextInput("0.1")
        field.performImeAction()
        assertEquals(10L, cents)
    }

    @Test
    fun negativeAmountsAreFlaggedUnlessAllowed() {
        // C-021: an amount out of range is still sent as typed, so a Save reads what is on screen; the range
        // message appears when the field is left, and the app validates before saving.
        var amount by mutableStateOf<Double?>(5.0)
        rule.setMonarchContent {
            MonarchCurrencyInput(
                value = amount,
                currencyCode = "USD",
                onValueChange = { amount = it },
                label = "Amount",
            )
        }
        field.performTextReplacement("-3")
        assertEquals(-3.0, amount)
        assertNull(field.error(), "Not flagged while typing")
        field.performImeAction()
        assertEquals(-3.0, amount)
        assertEquals("Enter \$0.00 or more.", field.error())
    }

    @Test
    fun negativeAmountsAreKeptWhenAllowed() {
        var amount by mutableStateOf<Double?>(5.0)
        rule.setMonarchContent {
            MonarchCurrencyInput(
                value = amount,
                currencyCode = "USD",
                onValueChange = { amount = it },
                label = "Refund",
                allowNegative = true,
            )
        }
        field.performTextClearance()
        field.performTextInput("-3")
        field.performImeAction()
        assertEquals(-3.0, amount)
    }

    @Test
    fun theStepperIsOffByDefaultAndStepsInMinorUnitsWhenShown() {
        var yen by mutableStateOf<Long?>(1500L)
        rule.setMonarchContent {
            MonarchCurrencyInput(
                minorUnits = yen,
                currencyCode = "JPY",
                onMinorUnitsChange = { yen = it },
                label = "Fee",
                step = 100L,
                maximum = 1600L,
                showSteppers = true,
                labels = MonarchNumberInputLabels(increment = "Add 100"),
            )
        }
        rule.onNodeWithContentDescription("Add 100").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1600L, yen)
    }

    @Test
    fun withoutStepperThereAreNoButtons() {
        rule.setMonarchContent {
            MonarchCurrencyInput(value = 1.0, currencyCode = "USD", onValueChange = {}, label = "Amount")
        }
        rule.onNodeWithContentDescription("Increase").assertDoesNotExist()
    }

    @Test
    fun theCodeAndNameFormsAreUsed() {
        rule.setMonarchContent {
            MonarchCurrencyInput(
                value = 12.0,
                currencyCode = "USD",
                onValueChange = {},
                label = "Amount",
                currencyDisplay = MonarchMoneyCurrencyDisplay.Code,
            )
        }
        assertEquals(true, field.text()?.contains("USD"))
    }

    @Test
    fun theButtonsMirrorRightToLeftAndTheTextFitsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale, layoutDirection = LayoutDirection.Rtl) {
            MonarchCurrencyInput(
                value = 12.0,
                currencyCode = "USD",
                onValueChange = {},
                label = "Amount",
                helperText = "Charged today.",
                showSteppers = true,
            )
        }
        rule.onNodeWithContentDescription("Decrease")
            .assertIsBefore(rule.onNodeWithContentDescription("Increase"), LayoutDirection.Rtl)
        rule.onNode(hasSetTextAction()).assertExists()
        rule.onNode(hasText("Charged today."), useUnmergedTree = true).assertTextFullyVisible()
    }
}
