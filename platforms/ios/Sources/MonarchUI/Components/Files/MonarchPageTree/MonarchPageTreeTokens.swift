import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and editing logic for ``MonarchPageTree``. Every edit is
/// a pure function from the groups to the new groups and the move, so the
/// rules are tested without a list.
enum MonarchPageTreeTokens {
    /// The letter spacing of the uppercase group headings, as on the web.
    static let headerTracking: CGFloat = 0.5

    /// The gap between a page number and its title.
    static let numberSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the parts of a group heading.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space2
    /// The digits a page number is padded to: 01, 02 … 10.
    static let numberDigits = 2
    /// The SF Symbol of the add buttons.
    static let addSymbol = "plus"
    /// The SF Symbol of a group's menu.
    static let menuSymbol = "ellipsis.circle"
    /// The SF Symbol of the delete actions.
    static let deleteSymbol = "trash"
    /// The SF Symbol of the move menu.
    static let moveSymbol = "folder"

    /// An item of a group's menu.
    enum GroupAction: Equatable {
        case rename
        case moveUp
        case moveDown
        case delete
    }

    /// The colour role of a page number.
    static func numberColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textSubtle : \.textDisabled
    }

    /// The colour role of a page title.
    static func titleColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The colour role of secondary text, such as "No pages yet."
    static func mutedColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// The running number of every page, across groups, from 1.
    static func pageNumbers(_ groups: [MonarchPageTreeGroup]) -> [String: Int] {
        var numbers: [String: Int] = [:]
        for (index, page) in groups.flatMap(\.pages).enumerated() { numbers[page.id] = index + 1 }
        return numbers
    }

    /// A page number padded to two digits in the locale's digits: "01".
    static func numberText(_ number: Int, locale: Locale) -> String {
        number.formatted(
            IntegerFormatStyle<Int>(locale: locale)
                .precision(.integerLength(numberDigits...))
                .grouping(.never)
        )
    }

    /// The title shown for a page: its own, or "Untitled page" when blank.
    static func title(_ page: MonarchPageTreePage, labels: MonarchPageTreeLabels) -> String {
        let trimmed = page.title.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? labels.untitled ?? MonarchPageTreeStrings.untitled : trimmed
    }

    /// The total number of pages.
    static func pageCount(_ groups: [MonarchPageTreeGroup]) -> Int {
        groups.reduce(0) { $0 + $1.pages.count }
    }

    /// Whether a page can be deleted: never the last page.
    static func canDeletePage(_ groups: [MonarchPageTreeGroup]) -> Bool {
        pageCount(groups) > 1
    }

    /// Whether a group can be deleted: never the last group.
    static func canDeleteGroup(_ groups: [MonarchPageTreeGroup]) -> Bool {
        groups.count > 1
    }

    /// The groups a page of `groupID` can move to.
    static func moveTargets(_ groups: [MonarchPageTreeGroup], from groupID: String) -> [MonarchPageTreeGroup] {
        groups.filter { $0.id != groupID }
    }

    /// The items of a group's menu, in order.
    static func groupActions(
        index: Int,
        groupCount: Int,
        canRename: Bool,
        canReorder: Bool,
        canDelete: Bool
    ) -> [GroupAction] {
        var actions: [GroupAction] = []
        if canRename { actions.append(.rename) }
        if canReorder && index > 0 { actions.append(.moveUp) }
        if canReorder && index < groupCount - 1 { actions.append(.moveDown) }
        if canDelete && groupCount > 1 { actions.append(.delete) }
        return actions
    }

    /// The groups after the list's drag reorder within one group.
    static func reordering(
        _ groups: [MonarchPageTreeGroup],
        groupID: String,
        from offsets: IndexSet,
        to destination: Int
    ) -> (groups: [MonarchPageTreeGroup], move: MonarchPageTreeMove)? {
        guard let groupIndex = groups.firstIndex(where: { $0.id == groupID }),
            let first = offsets.first,
            groups[groupIndex].pages.indices.contains(first)
        else { return nil }
        var next = groups
        let moved = next[groupIndex].pages[first]
        next[groupIndex].pages.move(fromOffsets: offsets, toOffset: destination)
        guard next != groups, let index = next[groupIndex].pages.firstIndex(of: moved) else { return nil }
        let move = MonarchPageTreeMove(
            kind: .page,
            id: moved.id,
            fromGroupID: groupID,
            toGroupID: groupID,
            toIndex: index
        )
        return (next, move)
    }

    /// The groups after a page moves one place up (`delta` -1) or down (1) in its group.
    static func movingPage(
        _ groups: [MonarchPageTreeGroup],
        pageID: String,
        by delta: Int
    ) -> (groups: [MonarchPageTreeGroup], move: MonarchPageTreeMove)? {
        guard let group = groups.first(where: { $0.pages.contains { $0.id == pageID } }),
            let index = group.pages.firstIndex(where: { $0.id == pageID })
        else { return nil }
        let target = index + delta
        guard group.pages.indices.contains(target) else { return nil }
        return reordering(groups, groupID: group.id, from: [index], to: delta > 0 ? target + 1 : target)
    }

    /// The groups after a page moves to the end of another group.
    static func movingPage(
        _ groups: [MonarchPageTreeGroup],
        pageID: String,
        toGroup targetID: String
    ) -> (groups: [MonarchPageTreeGroup], move: MonarchPageTreeMove)? {
        guard let fromIndex = groups.firstIndex(where: { $0.pages.contains { $0.id == pageID } }),
            let toIndex = groups.firstIndex(where: { $0.id == targetID }),
            fromIndex != toIndex,
            let pageIndex = groups[fromIndex].pages.firstIndex(where: { $0.id == pageID })
        else { return nil }
        var next = groups
        let page = next[fromIndex].pages.remove(at: pageIndex)
        next[toIndex].pages.append(page)
        let move = MonarchPageTreeMove(
            kind: .page,
            id: pageID,
            fromGroupID: groups[fromIndex].id,
            toGroupID: targetID,
            toIndex: next[toIndex].pages.count - 1
        )
        return (next, move)
    }

    /// The groups after a group moves one place up (`delta` -1) or down (1).
    static func movingGroup(
        _ groups: [MonarchPageTreeGroup],
        groupID: String,
        by delta: Int
    ) -> (groups: [MonarchPageTreeGroup], move: MonarchPageTreeMove)? {
        guard let from = groups.firstIndex(where: { $0.id == groupID }) else { return nil }
        let to = from + delta
        guard groups.indices.contains(to) else { return nil }
        var next = groups
        next.swapAt(from, to)
        return (next, MonarchPageTreeMove(kind: .group, id: groupID, toIndex: to))
    }

    /// The announcement after a move: the new position, or the new group.
    static func announcement(
        for move: MonarchPageTreeMove,
        in groups: [MonarchPageTreeGroup],
        labels: MonarchPageTreeLabels
    ) -> String? {
        switch move.kind {
        case .group:
            guard let group = groups.first(where: { $0.id == move.id }) else { return nil }
            return moved(group.label, move.toIndex + 1, groups.count, labels: labels)
        case .page:
            guard let target = groups.first(where: { $0.id == move.toGroupID }),
                let page = target.pages.first(where: { $0.id == move.id })
            else { return nil }
            let name = title(page, labels: labels)
            if move.fromGroupID != move.toGroupID {
                return labels.movedTo?(name, target.label) ?? MonarchPageTreeStrings.movedTo(name, target.label)
            }
            return moved(name, move.toIndex + 1, target.pages.count, labels: labels)
        }
    }

    /// A name cleaned of surrounding spaces, or `nil` when blank.
    static func cleanedName(_ text: String) -> String? {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }

    /// Whether deleting a group asks for its name: when it has pages.
    static func needsPhrase(_ group: MonarchPageTreeGroup) -> Bool {
        !group.pages.isEmpty
    }

    /// Whether the delete group button can be pressed with the typed text.
    static func canDelete(_ group: MonarchPageTreeGroup, typed: String) -> Bool {
        !needsPhrase(group) || MonarchTypeToConfirmInput.matches(typed, phrase: group.label)
    }

    /// The message of the delete group dialog.
    static func deleteGroupMessage(_ group: MonarchPageTreeGroup, labels: MonarchPageTreeLabels) -> String {
        var parts = [labels.cannotUndo ?? MonarchPageTreeStrings.cannotUndo]
        if needsPhrase(group) {
            let count = group.pages.count
            parts.append(
                labels.deleteGroupConsequence?(count) ?? MonarchPageTreeStrings.deleteGroupConsequence(count)
            )
            parts.append(labels.typeToConfirm?(group.label) ?? MonarchPageTreeStrings.typeToConfirm(group.label))
        }
        return parts.joined(separator: " ")
    }

    private static func moved(_ name: String, _ position: Int, _ count: Int, labels: MonarchPageTreeLabels) -> String {
        labels.moved?(name, position, count) ?? MonarchPageTreeStrings.moved(name, position, count)
    }
}
