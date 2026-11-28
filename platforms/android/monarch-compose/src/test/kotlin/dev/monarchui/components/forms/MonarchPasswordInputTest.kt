package dev.monarchui.components.forms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.forms.passwordinput.MonarchPasswordInput
import dev.monarchui.components.forms.passwordinput.MonarchPasswordInputLabels
import dev.monarchui.components.forms.passwordinput.MonarchPasswordInputPurpose
import dev.monarchui.components.forms.passwordinput.MonarchPasswordInputTokens
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.textLayout
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal object MonarchPasswordInputFixtures : ComponentFixtures {
    override val id = "password-input"
    override val fixtures = listOf(
        fixture("empty with helper") {
            MonarchPasswordInput(
                value = "",
                onValueChange = {},
                label = "Password",
                helperText = "At least 8 characters.",
            )
        },
        fixture("hidden") { MonarchPasswordInput(value = "correct horse", onValueChange = {}, label = "Password") },
        fixture("revealed") {
            MonarchPasswordInput(value = "correct horse", onValueChange = {}, label = "Password", revealed = true)
        },
        fixture("new password with error") {
            MonarchPasswordInput(
                value = "abc",
                onValueChange = {},
                label = "New password",
                purpose = MonarchPasswordInputPurpose.New,
                errorText = "Use at least 8 characters, with a number and a symbol.",
            )
        },
        fixture("disabled") {
            MonarchPasswordInput(value = "secret", onValueChange = {}, label = "Password", enabled = false)
        },
        fixture("icon toggle") {
            MonarchPasswordInput(
                value = "secret",
                onValueChange = {},
                label = "Password",
                revealIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchPasswordInputTest {
    @get:Rule
    val rule = createComposeRule()

    private val field get() = rule.onNode(hasSetTextAction())
    private val toggle get() = rule.onNodeWithContentDescription("Show password")

    @Test
    fun isAPasswordFieldWithThePasswordAutofillType() {
        var value by mutableStateOf("")
        rule.setMonarchContent {
            MonarchPasswordInput(value = value, onValueChange = { value = it }, label = "Password")
        }
        val config = field.fetchSemanticsNode().config
        assertTrue(config.contains(SemanticsProperties.Password))
        assertEquals(ContentType.Password, config.getOrNull(SemanticsProperties.ContentType))
        field.performTextInput("hunter22")
        assertEquals("hunter22", value)
    }

    @Test
    fun aNewPasswordUsesTheNewPasswordAutofillType() {
        rule.setMonarchContent {
            MonarchPasswordInput(
                value = "",
                onValueChange = {},
                label = "Password",
                purpose = MonarchPasswordInputPurpose.New,
            )
        }
        val type = field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentType)
        assertEquals(ContentType.NewPassword, type)
    }

    private fun clickLabel(): String? = toggle.fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)?.label

    @Test
    fun theToggleKeepsItsNameAndShowsThePassword() {
        rule.setMonarchContent {
            MonarchPasswordInput(value = "hunter22", onValueChange = {}, label = "Password")
        }
        toggle.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).assertIsOff()
        assertEquals("Show", clickLabel())
        toggle.performClick()
        toggle.assertIsOn()
        assertEquals("Hide", clickLabel())
        // The drawn text is the password itself while revealed, and dots while hidden.
        assertEquals("hunter22", field.textLayout().layoutInput.text.text)
        toggle.performClick().assertIsOff()
        assertEquals("\u2022".repeat(8), field.textLayout().layoutInput.text.text)
    }

    @Test
    fun aHoistedStateIsUsedAndReported() {
        var revealed by mutableStateOf(true)
        val reported = mutableListOf<Boolean>()
        rule.setMonarchContent {
            MonarchPasswordInput(
                value = "hunter22",
                onValueChange = {},
                label = "Password",
                revealed = revealed,
                onRevealedChange = {
                    reported += it
                    revealed = it
                },
            )
        }
        toggle.assertIsOn().performClick()
        assertEquals(listOf(false), reported)
        toggle.assertIsOff()
    }

    @Test
    fun aDisabledFieldDisablesTheToggle() {
        var changes = 0
        rule.setMonarchContent {
            MonarchPasswordInput(
                value = "x",
                onValueChange = {},
                label = "Password",
                enabled = false,
                onRevealedChange = { changes++ },
            )
        }
        toggle.assertIsNotEnabled().performClick()
        assertEquals(0, changes)
    }

    @Test
    fun theToggleHasA48dpTargetAtTheEndInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchPasswordInput(value = "x", onValueChange = {}, label = "Password")
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            toggle.assertTouchTargetAtLeast48dp(rule.density)
            rule.onNodeWithText("Password", useUnmergedTree = true).assertIsBefore(toggle, direction)
        }
    }

    @Test
    fun theToggleTextFitsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchPasswordInput(value = "x", onValueChange = {}, label = "Password")
        }
        toggle.assertTouchTargetAtLeast48dp(rule.density)
        assertEquals("Show", clickLabel())
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchPasswordInput(
                value = "x",
                onValueChange = {},
                label = "Code",
                labels = MonarchPasswordInputLabels(reveal = "Show code", show = "View", hide = "Mask"),
            )
        }
        rule.onNodeWithContentDescription("Show code").performClick()
        val node = rule.onNodeWithContentDescription("Show code").fetchSemanticsNode()
        assertEquals("Mask", node.config.getOrNull(SemanticsActions.OnClick)?.label)
    }

    @Test
    fun theToggleColourComesFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchPasswordInputTokens.toggleColor(true, colors), theme.id)
            assertEquals(colors.textDisabled, MonarchPasswordInputTokens.toggleColor(false, colors), theme.id)
            assertNotEquals(
                MonarchPasswordInputTokens.toggleColor(true, colors),
                MonarchPasswordInputTokens.toggleColor(false, colors),
                theme.id,
            )
        }
    }
}
