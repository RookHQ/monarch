/// Where the trigger of a ``MonarchSlashMenuQuery`` may be typed.
public enum MonarchSlashMenuTriggerPosition: Sendable, Hashable, CaseIterable {
    /// Only at the start of a line, so paths and fractions such as `1/2`
    /// never open the menu. The default.
    case lineStart
    /// Also after a space or a tab.
    case wordStart
}
