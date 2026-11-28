import MonarchTokens
import SwiftUI

/// Records in columns: a SwiftUI `Table` at regular width, and a `List` of
/// records with a labelled line per field at compact width, with sorting,
/// selection, row actions, column visibility and order, and the loading,
/// empty, filtered and error states.
///
/// ```swift
/// MonarchDataTable(
///     "Open tickets",
///     rows: tickets,
///     columns: [
///         MonarchDataTableColumn("Subject", value: { $0.subject }),
///         MonarchDataTableColumn("Status", value: { $0.status }),
///         MonarchDataTableColumn("Age", isNumeric: true, value: { $0.ageText }, sortBy: { $0.age < $1.age }),
///     ],
///     sort: $sort,
///     selection: $selected,
///     onOpen: { open($0) }
/// )
/// ```
///
/// SwiftUI's `Table` shows only its first column at compact width, so
/// Monarch supplies the compact row: the first shown column is the row's
/// name, every other column is a "Field value" line, and actions sit in a
/// menu, a swipe and the context menu. Both layouts fill the space they are
/// given and scroll; do not put the table inside another scroll view.
///
/// Selection is a checkbox per row and a select-all checkbox above the rows,
/// in both layouts, so it works without edit mode. Columns take the state of
/// ``MonarchColumnChooser``: `hiddenColumns` and `columnOrder`.
///
/// Accessibility: the table is named by `caption`. In the list layout each
/// record is one element read as "Ticket 42, Status: Open, Owner: Ana Lima",
/// with "No value" for empty cells, the open hint when it opens, and its
/// actions as VoiceOver actions; its checkbox ("Select Ticket 42") and actions
/// menu ("Actions for Ticket 42") are separate. In the table layout the
/// system reads headers and cells, empty cells as "No value". The sort menu
/// reads the sorted column and its direction.
public struct MonarchDataTable<Row: Identifiable>: View {
    private let caption: String
    private let rows: [Row]
    private let columns: [MonarchDataTableColumn<Row>]
    private let sortBinding: Binding<MonarchDataTableSort?>?
    private let selection: Binding<Set<Row.ID>>?
    private let hiddenColumns: Set<String>
    private let columnOrder: [String]
    private let layout: MonarchDataTableLayout
    private let sortsRows: Bool
    private let isLoading: Bool
    private let errorMessage: String?
    private let isFiltered: Bool
    private let footer: MonarchLoadMore?
    private let labels: MonarchDataTableLabels
    private let rowLabel: ((Row) -> String)?
    private let isRowSelectable: ((Row) -> Bool)?
    private let rowActions: ((Row) -> [MonarchDataTableRowAction])?
    private let onRetry: (@MainActor () -> Void)?
    private let onClearFilters: (@MainActor () -> Void)?
    private let onOpen: (@MainActor (Row) -> Void)?

    @State private var internalSort: MonarchDataTableSort?
    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    #if os(iOS)
        @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    #endif

