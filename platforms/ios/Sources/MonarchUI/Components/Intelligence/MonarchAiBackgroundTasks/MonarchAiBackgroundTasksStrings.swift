/// Built-in strings of the background tasks tray, from the `MonarchAiBackgroundTasks` strings table.
enum MonarchAiBackgroundTasksStrings {
    private static let table = "MonarchAiBackgroundTasks"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiBackgroundTasks.\(key)", table: table)
    }

    private static func string(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }

    static var title: String { string("title") }
    static var needsInputGroup: String { string("group.needsInput") }
    static var runningGroup: String { string("group.running") }
    static var finishedGroup: String { string("group.finished") }
    static var open: String { string("open") }
    static var cancel: String { string("cancel") }
    static var retry: String { string("retry") }
    static var respond: String { string("respond") }
    static var dismiss: String { string("dismiss") }
    static var clearFinished: String { string("clearFinished") }
    static var empty: String { string("empty") }

    static func runningCount(_ count: String) -> String { string("count.running", count) }
    static func queuedCount(_ count: String) -> String { string("count.queued", count) }
    static var needsInputCountOne: String { string("count.needsInput.one") }
    static func needsInputCountOther(_ count: String) -> String { string("count.needsInput.other", count) }
    static func groupHeading(_ label: String, _ count: String) -> String { string("groupHeading", label, count) }
    static func elapsed(_ duration: String) -> String { string("elapsed", duration) }
    static func ended(_ relative: String) -> String { string("ended", relative) }
    static func progressLabel(_ title: String) -> String { string("progressLabel", title) }
    static func actionLabel(_ action: String, _ title: String) -> String { string("actionLabel", action, title) }
    static func announce(_ title: String, _ status: String) -> String { string("announce", title, status) }

    static func status(_ status: MonarchAiBackgroundTaskStatus) -> String {
        string("status.\(status.rawValue)")
    }
}
