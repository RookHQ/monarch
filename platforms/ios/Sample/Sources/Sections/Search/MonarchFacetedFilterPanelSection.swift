import MonarchUI
import SwiftUI

struct MonarchFacetedFilterPanelSection: CataloguePage {
    static let title = "Faceted filter panel"

    @Environment(\.monarchTheme) private var theme
    @State private var filters: [String: MonarchFacetSelection] = ["status": .options(["open"])]
    @State private var isPresented = false

    private let facets: [MonarchFacet] = [
        .options(
            id: "status",
            label: "Status",
            options: [
                MonarchFacetOption(id: "open", label: "Open", count: 128),
                MonarchFacetOption(id: "review", label: "In review", count: 42),
                MonarchFacetOption(id: "done", label: "Done", count: 1_204),
                MonarchFacetOption(id: "archived", label: "Archived", count: 0, isDisabled: true),
            ]
        ),
        .options(
            id: "owner",
            label: "Owner",
            options: [
                MonarchFacetOption(id: "alex", label: "Alex Kim", count: 64),
                MonarchFacetOption(id: "sam", label: "Sam Rivera", count: 51),
                MonarchFacetOption(id: "priya", label: "Priya Shah", count: 38),
                MonarchFacetOption(id: "jordan", label: "Jordan Lee", count: 22),
                MonarchFacetOption(id: "morgan", label: "Morgan Diaz", count: 9),
            ],
            maxVisible: 3
        ),
        .range(id: "size", label: "File size", bounds: 0...100, unit: "MB"),
        .options(
            id: "type",
            label: "Type",
            options: [
                MonarchFacetOption(id: "doc", label: "Document", count: 310),
                MonarchFacetOption(id: "sheet", label: "Spreadsheet", count: 88),
            ],
            isInitiallyExpanded: false
        ),
    ]

    var body: some View {
        CatalogueCaption("In a sheet, as on iPhone")
        MonarchButton("Filters", systemImage: "line.3.horizontal.decrease.circle") { isPresented = true }
            .sheet(isPresented: $isPresented) {
                NavigationStack {
                    ScrollView {
                        MonarchFacetedFilterPanel(facets: facets, selection: $filters, showsTitle: false)
                            .padding(theme.spacing.space4)
                    }
                    .background(theme.colors.bgCanvas)
                    .navigationTitle("Filters")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Done") { isPresented = false }
                        }
                    }
                }
                .presentationDetents([.medium, .large])
            }

        CatalogueCaption("Inline, as beside results on iPad")
        MonarchFacetedFilterPanel(facets: facets, selection: $filters)
        Text("Active: \(filters.keys.sorted().joined(separator: ", "))")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }
}
