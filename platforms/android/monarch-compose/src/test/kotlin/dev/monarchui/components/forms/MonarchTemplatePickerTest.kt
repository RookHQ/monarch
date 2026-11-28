package dev.monarchui.components.forms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
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
import androidx.compose.ui.unit.dp
import dev.monarchui.components.forms.templatepicker.MonarchTemplatePicker
import dev.monarchui.components.forms.templatepicker.MonarchTemplatePickerBlank
import dev.monarchui.components.forms.templatepicker.MonarchTemplatePickerCategory
import dev.monarchui.components.forms.templatepicker.MonarchTemplatePickerItem
import dev.monarchui.components.forms.templatepicker.MonarchTemplatePickerLabels
import dev.monarchui.components.forms.templatepicker.MonarchTemplatePickerTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val Standup = MonarchTemplatePickerItem("standup", "Stand-up", "Daily check-in", "meetings")
private val Retro = MonarchTemplatePickerItem("retro", "Retro", category = "meetings", badge = "New")
private val Roadmap = MonarchTemplatePickerItem(
    "roadmap",
    "Roadmap",
    "Quarters and themes",
    "planning",
    keywords = listOf("okr"),
    enabled = false,
)
private val Resume = MonarchTemplatePickerItem("resume", "Résumé")
private val Templates = listOf(Standup, Retro, Roadmap)
private val Categories = listOf(
    MonarchTemplatePickerCategory("meetings", "Meetings"),
    MonarchTemplatePickerCategory("planning", "Planning"),
)

