import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchApprovalTimeline``.
enum MonarchApprovalTimelineTokens {
    /// The gap between the marker column and the step.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space3
    /// The space after each step but the last.
    static let stepSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between the lines of one step.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between items of the meta line.
    static let metaSpacing: CGFloat = MonarchTokens.Space.space1
    /// The side of the marker before Dynamic Type.
    static let markerSide: CGFloat = MonarchTokens.Space.space5
    /// The inset of the glyph inside the marker, as a part of its side.
    static let glyphInsetRatio: CGFloat = 0.25
    /// The width of the marker's border.
    static let markerBorderWidth: CGFloat = MonarchTokens.Space.px * 1.5
    /// The dash of a skipped or cancelled marker's border.
    static let markerDash: [CGFloat] = [MonarchTokens.Space.space0_5, MonarchTokens.Space.space0_5]
    /// The width of the rail between markers.
    static let railWidth: CGFloat = MonarchTokens.Space.px * 2
    /// The gap between a marker and the rail below it.
    static let railGap: CGFloat = MonarchTokens.Space.space1
    /// Padding inside a comment.
    static let commentHorizontalPadding: CGFloat = MonarchTokens.Space.space2_5
    /// Padding above and below a comment's text.
    static let commentVerticalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The corner radius of a comment.
    static let commentRadius: CGFloat = MonarchTokens.Radius.md
    /// The default distance after which times are absolute: seven days.
    static let defaultAbsoluteAfter: TimeInterval = 7 * 86_400
    /// The separator between parts of what VoiceOver reads.
    static let separator = ", "
    /// The separator between approver names.
    static let nameSeparator = ", "
    /// The dot between the actor and the time.
    static let metaDot = "·"

    /// The colours and glyph of a step's marker.
    struct Marker: Equatable {
        /// The fill.
        let fill: MonarchColorRole
        /// The border.
        let border: MonarchColorRole
        /// The glyph colour.
        let symbol: MonarchColorRole
        /// The SF Symbol, or `nil` for an empty circle.
        let systemImage: String?
        /// Whether the border is dashed.
        let isDashed: Bool
    }

    /// The marker of a status: a check, a cross, a dot, a dash or an empty circle,
    /// so the status is told by shape as well as colour.
    static func marker(_ status: MonarchApprovalStepStatus) -> Marker {
        switch status {
        case .submitted, .approved:
            Marker(
                fill: \.successSolid,
                border: \.successSolid,
                symbol: \.successOnSolid,
                systemImage: "checkmark",
                isDashed: false
            )
        case .rejected:
            Marker(
                fill: \.dangerSolid,
                border: \.dangerSolid,
                symbol: \.dangerOnSolid,
                systemImage: "xmark",
                isDashed: false
            )
        case .pending:
            Marker(
                fill: \.warningSubtle,
                border: \.warningSolid,
                symbol: \.warningStrong,
                systemImage: "circle.fill",
                isDashed: false
            )
        case .waiting:
            Marker(fill: \.bgSurface, border: \.borderStrong, symbol: \.textMuted, systemImage: nil, isDashed: false)
        case .skipped:
            Marker(fill: \.bgSurface, border: \.borderStrong, symbol: \.textMuted, systemImage: "minus", isDashed: true)
        case .cancelled:
            Marker(fill: \.bgSurface, border: \.borderStrong, symbol: \.textMuted, systemImage: "xmark", isDashed: true)
        }
    }

    /// The rail below a step: the success border after a completed step, the subtle border otherwise.
    static func railRole(_ status: MonarchApprovalStepStatus) -> MonarchColorRole {
        status == .approved || status == .submitted ? \.successBorder : \.borderSubtle
    }

    /// The colour of the status text.
    static func statusRole(_ status: MonarchApprovalStepStatus) -> MonarchColorRole {
        switch status {
        case .rejected: \.dangerStrong
        case .pending: \.warningStrong
        default: \.textMuted
        }
    }

    /// Whether the status text is emphasised: rejected and pending steps.
    static func isStatusEmphasised(_ status: MonarchApprovalStepStatus) -> Bool {
        status == .rejected || status == .pending
    }

    /// The colour of the step's label: muted for steps that have not happened.
    static func labelRole(_ status: MonarchApprovalStepStatus) -> MonarchColorRole {
        status == .waiting || status == .skipped ? \.textMuted : \.textDefault
    }

    /// Whether the step has a decision or a submission to show with its actor and time.
    static func isDecided(_ status: MonarchApprovalStepStatus) -> Bool {
        switch status {
        case .submitted, .approved, .rejected, .cancelled: true
        case .pending, .waiting, .skipped: false
        }
    }

    /// Whether the step still waits for someone: approvers are shown.
    static func isUndecided(_ status: MonarchApprovalStepStatus) -> Bool {
        status == .pending || status == .waiting
    }

    /// The approvers line, with the delegation in brackets, or `nil` when it is not shown.
    static func approversLine(_ step: MonarchApprovalTimelineStep, labels: MonarchApprovalTimelineLabels) -> String? {
        guard isUndecided(step.status), !step.approvers.isEmpty else { return nil }
        let names = step.approvers.map(\.name).joined(separator: nameSeparator)
        var line = labels.approvers(names)
        if let delegatedFrom = step.delegatedFrom {
            line += " (\(labels.delegatedFrom(delegatedFrom.name)))"
        }
        return line
    }

    /// The delegation line of a decided step, or `nil`.
    static func delegationLine(_ step: MonarchApprovalTimelineStep, labels: MonarchApprovalTimelineLabels) -> String? {
        guard isDecided(step.status), let delegatedFrom = step.delegatedFrom else { return nil }
        return labels.delegatedFrom(delegatedFrom.name)
    }

    /// The time of a step: relative within `absoluteAfter` of `now`, the date and time beyond.
    static func timeText(
        _ date: Date,
        now: Date,
        absoluteAfter: TimeInterval,
        calendar: Calendar,
        locale: Locale
    ) -> String {
        if abs(now.timeIntervalSince(date)) > absoluteAfter {
            return absoluteTime(date, calendar: calendar, locale: locale)
        }
        return MonarchRelativeTime.string(date, now: now, locale: locale, calendar: calendar, allowsFuture: true)
    }

    /// The date and time, which VoiceOver reads.
    static func absoluteTime(_ date: Date, calendar: Calendar, locale: Locale) -> String {
        date.formatted(
            Date.FormatStyle(
                date: .abbreviated,
                time: .omitted,
                locale: locale,
                calendar: calendar,
                timeZone: calendar.timeZone
            ).monarchTime()
        )
    }

    /// What VoiceOver reads for a step: label, status, "Current step", actor,
    /// absolute time, approvers, delegation, comment and note.
    static func accessibilityLabel(
        _ step: MonarchApprovalTimelineStep,
        time: String?,
        labels: MonarchApprovalTimelineLabels
    ) -> String {
        var parts = [step.label, labels.status(step.status)]
        if step.status == .pending { parts.append(labels.current) }
        if isDecided(step.status) {
            if let actor = step.actor { parts.append(actor.name) }
            if let time { parts.append(time) }
        }
        if let approvers = approversLine(step, labels: labels) { parts.append(approvers) }
        if let delegation = delegationLine(step, labels: labels) { parts.append(delegation) }
        if let comment = step.comment, !comment.isEmpty { parts.append(comment) }
        if let note = step.note, !note.isEmpty { parts.append(note) }
        return parts.joined(separator: separator)
    }
}
