import MonarchUI
import SwiftUI

struct MonarchDataTableSection: CataloguePage {
    static let title = "Data table"

    private struct Ticket: Identifiable, Sendable {
        let id: Int
        let subject: String
        let status: String
        let owner: String
        let age: Int
    }

    private enum Shown: String, CaseIterable {
        case rows, loading, empty, filtered, failed
    }

    private static let columns: [MonarchDataTableColumn<Ticket>] = [
        MonarchDataTableColumn("Subject", isHideable: false, value: { $0.subject }),
        MonarchDataTableColumn("Status", value: { $0.status }),
        MonarchDataTableColumn("Owner", value: { $0.owner }),
        MonarchDataTableColumn(
            "Age",
            isNumeric: true,
            width: 80,
            value: { "\($0.age) d" },
            sortBy: { $0.age < $1.age }
        ),
    ]

    @State private var tickets = [
        Ticket(id: 42, subject: "Printer offline", status: "Open", owner: "Ana Lima", age: 3),
        Ticket(id: 43, subject: "Refund request", status: "Pending", owner: "", age: 12),
        Ticket(id: 44, subject: "Password reset", status: "Open", owner: "Kai Chen", age: 1),
        Ticket(id: 45, subject: "Invoice copy", status: "Solved", owner: "Sam Ortiz", age: 30),
    ]
    @State private var shown: Shown = .rows
    @State private var layout: MonarchDataTableLayout = .automatic
    @State private var sort: MonarchDataTableSort?
    @State private var selection: Set<Int> = []
    @State private var hidden: Set<String> = []
    @State private var order: [String] = []
    @State private var isChoosingColumns = false
    @State private var opened = "None"

    var body: some View {
        Picker("State", selection: $shown) {
            ForEach(Shown.allCases, id: \.self) { Text($0.rawValue).tag($0) }
        }
        .pickerStyle(.segmented)
        Picker("Layout", selection: $layout) {
            Text("Automatic").tag(MonarchDataTableLayout.automatic)
            Text("Table").tag(MonarchDataTableLayout.table)
            Text("List").tag(MonarchDataTableLayout.list)
        }
        .pickerStyle(.segmented)
        Button("Columns…") { isChoosingColumns = true }
        MonarchDataTable(
            "Open tickets",
            rows: shown == .rows ? tickets : [],
            columns: Self.columns,
            sort: $sort,
            selection: $selection,
            hiddenColumns: hidden,
            columnOrder: order,
            layout: layout,
            isLoading: shown == .loading,
            errorMessage: shown == .failed ? "The server did not answer." : nil,
            isFiltered: shown == .filtered,
            rowActions: { ticket in
                [
                    MonarchDataTableRowAction("Assign to me", systemImage: "person.badge.plus") {
                        opened = "Assigned \(ticket.id)"
                    },
                    MonarchDataTableRowAction("Delete", systemImage: "trash", isDestructive: true) {
                        tickets.removeAll { $0.id == ticket.id }
                    },
                ]
            },
            onRetry: { shown = .rows },
            onClearFilters: { shown = .rows },
            onOpen: { opened = $0.subject }
        )
        .frame(minHeight: 460)
        Text("Opened: \(opened). Selected: \(selection.count)").monarchTextStyle(.caption)
            .sheet(isPresented: $isChoosingColumns) {
                NavigationStack {
                    MonarchColumnChooser(
                        Self.columns.map(\.chooserColumn),
                        hiddenColumns: $hidden,
                        order: $order,
                        onReset: {
                            hidden = []
                            order = []
                        }
                    )
                    .toolbar {
                        Button("Done") { isChoosingColumns = false }
                    }
                }
            }
    }
}
