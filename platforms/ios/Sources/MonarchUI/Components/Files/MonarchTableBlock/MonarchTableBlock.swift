import SwiftUI

/// A small table of text inside a document.
///
/// A table that is wider than its column scrolls sideways instead of
/// widening the page. The read-only form shows the cells; the editable form
/// makes every cell a text field and adds a menu to add and delete rows and
/// columns, switch the header row and delete the table.
///
/// ```swift
/// MonarchTableBlock("Plans", data: MonarchTableBlockData(rows: [["Plan", "Price"], ["Team", "12"]]))
/// MonarchTableBlock("Plans", data: $table) { removeBlock() }
/// ```
///
/// Accessibility: the table is a container named by `label`, else the
/// caption, else "Table". A read-only cell is read as its text (or "Empty")
/// with its position, "Row 2, column 1", as the value; header cells have the
/// header trait and stronger text. An editable cell is a text field named by
/// its position. The controls are a standard menu; every editable cell and
/// the menu have a 44-point touch target. Text scales with Dynamic Type and
/// wraps inside its column.
public struct MonarchTableBlock: View {
    private let caption: String?
    private let fixedData: MonarchTableBlockData
    private let binding: Binding<MonarchTableBlockData>?
    private let label: String?
    private let labels: MonarchTableBlockLabels
    private let onDelete: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @FocusState private var focusedCell: MonarchTableBlockCell?
    @State private var activeCell: MonarchTableBlockCell?
    @ScaledMetric(relativeTo: .body) private var minColumnWidth = MonarchTableBlockTokens.minColumnWidth
    @ScaledMetric(relativeTo: .body) private var maxColumnWidth = MonarchTableBlockTokens.maxColumnWidth
    @ScaledMetric(relativeTo: .body) private var editableColumnWidth = MonarchTableBlockTokens.editableColumnWidth

    /// A read-only table.
    ///
    /// - Parameters:
    ///   - caption: Text above the table, already localised. Also the spoken name of the table.
    ///   - data: The cells.
    ///   - label: The spoken name of the table when the caption is not the right one.
    public init(_ caption: String? = nil, data: MonarchTableBlockData, label: String? = nil) {
        self.caption = caption
        self.fixedData = data
        self.binding = nil
        self.label = label
        self.labels = MonarchTableBlockLabels()
        self.onDelete = nil
    }

    /// An editable table.
    ///
    /// - Parameters:
    ///   - caption: Text above the table, already localised. Also the spoken name of the table.
    ///   - data: The cells. Edits, added and deleted rows and columns and the header switch are written here.
    ///   - label: The spoken name of the table when the caption is not the right one.
    ///   - labels: Replacements for the built-in strings.
    ///   - onDelete: Adds "Delete table" to the menu and runs when it is chosen. The app removes the table.
    public init(
        _ caption: String? = nil,
        data: Binding<MonarchTableBlockData>,
        label: String? = nil,
        labels: MonarchTableBlockLabels = MonarchTableBlockLabels(),
        onDelete: (@MainActor () -> Void)? = nil
    ) {
        self.caption = caption
        self.fixedData = data.wrappedValue
        self.binding = data
        self.label = label
        self.labels = labels
        self.onDelete = onDelete
    }

