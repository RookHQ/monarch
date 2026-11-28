package com.rookhq.monarch.components.charts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.barlist.MonarchBarList
import com.rookhq.monarch.components.charts.barlist.MonarchBarListItem
import com.rookhq.monarch.components.charts.barlist.MonarchBarListLayout
import com.rookhq.monarch.components.charts.barlist.MonarchBarListSort
import com.rookhq.monarch.components.charts.barlist.MonarchBarListTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val Pages = listOf(
    MonarchBarListItem("/blog", 1120.0),
    MonarchBarListItem("/pricing", 4210.0),
    MonarchBarListItem("/legacy", null),
    MonarchBarListItem("/docs", 2980.0, description = "Documentation"),
)

internal object MonarchBarListFixtures : ComponentFixtures {
    override val id = "bar-list"
    override val fixtures = MonarchBarListSort.entries.map { sort ->
        fixture("sort ${sort.name}, ranked") { MonarchBarList(items = Pages, sort = sort, ranked = true) }
    } + listOf(
        fixture("selectable rows") { MonarchBarList(items = Pages, onSelect = {}, label = "Top pages") },
        fixture("highlighted row with an icon and a display value") {
            MonarchBarList(
                items = listOf(
                    MonarchBarListItem(
                        "You",
                        0.31,
                        displayValue = "31%",
                        highlighted = true,
                        icon = {
                            MonarchIcon(imageVector = TestIcon, contentDescription = null, size = MonarchIconSize.Sm)
                        },
                    ),
                    MonarchBarListItem("Team average", 0.12, displayValue = "12%"),
                ),
                maximum = 1.0,
            )
        },
        fixture("limit with a footer") { MonarchBarList(items = Pages, limit = 2) },
        fixture("long labels in a narrow width") {
            MonarchBarList(
                items = listOf(MonarchBarListItem("/blog/the-longest-article-title-on-the-whole-site-so-far", 42.0)),
                modifier = Modifier.width(200.dp),
            )
        },
        fixture("empty") { MonarchBarList(items = emptyList()) },
        fixture("loading") { MonarchBarList(items = Pages, loading = true) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchBarListTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachRowIsReadAsOneElementWithItsValue() {
        rule.setMonarchContent { MonarchBarList(items = Pages) }
        rule.onNodeWithText("/docs").assert(hasText("Documentation")).assert(hasText("2,980"))
            .assertHasNoClickAction()
    }

    @Test
    fun aMissingValueIsReadAsNoValueAndTheTextCanBeReplaced() {
        rule.setMonarchContent {
            Box {
                MonarchBarList(items = listOf(MonarchBarListItem("/legacy", null)))
                MonarchBarList(items = listOf(MonarchBarListItem("/old", Double.NaN)), missingLabel = "Sin valor")
            }
        }
        rule.onNodeWithText("/legacy").assert(hasText("No value"))
        rule.onNodeWithText("/old").assert(hasText("Sin valor"))
    }

    @Test
    fun rowsAreSortedWithMissingValuesLast() {
        val descending = MonarchBarListLayout.sorted(Pages, MonarchBarListSort.Descending).map { it.label }
        assertEquals(listOf("/pricing", "/docs", "/blog", "/legacy"), descending)
        val ascending = MonarchBarListLayout.sorted(Pages, MonarchBarListSort.Ascending).map { it.label }
        assertEquals(listOf("/blog", "/docs", "/pricing", "/legacy"), ascending)
        assertEquals(Pages, MonarchBarListLayout.sorted(Pages, MonarchBarListSort.None))
        val ties = listOf(MonarchBarListItem("a", 1.0), MonarchBarListItem("b", 1.0))
        val tied = MonarchBarListLayout.sorted(ties, MonarchBarListSort.Descending).map { it.label }
        assertEquals(listOf("a", "b"), tied)
    }

    @Test
    fun theBarIsAShareOfTheLargestValueOrOfTheMaximum() {
        assertEquals(4210.0, MonarchBarListLayout.scale(Pages, null))
        assertEquals(0.5f, MonarchBarListLayout.fraction(50.0, 100.0))
        assertEquals(1f, MonarchBarListLayout.fraction(150.0, 100.0))
        assertEquals(0f, MonarchBarListLayout.fraction(-5.0, 100.0))
        assertEquals(0f, MonarchBarListLayout.fraction(null, 100.0))
        assertEquals(0f, MonarchBarListLayout.fraction(5.0, 0.0))
        assertEquals(1.0, MonarchBarListLayout.scale(Pages, 1.0))
        assertEquals(0.0, MonarchBarListLayout.scale(listOf(MonarchBarListItem("a", -3.0)), null))
    }

    @Test
    fun aLimitShowsTheFirstRowsAndAFooter() {
        rule.setMonarchContent { MonarchBarList(items = Pages, limit = 2) }
        rule.onNodeWithText("/pricing").assertExists()
        rule.onNodeWithText("/docs").assertExists()
        rule.onNodeWithText("/blog").assertDoesNotExist()
        rule.onNodeWithText("Top 2 of 4").assertExists()
    }

    @Test
    fun theOverflowLabelCanBeReplaced() {
        rule.setMonarchContent { MonarchBarList(items = Pages, limit = 1, overflowLabel = "And 3 more") }
        rule.onNodeWithText("And 3 more").assertExists()
    }

    @Test
    fun rankNumbersAreNotRead() {
        rule.setMonarchContent { MonarchBarList(items = Pages, ranked = true) }
        assertEquals(0, rule.onAllNodesWithText("1").fetchSemanticsNodes().size)
    }

    @Test
    fun selectableRowsAreButtonsWithA48dpTarget() {
        val selected = mutableListOf<String>()
        rule.setMonarchContent { MonarchBarList(items = Pages, onSelect = { selected += it.label }) }
        val row = rule.onNodeWithText("/docs")
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)).assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(listOf("/docs"), selected)
    }

