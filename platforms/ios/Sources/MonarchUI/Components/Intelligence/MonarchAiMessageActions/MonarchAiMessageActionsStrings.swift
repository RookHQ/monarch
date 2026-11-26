/// Built-in strings of the message actions, from the `MonarchAiMessageActions` strings table.
enum MonarchAiMessageActionsStrings {
    private static let table = "MonarchAiMessageActions"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiMessageActions.\(key)", table: table)
    }

    static var toolbar: String { string("toolbar") }
    static var copy: String { string("copy") }
    static var copied: String { string("copied") }
    static var regenerate: String { string("regenerate") }
    static var edit: String { string("edit") }
    static var good: String { string("good") }
    static var bad: String { string("bad") }
    static var share: String { string("share") }
    static var readAloud: String { string("readAloud") }
    static var stopReading: String { string("stopReading") }
    static var more: String { string("more") }
}
