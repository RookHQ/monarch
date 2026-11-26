import SwiftUI

/// Redraws a running SLA indicator at the moments its text or state can
/// change: every whole minute before and after the due time, and the moment
/// the target becomes at risk. Without a due time there is one entry and
/// nothing is redrawn.
struct MonarchSlaIndicatorSchedule: TimelineSchedule {
    /// When the target is due, or `nil` for a target that does not follow
    /// the clock.
    let dueAt: Date?
    /// How long before the due time the target becomes at risk.
    let warnBefore: TimeInterval?

    func entries(from startDate: Date, mode: Mode) -> UnfoldFirstSequence<Date> {
        let dueAt = dueAt
        let warnBefore = warnBefore
        return sequence(first: startDate) { date in
            dueAt.map { MonarchSlaIndicatorTokens.nextTick(after: date, dueAt: $0, warnBefore: warnBefore) }
        }
    }
}
