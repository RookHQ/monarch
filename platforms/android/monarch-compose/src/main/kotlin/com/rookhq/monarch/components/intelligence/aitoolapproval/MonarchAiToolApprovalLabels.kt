package com.rookhq.monarch.components.intelligence.aitoolapproval

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallRisk

/**
 * Replacements for the built-in strings of `MonarchAiToolApproval`. Every
 * `null` keeps the built-in text.
 *
 * @property eyebrow the small text above the title. Default "Approval needed".
 * @property risk the risk badge per risk. Default "Low risk", "Medium risk", "High risk", "Can't be undone".
 * @property consequence the consequence line per risk when the app gives none; `null` from it shows nothing. Default
 * "This can't be undone once it runs." for destructive calls only.
 * @property details the heading of the arguments. Default "Details".
 * @property noArguments shown when the call has no arguments. Default "No arguments.".
 * @property approve the approve button. Default "Approve".
 * @property deny the deny button. Default "Deny".
 * @property editArguments opens the JSON editor. Default "Edit arguments".
 * @property discardEdits closes the JSON editor and drops the edits. Default "Discard edits".
 * @property argumentsJson the label of the JSON editor. Default "Arguments (JSON)".
 * @property invalidJson shown when the edited arguments are not valid JSON. Default "Fix the JSON before approving.".
 * @property addReason opens the reason field. Default "Add a reason".
 * @property reason the label of the reason field. Default "Reason (optional)".
 * @property alwaysAllow the label of the always-allow checkbox. Default "Always allow this tool".
 * @property approved shown once the call is approved. Default "Approved".
 * @property denied shown once the call is denied. Default "Denied".
 * @property cancelled shown when the reply stopped before anyone decided. Default "Cancelled".
 * @property announce announced when the card appears; receives the title. Default "Approval needed: title".
 */
@Immutable
public class MonarchAiToolApprovalLabels(
    public val eyebrow: String? = null,
    public val risk: ((MonarchAiToolCallRisk) -> String)? = null,
    public val consequence: ((MonarchAiToolCallRisk) -> String?)? = null,
    public val details: String? = null,
    public val noArguments: String? = null,
    public val approve: String? = null,
    public val deny: String? = null,
    public val editArguments: String? = null,
    public val discardEdits: String? = null,
    public val argumentsJson: String? = null,
    public val invalidJson: String? = null,
    public val addReason: String? = null,
    public val reason: String? = null,
    public val alwaysAllow: String? = null,
    public val approved: String? = null,
    public val denied: String? = null,
    public val cancelled: String? = null,
    public val announce: ((title: String) -> String)? = null,
)
