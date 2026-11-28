package com.rookhq.monarch.components.workflow.approvaltimeline

import androidx.compose.runtime.Immutable

/**
 * One step of a `MonarchApprovalTimeline`.
 *
 * @property id a stable, unique id.
 * @property label what the step is, such as "Reporting manager" or "Finance check". Already localised.
 * @property status where the step stands.
 * @property actor who decided or submitted. Shown with the time on decided steps.
 * @property approvers who can decide a pending or waiting step.
 * @property delegatedFrom the person an approver is standing in for.
 * @property date when it was decided or submitted, as epoch milliseconds (UTC).
 * @property comment the decider's comment, shown as a quote.
 * @property note why the step was skipped or cancelled, or another short note.
 */
@Immutable
public class MonarchApprovalTimelineStep(
    public val id: String,
    public val label: String,
    public val status: MonarchApprovalStepStatus,
    public val actor: MonarchApprovalPerson? = null,
    public val approvers: List<MonarchApprovalPerson> = emptyList(),
    public val delegatedFrom: MonarchApprovalPerson? = null,
    public val date: Long? = null,
    public val comment: String? = null,
    public val note: String? = null,
)
