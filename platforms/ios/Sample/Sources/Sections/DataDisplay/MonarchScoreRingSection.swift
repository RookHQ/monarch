import MonarchUI
import SwiftUI

struct MonarchScoreRingSection: CataloguePage {
    static let title = "Score ring"

    @Environment(\.monarchTheme) private var theme
    @State private var score = 72.0

    var body: some View {
        CatalogueCaption("Bands follow the score")
        MonarchScoreRing("Profile strength", value: score, description: "Updated today", size: .lg)
        Slider(value: $score, in: 0...100, step: 1) {
            Text("Score")
        }
        CatalogueCaption("Sizes")
        HStack(alignment: .top, spacing: theme.spacing.space4) {
            ForEach(MonarchScoreRingSize.allCases, id: \.self) { size in
                MonarchScoreRing(String(describing: size), value: 85, size: size)
            }
        }
        CatalogueCaption("Accent, a five-point scale, and no score")
        HStack(alignment: .top, spacing: theme.spacing.space4) {
            MonarchScoreRing("Brand", value: 54, color: .accent)
            MonarchScoreRing(
                "Rating",
                value: 4.3,
                max: 5,
                format: { $0.formatted(.number.precision(.fractionLength(1))) }
            )
            MonarchScoreRing("New", value: nil)
        }
    }
}
