@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiEditProposalSection: CataloguePage {
    static let title = "AI edit proposal"

    @State private var state: MonarchAiEditProposalState = .proposed
    @State private var log = "No decision yet"

    private let hunks = [
        MonarchAiEditHunk(id: "1", before: "The quick brown fox", after: "The slow brown fox", label: "Title"),
        MonarchAiEditHunk(id: "2", before: "Ships on Friday.", after: "Ships on Monday after the review."),
        MonarchAiEditHunk(id: "3", before: "", after: "A new closing paragraph."),
    ]

    var body: some View {
        CatalogueCaption("Review the changes")
        MonarchAiEditProposal(
            MonarchAiEditProposalPart(id: "proposal", target: "doc-1", diff: .null, state: state),
            hunks: hunks,
            onAcceptAll: {
                log = "Accepted all"
                state = .applied
            },
            onRejectAll: {
                log = "Rejected all"
                state = .rejected
            },
            onAcceptHunk: { id in log = "Accepted \(id)" },
            onRejectHunk: { id in log = "Rejected \(id)" },
            onRevert: {
                log = "Reverted"
                state = .reverted
            }
        )
        Text(log).monarchTextStyle(.caption)
        Button("Start again") { state = .proposed }
            .buttonStyle(.monarch(.ghost, size: .sm))
    }
}
