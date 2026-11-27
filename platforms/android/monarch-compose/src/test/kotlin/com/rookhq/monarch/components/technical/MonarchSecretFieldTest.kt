package com.rookhq.monarch.components.technical

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
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
import androidx.test.core.app.ApplicationProvider
import com.rookhq.monarch.components.actions.copybutton.MonarchCopyButtonTokens
import com.rookhq.monarch.components.technical.secretfield.MonarchSecretField
import com.rookhq.monarch.components.technical.secretfield.MonarchSecretFieldLabels
import com.rookhq.monarch.components.technical.secretfield.MonarchSecretFieldTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.textLayout
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal object MonarchSecretFieldFixtures : ComponentFixtures {
    override val id = "secret-field"
    override val fixtures = listOf(
        fixture("masked with copy") {
            MonarchSecretField(value = "sk_live_51Hx", label = "API key", onValueChange = {}, copyable = true)
        },
        fixture("revealed") {
            MonarchSecretField(value = "sk_live_51Hx", label = "API key", onValueChange = {}, revealed = true)
        },
        fixture("saved, not shown") {
            MonarchSecretField(value = "", label = "Signing secret", onValueChange = {}, saved = true, copyable = true)
        },
        fixture("error") {
            MonarchSecretField(
                value = "abc",
                label = "Bot token",
                onValueChange = {},
                errorText = "Tokens start with xoxb-.",
            )
        },
        fixture("read-only with icons") {
            MonarchSecretField(
                value = "whsec_8f2c",
                label = "Webhook secret",
                onValueChange = {},
                readOnly = true,
                copyable = true,
                revealIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                copyIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        },
        fixture("disabled") {
            MonarchSecretField(
                value = "sk_live_51Hx",
                label = "API key",
                onValueChange = {},
                enabled = false,
                copyable = true,
                helperText = "Ask an admin to rotate it.",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSecretFieldTest {
    @get:Rule
    val rule = createComposeRule()

    private val field get() = rule.onNode(hasSetTextAction())
    private val toggle get() = rule.onNodeWithContentDescription("Show API key")
    private val copy get() = rule.onNodeWithContentDescription("Copy API key")
    private val isCheckbox = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)

    private fun clipboard(): ClipboardManager =
        ApplicationProvider.getApplicationContext<Context>().getSystemService(ClipboardManager::class.java)

    @Test
    fun theSecretIsMaskedUntilTheToggleShowsIt() {
        rule.setMonarchContent { MonarchSecretField(value = "sk_live_51Hx", label = "API key", onValueChange = {}) }
        assertEquals("•".repeat(12), field.textLayout().layoutInput.text.text)
        toggle.assert(isCheckbox).assertIsOff()
        rule.onNodeWithText("Show", useUnmergedTree = true).assertExists()
        toggle.performClick().assertIsOn()
        rule.onNodeWithText("Hide", useUnmergedTree = true).assertExists()
        assertEquals("sk_live_51Hx", field.textLayout().layoutInput.text.text)
        assertTrue(field.fetchSemanticsNode().config.contains(SemanticsProperties.Password))
    }

    @Test
    fun aHoistedRevealStateIsUsedAndReported() {
        var revealed by mutableStateOf(true)
        val reported = mutableListOf<Boolean>()
        rule.setMonarchContent {
            MonarchSecretField(
                value = "sk_live_51Hx",
                label = "API key",
                onValueChange = {},
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
    fun copyPutsASensitiveClipOnTheClipboardWithoutRevealing() {
        val copied = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchSecretField(
                value = "sk_live_51Hx",
                label = "API key",
                onValueChange = {},
                copyable = true,
                onCopy = { copied += it },
            )
        }
        copy.performClick()
        rule.waitForIdle()
        assertEquals(listOf("sk_live_51Hx"), copied)
        val clip = clipboard().primaryClip
        assertEquals("sk_live_51Hx", clip?.getItemAt(0)?.text?.toString())
        assertTrue(clip?.description?.extras?.getBoolean(MonarchCopyButtonTokens.EXTRA_IS_SENSITIVE) == true)
        toggle.assertIsOff()
    }

    @Test
    fun anEmptyFieldOffersNothingToRevealOrCopy() {
        rule.setMonarchContent {
            MonarchSecretField(value = "", label = "API key", onValueChange = {}, copyable = true)
        }
        toggle.assertIsNotEnabled()
        copy.assertIsNotEnabled()
    }

    @Test
    fun aSavedSecretSaysSoUntilANewValueIsTyped() {
        var value by mutableStateOf("")
        rule.setMonarchContent {
            MonarchSecretField(value = value, label = "API key", onValueChange = { value = it }, saved = true)
        }
        val description = "A value is saved and hidden. Leave blank to keep it."
        rule.onNodeWithText(description, useUnmergedTree = true).assertExists()
        toggle.assertIsNotEnabled()
        field.performTextInput("sk_new")
        assertEquals("sk_new", value)
        rule.onNodeWithText(description, useUnmergedTree = true).assertDoesNotExist()
        toggle.assertIsEnabled()
    }

    @Test
    fun anErrorReplacesTheHelperAndAnEmptyErrorIsNone() {
        var error by mutableStateOf<String?>("")
        rule.setMonarchContent {
            MonarchSecretField(
                value = "abc",
                label = "API key",
                onValueChange = {},
                helperText = "From the dashboard.",
                errorText = error,
            )
        }
        assertFalse(field.fetchSemanticsNode().config.contains(SemanticsProperties.Error))
        rule.onNodeWithText("From the dashboard.", useUnmergedTree = true).assertExists()
        error = "Keys start with sk_."
        rule.waitForIdle()
        assertEquals("Keys start with sk_.", field.fetchSemanticsNode().config[SemanticsProperties.Error])
        rule.onNodeWithText("From the dashboard.", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun aDisabledFieldDisablesBothButtons() {
        var changes = 0
        rule.setMonarchContent {
            MonarchSecretField(
                value = "sk_live_51Hx",
                label = "API key",
                onValueChange = {},
                enabled = false,
                copyable = true,
                onRevealedChange = { changes++ },
            )
        }
        toggle.assertIsNotEnabled().performClick()
        copy.assertIsNotEnabled()
        assertEquals(0, changes)
    }

    @Test
    fun theButtonsHave48dpTargetsInReadingOrder() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchSecretField(value = "sk_live_51Hx", label = "API key", onValueChange = {}, copyable = true)
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            toggle.assertTouchTargetAtLeast48dp(rule.density)
            copy.assertTouchTargetAtLeast48dp(rule.density)
            rule.onNodeWithText("API key", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Show", useUnmergedTree = true), direction)
            toggle.assertIsBefore(copy, direction)
        }
    }

    @Test
    fun theSavedDescriptionWrapsWholeAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchSecretField(value = "", label = "API key", onValueChange = {}, saved = true, copyable = true)
        }
        rule.onNodeWithText("A value is saved and hidden. Leave blank to keep it.", useUnmergedTree = true)
            .assertTextFullyVisible()
        toggle.assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchSecretField(
                value = "",
                label = "Clé",
                onValueChange = {},
                saved = true,
                copyable = true,
                labels = MonarchSecretFieldLabels(
                    show = { "Afficher la $it" },
                    showText = "Voir",
                    copy = "Copier",
                    savedDescription = "Une valeur est enregistrée.",
                ),
            )
        }
        rule.onNodeWithContentDescription("Afficher la Clé").assertExists()
        rule.onNodeWithText("Voir", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Copier Clé").assertExists()
        rule.onNodeWithText("Une valeur est enregistrée.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theToggleColourAndTheRulesComeFromTheTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.accentStrong, MonarchSecretFieldTokens.toggleColor(true, colors))
            assertEquals(colors.textDisabled, MonarchSecretFieldTokens.toggleColor(false, colors))
        }
        assertFalse(MonarchSecretFieldTokens.actionsEnabled(enabled = true, value = ""))
        assertFalse(MonarchSecretFieldTokens.actionsEnabled(enabled = false, value = "x"))
        assertTrue(MonarchSecretFieldTokens.actionsEnabled(enabled = true, value = "x"))
        assertTrue(MonarchSecretFieldTokens.showsSaved(saved = true, value = ""))
        assertFalse(MonarchSecretFieldTokens.showsSaved(saved = true, value = "x"))
        assertFalse(MonarchSecretFieldTokens.showsSaved(saved = false, value = ""))
    }
}
