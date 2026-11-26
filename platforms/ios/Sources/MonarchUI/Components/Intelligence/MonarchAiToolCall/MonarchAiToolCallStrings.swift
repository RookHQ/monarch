/// Built-in strings of the tool call, from the `MonarchAiToolCall` strings table.
enum MonarchAiToolCallStrings {
    private static let table = "MonarchAiToolCall"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiToolCall.\(key)", table: table)
    }

    static var input: String { string("input") }
    static var output: String { string("output") }
    static var error: String { string("error") }
    static var tool: String { string("tool") }
    static var empty: String { string("empty") }
    static var showMore: String { string("showMore") }

    /// The name of a state.
    static func state(_ state: MonarchAiToolCallState) -> String { string("state.\(state.rawValue)") }

    /// A duration under one minute. The argument is the formatted number of seconds.
    static func seconds(_ value: String) -> String {
        MonarchStrings.string("monarch.aiToolCall.seconds", table: table, value)
    }

    /// A duration of a minute or more.
    static func minutesSeconds(_ minutes: String, _ seconds: String) -> String {
        MonarchStrings.string("monarch.aiToolCall.minutesSeconds", table: table, minutes, seconds)
    }

    /// Announced when the state changes.
    static func stateChanged(_ label: String, _ state: String) -> String {
        MonarchStrings.string("monarch.aiToolCall.stateChanged", table: table, label, state)
    }

    /// Says how much of a long value is shown.
    static func truncated(_ shown: String, _ total: String) -> String {
        MonarchStrings.string("monarch.aiToolCall.truncated", table: table, shown, total)
    }

    /// The tool line in the details: "Tool: search_documents".
    static func toolLine(_ label: String, _ name: String) -> String {
        MonarchStrings.string("monarch.aiToolCall.toolLine", table: table, label, name)
    }

    /// The error line as VoiceOver reads it: "Error: message".
    static func errorLine(_ label: String, _ message: String) -> String {
        MonarchStrings.string("monarch.aiToolCall.errorLine", table: table, label, message)
    }
}
