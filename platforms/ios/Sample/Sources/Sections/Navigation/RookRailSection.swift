import MonarchUI
import SwiftUI

/// Recipe: Rook's primary navigation in a sidebar.
struct RookRailSection: CataloguePage {
    static let title = "Rook rail (recipe)"

    struct RailItem: Identifiable {
        let id: String
        let name: String
        let systemImage: String
        var unread = 0
    }

    private let items = [
        RailItem(id: "inbox", name: "Inbox", systemImage: "tray", unread: 3),
        RailItem(id: "tickets", name: "Tickets", systemImage: "list.bullet.rectangle"),
        RailItem(id: "customers", name: "Customers", systemImage: "person.2"),
    ]

    @Environment(\.monarchTheme) private var theme
    @State private var current: String? = "inbox"
    @State private var workspace: String? = "acme"
    @State private var space: String?
    @State private var showsSupport = false

    var body: some View {
        NavigationSplitView {
            List(selection: $current) {
                Section {
                    MonarchWorkspaceSwitcher(
                        selection: $workspace,
                        workspaces: [
                            MonarchWorkspaceOption(id: "acme", name: "Acme", handle: "acme"),
                            MonarchWorkspaceOption(id: "rook", name: "Rook Labs", handle: "rooklabs"),
                        ]
                    )
                    MonarchSpaceSwitcher(
                        selection: $space,
                        spaces: [
                            MonarchSpaceOption(id: "support", name: "Support"),
                            MonarchSpaceOption(id: "sales", name: "Sales"),
                        ]
                    )
                }
                Section {
                    ForEach(items) { item in
                        NavigationLink(value: item.id) {
                            Label(item.name, systemImage: item.systemImage)
                        }
                        .badge(item.unread)
                    }
                }
                Section("Workspace") {
                    if let members = URL(string: "https://id.example.com/members") {
                        Link(destination: members) {
                            Label("Members", systemImage: "person.3")
                        }
                        .accessibilityHint("Opens in Rook ID")
                    }
                }
                Section {
                    Button {
                        showsSupport = true
                    } label: {
                        Label("Help & support", systemImage: "questionmark.circle")
                    }
                }
            }
            .navigationTitle("Rook Tickets")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Menu {
                        Button("Profile", systemImage: "person.crop.circle") {}
                        Button("Sign out", systemImage: "rectangle.portrait.and.arrow.right", role: .destructive) {}
                    } label: {
                        MonarchAvatar(image: nil, name: "Priya Raman", size: .sm, isDecorative: true)
                            .monarchMinimumHitTarget()
                    }
                    .accessibilityLabel("Account menu for Priya Raman")
                }
            }
        } detail: {
            NavigationStack {
                Text(items.first { $0.id == current }?.name ?? "Choose a section")
                    .monarchTextStyle(.title)
                    .navigationTitle(items.first { $0.id == current }?.name ?? "")
            }
        }
        .sheet(isPresented: $showsSupport) {
            NavigationStack {
                List {
                    Section("Popular articles") {
                        Button("Reset your password") {}
                    }
                }
                .navigationTitle("Help & support")
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Done") { showsSupport = false }
                    }
                }
            }
            .presentationDetents([.medium, .large])
        }
        .frame(minHeight: theme.spacing.space24 * 6)
    }
}
