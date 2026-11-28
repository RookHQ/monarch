import MonarchUI
import SwiftUI

struct MonarchConnectedAppsListSection: CataloguePage {
    static let title = "Connected apps list"

    private struct RevokeFailure: LocalizedError {
        var errorDescription: String? { "Couldn’t remove access. Try again." }
    }

    private static let initialApps = [
        MonarchConnectedApp(
            id: "notes",
            name: "Notes",
            domain: "notes.example.com",
            isVerified: true,
            scopes: ["See your email address", "See your name and profile picture"],
            connectedAt: Date().addingTimeInterval(-3 * 86_400),
            lastUsedAt: Date().addingTimeInterval(-3_600)
        ),
        MonarchConnectedApp(
            id: "planner",
            name: "Planner",
            domain: "planner.example.com",
            workspace: "Example team",
            scopes: ["See your calendar"],
            connectedAt: Date().addingTimeInterval(-40 * 86_400)
        ),
        MonarchConnectedApp(
            id: "legacy",
            name: "Legacy reports",
            isSuspended: true,
            connectedAt: Date().addingTimeInterval(-400 * 86_400)
        ),
    ]

    @State private var apps = Self.initialApps
    @State private var state = MonarchConnectedAppsListState.ready
    @State private var failsNext = false

    var body: some View {
        CatalogueCaption("State")
        Picker("State", selection: $state) {
            Text("Ready").tag(MonarchConnectedAppsListState.ready)
            Text("Loading").tag(MonarchConnectedAppsListState.loading)
            Text("Error").tag(MonarchConnectedAppsListState.error)
        }
        .pickerStyle(.segmented)
        Toggle("Fail the next removal", isOn: $failsNext)
        MonarchButton("Restore apps", variant: .ghost, size: .sm) { apps = Self.initialApps }

        MonarchConnectedAppsList(
            apps,
            state: state,
            onRetry: { state = .ready },
            onRevoke: { app in
                try await Task.sleep(for: .seconds(1))
                if failsNext {
                    failsNext = false
                    throw RevokeFailure()
                }
                apps.removeAll { $0.id == app.id }
            }
        )
    }
}
