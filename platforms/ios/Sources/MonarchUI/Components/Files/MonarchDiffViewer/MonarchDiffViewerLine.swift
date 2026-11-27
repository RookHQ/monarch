/// One line of a line diff, with its line number on each side.
public struct MonarchDiffViewerLine: Sendable, Hashable {
    /// What happened to the line.
    public var kind: MonarchDiffViewerLineKind
    /// The text of the line.
    public var text: String
    /// The line number in the earlier text (same and removed lines).
    public var oldNumber: Int?
    /// The line number in the later text (same and added lines).
    public var newNumber: Int?

    /// A line of a diff.
    ///
    /// - Parameters:
    ///   - kind: What happened to the line.
    ///   - text: The text of the line.
    ///   - oldNumber: The line number in the earlier text.
    ///   - newNumber: The line number in the later text.
    public init(_ kind: MonarchDiffViewerLineKind, text: String, oldNumber: Int? = nil, newNumber: Int? = nil) {
        self.kind = kind
        self.text = text
        self.oldNumber = oldNumber
        self.newNumber = newNumber
    }

    /// The line diff (longest common subsequence) from `before` to `after`,
    /// numbered on both sides. Inputs of more than a million line pairs fall
    /// back to removing every line and adding every line.
    ///
    /// - Parameters:
    ///   - before: The lines or blocks of the earlier text.
    ///   - after: The lines or blocks of the later text.
    /// - Returns: The lines in reading order.
    public static func diff(from before: [String], to after: [String]) -> [MonarchDiffViewerLine] {
        MonarchTextDiff.lines(before, after)
    }

    /// The lines of a text: split on line breaks (`\n`, `\r\n` or `\r`). An
    /// empty text has no lines.
    ///
    /// - Parameter text: The text.
    /// - Returns: Its lines, without the line breaks.
    public static func lines(of text: String) -> [String] {
        MonarchTextDiff.split(text)
    }
}
