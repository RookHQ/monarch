/// The visual weight of a button.
///
/// `success`, `warning` and `dangerSubtle` were added after the first four:
/// a `switch` over `MonarchButtonVariant` in an app needs a case for each,
/// or a `default` (see the changelog).
public enum MonarchButtonVariant: Sendable, Hashable, CaseIterable {
    /// The main action of a screen. Filled with the accent.
    case primary
    /// Any other action. A bordered surface.
    case secondary
    /// A low-emphasis action. No fill until pressed.
    case ghost
    /// A destructive action. Filled with the danger colour.
    case danger
    /// A destructive action that should not dominate the screen, such as
    /// Remove in a list row. A tinted danger surface with a border.
    case dangerSubtle
    /// A confirming action, such as Approve. Filled with the success colour.
    case success
    /// An action that needs care but is not destructive, such as Override.
    /// Filled with the warning colour.
    case warning
}
