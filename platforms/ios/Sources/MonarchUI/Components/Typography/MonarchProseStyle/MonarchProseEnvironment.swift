import SwiftUI

/// Carries the prose style from ``MonarchProse`` to the blocks inside it.
private struct MonarchProseStyleKey: EnvironmentKey {
    static let defaultValue = MonarchProseStyle()
}

extension EnvironmentValues {
    /// The prose style of the enclosing ``MonarchProse``, or the default.
    var monarchProseStyle: MonarchProseStyle {
        get { self[MonarchProseStyleKey.self] }
        set { self[MonarchProseStyleKey.self] = newValue }
    }
}

/// The kind of block a child of ``MonarchProse`` is, read by the layout to
/// space blocks.
struct MonarchProseBlockKey: LayoutValueKey {
    static let defaultValue = MonarchProseBlock.paragraph
}
