/// One line of a code block, ready to draw: its diff kind and its tokens.
struct MonarchCodeBlockLine: Sendable, Hashable {
    /// What a line of a diff is.
    enum DiffKind: Sendable, Hashable, CaseIterable {
        /// A line the change adds (`+`).
        case add
        /// A line the change removes (`-`).
        case remove
        /// A hunk header (`@@`).
        case hunk
        /// A file header before the first hunk (`diff`, `index`, `+++`, `---`).
        case meta
        /// An unchanged line.
        case context
    }

    /// The diff kind, or `nil` when the block is not a diff.
    var diff: DiffKind?
    /// The tokens of the line, without the line break.
    var tokens: [MonarchCodeBlockTokenizer.Token]

    /// The text of the line.
    var text: String { tokens.map(\.text).joined() }

    private static let metaPrefixes = [
        "diff ", "index ", "+++ ", "--- ", "new file", "deleted file", "similarity", "rename ",
    ]

    /// The diff kind of every line. File headers count only before the first hunk.
    static func diffKinds(_ lines: [String]) -> [DiffKind] {
        var inHunk = false
        return lines.map { line in
            if line.hasPrefix("@@") {
                inHunk = true
                return .hunk
            }
            if !inHunk, metaPrefixes.contains(where: { line.hasPrefix($0) }) { return .meta }
            if line.hasPrefix("+") { return .add }
            if line.hasPrefix("-") { return .remove }
            return .context
        }
    }

    /// Splits code into lines. A final line break does not add an empty
    /// line. A diff loses its `+`, `-` or leading space, which the block draws
    /// as a separate marker; other code is coloured when `highlight` is true
    /// and the language is known.
    static func lines(code: String, language: String?, isDiff: Bool, highlight: Bool) -> [MonarchCodeBlockLine] {
        var source = MonarchCodeBlockTokenizer.normalized(code)
        if source.hasSuffix("\n") { source.removeLast() }
        if isDiff {
            let raw = source.split(separator: "\n", omittingEmptySubsequences: false).map(String.init)
            return zip(raw, diffKinds(raw)).map { text, kind in
                let dropsMarker = kind == .add || kind == .remove || (kind == .context && text.hasPrefix(" "))
                let body = dropsMarker ? String(text.dropFirst()) : text
                return MonarchCodeBlockLine(diff: kind, tokens: body.isEmpty ? [] : [.init(kind: .plain, text: body)])
            }
        }
        let tokens: [MonarchCodeBlockTokenizer.Token] =
            highlight
            ? MonarchCodeBlockTokenizer.tokenize(source, language: language)
            : (source.isEmpty ? [] : [.init(kind: .plain, text: source)])
        return MonarchCodeBlockTokenizer.lines(tokens).map { MonarchCodeBlockLine(diff: nil, tokens: $0) }
    }
}
