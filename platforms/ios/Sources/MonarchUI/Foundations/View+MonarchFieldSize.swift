import SwiftUI

private struct MonarchFieldSizeKey: EnvironmentKey {
    static let defaultValue = MonarchFieldSize.medium
}

extension EnvironmentValues {
    /// The size step of the Monarch fields below this view.
    var monarchFieldSize: MonarchFieldSize {
        get { self[MonarchFieldSizeKey.self] }
        set { self[MonarchFieldSizeKey.self] = newValue }
    }
}

extension View {
    /// Sets the height step of every Monarch field below this view: 40, 44
    /// (the default) or 52 points. The touch target stays 44 points.
    ///
    /// ```swift
    /// MonarchTextField("Search", text: $query)
    ///     .monarchFieldSize(.large)
    /// ```
    public func monarchFieldSize(_ size: MonarchFieldSize) -> some View {
        environment(\.monarchFieldSize, size)
    }
}

private struct MonarchHelperAboveKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    var monarchHelperAbove: Bool {
        get { self[MonarchHelperAboveKey.self] }
        set { self[MonarchHelperAboveKey.self] = newValue }
    }
}

extension View {
    /// Shows the helper text of the fields inside between the label and the control, as the web forms do, instead
    /// of under the control. An error still shows under the control.
    ///
    /// ```swift
    /// VStack { MonarchTextField("Username", text: $name, helper: "Lowercase letters only.") }
    ///     .monarchHelperAbove()
    /// ```
    public func monarchHelperAbove(_ isAbove: Bool = true) -> some View {
        environment(\.monarchHelperAbove, isAbove)
    }
}
