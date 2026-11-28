package com.rookhq.monarch.components.workflow.approvaltimeline

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchApprovalTimeline`. Every `null` uses the built-in string in
 * `strings_approvaltimeline.xml`.
 *
 * @property label the spoken name of the timeline: "Approval timeline".
 * @property status the status text after each step's label: "Submitted", "Approved", "Rejected", "Waiting for a
 * decision", "Up next", "Skipped", "Cancelled".
 * @property approvers the line naming who can decide a pending or waiting step, from the joined names: "Decided by
 * Ana, Ben".
 * @property delegatedFrom the text about an approver who stands in for someone, from that person's name: "standing in
 * for Sam".
 * @property current spoken on the step waiting for a decision now: "Current step".
 * @property empty shown when there are no steps: "Not submitted yet".
 */
@Immutable
public class MonarchApprovalTimelineLabels(
    public val label: String? = null,
    public val status: ((status: MonarchApprovalStepStatus) -> String)? = null,
    public val approvers: ((names: String) -> String)? = null,
    public val delegatedFrom: ((name: String) -> String)? = null,
    public val current: String? = null,
    public val empty: String? = null,
)
