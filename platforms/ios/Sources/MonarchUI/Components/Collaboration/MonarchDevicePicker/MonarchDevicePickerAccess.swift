/// Whether the app may use the camera and microphone, for the notice of a
/// ``MonarchDevicePicker``.
public enum MonarchDevicePickerAccess: Sendable, Hashable, CaseIterable {
    /// Access is allowed: no notice.
    case granted
    /// The person has not been asked yet: an info notice with Allow access.
    case notDetermined
    /// Access is refused or restricted: a warning that points to Settings.
    case denied
    /// There is no camera or microphone to choose: a neutral notice.
    case unavailable
}
