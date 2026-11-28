package com.rookhq.monarch.components.workflow.approvaltimeline

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import com.rookhq.monarch.R

/** The strings of one MonarchApprovalTimeline, with the caller's replacements applied. */
internal class MonarchApprovalTimelineStrings(
    private val labels: MonarchApprovalTimelineLabels,
    private val resources: Resources,
) {
    /** The spoken name of the timeline. */
    val label: String get() = labels.label ?: resources.getString(R.string.monarch_approvaltimeline_label)

    /** Spoken on the step waiting for a decision now. */
    val current: String get() = labels.current ?: resources.getString(R.string.monarch_approvaltimeline_current)

    /** Shown when there are no steps. */
    val empty: String get() = labels.empty ?: resources.getString(R.string.monarch_approvaltimeline_empty)

    /** The status text of [value]. */
    fun status(value: MonarchApprovalStepStatus): String = labels.status?.invoke(value) ?: resources.getString(
        when (value) {
            MonarchApprovalStepStatus.Submitted -> R.string.monarch_approvaltimeline_status_submitted
            MonarchApprovalStepStatus.Approved -> R.string.monarch_approvaltimeline_status_approved
            MonarchApprovalStepStatus.Rejected -> R.string.monarch_approvaltimeline_status_rejected
            MonarchApprovalStepStatus.Pending -> R.string.monarch_approvaltimeline_status_pending
            MonarchApprovalStepStatus.Waiting -> R.string.monarch_approvaltimeline_status_waiting
            MonarchApprovalStepStatus.Skipped -> R.string.monarch_approvaltimeline_status_skipped
            MonarchApprovalStepStatus.Cancelled -> R.string.monarch_approvaltimeline_status_cancelled
        },
    )

    /** "Decided by …". */
    fun approvers(names: String): String =
        labels.approvers?.invoke(names) ?: resources.getString(R.string.monarch_approvaltimeline_approvers, names)

    /** "standing in for …". */
    fun delegatedFrom(name: String): String =
        labels.delegatedFrom?.invoke(name)
            ?: resources.getString(R.string.monarch_approvaltimeline_delegated_from, name)
}

/** The strings of an approval timeline, from [labels] and the built-in resources. */
@Composable
internal fun monarchApprovalTimelineStrings(labels: MonarchApprovalTimelineLabels): MonarchApprovalTimelineStrings =
    MonarchApprovalTimelineStrings(labels, LocalResources.current)
