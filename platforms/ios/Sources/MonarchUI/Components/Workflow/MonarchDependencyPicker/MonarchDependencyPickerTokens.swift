import Foundation
import MonarchTokens
import SwiftUI

/// The resolved strings of ``MonarchDependencyPicker``: the app's replacements, else the built-in ones.
struct MonarchDependencyPickerText {
    let labels: MonarchDependencyPickerLabels

    private func pick(_ value: String?, _ key: String) -> String { value ?? MonarchDependencyPickerStrings.string(key) }

    var relationLabel: String { pick(labels.relationLabel, "relationLabel") }
    var searchLabel: String { pick(labels.searchLabel, "searchLabel") }
    var searchPrompt: String { pick(labels.searchPrompt, "searchPrompt") }
    var noResults: String { pick(labels.noResults, "noResults") }
    var empty: String { pick(labels.empty, "empty") }

    func relation(_ relation: MonarchDependencyRelation) -> String {
        labels.relation?(relation) ?? MonarchDependencyPickerStrings.string("relation.\(relation.rawValue)")
    }

    func remove(_ relation: String, _ item: String) -> String {
        labels.remove?(relation, item) ?? MonarchDependencyPickerStrings.string("remove", relation, item)
    }

    func added(_ relation: String, _ item: String) -> String {
        labels.added?(relation, item) ?? MonarchDependencyPickerStrings.string("added", relation, item)
    }

    func removed(_ relation: String, _ item: String) -> String {
        labels.removed?(relation, item) ?? MonarchDependencyPickerStrings.string("removed", relation, item)
    }

    func status(_ name: String) -> String {
        labels.status?(name) ?? MonarchDependencyPickerStrings.string("status", name)
    }
}

/// The links of one relation, in the list.
struct MonarchDependencyPickerGroup: Equatable {
    let relation: MonarchDependencyRelation
    let links: [MonarchDependencyLink]
}

/// Component tokens and logic for ``MonarchDependencyPicker``.
enum MonarchDependencyPickerTokens {
    /// The gap between relation groups and the add controls.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between the rows of a group.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the parts of a row.
    static let partSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the lines of a row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The symbol of the remove buttons.
    static let removeSymbol = "xmark"

    /// The colour role of an item's key.
    static func keyColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// The colour role of an item's title: the link role when it opens a URL.
    static func titleColor(isLink: Bool, isEnabled: Bool) -> MonarchColorRole {
        guard isEnabled else { return \.textDisabled }
        return isLink ? \.textLink : \.textDefault
    }

    /// The links grouped by relation, in the order of `relations`, leaving out empty groups.
    static func groups(
        _ links: [MonarchDependencyLink],
        relations: [MonarchDependencyRelation]
    ) -> [MonarchDependencyPickerGroup] {
        relations.compactMap { relation in
            let matching = links.filter { $0.relation == relation }
            return matching.isEmpty ? nil : MonarchDependencyPickerGroup(relation: relation, links: matching)
        }
    }

    /// The items that can be linked: not excluded and not already linked.
    static func candidates(
        _ items: [MonarchDependencyItem],
        links: [MonarchDependencyLink],
        excluding excluded: Set<String>
    ) -> [MonarchDependencyItem] {
        let linked = Set(links.map(\.item.id))
        return items.filter { !excluded.contains($0.id) && !linked.contains($0.id) }
    }

    /// An item as a search option: its title, its key as a hint (and searched), its status as the description.
    static func option(_ item: MonarchDependencyItem) -> MonarchComboboxOption {
        MonarchComboboxOption(
            id: item.id,
            label: item.title,
            description: item.status?.label,
            hint: item.key,
            keywords: item.key
        )
    }

    /// The name of an item: "STR-12 Fix login", or the title alone.
    static func name(_ item: MonarchDependencyItem) -> String {
        guard let key = item.key, !key.isEmpty else { return item.title }
        return "\(key) \(item.title)"
    }

    /// What VoiceOver reads for a row: the item's name, then its status.
    static func rowLabel(_ item: MonarchDependencyItem, text: MonarchDependencyPickerText) -> String {
        guard let status = item.status else { return name(item) }
        return "\(name(item)), \(text.status(status.label))"
    }

    /// The URL an item opens, only when it is `http` or `https`.
    static func safeURL(_ item: MonarchDependencyItem) -> URL? {
        guard let url = item.url, let scheme = url.scheme?.lowercased(), scheme == "http" || scheme == "https" else {
            return nil
        }
        return url
    }

    /// The relation chosen at first: the preferred one when offered, else the first offered.
    static func startRelation(
        _ relations: [MonarchDependencyRelation],
        preferred: MonarchDependencyRelation?
    ) -> MonarchDependencyRelation {
        if let preferred, relations.contains(preferred) { return preferred }
        return relations.first ?? .blockedBy
    }
}
