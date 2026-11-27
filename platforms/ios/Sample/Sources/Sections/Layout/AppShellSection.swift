import MonarchUI
import SwiftUI

/// Native adoption: TabView, sidebar-adaptable from iOS 18, or NavigationSplitView.
struct AppShellSection: CataloguePage {
    static let title = "App shell (TabView)"

    @Environment(\.monarchTheme) private var theme
    @State private var showsTabs = false
    @State private var showsSplit = false
    @State private var tab = "inbox"
    @State private var area: String? = "Tickets"

    private let areas = ["Tickets", "Customers", "Reports", "Automations", "Settings"]

    var body: some View {
        Text("The system container is the shell. Tabs become a sidebar on iPadOS 18 and later.")
            .monarchTextStyle(.body)
        MonarchButton("Show the tab shell", variant: .primary) { showsTabs = true }
            .fullScreenCover(isPresented: $showsTabs) { tabShell }
        MonarchButton("Show the sidebar shell") { showsSplit = true }
            .fullScreenCover(isPresented: $showsSplit) {
                NavigationSplitView(
                    sidebar: {
                        List(areas, id: \.self, selection: $area) { name in
                            Text(name).monarchTextStyle(.body)
                        }
                        .navigationTitle("Acme")
                    },
                    detail: { ShellPage(title: area ?? "Choose an area") }
                )
            }
    }

    @ViewBuilder private var tabShell: some View {
        let tabs = TabView(selection: $tab) {
            ShellPage(title: "Inbox")
                .tabItem { Label("Inbox", systemImage: "tray") }
                .badge(3)
                .tag("inbox")
            ShellPage(title: "Customers")
                .tabItem { Label("Customers", systemImage: "person.2") }
                .tag("customers")
            ShellPage(title: "Settings")
                .tabItem { Label("Settings", systemImage: "gearshape") }
                .tag("settings")
        }
        if #available(iOS 18.0, *) {
            tabs.tabViewStyle(.sidebarAdaptable)
        } else {
            tabs
        }
    }
}

/// One area of the demo shell, with a button that closes the cover it is in.
private struct ShellPage: View {
    let title: String

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            VStack(spacing: theme.spacing.space4) {
                Text(title).monarchTextStyle(.title)
                MonarchButton("Close") { dismiss() }
            }
            .foregroundStyle(theme.colors.textDefault)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(theme.colors.bgCanvas)
            .navigationTitle(title)
        }
    }
}
