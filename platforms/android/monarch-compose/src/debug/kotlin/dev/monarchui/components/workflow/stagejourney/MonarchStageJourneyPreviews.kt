package dev.monarchui.components.workflow.stagejourney

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private const val PreviewNow = 1_793_784_600_000L
private const val PreviewDay = 86_400_000L

private val PreviewStages = listOf(
    MonarchStageJourneyStage("filed", "Filed", group = "Application"),
    MonarchStageJourneyStage("exam", "Examination", group = "Application", description = "An examiner reviews it."),
    MonarchStageJourneyStage("granted", "Granted", group = "Outcome"),
)

@MonarchPreviews
@Composable
internal fun MonarchStageJourneyPreview() {
    PreviewFrame {
        MonarchStageJourney(
            stages = PreviewStages,
            currentId = "exam",
            enteredAt = PreviewNow - 3 * PreviewDay,
            currentMeta = "Phase 2",
            transitions = listOf(
                MonarchStageJourneyTransition("granted", "Granted"),
                MonarchStageJourneyTransition("withdrawn", "Withdrawn", MonarchStageJourneyTransitionKind.Exit),
                MonarchStageJourneyTransition("filed", "Filed", MonarchStageJourneyTransitionKind.Back),
            ),
            now = PreviewNow,
            onTransition = {},
        )
        MonarchStageJourney(
            stages = PreviewStages,
            currentId = "granted",
            terminal = MonarchStageJourneyTerminal.Success,
            variant = MonarchStageJourneyVariant.Strip,
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchStageHistoryPreview() {
    PreviewFrame {
        MonarchStageHistory(
            entries = listOf(
                MonarchStageHistoryEntry(
                    "2",
                    to = "Examination",
                    at = PreviewNow - PreviewDay,
                    from = "Filed",
                    actor = "Ana",
                ),
                MonarchStageHistoryEntry("1", to = "Filed", at = PreviewNow - 5 * PreviewDay, note = "Sent by post"),
            ),
            now = PreviewNow,
        )
        MonarchStageHistory(entries = emptyList())
    }
}
