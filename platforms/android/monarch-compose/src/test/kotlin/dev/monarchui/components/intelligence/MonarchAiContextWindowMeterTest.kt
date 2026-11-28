package dev.monarchui.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.components.intelligence.aiconversation.MonarchAiUsage
import dev.monarchui.components.intelligence.aicontextwindowmeter.MonarchAiContextWindowMeter
import dev.monarchui.components.intelligence.aicontextwindowmeter.MonarchAiContextWindowMeterLabels
import dev.monarchui.components.intelligence.aicontextwindowmeter.MonarchAiContextWindowMeterTokens
import dev.monarchui.components.intelligence.aicontextwindowmeter.MonarchAiContextWindowPart
import dev.monarchui.components.intelligence.aicontextwindowmeter.MonarchAiContextWindowState
import dev.monarchui.foundation.MonarchTone
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

private val Parts = listOf(
    MonarchAiContextWindowPart("Instructions", 4_000),
    MonarchAiContextWindowPart("Files", 36_000),
    MonarchAiContextWindowPart("Messages", 40_000),
)

internal object MonarchAiContextWindowMeterFixtures : ComponentFixtures {
    override val id = "ai-context-window-meter"
    override val fixtures = listOf(
        fixture("ok with summarize") { MonarchAiContextWindowMeter(used = 45_000, limit = 128_000, onSummarize = {}) },
        fixture("warning, approximate") {
            MonarchAiContextWindowMeter(used = 110_000, limit = 128_000, approximate = true, onSummarize = {})
        },
        fixture("full, summarizing") {
            MonarchAiContextWindowMeter(used = 130_000, limit = 128_000, summarizing = true, onSummarize = {})
        },
        fixture("breakdown with other") {
            MonarchAiContextWindowMeter(used = 90_000, limit = 128_000, breakdown = Parts)
        },
        fixture("compact") { MonarchAiContextWindowMeter(used = 120_000, limit = 128_000, compact = true) },
        fixture("no limit") { MonarchAiContextWindowMeter(used = -5, limit = 0) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiContextWindowMeterTest {
    @get:Rule
    val rule = createComposeRule()

    private val liveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    @Test
    fun theHeaderAndBarAreOneElementWithTheSummaryAndARange() {
        rule.setMonarchContent {
            MonarchAiContextWindowMeter(used = 45_000, limit = 128_000, modifier = Modifier.testTag("meter"))
        }
        val range = ProgressBarRangeInfo(0.3515625f, 0f..1f)
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, range))
            .assertTextContains("Context window").assertTextContains("45,000 of 128,000 tokens (35%)")
    }

    @Test
    fun anEstimateReadsAboutAndTheSummaryCanBeReplaced() {
        rule.setMonarchContent {
            androidx.compose.foundation.layout.Column {
                MonarchAiContextWindowMeter(used = 1_000, limit = 4_000, approximate = true)
                MonarchAiContextWindowMeter(
                    used = 1_000,
                    limit = 4_000,
                    labels = MonarchAiContextWindowMeterLabels(
                        label = "Fenêtre",
                        amount = { used, limit, percent -> "$used sur $limit ($percent)" },
                    ),
                )
            }
        }
        rule.onNodeWithText("About 1,000 of 4,000 tokens (25%)", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("1,000 sur 4,000 (25%)", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Fenêtre", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theUsageOfTheLatestReplyIsInputPlusOutput() {
        rule.setMonarchContent {
            MonarchAiContextWindowMeter(
                usage = MonarchAiUsage(inputTokens = 3_000, outputTokens = 1_000),
                limit = 8_000,
            )
        }
        rule.onNodeWithText("4,000 of 8,000 tokens (50%)", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theWarningAndFullStatesAreWrittenNotOnlyColoured() {
        rule.setMonarchContent {
            androidx.compose.foundation.layout.Column {
                MonarchAiContextWindowMeter(used = 90, limit = 100)
                MonarchAiContextWindowMeter(used = 100, limit = 100)
            }
        }
        rule.onNodeWithText("nearly at its context limit", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("The context window is full", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun theFirstStateIsNotAnnouncedButAChangeIs() {
        var used by mutableLongStateOf(10L)
        rule.setMonarchContent { MonarchAiContextWindowMeter(used = used, limit = 100) }
        rule.onNode(liveRegion, useUnmergedTree = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
        used = 85
        rule.waitForIdle()
        rule.onNode(liveRegion, useUnmergedTree = true).assertContentDescriptionEquals(
            "The conversation is nearly at its context limit. Earlier messages may be left out.",
        )
        used = 120
        rule.waitForIdle()
        rule.onNode(liveRegion, useUnmergedTree = true)
            .assertContentDescriptionEquals("The context window is full. Earlier messages are being left out.")
    }

    @Test
    fun aMeterThatStartsFullIsNotAnnouncedOnAppearance() {
        rule.setMonarchContent { MonarchAiContextWindowMeter(used = 200, limit = 100) }
        rule.onNode(liveRegion, useUnmergedTree = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }

    @Test
    fun summarizeCallsBackAndIgnoresTapsWhileSummarizing() {
        var calls = 0
        var summarizing by androidx.compose.runtime.mutableStateOf(false)
        rule.setMonarchContent {
            MonarchAiContextWindowMeter(
                used = 10,
                limit = 100,
                summarizing = summarizing,
                onSummarize = { calls++ },
                labels = MonarchAiContextWindowMeterLabels(summarize = "Compress history"),
            )
        }
        rule.onNodeWithText("Compress history").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, calls)
        summarizing = true
        rule.waitForIdle()
        rule.onNodeWithText("Compress history", useUnmergedTree = true).performClick()
        assertEquals(1, calls)
    }

    @Test
    fun theLegendNamesEveryPartWithItsTokensAndOther() {
        rule.setMonarchContent { MonarchAiContextWindowMeter(used = 90_000, limit = 128_000, breakdown = Parts) }
        rule.onNodeWithContentDescription("What fills the context window").assertExists()
        rule.onNodeWithText("Other", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("10,000 tokens", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("36,000 tokens", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun theCompactMeterIsAButtonThatOpensTheDetails() {
        rule.setMonarchContent {
            MonarchAiContextWindowMeter(used = 64_000, limit = 128_000, compact = true)
        }
        val button = rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        button.assertContentDescriptionEquals("Context window").assertTouchTargetAtLeast48dp(rule.density)
        assertEquals("64,000 of 128,000 tokens (50%)", button.stateDescription())
        button.performClick()
        rule.onNodeWithText("64,000 of 128,000 tokens (50%)", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theHeadingIsAtTheStartInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiContextWindowMeter(used = 1, limit = 1_000_000)
        }
        val summary = rule.onNodeWithText("1 of 1,000,000 tokens (0%)", useUnmergedTree = true)
        rule.onNodeWithText("Context window", useUnmergedTree = true).assertIsBefore(summary, LayoutDirection.Rtl)
    }

    @Test
    fun theSummaryWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiContextWindowMeter(used = 45_000, limit = 128_000)
        }
        rule.onNodeWithText("45,000 of 128,000 tokens (35%)", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theStateFollowsTheShareOfTheLimit() {
        val of = MonarchAiContextWindowState.Companion::of
        assertEquals(MonarchAiContextWindowState.Ok, of(79, 100, 0.8))
        assertEquals(MonarchAiContextWindowState.Warning, of(80, 100, 0.8))
        assertEquals(MonarchAiContextWindowState.Full, of(100, 100, 0.8))
        assertEquals(MonarchAiContextWindowState.Ok, of(500, 0, 0.8))
        assertEquals(MonarchAiContextWindowState.Ok, of(-5, 100, 0.8))
        assertEquals(MonarchAiContextWindowState.Warning, of(50, 100, 0.5))
    }

    @Test
    fun theRulesOfTheBar() {
        val tokens = MonarchAiContextWindowMeterTokens
        assertEquals(0f, tokens.share(10, 0))
        assertEquals(1f, tokens.share(300, 100))
        assertEquals(0f, tokens.share(-3, 100))
        val segments = tokens.segments(90_000, Parts, "Other")
        assertEquals(listOf("Instructions", "Files", "Messages", "Other"), segments.map { it.label })
        assertEquals(10_000.0, segments.last().value)
        assertEquals(MonarchChartColor.Neutral, segments.last().color)
        assertEquals(3, tokens.segments(50_000, Parts, "Other").size)
        assertEquals(128_000.0, tokens.total(90_000, 128_000, Parts))
        assertEquals(80_000.0, tokens.total(10, 1_000, Parts))
        assertNull(tokens.noticeTone(MonarchAiContextWindowState.Ok))
        assertEquals(MonarchTone.Danger, tokens.noticeTone(MonarchAiContextWindowState.Full))
    }

    @Test
    fun theColoursComeFromTheThemeInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchAiContextWindowMeterTokens
            assertEquals(colors.accentSolid, tokens.fill(MonarchAiContextWindowState.Ok, colors))
            assertEquals(colors.warningSolid, tokens.fill(MonarchAiContextWindowState.Warning, colors))
            assertEquals(colors.dangerSolid, tokens.fill(MonarchAiContextWindowState.Full, colors))
            assertEquals(colors.bgMuted, tokens.track(colors))
            assertEquals(colors.textMuted, tokens.compactText(MonarchAiContextWindowState.Ok, colors))
            assertEquals(colors.dangerStrong, tokens.compactText(MonarchAiContextWindowState.Full, colors))
        }
    }
}
