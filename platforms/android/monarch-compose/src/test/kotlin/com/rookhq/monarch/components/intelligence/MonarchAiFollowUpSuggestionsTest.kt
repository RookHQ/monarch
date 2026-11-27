package com.rookhq.monarch.components.intelligence

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.intelligence.aifollowupsuggestions.MonarchAiFollowUpSuggestions
import com.rookhq.monarch.components.intelligence.aifollowupsuggestions.MonarchAiFollowUpSuggestionsTokens
import com.rookhq.monarch.internal.MonarchControlState
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
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
import kotlin.test.assertTrue

private val followUps = listOf("What changed since last quarter?", "Show it as a table")

internal object MonarchAiFollowUpSuggestionsFixtures : ComponentFixtures {
    override val id = "ai-follow-up-suggestions"
    override val fixtures = listOf(
        fixture("two suggestions") { MonarchAiFollowUpSuggestions(suggestions = followUps, onSelect = {}) },
        fixture("disabled") { MonarchAiFollowUpSuggestions(suggestions = followUps, onSelect = {}, enabled = false) },
        fixture("long text") {
            MonarchAiFollowUpSuggestions(
                suggestions = listOf("Which customers are at risk of leaving this month, and what changed for them?"),
                onSelect = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiFollowUpSuggestionsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun choosingASuggestionHandsItsTextToTheApp() {
        var chosen: String? = null
        rule.setMonarchContent { MonarchAiFollowUpSuggestions(suggestions = followUps, onSelect = { chosen = it }) }
        rule.onNodeWithText("Show it as a table").performClick()
        assertEquals("Show it as a table", chosen)
    }

    @Test
    fun eachChipIsAButtonWithA48dpTarget() {
        rule.setMonarchContent { MonarchAiFollowUpSuggestions(suggestions = followUps, onSelect = {}) }
        rule.onNodeWithText("Show it as a table")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theGroupIsNamedAndTheNameCanBeReplaced() {
        rule.setMonarchContent {
            Column {
                MonarchAiFollowUpSuggestions(followUps, {}, Modifier.testTag("default"))
                MonarchAiFollowUpSuggestions(followUps, {}, Modifier.testTag("custom"), label = "Questions suivantes")
            }
        }
        rule.onNodeWithTag("default").assertContentDescriptionEquals("Suggested follow-ups")
        rule.onNodeWithTag("custom").assertContentDescriptionEquals("Questions suivantes")
    }

    @Test
    fun disabledChipsCannotBeChosen() {
        var chosen = 0
        rule.setMonarchContent {
            MonarchAiFollowUpSuggestions(suggestions = followUps, onSelect = { chosen++ }, enabled = false)
        }
        rule.onNodeWithText("Show it as a table").assertIsNotEnabled().performClick()
        assertEquals(0, chosen)
    }

    @Test
    fun blankAndRepeatedSuggestionsAreLeftOutAndNoneLeftShowsNothing() {
        assertEquals(
            listOf("A", "B"),
            MonarchAiFollowUpSuggestionsTokens.visible(listOf(" A ", "", "B", "A", "  ")),
        )
        rule.setMonarchContent {
            Column {
                MonarchAiFollowUpSuggestions(listOf("Again", "Again "), {}, Modifier.testTag("one"))
                MonarchAiFollowUpSuggestions(listOf(" "), {}, Modifier.testTag("none"))
            }
        }
        assertEquals(1, rule.onAllNodesWithText("Again").fetchSemanticsNodes().size)
        rule.onNodeWithTag("none").assertDoesNotExist()
    }

    @Test
    fun chipsFollowTheLayoutDirection() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiFollowUpSuggestions(suggestions = listOf("One", "Two"), onSelect = {})
        }
        rule.onNodeWithText("One").assertIsBefore(rule.onNodeWithText("Two"), LayoutDirection.Rtl)
    }

    @Test
    fun aShortSuggestionIsFullyVisibleAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiFollowUpSuggestions(suggestions = listOf("Show it as a table"), onSelect = {})
        }
        rule.onNodeWithText("Show it as a table", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun chipsUseTheAssistantBorderAndTheSubtleFillWhenPressedInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val resting = MonarchAiFollowUpSuggestionsTokens.colors(MonarchControlState(), colors)
            val pressed = MonarchAiFollowUpSuggestionsTokens.colors(MonarchControlState(pressed = true), colors)
            val disabled = MonarchAiFollowUpSuggestionsTokens.colors(MonarchControlState(enabled = false), colors)
            assertEquals(colors.bgSurface, resting.container, theme.id)
            assertEquals(colors.aiBorder, resting.border, theme.id)
            assertEquals(colors.aiStrong, resting.content, theme.id)
            assertEquals(colors.aiSubtleHover, pressed.container, theme.id)
            assertEquals(colors.textDisabled, disabled.content, theme.id)
            assertEquals(colors.borderDefault, disabled.border, theme.id)
            assertTrue(resting.content != resting.container, theme.id)
        }
    }
}
