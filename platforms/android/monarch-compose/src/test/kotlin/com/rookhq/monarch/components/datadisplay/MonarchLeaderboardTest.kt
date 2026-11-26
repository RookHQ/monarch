package com.rookhq.monarch.components.datadisplay

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.charts.barlist.MonarchBarListSort
import com.rookhq.monarch.components.datadisplay.leaderboard.MonarchLeaderboard
import com.rookhq.monarch.components.datadisplay.leaderboard.MonarchLeaderboardEntry
import com.rookhq.monarch.components.datadisplay.leaderboard.MonarchLeaderboardLabels
import com.rookhq.monarch.components.datadisplay.leaderboard.MonarchLeaderboardLogic
import com.rookhq.monarch.components.datadisplay.leaderboard.MonarchLeaderboardTokens
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarShape
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Entries = listOf(
    MonarchLeaderboardEntry(name = "Ana", value = 42.0, change = 0.12, current = true),
    MonarchLeaderboardEntry(name = "Ben", value = 42.0, secondary = "Tier 2"),
    MonarchLeaderboardEntry(name = "Chloé", value = 31.0, badge = "3 breached", badgeTone = MonarchTone.Danger),
    MonarchLeaderboardEntry(name = "Dev", value = 12.0, avatarShape = MonarchAvatarShape.Square),
)

private fun texts(node: SemanticsNode): List<String> =
    node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
        node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }

