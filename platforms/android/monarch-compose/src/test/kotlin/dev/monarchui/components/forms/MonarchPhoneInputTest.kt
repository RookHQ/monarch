package dev.monarchui.components.forms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import dev.monarchui.components.forms.phoneinput.MonarchPhoneInput
import dev.monarchui.components.forms.phoneinput.MonarchPhoneInputDefaults
import dev.monarchui.components.forms.phoneinput.MonarchPhoneInputLabels
import dev.monarchui.components.forms.phoneinput.MonarchPhoneInputParts
import dev.monarchui.components.forms.phoneinput.MonarchPhoneInputTokens
import dev.monarchui.components.geo.phonecountryselect.MonarchPhoneCountrySelectLabels
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

internal object MonarchPhoneInputFixtures : ComponentFixtures {
    override val id = "phone-input"
    override val fixtures = listOf(
        fixture("a number with helper") {
            MonarchPhoneInput(
                value = "+442079460958",
                onValueChange = {},
                label = "Phone",
                helperText = "We call only about your order.",
                locale = Locale.US,
            )
        },
        fixture("empty with placeholder and suggested countries") {
            MonarchPhoneInput(
                value = null,
                onValueChange = {},
                label = "Mobile",
                placeholder = "555 010 0123",
                preferred = listOf("US", "GB"),
                locale = Locale.US,
            )
        },
        fixture("limited countries") {
            MonarchPhoneInput(
                value = null,
                onValueChange = {},
                defaultCountry = "IN",
                label = "Mobile",
                countries = listOf("IN", "AE", "SG"),
                locale = Locale.US,
            )
        },
        fixture("error from the app") {
            MonarchPhoneInput(
                value = "+15550100123",
                onValueChange = {},
                label = "Office phone",
                errorText = "This number is already used by another member of the workspace.",
                locale = Locale.US,
            )
        },
        fixture("disabled") {
            MonarchPhoneInput(
                value = "+15550100123",
                onValueChange = {},
                label = "Office phone",
                enabled = false,
                locale = Locale.US,
            )
        },
        fixture("read only") {
            MonarchPhoneInput(
                value = "+35121234567",
                onValueChange = {},
                label = "Office phone",
                readOnly = true,
                locale = Locale.US,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchPhoneInputTest {
    @get:Rule
    val rule = createComposeRule()

    private val number =
        SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText) and hasContentDescription("Phone")

    private fun SemanticsNodeInteraction.text(): String? =
        fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text?.removePrefix("‎")

    private fun SemanticsNodeInteraction.error(): String? =
        fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error)

    private fun row(text: String) = hasText(text) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)

    @Test
    fun numbersAreJoinedIntoE164() {
        assertEquals("+442079460958", MonarchPhoneInputDefaults.toE164("+44", "020 7946 0958", "GB"))
        assertEquals("+15550100123", MonarchPhoneInputDefaults.toE164("1", "(555) 010-0123"))
        assertEquals("+390612345678", MonarchPhoneInputDefaults.toE164("+39", "06 1234 5678", "it"))
        assertEquals("+39612345678", MonarchPhoneInputDefaults.toE164("+39", "06 1234 5678", "FR"))
        assertEquals("+97141234567", MonarchPhoneInputDefaults.toE164("+971", "٠٤١٢٣٤٥٦٧"))
        assertNull(MonarchPhoneInputDefaults.toE164("+44", " - "))
        assertTrue(MonarchPhoneInputDefaults.isValidE164("+442079460958"))
        assertFalse(MonarchPhoneInputDefaults.isValidE164("+4412345"))
        assertFalse(MonarchPhoneInputDefaults.isValidE164("+0442079460958"))
        assertFalse(MonarchPhoneInputDefaults.isValidE164("+4420794609581234"))
    }

    @Test
    fun internationalNumbersAreSplitAndSharedCodesResolved() {
        assertEquals(
            MonarchPhoneInputParts("GB", "+44", "2079460958"),
            MonarchPhoneInputDefaults.parse("+44 20 7946 0958"),
        )
        assertEquals("GB", MonarchPhoneInputDefaults.parse("0044 (20) 7946-0958")?.country)
        assertEquals("BS", MonarchPhoneInputDefaults.parse("+1 242 555 0100")?.country)
        assertEquals("US", MonarchPhoneInputDefaults.parse("+1 555 010 0123")?.country)
        assertEquals("CA", MonarchPhoneInputDefaults.parse("+1 555 010 0123", preferred = "ca")?.country)
        assertEquals("KZ", MonarchPhoneInputDefaults.parse("+7 701 123 4567")?.country)
        assertEquals("RU", MonarchPhoneInputDefaults.parse("+7 912 123 4567")?.country)
        assertEquals("VA", MonarchPhoneInputDefaults.parse("+39 06698 12345")?.country)
        assertEquals("PT", MonarchPhoneInputDefaults.parse("+351 21 234 5678")?.country)
        assertNull(MonarchPhoneInputDefaults.parse("020 7946 0958"))
        assertNull(MonarchPhoneInputDefaults.parse("+"))
        assertNull(MonarchPhoneInputDefaults.parse("+44 abc"))
    }

    @Test
    fun theNumberIsSavedAsE164OnDoneOnly() {
        var value by mutableStateOf<String?>(null)
        var calls = 0
        rule.setMonarchContent {
            MonarchPhoneInput(
                value = value,
                onValueChange = {
                    value = it
                    calls++
                },
                defaultCountry = "GB",
                label = "Phone",
                locale = Locale.US,
            )
        }
        rule.onNodeWithText("United Kingdom (+44)").assertExists()
        rule.onNode(number).performTextInput("020 7946 0958")
        assertEquals(0, calls)
        rule.onNode(number).performImeAction()
        assertEquals("+442079460958", value)
        assertEquals("2079460958", rule.onNode(number).text())
        rule.onNode(number).performTextClearance()
        rule.onNode(number).performImeAction()
        assertNull(value)
        assertEquals(2, calls)
    }

    @Test
    fun aPastedInternationalNumberPicksItsCountry() {
        var value by mutableStateOf<String?>(null)
        var country: String? = null
        rule.setMonarchContent {
            MonarchPhoneInput(
                value = value,
                onValueChange = { value = it },
                onCountryChange = { country = it },
                label = "Phone",
                locale = Locale.US,
            )
        }
        rule.onNodeWithText("United States (+1)").assertExists()
        rule.onNode(number).performTextInput("+351 21 234 5678")
        assertEquals("PT", country)
        assertEquals("212345678", rule.onNode(number).text())
        rule.onNodeWithText("Portugal (+351)").assertExists()
        rule.onNode(number).performImeAction()
        assertEquals("+351212345678", value)
    }

    @Test
    fun aSharedCallingCodeMovesToTheCountryOfItsDigits() {
        var value by mutableStateOf<String?>(null)
        var country: String? = null
        rule.setMonarchContent {
            MonarchPhoneInput(
                value = value,
                onValueChange = { value = it },
                onCountryChange = { country = it },
                label = "Phone",
                locale = Locale.US,
            )
        }
        rule.onNode(number).performTextInput("242 555 0100")
        rule.onNode(number).performImeAction()
        assertEquals("+12425550100", value)
        assertEquals("BS", country)
    }

    @Test
    fun anIncompleteNumberIsFlaggedAndNotSent() {
        var value by mutableStateOf<String?>("+442079460958")
        rule.setMonarchContent {
            MonarchPhoneInput(value = value, onValueChange = { value = it }, label = "Phone", locale = Locale.US)
        }
        rule.onNode(number).performTextClearance()
        rule.onNode(number).performTextInput("1234")
        rule.onNode(number).performImeAction()
        assertEquals("+442079460958", value)
        assertEquals("Enter a complete phone number.", rule.onNode(number).error())
        assertEquals("1234", rule.onNode(number).text())
        rule.onNode(number).performTextInput("5")
        assertNull(rule.onNode(number).error())
    }

    @Test
    fun choosingAnotherCountrySendsTheNumberAgain() {
        var value by mutableStateOf<String?>("+442079460958")
        rule.setMonarchContent {
            MonarchPhoneInput(
                value = value,
                onValueChange = { value = it },
                label = "Phone",
                countries = listOf("GB", "PT", "US"),
                locale = Locale.US,
            )
        }
        rule.onNodeWithContentDescription("Country code").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        rule.onNode(row("Portugal (+351)")).performClick()
        rule.waitForIdle()
        assertEquals("+3512079460958", value)
        rule.onNodeWithText("Portugal (+351)").assertExists()
    }

    @Test
    fun aNewValueFromTheAppShowsItsCountryAndDigits() {
        var value by mutableStateOf<String?>("+15550100123")
        var country: String? = null
        rule.setMonarchContent {
            MonarchPhoneInput(
                value = value,
                onValueChange = { value = it },
                onCountryChange = { country = it },
                label = "Phone",
                locale = Locale.US,
            )
        }
        assertEquals("5550100123", rule.onNode(number).text())
        value = "+351212345678"
        rule.waitForIdle()
        assertEquals("212345678", rule.onNode(number).text())
        assertEquals("PT", country)
        rule.onNodeWithText("Portugal (+351)").assertExists()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchPhoneInput(
                value = null,
                onValueChange = {},
                label = "Phone",
                locale = Locale.FRANCE,
                labels = MonarchPhoneInputLabels(
                    invalid = "Numéro incomplet.",
                    country = MonarchPhoneCountrySelectLabels(label = "Indicatif"),
                ),
            )
        }
        rule.onNodeWithContentDescription("Indicatif").assertExists()
        rule.onNode(number).performTextInput("12")
        rule.onNode(number).performImeAction()
        assertEquals("Numéro incomplet.", rule.onNode(number).error())
    }

