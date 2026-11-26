import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchAiInlineDiff``.
enum MonarchAiInlineDiffTokens {
    /// The SwiftUI attributes, named in full so they are not mistaken for UIKit's.
    typealias SwiftUIAttributes = AttributeScopes.SwiftUIAttributes

    /// The gap between the navigation, the text and the decision buttons.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the decision buttons.
    static let buttonSpacing: CGFloat = MonarchTokens.Space.space2
    /// The URL scheme of the tappable change runs inside the text.
    static let scheme = "monarch-ai-inline-diff"
    /// The SF Symbol of the accept button.
    static let acceptSymbol = "checkmark"
    /// The SF Symbol of the reject button.
    static let rejectSymbol = "xmark"

    // MARK: - Changes

    /// Groups segments into changes. A change is a run of adjacent deleted and
    /// inserted segments that share an id (or all have none); its id is that
    /// id, or the index of its first segment as a string.
    static func changes(_ segments: [MonarchAiDiffSegment]) -> [MonarchAiInlineDiffChange] {
        struct Building {
            var id: String
            var indexes: [Int]
            var deleted: String
            var inserted: String
            var segmentID: String?
        }
        var out: [Building] = []
        var isOpen = false
        for (index, segment) in segments.enumerated() {
            if segment.kind == .equal {
                isOpen = false
                continue
            }
            if !isOpen || out.last?.segmentID != segment.id {
                out.append(
                    Building(
                        id: segment.id ?? String(index),
                        indexes: [],
                        deleted: "",
                        inserted: "",
                        segmentID: segment.id
                    )
                )
                isOpen = true
            }
            out[out.count - 1].indexes.append(index)
            if segment.kind == .delete {
                out[out.count - 1].deleted += segment.text
            } else {
                out[out.count - 1].inserted += segment.text
            }
        }
        return out.map {
            MonarchAiInlineDiffChange(id: $0.id, segmentIndexes: $0.indexes, deleted: $0.deleted, inserted: $0.inserted)
        }
    }

    /// The segments with one change decided: accepting keeps the inserted text,
    /// rejecting keeps the deleted text. Adjacent unchanged text without ids is
    /// merged. An unknown id returns the segments unchanged.
    static func resolve(
        _ segments: [MonarchAiDiffSegment],
        id: String,
        decision: MonarchAiInlineDiffDecision
    ) -> [MonarchAiDiffSegment] {
        guard let change = changes(segments).first(where: { $0.id == id }) else { return segments }
        let indexes = Set(change.segmentIndexes)
        let keep: MonarchAiDiffSegmentKind = decision == .accept ? .insert : .delete
        var out: [MonarchAiDiffSegment] = []
        for (index, segment) in segments.enumerated() {
            var next: MonarchAiDiffSegment? = segment
            if indexes.contains(index) {
                next = segment.kind == keep ? MonarchAiDiffSegment(.equal, text: segment.text) : nil
            }
            guard let next, !next.text.isEmpty else { continue }
            if next.kind == .equal, let last = out.last, last.kind == .equal, last.id == nil, next.id == nil {
                out[out.count - 1].text += next.text
            } else {
                out.append(next)
            }
        }
        return out
    }

    /// The active change kept inside the changes that exist.
    static func clamp(_ active: Int, total: Int) -> Int {
        min(max(active, 0), max(0, total - 1))
    }

    /// For each segment, the index of the change it belongs to, or nil for unchanged text.
    static func changeIndexes(_ segments: [MonarchAiDiffSegment], changes: [MonarchAiInlineDiffChange]) -> [Int?] {
        var out = [Int?](repeating: nil, count: segments.count)
        for (changeIndex, change) in changes.enumerated() {
            for index in change.segmentIndexes where out.indices.contains(index) {
                out[index] = changeIndex
            }
        }
        return out
    }

    // MARK: - Links

    /// The URL that a tap on a change opens, which makes it the active change.
    static func url(forChange index: Int) -> URL? {
        URL(string: "\(scheme)://change/\(index)")
    }

    /// The change a tapped URL names, or nil when the URL is not one of ours.
    static func change(from url: URL) -> Int? {
        guard url.scheme == scheme, url.host == "change" else { return nil }
        return Int(url.lastPathComponent)
    }

