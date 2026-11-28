package dev.monarchui.components.intelligence

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.intelligence.aiagentruntimeline.MonarchAiAgentRunTimeline
import dev.monarchui.components.intelligence.aiagentruntimeline.MonarchAiAgentRunTimelineFormat
import dev.monarchui.components.intelligence.aiagentruntimeline.MonarchAiAgentRunTimelineLabels
import dev.monarchui.components.intelligence.aiagentruntimeline.MonarchAiAgentRunTimelineMode
import dev.monarchui.components.intelligence.aiagentruntimeline.MonarchAiAgentRunTimelineTokens
import dev.monarchui.components.intelligence.aiagentruntimeline.MonarchAiRunEvent
import dev.monarchui.components.intelligence.aiagentruntimeline.MonarchAiRunEventKind
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val T0 = 1_700_000_000_000L

private val Events = listOf(
    MonarchAiRunEvent(
        "2",
        MonarchAiRunEventKind.Tool,
        "search_invoices",
        T0 + 1_000,
        T0 + 2_600,
        detail = "{\"query\":\"unpaid\"}",
        partId = "part_7",
    ),
    MonarchAiRunEvent(
        "1",
        MonarchAiRunEventKind.Model,
        "plan",
        T0,
        T0 + 900,
        inputTokens = 1_200,
        outputTokens = 340,
        costMicros = 4_200,
    ),
    MonarchAiRunEvent("3", MonarchAiRunEventKind.Error, "send_email", T0 + 2_700, T0 + 2_900),
)

