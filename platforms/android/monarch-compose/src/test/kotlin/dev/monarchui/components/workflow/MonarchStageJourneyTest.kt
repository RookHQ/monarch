package dev.monarchui.components.workflow

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.workflow.stagejourney.MonarchStageHistory
import dev.monarchui.components.workflow.stagejourney.MonarchStageHistoryEntry
import dev.monarchui.components.workflow.stagejourney.MonarchStageHistoryLabels
import dev.monarchui.components.workflow.stagejourney.MonarchStageJourney
import dev.monarchui.components.workflow.stagejourney.MonarchStageJourneyLabels
import dev.monarchui.components.workflow.stagejourney.MonarchStageJourneyStage
import dev.monarchui.components.workflow.stagejourney.MonarchStageJourneyTerminal
import dev.monarchui.components.workflow.stagejourney.MonarchStageJourneyTokens
import dev.monarchui.components.workflow.stagejourney.MonarchStageJourneyTransition
import dev.monarchui.components.workflow.stagejourney.MonarchStageJourneyTransitionKind
import dev.monarchui.components.workflow.stagejourney.MonarchStageJourneyVariant
import dev.monarchui.components.workflow.stagejourney.StageJourneyStatus
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
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
import kotlin.test.assertNull

private const val Now = 1_793_784_600_000L
private const val Day = 86_400_000L

private val Stages = listOf(
    MonarchStageJourneyStage("filed", "Filed", group = "Application"),
    MonarchStageJourneyStage("exam", "Examination", group = "Application", description = "An examiner reviews it."),
    MonarchStageJourneyStage("granted", "Granted", group = "Outcome"),
)
private val Moves = listOf(
    MonarchStageJourneyTransition("granted", "Granted", needsInput = true),
    MonarchStageJourneyTransition("withdrawn", "Withdrawn", MonarchStageJourneyTransitionKind.Exit),
    MonarchStageJourneyTransition("filed", "Filed", MonarchStageJourneyTransitionKind.Back),
)

