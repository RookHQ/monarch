@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package dev.monarchui.sample.sections.layout

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
import dev.monarchui.components.layout.sectionheader.MonarchSectionHeader
import dev.monarchui.components.layout.sectionheader.MonarchSectionHeaderSize
import dev.monarchui.components.navigation.link.MonarchLink
import dev.monarchui.components.navigation.link.MonarchLinkVariant
import dev.monarchui.components.search.searchbar.MonarchSearchBar
import dev.monarchui.components.search.searchresults.MonarchSearchResult
import dev.monarchui.components.search.searchresults.MonarchSearchResultGroup
import dev.monarchui.components.search.searchresults.MonarchSearchResults
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

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
