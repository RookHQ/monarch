import Foundation
import MonarchTokens
import SwiftUI

/// The shown columns of a grid, split by where they sit.
struct MonarchDataGridArrangement<Row> {
    /// Columns pinned to the leading edge.
    let start: [MonarchDataGridColumn<Row>]
    /// Columns that scroll sideways.
    let middle: [MonarchDataGridColumn<Row>]
    /// Columns pinned to the trailing edge.
    let end: [MonarchDataGridColumn<Row>]

    /// Every shown column in display order.
    var all: [MonarchDataGridColumn<Row>] { start + middle + end }
}

/// An item of a grid's column menu.
enum MonarchDataGridMenuItem: String, CaseIterable, Identifiable {
    case sortAscending, sortDescending, clearSort, pinStart, pinEnd, unpin, hide, widen, narrow

    /// The identity of the item.
    var id: String { rawValue }
}

/// What the app lets the column menu change: each is `true` when the app passed the binding.
struct MonarchDataGridMenuAbilities: Equatable {
    /// Sorting.
    var sort: Bool
    /// Pinning and unpinning.
    var pin: Bool
    /// Hiding.
    var hide: Bool
    /// Widening and narrowing.
    var resize: Bool
}

/// Component tokens and pure logic for ``MonarchDataGrid``.
enum MonarchDataGridTokens {
    /// The width of a column without its own width: 160 points.
    static let columnWidth: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space16
    /// The narrowest a column can be made from its menu.
    static let minimumColumnWidth: CGFloat = MonarchTokens.Space.space24
    /// The number of minimum widths in the widest column.
    static let maximumWidthSteps: CGFloat = 5
    /// The widest a column can be made from its menu.
    static let maximumColumnWidth: CGFloat = MonarchTokens.Space.space24 * maximumWidthSteps
    /// How much Widen and Narrow change a column, as the web's Alt+Arrow keys do.
    static let resizeStep: CGFloat = MonarchTokens.Space.space4
    /// The width of the checkbox column and the actions column: one touch target.
    static let utilityWidth: CGFloat = MonarchAccessibility.minimumHitTarget
    /// The space inside a cell at the leading and trailing edges.
    static let cellPaddingHorizontal: CGFloat = MonarchTokens.Space.space3
    /// The space inside a cell above and below, comfortable density.
    static let cellPaddingComfortable: CGFloat = MonarchTokens.Space.space2
    /// The space inside a cell above and below, compact density.
    static let cellPaddingCompact: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between a header's title and its sort mark.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space1
    /// The divider under each row and the line at the edge of the pinned columns.
    static let dividerWidth: CGFloat = MonarchTokens.Space.px
    /// At most this share of the grid's width is pinned; wider pins scroll with the rest.
    static let maximumPinnedShare: CGFloat = 0.5
    /// The number of placeholder rows while the first rows load.
    static let loadingRows = 4
    /// The name of the scroll view's coordinate space, which pinned columns measure against.
    static let coordinateSpace = "monarch.dataGrid.scroll"
    /// The mark of a header sorted ascending.
    static let ascendingSymbol = "arrow.up"
    /// The mark of a header sorted descending.
    static let descendingSymbol = "arrow.down"

    /// The colour of a row: the accent's subtle fill while selected.
    static func rowFill(isSelected: Bool) -> MonarchColorRole {
        isSelected ? \.accentSubtle : \.bgSurface
    }
    /// The fill of the header row.
    static var headerFill: MonarchColorRole { \.bgSubtle }
    /// The text of headers.
    static var headerText: MonarchColorRole { \.textMuted }
    /// The divider between rows.
    static var divider: MonarchColorRole { \.borderSubtle }
    /// The line at the inner edge of the pinned columns.
    static var pinnedEdge: MonarchColorRole { \.borderDefault }
    /// The text of cells.
    static var text: MonarchColorRole { \.textDefault }
    /// The text of empty cells.
    static var muted: MonarchColorRole { \.textMuted }
    /// The text of the first cell when rows open.
    static var link: MonarchColorRole { \.textLink }

