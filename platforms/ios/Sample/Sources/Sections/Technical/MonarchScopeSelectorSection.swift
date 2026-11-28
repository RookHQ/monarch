import MonarchUI
import SwiftUI

struct MonarchScopeSelectorSection: CataloguePage {
    static let title = "Scope selector"

    @Environment(\.monarchTheme) private var theme
    @State private var scopes: Set<String> = ["projects:read"]
    @State private var autoScopes: Set<String> = []
    @State private var isLoaded = false

    private let groups = [
        MonarchScopeGroup(
            id: "projects",
            label: "Projects API",
            description: "Example Inc.",
            systemImage: "folder",
            scopes: [
                MonarchScopeOption("projects:read", description: "List and read projects."),
                MonarchScopeOption("projects:write", description: "Create, update and archive projects."),
            ]
        ),
        MonarchScopeGroup(
            id: "reports",
            label: "Reports API",
            description: "Example Inc.",
            systemImage: "chart.bar",
            scopes: [
                MonarchScopeOption("reports:read", description: "Read generated reports."),
                MonarchScopeOption("reports:export", description: "Download reports as files."),
                MonarchScopeOption("reports:admin", description: "Only owners can grant this.", isDisabled: true),
            ]
        ),
    ]

    var body: some View {
        MonarchScopeSelector("Scopes", selection: $scopes, groups: groups, initiallyExpanded: ["projects"])
        Text("Selected: \(scopes.sorted().joined(separator: ", "))")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)

        CatalogueCaption("Loading, then selected automatically")
        MonarchScopeSelector(
            "Scopes",
            selection: $autoScopes,
            groups: isLoaded
                ? groups
                : groups.map { MonarchScopeGroup(id: $0.id, label: $0.label, scopes: [], isLoading: true) },
            autoSelect: .all
        )
        MonarchButton("Finish loading") { isLoaded = true }
            .disabled(isLoaded)
    }
}
