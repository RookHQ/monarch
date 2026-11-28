package dev.monarchui.components.forms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import dev.monarchui.components.forms.inlineedittext.MonarchInlineEditText
import dev.monarchui.components.forms.inlineedittext.MonarchInlineEditTextLabels
import dev.monarchui.components.forms.inlineedittext.MonarchInlineEditTextTokens
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal object MonarchInlineEditTextFixtures : ComponentFixtures {
    override val id = "inline-edit-text"
    override val fixtures = listOf(
        fixture("view with icon") {
            MonarchInlineEditText(
                value = "Quarterly plan",
                onSave = {},
                label = "Title",
                editIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        },
        fixture("empty with placeholder") {
            MonarchInlineEditText(value = "", onSave = {}, label = "Notes", placeholder = "Add notes")
        },
        fixture("editing") { MonarchInlineEditText(value = "Ada", onSave = {}, label = "Name", startEditing = true) },
        fixture("editing with an error from the app") {
            MonarchInlineEditText(
                value = "Ada",
                onSave = {},
                label = "Name",
                startEditing = true,
                errorText = "Someone else changed this name.",
            )
        },
        fixture("editing several lines") {
            MonarchInlineEditText(
                value = "First line",
                onSave = {},
                label = "Notes",
                multiline = true,
                startEditing = true,
            )
        },
        fixture("disabled") { MonarchInlineEditText(value = "Locked", onSave = {}, label = "Owner", enabled = false) },
        fixture("long text") {
            MonarchInlineEditText(
                value = "A long title that needs more than one line on a phone screen",
                onSave = {},
                label = "Title",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchInlineEditTextTest {
    @get:Rule
    val rule = createComposeRule()

    private val field get() = rule.onNode(hasSetTextAction())
    private val viewer get() = rule.onNodeWithContentDescription("Title")

    @Test
    fun inViewModeItIsOneButtonWithTheLabelAndTheValue() {
        rule.setMonarchContent {
            MonarchInlineEditText(value = "Quarterly plan", onSave = {}, label = "Title")
        }
        viewer.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        assertEquals("Quarterly plan", viewer.stateDescription())
        val click = viewer.fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)
        assertEquals("Edit", click?.label)
        viewer.assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun anEmptyValueShowsThePlaceholder() {
        rule.setMonarchContent {
            MonarchInlineEditText(value = "", onSave = {}, label = "Title", placeholder = "Untitled")
        }
        assertEquals("Untitled", viewer.stateDescription())
    }

    @Test
    fun editsSavesTheTrimmedTextAndCloses() {
        var value by mutableStateOf("Plan")
        val saved = mutableListOf<String>()
        val editing = mutableListOf<Boolean>()
        rule.setMonarchContent {
            MonarchInlineEditText(
                value = value,
                onSave = {
                    saved += it
                    value = it
                },
                label = "Title",
                onEditingChange = { editing += it },
            )
        }
        viewer.performClick()
        field.assertIsFocused()
        field.performTextReplacement("  New plan  ")
        field.performImeAction()
        rule.waitForIdle()
        assertEquals(listOf("New plan"), saved)
        assertEquals(listOf(true, false), editing)
        field.assertDoesNotExist()
        assertEquals("New plan", viewer.stateDescription())
    }

    @Test
    fun anUnchangedValueIsNotSaved() {
        var saves = 0
        rule.setMonarchContent {
            MonarchInlineEditText(value = "Plan", onSave = { saves++ }, label = "Title")
        }
        viewer.performClick()
        field.performTextReplacement("Plan ")
        rule.onNodeWithText("Save").performClick()
        rule.waitForIdle()
        assertEquals(0, saves)
        field.assertDoesNotExist()
    }

    @Test
    fun validationRefusesTheDraftAndKeepsTheEditorOpen() {
        var saves = 0
        rule.setMonarchContent {
            MonarchInlineEditText(
                value = "Ada",
                onSave = { saves++ },
                label = "Title",
                validate = { if (it.isEmpty()) "Enter a name." else null },
            )
        }
        viewer.performClick()
        field.performTextReplacement("")
        rule.onNodeWithText("Save").performClick()
        assertEquals(0, saves)
        assertEquals("Enter a name.", field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error))
    }

    @Test
    fun aFailedSaveKeepsTheDraftAndShowsTheMessage() {
        rule.setMonarchContent {
            MonarchInlineEditText(
                value = "Ada",
                onSave = { error("The server refused the name.") },
                label = "Title",
            )
        }
        viewer.performClick()
        field.performTextReplacement("Grace")
        rule.onNodeWithText("Save").performClick()
        rule.waitForIdle()
        assertEquals(
            "The server refused the name.",
            field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error),
        )
        assertEquals("Grace", field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text)
    }

    @Test
    fun whileSavingTheFieldIsDisabledAndSaveSaysSaving() {
        val gate = CompletableDeferred<Unit>()
        var value by mutableStateOf("Ada")
        rule.setMonarchContent {
            MonarchInlineEditText(
                value = value,
                onSave = {
                    gate.await()
                    value = it
                },
                label = "Title",
            )
        }
        viewer.performClick()
        field.performTextReplacement("Grace")
        // The progress indicator of the save button animates forever, so the clock is advanced by hand.
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Save").performClick()
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        assertEquals("Saving", rule.onNodeWithText("Save").stateDescription())
        rule.onNodeWithText("Cancel").assertIsNotEnabled()
        gate.complete(Unit)
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertEquals("Grace", viewer.stateDescription())
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun cancelAndEscapeLeaveWithoutSaving() {
        var saves = 0
        var cancels = 0
        rule.setMonarchContent {
            MonarchInlineEditText(value = "Ada", onSave = { saves++ }, label = "Title", onCancel = { cancels++ })
        }
        viewer.performClick()
        field.performTextReplacement("Grace")
        rule.onNodeWithText("Cancel").performClick()
        rule.waitForIdle()
        assertEquals("Ada", viewer.stateDescription())
        viewer.performClick()
        field.performTextReplacement("Grace")
        field.performKeyInput { pressKey(Key.Escape) }
        rule.waitForIdle()
        assertEquals(0, saves)
        assertEquals(2, cancels)
        field.assertDoesNotExist()
    }

    @Test
    fun aDisabledValueIsPlainText() {
        rule.setMonarchContent {
            MonarchInlineEditText(value = "Locked", onSave = {}, label = "Owner", enabled = false)
        }
        val text = rule.onNodeWithText("Locked").fetchSemanticsNode()
        assertNull(text.config.getOrNull(SemanticsActions.OnClick))
    }

    @Test
    fun startEditingOpensTheEditor() {
        rule.setMonarchContent {
            MonarchInlineEditText(value = "Ada", onSave = {}, label = "Name", startEditing = true)
        }
        field.assertIsFocused()
    }

    @Test
    fun builtInStringsCanBeReplacedAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchInlineEditText(
                value = "Ada",
                onSave = {},
                label = "Title",
                labels = MonarchInlineEditTextLabels(editHint = "Rename", save = "Keep", cancel = "Back"),
            )
        }
        assertEquals("Rename", viewer.fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)?.label)
        viewer.performClick()
        rule.onNodeWithText("Keep").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Back").assertExists()
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val t = MonarchInlineEditTextTokens
            assertEquals(colors.textDefault, t.viewText(empty = false, enabled = true, colors = colors), theme.id)
            assertEquals(colors.textSubtle, t.viewText(empty = true, enabled = true, colors = colors), theme.id)
            assertEquals(colors.textDisabled, t.viewText(empty = false, enabled = false, colors = colors), theme.id)
            assertEquals(colors.textMuted, t.icon(enabled = true, colors = colors), theme.id)
        }
    }

    @Test
    fun normalisingTrimsOnlyWhenAsked() {
        assertEquals("a b", MonarchInlineEditTextTokens.normalise(" a b ", trim = true))
        assertEquals(" a b ", MonarchInlineEditTextTokens.normalise(" a b ", trim = false))
    }
}
