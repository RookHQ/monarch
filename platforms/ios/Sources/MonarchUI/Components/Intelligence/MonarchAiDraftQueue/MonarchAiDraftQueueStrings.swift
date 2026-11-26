/// Built-in strings of the AI draft queue, from the `MonarchAiDraftQueue` strings table.
enum MonarchAiDraftQueueStrings {
    private static let table = "MonarchAiDraftQueue"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiDraftQueue." + key, table: table)
    }

    /// The spoken name of the queue.
    static var label: String { string("label") }
    /// The progress line.
    static func progress(_ reviewed: Int, _ total: Int) -> String {
        MonarchStrings.string("monarch.aiDraftQueue.progress", table: table, reviewed, total)
    }
    /// The empty title.
    static var empty: String { string("empty") }
    /// The name of a status.
    static func status(_ status: MonarchAiDraftStatus) -> String { string("status." + status.rawValue) }
    /// The writing placeholder.
    static var writing: String { string("writing") }
    /// The AI draft prefix.
    static var aiDraft: String { string("aiDraft") }
    /// The context heading.
    static var context: String { string("context") }
    /// The approve button.
    static var approve: String { string("approve") }
    /// The send button.
    static var send: String { string("send") }
    /// The edit button.
    static var edit: String { string("edit") }
    /// The regenerate button.
    static var regenerate: String { string("regenerate") }
    /// The skip button.
    static var skip: String { string("skip") }
    /// The save button.
    static var save: String { string("save") }
    /// The cancel button.
    static var cancel: String { string("cancel") }
    /// The editor name.
    static func editField(_ title: String) -> String {
        MonarchStrings.string("monarch.aiDraftQueue.editField", table: table, title)
    }
    /// The announcement of a status change: "New draft ready: …" when a
    /// draft is ready again, otherwise the status name and the title.
    static func announce(_ status: MonarchAiDraftStatus, title: String, statusName: String) -> String {
        if status == .draft {
            return MonarchStrings.string("monarch.aiDraftQueue.announceReady", table: table, title)
        }
        return MonarchStrings.string("monarch.aiDraftQueue.announce", table: table, statusName, title)
    }
}
