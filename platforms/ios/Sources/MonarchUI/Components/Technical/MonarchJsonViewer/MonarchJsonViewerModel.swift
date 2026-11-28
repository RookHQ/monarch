import Foundation

/// One step in a JSON path: an object key or an array index.
enum MonarchJsonViewerSegment: Hashable, Sendable {
    case key(String)
    case index(Int)
}

/// The kind of a value, which decides its colour and summary.
enum MonarchJsonViewerType: Hashable, Sendable {
    case object, array, string, number, bool, null
}

/// One visible row of the flattened tree.
struct MonarchJsonViewerRow: Identifiable, Equatable {
    enum Kind: Equatable {
        /// A value in the document.
        case node
        /// The "Show more" row after the shown children of a container.
        case more
    }

    let kind: Kind
    /// The path of the node (for `more`, the path of its container followed by `#more`).
    let id: String
    let segments: [MonarchJsonViewerSegment]
    let parentID: String?
    let name: MonarchJsonViewerSegment?
    let value: MonarchJsonValue
    let depth: Int
    /// The number of children of a container.
    let count: Int
    /// For `more`: how many children are shown before it.
    let shown: Int
    /// For `more`: how many children are still hidden.
    let remaining: Int

    var type: MonarchJsonViewerType { MonarchJsonViewerModel.type(of: value) }

    /// Whether the row opens and closes.
    var isExpandable: Bool { kind == .node && count > 0 }
}

/// What a search found.
struct MonarchJsonViewerSearch: Equatable {
    /// The ids of the matching nodes.
    var matched: Set<String> = []
    /// The ids of containers opened to show a match.
    var open: Set<String> = []
    /// For each container, how many children must be shown to reach the last match.
    var reveal: [String: Int] = [:]
    var count = 0
    var isCapped = false

    static let empty = MonarchJsonViewerSearch()
}

/// The pure logic of ``MonarchJsonViewer``: paths, the flattened rows, the
/// search and the text of values. Only open containers are walked, so the
/// cost follows what is visible, not the document size.
enum MonarchJsonViewerModel {
    /// The internal root of every id. Shown paths use the viewer's `rootPath`.
    static let idRoot = "$"
    /// The suffix of the id of a "Show more" row.
    static let moreSuffix = "#more"

    // MARK: Paths

    private static func isIdentifier(_ key: String) -> Bool {
        guard let first = key.unicodeScalars.first else { return false }
        func isLetter(_ scalar: Unicode.Scalar) -> Bool {
            scalar.isASCII
                && (("a"..."z").contains(scalar) || ("A"..."Z").contains(scalar) || scalar == "_" || scalar == "$")
        }
        guard isLetter(first) else { return false }
        return key.unicodeScalars.dropFirst().allSatisfy { isLetter($0) || ("0"..."9").contains($0) }
    }

    /// A path as JSONPath-style text: `$.users[0].name`, with keys that are
    /// not identifiers quoted (`$["first name"]`). With an empty root the
    /// first key has no dot (`users[0].name`).
    static func path(_ segments: [MonarchJsonViewerSegment], root: String) -> String {
        var out = root
        for segment in segments {
            switch segment {
            case .index(let index):
                out += "[\(index)]"
            case .key(let key) where isIdentifier(key):
                out += out.isEmpty ? key : ".\(key)"
            case .key(let key):
                out += "[\(quoted(key))]"
            }
        }
        return out
    }

    /// The internal id of a path.
    static func id(_ segments: [MonarchJsonViewerSegment]) -> String {
        path(segments, root: idRoot)
    }

    // MARK: Values

    /// The kind of a value.
    static func type(of value: MonarchJsonValue) -> MonarchJsonViewerType {
        switch value {
        case .object: .object
        case .array: .array
        case .string: .string
        case .number: .number
        case .bool: .bool
        case .null: .null
        }
    }

    /// The children of a container with their path segments.
    static func children(_ value: MonarchJsonValue) -> [(MonarchJsonViewerSegment, MonarchJsonValue)] {
        switch value {
        case .object(let members): members.map { (.key($0.key), $0.value) }
        case .array(let elements): elements.enumerated().map { (.index($0.offset), $0.element) }
        default: []
        }
    }

