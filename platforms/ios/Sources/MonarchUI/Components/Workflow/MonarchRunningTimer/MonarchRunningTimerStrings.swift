/// Built-in strings of the running timer, from the `MonarchRunningTimer`
/// strings table, with the replacements of a labels value applied.
enum MonarchRunningTimerStrings {
    private static let table = "MonarchRunningTimer"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.runningTimer." + key, table: table)
    }

    private static func string(_ key: String, _ argument: String) -> String {
        MonarchStrings.string("monarch.runningTimer." + key, table: table, argument)
    }

    static func start(_ labels: MonarchRunningTimerLabels) -> String { labels.start ?? string("start") }
    static func stop(_ labels: MonarchRunningTimerLabels) -> String { labels.stop ?? string("stop") }
    static func stopAndLog(_ labels: MonarchRunningTimerLabels) -> String {
        labels.stopAndLog ?? string("stopAndLog")
    }
    static func moreActions(_ labels: MonarchRunningTimerLabels) -> String {
        labels.moreActions ?? string("moreActions")
    }
    static func discard(_ labels: MonarchRunningTimerLabels) -> String { labels.discard ?? string("discard") }
    static func discardTitle(_ labels: MonarchRunningTimerLabels) -> String {
        labels.discardTitle ?? string("discardTitle")
    }
    static func discardMessage(_ labels: MonarchRunningTimerLabels) -> String {
        labels.discardMessage ?? string("discardMessage")
    }
    static func discardConfirm(_ labels: MonarchRunningTimerLabels) -> String {
        labels.discardConfirm ?? string("discardConfirm")
    }
    static func stoppedEarlier(_ labels: MonarchRunningTimerLabels) -> String {
        labels.stoppedEarlier ?? string("stoppedEarlier")
    }
    static func stoppedEarlierTitle(_ labels: MonarchRunningTimerLabels) -> String {
        labels.stoppedEarlierTitle ?? string("stoppedEarlierTitle")
    }
    static func stoppedEarlierMessage(_ labels: MonarchRunningTimerLabels) -> String {
        labels.stoppedEarlierMessage ?? string("stoppedEarlierMessage")
    }
    static func stoppedAt(_ labels: MonarchRunningTimerLabels) -> String { labels.stoppedAt ?? string("stoppedAt") }
    static func started(_ date: String, _ labels: MonarchRunningTimerLabels) -> String {
        labels.started?(date) ?? string("started", date)
    }
    static func workDuration(_ duration: String, _ labels: MonarchRunningTimerLabels) -> String {
        labels.workDuration?(duration) ?? string("workDuration", duration)
    }
    static func save(_ labels: MonarchRunningTimerLabels) -> String { labels.save ?? string("save") }
    static func cancel(_ labels: MonarchRunningTimerLabels) -> String { labels.cancel ?? string("cancel") }
    static func stoppedAtFuture(_ labels: MonarchRunningTimerLabels) -> String {
        labels.stoppedAtFuture ?? string("stoppedAtFuture")
    }
    static func stoppedAtTooEarly(_ labels: MonarchRunningTimerLabels) -> String {
        labels.stoppedAtTooEarly ?? string("stoppedAtTooEarly")
    }
    static func failed(_ labels: MonarchRunningTimerLabels) -> String { labels.failed ?? string("failed") }
    static func running(_ labels: MonarchRunningTimerLabels) -> String { labels.running ?? string("running") }
    static func stopped(_ labels: MonarchRunningTimerLabels) -> String { labels.stopped ?? string("stopped") }
    static func defaultLabel(_ labels: MonarchRunningTimerLabels) -> String {
        labels.defaultLabel ?? string("defaultLabel")
    }
    static func longRunning(_ duration: String, _ labels: MonarchRunningTimerLabels) -> String {
        labels.longRunning?(duration) ?? string("longRunning", duration)
    }
    static func openHint(_ labels: MonarchRunningTimerLabels) -> String { labels.openHint ?? string("openHint") }
}
