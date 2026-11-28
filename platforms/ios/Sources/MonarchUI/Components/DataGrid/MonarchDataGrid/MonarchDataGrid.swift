import MonarchTokens
import SwiftUI

/// An editable grid of records: fixed-width columns that sort, pin, hide and
/// resize from a menu on their header, cells that edit in place, selection,
/// row actions and a load-more footer. At compact width it is a list of
/// records whose editor opens in a sheet.
///
/// ```swift
/// MonarchDataGrid(
///     "Invoices",
///     rows: invoices,
///     columns: [
///         MonarchDataGridColumn("Invoice", id: "number", isHideable: false, pinned: .start, value: { $0.number }),
///         MonarchDataGridColumn(
///             "Customer",
///             edit: MonarchDataGridEdit(value: { .text($0.customer) }, onCommit: { row, value in try await save(row, value) }),
///             value: { $0.customer }
///         ),
///     ],
///     sort: $sort,
///     selection: $selected
/// )
/// ```
///
/// SwiftUI's `Table` does not edit cells, pin columns or scroll sideways on
/// iPad, so the grid is one `ScrollView` that scrolls both ways, with a
/// `LazyVStack` of rows under a header pinned to the top. Pinned columns are
/// held at their edge by a visual effect as the rows scroll sideways. Pins
/// that would take more than half the width scroll with the rest.
///
/// At compact width and at the accessibility text sizes (`layout:
/// .automatic`) the grid follows the iPhone pattern: ``MonarchDataTable``'s
/// list of records, and a tap on a record with editable cells opens a sheet
/// with the record's fields, where they edit.
///
/// The column menu offers only what the app can store: sorting with `sort`,
/// pinning with `pinning`, hiding with `hiddenColumns` and Widen and Narrow
/// with `columnWidths`. Without any of them a header is a plain header.
///
/// Accessibility: the grid is a container named by `label`. Headers are
/// headings; a header with a menu is a button whose value is its sort and
/// pin ("Sorted descending, Pinned to end") and whose hint is "Column
/// options". A read-only cell reads "Column: value", or "No value"; when rows
/// open it is a button with the hint "Open details". An editable cell is a
/// ``MonarchEditableCell`` named "Column for row". Row actions are a menu
/// named "Actions for row", the context menu, and VoiceOver actions on each
/// read-only cell. Resizing is a menu choice, never only a drag.
public struct MonarchDataGrid<Row: Identifiable>: View {
    private let label: String
    private let rows: [Row]
    private let columns: [MonarchDataGridColumn<Row>]
    private let sort: Binding<MonarchDataTableSort?>?
    private let selection: Binding<Set<Row.ID>>?
    private let hiddenColumns: Binding<Set<String>>?
    private let columnOrder: [String]
    private let pinning: Binding<[String: MonarchDataGridPin]?>?
    private let columnWidths: Binding<[String: CGFloat]>?
    private let layout: MonarchDataTableLayout
    private let density: MonarchDataGridDensity
    private let sortsRows: Bool
    private let isLoading: Bool
    private let footer: MonarchLoadMore?
    private let labels: MonarchDataGridLabels
    private let rowLabel: ((Row) -> String)?
    private let isRowSelectable: ((Row) -> Bool)?
    private let rowActions: ((Row) -> [MonarchDataTableRowAction])?
    private let onOpen: (@MainActor (Row) -> Void)?

    @State private var editingID: Row.ID?
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    #if os(iOS)
        @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    #endif