    /// The number of children of a container, 0 for anything else.
    static func childCount(_ value: MonarchJsonValue) -> Int {
        switch value {
        case .object(let members): members.count
        case .array(let elements): elements.count
        default: 0
        }
    }

    /// The text searched for a primitive: the string without quotes, the
    /// number as written, `true`, `false` or `null`. `nil` for containers.
    static func searchableText(_ value: MonarchJsonValue) -> String? {
        switch value {
        case .string(let text): text
        case .number(let text): text
        case .bool(let flag): flag ? "true" : "false"
        case .null: "null"
        case .object, .array: nil
        }
    }

    /// The text of a segment as shown before the colon.
    static func name(_ segment: MonarchJsonViewerSegment) -> String {
        switch segment {
        case .key(let key): key
        case .index(let index): String(index)
        }
    }

    /// A string as a JSON string literal.
    static func quoted(_ text: String) -> String {
        var out = "\""
        for scalar in text.unicodeScalars {
            switch scalar {
            case "\"": out += "\\\""
            case "\\": out += "\\\\"
            case "\n": out += "\\n"
            case "\r": out += "\\r"
            case "\t": out += "\\t"
            case "\u{08}": out += "\\b"
            case "\u{0C}": out += "\\f"
            case _ where scalar.value < 0x20:
                let hex = String(scalar.value, radix: 16)
                out += "\\u" + String(repeating: "0", count: 4 - hex.count) + hex
            default: out.unicodeScalars.append(scalar)
            }
        }
        return out + "\""
    }

    /// JSON text of a value, indented by two spaces, keys in order.
    static func stringify(_ value: MonarchJsonValue, indent: Int = 0) -> String {
        let pad = String(repeating: " ", count: indent + 2)
        let close = String(repeating: " ", count: indent)
        switch value {
        case .object(let members):
            guard !members.isEmpty else { return "{}" }
            let lines = members.map { pad + quoted($0.key) + ": " + stringify($0.value, indent: indent + 2) }
            return "{\n" + lines.joined(separator: ",\n") + "\n" + close + "}"
        case .array(let elements):
            guard !elements.isEmpty else { return "[]" }
            let lines = elements.map { pad + stringify($0, indent: indent + 2) }
            return "[\n" + lines.joined(separator: ",\n") + "\n" + close + "]"
        case .string(let text): return quoted(text)
        case .number(let text): return text
        case .bool(let flag): return flag ? "true" : "false"
        case .null: return "null"
        }
    }

    /// What Copy value copies: a string's own text, the JSON text of anything else.
    static func copyText(_ value: MonarchJsonValue) -> String {
        if case .string(let text) = value { return text }
        return stringify(value)
    }

    // MARK: Search

    /// Walks the whole document once and finds the nodes whose key or
    /// primitive value contains `needle` (already lower-cased and trimmed),
    /// stopping after `maxMatches`.
    static func search(_ value: MonarchJsonValue, needle: String, maxMatches: Int) -> MonarchJsonViewerSearch {
        var result = MonarchJsonViewerSearch()
        guard !needle.isEmpty else { return result }
        func walk(_ value: MonarchJsonValue, _ segments: [MonarchJsonViewerSegment]) -> Bool {
            if result.isCapped { return false }
            let nodeID = Self.id(segments)
            var hit = segments.last.map { name($0).lowercased().contains(needle) } ?? false
            if !hit, let text = searchableText(value) { hit = text.lowercased().contains(needle) }
            if hit {
                result.matched.insert(nodeID)
                result.count += 1
                if result.count >= maxMatches { result.isCapped = true }
            }
            var childHit = false
            for (offset, child) in children(value).enumerated() {
                if walk(child.1, segments + [child.0]) {
                    childHit = true
                    result.reveal[nodeID] = max(result.reveal[nodeID] ?? 0, offset + 1)
                }
                if result.isCapped { break }
            }
            if childHit { result.open.insert(nodeID) }
            return hit || childHit
        }
        _ = walk(value, [])
        return result
    }

