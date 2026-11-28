import Foundation
import MonarchTokens
import SwiftUI

/// An action in the saved views menu.
enum MonarchSavedViewsAction: Equatable, CaseIterable {
    case saveChanges
    case saveAsNew
    case discardChanges
    case rename
    case setDefault
    case delete
}

/// A group of views in the menu.
struct MonarchSavedViewsGroup: Equatable {
    /// The heading; `nil` for views without a group.
    let title: String?
    /// The views, in their order.
    let views: [MonarchSavedView]
}

/// Which alert is open.
enum MonarchSavedViewsDialog: Equatable {
    case saveAsNew
    case rename(MonarchSavedView)
    case delete(MonarchSavedView)
}

/// The resolved strings: the app's replacements, else the built-in ones.
struct MonarchSavedViewsText {
    let labels: MonarchSavedViewsLabels

    private func pick(_ value: String?, _ key: String) -> String { value ?? MonarchSavedViewsStrings.string(key) }

    var view: String { pick(labels.view, "view") }
    var noView: String { pick(labels.noView, "noView") }
    var modified: String { pick(labels.modified, "modified") }
    var shared: String { pick(labels.shared, "shared") }
    var isDefault: String { pick(labels.isDefault, "isDefault") }
    var views: String { pick(labels.views, "views") }
    var saveAsNew: String { pick(labels.saveAsNew, "saveAsNew") }
    var discardChanges: String { pick(labels.discardChanges, "discardChanges") }
    var rename: String { pick(labels.rename, "rename") }
    var setDefault: String { pick(labels.setDefault, "setDefault") }
    var delete: String { pick(labels.delete, "delete") }
    var saveTitle: String { pick(labels.saveTitle, "saveTitle") }
    var renameTitle: String { pick(labels.renameTitle, "renameTitle") }
    var name: String { pick(labels.name, "name") }
    var save: String { pick(labels.save, "save") }
    var cancel: String { pick(labels.cancel, "cancel") }
    var nameRequired: String { pick(labels.nameRequired, "nameRequired") }
    var nameTaken: String { pick(labels.nameTaken, "nameTaken") }
    var deleteMessage: String { pick(labels.deleteMessage, "deleteMessage") }
    var deleteConfirm: String { pick(labels.deleteConfirm, "deleteConfirm") }

    func saveChanges(_ name: String) -> String {
        labels.saveChanges?(name) ?? MonarchSavedViewsStrings.saveChanges(name)
    }

    func deleteTitle(_ name: String) -> String {
        labels.deleteTitle?(name) ?? MonarchSavedViewsStrings.deleteTitle(name)
    }
}

/// Component tokens and logic for ``MonarchSavedViews``.
enum MonarchSavedViewsTokens {
    /// The gap between the parts of the button.
    static let spacing: CGFloat = MonarchTokens.Space.space1_5
    /// The SF Symbol after the button's text.
    static let chevronSymbol = "chevron.down"
    /// The tone of the Edited marker.
    static let modifiedTone: MonarchTone = .warning
    /// The separator between the markers of a view in the menu.
    static let markerSeparator = " · "

    /// The menu actions offered, in order, for the selected view and the callbacks the app gave.
    static func actions(
        selected: MonarchSavedView?,
        isModified: Bool,
        available: Set<MonarchSavedViewsAction>
    ) -> [MonarchSavedViewsAction] {
        let canEdit = selected.map { !$0.isReadOnly } ?? false
        return MonarchSavedViewsAction.allCases.filter { action in
            guard available.contains(action) else { return false }
            switch action {
            case .saveChanges: return isModified && canEdit
            case .saveAsNew: return true
            case .discardChanges: return isModified
            case .rename, .delete: return canEdit
            case .setDefault: return selected.map { !$0.isDefault } ?? false
            }
        }
    }

    /// The views by group, groups in the order they first appear, views in their order.
    static func groups(_ views: [MonarchSavedView]) -> [MonarchSavedViewsGroup] {
        var order: [String?] = []
        var members: [String?: [MonarchSavedView]] = [:]
        for view in views {
            if members[view.group] == nil { order.append(view.group) }
            members[view.group, default: []].append(view)
        }
        return order.map { MonarchSavedViewsGroup(title: $0, views: members[$0] ?? []) }
    }

    /// The second line of a view in the menu: its count, Shared and Default view, or `nil`.
    static func subtitle(_ view: MonarchSavedView, text: MonarchSavedViewsText, locale: Locale = .autoupdatingCurrent)
        -> String?
    {
        var parts: [String] = []
        if let count = view.count { parts.append(count.formatted(.number.locale(locale))) }
        if view.isShared { parts.append(text.shared) }
        if view.isDefault { parts.append(text.isDefault) }
        return parts.isEmpty ? nil : parts.joined(separator: markerSeparator)
    }

    /// Why a name cannot be saved, or `nil` when it can: it is empty, or another view has it
    /// (ignoring case and surrounding spaces).
    static func problem(
        name: String,
        views: [MonarchSavedView],
        excluding id: String?,
        text: MonarchSavedViewsText
    ) -> String? {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return text.nameRequired }
        let taken = views.contains { view in
            view.id != id
                && view.name.trimmingCharacters(in: .whitespacesAndNewlines).caseInsensitiveCompare(trimmed)
                    == .orderedSame
        }
        return taken ? text.nameTaken : nil
    }

    /// The name cut to the longest allowed length.
    static func limit(_ name: String, to maxLength: Int) -> String {
        name.count > maxLength ? String(name.prefix(max(maxLength, 0))) : name
    }

    /// What the button shows: the selected view's name, or the no-view text.
    static func title(selected: MonarchSavedView?, text: MonarchSavedViewsText) -> String {
        selected?.name ?? text.noView
    }

    /// The value VoiceOver reads for the button: the view's name, then Edited when modified.
    static func accessibilityValue(selected: MonarchSavedView?, isModified: Bool, text: MonarchSavedViewsText) -> String
    {
        let name = title(selected: selected, text: text)
        return isModified ? "\(name), \(text.modified)" : name
    }
}