    // MARK: - Appearance

    /// The colour roles of one run.
    struct Appearance: Equatable {
        let foreground: MonarchColorRole
        let background: MonarchColorRole?
    }

    /// The roles of a run. Deletions read in the danger roles and insertions
    /// in the success roles; the active change has the stronger background.
    /// Colour is never alone: deletions are struck through and insertions
    /// underlined, and the active change is bold.
    static func appearance(_ kind: MonarchAiDiffSegmentKind, isActive: Bool) -> Appearance {
        switch kind {
        case .equal:
            Appearance(foreground: \.textDefault, background: nil)
        case .delete:
            Appearance(foreground: \.dangerStrong, background: isActive ? \.dangerSubtleHover : \.dangerSubtle)
        case .insert:
            Appearance(foreground: \.successStrong, background: isActive ? \.successSubtleHover : \.successSubtle)
        }
    }

    /// The text with every change marked, each change a link to itself.
    static func attributed(
        _ segments: [MonarchAiDiffSegment],
        changes: [MonarchAiInlineDiffChange],
        active: Int,
        colors: MonarchColors
    ) -> AttributedString {
        let owners = changeIndexes(segments, changes: changes)
        var out = AttributedString()
        for (index, segment) in segments.enumerated() {
            var run = AttributedString(segment.text)
            let owner = owners[index]
            let isActive = owner == active
            let appearance = appearance(segment.kind, isActive: isActive)
            run[SwiftUIAttributes.ForegroundColorAttribute.self] = colors[role: appearance.foreground]
            if let background = appearance.background {
                run[SwiftUIAttributes.BackgroundColorAttribute.self] = colors[role: background]
            }
            switch segment.kind {
            case .equal: break
            case .delete: run[SwiftUIAttributes.StrikethroughStyleAttribute.self] = Text.LineStyle.single
            case .insert: run[SwiftUIAttributes.UnderlineStyleAttribute.self] = Text.LineStyle.single
            }
            if let owner {
                run.link = url(forChange: owner)
                if isActive { run.inlinePresentationIntent = .stronglyEmphasized }
            }
            out += run
        }
        return out
    }

    // MARK: - Accessibility

    /// What VoiceOver reads for the text: unchanged text as it is, and each
    /// removal and addition after "Deleted:" or "Inserted:".
    static func spokenText(_ segments: [MonarchAiDiffSegment], labels: MonarchAiInlineDiffLabels) -> String {
        let deleted = labels.deleted ?? MonarchAiInlineDiffStrings.deleted
        let inserted = labels.inserted ?? MonarchAiInlineDiffStrings.inserted
        var parts: [String] = []
        for segment in segments {
            let text = segment.text.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !text.isEmpty else { continue }
            switch segment.kind {
            case .equal: parts.append(text)
            case .delete: parts.append("\(deleted) \(text)")
            case .insert: parts.append("\(inserted) \(text)")
            }
        }
        return parts.joined(separator: " ")
    }

    /// "Change 2 of 5", or "No suggested changes". `index` counts from 0.
    static func position(_ index: Int, total: Int, labels: MonarchAiInlineDiffLabels) -> String {
        guard total > 0 else { return labels.none ?? MonarchAiInlineDiffStrings.none }
        return labels.position?(index + 1, total) ?? MonarchAiInlineDiffStrings.position(index + 1, total)
    }

    /// The spoken name of a decision button. `index` counts from 0.
    static func buttonLabel(
        _ decision: MonarchAiInlineDiffDecision,
        index: Int,
        total: Int,
        labels: MonarchAiInlineDiffLabels
    ) -> String {
        switch decision {
        case .accept: labels.accept?(index + 1, total) ?? MonarchAiInlineDiffStrings.accept(index + 1, total)
        case .reject: labels.reject?(index + 1, total) ?? MonarchAiInlineDiffStrings.reject(index + 1, total)
        }
    }

    /// Announced after a decision, from the number of changes left.
    static func announcement(
        _ decision: MonarchAiInlineDiffDecision,
        left: Int,
        labels: MonarchAiInlineDiffLabels
    ) -> String {
        labels.announce?(decision, max(0, left)) ?? MonarchAiInlineDiffStrings.announce(decision, left: max(0, left))
    }
}
