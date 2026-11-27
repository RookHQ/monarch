import Foundation

/// Replacements for the built-in strings of ``MonarchRunningTimer``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchRunningTimerLabels: Sendable {
    /// The Start button of the panel. Built in: "Start timer".
    public var start: String?
    /// The Stop button of the panel. Built in: "Stop".
    public var stop: String?
    /// The name of the indicator's stop button. Built in: "Stop timer and log time".
    public var stopAndLog: String?
    /// The name of the indicator's menu button. Built in: "More timer actions".
    public var moreActions: String?
    /// The discard action. Built in: "Discard without logging".
    public var discard: String?
    /// The title of the discard confirmation. Built in: "Discard the running timer?".
    public var discardTitle: String?
    /// The message of the discard confirmation. Built in: "The tracked time will not be saved.".
    public var discardMessage: String?
    /// The confirming button of the discard confirmation. Built in: "Discard".
    public var discardConfirm: String?
    /// The action that stops at an earlier time. Built in: "I stopped earlier".
    public var stoppedEarlier: String?
    /// The title of the earlier-stop sheet. Built in: "When did you stop?".
    public var stoppedEarlierTitle: String?
    /// The explanation in the earlier-stop sheet.
    public var stoppedEarlierMessage: String?
    /// The label of the time picker. Built in: "I stopped at".
    public var stoppedAt: String?
    /// The start time line. Receives the formatted start. Built in: "Started 14 Nov 2026 at 09:00".
    public var started: (@Sendable (String) -> String)?
    /// The length of the work that will be saved. Receives "2h 05m". Built in: "That is 2h 05m of work.".
    public var workDuration: (@Sendable (String) -> String)?
    /// The save button of the sheet. Built in: "Save this time".
    public var save: String?
    /// The cancel buttons. Built in: "Cancel".
    public var cancel: String?
    /// The error for a stop time in the future.
    public var stoppedAtFuture: String?
    /// The error for a stop time less than a minute after the start.
    public var stoppedAtTooEarly: String?
    /// Shown when an action fails without a localized description.
    /// Built in: "That did not work. Try again.".
    public var failed: String?
    /// The state of a running timer, read and announced. Built in: "Timer running".
    public var running: String?
    /// The state of a stopped timer, read and announced. Built in: "Timer stopped".
    public var stopped: String?
    /// The label of the indicator when none is given. Built in: "Timer".
    public var defaultLabel: String?
    /// The warning after `warnAfter`. Receives the duration, such as "12h".
    public var longRunning: (@Sendable (String) -> String)?
    /// The hint of the indicator when it opens the timed item. Built in: "Opens what is being timed.".
    public var openHint: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        start: String? = nil,
        stop: String? = nil,
        stopAndLog: String? = nil,
        moreActions: String? = nil,
        discard: String? = nil,
        discardTitle: String? = nil,
        discardMessage: String? = nil,
        discardConfirm: String? = nil,
        stoppedEarlier: String? = nil,
        stoppedEarlierTitle: String? = nil,
        stoppedEarlierMessage: String? = nil,
        stoppedAt: String? = nil,
        started: (@Sendable (String) -> String)? = nil,
        workDuration: (@Sendable (String) -> String)? = nil,
        save: String? = nil,
        cancel: String? = nil,
        stoppedAtFuture: String? = nil,
        stoppedAtTooEarly: String? = nil,
        failed: String? = nil,
        running: String? = nil,
        stopped: String? = nil,
        defaultLabel: String? = nil,
        longRunning: (@Sendable (String) -> String)? = nil,
        openHint: String? = nil
    ) {
        self.start = start
        self.stop = stop
        self.stopAndLog = stopAndLog
        self.moreActions = moreActions
        self.discard = discard
        self.discardTitle = discardTitle
        self.discardMessage = discardMessage
        self.discardConfirm = discardConfirm
        self.stoppedEarlier = stoppedEarlier
        self.stoppedEarlierTitle = stoppedEarlierTitle
        self.stoppedEarlierMessage = stoppedEarlierMessage
        self.stoppedAt = stoppedAt
        self.started = started
        self.workDuration = workDuration
        self.save = save
        self.cancel = cancel
        self.stoppedAtFuture = stoppedAtFuture
        self.stoppedAtTooEarly = stoppedAtTooEarly
        self.failed = failed
        self.running = running
        self.stopped = stopped
        self.defaultLabel = defaultLabel
        self.longRunning = longRunning
        self.openHint = openHint
    }
}
