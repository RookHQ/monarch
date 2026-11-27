/// Built-in strings of the task progress card, from the `MonarchAiTaskProgress` strings table.
enum MonarchAiTaskProgressStrings {
    private static let table = "MonarchAiTaskProgress"

    /// The name of a status.
    static func status(_ status: MonarchAiTaskStatus) -> String {
        MonarchStrings.string("monarch.aiTaskProgress.status.\(status.rawValue)", table: table)
    }

    /// The spoken name of the progress bar.
    static func progressLabel(_ title: String) -> String {
        MonarchStrings.string("monarch.aiTaskProgress.progressLabel", table: table, title)
    }

    /// The completion.
    static func progressValue(_ percent: Int) -> String {
        MonarchStrings.string("monarch.aiTaskProgress.progressValue", table: table, percent)
    }

    /// The start time.
    static func started(_ relative: String) -> String {
        MonarchStrings.string("monarch.aiTaskProgress.started", table: table, relative)
    }

    /// Less than a minute left.
    static var etaUnderMinute: String { MonarchStrings.string("monarch.aiTaskProgress.eta.underMinute", table: table) }

    /// About one minute left.
    static var etaOneMinute: String { MonarchStrings.string("monarch.aiTaskProgress.eta.oneMinute", table: table) }

    /// About some minutes left.
    static func etaMinutes(_ minutes: Int) -> String {
        MonarchStrings.string("monarch.aiTaskProgress.eta.minutes", table: table, minutes)
    }

    /// About some hours left, already formatted.
    static func etaHours(_ hours: String) -> String {
        MonarchStrings.string("monarch.aiTaskProgress.eta.hours", table: table, hours)
    }

    /// The cancel button.
    static var cancel: String { MonarchStrings.string("monarch.aiTaskProgress.cancel", table: table) }

    /// The minimize button.
    static var minimize: String { MonarchStrings.string("monarch.aiTaskProgress.minimize", table: table) }

    /// The notify toggle.
    static var notify: String { MonarchStrings.string("monarch.aiTaskProgress.notify", table: table) }

    /// Announced when the status changes.
    static func announce(_ title: String, status: String) -> String {
        MonarchStrings.string("monarch.aiTaskProgress.announce", table: table, title, status)
    }
}
