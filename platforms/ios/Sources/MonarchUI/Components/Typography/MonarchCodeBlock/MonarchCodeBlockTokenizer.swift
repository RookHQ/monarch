/// A deliberately small syntax tokenizer, ported from the web CodeBlock:
/// keywords, strings, comments and numbers, plus tags, attributes and
/// properties for markup, JSON and CSS. It is not a grammar; it only makes
/// common snippets easier to scan. Unknown languages come back as one plain
/// token.
enum MonarchCodeBlockTokenizer {
    /// The kind of a token, which picks its colour.
    enum Kind: Sendable, Hashable, CaseIterable {
        case plain
        case keyword
        case string
        case comment
        case number
        case tag
        case attr
        case property
    }

    /// A run of text of one kind.
    struct Token: Sendable, Hashable {
        var kind: Kind
        var text: String
    }

    private struct CLike {
        var keywords: Set<String>
        var caseInsensitive = false
        var line: [String] = []
        var block: [(open: String, close: String)] = []
        var strings: [String]
        /// Strings that may span lines (template literals, triple quotes).
        var multiline: [String] = []
        /// Marks `"key":` strings as properties (JSON).
        var keys = false
        /// Marks `name:` inside braces as properties (CSS).
        var cssProps = false
        /// `#` starts a comment only at a line start or after white space (shell).
        var hashAfterSpace = false
        /// `'` is a character literal only when closed within 4 characters (Rust lifetimes).
        var shortSingle = false
    }

    private enum Grammar {
        case clike(CLike)
        case markup
    }

    private static func words(_ text: String) -> Set<String> {
        Set(text.split(whereSeparator: \.isWhitespace).map(String.init))
    }

    private static let jsKeywords = words(
        """
        abstract any as async await boolean break case catch class const constructor continue debugger declare
        default delete do else enum export extends false finally for from function get if implements import in
        infer instanceof interface is keyof let namespace never new null number of private protected public
        readonly return satisfies set static string super switch symbol this throw true try type typeof
        undefined unknown var void while with yield
        """
    )

    private static let grammars: [String: Grammar] = [
        "js": .clike(
            CLike(
                keywords: jsKeywords,
                line: ["//"],
                block: [("/*", "*/")],
                strings: ["'", "\""],
                multiline: ["`"]
            )
        ),
        "json": .clike(CLike(keywords: words("true false null"), strings: ["\""], keys: true)),
        "python": .clike(
            CLike(
                keywords: words(
                    """
                    False None True and as assert async await break class continue def del elif else except
                    finally for from global if import in is lambda match case nonlocal not or pass raise return
                    self try while with yield
                    """
                ),
                line: ["#"],
                strings: ["'", "\""],
                multiline: ["\"\"\"", "'''"]
            )
        ),
        "bash": .clike(
            CLike(
                keywords: words(
                    """
                    if then else elif fi for while until do done case esac function in return local export
                    readonly declare unset shift source exit echo cd set true false
                    """
                ),
                line: ["#"],
                strings: ["'", "\""],
                hashAfterSpace: true
            )
        ),
        "css": .clike(
            CLike(
                keywords: words("important inherit initial unset revert none auto"),
                block: [("/*", "*/")],
                strings: ["'", "\""],
                cssProps: true
            )
        ),
        "html": .markup,
        "sql": .clike(
            CLike(
                keywords: words(
                    """
                    select from where and or not null is in as on join left right inner outer full cross group by
                    order having limit offset insert into values update set delete create table alter drop index
                    view primary key foreign references default unique distinct union all case when then else end
                    exists like between returning with asc desc count sum avg min max begin commit rollback
                    transaction true false
                    """
                ),
                caseInsensitive: true,
                line: ["--"],
                block: [("/*", "*/")],
                strings: ["'"]
            )
        ),
        "go": .clike(
            CLike(
                keywords: words(
                    """
                    break case chan const continue default defer else fallthrough for func go goto if import
                    interface map package range return select struct switch type var nil true false iota error
                    string int bool byte rune float64 any
                    """
                ),
                line: ["//"],
                block: [("/*", "*/")],
                strings: ["\"", "'"],
                multiline: ["`"]
            )
        ),
        "rust": .clike(
            CLike(
                keywords: words(
                    """
                    as async await break const continue crate dyn else enum extern false fn for if impl in let
                    loop match mod move mut pub ref return self Self static struct super trait true type unsafe
                    use where while Some None Ok Err String Vec Option Result bool u8 u16 u32 u64 usize i8 i16
                    i32 i64 isize f32 f64 str
                    """
                ),
                line: ["//"],
                block: [("/*", "*/")],
                strings: ["\""],
                shortSingle: true
            )
        ),
    ]

