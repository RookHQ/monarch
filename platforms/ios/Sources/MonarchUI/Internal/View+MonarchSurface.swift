import SwiftUI

/// Draws a surface appearance: fill, border and foreground style.
struct MonarchSurfaceModifier<S: InsettableShape>: ViewModifier {
    let appearance: MonarchSurfaceAppearance
    let shape: S

    @Environment(\.monarchTheme) private var theme

    func body(content: Content) -> some View {
        content
            .foregroundStyle(theme.colors[role: appearance.foreground])
            .background {
                if let background = appearance.background {
                    shape.fill(theme.colors[role: background])
                }
            }
            .overlay {
                if let border = appearance.border {
                    shape.strokeBorder(theme.colors[role: border], lineWidth: MonarchShapes.borderWidth)
                }
            }
    }
}

extension View {
    /// Applies a surface appearance to this view: the foreground style, a
    /// fill behind it and a border on top, all in `shape`. Apply it after the
    /// padding, so the surface includes the padding.
    func monarchSurface<S: InsettableShape>(_ appearance: MonarchSurfaceAppearance, in shape: S) -> some View {
        modifier(MonarchSurfaceModifier(appearance: appearance, shape: shape))
    }
}
