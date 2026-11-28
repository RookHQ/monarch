import SwiftUI

/// The fill of the rows of a `List` or `Form` that sits on the Monarch canvas.
///
/// SwiftUI fills a row with the system background (pure black in Dark Mode)
/// even when the list's own background is hidden. These modifiers replace it:
/// rows of a plain list show the canvas, rows of a grouped list or form sit on
/// the surface colour, as cards do.
private struct MonarchListRowFill: ViewModifier {
    enum Kind { case canvas, surface }
    let kind: Kind
    @Environment(\.monarchTheme) private var theme

    func body(content: Content) -> some View {
        switch kind {
        case .canvas: content.listRowBackground(Color.clear)
        case .surface: content.listRowBackground(theme.colors.bgSurface)
        }
    }
}

extension View {
    /// The rows of a plain list show the Monarch canvas, not the system fill.
    /// Apply it to the rows, or to a `Group` around them.
    func monarchRowsOnCanvas() -> some View {
        modifier(MonarchListRowFill(kind: .canvas))
    }

    /// The rows of a grouped list or form sit on the Monarch surface colour.
    func monarchRowsOnSurface() -> some View {
        modifier(MonarchListRowFill(kind: .surface))
    }
}
