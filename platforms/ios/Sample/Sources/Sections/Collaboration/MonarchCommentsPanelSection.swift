@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchCommentsPanelSection: CataloguePage {
    static let title = "CommentsPanel"

    @State private var isOpen = false
    @State private var isLoading = false
    @State private var failsNext = false
    @State private var filter = MonarchCommentsPanelFilter.open
    @State private var selectedThreadID: String?
    @State private var threads: [MonarchCommentsPanelThread] = [
        MonarchCommentsPanelThread(
            id: "t1",
            comments: [
                MonarchCommentThreadComment(
                    id: "c1",
                    author: "Priya Raman",
                    body: "Should this say \"billed every quarter\" instead?",
                    createdAt: Date.now.addingTimeInterval(-3_000)
                ),
                MonarchCommentThreadComment(
                    id: "c2",
                    author: "Tom Becker",
                    body: "Yes. Monthly billing was retired last year.",
                    createdAt: Date.now.addingTimeInterval(-1_200),
                    canDelete: true
                ),
            ],
            quote: "Billed monthly, cancel any time."
        ),
        MonarchCommentsPanelThread(
            id: "t2",
            comments: [
                MonarchCommentThreadComment(
                    id: "c3",
                    author: "Lee Park",
                    body: "Can we drop the footnote under the pricing table?",
                    createdAt: Date.now.addingTimeInterval(-600)
                )
            ]
        ),
        MonarchCommentsPanelThread(
            id: "t3",
            comments: [
                MonarchCommentThreadComment(
                    id: "c4",
                    author: "Ana Lopez",
                    body: "Prices are shown with tax now.",
                    createdAt: Date.now.addingTimeInterval(-90_000)
                )
            ],
            quote: "All prices include tax.",
            isResolved: true
        ),
    ]

    var body: some View {
        CatalogueCaption("In a sheet")
        Toggle("Show loading", isOn: $isLoading)
        Toggle("Fail the next action", isOn: $failsNext)
        Text(selectedThreadID.map { "Highlighted passage of \($0)" } ?? "No passage highlighted")
            .monarchTextStyle(.caption)
        MonarchButton("Comments", systemImage: "text.bubble") { isOpen = true }
            .sheet(isPresented: $isOpen) {
                NavigationStack {
                    MonarchCommentsPanel(
                        isLoading ? nil : threads,
                        filter: $filter,
                        selectedThreadID: $selectedThreadID,
                        onClose: { isOpen = false },
                        onReply: { id, text in
                            try await check()
                            update(id) { thread in
                                let reply = MonarchCommentThreadComment(
                                    id: UUID().uuidString,
                                    author: "You",
                                    body: text,
                                    createdAt: .now,
                                    canDelete: true
                                )
                                return copy(thread, comments: thread.comments + [reply])
                            }
                        },
                        onResolvedChange: { id, isResolved in
                            try await check()
                            update(id) { copy($0, isResolved: isResolved) }
                        },
                        onDeleteComment: { id, commentID in
                            try await check()
                            update(id) { copy($0, comments: $0.comments.filter { $0.id != commentID }) }
                        }
                    )
                }
                .presentationDetents([.medium, .large])
            }
    }

    private func update(_ id: String, _ change: (MonarchCommentsPanelThread) -> MonarchCommentsPanelThread) {
        guard let index = threads.firstIndex(where: { $0.id == id }) else { return }
        threads[index] = change(threads[index])
    }

    private func copy(
        _ thread: MonarchCommentsPanelThread,
        comments: [MonarchCommentThreadComment]? = nil,
        isResolved: Bool? = nil
    ) -> MonarchCommentsPanelThread {
        MonarchCommentsPanelThread(
            id: thread.id,
            comments: comments ?? thread.comments,
            quote: thread.quote,
            isResolved: isResolved ?? thread.isResolved
        )
    }

    private func check() async throws {
        try await Task.sleep(for: .milliseconds(600))
        if failsNext {
            failsNext = false
            throw CancellationError()
        }
    }
}
