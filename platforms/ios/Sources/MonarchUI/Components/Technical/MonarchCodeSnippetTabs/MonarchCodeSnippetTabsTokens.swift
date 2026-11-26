import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchCodeSnippetTabs``.
enum MonarchCodeSnippetTabsTokens {
    /// Space between the tabs and the code.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The prefix of the `UserDefaults` key that keeps a synced choice, as
    /// the web's `localStorage` key.
    static let storagePrefix = "monarch.codeSnippetTabs."

    /// The `UserDefaults` key for a sync key.
    static func storageKey(_ syncKey: String?) -> String {
        storagePrefix + (syncKey ?? "")
    }

    /// The selected snippet's id: the app's selection, then the stored
    /// synced choice, then the component's own, each only when a snippet has
    /// that id; otherwise the first snippet. `nil` when there are no snippets.
    static func resolvedSelection(
        external: String?,
        stored: String?,
        own: String?,
        snippets: [MonarchCodeSnippet]
    ) -> String? {
        let ids = Set(snippets.map(\.id))
        for candidate in [external, stored, own] {
            if let candidate, ids.contains(candidate) { return candidate }
        }
        return snippets.first?.id
    }

    /// Whether the tab strip is shown: only with more than one snippet.
    static func showsTabs(count: Int) -> Bool {
        count > 1
    }

    /// The tab strip's spoken name, with the override applied.
    static func label(_ label: String?) -> String {
        label ?? MonarchCodeSnippetTabsStrings.label
    }

    /// The snippets with repeated ids left out, keeping the first.
    static func unique(_ snippets: [MonarchCodeSnippet]) -> [MonarchCodeSnippet] {
        var seen = Set<String>()
        return snippets.filter { seen.insert($0.id).inserted }
    }
}