    /// A data table.
    ///
    /// - Parameters:
    ///   - caption: Names the table for VoiceOver, such as "Open tickets". Already localised.
    ///   - rows: The rows, in the order the app gives them.
    ///   - columns: Every column, in their default order.
    ///   - sort: The sort. `nil` keeps the sort inside the table, starting unsorted.
    ///   - selection: The ids of selected rows. With it, rows get checkboxes.
    ///   - hiddenColumns: The ids of hidden columns, as ``MonarchColumnChooser`` writes them.
    ///   - columnOrder: Column ids in display order; missing ids follow in their default order.
    ///   - layout: A table, a list, or the one that suits the width.
    ///   - sortsRows: Whether the table sorts `rows` itself. Pass `false` when the server sorts.
    ///   - isLoading: Whether rows are loading. With no rows, shows placeholder rows.
    ///   - errorMessage: With no rows, shows the error state with this message.
    ///   - isFiltered: Whether filters are applied, so no rows means nothing matched.
    ///   - footer: A load-more footer under the rows.
    ///   - labels: Replacements for the built-in strings.
    ///   - rowLabel: A row's name in spoken labels. By default the first shown column's text.
    ///   - isRowSelectable: Whether a row can be selected.
    ///   - rowActions: The actions of a row, in a menu, the context menu and swipe actions.
    ///   - onRetry: Adds a retry button to the error state.
    ///   - onClearFilters: Adds a clear-filters button to the filtered state.
    ///   - onOpen: Opens a row: its name becomes a button.
    public init(
        _ caption: String,
        rows: [Row],
        columns: [MonarchDataTableColumn<Row>],
        sort: Binding<MonarchDataTableSort?>? = nil,
        selection: Binding<Set<Row.ID>>? = nil,
        hiddenColumns: Set<String> = [],
        columnOrder: [String] = [],
        layout: MonarchDataTableLayout = .automatic,
        sortsRows: Bool = true,
        isLoading: Bool = false,
        errorMessage: String? = nil,
        isFiltered: Bool = false,
        footer: MonarchLoadMore? = nil,
        labels: MonarchDataTableLabels = MonarchDataTableLabels(),
        rowLabel: ((Row) -> String)? = nil,
        isRowSelectable: ((Row) -> Bool)? = nil,
        rowActions: ((Row) -> [MonarchDataTableRowAction])? = nil,
        onRetry: (@MainActor () -> Void)? = nil,
        onClearFilters: (@MainActor () -> Void)? = nil,
        onOpen: (@MainActor (Row) -> Void)? = nil
    ) {
        self.caption = caption
        self.rows = rows
        self.columns = columns
        self.sortBinding = sort
        self.selection = selection
        self.hiddenColumns = hiddenColumns
        self.columnOrder = columnOrder
        self.layout = layout
        self.sortsRows = sortsRows
        self.isLoading = isLoading
        self.errorMessage = errorMessage
        self.isFiltered = isFiltered
        self.footer = footer
        self.labels = labels
        self.rowLabel = rowLabel
        self.isRowSelectable = isRowSelectable
        self.rowActions = rowActions
        self.onRetry = onRetry
        self.onClearFilters = onClearFilters
        self.onOpen = onOpen
    }

    private typealias Tokens = MonarchDataTableTokens
    private typealias Comparator = MonarchDataTableComparator<Row>

    private var sort: Binding<MonarchDataTableSort?> { sortBinding ?? $internalSort }

    private var isRegularWidth: Bool {
        #if os(iOS)
            horizontalSizeClass == .regular
        #else
            true
        #endif
    }

    private var supportsTable: Bool {
        if #available(iOS 17.4, macOS 14.4, *) { true } else { false }
    }

    /// The content of the view.
    public var body: some View {
        switch Tokens.phase(
            rowCount: rows.count,
            isLoading: isLoading,
            errorMessage: errorMessage,
            isFiltered: isFiltered
        ) {
        case .failed:
            MonarchErrorState(
                labels.errorTitle ?? MonarchDataTableStrings.errorTitle,
                message: errorMessage,
                size: .sm,
                isRetrying: isLoading,
                retryLabel: labels.retry,
                onRetry: onRetry
            )
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .loading:
            MonarchLoadingState(
                labels.loading ?? MonarchDataTableStrings.loading,
                variant: .rows,
                rows: Tokens.loadingRows
            )
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        case .filtered:
            MonarchEmptyState(
                labels.filteredTitle ?? MonarchDataTableStrings.filteredTitle,
                message: labels.filteredMessage ?? MonarchDataTableStrings.filteredMessage,
                kind: .filtered,
                size: .sm,
                onClearFilters: onClearFilters
            )
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .empty:
            MonarchEmptyState(
                labels.emptyTitle ?? MonarchDataTableStrings.emptyTitle,
                message: labels.emptyMessage ?? MonarchDataTableStrings.emptyMessage,
                size: .sm
            )
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .ready:
            ready
        }
    }

    // MARK: Ready

