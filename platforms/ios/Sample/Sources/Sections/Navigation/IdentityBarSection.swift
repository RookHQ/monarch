import MonarchUI
import SwiftUI

/// Recipe: the identity bar of a Rook app, placed in the navigation bar:
/// the product name, search, notifications and the account menu.
struct IdentityBarSection: CataloguePage {
    static let title = "Identity bar (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var query = ""
    @State private var unread = 3
    @State private var last = "Nothing chosen yet."

    var body: some View {
        NavigationStack {
            List {
                Text("Search: \(query.isEmpty ? "none" : query)")
                Text("Last action: \(last)")
            }
            .navigationTitle("Tickets")
            .searchable(text: $query, prompt: "Search Rook Tickets")
            .toolbar {
                ToolbarItem(placement: .navigation) {
                    Label("Rook Tickets", systemImage: "ticket")
                        .labelStyle(.titleAndIcon)
                        .monarchTextStyle(.label)
                        .foregroundStyle(theme.colors.textDefault)
                        .accessibilityAddTraits(.isHeader)
                }
                ToolbarItemGroup(placement: .primaryAction) {
                    MonarchNotificationsBell(unreadCount: unread) {
                        unread = 0
                        last = "Opened notifications"
                    }
                    Menu {
                        Section("Signed in as priya@example.com") {
                            Button("Profile and preferences", systemImage: "person.crop.circle") { last = "Profile" }
                            Button("Rook account", systemImage: "arrow.up.forward.square") { last = "Rook account" }
                        }
                        Button("Sign out", systemImage: "rectangle.portrait.and.arrow.right", role: .destructive) {
                            last = "Sign out"
                        }
                    } label: {
                        MonarchAvatar(image: nil, name: "Priya Raman", size: .sm, status: .online, isDecorative: true)
                            .monarchMinimumHitTarget()
                    }
                    .accessibilityLabel("Account: Priya Raman")
                }
            }
        }
        .frame(minHeight: theme.spacing.space24 * 5)
    }
}
