package com.rookhq.monarch.components.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.files.versionhistorypanel.MonarchVersionHistoryPanel
import com.rookhq.monarch.components.files.versionhistorypanel.MonarchVersionHistoryPanelColors
import com.rookhq.monarch.components.files.versionhistorypanel.MonarchVersionHistoryPanelLabels
import com.rookhq.monarch.components.files.versionhistorypanel.MonarchVersionHistoryPanelTokens
import com.rookhq.monarch.components.files.versionhistorypanel.MonarchVersionHistoryPanelVersion
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// 2026-11-03 14:00 UTC.
private const val Now = 1_793_714_400_000L
private const val Hour = 3_600_000L
private val Utc = TimeZone.getTimeZone("UTC")
private val Versions = listOf(
    MonarchVersionHistoryPanelVersion("v3", Now - Hour, "Ana Ruiz", note = "Ready for review"),
    MonarchVersionHistoryPanelVersion("v2", Now - 26 * Hour, "Ben Okafor", automatic = true),
)
private val CurrentLines = listOf("Ship on Monday.", "Tell support.")
private val OldLines = listOf("Ship on Friday.", "Tell support.")

@Composable
private fun Panel(
    selection: String? = null,
    onSelectionChange: (String?) -> Unit = {},
    versions: List<MonarchVersionHistoryPanelVersion> = Versions,
    selectedLines: List<String>? = OldLines,
    loading: Boolean = false,
    onSaveVersion: (suspend (String) -> Unit)? = null,
    onRestore: (suspend (String) -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    labels: MonarchVersionHistoryPanelLabels = MonarchVersionHistoryPanelLabels(),
) {
    MonarchVersionHistoryPanel(
        versions = versions,
        selection = selection,
        onSelectionChange = onSelectionChange,
        currentLines = CurrentLines,
        selectedLines = selectedLines,
        loading = loading,
        now = Now,
        locale = Locale.US,
        timeZone = Utc.id,
        onSaveVersion = onSaveVersion,
        onRestore = onRestore,
        onClose = onClose,
        labels = labels,
    )
}

internal object MonarchVersionHistoryPanelFixtures : ComponentFixtures {
    override val id = "version-history-panel"
    override val fixtures = listOf(
        fixture("list with close") { Panel(onClose = {}) },
        fixture("list with save") { Panel(versions = Versions.take(1), onSaveVersion = {}) },
        fixture("loading") { Panel(loading = true) },
        fixture("empty") { Panel(versions = emptyList()) },
        fixture("version, changes, restore") { Panel(selection = "v2", onRestore = {}) },
        fixture("version, loading") { Panel(selection = "v2", selectedLines = null) },
        fixture("current version, identical") { Panel(selection = "v3", selectedLines = CurrentLines) },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchVersionHistoryPanelTest {
    @get:Rule
    val rule = createComposeRule()

    private val live = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    /**
     * What the polite live regions say: the panel's announcer, and the (empty unless invalid) error region of
     * the version name field, which every Monarch field keeps.
     */
    private fun announced(): List<String> = rule.onAllNodes(live, useUnmergedTree = true).fetchSemanticsNodes()
        .mapNotNull { it.config.getOrNull(SemanticsProperties.ContentDescription)?.single() }

    @Test
    fun eachVersionIsOneButtonWithTheCurrentOneMarked() {
        rule.setMonarchContent { Panel() }
        rule.onNodeWithText("History").assert(isHeading())
        val newest = rule.onNodeWithContentDescription("1 hour ago, Ana Ruiz, Ready for review")
        newest.assert(hasClickAction()).assertTouchTargetAtLeast48dp(rule.density)
        assertEquals("Current", newest.stateDescription())
        val older = rule.onNodeWithContentDescription("yesterday, Ben Okafor, Saved automatically")
        assertNull(older.stateDescription())
    }

    @Test
    fun openingAVersionAndGoingBackChangeTheSelection() {
        var selection by mutableStateOf<String?>(null)
        rule.setMonarchContent { Panel(selection = selection, onSelectionChange = { selection = it }) }
        rule.onNodeWithContentDescription("yesterday, Ben Okafor, Saved automatically").performClick()
        assertEquals("v2", selection)
        rule.onNodeWithText("yesterday").assert(isHeading())
        rule.onNodeWithContentDescription("Ben Okafor, Saved automatically, Nov 2, 2026", substring = true)
            .assertExists()
        rule.onNodeWithContentDescription("Back to versions").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertNull(selection)
    }

    @Test
    fun theVersionScreenShowsChangesThenTheFullText() {
        rule.setMonarchContent { Panel(selection = "v2") }
        rule.onNode(hasText("Changes") and hasClickAction()).assertIsSelected()
        rule.onNode(hasText("Monday", substring = true), useUnmergedTree = true).assertExists()
        rule.onNode(hasText("Full text") and hasClickAction()).performClick()
        rule.onNodeWithText("Ship on Friday.").assertExists()
    }

    @Test
    fun placeholdersShowWhileTheVersionLoads() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { Panel(selection = "v2", selectedLines = null) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Loading").assertExists()
    }

    @Test
    fun anIdenticalVersionSaysSo() {
        rule.setMonarchContent { Panel(selection = "v2", selectedLines = CurrentLines) }
        rule.onNodeWithText("This version is identical to the current one.").assertExists()
    }

    @Test
    fun restoreAsksFirstThenRunsReturnsToTheListAndAnnounces() {
        var selection by mutableStateOf<String?>("v2")
        val restored = mutableListOf<String>()
        rule.setMonarchContent {
            Panel(selection = selection, onSelectionChange = { selection = it }, onRestore = { restored += it })
        }
        rule.onNode(hasText("Restore this version") and hasClickAction()).performClick()
        rule.onNode(isDialog()).assertExists()
        rule.onNodeWithText("Restore this version?").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        assertTrue(restored.isEmpty())
        rule.onNode(hasText("Restore this version") and hasClickAction()).performClick()
        rule.onNode(hasText("Restore") and hasClickAction()).performClick()
        rule.waitForIdle()
        assertEquals(listOf("v2"), restored)
        assertNull(selection)
        assertEquals(listOf("Version restored"), announced())
    }

    @Test
    fun aFailedRestoreStaysWithTheError() {
        var selection by mutableStateOf<String?>("v2")
        rule.setMonarchContent {
            Panel(selection = selection, onSelectionChange = { selection = it }, onRestore = { error("Locked") })
        }
        rule.onNode(hasText("Restore this version") and hasClickAction()).performClick()
        rule.onNode(hasText("Restore") and hasClickAction()).performClick()
        rule.waitForIdle()
        assertEquals("v2", selection)
        rule.onNodeWithText("Locked").assertExists()
        rule.onNodeWithContentDescription("Locked", useUnmergedTree = true).assert(live)
    }

    @Test
    fun theCurrentVersionHasNoRestore() {
        rule.setMonarchContent { Panel(selection = "v3", onRestore = {}) }
        rule.onNode(hasText("Restore this version")).assertDoesNotExist()
    }

    @Test
    fun saveSendsTheTrimmedNameAndAnnounces() {
        val saved = mutableListOf<String>()
        rule.setMonarchContent { Panel(onSaveVersion = { saved += it }) }
        rule.onNode(hasSetTextAction()).performTextInput("  Before the launch  ")
        rule.onNode(hasText("Save version") and hasClickAction()).performClick()
        rule.waitForIdle()
        assertEquals(listOf("Before the launch"), saved)
        assertEquals(listOf("Version saved"), announced())
    }

    @Test
    fun aFailedSaveShowsTheFallbackError() {
        rule.setMonarchContent { Panel(onSaveVersion = { throw IllegalStateException() }) }
        rule.onNode(hasText("Save version") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("The version could not be saved. Try again.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun loadingAndEmptyLists() {
        var loading by mutableStateOf(true)
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { Panel(versions = emptyList(), loading = loading) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Loading").assertExists()
        loading = false
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("No versions yet. Save one to mark this moment.", substring = true).assertExists()
    }

    @Test
    fun closeIsAButton() {
        var closed = 0
        rule.setMonarchContent { Panel(onClose = { closed++ }) }
        rule.onNode(hasText("Close") and hasClickAction()).assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, closed)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            Panel(
                onClose = {},
                labels = MonarchVersionHistoryPanelLabels(close = "Fermer", current = "Actuelle", automatic = "Auto"),
            )
        }
        rule.onNodeWithText("Fermer").assertExists()
        assertEquals("Actuelle", rule.onNodeWithContentDescription("1 hour ago", substring = true).stateDescription())
        rule.onNodeWithContentDescription("yesterday, Ben Okafor, Auto").assertExists()
    }

    @Test
    fun closeIsAtTheEndInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { Panel(onClose = {}) }
        rule.onNodeWithText("History").assertIsBefore(rule.onNodeWithText("Close"), LayoutDirection.Rtl)
    }

    @Test
    fun theTitleWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchVersionHistoryPanel(
                versions = Versions,
                selection = null,
                onSelectionChange = {},
                currentLines = CurrentLines,
                title = "History of the launch plan",
                now = Now,
            )
        }
        rule.onNodeWithText("History of the launch plan").assertTextFullyVisible()
    }

    @Test
    fun theLogicPicksNamesTimesAndFailures() {
        val t = MonarchVersionHistoryPanelTokens
        assertEquals("v3", t.currentId(null, Versions))
        assertEquals("v2", t.currentId("v2", Versions))
        assertNull(t.currentId(null, emptyList()))
        assertEquals("v2", t.selected("v2", Versions)?.id)
        assertNull(t.selected("gone", Versions))
        assertNull(t.selected(null, Versions))
        assertEquals("a".repeat(200), t.note("  " + "a".repeat(250)))
        assertEquals("Ready for review", t.name(Versions[0], "Auto"))
        assertEquals("Auto", t.name(Versions[1], "Auto"))
        assertNull(t.name(Versions[1].copy(automatic = false), "Auto"))
        assertEquals("a, c", t.spoken(listOf("a", null, " ", "c")))
        assertTrue(t.identical(CurrentLines, CurrentLines.toList()))
        assertFalse(t.identical(OldLines, CurrentLines))
        assertEquals("Locked", t.failureText(IllegalStateException("Locked"), "Fallback"))
        assertEquals("Fallback", t.failureText(IllegalStateException(), "Fallback"))
        assertFailsWith<CancellationException> { t.failureText(CancellationException("stop"), "Fallback") }
        assertEquals("1 hour ago", t.relativeTime(Now - Hour, Now, Locale.US))
        // The shared web rule: a future time reads as such, as on the web (MONARCH-COMPAT).
        assertEquals("in 1 hour", t.relativeTime(Now + Hour, Now, Locale.US))
        assertEquals("yesterday", t.relativeTime(Now - 24 * Hour, Now, Locale.US))
        assertEquals("Nov 3, 2026, 2:00 PM", t.date(Now, Locale.US, Utc).replace(' ', ' '))
    }

    @Test
    fun theColoursComeFromTheThemeRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchVersionHistoryPanelColors
            assertEquals(c.textDefault, t.titleColor(c), theme.id)
            assertEquals(c.textMuted, t.mutedColor(c), theme.id)
            assertEquals(c.dangerStrong, t.errorColor(c), theme.id)
        }
    }
}
