import SwiftUI

/// The history and outlook of one approval: who submitted it, each step
/// with its decision, who is deciding now and what comes next.
///
/// ```swift
/// MonarchApprovalTimeline([
///     MonarchApprovalTimelineStep(id: "0", label: "Submitted", status: .submitted,
///                                 actor: MonarchApprovalPerson("Sam Lee"), date: submittedAt),
///     MonarchApprovalTimelineStep(id: "1", label: "Reporting manager", status: .approved,
///                                 actor: MonarchApprovalPerson("Ana Ruiz"), date: approvedAt,
///                                 comment: "Fine by me."),
///     MonarchApprovalTimelineStep(id: "2", label: "Finance check", status: .pending,
///                                 approvers: [MonarchApprovalPerson("Jo Park")]),
/// ])
/// ```
///
/// Read-only: the app supplies the steps from its approval engine. Times
/// are relative to `now` (the current time, refreshed every minute, when
/// `nil`) and become a date once they are more than `absoluteAfter` away.
/// The environment's calendar, time zone and locale format them.
///
/// Accessibility: the timeline is a container named "Approval timeline".
/// Each step is one element read as its label, status, actor, date and
/// time, approvers, comment and note; the step waiting for a decision adds
/// "Current step". Markers differ by shape (check, cross, dot, dash,
/// empty), so status does not rely on colour.
public struct MonarchApprovalTimeline: View {
    private let steps: [MonarchApprovalTimelineStep]
    private let now: Date?
    private let absoluteAfter: TimeInterval
    private let showsAvatars: Bool
    private let labels: MonarchApprovalTimelineLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.calendar) private var calendar
    @Environment(\.timeZone) private var timeZone

    /// An approval timeline.
    ///
    /// - Parameters:
    ///   - steps: The steps in order, from submission to the outcome.
    ///   - now: The reference time for relative times. The current time when `nil`.
    ///   - absoluteAfter: How far from `now`, in seconds, a time is shown as a date. Seven days by default.
    ///   - showsAvatars: Whether deciders have an avatar beside their name.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ steps: [MonarchApprovalTimelineStep],
        now: Date? = nil,
        absoluteAfter: TimeInterval = 7 * 86_400,
        showsAvatars: Bool = true,
        labels: MonarchApprovalTimelineLabels = MonarchApprovalTimelineLabels()
    ) {
        self.steps = steps
        self.now = now
        self.absoluteAfter = absoluteAfter
        self.showsAvatars = showsAvatars
        self.labels = labels
    }

    private var zonedCalendar: Calendar {
        var zoned = calendar
        zoned.timeZone = timeZone
        return zoned
    }

    /// The content of the view.
    public var body: some View {
        if steps.isEmpty {
            Text(labels.empty)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
                .frame(maxWidth: .infinity, alignment: .leading)
        } else if let now {
            list(now: now)
        } else {
            MonarchRelativeTimeClock(dates: steps.map(\.date)) { current in
                list(now: current)
            }
        }
    }

    private func list(now: Date) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(steps.enumerated()), id: \.element.id) { index, step in
                MonarchApprovalTimelineRow(
                    step: step,
                    now: now,
                    absoluteAfter: absoluteAfter,
                    calendar: zonedCalendar,
                    showsAvatars: showsAvatars,
                    isLast: index == steps.count - 1,
                    labels: labels
                )
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.label)
    }
}

#if DEBUG
    #Preview("MonarchApprovalTimeline") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchApprovalTimeline(
                    [
                        MonarchApprovalTimelineStep(
                            id: "0",
                            label: "Submitted",
                            status: .submitted,
                            actor: MonarchApprovalPerson("Sam Lee"),
                            date: now.addingTimeInterval(-86_400 * 2)
                        ),
                        MonarchApprovalTimelineStep(
                            id: "1",
                            label: "Reporting manager",
                            status: .approved,
                            actor: MonarchApprovalPerson("Ana Ruiz"),
                            date: now.addingTimeInterval(-3_600),
                            comment: "Fine by me."
                        ),
                        MonarchApprovalTimelineStep(
                            id: "2",
                            label: "Finance check",
                            status: .pending,
                            approvers: [MonarchApprovalPerson("Jo Park")]
                        ),
                        MonarchApprovalTimelineStep(id: "3", label: "Payout", status: .waiting),
                    ],
                    now: now
                )
                MonarchApprovalTimeline([])
            }
        }
    }
#endif
