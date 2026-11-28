package com.rookhq.monarch.components.forms

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.forms.combobox.ComboboxEntries
import com.rookhq.monarch.components.forms.combobox.ComboboxEntry
import com.rookhq.monarch.components.forms.combobox.ComboboxFailure
import com.rookhq.monarch.components.forms.combobox.ComboboxFilter
import com.rookhq.monarch.components.forms.combobox.ComboboxMessages
import com.rookhq.monarch.components.forms.combobox.ComboboxRules
import com.rookhq.monarch.components.forms.combobox.MonarchCombobox
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxLabels
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxOption
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxPresentation
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val people = listOf(
    MonarchComboboxOption("ada", "Ada Lovelace", hint = "ada@example.com", group = "Team"),
    MonarchComboboxOption("alan", "Alan Turing", description = "On leave", enabled = false, group = "Team"),
    MonarchComboboxOption("zoe", "Zoë Quinn", keywords = "zq", group = "Guests"),
)

private val messages = ComboboxMessages(
    loading = "Loading",
    empty = "None",
    create = { "Create $it" },
    more = { shown, total -> "$shown/$total" },
)

internal object MonarchComboboxFixtures : ComponentFixtures {
    override val id = "combobox"
    override val fixtures = listOf(
        fixture("menu, chosen, clear icon") {
            MonarchCombobox(
                label = "Assignee",
                options = people,
                selected = "ada",
                onSelectedChange = {},
                clearIcon = { Icon(TestIcon, contentDescription = null) },
            )
        },
        fixture("sheet, empty with helper") {
            MonarchCombobox(
                label = "Assignee",
                options = people,
                selected = null,
                onSelectedChange = {},
                presentation = MonarchComboboxPresentation.Sheet,
                helperText = "Who works on it next.",
            )
        },
        fixture("error and placeholder") {
            MonarchCombobox(
                label = "Assignee",
                options = people,
                selected = null,
                onSelectedChange = {},
                placeholder = "Anyone",
                errorText = "Choose someone.",
            )
        },
        fixture("long label and value") {
            MonarchCombobox(
                label = "The person who reviews every change before release",
                options = listOf(MonarchComboboxOption("x", "A name that is much longer than the field is wide")),
                selected = "x",
                onSelectedChange = {},
            )
        },
        fixture("disabled") {
            MonarchCombobox("Assignee", options = people, selected = "zoe", onSelectedChange = {}, enabled = false)
        },
        fixture("leading content and a language") {
            MonarchCombobox(
                label = "Language",
                options = listOf(
                    MonarchComboboxOption("de", "Deutsch", leading = { Text("DE") }, labelLocale = Locale.GERMAN),
                    MonarchComboboxOption("fr", "Français", leading = { Text("FR") }, labelLocale = Locale.FRENCH),
                ),
                selected = "de",
                onSelectedChange = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchComboboxTest {
    @get:Rule
    val rule = createComposeRule()

    /** A row of the list: it has a selected state, unlike the field that may show the same text. */
    private fun row(text: String) = hasText(text) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)

    @Test
    fun typingFiltersTheMenuAndChoosesAnOption() {
        var chosen: String? = "unset"
        rule.setMonarchContent {
            var id by remember { mutableStateOf<String?>(null) }
            MonarchCombobox(
                label = "Assignee",
                options = people,
                selected = id,
                onSelectedChange = {
                    id = it
                    chosen = it
                },
            )
        }
        rule.onNode(hasSetTextAction()).assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNode(hasText("Team") and isHeading()).assertExists()
        rule.onNode(row("Alan Turing")).assertIsNotEnabled()
        rule.onNode(hasSetTextAction()).performTextInput("zoe")
        rule.waitForIdle()
        rule.onNode(row("Ada Lovelace")).assertDoesNotExist()
        rule.onNode(row("Zoë Quinn")).assertIsNotSelected().performClick()
        rule.waitForIdle()
        assertEquals("zoe", chosen)
        rule.onNode(hasSetTextAction()).assert(hasText("Zoë Quinn"))
    }

    @Test
    fun theChosenOptionIsSelectedAndTheClearRowClears() {
        var chosen: String? = "ada"
        rule.setMonarchContent {
            var id by remember { mutableStateOf<String?>("ada") }
            MonarchCombobox(
                label = "Assignee",
                options = people,
                selected = id,
                onSelectedChange = {
                    id = it
                    chosen = it
                },
                clearOptionLabel = "Unassigned",
            )
        }
        rule.onNode(hasSetTextAction()).performClick()
        rule.onNode(row("Ada Lovelace")).assertIsSelected()
        rule.onNode(row("Unassigned")).assertIsNotSelected().performClick()
        rule.waitForIdle()
        assertEquals(null, chosen)
    }

    @Test
    fun theClearButtonIsNamedAndClears() {
        var chosen: String? = "ada"
        rule.setMonarchContent {
            MonarchCombobox(
                label = "Assignee",
                options = people,
                selected = chosen,
                onSelectedChange = { chosen = it },
                clearIcon = { Icon(TestIcon, contentDescription = null) },
            )
        }
        rule.onNodeWithContentDescription("Clear").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(null, chosen)
    }

    @Test
    fun nothingMatchingSaysSoAndCreateAddsTheTypedValue() {
        var chosen: String? = null
        rule.setMonarchContent {
            MonarchCombobox(
                label = "Label",
                options = people,
                selected = null,
                onSelectedChange = { chosen = it },
                onCreate = { MonarchComboboxOption("new", it) },
            )
        }
        rule.onNode(hasSetTextAction()).performClick()
        rule.onNode(hasSetTextAction()).performTextInput("Urgent")
        rule.waitForIdle()
        rule.onNode(row("Create “Urgent”")).performClick()
        rule.waitForIdle()
        assertEquals("new", chosen)
    }

    @Test
    fun noMatchesIsShownWithoutCreate() {
        rule.setMonarchContent {
            MonarchCombobox(label = "Assignee", options = people, selected = null, onSelectedChange = {})
        }
        rule.onNode(hasSetTextAction()).performClick()
        rule.onNode(hasSetTextAction()).performTextInput("qqq")
        rule.waitForIdle()
        rule.onNodeWithText("No matches").assertExists()
    }

    @Test
    fun theSheetListsSelectableRowsWithASearchFieldAndCancel() {
        var chosen: String? = null
        rule.setMonarchContent {
            MonarchCombobox(
                label = "Assignee",
                options = people,
                selected = "ada",
                onSelectedChange = { chosen = it },
                presentation = MonarchComboboxPresentation.Sheet,
            )
        }
        rule.onNodeWithText("Ada Lovelace").performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Assignee") and isHeading()).assertExists()
        rule.onNode(row("Ada Lovelace"))
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertTouchTargetAtLeast48dp(rule.density)
        rule.onNode(row("Zoë Quinn")).performClick()
        rule.waitForIdle()
        assertEquals("zoe", chosen)
        rule.onNode(hasText("Guests") and isHeading()).assertDoesNotExist()
    }

    @Test
    fun aDisabledSheetFieldIsReadAsDisabled() {
        rule.setMonarchContent {
            MonarchCombobox(
                label = "Assignee",
                options = people,
                selected = "ada",
                onSelectedChange = {},
                presentation = MonarchComboboxPresentation.Sheet,
                enabled = false,
            )
        }
        rule.onNodeWithContentDescription("Assignee").assertIsNotEnabled()
    }

    @Test
    fun theSheetClosesWithCancelWithoutChoosing() {
        var changes = 0
        rule.setMonarchContent {
            MonarchCombobox(
                label = "Assignee",
                options = people,
                selected = "ada",
                onSelectedChange = { changes++ },
                presentation = MonarchComboboxPresentation.Sheet,
                labels = MonarchComboboxLabels(cancel = "Close"),
            )
        }
        rule.onNodeWithText("Ada Lovelace").performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Close") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Assignee") and isHeading()).assertDoesNotExist()
        assertEquals(0, changes)
    }

    @Test
    fun anErrorIsTheFieldsErrorAndTextFitsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale, layoutDirection = LayoutDirection.Rtl) {
            MonarchCombobox(
                label = "Assignee",
                options = people,
                selected = null,
                onSelectedChange = {},
                helperText = "Help",
                errorText = "Choose someone.",
            )
        }
        rule.onNodeWithText("Help").assertDoesNotExist()
        rule.onNodeWithText("Choose someone.", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNode(hasSetTextAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Choose someone."))
    }

    @Test
    fun theDefaultFilterIgnoresCaseAndAccentsAndSearchesHintsAndKeywords() {
        assertTrue(ComboboxFilter.matches(people[2], "ZOE"))
        assertTrue(ComboboxFilter.matches(people[2], "zq"))
        assertTrue(ComboboxFilter.matches(people[0], "example.com"))
        assertFalse(ComboboxFilter.matches(people[0], "turing"))
        assertEquals("zoe", ComboboxFilter.normalize("Zoë"))
    }

    @Test
    fun resultsSortByMatchCutToTheLimitAndOfferCreate() {
        val options = listOf(
            MonarchComboboxOption("1", "Northern Ireland"),
            MonarchComboboxOption("2", "Ireland"),
            MonarchComboboxOption("3", "Island Ireland"),
        )
        val sorted = ComboboxFilter.results(options, " ire ", ComboboxRules(sortByMatch = true))
        assertEquals(listOf("2", "1", "3"), sorted.shown.map { it.id })
        assertEquals(listOf(0, 1, 1), sorted.shown.map { ComboboxFilter.rank(it, "ire") })
        assertEquals(2, ComboboxFilter.rank(MonarchComboboxOption("4", "Spire"), "ire"))
        val kept = ComboboxFilter.results(options, "ire", ComboboxRules(limit = 2, canCreate = true))
        assertEquals(listOf("1", "2"), kept.shown.map { it.id })
        assertEquals(3, kept.total)
        assertTrue(kept.showCreate)
        assertFalse(ComboboxFilter.results(options, "ireland", ComboboxRules(canCreate = true)).showCreate)
        assertFalse(ComboboxFilter.results(options, "  ", ComboboxRules(canCreate = true)).showCreate)
        val all = ComboboxFilter.results(options, "x", ComboboxRules(filter = { _, _ -> true }))
        assertEquals(3, all.shown.size)
    }

    @Test
    fun theRowsComeInOrderWithMessages() {
        val all = ComboboxFilter.results(people, "", ComboboxRules(limit = 2))
        assertEquals(
            listOf(
                ComboboxEntry.Clear("Nobody"),
                ComboboxEntry.Header("Team"),
                ComboboxEntry.Option(people[0]),
                ComboboxEntry.Option(people[1]),
                ComboboxEntry.Message("2/3"),
            ),
            ComboboxEntries.build(all, "", "Nobody", loading = false, messages = messages),
        )
        val none = ComboboxFilter.results(people, "qq", ComboboxRules())
        assertEquals(
            listOf(ComboboxEntry.Message("None")),
            ComboboxEntries.build(none, "qq", "Nobody", loading = false, messages = messages),
        )
        assertEquals(
            listOf(ComboboxEntry.Message("Loading", loading = true)),
            ComboboxEntries.build(none, "qq", null, loading = true, messages = messages),
        )
        val create = ComboboxFilter.results(people, "qq", ComboboxRules(canCreate = true))
        assertEquals(
            listOf(ComboboxEntry.Create("qq", "Create qq")),
            ComboboxEntries.build(create, " qq", null, loading = false, messages = messages),
        )
    }

    @Test
    fun theAutomaticPresentationUsesAMenuForShortListsOnly() {
        val auto = MonarchComboboxPresentation.Automatic
        val tokens = MonarchComboboxTokens
        assertEquals(MonarchComboboxPresentation.Menu, tokens.presentation(auto, 8, searchesInApp = false))
        assertEquals(MonarchComboboxPresentation.Sheet, tokens.presentation(auto, 9, searchesInApp = false))
        assertEquals(MonarchComboboxPresentation.Sheet, tokens.presentation(auto, 2, searchesInApp = true))
        assertEquals(
            MonarchComboboxPresentation.Menu,
            MonarchComboboxTokens.presentation(MonarchComboboxPresentation.Menu, 200, searchesInApp = true),
        )
    }

    @Test
    fun theColoursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchComboboxTokens
            assertEquals(c.textDefault, t.labelColor(true, c), theme.id)
            assertEquals(c.textDisabled, t.labelColor(false, c), theme.id)
            assertEquals(c.textMuted, t.secondaryColor(true, c), theme.id)
            assertEquals(c.textMuted, t.valueColor(chosen = false, enabled = true, c), theme.id)
            assertEquals(c.textDefault, t.valueColor(chosen = true, enabled = true, c), theme.id)
            assertEquals(c.accentStrong, t.markColor(selected = true, enabled = true, c), theme.id)
            assertEquals(c.borderInput, t.markColor(selected = false, enabled = true, c), theme.id)
            assertEquals(c.textDisabled, t.markColor(selected = true, enabled = false, c), theme.id)
            assertEquals(c.bgSurface, t.sheetColor(c), theme.id)
        }
    }

    @Test
    fun aLoadErrorReplacesTheMessageAndRetryKeepsTheListOpen() {
        var retries = 0
        rule.setMonarchContent {
            MonarchCombobox(
                label = "Assignee",
                options = emptyList(),
                selected = null,
                onSelectedChange = {},
                presentation = MonarchComboboxPresentation.Menu,
                loading = true,
                loadError = "",
                onRetry = { retries++ },
            )
        }
        rule.onNode(hasSetTextAction()).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("The options could not be loaded.").assertExists()
        rule.onNodeWithText("Loading…").assertDoesNotExist()
        rule.onNode(hasText("Try again") and hasClickAction()).assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(1, retries)
        rule.onNodeWithText("The options could not be loaded.").assertExists()
    }

    @Test
    fun theFailureRowIsBuiltWithTheAppMessageAndOnlyARetryWhenOffered() {
        val results = ComboboxFilter.results(emptyList(), "", ComboboxRules(null, false, null, false))
        val withRetry = ComboboxEntries.build(results, "", null, false, messages, ComboboxFailure("Offline.", true))
        assertEquals(listOf<ComboboxEntry>(ComboboxEntry.Message("Offline.", failed = true, retry = "")), withRetry)
        val without = ComboboxEntries.build(results, "", null, false, messages, ComboboxFailure("Offline.", false))
        assertEquals(listOf<ComboboxEntry>(ComboboxEntry.Message("Offline.", failed = true, retry = null)), without)
    }

    @Test
    fun anOptionCanHaveLeadingContentAndALabelInItsOwnLanguage() {
        val option = MonarchComboboxOption(
            "de",
            "Deutsch",
            leading = { Text("DE", modifier = Modifier.testTag("flag")) },
            labelLocale = Locale.GERMAN,
        )
        val label = MonarchComboboxTokens.label(option)
        assertEquals("Deutsch", label.text)
        assertEquals(LocaleList("de"), label.spanStyles.single().item.localeList)
        assertEquals(0, MonarchComboboxTokens.label(people.first()).spanStyles.size)
        rule.setMonarchContent {
            MonarchCombobox(
                label = "Language",
                options = listOf(option),
                selected = null,
                onSelectedChange = {},
                presentation = MonarchComboboxPresentation.Menu,
            )
        }
        rule.onNode(hasSetTextAction()).performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("flag", useUnmergedTree = true).assertExists()
        rule.onNode(row("Deutsch")).assertExists()
    }
}
