/// Replacements for the built-in strings of ``MonarchRichTextEditor``.
/// A property that is `nil` keeps the built-in English string.
@_spi(Experimental) public struct MonarchRichTextEditorLabels: Sendable {
    /// The character count without a limit. Built in: "1 character", "12 characters".
    public var count: (@Sendable (Int) -> String)?
    /// The visible count against a limit. Receives the count and the limit. Built in: "12 / 280".
    public var countOfLimit: (@Sendable (Int, Int) -> String)?
    /// Read for the count against a limit. Receives the characters left,
    /// negative when over. Built in: "268 characters left", "3 characters over the limit".
    public var remaining: (@Sendable (Int) -> String)?
    /// The strings of the formatting toolbar.
    public var toolbar: MonarchEditorToolbarLabels
    /// The strings of the mention suggestions.
    public var mentions: MonarchMentionPickerLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        count: (@Sendable (Int) -> String)? = nil,
        countOfLimit: (@Sendable (Int, Int) -> String)? = nil,
        remaining: (@Sendable (Int) -> String)? = nil,
        toolbar: MonarchEditorToolbarLabels = MonarchEditorToolbarLabels(),
        mentions: MonarchMentionPickerLabels = MonarchMentionPickerLabels()
    ) {
        self.count = count
        self.countOfLimit = countOfLimit
        self.remaining = remaining
        self.toolbar = toolbar
        self.mentions = mentions
    }
}
