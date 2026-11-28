import MonarchUI
import SwiftUI

struct MonarchPresenceDotSection: CataloguePage {
    static let title = "Presence dot"

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        ForEach(MonarchPresenceStatus.allCases, id: \.self) { status in
            HStack(spacing: theme.spacing.space2) {
                MonarchPresenceDot(status, isDecorative: true)
                Text(MonarchPresenceDot.label(for: status))
                    .monarchTextStyle(.body)
            }
        }

        CatalogueCaption("Sizes")
        HStack(spacing: theme.spacing.space3) {
            ForEach(MonarchPresenceDotSize.allCases, id: \.self) { size in
                MonarchPresenceDot(.online, size: size)
            }
        }

        CatalogueCaption("On an avatar, with a status message")
        Circle()
            .fill(theme.colors.bgEmphasis)
            .frame(width: theme.spacing.space12, height: theme.spacing.space12)
            .overlay(alignment: .bottomTrailing) {
                MonarchPresenceDot(.away, customStatus: "In a meeting", size: .lg, isOverlay: true)
            }
    }
}
