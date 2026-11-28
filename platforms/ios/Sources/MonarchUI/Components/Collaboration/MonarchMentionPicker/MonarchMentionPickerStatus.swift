/// The state of the search behind a ``MonarchMentionPicker``.
public enum MonarchMentionPickerStatus: Sendable, Hashable, CaseIterable {
    /// A search is running: placeholder rows are shown.
    case loading
    /// The items are the results. Empty results show "No matches".
    case ready
    /// The search failed: an error with a Retry button is shown.
    case failed
}
