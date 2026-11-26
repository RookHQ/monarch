package com.rookhq.monarch.components.datadisplay

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.datadisplay.scorering.MonarchScoreRing
import com.rookhq.monarch.components.datadisplay.scorering.MonarchScoreRingColor
import com.rookhq.monarch.components.datadisplay.scorering.MonarchScoreRingLabels
import com.rookhq.monarch.components.datadisplay.scorering.MonarchScoreRingSize
import com.rookhq.monarch.components.datadisplay.scorering.MonarchScoreRingThreshold
import com.rookhq.monarch.components.datadisplay.scorering.MonarchScoreRingTokens
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchScoreRingFixtures : ComponentFixtures {
    override val id = "score-ring"
    override val fixtures = listOf(
        fixture("every band, small") {
            Row {
                listOf(92.0, 71.0, 45.0, 12.0).forEach { MonarchScoreRing(value = it, size = MonarchScoreRingSize.Sm) }
            }
        },
        fixture("medium with label and description") {
            MonarchScoreRing(value = 78.0, label = "Health", description = "Updated today")
        },
        fixture("large") { MonarchScoreRing(value = 100.0, size = MonarchScoreRingSize.Lg, label = "Overall") },
        fixture("no score") { MonarchScoreRing(value = null, label = "Quality") },
        fixture("accent and fixed tones") {
            Row {
                MonarchScoreRing(
                    value = 3.5,
                    max = 5.0,
                    color = MonarchScoreRingColor.Accent,
                    size = MonarchScoreRingSize.Sm,
                )
                MonarchScoreRing(
                    value = 30.0,
                    color = MonarchScoreRingColor.Tone(MonarchTone.Neutral),
                    size = MonarchScoreRingSize.Sm,
                )
            }
        },
        fixture("long label") {
            MonarchScoreRing(
                value = 64.0,
                label = "Search engine visibility across every tracked keyword",
                description = "Compared with the previous thirty days",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchScoreRingTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theRingIsOneElementWithItsNameValueAndRange() {
        rule.setMonarchContent {
            MonarchScoreRing(value = 78.0, modifier = Modifier.testTag("ring"), label = "Health", description = "Today")
        }
        val ring = rule.onNodeWithTag("ring")
        ring.assertContentDescriptionEquals("Health, Today")
        assertEquals("78 out of 100", ring.stateDescription())
        ring.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(78f, 0f..100f)),
        )
    }

    @Test
    fun theNumberIsAlwaysWrittenInTheCentre() {
        rule.setMonarchContent { MonarchScoreRing(value = 78.4) }
        rule.onNodeWithText("78", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("/ 100", useUnmergedTree = true).assertExists()
    }

    @Test
    fun withoutAScoreItSaysSoAndHasNoRange() {
        rule.setMonarchContent { MonarchScoreRing(value = null, modifier = Modifier.testTag("ring")) }
        val ring = rule.onNodeWithTag("ring")
        ring.assertContentDescriptionEquals("Score")
        assertEquals("No score yet", ring.stateDescription())
        ring.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ProgressBarRangeInfo))
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchScoreRing(
                value = 4.0,
                max = 5.0,
                modifier = Modifier.testTag("ring"),
                format = { "%.1f".format(it) },
                labels = MonarchScoreRingLabels(
                    name = "Rating",
                    valueText = { value, max -> "$value of $max stars" },
                    ofMax = { "of $it" },
                ),
            )
        }
        val ring = rule.onNodeWithTag("ring")
        ring.assertContentDescriptionEquals("Rating")
        assertEquals("4.0 of 5 stars", ring.stateDescription())
        rule.onNodeWithText("of 5", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theRingGrowsWithTheFontSoTheNumberFits() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchScoreRing(value = 100.0, modifier = Modifier.testTag("ring"), label = "Overall")
        }
        rule.onNodeWithText("100", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Overall", useUnmergedTree = true).assertTextFullyVisible()
        val bounds = rule.onNodeWithTag("ring").getUnclippedBoundsInRoot()
        val width = bounds.right - bounds.left
        assertTrue(width >= 160.dp)
    }

    @Test
    fun valuesAreLimitedAndABadMaximumFallsBackTo100() {
        assertEquals(100.0, MonarchScoreRingTokens.safeMax(0.0))
        assertEquals(100.0, MonarchScoreRingTokens.safeMax(Double.POSITIVE_INFINITY))
        assertEquals(5.0, MonarchScoreRingTokens.safeMax(5.0))
        assertEquals(100.0, MonarchScoreRingTokens.clamp(140.0, 100.0))
        assertEquals(0.0, MonarchScoreRingTokens.clamp(-3.0, 100.0))
        assertNull(MonarchScoreRingTokens.clamp(Double.NaN, 100.0))
        assertFalse(MonarchScoreRingTokens.showsMax(null, MonarchScoreRingSize.Sm, hasValue = true))
        assertTrue(MonarchScoreRingTokens.showsMax(null, MonarchScoreRingSize.Md, hasValue = true))
        assertFalse(MonarchScoreRingTokens.showsMax(true, MonarchScoreRingSize.Lg, hasValue = false))
    }

    @Test
    fun theBandsPickTheTone() {
        val defaults = MonarchScoreRingThreshold.Defaults
        assertEquals(MonarchTone.Success, MonarchScoreRingTokens.bandTone(80.0, defaults))
        assertEquals(MonarchTone.Info, MonarchScoreRingTokens.bandTone(79.9, defaults))
        assertEquals(MonarchTone.Warning, MonarchScoreRingTokens.bandTone(40.0, defaults))
        assertEquals(MonarchTone.Danger, MonarchScoreRingTokens.bandTone(0.0, defaults))
        assertEquals(MonarchTone.Neutral, MonarchScoreRingTokens.bandTone(5.0, emptyList()))
        assertEquals(MonarchTone.Neutral, MonarchScoreRingTokens.tone(MonarchScoreRingColor.Auto, null, defaults))
        assertNull(MonarchScoreRingTokens.tone(MonarchScoreRingColor.Accent, 90.0, defaults))
        assertEquals(
            MonarchTone.Warning,
            MonarchScoreRingTokens.tone(MonarchScoreRingColor.Tone(MonarchTone.Warning), 90.0, defaults),
        )
    }

    @Test
    fun coloursComeFromTheToneRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val success = MonarchScoreRingTokens.colors(MonarchTone.Success, colors)
            assertEquals(colors.successSolid, success.arc, theme.id)
            assertEquals(colors.successStrong, success.text, theme.id)
            assertEquals(colors.bgMuted, success.track, theme.id)
            val accent = MonarchScoreRingTokens.colors(null, colors)
            assertEquals(colors.accentSolid, accent.arc, theme.id)
            assertEquals(colors.accentStrong, accent.text, theme.id)
            val neutral = MonarchScoreRingTokens.colors(MonarchTone.Neutral, colors)
            assertEquals(colors.textMuted, neutral.arc, theme.id)
            assertEquals(colors.textDefault, neutral.text, theme.id)
        }
    }
}
