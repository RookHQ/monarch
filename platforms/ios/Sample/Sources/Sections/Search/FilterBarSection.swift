import MonarchUI
import SwiftUI

/// Recipe: a row of filter chips above a list, quick filters as toggles,
/// the full filter set in a sheet, search in the navigation bar.
struct FilterBarSection: CataloguePage {
    static let title = "Filter bar (recipe)"

    /// A ticket in the list.
    struct Ticket: Identifiable {
        let id: Int
        let title: String
        let status: String
        let isMine: Bool
    }

    @Environment(\.monarchTheme) private var theme
    @State private var query = ""
    @State private var isMine = false
    @State private var filters: [String: MonarchFacetSelection] = ["status": .options(["open"])]
    @State private var showsFilters = false
    @State private var sort = "newest"

    private let tickets = [
        Ticket(id: 1, title: "Printer offline on floor 3", status: "open", isMine: true),
        Ticket(id: 2, title: "VPN drops every hour", status: "open", isMine: false),
        Ticket(id: 3, title: "New laptop for Ada", status: "pending", isMine: true),
        Ticket(id: 4, title: "Reset badge access", status: "closed", isMine: false),
    ]

    private let statusLabels = ["open": "Open", "pending": "Pending", "closed": "Closed"]

    private var statuses: Set<String> {
        if case .options(let ids) = filters["status"] { return ids }
        return []
    }

    private var shown: [Ticket] {
        tickets.filter { ticket in
            (statuses.isEmpty || statuses.contains(ticket.status))
                && (!isMine || ticket.isMine)
                && (query.isEmpty || ticket.title.localizedCaseInsensitiveContains(query))
        }
    }

    private var activeCount: Int { filters.count + (isMine ? 1 : 0) }

    var body: some View {
        CatalogueCaption("Chips above the list; Filters opens the sheet")
        VStack(alignment: .leading, spacing: theme.spacing.space2) {
            TextField("Search tickets", text: $query)
                .textFieldStyle(.roundedBorder)
                .monarchTextStyle(.body)
            chips
            Text("\(shown.count) results")
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
            ForEach(shown) { ticket in
                HStack {
                    Text(ticket.title).monarchTextStyle(.body)
                    Spacer()
                    MonarchBadge(statusLabels[ticket.status] ?? ticket.status)
                }
                .accessibilityElement(children: .combine)
            }
        }
        .sheet(isPresented: $showsFilters) {
            NavigationStack {
                ScrollView {
                    MonarchFacetedFilterPanel(
                        facets: [
                            .options(
                                id: "status",
                                label: "Status",
                                options: [
                                    MonarchFacetOption(id: "open", label: "Open", count: 2),
                                    MonarchFacetOption(id: "pending", label: "Pending", count: 1),
                                    MonarchFacetOption(id: "closed", label: "Closed", count: 1),
                                ]
                            )
                        ],
                        selection: $filters,
                        showsTitle: false
                    )
                    .padding(theme.spacing.space4)
                }
                .navigationTitle("Filters")
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Show \(shown.count) results") { showsFilters = false }
                    }
                }
            }
            .presentationDetents([.medium, .large])
        }
    }

    private var chips: some View {
        ScrollView(.horizontal) {
            HStack(spacing: theme.spacing.space2) {
                Button(activeCount > 0 ? "Filters (\(activeCount))" : "Filters") { showsFilters = true }
                    .buttonStyle(.monarch(.secondary, size: .sm))
                MonarchTag("Assigned to me", isSelected: $isMine)
                ForEach(statuses.sorted(), id: \.self) { id in
                    MonarchTag(
                        "Status: \(statusLabels[id] ?? id)",
                        removeLabel: "Remove Status \(statusLabels[id] ?? id) filter",
                        onRemove: { remove(id) }
                    )
                }
                Menu {
                    Picker("Sort by", selection: $sort) {
                        Text("Newest").tag("newest")
                        Text("Oldest").tag("oldest")
                    }
                } label: {
                    Label("Sort", systemImage: "arrow.up.arrow.down")
                }
                .buttonStyle(.monarch(.ghost, size: .sm))
                if activeCount > 0 {
                    Button("Clear all") {
                        filters = [:]
                        isMine = false
                    }
                    .buttonStyle(.monarch(.ghost, size: .sm))
                }
            }
        }
        .scrollIndicators(.hidden)
    }

    private func remove(_ status: String) {
        let rest = statuses.subtracting([status])
        filters["status"] = rest.isEmpty ? nil : .options(rest)
    }
}
