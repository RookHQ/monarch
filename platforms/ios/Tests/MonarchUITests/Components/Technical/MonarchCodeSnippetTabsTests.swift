import SwiftUI
import Testing

@testable import MonarchUI

private let codeSnippetTabsSamples = [
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
    MonarchCodeSnippet(
        "Node",
        code: "await fetch(\"https://api.example.com/v1/items\");",
        language: "js",
        title: "items.mjs"
    ),
]

enum MonarchCodeSnippetTabsFixtures: ComponentFixtures {
    static let id = "code-snippet-tabs"
    static let fixtures = [
        Fixture("three languages, line tabs") {
            MonarchCodeSnippetTabs(snippets: codeSnippetTabsSamples)
        },
        Fixture("pill tabs, line numbers, second selected") {
            MonarchCodeSnippetTabs(
                "Request",
                snippets: codeSnippetTabsSamples,
                selection: .constant("python"),
                variant: .pill,
                showsLineNumbers: true
            )
        },
        Fixture("one snippet, no tabs") {
            MonarchCodeSnippetTabs(snippets: [MonarchCodeSnippet("JSON", code: "{ \"ok\": true }", language: "json")])
        },
        Fixture("no snippets") {
            MonarchCodeSnippetTabs(snippets: [])
        },
        Fixture("many long labels") {
            MonarchCodeSnippetTabs(
                snippets: (1...8).map {
                    MonarchCodeSnippet("Language with a long name \($0)", code: "print(\($0))", language: "python")
                }
            )
        },
    ]
}

@Suite("MonarchCodeSnippetTabs")
struct MonarchCodeSnippetTabsTests {
    typealias Tokens = MonarchCodeSnippetTabsTokens

    @Test("Snippet ids default to the label, as on the web")
    func ids() {
        #expect(codeSnippetTabsSamples.map(\.id) == ["cURL", "python", "Node"])
    }

    @Test("The app's selection wins, then the stored choice, then the own one, then the first")
    func resolution() {
        let samples = codeSnippetTabsSamples
        #expect(Tokens.resolvedSelection(external: "Node", stored: "python", own: "cURL", snippets: samples) == "Node")
        #expect(Tokens.resolvedSelection(external: nil, stored: "python", own: "Node", snippets: samples) == "python")
        #expect(Tokens.resolvedSelection(external: nil, stored: nil, own: "Node", snippets: samples) == "Node")
        #expect(Tokens.resolvedSelection(external: nil, stored: nil, own: nil, snippets: samples) == "cURL")
        #expect(Tokens.resolvedSelection(external: nil, stored: nil, own: nil, snippets: []) == nil)
    }

    @Test("A set ignores a stored choice it has no snippet for")
    func unknownChoices() {
        let samples = codeSnippetTabsSamples
        #expect(Tokens.resolvedSelection(external: "ruby", stored: "go", own: "python", snippets: samples) == "python")
        #expect(Tokens.resolvedSelection(external: nil, stored: "", own: nil, snippets: samples) == "cURL")
    }

    @Test("Sets with the same sync key share one UserDefaults key")
    func storageKey() {
        #expect(Tokens.storageKey("api-docs") == "monarch.codeSnippetTabs.api-docs")
        #expect(Tokens.storageKey("api-docs") == Tokens.storageKey("api-docs"))
        #expect(Tokens.storageKey("a") != Tokens.storageKey("b"))
    }

    @Test("Tabs show only with more than one snippet")
    func tabs() {
        #expect(Tokens.showsTabs(count: 0) == false)
        #expect(Tokens.showsTabs(count: 1) == false)
        #expect(Tokens.showsTabs(count: 2))
    }

    @Test("Repeated ids keep the first snippet")
    func unique() {
        let repeated = codeSnippetTabsSamples + [MonarchCodeSnippet("Python", code: "pass", id: "python")]
        #expect(Tokens.unique(repeated).count == 3)
        #expect(Tokens.unique(repeated)[1].code.hasPrefix("import requests"))
    }

    @Test("The strip's built-in name resolves and can be replaced")
    func label() {
        #expect(Tokens.label(nil) == "Language")
        #expect(Tokens.label("Sprache") == "Sprache")
    }
}
