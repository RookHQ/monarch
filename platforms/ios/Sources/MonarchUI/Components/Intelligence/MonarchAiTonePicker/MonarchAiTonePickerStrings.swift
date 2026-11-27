/// Built-in strings of the tone picker, from the `MonarchAiTonePicker` strings table.
enum MonarchAiTonePickerStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiTonePicker." + key, table: "MonarchAiTonePicker")
    }

    /// The name of the choice.
    static var label: String { string("label") }
    /// The name of the Custom option.
    static var custom: String { string("custom") }
    /// The description of the Custom option.
    static var customDetail: String { string("customDetail") }
    /// The label of the custom text field.
    static var customField: String { string("customField") }
    /// The placeholder of the custom text field.
    static var customPrompt: String { string("customPrompt") }
    /// The name of the Professional tone.
    static var professional: String { string("professional") }
    /// The description of the Professional tone.
    static var professionalDetail: String { string("professionalDetail") }
    /// The name of the Technical tone.
    static var technical: String { string("technical") }
    /// The description of the Technical tone.
    static var technicalDetail: String { string("technicalDetail") }
    /// The name of the Casual tone.
    static var casual: String { string("casual") }
    /// The description of the Casual tone.
    static var casualDetail: String { string("casualDetail") }
    /// The name of the Executive tone.
    static var executive: String { string("executive") }
    /// The description of the Executive tone.
    static var executiveDetail: String { string("executiveDetail") }
}
