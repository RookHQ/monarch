package com.rookhq.monarch.components.workflow

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.rookhq.monarch.components.workflow.slaindicator.MonarchSlaIndicator
import com.rookhq.monarch.components.workflow.slaindicator.MonarchSlaIndicatorLabels
import com.rookhq.monarch.components.workflow.slaindicator.MonarchSlaIndicatorState
import com.rookhq.monarch.components.workflow.slaindicator.MonarchSlaIndicatorTokens
import com.rookhq.monarch.components.workflow.slaindicator.MonarchSlaIndicatorVariant
import com.rookhq.monarch.components.workflow.slaindicator.SlaDuration
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val Now = 1_800_000_000_000L
private const val Minute = 60_000L
private const val Hour = 60 * Minute

internal object MonarchSlaIndicatorFixtures : ComponentFixtures {
    override val id = "sla-indicator"
    override val fixtures = MonarchSlaIndicatorVariant.entries.map { variant ->
        fixture("every state, ${variant.name}") {
            Column {
                MonarchSlaIndicator(
                    MonarchSlaIndicatorState.OnTrack,
                    variant = variant,
                    dueAt = Now + 2 * Hour + 15 * Minute,
                    now = Now,
                    icon = {
                        MonarchIcon(imageVector = TestIcon, contentDescription = null, size = MonarchIconSize.Sm)
                    },
                )
                MonarchSlaIndicator(
                    MonarchSlaIndicatorState.AtRisk,
                    variant = variant,
                    dueAt = Now + 40 * Minute,
                    now = Now,
                )
                MonarchSlaIndicator(
                    MonarchSlaIndicatorState.Breached,
                    variant = variant,
                    dueAt = Now - 40 * Minute,
                    now = Now,
                )
                MonarchSlaIndicator(MonarchSlaIndicatorState.Paused, variant = variant)
                MonarchSlaIndicator(MonarchSlaIndicatorState.Met, variant = variant)
                MonarchSlaIndicator(MonarchSlaIndicatorState.NotApplicable, variant = variant)
                MonarchSlaIndicator(MonarchSlaIndicatorState.NotApplicable, variant = variant, showNotApplicable = true)
            }
        }
    } + fixture("announcing, with a target") {
        MonarchSlaIndicator(
            MonarchSlaIndicatorState.OnTrack,
            dueAt = Now + 3 * 24 * Hour + 4 * Hour,
            now = Now,
            target = "Resolution",
            announceChanges = true,
            earlierBreach = true,
        )
    }
}

@RunWith(RobolectricTestRunner::class)
class MonarchSlaIndicatorTest {
    @get:Rule
    val rule = createComposeRule()

    private fun ComposeContentTestRule.onNodeWithVisibleText(text: String) {
        onNodeWithText(text, useUnmergedTree = true).assertExists()
    }

    @Test
    fun aCountdownIsReadInWordsWithTheTargetAndTheStateAndDueTimeAsState() {
        rule.setMonarchContent {
            MonarchSlaIndicator(
                MonarchSlaIndicatorState.OnTrack,
                modifier = Modifier.testTag("sla"),
                dueAt = Now + 2 * Hour + 15 * Minute,
                now = Now,
                target = "First response",
                earlierBreach = true,
            )
        }
        val node = rule.onNodeWithContentDescription("First response, 2 hours, 15 minutes left")
        node.assertHasNoClickAction()
        val state = checkNotNull(node.stateDescription())
        assertTrue(state.startsWith("On track, due "), state)
        assertTrue(state.endsWith(", an earlier target was breached"), state)
    }

    @Test
    fun theVisibleTextIsShortAndOverdueTimeIsShownForABreach() {
        rule.setMonarchContent {
            Column {
                MonarchSlaIndicator(MonarchSlaIndicatorState.OnTrack, dueAt = Now + 45 * Minute + 30_000, now = Now)
                MonarchSlaIndicator(MonarchSlaIndicatorState.Breached, dueAt = Now - 40 * Minute, now = Now)
                MonarchSlaIndicator(MonarchSlaIndicatorState.AtRisk, dueAt = Now + 20_000, now = Now)
            }
        }
        rule.onNodeWithVisibleText("45m left")
        rule.onNodeWithVisibleText("Overdue 40m")
        rule.onNodeWithVisibleText("<1m left")
    }

    @Test
    fun aRunningTargetPastItsDueTimeIsBreachedAndOneWithinTheWarningIsAtRisk() {
        val tokens = MonarchSlaIndicatorTokens
        assertEquals(
            MonarchSlaIndicatorState.Breached,
            tokens.resolve(MonarchSlaIndicatorState.OnTrack, Now, Now, null),
        )
        assertEquals(
            MonarchSlaIndicatorState.AtRisk,
            tokens.resolve(MonarchSlaIndicatorState.OnTrack, Now + 30 * Minute, Now, Hour),
        )
        assertEquals(
            MonarchSlaIndicatorState.OnTrack,
            tokens.resolve(MonarchSlaIndicatorState.OnTrack, Now + 2 * Hour, Now, Hour),
        )
        assertEquals(MonarchSlaIndicatorState.Paused, tokens.resolve(MonarchSlaIndicatorState.Paused, Now, Now, Hour))
        assertEquals(MonarchSlaIndicatorState.Met, tokens.resolve(MonarchSlaIndicatorState.Met, Now - Hour, Now, null))
        assertEquals(MonarchSlaIndicatorState.AtRisk, tokens.resolve(MonarchSlaIndicatorState.AtRisk, null, Now, null))
    }

