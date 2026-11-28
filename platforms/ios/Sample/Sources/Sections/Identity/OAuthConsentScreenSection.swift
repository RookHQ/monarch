import MonarchUI
import SwiftUI

/// Recipe: allow or cancel an app's access to the account.
struct OAuthConsentScreenSection: CataloguePage {
    static let title = "OAuth consent screen (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var isVerified = true
    @State private var isAllowing = false
    @State private var isDenying = false
    @State private var result = "Waiting for a decision"

    private let privacyURL = URL(string: "https://notes.example.com/privacy")
    private let termsURL = URL(string: "https://notes.example.com/terms")

    var body: some View {
        Toggle("The app is verified", isOn: $isVerified)
            .monarchTextStyle(.body)
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            HStack(spacing: theme.spacing.space3) {
                MonarchAvatar(
                    image: nil,
                    name: "Notes",
                    systemImage: "note.text",
                    shape: .square,
                    size: .lg,
                    isDecorative: true
                )
                VStack(alignment: .leading, spacing: theme.spacing.space0_5) {
                    Text("Notes").monarchTextStyle(.subheading)
                    Text("notes.example.com").monarchTextStyle(.caption)
                }
                .accessibilityElement(children: .combine)
                if isVerified {
                    MonarchBadge("Verified", tone: .success, systemImage: "checkmark.seal")
                }
            }
            Text("Notes wants to access your account")
                .monarchTextStyle(.title)
                .accessibilityAddTraits(.isHeader)
            if !isVerified {
                MonarchInlineNotice(
                    "Only continue if you know and trust Notes.",
                    title: "This app hasn’t been verified",
                    tone: .warning
                )
            }
            MonarchCard {
                VStack(alignment: .leading, spacing: theme.spacing.space2) {
                    HStack(spacing: theme.spacing.space3) {
                        MonarchAvatar(image: nil, name: "Ana Silva", isDecorative: true)
                        VStack(alignment: .leading, spacing: theme.spacing.space0_5) {
                            Text("Signed in as").monarchTextStyle(.caption)
                            Text("ana@example.com").monarchTextStyle(.bodyStrong)
                        }
                        .accessibilityElement(children: .combine)
                    }
                    Button("Use another account") { result = "The account picker would open" }
                        .buttonStyle(.monarchLink(.standalone))
                }
            }
            MonarchConsequenceList(
                [
                    MonarchConsequence(.kept, "See your name and profile photo"),
                    MonarchConsequence(.kept, "See your email address"),
                    MonarchConsequence(
                        .warning,
                        "Read and change your notes",
                        detail: "Including notes shared with you"
                    ),
                ],
                heading: "Notes will be able to:",
                kindLabel: { $0 == .warning ? "Sensitive" : "Allowed" }
            )
            HStack(spacing: theme.spacing.space3) {
                MonarchButton("Cancel", variant: .secondary, isLoading: isDenying, fullWidth: true) {
                    decide(allow: false)
                }
                MonarchButton("Allow", variant: .primary, isLoading: isAllowing, fullWidth: true) {
                    decide(allow: true)
                }
            }
            .disabled(isAllowing || isDenying)
            MonarchFlowLayout(spacing: theme.spacing.space1) {
                Text("Before you continue, read Notes’s").monarchTextStyle(.caption)
                if let privacyURL {
                    Link("privacy policy", destination: privacyURL)
                        .buttonStyle(.monarchLink(.inline, isExternal: true))
                        .monarchTextStyle(.caption)
                }
                Text("and").monarchTextStyle(.caption)
                if let termsURL {
                    Link("terms", destination: termsURL)
                        .buttonStyle(.monarchLink(.inline, isExternal: true))
                        .monarchTextStyle(.caption)
                }
            }
        }
        Text(result).monarchTextStyle(.caption)
    }

    private func decide(allow: Bool) {
        if allow { isAllowing = true } else { isDenying = true }
        Task {
            try? await Task.sleep(for: .milliseconds(800))
            isAllowing = false
            isDenying = false
            result = allow ? "Access allowed; back to notes.example.com" : "Access refused"
        }
    }
}
