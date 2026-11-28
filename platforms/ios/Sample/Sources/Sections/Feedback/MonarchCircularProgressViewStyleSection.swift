import MonarchUI
import SwiftUI

struct MonarchCircularProgressViewStyleSection: CataloguePage {
    static let title = "ProgressCircle"

    @Environment(\.monarchTheme) private var theme
    @State private var progress = 0.42

    var body: some View {
        CatalogueCaption("Determinate")
        ProgressView("Uploading report.pdf", value: progress)
            .progressViewStyle(.monarchCircular(size: .lg, showsValue: true))
        ProgressView("Medium", value: progress)
            .progressViewStyle(.monarchCircular)
        ProgressView("Small", value: progress)
            .progressViewStyle(.monarchCircular(size: .sm))
        HStack(spacing: theme.spacing.space2) {
            MonarchButton("Add 10%", size: .sm) { progress = min(progress + 0.1, 1) }
            MonarchButton("Reset", variant: .ghost, size: .sm) { progress = 0 }
        }
        CatalogueCaption("Tones")
        HStack(spacing: theme.spacing.space3) {
            ForEach(MonarchTone.allCases, id: \.self) { tone in
                ProgressView(value: 0.7)
                    .progressViewStyle(.monarchCircular(tone: tone))
                    .accessibilityLabel(String(describing: tone))
            }
        }
        CatalogueCaption("Indeterminate")
        ProgressView("Working")
            .progressViewStyle(.monarchCircular)
    }
}
