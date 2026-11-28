import Foundation
import MonarchTokens
import SwiftUI

/// The resolved strings of ``MonarchScopeSelector``: the app's replacements, else the built-in ones.
struct MonarchScopeSelectorText {
    let labels: MonarchScopeSelectorLabels

    private func pick(_ value: String?, _ key: String) -> String { value ?? MonarchScopeSelectorStrings.string(key) }

    var empty: String { pick(labels.empty, "empty") }
    var loading: String { pick(labels.loading, "loading") }
    var noScopes: String { pick(labels.noScopes, "noScopes") }
    var selectAll: String { pick(labels.selectAll, "selectAll") }
    var clearAll: String { MonarchScopeSelectorStrings.string("clearAll") }

    func count(_ selected: Int, _ total: Int) -> String {
        labels.count?(selected, total) ?? MonarchScopeSelectorStrings.count(selected, total)
    }

    func showMore(_ next: Int, _ remaining: Int) -> String {
        labels.showMore?(next, remaining) ?? MonarchScopeSelectorStrings.showMore(next, remaining)
    }
}

/// Component tokens and selection logic for ``MonarchScopeSelector``.
enum MonarchScopeSelectorTokens {
    /// The gap between groups.
    static let groupSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the scopes of a group.
    static let scopeSpacing: CGFloat = MonarchTokens.Space.space1
    /// How far the scopes are indented under Select all.
    static let nestedIndent: CGFloat = MonarchTokens.Space.space6
    /// The gap between the lines of a group header and of a scope.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between a group's icon and its name.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space2

    /// The colour role of a group's summary: muted, as the web; the count says it in words.
    static func summaryColor(selected: Int, isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// The colour role of secondary text.
    static func secondaryColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// The scope values select all acts on: those not disabled.
    static func enabledValues(_ group: MonarchScopeGroup) -> [String] {
        group.scopes.filter { !$0.isDisabled }.map(\.value)
    }

    /// How many of a group's scopes are selected.
    static func selectedCount(_ group: MonarchScopeGroup, selection: Set<String>) -> Int {
        group.scopes.filter { selection.contains($0.value) }.count
    }

    /// The summary under a group's name: loading, no scopes, or "1 of 2 scopes".
    static func summary(_ group: MonarchScopeGroup, selection: Set<String>, text: MonarchScopeSelectorText) -> String {
        if group.isLoading { return text.loading }
        if group.scopes.isEmpty { return group.emptyMessage ?? text.noScopes }
        return text.count(selectedCount(group, selection: selection), group.scopes.count)
    }

    /// The selection with one scope selected or cleared.
    static func setting(_ value: String, isOn: Bool, in selection: Set<String>) -> Set<String> {
        var next = selection
        if isOn { next.insert(value) } else { next.remove(value) }
        return next
    }

    /// The groups shown with a page limit, and the paging button's numbers when some are hidden.
    static func page(
        _ groups: [MonarchScopeGroup],
        limit: Int,
        pageSize: Int
    ) -> (visible: [MonarchScopeGroup], next: Int, remaining: Int) {
        let visible = Array(groups.prefix(max(limit, 0)))
        let remaining = groups.count - visible.count
        return (visible, min(max(pageSize, 1), remaining), remaining)
    }

    /// Auto-selection: every enabled scope of each group seen for the first time after it has
    /// loaded, when none of its scopes is selected. `seen` is `nil` before the first run; with
    /// `.added` the groups present then are only marked as seen.
    static func autoSelect(
        _ groups: [MonarchScopeGroup],
        selection: Set<String>,
        seen: Set<String>?,
        mode: MonarchScopeSelectorAutoSelect?
    ) -> (selection: Set<String>, seen: Set<String>?) {
        guard let mode else { return (selection, seen) }
        var marked = seen ?? (mode == .added ? Set(groups.map(\.id)) : [])
        var next = selection
        for group in groups where !marked.contains(group.id) && !group.isLoading {
            marked.insert(group.id)
            if !group.scopes.contains(where: { selection.contains($0.value) }) {
                next.formUnion(enabledValues(group))
            }
        }
        return (next, marked)
    }

    /// The name VoiceOver reads for a scope: its label, else the scope string.
    static func scopeName(_ scope: MonarchScopeOption) -> String {
        scope.label ?? scope.value
    }
}
