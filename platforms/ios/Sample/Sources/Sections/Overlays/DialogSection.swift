import MonarchUI
import SwiftUI

/// Native adoption: `alert` for a message, `sheet` for content.
struct DialogSection: CataloguePage {
    static let title = "Dialog (alert and sheet)"

    @Environment(\.monarchTheme) private var theme
    @State private var showsAlert = false
    @State private var showsSheet = false
    @State private var name = "Ada Lovelace"

    var body: some View {
        CatalogueCaption("A message: alert")
        MonarchButton("Show a message") { showsAlert = true }
            .alert(
                "The export is ready",
                isPresented: $showsAlert,
                actions: {
                    Button("OK") {}
                },
                message: {
                    Text("It was sent to your email address.")
                }
            )

        CatalogueCaption("Content: sheet, with the theme's colours and Monarch components")
        MonarchButton("Edit the profile", variant: .primary) { showsSheet = true }
            .sheet(isPresented: $showsSheet) {
                NavigationStack {
                    VStack(alignment: .leading, spacing: theme.spacing.space4) {
                        MonarchTextField("Name", text: $name)
                        Spacer()
                    }
                    .padding(theme.spacing.space4)
                    .background(theme.colors.bgCanvas)
                    .navigationTitle("Edit profile")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { showsSheet = false }
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Save") { showsSheet = false }
                        }
                    }
                }
            }
    }
}
