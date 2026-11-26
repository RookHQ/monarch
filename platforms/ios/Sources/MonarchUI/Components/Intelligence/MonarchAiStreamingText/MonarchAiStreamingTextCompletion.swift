import Foundation

/// Makes the unfinished end of a streaming answer displayable without
/// flashes, as the web's `completeStreamingMarkdown` does: closes open
/// emphasis and code spans, hides a link whose target is still arriving
/// (its text shows plainly), hides a lone trailing `*`, `_`, `~` or `` ` ``,
/// and holds back table rows until the delimiter row makes them a table.
/// Every function is pure. Isolated to the main actor, like the Markdown
/// parser, because Swift's `Regex` is not `Sendable`.
@MainActor
enum MonarchAiStreamingTextCompletion {
    private static let literalFirstLine = /^ {0,3}(`{3,}|~{3,}|\$\$|\\\[)/
    private static let tableRow = /^\s*\|/
    private static let pendingLinkTarget = /!?\[([^\]\n]*)\]\([^)\s]*\z/
    private static let pendingLinkOpen = /!?\[([^\]\n^][^\]\n]*)\]\z/
    private static let pendingLinkText = /!?\[\^?[^\]\n]*\z/
    private static let trailingDelimiters = /(\A|\s)[*_~`]+\z/
    private static let escapeOrCode = /\\.|`+/

    /// The source of the last block, completed for display. Code and display
    /// maths are left alone: an unclosed fence already renders as code.
    static func complete(_ source: String) -> String {
        let firstLine = source.split(separator: "\n", maxSplits: 1, omittingEmptySubsequences: false).first ?? ""
        if firstLine.firstMatch(of: literalFirstLine) != nil || firstLine.hasPrefix("    ")
            || firstLine.hasPrefix("\t")
        {
            return source
        }

        var lines = source.components(separatedBy: "\n")
        let isTable = MonarchAiMarkdownParser.parse(source).contains { block in
            if case .table = block { return true }
            return false
        }
        if !isTable {
            // A table header, and a partial delimiter row, are not a table yet: hold them back.
            while let last = lines.last, last.firstMatch(of: tableRow) != nil {
                lines.removeLast()
            }
        }
        var text = lines.joined(separator: "\n")

        // A link or image whose target is still arriving shows its text only.
        text.replace(pendingLinkTarget) { String($0.output.1) }
        text.replace(pendingLinkOpen) { String($0.output.1) }
        text.replace(pendingLinkText) { match in
            let found = String(match.output)
            if found.hasPrefix("[^") { return "" }
            return String(found.drop { $0 == "!" }.dropFirst())
        }
        // A delimiter run with nothing after it yet.
        text.replace(trailingDelimiters) { String($0.output.1) }

        // Close an open code span.
        var open = 0
        for match in text.matches(of: escapeOrCode) where match.output.first == "`" {
            let length = match.output.count
            if open == 0 {
                open = length
            } else if open == length {
                open = 0
            }
        }
        if open > 0 {
            return trimmingTrailingWhitespace(text) + String(repeating: "`", count: open)
        }

        let closers = openEmphasis(text)
        return closers.isEmpty ? text : trimmingTrailingWhitespace(text) + closers.joined()
    }

    /// The closing delimiters for the emphasis runs still open in `source`,
    /// innermost first, with the CommonMark flanking rules and code spans skipped.
    static func openEmphasis(_ source: String) -> [String] {
        let characters = Array(source)
        var stack: [(character: Character, length: Int)] = []
        var codeRun = 0
        var index = 0
        while index < characters.count {
            let character = characters[index]
            if character == "\\" {
                index += index + 1 < characters.count && characters[index + 1] != "\n" ? 2 : 1
                continue
            }
            if character == "`" {
                let length = run(of: character, in: characters, from: index)
                if codeRun == 0 {
                    codeRun = length
                } else if codeRun == length {
                    codeRun = 0
                }
                index += length
                continue
            }
            guard isDelimiter(character) else {
                index += 1
                continue
            }
            if codeRun > 0 {
                while index < characters.count, isDelimiter(characters[index]) { index += 1 }
                continue
            }
            // A run may mix characters ("*_"); handle the first character's run only.
            let length = run(of: character, in: characters, from: index)
            let before: Character = index == 0 ? " " : characters[index - 1]
            let after: Character = index + length < characters.count ? characters[index + length] : " "
            index += length
            if character == "~" && length > 2 { continue }
            let left = !after.isWhitespace && (!isPunctuation(after) || before.isWhitespace || isPunctuation(before))
            let right =
                !before.isWhitespace && (!isPunctuation(before) || after.isWhitespace || isPunctuation(after))
            let canOpen = character == "_" ? left && (!right || isPunctuation(before)) : left
            let canClose = character == "_" ? right && (!left || isPunctuation(after)) : right
            if canClose, let at = stack.lastIndex(where: { $0.character == character }) {
                stack.removeSubrange(at...)
            } else if canOpen {
                stack.append((character, length))
            }
        }
        return stack.reversed().map { String(repeating: String($0.character), count: $0.length) }
    }

    private static func isDelimiter(_ character: Character) -> Bool {
        character == "*" || character == "_" || character == "~"
    }

    private static func isPunctuation(_ character: Character) -> Bool {
        character.isPunctuation || character.isSymbol
    }

    private static func run(of character: Character, in characters: [Character], from start: Int) -> Int {
        var length = 0
        while start + length < characters.count, characters[start + length] == character { length += 1 }
        return length
    }

    private static func trimmingTrailingWhitespace(_ text: String) -> String {
        var result = Substring(text)
        while let last = result.last, last.isWhitespace { result.removeLast() }
        return String(result)
    }
}
