package com.rookhq.monarch.components.intelligence

import androidx.compose.material3.Icon
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.intelligence.aisafetynotice.MonarchAiSafetyNotice
import com.rookhq.monarch.components.intelligence.aisafetynotice.MonarchAiSafetyNoticeKind
import com.rookhq.monarch.components.intelligence.aisafetynotice.MonarchAiSafetyNoticeLabels
import com.rookhq.monarch.components.intelligence.aisafetynotice.MonarchAiSafetyNoticeTokens
import com.rookhq.monarch.foundation.MonarchTone
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
import kotlin.test.assertNull

private const val Guidelines = "https://example.com/guidelines"

internal object MonarchAiSafetyNoticeFixtures : ComponentFixtures {
    override val id = "ai-safety-notice"
    override val fixtures = MonarchAiSafetyNoticeKind.entries.map { kind ->
        fixture(kind.name) { MonarchAiSafetyNotice(kind = kind, message = "Try again.") }
    } + listOf(
        fixture("reason and next steps") {
            MonarchAiSafetyNotice(
                kind = MonarchAiSafetyNoticeKind.Blocked,
                message = "Try again.",
                reason = "Personal data",
                nextSteps = "Remove the number.",
            )
        },
        fixture("with actions and icon") {
            MonarchAiSafetyNotice(
                kind = MonarchAiSafetyNoticeKind.Filtered,
                message = "Try again.",
                learnMoreUrl = Guidelines,
                icon = { Icon(TestIcon, contentDescription = null) },
                onEditPrompt = {},
            )
        },
        fixture("built-in text, unsafe link dropped") {
            MonarchAiSafetyNotice(kind = MonarchAiSafetyNoticeKind.Modified, learnMoreUrl = "javascript:alert(1)")
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiSafetyNoticeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTextIsOneElementInReadingOrder() {
        rule.setMonarchContent {
            MonarchAiSafetyNotice(
                kind = MonarchAiSafetyNoticeKind.Blocked,
                reason = "Personal data",
                nextSteps = "Remove the phone number.",
            )
        }
        val expected = listOf(
            "This request wasn’t answered",
            "It may go against the content guidelines. You can rephrase it and try again.",
            "Reason: Personal data",
            "Remove the phone number.",
        )
        val merged = SemanticsMatcher("reads the text in order") { node ->
            node.config.getOrNull(SemanticsProperties.Text)?.map { it.text } == expected
        }
        rule.onNode(merged).assertExists()
    }

    @Test
    fun eachKindHasItsOwnTitleAndTone() {
        rule.setMonarchContent { MonarchAiSafetyNotice(kind = MonarchAiSafetyNoticeKind.Filtered) }
        rule.onNodeWithText("Some content was hidden", substring = true).assertExists()
        assertEquals(MonarchTone.Warning, MonarchAiSafetyNoticeTokens.tone(MonarchAiSafetyNoticeKind.Blocked))
        assertEquals(MonarchTone.Info, MonarchAiSafetyNoticeTokens.tone(MonarchAiSafetyNoticeKind.Modified))
        assertEquals(MonarchTone.Info, MonarchAiSafetyNoticeTokens.tone(MonarchAiSafetyNoticeKind.Filtered))
    }

    @Test
    fun blankReasonAndNextStepsAreNotShown() {
        rule.setMonarchContent {
            MonarchAiSafetyNotice(kind = MonarchAiSafetyNoticeKind.Modified, reason = "  ", nextSteps = "")
        }
        rule.onNodeWithText("Reason:", substring = true).assertDoesNotExist()
        assertNull(MonarchAiSafetyNoticeTokens.present(" \n"))
    }

    @Test
    fun editPromptIsAButtonThatCallsBack() {
        var edits = 0
        rule.setMonarchContent {
            MonarchAiSafetyNotice(kind = MonarchAiSafetyNoticeKind.Blocked, onEditPrompt = { edits++ })
        }
        rule.onNodeWithText("Edit prompt")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, edits)
    }

    @Test
    fun theGuidelinesLinkOpensThroughTheUriHandler() {
        val opened = mutableListOf<String>()
        val handler = object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }
        rule.setMonarchContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                MonarchAiSafetyNotice(kind = MonarchAiSafetyNoticeKind.Blocked, learnMoreUrl = " $Guidelines ")
            }
        }
        rule.onNode(hasText("Content guidelines")).assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf(Guidelines), opened)
    }

    @Test
    fun onOpenLearnMoreReplacesTheUriHandler() {
        var link = ""
        rule.setMonarchContent {
            MonarchAiSafetyNotice(
                kind = MonarchAiSafetyNoticeKind.Blocked,
                learnMoreUrl = Guidelines,
                onOpenLearnMore = { link = it },
            )
        }
        rule.onNodeWithText("Content guidelines").performClick()
        assertEquals(Guidelines, link)
    }

    @Test
    fun unsafeLinksAreDropped() {
        rule.setMonarchContent {
            MonarchAiSafetyNotice(kind = MonarchAiSafetyNoticeKind.Blocked, learnMoreUrl = "javascript:alert(1)")
        }
        rule.onNodeWithText("Content guidelines").assertDoesNotExist()
        assertNull(MonarchAiSafetyNoticeTokens.safeUrl("file:///etc/hosts"))
        assertNull(MonarchAiSafetyNoticeTokens.safeUrl("myapp://x"))
        assertEquals("mailto:a@example.com", MonarchAiSafetyNoticeTokens.safeUrl("mailto:a@example.com"))
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiSafetyNotice(
                kind = MonarchAiSafetyNoticeKind.Blocked,
                reason = "Données",
                labels = MonarchAiSafetyNoticeLabels(
                    title = { "Bloqué: ${it.name}" },
                    reason = "Raison :",
                    editPrompt = "Modifier",
                ),
                onEditPrompt = {},
            )
        }
        rule.onNodeWithText("Bloqué: Blocked", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Raison : Données", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Modifier").assertExists()
    }

    @Test
    fun theTitleOverridesTheLabels() {
        rule.setMonarchContent {
            MonarchAiSafetyNotice(
                kind = MonarchAiSafetyNoticeKind.Blocked,
                title = "Not answered",
                labels = MonarchAiSafetyNoticeLabels(title = { "Ignored" }),
            )
        }
        rule.onNodeWithText("Not answered", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Ignored", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun theButtonComesBeforeTheLinkInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiSafetyNotice(
                kind = MonarchAiSafetyNoticeKind.Blocked,
                learnMoreUrl = Guidelines,
                onEditPrompt = {},
            )
        }
        rule.onNodeWithText("Edit prompt")
            .assertIsBefore(rule.onNodeWithText("Content guidelines"), LayoutDirection.Rtl)
    }

    @Test
    fun theTextWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiSafetyNotice(kind = MonarchAiSafetyNoticeKind.Filtered)
        }
        rule.onNodeWithText("Some content was hidden", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theColoursFollowTheKindInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val blocked = MonarchAiSafetyNoticeTokens.colors(MonarchAiSafetyNoticeKind.Blocked, c)
            assertEquals(c.warningSubtle, blocked.container, theme.id)
            assertEquals(c.warningBorder, blocked.border, theme.id)
            assertEquals(c.warningStrong, blocked.content, theme.id)
            val modified = MonarchAiSafetyNoticeTokens.colors(MonarchAiSafetyNoticeKind.Modified, c)
            assertEquals(c.infoSubtle, modified.container, theme.id)
            assertEquals(c.infoBorder, modified.border, theme.id)
        }
    }
}
