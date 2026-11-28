/// What the detail screen of ``MonarchVersionHistoryPanel`` shows.
enum MonarchVersionHistoryPanelTab: Hashable, CaseIterable {
    /// The diff against the content now.
    case changes
    /// The version's whole text.
    case fullText
}
