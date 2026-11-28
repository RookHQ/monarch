package com.rookhq.monarch.components.intelligence

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.intelligence.aiprompttemplatepicker.MonarchAiPromptTemplate
import com.rookhq.monarch.components.intelligence.aiprompttemplatepicker.MonarchAiPromptTemplateCategory
import com.rookhq.monarch.components.intelligence.aiprompttemplatepicker.MonarchAiPromptTemplatePicker
import com.rookhq.monarch.components.intelligence.aiprompttemplatepicker.MonarchAiPromptTemplatePickerLabels
import com.rookhq.monarch.components.intelligence.aiprompttemplatepicker.MonarchAiPromptTemplatePickerTokens
import com.rookhq.monarch.components.intelligence.aiprompttemplatepicker.MonarchAiPromptTemplateVariable
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val Reply = MonarchAiPromptTemplate(
    id = "reply",
    title = "Reply",
    body = "Write to {{customer_name}} about {{ topic }}.",
    description = "Friendly",
    category = "support",
    keywords = listOf("email"),
)

private val Summary = MonarchAiPromptTemplate(id = "summary", title = "Summarise", body = "Summarise in 3 bullets.")

private val Optional = MonarchAiPromptTemplate(
    id = "optional",
    title = "Draft",
    body = "Draft about {{topic}}{{extra}}",
    variables = listOf(
        MonarchAiPromptTemplateVariable("topic", label = "Topic", required = true),
        MonarchAiPromptTemplateVariable("extra", label = "Extra", multiline = true),
    ),
)

