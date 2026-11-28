import SwiftUI

/// Draws the toasts of a center over the content, at one edge.
struct MonarchToastHostModifier: ViewModifier {
    let center: MonarchToastCenter
    let position: MonarchToastPosition
    let dismissLabel: String?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content
            .environment(center)
            .overlay(alignment: position == .bottom ? .bottom : .top) {
                VStack(spacing: MonarchToastTokens.stackSpacing) {
                    ForEach(center.toasts) { toast in
                        MonarchToastView(toast: toast, dismissLabel: dismissLabel) {
                            center.dismiss(toast.id)
                        }
                        .transition(
                            .move(edge: position == .bottom ? .bottom : .top).combined(with: .opacity)
                        )
                        .gesture(
                            DragGesture(minimumDistance: MonarchToastTokens.swipeDistance)
                                .onEnded { value in
                                    let drag = value.translation.height
                                    if MonarchToastTokens.dragDismisses(translation: drag, position: position) {
                                        center.dismiss(toast.id)
                                    }
                                }
                        )
                    }
                }
                .padding(MonarchToastTokens.screenMargin)
                .animation(
                    theme.motion.animation(.base, easing: .enter, reduceMotion: reduceMotion),
                    value: center.toasts.map(\.id)
                )
            }
    }
}

extension View {
    /// Shows the toasts of a center over this view, and puts the center in
    /// the environment so descendants can show toasts.
    ///
    /// Apply it once, to the root view of a scene. The toasts sit inside the
    /// safe area, so they move above the keyboard. A sheet or a full-screen
    /// cover is a separate presentation that covers this view: apply the
    /// modifier to its content too, with a center of its own, to show toasts
    /// over it.
    ///
    /// ```swift
    /// ContentView()
    ///     .monarchToastHost(toasts)
    /// ```
    ///
    /// - Parameters:
    ///   - center: The center that holds the toasts.
    ///   - position: The edge where toasts appear.
    ///   - dismissLabel: Replaces the built-in "Dismiss" spoken name of the close button.
    /// - Returns: The view with the toasts over it.
    public func monarchToastHost(
        _ center: MonarchToastCenter,
        position: MonarchToastPosition = .bottom,
        dismissLabel: String? = nil
    ) -> some View {
        modifier(MonarchToastHostModifier(center: center, position: position, dismissLabel: dismissLabel))
    }
}
