/// Built-in strings of the model picker, from the `MonarchAiModelPicker` strings table.
enum MonarchAiModelPickerStrings {
    private static let table = "MonarchAiModelPicker"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiModelPicker." + key, table: table)
    }

    /// Names the button and the list.
    static var label: String { string("label") }
    /// The button's text without a selection.
    static var placeholder: String { string("placeholder") }
    /// The badge of a model on a configured key.
    static var ownKey: String { string("ownKey") }
    /// The badge of a model whose key is missing.
    static var keyNeeded: String { string("keyNeeded") }
    /// The hint of a model that leads to adding a key.
    static var addKeyHint: String { string("addKeyHint") }
    /// Read on a model that cannot be picked.
    static var unavailable: String { string("unavailable") }
    /// The button that closes the list.
    static var done: String { string("done") }
    /// What separates the parts of the details line.
    static var separator: String { string("separator") }
    /// The name of a capability.
    static func capability(_ capability: MonarchAiModelCapability) -> String {
        string("capability." + capability.rawValue)
    }
    /// The name of a speed.
    static func speed(_ speed: MonarchAiModelSpeed) -> String { string("speed." + speed.rawValue) }
    /// "Cost: medium", for a tier from 1 to 3.
    static func cost(_ tier: Int) -> String { string("cost.\(tier)") }
    /// "200K context".
    static func context(_ formatted: String) -> String {
        MonarchStrings.string("monarch.aiModelPicker.context", table: table, formatted)
    }
}