    private var data: MonarchTableBlockData { binding?.wrappedValue ?? fixedData }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchTableBlockTokens.stackSpacing) {
            if let binding {
                controls(binding)
            }
            if let caption, !caption.isEmpty {
                Text(caption)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchTableBlockTokens.caption])
                    .accessibilityHidden(MonarchTableBlockTokens.captionIsSpokenName(label: label))
            }
            ScrollView(.horizontal) {
                grid
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel(MonarchTableBlockTokens.regionLabel(label: label, caption: caption))
        }
        .onChange(of: focusedCell) { _, cell in
            if let cell { activeCell = cell }
        }
    }

    private var grid: some View {
        let table = data
        return Grid(alignment: .topLeading, horizontalSpacing: 0, verticalSpacing: 0) {
            ForEach(0..<table.rowCount, id: \.self) { row in
                GridRow {
                    ForEach(0..<table.columnCount, id: \.self) { column in
                        cell(MonarchTableBlockCell(row: row, column: column), in: table)
                    }
                }
            }
        }
        .fixedSize(horizontal: false, vertical: true)
        // The border belongs to the table, not to the scrolling area, so a
        // table narrower than the page is not drawn wider than it is.
        .clipShape(MonarchShapes.control)
        .overlay(
            MonarchShapes.control.strokeBorder(
                theme.colors[role: MonarchTableBlockTokens.line],
                lineWidth: MonarchShapes.borderWidth
            )
            .allowsHitTesting(false)
        )
    }

    private func cell(_ cell: MonarchTableBlockCell, in table: MonarchTableBlockData) -> some View {
        let isHeader = table.isHeader(row: cell.row)
        let line = theme.colors[role: MonarchTableBlockTokens.line]
        return content(of: cell, in: table)
            .monarchTextStyle(MonarchTableBlockTokens.textStyle(isHeader: isHeader))
            .foregroundStyle(theme.colors[role: MonarchTableBlockTokens.foreground(isEnabled: isEnabled)])
            .multilineTextAlignment(.leading)
            .padding(.horizontal, MonarchTableBlockTokens.cellHorizontalPadding)
            .padding(.vertical, MonarchTableBlockTokens.cellVerticalPadding)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .background(theme.colors[role: MonarchTableBlockTokens.background(isHeader: isHeader)])
            .overlay(alignment: .bottom) {
                if cell.row < table.rowCount - 1 {
                    Rectangle().fill(line).frame(height: MonarchShapes.borderWidth)
                }
            }
            .overlay(alignment: .trailing) {
                if cell.column < table.columnCount - 1 {
                    Rectangle().fill(line).frame(width: MonarchShapes.borderWidth)
                }
            }
    }

    @ViewBuilder
    private func content(of cell: MonarchTableBlockCell, in table: MonarchTableBlockData) -> some View {
        let position = MonarchTableBlockTokens.position(of: cell, labels: labels)
        if let binding {
            TextField(
                position,
                text: Binding(
                    get: { binding.wrappedValue[cell.row, cell.column] },
                    set: { binding.wrappedValue[cell.row, cell.column] = $0 }
                ),
                axis: .vertical
            )
            .textFieldStyle(.plain)
            .focused($focusedCell, equals: cell)
            .accessibilityLabel(position)
            .accessibilityAddTraits(table.isHeader(row: cell.row) ? .isHeader : [])
            .frame(
                width: editableColumnWidth,
                alignment: .leading
            )
            .frame(minHeight: MonarchTableBlockTokens.editableCellMinHeight, alignment: .leading)
            .contentShape(Rectangle())
            .onTapGesture { focusedCell = cell }
        } else {
            Text(table[cell.row, cell.column])
                .frame(minWidth: minColumnWidth, maxWidth: maxColumnWidth, alignment: .leading)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(
                    MonarchTableBlockTokens.cellLabel(text: table[cell.row, cell.column], labels: labels)
                )
                .accessibilityValue(position)
                .accessibilityAddTraits(table.isHeader(row: cell.row) ? .isHeader : [])
        }
    }

    private func controls(_ binding: Binding<MonarchTableBlockData>) -> some View {
        let table = binding.wrappedValue
        let target = MonarchTableBlockTokens.target(active: activeCell, in: table)
        return Menu {
            Button(labels.addRow ?? MonarchTableBlockStrings.addRow) {
                if binding.wrappedValue.addRow(after: target.row) {
                    move(to: MonarchTableBlockCell(row: target.row + 1, column: target.column))
                }
            }
            .disabled(!table.canAddRow)
            Button(labels.addColumn ?? MonarchTableBlockStrings.addColumn) {
                if binding.wrappedValue.addColumn(after: target.column) {
                    move(to: MonarchTableBlockCell(row: target.row, column: target.column + 1))
                }
            }
            .disabled(!table.canAddColumn)
            Button(labels.deleteRow ?? MonarchTableBlockStrings.deleteRow) {
                if binding.wrappedValue.deleteRow(at: target.row) {
                    move(to: MonarchTableBlockTokens.target(active: target, in: binding.wrappedValue))
                }
            }
            .disabled(!table.canDeleteRow)
            Button(labels.deleteColumn ?? MonarchTableBlockStrings.deleteColumn) {
                if binding.wrappedValue.deleteColumn(at: target.column) {
                    move(to: MonarchTableBlockTokens.target(active: target, in: binding.wrappedValue))
                }
            }
            .disabled(!table.canDeleteColumn)
            Toggle(labels.headerRow ?? MonarchTableBlockStrings.headerRow, isOn: binding.hasHeaderRow)
            if let onDelete {
                Divider()
                Button(labels.deleteTable ?? MonarchTableBlockStrings.deleteTable, role: .destructive, action: onDelete)
            }
        } label: {
            Label(
                labels.controls ?? MonarchTableBlockStrings.controls,
                systemImage: MonarchTableBlockTokens.controlsSymbol
            )
        }
        .menuStyle(.button)
        .buttonStyle(.monarch(.secondary, size: .sm))
    }

    /// Makes a cell the target of the controls and gives it focus.
    private func move(to cell: MonarchTableBlockCell) {
        activeCell = cell
        focusedCell = cell
    }
}

#if DEBUG
    #Preview("MonarchTableBlock") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchTableBlock(
                    "Plans",
                    data: MonarchTableBlockData(rows: [
                        ["Plan", "Seats", "Price"], ["Team", "10", "12"], ["Business", "Unlimited", "30"],
                    ])
                )
                MonarchTableBlock(data: .constant(MonarchTableBlockData(rowCount: 2, columnCount: 2))) {}
                MonarchTableBlock(data: .constant(MonarchTableBlockData(rows: [["Read only"]]))).disabled(true)
            }
        }
    }
#endif
