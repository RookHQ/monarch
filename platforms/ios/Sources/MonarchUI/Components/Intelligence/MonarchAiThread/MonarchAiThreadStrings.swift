/// Built-in strings of the AI thread, from the `MonarchAiThread` strings table.
enum MonarchAiThreadStrings {
    private static let tableName = "MonarchAiThread"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiThread.\(key)", table: tableName)
    }

    private static func counted(_ key: String, _ count: Int) -> String {
        let plural = count == 1 ? "one" : "other"
        return MonarchStrings.string("monarch.aiThread.\(key).\(plural)", table: tableName, count)
    }

    /// The name of the list.
    static var label: String { string("label") }
    /// The jump button.
    static var jumpToLatest: String { string("jumpToLatest") }
    /// The load-earlier button.
    static var loadEarlier: String { string("loadEarlier") }
    /// The load-earlier button while loading.
    static var loadingEarlier: String { string("loadingEarlier") }
    /// The thinking indicator and its announcement.
    static var thinking: String { string("thinking") }
    /// Announced when a reply finishes.
    static var responseComplete: String { string("responseComplete") }
    /// Announced when a reply is stopped.
    static var responseStopped: String { string("responseStopped") }
    /// Spoken for a code block.
    static var codeBlock: String { string("codeBlock") }
    /// Spoken for a table.
    static var table: String { string("table") }
    /// The day label for today.
    static var today: String { string("today") }
    /// The day label for yesterday.
    static var yesterday: String { string("yesterday") }
    /// The offline notice with nothing queued.
    static var offline: String { string("offline") }
    /// The slow-reply notice.
    static var slow: String { string("slow") }
    /// The VoiceOver action that stops a reply.
    static var stop: String { string("stop") }

    /// "2 new messages" (English plural rule).
    static func newMessages(_ count: Int) -> String { counted("newMessages", count) }
    /// The offline notice with messages queued.
    static func offlineQueued(_ count: Int) -> String { counted("offlineQueued", count) }
    /// The notice while queued messages are sent.
    static func sendingQueued(_ count: Int) -> String { counted("sendingQueued", count) }
    /// "Error: <message>".
    static func error(_ message: String) -> String {
        MonarchStrings.string("monarch.aiThread.error", table: tableName, message)
    }
}