    /// The padding above and below a cell at a density.
    static func cellPadding(_ density: MonarchDataGridDensity) -> CGFloat {
        switch density {
        case .comfortable: cellPaddingComfortable
        case .compact: cellPaddingCompact
        }
    }

    // MARK: Pins and widths

    /// The pin of a column: from `pinning` when the app passes it, otherwise the column's own.
    static func pin<Row>(of column: MonarchDataGridColumn<Row>, pinning: [String: MonarchDataGridPin]?)
        -> MonarchDataGridPin?
    {
        if let pinning { return pinning[column.id] }
        return column.pinned
    }

    /// Every column's pin as a map, the form the pinning binding holds.
    static func pinning<Row>(_ columns: [MonarchDataGridColumn<Row>], pinning: [String: MonarchDataGridPin]?)
        -> [String: MonarchDataGridPin]
    {
        if let pinning { return pinning }
        var result: [String: MonarchDataGridPin] = [:]
        for column in columns {
            if let pin = column.pinned { result[column.id] = pin }
        }
        return result
    }

    /// The pinning after a column is pinned, or unpinned with `nil`.
    static func pinned<Row>(
        _ columns: [MonarchDataGridColumn<Row>],
        pinning: [String: MonarchDataGridPin]?,
        columnID: String,
        pin: MonarchDataGridPin?
    ) -> [String: MonarchDataGridPin] {
        var result = Self.pinning(columns, pinning: pinning)
        result[columnID] = pin
        return result
    }

    /// The width of a column: the app's, the column's own, or the default.
    static func width<Row>(of column: MonarchDataGridColumn<Row>, widths: [String: CGFloat]) -> CGFloat {
        widths[column.id] ?? column.width ?? columnWidth
    }

    /// The widths after a column grows by `delta` (negative to shrink), kept
    /// between the narrowest and widest a menu can make it.
    static func resized<Row>(_ column: MonarchDataGridColumn<Row>, widths: [String: CGFloat], delta: CGFloat)
        -> [String: CGFloat]
    {
        var result = widths
        result[column.id] = min(max(width(of: column, widths: widths) + delta, minimumColumnWidth), maximumColumnWidth)
        return result
    }

