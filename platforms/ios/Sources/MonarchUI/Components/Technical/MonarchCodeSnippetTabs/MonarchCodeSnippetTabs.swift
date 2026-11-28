import SwiftUI

/// The same code example in several languages or tools, one tab each, with
/// a copy button on the code.
///
/// ```swift
/// MonarchCodeSnippetTabs(
///     snippets: [
///         MonarchCodeSnippet("cURL", code: curl, language: "bash"),
///         MonarchCodeSnippet("Python", code: python, language: "python"),
///         MonarchCodeSnippet("Node", code: node, language: "js"),
///     ],
///     syncKey: "api-docs"
/// )
/// ```
///
/// A ``MonarchTabs`` strip over a ``MonarchCodeBlock`` that shows the
/// selected snippet. With `syncKey`, the choice is kept in `UserDefaults`
/// through `@AppStorage`, so every snippet set with the same key shows the
/// same language, now and on the next launch. A set only follows a choice it
/// has a snippet for. With one snippet there is no tab strip.
///
/// Accessibility: the strip is a tab bar named "Language" and each tab
/// reads its label with the selected trait. The code block is named after
/// its title or language and reads line by line; the copy button announces
/// the copy. Both scale with Dynamic Type.
public struct MonarchCodeSnippetTabs: View {
    private let label: String?
    private let snippets: [MonarchCodeSnippet]
    private let externalSelection: Binding<String>?
    private let syncKey: String?
    private let variant: MonarchTabsVariant
    private let showsLineNumbers: Bool
    private let codeBlockLabels: MonarchCodeBlockLabels
    private let onCopy: (@MainActor (String) -> Void)?

    @State private var own: String?
    @AppStorage private var stored: String

    /// Snippet tabs.
    ///
    /// - Parameters:
    ///   - label: The spoken name of the tab strip. Built in: "Language".
    ///   - snippets: The same example in each language, in tab order. Ids are unique; repeats are left out.
    ///   - selection: The selected snippet's id, when the app controls it.
    ///   - syncKey: Keeps every snippet set with this key on the same
    ///     language and remembers it in `UserDefaults`. Use one key per app
    ///     area, such as `"api-docs"`.
    ///   - variant: A line under the selected tab, or a pill on a track.
    ///   - showsLineNumbers: Whether the code shows line numbers.
    ///   - codeBlockLabels: Replacements for the code block's built-in strings.
    ///   - onCopy: Runs with the copied code after each copy.
    public init(
        _ label: String? = nil,
        snippets: [MonarchCodeSnippet],
        selection: Binding<String>? = nil,
        syncKey: String? = nil,
        variant: MonarchTabsVariant = .line,
        showsLineNumbers: Bool = false,
        codeBlockLabels: MonarchCodeBlockLabels = MonarchCodeBlockLabels(),
        onCopy: (@MainActor (String) -> Void)? = nil
    ) {
        self.label = label
        self.snippets = MonarchCodeSnippetTabsTokens.unique(snippets)
        self.externalSelection = selection
        self.syncKey = syncKey
        self.variant = variant
        self.showsLineNumbers = showsLineNumbers
        self.codeBlockLabels = codeBlockLabels
        self.onCopy = onCopy
        _stored = AppStorage(wrappedValue: "", MonarchCodeSnippetTabsTokens.storageKey(syncKey))
    }

    private var selectedID: String? {
        MonarchCodeSnippetTabsTokens.resolvedSelection(
            external: externalSelection?.wrappedValue,
            stored: syncKey == nil ? nil : stored,
            own: own,
            snippets: snippets
        )
    }

    private var selectionBinding: Binding<String> {
        Binding(
            get: { selectedID ?? "" },
            set: { newValue in
                own = newValue
                externalSelection?.wrappedValue = newValue
                if syncKey != nil {
                    stored = newValue
                }
            }
        )
    }

    /// The content of the view.
    public var body: some View {
        let selected = snippets.first { $0.id == selectedID }
        VStack(alignment: .leading, spacing: MonarchCodeSnippetTabsTokens.spacing) {
            if MonarchCodeSnippetTabsTokens.showsTabs(count: snippets.count) {
                MonarchTabs(
                    MonarchCodeSnippetTabsTokens.label(label),
                    selection: selectionBinding,
                    items: snippets.map { MonarchTabsItem($0.label, value: $0.id) },
                    variant: variant
                )
            }
            if let selected {
                MonarchCodeBlock(
                    selected.code,
                    language: selected.language,
                    title: selected.title ?? selected.label,
                    showsLineNumbers: showsLineNumbers,
                    labels: codeBlockLabels,
                    onCopy: onCopy
                )
                .id(selected.id)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

#if DEBUG
    #Preview("MonarchCodeSnippetTabs") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchCodeSnippetTabs(
                    snippets: [
                        MonarchCodeSnippet("cURL", code: "curl https://api.example.com/v1/items", language: "bash"),
                        MonarchCodeSnippet(
                            "Python",
                            code: "import requests\nrequests.get(\"https://api.example.com/v1/items\")",
                            language: "python"
                        ),
                    ]
                )
                MonarchCodeSnippetTabs(
                    snippets: [MonarchCodeSnippet("JSON", code: "{ \"ok\": true }", language: "json")],
                    variant: .pill
                )
            }
        }
    }
#endif
