package dev.monarchui.components.forms

import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.forms.entitypicker.EntityPickerMessages
import dev.monarchui.components.forms.entitypicker.EntityPickerPhase
import dev.monarchui.components.forms.entitypicker.EntityPickerRow
import dev.monarchui.components.forms.entitypicker.EntityPickerSelection
import dev.monarchui.components.forms.entitypicker.MonarchEntityPicker
import dev.monarchui.components.forms.entitypicker.MonarchEntityPickerEntity
import dev.monarchui.components.forms.entitypicker.MonarchEntityPickerLabels
import dev.monarchui.components.forms.entitypicker.MonarchEntityPickerTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val people = listOf(
    MonarchEntityPickerEntity("ada", "Ada Lovelace", description = "Engineering", hint = "ada@example.com"),
    MonarchEntityPickerEntity("grace", "Grace Hopper", description = "Research", keywords = "admiral"),
    MonarchEntityPickerEntity("alan", "Alan Turing", enabled = false),
)

private val messages = EntityPickerMessages(
    loading = "loading",
    empty = "empty",
    tooShort = "short",
    failed = "failed",
    create = { "create $it" },
)

internal object MonarchEntityPickerFixtures : ComponentFixtures {
    override val id = "entity-picker"
    override val fixtures = listOf(
        fixture("empty") {
            MonarchEntityPicker(
                label = "Owner",
                entities = people,
                selected = null,
                onSelectedChange = { _: String? -> },
            )
        },
        fixture("chosen with clear") {
            MonarchEntityPicker(
                label = "Owner",
                entities = people,
                selected = "ada",
                onSelectedChange = { _: String? -> },
                clearIcon = { Icon(TestIcon, contentDescription = null) },
            )
        },
        fixture("several chosen") {
            MonarchEntityPicker(
                label = "Reviewers",
                entities = people,
                selected = listOf("ada", "grace"),
                onSelectedChange = { _: List<String> -> },
                helperText = "They are notified.",
            )
        },
        fixture("required with error") {
            MonarchEntityPicker(
                label = "Owner",
                entities = people,
                selected = null,
                onSelectedChange = { _: String? -> },
                required = true,
                errorText = "Choose an owner.",
            )
        },
        fixture("disabled") {
            MonarchEntityPicker(
                label = "Owner",
                entities = people,
                selected = "grace",
                onSelectedChange = { _: String? -> },
                enabled = false,
            )
        },
        fixture("long name") {
            MonarchEntityPicker(
                label = "The person who owns the account from now on",
                entities = listOf(MonarchEntityPickerEntity("x", "A person with a name far longer than the field")),
                selected = "x",
                onSelectedChange = { _: String? -> },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchEntityPickerTest {
    @get:Rule
    val rule = createComposeRule()

    private val field get() = rule.onNodeWithContentDescription("Owner")

    /** A row of the sheet: it has a selected or toggle state. */
    private fun row(text: String) = rule.onNode(
        hasText(text) and (
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected) or
                SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState)
            ),
    )

    @Test
    fun theFieldIsOneButtonNamedByTheLabelThatReadsTheChoice() {
        rule.setMonarchContent {
            MonarchEntityPicker(
                label = "Owner",
                entities = people,
                selected = "ada",
                onSelectedChange = { _: String? -> },
            )
        }
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        assertEquals("Ada Lovelace", field.stateDescription())
        field.assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun nothingChosenReadsNone() {
        rule.setMonarchContent {
            MonarchEntityPicker(
                label = "Owner",
                entities = people,
                selected = null,
                onSelectedChange = { _: String? -> },
            )
        }
        assertEquals("None", field.stateDescription())
    }

    @Test
    fun choosingARowSelectsItAndClosesTheSheet() {
        var chosen by mutableStateOf<String?>("ada")
        rule.setMonarchContent {
            MonarchEntityPicker(
                label = "Owner",
                entities = people,
                selected = chosen,
                onSelectedChange = { chosen = it },
            )
        }
        field.performClick()
        row("Ada Lovelace").assertIsSelected()
        row("Alan Turing").assertIsNotEnabled()
        row("Grace Hopper").assertIsNotSelected().assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals("grace", chosen)
        row("Grace Hopper").assertDoesNotExist()
        assertEquals("Grace Hopper", field.stateDescription())
    }

    @Test
    fun theClearRowClearsTheChoice() {
        var chosen by mutableStateOf<String?>("ada")
        rule.setMonarchContent {
            MonarchEntityPicker(
                label = "Owner",
                entities = people,
                selected = chosen,
                onSelectedChange = { chosen = it },
                clearOptionLabel = "Unassigned",
            )
        }
        field.performClick()
        row("Unassigned").assertIsNotSelected().performClick()
        rule.waitForIdle()
        assertEquals(null, chosen)
    }

    @Test
    fun severalAreToggledInPickOrderUntilDone() {
        var chosen by mutableStateOf(listOf<String>())
        rule.setMonarchContent {
            MonarchEntityPicker(
                label = "Reviewers",
                entities = people,
                selected = chosen,
                onSelectedChange = { chosen = it },
            )
        }
        rule.onNodeWithContentDescription("Reviewers").performClick()
        row("Grace Hopper").assertIsOff().performClick()
        row("Ada Lovelace").performClick()
        rule.waitForIdle()
        assertEquals(listOf("grace", "ada"), chosen)
        row("Grace Hopper").assertIsOn()
        rule.onNodeWithText("Done").performClick()
        rule.waitForIdle()
        row("Grace Hopper").assertDoesNotExist()
        assertEquals("Grace Hopper, Ada Lovelace", rule.onNodeWithContentDescription("Reviewers").stateDescription())
    }

    @Test
    fun withoutSearchTheListIsFilteredByNameHintAndKeywords() {
        rule.setMonarchContent {
            MonarchEntityPicker(
                label = "Owner",
                entities = people,
                selected = null,
                onSelectedChange = { _: String? -> },
            )
        }
        field.performClick()
        rule.onNode(hasSetTextAction()).performTextInput("admiral")
        rule.waitForIdle()
        row("Grace Hopper").assertExists()
        row("Ada Lovelace").assertDoesNotExist()
        rule.onNode(hasSetTextAction()).performTextInput("zzz")
        rule.waitForIdle()
        rule.onNodeWithText("No matches").assertExists()
    }

    @Test
    fun theAppSearchRunsWithTheTrimmedTextAndFailuresOfferTryAgain() {
        val queries = mutableListOf<String>()
        var fail = true
        rule.setMonarchContent {
            MonarchEntityPicker(
                label = "Owner",
                entities = people,
                selected = null,
                onSelectedChange = { _: String? -> },
                debounceMillis = 0L,
                search = { text ->
                    queries += text
                    if (fail) error("offline")
                    listOf(MonarchEntityPickerEntity("kat", "Katherine Johnson"))
                },
            )
        }
        field.performClick()
        rule.onNode(hasSetTextAction()).performTextInput(" kat ")
        rule.waitForIdle()
        rule.onNodeWithText("The results could not be loaded.").assertExists()
        fail = false
        rule.onNodeWithText("Try again").performClick()
        rule.waitForIdle()
        assertEquals(listOf("kat", "kat"), queries)
        row("Katherine Johnson").assertExists()
        row("Ada Lovelace").assertDoesNotExist()
    }

    @Test
    fun shortTextAsksForMoreAndDoesNotSearch() {
        var searches = 0
        rule.setMonarchContent {
            MonarchEntityPicker(
                label = "Owner",
                entities = people,
                selected = null,
                onSelectedChange = { _: String? -> },
                minSearchLength = 3,
                debounceMillis = 0L,
                search = {
                    searches++
                    emptyList()
                },
            )
        }
        field.performClick()
        rule.onNode(hasSetTextAction()).performTextInput("ad")
        rule.waitForIdle()
        rule.onNodeWithText("Type at least 3 characters to search.").assertExists()
        assertEquals(0, searches)
    }

    @Test
    fun createChoosesTheReturnedEntity() {
        var chosen by mutableStateOf<String?>(null)
        rule.setMonarchContent {
            MonarchEntityPicker(
                label = "Owner",
                entities = people,
                selected = chosen,
                onSelectedChange = { chosen = it },
                onCreate = { MonarchEntityPickerEntity("new", it) },
            )
        }
        field.performClick()
        rule.onNode(hasSetTextAction()).performTextInput("Mary Jackson")
        rule.waitForIdle()
        rule.onNodeWithText("Create “Mary Jackson”").performClick()
        rule.waitForIdle()
        assertEquals("new", chosen)
        assertEquals("Mary Jackson", field.stateDescription())
    }

    @Test
    fun theClearButtonClearsAndFollowsTheField() {
        var chosen by mutableStateOf<String?>("ada")
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchEntityPicker(
                label = "Owner",
                entities = people,
                selected = chosen,
                onSelectedChange = { chosen = it },
                clearIcon = { Icon(TestIcon, contentDescription = null) },
            )
        }
        val clear = rule.onNodeWithContentDescription("Clear")
        field.assertIsBefore(clear, LayoutDirection.Rtl)
        clear.assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(null, chosen)
        rule.onNodeWithContentDescription("Clear").assertDoesNotExist()
    }

    @Test
    fun aDisabledFieldDoesNotOpen() {
        rule.setMonarchContent {
            MonarchEntityPicker(
                label = "Owner",
                entities = people,
                selected = null,
                onSelectedChange = { _: String? -> },
                enabled = false,
            )
        }
        field.assertIsNotEnabled().performClick()
        rule.waitForIdle()
        row("Ada Lovelace").assertDoesNotExist()
    }

    @Test
    fun theErrorIsSetOnTheField() {
        rule.setMonarchContent {
            MonarchEntityPicker(
                label = "Owner",
                entities = people,
                selected = null,
                onSelectedChange = { _: String? -> },
                errorText = "Choose an owner.",
            )
        }
        assertEquals("Choose an owner.", field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error))
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchEntityPicker(
                label = "Owner",
                entities = people,
                selected = null,
                onSelectedChange = { _: String? -> },
                labels = MonarchEntityPickerLabels(none = "Nobody", cancel = "Close", empty = "Nothing"),
            )
        }
        assertEquals("Nobody", field.stateDescription())
        field.performClick()
        rule.onNodeWithText("Close").assertExists()
        rule.onNode(hasSetTextAction()).performTextInput("zzz")
        rule.waitForIdle()
        rule.onNodeWithText("Nothing").assertExists()
    }

