/// The mention suggestions of a ``MonarchMessageComposer``.
///
/// The composer finds the mention being typed and reports it through
/// `onQueryChange`; the app runs its search and passes the results back in
/// `suggestions` and `status`. Picking one writes "@Label" into the text and
/// records the item in ``MonarchMessageComposerMessage/mentions``.
public struct MonarchMessageComposerMentions {
    /// The suggestions for the current query.
    public var suggestions: [MonarchMentionItem]
    /// Loading, ready or failed.
    public var status: MonarchMentionPickerStatus
    /// The characters that start a mention. Add `#` for channels.
    public var triggers: Set<Character>
    /// Receives the mention being typed, or `nil` when there is none.
    public var onQueryChange: @MainActor (MonarchMentionQuery?) -> Void
    /// Repeats a failed search. Adds a retry button to the error.
    public var onRetry: (@MainActor () -> Void)?

    /// Mention suggestions.
    ///
    /// - Parameters:
    ///   - suggestions: The suggestions for the current query.
    ///   - status: Loading, ready or failed.
    ///   - triggers: The characters that start a mention.
    ///   - onRetry: Repeats a failed search.
    ///   - onQueryChange: Receives the mention being typed, or `nil`.
    public init(
        _ suggestions: [MonarchMentionItem],
        status: MonarchMentionPickerStatus = .ready,
        triggers: Set<Character> = ["@"],
        onRetry: (@MainActor () -> Void)? = nil,
        onQueryChange: @escaping @MainActor (MonarchMentionQuery?) -> Void
    ) {
        self.suggestions = suggestions
        self.status = status
        self.triggers = triggers
        self.onRetry = onRetry
        self.onQueryChange = onQueryChange
    }
}
