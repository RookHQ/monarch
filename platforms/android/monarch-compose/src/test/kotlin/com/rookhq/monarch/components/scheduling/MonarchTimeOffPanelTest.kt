@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.scheduling

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.scheduling.timeoffpanel.MonarchTimeOffDraft
import com.rookhq.monarch.components.scheduling.timeoffpanel.MonarchTimeOffEntry
import com.rookhq.monarch.components.scheduling.timeoffpanel.MonarchTimeOffPanel
import com.rookhq.monarch.components.scheduling.timeoffpanel.MonarchTimeOffPanelLabels
import com.rookhq.monarch.components.scheduling.timeoffpanel.MonarchTimeOffPanelTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val Now = 1_793_793_600_000L
private const val Nov4 = 1_793_750_400_000L
private const val Nov5 = Nov4 + 86_400_000L
private val Utc: TimeZone = TimeZone.getTimeZone("UTC")

private val Leave = MonarchTimeOffEntry(
    id = "1",
    personId = "p1",
    allDay = true,
    start = 1_794_182_400_000L,
    end = 1_794_528_000_000L,
    personName = "Alex Kim",
    type = "Annual leave",
)
private val Closure = MonarchTimeOffEntry(
    id = "2",
    personId = null,
    allDay = true,
    start = 1_798_156_800_000L,
    end = 1_798_243_200_000L,
    reason = "Office closed",
)
private val Training = MonarchTimeOffEntry(
    id = "3",
    personId = "p2",
    allDay = false,
    start = 1_793_869_200_000L,
    end = 1_793_883_600_000L,
    personName = "Sam Lee",
    reason = "Training",
    status = "Pending",
)
private val Earlier = MonarchTimeOffEntry(
    id = "4",
    personId = "p2",
    allDay = true,
    start = 1_791_763_200_000L,
    end = 1_791_763_200_000L,
    personName = "Sam Lee",
)
private val People = listOf(MonarchSelectOption("p1", "Alex Kim"), MonarchSelectOption("p2", "Sam Lee"))

internal object MonarchTimeOffPanelFixtures : ComponentFixtures {
    override val id = "time-off-panel"
    override val fixtures = listOf(
        fixture("entries with add and remove") {
            MonarchTimeOffPanel(
                entries = listOf(Leave, Closure),
                onAdd = {},
                onRemove = {},
                timeZone = Utc.id,
                now = Now,
                is24Hour = false,
            )
        },
        fixture("timed and past entries, remove icons") {
            MonarchTimeOffPanel(
                entries = listOf(Training, Earlier),
                onRemove = {},
                showPast = true,
                timeZone = Utc.id,
                now = Now,
                is24Hour = true,
                removeIcon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        },
        fixture("loading") { MonarchTimeOffPanel(entries = emptyList(), loading = true, now = Now) },
        fixture("error with retry") {
            MonarchTimeOffPanel(entries = emptyList(), error = "Time off could not be loaded.", onRetry = {}, now = Now)
        },
        fixture("empty") { MonarchTimeOffPanel(entries = listOf(Earlier), onAdd = {}, timeZone = Utc.id, now = Now) },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchTimeOffPanelTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTitleIsAHeadingAndEachEntryIsOneElement() {
        rule.setMonarchContent {
            MonarchTimeOffPanel(
                entries = listOf(Leave, Closure, Training),
                timeZone = Utc.id,
                now = Now,
                is24Hour = false,
            )
        }
        rule.onNode(hasText("Time off") and isHeading()).assertExists()
        rule.onNodeWithText("Add time off").assertDoesNotExist()
        rule.onNode(hasText("Alex Kim") and hasText("Annual leave") and hasText("Mon, Nov 9, 2026", substring = true))
            .assertExists()
        rule.onNodeWithText("Mon, Nov 9, 2026 – Fri, Nov 13, 2026 (all day)").assertExists()
        rule.onNode(hasText("Everyone") and hasText("Closure")).assertExists()
        rule.onNodeWithText("Thu, Nov 5, 2026, 9:00\u202FAM – 1:00\u202FPM · Training").assertExists()
        rule.onNodeWithText("Pending").assertExists()
    }

    @Test
    fun pastEntriesAreHiddenUntilIncludePast() {
        val changes = mutableListOf<Boolean>()
        rule.setMonarchContent {
            MonarchTimeOffPanel(
                entries = listOf(Leave, Earlier),
                onShowPastChange = { changes += it },
                timeZone = Utc.id,
                now = Now,
            )
        }
        rule.onNodeWithText("Sam Lee").assertDoesNotExist()
        rule.onNodeWithText("Include past").assertIsOff().assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf(true), changes)
        rule.onNodeWithText("Include past").assertIsOn()
        rule.onNode(hasText("Sam Lee") and hasText("Past")).assertExists()
    }

    @Test
    fun removingAsksAndCallsBack() {
        var entries by mutableStateOf(listOf(Leave, Closure))
        val removed = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchTimeOffPanel(
                entries = entries,
                onRemove = { entry ->
                    removed += entry.id
                    entries = entries - entry
                },
                timeZone = Utc.id,
                now = Now,
            )
        }
        val name = "Remove time off for Alex Kim, Mon, Nov 9, 2026 – Fri, Nov 13, 2026 (all day)"
        rule.onNodeWithContentDescription(name)
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithText("Remove this time off?").assertExists()
        rule.onNodeWithText("Alex Kim · Mon, Nov 9, 2026", substring = true).assertExists()
        rule.onAllNodesWithText("Remove").onLast().performClick()
        rule.waitForIdle()
        assertEquals(listOf("1"), removed)
        rule.onNodeWithText("Remove this time off?").assertDoesNotExist()
        rule.onNodeWithText("Alex Kim").assertDoesNotExist()
    }

