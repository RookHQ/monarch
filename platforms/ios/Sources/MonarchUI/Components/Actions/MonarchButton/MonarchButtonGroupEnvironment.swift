import SwiftUI

/// Whether a ``MonarchButton`` leaves its style to the container.
///
/// A container that draws its buttons itself, such as ``MonarchButtonGroup``
/// with its joined segments, sets this to `true` and applies its own
/// `ButtonStyle`. A ``MonarchButton`` inside it then applies no style of its
/// own, so the container's style reaches the standard `Button` within.
private struct MonarchButtonDefersStyleKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    /// Whether ``MonarchButton`` leaves its style to the container.
    var monarchButtonDefersStyle: Bool {
        get { self[MonarchButtonDefersStyleKey.self] }
        set { self[MonarchButtonDefersStyleKey.self] = newValue }
    }
}

extension View {
    /// Makes every ``MonarchButton`` inside use the `ButtonStyle` of the
    /// environment instead of its own. For containers that style their
    /// buttons themselves.
    func monarchButtonDefersStyle(_ defers: Bool = true) -> some View {
        environment(\.monarchButtonDefersStyle, defers)
    }
}
