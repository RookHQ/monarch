package com.rookhq.monarch.components.datagrid

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rookhq.monarch.components.datagrid.loadmore.LoadMoreState
import com.rookhq.monarch.components.datagrid.loadmore.MonarchLoadMore
import com.rookhq.monarch.components.datagrid.loadmore.MonarchLoadMoreLabels
import com.rookhq.monarch.components.datagrid.loadmore.MonarchLoadMoreTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

internal object MonarchLoadMoreFixtures : ComponentFixtures {
    override val id = "load-more"
    override val fixtures = listOf(
        fixture("idle with a count") {
            MonarchLoadMore(hasMore = true, onLoadMore = {}, loadedCount = 40, totalCount = 120)
        },
        fixture("loading") { MonarchLoadMore(hasMore = true, onLoadMore = {}, loading = true, loadedCount = 40) },
        fixture("failed") { MonarchLoadMore(hasMore = true, onLoadMore = {}, failed = true) },
        fixture("end and end hidden") {
            Column {
                MonarchLoadMore(hasMore = false, onLoadMore = {}, loadedCount = 1, totalCount = 1)
                MonarchLoadMore(hasMore = false, onLoadMore = {}, showEnd = false)
            }
        },
        fixture("long text") {
            MonarchLoadMore(
                hasMore = true,
                onLoadMore = {},
                failed = true,
                labels = MonarchLoadMoreLabels(
                    failed = "The next page of invoices could not be loaded from the server.",
                ),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchLoadMoreTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theButtonLoadsTheNextPageAndHasAFullTarget() {
        var loads = 0
        rule.setMonarchContent { MonarchLoadMore(hasMore = true, onLoadMore = { loads++ }) }
        rule.onNodeWithText("Load more").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, loads)
    }

    @Test
    fun whileLoadingTheButtonIgnoresTapsAndSaysLoading() {
        var loads = 0
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchLoadMore(hasMore = true, onLoadMore = { loads++ }, loading = true) }
        rule.mainClock.advanceTimeByFrame()
        val button = rule.onNodeWithText("Load more")
        assertEquals("Loading", button.stateDescription())
        button.performClick()
        assertEquals(0, loads)
    }

    @Test
    fun afterAFailureTheButtonRetriesWithOnRetry() {
        var loads = 0
        var retries = 0
        rule.setMonarchContent {
            MonarchLoadMore(hasMore = true, onLoadMore = { loads++ }, failed = true, onRetry = { retries++ })
        }
        rule.onNodeWithText("Couldn't load more.").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(0, loads)
        assertEquals(1, retries)
    }

    @Test
    fun theCountAndTheEndAreShown() {
        rule.setMonarchContent {
            Column {
                MonarchLoadMore(hasMore = true, onLoadMore = {}, loadedCount = 40, totalCount = 120)
                MonarchLoadMore(hasMore = true, onLoadMore = {}, loadedCount = 1)
                MonarchLoadMore(hasMore = false, onLoadMore = {}, loadedCount = 7, showCount = false)
            }
        }
        rule.onNodeWithText("Showing 40 of 120 items").assertExists()
        rule.onNodeWithText("Showing 1 item").assertExists()
        rule.onNodeWithText("Showing 7 items").assertDoesNotExist()
        rule.onNodeWithText("That's everything").assertExists()
    }

    @Test
    fun aGrowingCountAFailureAndTheEndAreAnnouncedPolitely() {
        var count by mutableIntStateOf(20)
        var hasMore by mutableStateOf(true)
        var failed by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchLoadMore(hasMore = hasMore, onLoadMore = {}, failed = failed, loadedCount = count)
        }
        rule.onNode(polite and hasContentDescription("loaded", substring = true)).assertDoesNotExist()
        count = 40
        rule.waitForIdle()
        rule.onNode(polite and hasContentDescription("20 more items loaded.")).assertExists()
        failed = true
        rule.waitForIdle()
        rule.onNode(polite and hasContentDescription("Couldn't load more.")).assertExists()
        failed = false
        count = 41
        hasMore = false
        rule.waitForIdle()
        rule.onNode(polite and hasContentDescription("1 more item loaded. That's everything")).assertExists()
    }

    @Test
    fun autoLoadLoadsOnceComposedAndPausesAfterAFailure() {
        var loads = 0
        var failed by mutableStateOf(false)
        var loading by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchLoadMore(
                hasMore = true,
                onLoadMore = {
                    loads++
                    loading = true
                },
                loading = loading,
                failed = failed,
                autoLoad = true,
            )
        }
        rule.waitForIdle()
        assertEquals(1, loads)
        failed = true
        loading = false
        rule.waitForIdle()
        assertEquals(1, loads)
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchLoadMore(
                hasMore = true,
                onLoadMore = {},
                loadedCount = 3,
                totalCount = 9,
                labels = MonarchLoadMoreLabels(loadMore = "Show more", count = { loaded, total -> "$loaded / $total" }),
            )
        }
        rule.onNodeWithText("Show more").assertExists()
        rule.onNodeWithText("3 / 9").assertExists()
    }

    @Test
    fun aLongFailureWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchLoadMore(
                hasMore = true,
                onLoadMore = {},
                failed = true,
                labels = MonarchLoadMoreLabels(failed = "The next page of invoices could not be loaded."),
            )
        }
        rule.onNodeWithText("The next page", substring = true).assertTextFullyVisible()
    }

    @Test
    fun theRulesOfTheFooter() {
        val t = MonarchLoadMoreTokens
        assertEquals(LoadMoreState.Failed, t.state(hasMore = true, loading = true, failed = true))
        assertEquals(LoadMoreState.Loading, t.state(hasMore = true, loading = true, failed = false))
        assertEquals(LoadMoreState.Done, t.state(hasMore = false, loading = false, failed = false))
        assertTrue(t.showsCount(null, 3))
        assertFalse(t.showsCount(true, null))
        assertFalse(t.autoLoads(autoLoad = true, hasMore = true, loading = false, failed = true))
        assertEquals(20, t.added(20, 40))
        assertEquals(0, t.added(null, 40))
        assertEquals(0, t.added(40, 20))
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchLoadMoreTokens.mutedText(colors), theme.id)
            assertEquals(colors.dangerStrong, MonarchLoadMoreTokens.failedText(colors), theme.id)
        }
    }
}
