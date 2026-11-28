package dev.monarchui.components.collaboration

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionItem
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionKind
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionPicker
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionPickerLabels
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionPickerStatus
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionPickerTokens
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionQuery
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Query = MonarchMentionQuery(trigger = '@', text = "an", start = 0, end = 3)

private val People = listOf(
    MonarchMentionItem(id = "u1", label = "Ana Lima", detail = "Designer"),
    MonarchMentionItem(id = "u2", label = "Anders Berg", detail = "No access", enabled = false),
)
private val Channel = MonarchMentionItem(id = "c1", label = "announcements", kind = MonarchMentionKind.Channel)

internal object MonarchMentionPickerFixtures : ComponentFixtures {
    override val id = "mention-picker"
    override val fixtures = listOf(
        fixture("people only, one disabled") { MonarchMentionPicker(query = Query, items = People, onSelect = {}) },
        fixture("people and channels") {
            MonarchMentionPicker(query = Query, items = People.take(1) + Channel, onSelect = {})
        },
        fixture("no matches") { MonarchMentionPicker(query = Query, items = emptyList(), onSelect = {}) },
        fixture("failed with retry") {
            MonarchMentionPicker(
                query = Query,
                items = emptyList(),
                onSelect = {},
                status = MonarchMentionPickerStatus.Failed,
                onRetry = {},
            )
        },
        fixture("failed without retry") {
            MonarchMentionPicker(
                query = Query,
                items = emptyList(),
                onSelect = {},
                status = MonarchMentionPickerStatus.Failed,
            )
        },
        fixture("long label and detail") {
            MonarchMentionPicker(
                query = Query,
                items = listOf(
                    MonarchMentionItem(
                        id = "u9",
                        label = "Maria Fernanda de los Santos",
                        detail = "Head of procurement for the southern region",
                    ),
                ),
                onSelect = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchMentionPickerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun suggestionsAreButtonsAndTheCountIsAnnounced() {
        var chosen: String? = null
        rule.setMonarchContent { MonarchMentionPicker(query = Query, items = People, onSelect = { chosen = it.id }) }
        val strip = rule.onNodeWithContentDescription("Suggestions")
        strip.assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        assertEquals("2 suggestions", strip.stateDescription())
        rule.onNodeWithText("Ana Lima")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("u1", chosen)
    }

    @Test
    fun aDisabledSuggestionCannotBeChosen() {
        var chosen: String? = null
        rule.setMonarchContent { MonarchMentionPicker(query = Query, items = People, onSelect = { chosen = it.id }) }
        rule.onNodeWithText("Anders Berg").assertIsNotEnabled().performClick()
        assertNull(chosen)
    }

    @Test
    fun peopleAndChannelsAreGroupedUnderHeadingsOnlyWhenBothArePresent() {
        rule.setMonarchContent { MonarchMentionPicker(query = Query, items = People + Channel, onSelect = {}) }
        rule.onNodeWithText("People").assert(isHeading())
        rule.onNodeWithText("Channels").assert(isHeading())
        rule.onNodeWithText("#", useUnmergedTree = true).assertExists()
    }

    @Test
    fun oneKindHasNoHeading() {
        rule.setMonarchContent { MonarchMentionPicker(query = Query, items = People, onSelect = {}) }
        rule.onAllNodes(isHeading()).assertCountEquals(0)
    }

    @Test
    fun loadingShowsPlaceholdersReadAsSearching() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchMentionPicker(
                query = Query,
                items = emptyList(),
                onSelect = {},
                status = MonarchMentionPickerStatus.Loading,
            )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Searching…").assertExists()
        assertEquals("Searching…", rule.onNodeWithContentDescription("Suggestions").stateDescription())
    }

    @Test
    fun aFailedSearchOffersRetry() {
        var retries = 0
        rule.setMonarchContent {
            MonarchMentionPicker(
                query = Query,
                items = emptyList(),
                onSelect = {},
                status = MonarchMentionPickerStatus.Failed,
                onRetry = { retries++ },
            )
        }
        val strip = rule.onNodeWithContentDescription("Suggestions")
        assertEquals("Couldn’t load suggestions.", strip.stateDescription())
        rule.onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun noQueryOrAProseQueryWithoutMatchesDrawsNothing() {
        rule.setMonarchContent {
            Box(Modifier.testTag("host")) {
                MonarchMentionPicker(query = null, items = People, onSelect = {})
                MonarchMentionPicker(
                    query = MonarchMentionQuery('@', "ana lopez", 0, 10),
                    items = emptyList(),
                    onSelect = {},
                )
            }
        }
        assertEquals(0, rule.onNodeWithTag("host").fetchSemanticsNode().children.size)
    }

    @Test
    fun aOneWordQueryWithoutMatchesSaysSo() {
        rule.setMonarchContent { MonarchMentionPicker(query = Query, items = emptyList(), onSelect = {}) }
        rule.onNodeWithText("No matches").assertExists()
        assertEquals("No matches", rule.onNodeWithContentDescription("Suggestions").stateDescription())
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchMentionPicker(
                query = Query,
                items = People + Channel,
                onSelect = {},
                labels = MonarchMentionPickerLabels(
                    label = "Sugerencias",
                    people = "Personas",
                    channels = "Canales",
                    count = { "$it resultados" },
                ),
            )
        }
        assertEquals("3 resultados", rule.onNodeWithContentDescription("Sugerencias").stateDescription())
        rule.onNodeWithText("Personas").assertExists()
        rule.onNodeWithText("Canales").assertExists()
    }

    @Test
    fun theAvatarComesBeforeTheNameInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchMentionPicker(query = Query, items = People.take(1), onSelect = {})
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("AL", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Ana Lima", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun longDetailsWrapAtTheLargestFontScale() {
        val detail = "Head of procurement for the southern region"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchMentionPicker(
                query = Query,
                items = listOf(MonarchMentionItem(id = "u9", label = "Maria", detail = detail)),
                onSelect = {},
            )
        }
        rule.onNodeWithText(detail, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun findsTheMentionBeingTypedLikeTheWeb() {
        assertEquals(MonarchMentionQuery('@', "an", 6, 9), MonarchMentionQuery.find("Hello @an"))
        assertEquals(MonarchMentionQuery('@', "", 0, 1), MonarchMentionQuery.find("@"))
        assertEquals(MonarchMentionQuery('@', "ana lo", 3, 10), MonarchMentionQuery.find("Hi @ana lo"))
        assertEquals(MonarchMentionQuery('@', "ana", 1, 5), MonarchMentionQuery.find("(@ana"))
        assertNull(MonarchMentionQuery.find("ana@example"))
        assertNull(MonarchMentionQuery.find("@ana lopez smith"))
        assertNull(MonarchMentionQuery.find("@ana\nlopez"))
        assertNull(MonarchMentionQuery.find("@" + "a".repeat(41)))
        assertNull(MonarchMentionQuery.find("#gen"))
        assertEquals(
            MonarchMentionQuery('#', "gen", 0, 4),
            MonarchMentionQuery.find("#gen", triggers = setOf('@', '#')),
        )
        assertEquals(MonarchMentionQuery('@', "an", 0, 3), MonarchMentionQuery.find("@an and more", caret = 3))
        val value = TextFieldValue("Hi @an", selection = TextRange(6))
        assertEquals(MonarchMentionQuery('@', "an", 3, 6), MonarchMentionQuery.find(value))
        assertNull(MonarchMentionQuery.find(TextFieldValue("Hi @an", selection = TextRange(0, 6))))
    }

    @Test
    fun replacingWritesTheTokenAndKeepsOneSpace() {
        val item = MonarchMentionItem(id = "u_123", label = "Ana (Lopez) [Ops]")
        assertEquals("@[Ana Lopez Ops](u_123)", item.token())
        assertEquals("#[general](c1)", MonarchMentionItem(id = "c1", label = "general").token('#'))
        val text = "Hi @an thanks"
        val query = MonarchMentionQuery.find(text, caret = 6)!!
        assertEquals("Hi @[Ana](u1) thanks", query.replacing(text, "@[Ana](u1) "))
        val value = query.replacing(TextFieldValue(text, TextRange(6)), "@[Ana](u1) ")
        assertEquals("Hi @[Ana](u1) thanks", value.text)
        assertEquals(TextRange(14), value.selection)
        assertEquals("short", query.replacing("short", "x"))
    }

    @Test
    fun theOpenAndGroupRulesMatchTheWeb() {
        val t = MonarchMentionPickerTokens
        val ready = MonarchMentionPickerStatus.Ready
        assertFalse(t.isOpen(null, ready, 3))
        assertTrue(t.isOpen(Query, ready, 0))
        assertFalse(t.isOpen(MonarchMentionQuery('@', "ana lo", 0, 7), ready, 0))
        assertTrue(t.isOpen(MonarchMentionQuery('@', "ana lo", 0, 7), MonarchMentionPickerStatus.Loading, 0))
        assertTrue(t.grouped(People + Channel))
        assertFalse(t.grouped(People))
    }

    @Test
    fun theColoursComeFromTheirRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchMentionPickerTokens
            assertEquals(c.bgSurface, t.background(c), theme.id)
            assertEquals(c.borderSubtle, t.border(c), theme.id)
            assertEquals(c.textMuted, t.noteColor(c), theme.id)
        }
    }
}
