package dev.monarchui.components.workflow.stagejourney

import androidx.compose.runtime.Immutable

/**
 * One move in a `MonarchStageHistory`.
 *
 * @property id a stable identifier.
 * @property to the stage moved to.
 * @property at when the move happened, in epoch milliseconds.
 * @property from the stage moved from; `null` for the first stage ("Started at …").
 * @property actor who or what made the move, such as a name or "Automation".
 * @property meta extra facts, such as "2 deadlines added".
 * @property note why, in the mover's words.
 * @property backward the move went back to an earlier stage.
 */
@Immutable
public class MonarchStageHistoryEntry(
    public val id: String,
    public val to: String,
    public val at: Long,
    public val from: String? = null,
    public val actor: String? = null,
    public val meta: String? = null,
    public val note: String? = null,
    public val backward: Boolean = false,
)
