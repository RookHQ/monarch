/// Replacements for the built-in strings of ``MonarchReadReceipt``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchReadReceiptLabels: Sendable {
    /// The visible text of the text and avatars displays. Built in: "Seen".
    public var seen: String?
    /// The visible text of the count display. Receives the number of
    /// readers. Built in: "Seen by 3".
    public var seenByCount: (@Sendable (Int) -> String)?
    /// The full sentence read by VoiceOver and shown on hover. Receives the
    /// joined names. Built in: "Seen by Ana, Ben and Chen".
    public var seenBy: (@Sendable (String) -> String)?
    /// Joins the readers' names. Built in: the locale's list format.
    public var list: (@Sendable ([String]) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        seen: String? = nil,
        seenByCount: (@Sendable (Int) -> String)? = nil,
        seenBy: (@Sendable (String) -> String)? = nil,
        list: (@Sendable ([String]) -> String)? = nil
    ) {
        self.seen = seen
        self.seenByCount = seenByCount
        self.seenBy = seenBy
        self.list = list
    }
}
