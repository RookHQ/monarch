package com.rookhq.monarch.components.workflow.approvalqueue

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchApprovalQueue`. Every
 * `null` keeps the built-in string `monarch_approvalqueue_<key>`.
 *
 * @property label the TalkBack name of the list, "Requests waiting for you".
 * @property approve the Approve button and swipe action.
 * @property reject the Send back button and the confirm button of its dialog.
 * @property review the Review button and swipe action.
 * @property approveItem the TalkBack name of Approve, from the requester's name: "Approve request from Sam Lee".
 * @property rejectItem the TalkBack name of Send back: "Send back request from Sam Lee".
 * @property reviewItem the TalkBack name of Review: "Review request from Sam Lee".
 * @property selectItem the TalkBack name of a request's checkbox: "Select request from Sam Lee".
 * @property selectAll the Select all checkbox.
 * @property approveSelected the bulk action, "Approve selected".
 * @property overdue the badge of a request past its deadline, "Past due".
 * @property search the placeholder of the search field, "Search by name".
 * @property emptyTitle shown when no requests wait, "Nothing to review".
 * @property emptyDescription the line under it.
 * @property noMatchesTitle shown when the search hides every request.
 * @property rejectTitle the title of the Send back dialog, from the requester's name.
 * @property rejectDescription the text of the Send back dialog.
 * @property rejectReason the label of the reason field, "Reason".
 * @property rejectReasonPrompt the placeholder of the reason field.
 * @property bulkTitle the title of the bulk confirmation, from the number of requests: "Approve 3 requests?".
 * @property bulkDescription the text of the bulk confirmation.
 * @property bulkConfirm the confirm button of the bulk confirmation, "Approve all".
 * @property cancel the button that closes a dialog without acting.
 * @property approved announced after an approval, from the requester's name.
 * @property rejected announced after a request was sent back, from the requester's name.
 * @property bulkApproved announced after a bulk approval, from the number of requests.
 * @property failed shown and announced when a decision failed, "That did not work. Try again."
 */
@Immutable
public class MonarchApprovalQueueLabels(
    public val label: String? = null,
    public val approve: String? = null,
    public val reject: String? = null,
    public val review: String? = null,
    public val approveItem: ((requester: String) -> String)? = null,
    public val rejectItem: ((requester: String) -> String)? = null,
    public val reviewItem: ((requester: String) -> String)? = null,
    public val selectItem: ((requester: String) -> String)? = null,
    public val selectAll: String? = null,
    public val approveSelected: String? = null,
    public val overdue: String? = null,
    public val search: String? = null,
    public val emptyTitle: String? = null,
    public val emptyDescription: String? = null,
    public val noMatchesTitle: String? = null,
    public val rejectTitle: ((requester: String) -> String)? = null,
    public val rejectDescription: String? = null,
    public val rejectReason: String? = null,
    public val rejectReasonPrompt: String? = null,
    public val bulkTitle: ((count: Int) -> String)? = null,
    public val bulkDescription: String? = null,
    public val bulkConfirm: String? = null,
    public val cancel: String? = null,
    public val approved: ((requester: String) -> String)? = null,
    public val rejected: ((requester: String) -> String)? = null,
    public val bulkApproved: ((count: Int) -> String)? = null,
    public val failed: String? = null,
)
