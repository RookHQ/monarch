/// What ``MonarchAiThread`` tells the view that draws one message.
@_spi(Experimental) public struct MonarchAiThreadMessageContext: Sendable, Hashable {
    /// The position of the message in the thread, from 0 for the oldest.
    public let index: Int
    /// Whether this is the newest message.
    public let isLast: Bool
    /// Whether the message should announce its own streaming text. `false`
    /// while the thread announces for the whole conversation; pass it to
    /// ``MonarchAiMessage``'s `announces`.
    public let announces: Bool

    /// A message context.
    ///
    /// - Parameters:
    ///   - index: The position of the message, from 0 for the oldest.
    ///   - isLast: Whether this is the newest message.
    ///   - announces: Whether the message announces its own streaming text.
    public init(index: Int, isLast: Bool, announces: Bool) {
        self.index = index
        self.isLast = isLast
        self.announces = announces
    }
}
