import Foundation
@_spi(Experimental) import MonarchUI
import SwiftUI

/// A stand-in for the app's own failure.
private struct ResendError: LocalizedError {
    var errorDescription: String? { "The mail server did not answer. Try again." }
}

struct MonarchPendingInvitesListSection: CataloguePage {
    static let title = "Pending invites list"

    @State private var invites = [
        MonarchPendingInvite(
            id: "ana",
            email: "ana@example.com",
            role: "Member",
            invitedBy: "Sam Rivera",
            sentAt: .now.addingTimeInterval(-86_400 * 2),
            expiresAt: .now.addingTimeInterval(86_400 * 5),
            link: "https://example.com/join/ana"
        ),
        MonarchPendingInvite(
            id: "bo",
            email: "bo@example.com",
            role: "Admin",
            invitedBy: "Sam Rivera",
            sentAt: .now.addingTimeInterval(-86_400 * 9),
            expiresAt: .now.addingTimeInterval(-86_400 * 2),
            link: "https://example.com/join/bo"
        ),
    ]
    @State private var resendFails = true
    @State private var state = MonarchSectionCardState.ready

    var body: some View {
        CatalogueCaption("The first resend fails, the next one succeeds")
        MonarchPendingInvitesList(
            invites,
            state: state,
            onRetry: { state = .ready },
            onResend: { _ in
                try await Task.sleep(for: .milliseconds(700))
                if resendFails {
                    resendFails = false
                    throw ResendError()
                }
            },
            onRevoke: { id in
                try await Task.sleep(for: .milliseconds(600))
                invites.removeAll { $0.id == id }
            }
        )

        CatalogueCaption("State")
        Picker("State", selection: $state) {
            Text("Ready").tag(MonarchSectionCardState.ready)
            Text("Loading").tag(MonarchSectionCardState.loading)
            Text("Error").tag(MonarchSectionCardState.error)
        }
        .pickerStyle(.segmented)

        CatalogueCaption("Empty")
        MonarchPendingInvitesList([])
    }
}
