package com.rookhq.monarch.components.charts

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.stackedproportionbar.MonarchStackedProportionBar
import com.rookhq.monarch.components.charts.stackedproportionbar.MonarchStackedProportionBarGeometry
import com.rookhq.monarch.components.charts.stackedproportionbar.MonarchStackedProportionBarLegendContent
import com.rookhq.monarch.components.charts.stackedproportionbar.MonarchStackedProportionBarSegment
import com.rookhq.monarch.components.charts.stackedproportionbar.MonarchStackedProportionBarSize
import com.rookhq.monarch.components.charts.stackedproportionbar.MonarchStackedProportionBarTokens
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val pipeline = listOf(
    MonarchStackedProportionBarSegment("new", "New", 1240.0),
    MonarchStackedProportionBarSegment("campaign", "In a campaign", 610.0),
    MonarchStackedProportionBarSegment("replied", "Replied", 182.0),
    MonarchStackedProportionBarSegment("converted", "Converted", 41.0),
)
private val monitors = listOf(
    MonarchStackedProportionBarSegment("up", "Operational", 41.0, MonarchTone.Success),
    MonarchStackedProportionBarSegment("slow", "Degraded", 3.0, MonarchTone.Warning),
    MonarchStackedProportionBarSegment("down", "Down", 1.0, MonarchTone.Danger),
)
private val storage = listOf(
    MonarchStackedProportionBarSegment("media", "Media", 42.0),
    MonarchStackedProportionBarSegment("docs", "Documents", 18.0),
)
private val count: (Double) -> String = { it.roundToInt().toString() }

