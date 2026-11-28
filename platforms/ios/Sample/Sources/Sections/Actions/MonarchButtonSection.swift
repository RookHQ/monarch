import MonarchUI
import SwiftUI

struct MonarchButtonSection: CataloguePage {
    static let title = "Button"

    @Environment(\.monarchTheme) private var theme
    @State private var isLoading = false
    @State private var tapCount = 0

    var body: some View {
        ForEach(MonarchButtonVariant.allCases, id: \.self) { variant in
            CatalogueCaption(String(describing: variant))
            ViewThatFits(in: .horizontal) {
                HStack(spacing: theme.spacing.space2) { sizes(variant) }
                VStack(alignment: .leading, spacing: theme.spacing.space2) { sizes(variant) }
            }
        }

        CatalogueCaption("Loading, disabled, and the style on a standard Button")
        MonarchButton("Save", systemImage: "checkmark", variant: .primary, isLoading: isLoading, fullWidth: true) {
            isLoading = true
            Task {
                try? await Task.sleep(for: .seconds(2))
                isLoading = false
            }
        }
        MonarchButton("Disabled", variant: .primary) {}
            .disabled(true)
        Button {
            tapCount += 1
        } label: {
            Label("Standard Button with the style", systemImage: "hand.tap")
        }
        .buttonStyle(.monarch(.ghost))

        CatalogueCaption("Icon only: the title is the spoken name")
        HStack(spacing: theme.spacing.space2) {
            MonarchButton("Dismiss", systemImage: "xmark", variant: .ghost, isIconOnly: true) { tapCount += 1 }
            MonarchButton("Add item", systemImage: "plus", variant: .secondary, isIconOnly: true) { tapCount += 1 }
            MonarchButton("Approve", systemImage: "checkmark", variant: .success, isIconOnly: true) { tapCount += 1 }
        }

        CatalogueCaption("Taps: \(tapCount)")
    }

    @ViewBuilder
    private func sizes(_ variant: MonarchButtonVariant) -> some View {
        MonarchButton("Small", variant: variant, size: .sm) { tapCount += 1 }
        MonarchButton("Medium", variant: variant, size: .md) { tapCount += 1 }
        MonarchButton("Large", trailingSystemImage: "chevron.forward", variant: variant, size: .lg) {
            tapCount += 1
        }
    }
}
