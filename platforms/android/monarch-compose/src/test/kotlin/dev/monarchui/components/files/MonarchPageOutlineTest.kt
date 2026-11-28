package dev.monarchui.components.files

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.files.pageoutline.MonarchPageOutline
import dev.monarchui.components.files.pageoutline.MonarchPageOutlineHeading
import dev.monarchui.components.files.pageoutline.MonarchPageOutlineLabels
import dev.monarchui.components.files.pageoutline.MonarchPageOutlineStats
import dev.monarchui.components.files.pageoutline.MonarchPageOutlineTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val Headings = listOf(
    MonarchPageOutlineHeading("Overview", 1, "overview"),
    MonarchPageOutlineHeading("Goals", 2, "goals"),
    MonarchPageOutlineHeading("Risks", 3, "risks"),
    MonarchPageOutlineHeading("Next steps", 1, "next"),
)

private const val LongHeading = "What changes for the support team when the new routing rules go live next quarter"

internal object MonarchPageOutlineFixtures : ComponentFixtures {
    override val id = "page-outline"
    override val fixtures = listOf(
        fixture("nested with a current heading") {
            MonarchPageOutline(headings = Headings, onNavigate = {}, currentId = "goals")
        },
        fixture("stats, disabled") {
            MonarchPageOutline(
                headings = Headings.take(2),
                onNavigate = {},
                enabled = false,
                stats = MonarchPageOutlineStats(words = 1_234, minutes = 6),
            )
        },
        fixture("long heading, no title") {
            MonarchPageOutline(
                headings = listOf(MonarchPageOutlineHeading(LongHeading, 2, "long")),
                onNavigate = {},
                currentId = "long",
                showTitle = false,
            )
        },
        fixture("empty") { MonarchPageOutline(headings = emptyList(), onNavigate = {}) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchPageOutlineTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theCurrentHeadingIsSelectedAndSaysSo() {
        rule.setMonarchContent { MonarchPageOutline(headings = Headings, onNavigate = {}, currentId = "goals") }
        rule.onNodeWithText("On this page").assert(isHeading())
        rule.onNodeWithContentDescription("On this page").assertExists()
        val goals = rule.onNodeWithText("Goals").assertIsSelected()
        assertEquals("Current section", goals.stateDescription())
        val overview = rule.onNodeWithText("Overview").assertIsNotSelected()
        assertNull(overview.stateDescription())
    }

    @Test
    fun pickingAHeadingNavigatesUnlessDisabled() {
        val picked = mutableListOf<String>()
        var enabled by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchPageOutline(headings = Headings, onNavigate = { picked += it.id }, enabled = enabled)
        }
        rule.onNodeWithText("Risks").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf("risks"), picked)
        enabled = false
        rule.waitForIdle()
        rule.onNodeWithText("Risks").assertIsNotEnabled().performClick()
        assertEquals(listOf("risks"), picked)
    }

    @Test
    fun theOutlineScrollsToTheCurrentHeading() {
        val many = (1..30).map { MonarchPageOutlineHeading("Section $it", 1, "s$it") }
        var current by mutableStateOf<String?>(null)
        rule.setMonarchContent {
            MonarchPageOutline(
                headings = many,
                onNavigate = {},
                modifier = Modifier.height(200.dp),
                currentId = current,
            )
        }
        rule.onNodeWithText("Section 30").assertDoesNotExist()
        current = "s30"
        rule.waitForIdle()
        rule.onNodeWithText("Section 30").assertIsDisplayed().assertIsSelected()
    }

    @Test
    fun nestedHeadingsAreIndentedTowardsTheEndRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchPageOutline(headings = Headings, onNavigate = {})
        }
        rule.onNodeWithText("Overview", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Risks", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun longHeadingsWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchPageOutline(headings = listOf(MonarchPageOutlineHeading(LongHeading, 1, "l")), onNavigate = {})
        }
        rule.onNodeWithText(LongHeading, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun emptyAndStatsUseTheBuiltInStringsOrTheOverrides() {
        rule.setMonarchContent {
            MonarchPageOutline(
                headings = emptyList(),
                onNavigate = {},
                stats = MonarchPageOutlineStats(words = 1_234, minutes = 6),
            )
        }
        rule.onNodeWithText("Headings you add appear here.").assertExists()
        rule.onNodeWithText("1,234 words · 6 min read").assertExists()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchPageOutline(
                headings = Headings,
                onNavigate = {},
                currentId = "next",
                stats = MonarchPageOutlineStats(words = 1, minutes = 1),
                labels = MonarchPageOutlineLabels(
                    title = "En esta página",
                    current = "Sección actual",
                    stats = { "${it.words} palabra" },
                ),
            )
        }
        rule.onNodeWithText("En esta página").assertExists()
        rule.onNodeWithText("1 palabra").assertExists()
        assertEquals("Sección actual", rule.onNodeWithText("Next steps").stateDescription())
    }

    @Test
    fun aHiddenTitleStaysTheSpokenName() {
        rule.setMonarchContent { MonarchPageOutline(headings = Headings, onNavigate = {}, showTitle = false) }
        rule.onNodeWithText("On this page").assertDoesNotExist()
        rule.onNodeWithContentDescription("On this page").assertExists()
    }

    @Test
    fun entriesNestByTheHeadingTree() {
        val headings = listOf(
            MonarchPageOutlineHeading("A", 1, "a"),
            MonarchPageOutlineHeading("B", 3, "b"),
            MonarchPageOutlineHeading("  ", 2, "blank"),
            MonarchPageOutlineHeading("C", 4, "deep"),
            MonarchPageOutlineHeading("D", 2, "d"),
            MonarchPageOutlineHeading("E", 0, "e"),
            MonarchPageOutlineHeading("F", 9, "f"),
        )
        val entries = MonarchPageOutlineTokens.entries(headings, maxLevel = 3)
        assertEquals(listOf("a" to 0, "b" to 1, "d" to 1, "e" to 0), entries.map { it.heading.id to it.depth })
        assertEquals("f", MonarchPageOutlineTokens.entries(headings, maxLevel = 6).last().heading.id)
    }

    @Test
    fun statsCountWordsAndRoundMinutesUp() {
        val stats = MonarchPageOutlineStats.fromText("one  two\nthree ")
        assertEquals(3, stats.words)
        assertEquals(1, stats.minutes)
        assertEquals(5, MonarchPageOutlineStats.fromText(List(1_100) { "w" }.joinToString(" ")).minutes)
        assertEquals(0, MonarchPageOutlineStats.fromText("   ").words)
    }

    @Test
    fun theColoursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val tokens = MonarchPageOutlineTokens
            assertEquals(c.accentStrong, tokens.textColor(current = true, enabled = true, c), theme.id)
            assertEquals(c.textMuted, tokens.textColor(current = false, enabled = true, c), theme.id)
            assertEquals(c.textDisabled, MonarchPageOutlineTokens.textColor(current = true, enabled = false, c))
            assertEquals(c.accentStrong, MonarchPageOutlineTokens.barColor(current = true, enabled = true, c))
            assertEquals(c.borderSubtle, MonarchPageOutlineTokens.barColor(current = false, enabled = true, c))
            assertEquals(c.borderSubtle, MonarchPageOutlineTokens.barColor(current = true, enabled = false, c))
        }
    }
}
