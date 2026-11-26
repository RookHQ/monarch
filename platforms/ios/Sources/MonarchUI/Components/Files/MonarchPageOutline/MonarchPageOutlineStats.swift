/// The word count and reading time shown under a ``MonarchPageOutline``.
public struct MonarchPageOutlineStats: Sendable, Hashable {
    /// The number of words.
    public let words: Int
    /// The reading time in minutes, at least one.
    public let minutes: Int

    /// The words read per minute used to work out the reading time.
    public static let wordsPerMinute = 220

    /// Stats from known values.
    ///
    /// - Parameters:
    ///   - words: The number of words.
    ///   - minutes: The reading time in minutes.
    public init(words: Int, minutes: Int) {
        self.words = max(0, words)
        self.minutes = max(1, minutes)
    }

    /// Counts the words of a text and estimates the reading time at 220 words a minute.
    ///
    /// - Parameter text: The text of the document.
    public init(text: String) {
        let words = text.split(whereSeparator: { $0.isWhitespace || $0.isNewline }).count
        let minutes = Int((Double(words) / Double(Self.wordsPerMinute)).rounded())
        self.init(words: words, minutes: minutes)
    }
}
