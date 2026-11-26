import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchConsequenceList``.
enum MonarchConsequenceListTokens {
    /// The gap between rows.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the heading, the groups and the rows.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the icon and the text of a row.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the label and the detail of a row.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The order of the groups: what is lost first, what stays last.
    static let groupOrder: [MonarchConsequenceKind] = [.removed, .changed, .warning, .kept]
    /// Placeholder text for a row while loading. Never read or shown as text.
    static let placeholder = "Placeholder consequence"

    /// The SF Symbol of a kind. Each kind has its own shape.
    static func symbol(_ kind: MonarchConsequenceKind) -> String {
        switch kind {
        case .removed: "minus.circle"
        case .changed: "arrow.left.arrow.right.circle"
        case .warning: "exclamationmark.triangle"
        case .kept: "checkmark.circle"
        }
    }

    /// The colour role of the icon of a kind.
    static func iconRole(_ kind: MonarchConsequenceKind) -> MonarchColorRole {
        switch kind {
        case .removed: \.dangerStrong
        case .changed: \.textMuted
        case .warning: \.warningStrong
        case .kept: \.successStrong
        }
    }

    /// The items of one kind, under one heading.
    struct Group: Equatable, Identifiable {
        let kind: MonarchConsequenceKind
        let items: [MonarchConsequence]

        var id: MonarchConsequenceKind { kind }
    }

    /// The items of each kind that has any, in `groupOrder`. The order of
    /// the items inside a group is kept.
    static func groups(_ items: [MonarchConsequence]) -> [Group] {
        groupOrder.compactMap { kind in
            let matching = items.filter { $0.kind == kind }
            return matching.isEmpty ? nil : Group(kind: kind, items: matching)
        }
    }

    /// The visible first line of a row: the count, formatted for the locale,
    /// then the label.
    static func title(_ item: MonarchConsequence, locale: Locale = .autoupdatingCurrent) -> String {
        guard let count = item.count else { return item.label }
        return "\(count.formatted(.number.locale(locale))) \(item.label)"
    }

    /// What VoiceOver reads for a row. The kind is spoken first unless the
    /// row is under a group heading that already says it.
    static func accessibilityLabel(
        _ item: MonarchConsequence,
        kindName: String?,
        locale: Locale = .autoupdatingCurrent
    ) -> String {
        var parts: [String] = []
        if let kindName, !kindName.isEmpty {
            parts.append("\(kindName): \(title(item, locale: locale))")
        } else {
            parts.append(title(item, locale: locale))
        }
        if let detail = item.detail, !detail.isEmpty {
            parts.append(detail)
        }
        return parts.joined(separator: ", ")
    }

    /// The number of placeholder rows while loading: at least one.
    static func loadingRows(_ requested: Int) -> Int {
        max(1, requested)
    }
}
