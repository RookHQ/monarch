import MonarchUI
import SwiftUI

/// Native adoption: `help(_:)`, which is the accessibility hint on iOS. Touch shows no tooltip.
struct TooltipSection: CataloguePage {
    static let title = "Tooltip (help text)"

    @Environment(\.monarchTheme) private var theme
    @State private var isFiltered = false

    var body: some View {
        CatalogueCaption("Turn on VoiceOver: the name is read, then the help text as the hint")
        HStack(spacing: theme.spacing.space2) {
            Button {
                isFiltered.toggle()
            } label: {
                MonarchIcon(systemName: "line.3.horizontal.decrease")
            }
            .buttonStyle(.monarch(isFiltered ? .primary : .ghost))
            .accessibilityLabel("Filter")
            .accessibilityAddTraits(isFiltered ? .isSelected : [])
            .help("Shows only unpaid invoices")

            Text(isFiltered ? "Unpaid invoices" : "All invoices")
                .monarchTextStyle(.body)
        }

        CatalogueCaption("Text people need is on screen, not in help text")
        MonarchButton("Export") {}
            .disabled(true)
        Text("Exporting needs the Admin role.")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }
}
