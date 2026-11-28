import MonarchUI
import SwiftUI

/// Recipe: the screen shown when the signed-in person has no access to an app.
struct NoAppAccessSection: CataloguePage {
    static let title = "No app access (recipe)"

    @Environment(\.monarchTheme) private var theme
    @Environment(\.openURL) private var openURL
    @State private var isRequested = false
    @State private var workspace: String? = "acme"

    var body: some View {
        VStack(spacing: theme.spacing.space4) {
            MonarchEmptyState(
                "You don’t have access to Tickets",
                message: isRequested
                    ? "Request sent. You can open Tickets once an admin approves it."
                    : "Ask an admin of Acme to give you access to Tickets.",
                systemImage: "lock",
                actionTitle: isRequested ? nil : "Request access",
                secondaryActionTitle: "Contact an admin",
                action: {
                    isRequested = true
                    AccessibilityNotification.Announcement("Request sent.").post()
                },
                secondaryAction: {
                    if let url = URL(string: "mailto:admins@example.com") { openURL(url) }
                }
            )
            MonarchWorkspaceSwitcher(
                selection: $workspace,
                workspaces: [
                    MonarchWorkspaceOption(id: "acme", name: "Acme", handle: "acme"),
                    MonarchWorkspaceOption(id: "rook", name: "Rook Labs", handle: "rooklabs"),
                ]
            )
        }
        .frame(maxWidth: .infinity)
    }
}