    private static let aliases: [String: String] = [
        "javascript": "js", "jsx": "js", "mjs": "js", "cjs": "js", "ts": "js", "typescript": "js", "tsx": "js",
        "jsonc": "json", "py": "python", "sh": "bash", "shell": "bash", "zsh": "bash", "console": "bash",
        "scss": "css", "xml": "html", "svg": "html", "vue": "html", "postgres": "sql", "postgresql": "sql",
        "mysql": "sql", "sqlite": "sql", "golang": "go", "rs": "rust",
    ]

    /// The grammar id used for `language`, or `nil` when there is none.
    static func resolve(_ language: String?) -> String? {
        let id = (language ?? "").trimmingCharacters(in: .whitespaces).lowercased()
        guard !id.isEmpty else { return nil }
        let resolved = aliases[id] ?? id
        return grammars[resolved] == nil ? nil : resolved
    }

    /// Splits code into tokens for `language`. Unknown languages are one plain token.
    static func tokenize(_ code: String, language: String?) -> [Token] {
        let chars = Array(normalized(code))
        guard let id = resolve(language), let grammar = grammars[id] else {
            return chars.isEmpty ? [] : [Token(kind: .plain, text: String(chars))]
        }
        var scanner = Scanner(chars: chars)
        switch grammar {
        case .clike(let g): scanner.clike(g)
        case .markup: scanner.markup()
        }
        return scanner.out
    }

    /// Splits tokens at line breaks: one token list per line.
    static func lines(_ tokens: [Token]) -> [[Token]] {
        var lines: [[Token]] = [[]]
        for token in tokens {
            let parts = token.text.split(separator: "\n", omittingEmptySubsequences: false)
            for (index, part) in parts.enumerated() {
                if index > 0 { lines.append([]) }
                if !part.isEmpty { lines[lines.count - 1].append(Token(kind: token.kind, text: String(part))) }
            }
        }
        return lines
    }

    /// Code with Windows and old Mac line breaks turned into `\n`. Swift
    /// treats `\r\n` as one character, which would hide the line break.
    static func normalized(_ code: String) -> String {
        code.replacingOccurrences(of: "\r\n", with: "\n").replacingOccurrences(of: "\r", with: "\n")
    }

    // MARK: - Scanner

    private struct Scanner {
        let chars: [Character]
        var out: [Token] = []

        init(chars: [Character]) {
            self.chars = chars
        }

        private mutating func push(_ kind: Kind, _ start: Int, _ end: Int) {
            guard end > start else { return }
            push(kind, String(chars[start..<end]))
        }

        private mutating func push(_ kind: Kind, _ text: String) {
            guard !text.isEmpty else { return }
            if let last = out.indices.last, out[last].kind == kind {
                out[last].text += text
            } else {
                out.append(Token(kind: kind, text: text))
            }
        }

        private func starts(_ prefix: String, at index: Int) -> Bool {
            var i = index
            for c in prefix {
                guard i < chars.count, chars[i] == c else { return false }
                i += 1
            }
            return true
        }

        private func find(_ text: String, from index: Int) -> Int? {
            var i = index
            while i < chars.count {
                if starts(text, at: i) { return i }
                i += 1
            }
            return nil
        }

        private func endOfLine(from index: Int) -> Int {
            var i = index
            while i < chars.count, chars[i] != "\n" { i += 1 }
            return i
        }

        private func char(_ index: Int) -> Character? {
            index >= 0 && index < chars.count ? chars[index] : nil
        }

        private static func isWord(_ c: Character?) -> Bool {
            guard let c, c.isASCII else { return false }
            return c.isLetter || c.isNumber || c == "_" || c == "$"
        }

