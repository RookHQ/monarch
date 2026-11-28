package dev.monarchui.components.datadisplay

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.charts.sparkline.MonarchSparkline
import dev.monarchui.components.datadisplay.metric.MonarchMetricChange
import dev.monarchui.components.datadisplay.metric.MonarchMetricFormat
import dev.monarchui.components.datadisplay.statcard.MonarchStatCard
import dev.monarchui.components.datadisplay.statcard.MonarchStatCardLabels
import dev.monarchui.components.datadisplay.statcard.MonarchStatCardSize
import dev.monarchui.components.datadisplay.statcard.MonarchStatCardTokens
import dev.monarchui.components.datadisplay.statcard.MonarchStatCardVariant
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
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
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal object MonarchStatCardFixtures : ComponentFixtures {
    override val id = "stat-card"
    override val fixtures = listOf(
        fixture("currency with a change, help and drill-down") {
            MonarchStatCard(
                label = "Revenue",
                value = 48_200.0,
                format = MonarchMetricFormat.Currency("USD"),
                locale = Locale.US,
                change = MonarchMetricChange(0.12),
                caption = "vs last month",
                help = "Invoiced revenue, before refunds.",
                onClick = {},
            )
        },
        fixture("every tone, small") {
            Column {
                MonarchTone.entries.forEach { tone ->
                    MonarchStatCard(label = tone.name, value = 42.0, tone = tone, size = MonarchStatCardSize.Sm)
                }
            }
        },
        fixture("every size, plain") {
            Column {
                MonarchStatCardSize.entries.forEach { size ->
                    MonarchStatCard(label = "Seats", value = 38.0, variant = MonarchStatCardVariant.Plain, size = size)
                }
            }
        },
        fixture("highlighted, compact percent, hint") {
            MonarchStatCard(
                label = "Activation",
                value = 0.73,
                format = MonarchMetricFormat.Percent,
                locale = Locale.US,
                highlighted = true,
                hint = "38 of 52 people",
            )
        },
        fixture("icon, visual and sparkline") {
            MonarchStatCard(
                label = "Signups",
                value = 1_204.0,
                icon = { Icon(TestIcon, contentDescription = null) },
                visual = { Icon(TestIcon, contentDescription = null) },
                sparkline = { MonarchSparkline(values = listOf(2.0, 4.0, 3.0, 6.0)) },
            )
        },
        fixture("loading") { MonarchStatCard(label = "Uptime", value = null, loading = true) },
        fixture("empty") { MonarchStatCard(label = "Uptime", value = null, unit = "%") },
        fixture("error with retry") {
            MonarchStatCard(label = "Uptime", value = 99.9, error = "The server did not answer.", onRetry = {})
        },
        fixture("error without retry") {
            MonarchStatCard(label = "Uptime", value = 99.9, error = "No access to this metric.")
        },
        fixture("text value, large") {
            MonarchStatCard(
                label = "Pipeline",
                text = "₹12.4L",
                spokenText = "12.4 lakh rupees",
                size = MonarchStatCardSize.Lg,
            )
        },
        fixture("long label") {
            MonarchStatCard(
                label = "Median time from ticket creation to the first reply from an agent",
                value = 4.5,
                unit = "hours",
                help = "Business hours only.",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchStatCardTest {
    @get:Rule
    val rule = createComposeRule()

    private val usd = MonarchMetricFormat.Currency("USD")

    @Test
    fun theCardIsOneElementWithLabelValueChangeAndHint() {
        rule.setMonarchContent {
            MonarchStatCard(
                label = "Revenue",
                value = 48_200.0,
                format = usd,
                locale = Locale.US,
                change = MonarchMetricChange(0.12),
                caption = "vs last month",
                hint = "From 120 invoices",
            )
        }
        val shown = usd.format(48_200.0, false, Locale.US)
        rule.onNode(hasText("Revenue") and hasText(shown) and hasText("From 120 invoices"))
            .assertExists()
            .assertHasNoClickAction()
        // The change is read with the card, as the delta indicator's sentence.
        rule.onNode(hasText("Revenue") and hasContentDescription("vs last month", substring = true)).assertExists()
    }

    @Test
    fun aCompactValueIsReadInFull() {
        rule.setMonarchContent {
            MonarchStatCard(label = "Revenue", value = 3_400_000.0, format = usd, compact = true, locale = Locale.US)
        }
        val full = usd.format(3_400_000.0, false, Locale.US)
        rule.onNodeWithText(usd.format(3_400_000.0, true, Locale.US), useUnmergedTree = true)
            .assert(hasContentDescription(full))
    }

    @Test
    fun theCaptionStandsAloneWithoutAChange() {
        rule.setMonarchContent { MonarchStatCard(label = "Seats", value = 38.0, caption = "This quarter") }
        rule.onNodeWithText("This quarter", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aMissingValueIsADashReadAsNoData() {
        rule.setMonarchContent {
            Column {
                MonarchStatCard(label = "Uptime", value = Double.NaN, unit = "%")
                MonarchStatCard(label = "Seats", text = "", labels = MonarchStatCardLabels(empty = "Sin datos"))
            }
        }
        rule.onNodeWithContentDescription("No data", useUnmergedTree = true).assert(hasText("—"))
        rule.onNodeWithText("%", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithContentDescription("Sin datos", useUnmergedTree = true).assertExists()
    }

    @Test
    fun loadingHidesTheValueAndSaysLoading() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            Column {
                MonarchStatCard(label = "Uptime", value = 99.9, loading = true, hint = "Last 30 days")
                MonarchStatCard(
                    label = "Seats",
                    value = 4.0,
                    loading = true,
                    labels = MonarchStatCardLabels(loading = "Chargement"),
                )
            }
        }
        rule.mainClock.advanceTimeByFrame()
        assertEquals("Loading", rule.onNode(hasText("Uptime")).stateDescription())
        assertEquals("Chargement", rule.onNode(hasText("Seats")).stateDescription())
        rule.onNodeWithText("99.9", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Last 30 days").assertDoesNotExist()
    }

    @Test
    fun anErrorReplacesTheValueAndOffersRetry() {
        var retries = 0
        rule.setMonarchContent {
            MonarchStatCard(
                label = "Uptime",
                value = 99.9,
                change = MonarchMetricChange(0.1),
                error = "The server did not answer.",
                onRetry = { retries++ },
            )
        }
        rule.onNodeWithText("The server did not answer.", substring = true).assertExists()
        rule.onNodeWithText("99.9", substring = true).assertDoesNotExist()
        rule.onNode(hasClickAction() and hasText("Try again", substring = true))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, retries)
    }

    @Test
    fun anErrorWithoutRetryIsAnErrorState() {
        rule.setMonarchContent { MonarchStatCard(label = "Uptime", value = 99.9, error = "No access.") }
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Error, "No access."), useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun theCardDrillsDownWhenTapped() {
        var opened = 0
        rule.setMonarchContent { MonarchStatCard(label = "Revenue", value = 10.0, onClick = { opened++ }) }
        rule.onNode(hasText("Revenue") and hasClickAction())
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, opened)
    }

    @Test
    fun helpIsASeparateButtonThatOpensTheDefinition() {
        var opened = 0
        rule.setMonarchContent {
            MonarchStatCard(
                label = "Revenue",
                value = 10.0,
                help = "Invoiced revenue, before refunds.",
                onClick = { opened++ },
            )
        }
        rule.onNodeWithContentDescription("About Revenue")
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithText("Invoiced revenue, before refunds.").assertExists()
        assertEquals(0, opened)
    }

    @Test
    fun theHelpNameCanBeReplaced() {
        rule.setMonarchContent {
            MonarchStatCard(
                label = "Revenue",
                value = 10.0,
                help = "Invoiced.",
                labels = MonarchStatCardLabels(help = { "Qu'est-ce que $it ?" }),
            )
        }
        rule.onNodeWithContentDescription("Qu'est-ce que Revenue ?").assertExists()
    }

    @Test
    fun theHelpButtonIsAtTheEndInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchStatCard(label = "Revenue", value = 10.0, help = "Invoiced.")
        }
        rule.onNodeWithText("Revenue", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithContentDescription("About Revenue"), LayoutDirection.Rtl)
    }

    @Test
    fun aLongLabelWrapsAtTheLargestFontSize() {
        val label = "Median time from ticket creation to the first reply"
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchStatCard(label = label, value = 4.5) }
        rule.onNodeWithText(label, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theColoursFollowToneVariantAndHighlightInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchStatCardTokens
            assertEquals(c.textDefault, t.valueColor(MonarchTone.Neutral, false, c), theme.id)
            assertEquals(c.infoStrong, t.valueColor(MonarchTone.Info, false, c), theme.id)
            assertEquals(c.successStrong, t.valueColor(MonarchTone.Success, false, c), theme.id)
            assertEquals(c.warningStrong, t.valueColor(MonarchTone.Warning, false, c), theme.id)
            assertEquals(c.dangerStrong, t.valueColor(MonarchTone.Danger, false, c), theme.id)
            assertEquals(c.textSubtle, t.valueColor(MonarchTone.Danger, true, c), theme.id)
            val card = t.container(MonarchStatCardVariant.Card, false, c)
            assertEquals(c.bgSurface to c.borderDefault, card.container to card.border, theme.id)
            val accent = t.container(MonarchStatCardVariant.Card, true, c)
            assertEquals(c.accentSubtle to c.accentBorder, accent.container to accent.border, theme.id)
            assertNull(t.container(MonarchStatCardVariant.Plain, true, c).border, theme.id)
        }
    }

    @Test
    fun theValueLogicMatchesTheMetric() {
        val t = MonarchStatCardTokens
        assertNull(t.shown(null, MonarchMetricFormat.Number, false, Locale.US))
        assertNull(t.shown(Double.POSITIVE_INFINITY, MonarchMetricFormat.Number, false, Locale.US))
        assertEquals("1,204", t.shown(1_204.0, MonarchMetricFormat.Number, false, Locale.US))
        assertNull(t.spoken(1_204.0, MonarchMetricFormat.Number, false, Locale.US))
        assertEquals("1,204", t.spoken(1_204.0, MonarchMetricFormat.Number, true, Locale.US))
    }
}
