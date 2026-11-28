package com.rookhq.monarch.sample.sections.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.search.searchresultitem.MonarchSearchResultItem
import com.rookhq.monarch.components.search.searchresultitem.monarchQueryTerms
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

/** The catalogue page of SearchResultItem: results that highlight what is typed in the field. */
object MonarchSearchResultItemSection : CataloguePage {
    override val title = "SearchResultItem"

    @Composable
    override fun Content() {
        var query by rememberSaveable { mutableStateOf("travel") }
        var opened by rememberSaveable { mutableStateOf("") }
        val terms = monarchQueryTerms(query)
        CatalogueCaption("Type to change the highlighted words")
        MonarchTextField(value = query, onValueChange = { query = it }, label = "Search")
        MonarchSearchResultItem(
            title = "Travel policy",
            path = listOf("Handbook", "Finance"),
            snippet = "Book travel at least two weeks ahead. Keep receipts for every expense.",
            terms = terms,
            onClick = { opened = "Travel policy" },
            meta = { MonarchBadge(text = "Page") },
        )
        MonarchSearchResultItem(
            title = "Q3 travel budget.xlsx",
            fileName = "Q3 travel budget.xlsx",
            path = listOf("Drive", "Finance"),
            terms = terms,
            onClick = { opened = "Q3 travel budget.xlsx" },
        )
        MonarchSearchResultItem(
            title = "Expense reports",
            snippet = "Submit <mark>receipts</mark> within 30 days.",
            snippetMarkTags = true,
            icon = { MonarchIcon(imageVector = SampleIcons.Document, contentDescription = null) },
        )
        if (opened.isNotEmpty()) MonarchText(text = "Opened: $opened")
        CatalogueCaption("Disabled")
        MonarchSearchResultItem(title = "Archived page", path = listOf("Archive"), enabled = false, onClick = {})
    }
}
