package com.rookhq.monarch.components.workflow.approvaltimeline

import android.icu.text.DateFormat
import com.rookhq.monarch.internal.MonarchRelativeTime
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** The pure text rules of MonarchApprovalTimeline: which lines a step shows, its times and what TalkBack reads. */
internal object MonarchApprovalTimelineText {
    /** The skeleton of an absolute date and time: "Mar 4, 2026, 3:00 PM". */
    const val DateTimeSkeleton: String = "yMMMdjmm"

    private const val Separator = ", "
    private const val NameSeparator = ", "

    /** Whether the step has a decision or a submission to show with its actor and time. */
    fun decided(status: MonarchApprovalStepStatus): Boolean = when (status) {
        MonarchApprovalStepStatus.Submitted,
        MonarchApprovalStepStatus.Approved,
        MonarchApprovalStepStatus.Rejected,
        MonarchApprovalStepStatus.Cancelled,
        -> true
        else -> false
    }

    /** Whether the step still waits for someone: approvers are shown. */
    fun undecided(status: MonarchApprovalStepStatus): Boolean =
        status == MonarchApprovalStepStatus.Pending || status == MonarchApprovalStepStatus.Waiting

    /** The approvers line with the delegation in brackets, or `null` when it is not shown. */
    fun approversLine(
        step: MonarchApprovalTimelineStep,
        approvers: (String) -> String,
        delegatedFrom: (String) -> String,
    ): String? {
        if (!undecided(step.status) || step.approvers.isEmpty()) return null
        val line = approvers(step.approvers.joinToString(NameSeparator) { it.name })
        return step.delegatedFrom?.let { "$line (${delegatedFrom(it.name)})" } ?: line
    }

    /** The delegation line of a decided step, or `null`. */
    fun delegationLine(step: MonarchApprovalTimelineStep, delegatedFrom: (String) -> String): String? =
        step.delegatedFrom?.takeIf { decided(step.status) }?.let { delegatedFrom(it.name) }

    /** The date and time of [time] in [zone], which TalkBack reads: "Mar 4, 2026, 3:00 PM". */
    fun absoluteTime(time: Long, zone: TimeZone, locale: Locale): String {
        val format = DateFormat.getInstanceForSkeleton(DateTimeSkeleton, locale)
        format.timeZone = android.icu.util.TimeZone.getTimeZone(zone.id)
        return format.format(Date(time))
    }

    /**
     * The shown time: relative within [absoluteAfterMillis] of [now] by the shared Monarch rule (the web rule),
     * the date and time beyond.
     */
    fun timeText(time: Long, now: Long, absoluteAfterMillis: Long, zone: TimeZone, locale: Locale): String =
        if (MonarchRelativeTime.showsAbsolute(time, now, absoluteAfterMillis)) {
            absoluteTime(time, zone, locale)
        } else {
            MonarchRelativeTime.format(time, now, locale)
        }

    /**
     * What TalkBack reads for a step: the label, the status, [current] on the pending step, the actor and absolute
     * time on decided steps, the approvers, the delegation, the comment and the note.
     */
    fun spoken(
        step: MonarchApprovalTimelineStep,
        status: String,
        current: String,
        time: String?,
        lines: List<String?>,
    ): String {
        val parts = mutableListOf(step.label, status)
        if (step.status == MonarchApprovalStepStatus.Pending) parts += current
        if (decided(step.status)) {
            step.actor?.let { parts += it.name }
            time?.let { parts += it }
        }
        parts += lines.filterNotNull()
        step.comment?.takeIf { it.isNotEmpty() }?.let { parts += it }
        step.note?.takeIf { it.isNotEmpty() }?.let { parts += it }
        return parts.joinToString(Separator)
    }
}