    /// A data grid.
    ///
    /// - Parameters:
    ///   - label: Names the grid for VoiceOver, such as "Invoices". Already localised.
    ///   - rows: The rows, in the app's order.
    ///   - columns: Every column in its default order.
    ///   - sort: The sort. With it, column menus offer sorting.
    ///   - selection: The ids of selected rows. With it, rows get checkboxes
    ///     and the header a select-all checkbox.
    ///   - hiddenColumns: The ids of hidden columns. With it, column menus offer Hide column.
    ///   - columnOrder: Column ids in display order; missing ids follow in their default order.
    ///   - pinning: Every pinned column by id, or `nil` inside the binding to
    ///     use each column's own pin. With it, column menus offer pinning.
    ///   - columnWidths: Widths by column id. With it, column menus offer Widen
    ///     and Narrow, in 16-point steps between 96 and 480 points.
    ///   - layout: The grid, the record list, or the one that suits the width.
    ///   - density: The padding above and below cells.
    ///   - sortsRows: Whether the grid sorts `rows` itself. Pass `false` when the server sorts.
    ///   - isLoading: Without rows shows placeholder rows; with rows shows a progress bar above them.
    ///   - footer: A load-more footer under the rows. Pass `autoLoads: true`
    ///     to load the next page when the end comes into view.
    ///   - labels: Replacements for the built-in strings.
    ///   - rowLabel: A row's name in spoken labels and the record list. By default the first shown cell.
    ///   - isRowSelectable: Whether a row has a checkbox.
    ///   - rowActions: A row's actions: an actions column with a menu, the context menu and VoiceOver actions.
    ///   - onOpen: Opens a row: a tap on a read-only cell, or Open details in the record editor.
    public init(
        _ label: String,
        rows: [Row],
        columns: [MonarchDataGridColumn<Row>],
        sort: Binding<MonarchDataTableSort?>? = nil,
        selection: Binding<Set<Row.ID>>? = nil,
        hiddenColumns: Binding<Set<String>>? = nil,
        columnOrder: [String] = [],
        pinning: Binding<[String: MonarchDataGridPin]?>? = nil,
        columnWidths: Binding<[String: CGFloat]>? = nil,
        layout: MonarchDataTableLayout = .automatic,
        density: MonarchDataGridDensity = .comfortable,
        sortsRows: Bool = true,
        isLoading: Bool = false,
        footer: MonarchLoadMore? = nil,
        labels: MonarchDataGridLabels = MonarchDataGridLabels(),
        rowLabel: ((Row) -> String)? = nil,
        isRowSelectable: ((Row) -> Bool)? = nil,
        rowActions: ((Row) -> [MonarchDataTableRowAction])? = nil,
        onOpen: (@MainActor (Row) -> Void)? = nil
    ) {
        self.label = label
        self.rows = rows
        self.columns = columns
        self.sort = sort
        self.selection = selection
        self.hiddenColumns = hiddenColumns
        self.columnOrder = columnOrder
        self.pinning = pinning
        self.columnWidths = columnWidths
        self.layout = layout
        self.density = density
        self.sortsRows = sortsRows
        self.isLoading = isLoading
        self.footer = footer
        self.labels = labels
        self.rowLabel = rowLabel
        self.isRowSelectable = isRowSelectable
        self.rowActions = rowActions
        self.onOpen = onOpen
    }

    private typealias Tokens = MonarchDataGridTokens

    private var isRegularWidth: Bool {
        #if os(iOS)
            horizontalSizeClass == .regular
        #else
            true
        #endif
    }

    private var shownRows: [Row] {
        guard sortsRows, let sort else { return rows }
        return Tokens.sorted(rows, by: sort.wrappedValue, columns: columns)
    }