    @Test
    fun emptyAndLoadingShowTheirTextAndCanBeReplaced() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            Box {
                MonarchBarList(items = emptyList())
                MonarchBarList(items = emptyList(), emptyLabel = "Nothing yet", modifier = Modifier.testTag("own"))
                MonarchBarList(items = Pages, loading = true, loadingLabel = "Loading pages")
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("No data yet").assertExists()
        rule.onNodeWithText("Nothing yet").assertExists()
        rule.onNodeWithText("Loading pages").assertExists()
        rule.onNodeWithText("/docs").assertDoesNotExist()
    }

    @Test
    fun theRowContentStartsAtTheStartEdgeInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            Box(Modifier.width(300.dp)) {
                MonarchBarList(
                    items = listOf(MonarchBarListItem("Half", 50.0), MonarchBarListItem("Full", 100.0)),
                    modifier = Modifier.testTag("list"),
                )
            }
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            val list = rule.onNodeWithTag("list").getUnclippedBoundsInRoot()
            val label = rule.onNodeWithText("Half", useUnmergedTree = true).getUnclippedBoundsInRoot()
            if (direction == LayoutDirection.Ltr) {
                assertTrue(label.left - list.left < 20.dp, "LTR: the label starts at the left")
            } else {
                assertTrue(list.right - label.right < 20.dp, "RTL: the label starts at the right")
            }
        }
    }

    @Test
    fun longLabelsWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchBarList(
                items = listOf(MonarchBarListItem("/blog/the-longest-article-title", 42.0)),
                modifier = Modifier.width(220.dp),
            )
        }
        rule.onNodeWithText("/blog/the-longest-article-title", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("42", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun barsAreTheFirstDataColourOnAQuietMutedTrack() {
        // PREMIUM rule 8: an 8dp pill under the label, not a slab behind it.
        assertEquals(8.dp, MonarchBarListTokens.TrackHeight)
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.bgMuted, MonarchBarListTokens.trackColor(colors), theme.id)
            assertEquals(colors.data1, MonarchBarListTokens.barColor(colors), theme.id)
            assertEquals(colors.textMuted, MonarchBarListTokens.mutedColor(colors), theme.id)
        }
    }
}
