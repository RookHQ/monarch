import MonarchUI
import SwiftUI

/// Recipe: confirm revoking a key, with type-to-confirm and re-authentication.
struct RevokeAccessFlowSection: CataloguePage {
    static let title = "Revoke access flow (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var isConfirming = false
    @State private var isSteppingUp = false
    @State private var phrase = ""
    @State private var isRevoking = false
    @State private var failure: String?
    @State private var isRevoked = false

    private let keyName = "CI deploy key"
    private let keyPrefix = "rk_live_4f…9a"
    private let lastUsed = Date.now.addingTimeInterval(-7_200)

    var body: some View {
        CatalogueCaption("Type the key name, then confirm with any password")
        HStack {
            VStack(alignment: .leading) {
                Text(keyName).monarchTextStyle(.bodyStrong)
                Text(isRevoked ? "Revoked" : keyPrefix).monarchTextStyle(.caption)
            }
            Spacer()
            if !isRevoked {
                MonarchButton("Revoke", variant: .danger, size: .sm) {
                    phrase = ""
                    failure = nil
                    isConfirming = true
                }
                .accessibilityLabel("Revoke \(keyName)")
            }
        }
        .sheet(isPresented: $isConfirming) {
            ScrollView {
                VStack(alignment: .leading, spacing: theme.spacing.space4) {
                    Text("Revoke API key?")
                        .monarchTextStyle(.heading)
                        .accessibilityAddTraits(.isHeader)
                    Text("Apps and scripts that use it stop working at once. This can’t be undone.")
                        .monarchTextStyle(.body)
                    VStack(alignment: .leading, spacing: theme.spacing.space1) {
                        Text(keyName).monarchTextStyle(.bodyStrong)
                        Text(keyPrefix).monarchTextStyle(.code)
                        Text("Last used \(lastUsed.formatted(.relative(presentation: .named)))")
                            .monarchTextStyle(.caption)
                    }
                    .accessibilityElement(children: .combine)
                    MonarchConsequenceList(
                        [
                            MonarchConsequence(.removed, "Requests signed with this key are refused"),
                            MonarchConsequence(
                                .warning,
                                "Integrations that use it stop working until they get a new key"
                            ),
                        ],
                        heading: "What happens"
                    )
                    MonarchTypeToConfirmInput(phrase: keyName, text: $phrase)
                    if let failure {
                        MonarchInlineNotice(failure, tone: .danger)
                    }
                    MonarchButton("Revoke key", variant: .danger, isLoading: isRevoking, fullWidth: true) {
                        isSteppingUp = true
                    }
                    .disabled(phrase != keyName)
                    MonarchButton("Cancel", variant: .ghost, fullWidth: true) { isConfirming = false }
                        .disabled(isRevoking)
                }
                .padding(theme.spacing.space6)
            }
            .interactiveDismissDisabled(isRevoking)
            .sheet(isPresented: $isSteppingUp) {
                MonarchStepUpAuthDialog(
                    description: "Revoking access needs you to confirm it’s you.",
                    onVerified: revoke,
                    onVerify: { _ in try await Task.sleep(for: .milliseconds(600)) }
                )
            }
        }
    }

    private func revoke() {
        isRevoking = true
        Task {
            try? await Task.sleep(for: .milliseconds(800))
            isRevoking = false
            isRevoked = true
            isConfirming = false
        }
    }
}
