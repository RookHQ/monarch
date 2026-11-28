package dev.monarchui.components.workflow.stagejourney

import androidx.compose.runtime.Immutable

/**
 * One stage of a pipeline or lifecycle in a `MonarchStageJourney`.
 *
 * @property id a stable identifier, matched against `currentId`.
 * @property label the short name, such as "Examination".
 * @property group the phase; consecutive stages with the same group are shown together under its name.
 * @property description what the stage means, shown in the current-stage card while it is current.
 */
@Immutable
public class MonarchStageJourneyStage(
    public val id: String,
    public val label: String,
    public val group: String? = null,
    public val description: String? = null,
)
