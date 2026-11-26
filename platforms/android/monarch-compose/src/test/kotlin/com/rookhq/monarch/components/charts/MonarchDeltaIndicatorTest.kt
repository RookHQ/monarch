package com.rookhq.monarch.components.charts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.deltaindicator.MonarchDeltaIndicator
import com.rookhq.monarch.components.charts.deltaindicator.MonarchDeltaIndicatorDirection
import com.rookhq.monarch.components.charts.deltaindicator.MonarchDeltaIndicatorGoodDirection
import com.rookhq.monarch.components.charts.deltaindicator.MonarchDeltaIndicatorReading
import com.rookhq.monarch.components.charts.deltaindicator.MonarchDeltaIndicatorSize
import com.rookhq.monarch.components.charts.deltaindicator.MonarchDeltaIndicatorTokens
import com.rookhq.monarch.components.charts.deltaindicator.MonarchDeltaIndicatorVariant
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.TestIcon
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
import kotlin.test.assertTrue

internal object MonarchDeltaIndicatorFixtures : ComponentFixtures {
    override val id = "delta-indicator"
    override val fixtures = MonarchDeltaIndicatorVariant.entries.flatMap { variant ->
        MonarchDeltaIndicatorGoodDirection.entries.map { good ->
            fixture("${variant.name}, good ${good.name}, up and down") {
                Column {
                    listOf(0.12, -0.12).forEach { change ->
                        MonarchDeltaIndicator(
                            value = change,
                            comparison = "vs last month",
                            goodDirection = good,
                            variant = variant,
                        )
                    }
                }
            }
        }
    } + MonarchDeltaIndicatorSize.entries.map { size ->
        fixture("size ${size.name} with an icon") {
            MonarchDeltaIndicator(
                value = 0.12,
                size = size,
                icon = { MonarchIcon(imageVector = TestIcon, contentDescription = null, size = MonarchIconSize.Sm) },
            )
        }
    } + listOf(
        fixture("no change") { MonarchDeltaIndicator(value = 0.0, comparison = "vs last month") },
        fixture("no earlier data") { MonarchDeltaIndicator(value = null, comparison = "vs last month") },
        fixture("from two periods") { MonarchDeltaIndicator(current = 120.0, previous = 100.0) },
        fixture("own writer") { MonarchDeltaIndicator(value = -42.0, formatValue = { "${it.toInt()} ms" }) },
        fixture("long comparison in a narrow width") {
            MonarchDeltaIndicator(
                value = 0.12,
                modifier = Modifier.width(120.dp),
                comparison = "compared with the same week of the previous financial year",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchDeltaIndicatorTest {
    @get:Rule
    val rule = createComposeRule()

    private val percent = MonarchDeltaIndicatorReading.percent(Locale.US)

    private fun spoken(content: @Composable () -> Unit): String {
        rule.setMonarchContent { Box(Modifier.testTag("delta")) { content() } }
        return rule.onNodeWithTag("delta").fetchSemanticsNode().children.single()
            .config[SemanticsProperties.ContentDescription].single()
    }

    @Test
    fun anIncreaseInTheGoodDirectionIsReadAsOneSentence() {
        rule.setMonarchContent {
            MonarchDeltaIndicator(value = 0.12, modifier = Modifier.testTag("delta"), comparison = "vs last month")
        }
        rule.onNodeWithTag("delta")
            .assertContentDescriptionEquals("up 12% vs last month, better")
            .assertHasNoClickAction()
    }

    @Test
    fun aDecreaseIsBetterWhenDownIsTheGoodDirection() {
        rule.setMonarchContent {
            MonarchDeltaIndicator(
                value = -0.042,
                modifier = Modifier.testTag("delta"),
                goodDirection = MonarchDeltaIndicatorGoodDirection.Down,
            )
        }
        rule.onNodeWithTag("delta").assertContentDescriptionEquals("down 4.2%, better")
    }

    @Test
    fun anIncreaseIsWorseWhenDownIsTheGoodDirectionAndNeitherSaysNoQuality() {
        assertEquals(
            "up 5%, worse",
            spoken { MonarchDeltaIndicator(value = 0.05, goodDirection = MonarchDeltaIndicatorGoodDirection.Down) },
        )
    }

    @Test
    fun neitherDirectionHasNoQualityWord() {
        assertEquals(
            "up 5%",
            spoken { MonarchDeltaIndicator(value = 0.05, goodDirection = MonarchDeltaIndicatorGoodDirection.Neither) },
        )
    }

    @Test
    fun aChangeThatIsWrittenAsZeroIsNoChange() {
        val sentence = spoken { MonarchDeltaIndicator(value = 0.00001, comparison = "vs last month") }
        assertEquals("No change vs last month", sentence)
    }

    @Test
    fun noValueReadsNoEarlierDataAndDropsTheComparison() {
        assertEquals("No earlier data", spoken { MonarchDeltaIndicator(value = null, comparison = "vs last month") })
    }

    @Test
    fun theBuiltInWordsCanBeReplaced() {
        assertEquals("Sin datos", spoken { MonarchDeltaIndicator(value = Double.NaN, noDataLabel = "Sin datos") })
    }

    @Test
    fun theFlatLabelCanBeReplaced() {
        assertEquals("Igual", spoken { MonarchDeltaIndicator(value = 0.0, flatLabel = "Igual") })
    }

    @Test
    fun theSpokenSentenceCanBeReplaced() {
        assertEquals("Revenue grew", spoken { MonarchDeltaIndicator(value = 0.2, spokenLabel = "Revenue grew") })
    }

    @Test
    fun twoPeriodsGiveARatioOfThePreviousValue() {
        assertEquals("up 20% vs last week, better", spoken {
            MonarchDeltaIndicator(current = 120.0, previous = 100.0, comparison = "vs last week")
        })
        assertNull(MonarchDeltaIndicatorReading.change(current = 5.0, previous = 0.0))
        // A negative baseline uses its absolute value: −100 to −50 is an increase of 50%.
        assertEquals(0.5, MonarchDeltaIndicatorReading.change(current = -50.0, previous = -100.0))
        assertNull(MonarchDeltaIndicatorReading.change(current = null, previous = 1.0))
    }

    @Test
    fun theVisibleChangeHasASignAndARealMinus() {
        val up = MonarchDeltaIndicatorReading.of(0.12, MonarchDeltaIndicatorGoodDirection.Up, 0.0, percent)
        assertEquals("+12%", up.signedText)
        assertEquals(MonarchTone.Success, up.tone)
        val down = MonarchDeltaIndicatorReading.of(-0.12, MonarchDeltaIndicatorGoodDirection.Up, 0.0, percent)
        assertEquals("−12%", down.signedText)
        assertEquals(MonarchTone.Danger, down.tone)
        val flat = MonarchDeltaIndicatorReading.of(0.04, MonarchDeltaIndicatorGoodDirection.Up, 0.05, percent)
        assertEquals(MonarchDeltaIndicatorDirection.Flat, flat.direction)
        assertEquals(MonarchTone.Neutral, flat.tone)
        val infinite = MonarchDeltaIndicatorReading.of(
            Double.POSITIVE_INFINITY,
            MonarchDeltaIndicatorGoodDirection.Up,
            0.0,
            percent,
        )
        assertNull(infinite.direction)
    }

    @Test
    fun theOwnWriterWritesTheSize() {
        assertEquals("down 42 ms, better", spoken {
            MonarchDeltaIndicator(
                value = -42.0,
                goodDirection = MonarchDeltaIndicatorGoodDirection.Down,
                formatValue = { "${it.toInt()} ms" },
            )
        })
    }

    @Test
    fun theIconIsAtTheStartInBothLayoutDirections() {
        val environment = MonarchTestEnvironment()
        var icon = Rect.Zero
        var whole = Rect.Zero
        val directions = mutableListOf<MonarchDeltaIndicatorDirection>()
        rule.setMonarchContent(environment) {
            MonarchDeltaIndicator(
                value = 0.12,
                modifier = Modifier.onGloballyPositioned { whole = it.boundsInRoot() },
                comparison = "vs last month",
                icon = { direction ->
                    directions += direction
                    Box(Modifier.size(16.dp).onGloballyPositioned { icon = it.boundsInRoot() })
                },
            )
        }
        assertEquals(whole.left, icon.left, "LTR: the icon starts the row")
        environment.layoutDirection = LayoutDirection.Rtl
        rule.waitForIdle()
        assertEquals(whole.right, icon.right, "RTL: the icon starts the row on the right")
        assertEquals(MonarchDeltaIndicatorDirection.Up, directions.last())
    }

    @Test
    fun aLongComparisonWrapsInsideTheWidthAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Box(Modifier.width(160.dp)) {
                MonarchDeltaIndicator(
                    value = 0.12,
                    modifier = Modifier.testTag("delta"),
                    comparison = "compared with the same week of the previous year",
                )
            }
        }
        val bounds = rule.onNodeWithTag("delta").getUnclippedBoundsInRoot()
        assertTrue(bounds.right - bounds.left <= 160.dp, "The indicator stays inside its width")
        assertTrue(bounds.bottom - bounds.top > 40.dp, "The comparison wraps onto more lines")
    }

    @Test
    fun coloursFollowTheToneInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val plain = { tone: MonarchTone -> MonarchDeltaIndicatorTokens.plainColor(tone, colors) }
            assertEquals(colors.successStrong, plain(MonarchTone.Success), theme.id)
            assertEquals(colors.dangerStrong, plain(MonarchTone.Danger), theme.id)
            assertEquals(colors.textMuted, plain(MonarchTone.Neutral), theme.id)
            val pill = MonarchDeltaIndicatorTokens.pillColors(MonarchTone.Success, colors)
            assertEquals(colors.successSubtle, pill.container, theme.id)
            assertEquals(colors.successStrong, pill.content, theme.id)
            assertEquals(colors.textMuted, MonarchDeltaIndicatorTokens.mutedColor(colors), theme.id)
        }
    }
}
