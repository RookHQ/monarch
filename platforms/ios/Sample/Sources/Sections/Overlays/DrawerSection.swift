import MonarchUI
import SwiftUI

/// Native adoption: a `sheet` with detents and a drag indicator.
struct DrawerSection: CataloguePage {
    static let title = "Drawer (sheet with detents)"

    @Environment(\.monarchTheme) private var theme
    @State private var showsDrawer = false
    @State private var showsLocked = false

    var body: some View {
        CatalogueCaption("Half height, pulls up to full height, drags down to close")
        MonarchButton("Show the drawer", variant: .primary) { showsDrawer = true }
            .sheet(isPresented: $showsDrawer) {
                content(close: { showsDrawer = false })
                    .presentationDetents([.medium, .large])
                    .presentationDragIndicator(.visible)
            }

        CatalogueCaption("As tall as its content, and not closed by a drag")
        MonarchButton("Show a drawer that must be answered") { showsLocked = true }
            .sheet(isPresented: $showsLocked) {
                content(close: { showsLocked = false })
                    .presentationDetents([.height(theme.spacing.space24 * 2)])
                    .interactiveDismissDisabled()
            }
    }

    private func content(close: @escaping @MainActor () -> Void) -> some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            Text("Filters").monarchTextStyle(.heading)
            Toggle("Only unread", isOn: .constant(true)).monarchTextStyle(.body)
            MonarchButton("Done", variant: .primary, fullWidth: true, action: close)
        }
        .foregroundStyle(theme.colors.textDefault)
        .padding(theme.spacing.space6)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(theme.colors.bgRaised)
    }
}
