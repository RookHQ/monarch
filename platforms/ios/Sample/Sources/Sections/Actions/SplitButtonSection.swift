import MonarchUI
import SwiftUI

/// Style coverage: the standard `Menu` with a primary action, and a joined
/// main button and arrow for when the alternatives must be visible.
struct SplitButtonSection: CataloguePage {
    static let title = "SplitButton (Menu with a primary action)"

    @Environment(\.monarchTheme) private var theme
    @State private var last = "Nothing done yet"

    var body: some View {
        Text(last)
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textMuted)

        CatalogueCaption("Tap to save; press and hold for the alternatives")
        Menu {
            Button("Save as draft") { last = "Saved as draft" }
            Button("Save and close") { last = "Saved and closed" }
        } label: {
            Text("Save")
        } primaryAction: {
            last = "Saved"
        }
        .buttonStyle(.monarch(.primary))

        CatalogueCaption("A main button joined to a visible arrow")
        HStack(spacing: theme.spacing.space0_5) {
            Button("Merge") { last = "Merged" }
                .buttonStyle(.monarch())
            Menu {
                Button("Squash and merge") { last = "Squashed and merged" }
                Button("Rebase and merge") { last = "Rebased and merged" }
            } label: {
                MonarchIcon(systemName: "chevron.down", size: .sm)
            }
            .buttonStyle(.monarch())
            .accessibilityLabel("More options for Merge")
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Merge")

        CatalogueCaption("Disabled")
        Menu {
            Button("Send later") {}
        } label: {
            Text("Send")
        } primaryAction: {
        }
        .buttonStyle(.monarch(.primary))
        .disabled(true)
    }
}
