import SwiftUI

/// One absence or other entry on a person's row of a ``MonarchTeamCalendar``.
///
/// ```swift
/// MonarchTeamCalendarEntry("Annual leave", id: "1", memberID: "alex", start: monday, end: thursday)
/// MonarchTeamCalendarEntry("Doctor", id: "2", memberID: "sam", start: tuesday, portion: .firstHalf, isPending: true)
/// ```
///
/// Days are read in the calendar's time zone; any time on a day stands for
/// that day.
@_spi(Experimental) public struct MonarchTeamCalendarEntry: Identifiable, Hashable, Sendable {
    /// A stable id, passed back when the entry is tapped.
    public let id: String
    /// What it is, such as "Annual leave", or "Away" for a private type. Already localised.
    public var label: String
    /// The `id` of the member whose row it is on.
    public var memberID: String
    /// Any time on the first day.
    public var start: Date
    /// Any time on the last day, inclusive. `nil`, or a day before `start`, is `start`.
    public var end: Date?
    /// The colour of the bar. `nil` is a neutral grey.
    public var color: Color?
    /// The part of every day the entry covers.
    public var portion: MonarchTeamCalendarPortion
    /// Per-day portions that replace `portion` on those days, keyed by any time on the day.
    public var portions: [Date: MonarchTeamCalendarPortion]
    /// Waiting for approval: a faded bar, and "pending" in what VoiceOver reads.
    public var isPending: Bool

    /// An entry.
    ///
    /// - Parameters:
    ///   - label: What it is, already localised. Shown in a bar of two days or more and read by VoiceOver.
    ///   - id: A stable id, passed back when the entry is tapped.
    ///   - memberID: The `id` of the member whose row it is on.
    ///   - start: Any time on the first day.
    ///   - end: Any time on the last day, inclusive. `nil` is a one-day entry.
    ///   - color: The colour of the bar. `nil` is a neutral grey.
    ///   - portion: The part of every day the entry covers: half days are half bars.
    ///   - portions: Per-day portions that replace `portion`, keyed by any time on the day.
    ///   - isPending: Waiting for approval: a faded bar, and "pending" in what VoiceOver reads.
    public init(
        _ label: String,
        id: String,
        memberID: String,
        start: Date,
        end: Date? = nil,
        color: Color? = nil,
        portion: MonarchTeamCalendarPortion = .full,
        portions: [Date: MonarchTeamCalendarPortion] = [:],
        isPending: Bool = false
    ) {
        self.label = label
        self.id = id
        self.memberID = memberID
        self.start = start
        self.end = end
        self.color = color
        self.portion = portion
        self.portions = portions
        self.isPending = isPending
    }
}
