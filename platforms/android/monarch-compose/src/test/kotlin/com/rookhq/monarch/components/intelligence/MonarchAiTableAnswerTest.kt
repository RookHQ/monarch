package com.rookhq.monarch.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON
import com.rookhq.monarch.components.intelligence.aitableanswer.MonarchAiTableAnswer
import com.rookhq.monarch.components.intelligence.aitableanswer.MonarchAiTableAnswerData
import com.rookhq.monarch.components.intelligence.aitableanswer.MonarchAiTableAnswerLabels
import com.rookhq.monarch.components.intelligence.aitableanswer.MonarchAiTableAnswerTokens
import com.rookhq.monarch.components.intelligence.aitableanswer.MonarchAiTableAnswerValues
import com.rookhq.monarch.components.intelligence.aitableanswer.MonarchAiTableColumn
import com.rookhq.monarch.components.intelligence.aitableanswer.MonarchAiTableColumnAlignment
import com.rookhq.monarch.components.intelligence.aitableanswer.MonarchAiTableColumnType
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun row(region: String?, revenue: MonarchAiJSON) = buildMap {
    put("Region", region?.let { MonarchAiJSON.Text(it) } ?: MonarchAiJSON.Null)
    put("Revenue", revenue)
}

private val Rows = listOf(
    row("North", MonarchAiJSON.Number(1200.0)),
    row("South", MonarchAiJSON.Number(-3.0)),
    row(null, MonarchAiJSON.Number(50.0)),
)

