import SwiftUI

private struct MonarchThemeKey: EnvironmentKey {
    static let defaultValue = MonarchTheme.light
}

extension EnvironmentValues {
    /// The Monarch theme for this part of the view tree.
    ///
    /// The default is the light theme, so a component placed outside
    /// `.monarchTheme()` still renders.
    public var monarchTheme: MonarchTheme {
        get { self[MonarchThemeKey.self] }
        set { self[MonarchThemeKey.self] = newValue }
    }
}
