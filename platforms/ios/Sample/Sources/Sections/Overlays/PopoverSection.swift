import MonarchUI
import SwiftUI

/// Native adoption: the system popover, kept as a popover on a phone.
struct PopoverSection: CataloguePage {
    static let title = "Popover (standard popover)"

    @Environment(\.monarchTheme) private var theme
    @State private var showsFilter = false
    @State private var showsAdapted = false
    @State private var onlyUnpaid = true

    var body: some View {
        CatalogueCaption("Stays a popover at every width")
        MonarchButton("Filter", systemImage: "line.3.horizontal.decrease") { showsFilter = true }
            .popover(isPresented: $showsFilter, arrowEdge: .top) {
                panel { showsFilter = false }
                    .presentationCompactAdaptation(.popover)
                    .presentationBackground(theme.colors.bgRaised)
            }

        CatalogueCaption("Without the adaptation it is a sheet on a phone")
        MonarchButton("Filter (system default)") { showsAdapted = true }
            .popover(isPresented: $showsAdapted) {
                panel { showsAdapted = false }
                    .presentationBackground(theme.colors.bgRaised)
            }
    }

    private func panel(dismiss: @escaping @MainActor () -> Void) -> some View {
        VStack(alignment: .leading, spacing: theme.spacing.space3) {
            Text("Filter invoices")
                .monarchTextStyle(.subheading)
                .accessibilityAddTraits(.isHeader)
            Toggle("Only unpaid", isOn: $onlyUnpaid)
                .monarchTextStyle(.body)
            MonarchButton("Done", variant: .primary, action: dismiss)
        }
        .foregroundStyle(theme.colors.textDefault)
        .padding(theme.spacing.space4)
    }
}
