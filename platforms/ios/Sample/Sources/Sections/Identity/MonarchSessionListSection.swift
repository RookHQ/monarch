import Foundation
@_spi(Experimental) import MonarchUI
import SwiftUI

/// A stand-in for the app's own failure.
private struct SignOutError: Error {}

struct MonarchSessionListSection: CataloguePage {
    static let title = "Session list"

    @State private var sessions = [
        MonarchSession(
            id: "phone",
            label: "Safari on iPhone",
            kind: .mobile,
            detail: "Berlin, Germany",
            signedIn: .now.addingTimeInterval(-86_400 * 12),
            isCurrent: true
        ),
        MonarchSession(
            id: "windows",
            label: "Chrome on Windows",
            detail: "Lisbon, Portugal",
            signedIn: .now.addingTimeInterval(-86_400 * 40),
            lastActive: .now.addingTimeInterval(-3_600)
        ),
        MonarchSession(
            id: "ipad",
            label: "Rook for iPad",
            kind: .tablet,
            signedIn: .now.addingTimeInterval(-86_400 * 3),
            lastActive: .now.addingTimeInterval(-86_400)
        ),
    ]
    @State private var failsOnce = true
    @State private var state = MonarchSessionListState.ready

    var body: some View {
        CatalogueCaption("The first sign-out fails, the next one succeeds")
        MonarchSessionList(
            sessions,
            state: state,
            footnote: "Apps that are open stay signed in for up to an hour.",
            onRetry: { state = .ready },
            onSignOut: { id in
                try await Task.sleep(for: .milliseconds(800))
                if failsOnce {
                    failsOnce = false
                    throw SignOutError()
                }
                sessions.removeAll { $0.id == id }
            },
            onSignOutOthers: { ids in
                try await Task.sleep(for: .milliseconds(800))
                sessions.removeAll { ids.contains($0.id) }
            }
        )

        CatalogueCaption("State")
        Picker("State", selection: $state) {
            Text("Ready").tag(MonarchSessionListState.ready)
            Text("Loading").tag(MonarchSessionListState.loading)
            Text("Failed").tag(MonarchSessionListState.failed)
        }
        .pickerStyle(.segmented)

        CatalogueCaption("Empty")
        MonarchSessionList([])
    }
}
