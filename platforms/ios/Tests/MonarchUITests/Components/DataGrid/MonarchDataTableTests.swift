import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private struct DataTableTicket: Identifiable, Sendable {
    let id: Int
    let subject: String
    let status: String
    let age: Int
}

private let dataTableTickets = [
    DataTableTicket(id: 42, subject: "Printer offline on the third floor", status: "Open", age: 3),
    DataTableTicket(id: 43, subject: "Refund request", status: "", age: 12),
    DataTableTicket(id: 44, subject: "Item 9", status: "Pending", age: 1),
    DataTableTicket(id: 45, subject: "Item 10", status: "Open", age: 3),
]

private let dataTableColumns: [MonarchDataTableColumn<DataTableTicket>] = [
    MonarchDataTableColumn("Subject", isHideable: false, value: { $0.subject }),
    MonarchDataTableColumn("Status", value: { $0.status }),
    MonarchDataTableColumn("Age", isNumeric: true, value: { "\($0.age) d" }, sortBy: { $0.age < $1.age }),
    MonarchDataTableColumn("Ticket", isSortable: false, value: { "#\($0.id)" }),
]

private let dataTableHeight = MonarchTokens.Space.space24 * 4

private func ticketActions(_ ticket: DataTableTicket) -> [MonarchDataTableRowAction] {
    [
        MonarchDataTableRowAction("Assign", systemImage: "person.badge.plus") {},
        MonarchDataTableRowAction("Delete", systemImage: "trash", isDestructive: true, isDisabled: ticket.id == 43) {},
    ]
}

enum MonarchDataTableFixtures: ComponentFixtures {
    static let id = "data-table"
    static let fixtures = [
        Fixture("list: selection, sort menu, actions, open, empty cell") {
            MonarchDataTable(
                "Open tickets",
                rows: dataTableTickets,
                columns: dataTableColumns,
                sort: .constant(MonarchDataTableSort("Age", order: .reverse)),
                selection: .constant([42]),
                layout: .list,
                isRowSelectable: { $0.id != 44 },
                rowActions: ticketActions,
                onOpen: { _ in }
            )
            .frame(height: dataTableHeight)
        },
        Fixture("table: selection, actions, hidden and reordered columns") {
            MonarchDataTable(
                "Open tickets",
                rows: dataTableTickets,
                columns: dataTableColumns,
                selection: .constant([]),
                hiddenColumns: ["Status"],
                columnOrder: ["Ticket"],
                layout: .table,
                rowActions: ticketActions,
                onOpen: { _ in }
            )
            .frame(height: dataTableHeight)
        },
        Fixture("automatic layout with a load-more footer") {
            MonarchDataTable(
                "Open tickets",
                rows: dataTableTickets,
                columns: dataTableColumns,
                footer: MonarchLoadMore(hasMore: true, loadedCount: 4, totalCount: 30, onLoadMore: {})
            )
            .frame(height: dataTableHeight)
        },
        Fixture("loading") {
            MonarchDataTable("Open tickets", rows: [DataTableTicket](), columns: dataTableColumns, isLoading: true)
                .frame(height: dataTableHeight)
        },
        Fixture("failed, retrying") {
            MonarchDataTable(
                "Open tickets",
                rows: [DataTableTicket](),
                columns: dataTableColumns,
                isLoading: true,
                errorMessage: "The server did not answer.",
                onRetry: {}
            )
            .frame(height: dataTableHeight)
        },
        Fixture("filtered to nothing") {
            MonarchDataTable(
                "Open tickets",
                rows: [DataTableTicket](),
                columns: dataTableColumns,
                isFiltered: true,
                onClearFilters: {}
            )
            .frame(height: dataTableHeight)
        },
        Fixture("empty with custom labels") {
            MonarchDataTable(
                "Open tickets",
                rows: [DataTableTicket](),
                columns: dataTableColumns,
                labels: MonarchDataTableLabels(emptyTitle: "No tickets", emptyMessage: "New tickets appear here.")
            )
            .frame(height: dataTableHeight)
        },
    ]
}

@Suite("MonarchDataTable")
struct MonarchDataTableTests {
    private typealias Tokens = MonarchDataTableTokens

