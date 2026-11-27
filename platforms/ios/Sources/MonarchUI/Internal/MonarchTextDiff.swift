import Foundation

/// One step of an edit script: keep, delete or insert an item.
enum MonarchDiffOperation<Value: Equatable>: Equatable {
    case equal(Value)
    case delete(Value)
    case insert(Value)
}

/// A piece of a changed line in a word diff.
struct MonarchDiffSegment: Equatable {
    let kind: MonarchDiffViewerLineKind
    var text: String
}

/// One row of the diff as drawn: a line (unified), a pair of lines (side by
/// side) or a folded run of unchanged lines. Lines are indexes into the diff.
enum MonarchDiffRow: Equatable {
    /// A line in the unified layout; `hunk` is set on the first line of a change.
    case line(Int, hunk: Int?)
    /// A row of the side by side layout; either side may be empty.
    case pair(old: Int?, new: Int?, hunk: Int?)
    /// A folded run of unchanged lines with its own id.
    case gap(id: Int, lines: [Int])
}

/// The line and word diff of ``MonarchDiffViewer``, the version history
/// panel and the AI edit proposal, ported from the web: a
/// longest common subsequence over lines, then over words inside changed lines.
enum MonarchTextDiff {
    /// Above this many line pairs the diff removes every line and adds every line.
    static let maxLinePairs = 1_000_000
    /// Above this many word pairs a changed line is not split into words.
    static let maxWordPairs = 250_000

    /// The edit script from `a` to `b` along a longest common subsequence.
    /// When a deletion and an insertion are both optimal, the deletion comes first.
    static func sequences<Value: Equatable>(
        _ a: [Value],
        _ b: [Value],
        maxPairs: Int = .max
    ) -> [MonarchDiffOperation<Value>] {
        let n = a.count
        let m = b.count
        if n.multipliedReportingOverflow(by: m).partialValue > maxPairs {
            return a.map { .delete($0) } + b.map { .insert($0) }
        }
        let width = m + 1
        var lcs = [UInt32](repeating: 0, count: (n + 1) * width)
        if n > 0 && m > 0 {
            for i in stride(from: n - 1, through: 0, by: -1) {
                for j in stride(from: m - 1, through: 0, by: -1) {
                    lcs[i * width + j] =
                        a[i] == b[j]
                        ? lcs[(i + 1) * width + j + 1] + 1
                        : max(lcs[(i + 1) * width + j], lcs[i * width + j + 1])
                }
            }
        }
        var result: [MonarchDiffOperation<Value>] = []
        var i = 0
        var j = 0
        while i < n && j < m {
            if a[i] == b[j] {
                result.append(.equal(a[i]))
                i += 1
                j += 1
            } else if lcs[(i + 1) * width + j] >= lcs[i * width + j + 1] {
                result.append(.delete(a[i]))
                i += 1
            } else {
                result.append(.insert(b[j]))
                j += 1
            }
        }
        while i < n {
            result.append(.delete(a[i]))
            i += 1
        }
        while j < m {
            result.append(.insert(b[j]))
            j += 1
        }
        return result
    }

    /// The lines of a text, split on `\n`, `\r\n` or `\r`. An empty text has no lines.
    static func split(_ text: String) -> [String] {
        guard !text.isEmpty else { return [] }
        let normalized = text.replacingOccurrences(of: "\r\n", with: "\n").replacingOccurrences(of: "\r", with: "\n")
        return normalized.split(separator: "\n", omittingEmptySubsequences: false).map(String.init)
    }

    /// The numbered line diff from `before` to `after`.
    static func lines(_ before: [String], _ after: [String]) -> [MonarchDiffViewerLine] {
        var old = 0
        var new = 0
        return sequences(before, after, maxPairs: maxLinePairs).map { operation in
            switch operation {
            case .equal(let text):
                old += 1
                new += 1
                return MonarchDiffViewerLine(.same, text: text, oldNumber: old, newNumber: new)
            case .delete(let text):
                old += 1
                return MonarchDiffViewerLine(.removed, text: text, oldNumber: old)
            case .insert(let text):
                new += 1
                return MonarchDiffViewerLine(.added, text: text, newNumber: new)
            }
        }
    }

