@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rookhq.monarch.sample.sections.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.layout.sectionheader.MonarchSectionHeader
import com.rookhq.monarch.components.layout.sectionheader.MonarchSectionHeaderSize
import com.rookhq.monarch.components.navigation.link.MonarchLink
import com.rookhq.monarch.components.navigation.link.MonarchLinkVariant
import com.rookhq.monarch.components.search.searchbar.MonarchSearchBar
import com.rookhq.monarch.components.search.searchresults.MonarchSearchResult
import com.rookhq.monarch.components.search.searchresults.MonarchSearchResultGroup
import com.rookhq.monarch.components.search.searchresults.MonarchSearchResults
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

private const val PageHeightSteps = 5

/** The catalogue page of HelpCenterHome: a recipe of existing components. */
object HelpCenterHomeSection : CataloguePage {
    override val title = "HelpCenterHome"

    @Composable
    override fun Content() {
        var opened by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Help home on a phone: type to filter the articles")
        run {
            var query by rememberSaveable { mutableStateOf("") }
            val articles = listOf(
                Triple("a1", "Reset your password", "Account"),
                Triple("a2", "Change your email address", "Account"),
                Triple("a3", "Download an invoice", "Billing"),
                Triple("a4", "Update your card", "Billing"),
            )
            Column(
                modifier = Modifier.height(MonarchTheme.spacing.space24 * PageHeightSteps),
                verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
            ) {
                MonarchSectionHeader(
                    title = "Help center",
                    description = "Answers to common questions.",
                    size = MonarchSectionHeaderSize.Md,
                )
                MonarchSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = {},
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Search articles",
                )
                val words = query.lowercase().split(" ").filter { it.isNotBlank() }
                val matches = articles.filter { (_, title, category) ->
                    words.all { "$title $category".lowercase().contains(it) }
                }
                MonarchSearchResults(
                    groups = matches.groupBy { it.third }.map { (category, list) ->
                        MonarchSearchResultGroup(
                            title = category,
                            id = category,
                            results = list.map { (id, title, _) -> MonarchSearchResult(title = title, id = id) },
                        )
                    },
                    onSelect = { result, _ -> opened = result.title },
                    modifier = Modifier.weight(1f),
                    query = query,
                )
                MonarchLink(
                    text = "Still need help? Contact support",
                    onClick = { opened = "Contact support" },
                    variant = MonarchLinkVariant.Standalone,
                )
            }
        }
        if (opened.isNotEmpty()) MonarchText(text = "Would open: $opened")
    }
}
