/// Built-in strings of the AI safety notice, from the `MonarchAiSafetyNotice` strings table.
enum MonarchAiSafetyNoticeStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiSafetyNotice." + key, table: "MonarchAiSafetyNotice")
    }

    /// The title of a kind.
    static func title(_ kind: MonarchAiSafetyNoticeKind) -> String { string("title." + kind.rawValue) }
    /// The explanation of a kind.
    static func message(_ kind: MonarchAiSafetyNoticeKind) -> String { string("message." + kind.rawValue) }
    /// The label before the reason.
    static var reason: String { string("reason") }
    /// The title of the edit prompt button.
    static var editPrompt: String { string("editPrompt") }
    /// The title of the guidelines link.
    static var learnMore: String { string("learnMore") }
}
