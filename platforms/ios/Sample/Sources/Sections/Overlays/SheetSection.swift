import MonarchUI
import SwiftUI

/// Native adoption: the system sheet with detents.
struct SheetSection: CataloguePage {
    static let title = "Sheet (standard sheet)"

    @Environment(\.monarchTheme) private var theme
    @State private var showsDetents = false
    @State private var showsGuarded = false
    @State private var isSaving = false
    @State private var name = "Ada Lovelace"

    var body: some View {
        CatalogueCaption("Two heights and a grabber")
        MonarchButton("Edit details") { showsDetents = true }
            .sheet(isPresented: $showsDetents) {
                VStack(alignment: .leading, spacing: theme.spacing.space4) {
                    Text("Edit details")
                        .monarchTextStyle(.heading)
                        .accessibilityAddTraits(.isHeader)
                    MonarchTextField("Name", text: $name)
                    MonarchButton("Done", variant: .primary, fullWidth: true) { showsDetents = false }
                }
                .foregroundStyle(theme.colors.textDefault)
                .padding(theme.spacing.space4)
                .frame(maxHeight: .infinity, alignment: .top)
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
                .presentationBackground(theme.colors.bgSurface)
            }

        CatalogueCaption("Cannot be swiped away while saving")
        MonarchButton("Import contacts") {
            isSaving = false
            showsGuarded = true
        }
        .sheet(isPresented: $showsGuarded) {
            VStack(spacing: theme.spacing.space3) {
                Text("Import 240 contacts")
                    .monarchTextStyle(.heading)
                    .accessibilityAddTraits(.isHeader)
                MonarchButton("Import", variant: .primary, isLoading: isSaving, fullWidth: true) { isSaving = true }
                MonarchButton(isSaving ? "Stop and close" : "Cancel", variant: .ghost, fullWidth: true) {
                    showsGuarded = false
                }
            }
            .foregroundStyle(theme.colors.textDefault)
            .padding(theme.spacing.space4)
            .presentationDetents([.medium])
            .interactiveDismissDisabled(isSaving)
            .presentationBackground(theme.colors.bgSurface)
        }
    }
}
