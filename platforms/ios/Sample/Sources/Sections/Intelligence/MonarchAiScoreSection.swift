import MonarchUI
import SwiftUI

struct MonarchAiScoreSection: CataloguePage {
    static let title = "AI score"

    @State private var value = 72.0

    var body: some View {
        CatalogueCaption("Ring with rationale and factors")
        MonarchAiScore(
            "Lead score",
            value: value,
            rationale: "Replied twice this week and matches the target industry.",
            factors: [
                MonarchAiScoreFactor("Replied to the last two emails", impact: 18),
                MonarchAiScoreFactor("No budget confirmed", impact: -9, detail: "Asked on the last call"),
            ]
        )
        Slider(value: $value, in: 0...100, step: 1) { Text("Score") }

        CatalogueCaption("Bar")
        MonarchAiScore("Churn risk", value: value, variant: .bar)

        CatalogueCaption("No score yet, small")
        MonarchAiScore("Fit", value: nil, size: .sm, showsBands: false)
    }
}
