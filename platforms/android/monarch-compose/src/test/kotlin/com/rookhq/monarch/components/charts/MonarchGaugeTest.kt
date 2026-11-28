package com.rookhq.monarch.components.charts

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.gauge.MonarchGauge
import com.rookhq.monarch.components.charts.gauge.MonarchGaugeBand
import com.rookhq.monarch.components.charts.gauge.MonarchGaugeIndicator
import com.rookhq.monarch.components.charts.gauge.MonarchGaugeScale
import com.rookhq.monarch.components.charts.gauge.MonarchGaugeSize
import com.rookhq.monarch.components.charts.gauge.MonarchGaugeTokens
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Health = listOf(
    MonarchGaugeBand(0.0..70.0, MonarchTone.Success, "Healthy"),
    MonarchGaugeBand(70.0..90.0, MonarchTone.Warning, "At risk"),
    MonarchGaugeBand(90.0..100.0, MonarchTone.Danger, "Critical"),
)

internal object MonarchGaugeFixtures : ComponentFixtures {
    override val id = "gauge"
    override val fixtures = MonarchGaugeIndicator.entries.flatMap { indicator ->
        MonarchGaugeSize.entries.map { size ->
            fixture("${indicator.name}, size ${size.name}, bands and a target") {
                MonarchGauge(
                    value = 72.0,
                    bands = Health,
                    target = 80.0,
                    indicator = indicator,
                    size = size,
                    label = "CPU",
                    description = "Last hour",
                )
            }
        }
    } + MonarchTone.entries.map { tone ->
        fixture("tone ${tone.name}") { MonarchGauge(value = 40.0, tone = tone) }
    } + listOf(
        fixture("accent, no bands") { MonarchGauge(value = 0.55, range = 0.0..1.0) },
        fixture("no value") { MonarchGauge(value = null, label = "Memory") },
        fixture("out of range") { MonarchGauge(value = 140.0, showRange = false) },
        fixture("long label") {
            MonarchGauge(value = 12.0, label = "Average response time across every region", description = "Seconds")
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchGaugeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theGaugeIsOneElementWithTheValueTheBandAndTheTarget() {
        rule.setMonarchContent {
            MonarchGauge(
                value = 72.0,
                modifier = Modifier.testTag("gauge"),
                bands = Health,
                target = 80.0,
                label = "CPU",
            )
        }
        val gauge = rule.onNodeWithTag("gauge")
            .assertContentDescriptionEquals("CPU")
            .assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(72f, 0f..100f)))
            .assertHasNoClickAction()
        assertEquals("72, At risk, Target 80", gauge.stateDescription())
    }

    @Test
    fun withoutABandTheRangeIsSpokenAndWithoutALabelTheNameIsGauge() {
        rule.setMonarchContent { MonarchGauge(value = 72.0, modifier = Modifier.testTag("gauge")) }
        val gauge = rule.onNodeWithTag("gauge").assertContentDescriptionEquals("Gauge")
        assertEquals("72 (from 0 to 100)", gauge.stateDescription())
    }

    @Test
    fun noValueReadsNoDataAndHasNoRange() {
        rule.setMonarchContent { MonarchGauge(value = Double.NaN, modifier = Modifier.testTag("gauge")) }
        val gauge = rule.onNodeWithTag("gauge")
        assertEquals("No data", gauge.stateDescription())
        assertNull(gauge.fetchSemanticsNode().config.getOrNull(SemanticsProperties.ProgressBarRangeInfo))
    }

    @Test
    fun theSpokenWordsCanBeReplaced() {
        rule.setMonarchContent {
            Box {
                MonarchGauge(value = null, modifier = Modifier.testTag("none"), noValueLabel = "Sin datos")
                MonarchGauge(value = 0.5, modifier = Modifier.testTag("own"), valueLabel = "Half full")
                MonarchGauge(
                    value = 0.25,
                    modifier = Modifier.testTag("format"),
                    range = 0.0..1.0,
                    formatValue = { "${(it * 100).toInt()} pc" },
                )
            }
        }
        assertEquals("Sin datos", rule.onNodeWithTag("none").stateDescription())
        assertEquals("Half full", rule.onNodeWithTag("own").stateDescription())
        assertEquals("25 pc (from 0 pc to 100 pc)", rule.onNodeWithTag("format").stateDescription())
    }

