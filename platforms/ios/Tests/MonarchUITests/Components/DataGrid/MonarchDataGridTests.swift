import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private struct GridInvoice: Identifiable, Sendable {
    let id: String
    let customer: String
    let status: String
    let amount: Double
}

private let gridInvoices = [
    GridInvoice(id: "INV-1", customer: "Example Ltd", status: "Sent", amount: 120),
    GridInvoice(id: "INV-2", customer: "", status: "Paid", amount: 80.5),
    GridInvoice(
        id: "INV-3",
        customer: "A customer with a very long name that wraps in its cell",
        status: "",
        amount: 9
    ),
]

private func gridColumns(editable: Bool = true) -> [MonarchDataGridColumn<GridInvoice>] {
    [
        MonarchDataGridColumn("Invoice", id: "number", isHideable: false, pinned: .start, value: { $0.id }),
        MonarchDataGridColumn(
            "Customer",
            id: "customer",
            edit: editable
                ? MonarchDataGridEdit(
                    isRequired: true,
                    isEditable: { $0.id != "INV-2" },
                    value: { .text($0.customer) },
                    onCommit: { _, _ in }
                ) : nil,
            value: { $0.customer }
        ),
        MonarchDataGridColumn(
            "Status",
            id: "status",
            edit: editable
                ? MonarchDataGridEdit(
                    kind: .select,
                    options: [
                        MonarchEditableCellOption("Sent", value: "Sent"),
                        MonarchEditableCellOption("Paid", value: "Paid"),
                    ],
                    value: { .text($0.status) },
                    onCommit: { _, _ in }
                ) : nil,
            value: { $0.status }
        ),
        MonarchDataGridColumn(
            "Amount",
            id: "amount",
            isNumeric: true,
            pinned: .end,
            width: 120,
            value: { String(format: "%.2f", $0.amount) },
            sortBy: { $0.amount < $1.amount }
        ),
        MonarchDataGridColumn("Note", id: "note", isSortable: false, isPinnable: false, value: { _ in "" }),
    ]
}

private let gridHeight = MonarchTokens.Space.space24 * 4

private func gridActions(_ invoice: GridInvoice) -> [MonarchDataTableRowAction] {
    [
        MonarchDataTableRowAction("Send reminder", systemImage: "bell") {},
        MonarchDataTableRowAction("Void", systemImage: "trash", isDestructive: true, isDisabled: invoice.id == "INV-2")
        {},
    ]
}

enum MonarchDataGridFixtures: ComponentFixtures {
    static let id = "data-grid"
    static let fixtures = [
        Fixture("grid: pinned, editable, sorted, selected, actions, open") {
            MonarchDataGrid(
                "Invoices",
                rows: gridInvoices,
                columns: gridColumns(),
                sort: .constant(MonarchDataTableSort("amount", order: .reverse)),
                selection: .constant(["INV-1"]),
                hiddenColumns: .constant([]),
                pinning: .constant(nil),
                columnWidths: .constant(["customer": 200]),
                layout: .table,
                isRowSelectable: { $0.id != "INV-3" },
                rowActions: gridActions,
                onOpen: { _ in }
            )
            .frame(height: gridHeight)
        },
        Fixture("grid: compact, read-only, hidden and reordered, loading more") {
            MonarchDataGrid(
                "Invoices",
                rows: gridInvoices,
                columns: gridColumns(editable: false),
                hiddenColumns: .constant(["status"]),
                columnOrder: ["amount"],
                layout: .table,
                density: .compact,
                isLoading: true,
                footer: MonarchLoadMore(hasMore: true, loadedCount: 3, totalCount: 30, onLoadMore: {})
            )
            .frame(height: gridHeight)
        },
        Fixture("list: records with an editor, selection, sort, actions") {
            MonarchDataGrid(
                "Invoices",
                rows: gridInvoices,
                columns: gridColumns(),
                sort: .constant(MonarchDataTableSort("number")),
                selection: .constant([]),
                layout: .list,
                rowActions: gridActions,
                onOpen: { _ in }
            )
            .frame(height: gridHeight)
        },
        Fixture("automatic layout") {
            MonarchDataGrid("Invoices", rows: gridInvoices, columns: gridColumns())
                .frame(height: gridHeight)
        },
        Fixture("loading") {
            MonarchDataGrid("Invoices", rows: [GridInvoice](), columns: gridColumns(), layout: .table, isLoading: true)
                .frame(height: gridHeight)
        },
        Fixture("empty with custom labels") {
            MonarchDataGrid(
                "Invoices",
                rows: [GridInvoice](),
                columns: gridColumns(),
                layout: .table,
                labels: MonarchDataGridLabels(emptyTitle: "No invoices", emptyMessage: "New invoices appear here.")
            )
            .frame(height: gridHeight)
        },
        Fixture("record editor") {
            MonarchDataGridRecordEditor(
                row: gridInvoices[0],
                name: "INV-1",
                columns: gridColumns(),
                labels: MonarchDataGridLabels(),
                onOpen: { _ in }
            )
            .frame(height: gridHeight * 2)
        },
    ]
}

