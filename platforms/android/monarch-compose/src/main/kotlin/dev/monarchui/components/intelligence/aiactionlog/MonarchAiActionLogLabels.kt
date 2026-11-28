package dev.monarchui.components.intelligence.aiactionlog

import androidx.compose.runtime.Immutable

/**
 * Replacements for the strings `MonarchAiActionLog` supplies. Each `null`
 * keeps the built-in English string `monarch_aiactionlog_<key>`. Functions
 * that take a description receive the entry as a sentence without the actor
 * kind: "Research agent added a summary to Quarterly report".
 *
 * @property label the TalkBack name of the list. Default "AI activity".
 * @property actorKind the badge after the agent's name. Default "AI agent".
 * @property undo the visible text of the undo button. Default "Undo".
 * @property undoLabel the TalkBack name of an undo button. Default "Undo: {description}".
 * @property undone added to an undone entry's time. Default "Undone".
 * @property undoneAnnouncement announced after an undo. Default "Undone: {description}".
 * @property undoFailed shown and announced when an undo fails. Default "Could not undo: {description}".
 * @property openTarget the TalkBack action that opens a target link. Default "Open {target}".
 * @property empty shown when there are no entries. Default "No AI activity yet.".
 * @property showMore the button that shows more entries, from how many it adds. Default "Show 5 more actions".
 */
@Immutable
public class MonarchAiActionLogLabels(
    public val label: String? = null,
    public val actorKind: String? = null,
    public val undo: String? = null,
    public val undoLabel: ((description: String) -> String)? = null,
    public val undone: String? = null,
    public val undoneAnnouncement: ((description: String) -> String)? = null,
    public val undoFailed: ((description: String) -> String)? = null,
    public val openTarget: ((target: String) -> String)? = null,
    public val empty: String? = null,
    public val showMore: ((count: Int) -> String)? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchAiActionLogLabels && other.label == label && other.actorKind == actorKind &&
            other.undo == undo && other.undoLabel == undoLabel && other.undone == undone &&
            other.undoneAnnouncement == undoneAnnouncement && other.undoFailed == undoFailed &&
            other.openTarget == openTarget && other.empty == empty && other.showMore == showMore

    override fun hashCode(): Int = listOf(
        label, actorKind, undo, undoLabel, undone, undoneAnnouncement, undoFailed, openTarget, empty, showMore,
    ).hashCode()
}
