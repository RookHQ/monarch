package com.rookhq.monarch.components.workflow.stagejourney

import androidx.compose.runtime.Immutable

/**
 * A move offered from the current stage of a `MonarchStageJourney`.
 *
 * @property id a stable identifier, usually the target stage's id.
 * @property label the target stage's name, such as "Granted".
 * @property kind forward, exit or back.
 * @property needsInput the move asks for more details (the app opens a form); TalkBack says so.
 * @property enabled whether the move is possible now.
 */
@Immutable
public class MonarchStageJourneyTransition(
    public val id: String,
    public val label: String,
    public val kind: MonarchStageJourneyTransitionKind = MonarchStageJourneyTransitionKind.Forward,
    public val needsInput: Boolean = false,
    public val enabled: Boolean = true,
)