@Suite("MonarchDataGrid")
struct MonarchDataGridTests {
    private typealias Tokens = MonarchDataGridTokens

    @Test("A column's pin comes from the pinning when given, else from the column")
    func pins() {
        let columns = gridColumns()
        #expect(Tokens.pin(of: columns[0], pinning: nil) == .start)
        #expect(Tokens.pin(of: columns[3], pinning: nil) == .end)
        #expect(Tokens.pin(of: columns[0], pinning: [:]) == nil)
        #expect(Tokens.pin(of: columns[1], pinning: ["customer": .end]) == .end)
        #expect(Tokens.pinning(columns, pinning: nil) == ["number": .start, "amount": .end])
        #expect(Tokens.pinned(columns, pinning: nil, columnID: "number", pin: nil) == ["amount": .end])
        #expect(
            Tokens.pinned(columns, pinning: ["status": .start], columnID: "customer", pin: .end)
                == ["status": .start, "customer": .end]
        )
    }

    @Test("Widths come from the app, the column or the default, and resize within bounds")
    func widths() {
        let columns = gridColumns()
        #expect(Tokens.width(of: columns[0], widths: [:]) == 160)
        #expect(Tokens.width(of: columns[3], widths: [:]) == 120)
        #expect(Tokens.width(of: columns[3], widths: ["amount": 200]) == 200)
        #expect(Tokens.resized(columns[0], widths: [:], delta: Tokens.resizeStep) == ["number": 176])
        #expect(Tokens.resized(columns[0], widths: ["number": 470], delta: Tokens.resizeStep) == ["number": 480])
        #expect(Tokens.resized(columns[0], widths: ["number": 100], delta: -Tokens.resizeStep) == ["number": 96])
        #expect(Tokens.minimumColumnWidth == 96)
        #expect(Tokens.maximumColumnWidth == 480)
    }

    @Test("Columns are split into start, middle and end; wide pins scroll with the rest")
    func arrangement() {
        let columns = gridColumns()
        let wide = Tokens.arranged(columns, hidden: [], order: [], pinning: nil, widths: [:], viewport: 1000)
        #expect(wide.start.map(\.id) == ["number"])
        #expect(wide.middle.map(\.id) == ["customer", "status", "note"])
        #expect(wide.end.map(\.id) == ["amount"])
        #expect(wide.all.map(\.id) == ["number", "customer", "status", "note", "amount"])
        // 160 + 120 pinned is more than half of 500.
        let narrow = Tokens.arranged(columns, hidden: [], order: [], pinning: nil, widths: [:], viewport: 500)
        #expect(narrow.start.isEmpty && narrow.end.isEmpty)
        #expect(narrow.middle.map(\.id) == ["number", "customer", "status", "amount", "note"])
        let hidden = Tokens.arranged(
            columns,
            hidden: ["status", "number"],
            order: ["note"],
            pinning: [:],
            widths: [:],
            viewport: 1000
        )
        #expect(hidden.middle.map(\.id) == ["note", "number", "customer", "amount"])
    }

    @Test("A pinned group is held inside the visible width")
    func sticky() {
        #expect(Tokens.stickyOffset(minX: 0, maxX: 160, visibleWidth: 400) == 0)
        #expect(Tokens.stickyOffset(minX: -50, maxX: 110, visibleWidth: 400) == 50)
        #expect(Tokens.stickyOffset(minX: 600, maxX: 720, visibleWidth: 400) == -320)
        #expect(Tokens.stickyOffset(minX: 300, maxX: 400, visibleWidth: 400) == 0)
        // A group wider than the view stays at the leading edge.
        #expect(Tokens.stickyOffset(minX: 100, maxX: 600, visibleWidth: 400) == -100)
    }

    @Test("Rows sort by the column's rule or text, stable, and stay put for unsortable columns")
    func sorting() {
        let columns = gridColumns()
        func ids(_ sort: MonarchDataTableSort?) -> [String] {
            Tokens.sorted(gridInvoices, by: sort, columns: columns).map(\.id)
        }
        #expect(ids(nil) == ["INV-1", "INV-2", "INV-3"])
        #expect(ids(MonarchDataTableSort("amount")) == ["INV-3", "INV-2", "INV-1"])
        #expect(ids(MonarchDataTableSort("amount", order: .reverse)) == ["INV-1", "INV-2", "INV-3"])
        #expect(ids(MonarchDataTableSort("note", order: .reverse)) == ["INV-1", "INV-2", "INV-3"])
        #expect(ids(MonarchDataTableSort("gone")) == ["INV-1", "INV-2", "INV-3"])
        #expect(Tokens.order(of: "amount", sort: MonarchDataTableSort("amount", order: .reverse)) == .reverse)
        #expect(Tokens.order(of: "number", sort: MonarchDataTableSort("amount")) == nil)
    }

    @Test("The column menu offers only what the column and the app allow")
    func menu() {
        let columns = gridColumns()
        let all = MonarchDataGridMenuAbilities(sort: true, pin: true, hide: true, resize: true)
        #expect(
            Tokens.menuItems(columns[1], order: nil, pin: nil, width: 160, abilities: all)
                == [.sortAscending, .sortDescending, .pinStart, .pinEnd, .hide, .widen, .narrow]
        )
        #expect(
            Tokens.menuItems(columns[0], order: .forward, pin: .start, width: 96, abilities: all)
                == [.sortDescending, .clearSort, .pinEnd, .unpin, .widen]
        )
        #expect(
            Tokens.menuItems(columns[3], order: .reverse, pin: .end, width: 480, abilities: all)
                == [.sortAscending, .clearSort, .pinStart, .unpin, .hide, .narrow]
        )
        #expect(
            Tokens.menuItems(columns[4], order: nil, pin: nil, width: 160, abilities: all) == [.hide, .widen, .narrow]
        )
        let none = MonarchDataGridMenuAbilities(sort: false, pin: false, hide: false, resize: false)
        #expect(Tokens.menuItems(columns[1], order: nil, pin: nil, width: 160, abilities: none).isEmpty)
    }

    @Test("Menu items turn into a sort, a pin and a width change")
    func menuEffects() {
        let current = MonarchDataTableSort("amount")
        #expect(
            Tokens.sort(after: .sortAscending, columnID: "status", current: current) == MonarchDataTableSort("status")
        )
        #expect(
            Tokens.sort(after: .sortDescending, columnID: "status", current: current)
                == MonarchDataTableSort("status", order: .reverse)
        )
        #expect(Tokens.sort(after: .clearSort, columnID: "amount", current: current) == nil)
        #expect(Tokens.sort(after: .hide, columnID: "amount", current: current) == current)
        #expect(Tokens.pin(after: .pinStart) == .some(.start))
        #expect(Tokens.pin(after: .pinEnd) == .some(.end))
        #expect(Tokens.pin(after: .unpin) == .some(nil))
        #expect(Tokens.pin(after: .widen) == nil)
        #expect(Tokens.resizeDelta(after: .widen) == 16)
        #expect(Tokens.resizeDelta(after: .narrow) == -16)
        #expect(Tokens.resizeDelta(after: .hide) == nil)
        for item in MonarchDataGridMenuItem.allCases {
            #expect(!Tokens.title(item, labels: MonarchDataGridLabels()).hasPrefix("monarch."))
            #expect(!Tokens.symbol(item).isEmpty)
        }
        #expect(Tokens.title(.hide, labels: MonarchDataGridLabels(hideColumn: "Ocultar")) == "Ocultar")
    }

    @Test("Headers read their sort and pin")
    func headerState() {
        let labels = MonarchDataGridLabels()
        #expect(Tokens.headerState(isSortable: true, order: nil, pin: nil, labels: labels) == "Not sorted")
        #expect(Tokens.headerState(isSortable: false, order: nil, pin: nil, labels: labels).isEmpty)
        #expect(
            Tokens.headerState(isSortable: true, order: .reverse, pin: .end, labels: labels)
                == "Sorted descending, Pinned to end"
        )
        #expect(
            Tokens.headerState(isSortable: false, order: .forward, pin: .start, labels: labels)
                == "Sorted ascending, Pinned to start"
        )
        #expect(
            Tokens.headerState(
                isSortable: true,
                order: nil,
                pin: nil,
                labels: MonarchDataGridLabels(notSorted: "Sin ordenar")
            ) == "Sin ordenar"
        )
    }

    @Test("Cells, checkboxes and menus are named for VoiceOver")
    func spokenText() {
        let labels = MonarchDataGridLabels()
        #expect(Tokens.cellLabel(column: "Status", value: "Paid", labels: labels) == "Status: Paid")
        #expect(Tokens.cellLabel(column: "Status", value: "", labels: labels) == "Status: No value")
        #expect(Tokens.editLabel(column: "Customer", row: "INV-1", labels: labels) == "Customer for INV-1")
        #expect(Tokens.selectLabel(row: "INV-1", labels: labels) == "Select INV-1")
        #expect(Tokens.actionsLabel(row: "INV-1", labels: labels) == "Actions for INV-1")
        let custom = MonarchDataGridLabels(
            selectRow: { "Elegir \($0)" },
            rowActions: { "Acciones de \($0)" },
            noValue: "Vacío",
            editLabel: { "\($0) de \($1)" },
            cell: { "\($0) = \($1)" }
        )
        #expect(Tokens.cellLabel(column: "Estado", value: "", labels: custom) == "Estado = Vacío")
        #expect(Tokens.editLabel(column: "Cliente", row: "INV-1", labels: custom) == "Cliente de INV-1")
        #expect(Tokens.selectLabel(row: "INV-1", labels: custom) == "Elegir INV-1")
        #expect(Tokens.actionsLabel(row: "INV-1", labels: custom) == "Acciones de INV-1")
        #expect(Tokens.cellText("") == "—")
        #expect(Tokens.cellText("Paid") == "Paid")
    }

    @Test("Colours: selected rows on the accent's subtle fill, roles for every part")
    func colors() {
        #expect(Tokens.rowFill(isSelected: true) == \MonarchColorTokens.accentSubtle)
        #expect(Tokens.rowFill(isSelected: false) == \MonarchColorTokens.bgSurface)
        #expect(Tokens.headerFill == \MonarchColorTokens.bgSubtle)
        #expect(Tokens.pinnedEdge == \MonarchColorTokens.borderDefault)
        #expect(Tokens.divider == \MonarchColorTokens.borderSubtle)
        #expect(Tokens.link == \MonarchColorTokens.textLink)
        #expect(Tokens.cellPadding(.comfortable) > Tokens.cellPadding(.compact))
    }

    @Test("Records with an editable cell open the editor; the list hint says Edit")
    func records() {
        let columns = gridColumns()
        #expect(Tokens.editsRow(gridInvoices[0], columns: columns))
        // INV-2's customer cannot be edited, but its status can.
        #expect(Tokens.editsRow(gridInvoices[1], columns: columns))
        #expect(!Tokens.editsRow(gridInvoices[1], columns: Array(columns.prefix(2))))
        #expect(!Tokens.editsRow(gridInvoices[0], columns: gridColumns(editable: false)))
        let labels = Tokens.tableLabels(MonarchDataGridLabels(), editsRecords: true)
        #expect(labels.openHint == "Edit")
        #expect(labels.selectRow?("INV-1") == "Select INV-1")
        #expect(labels.selectedCount?(3) == "3 selected")
        #expect(Tokens.tableLabels(MonarchDataGridLabels(), editsRecords: false).openHint == nil)
        let list = Tokens.listColumns(columns, sorts: false)
        #expect(list.allSatisfy { !$0.isSortable })
        #expect(Tokens.listColumns(columns, sorts: true).map(\.isSortable) == [true, true, true, true, false])
        #expect(columns[0].chooserColumn == MonarchColumnChooserColumn("Invoice", id: "number", isHideable: false))
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchDataGridStrings.selectAll == "Select all rows")
        #expect(MonarchDataGridStrings.selectRow("INV-1") == "Select INV-1")
        #expect(MonarchDataGridStrings.rowActions("INV-1") == "Actions for INV-1")
        #expect(MonarchDataGridStrings.selectedCount(3) == "3 selected")
        #expect(MonarchDataGridStrings.actionsHeader == "Actions")
        #expect(MonarchDataGridStrings.columnOptions == "Column options")
        #expect(MonarchDataGridStrings.sortAscending == "Sort ascending")
        #expect(MonarchDataGridStrings.sortDescending == "Sort descending")
        #expect(MonarchDataGridStrings.clearSort == "Clear sort")
        #expect(MonarchDataGridStrings.pinStart == "Pin to start")
        #expect(MonarchDataGridStrings.pinEnd == "Pin to end")
        #expect(MonarchDataGridStrings.unpin == "Unpin")
        #expect(MonarchDataGridStrings.hideColumn == "Hide column")
        #expect(MonarchDataGridStrings.widen == "Widen column")
        #expect(MonarchDataGridStrings.narrow == "Narrow column")
        #expect(MonarchDataGridStrings.sortedAscending == "Sorted ascending")
        #expect(MonarchDataGridStrings.sortedDescending == "Sorted descending")
        #expect(MonarchDataGridStrings.notSorted == "Not sorted")
        #expect(MonarchDataGridStrings.pinnedStart == "Pinned to start")
        #expect(MonarchDataGridStrings.pinnedEnd == "Pinned to end")
        #expect(MonarchDataGridStrings.open == "Open details")
        #expect(MonarchDataGridStrings.editRecord == "Edit")
        #expect(MonarchDataGridStrings.done == "Done")
        #expect(MonarchDataGridStrings.noValue == "No value")
        #expect(MonarchDataGridStrings.loading == "Loading rows")
        #expect(MonarchDataGridStrings.emptyTitle == "Nothing here yet")
        #expect(MonarchDataGridStrings.emptyMessage == "Rows appear here once they are added.")
        #expect(MonarchDataGridStrings.editLabel("Customer", "INV-1") == "Customer for INV-1")
        #expect(MonarchDataGridStrings.cell("Status", "Paid") == "Status: Paid")
    }
}
