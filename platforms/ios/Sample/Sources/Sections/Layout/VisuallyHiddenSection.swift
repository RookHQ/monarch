import MonarchUI
import SwiftUI

/// Native adoption: accessibility modifiers on the element itself.
struct VisuallyHiddenSection: CataloguePage {
    static let title = "Visually hidden (accessibility modifiers)"

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Text("Turn VoiceOver on to hear the difference. Nothing here is hidden on screen.")
            .monarchTextStyle(.body)

        CatalogueCaption("An icon-only button named with accessibilityLabel")
        Button {
        } label: {
            MonarchIcon(systemName: "trash")
        }
        .buttonStyle(.monarch(.ghost))
        .accessibilityLabel("Delete invoice 1042")

        CatalogueCaption("Extra context for VoiceOver: read as \"Save changes to the ticket\"")
        MonarchButton("Save", variant: .primary) {}
            .accessibilityLabel("Save changes to the ticket")

        CatalogueCaption("A decorative image hidden with accessibilityHidden")
        HStack(spacing: theme.spacing.space2) {
            Image(systemName: "cpu").accessibilityHidden(true)
            Text("New in this release").monarchTextStyle(.body)
        }

        CatalogueCaption("Two texts read as one element: \"Balance, 240 dollars\"")
        VStack(alignment: .leading) {
            Text("Balance").monarchTextStyle(.caption)
            Text("$240").monarchTextStyle(.heading)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Balance")
        .accessibilityValue("240 dollars")
    }
}
