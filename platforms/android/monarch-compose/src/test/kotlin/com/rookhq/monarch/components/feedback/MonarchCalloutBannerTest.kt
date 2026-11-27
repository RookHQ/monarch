package com.rookhq.monarch.components.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.feedback.banner.MonarchBannerAnnouncement
import com.rookhq.monarch.components.feedback.calloutbanner.MonarchCalloutBanner
import com.rookhq.monarch.components.feedback.calloutbanner.MonarchCalloutBannerPlacement
import com.rookhq.monarch.components.feedback.calloutbanner.MonarchCalloutBannerTokens
import com.rookhq.monarch.components.feedback.calloutbanner.MonarchCalloutBannerVariant
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

internal object MonarchCalloutBannerFixtures : ComponentFixtures {
    override val id = "callout-banner"
    override val fixtures = MonarchTone.entries.map { tone ->
        fixture("${tone.name}, title, icon, action and dismiss") {
            MonarchCalloutBanner(
                title = "${tone.name} callout",
                message = "A message in this tone.",
                tone = tone,
                icon = { MonarchIcon(TestIcon, contentDescription = null) },
                action = { MonarchButton(text = "Action", onClick = {}, size = MonarchButtonSize.Sm) },
                onDismiss = {},
                dismissIcon = null,
            )
        }
    } + MonarchCalloutBannerVariant.entries.flatMap { variant ->
        MonarchCalloutBannerPlacement.entries.map { placement ->
            fixture("${variant.name}, ${placement.name}") {
                MonarchCalloutBanner(
                    message = "Showing tickets in Support",
                    variant = variant,
                    placement = placement,
                    action = { MonarchButton(text = "Show all", onClick = {}, size = MonarchButtonSize.Sm) },
                    onDismiss = {},
                    dismissIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                    icon = null,
                )
            }
        }
    } + listOf(
        fixture("long text, announced") {
            MonarchCalloutBanner(
                title = "Scheduled maintenance on Saturday from midnight to four in the morning",
                message = "Exports, imports and the public API are unavailable during the window. Plan large " +
                    "imports for another day.",
                tone = MonarchTone.Warning,
                announcement = MonarchBannerAnnouncement.Polite,
                onDismiss = {},
                icon = null,
                dismissIcon = null,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchCalloutBannerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun toneTitleAndMessageAreOneElementReadInThatOrder() {
        rule.setMonarchContent {
            MonarchCalloutBanner(
                title = "Payment failed",
                message = "Update your card.",
                tone = MonarchTone.Danger,
                icon = null,
                dismissIcon = null,
            )
        }
        rule.onNodeWithText("Payment failed")
            .assertTextEquals("Error", "Payment failed", "Update your card.")
            .assertHasNoClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
    }

    @Test
    fun theToneLabelReplacesTheBuiltInToneName() {
        rule.setMonarchContent {
            MonarchCalloutBanner(
                message = "Trial ends soon.",
                tone = MonarchTone.Warning,
                toneLabel = "Heads up",
                icon = null,
                dismissIcon = null,
            )
        }
        rule.onNodeWithText("Trial ends soon.").assertTextEquals("Heads up", "Trial ends soon.")
    }

    @Test
    fun theNeutralToneAndTheScopeVariantHaveNoSpokenTone() {
        rule.setMonarchContent {
            Column {
                MonarchCalloutBanner(message = "Neutral.", tone = MonarchTone.Neutral, icon = null, dismissIcon = null)
                MonarchCalloutBanner(
                    message = "Showing tickets in Support",
                    tone = MonarchTone.Info,
                    variant = MonarchCalloutBannerVariant.Scope,
                    icon = null,
                    dismissIcon = null,
                )
            }
        }
        rule.onNodeWithText("Neutral.").assertTextEquals("Neutral.")
        rule.onNodeWithText("Showing tickets in Support").assertTextEquals("Showing tickets in Support")
    }

    @Test
    fun anAnnouncedBannerIsALiveRegion() {
        rule.setMonarchContent {
            MonarchCalloutBanner(
                message = "Sync failed.",
                announcement = MonarchBannerAnnouncement.Polite,
                icon = null,
                dismissIcon = null,
            )
        }
        rule.onNodeWithText("Sync failed.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun theTextDismissButtonIsNamedDismissAndCallsBack() {
        var dismissed = 0
        rule.setMonarchContent { MonarchCalloutBanner(
            message = "Saved.",
            onDismiss = { dismissed++ },
            icon = null,
            dismissIcon = null,
        ) }
        rule.onNodeWithText("Dismiss")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun theIconDismissButtonIsNamedByTheDismissLabel() {
        var dismissed = 0
        rule.setMonarchContent {
            MonarchCalloutBanner(
                message = "Saved.",
                onDismiss = { dismissed++ },
                dismissIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                dismissLabel = "Close banner",
                icon = null,
            )
        }
        rule.onNodeWithContentDescription("Close banner")
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun theActionIsASeparateButton() {
        var upgrades = 0
        rule.setMonarchContent {
            MonarchCalloutBanner(
                message = "Your trial ends in 3 days.",
                action = { MonarchButton(text = "Upgrade", onClick = { upgrades++ }, size = MonarchButtonSize.Sm) },
                icon = null,
                dismissIcon = null,
            )
        }
        rule.onNodeWithText("Upgrade").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, upgrades)
    }

    @Test
    fun theIconIsHiddenAndDismissIsAtTheEndInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchCalloutBanner(
                message = "Message",
                icon = { Box(Modifier.testTag("icon").size(20.dp)) },
                onDismiss = {},
                dismissIcon = null,
            )
        }
        val message = rule.onNodeWithText("Message", useUnmergedTree = true)
        rule.onNodeWithTag("icon").assertDoesNotExist()
        message.assertIsBefore(rule.onNodeWithText("Dismiss"), LayoutDirection.Rtl)
    }

    @Test
    fun coloursComeFromTheToneRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val warning = MonarchCalloutBannerTokens.colors(MonarchTone.Warning, colors)
            assertEquals(colors.warningSubtle, warning.container, theme.id)
            assertEquals(colors.warningStrong, warning.content, theme.id)
            assertEquals(colors.warningBorder, warning.border, theme.id)
            val neutral = MonarchCalloutBannerTokens.colors(MonarchTone.Neutral, colors)
            assertEquals(colors.bgSubtle, neutral.container, theme.id)
            assertEquals(colors.textMuted, neutral.content, theme.id)
            assertEquals(colors.borderDefault, neutral.border, theme.id)
        }
    }

    @Test
    fun theTextIsNotCutOffAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchCalloutBanner(
                title = "Scheduled maintenance",
                message = "Exports and imports are unavailable on Saturday night.",
                onDismiss = {},
                icon = null,
                dismissIcon = null,
            )
        }
        rule.onNodeWithText("Exports", substring = true, useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Dismiss").assertTouchTargetAtLeast48dp(rule.density)
    }
}
