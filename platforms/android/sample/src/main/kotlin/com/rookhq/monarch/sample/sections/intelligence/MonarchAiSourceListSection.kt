package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiSourceKind
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiSourcePart
import com.rookhq.monarch.components.intelligence.aisourcelist.MonarchAiSourceList
import com.rookhq.monarch.components.intelligence.aisourcelist.MonarchAiSourceListItem
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private val Sources = listOf(
    MonarchAiSourceListItem(
        MonarchAiSourcePart("s1", "Q3 revenue report", "https://example.com/q3", MonarchAiSourceKind.Doc),
        snippet = "Revenue grew 12% over the quarter, led by the Payments team.",
    ),
    MonarchAiSourceListItem(
        MonarchAiSourcePart("s2", "Acme Corp", kind = MonarchAiSourceKind.Record, product = "CRM"),
    ),
    MonarchAiSourceListItem(
        MonarchAiSourcePart("s3", "Pricing", "https://example.com/pricing", MonarchAiSourceKind.Web),
    ),
    MonarchAiSourceListItem(
        MonarchAiSourcePart("s4", "Old forecast.xlsx", kind = MonarchAiSourceKind.File),
        used = false,
    ),
)

/** The catalogue page of AiSourceList. */
object MonarchAiSourceListSection : CataloguePage {
    override val title = "AiSourceList"

    @Composable
    override fun Content() {
        var opened by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Used and considered sources, collapsed to three; each opens a preview")
        MonarchAiSourceList(items = Sources, onOpen = { opened = it.source.title })
        if (opened.isNotEmpty()) MonarchText(text = "Opened: $opened")
        CatalogueCaption("Links, without numbers")
        MonarchAiSourceList(items = Sources.take(2), numbered = false)
    }
}
