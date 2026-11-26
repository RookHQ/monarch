import SwiftUI

private struct MonarchStartsPresentedKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    /// Whether a component that opens a sheet or popover of its own starts with it open.
    ///
    /// False by default, so a picker shows its field until it is tapped. The documentation preview host and the
    /// tests set it to draw a picker open without a tap: ``MonarchCombobox``, ``MonarchEntityPicker`` and the
    /// pickers built on them (country, currency, assignee) read it once, when they appear.
    ///
    /// ```swift
    /// MonarchCountrySelect("Country", selection: $country)
    ///     .environment(\.monarchStartsPresented, true)
    /// ```
    public var monarchStartsPresented: Bool {
        get { self[MonarchStartsPresentedKey.self] }
        set { self[MonarchStartsPresentedKey.self] = newValue }
    }
}
