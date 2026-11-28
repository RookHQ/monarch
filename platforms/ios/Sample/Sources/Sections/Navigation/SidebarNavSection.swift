import MonarchUI
import SwiftUI

/// Native adoption: a sidebar `List` in `NavigationSplitView`.
struct SidebarNavSection: CataloguePage {
    static let title = "Sidebar navigation (List in NavigationSplitView)"

    @Environment(\.monarchTheme) private var theme
    @State private var showsDemo = false
    @State private var selection: String? = "Issues"
    @State private var isTeamOpen = true

    var body: some View {
        Text(
            "Sections, nested groups, counts and the current page. A sidebar on an iPad, a list that pushes on a phone."
        )
        .monarchTextStyle(.body)
        MonarchButton("Show the sidebar", variant: .primary) { showsDemo = true }
            .fullScreenCover(isPresented: $showsDemo) {
                NavigationSplitView(
                    sidebar: {
                        List(selection: $selection) {
                            Section("Workspace") {
                                Label("Inbox", systemImage: "tray").badge(4).tag("Inbox")
                                Label("My issues", systemImage: "person").tag("My issues")
                            }
                            Section("Teams") {
                                DisclosureGroup(isExpanded: $isTeamOpen) {
                                    Label("Issues", systemImage: "circle.dashed").tag("Issues")
                                    Label("Backlog", systemImage: "square.stack").tag("Backlog")
                                    Label("Sprints", systemImage: "flag").tag("Sprints")
                                } label: {
                                    Label("Design", systemImage: "paintbrush")
                                }
                            }
                        }
                        .listStyle(.sidebar)
                        .navigationTitle("Stride")
                        .toolbar {
                            ToolbarItem(placement: .cancellationAction) {
                                Button("Close") { showsDemo = false }
                            }
                        }
                    },
                    detail: {
                        Text(selection ?? "Nothing selected")
                            .monarchTextStyle(.title)
                            .foregroundStyle(theme.colors.textDefault)
                    }
                )
            }
    }
}
