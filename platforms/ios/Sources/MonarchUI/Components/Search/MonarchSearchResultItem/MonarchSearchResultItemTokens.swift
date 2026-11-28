import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchSearchResultItem``.
enum MonarchSearchResultItemTokens {
    /// Padding around the row.
    static let padding: CGFloat = MonarchTokens.Space.space3
    /// The gap between the icon, the text and the meta.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the title, the path and the snippet.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between path segments and their separators.
    static let pathSpacing: CGFloat = MonarchTokens.Space.space1
    /// The side of the icon tile, before Dynamic Type scaling.
    static let iconTile: CGFloat = MonarchTokens.Control.sm
    /// The corner of the icon tile.
    static let iconRadius: CGFloat = MonarchTokens.Radius.sm
    /// The corner of the row.
    static let rowRadius: CGFloat = MonarchTokens.Radius.md
    /// The most lines the snippet shows.
    static let snippetLineLimit = 3
    /// The type role of the title.
    static let titleStyle: MonarchTextStyle = .body
    /// The weight of the title.
    static let titleWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.medium)
    /// The type role of the snippet.
    static let snippetStyle: MonarchTextStyle = .body
    /// The type role of the path and the meta.
    static let detailStyle: MonarchTextStyle = .caption
    /// The visible separator between path segments. Hidden from VoiceOver.
    static let pathSeparator = "/"
    /// The most words ``terms(from:)`` keeps, as on the web.
    static let maximumTerms = 8

    /// The colour of the title: a link colour when the row does something.
    static func titleRole(isInteractive: Bool) -> MonarchColorRole {
        isInteractive ? \.textLink : \.textDefault
    }

    /// The colour of the title and the icon for a status tone, or `nil` for a tone that keeps the usual colours.
    static func toneRole(_ tone: MonarchTone) -> MonarchColorRole? {
        switch tone {
        case .danger: \.dangerStrong
        case .warning: \.warningStrong
        case .success: \.successStrong
        case .info: \.infoStrong
        case .neutral, .accent, .ai: nil
        }
    }

    /// The fill of the row: subtle while pressed, none otherwise.
    static func rowFill(isPressed: Bool) -> MonarchColorRole? {
        isPressed ? \.bgSubtle : nil
    }

    /// The fill of a matched run, as the web's `mark`.
    static var highlightBackground: MonarchColorRole { \.warningSubtle }
    /// The text colour of a matched run. Matches are also semibold, so they
    /// do not depend on colour.
    static var highlightForeground: MonarchColorRole { \.textDefault }

    // MARK: - Terms and highlighting

    /// Splits a query into words to highlight: letters and digits only,
    /// lower-cased, without repeats, at most eight.
    static func terms(from query: String) -> [String] {
        var seen = Set<String>()
        var result: [String] = []
        for word in query.lowercased().split(whereSeparator: { !($0.isLetter || $0.isNumber) }) {
            let word = String(word)
            if seen.insert(word).inserted { result.append(word) }
            if result.count == maximumTerms { break }
        }
        return result
    }

    /// One run of text and whether it matched.
    struct Segment: Equatable {
        /// The text of the run.
        let text: String
        /// Whether the run matched the query.
        let isMatch: Bool
    }

    /// Splits text into matched and unmatched runs.
    ///
    /// - `markTags`: the text carries `<mark>` and `</mark>` markers from the
    ///   search service. Only those two markers are read; other markup stays text.
    /// - `ranges`: exact matches as UTF-16 offsets, as a JavaScript or
    ///   PostgreSQL service reports them. Overlaps are merged and offsets
    ///   inside a character are widened to the whole character.
    /// - otherwise `terms`, matched without regard to case.
    static func segments(_ text: String, terms: [String], ranges: [Range<Int>]?, markTags: Bool) -> [Segment] {
        if text.isEmpty { return [] }
        if markTags { return markedSegments(text) }
        let matches: [Range<String.Index>]
        if let ranges, !ranges.isEmpty {
            matches = ranges.compactMap { indexRange($0, in: text) }
        } else {
            matches = termRanges(text, terms: terms)
        }
        return split(text, at: merge(matches))
    }

    /// The text without the `<mark>` markers, for VoiceOver and for plain display.
    static func plainText(_ text: String, markTags: Bool) -> String {
        markTags ? markedSegments(text).map(\.text).joined() : text
    }

    private static func markedSegments(_ text: String) -> [Segment] {
        var result: [Segment] = []
        var isMatch = false
        var rest = text[...]
        while let open = rest.range(of: "<", options: .literal) {
            let after = rest[open.lowerBound...]
            let tag: (length: Int, opens: Bool)? =
                if after.lowercased().hasPrefix("<mark>") {
                    (6, true)
                } else if after.lowercased().hasPrefix("</mark>") {
                    (7, false)
                } else {
                    nil
                }
            guard let tag else {
                // Not a marker: keep the "<" as text and continue after it.
                let next = rest.index(after: open.lowerBound)
                append(String(rest[..<next]), isMatch: isMatch, to: &result)
                rest = rest[next...]
                continue
            }
            append(String(rest[..<open.lowerBound]), isMatch: isMatch, to: &result)
            isMatch = tag.opens
            rest = rest[rest.index(open.lowerBound, offsetBy: tag.length)...]
        }
        append(String(rest), isMatch: isMatch, to: &result)
        return result
    }

    /// Appends a run, joining it to the previous one when both match or both do not.
    private static func append(_ text: String, isMatch: Bool, to result: inout [Segment]) {
        guard !text.isEmpty else { return }
        if let last = result.last, last.isMatch == isMatch {
            result[result.count - 1] = Segment(text: last.text + text, isMatch: isMatch)
        } else {
            result.append(Segment(text: text, isMatch: isMatch))
        }
    }

    private static func termRanges(_ text: String, terms: [String]) -> [Range<String.Index>] {
        let terms = terms.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }
        var result: [Range<String.Index>] = []
        for term in terms {
            var start = text.startIndex
            while start < text.endIndex,
                let found = text.range(of: term, options: [.caseInsensitive], range: start..<text.endIndex)
            {
                result.append(found)
                start = found.upperBound
            }
        }
        return result
    }

    /// A UTF-16 offset range as string indices on character boundaries, or
    /// `nil` when it is empty or outside the text.
    private static func indexRange(_ range: Range<Int>, in text: String) -> Range<String.Index>? {
        let count = text.utf16.count
        let lower = min(max(range.lowerBound, 0), count)
        let upper = min(max(range.upperBound, 0), count)
        guard upper > lower else { return nil }
        let start = text.utf16.index(text.startIndex, offsetBy: lower)
        let end = text.utf16.index(text.startIndex, offsetBy: upper)
        return text.rangeOfComposedCharacterSequences(for: start..<end)
    }

    private static func merge(_ ranges: [Range<String.Index>]) -> [Range<String.Index>] {
        var result: [Range<String.Index>] = []
        for range in ranges.filter({ !$0.isEmpty }).sorted(by: { $0.lowerBound < $1.lowerBound }) {
            if let last = result.last, range.lowerBound <= last.upperBound {
                result[result.count - 1] = last.lowerBound..<max(last.upperBound, range.upperBound)
            } else {
                result.append(range)
            }
        }
        return result
    }

    private static func split(_ text: String, at matches: [Range<String.Index>]) -> [Segment] {
        var result: [Segment] = []
        var last = text.startIndex
        for match in matches {
            if match.lowerBound > last {
                result.append(Segment(text: String(text[last..<match.lowerBound]), isMatch: false))
            }
            result.append(Segment(text: String(text[match]), isMatch: true))
            last = match.upperBound
        }
        if last < text.endIndex { result.append(Segment(text: String(text[last...]), isMatch: false)) }
        return result
    }

    // MARK: - Accessibility

    /// What VoiceOver reads for the path: "Location: Handbook, Travel".
    static func pathAccessibilityLabel(_ path: [String], pathLabel: String) -> String? {
        let segments = path.filter { !$0.isEmpty }
        guard !segments.isEmpty else { return nil }
        return "\(pathLabel): \(segments.joined(separator: ", "))"
    }
}