    // MARK: Rows

    /// The visible rows: the root, and the children of every open container
    /// up to its limit, followed by a "Show more" row when some are hidden.
    static func rows(
        _ value: MonarchJsonValue,
        isOpen: (String, Int) -> Bool,
        limits: [String: Int],
        maxChildren: Int,
        reveal: [String: Int]
    ) -> [MonarchJsonViewerRow] {
        var out: [MonarchJsonViewerRow] = []
        func visit(_ value: MonarchJsonValue, _ segments: [MonarchJsonViewerSegment], parentID: String?, depth: Int) {
            let nodeID = Self.id(segments)
            let count = childCount(value)
            out.append(
                MonarchJsonViewerRow(
                    kind: .node,
                    id: nodeID,
                    segments: segments,
                    parentID: parentID,
                    name: segments.last,
                    value: value,
                    depth: depth,
                    count: count,
                    shown: 0,
                    remaining: 0
                )
            )
            guard count > 0, isOpen(nodeID, depth) else { return }
            let limit = max(limits[nodeID] ?? maxChildren, reveal[nodeID] ?? 0, 1)
            let entries = children(value)
            let shown = min(entries.count, limit)
            for (segment, child) in entries.prefix(shown) {
                visit(child, segments + [segment], parentID: nodeID, depth: depth + 1)
            }
            if shown < entries.count {
                out.append(
                    MonarchJsonViewerRow(
                        kind: .more,
                        id: nodeID + moreSuffix,
                        segments: segments,
                        parentID: nodeID,
                        name: nil,
                        value: .null,
                        depth: depth + 1,
                        count: 0,
                        shown: shown,
                        remaining: entries.count - shown
                    )
                )
            }
        }
        visit(value, [], parentID: nil, depth: 0)
        return out
    }

    /// Whether a container is open: the user's choice first, then a search
    /// that reaches inside it, then the expand depth.
    static func isOpen(
        id: String,
        depth: Int,
        overrides: [String: Bool],
        search: MonarchJsonViewerSearch,
        baseDepth: Int
    ) -> Bool {
        if let choice = overrides[id] { return choice }
        if search.open.contains(id) { return true }
        return depth < baseDepth
    }

    // MARK: Text

    /// The summary of a value: `{ } 3 keys`, `[ ] 2 items`, a quoted string
    /// cut after `maxStringLength` characters, or the primitive.
    static func summary(_ value: MonarchJsonValue, maxStringLength: Int, labels: MonarchJsonViewerLabels) -> String {
        switch value {
        case .object(let members): "{ } " + labels.keysText(members.count)
        case .array(let elements): "[ ] " + labels.itemsText(elements.count)
        case .string(let text):
            text.count > maxStringLength
                ? "\"" + String(text.prefix(maxStringLength)) + labels.truncatedText(text.count) + "\""
                : "\"" + text + "\""
        case .number(let text): text
        case .bool(let flag): flag ? "true" : "false"
        case .null: "null"
        }
    }

    /// What VoiceOver reads for a node: "key: summary", or the summary for the root.
    static func accessibilityLabel(name: MonarchJsonViewerSegment?, summary: String) -> String {
        guard let name else { return summary }
        return self.name(name) + ": " + summary
    }

    /// The ranges of `text` that contain `needle`, ignoring case.
    static func matchRanges(in text: String, needle: String) -> [Range<String.Index>] {
        guard !needle.isEmpty else { return [] }
        var ranges: [Range<String.Index>] = []
        var start = text.startIndex
        while start < text.endIndex,
            let range = text.range(of: needle, options: [.caseInsensitive], range: start..<text.endIndex)
        {
            ranges.append(range)
            start = range.upperBound
        }
        return ranges
    }

    /// The row to select after a "Show more": the first newly shown child.
    static func firstRevealedID(after row: MonarchJsonViewerRow, in value: MonarchJsonValue) -> String? {
        guard row.kind == .more else { return nil }
        let entries = children(value)
        guard row.shown < entries.count else { return nil }
        return id(row.segments + [entries[row.shown].0])
    }
}
