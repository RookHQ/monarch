import MonarchUI
import SwiftUI

struct MonarchSearchResultsSection: CataloguePage {
    static let title = "Search results"

    private enum Phase: String, CaseIterable, Identifiable {
        case results = "Results"
        case loading = "Loading"
        case empty = "Empty"
        case failed = "Failed"
        var id: Self { self }
    }

    @Environment(\.monarchTheme) private var theme
    @State private var phase = Phase.results
    @State private var query = "travel"
    @State private var opened = "Nothing yet"

    private let groups = [
        MonarchSearchResultGroup(
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
                    snippet: "Claim travel costs within 30 days."
                ),
            ],
            total: 24,
            systemImage: "doc.text"
        ),
        MonarchSearchResultGroup(
            "People",
            id: "people",
            results: [
                MonarchSearchResult("Ana Silva", id: "p-1", snippet: "Travel coordinator", systemImage: "person")
            ],
            systemImage: "person.2"
        ),
    ]

    var body: some View {
        Picker("State", selection: $phase) {
            ForEach(Phase.allCases) { Text($0.rawValue).tag($0) }
        }
        .pickerStyle(.segmented)
        TextField("Search", text: $query)
            .textFieldStyle(.roundedBorder)
            .monarchTextStyle(.body)
        MonarchSearchResults(
            groups: phase == .results ? groups : [],
            query: query,
            isLoading: phase == .loading,
            isFailed: phase == .failed,
            errorMessage: "The search service did not respond.",
            onRetry: { phase = .results },
            onShowAll: { opened = "All in \($0.title ?? "results")" },
            onSelect: { result, _ in opened = result.title }
        )
        .frame(minHeight: MonarchAccessibility.minimumHitTarget * 10)
        Text("Opened: \(opened)")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }
}
