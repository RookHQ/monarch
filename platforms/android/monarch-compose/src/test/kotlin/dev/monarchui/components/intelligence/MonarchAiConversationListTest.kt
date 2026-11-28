package dev.monarchui.components.intelligence

import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationMessage
import dev.monarchui.components.intelligence.aiconversation.MonarchAiRole
import dev.monarchui.components.intelligence.aiconversation.MonarchAiTextPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallState
import dev.monarchui.components.intelligence.aiconversationlist.MonarchAiConversationGroup
import dev.monarchui.components.intelligence.aiconversationlist.MonarchAiConversationList
import dev.monarchui.components.intelligence.aiconversationlist.MonarchAiConversationListLabels
import dev.monarchui.components.intelligence.aiconversationlist.MonarchAiConversationListTokens
import dev.monarchui.components.intelligence.aiconversationlist.MonarchAiConversationSummary
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
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 2025-11-14 12:00 UTC. */
private const val Now = 1_763_121_600_000L
private const val Day = 86_400_000L
private val Utc: TimeZone = TimeZone.getTimeZone("UTC")

private val Pinned = MonarchAiConversationSummary("p", "Quarterly plan", Now - Day * 40, pinned = true)
private val Today = MonarchAiConversationSummary("t", "Trip to Lisbon", Now - 1_000, preview = "Day two: Belém")
private val Recent = MonarchAiConversationSummary("r", "Invoice questions", Now - Day * 3)
private val Old = MonarchAiConversationSummary("o", "", Now - Day * 30)
private val All = listOf(Old, Recent, Today, Pinned)

