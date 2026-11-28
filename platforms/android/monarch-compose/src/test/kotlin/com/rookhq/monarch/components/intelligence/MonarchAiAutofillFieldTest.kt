package com.rookhq.monarch.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.intelligence.aiautofillfield.MonarchAiAutofillField
import com.rookhq.monarch.components.intelligence.aiautofillfield.MonarchAiAutofillFieldLabels
import com.rookhq.monarch.components.intelligence.aiautofillfield.MonarchAiAutofillFieldTokens
import com.rookhq.monarch.components.intelligence.aiautofillfield.MonarchAiAutofillState
import com.rookhq.monarch.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadgeKind
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal object MonarchAiAutofillFieldFixtures : ComponentFixtures {
    override val id = "ai-autofill-field"
    override val fixtures = MonarchAiAutofillState.entries.map { state ->
        fixture("state ${state.name.lowercase()}") {
            MonarchAiAutofillField(
                state = state,
                suggestion = "Acme Ltd",
                onAccept = {},
                onClear = {},
                fieldName = "Company",
                source = "invoice-2024-03-from-the-accounts-payable-mailbox.pdf",
                confidence = 0.55f,
            ) {
                MonarchTextField(value = "Acme Ltd", onValueChange = {}, label = "Company")
            }
        }
    } + fixture("disabled, no confidence and no source") {
        MonarchAiAutofillField(
            state = MonarchAiAutofillState.Suggested,
            suggestion = "Acme Ltd",
            onAccept = {},
            onClear = {},
            enabled = false,
        ) {
            MonarchTextField(value = "Acme Ltd", onValueChange = {}, label = "Company")
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiAutofillFieldTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun suggestedShowsTheBadgeConfidenceSourceAndNamedActions() {
        var accepted = 0
        var state by mutableStateOf(MonarchAiAutofillState.Suggested)
        rule.setMonarchContent {
            MonarchAiAutofillField(
                state = state,
                suggestion = "Acme Ltd",
                onAccept = {
                    accepted++
                    state = MonarchAiAutofillState.Accepted
                },
                onClear = {},
                fieldName = "Company",
                source = "invoice.pdf",
                confidence = 0.92f,
            ) {
                MonarchTextField(value = "Acme Ltd", onValueChange = {}, label = "Company")
            }
        }
        rule.onNodeWithContentDescription("AI suggested", substring = true).assertExists()
        rule.onNodeWithText("From invoice.pdf", substring = true).assertExists()
        rule.onNodeWithContentDescription("High confidence", substring = true).assertExists()
        rule.onNodeWithContentDescription("Clear suggested Company").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithContentDescription("Accept suggested Company")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, accepted)
        rule.onNodeWithContentDescription("AI filled", substring = true).assertExists()
        rule.onNodeWithContentDescription("Accept suggested Company").assertDoesNotExist()
        rule.onNodeWithContentDescription("Suggestion accepted")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun editedShowsTheOriginalAndClearedShowsOnlyTheControl() {
        var state by mutableStateOf(MonarchAiAutofillState.Edited)
        rule.setMonarchContent {
            MonarchAiAutofillField(
                state = state,
                suggestion = "Acme Ltd",
                onAccept = {},
                onClear = {},
                source = "a.pdf",
            ) {
                MonarchTextField(value = "Acme", onValueChange = {}, label = "Company")
            }
        }
        rule.onNodeWithContentDescription("Edited by you", substring = true).assertExists()
        rule.onNodeWithText("AI suggested: Acme Ltd", substring = true).assertExists()
        rule.onNodeWithText("From a.pdf", substring = true).assertDoesNotExist()
        state = MonarchAiAutofillState.Cleared
        rule.onNodeWithContentDescription("Edited by you", substring = true).assertDoesNotExist()
        // The field is named by its label, which sits above the box.
        rule.onNodeWithContentDescription("Company", substring = true).assertExists()
    }

    @Test
    fun clearIsAnnouncedAndDisabledActionsDoNothing() {
        var cleared = 0
        var enabled by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchAiAutofillField(
                state = MonarchAiAutofillState.Suggested,
                suggestion = "Acme Ltd",
                onAccept = {},
                onClear = { cleared++ },
                enabled = enabled,
            ) {
                MonarchTextField(value = "Acme Ltd", onValueChange = {}, label = "Company")
            }
        }
        rule.onNodeWithText("Clear").assertIsNotEnabled().performClick()
        assertEquals(0, cleared)
        enabled = true
        rule.onNodeWithText("Clear").performClick()
        assertEquals(1, cleared)
        rule.onNodeWithContentDescription("Suggestion cleared").assertExists()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchAiAutofillField(
                state = MonarchAiAutofillState.Suggested,
                suggestion = "Acme Ltd",
                onAccept = {},
                onClear = {},
                fieldName = "Société",
                source = "facture.pdf",
                labels = MonarchAiAutofillFieldLabels(
                    badge = { "Suggéré par l'IA" },
                    accept = "Accepter",
                    acceptField = { "Accepter $it" },
                    source = { "Depuis $it" },
                ),
            ) {
                MonarchTextField(value = "Acme Ltd", onValueChange = {}, label = "Société")
            }
        }
        rule.onNodeWithContentDescription("Suggéré par l'IA", substring = true).assertExists()
        rule.onNodeWithText("Depuis facture.pdf", substring = true).assertExists()
        rule.onNodeWithContentDescription("Accepter Société").assertExists()
    }

    @Test
    fun actionsFollowTheReadingDirectionAndFitAtLargeFonts() {
        rule.setMonarchContent(fontScale = LargestFontScale, layoutDirection = LayoutDirection.Rtl) {
            MonarchAiAutofillField(
                state = MonarchAiAutofillState.Suggested,
                suggestion = "Acme Ltd",
                onAccept = {},
                onClear = {},
            ) {
                MonarchTextField(value = "Acme Ltd", onValueChange = {}, label = "Company")
            }
        }
        rule.onNodeWithText("Accept").assertTextFullyVisible()
        rule.onNodeWithText("Accept").assertIsBefore(rule.onNodeWithText("Clear"), LayoutDirection.Rtl)
    }

    @Test
    fun whatShowsInEachStateAndTheMarkerColour() {
        val tokens = MonarchAiAutofillFieldTokens
        assertTrue(tokens.showsActions(MonarchAiAutofillState.Suggested))
        assertFalse(tokens.showsActions(MonarchAiAutofillState.Accepted))
        assertFalse(tokens.showsStatus(MonarchAiAutofillState.Cleared))
        assertTrue(tokens.showsConfidence(MonarchAiAutofillState.Suggested, 0.4f))
        assertFalse(tokens.showsConfidence(MonarchAiAutofillState.Accepted, 0.4f))
        assertFalse(tokens.showsConfidence(MonarchAiAutofillState.Suggested, Float.NaN))
        assertTrue(tokens.showsSource(MonarchAiAutofillState.Accepted, "a.pdf"))
        assertFalse(tokens.showsSource(MonarchAiAutofillState.Edited, "a.pdf"))
        assertFalse(tokens.showsSource(MonarchAiAutofillState.Suggested, " "))
        assertEquals(MonarchAiGeneratedBadgeKind.Edited, tokens.badgeKind(MonarchAiAutofillState.Edited))
        assertEquals(MonarchAiGeneratedBadgeKind.Generated, tokens.badgeKind(MonarchAiAutofillState.Accepted))
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.aiBorder, tokens.markerColor(colors))
            assertEquals(colors.textMuted, tokens.lineColor(colors))
        }
    }
}
