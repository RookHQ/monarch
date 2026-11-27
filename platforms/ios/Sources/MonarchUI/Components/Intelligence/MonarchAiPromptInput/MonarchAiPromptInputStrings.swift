/// Built-in strings of the prompt input, from the `MonarchAiPromptInput` strings table.
enum MonarchAiPromptInputStrings {
    private static let table = "MonarchAiPromptInput"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiPromptInput.\(key)", table: table)
    }

    private static func string(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }

    /// Shown in the empty prompt.
    static var placeholder: String { string("placeholder") }
    /// The send button.
    static var send: String { string("send") }
    /// The stop button.
    static var stop: String { string("stop") }
    /// The attach button.
    static var attach: String { string("attach") }
    /// Shown while files are dragged over the prompt.
    static var dropHint: String { string("dropHint") }
    /// The visible counter, "120 / 4000".
    static func counter(_ count: Int, _ max: Int) -> String { string("counter", count, max) }
    /// The spoken counter, "120 of 4000 characters".
    static func counterLabel(_ count: Int, _ max: Int) -> String { string("counterLabel", count, max) }
    /// The characters left near the limit.
    static func counterWarning(_ remaining: Int) -> String {
        remaining == 1 ? string("counterWarningOne") : string("counterWarning", remaining)
    }
    /// The characters over the limit.
    static func counterOver(_ over: Int) -> String {
        over == 1 ? string("counterOverOne") : string("counterOver", over)
    }
    /// The estimated tokens, "≈ 120 tokens".
    static func tokens(_ count: Int) -> String { string("tokens", count) }
}
