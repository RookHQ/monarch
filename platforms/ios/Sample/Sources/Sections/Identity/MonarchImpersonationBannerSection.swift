import Foundation
import MonarchUI
import SwiftUI

/// A stand-in for the app's own failure.
private struct StopError: LocalizedError {
    var errorDescription: String? { "The session could not be closed. Try again." }
}

struct MonarchImpersonationBannerSection: CataloguePage {
    static let title = "Impersonation banner"

    @State private var isViewingAs = true
    @State private var stopFails = true
    private let start = Date().addingTimeInterval(-420)

    var body: some View {
        CatalogueCaption("The first stop fails, the next one ends the session")
        if isViewingAs {
            MonarchImpersonationBanner(
                name: "Sam Lee",
                detail: "sam@example.com",
                reason: "Ticket 4821",
                startedAt: start
            ) {
                try await Task.sleep(for: .milliseconds(800))
                if stopFails {
                    stopFails = false
                    throw StopError()
                }
                isViewingAs = false
            }
        } else {
            MonarchButton("View as Sam again") {
                stopFails = true
                isViewingAs = true
            }
        }

        CatalogueCaption("At the top of a screen")
        List {
            Text("Sam's inbox")
            Text("Sam's settings")
        }
        .frame(minHeight: 220)
        .safeAreaInset(edge: .top, spacing: 0) {
            MonarchImpersonationBanner(name: "Sam Lee") {}
        }
    }
}
