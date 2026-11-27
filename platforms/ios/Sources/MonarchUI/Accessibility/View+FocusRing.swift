import SwiftUI

/// Strokes the theme's focus ring outside a shape.
struct MonarchFocusRingModifier<S: InsettableShape>: ViewModifier {
    let isVisible: Bool
    let shape: S

    @Environment(\.monarchTheme) private var theme

    func body(content: Content) -> some View {
        content.overlay {
            if isVisible {
                shape
                    .inset(by: -(MonarchControlMetrics.focusRingOffset + theme.focusRingWidth / 2))
                    .stroke(theme.colors.focusRing, lineWidth: theme.focusRingWidth)
                    .allowsHitTesting(false)
                    .accessibilityHidden(true)
            }
        }
    }
}

extension View {
    /// Draws the theme's focus ring around this view while `isVisible` is true.
    ///
    /// The ring uses the theme's focus colour and width (wider in the
    /// high-contrast themes) and sits 2 points outside `shape`. It does not
    /// change layout. Drive it from `@FocusState`.
    public func monarchFocusRing<S: InsettableShape>(_ isVisible: Bool, in shape: S) -> some View {
        modifier(MonarchFocusRingModifier(isVisible: isVisible, shape: shape))
    }
}
