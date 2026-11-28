import MonarchUI
import SwiftUI

struct MonarchCursorListSection: CataloguePage {
    static let title = "Cursor list"

    private struct Request: Identifiable {
        let id: Int
    }

    @State private var items: [Request] = []
    @State private var isLoading = false
    @State private var isFailed = false
    @State private var failsNext = false
    @State private var isEmpty = false

    private let total = 23
    private let pageSize = 8

    var body: some View {
        Toggle("Fail the next page", isOn: $failsNext).monarchTextStyle(.body)
        Toggle("Empty workspace", isOn: $isEmpty).monarchTextStyle(.body)
        Button("Start over") {
            items = []
            isFailed = false
            loadPage()
        }
        CatalogueCaption("Access requests, \(pageSize) at a time")
        MonarchCursorList(
            items,
            label: "Access requests",
            hasMore: !isEmpty && items.count < total,
            isLoading: isLoading,
            isFailed: isFailed,
            totalCount: isEmpty ? 0 : total,
            onLoadMore: loadPage,
            row: { request in
                VStack(alignment: .leading) {
                    Text("Access request \(request.id)").monarchTextStyle(.body)
                    Text("Waiting for review").monarchTextStyle(.caption)
                }
            }
        )
        .frame(minHeight: 420)
        .onAppear { if items.isEmpty { loadPage() } }
    }

    private func loadPage() {
        guard !isLoading else { return }
        isLoading = true
        Task {
            try? await Task.sleep(for: .seconds(1))
            if failsNext {
                isFailed = true
                failsNext = false
            } else if !isEmpty {
                isFailed = false
                let next = items.count + 1
                items += (next...min(next + pageSize - 1, total)).map(Request.init)
            }
            isLoading = false
        }
    }
}
