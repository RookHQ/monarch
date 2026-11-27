import Foundation

/// The block parser of ``MonarchAiMarkdown``: a port of the web
/// `ai-markdown.parser.ts` (through the Android port, which keeps the same
/// structure), so every platform reads model output the same way.
///
/// Supported: ATX and setext headings, paragraphs, ordered, unordered,
/// nested and task lists, blockquotes, fenced and indented code, tables with
/// alignment, thematic breaks, footnotes and display maths (`$$` or `\[` on
/// their own line, kept as a `math` code block). Not supported, as on the
/// web: link reference definitions and HTML blocks, which stay text.
///
/// The parser is not tied to the main actor, so a long reply can be parsed
/// off it. The compiled patterns are kept once and reused; they are never
/// changed after they are created, so they are safe to share
/// (`nonisolated(unsafe)` only because Swift's `Regex` is not marked `Sendable`).
enum MonarchAiMarkdownParser {
    private static let tabStop = 4
    private static let codeIndent = 4

    private nonisolated(unsafe) static let fenceOpenPattern = /^( {0,3})(`{3,}|~{3,})(.*)$/
    private nonisolated(unsafe) static let atxPattern = /^ {0,3}(#{1,6})(?:[ \t]+(.*?))?[ \t]*$/
    private nonisolated(unsafe) static let atxClosingPattern = /(?:^|[ \t]+)#+$/
    private nonisolated(unsafe) static let rulePattern = /^ {0,3}(?:(?:\*[ \t]*){3,}|(?:-[ \t]*){3,}|(?:_[ \t]*){3,})$/
    private nonisolated(unsafe) static let quotePattern = /^ {0,3}> ?/
    private nonisolated(unsafe) static let footnoteDefinitionPattern = /^ {0,3}\[\^([^\]\s]+)\]:[ \t]*(.*)$/
    private nonisolated(unsafe) static let setextOnePattern = /^ {0,3}=+[ \t]*$/
    private nonisolated(unsafe) static let setextTwoPattern = /^ {0,3}-+[ \t]*$/
    private nonisolated(unsafe) static let tableDelimiterPattern =
        /^ {0,3}\|?[ \t]*:?-+:?[ \t]*(?:\|[ \t]*:?-+:?[ \t]*)*\|?[ \t]*$/
    private nonisolated(unsafe) static let markerPattern = /^( {0,3})([-+*]|[0-9]{1,9}[.)])(?=[ \t]|$)/
    private nonisolated(unsafe) static let mathOpenPattern = /^ {0,3}(\$\$|\\\[)(.*)$/
    private nonisolated(unsafe) static let unescapedPipePattern = /(?:^|[^\\])\|/
    private nonisolated(unsafe) static let taskPattern = /^\[([ xX])\](?:[ \t]+|$)/
    private nonisolated(unsafe) static let codeFilenamePattern = /(?:title|filename|file)=(?:"([^"]*)"|'([^']*)'|(\S+))/
    private nonisolated(unsafe) static let codeColonPattern = /^([\w+#-]+):(\S+)$/

    /// Parses Markdown into top-level block tokens.
    static func parse(_ text: String) -> [MonarchAiMarkdownBlock] {
        parseLines(splitLines(text).lines).blocks
    }

    /// Re-parses only what can have changed. When `text` extends the
    /// previous text, every block before the last is kept as it is and
    /// parsing restarts at the last block's first line. Anything else is a
    /// full parse.
    static func parseIncremental(
        _ text: String,
        previous: MonarchAiMarkdownParseState?
    ) -> MonarchAiMarkdownParseState {
        if let previous, previous.text == text { return previous }
        var keep = 0
        var from = 0
        if let previous, previous.blocks.count > 1, text.hasPrefix(previous.text), !previous.text.contains("\r") {
            keep = previous.blocks.count - 1
            from = previous.offsets[keep]
        }
        let start = String.Index(utf16Offset: from, in: text)
        let split = splitLines(String(text[start...]))
        let parsed = parseLines(split.lines)
        return MonarchAiMarkdownParseState(
            text: text,
            blocks: Array((previous?.blocks ?? []).prefix(keep)) + parsed.blocks,
            offsets: Array((previous?.offsets ?? []).prefix(keep)) + parsed.starts.map { from + split.offsets[$0] }
        )
    }

    // MARK: Lines and indentation

    static func splitLines(_ text: String) -> (lines: [String], offsets: [Int]) {
        let normalized = text.replacingOccurrences(of: "\r\n", with: "\n").replacingOccurrences(of: "\r", with: "\n")
        let lines = normalized.split(separator: "\n", omittingEmptySubsequences: false).map(String.init)
        var offsets: [Int] = []
        offsets.reserveCapacity(lines.count)
        var at = 0
        for line in lines {
            offsets.append(at)
            at += line.utf16.count + 1
        }
        return (lines, offsets)
    }

    static func isBlank(_ line: String) -> Bool {
        line.allSatisfy { $0 == " " || $0 == "\t" }
    }

    /// Indentation in columns; a tab advances to the next multiple of 4.
    static func indent(of line: String) -> Int {
        var column = 0
        for character in line {
            switch character {
            case " ": column += 1
            case "\t": column += tabStop - column % tabStop
            default: return column
            }
        }
        return column
    }

    /// Removes up to `columns` columns of leading white space, splitting a tab if needed.
    static func strip(_ line: String, columns: Int) -> String {
        var column = 0
        var index = line.startIndex
        while index < line.endIndex, column < columns {
            let character = line[index]
            if character == " " {
                column += 1
            } else if character == "\t" {
                let width = tabStop - column % tabStop
                if column + width > columns {
                    return String(repeating: " ", count: column + width - columns) + line[line.index(after: index)...]
                }
                column += width
            } else {
                break
            }
            index = line.index(after: index)
        }
        return String(line[index...])
    }

    private static func trimmed(_ text: some StringProtocol) -> String {
        text.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    // MARK: Block patterns

    private struct MathOpen {
        let close: String
        let single: String?
    }

    private static func mathOpen(_ line: String) -> MathOpen? {
        guard let match = line.firstMatch(of: mathOpenPattern) else { return nil }
        let close = match.output.1 == "$$" ? "$$" : "\\]"
        let rest = trimmed(match.output.2)
        if rest.isEmpty { return MathOpen(close: close, single: nil) }
        if rest.count > close.count, rest.hasSuffix(close) {
            return MathOpen(close: close, single: trimmed(rest.dropLast(close.count)))
        }
        return nil
    }

    private struct Fence {
        let indent: Int
        let fence: String
        let info: String
    }

    private static func fenceOpen(_ line: String) -> Fence? {
        guard let match = line.firstMatch(of: fenceOpenPattern) else { return nil }
        let fence = String(match.output.2)
        let info = match.output.3
        // A backtick fence's info string may not contain a backtick (that is inline code).
        if fence.first == "`", info.contains("`") { return nil }
        return Fence(indent: match.output.1.count, fence: fence, info: trimmed(info))
    }

    /// Whether `line` closes a fence of at least `length` of `character`.
    private static func closesFence(_ line: String, character: Character, length: Int) -> Bool {
        var rest = Substring(line)
        var spaces = 0
        while rest.first == " ", spaces < 3 {
            rest = rest.dropFirst()
            spaces += 1
        }
        let run = rest.prefix { $0 == character }.count
        guard run >= length else { return false }
        return rest.dropFirst(run).allSatisfy { $0 == " " || $0 == "\t" }
    }

    private struct Marker {
        let ordered: Bool
        let key: String
        let start: Int
        let contentColumn: Int
        let first: String
        let isEmpty: Bool
    }

    private static func listMarker(_ line: String) -> Marker? {
        guard let match = line.firstMatch(of: markerPattern) else { return nil }
        let lead = match.output.1.count
        let marker = String(match.output.2)
        let markerEnd = lead + marker.count
        let after = String(line[match.range.upperBound...])
        let padded = String(repeating: " ", count: markerEnd) + after
        let spaces = indent(of: padded) - markerEnd
        let isEmpty = isBlank(after)
        let contentColumn = isEmpty || spaces > codeIndent ? markerEnd + 1 : markerEnd + spaces
        let ordered = marker.first?.isASCII == true && marker.first?.isNumber == true
        return Marker(
            ordered: ordered,
            key: ordered ? String(marker.suffix(1)) : marker,
            start: ordered ? Int(marker.dropLast()) ?? 1 : 1,
            contentColumn: contentColumn,
            first: isEmpty ? "" : strip(padded, columns: contentColumn),
            isEmpty: isEmpty
        )
    }

    private static func isRule(_ line: String) -> Bool {
        line.firstMatch(of: rulePattern) != nil
    }

    private static func isQuote(_ line: String) -> Bool {
        line.firstMatch(of: quotePattern) != nil
    }

    /// True when `line` ends a paragraph by starting another block.
    private static func interruptsParagraph(_ line: String) -> Bool {
        if fenceOpen(line) != nil || mathOpen(line) != nil || line.firstMatch(of: atxPattern) != nil
            || isRule(line) || isQuote(line)
        {
            return true
        }
        if line.firstMatch(of: footnoteDefinitionPattern) != nil { return true }
        guard let marker = listMarker(line) else { return false }
        // Only a non-empty item interrupts a paragraph; an ordered one must start at 1.
        return !marker.isEmpty && (!marker.ordered || marker.start == 1)
    }

    /// Splits a table row into raw cell strings, honouring `\|` and code spans.
    static func splitRow(_ line: String) -> [String] {
        var characters = Array(trimmed(line))
        if characters.first == "|" { characters.removeFirst() }
        if characters.last == "|", !(characters.count >= 2 && characters[characters.count - 2] == "\\") {
            characters.removeLast()
        }
        var cells: [String] = []
        var cell = ""
        var tick = 0
        var index = 0
        while index < characters.count {
            let character = characters[index]
            if character == "\\", index + 1 < characters.count, characters[index + 1] == "|" {
                cell.append("|")
                index += 1
            } else if character == "`" {
                var run = 1
                while index + run < characters.count, characters[index + run] == "`" { run += 1 }
                if tick == 0 {
                    tick = run
                } else if tick == run {
                    tick = 0
                }
                cell += String(repeating: "`", count: run)
                index += run - 1
            } else if character == "|", tick == 0 {
                cells.append(trimmed(cell))
                cell = ""
            } else {
                cell.append(character)
            }
            index += 1
        }
        cells.append(trimmed(cell))
        return cells
    }

    private static func tableStart(_ lines: [String], _ index: Int) -> [MonarchAiMarkdownAlignment?]? {
        guard index + 1 < lines.count else { return nil }
        let head = lines[index]
        let delimiter = lines[index + 1]
        guard head.firstMatch(of: unescapedPipePattern) != nil,
            delimiter.firstMatch(of: tableDelimiterPattern) != nil
        else { return nil }
        let cells = splitRow(delimiter)
        guard cells.count == splitRow(head).count else { return nil }
        return cells.map { cell in
            let leading = cell.hasPrefix(":")
            let trailing = cell.hasSuffix(":")
            switch (leading, trailing) {
            case (true, true): return .center
            case (true, false): return .leading
            case (false, true): return .trailing
            case (false, false): return nil
            }
        }
    }

    /// The language and file name of a fence's info string ("ts title=app.ts", "ts:app.ts").
    static func codeInfo(_ info: String) -> (language: String, filename: String?) {
        let first = String(info.split(whereSeparator: \.isWhitespace).first ?? "")
        let language = first.lowercased()
        var filename: String?
        if let match = info.firstMatch(of: codeFilenamePattern) {
            filename = (match.output.1 ?? match.output.2 ?? match.output.3).map(String.init)
        }
        if filename == nil, let colon = first.firstMatch(of: codeColonPattern) {
            return (String(colon.output.1).lowercased(), String(colon.output.2))
        }
        return (language, filename)
    }

    // MARK: Block parser

    private struct Parsed {
        var blocks: [MonarchAiMarkdownBlock] = []
        var starts: [Int] = []
        var ends: [Int] = []

        mutating func push(_ block: MonarchAiMarkdownBlock, _ start: Int, _ end: Int) {
            blocks.append(block)
            starts.append(start)
            ends.append(end)
        }
    }

    // A port of the web parser: the structure is kept so the two can be compared.
    private static func parseLines(_ lines: [String]) -> Parsed {
        var out = Parsed()
        var index = 0
        while index < lines.count {
            let line = lines[index]
            if isBlank(line) {
                index += 1
                continue
            }
            let start = index

            if let math = mathOpen(line) {
                var body: [String] = []
                var closed = math.single != nil
                index += 1
                if let single = math.single {
                    body.append(single)
                } else {
                    while index < lines.count {
                        let next = lines[index]
                        index += 1
                        if trimmed(next) == math.close {
                            closed = true
                            break
                        }
                        body.append(next)
                    }
                }
                let code = MonarchAiMarkdownCode(
                    language: "math",
                    info: "math",
                    filename: nil,
                    code: body.joined(separator: "\n"),
                    isClosed: closed
                )
                out.push(.code(code), start, index)
                continue
            }

            // Fenced code. An unclosed fence runs to the end, so a half-streamed block already renders as code.
            if let fence = fenceOpen(line) {
                let character: Character = fence.fence.first == "`" ? "`" : "~"
                var body: [String] = []
                var closed = false
                index += 1
                while index < lines.count {
                    let next = lines[index]
                    index += 1
                    if closesFence(next, character: character, length: fence.fence.count) {
                        closed = true
                        break
                    }
                    body.append(strip(next, columns: fence.indent))
                }
                let info = codeInfo(fence.info)
                let code = MonarchAiMarkdownCode(
                    language: info.language,
                    info: fence.info,
                    filename: info.filename,
                    code: body.joined(separator: "\n"),
                    isClosed: closed
                )
                out.push(.code(code), start, index)
                continue
            }

            // Indented code.
            if indent(of: line) >= codeIndent {
                var end = index
                while end < lines.count, isBlank(lines[end]) || indent(of: lines[end]) >= codeIndent { end += 1 }
                while end > index, isBlank(lines[end - 1]) { end -= 1 }
                let code = lines[index..<end].map { strip($0, columns: codeIndent) }.joined(separator: "\n")
                let block = MonarchAiMarkdownCode(language: "", info: "", filename: nil, code: code, isClosed: true)
                out.push(.code(block), index, end)
                index = end
                continue
            }

            if let atx = line.firstMatch(of: atxPattern) {
                let raw = String(atx.output.2 ?? "")
                let text = trimmed(raw.replacing(atxClosingPattern, with: ""))
                index += 1
                out.push(
                    .heading(level: atx.output.1.count, content: MonarchAiMarkdownInlineParser.parse(text)),
                    start,
                    index
                )
                continue
            }

            if isRule(line) {
                index += 1
                out.push(.rule, start, index)
                continue
            }

            if isQuote(line) {
                var inner: [String] = []
                while index < lines.count {
                    let next = lines[index]
                    if let match = next.firstMatch(of: quotePattern) {
                        inner.append(String(next[match.range.upperBound...]))
                    } else if !isBlank(next), let last = inner.last, !isBlank(last), !interruptsParagraph(next) {
                        inner.append(next)  // lazy continuation
                    } else {
                        break
                    }
                    index += 1
                }
                out.push(.quote(parseLines(inner).blocks), start, index)
                continue
            }

            if let footnote = line.firstMatch(of: footnoteDefinitionPattern) {
                var inner = [String(footnote.output.2)]
                index += 1
                while index < lines.count {
                    let next = lines[index]
                    if isBlank(next) {
                        let following = lines[index...].first { !isBlank($0) }
                        guard let following, indent(of: following) >= codeIndent else { break }
                        inner.append("")
                    } else if indent(of: next) >= codeIndent {
                        inner.append(strip(next, columns: codeIndent))
                    } else if let last = inner.last, !isBlank(last), !interruptsParagraph(next) {
                        inner.append(next)
                    } else {
                        break
                    }
                    index += 1
                }
                let label = normalizeLabel(String(footnote.output.1))
                out.push(.footnoteDefinition(label: label, children: parseLines(inner).blocks), start, index)
                continue
            }

            if let marker = listMarker(line) {
                let (end, list) = parseList(lines, from: index, first: marker)
                index = end
                out.push(list, start, index)
                continue
            }

            if let alignments = tableStart(lines, index) {
                let header = splitRow(line).map { MonarchAiMarkdownInlineParser.parse($0) }
                var rows: [[[MonarchAiMarkdownInline]]] = []
                index += 2
                while index < lines.count {
                    let next = lines[index]
                    if isBlank(next) || interruptsParagraph(next) { break }
                    let cells = splitRow(next)
                    rows.append(
                        alignments.indices.map { column in
                            MonarchAiMarkdownInlineParser.parse(column < cells.count ? cells[column] : "")
                        }
                    )
                    index += 1
                }
                let table = MonarchAiMarkdownTableContent(alignments: alignments, header: header, rows: rows)
                out.push(.table(table), start, index)
                continue
            }

            // Paragraph, possibly turned into a setext heading.
            var paragraph = [line]
            var level: Int?
            index += 1
            while index < lines.count {
                let next = lines[index]
                if isBlank(next) { break }
                let isSetextOne = next.firstMatch(of: setextOnePattern) != nil
                if isSetextOne || next.firstMatch(of: setextTwoPattern) != nil {
                    level = isSetextOne ? 1 : 2
                    index += 1
                    break
                }
                if interruptsParagraph(next) || tableStart(lines, index) != nil { break }
                paragraph.append(next)
                index += 1
            }
            let text = trimTrailingSpaces(
                paragraph.map { String($0.drop { $0 == " " || $0 == "\t" }) }.joined(separator: "\n")
            )
            let content = MonarchAiMarkdownInlineParser.parse(text)
            if let level {
                out.push(.heading(level: level, content: content), start, index)
            } else {
                out.push(.paragraph(content), start, index)
            }
        }
        return out
    }

    private static func trimTrailingSpaces(_ text: String) -> String {
        var result = Substring(text)
        while let last = result.last, last == " " || last == "\t" { result = result.dropLast() }
        return String(result)
    }

    private static func parseList(
        _ lines: [String],
        from: Int,
        first: Marker
    ) -> (end: Int, block: MonarchAiMarkdownBlock) {
        var items: [MonarchAiMarkdownListItem] = []
        var isLoose = false
        var index = from
        while index < lines.count {
            let line = lines[index]
            if isRule(line) { break }
            guard let marker = listMarker(line), marker.key == first.key, marker.ordered == first.ordered else { break }
            var body = [marker.first]
            index += 1
            while index < lines.count {
                let next = lines[index]
                if isBlank(next) {
                    body.append("")
                } else if indent(of: next) >= marker.contentColumn {
                    body.append(strip(next, columns: marker.contentColumn))
                } else {
                    let previous = body.last ?? ""
                    let hasOpenFence = body.filter { fenceOpen($0) != nil }.count % 2 == 1
                    // Lazy continuation of the item's paragraph.
                    if !isBlank(previous), !hasOpenFence, !interruptsParagraph(next), listMarker(next) == nil {
                        body.append(trimmed(next))
                    } else {
                        break
                    }
                }
                index += 1
            }
            // Trailing blank lines belong between items, not to this item.
            var trailing = 0
            while body.count > 1, let last = body.last, isBlank(last) {
                body.removeLast()
                trailing += 1
            }
            index -= trailing
            var checked: Bool?
            if let task = body[0].firstMatch(of: taskPattern) {
                checked = task.output.1 != " "
                body[0] = String(body[0][task.range.upperBound...])
            }
            let parsed = parseLines(body)
            if parsed.blocks.count > 1 {
                for child in 1..<parsed.blocks.count {
                    for lineIndex in parsed.ends[child - 1]..<parsed.starts[child] where isBlank(body[lineIndex]) {
                        isLoose = true
                    }
                }
            }
            items.append(MonarchAiMarkdownListItem(checked: checked, children: parsed.blocks))
            if trailing > 0 {
                let nextIndex = index + trailing
                if nextIndex < lines.count, let nextMarker = listMarker(lines[nextIndex]), nextMarker.key == first.key,
                    !isRule(lines[nextIndex])
                {
                    isLoose = true
                    index += trailing
                } else {
                    break
                }
            }
        }
        return (index, .list(ordered: first.ordered, start: first.start, isLoose: isLoose, items: items))
    }

    /// Footnote labels match case-insensitively.
    static func normalizeLabel(_ label: String) -> String {
        trimmed(label).lowercased()
    }
}
