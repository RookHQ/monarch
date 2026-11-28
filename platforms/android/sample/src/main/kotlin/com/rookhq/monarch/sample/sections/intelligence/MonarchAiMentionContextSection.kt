package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.collaboration.mentionpicker.MonarchMentionPickerStatus
import com.rookhq.monarch.components.collaboration.mentionpicker.MonarchMentionQuery
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.intelligence.aicontextchips.MonarchAiContextChips
import com.rookhq.monarch.components.intelligence.aicontextchips.MonarchAiContextItem
import com.rookhq.monarch.components.intelligence.aimentioncontext.MonarchAiMentionContext
import com.rookhq.monarch.components.intelligence.aimentioncontext.MonarchAiMentionItem
import com.rookhq.monarch.components.intelligence.aimentioncontext.MonarchAiMentionKind
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private val SampleResults = listOf(
    MonarchAiMentionItem("doc-1", MonarchAiMentionKind.Doc, "Q3 report.pdf", "Finance", tokens = 12_400),
    MonarchAiMentionItem("doc-2", MonarchAiMentionKind.Doc, "Board deck", "Shared drive", tokens = 3_100),
    MonarchAiMentionItem("user-1", MonarchAiMentionKind.Person, "Ana Lopez", "Finance lead"),
    MonarchAiMentionItem("deal-1", MonarchAiMentionKind.Record, "Acme renewal", "Deal", tokens = 800),
    MonarchAiMentionItem("hr-1", MonarchAiMentionKind.Doc, "Salaries.xlsx", "You cannot share this", enabled = false),
)

/** The catalogue page of AiMentionContext. */
object MonarchAiMentionContextSection : CataloguePage {
    override val title = "AiMentionContext"

    @Composable
    override fun Content() {
        var prompt by rememberSaveable { mutableStateOf("Summarize @") }
        var context by remember { mutableStateOf(listOf<MonarchAiContextItem>()) }
        val query = MonarchMentionQuery.find(prompt)
        CatalogueCaption("Type after @ to filter; choose a result to add it")
        MonarchAiContextChips(items = context, onRemove = { id -> context = context.filterNot { it.id == id } })
        MonarchAiMentionContext(
            query = query,
            items = SampleResults.filter {
                it.id !in context.map { chip -> chip.id } &&
                    it.label.contains(query?.text.orEmpty(), ignoreCase = true)
            },
            onSelect = { item ->
                context = context + item.contextItem
                prompt = query?.replacing(prompt, "") ?: prompt
            },
        )
        MonarchTextField(value = prompt, onValueChange = { prompt = it }, label = "Prompt")
        CatalogueCaption("Searching and failed")
        MonarchAiMentionContext(
            query = MonarchMentionQuery.find("@q"),
            items = emptyList(),
            onSelect = {},
            status = MonarchMentionPickerStatus.Loading,
        )
        MonarchAiMentionContext(
            query = MonarchMentionQuery.find("@q"),
            items = emptyList(),
            onSelect = {},
            status = MonarchMentionPickerStatus.Failed,
            onRetry = {},
        )
    }
}
