/// A run of a word diff: unchanged, added or removed text.
struct MonarchAiEditProposalSegment: Equatable {
    /// What happened to the text.
    enum Kind: Equatable {
        case equal
        case insert
        case delete
    }

    let kind: Kind
    var text: String
}

/// The word diff of ``MonarchAiEditProposal``, ported from the web's
/// `diffWords`. It reuses the longest common subsequence and the word
/// tokens of ``MonarchDiffViewer``, then groups adjacent changes so each
/// change reads as one removal followed by one insertion, and whitespace
/// alone never splits a change in two.
enum MonarchAiEditProposalDiff {
    /// Above this many word pairs the middle of the diff is one removal and one insertion.
    static let maxPairs = 2_000_000

    /// The word diff of `before` and `after`. Empty when both are empty.
    static func words(_ before: String, _ after: String) -> [MonarchAiEditProposalSegment] {
        let a = MonarchTextDiff.tokens(before)
        let b = MonarchTextDiff.tokens(after)
        var start = 0
        while start < a.count && start < b.count && a[start] == b[start] { start += 1 }
        var endA = a.count
        var endB = b.count
        while endA > start && endB > start && a[endA - 1] == b[endB - 1] {
            endA -= 1
            endB -= 1
        }

        // Each raw piece, with whitespace alone between two changes marked to join them.
        enum Raw {
            case equal(String)
            case delete(String)
            case insert(String)
        }
        var raw: [Raw] = []
        if start > 0 { raw.append(.equal(a[0..<start].joined())) }
        for operation in MonarchTextDiff.sequences(
            Array(a[start..<endA]),
            Array(b[start..<endB]),
            maxPairs: maxPairs
        ) {
            switch operation {
            case .equal(let text): raw.append(.equal(text))
            case .delete(let text): raw.append(.delete(text))
            case .insert(let text): raw.append(.insert(text))
            }
        }
        if endA < a.count { raw.append(.equal(a[endA...].joined())) }

        func isChange(_ index: Int) -> Bool {
            guard raw.indices.contains(index) else { return false }
            if case .equal = raw[index] { return false }
            return true
        }

        var out: [MonarchAiEditProposalSegment] = []
        var deleted = ""
        var inserted = ""
        func append(_ kind: MonarchAiEditProposalSegment.Kind, _ text: String) {
            guard !text.isEmpty else { return }
            if let last = out.last, last.kind == kind {
                out[out.count - 1].text += text
            } else {
                out.append(MonarchAiEditProposalSegment(kind: kind, text: text))
            }
        }
        func flush() {
            append(.delete, deleted)
            append(.insert, inserted)
            deleted = ""
            inserted = ""
        }
        for (index, piece) in raw.enumerated() {
            switch piece {
            case .equal(let text):
                let joinsChanges =
                    isChange(index - 1) && isChange(index + 1)
                    && text.allSatisfy(\.isWhitespace)
                if joinsChanges {
                    deleted += text
                    inserted += text
                } else {
                    flush()
                    append(.equal, text)
                }
            case .delete(let text): deleted += text
            case .insert(let text): inserted += text
            }
        }
        flush()
        return out
    }
}
