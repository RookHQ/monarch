package com.rookhq.monarch.components.intelligence

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.intelligence.aierrormessage.MonarchAiErrorAction
import com.rookhq.monarch.components.intelligence.aierrormessage.MonarchAiErrorKind
import com.rookhq.monarch.components.intelligence.aierrormessage.MonarchAiErrorMessage
import com.rookhq.monarch.components.intelligence.aierrormessage.MonarchAiErrorMessageLabels
import com.rookhq.monarch.components.intelligence.aierrormessage.MonarchAiErrorMessageTokens
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val Short = "Try again."

internal object MonarchAiErrorMessageFixtures : ComponentFixtures {
    override val id = "ai-error-message"
    override val fixtures = MonarchAiErrorKind.entries.map { kind ->
        // A short message keeps each fixture inside the test window at the largest font size.
        fixture(kind.name) {
            MonarchAiErrorMessage(kind = kind, message = Short, announce = false, onRetry = {}, onEditPrompt = {})
        }
    } + listOf(
        fixture("context overflow with summarize") {
            MonarchAiErrorMessage(
                kind = MonarchAiErrorKind.ContextOverflow,
                message = Short,
                announce = false,
                onNewConversation = {},
                onSummarize = {},
            )
        },
        fixture("counting down") {
            MonarchAiErrorMessage(
                kind = MonarchAiErrorKind.RateLimit,
                message = Short,
                retryAfterMillis = 12_000L,
                onRetry = {},
            )
        },
        fixture("retrying with details") {
            MonarchAiErrorMessage(
                message = Short,
                details = "request_id: req_8f3a2c",
                retrying = true,
                announce = false,
                onRetry = {},
            )
        },
        fixture("no actions, built-in text") {
            MonarchAiErrorMessage(kind = MonarchAiErrorKind.SafetyBlock, announce = false)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiErrorMessageTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun codesMapToKindsAsOnTheWeb() {
        assertEquals(MonarchAiErrorKind.RateLimit, MonarchAiErrorKind.fromCode("429"))
        assertEquals(MonarchAiErrorKind.RateLimit, MonarchAiErrorKind.fromCode("rate_limit_exceeded"))
        assertEquals(MonarchAiErrorKind.RateLimit, MonarchAiErrorKind.fromCode("http-429"))
        assertEquals(MonarchAiErrorKind.ContextOverflow, MonarchAiErrorKind.fromCode("context_length_exceeded"))
        assertEquals(MonarchAiErrorKind.ContextOverflow, MonarchAiErrorKind.fromCode("413"))
        assertEquals(MonarchAiErrorKind.SafetyBlock, MonarchAiErrorKind.fromCode("content_filter"))
        assertEquals(MonarchAiErrorKind.Network, MonarchAiErrorKind.fromCode("ETIMEDOUT"))
        assertEquals(MonarchAiErrorKind.Network, MonarchAiErrorKind.fromCode("http-503"))
        assertEquals(MonarchAiErrorKind.ToolFailure, MonarchAiErrorKind.fromCode("tool_error"))
        assertEquals(MonarchAiErrorKind.Unknown, MonarchAiErrorKind.fromCode("bad_request"))
        assertEquals(MonarchAiErrorKind.Unknown, MonarchAiErrorKind.fromCode(null))
    }

    @Test
    fun theToneNameTitleAndMessageAreOneElement() {
        rule.setMonarchContent { MonarchAiErrorMessage(kind = MonarchAiErrorKind.Network, announce = false) }
        rule.onNodeWithContentDescription(
            "Error. Connection lost. The assistant couldn’t be reached. Check your connection and try again.",
        ).assertExists()
    }

    @Test
    fun theFailureIsAnnouncedAssertivelyAfterItAppears() {
        rule.setMonarchContent {
            MonarchAiErrorMessage(kind = MonarchAiErrorKind.RateLimit, title = "Slow down", message = "Wait.")
        }
        rule.waitForIdle()
        val assertive = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive)
        val spoken = listOf("Warning. Slow down. Wait.")
        val text = SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, spoken)
        rule.onNode(assertive and text).assertExists()
    }

