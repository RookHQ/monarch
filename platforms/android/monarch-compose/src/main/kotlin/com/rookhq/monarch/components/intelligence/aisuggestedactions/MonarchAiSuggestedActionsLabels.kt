package com.rookhq.monarch.components.intelligence.aisuggestedactions

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallRisk

/**
 * Replacements for the built-in strings of `MonarchAiSuggestedActions`.
 * Every `null` keeps the built-in string `monarch_aisuggestedactions_<key>`.
 *
 * @property title the default heading: "Suggested actions".
 * @property originAi the origin label for model suggestions: "Suggested by AI".
 * @property originRule the origin label for rule-based suggestions: "Rule-based".
 * @property run the Run button: "Run".
 * @property runAction what TalkBack reads for the Run button, given the action's label: "Run Send the reminder".
 * @property running what the Run button reads while the action runs: "Running…".
 * @property done shown in place of Run once the action finished: "Done".
 * @property failed shown under an action whose run failed: "That didn’t work. Try again."
 * @property dismiss the name of the dismiss button, given the action's label: "Dismiss “Send the reminder”".
 * @property dismissText the dismiss button's text when the app gives no icon: "Dismiss".
 * @property needsApproval the tag of an action that asks for approval: "Needs approval".
 * @property risk the tag of a risk, or `null` for no tag; the built-in tags name medium, high and destructive.
 * @property approvalTitle the approval dialog's title, given the action's label: "Run “Send the reminder”?".
 * @property approvalDescription the approval dialog's text when the action has no description.
 * @property approve the approval dialog's confirm button when the action has no confirm label: "Run action".
 * @property cancel the approval dialog's cancel button: "Cancel".
 * @property doneAnnouncement announced when an action finishes, given its label: "“Send the reminder” done."
 * @property empty shown when there are no actions: "No suggestions right now."
 */
@Immutable
public class MonarchAiSuggestedActionsLabels(
    public val title: String? = null,
    public val originAi: String? = null,
    public val originRule: String? = null,
    public val run: String? = null,
    public val runAction: ((label: String) -> String)? = null,
    public val running: String? = null,
    public val done: String? = null,
    public val failed: String? = null,
    public val dismiss: ((label: String) -> String)? = null,
    public val dismissText: String? = null,
    public val needsApproval: String? = null,
    public val risk: ((MonarchAiToolCallRisk) -> String?)? = null,
    public val approvalTitle: ((label: String) -> String)? = null,
    public val approvalDescription: String? = null,
    public val approve: String? = null,
    public val cancel: String? = null,
    public val doneAnnouncement: ((label: String) -> String)? = null,
    public val empty: String? = null,
)
