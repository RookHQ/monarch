import MonarchUI
import SwiftUI

/// Native adoption: `contextMenu(menuItems:preview:)` as the touch form of a hover card.
struct HoverCardSection: CataloguePage {
    static let title = "Hover card (contextMenu preview)"

    @Environment(\.monarchTheme) private var theme
    @State private var last = "Touch and hold the name"

    var body: some View {
        Text(last).monarchTextStyle(.bodyStrong)
        CatalogueCaption("Tap opens the profile; touch and hold shows the card and its actions")
        HStack(spacing: theme.spacing.space1) {
            Text("Assigned to").monarchTextStyle(.body)
            Button("Ada Lovelace") { last = "Opened Ada's profile" }
                .buttonStyle(.monarchLink(.standalone))
                .monarchTextStyle(.body)
                .contextMenu(
                    menuItems: {
                        Button("Send message", systemImage: "message") { last = "Message" }
                        Button("Copy email address", systemImage: "doc.on.doc") { last = "Copied" }
                    },
                    preview: { card }
                )
        }
    }

    private var card: some View {
        HStack(spacing: theme.spacing.space3) {
            MonarchAvatar(image: nil, name: "Ada Lovelace", size: .lg, status: .online)
            VStack(alignment: .leading, spacing: theme.spacing.space1) {
                Text("Ada Lovelace").monarchTextStyle(.bodyStrong)
                Text("Engineering lead, London")
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .foregroundStyle(theme.colors.textDefault)
        .padding(theme.spacing.space4)
        .background(theme.colors.bgRaised)
    }
}
