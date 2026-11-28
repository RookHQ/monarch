package dev.monarchui.sample.sections.search

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.components.search.searchresults.MonarchSearchResult
import dev.monarchui.components.search.searchresults.MonarchSearchResultGroup
import dev.monarchui.components.search.searchresults.MonarchSearchResults
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

private const val ListHeightSteps = 4
private val States = listOf("Results", "Loading", "Empty", "Error")

private val Groups = listOf(
    MonarchSearchResultGroup(
        title = "Documents",
        id = "docs",
        total = 24,
        results = listOf(
            MonarchSearchResult(
                title = "Travel budget 2026",
                id = "d1",
                path = listOf("Finance", "Budgets"),
                snippet = "The travel budget covers flights, hotels and meals.",
                meta = "Edited 2 days ago",
            ),
            MonarchSearchResult(title = "Budget review notes", id = "d2", fileName = "review.pdf"),
        ),
    ),
    MonarchSearchResultGroup(
        title = "People",
        id = "people",
        results = listOf(MonarchSearchResult(title = "Ana Lima", id = "p1", snippet = "Budget owner, Finance")),
    ),
)

/** The catalogue page of SearchResults: grouped results and the loading, empty and error states. */
object MonarchSearchResultsSection : CataloguePage {
    override val title = "SearchResults"

    @Composable
    override fun Content() {
        var state by rememberSaveable { mutableIntStateOf(0) }
        var opened by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("State")
        MonarchSegmentedControl(modifier = Modifier.fillMaxWidth()) {
            States.forEachIndexed { index, label ->
                Segment(
                    selected = state == index,
                    onClick = { state = index },
                    label = { Text(label) },
                )
            }
        }
        MonarchSearchResults(
            groups = if (state == 0) Groups else emptyList(),
            onSelect = { result, _ -> opened = result.title },
            modifier = Modifier.height(MonarchTheme.spacing.space24 * ListHeightSteps),
            query = "budget",
            loading = state == 1,
            failed = state == 3,
            errorMessage = "The search service did not answer.",
            onRetry = { state = 0 },
            onShowAll = { opened = "All ${it.title}" },
        )
        if (opened.isNotEmpty()) MonarchText(text = "Opened: $opened")
    }
}