    @Test
    fun aFailedRemovalKeepsTheConfirmationWithTheError() {
        rule.setMonarchContent {
            MonarchTimeOffPanel(
                entries = listOf(Leave),
                onRemove = { error("The server said no.") },
                removeIcon = null,
                timeZone = Utc.id,
                now = Now,
            )
        }
        rule.onNodeWithText("Remove").performClick()
        rule.onAllNodesWithText("Remove").onLast().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("The server said no.").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Remove this time off?").assertDoesNotExist()
    }

    @Test
    fun canRemoveHidesTheButton() {
        rule.setMonarchContent {
            MonarchTimeOffPanel(
                entries = listOf(Leave, Closure),
                onRemove = {},
                canRemove = { it.personId != null },
                timeZone = Utc.id,
                now = Now,
            )
        }
        rule.onNodeWithContentDescription("Remove time off for Alex Kim", substring = true).assertExists()
        rule.onNodeWithContentDescription("Remove time off for Everyone", substring = true).assertDoesNotExist()
    }

    @Test
    fun addingSendsTheDraftFromTheSheet() {
        val drafts = mutableListOf<MonarchTimeOffDraft>()
        rule.setMonarchContent {
            MonarchTimeOffPanel(
                entries = emptyList(),
                onAdd = { drafts += it },
                people = People,
                allowClosure = true,
                types = listOf(MonarchSelectOption("leave", "Annual leave")),
                timeZone = Utc.id,
                now = Now,
            )
        }
        rule.onNodeWithText("Add time off").assertTouchTargetAtLeast48dp(rule.density).performClick()
        // The menus are named by their labels, which sit above the boxes.
        rule.onNodeWithContentDescription("Who", substring = true).assertExists()
        rule.onNodeWithContentDescription("Type", substring = true).assertExists()
        rule.onAllNodesWithText("Wed, Nov 4, 2026").assertCountEquals(2)
        rule.onAllNodes(hasText("Add time off") and hasClickAction()).onLast().performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(listOf(MonarchTimeOffDraft("p1", allDay = true, start = Nov4, end = Nov4, type = "leave")), drafts)
        rule.onNodeWithContentDescription("Reason (optional)", substring = true).assertDoesNotExist()
    }

    @Test
    fun aFailedAddKeepsTheSheetWithTheError() {
        rule.setMonarchContent {
            MonarchTimeOffPanel(entries = emptyList(), onAdd = { error("") }, timeZone = Utc.id, now = Now)
        }
        rule.onNodeWithText("Add time off").performClick()
        rule.onAllNodes(hasText("Add time off") and hasClickAction()).onLast().performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("That did not work. Try again.").assertExists()
    }

