import SwiftUI
import Testing

@testable import MonarchUI

private let searchResultsDocuments = MonarchSearchResultGroup(
    "Documents",
    id: "docs",
    results: [
        MonarchSearchResult(
            "Travel policy",
            id: "doc-1",
            path: ["Handbook", "Finance"],
            snippet: "Book travel at least two weeks ahead.",
            fileName: "travel-policy.pdf",
            meta: "PDF"
        ),
        MonarchSearchResult(
            "Expenses",
            id: "doc-2",
            path: ["Handbook"],
            snippet: "Claim <mark>travel</mark> costs within 30 days.",
            snippetHasMarkTags: true
        ),
    ],
    total: 24,
    systemImage: "doc.text"
)

private let searchResultsPeople = MonarchSearchResultGroup(
    "People",
    id: "people",
    results: [MonarchSearchResult("Ana Silva", id: "person-1", snippet: "Travel coordinator", systemImage: "person")]
)

enum MonarchSearchResultsFixtures: ComponentFixtures {
    static let id = "search-results"
    static let fixtures = [
        Fixture("grouped, with show all") {
            MonarchSearchResults(
                groups: [searchResultsDocuments, searchResultsPeople],
                query: "travel",
                onShowAll: { _ in },
                onSelect: { _, _ in }
            )
            .frame(minHeight: MonarchAccessibility.minimumHitTarget * 10)
        },
        Fixture("ungrouped, refreshing") {
            MonarchSearchResults(
                groups: [MonarchSearchResultGroup(nil, id: "all", results: searchResultsPeople.results)],
                query: "ana",
                isLoading: true
            ) { _, _ in }
            .frame(minHeight: MonarchAccessibility.minimumHitTarget * 5)
        },
        Fixture("loading, no results yet") {
            MonarchSearchResults(groups: [], query: "travel", isLoading: true) { _, _ in }
        },
        Fixture("empty") {
            MonarchSearchResults(
                groups: [MonarchSearchResultGroup("Documents", id: "docs", results: [])],
                query: "zzz",
                onSelect: { _, _ in }
            )
        },
        Fixture("failed, with retry") {
            MonarchSearchResults(
                groups: [searchResultsPeople],
                isFailed: true,
                errorMessage: "The search service did not respond.",
                onRetry: {},
                onSelect: { _, _ in }
            )
        },
    ]
}

@Suite("MonarchSearchResults")
struct MonarchSearchResultsTests {
    typealias Tokens = MonarchSearchResultsTokens
    private let labels = MonarchSearchResultsLabels()

    @Test("Error wins, then results, then loading, then empty")
    func status() {
        #expect(Tokens.status(isFailed: true, resultCount: 3, isLoading: true) == .error)
        #expect(Tokens.status(isFailed: false, resultCount: 3, isLoading: true) == .ready)
        #expect(Tokens.status(isFailed: false, resultCount: 0, isLoading: true) == .loading)
        #expect(Tokens.status(isFailed: false, resultCount: 0, isLoading: false) == .empty)
    }

    @Test("Empty groups are left out and totals never undercount")
    func totals() {
        let empty = MonarchSearchResultGroup("Empty", id: "e", results: [])
        let short = MonarchSearchResultGroup("Short", id: "s", results: searchResultsPeople.results, total: 0)
        #expect(
            Tokens.visibleGroups([searchResultsDocuments, empty, searchResultsPeople]).map(\.id) == ["docs", "people"]
        )
        #expect(Tokens.total(of: searchResultsDocuments) == 24)
        #expect(Tokens.total(of: searchResultsPeople) == 1)
        #expect(Tokens.total(of: short) == 1)
        #expect(Tokens.total(of: [searchResultsDocuments, empty, searchResultsPeople]) == 25)
    }

    @Test("The summary names the count and the query, or says it is searching")
    func summary() {
        #expect(
            Tokens.summary(status: .ready, isLoading: false, total: 12, query: "travel", labels: labels)
                == "12 results for “travel”"
        )
        #expect(
            Tokens.summary(status: .ready, isLoading: false, total: 1, query: "travel", labels: labels)
                == "1 result for “travel”"
        )
        #expect(Tokens.summary(status: .ready, isLoading: false, total: 3, query: "  ", labels: labels) == "3 results")
        #expect(Tokens.summary(status: .ready, isLoading: true, total: 3, query: "a", labels: labels) == "Searching…")
        #expect(Tokens.summary(status: .loading, isLoading: true, total: 0, query: "a", labels: labels) == "Searching…")
        #expect(
            Tokens.summary(status: .empty, isLoading: false, total: 0, query: "zzz", labels: labels)
                == "No results for “zzz”"
        )
        #expect(Tokens.summary(status: .empty, isLoading: false, total: 0, query: "", labels: labels) == "No results")
        #expect(Tokens.summary(status: .error, isLoading: false, total: 0, query: "a", labels: labels).isEmpty)
    }

    @Test("Show all appears only with a handler and more matches than shown")
    func showAll() {
        #expect(Tokens.showsShowAll(searchResultsDocuments, hasHandler: true))
        #expect(Tokens.showsShowAll(searchResultsDocuments, hasHandler: false) == false)
        #expect(Tokens.showsShowAll(searchResultsPeople, hasHandler: true) == false)
        #expect(Tokens.showAllTitle(searchResultsDocuments, labels: labels) == "Show all 24 in Documents")
        let untitled = MonarchSearchResultGroup(nil, id: "u", results: searchResultsPeople.results, total: 9)
        #expect(Tokens.showAllTitle(untitled, labels: labels) == "Show all 9")
    }

    @Test("Headings are read with their count")
    func headers() {
        #expect(Tokens.headerAccessibilityLabel(searchResultsDocuments, labels: labels) == "Documents, 24 results")
        #expect(Tokens.headerAccessibilityLabel(searchResultsPeople, labels: labels) == "People, 1 result")
        #expect(Tokens.countText(searchResultsDocuments) == 24.formatted())
    }

    @Test("The built-in state strings resolve")
    func stateStrings() {
        #expect(Tokens.errorTitle(labels: labels) == "Search failed")
        #expect(Tokens.loading(labels: labels) == "Searching…")
        #expect(Tokens.emptyMessage(labels: labels) == "Try other words, check the spelling, or remove a filter.")
    }

    @Test("Overrides replace the strings, including counted ones")
    func overrides() {
        let custom = MonarchSearchResultsLabels(
            summary: { count, query in "\(count) Treffer für \(query)" },
            loading: "Suche…",
            emptyTitle: { _ in "Keine Treffer" },
            errorTitle: "Fehler",
            showAll: { title, total in "Alle \(total) in \(title ?? "-")" },
            groupCount: { "\($0) Treffer" }
        )
        #expect(
            Tokens.summary(status: .ready, isLoading: false, total: 2, query: "x", labels: custom) == "2 Treffer für x"
        )
        #expect(
            Tokens.summary(status: .empty, isLoading: false, total: 0, query: "x", labels: custom) == "Keine Treffer"
        )
        #expect(Tokens.loading(labels: custom) == "Suche…")
        #expect(Tokens.errorTitle(labels: custom) == "Fehler")
        #expect(Tokens.showAllTitle(searchResultsDocuments, labels: custom) == "Alle 24 in Documents")
        #expect(Tokens.headerAccessibilityLabel(searchResultsDocuments, labels: custom) == "Documents, 24 Treffer")
    }
}
