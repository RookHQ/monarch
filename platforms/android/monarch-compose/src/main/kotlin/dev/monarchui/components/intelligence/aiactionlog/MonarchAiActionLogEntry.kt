package dev.monarchui.components.intelligence.aiactionlog

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef

/**
 * One thing an AI agent did, as a `MonarchAiActionLog` shows it.
 *
 * @property id a stable id, passed to `onUndo`.
 * @property date when it happened, in epoch milliseconds (UTC).
 * @property agent the agent that acted.
 * @property action what it did, as a verb phrase, such as "added a summary to".
 * @property target what it acted on, such as "Quarterly report".
 * @property url a link to the target. Only an http or https URL makes the target a link.
 * @property undoable the action can be reversed; shows Undo when the log has `onUndo`.
 * @property undone the action has already been undone.
 * @property agentImage the agent's picture, loaded by the app; `null` shows the avatar's initials.
 */
@Immutable
public data class MonarchAiActionLogEntry(
    public val id: String,
    public val date: Long,
    public val agent: MonarchAiAgentRef,
    public val action: String,
    public val target: String? = null,
    public val url: String? = null,
    public val undoable: Boolean = false,
    public val undone: Boolean = false,
    public val agentImage: Painter? = null,
)
