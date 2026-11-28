import Foundation

/// Replacements for the built-in strings of ``MonarchSlaIndicator``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchSlaIndicatorLabels: Sendable {
    /// The name of the on-track state. Built in: "On track".
    public var onTrack: String?
    /// The name of the at-risk state. Built in: "At risk".
    public var atRisk: String?
    /// The name of the breached state. Built in: "Breached".
    public var breached: String?
    /// The name of the paused state. Built in: "Paused".
    public var paused: String?
    /// The name of the met state. Built in: "Met".
    public var met: String?
    /// Shown and read when no target applies. Built in: "No SLA".
    public var notApplicable: String?
    /// Shown when no target applies and the name is not wanted. Built in: "—".
    public var notApplicableMark: String?
    /// The time left. Receives the duration. Built in: "2h 15m left".
    public var remaining: (@Sendable (String) -> String)?
    /// The time overdue. Receives the duration. Built in: "Overdue 40m".
    public var overdue: (@Sendable (String) -> String)?
    /// Writes a span of time, shown and read. Receives the seconds, always
    /// positive. Built in: "<1m", "45m", "2h 15m" or "3d 4h" in the user's
    /// locale, and the same in words for VoiceOver.
    public var duration: (@Sendable (TimeInterval) -> String)?
    /// Read after the state. Receives the formatted due date and time.
    /// Built in: "due 13 Nov 2026 at 15:00".
    public var due: (@Sendable (String) -> String)?
    /// Read when an earlier target was breached. Built in: "an earlier
    /// target was breached".
    public var earlierBreach: String?
    /// Announced when the state changes. Receives the name of the new state
    /// and the name of the target, if any. Built in: "First response: At
    /// risk", or "SLA At risk" without a target.
    public var changed: (@Sendable (String, String?) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        onTrack: String? = nil,
        atRisk: String? = nil,
        breached: String? = nil,
        paused: String? = nil,
        met: String? = nil,
        notApplicable: String? = nil,
        notApplicableMark: String? = nil,
        remaining: (@Sendable (String) -> String)? = nil,
        overdue: (@Sendable (String) -> String)? = nil,
        duration: (@Sendable (TimeInterval) -> String)? = nil,
        due: (@Sendable (String) -> String)? = nil,
        earlierBreach: String? = nil,
        changed: (@Sendable (String, String?) -> String)? = nil
    ) {
        self.onTrack = onTrack
        self.atRisk = atRisk
        self.breached = breached
        self.paused = paused
        self.met = met
        self.notApplicable = notApplicable
        self.notApplicableMark = notApplicableMark
        self.remaining = remaining
        self.overdue = overdue
        self.duration = duration
        self.due = due
        self.earlierBreach = earlierBreach
        self.changed = changed
    }
}
