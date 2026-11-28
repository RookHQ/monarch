package dev.monarchui.components.workflow.stagejourney

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchStageHistory`. Every
 * `null` keeps the built-in string `monarch_stagejourney_history_<key>`.
 *
 * @property started the first move: "Started at Filed".
 * @property to the word TalkBack reads between two stages, "to".
 * @property movedBack the marker of a backward move, "(moved back)".
 * @property empty shown when there are no moves, "No stage moves yet.".
 */
@Immutable
public class MonarchStageHistoryLabels(
    public val started: ((String) -> String)? = null,
    public val to: String? = null,
    public val movedBack: String? = null,
    public val empty: String? = null,
)
