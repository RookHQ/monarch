import MonarchUI
import SwiftUI

/// Recipe: name, email and a new password.
struct SignUpFormSection: CataloguePage {
    static let title = "Sign-up form (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var name = ""
    @State private var email = ""
    @State private var password = ""
    @State private var acceptsTerms = false
    @State private var nameError: String?
    @State private var emailError: String?
    @State private var passwordError: String?
    @State private var showsTermsError = false
    @State private var formError: String?
    @State private var isCreating = false
    @State private var result = "No account yet"

    private let rules = MonarchPasswordRule.defaults
    private let documents = [
        MonarchTermsDocument("Terms of Service", url: URL(filePath: "/terms"), version: "2026-11-01"),
        MonarchTermsDocument("Privacy Policy", url: URL(filePath: "/privacy"), version: "2026-11-01"),
    ]

    var body: some View {
        CatalogueCaption("The email “taken@example.com” is already used")
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            Text("Create your account")
                .monarchTextStyle(.title)
                .accessibilityAddTraits(.isHeader)
            MonarchSignInWithRookButton(fullWidth: true) { result = "Single sign-on started" }
            HStack(spacing: theme.spacing.space3) {
                MonarchDivider()
                Text("or").monarchTextStyle(.caption)
                MonarchDivider()
            }
            .accessibilityHidden(true)
            if let formError {
                MonarchInlineNotice(formError, tone: .danger)
            }
            MonarchTextField("Full name", text: $name, error: nameError)
                .textContentType(.name)
            MonarchTextField("Email", text: $email, error: emailError)
                .textContentType(.username)
                .keyboardType(.emailAddress)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            MonarchPasswordInput("Password", text: $password, error: passwordError, purpose: .new)
                .onSubmit(createAccount)
            MonarchPasswordStrengthMeter(password: password, rules: rules)
            MonarchTermsAcceptance(isAccepted: $acceptsTerms, documents: documents, showsRequiredError: showsTermsError)
            MonarchButton(
                "Create account",
                variant: .primary,
                isLoading: isCreating,
                fullWidth: true,
                action: createAccount
            )
            HStack(spacing: theme.spacing.space1) {
                Text("Already have an account?").monarchTextStyle(.body)
                Button("Sign in") { result = "Sign-in opened" }
                    .buttonStyle(.monarchLink(.standalone))
                    .monarchTextStyle(.body)
            }
        }
        Text(result).monarchTextStyle(.caption)
    }

    private func createAccount() {
        let trimmedName = name.trimmingCharacters(in: .whitespaces)
        let trimmedEmail = email.trimmingCharacters(in: .whitespaces)
        nameError = trimmedName.isEmpty ? "Enter your name." : nil
        emailError =
            trimmedEmail.isEmpty
            ? "Enter your email address."
            : (trimmedEmail.contains("@") && trimmedEmail.contains(".")
                ? nil : "Enter an email address like name@example.com.")
        passwordError =
            password.isEmpty
            ? "Choose a password."
            : (rules.allSatisfy { $0.test(password) } ? nil : "Choose a password that meets every requirement.")
        showsTermsError = !acceptsTerms
        guard nameError == nil, emailError == nil, passwordError == nil, acceptsTerms else { return }
        formError = nil
        isCreating = true
        Task {
            try? await Task.sleep(for: .milliseconds(800))
            isCreating = false
            if trimmedEmail == "taken@example.com" {
                emailError = "An account with this email already exists."
            } else {
                result = "Account created for \(trimmedEmail)"
            }
        }
    }
}
