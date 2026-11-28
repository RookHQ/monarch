/// How ``MonarchAiAgentPicker`` lays out its agents.
@_spi(Experimental) public enum MonarchAiAgentPickerLayout: String, CaseIterable, Hashable, Sendable {
    /// One agent per row.
    case list
    /// Two columns on a regular width; one on a compact width and at the accessibility text sizes.
    case grid
}
