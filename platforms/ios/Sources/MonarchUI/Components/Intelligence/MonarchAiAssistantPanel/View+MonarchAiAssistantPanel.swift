import SwiftUI

/// The placement that `View.monarchAiAssistantPanel(isPresented:panel:)` gives its panel.
private struct MonarchAiAssistantPanelPresentationKey: EnvironmentKey {
    static let defaultValue: MonarchAiAssistantPanelPlacement? = nil
}

extension EnvironmentValues {
    /// The placement chosen by the presenting modifier, read by `automatic` panels.
    var monarchAiAssistantPanelPresentation: MonarchAiAssistantPanelPlacement? {
        get { self[MonarchAiAssistantPanelPresentationKey.self] }
        set { self[MonarchAiAssistantPanelPresentationKey.self] = newValue }
    }
}

/// Presents the panel as a sheet in a compact width and as an inspector in a regular width.
private struct MonarchAiAssistantPanelPresenter<Panel: View>: ViewModifier {
    @Binding var isPresented: Bool
    let panel: Panel

    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    func body(content: Content) -> some View {
        let isCompact = horizontalSizeClass == .compact
        // Both presentations stay attached so the host keeps its identity
        // when the width class changes; only one is ever bound.
        content
            .sheet(isPresented: isCompact ? $isPresented : .constant(false)) {
                panel.environment(\.monarchAiAssistantPanelPresentation, .sheet)
            }
            .inspector(isPresented: isCompact ? .constant(false) : $isPresented) {
                panel.environment(\.monarchAiAssistantPanelPresentation, .inline)
            }
    }
}

extension View {
    /// Shows an assistant panel beside this view: a `.sheet` with the medium
    /// and large detents in a compact width (iPhone), and an `.inspector`
    /// column in a regular width (iPad).
    ///
    /// ```swift
    /// DocumentView()
    ///     .monarchAiAssistantPanel(isPresented: $showsAssistant) {
    ///         MonarchAiAssistantPanel("Assistant", conversation: conversation, isPresented: $showsAssistant)
    ///     }
    /// ```
    ///
    /// A panel with the `automatic` placement draws the matching chrome.
    /// Hold the conversation and the draft above this view: the sheet's
    /// content is rebuilt each time it opens.
    ///
    /// - Parameters:
    ///   - isPresented: Whether the panel shows. The panel's Close sets it to `false`.
    ///   - panel: The ``MonarchAiAssistantPanel``.
    /// - Returns: The view with the panel attached.
    @_spi(Experimental) public func monarchAiAssistantPanel<Panel: View>(
        isPresented: Binding<Bool>,
        @ViewBuilder panel: () -> Panel
    ) -> some View {
        modifier(MonarchAiAssistantPanelPresenter(isPresented: isPresented, panel: panel()))
    }
}
