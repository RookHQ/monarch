package dev.monarchui.components.forms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.forms.otppininput.MonarchOtpPinInput
import dev.monarchui.components.forms.otppininput.MonarchOtpPinInputMode
import dev.monarchui.components.forms.otppininput.MonarchOtpPinInputSize
import dev.monarchui.components.forms.otppininput.MonarchOtpPinInputTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal object MonarchOtpPinInputFixtures : ComponentFixtures {
    override val id = "otp-pin-input"
    override val fixtures = listOf(
        fixture("empty with helper") {
            MonarchOtpPinInput(
                value = "",
                onValueChange = {},
                label = "Verification code",
                helperText = "Sent by text.",
            )
        },
        fixture("partly filled, grouped") {
            MonarchOtpPinInput(value = "123", onValueChange = {}, label = "Verification code", groupSize = 3)
        },
        fixture("masked PIN") {
            MonarchOtpPinInput(value = "12", onValueChange = {}, label = "PIN", length = 4, masked = true)
        },
        fixture("large, alphanumeric") {
            MonarchOtpPinInput(
                value = "AB1",
                onValueChange = {},
                label = "Room code",
                length = 4,
                mode = MonarchOtpPinInputMode.Alphanumeric,
                size = MonarchOtpPinInputSize.Lg,
            )
        },
        fixture("error") {
            MonarchOtpPinInput(
                value = "123456",
                onValueChange = {},
                label = "Verification code",
                errorText = "That code has expired. Ask for a new one.",
            )
        },
        fixture("disabled") { MonarchOtpPinInput(value = "12", onValueChange = {}, label = "Code", enabled = false) },
        fixture("eight cells, large") {
            MonarchOtpPinInput(
                value = "",
                onValueChange = {},
                label = "Recovery code",
                length = 8,
                size = MonarchOtpPinInputSize.Lg,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchOtpPinInputTest {
    @get:Rule
    val rule = createComposeRule()

    private val field get() = rule.onNode(hasSetTextAction())

    @Test
    fun isOneFieldNamedByItsLabelWithTheOneTimeCodeAutofillType() {
        rule.setMonarchContent {
            MonarchOtpPinInput(value = "", onValueChange = {}, label = "Verification code")
        }
        field.assertTextContains("Verification code")
        val config = field.fetchSemanticsNode().config
        assertEquals(ContentType.SmsOtpCode, config.getOrNull(SemanticsProperties.ContentType))
        assertEquals("Code length: 6", field.stateDescription())
    }

    @Test
    fun acceptsOnlyDigitsUpToTheLengthAndReportsCompletion() {
        var value by mutableStateOf("")
        val completed = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchOtpPinInput(
                value = value,
                onValueChange = { value = it },
                label = "Code",
                length = 4,
                onComplete = { completed += it },
            )
        }
        field.performTextInput("1a2-3")
        assertEquals("123", value)
        assertEquals(emptyList(), completed)
        field.performTextInput("45")
        assertEquals("1234", value)
        assertEquals(listOf("1234"), completed)
        field.performTextReplacement("9876 5")
        assertEquals("9876", value)
        assertEquals(listOf("1234", "9876"), completed)
    }

    @Test
    fun theAlphanumericModeUpperCasesLetters() {
        assertEquals("AB12", MonarchOtpPinInputTokens.clean("ab-12", MonarchOtpPinInputMode.Alphanumeric, 6))
        assertEquals("12", MonarchOtpPinInputTokens.clean("ab-12", MonarchOtpPinInputMode.Numeric, 6))
        assertEquals("123", MonarchOtpPinInputTokens.clean("١٢٣", MonarchOtpPinInputMode.Numeric, 6))
    }

    @Test
    fun theCharactersAreDrawnInCellsOrAsDotsWhenMasked() {
        var masked by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchOtpPinInput(value = "47", onValueChange = {}, label = "PIN", length = 4, masked = masked)
        }
        rule.onNodeWithText("4", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("7", useUnmergedTree = true).assertExists()
        masked = true
        rule.waitForIdle()
        rule.onNodeWithText("4", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun theErrorIsExposedAndReplacesTheHelper() {
        rule.setMonarchContent {
            MonarchOtpPinInput(
                value = "123456",
                onValueChange = {},
                label = "Code",
                helperText = "Sent by text.",
                errorText = "That code has expired.",
            )
        }
        assertEquals("That code has expired.", field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error))
        rule.onNodeWithText("Sent by text.", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("That code has expired.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aDisabledFieldIsNotEditable() {
        rule.setMonarchContent {
            MonarchOtpPinInput(value = "1", onValueChange = {}, label = "Code", enabled = false)
        }
        // A disabled field offers no way to set its text.
        field.assertDoesNotExist()
        rule.onNodeWithText("Code").assertIsNotEnabled()
    }

    @Test
    fun theCellsStayLeftToRightAndTheFieldIsAtLeast48dpHigh() {
        val environment = MonarchTestEnvironment(fontScale = LargestFontScale)
        rule.setMonarchContent(environment) {
            MonarchOtpPinInput(value = "12", onValueChange = {}, label = "Code", length = 4)
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("1", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("2", useUnmergedTree = true), LayoutDirection.Ltr)
            rule.onNodeWithText("1", useUnmergedTree = true).assertTextFullyVisible()
            field.assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun theLengthHintCanBeReplaced() {
        rule.setMonarchContent {
            MonarchOtpPinInput(value = "", onValueChange = {}, label = "Code", length = 4, lengthHint = "Four digits")
        }
        assertEquals("Four digits", field.stateDescription())
    }

    @Test
    fun groupsAddAGapAndTheRowNeverExceedsItsNaturalWidth() {
        val t = MonarchOtpPinInputTokens
        assertTrue(t.groupEndsAfter(2, 6, 3))
        assertTrue(!t.groupEndsAfter(5, 6, 3))
        assertTrue(!t.groupEndsAfter(2, 6, null))
        assertEquals(
            t.cellSize(MonarchOtpPinInputSize.Md) * 6 + t.Gap * 6,
            t.rowWidth(6, 3, MonarchOtpPinInputSize.Md),
        )
    }

    @Test
    fun cellColoursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val t = MonarchOtpPinInputTokens
            assertEquals(colors.borderInput, t.cell(false, false, false, true, colors).border, theme.id)
            assertEquals(colors.textSubtle, t.cell(true, false, false, true, colors).border, theme.id)
            assertEquals(colors.focusRing, t.cell(false, true, false, true, colors).border, theme.id)
            assertEquals(colors.dangerStrong, t.cell(true, true, true, true, colors).border, theme.id)
            val disabled = t.cell(true, true, true, false, colors)
            assertEquals(colors.bgSubtle, disabled.container, theme.id)
            assertEquals(colors.textDisabled, disabled.content, theme.id)
            assertNotEquals(t.cell(false, false, false, true, colors), disabled, theme.id)
        }
    }
}
