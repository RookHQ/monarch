import MonarchUI
import SwiftUI

struct MonarchAiConfidenceMeterSection: CataloguePage {
    static let title = "AI confidence meter"

    @State private var value = 0.72

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Slider(value: $value, in: 0...1) {
            Text("Confidence")
        }

        CatalogueCaption("Badge")
        HStack(spacing: theme.spacing.space2) {
            MonarchAiConfidenceMeter(value: value)
            MonarchAiConfidenceMeter(value: value, size: .sm, showsValue: true)
        }

        CatalogueCaption("Bar")
        MonarchAiConfidenceMeter(value: value, explanation: "Two of three sources agree.", variant: .bar)
        MonarchAiConfidenceMeter(value: value, variant: .bar, size: .sm)

        CatalogueCaption("Every band")
        HStack(spacing: theme.spacing.space2) {
            MonarchAiConfidenceMeter(value: 0.3)
            MonarchAiConfidenceMeter(value: 0.7)
            MonarchAiConfidenceMeter(value: 0.95)
        }
    }
}
