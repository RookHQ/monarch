/// Built-in strings of the keyboard key, from the `MonarchKbd` strings
/// table: the spoken names of the keys that are written as glyphs.
enum MonarchKbdStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.kbd." + key, table: "MonarchKbd")
    }

    /// The Command key.
    static var command: String { string("command") }
    /// The Shift key.
    static var shift: String { string("shift") }
    /// The Option key.
    static var option: String { string("option") }
    /// The Control key.
    static var control: String { string("control") }
    /// The Return key.
    static var `return`: String { string("return") }
    /// The Escape key.
    static var escape: String { string("escape") }
    /// The Delete key.
    static var delete: String { string("delete") }
    /// The Tab key.
    static var tab: String { string("tab") }
    /// The Space bar.
    static var space: String { string("space") }
    /// The Caps Lock key.
    static var capsLock: String { string("capsLock") }
    /// The Up Arrow key.
    static var upArrow: String { string("upArrow") }
    /// The Down Arrow key.
    static var downArrow: String { string("downArrow") }
    /// The Left Arrow key.
    static var leftArrow: String { string("leftArrow") }
    /// The Right Arrow key.
    static var rightArrow: String { string("rightArrow") }
}