    @Test
    fun disabledAndReadOnlyLockBothParts() {
        var value by mutableStateOf<String?>("+15550100123")
        var readOnly by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchPhoneInput(
                value = value,
                onValueChange = { value = it },
                label = "Phone",
                locale = Locale.US,
                enabled = readOnly,
                readOnly = readOnly,
            )
        }
        rule.onNode(number).assertIsNotEnabled()
        rule.onNodeWithText("United States (+1)").assertIsNotEnabled()
        readOnly = true
        rule.waitForIdle()
        rule.onNodeWithText("United States (+1)").assertIsNotEnabled()
        assertEquals("5550100123", rule.onNode(number).text())
    }

    @Test
    fun theNumberReadsLeftToRightAndKeepsItsValue() {
        val shown = MonarchPhoneInputTokens.LeftToRight.filter(AnnotatedString("2079460958"))
        assertEquals("‎2079460958", shown.text.text)
        assertEquals(1, shown.offsetMapping.originalToTransformed(0))
        assertEquals(10, shown.offsetMapping.transformedToOriginal(11))
        assertEquals(0, shown.offsetMapping.transformedToOriginal(0))
    }

    @Test
    fun theFieldsFitAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchPhoneInput(value = null, onValueChange = {}, label = "Phone", locale = Locale.US)
        }
        rule.onNode(number).assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Phone", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("United States (+1)", useUnmergedTree = true).assertTextFullyVisible()
    }
}
