package com.rookhq.monarch.components.intelligence

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationMessage
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiRole
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiSourceKind
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiSourcePart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiTextPart
import com.rookhq.monarch.components.intelligence.aisourcelist.MonarchAiSourceList
import com.rookhq.monarch.components.intelligence.aisourcelist.MonarchAiSourceListItem
import com.rookhq.monarch.components.intelligence.aisourcelist.MonarchAiSourceListLabels
import com.rookhq.monarch.components.intelligence.aisourcelist.MonarchAiSourceListTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun source(
    n: Int,
    url: String? = "https://www.example.com/$n",
    kind: MonarchAiSourceKind = MonarchAiSourceKind.Web,
) = MonarchAiSourcePart(id = "s$n", title = "Source title $n", url = url, kind = kind)

private fun items(count: Int, considered: Set<Int> = emptySet()) =
    (1..count).map { MonarchAiSourceListItem(source(it), used = it !in considered) }

internal object MonarchAiSourceListFixtures : ComponentFixtures {
    override val id = "ai-source-list"
    override val fixtures = listOf(
        fixture("ungrouped, links") { MonarchAiSourceList(items = items(2)) },
        fixture("grouped, buttons") { MonarchAiSourceList(items = items(2, considered = setOf(2)), onOpen = {}) },
        fixture("collapsed") { MonarchAiSourceList(items = items(3), collapseAfter = 1) },
        fixture("plain, not numbered, with snippet") {
            MonarchAiSourceList(
                items = listOf(
                    MonarchAiSourceListItem(
                        source(1, url = null, kind = MonarchAiSourceKind.Record).copy(product = "CRM"),
                        snippet = "Renewal is due in March.",
                    ),
                ),
                numbered = false,
            )
        },
        fixture("empty") { MonarchAiSourceList(items = emptyList()) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiSourceListTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachSourceIsOneElementReadWithItsNumber() {
        rule.setMonarchContent {
            MonarchAiSourceList(
                items = listOf(MonarchAiSourceListItem(source(1).copy(product = "Docs"), snippet = "An excerpt.")),
            )
        }
        rule.onNodeWithContentDescription("Source 1: Source title 1, example.com · Docs · Web page, An excerpt.")
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Sources").assert(isHeading())
    }

    @Test
    fun withOnOpenEachRowIsAButton() {
        var opened = ""
        rule.setMonarchContent { MonarchAiSourceList(items = items(1), onOpen = { opened = it.id }) }
        rule.onNodeWithContentDescription("Source 1", substring = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        assertEquals("s1", opened)
    }

    @Test
    fun withoutOnOpenSafeAddressesOpenThroughTheUriHandler() {
        val opened = mutableListOf<String>()
        val handler = object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }
        rule.setMonarchContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                MonarchAiSourceList(
                    items = listOf(
                        MonarchAiSourceListItem(source(1)),
                        MonarchAiSourceListItem(source(2, url = "javascript:alert(1)")),
                    ),
                )
            }
        }
        rule.onNodeWithContentDescription("Source 1", substring = true).performClick()
        rule.onNodeWithContentDescription("Source 2", substring = true).assertHasNoClickAction()
        assertEquals(listOf("https://www.example.com/1"), opened)
    }

    @Test
    fun someConsideredSourcesSplitTheListIntoGroups() {
        rule.setMonarchContent { MonarchAiSourceList(items = items(3, considered = setOf(1))) }
        rule.onNodeWithText("Used (2)").assert(isHeading())
        rule.onNodeWithText("Considered (1)").assert(isHeading())
        // Used sources come first; each keeps its citation number.
        rule.onNodeWithContentDescription("Source 2", substring = true)
            .assertIsAbove(rule.onNodeWithContentDescription("Source 1", substring = true))
    }

    @Test
    fun aLongListCollapsesAndExpands() {
        rule.setMonarchContent { MonarchAiSourceList(items = items(5), collapseAfter = 2) }
        rule.onNodeWithContentDescription("Source 3", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Show all 5 sources").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("Source 5", substring = true).assertExists()
        rule.onNodeWithText("Show fewer").performClick()
        rule.onNodeWithContentDescription("Source 3", substring = true).assertDoesNotExist()
    }

    @Test
    fun groupHeadingsCountHiddenSourcesToo() {
        rule.setMonarchContent { MonarchAiSourceList(items = items(4, considered = setOf(4)), collapseAfter = 2) }
        rule.onNodeWithText("Used (3)").assertExists()
        // The considered group is hidden while collapsed, so its heading is not shown.
        rule.onNodeWithText("Considered (1)").assertDoesNotExist()
    }

    @Test
    fun theNumberCanBeLeftOutAndStringsReplaced() {
        rule.setMonarchContent {
            MonarchAiSourceList(
                items = items(1),
                numbered = false,
                labels = MonarchAiSourceListLabels(heading = "Fuentes", kind = { "Página" }),
            )
        }
        rule.onNodeWithText("Fuentes").assertExists()
        rule.onNodeWithContentDescription("Source title 1, example.com · Página").assertExists()
    }

    @Test
    fun sourcesOfAMessageAreInFirstAppearanceOrderOnce() {
        val message = MonarchAiConversationMessage(
            id = "a",
            role = MonarchAiRole.Assistant,
            parts = listOf(source(2), MonarchAiTextPart("Hi"), source(1), source(2)),
            createdAt = "2025-03-10T12:00:00.000Z",
        )
        assertEquals(listOf("s2", "s1"), MonarchAiSourceListItem.sources(message).map { it.id })
    }

    @Test
    fun theLogicOrdersAndCollapses() {
        val list = items(4, considered = setOf(1))
        assertEquals(listOf(2, 3, 4, 1), MonarchAiSourceListTokens.ordered(list).map { it.number })
        assertEquals(listOf(2, 3), MonarchAiSourceListTokens.shown(list, 2, expanded = false).map { it.number })
        assertEquals(4, MonarchAiSourceListTokens.shown(list, 2, expanded = true).size)
        assertEquals(0, MonarchAiSourceListTokens.shown(list, 0, expanded = false).size)
        assertEquals(4, MonarchAiSourceListTokens.shown(list, null, expanded = false).size)
        assertEquals("example.com", MonarchAiSourceListTokens.host("https://www.example.com/a"))
        assertNull(MonarchAiSourceListTokens.host("not a url"))
        assertNull(MonarchAiSourceListTokens.safeUrl("file:///x"))
    }

    @Test
    fun longTitlesWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchAiSourceList(items = items(1)) }
        rule.onNodeWithText("Source title 1", useUnmergedTree = true).assertTextFullyVisible()
    }

    private fun SemanticsNodeInteraction.assertIsAbove(other: SemanticsNodeInteraction) {
        val mine = fetchSemanticsNode().boundsInRoot
        val theirs = other.fetchSemanticsNode().boundsInRoot
        assertTrue(mine.bottom <= theirs.top, "Expected $mine above $theirs")
    }

    @Test
    fun theTitleColourSaysWhetherItOpensInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.textLink, MonarchAiSourceListTokens.titleColor(true, c), theme.id)
            assertEquals(c.textDefault, MonarchAiSourceListTokens.titleColor(false, c), theme.id)
        }
    }
}
