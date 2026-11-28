/// The slash command being typed: from the trigger to the caret.
///
/// ```swift
/// .onChange(of: text) { _, new in
///     query = MonarchSlashMenuQuery.find(in: new)
/// }
/// ```
public struct MonarchSlashMenuQuery: Sendable, Hashable {
    /// The trigger that opened the menu, such as `/`.
    public let trigger: Character
    /// What has been typed after the trigger.
    public let text: String
    /// The trigger and the text, in the string the query was found in.
    public let range: Range<String.Index>

    /// A query. Use ``find(in:caret:trigger:position:maxLength:)`` to find one in text.
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

    /// Finds a slash command being typed before the caret: the trigger
    /// followed by letters, digits or hyphens, at the start of a line (or of
    /// a word with `.wordStart`).
    ///
    /// - Parameters:
    ///   - text: The text of the field.
    ///   - caret: Where the caret is. The end of the text by default, which
    ///     is where it is while someone types in a `TextField`.
    ///   - trigger: The character that opens the menu.
    ///   - position: Where the trigger may be typed.
    ///   - maxLength: The longest query that keeps the menu open.
    /// - Returns: The query, or `nil` when no command is being typed.
    public static func find(
        in text: String,
        caret: String.Index? = nil,
        trigger: Character = "/",
        position: MonarchSlashMenuTriggerPosition = .lineStart,
        maxLength: Int = 24
    ) -> MonarchSlashMenuQuery? {
        let end = caret ?? text.endIndex
        let before = text[..<end]
        let lineStart = before.lastIndex(where: \.isNewline).map { before.index(after: $0) } ?? before.startIndex
        let line = before[lineStart...]
        guard let start = line.lastIndex(of: trigger) else { return nil }
        let typed = line[line.index(after: start)...]
        guard typed.count <= maxLength,
            typed.allSatisfy({ $0.isLetter || $0.isNumber || $0 == "-" })
        else { return nil }
        if start > line.startIndex {
            let previous = line[line.index(before: start)]
            guard position == .wordStart, previous == " " || previous == "\t" else { return nil }
        }
        return MonarchSlashMenuQuery(trigger: trigger, text: String(typed), range: start..<end)
    }

    /// Replaces the trigger and the query in `text` with `replacement`, for
    /// example `"## "` for a heading, or nothing to only remove them. Pass the
    /// same text the query was found in.
    ///
    /// - Parameters:
    ///   - text: The text the query was found in.
    ///   - replacement: What the command becomes.
    /// - Returns: The new text.
    public func replacing(in text: String, with replacement: String = "") -> String {
        guard range.upperBound <= text.endIndex else { return text }
        return String(text[..<range.lowerBound]) + replacement + text[range.upperBound...]
    }
}