    @Test
    fun anOutOfRangeValueIsDrawnAtTheEndButSpokenAsItIs() {
        rule.setMonarchContent { MonarchGauge(value = 140.0, modifier = Modifier.testTag("gauge")) }
        val gauge = rule.onNodeWithTag("gauge").assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(100f, 0f..100f)))
        assertEquals("140 (from 0 to 100)", gauge.stateDescription())
        assertEquals(1f, MonarchGaugeScale(0.0, 100.0).share(140.0))
        assertEquals(0f, MonarchGaugeScale(0.0, 100.0).share(-5.0))
    }

    @Test
    fun aValueOnABoundaryBelongsToTheUpperBandAndTheEndToTheLast() {
        val scale = MonarchGaugeScale.of(0.0..100.0)
        assertEquals("At risk", MonarchGaugeTokens.bandAt(Health, 70.0, scale)?.label)
        assertEquals("Healthy", MonarchGaugeTokens.bandAt(Health, 69.9, scale)?.label)
        assertEquals("Critical", MonarchGaugeTokens.bandAt(Health, 100.0, scale)?.label)
        assertNull(MonarchGaugeTokens.bandAt(Health, 120.0, scale))
    }

    @Test
    fun aRangeWithEqualEndsIsWidenedByOne() {
        assertEquals(MonarchGaugeScale(5.0, 6.0), MonarchGaugeScale.of(5.0..5.0))
    }

    @Test
    fun theToneComesFromTheBandOrTheAccent() {
        val band = Health[1]
        assertEquals(MonarchTone.Warning, MonarchGaugeTokens.resolvedTone(null, true, band, true))
        assertEquals(MonarchTone.Neutral, MonarchGaugeTokens.resolvedTone(null, true, null, true))
        assertNull(MonarchGaugeTokens.resolvedTone(null, true, null, false))
        assertEquals(MonarchTone.Neutral, MonarchGaugeTokens.resolvedTone(null, false, null, false))
        assertEquals(MonarchTone.Info, MonarchGaugeTokens.resolvedTone(MonarchTone.Info, true, band, true))
    }

    @Test
    fun theDialHasTheWidthOfItsSize() {
        rule.setMonarchContent {
            Box {
                MonarchGauge(value = 1.0, modifier = Modifier.testTag("sm"), size = MonarchGaugeSize.Sm)
                MonarchGauge(value = 1.0, modifier = Modifier.testTag("md"))
                MonarchGauge(value = 1.0, modifier = Modifier.testTag("lg"), size = MonarchGaugeSize.Lg)
            }
        }
        rule.onNodeWithTag("sm").assertWidthIsEqualTo(120.dp)
        rule.onNodeWithTag("md").assertWidthIsEqualTo(160.dp)
        rule.onNodeWithTag("lg").assertWidthIsEqualTo(200.dp)
    }

    @Test
    fun theValueMovesUnderTheDialWithANeedleOrALargeFont() {
        assertTrue(MonarchGaugeTokens.valueInside(MonarchGaugeIndicator.Arc, 1f))
        assertTrue(!MonarchGaugeTokens.valueInside(MonarchGaugeIndicator.Arc, LargestFontScale))
        assertTrue(!MonarchGaugeTokens.valueInside(MonarchGaugeIndicator.Needle, 1f))
    }

    @Test
    fun atTheLargestFontSizeTheTextIsLaidOutUnderTheDial() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchGauge(value = 72.0, modifier = Modifier.testTag("gauge"), label = "CPU")
        }
        val node = rule.onNodeWithTag("gauge").fetchSemanticsNode()
        // The 80dp dial, then the range, the value and the label under it: the column grows instead of clipping.
        assertTrue(with(rule.density) { node.size.height.toDp() } > 160.dp)
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.accentSolid, MonarchGaugeTokens.toneColor(null, colors), theme.id)
            assertEquals(colors.warningSolid, MonarchGaugeTokens.toneColor(MonarchTone.Warning, colors), theme.id)
            assertEquals(colors.borderStrong, MonarchGaugeTokens.toneColor(MonarchTone.Neutral, colors), theme.id)
            val dial = MonarchGaugeTokens.colors(MonarchTone.Success, colors)
            assertEquals(colors.bgMuted, dial.track, theme.id)
            assertEquals(colors.textDefault, dial.mark, theme.id)
        }
    }
}
