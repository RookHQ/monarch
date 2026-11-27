package com.rookhq.monarch.components.intelligence.aieditproposal

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiEditProposalState

/**
 * Replacements for the built-in strings of a `MonarchAiEditProposal`. Every
 * `null` keeps the built-in string `monarch_aieditproposal_<key>`.
 *
 * @property badge the provenance badge, "AI generated".
 * @property state the badge of the proposal's state: "Proposed", "Applied", "Rejected", "Reverted".
 * @property hunkState the decision of a change: "Accepted", "Rejected".
 * @property count the count line from the number of changes and those left: "3 changes", "1 of 3 changes left".
 * @property hunkName the name of a change without a label, from its position and the total: "Change 2 of 3".
 * @property acceptAll the button that accepts every undecided change, "Accept all".
 * @property rejectAll the button that rejects every undecided change, "Reject all".
 * @property accept the button that accepts one change, "Accept".
 * @property reject the button that rejects one change, "Reject".
 * @property revert the button that undoes applied edits, "Revert".
 * @property deleted read before removed text, "Deleted:".
 * @property inserted read before added text, "Inserted:".
 * @property unchanged shown after a change whose text is the same, "No change".
 * @property announcement announced after a decision, from the event and the changes left: "Change accepted.
 * 2 left."
 * @property readOnly the line shown instead of the count once the proposal is decided: "These edits were
 * applied."
 */
@Immutable
public class MonarchAiEditProposalLabels(
    public val badge: String? = null,
    public val state: ((MonarchAiEditProposalState) -> String)? = null,
    public val hunkState: ((MonarchAiEditHunkStatus) -> String)? = null,
    public val count: ((total: Int, pending: Int) -> String)? = null,
    public val hunkName: ((index: Int, total: Int) -> String)? = null,
    public val acceptAll: String? = null,
    public val rejectAll: String? = null,
    public val accept: String? = null,
    public val reject: String? = null,
    public val revert: String? = null,
    public val deleted: String? = null,
    public val inserted: String? = null,
    public val unchanged: String? = null,
    public val announcement: ((event: MonarchAiEditProposalEvent, left: Int) -> String)? = null,
    public val readOnly: ((MonarchAiEditProposalState) -> String)? = null,
)
