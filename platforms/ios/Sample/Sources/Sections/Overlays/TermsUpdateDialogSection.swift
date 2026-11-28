import MonarchUI
import SwiftUI

/// Recipe: a sheet with a summary, the effective date, links and accept or decline.
struct TermsUpdateDialogSection: CataloguePage {
    static let title = "Terms update dialog (sheet recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var isBlockingShown = false
    @State private var isInformationShown = false
    @State private var isAccepting = false
    @State private var failsOnce = true
    @State private var error: String?
    @State private var result = "Not answered yet"

    private let effective = Calendar.current.date(byAdding: .day, value: 14, to: Date()) ?? Date()

    var body: some View {
        Text(result).monarchTextStyle(.bodyStrong)

        CatalogueCaption("Blocking: cannot be swiped away. The first acceptance fails, to show the error.")
        MonarchButton("Show the blocking update", variant: .primary) {
            failsOnce = true
            error = nil
            isBlockingShown = true
        }
        .sheet(isPresented: $isBlockingShown) {
            content(isBlocking: true)
                .interactiveDismissDisabled()
        }

        CatalogueCaption("Informational: one button, and it can be swiped away")
        MonarchButton("Show the informational update") { isInformationShown = true }
            .sheet(isPresented: $isInformationShown) {
                content(isBlocking: false)
            }
    }

    private func content(isBlocking: Bool) -> some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            ScrollView {
                VStack(alignment: .leading, spacing: theme.spacing.space3) {
                    Text("We’ve updated our terms")
                        .monarchTextStyle(.heading)
                        .accessibilityAddTraits(.isHeader)
                    Text("We now explain how long we keep invoices and how to export your data.")
                        .monarchTextStyle(.body)
                    Text("Effective from \(effective.formatted(date: .long, time: .omitted))")
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                    CatalogueCaption("Read the full documents")
                    if let terms = URL(string: "https://example.com/terms"),
                        let privacy = URL(string: "https://example.com/privacy")
                    {
                        Link("Terms of Service (version 7)", destination: terms)
                            .buttonStyle(.monarchLink(.standalone, isExternal: true))
                        Link("Privacy Policy (version 4)", destination: privacy)
                            .buttonStyle(.monarchLink(.standalone, isExternal: true))
                    }
                }
                .monarchTextStyle(.body)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            if let error {
                MonarchInlineNotice(error, tone: .danger)
            }
            if isBlocking {
                MonarchButton("Accept and continue", variant: .primary, isLoading: isAccepting, fullWidth: true) {
                    accept()
                }
                MonarchButton("Sign out", variant: .ghost, fullWidth: true) {
                    result = "Declined and signed out"
                    isBlockingShown = false
                }
                .disabled(isAccepting)
            } else {
                MonarchButton("Got it", variant: .primary, fullWidth: true) {
                    result = "Read"
                    isInformationShown = false
                }
            }
        }
        .foregroundStyle(theme.colors.textDefault)
        .padding(theme.spacing.space4)
        .presentationBackground(theme.colors.bgSurface)
    }

    private func accept() {
        isAccepting = true
        error = nil
        Task {
            try? await Task.sleep(for: .seconds(1))
            isAccepting = false
            if failsOnce {
                failsOnce = false
                error = "Your answer could not be saved. Try again."
            } else {
                result = "Accepted"
                isBlockingShown = false
            }
        }
    }
}
