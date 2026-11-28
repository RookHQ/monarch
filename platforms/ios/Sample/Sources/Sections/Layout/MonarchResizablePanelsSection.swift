import MonarchUI
import SwiftUI

struct MonarchResizablePanelsSection: CataloguePage {
    static let title = "Resizable panels"

    @Environment(\.monarchTheme) private var theme
    @State private var horizontal = 0.4
    @State private var vertical = 0.5
    @State private var saved = 0.4

    var body: some View {
        CatalogueCaption(
            "Side by side, with a grip. Saved share: \(saved.formatted(.percent.precision(.fractionLength(0))))"
        )
        MonarchResizablePanels(
            fraction: $horizontal,
            range: 0.2...0.8,
            showsGrip: true,
            stacksWhenCompact: false,
            onResizeEnded: { saved = $0 },
            first: { pane("List") },
            second: { pane("Detail") }
        )
        .frame(height: theme.spacing.space24 * 2)

        CatalogueCaption("Stacked on top of each other")
        MonarchResizablePanels(
            fraction: $vertical,
            axis: .vertical,
            first: { pane("Editor") },
            second: { pane("Preview") }
        )
        .frame(height: theme.spacing.space24 * 3)

        CatalogueCaption("Disabled")
        MonarchResizablePanels(
            fraction: .constant(0.5),
            showsGrip: true,
            stacksWhenCompact: false,
            first: { pane("List") },
            second: { pane("Detail") }
        )
        .frame(height: theme.spacing.space24)
        .disabled(true)

        CatalogueCaption("Default: stacked at a compact width, side by side at a regular width")
        MonarchResizablePanels(
            fraction: $horizontal,
            first: { pane("List").frame(minHeight: theme.spacing.space16) },
            second: { pane("Detail").frame(minHeight: theme.spacing.space16) }
        )
        .frame(minHeight: theme.spacing.space24 * 2)
    }

    private func pane(_ title: String) -> some View {
        Text(title)
            .monarchTextStyle(.label)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(theme.colors.bgMuted)
    }
}
