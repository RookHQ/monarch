import SwiftUI

/// Redraws a running timer once a second, on the second of its start. A
/// stopped timer, or one with a fixed reference time, has one entry and is
/// not redrawn.
struct MonarchRunningTimerSchedule: TimelineSchedule {
    /// When the timer started, or `nil` when the clock does not tick.
    let startedAt: Date?

    func entries(from startDate: Date, mode: Mode) -> UnfoldFirstSequence<Date> {
        let startedAt = startedAt
        return sequence(first: startDate) { date in
            startedAt.map { start in
                let elapsed = date.timeIntervalSince(start)
                let next = start.addingTimeInterval(elapsed.rounded(.down) + MonarchRunningTimerTokens.tick)
                return next > date ? next : date.addingTimeInterval(MonarchRunningTimerTokens.tick)
            }
        }
    }
}