    @Test
    fun durationsAreWholeMinutesInAtMostTwoUnits() {
        val tokens = MonarchSlaIndicatorTokens
        assertEquals(SlaDuration(0, 0, 0), tokens.duration(59_999))
        assertEquals(SlaDuration(0, 0, 45), tokens.duration(45 * Minute + 59_000))
        assertEquals(SlaDuration(0, 2, 15), tokens.duration(2 * Hour + 15 * Minute))
        assertEquals(SlaDuration(3, 4, 0), tokens.duration(3 * 24 * Hour + 4 * Hour + 59 * Minute))
        assertEquals(SlaDuration(0, 2, 15), tokens.duration(-(2 * Hour + 15 * Minute)))
    }

    @Test
    fun theClockWakesAtTheNextMinuteOrTheWarningEdge() {
        val tokens = MonarchSlaIndicatorTokens
        assertEquals(30_001, tokens.nextTick(Now, Now + 5 * Minute + 30_000, null))
        assertEquals(20_000, tokens.nextTick(Now, Now + Hour + 20_000 + 10_000, Hour + 10_000))
        assertEquals(Minute - 15_000, tokens.nextTick(Now, Now - 15_000, null))
    }

    @Test
    fun aChangeOfTheShownStateIsAnnouncedAndReported() {
        var now by mutableLongStateOf(Now)
        val changes = mutableListOf<MonarchSlaIndicatorState>()
        rule.setMonarchContent {
            MonarchSlaIndicator(
                MonarchSlaIndicatorState.OnTrack,
                modifier = Modifier.testTag("sla"),
                dueAt = Now + Minute,
                now = now,
                target = "First response",
                announceChanges = true,
                onStateChange = { changes += it },
            )
        }
        val liveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        rule.onNode(liveRegion).assertExists()
        assertEquals(emptyList(), changes)
        now = Now + 2 * Minute
        rule.waitForIdle()
        assertEquals(listOf(MonarchSlaIndicatorState.Breached), changes)
        rule.onNode(liveRegion).assertContentDescriptionEquals("First response: Breached")
        rule.onNodeWithContentDescription("First response, Overdue 1 minute").assertExists()
    }

    @Test
    fun notApplicableIsADashReadAsNoSlaAndLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            Column {
                MonarchSlaIndicator(MonarchSlaIndicatorState.NotApplicable, modifier = Modifier.testTag("dash"))
                MonarchSlaIndicator(
                    MonarchSlaIndicatorState.Met,
                    modifier = Modifier.testTag("met"),
                    labels = MonarchSlaIndicatorLabels(met = "Erfüllt"),
                )
                MonarchSlaIndicator(
                    MonarchSlaIndicatorState.OnTrack,
                    dueAt = Now + Hour,
                    now = Now,
                    labels = MonarchSlaIndicatorLabels(remaining = { "noch $it" }, duration = { "${it / Minute} min" }),
                )
            }
        }
        rule.onNodeWithContentDescription("No SLA").assertExists()
        rule.onNodeWithVisibleText("—")
        rule.onNodeWithContentDescription("Erfüllt").assertExists()
        rule.onNodeWithContentDescription("noch 60 min").assertExists()
    }

    @Test
    fun theTextGrowsWithTheFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchSlaIndicator(MonarchSlaIndicatorState.Paused, modifier = Modifier.testTag("sla"))
        }
        val height = rule.onNodeWithTag("sla").fetchSemanticsNode().size.height
        with(rule.density) { assertTrue(height.toDp() >= MonarchSlaIndicatorTokens.MinHeight) }
    }

    @Test
    fun coloursComeFromTheStateRolesInEveryTheme() {
        val tokens = MonarchSlaIndicatorTokens
        val chip = MonarchSlaIndicatorVariant.Chip
        val text = MonarchSlaIndicatorVariant.Text
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val atRisk = tokens.colors(MonarchSlaIndicatorState.AtRisk, chip, colors)
            assertEquals(colors.warningSubtle, atRisk.container, theme.id)
            assertEquals(colors.warningStrong, atRisk.content, theme.id)
            assertEquals(colors.warningBorder, atRisk.border, theme.id)
            val breached = tokens.colors(MonarchSlaIndicatorState.Breached, chip, colors)
            assertEquals(colors.dangerSolid, breached.container, theme.id)
            assertEquals(colors.dangerOnSolid, breached.content, theme.id)
            val breachedText = tokens.colors(MonarchSlaIndicatorState.Breached, text, colors)
            assertEquals(Color.Transparent, breachedText.container, theme.id)
            assertEquals(colors.dangerStrong, breachedText.content, theme.id)
            assertEquals(colors.bgMuted, tokens.colors(MonarchSlaIndicatorState.Paused, chip, colors).container)
            assertEquals(colors.successStrong, tokens.colors(MonarchSlaIndicatorState.Met, chip, colors).content)
            val none = tokens.colors(MonarchSlaIndicatorState.NotApplicable, chip, colors)
            assertEquals(colors.textSubtle, none.content, theme.id)
            assertNull(none.border, theme.id)
            assertEquals(colors.textMuted, tokens.colors(MonarchSlaIndicatorState.OnTrack, chip, colors).content)
        }
    }
}
