import MonarchUI
import SwiftUI

/// Recipe: the home screen of in-app help, a searchable list of articles by
/// category, presented in a sheet.
struct HelpCenterHomeSection: CataloguePage {
    static let title = "Help center home (recipe)"

    @State private var showsHelp = false

    var body: some View {
        CatalogueCaption("A Help button presents the help home in a sheet. Search filters as you type.")
        MonarchButton("Help", systemImage: "questionmark.circle") { showsHelp = true }
            .sheet(isPresented: $showsHelp) { HelpCenterHomeRecipe() }

        CatalogueCaption("Loading, error and empty states")
        MonarchLoadingState("Loading articles", variant: .list, rows: 3)
        MonarchErrorState(
            "Couldn’t load the help center",
            message: "Check your connection and try again.",
            kind: .offline,
            onRetry: {}
        )
        MonarchEmptyState(
            "No articles yet",
            message: "Articles appear here once they are published.",
            systemImage: "book.closed",
            size: .sm
        )
    }
}

private struct HelpCenterHomeArticle: Identifiable, Hashable {
    let id: String
    let title: String
    let category: String?
}

private struct HelpCenterHomeRecipe: View {
    @Environment(\.dismiss) private var dismiss
    @State private var query = ""

    private let articles = [
        HelpCenterHomeArticle(id: "reset", title: "Reset your password", category: "Account"),
        HelpCenterHomeArticle(id: "2fa", title: "Turn on two-step verification", category: "Account"),
        HelpCenterHomeArticle(id: "invoice", title: "Download an invoice", category: "Billing"),
        HelpCenterHomeArticle(id: "refund", title: "Ask for a refund", category: "Billing"),
        HelpCenterHomeArticle(id: "export", title: "Export your data", category: nil),
    ]

    /// Every typed word must appear in the title or category, ignoring case and accents.
    private var matches: [MonarchSearchResult] {
        let words = query.split(whereSeparator: \.isWhitespace)
        return
            articles
            .filter { article in
                let haystack = "\(article.title) \(article.category ?? "")"
                return words.allSatisfy { haystack.localizedStandardContains($0) }
            }
            .map { MonarchSearchResult($0.title, id: $0.id, path: [$0.category ?? "General"]) }
    }

    /// Categories in first-seen order; articles without one under General.
    private var categories: [(name: String, articles: [HelpCenterHomeArticle])] {
        var order: [String] = []
        var groups: [String: [HelpCenterHomeArticle]] = [:]
        for article in articles {
            let name = article.category ?? "General"
            if groups[name] == nil { order.append(name) }
            groups[name, default: []].append(article)
        }
        return order.map { ($0, groups[$0] ?? []) }
    }

    var body: some View {
        NavigationStack {
            Group {
                if query.isEmpty {
                    List {
                        ForEach(categories, id: \.name) { category in
                            Section(category.name) {
                                ForEach(category.articles) { article in
                                    NavigationLink(article.title, value: article)
                                        .monarchTextStyle(.body)
                                }
                            }
                        }
                        Section {
                            MonarchSectionHeader(
                                "Still need help?",
                                description: "Our team answers within one working day."
                            )
                            if let url = URL(string: "https://example.com/support") {
                                Link("Contact support", destination: url)
                                    .buttonStyle(.monarchLink(.standalone, isExternal: true))
                            }
                        }
                    }
                } else {
                    MonarchSearchResults(
                        groups: [MonarchSearchResultGroup(nil, id: "articles", results: matches)],
                        query: query,
                        onSelect: { _, _ in }
                    )
                }
            }
            .searchable(text: $query, prompt: "Search articles")
            .navigationTitle("Help center")
            .navigationDestination(for: HelpCenterHomeArticle.self) { article in
                Text(article.title)
                    .monarchTextStyle(.body)
                    .navigationTitle(article.title)
            }
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
