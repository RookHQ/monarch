import MonarchUI
import SwiftUI

/// Native adoption: system modals confine VoiceOver and keyboard focus.
struct FocusTrapSection: CataloguePage {
    static let title = "Focus trap (system modals)"

    @Environment(\.monarchTheme) private var theme
    @State private var showsSheet = false
    @State private var showsCover = false
    @State private var showsOverlay = false

    var body: some View {
        CatalogueCaption("A sheet keeps focus inside until it is dismissed")
        MonarchButton("Show a sheet") { showsSheet = true }
            .sheet(isPresented: $showsSheet) {
                modalContent("Sheet") { showsSheet = false }
                    .presentationDetents([.medium])
            }

        CatalogueCaption("A full-screen cover does the same")
        MonarchButton("Show a full-screen cover") { showsCover = true }
            .fullScreenCover(isPresented: $showsCover) {
                modalContent("Full-screen cover") { showsCover = false }
            }

        CatalogueCaption("A custom overlay needs the modal trait")
        MonarchButton("Show a custom overlay") { showsOverlay = true }
        if showsOverlay {
            modalContent("Custom overlay") { showsOverlay = false }
                .background(theme.colors.bgRaised, in: RoundedRectangle(cornerRadius: theme.radius.lg))
                .accessibilityAddTraits(.isModal)
        }
    }

    private func modalContent(_ title: String, dismiss: @escaping @MainActor () -> Void) -> some View {
        VStack(spacing: theme.spacing.space4) {
            Text(title).monarchTextStyle(.heading)
            Text("VoiceOver and the Tab key stay inside this view.").monarchTextStyle(.body)
            MonarchButton("Done", variant: .primary, action: dismiss)
        }
        .foregroundStyle(theme.colors.textDefault)
        .padding(theme.spacing.space6)
    }
}
