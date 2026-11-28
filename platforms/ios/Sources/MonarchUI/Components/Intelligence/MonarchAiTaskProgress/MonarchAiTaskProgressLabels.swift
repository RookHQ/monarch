import Foundation

/// Replacements for the built-in strings of ``MonarchAiTaskProgress``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiTaskProgressLabels: Sendable {
    /// The name of a status. Built in: "Running", "Paused", "Done", "Failed", "Cancelled".
    public var status: (@Sendable (MonarchAiTaskStatus) -> String)?
    /// The spoken name of the progress bar, from the title. Built in: "Summarising 40 documents progress".
    public var progressLabel: (@Sendable (String) -> String)?
    /// The written and spoken completion, from the percentage. Built in: "40% complete".
    public var progressValue: (@Sendable (Int) -> String)?
    /// The start time, from the relative time ("5 minutes ago"). Built in: "Started 5 minutes ago".
    public var started: (@Sendable (String) -> String)?
    /// The time left, from the estimate. Built in: "About 3 minutes left".
    public var eta: (@Sendable (TimeInterval) -> String)?
    /// The cancel button. Built in: "Cancel".
    public var cancel: String?
    /// The spoken name of the minimize button. Built in: "Minimize".
    public var minimize: String?
    /// The notify toggle. Built in: "Notify me when done".
    public var notify: String?
    /// Announced when the status changes, from the title and the status name. Built in: "Summarising 40 documents: Done".
    public var announce: (@Sendable (String, String) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        status: (@Sendable (MonarchAiTaskStatus) -> String)? = nil,
        progressLabel: (@Sendable (String) -> String)? = nil,
        progressValue: (@Sendable (Int) -> String)? = nil,
        started: (@Sendable (String) -> String)? = nil,
        eta: (@Sendable (TimeInterval) -> String)? = nil,
        cancel: String? = nil,
        minimize: String? = nil,
        notify: String? = nil,
        announce: (@Sendable (String, String) -> String)? = nil
    ) {
        self.status = status
        self.progressLabel = progressLabel
        self.progressValue = progressValue
        self.started = started
        self.eta = eta
        self.cancel = cancel
        self.minimize = minimize
        self.notify = notify
        self.announce = announce
    }
}