    @Test("Automatic is a table only at regular width, below accessibility sizes, with dynamic columns")
    func layout() {
        #expect(
            Tokens.resolvedLayout(.automatic, isRegularWidth: true, isAccessibilitySize: false, supportsTable: true)
                == .table
        )
        #expect(
            Tokens.resolvedLayout(.automatic, isRegularWidth: false, isAccessibilitySize: false, supportsTable: true)
                == .list
        )
        #expect(
            Tokens.resolvedLayout(.automatic, isRegularWidth: true, isAccessibilitySize: true, supportsTable: true)
                == .list
        )
        #expect(
            Tokens.resolvedLayout(.table, isRegularWidth: false, isAccessibilitySize: true, supportsTable: true)
                == .table
        )
        #expect(
            Tokens.resolvedLayout(.table, isRegularWidth: true, isAccessibilitySize: false, supportsTable: false)
                == .list
        )
        #expect(
            Tokens.resolvedLayout(.list, isRegularWidth: true, isAccessibilitySize: false, supportsTable: true) == .list
        )
    }

    @Test("With no rows a failure wins, then loading, then filtered, then empty")
    func phase() {
        #expect(Tokens.phase(rowCount: 0, isLoading: true, errorMessage: "x", isFiltered: true) == .failed)
        #expect(Tokens.phase(rowCount: 0, isLoading: true, errorMessage: nil, isFiltered: true) == .loading)
        #expect(Tokens.phase(rowCount: 0, isLoading: false, errorMessage: nil, isFiltered: true) == .filtered)
        #expect(Tokens.phase(rowCount: 0, isLoading: false, errorMessage: nil, isFiltered: false) == .empty)
        #expect(Tokens.phase(rowCount: 2, isLoading: true, errorMessage: "x", isFiltered: false) == .ready)
    }

    @Test("Columns follow the order and drop hidden ones, except those that cannot hide")
    func visibleColumns() {
        let ids = { (hidden: Set<String>, order: [String]) in
            Tokens.visibleColumns(dataTableColumns, hidden: hidden, order: order).map(\.id)
        }
        #expect(ids([], []) == ["Subject", "Status", "Age", "Ticket"])
        #expect(ids(["Status", "Subject"], ["Ticket", "Age"]) == ["Ticket", "Age", "Subject"])
        #expect(dataTableColumns[1].chooserColumn == MonarchColumnChooserColumn("Status"))
        #expect(!dataTableColumns[0].chooserColumn.isHideable)
    }

    @Test("Sorting uses the column's rule or Finder order, is stable, and skips unsortable columns")
    func sorting() {
        let subjects = { (sort: MonarchDataTableSort?) in
            Tokens.sorted(dataTableTickets, by: sort, columns: dataTableColumns).map(\.id)
        }
        #expect(subjects(nil) == [42, 43, 44, 45])
        #expect(subjects(MonarchDataTableSort("Age")) == [44, 42, 45, 43])
        #expect(subjects(MonarchDataTableSort("Age", order: .reverse)) == [43, 42, 45, 44])
        #expect(subjects(MonarchDataTableSort("Subject")) == [44, 45, 42, 43])
        #expect(subjects(MonarchDataTableSort("Ticket", order: .reverse)) == [42, 43, 44, 45])
        #expect(subjects(MonarchDataTableSort("Gone")) == [42, 43, 44, 45])
    }

    @Test("The comparator matches the rule in both orders and is equal by column and order")
    func comparator() {
        let age = dataTableColumns[2]
        let forward = MonarchDataTableComparator(age, order: .forward)
        #expect(forward.compare(dataTableTickets[2], dataTableTickets[0]) == .orderedAscending)
        #expect(forward.compare(dataTableTickets[0], dataTableTickets[3]) == .orderedSame)
        var reverse = forward
        reverse.order = .reverse
        #expect(reverse.compare(dataTableTickets[2], dataTableTickets[0]) == .orderedDescending)
        #expect(forward == MonarchDataTableComparator(age, order: .forward))
        #expect(forward != reverse)
        #expect(reverse.sort == MonarchDataTableSort("Age", order: .reverse))
    }

    @Test("Choosing a column flips its order, another column starts ascending")
    func toggledSort() {
        let age = MonarchDataTableSort("Age")
        #expect(Tokens.toggledSort(nil, columnID: "Age", columns: dataTableColumns) == age)
        #expect(Tokens.toggledSort(age, columnID: "Age", columns: dataTableColumns)?.order == .reverse)
        #expect(
            Tokens.toggledSort(age, columnID: "Status", columns: dataTableColumns) == MonarchDataTableSort("Status")
        )
        #expect(Tokens.toggledSort(age, columnID: "Ticket", columns: dataTableColumns) == age)
        #expect(Tokens.sort(fromHeader: MonarchDataTableSort("Ticket"), current: age, columns: dataTableColumns) == age)
        #expect(Tokens.sort(fromHeader: nil, current: age, columns: dataTableColumns) == nil)
        let status = MonarchDataTableSort("Status", order: .reverse)
        #expect(Tokens.sort(fromHeader: status, current: age, columns: dataTableColumns) == status)
    }

    @Test("Sort states are spoken for the sorted column only")
    func sortState() {
        let labels = MonarchDataTableLabels()
        #expect(
            Tokens.sortState(columnID: "Age", sort: MonarchDataTableSort("Age"), labels: labels) == "sorted ascending"
        )
        #expect(
            Tokens.sortState(columnID: "Age", sort: MonarchDataTableSort("Age", order: .reverse), labels: labels)
                == "sorted descending"
        )
        #expect(Tokens.sortState(columnID: "Status", sort: MonarchDataTableSort("Age"), labels: labels) == nil)
    }

    @Test("Select all covers the selectable rows and keeps other selections")
    func selection() {
        let ids = Tokens.selectableIDs(dataTableTickets, isRowSelectable: { $0.id != 44 })
        #expect(ids == [42, 43, 45])
        #expect(!Tokens.isAllSelected([42, 43], selectable: ids))
        #expect(Tokens.isAllSelected([42, 43, 45, 99], selectable: ids))
        #expect(!Tokens.isAllSelected([1], selectable: []))
        #expect(Tokens.selectingAll([99], selectable: ids, isOn: true) == [42, 43, 45, 99])
        #expect(Tokens.selectingAll([42, 99], selectable: ids, isOn: false) == [99])
        #expect(Tokens.selecting([42], id: 43, isOn: true) == [42, 43])
        #expect(Tokens.selecting([42, 43], id: 43, isOn: false) == [42])
    }

    @Test("A compact row reads its name and every other field, with No value for empty cells")
    func rowSummary() {
        let labels = MonarchDataTableLabels()
        let ticket = dataTableTickets[1]
        let name = Tokens.rowName(ticket, columns: dataTableColumns, rowLabel: nil)
        #expect(name == "Refund request")
        #expect(Tokens.rowName(ticket, columns: dataTableColumns, rowLabel: { "Ticket \($0.id)" }) == "Ticket 43")
        #expect(
            Tokens.rowSummary(ticket, name: name, columns: dataTableColumns, labels: labels)
                == "Refund request, Status: No value, Age: 12 d, Ticket: #43"
        )
        #expect(Tokens.cellLabel("", labels: labels) == "No value")
        #expect(Tokens.cellLabel("Open", labels: labels) == "Open")
        #expect(Tokens.cellText("") == "—")
        let custom = MonarchDataTableLabels(
            emptyCell: "n/a",
            ascendingSymbol: "arrow.up",
            descendingSymbol: "arrow.down"
        )
        #expect(Tokens.cellText("", labels: custom) == "n/a")
        #expect(Tokens.cellText("Paid", labels: custom) == "Paid")
        #expect(Tokens.sortMark(.forward, labels: custom) == "arrow.up")
        #expect(Tokens.sortMark(.reverse, labels: custom) == "arrow.down")
        #expect(Tokens.sortMark(.forward) == "chevron.up" && Tokens.sortMark(.reverse) == "chevron.down")
        #expect(Tokens.selectedCountText(3, labels: labels) == "3 selected")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchDataTableStrings.selectAll == "Select all rows")
        #expect(MonarchDataTableStrings.selectRow("Ticket 42") == "Select Ticket 42")
        #expect(MonarchDataTableStrings.rowActions("Ticket 42") == "Actions for Ticket 42")
        #expect(MonarchDataTableStrings.actionsHeader == "Actions")
        #expect(MonarchDataTableStrings.openHint == "Opens the details")
        #expect(MonarchDataTableStrings.sort == "Sort")
        #expect(MonarchDataTableStrings.loading == "Loading rows")
        #expect(MonarchDataTableStrings.emptyTitle == "Nothing here yet")
        #expect(MonarchDataTableStrings.emptyMessage == "Rows appear here once they are added.")
        #expect(MonarchDataTableStrings.filteredTitle == "No rows match these filters")
        #expect(MonarchDataTableStrings.filteredMessage == "Try removing a filter or searching for something else.")
        #expect(MonarchDataTableStrings.errorTitle == "These rows could not be loaded")
    }
}
