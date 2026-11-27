/// The mention being typed: from the trigger character to the caret.
///
/// ```swift
/// .onChange(of: text) { _, new in
///     query = MonarchMentionQuery.find(in: new)
/// }
/// ```
public struct MonarchMentionQuery: Sendable, Hashable {
    /// The trigger that started it, such as `@`.
    public let trigger: Character
    /// What has been typed after the trigger.
    public let text: String
    /// The trigger and the text, in the string the query was found in.
    public let range: Range<String.Index>

    /// A query. Use ``find(in:caret:triggers:)`` to find one in text.
    ///
    /// - Parameters:
    ///   - trigger: The trigger character.
    ///   - text: What has been typed after it.
    ///   - range: The trigger and the text in the searched string.
    public init(trigger: Character, text: String, range: Range<String.Index>) {
        self.trigger = trigger
        self.text = text
        self.range = range
    }

    /// The longest word of a query, in characters.
    static let maxWordLength = 40

    /// Finds a mention being typed at the caret: a trigger at the start or
    /// after white space or "(", followed by up to two words of at most 40
    /// characters, without a line break.
    ///
    /// - Parameters:
    ///   - text: The text of the field.
    ///   - caret: Where the caret is. The end of the text by default, which
    ///     is where it is while someone types in a `TextField`.
    ///   - triggers: The characters that start a mention. Add `#` for channels.
    /// - Returns: The query, or `nil` when no mention is being typed.
    public static func find(
        in text: String,
        caret: String.Index? = nil,
        triggers: Set<Character> = ["@"]
    ) -> MonarchMentionQuery? {
        let end = caret ?? text.endIndex
        let before = text[..<end]
        guard let start = before.lastIndex(where: { triggers.contains($0) }) else { return nil }
        if start > before.startIndex {
            let previous = before[before.index(before: start)]
            guard previous.isWhitespace || previous == "(" else { return nil }
        }
        let typed = before[before.index(after: start)...]
        let words = typed.split(separator: " ", omittingEmptySubsequences: false)
        guard words.count <= 2,
            words.allSatisfy({ $0.count <= maxWordLength && !$0.contains(where: \.isWhitespace) })
        else { return nil }
        return MonarchMentionQuery(trigger: text[start], text: String(typed), range: start..<end)
    }

    /// Replaces the query in `text` with `replacement` and one space, so
    /// typing goes on after the mention. Pass the same text the query was
    /// found in.
    ///
    /// - Parameters:
    ///   - text: The text the query was found in.
    ///   - replacement: What the mention becomes, such as "@Ana Lopez" or a token.
    /// - Returns: The new text.
    public func replacing(in text: String, with replacement: String) -> String {
        guard range.upperBound <= text.endIndex else { return text }
        var after = text[range.upperBound...]
        if after.first == " " { after = after.dropFirst() }
        return String(text[..<range.lowerBound]) + replacement + " " + after
    }
}
