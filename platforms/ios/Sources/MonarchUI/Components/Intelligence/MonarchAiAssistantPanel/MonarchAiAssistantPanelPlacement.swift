/// How a ``MonarchAiAssistantPanel`` draws its chrome.
@_spi(Experimental) public enum MonarchAiAssistantPanelPlacement: Sendable, Hashable, CaseIterable {
    /// The placement chosen by `View.monarchAiAssistantPanel(isPresented:panel:)`;
    /// outside it, `sheet` in a compact width and `inline` in a regular width.
    case automatic
    /// A pane the app places, such as the content of `.inspector` or a split
    /// view column: the panel draws its own header with the title and Close.
    case inline
    /// The content of a `.sheet`: a `NavigationStack` with the title and Close
    /// in the navigation bar, and the medium and large detents.
    case sheet
}
