import MonarchUI
import SwiftUI

struct MonarchAiTypingDotsSection: CataloguePage {
    static let title = "AI typing dots"

    @Environment(\.monarchTheme) private var theme
    @State private var isTyping = true

    var body: some View {
        CatalogueCaption("Sizes")
        HStack(spacing: theme.spacing.space6) {
            MonarchAiTypingDots(size: .sm, isAnnounced: false)
            MonarchAiTypingDots(size: .md, isAnnounced: false)
        }

        CatalogueCaption("In a conversation row. The label is announced when the dots appear.")
        Toggle("Assistant is writing", isOn: $isTyping)
            .monarchTextStyle(.body)
        HStack(spacing: theme.spacing.space2) {
            Text("Assistant").monarchTextStyle(.label)
            if isTyping {
                MonarchAiTypingDots(size: .sm)
            } else {
                Text("Replied just now")
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }

        CatalogueCaption("Turn on Reduce Motion in Settings to see the still form.")
    }
}