    private var ready: some View {
        let shownColumns = Tokens.visibleColumns(columns, hidden: hiddenColumns, order: columnOrder)
        let shownRows = sortsRows ? Tokens.sorted(rows, by: sort.wrappedValue, columns: columns) : rows
        let resolved = Tokens.resolvedLayout(
            layout,
            isRegularWidth: isRegularWidth,
            isAccessibilitySize: dynamicTypeSize.isAccessibilitySize,
            supportsTable: supportsTable
        )
        // A table at compact width is the Monarch grid: the columns keep their headers and the rows scroll sideways with
        // the first column pinned, instead of a card per record.
        if layout == .automatic, !isRegularWidth, !dynamicTypeSize.isAccessibilitySize {
            return AnyView(compactGrid(shownRows, columns: shownColumns))
        }
        return AnyView(
            VStack(alignment: .leading, spacing: 0) {
                bar(columns: shownColumns, showsSort: resolved == .list)
                if resolved == .table {
                    if #available(iOS 17.4, macOS 14.4, *) {
                        table(shownRows, columns: shownColumns)
                        if let footer { footer }
                    } else {
                        list(shownRows, columns: shownColumns)
                    }
                } else {
                    list(shownRows, columns: shownColumns)
                }
            }
        )
    }

    /// The compact-width table: ``MonarchDataGrid``'s grid over this table's columns, rows, sort and selection.
    private func compactGrid(_ shownRows: [Row], columns shownColumns: [MonarchDataTableColumn<Row>]) -> some View {
        return VStack(alignment: .leading, spacing: MonarchTokens.Space.space2) {
            compactGridTable(shownRows, columns: shownColumns)
            if let selection, !selection.wrappedValue.isEmpty {
                Text(
                    labels.selectedCount?(selection.wrappedValue.count)
                        ?? MonarchDataTableStrings.selectedCount(selection.wrappedValue.count)
                )
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textDefault)
            }
        }
        .frame(maxHeight: .infinity, alignment: .top)
    }

    private func compactGridTable(
        _ shownRows: [Row],
        columns shownColumns: [MonarchDataTableColumn<Row>]
    ) -> some View {
        MonarchDataGridTable(
            label: caption,
            rows: shownRows,
            columns: shownColumns.enumerated().map { index, column in
                MonarchDataGridColumn(
                    column.title,
                    id: column.id,
                    isNumeric: column.isNumeric,
                    isSortable: column.isSortable,
                    isHideable: column.isHideable,
                    isPinnable: false,
                    pinned: index == 0 ? .start : nil,
                    width: column.width,
                    value: column.value,
                    sortBy: column.sortBy,
                    tone: column.tone
                )
            },
            sort: sort,
            selection: selection,
            hiddenColumns: nil,
            columnOrder: columnOrder,
            pinning: nil,
            columnWidths: nil,
            density: .comfortable,
            isLoading: false,
            footer: footer,
            labels: MonarchDataGridLabels(
                selectAll: labels.selectAll,
                selectRow: labels.selectRow,
                selectedCount: labels.selectedCount,
                rowActions: labels.rowActions,
                actionsHeader: labels.actionsHeader
            ),
            rowLabel: rowLabel,
            isRowSelectable: isRowSelectable,
            rowActions: rowActions,
            onOpen: onOpen,
            fitsViewport: true
        )
    }

    @ViewBuilder private func bar(columns: [MonarchDataTableColumn<Row>], showsSort: Bool) -> some View {
        let sortable = columns.filter(\.isSortable)
        let showsSortMenu = showsSort && !sortable.isEmpty
        if selection != nil || showsSortMenu {
            HStack(spacing: Tokens.spacing) {
                if let selection {
                    selectAll(selection)
                }
                Spacer(minLength: 0)
                if showsSortMenu {
                    sortMenu(sortable)
                }
            }
            .padding(.vertical, Tokens.lineSpacing)
        }
    }

    private func selectAll(_ selection: Binding<Set<Row.ID>>) -> some View {
        let ids = Tokens.selectableIDs(rows, isRowSelectable: isRowSelectable)
        let isOn = Binding(
            get: { Tokens.isAllSelected(selection.wrappedValue, selectable: ids) },
            set: { selection.wrappedValue = Tokens.selectingAll(selection.wrappedValue, selectable: ids, isOn: $0) }
        )
        let count = selection.wrappedValue.count
        return HStack(spacing: Tokens.spacing) {
            Toggle(labels.selectAll ?? MonarchDataTableStrings.selectAll, isOn: isOn)
                .toggleStyle(.monarchCheckbox)
                .monarchTextStyle(.body)
                .disabled(ids.isEmpty)
            if count > 0 {
                Text(Tokens.selectedCountText(count, labels: labels))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: Tokens.mutedRole])
            }
        }
    }

    private func sortMenu(_ sortable: [MonarchDataTableColumn<Row>]) -> some View {
        let current = sort.wrappedValue
        let title = labels.sort ?? MonarchDataTableStrings.sort
        let sortedColumn = sortable.first { $0.id == current?.columnID }
        let state = sortedColumn.flatMap { column in
            Tokens.sortState(columnID: column.id, sort: current, labels: labels).map { "\(column.title), \($0)" }
        }
        return Menu {
            ForEach(sortable) { column in
                Button {
                    sort.wrappedValue = Tokens.toggledSort(sort.wrappedValue, columnID: column.id, columns: columns)
                } label: {
                    if let current, current.columnID == column.id {
                        Label(
                            column.title,
                            systemImage: Tokens.sortMark(current.order, labels: labels)
                        )
                    } else {
                        Text(column.title)
                    }
                }
            }
        } label: {
            Label(sortedColumn?.title ?? title, systemImage: Tokens.sortSymbol)
        }
        .menuStyle(.button)
        .buttonStyle(.monarch(.ghost, size: .sm))
        .accessibilityLabel(title)
        .accessibilityValue(state ?? "")
    }

    // MARK: List

    private func list(_ shownRows: [Row], columns shownColumns: [MonarchDataTableColumn<Row>]) -> some View {
        List {
            ForEach(shownRows) { row in
                MonarchDataTableListRow(
                    row: row,
                    name: Tokens.rowName(row, columns: shownColumns, rowLabel: rowLabel),
                    columns: shownColumns,
                    isSelected: selectionBinding(for: row),
                    actions: rowActions?(row) ?? [],
                    labels: labels,
                    onOpen: onOpen
                )
                // The rows sit on the screen's own background, never the
                // system's pure white or black list fill.
                .listRowBackground(Color.clear)
                // The rows start on the same left edge as the toolbar above.
                .listRowInsets(Tokens.listRowInsets)
                .listRowSeparatorTint(theme.colors[role: \.borderSubtle])
            }
            if let footer {
                footer.listRowSeparator(.hidden)
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .accessibilityLabel(caption)
    }

    private func selectionBinding(for row: Row) -> Binding<Bool>? {
        guard let selection, isRowSelectable?(row) ?? true else { return nil }
        return Binding(
            get: { selection.wrappedValue.contains(row.id) },
            set: { selection.wrappedValue = Tokens.selecting(selection.wrappedValue, id: row.id, isOn: $0) }
        )
    }

    // MARK: Table

    @available(iOS 17.4, macOS 14.4, *)
    private func table(_ shownRows: [Row], columns shownColumns: [MonarchDataTableColumn<Row>]) -> some View {
        let order = Binding<[Comparator]>(
            get: {
                guard let current = sort.wrappedValue,
                    let column = columns.first(where: { $0.id == current.columnID })
                else { return [] }
                return [Comparator(column, order: current.order)]
            },
            set: { requested in
                sort.wrappedValue = Tokens.sort(
                    fromHeader: requested.first?.sort,
                    current: sort.wrappedValue,
                    columns: columns
                )
            }
        )
        let firstID = shownColumns.first?.id
        return Table(shownRows, sortOrder: order) {
            if selection != nil {
                TableColumn("") { row in
                    if let isSelected = selectionBinding(for: row) {
                        checkbox(isSelected, row: row, columns: shownColumns)
                    }
                }
                .width(Tokens.selectionWidth)
            }
            TableColumnForEach(shownColumns) { column in
                TableColumn(column.title, sortUsing: Comparator(column, order: .forward)) { row in
                    cell(row, column: column, isFirst: column.id == firstID, columns: shownColumns)
                }
                .width(ideal: column.width)
            }
            if let rowActions {
                TableColumn(labels.actionsHeader ?? MonarchDataTableStrings.actionsHeader) { row in
                    let actions = rowActions(row)
                    if !actions.isEmpty {
                        MonarchRowActionsMenu(
                            actions: actions,
                            label: rowActionsLabel(row, columns: shownColumns)
                        )
                    }
                }
                .width(Tokens.actionsWidth)
            }
        }
        .accessibilityLabel(caption)
    }

    private func checkbox(_ isSelected: Binding<Bool>, row: Row, columns shownColumns: [MonarchDataTableColumn<Row>])
        -> some View
    {
        let name = Tokens.rowName(row, columns: shownColumns, rowLabel: rowLabel)
        let label = labels.selectRow?(name) ?? MonarchDataTableStrings.selectRow(name)
        return Toggle(isOn: isSelected) { Text(label) }
            .toggleStyle(.monarchCheckbox)
            .labelsHidden()
            .accessibilityLabel(label)
    }

    private func rowActionsLabel(_ row: Row, columns shownColumns: [MonarchDataTableColumn<Row>]) -> String {
        let name = Tokens.rowName(row, columns: shownColumns, rowLabel: rowLabel)
        return labels.rowActions?(name) ?? MonarchDataTableStrings.rowActions(name)
    }

    @ViewBuilder private func cell(
        _ row: Row,
        column: MonarchDataTableColumn<Row>,
        isFirst: Bool,
        columns shownColumns: [MonarchDataTableColumn<Row>]
    ) -> some View {
        let value = column.value(row)
        let text = Text(Tokens.cellText(value, labels: labels))
            .monarchTextStyle(isFirst ? .bodyStrong : .body)
            .monospacedDigit()
            .frame(maxWidth: .infinity, alignment: column.isNumeric ? .trailing : .leading)
        if !isFirst, !value.isEmpty, let tone = column.tone?(row) {
            // A status as a badge, as on the web.
            MonarchBadge(value, tone: tone, size: .sm)
                .frame(maxWidth: .infinity, alignment: column.isNumeric ? .trailing : .leading)
                .accessibilityLabel(Tokens.cellLabel(value, labels: labels))
        } else if isFirst, let onOpen {
            Button {
                onOpen(row)
            } label: {
                // The web draws the opening cell in the text colour, as a button; the button trait says it opens.
                text.foregroundStyle(theme.colors.textDefault)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Tokens.cellLabel(value, labels: labels))
            .accessibilityHint(labels.openHint ?? MonarchDataTableStrings.openHint)
            .contextMenu {
                if let actions = rowActions?(row), !actions.isEmpty {
                    MonarchRowActionButtons(actions: actions)
                }
            }
        } else {
            text
                .foregroundStyle(theme.colors[role: value.isEmpty ? Tokens.mutedRole : Tokens.textRole])
                .accessibilityLabel(Tokens.cellLabel(value, labels: labels))
        }
    }
}

#if DEBUG
    private struct MonarchDataTablePreviewRow: Identifiable {
        let id: Int
        let subject: String
        let status: String
    }

    #Preview("MonarchDataTable") {
        MonarchPreviewMatrix {
            MonarchDataTable(
                "Open tickets",
                rows: [
                    MonarchDataTablePreviewRow(id: 42, subject: "Printer offline", status: "Open"),
                    MonarchDataTablePreviewRow(id: 43, subject: "Refund request", status: ""),
                ],
                columns: [
                    MonarchDataTableColumn("Subject", value: { $0.subject }),
                    MonarchDataTableColumn("Status", value: { $0.status }),
                ],
                selection: .constant([42]),
                layout: .list
            )
            .frame(height: MonarchTokens.Space.space24 * 4)
        }
    }
#endif
