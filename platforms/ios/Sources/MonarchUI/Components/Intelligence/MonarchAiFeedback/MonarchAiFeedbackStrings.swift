/// Built-in strings of the feedback control, from the `MonarchAiFeedback` strings table.
enum MonarchAiFeedbackStrings {
    private static let table = "MonarchAiFeedback"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiFeedback." + key, table: table)
    }

    /// Names the thumbs and the sheet.
    static var group: String { string("group") }
    /// The thumbs up.
    static var good: String { string("good") }
    /// The thumbs down.
    static var bad: String { string("bad") }
    /// The question after a thumbs down.
    static var reasonsDown: String { string("reasonsDown") }
    /// The question after a thumbs up.
    static var reasonsUp: String { string("reasonsUp") }
    /// The label of the comment.
    static var comment: String { string("comment") }
    /// The placeholder of the comment.
    static var commentPlaceholder: String { string("commentPlaceholder") }
    /// The send button.
    static var submit: String { string("submit") }
    /// The cancel button.
    static var cancel: String { string("cancel") }
    /// Shown after sending.
    static var thanks: String { string("thanks") }
    /// Shown when sending failed.
    static var error: String { string("error") }

    /// The label of a default reason after a thumbs down.
    static func reason(_ id: String) -> String { string("reason." + id) }
}
