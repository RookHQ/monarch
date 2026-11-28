import MonarchUI
import SwiftUI

/// Recipe: a centred, scrollable sign-in layout.
struct AuthPageSection: CataloguePage {
    static let title = "Auth page (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var showsDemo = false
    @State private var email = ""
    @State private var password = ""
    @State private var isSigningIn = false

    var body: some View {
        Text("Opens a full-screen sign-in layout. Focus a field to see it stay above the keyboard.")
            .monarchTextStyle(.body)
        MonarchButton("Show the sign-in screen", variant: .primary) { showsDemo = true }
            .fullScreenCover(isPresented: $showsDemo) { screen }
    }

    private var screen: some View {
        ScrollView {
            VStack(spacing: theme.spacing.space6) {
                MonarchIcon(systemName: "building.2", size: .lg)
                    .foregroundStyle(theme.colors.accentStrong)
                MonarchCard {
                    Text("Sign in").monarchTextStyle(.title).accessibilityAddTraits(.isHeader)
                    Text("Use the email address of your work account.")
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                    MonarchTextField("Email", text: $email, prompt: "name@example.com")
                        .textContentType(.emailAddress)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                    MonarchPasswordInput("Password", text: $password)
                    MonarchButton("Sign in", variant: .primary, isLoading: isSigningIn, fullWidth: true) {
                        isSigningIn = true
                        Task {
                            try? await Task.sleep(for: .seconds(1))
                            isSigningIn = false
                            showsDemo = false
                        }
                    }
                }
                Button("Close") { showsDemo = false }
                    .buttonStyle(.monarchLink(.standalone))
                    .monarchTextStyle(.body)
                HStack(spacing: theme.spacing.space4) {
                    if let terms = URL(string: "https://example.com/terms") {
                        Link("Terms", destination: terms).buttonStyle(.monarchLink(.subtle))
                    }
                    if let privacy = URL(string: "https://example.com/privacy") {
                        Link("Privacy", destination: privacy).buttonStyle(.monarchLink(.subtle))
                    }
                }
                .monarchTextStyle(.caption)
            }
            .padding(theme.spacing.space4)
            .frame(maxWidth: theme.spacing.space24 * 5)
            .frame(maxWidth: .infinity)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(theme.colors.bgCanvas)
    }
}
