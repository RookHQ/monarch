package com.rookhq.monarch.components.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
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
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.banner.MonarchBannerAnnouncement
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNoticeTokens
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNoticeVariant
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
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

internal object MonarchInlineNoticeFixtures : ComponentFixtures {
    override val id = "inline-notice"
    override val fixtures = MonarchInlineNoticeVariant.entries.map { variant ->
        fixture("${variant.name}, every tone with an icon") {
            Column {
                MonarchTone.entries.forEach { tone ->
                    MonarchInlineNotice(
                        title = "${tone.name}.",
                        message = "A notice.",
                        tone = tone,
                        variant = variant,
                        icon = { MonarchIcon(TestIcon, null, size = MonarchIconSize.Sm) },
                        dismissIcon = null,
                    )
                }
            }
        }
    } + listOf(
        fixture("action and text dismiss") {
            MonarchInlineNotice(
                message = "Two people are editing this record.",
                tone = MonarchTone.Warning,
                action = {
                    MonarchButton(
                        text = "Reload",
                        onClick = {},
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                    )
                },
                onDismiss = {},
                dismissIcon = null,
            )
        },
        fixture("icon dismiss, announced") {
            MonarchInlineNotice(
                message = "Saved.",
                tone = MonarchTone.Success,
                onDismiss = {},
                dismissIcon = { MonarchIcon(TestIcon, null) },
                announcement = MonarchBannerAnnouncement.Polite,
            )
        },
        fixture("long text") {
            MonarchInlineNotice(
                title = "Conflict.",
                message = "Someone else changed the billing address of this account while you were editing it.",
                tone = MonarchTone.Danger,
                dismissIcon = null,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchInlineNoticeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun toneTitleAndMessageAreOneElement() {
        rule.setMonarchContent {
            MonarchInlineNotice(
                title = "Conflict.",
                message = "Reload to see the changes.",
                tone = MonarchTone.Warning,
                dismissIcon = null,
            )
        }
        rule.onNodeWithText("Conflict.", substring = true)
            .assertTextEquals("Warning", "Conflict.", "Reload to see the changes.")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
    }

    @Test
    fun theNeutralToneIsNotSpokenAndTheToneLabelReplacesTheName() {
        rule.setMonarchContent {
            Column {
                MonarchInlineNotice(message = "Neutral.", tone = MonarchTone.Neutral, dismissIcon = null)
                MonarchInlineNotice(
                    message = "Done.",
                    tone = MonarchTone.Success,
                    toneLabel = "All good",
                    dismissIcon = null,
                )
            }
        }
        rule.onNodeWithText("Neutral.").assertTextEquals("Neutral.")
        rule.onNodeWithText("Done.").assertTextEquals("All good", "Done.")
    }

    @Test
    fun anAssertiveNoticeIsAnAssertiveLiveRegion() {
        rule.setMonarchContent {
            MonarchInlineNotice(
                message = "Card declined.",
                tone = MonarchTone.Danger,
                announcement = MonarchBannerAnnouncement.Assertive,
                dismissIcon = null,
            )
        }
        rule.onNodeWithText("Card declined.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive))
    }

    @Test
    fun theDismissButtonsCallBackAndHaveFullTargets() {
        var dismissed = 0
        rule.setMonarchContent {
            Column {
                MonarchInlineNotice(message = "One.", onDismiss = { dismissed++ }, dismissIcon = null)
                MonarchInlineNotice(
                    message = "Two.",
                    onDismiss = { dismissed++ },
                    dismissIcon = { MonarchIcon(TestIcon, null) },
                    dismissLabel = "Hide notice",
                )
            }
        }
        rule.onNodeWithText("Dismiss").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("Hide notice").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(2, dismissed)
    }

    @Test
    fun theIconIsHiddenAndTheActionIsAtTheEndInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchInlineNotice(
                message = "Message",
                icon = { Box(Modifier.testTag("icon").size(16.dp)) },
                action = { Box(Modifier.testTag("action").size(16.dp)) },
                dismissIcon = null,
            )
        }
        rule.onNodeWithTag("icon").assertDoesNotExist()
        rule.onNodeWithText("Message", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithTag("action", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun coloursComeFromTheToneRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val subtle = MonarchInlineNoticeTokens.colors(MonarchTone.Danger, MonarchInlineNoticeVariant.Subtle, colors)
            assertEquals(colors.dangerSubtle, subtle.container, theme.id)
            assertEquals(colors.dangerStrong, subtle.content, theme.id)
            val plain = MonarchInlineNoticeTokens.colors(MonarchTone.Danger, MonarchInlineNoticeVariant.Plain, colors)
            assertEquals(Color.Transparent, plain.container, theme.id)
            assertEquals(
                colors.textDefault,
                MonarchInlineNoticeTokens.textColor(MonarchTone.Danger, MonarchInlineNoticeVariant.Subtle, colors),
                theme.id,
            )
            assertEquals(
                colors.dangerStrong,
                MonarchInlineNoticeTokens.textColor(MonarchTone.Danger, MonarchInlineNoticeVariant.Plain, colors),
                theme.id,
            )
            assertEquals(
                colors.textMuted,
                MonarchInlineNoticeTokens.textColor(MonarchTone.Neutral, MonarchInlineNoticeVariant.Plain, colors),
                theme.id,
            )
            val neutral =
                MonarchInlineNoticeTokens.colors(MonarchTone.Neutral, MonarchInlineNoticeVariant.Subtle, colors)
            assertEquals(colors.bgMuted, neutral.container, theme.id)
        }
    }

    @Test
    fun theTextWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchInlineNotice(
                modifier = Modifier.width(300.dp),
                title = "Conflict.",
                message = "Someone else changed this record while you were editing it.",
                onDismiss = {},
                dismissIcon = null,
            )
        }
        rule.onNodeWithText("Conflict.", substring = true, useUnmergedTree = true).assertTextFullyVisible()
    }
}
