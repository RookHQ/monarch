import MonarchUI
import SwiftUI

/// Native adoption: the navigation bar is the page header.
struct PageHeaderSection: CataloguePage {
    static let title = "Page header (navigation bar)"

    @Environment(\.monarchTheme) private var theme
    @State private var showsDemo = false
    @State private var section = "overview"
    @State private var lastAction = "No action yet."

    var body: some View {
        Text("The title, back button and actions live in the navigation bar; status and tabs are the first content.")
            .monarchTextStyle(.body)
        Text(lastAction).monarchTextStyle(.caption)
        MonarchButton("Show a detail screen", variant: .primary) { showsDemo = true }
            .sheet(isPresented: $showsDemo) {
                NavigationStack {
                    List {
                        Section {
                            HStack(spacing: theme.spacing.space2) {
                                MonarchBadge("Active", tone: .success)
                                Text("Updated 5 min ago")
                                    .monarchTextStyle(.caption)
                                    .foregroundStyle(theme.colors.textMuted)
                            }
                            MonarchTabs(
                                "Account sections",
                                selection: $section,
                                items: [
                                    MonarchTabsItem("Overview", value: "overview"),
                                    MonarchTabsItem("Invoices", value: "invoices"),
                                ]
                            )
                        }
                        Section("Contacts") {
                            Text("Ana Flores").monarchTextStyle(.body)
                            Text("Ben Okafor").monarchTextStyle(.body)
                        }
                    }
                    .navigationTitle("Acme Ltd")
                    .navigationBarTitleDisplayMode(.large)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Close") { showsDemo = false }
                        }
                        ToolbarItem(placement: .primaryAction) {
                            Button("Edit") { lastAction = "Edit" }
                        }
                        ToolbarItem(placement: .secondaryAction) {
                            Button("Duplicate", systemImage: "plus.square.on.square") { lastAction = "Duplicate" }
                        }
                        ToolbarItem(placement: .secondaryAction) {
                            Button("Archive", systemImage: "archivebox") { lastAction = "Archive" }
                        }
                    }
                }
            }
    }
}
