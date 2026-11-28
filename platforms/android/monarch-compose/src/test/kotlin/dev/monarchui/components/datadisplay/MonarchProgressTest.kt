package dev.monarchui.components.datadisplay

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.datadisplay.progress.MonarchProgressSize
import dev.monarchui.components.datadisplay.progress.MonarchProgressTokens
import dev.monarchui.components.datadisplay.progress.MonarchProgressVariant
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

internal object MonarchProgressFixtures : ComponentFixtures {
    override val id = "progress-bar"
    override val fixtures = listOf(
        fixture("linear, determinate") { MonarchProgress(progress = 0.6f, label = "Uploading report") },
        fixture("linear, empty and full") {
            Column {
                MonarchProgress(progress = 0f, label = "Not started")
                MonarchProgress(progress = 1f, label = "Done")
            }
        },
        fixture("linear, indeterminate") { MonarchProgress() },
        fixture("circular, determinate") {
            MonarchProgress(progress = 0.6f, variant = MonarchProgressVariant.Circular, label = "Syncing")
        },
        fixture("every tone") {
            Column {
                MonarchTone.entries.forEach { MonarchProgress(progress = 0.5f, label = it.name, tone = it) }
            }
        },
        fixture("every size, with value") {
            Column {
                MonarchProgressSize.entries.forEach { MonarchProgress(progress = 0.4f, size = it, showValue = true) }
            }
        },
        fixture("rings with value") {
            Column {
                MonarchProgressSize.entries.forEach {
                    MonarchProgress(
                        progress = 1f,
                        variant = MonarchProgressVariant.Circular,
                        size = it,
                        showValue = true,
                    )
                }
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchProgressTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun determinateProgressExposesItsValueAndLabel() {
        rule.setMonarchContent {
            Column {
                MonarchProgress(progress = 0.6f, label = "Uploading report")
                MonarchProgress(progress = 0.25f, variant = MonarchProgressVariant.Circular, label = "Syncing")
            }
        }
        rule.onNodeWithContentDescription("Uploading report")
            .assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.6f, 0f..1f)))
        rule.onNodeWithContentDescription("Syncing")
            .assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.25f, 0f..1f)))
    }

    @Test
    fun progressOutsideTheRangeIsClamped() {
        rule.setMonarchContent { MonarchProgress(progress = 1.5f, label = "Done") }
        rule.onNodeWithContentDescription("Done").assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(1f, 0f..1f)))
    }

    @Test
    fun indeterminateLinearProgressHasADefaultLabel() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchProgress(modifier = Modifier.testTag("linear")) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("linear")
            .assertContentDescriptionEquals("Loading")
            .assert(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
    }

    @Test
    fun theToneSetsTheIndicatorColourInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            fun indicator(tone: MonarchTone) = MonarchProgressTokens.colors(tone, colors).indicator
            assertEquals(colors.accentStrong, indicator(MonarchTone.Accent), theme.id)
            assertEquals(colors.dangerStrong, indicator(MonarchTone.Danger), theme.id)
            assertEquals(colors.successStrong, indicator(MonarchTone.Success), theme.id)
            assertEquals(colors.aiStrong, indicator(MonarchTone.Ai), theme.id)
            assertEquals(colors.textMuted, indicator(MonarchTone.Neutral), theme.id)
            assertEquals(colors.bgEmphasis, MonarchProgressTokens.colors(MonarchTone.Warning, colors).track, theme.id)
        }
    }

    @Test
    fun theSizeSetsTheBarHeightAndTheRingDiameter() {
        rule.setMonarchContent {
            Column {
                MonarchProgress(progress = 0.5f, label = "bar", size = MonarchProgressSize.Lg)
                MonarchProgress(
                    progress = 0.5f,
                    label = "ring",
                    variant = MonarchProgressVariant.Circular,
                    size = MonarchProgressSize.Sm,
                )
            }
        }
        assertEquals(MonarchTokens.Space.space3.dp, MonarchProgressTokens.barHeight(MonarchProgressSize.Lg))
        assertEquals(MonarchTokens.Space.space2.dp, MonarchProgressTokens.barHeight(MonarchProgressSize.Md))
        assertEquals(MonarchTokens.Space.space1.dp, MonarchProgressTokens.barHeight(MonarchProgressSize.Sm))
        assertEquals(MonarchTokens.Space.space10.dp, MonarchProgressTokens.ringDiameter(MonarchProgressSize.Md))
        rule.onNodeWithContentDescription("ring").assertWidthIsEqualTo(MonarchTokens.Space.space6.dp)
    }

    @Test
    fun theValueIsShownButReadOnceFromTheIndicator() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Column {
                MonarchProgress(progress = 0.42f, label = "Storage", showValue = true)
                MonarchProgress(
                    progress = 0.75f,
                    label = "Score",
                    variant = MonarchProgressVariant.Circular,
                    size = MonarchProgressSize.Lg,
                    showValue = true,
                )
            }
        }
        rule.onNodeWithText("42%", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("75%", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithContentDescription("Storage")
            .assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.42f, 0f..1f)))
        rule.onNodeWithContentDescription("Score")
            .assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.75f, 0f..1f)))
            .assertWidthIsAtLeast(MonarchTokens.Space.space16.dp)
        rule.onNodeWithText("42%", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility))
    }

    @Test
    fun anIndeterminateIndicatorShowsNoValue() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchProgress(showValue = true, label = "Working") }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("%", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun withAnimationsOffAnIndeterminateIndicatorStandsStillAndIsStillIndeterminate() {
        rule.setContent {
            MonarchTheme(tokens = MonarchTokens.light, reducedMotion = true) {
                Column {
                    MonarchProgress(modifier = Modifier.testTag("bar"), label = "Loading list")
                    MonarchProgress(
                        modifier = Modifier.testTag("ring"),
                        variant = MonarchProgressVariant.Circular,
                        label = "Loading ring",
                    )
                }
            }
        }
        // No endless animation: the test clock can settle.
        rule.waitForIdle()
        rule.onNodeWithTag("bar")
            .assertContentDescriptionEquals("Loading list")
            .assert(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
        rule.onNodeWithTag("ring")
            .assertContentDescriptionEquals("Loading ring")
            .assert(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
    }
}
