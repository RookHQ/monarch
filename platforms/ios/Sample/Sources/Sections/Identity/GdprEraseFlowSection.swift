import Foundation
import MonarchUI
import SwiftUI

/// A stand-in for the server rejecting a password.
private struct WrongPassword: LocalizedError {
    var errorDescription: String? { "That password isn’t right." }
}

/// Recipe: erase one person's personal data: what is erased and what is
/// kept, acknowledge and type to confirm, confirm it is you, done with an
/// audit reference.
struct GdprEraseFlowSection: CataloguePage {
    static let title = "GDPR erase flow (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var isOpen = false
    @State private var isLoadingSummary = false
    @State private var understands = false
    @State private var typed = ""
    @State private var isMatched = false
    @State private var isVerifying = false
    @State private var isErasing = false
    @State private var auditID: String?
    @State private var failure: String?
    @State private var result = "Nothing erased"

    private let subject = "Dana Fox"
    private let phrase = "dana@example.com"

    var body: some View {
        CatalogueCaption("The password “monarch” is accepted")
        Toggle("Summary still loading", isOn: $isLoadingSummary)
        MonarchButton("Erase personal data", systemImage: "person.crop.circle.badge.xmark", variant: .danger) {
            understands = false
            typed = ""
            auditID = nil
            failure = nil
            isOpen = true
        }
        .sheet(isPresented: $isOpen) {
            NavigationStack {
                if let auditID {
                    done(auditID)
                } else {
                    review
                }
            }
            .interactiveDismissDisabled(isErasing)
        }
        Text(result).monarchTextStyle(.caption)
    }

    private var review: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: theme.spacing.space4) {
                Text("This erases the personal data held about \(subject).")
                    .monarchTextStyle(.body)
                MonarchConsequenceList(
                    [
                        MonarchConsequence(.removed, "Profile, email address and phone number"),
                        MonarchConsequence(.removed, "Messages and comments", count: 214),
                        MonarchConsequence(.removed, "Sign-in history and devices"),
                    ],
                    heading: "Will be erased",
                    isLoading: isLoadingSummary
                )
                MonarchConsequenceList(
                    [
                        MonarchConsequence(
                            .kept,
                            "Invoices",
                            detail: "Kept for the legally required retention period, without contact details"
                        ),
                        MonarchConsequence(.kept, "Audit log entries", detail: "Kept with the name replaced by an id"),
                    ],
                    heading: "Will be kept",
                    isLoading: isLoadingSummary
                )
                MonarchInlineNotice(
                    "Erased data cannot be recovered, by you or by support.",
                    title: "This cannot be undone",
                    tone: .warning
                )
                NavigationLink("Continue", value: "confirm")
                    .buttonStyle(.monarch(.primary, fullWidth: true))
                    .disabled(isLoadingSummary)
                if isLoadingSummary {
                    Text("The summary must load before you continue.")
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .padding(theme.spacing.space6)
        }
        .navigationTitle("Erase personal data")
        .navigationDestination(for: String.self) { _ in confirm }
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") { isOpen = false }
            }
        }
    }

    private var confirm: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: theme.spacing.space4) {
                Text("Erase the personal data of \(subject) now.")
                    .monarchTextStyle(.body)
                Toggle("I understand this cannot be undone", isOn: $understands)
                    .toggleStyle(.monarchCheckbox)
                MonarchTypeToConfirmInput(
                    phrase: phrase,
                    text: $typed,
                    label: "Type \(phrase) to confirm",
                    onMatchChange: { isMatched = $0 }
                )
                if let failure {
                    MonarchInlineNotice(failure, tone: .danger)
                }
                MonarchButton("Erase data", variant: .danger, isLoading: isErasing, fullWidth: true) {
                    isVerifying = true
                }
                .disabled(!(understands && isMatched))
                if !(understands && isMatched) {
                    Text("Tick the box and type the confirmation text to continue.")
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .padding(theme.spacing.space6)
        }
        .navigationTitle("Confirm the erasure")
        .sheet(isPresented: $isVerifying) {
            MonarchStepUpAuthDialog(
                description: "Erasing personal data needs you to confirm your identity.",
                onVerified: { erase() },
                onVerify: { credential in
                    try await Task.sleep(for: .milliseconds(600))
                    guard case .password("monarch") = credential else { throw WrongPassword() }
                }
            )
        }
    }

    private func done(_ auditID: String) -> some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            MonarchEmptyState(
                "Personal data erased",
                message: "The personal data of \(subject) has been erased.",
                systemImage: "checkmark.circle",
                kind: .success
            )
            LabeledContent("Reference", value: auditID)
                .labeledContentStyle(.monarch)
                .textSelection(.enabled)
            Text("Logged in the audit log")
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
            MonarchButton("Close", fullWidth: true) {
                isOpen = false
                result = "Erased, reference \(auditID)"
            }
        }
        .padding(theme.spacing.space6)
    }

    private func erase() {
        isErasing = true
        failure = nil
        Task {
            try? await Task.sleep(for: .milliseconds(800))
            isErasing = false
            auditID = "AUD-2026-11-0042"
        }
    }
}