internal object MonarchAiConversationListFixtures : ComponentFixtures {
    override val id = "ai-conversation-list"
    override val fixtures = listOf(
        fixture("grouped, active, with new") {
            MonarchAiConversationList(
                conversations = All,
                onSelect = {},
                activeId = "t",
                searchable = false,
                nowMillis = Now,
                timeZone = Utc.id,
                onNew = {},
                onRename = { _, _ -> },
                onPin = { _, _ -> },
                onDelete = {},
            )
        },
        fixture("searchable with menu buttons") {
            MonarchAiConversationList(
                conversations = listOf(Today),
                onSelect = {},
                nowMillis = Now,
                timeZone = Utc.id,
                onDelete = {},
                menuIcon = { Icon(TestIcon, contentDescription = null) },
            )
        },
        fixture("long history") {
            MonarchAiConversationList(
                conversations = All,
                onSelect = {},
                searchable = false,
                visibleCount = 1,
                nowMillis = Now,
                timeZone = Utc.id,
            )
        },
        fixture("empty") { MonarchAiConversationList(conversations = emptyList(), onSelect = {}, onNew = {}) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiConversationListTest {
    @get:Rule
    val rule = createComposeRule()

    private val liveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
    private val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    private fun customAction(matcher: SemanticsMatcher, label: String) {
        val action = rule.onNode(matcher).fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .single { it.label == label }
        rule.runOnIdle { action.action() }
        rule.waitForIdle()
    }

    private fun list(
        conversations: List<MonarchAiConversationSummary> = All,
        onSelect: (String) -> Unit = {},
        onRename: ((String, String) -> Unit)? = null,
        onPin: ((String, Boolean) -> Unit)? = null,
        onDelete: ((String) -> Unit)? = null,
        onSearch: ((String) -> Unit)? = null,
        searchable: Boolean = false,
        visibleCount: Int = 200,
        labels: MonarchAiConversationListLabels = MonarchAiConversationListLabels(),
    ) {
        rule.setMonarchContent {
            MonarchAiConversationList(
                conversations = conversations,
                onSelect = onSelect,
                activeId = "t",
                searchable = searchable,
                visibleCount = visibleCount,
                nowMillis = Now,
                timeZone = Utc.id,
                onSearch = onSearch,
                onRename = onRename,
                onPin = onPin,
                onDelete = onDelete,
                labels = labels,
            )
        }
    }

    @Test
    fun theGroupsAreHeadingsInOrderAndEmptyGroupsAreLeftOut() {
        list(conversations = listOf(Pinned, Today, Old))
        rule.onNodeWithContentDescription("Conversation history").assertExists()
        rule.onNodeWithText("Pinned").assert(heading)
        rule.onNodeWithText("Today").assert(heading)
        rule.onNodeWithText("Older").assert(heading)
        rule.onNodeWithText("Previous 7 days").assertDoesNotExist()
        val pinnedTop = rule.onNodeWithText("Pinned").fetchSemanticsNode().boundsInRoot.top
        val todayTop = rule.onNodeWithText("Today").fetchSemanticsNode().boundsInRoot.top
        assertTrue(pinnedTop < todayTop)
    }

    @Test
    fun aRowIsAButtonThatOpensItsConversationAndTheActiveOneIsSelected() {
        var selected: String? = null
        list(onSelect = { selected = it })
        rule.onNode(hasText("Trip to Lisbon", substring = true))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsSelected()
            .assertTouchTargetAtLeast48dp(rule.density)
        rule.onNode(hasText("Invoice questions")).assertIsNotSelected().performClick()
        assertEquals("r", selected)
        rule.onNode(hasText("Day two: Belém", substring = true)).assertExists()
        rule.onNode(hasText("New conversation")).assertExists()
    }

    @Test
    fun renameAsksForATrimmedNewTitle() {
        val renamed = mutableListOf<Pair<String, String>>()
        list(onRename = { id, title -> renamed += id to title })
        rule.onNode(hasText("Invoice questions")).performTouchInput { longClick() }
        rule.onNodeWithText("Rename").performClick()
        rule.onNodeWithText("Rename conversation").assertExists()
        rule.onNodeWithText("Save").assertIsNotEnabled()
        rule.onNode(hasSetTextAction()).performTextReplacement("  Invoices for May  ")
        rule.onNodeWithText("Save").performClick()
        assertEquals(listOf("r" to "Invoices for May"), renamed)
        rule.onNodeWithText("Rename conversation").assertDoesNotExist()
    }

    @Test
    fun deleteIsAnActionThatAsksFirst() {
        val deleted = mutableListOf<String>()
        list(onDelete = { deleted += it })
        customAction(hasText("Invoice questions"), "Delete")
        rule.onNodeWithText("Delete “Invoice questions”?").assertExists()
        rule.onNodeWithText("Delete conversation").performClick()
        assertEquals(listOf("r"), deleted)
    }

    @Test
    fun pinAndUnpinFollowTheState() {
        val pins = mutableListOf<Pair<String, Boolean>>()
        list(onPin = { id, pinned -> pins += id to pinned })
        customAction(hasText("Quarterly plan"), "Unpin")
        customAction(hasText("Invoice questions"), "Pin")
        assertEquals(listOf("p" to false, "r" to true), pins)
    }

    @Test
    fun aLocalSearchFiltersAndAnnouncesTheCount() {
        list(searchable = true)
        rule.onNode(hasSetTextAction()).performTextInput("belém")
        rule.onNode(hasText("Trip to Lisbon", substring = true)).assertExists()
        rule.onNode(hasText("Invoice questions")).assertDoesNotExist()
        rule.onNode(liveRegion, useUnmergedTree = true).assertContentDescriptionEquals("1 conversation")
    }

    @Test
    fun aSearchWithoutResultsCanBeCleared() {
        list(searchable = true)
        rule.onNode(hasSetTextAction()).performTextInput("zzz")
        rule.onNodeWithText("No conversations match “zzz”", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Clear filters", substring = true).performClick()
        rule.onNode(hasText("Invoice questions")).assertExists()
    }

    @Test
    fun anAppSearchShowsTheConversationsAsGiven() {
        val searches = mutableListOf<String>()
        list(searchable = true, onSearch = { searches += it })
        rule.onNode(hasSetTextAction()).performTextInput("zzz")
        assertEquals("zzz", searches.last())
        rule.onNode(hasText("Invoice questions")).assertExists()
    }

    @Test
    fun aLongHistoryShowsMoreOnRequest() {
        list(visibleCount = 2)
        rule.onNode(hasText("Invoice questions")).assertDoesNotExist()
        rule.onNodeWithText("Show 2 more conversations").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNode(hasText("Invoice questions")).assertExists()
    }

    @Test
    fun anEmptyHistorySaysSo() {
        list(conversations = emptyList())
        rule.onNodeWithText("No conversations yet", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theMenuButtonIsNamedAfterTheConversationAndAtTheEndInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiConversationList(
                conversations = listOf(Recent),
                onSelect = {},
                searchable = false,
                nowMillis = Now,
                timeZone = Utc.id,
                onDelete = {},
                menuIcon = { Icon(TestIcon, contentDescription = null) },
            )
        }
        val button = rule.onNodeWithContentDescription("More actions for Invoice questions")
        button.assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Invoice questions", useUnmergedTree = true).assertIsBefore(button, LayoutDirection.Rtl)
        button.performClick()
        rule.onNodeWithText("Delete").assertExists()
    }

    @Test
    fun aTitleIsReadableAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiConversationList(
                conversations = listOf(Recent),
                onSelect = {},
                searchable = false,
                nowMillis = Now,
                timeZone = Utc.id,
            )
        }
        rule.onNodeWithText("Invoice questions", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theStringsCanBeReplaced() {
        list(
            conversations = listOf(Old),
            labels = MonarchAiConversationListLabels(
                untitled = "Sans titre",
                group = { if (it == MonarchAiConversationGroup.Older) "Plus ancien" else it.name },
            ),
        )
        rule.onNodeWithText("Plus ancien").assert(heading)
        rule.onNode(hasText("Sans titre")).assertExists()
    }

    @Test
    fun theGroupsFollowCalendarDays() {
        val tokens = MonarchAiConversationListTokens
        fun at(millis: Long, pinned: Boolean = false) = MonarchAiConversationSummary("x", "x", millis, pinned)
        val startOfToday = Now - Day / 2
        assertEquals(MonarchAiConversationGroup.Today, tokens.group(at(startOfToday), Now, Utc))
        assertEquals(MonarchAiConversationGroup.Today, tokens.group(at(Now + Day), Now, Utc))
        assertEquals(MonarchAiConversationGroup.Previous7Days, tokens.group(at(startOfToday - 1), Now, Utc))
        assertEquals(MonarchAiConversationGroup.Previous7Days, tokens.group(at(startOfToday - Day * 7), Now, Utc))
        assertEquals(MonarchAiConversationGroup.Older, tokens.group(at(startOfToday - Day * 7 - 1), Now, Utc))
        assertEquals(MonarchAiConversationGroup.Pinned, tokens.group(at(0, pinned = true), Now, Utc))
        // Just before midnight UTC is already the morning of today in Tokyo.
        val tokyo = TimeZone.getTimeZone("Asia/Tokyo")
        assertEquals(MonarchAiConversationGroup.Today, tokens.group(at(startOfToday - 1), Now, tokyo))
    }

    @Test
    fun theRulesOfOrderSearchAndRename() {
        val tokens = MonarchAiConversationListTokens
        val grouped = tokens.grouped(All, Now, Utc, limit = 3)
        assertEquals(
            listOf(
                MonarchAiConversationGroup.Pinned,
                MonarchAiConversationGroup.Today,
                MonarchAiConversationGroup.Previous7Days,
            ),
            grouped.map { it.first },
        )
        assertEquals(listOf(Today), tokens.filter(All, " BELÉM "))
        assertEquals(All, tokens.filter(All, ""))
        assertEquals("New", tokens.renamed("Old", "  New "))
        assertNull(tokens.renamed("Old", " Old "))
        assertNull(tokens.renamed("Old", "   "))
        assertEquals("Untitled", tokens.title(" ", "Untitled"))
    }

    @Test
    fun aSummaryCanBeMadeFromMessages() {
        val messages = listOf(
            MonarchAiConversationMessage(
                "1",
                MonarchAiRole.User,
                listOf(MonarchAiTextPart("Plan\n  a trip")),
                "2025-11-14T10:00:00.000Z",
            ),
            MonarchAiConversationMessage(
                "2",
                MonarchAiRole.Assistant,
                listOf(MonarchAiToolCallPart(id = "c", name = "search", state = MonarchAiToolCallState.Running)),
                "2025-11-14T11:00:00.000Z",
            ),
        )
        val summary = MonarchAiConversationSummary("c", "Trip", messages)
        assertEquals(1_763_118_000_000L, summary.updatedAtMillis)
        assertEquals("Plan a trip", summary.preview)
        assertEquals(0L, MonarchAiConversationSummary("e", "Empty", emptyList()).updatedAtMillis)
    }

    @Test
    fun theColoursComeFromTheThemeInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchAiConversationListTokens
            assertEquals(colors.accentSolid, tokens.marker(colors))
            assertEquals(colors.bgSubtle, tokens.activeBackground(colors))
            assertEquals(colors.bgSurface, tokens.headingBackground(colors))
            assertEquals(colors.textMuted, tokens.muted(colors))
        }
    }
}
