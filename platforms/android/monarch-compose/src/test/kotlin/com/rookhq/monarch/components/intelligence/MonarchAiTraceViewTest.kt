@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aitraceview.MonarchAiTraceFilter
import com.rookhq.monarch.components.intelligence.aitraceview.MonarchAiTraceSpan
import com.rookhq.monarch.components.intelligence.aitraceview.MonarchAiTraceSpanKind
import com.rookhq.monarch.components.intelligence.aitraceview.MonarchAiTraceSpanStatus
import com.rookhq.monarch.components.intelligence.aitraceview.MonarchAiTraceView
import com.rookhq.monarch.components.intelligence.aitraceview.MonarchAiTraceViewColors
import com.rookhq.monarch.components.intelligence.aitraceview.MonarchAiTraceViewDetailsContent
import com.rookhq.monarch.components.intelligence.aitraceview.MonarchAiTraceViewFormat
import com.rookhq.monarch.components.intelligence.aitraceview.MonarchAiTraceViewLabels
import com.rookhq.monarch.components.intelligence.aitraceview.MonarchAiTraceViewTexts
import com.rookhq.monarch.components.intelligence.aitraceview.MonarchAiTraceViewTokens
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
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val Run = MonarchAiTraceSpan(
    "run",
    MonarchAiTraceSpanKind.Agent,
    "Answer",
    MonarchAiTraceSpanStatus.Ok,
    0,
    4_200,
)
private val Plan = MonarchAiTraceSpan(
    "plan",
    MonarchAiTraceSpanKind.Model,
    "Plan",
    MonarchAiTraceSpanStatus.Ok,
    100,
    1_300,
    parentId = "run",
    inputTokens = 1_200,
    outputTokens = 180,
    costMicros = 4_200,
    model = "large-1",
    messageId = "m1",
)
private val Search = MonarchAiTraceSpan(
    "search",
    MonarchAiTraceSpanKind.Tool,
    "search_documents",
    MonarchAiTraceSpanStatus.Error,
    1_400,
    2_000,
    parentId = "run",
    error = "Timed out",
    attributes = mapOf("query" to "invoices"),
    input = "{\"q\":\"invoices\"}",
)
private val Spans = listOf(Search, Run, Plan)
private val RunningStatus = MonarchAiTraceSpanStatus.Running

private val Chevron: @androidx.compose.runtime.Composable () -> Unit = {
    MonarchIcon(TestIcon, contentDescription = null)
}