    @Test
    fun actionsFollowTheOrderOfTheKindAndCallBack() {
        var calls = ""
        rule.setMonarchContent {
            MonarchAiErrorMessage(
                kind = MonarchAiErrorKind.ContextOverflow,
                announce = false,
                onRetry = { calls += "r" },
                onNewConversation = { calls += "n" },
                onSummarize = { calls += "s" },
            )
        }
        rule.onNodeWithText("New conversation").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Summarize and continue").performClick()
        rule.onNodeWithText("Try again").performClick()
        assertEquals("nsr", calls)
        assertEquals(
            listOf(MonarchAiErrorAction.NewConversation, MonarchAiErrorAction.Summarize),
            MonarchAiErrorMessageTokens.order(MonarchAiErrorKind.ContextOverflow).take(2),
        )
    }

    @Test
    fun summarizeIsOnlyOfferedForALongConversation() {
        rule.setMonarchContent {
            MonarchAiErrorMessage(kind = MonarchAiErrorKind.Network, announce = false, onSummarize = {})
        }
        rule.onNodeWithText("Summarize and continue").assertDoesNotExist()
    }

    @Test
    fun retryCountsDownThenAnnouncesThatItIsReady() {
        rule.setMonarchContent {
            MonarchAiErrorMessage(
                kind = MonarchAiErrorKind.RateLimit,
                retryAfterMillis = 2_000L,
                announce = false,
                onRetry = {},
            )
        }
        rule.onNodeWithText("Try again in 2s").assertIsNotEnabled()
        rule.mainClock.advanceTimeBy(1_100L)
        rule.onNodeWithText("Try again in 1s").assertIsNotEnabled()
        rule.mainClock.advanceTimeBy(1_500L)
        rule.onNodeWithText("Try again").assertIsEnabled()
        rule.onNodeWithContentDescription("You can try again now.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun theDetailsAreBehindADisclosure() {
        rule.setMonarchContent { MonarchAiErrorMessage(details = "request_id: req_1", announce = false) }
        rule.onNodeWithText("request_id: req_1").assertDoesNotExist()
        rule.onNodeWithText("Technical details").performClick()
        rule.onNodeWithText("request_id: req_1").assertExists()
    }

    @Test
    fun theStringsCanBeReplacedAndExtraActionsAdded() {
        rule.setMonarchContent {
            MonarchAiErrorMessage(
                announce = false,
                labels = MonarchAiErrorMessageLabels(retry = "Réessayer"),
                onRetry = {},
            ) {
                MonarchButton(
                    text = "Learn more",
                    onClick = {},
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            }
        }
        rule.onNodeWithText("Réessayer").assertExists()
        rule.onNodeWithText("Learn more").assertExists()
    }

    @Test
    fun theMessageWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiErrorMessage(kind = MonarchAiErrorKind.Network, announce = false)
        }
        rule.onNodeWithText("Connection lost", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theToneAndColoursFollowTheKindInEveryTheme() {
        assertEquals(MonarchTone.Warning, MonarchAiErrorMessageTokens.tone(MonarchAiErrorKind.SafetyBlock))
        assertEquals(MonarchTone.Danger, MonarchAiErrorMessageTokens.tone(MonarchAiErrorKind.ToolFailure))
        assertEquals(12, MonarchAiErrorMessageTokens.secondsLeft(11_001L))
        assertTrue(MonarchAiErrorMessageTokens.countsDown(1L))
        assertFalse(MonarchAiErrorMessageTokens.countsDown(0L))
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val warning = MonarchAiErrorMessageTokens.colors(MonarchAiErrorKind.RateLimit, c)
            assertEquals(c.warningSubtle, warning.container, theme.id)
            assertEquals(c.warningBorder, warning.border, theme.id)
            val danger = MonarchAiErrorMessageTokens.colors(MonarchAiErrorKind.Network, c)
            assertEquals(c.dangerSubtle, danger.container, theme.id)
            assertEquals(c.dangerStrong, danger.content, theme.id)
        }
    }
}
