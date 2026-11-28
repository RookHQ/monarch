import MonarchUI
import SwiftUI

struct MonarchApiKeyRowSection: CataloguePage {
    static let title = "API key row"

    private struct DemoFailure: LocalizedError {
        var errorDescription: String? { "The server is offline. Try again in a minute." }
    }

    @Environment(\.monarchTheme) private var theme
    @State private var status = MonarchApiKeyStatus.active
    @State private var log = "Nothing yet"
    private let now = Date.now

    var body: some View {
        CatalogueCaption("Rotate succeeds, revoke asks for the name")
        MonarchApiKeyRow(
            "Reporting job",
            prefix: "sk_live_4f2a",
            createdAt: now.addingTimeInterval(-90 * 86_400),
            lastUsedAt: now.addingTimeInterval(-7_200),
            expiresAt: now.addingTimeInterval(30 * 86_400),
            scopes: ["items:read", "items:write", "reports:read", "users:read", "billing:read"],
            status: status,
            revokeConfirmPhrase: "Reporting job",
            onRotate: {
                try await Task.sleep(for: .seconds(1))
                log = "Rotated"
            },
            onRevoke: {
                try await Task.sleep(for: .seconds(1))
                status = .revoked
                log = "Revoked"
            },
            actions: {
                Button("Edit", systemImage: "pencil") { log = "Edit" }
            }
        )
        Text("Last action: \(log)")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
        MonarchButton("Reset", variant: .ghost, size: .sm) { status = .active }
        CatalogueCaption("Rotation fails")
        MonarchApiKeyRow(
            "CI deploys",
            prefix: "sk_test_91bc",
            createdAt: now.addingTimeInterval(-3 * 86_400),
            scopes: ["deploys:write"],
            status: .expiring,
            onRotate: {
                try await Task.sleep(for: .seconds(1))
                throw DemoFailure()
            }
        )
        CatalogueCaption("Expired, no actions")
        MonarchApiKeyRow("Old webhook signer", prefix: "whsec_77", createdAt: now, status: .expired)
    }
}
