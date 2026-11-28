@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchCommentThreadSection: CataloguePage {
    static let title = "CommentThread"

    @State private var comments = [
        MonarchCommentThreadComment(
            id: "c1",
            author: "Priya Raman",
            body: "Should this say \"billed every quarter\" instead?",
            createdAt: Date.now.addingTimeInterval(-9_000)
        ),
        MonarchCommentThreadComment(
            id: "c2",
            author: "Tom Becker",
            body: "Yes. Monthly billing was retired last year.",
            createdAt: Date.now.addingTimeInterval(-7_200),
            canDelete: true
        ),
    ]
    @State private var isResolved = false
    @State private var failsNext = false

    private let long = [
        "Agreed.", "Updated the copy.", "Checked the pricing table too.", "Legal is fine with it.",
        "Shipping tomorrow.",
    ]
    .enumerated()
    .map { index, body in
        MonarchCommentThreadComment(
            id: "r\(index)",
            author: index.isMultiple(of: 2) ? "Tom Becker" : "Lee Park",
            body: body,
            createdAt: Date.now.addingTimeInterval(TimeInterval(index * 300 - 6_000))
        )
    }

    var body: some View {
        CatalogueCaption("Open, with a quote")
        Toggle("Fail the next action", isOn: $failsNext)
        MonarchCommentThread(
            comments,
            quote: "Billed monthly, cancel any time.",
            isResolved: isResolved,
            onResolvedChange: { value in
                try await check()
                isResolved = value
            },
            onDelete: { id in
                try await check()
                comments.removeAll { $0.id == id }
            },
            onReply: { body in
                try await check()
                comments.append(
                    MonarchCommentThreadComment(
                        id: UUID().uuidString,
                        author: "Lee Park",
                        body: body,
                        createdAt: .now,
                        canDelete: true
                    )
                )
            }
        )

        CatalogueCaption("Older replies folded")
        MonarchCommentThread(
            [
                MonarchCommentThreadComment(
                    id: "root",
                    author: "Lee Park",
                    body: "Can we drop the footnote under the pricing table?",
                    createdAt: Date.now.addingTimeInterval(-7_000)
                )
            ] + long
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