internal object MonarchTemplatePickerFixtures : ComponentFixtures {
    override val id = "template-picker"
    override val fixtures = listOf(
        fixture("blank and a selected template") {
            MonarchTemplatePicker(
                label = "Templates",
                templates = listOf(Standup, Retro),
                selected = "retro",
                onSelect = {},
                blank = MonarchTemplatePickerBlank(description = "Start with an empty page"),
            )
        },
        fixture("categories and a disabled template") {
            MonarchTemplatePicker(
                label = "Templates",
                templates = listOf(Roadmap),
                selected = null,
                onSelect = {},
                categories = Categories,
            )
        },
        fixture("search with no results") {
            MonarchTemplatePicker(
                label = "Templates",
                templates = Templates,
                selected = null,
                onSelect = {},
                query = "budget",
                onQueryChange = {},
            )
        },
        fixture("disabled") {
            MonarchTemplatePicker(
                label = "Templates",
                templates = listOf(Standup),
                selected = "standup",
                onSelect = {},
                enabled = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchTemplatePickerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun cardsAreRadioButtonsAndChoosingOneSelectsIt() {
        var selected by mutableStateOf<String?>(null)
        rule.setMonarchContent {
            MonarchTemplatePicker(
                label = "Templates",
                templates = Templates,
                selected = selected,
                onSelect = { selected = it },
                blank = MonarchTemplatePickerBlank(),
            )
        }
        rule.onNodeWithContentDescription("Templates").assertExists()
        val standup = rule.onNode(hasText("Stand-up"))
        standup.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsNotSelected()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("standup", selected)
        standup.assertIsSelected()
        rule.onNode(hasText("Blank")).performClick()
        assertEquals("blank", selected)
    }

    @Test
    fun aDisabledTemplateCannotBeChosen() {
        var selected by mutableStateOf<String?>(null)
        rule.setMonarchContent {
            MonarchTemplatePicker(
                label = "Templates",
                templates = Templates,
                selected = selected,
                onSelect = { selected = it },
            )
        }
        rule.onNode(hasText("Roadmap")).assertIsNotEnabled().performClick()
        assertEquals(null, selected)
    }

    @Test
    fun aCategoryFiltersTheCardsAndAnnouncesTheCount() {
        rule.setMonarchContent {
            MonarchTemplatePicker(
                label = "Templates",
                templates = Templates,
                selected = null,
                onSelect = {},
                categories = Categories,
                blank = MonarchTemplatePickerBlank(),
            )
        }
        rule.onNodeWithContentDescription("Categories").assertExists()
        rule.onNodeWithText("Planning").performClick()
        rule.onNode(hasText("Stand-up")).assertDoesNotExist()
        rule.onNode(hasText("Roadmap")).assertExists()
        rule.onNode(hasText("Blank")).assertExists()
        rule.onNodeWithContentDescription("1 template")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithText("All").performClick()
        rule.onNode(hasText("Stand-up")).assertExists()
    }

    @Test
    fun theSearchFieldReportsTypingAndMatchesKeywords() {
        var query by mutableStateOf("")
        rule.setMonarchContent {
            MonarchTemplatePicker(
                label = "Templates",
                templates = Templates,
                selected = null,
                onSelect = {},
                query = query,
                onQueryChange = { query = it },
            )
        }
        rule.onNode(hasSetTextAction()).performTextInput("OKR")
        assertEquals("OKR", query)
        rule.onNode(hasText("Roadmap")).assertExists()
        rule.onNode(hasText("Stand-up")).assertDoesNotExist()
        rule.onNodeWithContentDescription("1 template").assertExists()
    }

    @Test
    fun noResultsKeepsTheBlankCardAndCanClearTheFilters() {
        var query by mutableStateOf("budget")
        rule.setMonarchContent {
            MonarchTemplatePicker(
                label = "Templates",
                templates = Templates,
                selected = null,
                onSelect = {},
                categories = Categories,
                query = query,
                onQueryChange = { query = it },
                blank = MonarchTemplatePickerBlank(name = "Empty doc"),
            )
        }
        rule.onNodeWithText("No templates match “budget”.", useUnmergedTree = true).assertExists()
        rule.onNode(hasText("Empty doc")).assertExists()
        rule.onNodeWithText("Clear filters").performClick()
        assertEquals("", query)
        rule.onNode(hasText("Stand-up")).assertExists()
    }

    @Test
    fun anEmptyCategorySaysSoAndTheStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchTemplatePicker(
                label = "Plantillas",
                templates = listOf(Standup),
                selected = null,
                onSelect = {},
                categories = Categories,
                initialCategory = "planning",
                blank = MonarchTemplatePickerBlank(),
                labels = MonarchTemplatePickerLabels(
                    allCategories = "Todas",
                    blank = "En blanco",
                    emptyCategory = "Nada aquí.",
                    results = { "$it plantillas" },
                ),
            )
        }
        rule.onNodeWithText("Todas").assertExists()
        rule.onNode(hasText("En blanco")).assertExists()
        rule.onNodeWithText("Nada aquí.", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("0 plantillas").assertExists()
    }

    @Test
    fun theFirstCardIsAtTheStartRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchTemplatePicker(
                label = "Templates",
                templates = listOf(Standup, Retro),
                selected = null,
                onSelect = {},
            )
        }
        rule.onNodeWithText("Stand-up", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Retro", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun theLargestFontSizeShowsOneColumn() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchTemplatePicker(
                label = "Templates",
                templates = listOf(Standup, Retro),
                selected = null,
                onSelect = {},
            )
        }
        val first = rule.onNode(hasText("Stand-up")).fetchSemanticsNode().boundsInRoot
        val second = rule.onNode(hasText("Retro")).fetchSemanticsNode().boundsInRoot
        assertTrue(second.top >= first.bottom)
        rule.onNodeWithText("Daily check-in", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun searchIgnoresCaseAccentsAndWordOrder() {
        val tokens = MonarchTemplatePickerTokens
        assertTrue(tokens.matches(Resume, "resume"))
        assertTrue(tokens.matches(Standup, "check-in DAILY"))
        assertTrue(tokens.matches(Roadmap, "themes okr"))
        assertFalse(tokens.matches(Standup, "daily budget"))
        assertTrue(tokens.matches(Standup, "  "))
        assertEquals(listOf(Standup, Retro), tokens.filter(Templates, "meetings", ""))
        assertEquals(listOf(Roadmap), tokens.filter(Templates, null, "quarters"))
        assertEquals(tokens.MinCardWidth, tokens.minCardWidth(0.85f))
        assertEquals(tokens.MinCardWidth * 2f, tokens.minCardWidth(2f))
        assertEquals(144.dp, tokens.MinCardWidth)
    }

    @Test
    fun theColoursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.bgSubtle, MonarchTemplatePickerTokens.previewColor(enabled = true, c), theme.id)
            assertEquals(c.bgMuted, MonarchTemplatePickerTokens.previewColor(enabled = false, c), theme.id)
            assertEquals(c.textMuted, MonarchTemplatePickerTokens.descriptionColor(enabled = true, c), theme.id)
            assertEquals(c.textDisabled, MonarchTemplatePickerTokens.descriptionColor(enabled = false, c), theme.id)
        }
    }
}
