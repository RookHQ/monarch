import MonarchUI
import SwiftUI

struct MonarchCodeSnippetTabsSection: CataloguePage {
    static let title = "Code snippet tabs"

    @State private var selection = "python"
    @State private var copied = "Nothing yet"
    @Environment(\.monarchTheme) private var theme

    private let request = [
        MonarchCodeSnippet(
            "cURL",
            code: "curl https://api.example.com/v1/items \\\n  -H \"Authorization: Bearer $TOKEN\"",
            language: "bash"
        ),
        MonarchCodeSnippet(
            "Python",
            code: "import requests\n\nrequests.get(\"https://api.example.com/v1/items\")",
            language: "python",
            id: "python"
        ),
        MonarchCodeSnippet("Node", code: "await fetch(\"https://api.example.com/v1/items\");", language: "js"),
    ]

    private let response = [
        MonarchCodeSnippet("cURL", code: "curl -X DELETE https://api.example.com/v1/items/42", language: "bash"),
        MonarchCodeSnippet(
            "Python",
            code: "requests.delete(\"https://api.example.com/v1/items/42\")",
            language: "python",
            id: "python"
        ),
    ]

    var body: some View {
        CatalogueCaption("Two sets that follow each other (sync key)")
        MonarchCodeSnippetTabs("List items", snippets: request, syncKey: "catalogue-api") { copied = $0 }
        MonarchCodeSnippetTabs("Delete an item", snippets: response, syncKey: "catalogue-api", variant: .pill)
        Text("Copied: \(copied)")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
            .lineLimit(1)
        CatalogueCaption("Controlled by the app")
        MonarchCodeSnippetTabs(snippets: request, selection: $selection, showsLineNumbers: true)
        Text("Selection: \(selection)")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }
}
