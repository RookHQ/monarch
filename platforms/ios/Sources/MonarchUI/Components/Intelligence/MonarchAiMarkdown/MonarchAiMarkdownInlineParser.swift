import Foundation

/// The inline parser of ``MonarchAiMarkdown``, ported from the web: code
/// spans, emphasis with CommonMark's delimiter rules, strikethrough, links,
/// images, autolinks (`<…>` and bare `https://` or `www.`), footnote
/// references, entities, escapes, hard breaks and inline maths (`\(…\)` or
/// `$$…$$`). Raw HTML is never interpreted: it stays text.
enum MonarchAiMarkdownInlineParser {
    private static let footnoteWindow = 200
    private static let autolinkWindow = 2048
    private static let entityWindow = 12
    private static let ruleOfThree = 3
    private static let strongRun = 2

    private nonisolated(unsafe) static let entityPattern = /^&(?:#([0-9]{1,7})|#[xX]([0-9a-fA-F]{1,6})|([a-zA-Z]+));/
    private nonisolated(unsafe) static let footnoteReferencePattern = /^\[\^([^\]\s]+)\]/
    private nonisolated(unsafe) static let autolinkURIPattern = /^<([a-zA-Z][a-zA-Z0-9+.\-]{1,31}:[^<>\s]*)>/
    private nonisolated(unsafe) static let autolinkEmailPattern =
        /^<([a-zA-Z0-9.!#$%&'*+\/=?^_`{|}~\-]+@[a-zA-Z0-9](?:[a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?)*)>/
    private nonisolated(unsafe) static let bareURLPattern = #/^(?:https?://[^\s<]+|www\.[a-zA-Z0-9\-][^\s<]*)/#
        .ignoresCase()
    private nonisolated(unsafe) static let bareURLValidPattern = #/^(?:https?://|www\.)[^./\s]+(?:\.[^./\s]+)*/#
        .ignoresCase()
    private nonisolated(unsafe) static let bareURLSchemeOnlyPattern = #/^https?://$/#.ignoresCase()
    private nonisolated(unsafe) static let wwwPattern = /^www\./.ignoresCase()

    private static let entities: [String: String] = [
        "amp": "&", "lt": "<", "gt": ">", "quot": "\"", "apos": "'", "nbsp": "\u{00A0}",
        "copy": "\u{00A9}", "reg": "\u{00AE}", "trade": "\u{2122}", "hellip": "\u{2026}", "mdash": "\u{2014}",
        "ndash": "\u{2013}", "lsquo": "\u{2018}", "rsquo": "\u{2019}", "ldquo": "\u{201C}", "rdquo": "\u{201D}",
    ]

    /// A run of `*`, `_` or `~` waiting to be matched.
    private final class Delimiter {
        let character: Character
        var count: Int
        let original: Int
        let canOpen: Bool
        let canClose: Bool

        init(character: Character, count: Int, canOpen: Bool, canClose: Bool) {
            self.character = character
            self.count = count
            self.original = count
            self.canOpen = canOpen
            self.canClose = canClose
        }
    }

    private enum Node {
        case inline(MonarchAiMarkdownInline)
        case delimiter(Delimiter)
    }

    private static func isASCIIPunctuation(_ character: Character) -> Bool {
        guard let ascii = character.asciiValue else { return false }
        return (0x21...0x2F).contains(ascii) || (0x3A...0x40).contains(ascii) || (0x5B...0x60).contains(ascii)
            || (0x7B...0x7E).contains(ascii)
    }

    private static func isPunctuation(_ character: Character) -> Bool {
        character.isPunctuation || character.isSymbol
    }

    private static func isSpace(_ character: Character) -> Bool {
        character.isWhitespace || character == "\u{FEFF}"
    }

    private static func codePoint(_ value: Int) -> String {
        guard (1...0x10FFFF).contains(value), !(0xD800...0xDFFF).contains(value),
            let scalar = Unicode.Scalar(value)
        else { return "\u{FFFD}" }
        return String(Character(scalar))
    }

    /// Removes the backslash before escaped ASCII punctuation.
    static func unescape(_ text: String) -> String {
        var result = ""
        var iterator = Array(text)[...]
        while let character = iterator.popFirst() {
            if character == "\\", let next = iterator.first, isASCIIPunctuation(next) {
                result.append(next)
                iterator = iterator.dropFirst()
            } else {
                result.append(character)
            }
        }
        return result
    }

    private static func string(_ source: [Character], _ from: Int, _ to: Int) -> String {
        String(source[max(0, from)..<min(source.count, max(from, to))])
    }

    private static func character(_ source: [Character], _ index: Int) -> Character? {
        index >= 0 && index < source.count ? source[index] : nil
    }

    private static func index(of needle: String, in source: [Character], from: Int) -> Int? {
        let pattern = Array(needle)
        guard !pattern.isEmpty, from >= 0, source.count >= pattern.count else { return nil }
        var index = from
        while index <= source.count - pattern.count {
            if Array(source[index..<index + pattern.count]) == pattern { return index }
            index += 1
        }
        return nil
    }

    private static func findBacktickRun(_ source: [Character], from: Int, run: Int) -> Int? {
        var index = from
        while index < source.count {
            if source[index] == "`" {
                var length = 1
                while character(source, index + length) == "`" { length += 1 }
                if length == run { return index }
                index += length
            } else {
                index += 1
            }
        }
        return nil
    }

    private static func findClosingBracket(_ source: [Character], open: Int) -> Int? {
        var depth = 0
        var index = open
        while index < source.count {
            switch source[index] {
            case "\\":
                index += 1
            case "`":
                var run = 1
                while character(source, index + run) == "`" { run += 1 }
                if let close = findBacktickRun(source, from: index + run, run: run) {
                    index = close + run - 1
                } else {
                    index += run - 1
                }
            case "[":
                depth += 1
            case "]":
                depth -= 1
                if depth == 0 { return index }
            default:
                break
            }
            index += 1
        }
        return nil
    }

    private struct ParsedLink {
        let text: String
        let href: String
        let title: String?
        let end: Int
    }

    // A port of the web parser: the structure is kept so the two can be compared.
    private static func parseLink(_ source: [Character], open: Int) -> ParsedLink? {
        guard let close = findClosingBracket(source, open: open), character(source, close + 1) == "(" else {
            return nil
        }
        var index = close + 2
        func skip() {
            while index < source.count, isSpace(source[index]) { index += 1 }
        }
        skip()
        let href: String
        if character(source, index) == "<" {
            guard let end = source[index...].firstIndex(of: ">"), !source[index..<end].contains("\n") else {
                return nil
            }
            href = string(source, index + 1, end)
            index = end + 1
        } else {
            let begin = index
            var depth = 0
            while index < source.count {
                let current = source[index]
                if current == "\\", index + 1 < source.count {
                    index += 2
                    continue
                }
                if isSpace(current) { break }
                if current == "(" { depth += 1 }
                if current == ")" {
                    if depth == 0 { break }
                    depth -= 1
                }
                index += 1
            }
            href = string(source, begin, index)
        }
        skip()
        var title: String?
        if let quote = character(source, index), quote == "\"" || quote == "'" || quote == "(" {
            let endCharacter: Character = quote == "(" ? ")" : quote
            guard index + 1 <= source.count, let end = source[(index + 1)...].firstIndex(of: endCharacter) else {
                return nil
            }
            title = unescape(string(source, index + 1, end))
            index = end + 1
            skip()
        }
        guard character(source, index) == ")" else { return nil }
        return ParsedLink(text: string(source, open + 1, close), href: unescape(href), title: title, end: index + 1)
    }

    private static func trimBareURL(_ url: String) -> String {
        var result = url
        while let last = result.last {
            if "?!.,:*_~'\";".contains(last) {
                result.removeLast()
            } else if last == ")", result.filter({ $0 == "(" }).count < result.filter({ $0 == ")" }).count {
                result.removeLast()
            } else {
                break
            }
        }
        return result
    }

    /// Parses inline Markdown into tokens. `links` is false inside link text: links do not nest.
    static func parse(_ text: String, links: Bool = true) -> [MonarchAiMarkdownInline] {
        let source = Array(text)
        var nodes: [Node] = []
        var buffer = ""
        func flush() {
            if !buffer.isEmpty { nodes.append(.inline(.text(buffer))) }
            buffer = ""
        }
        var index = 0
        while index < source.count {
            let current = source[index]

            if current == "\\" {
                let next = character(source, index + 1)
                // `\(…\)` is inline maths; without its closer the backslash is an ordinary escape.
                if next == "(", let close = Self.index(of: "\\)", in: source, from: index + 2) {
                    let value = string(source, index + 2, close).trimmingCharacters(in: .whitespacesAndNewlines)
                    if !value.isEmpty, !value.contains("\n\n") {
                        flush()
                        nodes.append(.inline(.math(value)))
                        index = close + 2
                        continue
                    }
                }
                if next == "\n" {
                    flush()
                    nodes.append(.inline(.lineBreak))
                    index += 2
                    continue
                }
                if let next, isASCIIPunctuation(next) {
                    buffer.append(next)
                    index += 2
                    continue
                }
                buffer.append(current)
                index += 1
                continue
            }

            // `$$…$$` inside a line. A single `$` is never maths: it is usually money.
            if current == "$", character(source, index + 1) == "$",
                let close = Self.index(of: "$$", in: source, from: index + 2)
            {
                let value = string(source, index + 2, close).trimmingCharacters(in: .whitespacesAndNewlines)
                if !value.isEmpty, !value.contains("\n\n") {
                    flush()
                    nodes.append(.inline(.math(value)))
                    index = close + 2
                    continue
                }
            }

            if current == "`" {
                var run = 1
                while character(source, index + run) == "`" { run += 1 }
                if let close = findBacktickRun(source, from: index + run, run: run) {
                    flush()
                    var value = string(source, index + run, close).replacingOccurrences(of: "\n", with: " ")
                    if value.count > 1, value.hasPrefix(" "), value.hasSuffix(" "),
                        !value.allSatisfy({ $0 == " " })
                    {
                        value = String(value.dropFirst().dropLast())
                    }
                    nodes.append(.inline(.code(value)))
                    index = close + run
                } else {
                    buffer += String(repeating: "`", count: run)
                    index += run
                }
                continue
            }

            if current == "\n" {
                let spaces = buffer.reversed().prefix { $0 == " " }.count
                buffer.removeLast(spaces)
                if spaces >= 2 {
                    flush()
                    nodes.append(.inline(.lineBreak))
                } else {
                    buffer.append("\n")
                }
                index += 1
                while character(source, index) == " " || character(source, index) == "\t" { index += 1 }
                continue
            }

            if current == "!", character(source, index + 1) == "[" {
                if let link = parseLink(source, open: index + 1) {
                    flush()
                    let alt = MonarchAiMarkdownText.inline(parse(link.text, links: false))
                    nodes.append(.inline(.image(src: link.href, alt: alt, title: link.title)))
                    index = link.end
                    continue
                }
                buffer.append(current)
                index += 1
                continue
            }

            if current == "[" {
                let window = string(source, index, index + footnoteWindow)
                if let reference = window.firstMatch(of: footnoteReferencePattern) {
                    flush()
                    let label = MonarchAiMarkdownParser.normalizeLabel(String(reference.output.1))
                    nodes.append(.inline(.footnoteReference(label)))
                    index += reference.output.0.count
                    continue
                }
                if links, let link = parseLink(source, open: index) {
                    flush()
                    let children = parse(link.text, links: false)
                    nodes.append(
                        .inline(.link(href: link.href, title: link.title, autolink: false, children: children))
                    )
                    index = link.end
                    continue
                }
                buffer.append(current)
                index += 1
                continue
            }

            if current == "<", links {
                let window = string(source, index, index + autolinkWindow)
                var autolink: (value: String, length: Int, isEmail: Bool)?
                if let uri = window.firstMatch(of: autolinkURIPattern) {
                    autolink = (String(uri.output.1), uri.output.0.count, false)
                } else if let email = window.firstMatch(of: autolinkEmailPattern) {
                    autolink = (String(email.output.1), email.output.0.count, true)
                }
                if let autolink {
                    flush()
                    nodes.append(
                        .inline(
                            .link(
                                href: autolink.isEmail ? "mailto:" + autolink.value : autolink.value,
                                title: nil,
                                autolink: true,
                                children: [.text(autolink.value)]
                            )
                        )
                    )
                    index += autolink.length
                    continue
                }
            }

            if current == "*" || current == "_" || current == "~" {
                var run = 1
                while character(source, index + run) == current { run += 1 }
                let before = character(source, index - 1) ?? " "
                let after = character(source, index + run) ?? " "
                let leftFlanking =
                    !isSpace(after) && (!isPunctuation(after) || isSpace(before) || isPunctuation(before))
                let rightFlanking =
                    !isSpace(before) && (!isPunctuation(before) || isSpace(after) || isPunctuation(after))
                var canOpen = leftFlanking
                var canClose = rightFlanking
                if current == "_" {
                    canOpen = leftFlanking && (!rightFlanking || isPunctuation(before))
                    canClose = rightFlanking && (!leftFlanking || isPunctuation(after))
                }
                if current == "~", run > 2 {
                    buffer += String(repeating: current, count: run)
                } else {
                    flush()
                    nodes.append(
                        .delimiter(Delimiter(character: current, count: run, canOpen: canOpen, canClose: canClose))
                    )
                }
                index += run
                continue
            }

            if current == "&" {
                let window = string(source, index, index + entityWindow)
                if let match = window.firstMatch(of: entityPattern) {
                    var value: String?
                    if let decimal = match.output.1, let number = Int(decimal) {
                        value = codePoint(number)
                    } else if let hex = match.output.2, let number = Int(hex, radix: 16) {
                        value = codePoint(number)
                    } else if let name = match.output.3 {
                        value = entities[String(name)]
                    }
                    if let value {
                        buffer += value
                        index += match.output.0.count
                        continue
                    }
                }
            }

            if links, current == "h" || current == "H" || current == "w" || current == "W" {
                let before = character(source, index - 1) ?? " "
                if isSpace(before) || "*_~(".contains(before) {
                    let window = string(source, index, index + autolinkWindow)
                    if let match = window.firstMatch(of: bareURLPattern) {
                        let url = trimBareURL(String(match.output))
                        if url.firstMatch(of: bareURLValidPattern) != nil,
                            url.firstMatch(of: bareURLSchemeOnlyPattern) == nil
                        {
                            flush()
                            let href = url.firstMatch(of: wwwPattern) != nil ? "https://" + url : url
                            nodes.append(.inline(.link(href: href, title: nil, autolink: true, children: [.text(url)])))
                            index += url.count
                            continue
                        }
                    }
                }
            }

            buffer.append(current)
            index += 1
        }
        flush()
        return finalize(processEmphasis(nodes))
    }

    /// CommonMark's "process emphasis" over the delimiter runs, closest opener first.
    private static func processEmphasis(_ input: [Node]) -> [Node] {
        var nodes = input
        var index = 0
        while index < nodes.count {
            guard case .delimiter(let closer) = nodes[index], closer.canClose, closer.count > 0 else {
                index += 1
                continue
            }
            var matched = false
            var opening = index - 1
            while opening >= 0 {
                defer { opening -= 1 }
                guard case .delimiter(let opener) = nodes[opening], opener.character == closer.character,
                    opener.canOpen, opener.count > 0
                else { continue }
                if closer.character != "~", opener.canClose || closer.canOpen,
                    (opener.original + closer.original) % ruleOfThree == 0,
                    !(opener.original % ruleOfThree == 0 && closer.original % ruleOfThree == 0)
                {
                    continue
                }
                if closer.character == "~", opener.count != closer.count { continue }
                let use: Int
                let kind: MonarchAiMarkdownEmphasis
                if closer.character == "~" {
                    use = opener.count
                    kind = .strikethrough
                } else if opener.count >= strongRun, closer.count >= strongRun {
                    use = strongRun
                    kind = .strong
                } else {
                    use = 1
                    kind = .emphasis
                }
                opener.count -= use
                closer.count -= use
                let inner = finalize(Array(nodes[(opening + 1)..<index]))
                nodes.replaceSubrange((opening + 1)..<index, with: [.inline(.styled(kind, inner))])
                index = opening + 2
                matched = true
                break
            }
            if !matched { index += 1 }
        }
        return nodes
    }

    private static func finalize(_ nodes: [Node]) -> [MonarchAiMarkdownInline] {
        var out: [MonarchAiMarkdownInline] = []
        for node in nodes {
            let next: MonarchAiMarkdownInline
            switch node {
            case .delimiter(let delimiter):
                guard delimiter.count > 0 else { continue }
                next = .text(String(repeating: delimiter.character, count: delimiter.count))
            case .inline(let inline):
                next = inline
            }
            if case .text(let value) = next, case .text(let previous) = out.last {
                out[out.count - 1] = .text(previous + value)
            } else {
                out.append(next)
            }
        }
        return out
    }
}
