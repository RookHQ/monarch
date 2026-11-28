/// Built-in strings of the colour picker, from the `MonarchColorPicker` strings table.
enum MonarchColorPickerStrings {
    /// The title of the row that opens the system colour picker.
    static var custom: String { MonarchStrings.string("monarch.colorPicker.custom", table: "MonarchColorPicker") }
    /// Shown in place of the hex value while no colour is selected.
    static var empty: String { MonarchStrings.string("monarch.colorPicker.empty", table: "MonarchColorPicker") }

    /// The line under the custom row: "Selected: #2563eb".
    static func selected(_ value: String) -> String {
        MonarchStrings.string("monarch.colorPicker.selected", table: "MonarchColorPicker", value)
    }

    /// The spoken name of a swatch.
    static func swatchLabel(name: String, hex: String) -> String {
        MonarchStrings.string("monarch.colorPicker.swatchLabel", table: "MonarchColorPicker", name, hex)
    }

    /// The name of a default swatch, by its key: `slate`, `red` and so on.
    static func swatchName(_ key: String) -> String {
        MonarchStrings.string("monarch.colorPicker.swatch.\(key)", table: "MonarchColorPicker")
    }
}
