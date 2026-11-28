import MonarchUI
import SwiftUI

struct MonarchProgressViewStyleSection: CataloguePage {
    static let title = "Progress"

    @Environment(\.monarchTheme) private var theme
    @State private var progress = 0.4

    var body: some View {
        ProgressView("Uploading", value: progress)
            .progressViewStyle(.monarchLinear)
        HStack(spacing: theme.spacing.space2) {
            MonarchButton("Add 10%", size: .sm) { progress = min(progress + 0.1, 1) }
            MonarchButton("Reset", variant: .ghost, size: .sm) { progress = 0 }
        }
        ProgressView("Indeterminate")
            .progressViewStyle(.monarchLinear)

        CatalogueCaption("Tones, for meters")
        ProgressView("Checklist done", value: 1)
            .progressViewStyle(.monarchLinear(tone: .success))
        ProgressView("Storage nearly full", value: 0.92)
            .progressViewStyle(.monarchLinear(tone: .warning))
        ProgressView("AI credits used", value: 0.3)
            .progressViewStyle(.monarchLinear(tone: .ai))
    }
}
