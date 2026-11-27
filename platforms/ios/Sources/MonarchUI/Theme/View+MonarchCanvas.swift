import SwiftUI

/// Paints the theme canvas behind a screen and hides the system background
/// of the scroll container, list or navigation stack that sits on it.
private struct MonarchCanvasModifier: ViewModifier {
    @Environment(\.monarchTheme) private var theme

    func body(content: Content) -> some View {
        let canvas = theme.colors.bgCanvas
        content
            .scrollContentBackground(.hidden)
            .background(canvas.ignoresSafeArea())
            .modifier(MonarchNavigationCanvas(canvas: canvas))
    }
}

/// On iOS 18 and later the navigation container has its own background,
/// which is the system one (pure black or white) unless it is replaced.
private struct MonarchNavigationCanvas: ViewModifier {
    let canvas: Color

    func body(content: Content) -> some View {
        #if os(iOS)
            if #available(iOS 18.0, *) {
                content.containerBackground(canvas, for: .navigation)
            } else {
                content
            }
        #else
            content
        #endif
    }
}

extension View {
    /// Paints the theme's canvas colour behind this screen.
    ///
    /// SwiftUI fills a `NavigationStack`, `List` and `Form` with the system
    /// background, which is pure black in Dark Mode and pure white in Light
    /// Mode. Apply this to the content of the stack, the list or the form so
    /// the screen sits on the Monarch canvas instead.
    ///
    /// ```swift
    /// NavigationStack {
    ///     ScrollView { … }
    ///         .monarchCanvas()
    /// }
    /// ```
    public func monarchCanvas() -> some View {
        modifier(MonarchCanvasModifier())
    }

    /// Tints the system controls below this view with the strong accent: a
    /// `Toggle` (switch), `Slider` and `Stepper` take their on and filled
    /// colour from it. The controls stay the system controls.
    public func monarchControlTint() -> some View {
        modifier(MonarchControlTintModifier())
    }
}

private struct MonarchControlTintModifier: ViewModifier {
    @Environment(\.monarchTheme) private var theme

    func body(content: Content) -> some View {
        content.tint(theme.colors.accentStrong)
    }
}
