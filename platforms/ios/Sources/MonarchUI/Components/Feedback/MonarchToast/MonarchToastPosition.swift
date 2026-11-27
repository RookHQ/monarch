/// The edge of the screen where toasts appear.
public enum MonarchToastPosition: Sendable, Hashable, CaseIterable {
    /// Above the bottom safe area, and above the keyboard. The default.
    case bottom
    /// Below the top safe area.
    case top
}