internal object MonarchAiTraceViewFixtures : ComponentFixtures {
    override val id = "ai-trace-view"
    override val fixtures = listOf(
        fixture("run with an error") {
            MonarchAiTraceView(spans = listOf(Run, Search), expandIcon = Chevron, locale = Locale.US)
        },
        fixture("running span") {
            MonarchAiTraceView(
                spans = listOf(
                    MonarchAiTraceSpan("r", MonarchAiTraceSpanKind.Retrieval, "fetch", RunningStatus, 0),
                ),
                expandIcon = Chevron,
                now = 2_000,
            )
        },
        fixture("closed and slowest first") {
            MonarchAiTraceView(
                spans = Spans,
                expandIcon = Chevron,
                expanded = emptySet(),
                filter = MonarchAiTraceFilter.Slowest,
            )
        },
        fixture("errors only without errors") {
            MonarchAiTraceView(spans = listOf(Run), expandIcon = Chevron, filter = MonarchAiTraceFilter.Errors)
        },
        fixture("empty") { MonarchAiTraceView(spans = emptyList(), expandIcon = Chevron) },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiTraceViewTest {
    @get:Rule
    val rule = createComposeRule()

    private fun texts() = MonarchAiTraceViewTexts(
        MonarchAiTraceViewLabels(),
        RuntimeEnvironment.getApplication().resources,
        Locale.US,
        "USD",
    )

    @Test
    fun totalsAndSpansAreReadInWords() {
        rule.setMonarchContent { MonarchAiTraceView(spans = Spans, expandIcon = Chevron, locale = Locale.US) }
        rule.onNodeWithText("Run trace").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithContentDescription("Total time, 4.2 s").assertExists()
        rule.onNodeWithContentDescription("Tokens, 1,380").assertExists()
        rule.onNodeWithContentDescription("Cost, $0.0042").assertExists()
        rule.onNodeWithContentDescription("Errors, 1").assertExists()
        rule.onNodeWithContentDescription(
            "Plan, Model, 1,380 tokens, $0.0042, Starts at +100 ms, takes 1.2 s",
        ).assertExists()
        rule.onNodeWithContentDescription(
            "search_documents, Error, Tool, Starts at +1.4 s, takes 600 ms",
        ).assertExists()
    }

    @Test
    fun aSpanOpensItsDetailsAndTheSelectionIsReported() {
        val selections = mutableListOf<String?>()
        rule.setMonarchContent {
            MonarchAiTraceView(spans = Spans, expandIcon = Chevron, onSelectionChange = { selections += it })
        }
        val row = rule.onNodeWithContentDescription("search_documents", substring = true)
        row.assertTouchTargetAtLeast48dp(rule.density).performClick()
        row.assertIsSelected()
        assertEquals(listOf<String?>("search"), selections)
        rule.onNodeWithText("Timed out", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Close details").performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(listOf("search", null), selections)
    }

    @Test
    fun detailsShowFactsErrorAttributesInputAndShowInConversation() {
        val opened = mutableListOf<String>()
        rule.setMonarchContent {
            val totals = MonarchAiTraceViewTokens.totals(Spans, null, "USD")
            androidx.compose.foundation.layout.Column {
                MonarchAiTraceViewDetailsContent(Plan, totals, texts(), { opened += it.id }, {})
                MonarchAiTraceViewDetailsContent(Search, totals, texts(), null, {})
            }
        }
        rule.onNodeWithText("large-1").assertExists()
        rule.onNodeWithText("Timed out", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("invoices").assertExists()
        rule.onNodeWithText("{\"q\":\"invoices\"}").assertExists()
        rule.onNodeWithText("Show in conversation").performScrollTo()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(listOf("plan"), opened)
    }

    @Test
    fun togglesFiltersAndTheirHoistedState() {
        var open by mutableStateOf(setOf("run"))
        val filters = mutableListOf<MonarchAiTraceFilter>()
        rule.setMonarchContent {
            MonarchAiTraceView(
                spans = Spans,
                expandIcon = Chevron,
                expanded = open,
                onExpandedChange = { open = it },
                onFilterChange = { filters += it },
            )
        }
        rule.onNodeWithContentDescription("Collapse Answer").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(emptySet(), open)
        rule.onNodeWithContentDescription("Plan", substring = true).assertDoesNotExist()
        rule.onNodeWithContentDescription("Expand Answer").performClick()
        rule.onNodeWithText("Errors only").performClick()
        assertEquals(listOf(MonarchAiTraceFilter.Errors), filters)
        rule.onNodeWithContentDescription("Plan", substring = true).assertDoesNotExist()
        rule.onNodeWithContentDescription("search_documents", substring = true).assertExists()
    }

    @Test
    fun emptyTraceNoErrorsAndLabels() {
        rule.setMonarchContent {
            androidx.compose.foundation.layout.Column {
                MonarchAiTraceView(spans = emptyList(), expandIcon = Chevron)
                MonarchAiTraceView(
                    spans = listOf(Run),
                    expandIcon = Chevron,
                    filter = MonarchAiTraceFilter.Errors,
                    labels = MonarchAiTraceViewLabels(noErrors = "Aucune erreur", formatDuration = { "${it}ms" }),
                )
            }
        }
        rule.onNodeWithText("No spans recorded").assertExists()
        rule.onNodeWithText("Aucune erreur").assertExists()
        rule.onNodeWithContentDescription("Total time, 4200ms").assertExists()
    }

    @Test
    fun theToggleSitsBeforeTheSpanRightToLeftAndNamesWrap() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl, fontScale = LargestFontScale) {
            MonarchAiTraceView(spans = listOf(Run, Plan), expandIcon = Chevron)
        }
        rule.onNodeWithContentDescription("Collapse Answer")
            .assertIsBefore(rule.onNodeWithText("Answer", useUnmergedTree = true), LayoutDirection.Rtl)
        rule.onNodeWithText("Run trace").assertTextFullyVisible()
    }

    @Test
    fun logicBuildsTheTreeTotalsAndFormats() {
        val tokens = MonarchAiTraceViewTokens
        val totals = tokens.totals(Spans, now = null, currency = "USD")
        assertEquals(4_200L, totals.duration)
        assertEquals(1_380L, totals.totalTokens)
        assertEquals(listOf("USD" to 4_200L), totals.costs)
        assertEquals(1, totals.errorCount)
        val running = MonarchAiTraceSpan("x", MonarchAiTraceSpanKind.Other, "x", MonarchAiTraceSpanStatus.Running, 0)
        assertEquals(9_000L, tokens.totals(listOf(running), 9_000, "USD").duration)
        assertEquals(setOf("run", "search"), tokens.defaultExpanded(Spans))
        fun ids(filter: MonarchAiTraceFilter) = tokens.lines(Spans, filter, setOf("run"), 4_200).map { it.span.id }
        assertEquals(listOf("run", "plan", "search"), ids(MonarchAiTraceFilter.All))
        assertEquals(listOf("run", "plan", "search"), ids(MonarchAiTraceFilter.Slowest))
        assertEquals(listOf("run", "search"), ids(MonarchAiTraceFilter.Errors))
        val ok = MonarchAiTraceSpanStatus.Ok
        val loopA = MonarchAiTraceSpan("a", MonarchAiTraceSpanKind.Tool, "a", ok, 0, 1, parentId = "b")
        val loopB = MonarchAiTraceSpan("b", MonarchAiTraceSpanKind.Tool, "b", ok, 1, 2, parentId = "a")
        assertEquals(2, tokens.lines(listOf(loopA, loopB), MonarchAiTraceFilter.All, emptySet(), 2).size)
        assertTrue(tokens.opensPart(Plan))
        assertFalse(tokens.opensPart(Search))
        val resources = RuntimeEnvironment.getApplication().resources
        val format = MonarchAiTraceViewFormat
        assertEquals("850 ms", format.duration(850, Locale.US, resources))
        assertEquals("1.42 s", format.duration(1_420, Locale.US, resources))
        assertEquals("12.5 s", format.duration(12_500, Locale.US, resources))
        assertEquals("2 min 5 s", format.duration(125_000, Locale.US, resources))
        assertEquals("1,42 s", format.duration(1_420, Locale.GERMANY, resources))
        assertEquals("$1.25", format.cost(1_250_000, "USD", Locale.US))
        assertEquals("$0.0042", format.cost(4_200, "USD", Locale.US))
        assertTrue(format.cost(1_000_000, "XYZ", Locale.US).endsWith("XYZ"))
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.aiSolid, MonarchAiTraceViewColors.barColor(Plan, colors))
            assertEquals(colors.dangerSolid, MonarchAiTraceViewColors.barColor(Search, colors))
            assertEquals(colors.aiThinking, MonarchAiTraceViewColors.barColor(running, colors))
            assertEquals(colors.textDefault, MonarchAiTraceViewColors.barColor(Run, colors))
            assertEquals(colors.bgMuted, MonarchAiTraceViewColors.trackColor(colors))
        }
    }
}
