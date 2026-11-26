@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiInlineDiffSection: CataloguePage {
    static let title = "AI inline diff"

    private static let original =
        "Our quarterly review covers revenue, hiring and the roadmap. The meeting is on Monday at noon."
    private static let suggested =
        "Our quarterly planning review covers revenue, hiring plans and the product roadmap. The meeting is on Tuesday at noon."

    @State private var segments = MonarchAiDiffSegment.words(from: original, to: suggested)

    var body: some View {
        CatalogueCaption("Accept or reject each suggestion; tap a change to show it")
        MonarchAiInlineDiff(
            segments,
            onAccept: { id in segments = MonarchAiInlineDiff.resolve(segments, change: id, decision: .accept) },
            onReject: { id in segments = MonarchAiInlineDiff.resolve(segments, change: id, decision: .reject) }
        )
        MonarchButton("Start again", variant: .ghost, size: .sm) {
            segments = MonarchAiDiffSegment.words(from: Self.original, to: Self.suggested)
        }

        CatalogueCaption("Read only")
        MonarchAiInlineDiff(
            MonarchAiDiffSegment.words(from: "Send the report on Friday.", to: "Send the final report on Thursday.")
        )
    }
}
