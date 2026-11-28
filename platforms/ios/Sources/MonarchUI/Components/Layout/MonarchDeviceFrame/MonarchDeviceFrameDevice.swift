/// The device a ``MonarchDeviceFrame`` draws.
public enum MonarchDeviceFrameDevice: Sendable, Hashable, CaseIterable {
    /// A phone: rounded, with a camera island. 390 by 844 points by default.
    case phone
    /// A tablet: a wider bezel and no island. 820 by 1180 points by default.
    case tablet
}