internal object MonarchAiAgentRunTimelineFixtures : ComponentFixtures {
    override val id = "ai-agent-run-timeline"
    override val fixtures = listOf(
        fixture("timeline") { MonarchAiAgentRunTimeline(events = Events.take(2)) },
        fixture("running event and error") {
            MonarchAiAgentRunTimeline(
                events = listOf(
                    Events[2],
                    MonarchAiRunEvent("4", MonarchAiRunEventKind.Handoff, "billing_agent", T0 + 3_000),
                ),
                now = T0 + 4_000,
            )
        },
        fixture("table") {
            MonarchAiAgentRunTimeline(events = Events.take(2), initialMode = MonarchAiAgentRunTimelineMode.Table)
        },
        fixture("empty") { MonarchAiAgentRunTimeline(events = emptyList()) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiAgentRunTimelineTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun totalsAndEventsAreReadInWordsInStartOrder() {
        rule.setMonarchContent { MonarchAiAgentRunTimeline(events = Events) }
        rule.onNodeWithContentDescription("Total time, 2.9 s").assertExists()
        rule.onNodeWithContentDescription("Input tokens, 1,200").assertExists()
        rule.onNodeWithContentDescription("Cost, $0.0042").assertExists()
        rule.onNodeWithText("Agent run").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        val model = rule.onNodeWithContentDescription("Model, plan, 900 ms")
        val tool = rule.onNodeWithContentDescription("Tool, search_invoices, 1.6 s")
        assertTrue(model.fetchSemanticsNode().boundsInRoot.top < tool.fetchSemanticsNode().boundsInRoot.top)
        rule.onNodeWithContentDescription("Error, send_email, 200 ms").assertExists()
    }

    @Test
    fun anEventOpensWithItsFactsAndShowInConversation() {
        val opened = mutableListOf<String>()
        rule.setMonarchContent { MonarchAiAgentRunTimeline(events = Events, onOpenPart = { opened += it.id }) }
        val tool = rule.onNodeWithContentDescription("Tool, search_invoices, 1.6 s")
        assertEquals("Collapsed", tool.stateDescription())
        tool.assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals("Expanded", tool.stateDescription())
        rule.onNodeWithText("Start, +1.0 s").assertExists()
        rule.onNodeWithText("{\"query\":\"unpaid\"}").assertExists()
        rule.onNodeWithText("Show in conversation").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf("2"), opened)
        tool.performSemanticsAction(SemanticsActions.Collapse)
        rule.onNodeWithText("Show in conversation").assertDoesNotExist()
    }

    @Test
    fun aRunningEventEndsAtNowAndSaysRunning() {
        rule.setMonarchContent {
            MonarchAiAgentRunTimeline(
                events = listOf(MonarchAiRunEvent("1", MonarchAiRunEventKind.Model, "answer", T0)),
                now = T0 + 5_000,
            )
        }
        rule.onNodeWithContentDescription("Model, answer, Running").assertExists()
        rule.onNodeWithContentDescription("Total time, 5.0 s").assertExists()
    }

    @Test
    fun theTableReadsEachRowAsOneElementAndTheModeIsHoisted() {
        val modes = mutableListOf<MonarchAiAgentRunTimelineMode>()
        rule.setMonarchContent {
            MonarchAiAgentRunTimeline(
                events = Events,
                mode = MonarchAiAgentRunTimelineMode.Table,
                onModeChange = { modes += it },
            )
        }
        rule.onNodeWithContentDescription(
            "plan, Model, Start, +0 ms, Duration, 900 ms, Input tokens, 1,200, Output tokens, 340, Cost, $0.0042",
        ).assertExists()
        rule.onNodeWithContentDescription("send_email, Error, Start, +2.7 s, Duration, 200 ms").assertExists()
        rule.onNodeWithText("Timeline").performClick()
        assertEquals(listOf(MonarchAiAgentRunTimelineMode.Timeline), modes)
    }

    @Test
    fun emptyRunSaysSoAndLabelsReplaceStrings() {
        rule.setMonarchContent {
            MonarchAiAgentRunTimeline(
                events = emptyList(),
                labels = MonarchAiAgentRunTimelineLabels(empty = "Aucun événement", formatDuration = { "${it}ms" }),
            )
        }
        rule.onNodeWithText("Aucun événement").assertExists()
        rule.onNodeWithContentDescription("Total time, 0ms").assertExists()
    }

    @Test
    fun namesWrapAtLargeFontsRightToLeft() {
        rule.setMonarchContent(fontScale = LargestFontScale, layoutDirection = LayoutDirection.Rtl) {
            MonarchAiAgentRunTimeline(events = Events.take(1))
        }
        rule.onNodeWithText("Agent run").assertTextFullyVisible()
        rule.onNodeWithText("Timeline").assertTextFullyVisible()
    }

    @Test
    fun logicPlacesBarsSumsTotalsAndFormats() {
        val tokens = MonarchAiAgentRunTimelineTokens
        val sorted = tokens.sorted(Events)
        assertEquals(listOf("1", "2", "3"), sorted.map { it.id })
        val span = tokens.span(sorted[1], T0, T0 + 2_900)
        assertEquals(1_000f / 2_900f, span.start, 0.0001f)
        assertEquals(1_600f / 2_900f, span.length, 0.0001f)
        val backwards = MonarchAiRunEvent("x", MonarchAiRunEventKind.Tool, "x", T0 + 10, T0)
        assertEquals(0L, tokens.duration(backwards, T0 + 100))
        val totals = tokens.totals(Events, now = null)
        assertEquals(2_900L, totals.durationMillis)
        assertEquals(1_200L, totals.inputTokens)
        assertEquals(4_200L, totals.costMicros)
        assertNull(tokens.totals(Events.drop(1).takeLast(1), null).inputTokens)
        assertFalse(tokens.opensPart(Events[2], hasCallback = true))
        assertTrue(tokens.opensPart(Events[0], hasCallback = true))
        val resources = RuntimeEnvironment.getApplication().resources
        val format = MonarchAiAgentRunTimelineFormat
        assertEquals("850 ms", format.formatDuration(850, resources))
        assertEquals("4.2 s", format.formatDuration(4_200, resources))
        assertEquals("12 s", format.formatDuration(12_000, resources))
        assertEquals("2 min 5 s", format.formatDuration(125_000, resources))
        assertEquals("3 min", format.formatDuration(180_000, resources))
        assertTrue(format.formatCost(4_200_000, "XYZ").endsWith("XYZ"))
        assertEquals("$0.0042", format.formatCost(4_200, "USD"))
        assertEquals("$12.50", format.formatCost(12_500_000, "USD"))
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.aiSolid, tokens.barColor(MonarchAiRunEventKind.Model, colors))
            assertEquals(colors.aiTool, tokens.barColor(MonarchAiRunEventKind.Tool, colors))
            assertEquals(colors.infoSolid, tokens.barColor(MonarchAiRunEventKind.Handoff, colors))
            assertEquals(colors.dangerSolid, tokens.barColor(MonarchAiRunEventKind.Error, colors))
            assertEquals(colors.aiThinking, tokens.runningColor(colors))
        }
    }
}
