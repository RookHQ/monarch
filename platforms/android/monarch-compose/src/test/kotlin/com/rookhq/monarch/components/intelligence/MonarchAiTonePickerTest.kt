package com.rookhq.monarch.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.intelligence.aitonepicker.MonarchAiTone
import com.rookhq.monarch.components.intelligence.aitonepicker.MonarchAiTonePicker
import com.rookhq.monarch.components.intelligence.aitonepicker.MonarchAiTonePickerTokens
import com.rookhq.monarch.components.intelligence.aitonepicker.MonarchAiTonePickerVariant
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
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

private val twoTones = listOf(
    MonarchAiTone("formal", "Formal", "Complete sentences, no contractions."),
    MonarchAiTone("brief", "Brief", ""),
)

internal object MonarchAiTonePickerFixtures : ComponentFixtures {
    override val id = "ai-tone-picker"
    override val fixtures = listOf(
        fixture("cards, two tones") {
            MonarchAiTonePicker(selection = "formal", onSelectionChange = {}, tones = twoTones)
        },
        fixture("cards, disabled") {
            MonarchAiTonePicker(selection = "brief", onSelectionChange = {}, tones = twoTones, enabled = false)
        },
        fixture("custom selected") {
            MonarchAiTonePicker(
                selection = MonarchAiTone.CUSTOM_ID,
                onSelectionChange = {},
                tones = emptyList(),
                customText = "Warm and brief",
                onCustomTextChange = {},
            )
        },
        fixture("compact") {
            MonarchAiTonePicker(
                selection = "casual",
                onSelectionChange = {},
                variant = MonarchAiTonePickerVariant.Compact,
            )
        },
        fixture("compact, disabled") {
            MonarchAiTonePicker(
                selection = "casual",
                onSelectionChange = {},
                variant = MonarchAiTonePickerVariant.Compact,
                enabled = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiTonePickerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theBuiltInTonesAreOfferedWithTheirDescriptions() {
        rule.setMonarchContent { MonarchAiTonePicker(selection = "technical", onSelectionChange = {}) }
        listOf("Professional", "Technical", "Casual", "Executive").forEach { rule.onNodeWithText(it).assertExists() }
        rule.onNodeWithText("Precise terms, detail and structure.").assertExists()
        rule.onNodeWithText("Tone").assertExists()
    }

    @Test
    fun eachToneIsARadioButtonRowAndTheSelectedOneIsSelected() {
        rule.setMonarchContent { MonarchAiTonePicker(selection = "formal", onSelectionChange = {}, tones = twoTones) }
        val radio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        rule.onNode(hasText("Formal") and radio).assertIsSelected()
        rule.onNode(hasText("Brief") and radio).assertIsNotSelected()
    }

    @Test
    fun choosingAToneReportsItsId() {
        var selection by mutableStateOf("formal")
        rule.setMonarchContent {
            MonarchAiTonePicker(selection = selection, onSelectionChange = { selection = it }, tones = twoTones)
        }
        rule.onNodeWithText("Brief").performClick()
        assertEquals("brief", selection)
    }

    @Test
    fun aDisabledPickerDoesNotChange() {
        var changes = 0
        rule.setMonarchContent {
            MonarchAiTonePicker(
                selection = "formal",
                onSelectionChange = { changes++ },
                tones = twoTones,
                enabled = false,
            )
        }
        rule.onNodeWithText("Brief").assertIsNotEnabled().performClick()
        assertEquals(0, changes)
    }

    @Test
    fun theCustomOptionAppearsWithACallbackAndShowsItsFieldWhenSelected() {
        var selection by mutableStateOf("formal")
        var custom by mutableStateOf("")
        rule.setMonarchContent {
            MonarchAiTonePicker(
                selection = selection,
                onSelectionChange = { selection = it },
                tones = twoTones,
                customText = custom,
                onCustomTextChange = { custom = it },
            )
        }
        rule.onNode(hasSetTextAction()).assertDoesNotExist()
        rule.onNodeWithText("Custom").performClick()
        assertEquals(MonarchAiTone.CUSTOM_ID, selection)
        rule.onNode(hasSetTextAction()).performTextInput("Warm")
        assertEquals("Warm", custom)
        // The custom field is named by its label, which sits above the box.
        rule.onNodeWithContentDescription("Describe the tone").assertExists()
    }

    @Test
    fun withoutACallbackThereIsNoCustomOption() {
        rule.setMonarchContent { MonarchAiTonePicker(selection = "formal", onSelectionChange = {}, tones = twoTones) }
        rule.onNodeWithText("Custom").assertDoesNotExist()
    }

    @Test
    fun aToneWithTheCustomIdIsDroppedWhileTheCustomOptionShows() {
        val custom = MonarchAiTone(MonarchAiTone.CUSTOM_ID, "Mine", "Own")
        val clash = twoTones + MonarchAiTone(MonarchAiTone.CUSTOM_ID, "Clash", "")
        val options = MonarchAiTonePickerTokens.options(clash, custom)
        assertEquals(listOf("formal", "brief", MonarchAiTone.CUSTOM_ID), options.map { it.id })
        assertEquals("Mine", options.last().label)
        assertEquals(clash, MonarchAiTonePickerTokens.options(clash, null))
    }

    @Test
    fun everyBuiltInStringCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiTonePicker(
                selection = MonarchAiTone.CUSTOM_ID,
                onSelectionChange = {},
                tones = twoTones,
                onCustomTextChange = {},
                label = "Ton",
                customLabel = "Personnalisé",
                customDetail = "Décrivez le ton.",
                customFieldLabel = "Votre ton",
            )
        }
        listOf(
            "Ton",
            "Personnalisé",
            "Décrivez le ton.",
        ).forEach { rule.onNodeWithText(it).assertExists() }
        rule.onNodeWithContentDescription("Votre ton").assertExists()
    }

    @Test
    fun theCompactVariantShowsTheSelectedToneAndOpensAMenu() {
        var selection by mutableStateOf("casual")
        rule.setMonarchContent {
            MonarchAiTonePicker(
                selection = selection,
                onSelectionChange = { selection = it },
                variant = MonarchAiTonePickerVariant.Compact,
            )
        }
        rule.onNodeWithText("Casual").assertExists().performClick()
        rule.onNodeWithText("Executive").performClick()
        assertEquals("executive", selection)
    }

    @Test
    fun everyRowHasA48dpTarget() {
        rule.setMonarchContent { MonarchAiTonePicker(selection = "formal", onSelectionChange = {}, tones = twoTones) }
        rule.onNode(hasText("Brief") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theDescriptionWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiTonePicker(selection = "formal", onSelectionChange = {}, tones = twoTones)
        }
        rule.onNodeWithText("Complete sentences, no contractions.", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theLabelFollowsTheRadioMarkInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchAiTonePicker(selection = "formal", onSelectionChange = {}, tones = twoTones)
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            val radio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
            val row = rule.onNode(hasText("Formal") and radio)
            val text = rule.onNodeWithText("Formal", useUnmergedTree = true)
            val rowBounds = row.fetchSemanticsNode().boundsInRoot
            val textBounds = text.fetchSemanticsNode().boundsInRoot
            val gap = if (direction == LayoutDirection.Ltr) {
                textBounds.left - rowBounds.left
            } else {
                rowBounds.right - textBounds.right
            }
            assertTrue(gap > 0f, "$direction: the mark comes first")
        }
    }

    @Test
    fun theSelectedOptionHasTheAssistantFillAndAThickerBorderInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val on = MonarchAiTonePickerTokens.optionColors(selected = true, enabled = true, colors)
            val off = MonarchAiTonePickerTokens.optionColors(selected = false, enabled = true, colors)
            val disabled = MonarchAiTonePickerTokens.optionColors(selected = false, enabled = false, colors)
            assertEquals(colors.aiSubtle, on.container, theme.id)
            assertEquals(colors.aiBorder, on.border, theme.id)
            assertEquals(colors.bgSurface, off.container, theme.id)
            assertEquals(colors.borderDefault, off.border, theme.id)
            assertEquals(colors.textDisabled, disabled.content, theme.id)
        }
        assertEquals(MonarchAiTonePickerTokens.BorderWidth, MonarchAiTonePickerTokens.borderWidth(true))
    }
}
