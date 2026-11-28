/// The motion choice of a ``MonarchAppearancePanel``.
public enum MonarchAppearanceMotion: Sendable, Hashable, CaseIterable {
    /// Follow the Reduce Motion accessibility setting. The default.
    case system
    /// Always reduce animation and movement, whatever the device setting.
    case reduce
}
