package dev.monarchui.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.intelligence.aiconversation.MonarchAiMessageFeedback
import dev.monarchui.components.intelligence.aimessageactions.MonarchAiMessageAction
import dev.monarchui.components.intelligence.aimessageactions.MonarchAiMessageActions
import dev.monarchui.components.intelligence.aimessageactions.MonarchAiMessageActionsLabels
import dev.monarchui.components.intelligence.aimessageactions.MonarchAiMessageActionsTokens
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

private const val Answer = "The invoice is due on 4 November."

internal object MonarchAiMessageActionsFixtures : ComponentFixtures {
    override val id = "ai-message-actions"
    override val fixtures = listOf(
        fixture("every action with icons") {
            MonarchAiMessageActions(
                messageText = Answer,
                onCopy = {},
                onRegenerate = {},
                feedback = MonarchAiMessageFeedback.Up,
                onFeedback = {},
                onShare = {},
                onReadAloud = {},
                icon = { _, _ -> MonarchIcon(TestIcon, contentDescription = null) },
                moreIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                more = { close -> MonarchMenuItem(text = "Report", onClick = close) },
            )
        },
        fixture("as text, thumbs down") {
            MonarchAiMessageActions(
                messageText = Answer,
                onCopy = {},
                onRegenerate = {},
                feedback = MonarchAiMessageFeedback.Down,
                onFeedback = {},
                icon = null,
                moreIcon = null,
            )
        },
        fixture("own message, speaking") {
            MonarchAiMessageActions(
                messageText = Answer,
                onEdit = {},
                speaking = true,
                onReadAloud = {},
                icon = null,
                moreIcon = null,
            )
        },
        fixture("disabled") {
            MonarchAiMessageActions(
                messageText = Answer,
                onCopy = {},
                onRegenerate = {},
                onFeedback = {},
                enabled = false,
                icon = { _, _ -> MonarchIcon(TestIcon, contentDescription = null) },
                moreIcon = null,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiMessageActionsTest {
    @get:Rule
    val rule = createComposeRule()

    private val icon: @androidx.compose.runtime.Composable (MonarchAiMessageAction, Boolean) -> Unit =
        { _, _ -> MonarchIcon(TestIcon, contentDescription = null) }

    @Test
    fun onlyActionsWithACallbackAreShown() {
        rule.setMonarchContent { MonarchAiMessageActions(
            messageText = Answer,
            onRegenerate = {},
            icon = null,
            moreIcon = null,
        ) }
        rule.onNodeWithText("Regenerate").assertExists()
        rule.onNodeWithContentDescription("Copy").assertDoesNotExist()
        rule.onNodeWithText("Edit").assertDoesNotExist()
        rule.onNodeWithText("Good response").assertDoesNotExist()
        rule.onNodeWithContentDescription("Message actions").assertExists()
    }

    @Test
    fun iconButtonsAreNamedAndHave48dpTargets() {
        var regenerated = 0
        var shared = 0
        rule.setMonarchContent {
            MonarchAiMessageActions(
                messageText = Answer,
                onCopy = {},
                onRegenerate = { regenerated++ },
                onShare = { shared++ },
                icon = icon,
                moreIcon = null,
            )
        }
        rule.onNodeWithContentDescription("Regenerate")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithContentDescription("Share").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("Copy").assertTouchTargetAtLeast48dp(rule.density)
        assertEquals(1, regenerated)
        assertEquals(1, shared)
    }

    @Test
    fun copyWritesTheTextAndCallsBack() {
        val copied = mutableListOf<String>()
        rule.setMonarchContent { MonarchAiMessageActions(
            messageText = Answer,
            onCopy = { copied += it },
            icon = null,
            moreIcon = null,
        ) }
        // MonarchCopyButton names itself with a content description and hides its two visible labels.
        rule.onNodeWithContentDescription("Copy").performClick()
        rule.waitForIdle()
        assertEquals(listOf(Answer), copied)
    }

    @Test
    fun thumbsToggleAndPressingAgainClears() {
        var feedback by mutableStateOf<MonarchAiMessageFeedback?>(null)
        rule.setMonarchContent {
            MonarchAiMessageActions(
                messageText = Answer,
                feedback = feedback,
                onFeedback = { feedback = it },
                icon = icon,
                moreIcon = null,
            )
        }
        val good = rule.onNodeWithContentDescription("Good response")
        good.assertIsOff().assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(MonarchAiMessageFeedback.Up, feedback)
        good.assertIsOn()
        rule.onNodeWithContentDescription("Bad response").performClick()
        assertEquals(MonarchAiMessageFeedback.Down, feedback)
        rule.onNodeWithContentDescription("Bad response").assertIsOn().performClick()
        assertEquals(null, feedback)
    }

    @Test
    fun withoutIconsThumbsAreChipsWithTheirNames() {
        rule.setMonarchContent {
            MonarchAiMessageActions(
                messageText = Answer,
                feedback = MonarchAiMessageFeedback.Down,
                onFeedback = {},
                icon = null,
                moreIcon = null,
            )
        }
        rule.onNodeWithText("Bad response").assertIsSelected().assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Good response").assertExists()
    }

    @Test
    fun readAloudAsksTheAppToStartAndStop() {
        var speaking by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchAiMessageActions(
                messageText = Answer,
                speaking = speaking,
                onReadAloud = { speaking = it },
                icon = null,
                moreIcon = null,
            )
        }
        rule.onNodeWithText("Read aloud").performClick()
        assertEquals(true, speaking)
        rule.onNodeWithText("Stop reading").performClick()
        assertEquals(false, speaking)
        rule.onNodeWithText("Read aloud").assertExists()
    }

    @Test
    fun disabledActionsDoNotFire() {
        var edits = 0
        rule.setMonarchContent {
            MonarchAiMessageActions(
                messageText = Answer,
                onEdit = { edits++ },
                onFeedback = {},
                enabled = false,
                icon = null,
                moreIcon = null,
            )
        }
        rule.onNodeWithText("Edit").assertIsNotEnabled().performClick()
        rule.onNodeWithText("Good response").assertIsNotEnabled()
        assertEquals(0, edits)
    }

    @Test
    fun theMoreMenuOpensAndCloses() {
        var reported = 0
        rule.setMonarchContent {
            MonarchAiMessageActions(
                messageText = Answer,
                more = { close ->
                    MonarchMenuItem(
                        text = "Report",
                        onClick = {
                            reported++
                            close()
                        },
                    )
                },
                icon = null,
                moreIcon = null,
            )
        }
        rule.onNodeWithText("More actions").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Report").performClick()
        assertEquals(1, reported)
        rule.onNodeWithText("Report").assertDoesNotExist()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchAiMessageActions(
                messageText = Answer,
                onRegenerate = {},
                onReadAloud = {},
                labels = MonarchAiMessageActionsLabels(
                    toolbar = "Aktionen",
                    regenerate = "Neu erzeugen",
                    readAloud = "Vorlesen",
                ),
                icon = null,
                moreIcon = null,
            )
        }
        rule.onNodeWithContentDescription("Aktionen").assertExists()
        rule.onNodeWithText("Neu erzeugen").assertExists()
        rule.onNodeWithText("Vorlesen").assertExists()
    }

    @Test
    fun theOrderFollowsTheLayoutDirection() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiMessageActions(messageText = Answer, onRegenerate = {}, onShare = {}, icon = icon, moreIcon = null)
        }
        rule.onNodeWithContentDescription("Regenerate")
            .assertIsBefore(rule.onNodeWithContentDescription("Share"), LayoutDirection.Rtl)
    }

    @Test
    fun textActionsWrapAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiMessageActions(
                messageText = Answer,
                onRegenerate = {},
                onShare = {},
                onReadAloud = {},
                icon = null,
                moreIcon = null,
            )
        }
        rule.onNodeWithText("Regenerate").assertTextFullyVisible()
        rule.onNodeWithText("Read aloud").assertTextFullyVisible()
    }

    @Test
    fun theRatingLogicHolds() {
        val tokens = MonarchAiMessageActionsTokens
        assertEquals(MonarchAiMessageFeedback.Up, tokens.nextFeedback(null, MonarchAiMessageFeedback.Up))
        assertEquals(null, tokens.nextFeedback(MonarchAiMessageFeedback.Up, MonarchAiMessageFeedback.Up))
        assertEquals(
            MonarchAiMessageFeedback.Down,
            tokens.nextFeedback(MonarchAiMessageFeedback.Up, MonarchAiMessageFeedback.Down),
        )
    }
}