internal object MonarchStageJourneyFixtures : ComponentFixtures {
    override val id = "stage-journey"
    override val fixtures = listOf(
        fixture("full, with moves") {
            MonarchStageJourney(
                stages = Stages,
                currentId = "exam",
                enteredAt = Now - 3 * Day,
                currentMeta = "Phase 2",
                transitions = Moves,
                now = Now,
                onTransition = {},
            )
        },
        fixture("success, closed and strip") {
            Column {
                MonarchStageJourney(
                    stages = Stages,
                    currentId = "granted",
                    terminal = MonarchStageJourneyTerminal.Success,
                )
                MonarchStageJourney(
                    stages = Stages,
                    currentId = "exam",
                    terminal = MonarchStageJourneyTerminal.Closed,
                    variant = MonarchStageJourneyVariant.Strip,
                )
                MonarchStageJourney(stages = Stages, currentId = "unknown")
            }
        },
        fixture("history and empty history") {
            Column {
                MonarchStageHistory(
                    entries = listOf(
                        MonarchStageHistoryEntry("2", "Filed", Now - Day, from = "Examination", backward = true),
                        MonarchStageHistoryEntry("1", "Filed", Now - 5 * Day, actor = "Ana", note = "Sent by post"),
                    ),
                    now = Now,
                )
                MonarchStageHistory(entries = emptyList())
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchStageJourneyTest {
    @get:Rule
    val rule = createComposeRule()

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test
    fun stagesAreReadByNameWithTheirStateAndTheCurrentOneIsSelected() {
        rule.setMonarchContent { MonarchStageJourney(stages = Stages, currentId = "exam", now = Now) }
        val filed = rule.onNodeWithContentDescription("Filed")
        assertEquals("done", filed.stateDescription())
        filed.assertIsNotSelected()
        val exam = rule.onNodeWithContentDescription("Examination")
        assertEquals("current stage", exam.stateDescription())
        exam.assertIsSelected()
        assertNull(rule.onNodeWithContentDescription("Granted").stateDescription())
        rule.onNodeWithContentDescription("Stages").assertExists()
    }

    @Test
    fun theCardTitleIsAHeadingWithTheOutcome() {
        rule.setMonarchContent {
            MonarchStageJourney(stages = Stages, currentId = "granted", terminal = MonarchStageJourneyTerminal.Success)
        }
        rule.onNodeWithContentDescription("Now: Granted, Completed").assert(isHeading)
        assertEquals("Completed", rule.onNodeWithContentDescription("Granted").stateDescription())
    }

    @Test
    fun theCardShowsTheDescriptionTheMetaAndTheTimeInTheStage() {
        rule.setMonarchContent {
            MonarchStageJourney(
                stages = Stages,
                currentId = "exam",
                enteredAt = Now - 3 * Day,
                currentMeta = "Phase 2",
                now = Now,
            )
        }
        rule.onNodeWithText("An examiner reviews it.").assertExists()
        rule.onNodeWithText("Phase 2").assertExists()
        rule.onNodeWithText("since 3 days ago").assertExists()
    }

    @Test
    fun movesAreNamedForTheirTargetAndRunOneAtATime() {
        val chosen = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchStageJourney(
                stages = Stages,
                currentId = "exam",
                transitions = Moves,
                onTransition = { chosen += it.id },
            )
        }
        rule.onNodeWithText("Next steps").assert(isHeading)
        rule.onNodeWithContentDescription("Move to Granted, asks for details")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Move to Withdrawn").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Back to Filed").performClick()
        rule.waitForIdle()
        assertEquals(listOf("granted", "withdrawn", "filed"), chosen)
    }

    @Test
    fun withoutOnTransitionNoMovesAreShownAndADisabledMoveStaysDisabled() {
        rule.setMonarchContent {
            Column {
                MonarchStageJourney(stages = Stages, currentId = "exam", transitions = Moves)
                MonarchStageJourney(
                    stages = Stages,
                    currentId = "filed",
                    transitions = listOf(MonarchStageJourneyTransition("x", "Examination", enabled = false)),
                    onTransition = {},
                )
            }
        }
        rule.onNodeWithContentDescription("Move to Granted, asks for details").assertDoesNotExist()
        rule.onNodeWithContentDescription("Move to Examination").assertIsNotEnabled()
    }

    @Test
    fun aFailedMoveIsShownAndTheButtonsComeBack() {
        rule.setMonarchContent {
            MonarchStageJourney(
                stages = Stages,
                currentId = "exam",
                transitions = Moves,
                onTransition = { error("Offline") },
            )
        }
        rule.onNodeWithContentDescription("Move to Withdrawn").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Offline")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithContentDescription("Move to Withdrawn").assertIsEnabled()
    }

    @Test
    fun aChangeOfStageIsAnnounced() {
        var current by mutableStateOf("filed")
        rule.setMonarchContent { MonarchStageJourney(stages = Stages, currentId = current) }
        rule.onNode(hasContentDescription("Moved to", substring = true)).assertDoesNotExist()
        current = "exam"
        rule.waitForIdle()
        rule.onNode(hasContentDescription("Moved to Examination"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun anUnknownCurrentStageShowsNoCardAndTheStripOnlyVariantNoCard() {
        rule.setMonarchContent {
            Column {
                MonarchStageJourney(stages = Stages, currentId = "unknown")
                MonarchStageJourney(stages = Stages, currentId = "exam", variant = MonarchStageJourneyVariant.Strip)
            }
        }
        rule.onNode(hasContentDescription("Now:", substring = true)).assertDoesNotExist()
        rule.onNodeWithText("Now: Examination").assertDoesNotExist()
    }

    @Test
    fun groupsAreConsecutiveRunsAndMovesAreSplit() {
        val tokens = MonarchStageJourneyTokens
        val stages = Stages + MonarchStageJourneyStage("appeal", "Appeal", group = "Application")
        assertEquals(listOf("Application", "Outcome", "Application"), tokens.groups(stages).map { it.name })
        assertEquals(StageJourneyStatus.Upcoming, tokens.status(0, -1))
        assertEquals(StageJourneyStatus.Done, tokens.status(0, 1))
        val extraBack = MonarchStageJourneyTransition("x", "X", MonarchStageJourneyTransitionKind.Back)
        val moves = tokens.moves(Moves + extraBack)
        assertEquals("filed", moves.back?.id)
        assertEquals(listOf("granted"), moves.forward.map { it.id })
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchStageJourney(
                stages = Stages,
                currentId = "exam",
                transitions = Moves,
                labels = MonarchStageJourneyLabels(
                    now = { "Ahora: $it" },
                    done = "hecho",
                    nextSteps = "Siguientes pasos",
                    backTo = { "Volver a $it" },
                ),
                onTransition = {},
            )
        }
        rule.onNodeWithText("Ahora: Examination").assertExists()
        assertEquals("hecho", rule.onNodeWithContentDescription("Filed").stateDescription())
        rule.onNodeWithText("Siguientes pasos").assertExists()
        rule.onNodeWithContentDescription("Volver a Filed").assertExists()
    }

    @Test
    fun historyEntriesAreReadInWords() {
        rule.setMonarchContent {
            MonarchStageHistory(
                entries = listOf(
                    MonarchStageHistoryEntry(
                        "2",
                        "Filed",
                        Now - 2 * Day,
                        from = "Examination",
                        actor = "Ana",
                        backward = true,
                    ),
                    MonarchStageHistoryEntry("1", "Examination", Now - 5 * Day),
                ),
                now = Now,
                labels = MonarchStageHistoryLabels(started = { "Opened at $it" }),
            )
        }
        rule.onNodeWithContentDescription("Examination to Filed, (moved back), 2 days ago, Ana").assertExists()
        rule.onNodeWithContentDescription("Opened at Examination, 5 days ago").assertExists()
    }

    @Test
    fun anEmptyHistorySaysSo() {
        rule.setMonarchContent {
            Column {
                MonarchStageHistory(entries = emptyList())
                MonarchStageHistory(entries = emptyList(), emptyMessage = "Nothing yet")
            }
        }
        rule.onNodeWithText("No stage moves yet.").assertExists()
        rule.onNodeWithText("Nothing yet").assertExists()
    }

    @Test
    fun stagesFollowTheReadingDirectionRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchStageJourney(stages = Stages, currentId = "filed", variant = MonarchStageJourneyVariant.Strip)
        }
        rule.onNodeWithContentDescription("Filed")
            .assertIsBefore(rule.onNodeWithContentDescription("Examination"), LayoutDirection.Rtl)
        assertEquals("←", MonarchStageJourneyTokens.arrow(LayoutDirection.Rtl))
    }

    @Test
    fun theCardTitleWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchStageJourney(stages = Stages, currentId = "exam")
        }
        rule.onNodeWithText("Now: Examination").assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchStageJourneyTokens
            assertEquals(colors.accentSubtle, tokens.stageColors(colors, StageJourneyStatus.Done, null).container)
            assertEquals(colors.accentSolid, tokens.stageColors(colors, StageJourneyStatus.Current, null).container)
            assertEquals(
                colors.successSolid,
                tokens.stageColors(colors, StageJourneyStatus.Current, MonarchStageJourneyTerminal.Success).container,
            )
            assertEquals(
                colors.borderStrong,
                tokens.stageColors(colors, StageJourneyStatus.Current, MonarchStageJourneyTerminal.Closed).border,
            )
            assertEquals(Color.Transparent, tokens.stageColors(colors, StageJourneyStatus.Upcoming, null).container)
            assertEquals(colors.successSubtle, tokens.cardColors(colors, MonarchStageJourneyTerminal.Success).first)
            assertEquals(colors.warningSolid, tokens.historyColors(colors, backward = true).first)
        }
    }
}
