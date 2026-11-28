package dev.monarchui.components.collaboration

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import dev.monarchui.components.collaboration.audiolevelmeter.MonarchAudioLevelMeter
import dev.monarchui.components.collaboration.audiolevelmeter.MonarchAudioLevelMeterSize
import dev.monarchui.components.collaboration.audiolevelmeter.MonarchAudioLevelMeterTokens
import dev.monarchui.components.collaboration.audiolevelmeter.MonarchAudioLevelMeterVariant
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object MonarchAudioLevelMeterFixtures : ComponentFixtures {
    override val id = "audio-level-meter"
    override val fixtures = MonarchAudioLevelMeterSize.entries.map { size ->
        fixture("segments, size ${size.name}, silent to loud") {
            Column {
                listOf(0f, 0.3f, 0.6f, 1f).forEach { MonarchAudioLevelMeter(level = it, size = size) }
            }
        }
    } + listOf(
        fixture("eight segments") { MonarchAudioLevelMeter(level = 0.5f, segments = 8) },
        fixture("no segments asked for") { MonarchAudioLevelMeter(level = 0.5f, segments = 0) },
        fixture("muted") { MonarchAudioLevelMeter(level = 0.8f, muted = true) },
        fixture("bar") { MonarchAudioLevelMeter(level = 0.6f, variant = MonarchAudioLevelMeterVariant.Bar) },
        fixture("bar, muted") {
            MonarchAudioLevelMeter(level = 0.6f, variant = MonarchAudioLevelMeterVariant.Bar, muted = true)
        },
        fixture("decorative") { MonarchAudioLevelMeter(level = 0.6f, decorative = true) },
        fixture("not a number") { MonarchAudioLevelMeter(level = Float.NaN) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAudioLevelMeterTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theMeterIsNamedAndReadsTheLevelAsAPercentage() {
        rule.setMonarchContent { MonarchAudioLevelMeter(level = 0.62f, modifier = Modifier.testTag("meter")) }
        val meter = rule.onNodeWithTag("meter")
            .assertContentDescriptionEquals("Microphone level")
            .assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.62f, 0f..1f)))
            .assertHasNoClickAction()
        assertEquals("62%", meter.stateDescription())
    }

    @Test
    fun aMutedMeterReadsMutedAndIsEmpty() {
        rule.setMonarchContent {
            MonarchAudioLevelMeter(level = 0.8f, modifier = Modifier.testTag("meter"), muted = true)
        }
        val meter = rule.onNodeWithTag("meter").assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(0f, 0f..1f)))
        assertEquals("Muted", meter.stateDescription())
    }

    @Test
    fun theBuiltInWordsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAudioLevelMeter(
                level = 0.5f,
                modifier = Modifier.testTag("meter"),
                muted = true,
                label = "Niveau du micro",
                mutedLabel = "Coupé",
            )
        }
        val meter = rule.onNodeWithTag("meter").assertContentDescriptionEquals("Niveau du micro")
        assertEquals("Coupé", meter.stateDescription())
    }

    @Test
    fun aDecorativeMeterIsHidden() {
        rule.setMonarchContent {
            Box(Modifier.testTag("box")) { MonarchAudioLevelMeter(level = 0.5f, decorative = true) }
        }
        assertTrue(rule.onNodeWithTag("box").fetchSemanticsNode().children.all { it.config.none() })
    }

    @Test
    fun levelsAreLimitedAndNotANumberIsSilence() {
        assertEquals(1f, MonarchAudioLevelMeterTokens.level(4f, muted = false))
        assertEquals(0f, MonarchAudioLevelMeterTokens.level(-1f, muted = false))
        assertEquals(0f, MonarchAudioLevelMeterTokens.level(Float.NaN, muted = false))
        assertEquals(0f, MonarchAudioLevelMeterTokens.level(0.7f, muted = true))
        assertEquals(1, MonarchAudioLevelMeterTokens.count(0))
        assertEquals(3, MonarchAudioLevelMeterTokens.lit(0.6f, 5))
        assertEquals(5, MonarchAudioLevelMeterTokens.lit(1f, 5))
        assertEquals(0, MonarchAudioLevelMeterTokens.lit(0.05f, 5))
    }

    @Test
    fun withAnimationsRemovedTheMeterOnlyShowsWhetherThereIsSound() {
        assertEquals(1f, MonarchAudioLevelMeterTokens.shown(0.1f, reducedMotion = true))
        assertEquals(0f, MonarchAudioLevelMeterTokens.shown(0.05f, reducedMotion = true))
        assertEquals(0.1f, MonarchAudioLevelMeterTokens.shown(0.1f, reducedMotion = false))
    }

    @Test
    fun segmentsAreWordSizedAndTheBarFillsTheWidth() {
        rule.setMonarchContent {
            Column(Modifier.width(200.dp)) {
                MonarchAudioLevelMeter(level = 0.5f, modifier = Modifier.testTag("md"))
                MonarchAudioLevelMeter(
                    level = 0.5f,
                    modifier = Modifier.testTag("sm"),
                    size = MonarchAudioLevelMeterSize.Sm,
                )
                MonarchAudioLevelMeter(
                    level = 0.5f,
                    modifier = Modifier.testTag("bar"),
                    variant = MonarchAudioLevelMeterVariant.Bar,
                )
            }
        }
        // Five 4dp segments with 2dp gaps.
        rule.onNodeWithTag("md").assertWidthIsEqualTo(28.dp).assertHeightIsEqualTo(16.dp)
        rule.onNodeWithTag("sm").assertHeightIsEqualTo(12.dp)
        rule.onNodeWithTag("bar").assertWidthIsEqualTo(200.dp).assertHeightIsEqualTo(4.dp)
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val live = MonarchAudioLevelMeterTokens.colors(muted = false, colors = colors)
            assertEquals(colors.successSolid, live.on, theme.id)
            assertEquals(colors.borderStrong, live.off, theme.id)
            assertEquals(colors.bgMuted, live.track, theme.id)
            val muted = MonarchAudioLevelMeterTokens.colors(muted = true, colors = colors)
            assertEquals(colors.textDisabled, muted.on, theme.id)
            assertEquals(colors.textDisabled, muted.off, theme.id)
        }
    }
}