internal object MonarchAiTableAnswerFixtures : ComponentFixtures {
    override val id = "ai-table-answer"
    override val fixtures = listOf(
        fixture("rows, sortable, copyable") { MonarchAiTableAnswer(rows = Rows.take(2), title = "Revenue") },
        fixture("not sortable, not copyable, with artifact") {
            MonarchAiTableAnswer(rows = Rows.take(2), sortable = false, copyable = false, onOpenAsArtifact = {})
        },
        fixture("streaming") { MonarchAiTableAnswer(rows = Rows.take(1), streaming = true, copyable = false) },
        fixture("empty") { MonarchAiTableAnswer(rows = emptyList()) },
        fixture("long, one row shown") { MonarchAiTableAnswer(rows = Rows, visibleRows = 1, copyable = false) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiTableAnswerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theHeaderIsOneHeadingWithTheRowCount() {
        rule.setMonarchContent { MonarchAiTableAnswer(rows = Rows, title = "Revenue by region") }
        rule.onNode(hasText("AI table") and hasText("Revenue by region") and hasText("3 rows")).assert(isHeading())
    }

    @Test
    fun eachRowIsReadAsHeaderAndValuePairs() {
        rule.setMonarchContent { MonarchAiTableAnswer(rows = Rows) }
        rule.onNodeWithContentDescription("Region: North, Revenue: 1,200").assertExists()
        rule.onNodeWithContentDescription("Region: Empty, Revenue: 50").assertExists()
    }

    @Test
    fun headersAreHeadingButtonsThatSortAndAnnounce() {
        rule.setMonarchContent { MonarchAiTableAnswer(rows = Rows) }
        val button = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        val header = rule.onNode(hasText("Revenue") and button)
        header.assert(isHeading()).assertTouchTargetAtLeast48dp(rule.density).performClick()
        header.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Sorted ascending"))
        val rows = rule.onAllNodesWithContentDescription("Region:", substring = true).fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.ContentDescription].single() }
        assertEquals(
            listOf("Region: South, Revenue: -3", "Region: Empty, Revenue: 50", "Region: North, Revenue: 1,200"),
            rows,
        )
        val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        rule.onNode(polite and SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription,
            listOf("Sorted by Revenue, ascending."),
        )).assertExists()
        header.performClick()
        header.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Sorted descending"))
    }

    @Test
    fun blanksStayLastInBothDirections() {
        val columns = MonarchAiTableAnswerData.columns(null, Rows)
        val up = MonarchAiTableAnswerData.Sort("Region", ascending = true)
        assertEquals(listOf(0, 1, 2), MonarchAiTableAnswerData.order(Rows, up, columns, Locale.US))
        val down = up.copy(ascending = false)
        assertEquals(listOf(1, 0, 2), MonarchAiTableAnswerData.order(Rows, down, columns, Locale.US))
        assertEquals(up.copy(ascending = false), MonarchAiTableAnswerData.toggled(up, "Region"))
        assertEquals(up, MonarchAiTableAnswerData.toggled(up.copy(ascending = false), "Region"))
    }

    @Test
    fun withoutSortingHeadersAreNotButtons() {
        rule.setMonarchContent { MonarchAiTableAnswer(rows = Rows, sortable = false) }
        rule.onNodeWithText("Revenue").assertHasNoClickAction().assert(isHeading())
    }

    @Test
    fun streamingShowsAPlaceholderAndAnnouncesTheEnd() {
        var streaming by mutableStateOf(true)
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchAiTableAnswer(rows = Rows, streaming = streaming, onOpenAsArtifact = {}) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("More rows are being generated").assertExists()
        rule.onNodeWithText("Open as artifact").assertIsNotEnabled()
        streaming = false
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("More rows are being generated").assertDoesNotExist()
        rule.onNodeWithText("Open as artifact").assertIsEnabled()
        rule.onNodeWithContentDescription("Table complete.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun noRowsSaysSoAndDisablesCopy() {
        rule.setMonarchContent { MonarchAiTableAnswer(rows = emptyList()) }
        rule.onNodeWithText("No rows.").assertExists()
        rule.onNodeWithContentDescription("Copy as CSV").assertIsNotEnabled()
    }

    @Test
    fun aLongTableShowsTheFirstRowsThenAll() {
        rule.setMonarchContent { MonarchAiTableAnswer(rows = Rows, visibleRows = 1) }
        rule.onNodeWithContentDescription("Region: South", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Show all 3 rows").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("Region: South", substring = true).assertExists()
        rule.onNodeWithText("Show all 3 rows").assertDoesNotExist()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiTableAnswer(
                rows = Rows.take(1),
                labels = MonarchAiTableAnswerLabels(
                    aiTable = "Tableau IA",
                    rowCount = { "$it ligne" },
                    copyCsv = "Copier",
                ),
            )
        }
        rule.onNodeWithText("Tableau IA", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("1 ligne", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Copier").assertExists()
    }

    @Test
    fun numbersSitAtTheEndAndColumnsMirrorInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { MonarchAiTableAnswer(rows = Rows.take(1)) }
        rule.onNodeWithText("Region", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Revenue", useUnmergedTree = true), LayoutDirection.Rtl)
        val columns = MonarchAiTableAnswerData.columns(null, Rows)
        assertEquals(listOf("Region", "Revenue"), columns.map { it.key })
        assertEquals(MonarchAiTableColumnAlignment.End, columns[1].alignment)
        assertEquals(MonarchAiTableColumnType.Number, columns[1].type)
    }

    @Test
    fun cellsStayOnOneLineAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchAiTableAnswer(rows = Rows.take(1)) }
        rule.onNodeWithText("North", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("1,200", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun typesAreInferredFromTheValues() {
        val text = { s: String -> MonarchAiJSON.Text(s) }
        assertEquals(1200.0, MonarchAiTableAnswerValues.number(text("1,200")))
        assertEquals(12.0, MonarchAiTableAnswerValues.number(text("12%")))
        assertEquals(-3.0, MonarchAiTableAnswerValues.number(text("−3")))
        assertNull(MonarchAiTableAnswerValues.number(text("1,20")))
        assertEquals(0L, MonarchAiTableAnswerValues.date(text("1970-01-01")))
        assertEquals(1_728_898_200_000L, MonarchAiTableAnswerValues.date(text("2024-10-14T09:30:00Z")))
        assertEquals(1_728_898_200_000L, MonarchAiTableAnswerValues.date(text("2024-10-14T11:30+02:00")))
        assertNull(MonarchAiTableAnswerValues.date(text("2024-13-01")))
        assertEquals(
            MonarchAiTableColumnType.Number,
            MonarchAiTableAnswerValues.inferType(listOf(text("12%"), MonarchAiJSON.Number(3.0), MonarchAiJSON.Null)),
        )
        assertEquals(MonarchAiTableColumnType.Date, MonarchAiTableAnswerValues.inferType(listOf(text("2024-01-02"))))
        assertEquals(MonarchAiTableColumnType.Text, MonarchAiTableAnswerValues.inferType(listOf(text("12"), text("a"))))
        assertEquals("[1,2]", MonarchAiTableAnswerValues.text(MonarchAiJSON.parse("[1,2]")))
        assertEquals(-1, MonarchAiTableAnswerValues.compareText("item 2", "Item 10", Locale.US).coerceIn(-1, 1))
        assertEquals(0, MonarchAiTableAnswerValues.compareText("Éclair", "eclair", Locale.US))
    }

    @Test
    fun theCsvIsQuotedAndSafeForSpreadsheets() {
        val rows = listOf(
            mapOf("a" to MonarchAiJSON.Text("=SUM(A1)"), "b" to MonarchAiJSON.Text("-3")),
            mapOf("a" to MonarchAiJSON.Text("say \"hi\", then"), "b" to MonarchAiJSON.Number(2.5)),
        )
        val columns = listOf(MonarchAiTableColumn("a", label = "A"), MonarchAiTableColumn("b"))
        assertEquals(
            "A,b\r\n'=SUM(A1),-3\r\n\"say \"\"hi\"\", then\",2.5",
            MonarchAiTableAnswerData.csv(rows, MonarchAiTableAnswerData.columns(columns, rows)),
        )
    }

    @Test
    fun theColoursComeFromTheThemeInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.aiStrong, MonarchAiTableAnswerTokens.eyebrowColor(c), theme.id)
            assertEquals(c.bgSubtle, MonarchAiTableAnswerTokens.headerBackground(c), theme.id)
            assertEquals(c.borderSubtle, MonarchAiTableAnswerTokens.border(c), theme.id)
            assertEquals(c.textSubtle, MonarchAiTableAnswerTokens.cellColor(blank = true, c), theme.id)
            assertEquals(c.textDefault, MonarchAiTableAnswerTokens.chevronColor(active = true, c), theme.id)
        }
    }
}
