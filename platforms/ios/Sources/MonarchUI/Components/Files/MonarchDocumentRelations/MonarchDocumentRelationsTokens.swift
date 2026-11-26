import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchDocumentRelations``.
enum MonarchDocumentRelationsTokens {
    /// The gap between the icon, the text and the remove button of a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of a row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// Padding above and below a row.
    static let rowPadding: CGFloat = MonarchTokens.Space.space2
    /// The gap between the relation's symbol and its label.
    static let typeSpacing: CGFloat = MonarchTokens.Space.space1
    /// The most search results shown.
    static let maxResults = 12
    /// The SF Symbol of the add button.
    static let addSymbol = "plus"
    /// The SF Symbol of the remove button.
    static let removeSymbol = "xmark"
    /// The SF Symbol of a target without a file name.
    static let documentSymbol = "doc.text"
    /// Colours.
    static var titleColor: MonarchColorRole { \.textDefault }
    static var mutedColor: MonarchColorRole { \.textMuted }
    static var errorColor: MonarchColorRole { \.dangerStrong }
    static var separatorColor: MonarchColorRole { \.borderSubtle }

    /// The SF Symbol of a relation type, read in the layout direction:
    /// up for parent, down for child, a check for implements, forward otherwise.
    static func symbol(_ value: String) -> String {
        switch value {
        case "parent-of": "arrow.up"
        case "child-of": "arrow.down"
        case "implements": "checkmark"
        default: "arrow.forward"
        }
    }

    /// The type a relation reads as from this document, and its label.
    /// An incoming relation reads as the inverse of its type. An unknown
    /// value is its own label.
    static func type(
        of relation: MonarchDocumentRelation,
        types: [MonarchDocumentRelationType]
    ) -> (value: String, label: String) {
        let value =
            relation.direction == .incoming
            ? types.first { $0.value == relation.type }?.inverse ?? relation.type
            : relation.type
        return (value, types.first { $0.value == value }?.label ?? value)
    }

    /// The type chosen when the sheet opens: `related-to` when offered, else the first type.
    static func initialType(_ preferred: String?, types: [MonarchDocumentRelationType]) -> String {
        if let preferred, types.contains(where: { $0.value == preferred }) { return preferred }
        return types.contains { $0.value == "related-to" } ? "related-to" : types.first?.value ?? ""
    }

    /// The candidates shown in the sheet: not already related, filtered by
    /// title unless the app searches, at most twelve.
    static func results(
        _ candidates: [MonarchDocumentRelationTarget],
        relations: [MonarchDocumentRelation],
        query: String,
        searchesRemotely: Bool
    ) -> [MonarchDocumentRelationTarget] {
        let related = Set(relations.map(\.target.id))
        let open = candidates.filter { !related.contains($0.id) }
        let needle = query.trimmingCharacters(in: .whitespaces)
        guard !searchesRemotely, !needle.isEmpty else { return Array(open.prefix(maxResults)) }
        return Array(open.filter { $0.title.localizedStandardContains(needle) }.prefix(maxResults))
    }

    /// What VoiceOver reads for a relation: the relation, then the title and description.
    static func accessibilityLabel(typeLabel: String, target: MonarchDocumentRelationTarget) -> String {
        [typeLabel, target.title, target.description].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
    }

    /// The message of a failed add or remove: the error's description when it has one.
    static func failureMessage(_ error: any Error, labels: MonarchDocumentRelationsLabels) -> String {
        MonarchFailureMessage.message(
            for: error,
            override: labels.failed,
            fallback: MonarchDocumentRelationsStrings.failed
        )
    }

    static func removeLabel(_ title: String, labels: MonarchDocumentRelationsLabels) -> String {
        labels.remove?(title) ?? MonarchDocumentRelationsStrings.remove(title)
    }

    static func addedText(_ title: String, labels: MonarchDocumentRelationsLabels) -> String {
        labels.added?(title) ?? MonarchDocumentRelationsStrings.added(title)
    }

    static func removedText(_ title: String, labels: MonarchDocumentRelationsLabels) -> String {
        labels.removed?(title) ?? MonarchDocumentRelationsStrings.removed(title)
    }

    /// The id Add takes in the focus state.
    static let addFocusID = "monarch.documentRelations.add"

    /// Where VoiceOver focus goes after the relation `removed` is taken out of `ids`: the next
    /// relation's remove button, else the one before it, else Add (when there is one).
    static func focusAfterRemoval(of removed: String, in ids: [String], canAdd: Bool) -> String? {
        MonarchFocusAfterRemoval.target(removing: removed, from: ids) ?? (canAdd ? addFocusID : nil)
    }
}
