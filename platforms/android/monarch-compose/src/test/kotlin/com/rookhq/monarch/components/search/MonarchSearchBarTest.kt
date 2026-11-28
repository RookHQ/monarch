package com.rookhq.monarch.components.search

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.search.searchbar.MonarchSearchBar
import com.rookhq.monarch.components.search.searchbar.MonarchSearchBarTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
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

@Composable
private fun SearchIcon(tag: String = "search") {
    MonarchIcon(imageVector = TestIcon, contentDescription = null, modifier = Modifier.testTag(tag))
}

internal object MonarchSearchBarFixtures : ComponentFixtures {
    override val id = "search-input"
    override val fixtures = listOf(
        fixture("empty and with text") {
            Column {
                MonarchSearchBar(query = "", onQueryChange = {}, onSearch = {}, leadingIcon = { SearchIcon() })
                MonarchSearchBar(
                    query = "invoices",
                    onQueryChange = {},
                    onSearch = {},
                    leadingIcon = { SearchIcon() },
                    clearIcon = { SearchIcon("clear") },
                )
            }
        },
        fixture("pending and disabled") {
            Column {
                MonarchSearchBar(query = "invoices", onQueryChange = {}, onSearch = {}, pending = true)
                MonarchSearchBar(query = "invoices", onQueryChange = {}, onSearch = {}, enabled = false)
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSearchBarTest {
    @get:Rule
    val rule = createComposeRule()

    /** The text field; a disabled one has no set-text action, so it is found by its editable text. */
    private val field get() = rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText))

    @Composable
    private fun StatefulSearchBar(
        searches: MutableList<String>,
        debounceMillis: Long = 0L,
        onClear: (() -> Unit)? = null,
        initial: String = "",
    ) {
        var query by remember { mutableStateOf(initial) }
        MonarchSearchBar(
            query = query,
            onQueryChange = { query = it },
            onSearch = { searches += it },
            debounceMillis = debounceMillis,
            leadingIcon = { SearchIcon() },
            clearIcon = { SearchIcon("clear") },
            onClear = onClear,
        )
    }

    @Test
    fun withoutDebounceEveryChangeSearchesAndTheSearchKeySearchesAtOnce() {
        val searches = mutableListOf<String>()
        rule.setMonarchContent { StatefulSearchBar(searches) }
        field.performTextInput("ab")
        assertEquals("ab", searches.last())
        searches.clear()
        field.performImeAction()
        assertEquals(listOf("ab"), searches)
    }

    @Test
    fun theDebounceWaitsForTheLastKeystroke() {
        val searches = mutableListOf<String>()
        rule.setMonarchContent { StatefulSearchBar(searches, debounceMillis = 300) }
        rule.mainClock.autoAdvance = false
        field.performTextInput("inv")
        rule.mainClock.advanceTimeBy(100)
        assertEquals(emptyList(), searches)
        rule.mainClock.advanceTimeBy(400)
        assertEquals(listOf("inv"), searches)
    }

    @Test
    fun theClearButtonEmptiesTheFieldAndSearchesForNothing() {
        val searches = mutableListOf<String>()
        var cleared = 0
        rule.setMonarchContent { StatefulSearchBar(searches, onClear = { cleared++ }, initial = "invoices") }
        rule.onNodeWithContentDescription("Clear search").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf(""), searches)
        assertEquals(1, cleared)
        rule.onNodeWithContentDescription("Clear search").assertDoesNotExist()
    }

    @Test
    fun whilePendingTheFieldSaysSearchingAndShowsAProgressIndicator() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchSearchBar(query = "x", onQueryChange = {}, onSearch = {}, pending = true) }
        rule.mainClock.advanceTimeByFrame()
        assertEquals("Searching", field.stateDescription())
        rule.onNodeWithContentDescription("Searching").assertExists()
    }

    @Test
    fun thePlaceholderAndLabelsCanBeReplaced() {
        rule.setMonarchContent {
            Column {
                MonarchSearchBar(query = "", onQueryChange = {}, onSearch = {})
                MonarchSearchBar(
                    query = "a",
                    onQueryChange = {},
                    onSearch = {},
                    placeholder = "Search invoices",
                    clearIcon = { SearchIcon("clear") },
                    clearLabel = "Leeren",
                )
            }
        }
        rule.onNodeWithText("Search", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Leeren").assertExists()
    }

    @Test
    fun aDisabledFieldCannotBeEditedAndHasNoClearButton() {
        rule.setMonarchContent {
            MonarchSearchBar(
                query = "a",
                onQueryChange = {},
                onSearch = {},
                enabled = false,
                clearIcon = { SearchIcon() },
            )
        }
        field.assertIsNotEnabled()
        rule.onNodeWithContentDescription("Clear search").assertDoesNotExist()
    }

    @Test
    fun rightToLeftPutsTheIconAtTheRightAndTheClearButtonAtTheLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            StatefulSearchBar(mutableListOf(), initial = "a")
        }
        rule.onNodeWithTag("search", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithTag("clear", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun thePlaceholderIsNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchSearchBar(query = "", onQueryChange = {}, onSearch = {})
        }
        rule.onNodeWithText("Search", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheThemeRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val roles = MonarchSearchBarTokens.roles(colors)
            assertEquals(colors.bgSubtle, roles.container, theme.id)
            assertEquals(colors.textDefault, roles.text, theme.id)
            assertEquals(colors.textSubtle, roles.placeholder, theme.id)
            assertEquals(colors.textMuted, roles.icon, theme.id)
            assertEquals(colors.textDisabled, roles.disabledText, theme.id)
            assertEquals(colors.accentSolid, roles.cursor, theme.id)
            assertEquals(colors.borderSubtle, roles.divider, theme.id)
        }
    }
}
