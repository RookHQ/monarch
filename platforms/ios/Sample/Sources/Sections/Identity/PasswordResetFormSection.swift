import MonarchUI
import SwiftUI

/// Recipe: request a reset link, then choose a new password when the link
/// opens the app.
struct PasswordResetFormSection: CataloguePage {
    static let title = "Password reset form (recipe)"

    private enum Step: String, CaseIterable {
        case request, sent, reset, done
    }

    @Environment(\.monarchTheme) private var theme
    @State private var step = Step.request
    @State private var email = ""
    @State private var emailError: String?
    @State private var password = ""
    @State private var confirmation = ""
    @State private var isWorking = false

    private var meetsRules: Bool { MonarchPasswordRule.defaults.allSatisfy { $0.test(password) } }
    private var mismatch: String? {
        confirmation.isEmpty || confirmation == password ? nil : "The passwords don’t match."
    }

    var body: some View {
        CatalogueCaption("Jump to a step, as the emailed link would")
        Picker("Step", selection: $step) {
            ForEach(Step.allCases, id: \.self) { Text($0.rawValue.capitalized).tag($0) }
        }
        .pickerStyle(.segmented)

        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            switch step {
            case .request:
                Text("Reset your password").monarchTextStyle(.title).accessibilityAddTraits(.isHeader)
                Text("Enter the email address of your account and we’ll send you a link to choose a new password.")
                    .monarchTextStyle(.body)
                MonarchTextField("Email", text: $email, prompt: "name@example.com", error: emailError)
                    .textContentType(.emailAddress)
                    .keyboardType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .submitLabel(.send)
                    .onSubmit(requestLink)
                MonarchButton(
                    "Send reset link",
                    variant: .primary,
                    isLoading: isWorking,
                    fullWidth: true,
                    action: requestLink
                )
            case .sent:
                Text("Check your email").monarchTextStyle(.title).accessibilityAddTraits(.isHeader)
                Text("If an account exists for \(email), you’ll get an email with a link to reset your password.")
                    .monarchTextStyle(.body)
                MonarchButton("Use a different email", variant: .ghost) { step = .request }
            case .reset:
                Text("Choose a new password").monarchTextStyle(.title).accessibilityAddTraits(.isHeader)
                Text("You’ll use it the next time you sign in.").monarchTextStyle(.body)
                MonarchPasswordInput("New password", text: $password, purpose: .new)
                MonarchPasswordStrengthMeter(password: password)
                MonarchPasswordInput("Confirm new password", text: $confirmation, error: mismatch, purpose: .new)
                MonarchButton(
                    "Change password",
                    variant: .primary,
                    isLoading: isWorking,
                    fullWidth: true,
                    action: changePassword
                )
                .disabled(!meetsRules || confirmation.isEmpty || mismatch != nil)
            case .done:
                Text("Password changed").monarchTextStyle(.title).accessibilityAddTraits(.isHeader)
                Text("You can now sign in with your new password.").monarchTextStyle(.body)
                MonarchButton("Back to sign in", variant: .primary, fullWidth: true) { step = .request }
            }
        }
        .onOpenURL { url in
            if url.path == "/reset-password" { step = .reset }
        }
    }

    private func requestLink() {
        guard email.contains("@"), email.contains(".") else {
            emailError = "Enter an email address like name@example.com."
            return
        }
        emailError = nil
        isWorking = true
        Task {
            try? await Task.sleep(for: .milliseconds(800))
            isWorking = false
            step = .sent
        }
    }

    private func changePassword() {
        isWorking = true
        Task {
            try? await Task.sleep(for: .milliseconds(800))
            isWorking = false
            step = .done
        }
    }
}
