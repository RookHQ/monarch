import MonarchUI
import SwiftUI

struct MonarchLoadMoreSection: CataloguePage {
    static let title = "Load more"

    @Environment(\.monarchTheme) private var theme
    @State private var items = Array(1...5)
    @State private var isLoading = false
    @State private var isFailed = false
    @State private var failsNext = false

    private let total = 18

    var body: some View {
        Toggle("Fail the next page", isOn: $failsNext)
            .monarchTextStyle(.body)
        CatalogueCaption("A list of \(total) invoices, five at a time")
        VStack(alignment: .leading, spacing: theme.spacing.space2) {
            ForEach(items, id: \.self) { item in
                Text("Invoice \(item)").monarchTextStyle(.body)
            }
        }
        MonarchLoadMore(
            hasMore: items.count < total,
            isLoading: isLoading,
            isFailed: isFailed,
            loadedCount: items.count,
            totalCount: total,
            onLoadMore: loadPage
        )
    }

    private func loadPage() {
        guard !isLoading else { return }
        isLoading = true
        Task {
            try? await Task.sleep(for: .seconds(1))
            if failsNext {
                isFailed = true
                failsNext = false
            } else {
                isFailed = false
                let next = items.count + 1
                items += Array(next...min(next + 4, total))
            }
            isLoading = false
        }
    }
}
