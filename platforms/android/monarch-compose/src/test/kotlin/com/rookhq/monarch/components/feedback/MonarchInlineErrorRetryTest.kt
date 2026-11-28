package com.rookhq.monarch.components.feedback

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import com.rookhq.monarch.components.feedback.inlineerrorretry.MonarchInlineErrorRetryTokens
import com.rookhq.monarch.components.feedback.inlineerrorretry.MonarchInlineErrorRetryVariant
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object MonarchInlineErrorRetryFixtures : ComponentFixtures {
    override val id = "inline-error-retry"
    override val fixtures = MonarchInlineErrorRetryVariant.entries.map { variant ->
        fixture("${variant.name}, danger and warning with icons") {
            Column {
                listOf(MonarchTone.Danger, MonarchTone.Warning).forEach { tone ->
                    MonarchInlineErrorRetry(
                        message = "Your earnings did not load.",
                        onRetry = {},
                        tone = tone,
                        variant = variant,
                        icon = { MonarchIcon(TestIcon, null, size = MonarchIconSize.Sm) },
                    )
                }
            }
        }
    } + listOf(
        fixture("every tone") {
            Column {
                MonarchTone.entries.forEach { tone ->
                    MonarchInlineErrorRetry(message = "${tone.name} message.", onRetry = {}, tone = tone)
                }
            }
        },
        fixture("retrying") { MonarchInlineErrorRetry(message = "Did not load.", onRetry = {}, retrying = true) },
        fixture("long text") {
            MonarchInlineErrorRetry(
                message = "The list of invoices for the last quarter could not be loaded from the billing service.",
                onRetry = {},
                variant = MonarchInlineErrorRetryVariant.Banner,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchInlineErrorRetryTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun toneAndMessageAreOnePoliteLiveRegion() {
        rule.setMonarchContent { MonarchInlineErrorRetry(message = "Did not load.", onRetry = {}) }
        rule.onNodeWithText("Did not load.")
            .assertTextEquals("Error", "Did not load.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun theToneLabelReplacesTheBuiltInToneName() {
        rule.setMonarchContent {
            MonarchInlineErrorRetry(
                message = "Showing saved data.",
                onRetry = {},
                tone = MonarchTone.Warning,
                toneLabel = "Out of date",
            )
        }
        rule.onNodeWithText("Showing saved data.").assertTextEquals("Out of date", "Showing saved data.")
    }

    @Test
    fun theRetryButtonCallsBackAndHasAFullTarget() {
        var retries = 0
        rule.setMonarchContent { MonarchInlineErrorRetry(message = "Did not load.", onRetry = { retries++ }) }
        rule.onNodeWithText("Try again").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun whileRetryingTheButtonIgnoresTapsAndSaysRetrying() {
        var retries = 0
        var retrying by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchInlineErrorRetry(message = "Did not load.", onRetry = { retries++ }, retrying = retrying)
        }
        val button = rule.onNodeWithText("Try again")
        assertEquals("Retrying", button.stateDescription())
        button.performClick()
        assertEquals(0, retries)
        retrying = false
        rule.onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchInlineErrorRetry(
                message = "Did not load.",
                onRetry = {},
                retrying = true,
                retryLabel = "Reload",
                retryingLabel = "Reloading",
            )
        }
        assertEquals("Reloading", rule.onNodeWithText("Reload").stateDescription())
    }

    @Test
    fun theButtonFollowsTheMessageInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchInlineErrorRetry(message = "Did not load.", onRetry = {}, modifier = Modifier.width(400.dp))
        }
        rule.onNodeWithText("Did not load.", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Try again"), LayoutDirection.Rtl)
    }

    @Test
    fun theButtonMovesUnderTheMessageWhenBothDoNotFit() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchInlineErrorRetry(
                message = "The invoices of the last quarter did not load.",
                onRetry = {},
                modifier = Modifier.width(300.dp),
            )
        }
        val message = rule.onNodeWithText("The invoices", substring = true, useUnmergedTree = true)
        message.assertTextFullyVisible()
        val button = rule.onNodeWithText("Try again").getUnclippedBoundsInRoot()
        assertTrue(button.top >= message.getUnclippedBoundsInRoot().bottom)
    }

    @Test
    fun coloursComeFromTheToneRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val inline = MonarchInlineErrorRetryTokens.colors(
                MonarchTone.Danger,
                MonarchInlineErrorRetryVariant.Inline,
                colors,
            )
            assertEquals(Color.Transparent, inline.container, theme.id)
            assertEquals(colors.dangerStrong, inline.content, theme.id)
            assertEquals(null, inline.border, theme.id)
            val banner = MonarchInlineErrorRetryTokens.colors(
                MonarchTone.Warning,
                MonarchInlineErrorRetryVariant.Banner,
                colors,
            )
            assertEquals(colors.warningSubtle, banner.container, theme.id)
            assertEquals(colors.warningStrong, banner.content, theme.id)
            assertEquals(colors.warningBorder, banner.border, theme.id)
        }
    }
}