    @Test
    fun loadingErrorAndEmptyReplaceTheList() {
        var loading by mutableStateOf(true)
        var error by mutableStateOf<String?>(null)
        var retried = 0
        rule.setMonarchContent {
            MonarchTimeOffPanel(
                entries = emptyList(),
                loading = loading,
                error = error,
                onRetry = { retried++ },
                now = Now,
            )
        }
        rule.onNodeWithContentDescription("Loading time off").assertExists()
        loading = false
        rule.onNodeWithText("No time off planned").assertExists()
        error = "Time off could not be loaded."
        rule.onNodeWithText("Time off could not be loaded.").assertExists()
        rule.onNodeWithText("Try again").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchTimeOffPanel(
                entries = listOf(Closure),
                onRemove = {},
                timeZone = Utc.id,
                now = Now,
                labels = MonarchTimeOffPanelLabels(
                    title = "Abwesenheiten",
                    everyone = "Alle",
                    closure = "Geschlossen",
                    allDay = "ganztägig",
                    removeEntry = { who, _ -> "$who entfernen" },
                ),
            )
        }
        rule.onNode(hasText("Abwesenheiten") and isHeading()).assertExists()
        rule.onNode(hasText("Alle") and hasText("Geschlossen")).assertExists()
        rule.onNodeWithText("(ganztägig)", substring = true).assertExists()
        rule.onNodeWithContentDescription("Alle entfernen").assertExists()
    }

    @Test
    fun theRemoveButtonIsAtTheEnd() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchTimeOffPanel(
                entries = listOf(Leave),
                onRemove = {},
                removeIcon = null,
                timeZone = Utc.id,
                now = Now,
            )
        }
        rule.onNodeWithText("Alex Kim").assertIsBefore(rule.onNodeWithText("Remove"), LayoutDirection.Rtl)
    }

    @Test
    fun textWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchTimeOffPanel(entries = listOf(Leave), onRemove = {}, timeZone = Utc.id, now = Now)
        }
        rule.onNodeWithText("Mon, Nov 9, 2026 – Fri, Nov 13, 2026 (all day)").assertTextFullyVisible()
        rule.onNodeWithText("Time off").assertTextFullyVisible()
    }

    @Test
    fun theLogicAndColoursHoldInEveryTheme() {
        val tokens = MonarchTimeOffPanelTokens
        assertEquals(Nov4, tokens.dayOf(Now, Utc))
        assertTrue(tokens.isPast(Earlier, Now, Utc))
        assertFalse(tokens.isPast(Leave, Now, Utc))
        assertFalse(tokens.isPast(Leave.copy(start = Nov4, end = Nov4), Now, Utc))
        assertTrue(tokens.isPast(Training.copy(end = Now - 1), Now, Utc))
        assertEquals(listOf(Leave), tokens.visible(listOf(Leave, Earlier), showPast = false, now = Now, zone = Utc))
        val newYork = TimeZone.getTimeZone("America/New_York")
        assertEquals(Nov5 + 14 * 3_600_000L, tokens.wallToInstant(Nov5, 9 * 60, newYork))
        assertEquals(Nov5 + 9 * 3_600_000L, tokens.wallToInstant(Nov5, 9 * 60, Utc))
        assertTrue(tokens.removable(Leave, offered = true, canRemove = null))
        assertFalse(tokens.removable(Leave, offered = true) { false })
        assertFalse(tokens.removable(Leave, offered = false, canRemove = null))
        assertTrue(tokens.endBeforeStart(allDay = false, start = Nov4, end = Nov4))
        assertFalse(tokens.endBeforeStart(allDay = true, start = Nov4, end = Nov4))
        assertNull(tokens.draft("p1", allDay = true, start = Nov5, end = Nov4, type = null, reason = ""))
        assertEquals(
            MonarchTimeOffDraft(null, allDay = true, start = Nov4, end = Nov5, reason = "Conference"),
            tokens.draft(null, allDay = true, start = Nov4, end = Nov5, type = null, reason = "  Conference "),
        )
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textDefault, tokens.text(colors), theme.id)
            assertEquals(colors.textMuted, tokens.muted(colors), theme.id)
            assertEquals(colors.dangerStrong, tokens.error(colors), theme.id)
        }
    }
}
