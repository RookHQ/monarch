import MonarchUI
import SwiftUI

/// Recipe: a page MonarchCalloutBanner in the top safe-area inset.
struct AnnouncementBarSection: CataloguePage {
    static let title = "Announcement bar (recipe)"

    @Environment(\.monarchTheme) private var theme
    @Environment(\.openURL) private var openURL
    @State private var isDismissed = false

    var body: some View {
        Text("The bar sits above the scrolling content. Close it, then bring it back.")
            .monarchTextStyle(.body)
        ScrollView {
            VStack(alignment: .leading, spacing: theme.spacing.space2) {
                ForEach(1..<8) { index in
                    Text("Row \(index)").monarchTextStyle(.body)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(theme.spacing.space4)
        }
        .safeAreaInset(edge: .top, spacing: 0) {
            if !isDismissed {
                MonarchCalloutBanner(
                    message: "Shared inboxes are now available on every plan.",
                    systemImage: "megaphone",
                    tone: .info,
                    placement: .page,
                    actionTitle: "Read the release notes",
                    action: {
                        if let url = URL(string: "https://example.com/releases") { openURL(url) }
                    },
                    onDismiss: { isDismissed = true }
                )
            }
        }
        .frame(height: theme.spacing.space24 * 3)
        .background(theme.colors.bgCanvas)
        if isDismissed {
            MonarchButton("Show the announcement again", size: .sm) { isDismissed = false }
        }
    }
}
