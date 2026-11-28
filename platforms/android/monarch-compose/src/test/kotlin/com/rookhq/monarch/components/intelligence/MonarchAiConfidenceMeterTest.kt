package com.rookhq.monarch.components.intelligence

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.rookhq.monarch.components.intelligence.aiconfidencemeter.MonarchAiConfidenceLevel
import com.rookhq.monarch.components.intelligence.aiconfidencemeter.MonarchAiConfidenceMeter
import com.rookhq.monarch.components.intelligence.aiconfidencemeter.MonarchAiConfidenceMeterLabels
import com.rookhq.monarch.components.intelligence.aiconfidencemeter.MonarchAiConfidenceMeterSize
import com.rookhq.monarch.components.intelligence.aiconfidencemeter.MonarchAiConfidenceMeterTokens
import com.rookhq.monarch.components.intelligence.aiconfidencemeter.MonarchAiConfidenceMeterVariant
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
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val Values = listOf(0.42f, 0.72f, 0.91f)

internal object MonarchAiConfidenceMeterFixtures : ComponentFixtures {
    override val id = "ai-confidence-meter"
    override val fixtures = MonarchAiConfidenceMeterSize.entries.flatMap { size ->
        listOf(
            fixture("badges, ${size.name}") {
                Column { Values.forEach { MonarchAiConfidenceMeter(value = it, size = size, showValue = true) } }
            },
            fixture("bars, ${size.name}") {
                Column {
                    Values.forEach {
                        MonarchAiConfidenceMeter(value = it, variant = MonarchAiConfidenceMeterVariant.Bar, size = size)
                    }
                }
            },
        )
    } + listOf(
        fixture("bar with an explanation") {
            MonarchAiConfidenceMeter(
                value = 0.55f,
                variant = MonarchAiConfidenceMeterVariant.Bar,
                explanation = "Only one source mentions this figure, and it is two years old.",
            )
        },
        fixture("out of range") { MonarchAiConfidenceMeter(value = Float.NaN) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiConfidenceMeterTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theBandsFollowTheThresholds() {
        assertEquals(MonarchAiConfidenceLevel.Low, MonarchAiConfidenceLevel.of(0.59f))
        assertEquals(MonarchAiConfidenceLevel.Medium, MonarchAiConfidenceLevel.of(0.6f))
        assertEquals(MonarchAiConfidenceLevel.High, MonarchAiConfidenceLevel.of(0.85f))
        assertEquals(MonarchAiConfidenceLevel.Low, MonarchAiConfidenceLevel.of(Float.NaN))
        assertEquals(MonarchAiConfidenceLevel.High, MonarchAiConfidenceLevel.of(7f))
        val custom = MonarchAiConfidenceLevel.of(0.5f, lowThreshold = 0.3f, highThreshold = 0.5f)
        assertEquals(MonarchAiConfidenceLevel.High, custom)
        // A low edge above the high edge leaves no medium band.
        val crossed = MonarchAiConfidenceLevel.of(0.7f, lowThreshold = 0.9f, highThreshold = 0.5f)
        assertEquals(MonarchAiConfidenceLevel.Low, crossed)
    }

    @Test
    fun theBadgeIsOneElementReadWithTheBandInWords() {
        rule.setMonarchContent { MonarchAiConfidenceMeter(value = 0.42f, explanation = "One source.") }
        rule.onNodeWithContentDescription("Needs check, 42% confidence. One source.").assertHasNoClickAction()
    }

    @Test
    fun theBarIsAProgressBarNamedConfidenceWithTheValueAsItsState() {
        rule.setMonarchContent {
            MonarchAiConfidenceMeter(
                value = 0.91f,
                modifier = Modifier.testTag("meter"),
                variant = MonarchAiConfidenceMeterVariant.Bar,
                explanation = "Matches two sources.",
            )
        }
        val bar = rule.onNodeWithContentDescription("Confidence")
        assertEquals("High confidence, 91% confidence", bar.stateDescription())
        val range = ProgressBarRangeInfo(0.91f, 0f..1f)
        bar.assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, range))
        rule.onNodeWithText("Matches two sources.").assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiConfidenceMeter(
                value = 0.7f,
                labels = MonarchAiConfidenceMeterLabels(
                    medium = "Plausible",
                    valueText = { label, percent -> "$label ($percent)" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Plausible (70%)").assertExists()
    }

    @Test
    fun theBarLabelIsNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiConfidenceMeter(value = 0.72f, variant = MonarchAiConfidenceMeterVariant.Bar)
        }
        rule.onNodeWithText("Medium confidence", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun onlyTheBarShowsThePercentageByDefault() {
        assertTrue(MonarchAiConfidenceMeterTokens.showsValue(null, MonarchAiConfidenceMeterVariant.Bar))
        assertFalse(MonarchAiConfidenceMeterTokens.showsValue(null, MonarchAiConfidenceMeterVariant.Badge))
        assertTrue(MonarchAiConfidenceMeterTokens.showsValue(true, MonarchAiConfidenceMeterVariant.Badge))
        assertEquals("42%", MonarchAiConfidenceMeterTokens.percent(0.42f, Locale.US))
    }

    @Test
    fun theColoursComeFromTheConfidenceRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val low = MonarchAiConfidenceMeterTokens.badgeColors(MonarchAiConfidenceLevel.Low, c)
            assertEquals(c.dangerSubtle, low.container, theme.id)
            assertEquals(c.confidenceLow, low.content, theme.id)
            assertEquals(c.dangerBorder, low.border, theme.id)
            val medium = MonarchAiConfidenceMeterTokens.badgeColors(MonarchAiConfidenceLevel.Medium, c)
            assertEquals(c.warningSubtle, medium.container, theme.id)
            assertEquals(c.confidenceMedium, medium.content, theme.id)
            val high = MonarchAiConfidenceMeterTokens.badgeColors(MonarchAiConfidenceLevel.High, c)
            assertEquals(c.successBorder, high.border, theme.id)
            assertEquals(c.confidenceHigh, MonarchAiConfidenceMeterTokens.foreground(MonarchAiConfidenceLevel.High, c))
            assertEquals(c.bgMuted, MonarchAiConfidenceMeterTokens.track(c), theme.id)
        }
    }
}
