import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchFavoritesList``.
enum MonarchFavoritesListTokens {
    /// Space between the icon and the name.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// Space between the star and the heading.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space1_5

    /// The colours of a row.
    struct RowColors: Equatable {
        /// The name.
        let text: MonarchColorRole
        /// The icon.
        let icon: MonarchColorRole
        /// The row fill, or `nil` for the list's own.
        let fill: MonarchColorRole?
    }

    /// The colours of a row: the current screen takes the accent's subtle
    /// fill and strong text; other rows keep the default text and a muted icon.
    static func colors(isCurrent: Bool) -> RowColors {
        isCurrent
            ? RowColors(text: \.textDefault, icon: \.accentStrong, fill: \.accentSubtle)
            : RowColors(text: \.textDefault, icon: \.textMuted, fill: nil)
    }

    /// Whether the list can be edited: the app handles a removal or a new order.
    static func isEditable(canRemove: Bool, canReorder: Bool) -> Bool {
        canRemove || canReorder
    }

    /// Whether the list is in edit mode: only when it can be edited and has items.
    static func isEditing(requested: Bool, isEditable: Bool, count: Int) -> Bool {
        requested && isEditable && count > 0
    }

    /// Whether the whole list is left out: no items and no empty message, as
    /// navigation sections disappear when empty.
    static func isHidden(count: Int, hasEmptyMessage: Bool) -> Bool {
        count == 0 && !hasEmptyMessage
    }

    /// The items after `onMove`'s offsets are applied.
    static func moved(
        _ items: [MonarchFavoriteItem],
        fromOffsets source: IndexSet,
        toOffset destination: Int
    ) -> [MonarchFavoriteItem] {
        var result = items
        result.move(fromOffsets: source, toOffset: destination)
        return result
    }

    /// The items with one moved by `offset` places, or `nil` when it cannot move that far.
    static func moved(_ items: [MonarchFavoriteItem], id: String, by offset: Int) -> [MonarchFavoriteItem]? {
        guard let index = items.firstIndex(where: { $0.id == id }) else { return nil }
        let target = index + offset
        guard offset != 0, items.indices.contains(target) else { return nil }
        var result = items
        let item = result.remove(at: index)
        result.insert(item, at: target)
        return result
    }

    /// The items `onDelete`'s offsets point at, in order.
    static func removed(_ items: [MonarchFavoriteItem], atOffsets offsets: IndexSet) -> [MonarchFavoriteItem] {
        offsets.filter { items.indices.contains($0) }.map { items[$0] }
    }

    /// The heading, with the override applied.
    static func title(labels: MonarchFavoritesListLabels) -> String {
        labels.title ?? MonarchFavoritesListStrings.title
    }

    /// The visible title of the edit button for the current mode.
    static func editTitle(isEditing: Bool, labels: MonarchFavoritesListLabels) -> String {
        isEditing ? labels.done ?? MonarchFavoritesListStrings.done : labels.edit ?? MonarchFavoritesListStrings.edit
    }

    /// The spoken name of the edit button for the current mode.
    static func editAccessibilityLabel(isEditing: Bool, labels: MonarchFavoritesListLabels) -> String {
        isEditing
            ? labels.done ?? MonarchFavoritesListStrings.done
            : labels.editLabel ?? MonarchFavoritesListStrings.editLabel
    }

    /// The announcement after a removal, such as "Roadmap removed from favorites."
    static func removedAnnouncement(_ title: String, labels: MonarchFavoritesListLabels) -> String {
        let format = labels.removed ?? MonarchFavoritesListStrings.removed
        return String(format: format, title)
    }

    /// The names of the move actions.
    static func moveActions(labels: MonarchFavoritesListLabels) -> (up: String, down: String) {
        (labels.moveUp ?? MonarchFavoritesListStrings.moveUp, labels.moveDown ?? MonarchFavoritesListStrings.moveDown)
    }
}
