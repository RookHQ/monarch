package dev.monarchui.components.workflow.approvalqueue

import android.content.res.Resources
import dev.monarchui.R

/** The strings of a MonarchApprovalQueue: each override from [labels], or the built-in resource. */
internal class MonarchApprovalQueueText(
    private val labels: MonarchApprovalQueueLabels,
    private val resources: Resources,
) {
    private fun string(override: String?, id: Int): String = override ?: resources.getString(id)

    val label: String get() = string(labels.label, R.string.monarch_approvalqueue_label)
    val approve: String get() = string(labels.approve, R.string.monarch_approvalqueue_approve)
    val reject: String get() = string(labels.reject, R.string.monarch_approvalqueue_reject)
    val review: String get() = string(labels.review, R.string.monarch_approvalqueue_review)
    val selectAll: String get() = string(labels.selectAll, R.string.monarch_approvalqueue_select_all)
    val approveSelected: String
        get() = string(labels.approveSelected, R.string.monarch_approvalqueue_approve_selected)
    val overdue: String get() = string(labels.overdue, R.string.monarch_approvalqueue_overdue)
    val search: String get() = string(labels.search, R.string.monarch_approvalqueue_search)
    val emptyTitle: String get() = string(labels.emptyTitle, R.string.monarch_approvalqueue_empty_title)
    val emptyDescription: String
        get() = string(labels.emptyDescription, R.string.monarch_approvalqueue_empty_description)
    val noMatchesTitle: String get() = string(labels.noMatchesTitle, R.string.monarch_approvalqueue_no_matches_title)
    val rejectDescription: String
        get() = string(labels.rejectDescription, R.string.monarch_approvalqueue_reject_description)
    val rejectReason: String get() = string(labels.rejectReason, R.string.monarch_approvalqueue_reject_reason)
    val rejectReasonPrompt: String
        get() = string(labels.rejectReasonPrompt, R.string.monarch_approvalqueue_reject_reason_prompt)
    val bulkDescription: String
        get() = string(labels.bulkDescription, R.string.monarch_approvalqueue_bulk_description)
    val bulkConfirm: String get() = string(labels.bulkConfirm, R.string.monarch_approvalqueue_bulk_confirm)
    val cancel: String get() = string(labels.cancel, R.string.monarch_approvalqueue_cancel)
    val failed: String get() = string(labels.failed, R.string.monarch_approvalqueue_failed)

    private fun named(override: ((String) -> String)?, id: Int, name: String): String =
        override?.invoke(name) ?: resources.getString(id, name)

    fun approveItem(name: String): String = named(labels.approveItem, R.string.monarch_approvalqueue_approve_item, name)
    fun rejectItem(name: String): String = named(labels.rejectItem, R.string.monarch_approvalqueue_reject_item, name)
    fun reviewItem(name: String): String = named(labels.reviewItem, R.string.monarch_approvalqueue_review_item, name)
    fun selectItem(name: String): String = named(labels.selectItem, R.string.monarch_approvalqueue_select_item, name)
    fun rejectTitle(name: String): String = named(labels.rejectTitle, R.string.monarch_approvalqueue_reject_title, name)
    fun approved(name: String): String = named(labels.approved, R.string.monarch_approvalqueue_approved, name)
    fun rejected(name: String): String = named(labels.rejected, R.string.monarch_approvalqueue_rejected, name)

    fun bulkTitle(count: Int): String = labels.bulkTitle?.invoke(count)
        ?: resources.getQuantityString(R.plurals.monarch_approvalqueue_bulk_title, count, count)

    fun bulkApproved(count: Int): String = labels.bulkApproved?.invoke(count)
        ?: resources.getQuantityString(R.plurals.monarch_approvalqueue_bulk_approved, count, count)
}
