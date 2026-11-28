package dev.monarchui.components.datadisplay

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.charts.deltaindicator.MonarchDeltaIndicatorGoodDirection
import dev.monarchui.components.charts.deltaindicator.MonarchDeltaIndicatorSize
import dev.monarchui.components.charts.deltaindicator.MonarchDeltaIndicatorVariant
import dev.monarchui.components.datadisplay.metric.MonarchMetric
import dev.monarchui.components.datadisplay.metric.MonarchMetricChange
import dev.monarchui.components.datadisplay.metric.MonarchMetricFormat
import dev.monarchui.components.datadisplay.metric.MonarchMetricLayout
import dev.monarchui.components.datadisplay.metric.MonarchMetricSize
import dev.monarchui.components.datadisplay.metric.MonarchMetricTokens
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal object MonarchMetricFixtures : ComponentFixtures {
    override val id = "metric"
    private val down = MonarchMetricChange(-0.08, "vs last week", MonarchDeltaIndicatorGoodDirection.Down)
    override val fixtures = listOf(
        fixture("every size") {
            Column {
                MonarchMetricSize.entries.forEach {
                    MonarchMetric(label = "Latency", value = 840.0, unit = "ms", size = it)
                }
            }
        },
        fixture("with a change") {
            MonarchMetric(label = "Latency", value = 840.0, unit = "ms", change = down, size = MonarchMetricSize.Lg)
        },
        fixture("percent, currency and compact") {
            Column {
                MonarchMetric(label = "Conversion", value = 0.12, format = MonarchMetricFormat.Percent)
                MonarchMetric(
                    label = "Revenue",
                    value = 3_400_000.0,
                    format = MonarchMetricFormat.Currency("EUR"),
                    compact = true,
                )
            }
        },
        fixture("inline and end aligned") {
            Column {
                MonarchMetric(label = "Open", value = 128.0, change = down, layout = MonarchMetricLayout.Inline)
                MonarchMetric(label = "Closed", value = 64.0, alignment = Alignment.End, size = MonarchMetricSize.Sm)
            }
        },
        fixture("empty and text") {
            Column {
                MonarchMetric(label = "Errors", value = null, unit = "per hour")
                MonarchMetric(label = "Seats", text = "3 of 5", change = MonarchMetricChange.between(3.0, 2.0))
            }
        },
        fixture("long label") {
            MonarchMetric(
                label = "Median time to first response across every support queue",
                value = 1_234_567.0,
                unit = "seconds per ticket",
                size = MonarchMetricSize.Lg,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchMetricTest {
    @get:Rule
    val rule = createComposeRule()

    private val us = Locale.US

    @Test
    fun theMetricIsOneElementWithTheLabelTheValueTheUnitAndTheChange() {
        rule.setMonarchContent {
            MonarchMetric(
                label = "Latency",
                value = 840.0,
                unit = "ms",
                change = MonarchMetricChange(-0.08, "vs last week", MonarchDeltaIndicatorGoodDirection.Down),
            )
        }
        rule.onNodeWithText("Latency")
            .assert(hasText("840"))
            .assert(hasText("ms"))
            .assert(hasContentDescription("down 8% vs last week, better"))
    }

    @Test
    fun aChangeCanBeWorkedOutFromTwoValues() {
        rule.setMonarchContent {
            MonarchMetric(label = "Signups", value = 120.0, change = MonarchMetricChange.between(120.0, 100.0))
        }
        rule.onNodeWithText("Signups").assert(hasContentDescription("up 20%, better"))
    }

    @Test
    fun aCompactValueIsReadInFull() {
        rule.setMonarchContent { MonarchMetric(label = "Views", value = 12_345.0, compact = true, locale = us) }
        rule.onNodeWithText("Views").assert(hasText("12K")).assert(hasContentDescription("12,345"))
    }

    @Test
    fun aMissingOrInfiniteValueIsADashReadAsNoDataAndHidesTheUnit() {
        rule.setMonarchContent {
            Column {
                MonarchMetric(label = "Errors", value = null, unit = "per hour")
                MonarchMetric(label = "Rate", value = Double.POSITIVE_INFINITY, emptyLabel = "Not measured")
                MonarchMetric(label = "Seats", text = "")
            }
        }
        rule.onNodeWithText("Errors").assert(hasText("—")).assert(hasContentDescription("No data"))
        rule.onNodeWithText("per hour").assertDoesNotExist()
        rule.onNodeWithText("Rate").assert(hasContentDescription("Not measured"))
        rule.onNodeWithText("Seats").assert(hasContentDescription("No data"))
    }

    @Test
    fun aWrittenValueCanHaveASpokenForm() {
        rule.setMonarchContent { MonarchMetric(label = "Storage", text = "1.2K", spokenText = "1,200 files") }
        rule.onNodeWithText("Storage").assert(hasText("1.2K")).assert(hasContentDescription("1,200 files"))
    }

    @Test
    fun theFormatsFollowTheLocale() {
        assertEquals("12,345", MonarchMetricFormat.Number.format(12_345.0, false, us))
        assertEquals("12K", MonarchMetricFormat.Number.format(12_345.0, true, us))
        assertEquals("12%", MonarchMetricFormat.Percent.format(0.12, true, us))
        assertEquals("€1,234.50", MonarchMetricFormat.Currency("EUR").format(1234.5, false, us))
        assertEquals("€3.4M", MonarchMetricFormat.Currency("EUR").format(3_400_000.0, true, us))
        assertEquals("12.345", MonarchMetricFormat.Number.format(12_345.0, false, Locale.GERMANY))
    }

    @Test
    fun theShownAndSpokenRules() {
        val t = MonarchMetricTokens
        val number = MonarchMetricFormat.Number
        assertNull(t.shown(Double.NaN, number, false, us))
        assertNull(t.spoken(12_345.0, number, false, us))
        assertNull(t.spoken(12.0, number, true, us))
        assertEquals("12,345", t.spoken(12_345.0, number, true, us))
        assertEquals(MonarchDeltaIndicatorSize.Md, t.deltaSize(MonarchMetricSize.Lg))
        assertEquals(MonarchDeltaIndicatorSize.Sm, t.deltaSize(MonarchMetricSize.Md))
        MonarchMetricSize.entries.forEach {
            assertEquals("tnum", t.valueStyle(it, MonarchTypography.Default).fontFeatureSettings)
        }
    }

    @Test
    fun inlineTheLabelComesBeforeTheValueInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchMetric(
                label = "Open",
                value = 128.0,
                layout = MonarchMetricLayout.Inline,
                change = MonarchMetricChange(0.1, variant = MonarchDeltaIndicatorVariant.Subtle),
            )
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Open", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("128", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun aLongLabelAndValueWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchMetric(
                label = "Median time to first response across every support queue",
                value = 1_234_567.0,
                unit = "seconds per ticket",
                size = MonarchMetricSize.Lg,
                locale = us,
            )
        }
        rule.onNodeWithText("Median time", substring = true, useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("1,234,567", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val t = MonarchMetricTokens
            assertEquals(colors.textMuted, t.labelColor(colors), theme.id)
            assertEquals(colors.textMuted, t.unitColor(colors), theme.id)
            assertEquals(colors.textDefault, t.valueColor(false, colors), theme.id)
            assertEquals(colors.textSubtle, t.valueColor(true, colors), theme.id)
        }
    }
}
