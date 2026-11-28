import MonarchUI
import SwiftUI

/// Recipe: email and password with passkey and single sign-on.
struct SignInFormSection: CataloguePage {
    static let title = "Sign-in form (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var email = ""
    @State private var password = ""
    @State private var remembers = false
    @State private var emailError: String?
    @State private var passwordError: String?
    @State private var formError: String?
    @State private var isSigningIn = false
    @State private var result = "Signed out"

    var body: some View {
        CatalogueCaption("The password “monarch” signs in")
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            Text("Sign in")
                .monarchTextStyle(.title)
                .accessibilityAddTraits(.isHeader)
            MonarchSignInWithRookButton(fullWidth: true) { result = "Single sign-on started" }
            MonarchButton(
                "Sign in with a passkey",
                systemImage: "person.badge.key",
                variant: .secondary,
                fullWidth: true
            ) {
                result = "The app would open the system passkey sheet"
            }
            HStack(spacing: theme.spacing.space3) {
                MonarchDivider()
                Text("or").monarchTextStyle(.caption)
                MonarchDivider()
            }
            .accessibilityHidden(true)
            if let formError {
                MonarchInlineNotice(formError, tone: .danger)
            }
            MonarchTextField("Email", text: $email, error: emailError)
                .textContentType(.username)
                .keyboardType(.emailAddress)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            MonarchPasswordInput("Password", text: $password, error: passwordError, purpose: .current)
                .onSubmit(signIn)
            Button("Forgot password?") { result = "Password reset opened" }
                .buttonStyle(.monarchLink(.subtle))
                .monarchTextStyle(.caption)
            Toggle("Keep me signed in", isOn: $remembers)
                .toggleStyle(.monarchCheckbox)
            MonarchButton("Sign in", variant: .primary, isLoading: isSigningIn, fullWidth: true, action: signIn)
            HStack(spacing: theme.spacing.space1) {
                Text("New here?").monarchTextStyle(.body)
                Button("Create an account") { result = "Sign-up opened" }
                    .buttonStyle(.monarchLink(.standalone))
                    .monarchTextStyle(.body)
            }
        }
        Text(result).monarchTextStyle(.caption)
    }

    private func signIn() {
        let trimmed = email.trimmingCharacters(in: .whitespaces)
        emailError =
            trimmed.isEmpty
            ? "Enter your email."
            : (trimmed.contains("@") && trimmed.contains(".") ? nil : "Enter an email address like name@example.com.")
        passwordError = password.isEmpty ? "Enter your password." : nil
        guard emailError == nil, passwordError == nil else { return }
        formError = nil
        isSigningIn = true
        Task {
            try? await Task.sleep(for: .milliseconds(800))
            isSigningIn = false
            if password == "monarch" {
                result = "Signed in as \(trimmed)"
            } else {
                formError = "Email or password is incorrect."
                AccessibilityNotification.Announcement("Email or password is incorrect.").post()
            }
        }
    }
}
