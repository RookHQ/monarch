import Foundation

/// Replacements for the built-in strings of ``MonarchAiBackgroundTasks`` and
/// ``MonarchAiBackgroundTasksButton``. A property that is `nil` keeps the
/// built-in English string.
@_spi(Experimental) public struct MonarchAiBackgroundTasksLabels: Sendable {
    /// The button text and the panel's navigation title. Built in: "Background tasks".
    public var title: String?
    /// The button's count of running tasks. Built in: "2 running".
    public var runningCount: (@Sendable (Int) -> String)?
    /// The button's count when nothing runs but tasks wait to start. Built in: "1 queued".
    public var queuedCount: (@Sendable (Int) -> String)?
    /// The button's count of tasks waiting on the person. Built in: "1 needs you", "2 need you".
    public var needsInputCount: (@Sendable (Int) -> String)?
    /// The heading of tasks that wait on the person. Built in: "Needs you".
    public var needsInputGroup: String?
    /// The heading of running and queued tasks. Built in: "Running".
    public var runningGroup: String?
    /// The heading of finished tasks. Built in: "Finished".
    public var finishedGroup: String?
    /// A heading with its count. Built in: "Running (2)".
    public var groupHeading: (@Sendable (String, Int) -> String)?
    /// The name of a status. Built in: "Queued", "Running", "Needs your input", "Done",
    /// "Failed", "Cancelled".
    public var status: (@Sendable (MonarchAiBackgroundTaskStatus) -> String)?
    /// The elapsed time of a running task, from the formatted duration. Built in: "Running for 3 min".
    public var elapsed: (@Sendable (String) -> String)?
    /// The end of a finished task, from a relative time. Built in: "Ended 5 minutes ago".
    public var ended: (@Sendable (String) -> String)?
    /// The name of a task's progress bar, from its title. Built in: "… progress".
    public var progressLabel: (@Sendable (String) -> String)?
    /// The Open button. Built in: "Open".
    public var open: String?
    /// The Cancel button. Built in: "Cancel".
    public var cancel: String?
    /// The Retry button. Built in: "Retry".
    public var retry: String?
    /// The Respond button. Built in: "Respond".
    public var respond: String?
    /// The Dismiss button. Built in: "Dismiss".
    public var dismiss: String?
    /// The button that removes every finished task. Built in: "Clear finished".
    public var clearFinished: String?
    /// The spoken name of a row action, from the action and the task title. Built in: "Open: …".
    public var actionLabel: (@Sendable (String, String) -> String)?
    /// The text when there are no tasks. Built in: "No background tasks".
    public var empty: String?
    /// The announcement of a task that succeeded, failed or needs input, from the title and
    /// the status name. Built in: "…: Done".
    public var announce: (@Sendable (String, String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional; each matches the
    /// property of the same name.
    public init(
        title: String? = nil,
        runningCount: (@Sendable (Int) -> String)? = nil,
        queuedCount: (@Sendable (Int) -> String)? = nil,
        needsInputCount: (@Sendable (Int) -> String)? = nil,
        needsInputGroup: String? = nil,
        runningGroup: String? = nil,
        finishedGroup: String? = nil,
        groupHeading: (@Sendable (String, Int) -> String)? = nil,
        status: (@Sendable (MonarchAiBackgroundTaskStatus) -> String)? = nil,
        elapsed: (@Sendable (String) -> String)? = nil,
        ended: (@Sendable (String) -> String)? = nil,
        progressLabel: (@Sendable (String) -> String)? = nil,
        open: String? = nil,
        cancel: String? = nil,
        retry: String? = nil,
        respond: String? = nil,
        dismiss: String? = nil,
        clearFinished: String? = nil,
        actionLabel: (@Sendable (String, String) -> String)? = nil,
        empty: String? = nil,
        announce: (@Sendable (String, String) -> String)? = nil
    ) {
        self.title = title
        self.runningCount = runningCount
        self.queuedCount = queuedCount
        self.needsInputCount = needsInputCount
        self.needsInputGroup = needsInputGroup
        self.runningGroup = runningGroup
        self.finishedGroup = finishedGroup
        self.groupHeading = groupHeading
        self.status = status
        self.elapsed = elapsed
        self.ended = ended
        self.progressLabel = progressLabel
        self.open = open
        self.cancel = cancel
        self.retry = retry
        self.respond = respond
        self.dismiss = dismiss
        self.clearFinished = clearFinished
        self.actionLabel = actionLabel
        self.empty = empty
        self.announce = announce
    }
}
