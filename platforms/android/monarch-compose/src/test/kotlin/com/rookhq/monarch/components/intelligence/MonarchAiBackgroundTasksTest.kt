@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aibackgroundtasks.MonarchAiBackgroundTask
import com.rookhq.monarch.components.intelligence.aibackgroundtasks.MonarchAiBackgroundTaskStatus
import com.rookhq.monarch.components.intelligence.aibackgroundtasks.MonarchAiBackgroundTasks
import com.rookhq.monarch.components.intelligence.aibackgroundtasks.MonarchAiBackgroundTasksLabels
import com.rookhq.monarch.components.intelligence.aibackgroundtasks.MonarchAiBackgroundTasksRules
import com.rookhq.monarch.components.intelligence.aibackgroundtasks.MonarchAiBackgroundTasksTokens
import com.rookhq.monarch.foundation.MonarchTone
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
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val Now = 1_800_000_000_000L
private const val Minute = 60_000L
private val Running = MonarchAiBackgroundTask(
    "run",
    "Summarize contracts",
    MonarchAiBackgroundTaskStatus.Running,
    agent = "Research agent",
    progress = 0.25f,
    step = "Reading 12 of 40",
    startedAt = Now - 3 * Minute - 12_000L,
)
private val Waiting = MonarchAiBackgroundTask("wait", "Book the venue", MonarchAiBackgroundTaskStatus.NeedsInput)
private val Queued = MonarchAiBackgroundTask("queue", "Translate the FAQ", MonarchAiBackgroundTaskStatus.Queued)
private val Failed = MonarchAiBackgroundTask(
    "fail",
    "Draft newsletter",
    MonarchAiBackgroundTaskStatus.Failed,
    error = "Source unavailable",
    endedAt = Now - 5 * Minute,
)