internal object MonarchStackedProportionBarFixtures : ComponentFixtures {
    override val id = "stacked-proportion-bar"
    override val fixtures = listOf(
        fixture("pipeline, value and share") {
            MonarchStackedProportionBar(label = "Lead pipeline", segments = pipeline)
        },
        fixture("capacity with a remainder, large") {
            MonarchStackedProportionBar(
                label = "Storage",
                segments = storage,
                total = 100.0,
                remainderLabel = "Free",
                legendContent = MonarchStackedProportionBarLegendContent.Value,
                size = MonarchStackedProportionBarSize.Lg,
                formatValue = { "${it.roundToInt()} GB" },
            )
        },
        fixture("status mode, small, shares only") {
            MonarchStackedProportionBar(
                label = "Monitors",
                segments = monitors,
                legendContent = MonarchStackedProportionBarLegendContent.Percent,
                size = MonarchStackedProportionBarSize.Sm,
            )
        },
        fixture("no legend") {
            MonarchStackedProportionBar(label = "Lead pipeline", segments = pipeline, showLegend = false)
        },
        fixture("labels only, zeros kept, long names") {
            MonarchStackedProportionBar(
                label = "Tickets",
                segments = listOf(
                    MonarchStackedProportionBarSegment("a", "Waiting for a reply from the customer", 12.0),
                    MonarchStackedProportionBarSegment("b", "Escalated to the second line", 0.0),
                    MonarchStackedProportionBarSegment("c", "Closed", 88.0, MonarchChartColor.Data5),
                ),
                legendContent = MonarchStackedProportionBarLegendContent.None,
                hideZero = false,
            )
        },
        fixture("empty") {
            MonarchStackedProportionBar(
                label = "Lead pipeline",
                segments = listOf(MonarchStackedProportionBarSegment("a", "A", 0.0)),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchStackedProportionBarTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachLegendEntryReadsItsNameValueAndShare() {
        rule.setMonarchContent {
            MonarchStackedProportionBar(label = "Lead pipeline", segments = pipeline, formatValue = count)
        }
        rule.onNodeWithContentDescription("New 1240 · 60%").assertExists()
        rule.onNodeWithContentDescription("Replied 182 · 9%").assertExists()
        rule.onNodeWithContentDescription("Converted 41 · 2%").assertExists()
        rule.onNodeWithText("1240 · 60%", useUnmergedTree = true).assertExists()
        val legend = rule.onNodeWithContentDescription("Lead pipeline")
        legend.assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
        assertNull(legend.stateDescription(), "the bar is hidden while the legend lists the parts")
    }

    @Test
    fun withoutTheLegendTheBarReadsEveryPart() {
        rule.setMonarchContent {
            MonarchStackedProportionBar(
                label = "Lead pipeline",
                segments = pipeline,
                showLegend = false,
                formatValue = count,
            )
        }
        assertEquals(
            "New 1240 · 60%, In a campaign 610 · 29%, Replied 182 · 9%, Converted 41 · 2%",
            rule.onNodeWithContentDescription("Lead pipeline").stateDescription(),
        )
        rule.onNodeWithText("New", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun aCapacityListsItsRemainderWhenNamed() {
        rule.setMonarchContent {
            MonarchStackedProportionBar(
                label = "Storage",
                segments = storage,
                total = 100.0,
                remainderLabel = "Free",
                legendContent = MonarchStackedProportionBarLegendContent.Value,
                formatValue = count,
            )
        }
        rule.onNodeWithContentDescription("Media 42 · 42%").assertExists()
        rule.onNodeWithContentDescription("Free 40 · 40%").assertExists()
        rule.onNodeWithText("40", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("40 · 40%", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun theLegendCanShowOnlyTheShareOrOnlyTheLabel() {
        rule.setMonarchContent {
            MonarchStackedProportionBar(
                label = "Monitors",
                segments = monitors,
                legendContent = MonarchStackedProportionBarLegendContent.Percent,
                formatValue = count,
            )
        }
        rule.onNodeWithText("91%", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Down 1 · 2%").assertExists()
    }

    @Test
    fun withNoValuesOneElementReadsTheLabelAndNoData() {
        rule.setMonarchContent {
            MonarchStackedProportionBar(
                label = "Lead pipeline",
                segments = listOf(MonarchStackedProportionBarSegment("a", "A", -3.0)),
            )
        }
        assertEquals("No data yet", rule.onNodeWithContentDescription("Lead pipeline").stateDescription())
    }

    @Test
    fun theEmptyTextCanBeReplaced() {
        rule.setMonarchContent {
            MonarchStackedProportionBar(label = "Pipeline", segments = emptyList(), emptyLabel = "Noch keine Daten")
        }
        assertEquals("Noch keine Daten", rule.onNodeWithContentDescription("Pipeline").stateDescription())
    }

    @Test
    fun zerosAreHiddenUnlessAsked() {
        val parts = listOf(
            MonarchStackedProportionBarSegment("a", "Open", 3.0),
            MonarchStackedProportionBarSegment("b", "Paused", 0.0),
        )
        rule.setMonarchContent {
            MonarchStackedProportionBar(label = "Tickets", segments = parts, formatValue = count)
        }
        rule.onNodeWithContentDescription("Paused 0 · 0%").assertDoesNotExist()
        rule.onNodeWithContentDescription("Open 3 · 100%").assertExists()
    }

    @Test
    fun aKeptZeroIsListed() {
        val parts = listOf(
            MonarchStackedProportionBarSegment("a", "Open", 3.0),
            MonarchStackedProportionBarSegment("b", "Paused", 0.0),
        )
        rule.setMonarchContent {
            MonarchStackedProportionBar(label = "Tickets", segments = parts, hideZero = false, formatValue = count)
        }
        rule.onNodeWithContentDescription("Paused 0 · 0%").assertExists()
    }

    @Test
    fun tinySharesAreNeverRoundedAway() {
        rule.setMonarchContent {
            MonarchStackedProportionBar(
                label = "Uptime",
                segments = listOf(
                    MonarchStackedProportionBarSegment("up", "Up", 9999.0),
                    MonarchStackedProportionBarSegment("down", "Down", 1.0),
                ),
                formatValue = count,
            )
        }
        rule.onNodeWithContentDescription("Down 1 · <1%").assertExists()
        rule.onNodeWithContentDescription("Up 9999 · >99%").assertExists()
    }

    @Test
    fun percentDigitsAddDecimals() {
        rule.setMonarchContent {
            MonarchStackedProportionBar(label = "Pipeline", segments = pipeline, percentDigits = 1, formatValue = count)
        }
        rule.onNodeWithContentDescription("New 1240 · 59.8%").assertExists()
    }

    @Test
    fun theLegendRunsFromTheStartEdge() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchStackedProportionBar(label = "Storage", segments = storage, formatValue = count)
        }
        rule.onNodeWithContentDescription("Media 42 · 70%")
            .assertIsBefore(rule.onNodeWithContentDescription("Documents 18 · 30%"), LayoutDirection.Rtl)
    }

    @Test
    fun longLabelsWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchStackedProportionBar(
                label = "Tickets",
                segments = listOf(
                    MonarchStackedProportionBarSegment("a", "Waiting for a reply from the customer", 12.0),
                    MonarchStackedProportionBarSegment("b", "Closed", 88.0),
                ),
            )
        }
        rule.onNodeWithText("Waiting for a reply from the customer", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun partsKeepTheSlotOfTheirPositionAndToneOnlyInStatusMode() {
        val withZero = listOf(
            MonarchStackedProportionBarSegment("a", "A", 0.0),
            MonarchStackedProportionBarSegment("b", "B", 2.0),
            MonarchStackedProportionBarSegment("c", "C", -1.0, MonarchChartColor.Data5),
        )
        val model = MonarchStackedProportionBarGeometry.model(withZero, total = null, hideZero = true)
        assertEquals(listOf("b"), model.parts.map { it.key })
        assertEquals(MonarchChartColor.Data2, model.parts.single().slot)
        val kept = MonarchStackedProportionBarGeometry.model(withZero, total = null, hideZero = false)
        assertEquals(listOf(0.0, 2.0, 0.0), kept.parts.map { it.value })
        assertEquals(MonarchChartColor.Data5, kept.parts.last().slot)
        val status = MonarchStackedProportionBarGeometry.model(monitors, total = null, hideZero = true)
        assertEquals(listOf(MonarchTone.Success, MonarchTone.Warning, MonarchTone.Danger), status.parts.map { it.tone })
        val mixed = MonarchStackedProportionBarGeometry.model(
            monitors + pipeline.first(),
            total = null,
            hideZero = true,
        )
        assertTrue(mixed.parts.all { it.tone == null }, "a partial set of tones is ignored")
    }

    @Test
    fun theWholeIsTheLargerOfTheTotalAndTheSum() {
        val capacity = MonarchStackedProportionBarGeometry.model(storage, total = 100.0, hideZero = true)
        assertEquals(100.0, capacity.whole)
        assertEquals(40.0, capacity.remainder)
        val smaller = MonarchStackedProportionBarGeometry.model(storage, total = 10.0, hideZero = true)
        assertEquals(60.0, smaller.whole)
        assertEquals(0.0, smaller.remainder)
        assertTrue(MonarchStackedProportionBarGeometry.model(emptyList(), total = 5.0, hideZero = true).empty)
    }

    @Test
    fun spansShareTheWidthKeepAMinimumAndLeaveTheRest() {
        fun near(expected: Pair<Float, Float>, actual: Pair<Float, Float>) {
            val close = abs(expected.first - actual.first) < 0.01f && abs(expected.second - actual.second) < 0.01f
            assertTrue(close, "expected $expected, was $actual")
        }
        val two = MonarchStackedProportionBarGeometry.spans(listOf(3.0, 1.0), 4.0, 100f, 4f, 2f)
        near(0f to 73.5f, two[0])
        near(75.5f to 24.5f, two[1])
        val rest = MonarchStackedProportionBarGeometry.spans(listOf(1.0, 1.0), 4.0, 102f, 4f, 2f)
        near(0f to 24.5f, rest[0])
        near(26.5f to 24.5f, rest[1])
        val tiny = MonarchStackedProportionBarGeometry.spans(listOf(100.0, 1.0), 101.0, 100f, 4f, 0f)
        near(0f to 96f, tiny[0])
        near(96f to 4f, tiny[1])
        val zero = MonarchStackedProportionBarGeometry.spans(listOf(1.0, 0.0, 1.0), 2.0, 100f, 4f, 0f)
        assertEquals(listOf(0f to 50f, 50f to 0f, 50f to 50f), zero)
        assertEquals(listOf(0f to 0f), MonarchStackedProportionBarGeometry.spans(listOf(0.0), 0.0, 100f, 4f, 2f))
    }

    @Test
    fun coloursComeFromTheThemeInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchStackedProportionBarTokens
            assertEquals(colors.data3, tokens.partColor(MonarchChartColor.Data3, null, colors), theme.id)
            assertEquals(colors.successSolid, tokens.partColor(MonarchChartColor.Data1, MonarchTone.Success, colors))
            assertEquals(colors.warningSolid, tokens.partColor(MonarchChartColor.Data1, MonarchTone.Warning, colors))
            assertEquals(colors.dangerSolid, tokens.partColor(MonarchChartColor.Data1, MonarchTone.Danger, colors))
            assertEquals(colors.infoSolid, tokens.partColor(MonarchChartColor.Data1, MonarchTone.Info, colors))
            assertEquals(colors.textSubtle, tokens.partColor(MonarchChartColor.Data1, MonarchTone.Neutral, colors))
            assertEquals(colors.bgMuted, tokens.track(colors), theme.id)
            assertEquals(colors.borderStrong, tokens.remainderMarker(colors), theme.id)
            assertEquals(colors.textMuted, tokens.emptyText(colors), theme.id)
        }
    }
}
