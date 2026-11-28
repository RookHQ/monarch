import MonarchUI
import SwiftUI

/// Recipe: MonarchEmptyState with a "Coming soon" badge and a list of what is planned.
struct ComingSoonStateSection: CataloguePage {
    static let title = "Coming soon state (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var isNotified = false

    var body: some View {
        VStack(spacing: theme.spacing.space3) {
            MonarchBadge("Coming soon", tone: .info, systemImage: "clock")
            MonarchEmptyState(
                "Status pages",
                message: "Publish the health of your services on a page your customers can follow.",
                systemImage: "clock",
                actionTitle: isNotified ? "We'll let you know" : "Notify me"
            ) {
                isNotified = true
            }
            VStack(alignment: .leading, spacing: theme.spacing.space2) {
                Label("A page for each product", systemImage: "checkmark")
                Label("Incident history", systemImage: "checkmark")
                Label("Email updates for subscribers", systemImage: "checkmark")
            }
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textMuted)
        }
        .frame(maxWidth: .infinity)
    }
}
