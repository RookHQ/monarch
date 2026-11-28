package dev.monarchui.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.forms.checkboxgroup.MonarchCheckboxGroupOption
import dev.monarchui.components.intelligence.aiconversation.MonarchAiMessageFeedback
import dev.monarchui.components.intelligence.aifeedback.MonarchAiFeedback
import dev.monarchui.components.intelligence.aifeedback.MonarchAiFeedbackDetails
import dev.monarchui.components.intelligence.aifeedback.MonarchAiFeedbackLabels
import dev.monarchui.components.intelligence.aifeedback.MonarchAiFeedbackRules
import dev.monarchui.components.intelligence.aifeedback.MonarchAiFeedbackSubmission
import dev.monarchui.components.intelligence.aifeedback.MonarchAiFeedbackTokens
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
import org.robolectric.annotation.Config
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Up = MonarchAiMessageFeedback.Up
private val Down = MonarchAiMessageFeedback.Down

internal object MonarchAiFeedbackFixtures : ComponentFixtures {
    override val id = "ai-feedback"
    override val fixtures = listOf(
        fixture("unrated chips") { MonarchAiFeedback(selection = null, onSelectionChange = {}, icon = null) },
        fixture("rated up") { MonarchAiFeedback(selection = Up, onSelectionChange = {}, icon = null) },
        fixture("icons, rated down") {
            MonarchAiFeedback(
                selection = Down,
                onSelectionChange = {},
                icon = { _, _ -> MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        },
        fixture("disabled") { MonarchAiFeedback(
            selection = null,
            onSelectionChange = {},
            enabled = false,
            icon = null,
        ) },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiFeedbackTest {
    @get:Rule
    val rule = createComposeRule()

    private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    @Test
    fun iconThumbsAreTogglesNamedByTheirRating() {
        var rating by mutableStateOf<MonarchAiMessageFeedback?>(null)
        var sent: MonarchAiFeedbackSubmission? = null
        rule.setMonarchContent {
            MonarchAiFeedback(
                selection = rating,
                onSelectionChange = { rating = it },
                icon = { _, _ -> MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            ) { sent = it }
        }
        rule.onNodeWithContentDescription("Rate this response").assertExists()
        rule.onNodeWithContentDescription("Good response")
            .assertIsOff()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitForIdle()
        assertEquals(Up, rating)
        rule.onNodeWithContentDescription("Good response").assertIsOn()
        assertEquals(MonarchAiFeedbackSubmission(Up, emptyList(), ""), sent)
        rule.onNode(polite).assertContentDescriptionEquals("Thanks for your feedback.")
    }

    @Test
    fun pressingTheCurrentThumbAgainClearsTheRating() {
        var rating by mutableStateOf<MonarchAiMessageFeedback?>(Up)
        rule.setMonarchContent {
            MonarchAiFeedback(
                selection = rating,
                onSelectionChange = { rating = it },
                asksDetails = MonarchAiFeedbackDetails.None,
                icon = null,
            )
        }
        rule.onNodeWithText("Good response")
            .assertIsSelected()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertNull(rating)
    }

    @Test
    fun thumbsDownOpensTheSheetAndSendsReasonsInListedOrder() {
        var rating by mutableStateOf<MonarchAiMessageFeedback?>(null)
        var sent: MonarchAiFeedbackSubmission? = null
        rule.setMonarchContent {
            MonarchAiFeedback(selection = rating, onSelectionChange = { rating = it }, icon = null) { sent = it }
        }
        rule.onNodeWithText("Bad response").performClick()
        rule.onNodeWithText("What went wrong?", useUnmergedTree = true).assertExists()
        rule.onNode(hasText("Incomplete")).performClick()
        rule.onNode(hasText("Inaccurate")).performClick()
        rule.onNode(hasSetTextAction()).performTextInput("  Missed the totals ")
        rule.onNodeWithText("Send feedback").performScrollTo().assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(
            MonarchAiFeedbackSubmission(Down, listOf("inaccurate", "incomplete"), "Missed the totals"),
            sent,
        )
        rule.onNodeWithText("What went wrong?", useUnmergedTree = true).assertDoesNotExist()
        rule.onNode(polite).assertContentDescriptionEquals("Thanks for your feedback.")
    }

    @Test
    fun aFailedSendKeepsTheSheetFilledInAndShowsTheError() {
        var rating by mutableStateOf<MonarchAiMessageFeedback?>(null)
        var fail = true
        var sent: MonarchAiFeedbackSubmission? = null
        rule.setMonarchContent {
            MonarchAiFeedback(selection = rating, onSelectionChange = { rating = it }, icon = null) {
                if (fail) throw IOException("offline")
                sent = it
            }
        }
        rule.onNodeWithText("Bad response").performClick()
        rule.onNode(hasText("Not helpful")).performClick()
        rule.onNodeWithText("Send feedback").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Your feedback could not be sent. Try again.", useUnmergedTree = true).assertExists()
        fail = false
        rule.onNodeWithText("Send feedback").performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(listOf("not-helpful"), sent?.reasons)
    }

    @Test
    fun aFailedDirectSendIsShownUnderTheThumbs() {
        var rating by mutableStateOf<MonarchAiMessageFeedback?>(null)
        rule.setMonarchContent {
            MonarchAiFeedback(
                selection = rating,
                onSelectionChange = { rating = it },
                icon = null,
            ) { throw IOException("x") }
        }
        rule.onNodeWithText("Good response").performClick()
        rule.waitForIdle()
        rule.onNode(polite).assertContentDescriptionEquals("Your feedback could not be sent. Try again.")
    }

    @Test
    fun cancelClosesTheSheetWithoutSending() {
        var rating by mutableStateOf<MonarchAiMessageFeedback?>(null)
        var sends = 0
        rule.setMonarchContent {
            MonarchAiFeedback(
                selection = rating,
                onSelectionChange = { rating = it },
                reasons = mapOf(Up to listOf(MonarchCheckboxGroupOption("clear", "Clear"))),
                asksDetails = MonarchAiFeedbackDetails.Both,
                icon = null,
            ) { sends++ }
        }
        rule.onNodeWithText("Good response").performClick()
        rule.onNodeWithText("What went well?", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Cancel").performScrollTo().performClick()
        rule.onNodeWithText("What went well?", useUnmergedTree = true).assertDoesNotExist()
        assertEquals(0, sends)
        assertEquals(Up, rating)
    }

    @Test
    fun disabledThumbsDoNothing() {
        var changes = 0
        rule.setMonarchContent {
            MonarchAiFeedback(selection = null, onSelectionChange = { changes++ }, enabled = false, icon = null)
        }
        rule.onNodeWithText("Good response").assertIsNotEnabled().performClick()
        assertEquals(0, changes)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiFeedback(
                selection = null,
                onSelectionChange = {},
                labels = MonarchAiFeedbackLabels(good = "Gut", bad = "Schlecht", group = "Bewerten"),
                icon = null,
            )
        }
        rule.onNodeWithText("Gut").assertExists()
        rule.onNodeWithContentDescription("Bewerten").assertExists()
    }

    @Test
    fun thumbsUpComesFirstInRightToLeftAndFitsAtTheLargestScale() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl, fontScale = LargestFontScale) {
            MonarchAiFeedback(selection = null, onSelectionChange = {}, icon = null)
        }
        rule.onNodeWithText("Good response")
            .assertIsBefore(rule.onNodeWithText("Bad response"), LayoutDirection.Rtl)
        rule.onNodeWithText("Bad response", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theRulesRateAndBuildTheSubmission() {
        val rules = MonarchAiFeedbackRules
        assertNull(rules.next(Up, Up))
        assertEquals(Down, rules.next(Up, Down))
        assertTrue(rules.wantsDetails(MonarchAiFeedbackDetails.Down, Down))
        assertFalse(rules.wantsDetails(MonarchAiFeedbackDetails.Down, Up))
        assertFalse(rules.wantsDetails(MonarchAiFeedbackDetails.None, Down))
        val defaults = listOf(MonarchCheckboxGroupOption("a", "A"))
        assertEquals(defaults, rules.reasonsFor(null, Down, defaults))
        assertEquals(emptyList(), rules.reasonsFor(null, Up, defaults))
        assertEquals(emptyList(), rules.reasonsFor(mapOf(Up to defaults), Down, defaults))
        val offered = listOf(MonarchCheckboxGroupOption("a", "A"), MonarchCheckboxGroupOption("b", "B"))
        assertEquals(
            MonarchAiFeedbackSubmission(Down, listOf("a", "b"), "x"),
            rules.submission(Down, offered, listOf("b", "zzz", "a"), " x "),
        )
    }

    @Test
    fun theColoursAreRolesInEveryTheme() {
        AllThemes.forEach { tokens ->
            val colors = MonarchColors(tokens.colors)
            assertEquals(colors.dangerStrong, MonarchAiFeedbackTokens.messageColor(true, colors))
            assertEquals(colors.textMuted, MonarchAiFeedbackTokens.messageColor(false, colors))
        }
    }
}
