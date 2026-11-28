/// Replacements for the built-in strings of ``MonarchDevicePicker``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchDevicePickerLabels: Sendable {
    /// The microphone field. Built in: "Microphone".
    public var microphone: String?
    /// The camera field. Built in: "Camera".
    public var camera: String?
    /// The audio output field. Built in: "Speaker".
    public var speaker: String?
    /// The option that follows the system. Built in: "System default".
    public var systemDefault: String?
    /// A microphone without a name. Receives its position. Built in: "Microphone 2".
    public var unnamedMicrophone: (@Sendable (Int) -> String)?
    /// A camera without a name. Receives its position. Built in: "Camera 2".
    public var unnamedCamera: (@Sendable (Int) -> String)?
    /// The notice before access was asked. Built in: "Allow camera and microphone access to see device names.".
    public var notDetermined: String?
    /// The button that asks for access. Built in: "Allow access".
    public var allowAccess: String?
    /// The notice when access is refused. Built in: "Camera or microphone access is off. Turn it on in Settings, then try again.".
    public var denied: String?
    /// The button that opens the app's settings. Built in: "Open Settings".
    public var openSettings: String?
    /// The notice when there is nothing to choose. Built in: "No camera or microphone was found.".
    public var unavailable: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        microphone: String? = nil,
        camera: String? = nil,
        speaker: String? = nil,
        systemDefault: String? = nil,
        unnamedMicrophone: (@Sendable (Int) -> String)? = nil,
        unnamedCamera: (@Sendable (Int) -> String)? = nil,
        notDetermined: String? = nil,
        allowAccess: String? = nil,
        denied: String? = nil,
        openSettings: String? = nil,
        unavailable: String? = nil
    ) {
        self.microphone = microphone
        self.camera = camera
        self.speaker = speaker
        self.systemDefault = systemDefault
        self.unnamedMicrophone = unnamedMicrophone
        self.unnamedCamera = unnamedCamera
        self.notDetermined = notDetermined
        self.allowAccess = allowAccess
        self.denied = denied
        self.openSettings = openSettings
        self.unavailable = unavailable
    }
}
