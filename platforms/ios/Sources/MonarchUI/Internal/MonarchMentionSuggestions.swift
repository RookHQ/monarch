/// When the suggestions of an "@" mention are shown, for
/// ``MonarchMentionPicker`` and the AI mention context.
enum MonarchMentionSuggestions {
    /// Whether the suggestions are shown: while a query is open, except after
    /// a query with a space found nothing, because the person has moved on.
    static func isOpen(query: MonarchMentionQuery?, status: MonarchMentionPickerStatus, itemCount: Int) -> Bool {
        guard let query else { return false }
        return !(status == .ready && itemCount == 0 && query.text.contains(" "))
    }
}