    /// The units of a word diff: runs of white space, runs of letters, digits
    /// and underscores, and single other characters.
    static func tokens(_ text: String) -> [String] {
        enum Class { case space, word, other }
        func classify(_ character: Character) -> Class {
            if character.isWhitespace { return .space }
            if character.isLetter || character.isNumber || character == "_" { return .word }
            return .other
        }
        var tokens: [String] = []
        var current = ""
        var currentClass: Class?
        for character in text {
            let kind = classify(character)
            if kind != .other, kind == currentClass {
                current.append(character)
                continue
            }
            if !current.isEmpty { tokens.append(current) }
            current = String(character)
            currentClass = kind
        }
        if !current.isEmpty { tokens.append(current) }
        return tokens
    }

    /// The word diff of two lines, with neighbouring pieces of one kind merged.
    static func words(_ before: String, _ after: String) -> [MonarchDiffSegment] {
        var segments: [MonarchDiffSegment] = []
        for operation in sequences(tokens(before), tokens(after), maxPairs: maxWordPairs) {
            let (kind, text): (MonarchDiffViewerLineKind, String) =
                switch operation {
                case .equal(let text): (.same, text)
                case .delete(let text): (.removed, text)
                case .insert(let text): (.added, text)
                }
            if let last = segments.last, last.kind == kind {
                segments[segments.count - 1].text += text
            } else {
                segments.append(MonarchDiffSegment(kind: kind, text: text))
            }
        }
        return segments
    }

    /// The word segments of every changed line that pairs up with one of the
    /// other kind in its change (the k-th removed line with the k-th added
    /// line), by line index.
    static func wordSegments(_ lines: [MonarchDiffViewerLine]) -> [Int: [MonarchDiffSegment]] {
        var map: [Int: [MonarchDiffSegment]] = [:]
        var i = 0
        while i < lines.count {
            if lines[i].kind == .same {
                i += 1
                continue
            }
            var removed: [Int] = []
            var added: [Int] = []
            while i < lines.count && lines[i].kind != .same {
                if lines[i].kind == .removed { removed.append(i) } else { added.append(i) }
                i += 1
            }
            for k in 0..<min(removed.count, added.count) {
                let segments = words(lines[removed[k]].text, lines[added[k]].text)
                map[removed[k]] = segments
                map[added[k]] = segments
            }
        }
        return map
    }

    /// Added and removed line totals.
    static func stats(_ lines: [MonarchDiffViewerLine]) -> (added: Int, removed: Int) {
        (lines.count { $0.kind == .added }, lines.count { $0.kind == .removed })
    }

    /// Groups consecutive changes into numbered changes ("hunks") and folds
    /// runs of unchanged lines longer than the context around each change.
    /// A run at the start or end keeps no leading or trailing context.
    static func rows(
        _ lines: [MonarchDiffViewerLine],
        isSplit: Bool,
        context: Int,
        expanded: Set<Int>
    ) -> (rows: [MonarchDiffRow], hunks: Int) {
        var rows: [MonarchDiffRow] = []
        var hunks = 0
        var gapID = 0
        var i = 0
        func same(_ index: Int) -> MonarchDiffRow {
            isSplit ? .pair(old: index, new: index, hunk: nil) : .line(index, hunk: nil)
        }
        while i < lines.count {
            if lines[i].kind == .same {
                let start = i
                while i < lines.count && lines[i].kind == .same { i += 1 }
                let run = Array(start..<i)
                let lead = start == 0 ? 0 : context
                let trail = i == lines.count ? 0 : context
                let id = gapID
                gapID += 1
                if run.count <= lead + trail + 1 || expanded.contains(id) {
                    rows += run.map(same)
                } else {
                    rows += run.prefix(lead).map(same)
                    rows.append(.gap(id: id, lines: Array(run[lead..<(run.count - trail)])))
                    rows += run.suffix(trail).map(same)
                }
                continue
            }
            let hunk = hunks
            hunks += 1
            var removed: [Int] = []
            var added: [Int] = []
            while i < lines.count && lines[i].kind != .same {
                if lines[i].kind == .removed { removed.append(i) } else { added.append(i) }
                i += 1
            }
            if isSplit {
                for k in 0..<max(removed.count, added.count) {
                    rows.append(
                        .pair(
                            old: k < removed.count ? removed[k] : nil,
                            new: k < added.count ? added[k] : nil,
                            hunk: k == 0 ? hunk : nil
                        )
                    )
                }
            } else {
                for (k, index) in (removed + added).enumerated() {
                    rows.append(.line(index, hunk: k == 0 ? hunk : nil))
                }
            }
        }
        return (rows, hunks)
    }
}
