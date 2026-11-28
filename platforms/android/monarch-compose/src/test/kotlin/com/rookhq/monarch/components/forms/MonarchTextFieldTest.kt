package com.rookhq.monarch.components.forms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.forms.textfield.MonarchTextFieldLabelVisibility
import com.rookhq.monarch.components.forms.textfield.MonarchTextFieldTokens
import com.rookhq.monarch.foundation.MonarchFieldSize
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.field.MonarchFieldState
import com.rookhq.monarch.internal.field.fieldColors
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.contrastRatio
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchTextFieldFixtures : ComponentFixtures {
    override val id = "text-field"
    override val fixtures = listOf(
        fixture("empty with label, placeholder and helper") {
            MonarchTextField(
                value = "",
                onValueChange = {},
                label = "Email",
                placeholder = "name@example.com",
                helperText = "Used to sign in.",
            )
        },
        fixture("filled") { MonarchTextField(value = "ada@example.com", onValueChange = {}, label = "Email") },
        fixture("error") {
            MonarchTextField(
                value = "ada@",
                onValueChange = {},
                label = "Email",
                errorText = "Enter a complete email address.",
            )
        },
        fixture("disabled") {
            MonarchTextField(value = "ada@example.com", onValueChange = {}, label = "Email", enabled = false)
        },
        fixture("read only") {
            MonarchTextField(value = "Acme", onValueChange = {}, label = "Workspace", readOnly = true)
        },
        fixture("icons and password") {
            MonarchTextField(
                value = "secret",
                onValueChange = {},
                label = "Password",
                leadingIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                trailingIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
            )
        },
        fixture("multi-line with long text") {
            MonarchTextField(
                value = "A long note that needs more than one line to be shown in full on a phone.",
                onValueChange = {},
                label = "Note",
                singleLine = false,
            )
        },
        fixture("required") {
            MonarchTextField(value = "", onValueChange = {}, label = "Email", required = true)
        },
        fixture("success") {
            MonarchTextField(
                value = "ada",
                onValueChange = {},
                label = "Username",
                successText = "Username is available.",
            )
        },
        fixture("prefix and suffix") {
            MonarchTextField(
                value = "42",
                onValueChange = {},
                label = "Weight",
                prefix = { Text("~") },
                suffix = { Text("kg") },
            )
        },
        fixture("multi-line with minimum lines, monospace") {
            MonarchTextField(
                value = "SELECT 1",
                onValueChange = {},
                label = "Query",
                singleLine = false,
                minLines = 3,
                textStyle = MonarchTheme.typography.code,
            )
        },
        fixture("hidden label") {
            MonarchTextField(
                value = "",
                onValueChange = {},
                label = "Search members",
                labelVisibility = MonarchTextFieldLabelVisibility.Hidden,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchTextFieldTest {
    @get:Rule
    val rule = createComposeRule()

    private val field get() = rule.onNode(hasSetTextAction())

    @Test
    fun theTextFieldValueFieldKeepsTheCaretAndTheSelectionAndTheSameAnatomy() {
        var value by mutableStateOf(TextFieldValue("Draft", selection = TextRange(5)))
        rule.setMonarchContent {
            MonarchTextField(
                value = value,
                onValueChange = { value = it },
                label = "Message",
                helperText = "Edit your message",
                errorText = null,
            )
        }
        field.assertContentDescriptionEquals("Message")
        assertEquals(TextRange(5), field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange])
        rule.onNodeWithText("Edit your message", useUnmergedTree = true).assertExists()
        field.performTextInput(" two")
        assertEquals("Draft two", value.text)
        assertEquals(TextRange(9), value.selection)
    }

    @Test
    fun isAnEditableFieldNamedByItsLabel() {
        var value by mutableStateOf("")
        rule.setMonarchContent {
            MonarchTextField(value = value, onValueChange = { value = it }, label = "Email")
        }
        field.assertIsEnabled().assertContentDescriptionEquals("Email")
        field.performTextInput("ada@example.com")
        assertEquals("ada@example.com", value)
        val expected = AnnotatedString("ada@example.com")
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, expected))
    }

    @Test
    fun showsHelperTextAndHasNoErrorByDefault() {
        rule.setMonarchContent {
            MonarchTextField(value = "", onValueChange = {}, label = "Email", helperText = "Used to sign in.")
        }
        rule.onNodeWithText("Used to sign in.", useUnmergedTree = true).assertExists()
        assertNull(field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error))
    }

    @Test
    fun theErrorTextReplacesTheHelperAndIsExposedAsTheError() {
        rule.setMonarchContent {
            MonarchTextField(
                value = "ada@",
                onValueChange = {},
                label = "Email",
                helperText = "Used to sign in.",
                errorText = "Enter a complete email address.",
            )
        }
        rule.onNodeWithText("Used to sign in.", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("Enter a complete email address.", useUnmergedTree = true).assertExists()
        assertEquals(
            "Enter a complete email address.",
            field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error),
        )
    }

    @Test
    fun aDisabledFieldIsAnnouncedAsDisabled() {
        rule.setMonarchContent {
            MonarchTextField(value = "ada@example.com", onValueChange = {}, label = "Email", enabled = false)
        }
        rule.onNodeWithContentDescription("Email").assertIsNotEnabled()
    }

    @Test
    fun isAtLeast48dpHighAlsoAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchTextField(value = "ada@example.com", onValueChange = {}, label = "Email")
        }
        field.assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun aRequiredFieldShowsAMarkAndSaysRequired() {
        rule.setMonarchContent { MonarchTextField(value = "", onValueChange = {}, label = "Email", required = true) }
        field.assertContentDescriptionEquals("Email, required")
        // The visible label and its mark are decoration for TalkBack: the field carries the name.
        rule.onNodeWithText("Email", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("*", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theRequiredNameCanBeReplaced() {
        rule.setMonarchContent {
            MonarchTextField(
                value = "",
                onValueChange = {},
                label = "Email",
                required = true,
                requiredLabel = "Email, needed",
            )
        }
        field.assertContentDescriptionEquals("Email, needed")
    }

    @Test
    fun theSuccessTextReplacesTheHelperAndColoursTheOutline() {
        rule.setMonarchContent {
            MonarchTextField(
                value = "ada",
                onValueChange = {},
                label = "Username",
                helperText = "Letters and digits.",
                successText = "Username is available.",
            )
        }
        rule.onNodeWithText("Letters and digits.", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("Username is available.", useUnmergedTree = true).assertExists()
        assertNull(field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error))
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.successStrong, MonarchTextFieldTokens.successColor(colors), theme.id)
            assertEquals(colors.dangerStrong, MonarchTextFieldTokens.requiredMarkColor(colors), theme.id)
        }
    }

    @Test
    fun anErrorWinsOverTheSuccessText() {
        rule.setMonarchContent {
            MonarchTextField(
                value = "ada",
                onValueChange = {},
                label = "Username",
                successText = "Username is available.",
                errorText = "Taken.",
            )
        }
        rule.onNodeWithText("Username is available.", useUnmergedTree = true).assertDoesNotExist()
        assertEquals("Taken.", field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error))
    }

    @Test
    fun prefixAndSuffixAreShown() {
        rule.setMonarchContent {
            MonarchTextField(
                value = "42",
                onValueChange = {},
                label = "Weight",
                prefix = { Text("~") },
                suffix = { Text("kg") },
            )
        }
        rule.onNodeWithText("kg", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("~", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aMultiLineFieldStartsWithItsMinimumLines() {
        rule.setMonarchContent {
            MonarchTextField(value = "", onValueChange = {}, label = "Note", singleLine = false, minLines = 4)
        }
        field.assertHeightIsAtLeast(96.dp)
    }

    @Test
    fun aHiddenLabelIsNotDrawnButStillNamesTheField() {
        rule.setMonarchContent {
            MonarchTextField(
                value = "",
                onValueChange = {},
                label = "Search members",
                labelVisibility = MonarchTextFieldLabelVisibility.Hidden,
            )
        }
        rule.onNodeWithText("Search members", useUnmergedTree = true).assertDoesNotExist()
        field.assertContentDescriptionEquals("Search members")
    }

    @Test
    fun theTextStyleCanBeReplaced() {
        rule.setMonarchContent {
            MonarchTextField(
                value = "ABC-123",
                onValueChange = {},
                label = "Code",
                textStyle = MonarchTheme.typography.code,
            )
        }
        field.assertExists()
    }

    @Test
    fun aRequiredLabelFitsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchTextField(value = "x", onValueChange = {}, label = "Email", required = true)
        }
        field.assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun theLabelSitsAboveTheBoxAndNeverInsideIt() {
        rule.setMonarchContent {
            MonarchTextField(value = "", onValueChange = {}, label = "Email", placeholder = "name@example.com")
        }
        val label = rule.onNodeWithText("Email", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val value = rule.onNodeWithText("name@example.com", useUnmergedTree = true).getUnclippedBoundsInRoot()
        // The label ends before the text of the box begins: no floating or notched label.
        assertTrue(label.bottom <= value.top, "label ${label.bottom} above value ${value.top}")
        assertTrue(label.left <= value.left)
    }

    @Test
    fun theBoxIs48dpTallByDefaultAndFollowsTheSize() {
        rule.setMonarchContent {
            Column {
                MonarchTextField(value = "", onValueChange = {}, placeholder = "medium")
                MonarchTextField(value = "", onValueChange = {}, placeholder = "small", size = MonarchFieldSize.Small)
                MonarchTextField(value = "", onValueChange = {}, placeholder = "large", size = MonarchFieldSize.Large)
            }
        }
        rule.onNodeWithText("medium", useUnmergedTree = true).onParent().assertHeightIsAtLeast(48.dp)
        rule.onNodeWithText("large", useUnmergedTree = true).onParent().assertHeightIsAtLeast(52.dp)
    }

    @Test
    fun theFieldFillsTheAvailableWidthByDefault() {
        rule.setMonarchContent {
            Box(Modifier.width(300.dp)) { MonarchTextField(value = "", onValueChange = {}, label = "Email") }
        }
        field.assertWidthIsEqualTo(300.dp)
    }

    @Test
    fun aCallerCanNarrowTheField() {
        rule.setMonarchContent {
            Box(Modifier.width(300.dp)) {
                MonarchTextField(value = "", onValueChange = {}, modifier = Modifier.width(120.dp), label = "Email")
            }
        }
        field.assertWidthIsEqualTo(120.dp)
    }

    @Test
    fun theErrorLineIsHiddenFromTalkBackBecauseTheFieldCarriesTheErrorAndALiveRegionAnnouncesIt() {
        rule.setMonarchContent {
            MonarchTextField(value = "ada@", onValueChange = {}, label = "Email", errorText = "Not an email.")
        }
        val live = rule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
            useUnmergedTree = true,
        )
        live.assertCountEquals(1)
        live[0].assertContentDescriptionEquals("Not an email.")
        // The field node does not take the live region for itself, or every keystroke would be announced.
        assertNull(field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.LiveRegion))
    }

    @Test
    fun helperAndErrorNeverShowTogetherSoTheyCannotOverlap() {
        rule.setMonarchContent {
            MonarchTextField(
                value = "x",
                onValueChange = {},
                label = "Email",
                helperText = "Used to sign in.",
                errorText = "Not an email.",
            )
        }
        rule.onNodeWithText("Used to sign in.", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("Not an email.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aLongLabelWrapsAtTheLargestFontSizeInsteadOfBeingCut() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Box(Modifier.width(200.dp)) {
                MonarchTextField(value = "", onValueChange = {}, label = "Billing contact email address for invoices")
            }
        }
        val label = rule.onNodeWithText("Billing contact email address for invoices", useUnmergedTree = true)
        label.assertExists()
        // A wrapped label is taller than one line (at scale 2.0 one line is about 40dp).
        label.assertHeightIsAtLeast(60.dp)
    }

    @Test
    fun theTextCanBeTypedIntoAMultilineField() {
        var value by mutableStateOf("")
        rule.setMonarchContent {
            MonarchTextField(value = value, onValueChange = { value = it }, label = "Notes", singleLine = false)
        }
        field.performTextInput("one\ntwo")
        assertEquals("one\ntwo", value)
    }

    @Test
    fun theTextColoursOfAFieldReachTheContrastOfTheirFillInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val enabled = colors.fieldColors(MonarchFieldState())
            val invalid = colors.fieldColors(MonarchFieldState(invalid = true))
            val ratio = { fore: androidx.compose.ui.graphics.Color, back: androidx.compose.ui.graphics.Color ->
                contrastRatio(fore, back)
            }
            assertTrue(ratio(enabled.text, enabled.container) >= TextContrast, "${theme.id} value")
            assertTrue(ratio(enabled.placeholder, enabled.container) >= TextContrast, "${theme.id} placeholder")
            assertTrue(ratio(colors.textDefault, colors.bgSurface) >= TextContrast, "${theme.id} label")
            assertTrue(ratio(colors.textMuted, colors.bgSurface) >= TextContrast, "${theme.id} helper")
            assertTrue(ratio(colors.dangerStrong, colors.bgSurface) >= TextContrast, "${theme.id} error")
            assertTrue(ratio(colors.successStrong, colors.bgSurface) >= TextContrast, "${theme.id} success")
            assertTrue(ratio(invalid.border, invalid.container) >= BorderContrast, "${theme.id} invalid border")
            assertTrue(ratio(enabled.border, enabled.container) >= BorderContrast, "${theme.id} border")
        }
    }

    @Test
    fun aFieldWithoutLabelOrMessageIsExactlyItsBoxWithNoGapBelow() {
        rule.setMonarchContent {
            MonarchTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.testTag("field"),
                label = "Search",
                labelVisibility = MonarchTextFieldLabelVisibility.Hidden,
            )
        }
        rule.onNodeWithTag("field").assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun theFocusedBorderKeepsTheRestingColourSoOnlyTheRingChanges() {
        lateinit var fieldColors: androidx.compose.material3.TextFieldColors
        rule.setMonarchContent { fieldColors = MonarchTextFieldTokens.colors() }
        kotlin.test.assertEquals(fieldColors.unfocusedIndicatorColor, fieldColors.focusedIndicatorColor)
    }
}

private const val TextContrast = 4.5
private const val BorderContrast = 3.0
