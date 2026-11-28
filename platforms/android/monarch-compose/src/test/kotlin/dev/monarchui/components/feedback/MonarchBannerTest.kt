package dev.monarchui.components.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.feedback.banner.MonarchBanner
import dev.monarchui.components.feedback.banner.MonarchBannerAnnouncement
import dev.monarchui.components.feedback.banner.MonarchBannerTokens
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

internal object MonarchBannerFixtures : ComponentFixtures {
    override val id = "alert"
    override val fixtures = MonarchTone.entries.map { tone ->
        fixture("${tone.name}, title, icon and action") {
            MonarchBanner(
                title = "${tone.name} banner",
                message = "A message in this tone.",
                tone = tone,
                icon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                action = {
                    MonarchButton(
                        text = "Action",
                        onClick = {},
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                    )
                },
            )
        }
    } + listOf(
        fixture("message only") { MonarchBanner(message = "Draft saved.") },
        fixture("long text, announced") {
            MonarchBanner(
                title = "Your payment method was declined by the bank",
                message = "Check the card number, the expiry date and the security code, or use another card.",
                tone = MonarchTone.Danger,
                announcement = MonarchBannerAnnouncement.Assertive,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchBannerTest {
    @get:Rule
    val rule = createComposeRule()

    private fun liveRegion(mode: LiveRegionMode) = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, mode)
    private val noLiveRegion = SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion)

    @Test
    fun titleAndMessageAreOneElementAndSilentByDefault() {
        rule.setMonarchContent {
            MonarchBanner(title = "Saved", message = "Your changes are live.", tone = MonarchTone.Success)
        }
        rule.onNodeWithText("Saved").assertTextContains("Your changes are live.").assert(noLiveRegion)
    }

    @Test
    fun aPoliteBannerIsAPoliteLiveRegion() {
        rule.setMonarchContent {
            MonarchBanner(message = "Draft saved.", announcement = MonarchBannerAnnouncement.Polite)
        }
        rule.onNodeWithText("Draft saved.").assert(liveRegion(LiveRegionMode.Polite))
    }

    @Test
    fun anAssertiveBannerIsAnAssertiveLiveRegion() {
        rule.setMonarchContent {
            MonarchBanner(
                message = "Payment failed.",
                tone = MonarchTone.Danger,
                announcement = MonarchBannerAnnouncement.Assertive,
            )
        }
        rule.onNodeWithText("Payment failed.").assert(liveRegion(LiveRegionMode.Assertive))
    }

    @Test
    fun theActionStaysASeparateButtonWithAFullTouchTarget() {
        var retries = 0
        rule.setMonarchContent {
            MonarchBanner(
                message = "Payment failed.",
                tone = MonarchTone.Danger,
                action = {
                    MonarchButton(
                        text = "Retry",
                        onClick = { retries++ },
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                    )
                },
            )
        }
        rule.onNodeWithText("Payment failed.").assertHasNoClickAction()
        rule.onNodeWithText("Retry")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, retries)
    }

    @Test
    fun theIconIsAtTheStartAndTheActionAtTheEndInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchBanner(
                message = "Message",
                icon = { Box(Modifier.testTag("icon").size(20.dp)) },
                action = { Box(Modifier.testTag("action").size(20.dp)) },
            )
        }
        val message = rule.onNodeWithText("Message")
        rule.onNodeWithTag("icon").assertIsBefore(message, LayoutDirection.Rtl)
        message.assertIsBefore(rule.onNodeWithTag("action"), LayoutDirection.Rtl)
    }

    @Test
    fun coloursComeFromTheToneRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val warning = MonarchBannerTokens.colors(MonarchTone.Warning, colors)
            assertEquals(colors.warningSubtle, warning.container, theme.id)
            assertEquals(colors.warningStrong, warning.content, theme.id)
            assertEquals(colors.warningBorder, warning.border, theme.id)
            val neutral = MonarchBannerTokens.colors(MonarchTone.Neutral, colors)
            assertEquals(colors.bgSubtle, neutral.container, theme.id)
            assertEquals(colors.textMuted, neutral.content, theme.id)
            assertEquals(colors.borderDefault, neutral.border, theme.id)
        }
    }

    @Test
    fun theTextIsNotCutOffAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchBanner(title = "Payment failed", message = "Your card was declined. Try another payment method.")
        }
        rule.onNodeWithText("Your card was declined.", substring = true, useUnmergedTree = true)
            .assertTextFullyVisible()
    }
}
