package dev.monarchui.components.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.navigation.tabs.MonarchTabs
import dev.monarchui.components.navigation.tabs.MonarchTabsTokens
import dev.monarchui.foundation.MonarchTone
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
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

internal object TabsFixtures : ComponentFixtures {
    override val id = "tabs"
    override val fixtures = listOf(
        fixture("line tabs with a panel") {
            MonarchTabs(selectedIndex = 0, onSelectedIndexChange = {}, panel = { Text("Board view") }) {
                tab("Board")
                tab("List")
                tab("Timeline", enabled = false)
            }
        },
        fixture("rich faces with today") {
            MonarchTabs(selectedIndex = 1, onSelectedIndexChange = {}) {
                tab("Mon", secondary = "2")
                tab("Today", secondary = "3", accent = true)
                tab("Wed", secondary = "4")
            }
        },
        fixture("counts") {
            MonarchTabs(selectedIndex = 0, onSelectedIndexChange = {}) {
                tab("Open", count = "6", countTone = MonarchTone.Accent)
                tab("Closed", count = "15")
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchTabsTest {
    @get:Rule
    val rule = createComposeRule()

    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    @Test
    fun eachTabIsATabWithSelectedStateAndTheSelectionFollowsTheTap() {
        rule.setMonarchContent {
            var selected by remember { mutableIntStateOf(0) }
            MonarchTabs(selectedIndex = selected, onSelectedIndexChange = { selected = it }) {
                tab("Board")
                tab("List")
            }
        }
        rule.onNodeWithText("Board").assert(isTab).assertIsSelected()
        rule.onNodeWithText("List").assert(isTab).assertIsNotSelected().performClick()
        rule.onNodeWithText("List").assertIsSelected()
        rule.onNodeWithText("Board").assertIsNotSelected()
    }

    @Test
    fun aDisabledTabIsDisabledAndIgnoresTaps() {
        var chosen = -1
        rule.setMonarchContent {
            MonarchTabs(selectedIndex = 0, onSelectedIndexChange = { chosen = it }) {
                tab("Board")
                tab("Timeline", enabled = false)
            }
        }
        rule.onNodeWithText("Timeline").assertIsNotEnabled().performClick()
        assertEquals(-1, chosen)
    }

    @Test
    fun theContentDescriptionReplacesTheLabelAndCountForTalkBack() {
        rule.setMonarchContent {
            MonarchTabs(selectedIndex = 0, onSelectedIndexChange = {}) {
                tab("Open", count = "6", contentDescription = "Open, 6 requests")
            }
        }
        rule.onNode(isTab).assertContentDescriptionEquals("Open, 6 requests")
    }

    @Test
    fun everyTabHasATouchTargetOfAtLeast48dp() {
        rule.setMonarchContent {
            Column {
                MonarchTabs(selectedIndex = 0, onSelectedIndexChange = {}) {
                    tab("A")
                    tab("Overview")
                }
            }
        }
        listOf("A", "Overview").forEach { rule.onNodeWithText(it).assertTouchTargetAtLeast48dp(rule.density) }
        assertEquals(36.dp, MonarchTabsTokens.Height)
        assertEquals(12.dp, MonarchTabsTokens.ReachAbove)
    }

    @Test
    fun whenTheLabelsDoNotFitTheRowScrollsAndKeepsTheSelectedTabInView() {
        rule.setMonarchContent {
            MonarchTabs(
                selectedIndex = 5,
                onSelectedIndexChange = {},
                modifier = Modifier.width(240.dp).testTag("tabs"),
            ) {
                listOf("Summary", "Contacts", "Deals", "Invoices", "Files", "History").forEach { tab(it) }
            }
        }
        rule.waitForIdle()
        val tabs = rule.onNodeWithTag("tabs").fetchSemanticsNode().boundsInRoot
        val history = rule.onNodeWithText("History", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        kotlin.test.assertTrue(history.right <= tabs.right + 1f && history.left >= tabs.left, "History is in view")
    }

    @Test
    fun theLabelsAreNotCutOffAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchTabs(selectedIndex = 0, onSelectedIndexChange = {}, modifier = Modifier.width(300.dp)) {
                tab("Overview")
                tab("Activity", count = "12")
                tab("Members")
            }
        }
        listOf("Overview", "Activity", "Members").forEach {
            rule.onNodeWithText(it, useUnmergedTree = true).assertTextFullyVisible()
        }
    }

    @Test
    fun theFirstTabIsAtTheStartInBothLayoutDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchTabs(selectedIndex = 0, onSelectedIndexChange = {}) {
                tab("First")
                tab("Second")
            }
        }
        fun check(direction: LayoutDirection) = rule.onNodeWithText("First", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Second", useUnmergedTree = true), direction)
        check(LayoutDirection.Ltr)
        environment.layoutDirection = LayoutDirection.Rtl
        rule.waitForIdle()
        check(LayoutDirection.Rtl)
    }

    @Test
    fun theSelectedTabKeepsTheDefaultTextColourAndTheIndicatorIsTheStrongAccent() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val selected = MonarchTabsTokens.colors(selected = true, hovered = false, enabled = true, colors = colors)
            val idle = MonarchTabsTokens.colors(selected = false, hovered = false, enabled = true, colors = colors)
            assertEquals(colors.textDefault, selected.content, theme.id)
            assertEquals(colors.accentStrong, selected.indicator, theme.id)
            assertEquals(colors.textMuted, idle.content, theme.id)
            assertEquals(androidx.compose.ui.graphics.Color.Transparent, idle.indicator, theme.id)
        }
    }

    @Test
    fun aRichTabShowsASecondLineAndIsStillOneTabWithAtLeast48dp() {
        rule.setMonarchContent {
            MonarchTabs(selectedIndex = 0, onSelectedIndexChange = {}) {
                tab("Mon", secondary = "2")
                tab("Tue", secondary = "3", accent = true)
            }
        }
        rule.onNodeWithText("2", useUnmergedTree = true).assertExists()
        rule.onAllNodes(isTab).assertCountEquals(2)
        rule.onAllNodes(isTab).onFirst().assertHeightIsAtLeast(48.dp)
        rule.onAllNodes(isTab)[1].assertIsNotSelected()
    }

    @Test
    fun theAccentMarksTodayInTheStrongAccentAndTheSecondLineStaysMutedOtherwise() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val idle = MonarchTabsTokens.colors(selected = false, hovered = false, enabled = true, colors = c)
            assertEquals(c.accentStrong, MonarchTabsTokens.labelColor(true, idle, true, c), theme.id)
            assertEquals(c.textMuted, MonarchTabsTokens.labelColor(false, idle, true, c), theme.id)
            assertEquals(c.accentStrong, MonarchTabsTokens.secondaryColor(true, true, c), theme.id)
            assertEquals(c.textMuted, MonarchTabsTokens.secondaryColor(false, true, c), theme.id)
            assertEquals(c.textDisabled, MonarchTabsTokens.secondaryColor(false, false, c), theme.id)
        }
    }
}
