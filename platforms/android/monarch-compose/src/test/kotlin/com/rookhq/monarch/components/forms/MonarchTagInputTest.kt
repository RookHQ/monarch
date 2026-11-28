package com.rookhq.monarch.components.forms

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
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.forms.taginput.MonarchTagCommit
import com.rookhq.monarch.components.forms.taginput.MonarchTagInput
import com.rookhq.monarch.components.forms.taginput.MonarchTagInputEdit
import com.rookhq.monarch.components.forms.taginput.MonarchTagInputLabels
import com.rookhq.monarch.components.forms.taginput.MonarchTagInputLogic
import com.rookhq.monarch.components.forms.taginput.MonarchTagInputTokens
import com.rookhq.monarch.components.forms.taginput.MonarchTagRejection
import com.rookhq.monarch.components.forms.taginput.MonarchTagRules
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

private val Close: @androidx.compose.runtime.Composable () -> Unit = {
    MonarchIcon(imageVector = TestIcon, contentDescription = null, size = MonarchIconSize.Sm)
}

internal object MonarchTagInputFixtures : ComponentFixtures {
    override val id = "tag-input"
    override val fixtures = listOf(
        fixture("empty, with a placeholder and help") {
            MonarchTagInput(
                tags = emptyList(),
                onTagsChange = {},
                label = "Skills",
                removeIcon = Close,
                placeholder = "Add a skill",
                helperText = "Separate skills with commas.",
            )
        },
        fixture("with tags") {
            MonarchTagInput(
                tags = listOf("Research", "TypeScript"),
                onTagsChange = {},
                label = "Skills",
                removeIcon = Close,
            )
        },
        fixture("with an error and an icon") {
            MonarchTagInput(
                tags = listOf("ana"),
                onTagsChange = {},
                label = "Recipients",
                removeIcon = Close,
                errorText = "Enter an email address.",
                errorIcon = Close,
            )
        },
        fixture("read only") {
            MonarchTagInput(
                tags = listOf("Billing", "Priority"),
                onTagsChange = {},
                label = "Labels",
                removeIcon = Close,
                readOnly = true,
            )
        },
        fixture("disabled") {
            MonarchTagInput(
                tags = listOf("Billing"),
                onTagsChange = {},
                label = "Labels",
                removeIcon = Close,
                enabled = false,
            )
        },
        fixture("a long tag") {
            MonarchTagInput(
                tags = listOf("Accessibility reviews of every release candidate"),
                onTagsChange = {},
                label = "Skills",
                removeIcon = Close,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchTagInputTest {
    @get:Rule
    val rule = createComposeRule()

    private val logic = MonarchTagInputLogic
    private val field get() = rule.onNode(hasSetTextAction())

    @Test
    fun typingASeparatorAddsWhatComesBeforeItAndAPasteAddsEveryPart() {
        val comma = setOf(',')
        assertEquals(MonarchTagInputEdit(emptyList(), "Res"), logic.input("Re", "Res", comma))
        assertEquals(MonarchTagInputEdit(listOf("Research"), ""), logic.input("Research", "Research,", comma))
        assertEquals(MonarchTagInputEdit(listOf("a", " b", " c"), ""), logic.input("", "a, b, c", comma))
        assertEquals(MonarchTagInputEdit(listOf("a", "b"), ""), logic.input("", "a\nb", comma))
        assertEquals(listOf("a", "b", "c"), logic.split("a;b c", setOf(';', ' ')))
    }

    @Test
    fun commitRefusesDuplicatesWithoutCaseTheMaximumAndInvalidTags() {
        val validate: (String, List<String>) -> String? = { tag, _ -> if ('@' in tag) null else "Not an email." }
        assertEquals(
            MonarchTagCommit(
                listOf("Research", "Design"),
                listOf("Design"),
                listOf("research"),
                MonarchTagRejection.Duplicate("research"),
            ),
            logic.commit(listOf("research", " Design ", "  "), listOf("Research"), MonarchTagRules()),
        )
        assertEquals(
            MonarchTagCommit(listOf("a"), emptyList(), listOf("b"), MonarchTagRejection.Maximum(1)),
            logic.commit(listOf("b"), listOf("a"), MonarchTagRules(maxCount = 1)),
        )
        assertEquals(
            MonarchTagRejection.Invalid("Not an email."),
            logic.commit(listOf("ana"), emptyList(), MonarchTagRules(validate = validate)).rejection,
        )
        assertEquals(
            listOf("a", "A"),
            logic.commit(listOf("A"), listOf("a"), MonarchTagRules(allowDuplicates = true)).tags,
        )
        assertEquals(
            listOf("ana@x.io"),
            logic.commit(
                listOf(" ANA@x.io "),
                emptyList(),
                MonarchTagRules(normalize = { it.trim().lowercase() }),
            ).tags,
        )
    }

    @Test
    fun suggestionsMatchTheTypedTextAndLeaveOutAddedOnes() {
        val all = listOf("TypeScript", "Research", "Type theory", "Accessibility")
        assertEquals(listOf("Type theory"), logic.suggestions(all, "ty", listOf("typescript", "Accessibility")))
        assertEquals(emptyList(), logic.suggestions(all, " ", emptyList()))
        assertEquals(
            MonarchTagInputLogic.MAX_SUGGESTIONS,
            logic.suggestions(List(12) { "Tag $it" }, "tag", emptyList()).size,
        )
    }

    @Test
    fun aCommaAndDoneAddTagsAndTheAdditionIsAnnounced() {
        var tags by mutableStateOf(listOf<String>())
        rule.setMonarchContent {
            MonarchTagInput(
                tags = tags,
                onTagsChange = { tags = it },
                label = "Skills",
                removeIcon = Close,
                modifier = Modifier.testTag("input"),
            )
        }
        field.assert(hasContentDescription("Skills"))
        field.performTextInput("Research")
        field.performTextInput(",")
        assertEquals(listOf("Research"), tags)
        field.performTextInput("Design")
        field.performImeAction()
        assertEquals(listOf("Research", "Design"), tags)
        assertEquals("Added Design.", liveRegion())
    }

    @Test
    fun aRefusedTagStaysInTheFieldWithTheErrorAndClearsWhenTyping() {
        var tags by mutableStateOf(listOf("Research"))
        rule.setMonarchContent {
            MonarchTagInput(
                tags = tags,
                onTagsChange = { tags = it },
                label = "Skills",
                removeIcon = Close,
                modifier = Modifier.testTag("input"),
            )
        }
        field.performTextInput("research")
        field.performImeAction()
        assertEquals(listOf("Research"), tags)
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "research is already added."))
        assertEquals("research is already added.", liveRegion())
        assertEquals("research", field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        field.performTextInput("!")
        field.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
    }

    @Test
    fun removingATagCallsBackAndIsAnnounced() {
        var tags by mutableStateOf(listOf("Research", "Design"))
        rule.setMonarchContent {
            MonarchTagInput(
                tags = tags,
                onTagsChange = { tags = it },
                label = "Skills",
                removeIcon = Close,
                modifier = Modifier.testTag("input"),
                labels = MonarchTagInputLabels(removed = { "Retiré : $it" }),
            )
        }
        rule.onNodeWithContentDescription("Remove Research").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf("Design"), tags)
        assertEquals("Retiré : Research", liveRegion())
    }

    @Test
    fun suggestionsAreButtonsThatAddTheValue() {
        var tags by mutableStateOf(listOf<String>())
        rule.setMonarchContent {
            MonarchTagInput(
                tags = tags,
                onTagsChange = { tags = it },
                label = "Skills",
                removeIcon = Close,
                suggestions = listOf("TypeScript", "Research"),
            )
        }
        rule.onNodeWithContentDescription("Add TypeScript").assertDoesNotExist()
        field.performTextInput("type")
        rule.onNodeWithContentDescription("Add Research").assertDoesNotExist()
        rule.onNodeWithContentDescription("Add TypeScript").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf("TypeScript"), tags)
        assertEquals("", field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
    }

    @Test
    fun readOnlyHasNoFieldAndDisabledCannotRemove() {
        rule.setMonarchContent {
            androidx.compose.foundation.layout.Column {
                MonarchTagInput(
                    tags = listOf("Billing"),
                    onTagsChange = {},
                    label = "Labels",
                    removeIcon = Close,
                    readOnly = true,
                )
                MonarchTagInput(
                    tags = listOf("Priority"),
                    onTagsChange = {},
                    label = "Other",
                    removeIcon = Close,
                    enabled = false,
                )
            }
        }
        rule.onNodeWithText("Billing").assertHasNoClickAction()
        rule.onNodeWithText("Priority").assertHasNoClickAction()
        rule.onNode(hasSetTextAction() and hasContentDescription("Labels")).assertDoesNotExist()
        rule.onNode(hasContentDescription("Other") and SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText))
            .assertIsNotEnabled()
    }

    @Test
    fun tagsStartAtTheStartEdgeAndHelpWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale, layoutDirection = LayoutDirection.Rtl) {
            MonarchTagInput(
                tags = listOf("One", "Two"),
                onTagsChange = {},
                label = "Skills",
                removeIcon = Close,
                helperText = "Separate skills with commas.",
            )
        }
        rule.onNodeWithText("Separate skills with commas.").assertTextFullyVisible()
        rule.onNodeWithContentDescription("Remove One")
            .assertIsBefore(rule.onNodeWithContentDescription("Remove Two"), LayoutDirection.Rtl)
        field.assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun boxColoursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchTagInputTokens
            val idle = tokens.boxColors(
                enabled = true,
                readOnly = false,
                focused = false,
                error = false,
                colors = colors,
            )
            assertEquals(colors.bgSurface, idle.container, theme.id)
            assertEquals(colors.borderInput, idle.border, theme.id)
            assertEquals(colors.focusRing, tokens.boxColors(true, false, true, false, colors).border, theme.id)
            assertEquals(colors.dangerStrong, tokens.boxColors(true, false, true, true, colors).border, theme.id)
            val disabled = tokens.boxColors(false, false, false, true, colors)
            assertEquals(colors.bgSubtle, disabled.container, theme.id)
            assertEquals(colors.textDisabled, disabled.content, theme.id)
            assertEquals(colors.borderDefault, tokens.boxColors(true, true, false, false, colors).border, theme.id)
            assertEquals(colors.dangerStrong, tokens.errorColor(colors), theme.id)
            assertEquals(colors.accentStrong, tokens.cursorColor(colors), theme.id)
        }
    }

    private fun liveRegion(): String? {
        val node = rule.onNodeWithTag("input").fetchSemanticsNode().children.single {
            it.config.getOrNull(SemanticsProperties.LiveRegion) == LiveRegionMode.Polite
        }
        return node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
    }
}