    private var shownColumns: [MonarchDataGridColumn<Row>] {
        let byID = Dictionary(columns.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        return MonarchDataTableTokens.visibleColumns(
            columns.map(\.tableColumn),
            hidden: hiddenColumns?.wrappedValue ?? [],
            order: columnOrder
        )
        .compactMap { byID[$0.id] }
    }

    /// The content of the view.
    public var body: some View {
        let resolved = MonarchDataTableTokens.resolvedLayout(
            layout,
            isRegularWidth: isRegularWidth,
            isAccessibilitySize: dynamicTypeSize.isAccessibilitySize,
            supportsTable: true
        )
        if resolved == .list {
            records
        } else {
            switch MonarchDataTableTokens.phase(
                rowCount: rows.count,
                isLoading: isLoading,
                errorMessage: nil,
                isFiltered: false
            ) {
            case .loading:
                MonarchLoadingState(
                    labels.loading ?? MonarchDataGridStrings.loading,
                    variant: .rows,
                    rows: Tokens.loadingRows
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            case .ready:
                grid
            case .empty, .filtered, .failed:
                MonarchEmptyState(
                    labels.emptyTitle ?? MonarchDataGridStrings.emptyTitle,
                    message: labels.emptyMessage ?? MonarchDataGridStrings.emptyMessage,
                    size: .sm
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
    }

    private var grid: some View {
        MonarchDataGridTable(
            label: label,
            rows: shownRows,
            columns: columns,
            sort: sort,
            selection: selection,
            hiddenColumns: hiddenColumns,
            columnOrder: columnOrder,
            pinning: pinning,
            columnWidths: columnWidths,
            density: density,
            isLoading: isLoading,
            footer: footer,
            labels: labels,
            rowLabel: rowLabel,
            isRowSelectable: isRowSelectable,
            rowActions: rowActions,
            onOpen: onOpen,
            fitsViewport: !isRegularWidth
        )
    }

    // MARK: Record list

    private var records: some View {
        let shown = shownColumns
        let editsAny = columns.contains { $0.edit != nil }
        var openRecord: (@MainActor (Row) -> Void)?
        if editsAny || onOpen != nil {
            openRecord = { @MainActor row in open(row) }
        }
        return MonarchDataTable(
            label,
            rows: shownRows,
            columns: Tokens.listColumns(columns, sorts: sort != nil),
            sort: sort ?? .constant(nil),
            selection: selection,
            hiddenColumns: hiddenColumns?.wrappedValue ?? [],
            columnOrder: columnOrder,
            layout: .list,
            sortsRows: false,
            isLoading: isLoading,
            footer: footer,
            labels: Tokens.tableLabels(labels, editsRecords: editsAny),
            rowLabel: rowLabel,
            isRowSelectable: isRowSelectable,
            rowActions: listActions(columns: shown),
            onOpen: openRecord
        )
        .sheet(
            isPresented: Binding(
                get: { editingID != nil },
                set: { if !$0 { editingID = nil } }
            )
        ) {
            if let editingID, let row = rows.first(where: { $0.id == editingID }) {
                MonarchDataGridRecordEditor(
                    row: row,
                    name: MonarchDataTableTokens.rowName(row, columns: shown.map(\.tableColumn), rowLabel: rowLabel),
                    columns: shown,
                    labels: labels,
                    onOpen: onOpen
                )
            }
        }
        .onChange(of: rows.map(\.id)) { _, ids in
            if let editingID, !ids.contains(editingID) { self.editingID = nil }
        }
    }

    private func open(_ row: Row) {
        if Tokens.editsRow(row, columns: shownColumns) {
            editingID = row.id
        } else {
            onOpen?(row)
        }
    }

    private func listActions(columns shown: [MonarchDataGridColumn<Row>]) -> ((Row) -> [MonarchDataTableRowAction])? {
        let onOpen = onOpen
        let rowActions = rowActions
        let openTitle = labels.open ?? MonarchDataGridStrings.open
        guard rowActions != nil || onOpen != nil else { return nil }
        return { row in
            var actions = rowActions?(row) ?? []
            if let onOpen, Tokens.editsRow(row, columns: shown) {
                actions.append(MonarchDataTableRowAction(openTitle, systemImage: "info.circle") { onOpen(row) })
            }
            return actions
        }
    }
}

#if DEBUG
    private struct MonarchDataGridPreviewRow: Identifiable {
        let id: String
        let customer: String
        let amount: String
    }

    #Preview("MonarchDataGrid") {
        let rows = [
            MonarchDataGridPreviewRow(id: "INV-1", customer: "Example Ltd", amount: "120.00"),
            MonarchDataGridPreviewRow(id: "INV-2", customer: "", amount: "80.50"),
        ]
        let columns: [MonarchDataGridColumn<MonarchDataGridPreviewRow>] = [
            MonarchDataGridColumn("Invoice", pinned: .start, value: { $0.id }),
            MonarchDataGridColumn(
                "Customer",
                edit: MonarchDataGridEdit(value: { .text($0.customer) }, onCommit: { _, _ in }),
                value: { $0.customer }
            ),
            MonarchDataGridColumn("Amount", isNumeric: true, value: { $0.amount }),
        ]
        MonarchPreviewMatrix {
            MonarchDataGrid(
                "Invoices",
                rows: rows,
                columns: columns,
                sort: .constant(MonarchDataTableSort("Amount", order: .reverse)),
                selection: .constant(["INV-1"]),
                layout: .table
            )
            .frame(height: MonarchTokens.Space.space24 * 3)
            MonarchDataGrid("Invoices", rows: rows, columns: columns, layout: .list)
                .frame(height: MonarchTokens.Space.space24 * 3)
        }
    }
#endif
