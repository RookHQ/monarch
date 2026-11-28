package dev.monarchui.components.intelligence

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.monarchui.components.intelligence.aiscore.MonarchAiScore
import dev.monarchui.components.intelligence.aiscore.MonarchAiScoreBand
import dev.monarchui.components.intelligence.aiscore.MonarchAiScoreFactor
import dev.monarchui.components.intelligence.aiscore.MonarchAiScoreLabels
import dev.monarchui.components.intelligence.aiscore.MonarchAiScoreSize
import dev.monarchui.components.intelligence.aiscore.MonarchAiScoreTokens
import dev.monarchui.components.intelligence.aiscore.MonarchAiScoreVariant
import dev.monarchui.foundation.MonarchTone
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
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val scoreBands = listOf(
    MonarchAiScoreBand(0.0, "Low", MonarchTone.Danger),
    MonarchAiScoreBand(40.0, "Medium", MonarchTone.Warning),
    MonarchAiScoreBand(70.0, "High", MonarchTone.Success),
)

internal object MonarchAiScoreFixtures : ComponentFixtures {
    override val id = "ai-score"
    override val fixtures = listOf(
        fixture("ring, scored") { MonarchAiScore(value = 72.0, label = "Lead score", showBands = false) },
        fixture("ring, small, no score") {
            MonarchAiScore(value = null, size = MonarchAiScoreSize.Sm, label = "Fit", showBands = false)
        },
        fixture("bar with legend") { MonarchAiScore(value = 34.0, variant = MonarchAiScoreVariant.Bar) },
        fixture("rationale") {
            MonarchAiScore(
                value = 55.0,
                size = MonarchAiScoreSize.Sm,
                rationale = "Matches the ideal customer profile but has not replied in two weeks.",
                showBands = false,
            )
        },
        fixture("factors") {
            MonarchAiScore(
                value = 81.0,
                variant = MonarchAiScoreVariant.Bar,
                factors = listOf(
                    MonarchAiScoreFactor("Visited pricing", impact = 18.0, detail = "Twice in 7 days"),
                    MonarchAiScoreFactor("No reply", impact = -9.0),
                ),
                showBands = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiScoreTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theRingIsNamedByTheLabelWithItsValueAndTheBandIsWritten() {
        rule.setMonarchContent { MonarchAiScore(value = 72.4, label = "Lead score", showBands = false) }
        val ring = rule.onNodeWithContentDescription("Lead score")
        assertEquals("72 out of 100", ring.stateDescription())
        ring.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(72.4f, 0f..100f),
            ),
        )
        rule.onNodeWithText("High", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("AI score").assertExists()
        rule.onNodeWithText("Lead score", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun theBarReadsTheValueAndTheBand() {
        rule.setMonarchContent {
            MonarchAiScore(value = 34.0, variant = MonarchAiScoreVariant.Bar, label = "Churn risk", bands = scoreBands)
        }
        assertEquals("34 out of 100, Low", rule.onNodeWithContentDescription("Churn risk").stateDescription())
    }

    @Test
    fun withoutAScoreItSaysSoAndShowsNoBand() {
        rule.setMonarchContent { MonarchAiScore(value = null, variant = MonarchAiScoreVariant.Bar, bands = scoreBands) }
        val bar = rule.onNodeWithContentDescription("Score")
        assertEquals("No score yet", bar.stateDescription())
        bar.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ProgressBarRangeInfo))
        rule.onNodeWithText("Low", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun eachFactorIsOneElementWithItsSignedImpact() {
        rule.setMonarchContent {
            MonarchAiScore(
                value = 81.0,
                factors = listOf(
                    MonarchAiScoreFactor("Visited pricing", impact = 18.0, detail = "Twice in 7 days"),
                    MonarchAiScoreFactor("No reply", impact = -9.5),
                    MonarchAiScoreFactor("Industry", impact = 0.0),
                ),
                showBands = false,
            )
        }
        rule.onNodeWithContentDescription("Visited pricing, Raised the score by 18, Twice in 7 days").assertExists()
        rule.onNodeWithContentDescription("No reply, Lowered the score by 9.5").assertExists()
        rule.onNodeWithContentDescription("Industry, No effect on the score").assertExists()
        rule.onNodeWithText("What affects this score")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun theLegendMarksTheCurrentBandInWords() {
        rule.setMonarchContent { MonarchAiScore(value = 50.0, bands = scoreBands) }
        val header = rule.onNodeWithText("How to read this score")
        header.assertTouchTargetAtLeast48dp(rule.density)
        header.performClick()
        rule.onNodeWithContentDescription("Medium (this score), 40–69").assertExists()
        rule.onNodeWithContentDescription("High, 70–100").assertExists()
    }

    @Test
    fun textWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiScore(
                value = 72.0,
                label = "Likelihood to renew this quarter",
                rationale = "Usage grew every week.",
                showBands = false,
            )
        }
        rule.onNodeWithText("Likelihood to renew this quarter", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Usage grew every week.", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchAiScore(
                value = null,
                showBands = false,
                factors = listOf(MonarchAiScoreFactor("Seats", impact = 4.0)),
                labels = MonarchAiScoreLabels(
                    label = "Fit",
                    aiGenerated = "Model",
                    unavailable = "Pending",
                    factors = "Why",
                    impact = { amount, raised -> if (raised) "up $amount" else "down $amount" },
                ),
            )
        }
        assertEquals("Pending", rule.onNodeWithContentDescription("Fit").stateDescription())
        rule.onNodeWithContentDescription("Model").assertExists()
        rule.onNodeWithText("Why").assertExists()
        rule.onNodeWithContentDescription("Seats, up 4").assertExists()
    }

    @Test
    fun bandsAndValuesFollowTheWebRules() {
        val tokens = MonarchAiScoreTokens
        assertEquals(100.0, tokens.clamp(140.0))
        assertEquals(0.0, tokens.clamp(-5.0))
        assertNull(tokens.clamp(Double.NaN))
        assertEquals("Medium", tokens.band(40.0, scoreBands)?.band?.label)
        assertEquals("Low", tokens.band(39.9, scoreBands)?.band?.label)
        assertEquals("High", tokens.band(100.0, scoreBands)?.band?.label)
        assertNull(tokens.band(50.0, emptyList()))
        val ranges = tokens.ranges(scoreBands.reversed())
        assertEquals(listOf(39.0, 69.0, 100.0), ranges.map { it.max })
        assertEquals(18.0, tokens.maxImpact(listOf(MonarchAiScoreFactor("a", -18.0), MonarchAiScoreFactor("b", 2.0))))
        assertEquals(1.0, tokens.maxImpact(emptyList()))
        assertEquals(0.5f, tokens.impactFraction(-9.0, 18.0))
        assertEquals("1,250", tokens.formatWhole(1250.0, Locale.US))
        assertEquals("2.5", tokens.formatImpact(-2.5, Locale.US))
    }

    @Test
    fun coloursComeFromTheToneRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchAiScoreTokens
            assertEquals(colors.successStrong, tokens.impactColor(3.0, colors), theme.id)
            assertEquals(colors.dangerStrong, tokens.impactColor(-3.0, colors), theme.id)
            assertEquals(colors.textMuted, tokens.impactColor(0.0, colors), theme.id)
            assertEquals(colors.warningSolid, tokens.swatchColor(MonarchTone.Warning, colors), theme.id)
        }
    }

    @Test
    fun theBarTakesTheToneOfItsBand() {
        assertEquals(MonarchTone.Success, MonarchAiScoreTokens.barTone(MonarchTone.Success))
        assertEquals(MonarchTone.Neutral, MonarchAiScoreTokens.barTone(null))
    }
}