    @Test
    fun rowsFitAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchEntityPicker(
                label = "Owner",
                entities = people,
                selected = null,
                onSelectedChange = { _: String? -> },
            )
        }
        field.performClick()
        rule.onNodeWithText("Ada Lovelace", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun rowsAndSummaryFollowTheRules() {
        val tokens = MonarchEntityPickerTokens
        val shown = people.take(1)
        assertEquals(
            listOf(EntityPickerRow.Clear("None"), EntityPickerRow.Entity(people[0])),
            tokens.rows(shown, "", EntityPickerPhase.Ready, false, "None", false, messages),
        )
        assertEquals(
            listOf(EntityPickerRow.Entity(people[0]), EntityPickerRow.Create("Bob", "create Bob")),
            tokens.rows(shown, " Bob ", EntityPickerPhase.Ready, false, "None", true, messages),
        )
        assertEquals(
            listOf<EntityPickerRow>(EntityPickerRow.Entity(people[0])),
            tokens.rows(shown, "ada lovelace", EntityPickerPhase.Ready, false, null, true, messages),
        )
        assertEquals(
            listOf<EntityPickerRow>(EntityPickerRow.Message("loading")),
            tokens.rows(emptyList(), "", EntityPickerPhase.Ready, true, null, false, messages),
        )
        assertEquals(
            listOf<EntityPickerRow>(EntityPickerRow.Failed("failed")),
            tokens.rows(shown, "x", EntityPickerPhase.Failed, false, null, false, messages),
        )
        assertEquals(EntityPickerPhase.TooShort, tokens.phase(" a ", 2))
        assertEquals(EntityPickerPhase.Ready, tokens.phase("", 2))
        assertEquals(null, EntityPickerSelection.summary(emptyList()) { a, b -> "$a+$b" })
        assertEquals("Ada", EntityPickerSelection.summary(listOf("Ada")) { a, b -> "$a+$b" })
        assertEquals("Ada+2", EntityPickerSelection.summary(listOf("Ada", "B", "C")) { a, b -> "$a+$b" })
        assertEquals(listOf("b"), EntityPickerSelection.toggle(listOf("a", "b"), "a"))
        assertTrue(tokens.matches(people[0], "EXAMPLE"))
    }

    @Test
    fun coloursComeFromRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchEntityPickerTokens
            assertEquals(colors.borderInput, tokens.fieldColors(error = false, enabled = true, colors).border)
            assertEquals(colors.dangerBorder, tokens.fieldColors(error = true, enabled = true, colors).border)
            assertEquals(colors.bgMuted, tokens.fieldColors(error = false, enabled = false, colors).container)
            assertEquals(colors.textSubtle, tokens.valueColor(chosen = false, enabled = true, colors))
            assertEquals(colors.textDefault, tokens.valueColor(chosen = true, enabled = true, colors))
        }
    }
}
