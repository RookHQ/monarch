package dev.monarchui.components.collaboration

import android.icu.util.TimeZone
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.collaboration.inboxrow.MonarchInboxRow
import dev.monarchui.components.collaboration.inboxrow.MonarchInboxRowAge
import dev.monarchui.components.collaboration.inboxrow.MonarchInboxRowAgeUnit
import dev.monarchui.components.collaboration.inboxrow.MonarchInboxRowLabels
import dev.monarchui.components.collaboration.inboxrow.MonarchInboxRowLogic
import dev.monarchui.components.collaboration.inboxrow.MonarchInboxRowSwipeAction
import dev.monarchui.components.collaboration.inboxrow.MonarchInboxRowTokens
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// 2023-11-14T22:13:20Z.
private const val Now = 1_700_000_000_000L
private const val Minute = 60_000L
private const val Day = 24 * 60 * Minute

private fun descriptionOf(node: SemanticsNode): List<String> =
    node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()

internal object MonarchInboxRowFixtures : ComponentFixtures {
    override val id = "inbox-row"
    override val fixtures = listOf(
        fixture("read, with preview") {
            MonarchInboxRow(
                title = "Ana Lima",
                onClick = {},
                preview = "See you then",
                timestamp = Now - Day,
                now = Now,
            )
        },
        fixture("unread dot, icon") {
            MonarchInboxRow(
                title = "Ben Okafor",
                onClick = {},
                unread = true,
                timestamp = Now - 5 * Minute,
                now = Now,
                icon = { Icon(TestIcon, contentDescription = null) },
                iconLabel = "via email",
            )
        },
        fixture("unread count above 99, badges, trailing") {
            MonarchInboxRow(
                title = "Billing",
                onClick = {},
                unreadCount = 128,
                preview = "Invoice 2041 was paid",
                timestamp = Now,
                now = Now,
                badges = { MonarchBadge(text = "Urgent", tone = MonarchTone.Danger) },
                trailing = { MonarchBadge(text = "#2041") },
            )
        },
        fixture("selecting, selected, no avatar") {
            MonarchInboxRow(title = "Chloé", onClick = {}, selected = true, selecting = true, onSelectedChange = {})
        },
        fixture("swipe actions, no avatar") {
            MonarchInboxRow(
                title = "Dev",
                onClick = {},
                showAvatar = false,
                swipeActions = listOf(
                    MonarchInboxRowSwipeAction.ToggleRead {},
                    MonarchInboxRowSwipeAction.Archive {},
                    MonarchInboxRowSwipeAction.Delete {},
                ),
            )
        },
        fixture("long title and preview") {
            MonarchInboxRow(
                title = "Maria Fernanda de los Santos and the whole procurement team",
                onClick = {},
                preview = "We reviewed the second draft of the contract and have a few questions about the terms",
                timestamp = Now - 40 * Day,
                now = Now,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchInboxRowTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theRowIsOneElementReadInOrder() {
        rule.setMonarchContent {
            MonarchInboxRow(
                title = "Ana",
                onClick = {},
                unreadCount = 3,
                preview = "Lunch?",
                timestamp = Now - 5 * Minute,
                now = Now,
                iconLabel = "via email",
                icon = { Icon(TestIcon, contentDescription = null) },
                badges = { MonarchBadge(text = "Urgent") },
                modifier = Modifier.testTag("row"),
            )
        }
        val node = rule.onNodeWithTag("row").fetchSemanticsNode()
        val description = descriptionOf(node)
        assertEquals("Ana, 3 unread, via email", description.first())
        assertEquals(2, description.size)
        val text = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        assertEquals(listOf("Lunch?", "Urgent"), text)
        rule.onNodeWithText("5m", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("3", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aTapOpensTheConversation() {
        var opened = 0
        rule.setMonarchContent { MonarchInboxRow(title = "Ana", onClick = { opened++ }) }
        rule.onNodeWithContentDescription("Ana").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun aLongPressSelectsAndSelectionModeTurnsTapsIntoSelection() {
        var selected by mutableStateOf(false)
        var selecting by mutableStateOf(false)
        var opened = 0
        rule.setMonarchContent {
            MonarchInboxRow(
                title = "Ana",
                onClick = { opened++ },
                selected = selected,
                selecting = selecting,
                onSelectedChange = { selected = it },
            )
        }
        val row = rule.onNodeWithContentDescription("Ana")
        row.assertIsNotSelected()
        assertEquals("Select", row.fetchSemanticsNode().config.getOrNull(SemanticsActions.OnLongClick)?.label)
        row.performTouchInput { longClick() }
        assertTrue(selected)
        row.assertIsSelected()
        selecting = true
        rule.waitForIdle()
        row.performClick()
        assertEquals(false, selected)
        assertEquals(0, opened)
    }

    @Test
    fun swipeActionsAreTalkBackActionsNamedByTheReadState() {
        val calls = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchInboxRow(
                title = "Ana",
                onClick = {},
                unread = true,
                swipeActions = listOf(
                    MonarchInboxRowSwipeAction.ToggleRead { calls += "read" },
                    MonarchInboxRowSwipeAction.Archive { calls += "archive" },
                    MonarchInboxRowSwipeAction.Delete { calls += "delete" },
                ),
            )
        }
        val actions = rule.onNodeWithContentDescription("Ana, Unread").fetchSemanticsNode().config
            .getOrNull(SemanticsActions.CustomActions).orEmpty()
        assertEquals(listOf("Read", "Archive", "Delete"), actions.map { it.label })
        actions.forEach { it.action() }
        assertEquals(listOf("read", "archive", "delete"), calls)
    }

    @Test
    fun swipingRunsTheActionForThatDirection() {
        val calls = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchInboxRow(
                title = "Ana",
                onClick = {},
                swipeActions = listOf(
                    MonarchInboxRowSwipeAction.ToggleRead { calls += "read" },
                    MonarchInboxRowSwipeAction.Delete { calls += "delete" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Ana").performTouchInput { swipeRight() }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Ana").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertEquals(listOf("read", "delete"), calls)
        rule.onNodeWithContentDescription("Ana").assertExists()
    }

    @Test
    fun anOldTimeShowsAShortDateAndTalkBackReadsTheFullDate() {
        rule.setMonarchContent {
            MonarchInboxRow(
                title = "Ana",
                onClick = {},
                timestamp = Now - 40 * Day,
                now = Now,
                modifier = Modifier.testTag("row"),
            )
        }
        val description = descriptionOf(rule.onNodeWithTag("row").fetchSemanticsNode())
        assertEquals(2, description.size)
        assertTrue(description[1].contains("2023"), description[1])
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchInboxRow(
                title = "Ana",
                onClick = {},
                unread = true,
                timestamp = Now - 2 * 60 * Minute,
                now = Now,
                swipeActions = listOf(MonarchInboxRowSwipeAction.ToggleRead {}),
                labels = MonarchInboxRowLabels(
                    unread = { "no leído" },
                    age = { unit, value -> "$value ${unit.name}" },
                    markRead = "Leído",
                ),
            )
        }
        rule.onNodeWithText("2 Hours", useUnmergedTree = true).assertExists()
        val node = rule.onNodeWithContentDescription("Ana, no leído").fetchSemanticsNode()
        assertEquals(listOf("Leído"), node.config.getOrNull(SemanticsActions.CustomActions).orEmpty().map { it.label })
    }

    @Test
    fun theAvatarComesBeforeTheTitleAndTheAgeAfterInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchInboxRow(title = "Ana Lima", onClick = {}, timestamp = Now, now = Now)
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("AL", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithContentDescription("Ana Lima", useUnmergedTree = true), direction)
            rule.onNodeWithContentDescription("Ana Lima", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("now", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun theRowGrowsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchInboxRow(title = "Ana", onClick = {}, preview = "Lunch?", timestamp = Now, now = Now)
        }
        rule.onNodeWithContentDescription("Ana").assertTouchTargetAtLeast48dp(rule.density)
        rule.onAllNodesWithText("Lunch?", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun theAgeRulesMatchTheWeb() {
        val t = MonarchInboxRowLogic
        assertEquals(MonarchInboxRowAge(MonarchInboxRowAgeUnit.Now, 0), t.compactAge(Now + Minute, Now))
        assertEquals(MonarchInboxRowAge(MonarchInboxRowAgeUnit.Now, 0), t.compactAge(Now - 59_999L, Now))
        assertEquals(MonarchInboxRowAge(MonarchInboxRowAgeUnit.Minutes, 59), t.compactAge(Now - 59 * Minute, Now))
        assertEquals(MonarchInboxRowAge(MonarchInboxRowAgeUnit.Hours, 23), t.compactAge(Now - 23 * 60 * Minute, Now))
        assertEquals(MonarchInboxRowAge(MonarchInboxRowAgeUnit.Days, 6), t.compactAge(Now - 6 * Day, Now))
        assertEquals(MonarchInboxRowAge(MonarchInboxRowAgeUnit.Weeks, 4), t.compactAge(Now - 29 * Day, Now))
        assertNull(t.compactAge(Now - 30 * Day, Now))
        val utc = TimeZone.getTimeZone("UTC")
        assertEquals("Oct 15", t.shortDate(Now - 30 * Day, Now, Locale.US, utc))
        assertEquals("Nov 14, 2022", t.shortDate(Now - 365 * Day, Now, Locale.US, utc))
        assertEquals("99+", t.countText(100))
        assertEquals("99", t.countText(99))
        assertTrue(t.isUnread(unread = false, unreadCount = 2))
        assertEquals(false, t.isUnread(unread = false, unreadCount = 0))
    }

    @Test
    fun theSwipeDirectionsFollowMail() {
        val read = MonarchInboxRowSwipeAction.ToggleRead {}
        val archive = MonarchInboxRowSwipeAction.Archive {}
        val delete = MonarchInboxRowSwipeAction.Delete {}
        val t = MonarchInboxRowLogic
        assertEquals(read, t.towardEnd(listOf(delete, read)))
        assertEquals(archive, t.towardStart(listOf(delete, archive)))
        assertEquals(delete, t.towardStart(listOf(read, delete)))
        assertNull(t.towardEnd(listOf(archive)))
    }

    @Test
    fun theColoursComeFromTheirRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchInboxRowTokens
            assertEquals(c.textDefault, t.textColors(true, c).age, theme.id)
            assertEquals(c.textMuted, t.textColors(false, c).age, theme.id)
            assertEquals(c.textMuted, t.textColors(false, c).preview, theme.id)
            assertEquals(c.accentSubtle, t.container(true, c), theme.id)
            assertEquals(c.bgSurface, t.container(false, c), theme.id)
            assertEquals(c.accentSolid, t.markColors(c).container, theme.id)
            assertEquals(c.accentOnSolid, t.markColors(c).content, theme.id)
            assertEquals(c.infoSolid, t.swipeColors(MonarchInboxRowSwipeAction.ToggleRead {}, c).container, theme.id)
            assertEquals(c.accentSolid, t.swipeColors(MonarchInboxRowSwipeAction.Archive {}, c).container, theme.id)
            assertEquals(c.dangerSolid, t.swipeColors(MonarchInboxRowSwipeAction.Delete {}, c).container, theme.id)
        }
    }
}
