import MonarchUI
import SwiftUI

/// Recipe: a native list loads the next page at its end instead of showing numbered pages.
struct PaginationSection: CataloguePage {
    static let title = "Pagination (recipe)"

    @State private var invoices = Array(1...20)
    @State private var isLoading = false
    @State private var isFailed = false
    @State private var failsNext = false
    private let total = 60
    private let pageSize = 20

    var body: some View {
        Toggle("Fail the next page", isOn: $failsNext)
            .monarchTextStyle(.body)
        List {
            ForEach(invoices, id: \.self) { number in
                Text("Invoice \(number)")
                    .monarchTextStyle(.body)
            }
            MonarchLoadMore(
                hasMore: invoices.count < total,
                isLoading: isLoading,
                isFailed: isFailed,
                loadedCount: invoices.count,
                totalCount: total,
                onRetry: { loadNextPage() },
                onLoadMore: { loadNextPage() }
            )
            .listRowSeparator(.hidden)
        }
        .listStyle(.plain)
        .frame(minHeight: MonarchAccessibility.minimumHitTarget * 9)
        MonarchButton("Start over", variant: .ghost, size: .sm) {
            invoices = Array(1...pageSize)
            isFailed = false
        }
    }

    private func loadNextPage() {
        guard !isLoading else { return }
        isLoading = true
        isFailed = false
        Task {
            try? await Task.sleep(for: .milliseconds(600))
            if failsNext {
                failsNext = false
                isFailed = true
            } else {
                let next = invoices.count + 1
                invoices += Array(next..<min(next + pageSize, total + 1))
            }
            isLoading = false
        }
    }
}