internal object MonarchLeaderboardFixtures : ComponentFixtures {
    override val id = "leaderboard"
    override val fixtures = listOf(
        fixture("ranked, current, ties, badge, change") {
            MonarchLeaderboard(entries = Entries, valueLabel = "tickets", changeComparison = "vs last week", total = 9)
        },
        fixture("top three, selectable, no avatars") {
            MonarchLeaderboard(entries = Entries.take(3), highlightTop = true, showAvatars = false, onSelect = {})
        },
        fixture("no bars, more button") {
            MonarchLeaderboard(entries = Entries.take(2), showBars = false, total = 20, onShowMore = {})
        },
        fixture("empty") { MonarchLeaderboard(entries = emptyList()) },
        fixture("failed with retry") {
            MonarchLeaderboard(entries = emptyList(), errorMessage = "The ranking could not be loaded.", onRetry = {})
        },
        fixture("failed without retry") {
            MonarchLeaderboard(entries = emptyList(), errorMessage = "The ranking could not be loaded.")
        },
        fixture("long name") {
            MonarchLeaderboard(
                entries = listOf(
                    MonarchLeaderboardEntry(name = "Maria Fernanda de los Santos y Guerrero", value = 1204.0),
                ),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchLeaderboardTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachRowIsOneElementReadWithItsRankValueAndChange() {
        rule.setMonarchContent {
            MonarchLeaderboard(entries = Entries, valueLabel = "tickets solved", label = "Top agents")
        }
        rule.onNodeWithContentDescription("Top agents").assertExists()
        val row = rule.onNode(hasContentDescription("Rank 1:") and hasText("Ana"))
        val read = texts(row.fetchSemanticsNode())
        assertEquals("Rank 1:", read.first())
        assertTrue("Ana" in read && "(you)" in read && "42 tickets solved" in read, read.toString())
        assertTrue("up 12%, better" in read, read.toString())
        assertFalse(row.fetchSemanticsNode().config.contains(SemanticsProperties.Role))
    }

    @Test
    fun tiesShareARankAndTheNextRankSkips() {
        rule.setMonarchContent { MonarchLeaderboard(entries = Entries) }
        rule.onAllNodesWithText("1", useUnmergedTree = true).assertCountEquals(2)
        rule.onNodeWithText("3", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithText("2", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun withOnSelectEachRowIsAButton() {
        var chosen: String? = null
        rule.setMonarchContent { MonarchLeaderboard(entries = Entries, onSelect = { chosen = it.name }) }
        rule.onNode(hasContentDescription("Rank 3:"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("Chloé", chosen)
    }

    @Test
    fun aLimitAndATotalAddMore() {
        var more = 0
        rule.setMonarchContent {
            MonarchLeaderboard(entries = Entries, limit = 2, total = 10, onShowMore = { more++ })
        }
        rule.onAllNodesWithText("Chloé", useUnmergedTree = true).assertCountEquals(0)
        rule.onNodeWithText("8 more").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, more)
    }

    @Test
    fun loadingIsOneElementNamedLoadingRanking() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchLeaderboard(entries = Entries, loading = true, loadingRows = 2) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Loading ranking").assertExists()
        rule.onAllNodesWithText("Ana", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun anErrorReplacesTheList() {
        var retries = 0
        rule.setMonarchContent {
            MonarchLeaderboard(entries = Entries, errorMessage = "Could not load.", onRetry = { retries++ })
        }
        rule.onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
        rule.onAllNodesWithText("Ana", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun anErrorWithoutRetryIsAPoliteLiveRegion() {
        rule.setMonarchContent { MonarchLeaderboard(entries = Entries, errorMessage = "Could not load.") }
        rule.onNodeWithText("Could not load.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun noEntriesShowTheEmptyState() {
        rule.setMonarchContent { MonarchLeaderboard(entries = emptyList()) }
        rule.onNodeWithText("Nothing to rank yet", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchLeaderboard(
                entries = Entries,
                limit = 1,
                formatValue = { "${it.toInt()} pts" },
                labels = MonarchLeaderboardLabels(you = "(tú)", rank = { "Puesto $it." }, more = { "$it más" }),
            )
        }
        rule.onNode(hasContentDescription("Puesto 1.")).assertExists()
        rule.onNodeWithText("(tú)", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("42 pts", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("3 más").assertExists()
    }

    @Test
    fun theRankComesFirstAndTheValueLastInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MonarchLeaderboard(entries = Entries.take(1)) }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithContentDescription("Rank 1:", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Ana", useUnmergedTree = true), direction)
            rule.onNodeWithText("Ana", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("42", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun longNamesWrapAtTheLargestFontScale() {
        val name = "Maria Fernanda de los Santos y Guerrero"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchLeaderboard(entries = listOf(MonarchLeaderboardEntry(name = name, value = 1204.0)))
        }
        rule.onNodeWithText(name, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theRankingRulesMatchTheWeb() {
        val l = MonarchLeaderboardLogic
        val values = listOf(5.0, 9.0, 9.0, 1.0).map { MonarchLeaderboardEntry(name = "e$it", value = it) }
        assertEquals(listOf(1, 1, 3, 4), l.rank(values, MonarchBarListSort.Descending).map { it.rank })
        assertEquals(listOf(1.0, 5.0, 9.0, 9.0), l.rank(values, MonarchBarListSort.Ascending).map { it.entry.value })
        assertEquals(listOf(1, 2, 2, 4), l.rank(values, MonarchBarListSort.None).map { it.rank })
        val ranked = l.rank(values, MonarchBarListSort.None)
        assertEquals(4, l.shown(ranked, 0).size)
        assertEquals(4, l.shown(ranked, -1).size)
        assertEquals(2, l.shown(ranked, 2).size)
        assertEquals(0, l.hidden(total = 1, entries = 4, shown = 2))
        assertEquals(8, l.hidden(total = 10, entries = 4, shown = 2))
        assertEquals(2, l.hidden(total = null, entries = 4, shown = 2))
        val signed = l.rank(
            listOf(MonarchLeaderboardEntry("a", -10.0), MonarchLeaderboardEntry("b", 5.0)),
            MonarchBarListSort.None,
        )
        assertEquals(1f, l.share(-10.0, signed))
        assertEquals(0.5f, l.share(5.0, signed))
        val zero = l.rank(listOf(MonarchLeaderboardEntry("z", 0.0)), MonarchBarListSort.None)
        assertEquals(0f, l.share(0.0, zero))
        assertTrue(l.isTop(highlightTop = true, rank = 3))
        assertFalse(l.isTop(highlightTop = true, rank = 4))
        assertFalse(l.isTop(highlightTop = false, rank = 1))
    }

    @Test
    fun theColoursComeFromTheirRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchLeaderboardTokens
            assertEquals(c.accentSubtle, t.rowColors(true, c).container, theme.id)
            assertEquals(c.accentBorder, t.rowColors(true, c).border, theme.id)
            assertNull(t.rowColors(false, c).border, theme.id)
            assertEquals(c.accentSubtle, t.rankColors(true, c).container, theme.id)
            assertEquals(c.accentStrong, t.rankColors(true, c).content, theme.id)
            assertEquals(c.textMuted, t.rankColors(false, c).content, theme.id)
            assertEquals(c.textMuted, t.mutedColor(c), theme.id)
            assertEquals(c.dangerStrong, t.errorColor(c), theme.id)
        }
    }
}