    /// The shown columns in display order, split into start-pinned, scrolling
    /// and end-pinned ones. When the pinned columns together would take more
    /// than half of `viewport`, every column scrolls.
    static func arranged<Row>(
        _ columns: [MonarchDataGridColumn<Row>],
        hidden: Set<String>,
        order: [String],
        pinning: [String: MonarchDataGridPin]?,
        widths: [String: CGFloat],
        viewport: CGFloat
    ) -> MonarchDataGridArrangement<Row> {
        let byID = Dictionary(columns.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let shown = MonarchDataTableTokens.visibleColumns(columns.map(\.tableColumn), hidden: hidden, order: order)
            .compactMap { byID[$0.id] }
        let start = shown.filter { pin(of: $0, pinning: pinning) == .start }
        let end = shown.filter { pin(of: $0, pinning: pinning) == .end }
        let pinnedWidth = (start + end).reduce(0) { $0 + width(of: $1, widths: widths) }
        if pinnedWidth > viewport * maximumPinnedShare {
            return MonarchDataGridArrangement(start: [], middle: shown, end: [])
        }
        return MonarchDataGridArrangement(
            start: start,
            middle: shown.filter { pin(of: $0, pinning: pinning) == nil },
            end: end
        )
    }

    /// How far a pinned group moves so that it stays inside the visible
    /// width: a group scrolled past an edge is held at that edge.
    ///
    /// - Parameters:
    ///   - minX: The group's leading position in the scroll view.
    ///   - maxX: The group's trailing position in the scroll view.
    ///   - visibleWidth: The width of the scroll view.
    /// - Returns: The horizontal offset to apply to the group.
    static func stickyOffset(minX: CGFloat, maxX: CGFloat, visibleWidth: CGFloat) -> CGFloat {
        if minX < 0 { return -minX }
        if maxX > visibleWidth { return max(visibleWidth - maxX, -minX) }
        return 0
    }

    // MARK: Sorting

    /// The rows in the sort's order, stable; unchanged without a sort or for
    /// an unknown or unsortable column.
    static func sorted<Row>(_ rows: [Row], by sort: MonarchDataTableSort?, columns: [MonarchDataGridColumn<Row>])
        -> [Row]
    {
        MonarchDataTableTokens.sorted(rows, by: sort, columns: columns.map(\.tableColumn))
    }

    /// The sort order of a column, or `nil` when the rows are not sorted by it.
    static func order(of columnID: String, sort: MonarchDataTableSort?) -> SortOrder? {
        guard let sort, sort.columnID == columnID else { return nil }
        return sort.order
    }

    // MARK: Column menu

    /// The items of a column's menu, given its sort, pin and width and what the app allows.
    static func menuItems<Row>(
        _ column: MonarchDataGridColumn<Row>,
        order: SortOrder?,
        pin: MonarchDataGridPin?,
        width: CGFloat,
        abilities: MonarchDataGridMenuAbilities
    ) -> [MonarchDataGridMenuItem] {
        var items: [MonarchDataGridMenuItem] = []
        if abilities.sort, column.isSortable {
            if order != .forward { items.append(.sortAscending) }
            if order != .reverse { items.append(.sortDescending) }
            if order != nil { items.append(.clearSort) }
        }
        if abilities.pin, column.isPinnable {
            if pin != .start { items.append(.pinStart) }
            if pin != .end { items.append(.pinEnd) }
            if pin != nil { items.append(.unpin) }
        }
        if abilities.hide, column.isHideable { items.append(.hide) }
        if abilities.resize {
            if width < maximumColumnWidth { items.append(.widen) }
            if width > minimumColumnWidth { items.append(.narrow) }
        }
        return items
    }

    /// The sort after a sort item, or the current sort for other items.
    static func sort(after item: MonarchDataGridMenuItem, columnID: String, current: MonarchDataTableSort?)
        -> MonarchDataTableSort?
    {
        switch item {
        case .sortAscending: MonarchDataTableSort(columnID, order: .forward)
        case .sortDescending: MonarchDataTableSort(columnID, order: .reverse)
        case .clearSort: nil
        default: current
        }
    }

    /// The pin a pin item asks for: `.some(nil)` unpins, `nil` is not a pin item.
    static func pin(after item: MonarchDataGridMenuItem) -> MonarchDataGridPin?? {
        switch item {
        case .pinStart: .some(.start)
        case .pinEnd: .some(.end)
        case .unpin: .some(nil)
        default: nil
        }
    }

    /// The width change of a resize item, or `nil` for other items.
    static func resizeDelta(after item: MonarchDataGridMenuItem) -> CGFloat? {
        switch item {
        case .widen: resizeStep
        case .narrow: -resizeStep
        default: nil
        }
    }

    /// The title of a menu item.
    static func title(_ item: MonarchDataGridMenuItem, labels: MonarchDataGridLabels) -> String {
        switch item {
        case .sortAscending: labels.sortAscending ?? MonarchDataGridStrings.sortAscending
        case .sortDescending: labels.sortDescending ?? MonarchDataGridStrings.sortDescending
        case .clearSort: labels.clearSort ?? MonarchDataGridStrings.clearSort
        case .pinStart: labels.pinStart ?? MonarchDataGridStrings.pinStart
        case .pinEnd: labels.pinEnd ?? MonarchDataGridStrings.pinEnd
        case .unpin: labels.unpin ?? MonarchDataGridStrings.unpin
        case .hide: labels.hideColumn ?? MonarchDataGridStrings.hideColumn
        case .widen: labels.widen ?? MonarchDataGridStrings.widen
        case .narrow: labels.narrow ?? MonarchDataGridStrings.narrow
        }
    }

    /// The SF Symbol of a menu item.
    static func symbol(_ item: MonarchDataGridMenuItem) -> String {
        switch item {
        case .sortAscending: ascendingSymbol
        case .sortDescending: descendingSymbol
        case .clearSort: "xmark"
        case .pinStart, .pinEnd: "pin"
        case .unpin: "pin.slash"
        case .hide: "eye.slash"
        case .widen: "arrow.left.and.right"
        case .narrow: "arrow.right.and.line.vertical.and.arrow.left"
        }
    }

    // MARK: Text

    /// The spoken state of a header: its sort when it can sort or is sorted, then its pin.
    static func headerState(
        isSortable: Bool,
        order: SortOrder?,
        pin: MonarchDataGridPin?,
        labels: MonarchDataGridLabels
    ) -> String {
        var parts: [String] = []
        switch order {
        case .forward: parts.append(labels.sortedAscending ?? MonarchDataGridStrings.sortedAscending)
        case .reverse: parts.append(labels.sortedDescending ?? MonarchDataGridStrings.sortedDescending)
        case nil:
            if isSortable { parts.append(labels.notSorted ?? MonarchDataGridStrings.notSorted) }
        }
        switch pin {
        case .start: parts.append(labels.pinnedStart ?? MonarchDataGridStrings.pinnedStart)
        case .end: parts.append(labels.pinnedEnd ?? MonarchDataGridStrings.pinnedEnd)
        case nil: break
        }
        return parts.joined(separator: ", ")
    }

    /// What VoiceOver reads for a read-only cell: "Column: value", or "Column: No value".
    static func cellLabel(column: String, value: String, labels: MonarchDataGridLabels) -> String {
        let shown = value.isEmpty ? labels.noValue ?? MonarchDataGridStrings.noValue : value
        return labels.cell?(column, shown) ?? MonarchDataGridStrings.cell(column, shown)
    }

    /// The name of an editable cell: "Column for row".
    static func editLabel(column: String, row: String, labels: MonarchDataGridLabels) -> String {
        labels.editLabel?(column, row) ?? MonarchDataGridStrings.editLabel(column, row)
    }

    /// A row's checkbox: "Select row".
    static func selectLabel(row: String, labels: MonarchDataGridLabels) -> String {
        labels.selectRow?(row) ?? MonarchDataGridStrings.selectRow(row)
    }

    /// A row's actions menu: "Actions for row".
    static func actionsLabel(row: String, labels: MonarchDataGridLabels) -> String {
        labels.rowActions?(row) ?? MonarchDataGridStrings.rowActions(row)
    }

    /// The visible text of a read-only cell: the value, or a dash.
    static func cellText(_ value: String) -> String {
        MonarchDataTableTokens.cellText(value)
    }

    /// Whether any column edits a row, so the record list opens the record editor.
    static func editsRow<Row>(_ row: Row, columns: [MonarchDataGridColumn<Row>]) -> Bool {
        columns.contains { $0.edit?.canEdit(row) ?? false }
    }

    /// The labels the record list passes to ``MonarchDataTable``.
    static func tableLabels(_ labels: MonarchDataGridLabels, editsRecords: Bool) -> MonarchDataTableLabels {
        MonarchDataTableLabels(
            selectAll: labels.selectAll ?? MonarchDataGridStrings.selectAll,
            selectRow: labels.selectRow ?? { MonarchDataGridStrings.selectRow($0) },
            selectedCount: labels.selectedCount ?? { MonarchDataGridStrings.selectedCount($0) },
            rowActions: labels.rowActions ?? { MonarchDataGridStrings.rowActions($0) },
            actionsHeader: labels.actionsHeader ?? MonarchDataGridStrings.actionsHeader,
            openHint: editsRecords ? labels.editRecord ?? MonarchDataGridStrings.editRecord : nil,
            sort: labels.sort,
            sortedAscending: labels.sortedAscending,
            sortedDescending: labels.sortedDescending,
            noValue: labels.noValue ?? MonarchDataGridStrings.noValue,
            loading: labels.loading ?? MonarchDataGridStrings.loading,
            emptyTitle: labels.emptyTitle ?? MonarchDataGridStrings.emptyTitle,
            emptyMessage: labels.emptyMessage ?? MonarchDataGridStrings.emptyMessage
        )
    }

    /// The columns the record list shows: sortable only when the app passed a sort.
    static func listColumns<Row>(_ columns: [MonarchDataGridColumn<Row>], sorts: Bool) -> [MonarchDataTableColumn<Row>]
    {
        columns.map { column in
            var table = column.tableColumn
            table.isSortable = column.isSortable && sorts
            return table
        }
    }
}
