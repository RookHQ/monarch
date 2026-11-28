package com.rookhq.monarch.components.intelligence.aiprompttemplatepicker

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val PreviewTemplates = listOf(
    MonarchAiPromptTemplate(
        id = "reply",
        title = "Reply to a customer",
        body = "Write a reply to {{customer_name}} about {{topic}}.",
        description = "A short, friendly reply.",
        category = "support",
    ),
    MonarchAiPromptTemplate(id = "summary", title = "Summarise a thread", body = "Summarise this thread in 3 bullets."),
)

/** The picker and the preview scroll themselves, so the scrolling preview frame must bound them. */
private val PreviewHeight = 400.dp

@MonarchPreviews
@Composable
internal fun MonarchAiPromptTemplatePickerPreview() {
    PreviewFrame {
        MonarchAiPromptTemplatePicker(
            modifier = Modifier.heightIn(max = PreviewHeight),
            templates = PreviewTemplates,
            onUse = { _, _, _ -> },
            categories = listOf(MonarchAiPromptTemplateCategory("support", "Support")),
            onCancel = {},
        )
        MonarchAiPromptTemplatePicker(
            templates = PreviewTemplates,
            onUse = { _, _, _ -> },
            modifier = Modifier.heightIn(max = PreviewHeight),
            initialTemplateId = "reply",
        )
    }
}
