/// Built-in strings of ``MonarchAiAgentPicker``, from the `MonarchAiAgentPicker` strings table.
enum MonarchAiAgentPickerStrings {
    private static let table = "MonarchAiAgentPicker"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiAgentPicker.\(key)", table: table)
    }

    /// The spoken name of the group.
    static var label: String { string("label") }
    /// There are no agents.
    static var empty: String { string("empty") }
    /// The tag on an agent that cannot be chosen.
    static var unavailable: String { string("unavailable") }
}
