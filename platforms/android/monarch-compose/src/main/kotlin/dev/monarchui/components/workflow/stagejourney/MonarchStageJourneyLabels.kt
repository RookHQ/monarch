package dev.monarchui.components.workflow.stagejourney

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchStageJourney`. Every
 * `null` keeps the built-in string `monarch_stagejourney_<key>`.
 *
 * @property stages the name of the stage strip, "Stages".
 * @property now the card's title for a stage: "Now: Examination".
 * @property since the time in the stage for a relative time: "since 3 days ago".
 * @property done the state of a finished stage, "done".
 * @property current the state of the current stage, "current stage".
 * @property success the outcome of a journey that ended well, "Completed".
 * @property closed the outcome of a journey that ended early, "Closed".
 * @property nextSteps the heading above the moves, "Next steps".
 * @property moveTo what TalkBack reads for a forward or exit move: "Move to Granted".
 * @property backTo the text of a back move: "Back to Filed".
 * @property needsInput added to a move that asks for details, "asks for details".
 * @property moved announced when the current stage changes: "Moved to Granted".
 * @property failed shown when a move throws without a message, "The move did not go through. Try again.".
 */
@Immutable
public class MonarchStageJourneyLabels(
    public val stages: String? = null,
    public val now: ((String) -> String)? = null,
    public val since: ((String) -> String)? = null,
    public val done: String? = null,
    public val current: String? = null,
    public val success: String? = null,
    public val closed: String? = null,
    public val nextSteps: String? = null,
    public val moveTo: ((String) -> String)? = null,
    public val backTo: ((String) -> String)? = null,
    public val needsInput: String? = null,
    public val moved: ((String) -> String)? = null,
    public val failed: String? = null,
)
