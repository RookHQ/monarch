package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.intelligence.aiprompttemplatepicker.MonarchAiPromptTemplate
import com.rookhq.monarch.components.intelligence.aiprompttemplatepicker.MonarchAiPromptTemplateCategory
import com.rookhq.monarch.components.intelligence.aiprompttemplatepicker.MonarchAiPromptTemplatePicker
import com.rookhq.monarch.components.intelligence.aiprompttemplatepicker.MonarchAiPromptTemplateVariable
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private val Templates = listOf(
    MonarchAiPromptTemplate(
        id = "reply",
        title = "Reply to a customer",
        body = "Write a friendly reply to {{customer_name}} about {{topic}}.{{notes}}",
        description = "A short reply in our support tone.",
        category = "support",
        keywords = listOf("email"),
        variables = listOf(
            MonarchAiPromptTemplateVariable("customer_name", label = "Customer name", required = true),
            MonarchAiPromptTemplateVariable("topic", label = "Topic", required = true, placeholder = "Refund"),
            MonarchAiPromptTemplateVariable("notes", label = "Notes", multiline = true),
        ),
    ),
    MonarchAiPromptTemplate(
        id = "summary",
        title = "Summarise a thread",
        body = "Summarise this thread in three bullet points.",
        category = "writing",
    ),
)

private val Categories = listOf(
    MonarchAiPromptTemplateCategory("support", "Support"),
    MonarchAiPromptTemplateCategory("writing", "Writing"),
)

/** The catalogue page of AiPromptTemplatePicker. */
object MonarchAiPromptTemplatePickerSection : CataloguePage {
    override val title = "AiPromptTemplatePicker"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var prompt by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("In a bottom sheet; the filled prompt goes to the composer")
        MonarchButton(text = "Choose a template", onClick = { open = true })
        if (prompt.isNotEmpty()) MonarchText(text = "Prompt: $prompt")
        if (open) {
            MonarchBottomSheet(onDismissRequest = { open = false }) {
                MonarchAiPromptTemplatePicker(
                    templates = Templates,
                    onUse = { text, _, _ ->
                        prompt = text
                        dismiss()
                    },
                    categories = Categories,
                    onCancel = { dismiss() },
                )
            }
        }
    }
}
