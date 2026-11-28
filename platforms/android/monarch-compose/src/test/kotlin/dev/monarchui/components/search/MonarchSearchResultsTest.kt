package dev.monarchui.components.search

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.search.searchresults.MonarchSearchResult
import dev.monarchui.components.search.searchresults.MonarchSearchResultGroup
import dev.monarchui.components.search.searchresults.MonarchSearchResults
import dev.monarchui.components.search.searchresults.MonarchSearchResultsLabels
import dev.monarchui.components.search.searchresults.MonarchSearchResultsStatus
import dev.monarchui.components.search.searchresults.MonarchSearchResultsTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val Documents = MonarchSearchResultGroup(
    title = "Documents",
    id = "docs",
    total = 24,
    results = listOf(
        MonarchSearchResult(
            title = "Travel budget 2026",
            id = "d1",
            path = listOf("Finance", "Budgets"),
            snippet = "The travel budget covers flights and hotels.",
            meta = "PDF",
        ),
        MonarchSearchResult(title = "Budget review notes", id = "d2"),
    ),
)
private val People = MonarchSearchResultGroup(
    title = "People",
    id = "people",
    results = listOf(MonarchSearchResult(title = "Ana Lima, budget owner", id = "p1")),
)
private val Ungrouped = MonarchSearchResultGroup(
    title = null,
    id = "all",
    results = listOf(
        MonarchSearchResult(title = "Budget", id = "u1"),
        MonarchSearchResult(title = "Budgets", id = "u2"),
    ),
)

