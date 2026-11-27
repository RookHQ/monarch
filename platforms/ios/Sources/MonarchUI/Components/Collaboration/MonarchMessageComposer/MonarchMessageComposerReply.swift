/// The message a ``MonarchMessageComposer`` answers, shown above the text.
public struct MonarchMessageComposerReply: Sendable, Hashable {
    /// Who wrote the message being answered, already localised.
    public let author: String
    /// A short quote of the message, on one line.
    public let excerpt: String?

    /// A reply context.
    ///
    /// - Parameters:
    ///   - author: Who wrote the message being answered.
    ///   - excerpt: A short quote of it.
    public init(author: String, excerpt: String? = nil) {
        self.author = author
        self.excerpt = excerpt
    }
}
