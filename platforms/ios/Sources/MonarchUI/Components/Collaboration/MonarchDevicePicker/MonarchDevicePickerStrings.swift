/// Built-in strings of the device picker, from the `MonarchDevicePicker` strings table.
enum MonarchDevicePickerStrings {
    private static let table = "MonarchDevicePicker"
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.devicePicker.\(key)", table: table)
    }

    /// The microphone field.
    static var microphone: String { string("microphone") }
    /// The camera field.
    static var camera: String { string("camera") }
    /// The audio output field.
    static var speaker: String { string("speaker") }
    /// The option that follows the system.
    static var systemDefault: String { string("systemDefault") }
    /// The notice before access was asked.
    static var notDetermined: String { string("notDetermined") }
    /// The button that asks for access.
    static var allowAccess: String { string("allowAccess") }
    /// The notice when access is refused.
    static var denied: String { string("denied") }
    /// The button that opens the app's settings.
    static var openSettings: String { string("openSettings") }
    /// The notice when there is nothing to choose.
    static var unavailable: String { string("unavailable") }

    /// A microphone without a name.
    static func unnamedMicrophone(_ position: String) -> String {
        MonarchStrings.string("monarch.devicePicker.unnamedMicrophone", table: table, position)
    }

    /// A camera without a name.
    static func unnamedCamera(_ position: String) -> String {
        MonarchStrings.string("monarch.devicePicker.unnamedCamera", table: table, position)
    }
}
