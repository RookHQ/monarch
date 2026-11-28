import MonarchUI
import SwiftUI

struct MonarchDataGridSection: CataloguePage {
    static let title = "Data grid"

    private struct Invoice: Identifiable, Sendable {
        let id: String
        var customer: String
        var status: String
        var amount: Double
    }

    private enum Shown: String, CaseIterable {
        case rows, loading, empty
    }

    @State private var invoices = [
        Invoice(id: "INV-1", customer: "Example Ltd", status: "sent", amount: 120),
        Invoice(id: "INV-2", customer: "Sample Co", status: "paid", amount: 80.5),
        Invoice(id: "INV-3", customer: "", status: "draft", amount: 1_250),
        Invoice(id: "INV-4", customer: "Northwind", status: "sent", amount: 42),
    ]
    @State private var shown: Shown = .rows
    @State private var layout: MonarchDataTableLayout = .automatic
    @State private var density: MonarchDataGridDensity = .comfortable
    @State private var sort: MonarchDataTableSort?
    @State private var selection: Set<String> = []
    @State private var hidden: Set<String> = []
    @State private var pinning: [String: MonarchDataGridPin]?
    @State private var widths: [String: CGFloat] = [:]
    @State private var opened = "None"

    private static let statuses = [
        MonarchEditableCellOption("Draft", value: "draft"),
        MonarchEditableCellOption("Sent", value: "sent"),
        MonarchEditableCellOption("Paid", value: "paid"),
    ]

    private var columns: [MonarchDataGridColumn<Invoice>] {
        let statuses = Self.statuses
        return [
            MonarchDataGridColumn(
                "Invoice",
                id: "number",
                isHideable: false,
                pinned: .start,
                width: 112,
                value: { $0.id }
            ),
            MonarchDataGridColumn(
                "Customer",
                id: "customer",
                edit: MonarchDataGridEdit(
                    placeholder: "Add a customer",
                    isRequired: true,
                    value: { .text($0.customer) },
                    onCommit: { invoice, value in
                        guard case .text(let text) = value else { return }
                        update(invoice.id) { $0.customer = text }
                    }
                ),
                value: { $0.customer }
            ),
            MonarchDataGridColumn(
                "Status",
                id: "status",
                edit: MonarchDataGridEdit(
                    kind: .select,
                    options: statuses,
                    isRequired: true,
                    isEditable: { $0.status != "paid" },
                    value: { .text($0.status) },
                    onCommit: { invoice, value in
                        guard case .text(let text) = value else { return }
                        update(invoice.id) { $0.status = text }
                    }
                ),
                value: { invoice in statuses.first { $0.value == invoice.status }?.label ?? invoice.status }
            ),
            MonarchDataGridColumn(
                "Amount",
                id: "amount",
                isNumeric: true,
                pinned: .end,
                width: 112,
                edit: MonarchDataGridEdit(
                    kind: .number,
                    minimum: 0,
                    value: { .number($0.amount) },
                    onCommit: { invoice, value in
                        guard case .number(let number) = value else { return }
                        update(invoice.id) { $0.amount = number }
                    }
                ),
                value: { $0.amount.formatted(.number.precision(.fractionLength(2))) },
                sortBy: { $0.amount < $1.amount }
            ),
        ]
    }

    var body: some View {
        Picker("State", selection: $shown) {
            ForEach(Shown.allCases, id: \.self) { Text($0.rawValue).tag($0) }
        }
        .pickerStyle(.segmented)
        Picker("Layout", selection: $layout) {
            Text("Automatic").tag(MonarchDataTableLayout.automatic)
            Text("Grid").tag(MonarchDataTableLayout.table)
            Text("List").tag(MonarchDataTableLayout.list)
        }
        .pickerStyle(.segmented)
        Picker("Density", selection: $density) {
            Text("Comfortable").tag(MonarchDataGridDensity.comfortable)
            Text("Compact").tag(MonarchDataGridDensity.compact)
        }
        .pickerStyle(.segmented)
        Button("Show every column") { hidden = [] }
        MonarchDataGrid(
            "Invoices",
            rows: shown == .rows ? invoices : [],
            columns: columns,
            sort: $sort,
            selection: $selection,
            hiddenColumns: $hidden,
            pinning: $pinning,
            columnWidths: $widths,
            layout: layout,
            density: density,
            isLoading: shown == .loading,
            rowActions: { invoice in
                [
                    MonarchDataTableRowAction("Duplicate", systemImage: "plus.square.on.square") {
                        invoices.append(
                            Invoice(
                                id: "\(invoice.id)-copy",
                                customer: invoice.customer,
                                status: "draft",
                                amount: invoice.amount
                            )
                        )
                    },
                    MonarchDataTableRowAction("Delete", systemImage: "trash", isDestructive: true) {
                        invoices.removeAll { $0.id == invoice.id }
                    },
                ]
            },
            onOpen: { opened = $0.id }
        )
        .frame(height: 360)
        CatalogueCaption("Opened: \(opened). Selected: \(selection.sorted().joined(separator: ", "))")
    }

    private func update(_ id: String, _ change: (inout Invoice) -> Void) {
        guard let index = invoices.firstIndex(where: { $0.id == id }) else { return }
        change(&invoices[index])
    }
}
