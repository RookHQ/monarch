import Foundation

/// Best-effort parsing of JSON that is still arriving, such as a
/// component's props while they stream: open strings, arrays and objects are
/// closed, and a dangling key, colon, comma or unfinished literal is dropped.
/// Never throws.
enum MonarchAiPartialJSON {
    /// Complete JSON, or the longest sensible reading of a prefix of it. `nil` when nothing can be read.
    static func parse(_ text: String) -> MonarchAiJSON? {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return MonarchAiJSON?.none }
        if let value = decode(trimmed) { return value }
        let characters = Array(trimmed)
        // Shorter and shorter readings: the whole text, then up to each comma
        // or just after each opening bracket, from the end.
        var candidates = [characters.count]
        candidates += cutPoints(characters).reversed()
        for end in candidates {
            if let closed = close(characters[..<end]), let value = decode(closed) {
                return value
            }
        }
        return MonarchAiJSON?.none
    }

    private static func decode(_ text: String) -> MonarchAiJSON? {
        try? JSONDecoder().decode(MonarchAiJSON.self, from: Data(text.utf8))
    }

    /// Where the text can be cut: before each comma, and after each `{` or `[`, outside strings.
    private static func cutPoints(_ characters: [Character]) -> [Int] {
        var points: [Int] = []
        var inString = false
        var escaped = false
        for (index, character) in characters.enumerated() {
            if inString {
                if escaped {
                    escaped = false
                } else if character == "\\" {
                    escaped = true
                } else if character == "\"" {
                    inString = false
                }
                continue
            }
            switch character {
            case "\"": inString = true
            case ",": points.append(index)
            case "{", "[": points.append(index + 1)
            default: break
            }
        }
        return points
    }

    /// One open container while scanning.
    private struct Level {
        let opener: Character
        var hasColon = false
    }

    /// Closes a prefix: ends an open string, completes a dangling key or colon
    /// with `null`, drops a trailing comma and closes the open containers.
    private static func close(_ prefix: ArraySlice<Character>) -> String? {
        var stack: [Level] = []
        var inString = false
        var escaped = false
        for character in prefix {
            if inString {
                if escaped {
                    escaped = false
                } else if character == "\\" {
                    escaped = true
                } else if character == "\"" {
                    inString = false
                }
                continue
            }
            switch character {
            case "\"": inString = true
            case "{", "[": stack.append(Level(opener: character))
            case "}", "]": if !stack.isEmpty { stack.removeLast() }
            case ":": if !stack.isEmpty { stack[stack.count - 1].hasColon = true }
            case ",": if !stack.isEmpty { stack[stack.count - 1].hasColon = false }
            default: break
            }
        }
        var head = String(prefix)
        if inString {
            if escaped { head.removeLast() }
            head += "\""
        }
        while let last = head.last, last.isWhitespace { head.removeLast() }
        if head.last == "," { head.removeLast() }
        if let top = stack.last, top.opener == "{" {
            if head.last == ":" {
                head += "null"
            } else if !top.hasColon, head.last == "\"" {
                head += ":null"
            }
        }
        guard !head.isEmpty else { return nil }
        for level in stack.reversed() {
            head.append(level.opener == "{" ? "}" : "]")
        }
        return head
    }
}