        private static func isDigit(_ c: Character?) -> Bool {
            guard let c, c.isASCII else { return false }
            return c.isNumber
        }

        private static func isHex(_ c: Character?) -> Bool {
            guard let c, c.isASCII else { return false }
            return c.isHexDigit
        }

        private static func isAsciiLetter(_ c: Character?) -> Bool {
            guard let c, c.isASCII else { return false }
            return c.isLetter
        }

        /// Whether `\s*:` follows `index`, within the web's look-ahead of 8.
        private func colonFollows(_ index: Int) -> Bool {
            var i = index
            while i < chars.count, i < index + 8 {
                if chars[i] == ":" { return true }
                if !chars[i].isWhitespace { return false }
                i += 1
            }
            return false
        }

        /// The end of a string that starts at `index` with `quote`.
        private func stringEnd(_ index: Int, quote: String, multiline: Bool) -> Int {
            var j = index + quote.count
            while j < chars.count {
                if chars[j] == "\\" {
                    j += 2
                    continue
                }
                if starts(quote, at: j) { return j + quote.count }
                if !multiline, chars[j] == "\n" { return j }
                j += 1
            }
            return chars.count
        }

        /// The end of a number at `index`, or `nil`. Mirrors the web pattern:
        /// hex, binary, or decimal with an exponent, then unit letters.
        private func numberEnd(_ index: Int) -> Int? {
            var j = index
            let lower = char(j + 1).map { Character($0.lowercased()) }
            if char(j) == "0", lower == "x", Self.isHex(char(j + 2)) || char(j + 2) == "_" {
                j += 2
                while Self.isHex(char(j)) || char(j) == "_" { j += 1 }
            } else if char(j) == "0", lower == "b", char(j + 2) == "0" || char(j + 2) == "1" || char(j + 2) == "_" {
                j += 2
                while char(j) == "0" || char(j) == "1" || char(j) == "_" { j += 1 }
            } else {
                if Self.isDigit(char(j)) {
                    while Self.isDigit(char(j)) || char(j) == "_" { j += 1 }
                    if char(j) == "." { j += 1 }
                    while Self.isDigit(char(j)) || char(j) == "_" { j += 1 }
                } else if char(j) == ".", Self.isDigit(char(j + 1)) {
                    j += 1
                    while Self.isDigit(char(j)) || char(j) == "_" { j += 1 }
                } else {
                    return nil
                }
                if char(j) == "e" || char(j) == "E" {
                    var k = j + 1
                    if char(k) == "+" || char(k) == "-" { k += 1 }
                    if Self.isDigit(char(k)) {
                        while Self.isDigit(char(k)) { k += 1 }
                        j = k
                    }
                }
            }
            while Self.isAsciiLetter(char(j)) || char(j) == "%" { j += 1 }
            let text = chars[index..<j]
            return text.contains(where: { Self.isDigit($0) }) ? j : nil
        }

        /// The end of an identifier at `index`, or `nil`.
        private func identifierEnd(_ index: Int, css: Bool) -> Int? {
            var j = index
            if css {
                var dashes = 0
                while char(j) == "-", dashes < 2 {
                    j += 1
                    dashes += 1
                }
                guard Self.isAsciiLetter(char(j)) || char(j) == "_" else { return nil }
                j += 1
                while Self.isWord(char(j)) && char(j) != "$" || char(j) == "-" { j += 1 }
                return j
            }
            guard Self.isAsciiLetter(char(j)) || char(j) == "_" || char(j) == "$" else { return nil }
            j += 1
            while Self.isWord(char(j)) { j += 1 }
            return j
        }

