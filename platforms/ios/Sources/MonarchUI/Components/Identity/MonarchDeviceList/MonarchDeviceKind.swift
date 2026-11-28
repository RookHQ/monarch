/// The kind of a device in a ``MonarchDeviceList``. It picks the icon.
public enum MonarchDeviceKind: String, Sendable, Hashable, CaseIterable {
    /// A desktop or laptop computer.
    case desktop
    /// A phone.
    case mobile
    /// A tablet.
    case tablet
    /// A device of an unknown kind.
    case unknown
}