internal object MonarchSearchResultsFixtures : ComponentFixtures {
    override val id = "search-results"
    override val fixtures = listOf(
        fixture("grouped, with show all") {
            MonarchSearchResults(groups = listOf(Documents), onSelect = { _, _ -> }, query = "budget", onShowAll = {})
        },
        fixture("two groups, refreshing") {
            MonarchSearchResults(groups = listOf(People, Documents), onSelect = { _, _ -> }, loading = true)
        },
        fixture("without groups") {
            MonarchSearchResults(groups = listOf(Ungrouped), onSelect = { _, _ -> }, query = "budget")
        },
        fixture("loading") {
            MonarchSearchResults(groups = emptyList(), onSelect = { _, _ -> }, loading = true, loadingRows = 3)
        },
        fixture("empty") {
            MonarchSearchResults(groups = emptyList(), onSelect = { _, _ -> }, query = "zebra")
        },
        fixture("error with retry") {
            MonarchSearchResults(
                groups = listOf(Documents),
                onSelect = { _, _ -> },
                failed = true,
                errorMessage = "The search service did not answer.",
                onRetry = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchSearchResultsTest {
    @get:Rule
    val rule = createComposeRule()

    private val liveRegion = SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)

    private fun summary(): String? = rule.onNode(liveRegion).fetchSemanticsNode().config.let { config ->
        config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
            ?: config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text
    }

    @Test
    fun groupsHaveHeadingsWithCountsAndResultsOpen() {
        val opened = mutableListOf<Pair<String, String>>()
        rule.setMonarchContent {
            MonarchSearchResults(
                groups = listOf(Documents, People, MonarchSearchResultGroup("Empty", id = "e", results = emptyList())),
                onSelect = { result, group -> opened += result.id to group.id },
                modifier = Modifier.height(1000.dp),
                query = "budget",
            )
        }
        assertEquals("25 results for “budget”", summary())
        rule.onNodeWithText("25 results for “budget”", useUnmergedTree = true).assertExists()
        val headings = rule.onAllNodes(isHeading()).fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.ContentDescription].single() }
        assertEquals(listOf("Documents, 24 results", "People, 1 result"), headings)
        rule.onNodeWithText("Empty", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("Ana Lima, budget owner", substring = true).performClick()
        assertEquals(listOf("p1" to "people"), opened)
    }

    @Test
    fun showAllAppearsOnlyWhenAGroupHasMoreThanItShows() {
        val shown = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchSearchResults(
                groups = listOf(Documents, People),
                onSelect = { _, _ -> },
                modifier = Modifier.height(1000.dp),
                onShowAll = { shown += it.id },
            )
        }
        rule.onNodeWithText("Show all 24 in Documents").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf("docs"), shown)
        assertEquals(1, rule.onAllNodesWithText("Show all", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun theStatesFollowLoadingFailureAndResults() {
        var groups by mutableStateOf(emptyList<MonarchSearchResultGroup>())
        var loading by mutableStateOf(true)
        var failed by mutableStateOf(false)
        val retries = mutableListOf<Unit>()
        rule.setMonarchContent {
            MonarchSearchResults(
                groups = groups,
                onSelect = { _, _ -> },
                query = "  budget ",
                loading = loading,
                failed = failed,
                errorMessage = "Timed out.",
                onRetry = { retries += Unit },
            )
        }
        assertEquals("Searching…", summary())
        // The skeleton rows are hidden from TalkBack and the summary is not drawn.
        rule.onNodeWithText("Searching…", useUnmergedTree = true).assertDoesNotExist()
        loading = false
        rule.waitForIdle()
        assertEquals("No results for “budget”", summary())
        rule.onNodeWithText("No results for “budget”", useUnmergedTree = true).assertExists()
        groups = listOf(People)
        loading = true
        rule.waitForIdle()
        assertEquals("Searching…", summary())
        rule.onNodeWithText("Ana Lima, budget owner", substring = true).assertExists()
        failed = true
        rule.waitForIdle()
        assertEquals("Search failed", summary())
        rule.onNodeWithText("Timed out.", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Try again").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, retries.size)
        rule.onNodeWithText("Ana Lima, budget owner", substring = true).assertDoesNotExist()
    }

    @Test
    fun rowsHave48dpTargetsAndTheCountIsAtTheEndOfTheHeading() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchSearchResults(groups = listOf(People), onSelect = { _, _ -> }, query = "budget")
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Ana Lima, budget owner", substring = true).assertTouchTargetAtLeast48dp(rule.density)
            rule.onNodeWithText("People", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("1", useUnmergedTree = true), direction)
        }
        environment.layoutDirection = LayoutDirection.Ltr
        environment.fontScale = LargestFontScale
        rule.waitForIdle()
        rule.onNodeWithText("1 result for “budget”", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        var groups by mutableStateOf(listOf(Documents))
        rule.setMonarchContent {
            MonarchSearchResults(
                groups = groups,
                onSelect = { _, _ -> },
                modifier = Modifier.height(1000.dp),
                query = "budget",
                onShowAll = {},
                labels = MonarchSearchResultsLabels(
                    summary = { count, query -> "$count résultats pour $query" },
                    emptyTitle = { "Aucun résultat pour $it" },
                    showAll = { group, total -> "Voir les $total dans $group" },
                    groupCount = { "($it)" },
                    groupName = { group, total -> "$group : $total" },
                ),
            )
        }
        assertEquals("24 résultats pour budget", summary())
        rule.onNodeWithText("Voir les 24 dans Documents").assertExists()
        rule.onNodeWithText("(24)", useUnmergedTree = true).assertExists()
        rule.onNode(isHeading() and hasContentDescription("Documents : 24")).assertExists()
        groups = emptyList()
        rule.waitForIdle()
        assertEquals("Aucun résultat pour budget", summary())
    }

    @Test
    fun theLogicAndColoursComeFromTheTheme() {
        val t = MonarchSearchResultsTokens
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, t.mutedColor(colors))
            assertEquals(colors.textDefault, t.headingColor(colors))
        }
        assertEquals(MonarchSearchResultsStatus.Error, t.status(failed = true, hasResults = true, loading = true))
        assertEquals(MonarchSearchResultsStatus.Refreshing, t.status(failed = false, hasResults = true, loading = true))
        assertEquals(MonarchSearchResultsStatus.Ready, t.status(failed = false, hasResults = true, loading = false))
        assertEquals(MonarchSearchResultsStatus.Loading, t.status(failed = false, hasResults = false, loading = true))
        assertEquals(MonarchSearchResultsStatus.Empty, t.status(failed = false, hasResults = false, loading = false))
        assertEquals(2, t.groupTotal(MonarchSearchResultGroup("A", "a", Documents.results, total = 1)))
        assertEquals(25, t.total(listOf(Documents, People)))
        assertTrue(t.showsAll(Documents, canShowAll = true))
        assertFalse(t.showsAll(Documents, canShowAll = false))
        assertFalse(t.showsAll(People, canShowAll = true))
        assertEquals("", t.shownQuery("   "))
        assertEquals("1,234", t.count(1234, Locale.US))
        assertEquals("1.234", t.count(1234, Locale.GERMANY))
    }
}