        mutating func clike(_ g: CLike) {
            let delimiters = (g.multiline.map { ($0, true) } + g.strings.map { ($0, false) })
                .sorted { $0.0.count > $1.0.count }
            var depth = 0
            var i = 0
            while i < chars.count {
                let prev: Character = i == 0 ? "\n" : chars[i - 1]

                if let block = g.block.first(where: { starts($0.open, at: i) }) {
                    let stop = find(block.close, from: i + block.open.count).map { $0 + block.close.count }
                    push(.comment, i, stop ?? chars.count)
                    i = stop ?? chars.count
                    continue
                }
                if g.line.contains(where: { starts($0, at: i) }), !(g.hashAfterSpace && !prev.isWhitespace) {
                    let stop = endOfLine(from: i)
                    push(.comment, i, stop)
                    i = stop
                    continue
                }
                if g.shortSingle, chars[i] == "'", let stop = shortCharEnd(i) {
                    push(.string, i, stop)
                    i = stop
                    continue
                }
                if let (quote, multiline) = delimiters.first(where: { starts($0.0, at: i) }) {
                    let stop = stringEnd(i, quote: quote, multiline: multiline)
                    push(g.keys && colonFollows(stop) ? .property : .string, i, stop)
                    i = stop
                    continue
                }
                if Self.isDigit(chars[i]) || chars[i] == ".", !Self.isWord(prev), let stop = numberEnd(i) {
                    push(.number, i, stop)
                    i = stop
                    continue
                }
                if g.cssProps, chars[i] == "#", depth > 0, let stop = hexColourEnd(i) {
                    push(.number, i, stop)
                    i = stop
                    continue
                }
                if g.cssProps, chars[i] == "@" {
                    var j = i + 1
                    while Self.isWord(char(j)) && char(j) != "$" || char(j) == "-" { j += 1 }
                    if j > i + 1 {
                        push(.keyword, i, j)
                        i = j
                        continue
                    }
                }
                if !Self.isWord(prev), let stop = identifierEnd(i, css: g.cssProps) {
                    let word = String(chars[i..<stop])
                    let key = g.caseInsensitive ? word.lowercased() : word
                    if g.cssProps, depth > 0, colonFollows(stop) {
                        push(.property, word)
                    } else {
                        push(g.keywords.contains(key) ? .keyword : .plain, word)
                    }
                    i = stop
                    continue
                }
                if chars[i] == "{" { depth += 1 }
                if chars[i] == "}" { depth = max(0, depth - 1) }
                push(.plain, i, i + 1)
                i += 1
            }
        }

        /// A Rust character literal: `'` then one to four characters, then `'`.
        private func shortCharEnd(_ index: Int) -> Int? {
            var j = index + 1
            var count = 0
            while count < 4, let c = char(j), c != "'", c != "\n" {
                j += c == "\\" ? 2 : 1
                count += 1
            }
            return count >= 1 && char(j) == "'" ? j + 1 : nil
        }

        /// A CSS hex colour: `#` then 3 to 8 hex digits at a word boundary.
        private func hexColourEnd(_ index: Int) -> Int? {
            var j = index + 1
            while Self.isHex(char(j)) { j += 1 }
            let count = j - index - 1
            guard count >= 3, count <= 8, !Self.isWord(char(j)) else { return nil }
            return j
        }

        mutating func markup() {
            var i = 0
            while i < chars.count {
                if starts("<!--", at: i) {
                    let stop = find("-->", from: i + 4).map { $0 + 3 } ?? chars.count
                    push(.comment, i, stop)
                    i = stop
                    continue
                }
                let nameStart = char(i + 1) == "/" ? i + 2 : i + 1
                if chars[i] == "<", Self.isAsciiLetter(char(nameStart)) {
                    var j = nameStart + 1
                    while let c = char(j), Self.isWord(c) && c != "$" || c == ":" || c == "." || c == "-" { j += 1 }
                    push(.tag, i, j)
                    i = j
                    while i < chars.count, chars[i] != ">" {
                        if starts("/>", at: i) { break }
                        let c = chars[i]
                        if c == "\"" || c == "'" {
                            let stop = stringEnd(i, quote: String(c), multiline: true)
                            push(.string, i, stop)
                            i = stop
                            continue
                        }
                        var k = i
                        while let a = char(k), !a.isWhitespace, !"=>\"'/".contains(a) { k += 1 }
                        if k > i {
                            push(.attr, i, k)
                            i = k
                            continue
                        }
                        push(.plain, i, i + 1)
                        i += 1
                    }
                    if starts("/>", at: i) {
                        push(.tag, "/>")
                        i += 2
                    } else if char(i) == ">" {
                        push(.tag, ">")
                        i += 1
                    }
                    continue
                }
                push(.plain, i, i + 1)
                i += 1
            }
        }
    }
}
