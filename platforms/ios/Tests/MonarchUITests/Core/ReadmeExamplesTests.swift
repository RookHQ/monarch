import MonarchTokens
import MonarchUI
import SwiftUI
import Testing

// The view examples in README.md and in Documentation.docc/GettingStarted.md,
// kept by hand. When an example in either file changes, change it here too, so
// the documentation never shows code that does not compile.

private struct ContentView: View {
    var body: some View { InvoiceRow() }
}

private struct ThemedRoot: View {
    var body: some View {
        ContentView()
            .monarchTheme(
                scheme: .dark,
                contrast: .more,
                accent: MonarchTokens.Accents.neutral
            )
    }
}

private struct CustomFontRoot: View {
    var body: some View {
        ContentView()
            .monarchTheme(typography: MonarchTypography(fontFamily: "Poppins"))
    }
}

private struct InvoiceRow: View {
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space2) {
            Text("Invoice 1042")
                .monarchTextStyle(.bodyStrong)
                .foregroundStyle(theme.colors.textDefault)
            Text("Due in 3 days")
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
        }
        .padding(theme.spacing.space4)
        .monarchElevation(.sm, in: RoundedRectangle(cornerRadius: theme.radius.lg))
    }
}

private struct CheckoutForm: View {
    @State private var email = ""
    @State private var wantsReceipt = true
    @State private var isSaving = false

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            MonarchBanner(
                "Payment failed",
                message: "Check the card details and try again.",
                tone: .danger
            )

            MonarchTextField(
                "Email",
                text: $email,
                prompt: "name@example.com",
                helper: "We send the receipt here."
            )

            Toggle("Email me a receipt", isOn: $wantsReceipt)
                .toggleStyle(.monarchCheckbox)

            MonarchButton("Pay", variant: .primary, isLoading: isSaving, fullWidth: true) {
                isSaving = true
            }

            HStack {
                MonarchBadge("Paid", tone: .success)
                MonarchBadge("Draft", variant: .outline)
            }

            ProgressView("Uploading", value: 0.4)
                .progressViewStyle(.monarchLinear)
        }
        .padding()
    }
}

@Suite("README examples")
@MainActor
struct ReadmeExamplesTests {
    @Test("The README examples compile and lay out")
    func examplesLayOut() {
        let views: [AnyView] = [
            AnyView(ThemedRoot()), AnyView(CustomFontRoot()), AnyView(CheckoutForm().monarchTheme()),
        ]
        for view in views {
            let size = Host.layout(view)
            #expect(size.width > 0 && size.height > 0)
        }
    }
}