internal object MonarchAiPromptTemplatePickerFixtures : ComponentFixtures {
    override val id = "ai-prompt-template-picker"
    override val fixtures = listOf(
        fixture("gallery") { MonarchAiPromptTemplatePicker(templates = listOf(Summary), onUse = { _, _, _ -> }) },
        fixture("gallery with categories and cancel") {
            MonarchAiPromptTemplatePicker(
                templates = listOf(Reply),
                onUse = { _, _, _ -> },
                categories = listOf(MonarchAiPromptTemplateCategory("support", "Support")),
                onCancel = {},
            )
        },
        fixture("empty") { MonarchAiPromptTemplatePicker(templates = emptyList(), onUse = { _, _, _ -> }) },
        fixture("template without blanks") {
            MonarchAiPromptTemplatePicker(
                templates = listOf(Summary),
                onUse = { _, _, _ -> },
                initialTemplateId = "summary",
            )
        },
        fixture("template with a blank") {
            MonarchAiPromptTemplatePicker(
                templates = listOf(Summary.copy(body = "Sum up {{topic}}")),
                onUse = { _, _, _ -> },
                initialTemplateId = "summary",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiPromptTemplatePickerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachTemplateIsOneButtonThatIsChosenThenOpened() {
        rule.setMonarchContent {
            MonarchAiPromptTemplatePicker(templates = listOf(Reply, Summary), onUse = { _, _, _ -> })
        }
        rule.onNodeWithText("Prompt templates").assert(isHeading())
        rule.onNode(hasText("Reply") and hasText("Friendly"))
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithText("Continue").performClick()
        rule.onNode(hasText("Reply") and isHeading()).assertExists()
        rule.onNodeWithText("All templates").performClick()
        rule.onNodeWithText("Prompt templates").assertExists()
    }

    @Test
    fun searchIgnoresCaseAndAccentsAndCanBeCleared() {
        rule.setMonarchContent {
            MonarchAiPromptTemplatePicker(templates = listOf(Reply, Summary), onUse = { _, _, _ -> })
        }
        rule.onNode(hasSetTextAction()).performTextInput("ÉMAIL")
        rule.onNodeWithText("Summarise").assertDoesNotExist()
        rule.onNodeWithText("Reply").assertExists()
        rule.onNode(hasSetTextAction()).performTextInput("zzz")
        rule.onNodeWithText("No templates match “ÉMAILzzz”").assertExists()
        rule.onNodeWithText("Clear filters").performClick()
        rule.onNodeWithText("Summarise").assertExists()
    }

    @Test
    fun categoriesFilterTheList() {
        rule.setMonarchContent {
            MonarchAiPromptTemplatePicker(
                templates = listOf(Reply, Summary),
                onUse = { _, _, _ -> },
                categories = listOf(MonarchAiPromptTemplateCategory("support", "Support")),
            )
        }
        rule.onNodeWithText("Support").performClick()
        rule.onNodeWithText("Summarise").assertDoesNotExist()
        rule.onNodeWithText("All").performClick()
        rule.onNodeWithText("Summarise").assertExists()
    }

    @Test
    fun noTemplatesShowsTheFirstRunState() {
        rule.setMonarchContent { MonarchAiPromptTemplatePicker(templates = emptyList(), onUse = { _, _, _ -> }) }
        rule.onNodeWithText("No templates yet").assertExists()
    }

    @Test
    fun theBlanksAreFieldsWithALivePreview() {
        rule.setMonarchContent {
            MonarchAiPromptTemplatePicker(
                templates = listOf(Reply),
                onUse = { _, _, _ -> },
                initialTemplateId = "reply",
            )
        }
        rule.onNodeWithText("Write to [Customer name] about [Topic].").assertExists()
        rule.onNode(hasSetTextAction() and hasContentDescription("Customer name")).performTextInput("  Ada ")
        rule.onNodeWithText("Write to Ada about [Topic].").assertExists()
        rule.onNodeWithText("Prompt preview").assert(isHeading())
    }

    @Test
    fun useTemplateAsksForEmptyRequiredFieldsAndAnnouncesTheFirst() {
        var used: String? = null
        rule.setMonarchContent {
            MonarchAiPromptTemplatePicker(
                templates = listOf(Reply),
                onUse = { text, _, _ -> used = text },
                initialTemplateId = "reply",
            )
        }
        rule.onNodeWithText("Use template").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(null, used)
        val errors = rule.onAllNodesWithText("Enter customer name.", substring = true, useUnmergedTree = true)
        assertTrue(errors.fetchSemanticsNodes().isNotEmpty())
        // The field's own polite region holds the error too; the first missing field is announced assertively.
        rule.onNode(
            hasContentDescription("Enter customer name.") and
                SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive),
        ).assertExists()
    }

    @Test
    fun useTemplateHandsTheFilledPromptAndTrimmedValues() {
        var result: Triple<String, String, Map<String, String>>? = null
        rule.setMonarchContent {
            MonarchAiPromptTemplatePicker(
                templates = listOf(Optional),
                onUse = { text, template, values -> result = Triple(text, template.id, values) },
                initialTemplateId = "optional",
            )
        }
        rule.onNode(hasSetTextAction() and hasContentDescription("Topic")).performTextInput(" pricing ")
        rule.onNode(hasContentDescription("Extra, Optional")).assertExists()
        rule.onNodeWithText("Use template").performClick()
        assertEquals(Triple("Draft about pricing", "optional", mapOf("topic" to "pricing", "extra" to "")), result)
    }

    @Test
    fun aTemplateWithoutBlanksShowsItsText() {
        rule.setMonarchContent {
            MonarchAiPromptTemplatePicker(
                templates = listOf(Summary),
                onUse = { _, _, _ -> },
                initialTemplateId = "summary",
            )
        }
        rule.onNodeWithText("Template").assert(isHeading())
        rule.onNodeWithText("Summarise in 3 bullets.").assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiPromptTemplatePicker(
                templates = emptyList(),
                onUse = { _, _, _ -> },
                labels = MonarchAiPromptTemplatePickerLabels(
                    title = "Modèles",
                    empty = "Aucun modèle",
                    cancel = "Annuler",
                ),
                onCancel = {},
            )
        }
        rule.onNodeWithText("Modèles").assertExists()
        rule.onNodeWithText("Aucun modèle").assertExists()
        rule.onNodeWithText("Annuler").assertExists()
    }

    @Test
    fun cancelIsAtTheEndInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiPromptTemplatePicker(templates = listOf(Summary), onUse = { _, _, _ -> }, onCancel = {})
        }
        rule.onNodeWithText("Prompt templates").assertIsBefore(rule.onNodeWithText("Cancel"), LayoutDirection.Rtl)
    }

    @Test
    fun titlesWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiPromptTemplatePicker(templates = listOf(Summary), onUse = { _, _, _ -> })
        }
        rule.onNodeWithText("Prompt templates").assertTextFullyVisible()
    }

    @Test
    fun blanksAreReadFilledAndLabelled() {
        assertEquals(listOf("customer_name", "topic"), MonarchAiPromptTemplate.variableNames(Reply.body + "{{topic}}"))
        assertEquals(
            "Write to Ada about {{ topic }}.",
            MonarchAiPromptTemplate.fill(Reply.body, mapOf("customer_name" to "Ada")),
        )
        assertEquals("Draft about x", MonarchAiPromptTemplate.fill(Optional.body, mapOf("topic" to "x", "extra" to "")))
        assertEquals("Customer name", MonarchAiPromptTemplatePickerTokens.humanize("customer_name"))
        assertEquals("Customer name", MonarchAiPromptTemplatePickerTokens.humanize("customerName"))
        assertTrue(Reply.resolvedVariables.all { it.required })
        val missing = MonarchAiPromptTemplatePickerTokens.missing(Optional.resolvedVariables, mapOf("topic" to " "))
        assertEquals(listOf("topic"), missing)
        assertTrue(MonarchAiPromptTemplatePickerTokens.matches(Reply, "  frIEND "))
        assertFalse(MonarchAiPromptTemplatePickerTokens.matches(Summary, "email"))
    }

    @Test
    fun theBlocksUseTheSubtleRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val block = MonarchAiPromptTemplatePickerTokens.blockColors(c)
            assertEquals(c.bgSubtle, block.container, theme.id)
            assertEquals(c.borderSubtle, block.border, theme.id)
            assertEquals(c.textMuted, MonarchAiPromptTemplatePickerTokens.headingColor(c), theme.id)
        }
    }
}
