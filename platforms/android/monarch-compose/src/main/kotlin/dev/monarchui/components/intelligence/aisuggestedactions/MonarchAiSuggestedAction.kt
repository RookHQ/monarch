package dev.monarchui.components.intelligence.aisuggestedactions

import androidx.compose.runtime.Immutable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallRisk

/**
 * One suggested next step in `MonarchAiSuggestedActions`.
 *
 * @property id a stable id, passed to `onRun` and `onDismiss`.
 * @property label what the action does, as a verb phrase: "Send the renewal reminder".
 * @property description why it is suggested or what it will change.
 * @property requiresApproval asks for confirmation in a dialog before running. Implied by a destructive [risk].
 * @property risk how much harm running it could do. Medium and above is shown as a text tag; it also sets the
 * tone of the approval dialog's confirm button.
 * @property consequences the concrete consequences listed in the approval dialog: "3 people will get an email".
 * @property confirmLabel the approval dialog's confirm button; "Run action" without it.
 * @property status the status, when the app tracks it (for example work that finishes on the server later).
 * Without it the component tracks the status from `onRun`.
 */
@Immutable
public data class MonarchAiSuggestedAction(
    public val id: String,
    public val label: String,
    public val description: String? = null,
    public val requiresApproval: Boolean = false,
    public val risk: MonarchAiToolCallRisk = MonarchAiToolCallRisk.Low,
    public val consequences: List<String> = emptyList(),
    public val confirmLabel: String? = null,
    public val status: MonarchAiSuggestedActionStatus? = null,
) {
    /** Whether the action must be confirmed in a dialog before it runs: it asks for approval or is destructive. */
    public val needsApproval: Boolean
        get() = requiresApproval || risk == MonarchAiToolCallRisk.Destructive
}