internal object MonarchAiBackgroundTasksFixtures : ComponentFixtures {
    override val id = "ai-background-tasks"
    override val fixtures = listOf(
        fixture("button with counts") {
            MonarchAiBackgroundTasks(tasks = listOf(Running, Waiting, Queued), now = Now)
        },
        fixture("button, queued only") { MonarchAiBackgroundTasks(tasks = listOf(Queued), now = Now) },
        fixture("inline, waiting and failed with actions") {
            MonarchAiBackgroundTasks(
                tasks = listOf(Waiting, Failed),
                inline = true,
                now = Now,
                onRespond = {},
                onRetry = {},
                onDismiss = {},
            )
        },
        fixture("inline, empty") { MonarchAiBackgroundTasks(tasks = emptyList(), inline = true, now = Now) },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiBackgroundTasksTest {
    @get:Rule
    val rule = createComposeRule()

    private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    @Test
    fun theButtonShowsTheCountsAndOpensTheGroupedSheet() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiBackgroundTasks(tasks = listOf(Queued, Running, Waiting, Failed), now = Now)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("1 running", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("1 needs you", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Background tasks", useUnmergedTree = true).performClick()
        rule.mainClock.advanceTimeBy(1_000L)
        rule.onNodeWithText("Needs you (1)").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Running (2)").assertExists()
        rule.onNodeWithText("Finished (1)").assertExists()
        rule.onNodeWithText("Running for 3 min 12 sec", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Ended 5 minutes ago", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Source unavailable", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Summarize contracts progress").assertExists()
    }

    @Test
    fun eachActionIsNamedWithTheTaskAndCallsBack() {
        val calls = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiBackgroundTasks(
                tasks = listOf(Waiting, Failed),
                inline = true,
                now = Now,
                onRespond = { calls += "respond ${it.id}" },
                onRetry = { calls += "retry ${it.id}" },
                onDismiss = { calls += "dismiss ${it.id}" },
                onCancel = { calls += "cancel ${it.id}" },
                onClearFinished = { list -> calls += "clear ${list.map { it.id }}" },
            )
        }
        rule.onNodeWithContentDescription("Respond: Book the venue")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithContentDescription("Cancel: Book the venue").performClick()
        rule.onNodeWithContentDescription("Retry: Draft newsletter").performClick()
        rule.onNodeWithContentDescription("Dismiss: Draft newsletter").performClick()
        rule.onNodeWithContentDescription("Cancel: Draft newsletter").assertDoesNotExist()
        rule.onNodeWithText("Clear finished").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(
            listOf("respond wait", "cancel wait", "retry fail", "dismiss fail", "clear [fail]"),
            calls,
        )
    }

    @Test
    fun aStatusChangeIsAnnouncedAndNotifiedButNotTheFirstShow() {
        var tasks by mutableStateOf(listOf(Running, Failed))
        val notified = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiBackgroundTasks(tasks = tasks, inline = true, now = Now, onNotify = { notified += it.id })
        }
        rule.waitForIdle()
        rule.onNode(polite, useUnmergedTree = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
        tasks = listOf(Running.copy(status = MonarchAiBackgroundTaskStatus.Succeeded, endedAt = Now), Failed)
        rule.waitForIdle()
        rule.onNode(polite, useUnmergedTree = true).assertContentDescriptionEquals("Summarize contracts: Done")
        assertEquals(listOf("run"), notified)
    }

    @Test
    fun theEmptyPanelSaysSoAndTheStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiBackgroundTasks(
                tasks = emptyList(),
                inline = true,
                now = Now,
                labels = MonarchAiBackgroundTasksLabels(title = "Hintergrund", empty = "Nichts"),
            )
        }
        rule.onNodeWithText("Hintergrund").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Nichts").assertExists()
    }

    @Test
    fun theTitleComesBeforeClearFinishedInRightToLeftAndWraps() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl, fontScale = LargestFontScale) {
            MonarchAiBackgroundTasks(tasks = listOf(Failed), inline = true, now = Now, onClearFinished = {})
        }
        rule.onNodeWithText("Background tasks")
            .assertIsBefore(rule.onNodeWithText("Clear finished"), LayoutDirection.Rtl)
        rule.onNodeWithText("Draft newsletter", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theRulesGroupSortAndFormat() {
        val rules = MonarchAiBackgroundTasksRules
        val older = Running.copy(id = "old", startedAt = Now - 10 * Minute)
        val groups = rules.group(listOf(Queued, Running, older, Waiting, Failed))
        assertEquals(listOf("old", "run", "queue"), groups.running.map { it.id })
        assertEquals(listOf("wait"), groups.needsInput.map { it.id })
        assertEquals(listOf("fail"), groups.finished.map { it.id })
        assertEquals("42 sec", rules.elapsed(42_000L, Locale.US))
        assertEquals("1 hr 4 min", rules.elapsed(3_840_000L, Locale.US))
        assertEquals("0 sec", rules.elapsed(-5L, Locale.US))
        assertNull(rules.known(Running.copy(indeterminate = true)))
        assertEquals(1f, rules.known(Running.copy(progress = 2f)))
        assertEquals("Source unavailable", rules.detail(Failed))
        assertEquals(
            listOf("run"),
            rules.news(
                mapOf("run" to MonarchAiBackgroundTaskStatus.Running),
                listOf(Running.copy(status = MonarchAiBackgroundTaskStatus.NeedsInput), Waiting),
            ).map { it.id },
        )
    }

    @Test
    fun theColoursAndTonesAreRolesInEveryTheme() {
        AllThemes.forEach { tokens ->
            val colors = MonarchColors(tokens.colors)
            val t = MonarchAiBackgroundTasksTokens
            assertEquals(colors.dangerStrong, t.detailColor(MonarchAiBackgroundTaskStatus.Failed, colors))
            assertEquals(colors.textMuted, t.detailColor(MonarchAiBackgroundTaskStatus.Running, colors))
            assertEquals(colors.textDefault, t.titleColor(colors))
        }
        assertEquals(
            MonarchTone.Warning,
            MonarchAiBackgroundTasksTokens.statusTone(MonarchAiBackgroundTaskStatus.NeedsInput),
        )
    }

    @Test
    fun aRunningTaskUsesTheAiToneForItsStatusAndTheAccentForItsBar() {
        val t = MonarchAiBackgroundTasksTokens
        assertEquals(MonarchTone.Ai, t.statusTone(MonarchAiBackgroundTaskStatus.Running))
        assertEquals(MonarchTone.Accent, t.barTone(MonarchAiBackgroundTaskStatus.Running))
        assertEquals(MonarchTone.Neutral, t.barTone(MonarchAiBackgroundTaskStatus.Queued))
        assertEquals(MonarchTone.Warning, t.barTone(MonarchAiBackgroundTaskStatus.NeedsInput))
        assertEquals(MonarchTone.Success, t.barTone(MonarchAiBackgroundTaskStatus.Succeeded))
        assertEquals(MonarchTone.Danger, t.barTone(MonarchAiBackgroundTaskStatus.Failed))
        assertEquals(MonarchTone.Neutral, t.barTone(MonarchAiBackgroundTaskStatus.Cancelled))
    }
}
