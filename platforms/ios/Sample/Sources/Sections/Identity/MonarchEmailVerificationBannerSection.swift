import Foundation
import MonarchUI
import SwiftUI

/// A stand-in for the app's own failure.
private struct RateLimitError: LocalizedError {
    var errorDescription: String? { "Too many requests. Wait a minute and try again." }
}

struct MonarchEmailVerificationBannerSection: CataloguePage {
    static let title = "Email verification banner"

    @State private var isShown = true
    @State private var sends = 0
    @State private var changes = 0

    var body: some View {
        CatalogueCaption("Every second send fails. Change email pressed \(changes) times.")
        if isShown {
            MonarchEmailVerificationBanner(
                email: "ana@example.com",
                cooldownSeconds: 10,
                onChangeEmail: { changes += 1 },
                onDismiss: { isShown = false },
                onResend: {
                    try await Task.sleep(for: .milliseconds(800))
                    sends += 1
                    if sends.isMultiple(of: 2) { throw RateLimitError() }
                }
            )
        } else {
            MonarchButton("Show the banner again") { isShown = true }
        }

        CatalogueCaption("Info tone, right after sign-up")
        MonarchEmailVerificationBanner(email: "ana@example.com", tone: .info, initialCooldown: 30) {
            try await Task.sleep(for: .milliseconds(800))
        }
    }
}
