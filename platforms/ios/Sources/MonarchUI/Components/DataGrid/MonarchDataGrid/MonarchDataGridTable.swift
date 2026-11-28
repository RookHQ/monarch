import SwiftUI

/// Holds a pinned group of cells at the edge of the visible width while the
/// grid scrolls sideways. It measures the group against the scroll view's
/// coordinate space in a visual effect, so scrolling redraws no view body.
struct MonarchDataGridSticky: ViewModifier {
    /// The width of the scroll view.
    let visibleWidth: CGFloat

    func body(content: Content) -> some View {
        let width = visibleWidth
        return
            content
            .zIndex(1)
            .visualEffect { effect, proxy in
                let frame = proxy.frame(in: .named(MonarchDataGridTokens.coordinateSpace))
                return effect.offset(
                    x: MonarchDataGridTokens.stickyOffset(minX: frame.minX, maxX: frame.maxX, visibleWidth: width)
                )
            }
    }
}

/// The grid layout of ``MonarchDataGrid``: a header row pinned to the top,
/// then a row per record, in one scroll view that scrolls both ways, with
/// pinned columns held at their edges.
struct MonarchDataGridTable<Row: Identifiable>: View {
    let label: String
    let rows: [Row]
    let columns: [MonarchDataGridColumn<Row>]
    let sort: Binding<MonarchDataTableSort?>?
    let selection: Binding<Set<Row.ID>>?
    let hiddenColumns: Binding<Set<String>>?
    let columnOrder: [String]
    let pinning: Binding<[String: MonarchDataGridPin]?>?
    let columnWidths: Binding<[String: CGFloat]>?
    let density: MonarchDataGridDensity
    let isLoading: Bool
    let footer: MonarchLoadMore?
    let labels: MonarchDataGridLabels
    let rowLabel: ((Row) -> String)?
    let isRowSelectable: ((Row) -> Bool)?
    let rowActions: ((Row) -> [MonarchDataTableRowAction])?
    let onOpen: (@MainActor (Row) -> Void)?
    /// Sizes the columns to the width of the grid so that all of them show, with smaller text when they must (a
    /// phone), instead of scrolling sideways at the columns' own widths.
    var fitsViewport = false

    @Environment(\.monarchTheme) private var theme

    private typealias Tokens = MonarchDataGridTokens

    private var widths: [String: CGFloat] { columnWidths?.wrappedValue ?? [:] }
    private var currentPinning: [String: MonarchDataGridPin]? { pinning?.wrappedValue }

    private var abilities: MonarchDataGridMenuAbilities {
        MonarchDataGridMenuAbilities(
            sort: sort != nil,
            pin: pinning != nil,
            hide: hiddenColumns != nil,
            resize: columnWidths != nil
        )
    }

    /// The widths and sizes the grid draws with: the columns' own, or fitted to the viewport.
    struct Fit {
        var widths: [String: CGFloat]
        var utility: CGFloat = Tokens.utilityWidth
        var padding: CGFloat = Tokens.cellPaddingHorizontal
        var isSmall = false
    }

    private func fitted(_ arrangement: MonarchDataGridArrangement<Row>, viewport: CGFloat) -> Fit {
        guard fitsViewport else { return Fit(widths: widths) }
        let shown = arrangement.all
        let utilities = CGFloat((selection != nil ? 1 : 0) + (rowActions != nil ? 1 : 0))
        func natural(_ column: MonarchDataGridColumn<Row>, charWidth: CGFloat, padding: CGFloat, small: Bool) -> CGFloat
        {
            if let own = self.widths[column.id] { return own }
            let words = rows.flatMap { column.value($0).split(separator: " ") }.map(\.count)
            let longest = max(words.max() ?? 3, column.title.count)
            let mark: CGFloat = column.isSortable && sort != nil && !small ? 14 : 0
            let badge: CGFloat = column.tone != nil ? 14 : 0
            return min(max(CGFloat(longest) * charWidth + 2 * padding + max(mark, badge), 40), 176)
        }
        let modes = [
            (charWidth: CGFloat(8.2), padding: CGFloat(12), utility: CGFloat(40), small: false),
            (charWidth: CGFloat(7.0), padding: CGFloat(5), utility: CGFloat(28), small: true),
        ]
        for mode in modes {
            let base = shown.map { natural($0, charWidth: mode.charWidth, padding: mode.padding, small: mode.small) }
            let total = base.reduce(0, +)
            let room = viewport - mode.utility * utilities
            guard total <= room, total > 0 else { continue }
            let scale = room / total
            var result: [String: CGFloat] = [:]
            for (column, width) in zip(shown, base) { result[column.id] = (width * scale).rounded(.down) }
            return Fit(widths: result, utility: mode.utility, padding: mode.padding, isSmall: mode.small)
        }
        // Even the small sizes do not fit: scroll sideways at the roomy widths, the first column pinned.
        var scrolled: [String: CGFloat] = [:]
        for column in shown { scrolled[column.id] = natural(column, charWidth: 8.2, padding: 12, small: false) }
        return Fit(widths: scrolled)
    }

    var body: some View {
        GeometryReader { proxy in
            let viewport = proxy.size.width
            let arrangement = Tokens.arranged(
                columns,
                hidden: hiddenColumns?.wrappedValue ?? [],
                order: columnOrder,
                pinning: currentPinning,
                widths: widths,
                viewport: viewport
            )
            let fit = fitted(arrangement, viewport: viewport)
            VStack(spacing: 0) {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(.monarchLinear)
                        .accessibilityLabel(labels.loading ?? MonarchDataGridStrings.loading)
                }
                ScrollView([.horizontal, .vertical]) {
                    LazyVStack(alignment: .leading, spacing: 0, pinnedViews: [.sectionHeaders]) {
                        Section {
                            ForEach(rows) { row in
                                rowView(row, arrangement: arrangement, viewport: viewport, fit: fit)
                            }
                            if let footer {
                                footer
                                    .frame(width: viewport)
                                    .modifier(MonarchDataGridSticky(visibleWidth: viewport))
                            }
                        } header: {
                            header(arrangement, viewport: viewport, fit: fit)
                        }
                    }
                    .frame(minWidth: viewport, alignment: .leading)
                }
                .coordinateSpace(.named(Tokens.coordinateSpace))
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(label)
    }

    // MARK: Header

    private func header(_ arrangement: MonarchDataGridArrangement<Row>, viewport: CGFloat, fit: Fit) -> some View {
        HStack(spacing: 0) {
            group(edge: .start, hasColumns: !arrangement.start.isEmpty, viewport: viewport) {
                if let selection {
                    selectAll(selection)
                        .frame(width: fit.utility)
                }
                ForEach(arrangement.start) { headerCell($0, fit: fit) }
            }
            ForEach(arrangement.middle) { headerCell($0, fit: fit) }
            group(edge: .end, hasColumns: !arrangement.end.isEmpty, viewport: viewport) {
                ForEach(arrangement.end) { headerCell($0, fit: fit) }
                if rowActions != nil {
                    Color.clear
                        .frame(width: fit.utility)
                        .accessibilityHidden(true)
                }
            }
        }
        .fixedSize(horizontal: false, vertical: true)
        .background(theme.colors[role: Tokens.headerFill])
        .overlay(alignment: .bottom) { line(Tokens.divider) }
    }

    @ViewBuilder private func headerCell(_ column: MonarchDataGridColumn<Row>, fit: Fit) -> some View {
        let width = Tokens.width(of: column, widths: fit.widths)
        let order = Tokens.order(of: column.id, sort: sort?.wrappedValue)
        let pin = Tokens.pin(of: column, pinning: currentPinning)
        let items = Tokens.menuItems(column, order: order, pin: pin, width: width, abilities: abilities)
        let state = Tokens.headerState(
            isSortable: column.isSortable && sort != nil,
            order: order,
            pin: pin,
            labels: labels
        )
        let title = HStack(spacing: Tokens.headerSpacing) {
            Text(column.title)
                .monarchTextStyle(fit.isSmall ? .caption : .label)
                .fontWeight(fit.isSmall ? .medium : nil)
                .multilineTextAlignment(column.isNumeric ? .trailing : .leading)
                .fixedSize(horizontal: false, vertical: true)
            if let order {
                MonarchIcon(
                    systemName: order == .forward ? Tokens.ascendingSymbol : Tokens.descendingSymbol,
                    size: .sm
                )
            } else if column.isSortable && sort != nil && !fit.isSmall {
                // A sortable column shows a quiet chevron before it is sorted, as on the web.
                Image(systemName: "chevron.down")
                    .font(.system(size: 9, weight: .semibold))
                    .foregroundStyle(theme.colors.textSubtle)
                    .accessibilityHidden(true)
            }
        }
        .foregroundStyle(theme.colors[role: Tokens.headerText])
        .padding(.horizontal, fit.padding)
        .padding(.vertical, Tokens.cellPadding(density))
        .frame(width: width, alignment: column.isNumeric ? .trailing : .leading)
        .frame(minHeight: MonarchAccessibility.minimumHitTarget, maxHeight: .infinity)
        if items.isEmpty {
            title
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(column.title)
                .accessibilityValue(state)
                .accessibilityAddTraits(.isHeader)
        } else {
            Menu {
                ForEach(items) { item in
                    Button {
                        apply(item, to: column)
                    } label: {
                        Label(Tokens.title(item, labels: labels), systemImage: Tokens.symbol(item))
                    }
                }
            } label: {
                title.contentShape(Rectangle())
            }
            .menuStyle(.button)
            .buttonStyle(.plain)
            .accessibilityLabel(column.title)
            .accessibilityValue(state)
            .accessibilityHint(labels.columnOptions ?? MonarchDataGridStrings.columnOptions)
            .accessibilityAddTraits(.isHeader)
        }
    }

    private func apply(_ item: MonarchDataGridMenuItem, to column: MonarchDataGridColumn<Row>) {
        switch item {
        case .sortAscending, .sortDescending, .clearSort:
            sort?.wrappedValue = Tokens.sort(after: item, columnID: column.id, current: sort?.wrappedValue)
        case .pinStart, .pinEnd, .unpin:
            if let requested = Tokens.pin(after: item) {
                pinning?.wrappedValue = Tokens.pinned(
                    columns,
                    pinning: currentPinning,
                    columnID: column.id,
                    pin: requested
                )
            }
        case .hide:
            hiddenColumns?.wrappedValue.insert(column.id)
        case .widen, .narrow:
            if let delta = Tokens.resizeDelta(after: item) {
                columnWidths?.wrappedValue = Tokens.resized(column, widths: widths, delta: delta)
            }
        }
    }

    private func selectAll(_ selection: Binding<Set<Row.ID>>) -> some View {
        let ids = MonarchDataTableTokens.selectableIDs(rows, isRowSelectable: isRowSelectable)
        let sources = ids.map { id in
            Binding(
                get: { selection.wrappedValue.contains(id) },
                set: {
                    selection.wrappedValue = MonarchDataTableTokens.selecting(selection.wrappedValue, id: id, isOn: $0)
                }
            )
        }
        let title = labels.selectAll ?? MonarchDataGridStrings.selectAll
        return Toggle(sources: sources, isOn: \.self) { Text(title) }
            .toggleStyle(.monarchCheckbox)
            .labelsHidden()
            .disabled(ids.isEmpty)
            .accessibilityLabel(title)
    }

    // MARK: Rows

    private func rowView(_ row: Row, arrangement: MonarchDataGridArrangement<Row>, viewport: CGFloat, fit: Fit)
        -> some View
    {
        let name = MonarchDataTableTokens.rowName(row, columns: arrangement.all.map(\.tableColumn), rowLabel: rowLabel)
        let isSelected = selection?.wrappedValue.contains(row.id) ?? false
        let actions = rowActions?(row) ?? []
        let firstID = arrangement.all.first?.id
        return HStack(spacing: 0) {
            group(edge: .start, hasColumns: !arrangement.start.isEmpty, viewport: viewport, isSelected: isSelected) {
                if let selection {
                    checkbox(row, name: name, selection: selection)
                        .frame(width: fit.utility)
                }
                ForEach(arrangement.start) { column in
                    cell(row, column: column, name: name, isFirst: column.id == firstID, actions: actions, fit: fit)
                }
            }
            ForEach(arrangement.middle) { column in
                cell(row, column: column, name: name, isFirst: column.id == firstID, actions: actions, fit: fit)
            }
            group(edge: .end, hasColumns: !arrangement.end.isEmpty, viewport: viewport, isSelected: isSelected) {
                ForEach(arrangement.end) { column in
                    cell(row, column: column, name: name, isFirst: column.id == firstID, actions: actions, fit: fit)
                }
                if rowActions != nil {
                    Group {
                        if !actions.isEmpty {
                            MonarchRowActionsMenu(
                                actions: actions,
                                label: Tokens.actionsLabel(row: name, labels: labels)
                            )
                        }
                    }
                    .frame(width: fit.utility)
                }
            }
        }
        .fixedSize(horizontal: false, vertical: true)
        .background(theme.colors[role: Tokens.rowFill(isSelected: isSelected)])
        .overlay(alignment: .bottom) { line(Tokens.divider) }
        .contextMenu {
            if !actions.isEmpty {
                MonarchRowActionButtons(actions: actions)
            }
        }
    }

    @ViewBuilder private func checkbox(_ row: Row, name: String, selection: Binding<Set<Row.ID>>) -> some View {
        if isRowSelectable?(row) ?? true {
            let label = Tokens.selectLabel(row: name, labels: labels)
            Toggle(
                isOn: Binding(
                    get: { selection.wrappedValue.contains(row.id) },
                    set: {
                        selection.wrappedValue = MonarchDataTableTokens.selecting(
                            selection.wrappedValue,
                            id: row.id,
                            isOn: $0
                        )
                    }
                )
            ) {
                Text(label)
            }
            .toggleStyle(.monarchCheckbox)
            .labelsHidden()
            .accessibilityLabel(label)
        } else {
            Color.clear.accessibilityHidden(true)
        }
    }

    @ViewBuilder private func cell(
        _ row: Row,
        column: MonarchDataGridColumn<Row>,
        name: String,
        isFirst: Bool,
        actions: [MonarchDataTableRowAction],
        fit: Fit
    ) -> some View {
        let width = Tokens.width(of: column, widths: fit.widths)
        Group {
            if let edit = column.edit, edit.canEdit(row) {
                editableCell(row, column: column, edit: edit, name: name)
            } else {
                readOnlyCell(row, column: column, isFirst: isFirst, actions: actions, small: fit.isSmall)
            }
        }
        .padding(.horizontal, fit.padding)
        .padding(.vertical, Tokens.cellPadding(density))
        .frame(width: width, alignment: column.isNumeric ? .trailing : .leading)
        .frame(minHeight: MonarchAccessibility.minimumHitTarget, maxHeight: .infinity, alignment: .center)
    }

    private func editableCell(
        _ row: Row,
        column: MonarchDataGridColumn<Row>,
        edit: MonarchDataGridEdit<Row>,
        name: String
    ) -> some View {
        MonarchDataGridEditableCell(
            row: row,
            edit: edit,
            label: Tokens.editLabel(column: column.title, row: name, labels: labels),
            isNumeric: column.isNumeric,
            labels: labels.editableCell
        )
    }

    @ViewBuilder private func readOnlyCell(
        _ row: Row,
        column: MonarchDataGridColumn<Row>,
        isFirst: Bool,
        actions: [MonarchDataTableRowAction],
        small: Bool = false
    ) -> some View {
        let value = column.value(row)
        let spoken = Tokens.cellLabel(column: column.title, value: value, labels: labels)
        let role = value.isEmpty ? Tokens.muted : Tokens.text
        let text = Text(Tokens.cellText(value))
            .monarchTextStyle(small ? .caption : .body, weight: isFirst ? .medium : .regular)
            .monospacedDigit()
            .multilineTextAlignment(column.isNumeric ? .trailing : .leading)
            .fixedSize(horizontal: false, vertical: true)
            .foregroundStyle(theme.colors[role: role])
            .frame(maxWidth: .infinity, alignment: column.isNumeric ? .trailing : .leading)
        let shown = Group {
            if !isFirst, !value.isEmpty, let tone = column.tone?(row) {
                // A status as a badge, as on the web.
                MonarchBadge(value, tone: tone, size: .sm)
                    .frame(maxWidth: .infinity, alignment: column.isNumeric ? .trailing : .leading)
            } else {
                text
            }
        }
        if let onOpen {
            Button {
                onOpen(row)
            } label: {
                shown.contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(spoken)
            .accessibilityHint(labels.open ?? MonarchDataGridStrings.open)
            .accessibilityActions { actionButtons(actions) }
        } else {
            shown
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(spoken)
                .accessibilityActions { actionButtons(actions) }
        }
    }

    @ViewBuilder private func actionButtons(_ actions: [MonarchDataTableRowAction]) -> some View {
        ForEach(actions.filter { !$0.isDisabled }) { action in
            Button(action.title) { action.action() }
        }
    }

    // MARK: Pinned groups

    private enum PinnedEdge { case start, end }

    @ViewBuilder private func group<Content: View>(
        edge: PinnedEdge,
        hasColumns: Bool,
        viewport: CGFloat,
        isSelected: Bool? = nil,
        @ViewBuilder content: () -> Content
    ) -> some View {
        let fill = isSelected.map { Tokens.rowFill(isSelected: $0) } ?? Tokens.headerFill
        HStack(spacing: 0) { content() }
            .frame(maxHeight: .infinity)
            .background(theme.colors[role: fill])
            .overlay(alignment: edge == .start ? .trailing : .leading) {
                if hasColumns { verticalLine(Tokens.pinnedEdge) }
            }
            .modifier(MonarchDataGridSticky(visibleWidth: viewport))
    }

    private func line(_ role: MonarchColorRole) -> some View {
        Rectangle()
            .fill(theme.colors[role: role])
            .frame(height: Tokens.dividerWidth)
            .accessibilityHidden(true)
    }

    private func verticalLine(_ role: MonarchColorRole) -> some View {
        Rectangle()
            .fill(theme.colors[role: role])
            .frame(width: Tokens.dividerWidth)
            .accessibilityHidden(true)
    }
}

/// One editable cell of a grid row: a ``MonarchEditableCell`` whose value is
/// read from the row and saved through the column's `onCommit`.
struct MonarchDataGridEditableCell<Row>: View {
    let row: Row
    let edit: MonarchDataGridEdit<Row>
    let label: String
    let isNumeric: Bool
    let labels: MonarchEditableCellLabels

    var body: some View {
        let row = row
        let edit = edit
        MonarchEditableCell(
            label,
            value: Binding(
                get: { edit.value(row) },
                // The app writes the saved value into its rows in onCommit.
                set: { _ in }
            ),
            kind: edit.kind,
            options: edit.options,
            placeholder: edit.placeholder,
            minimum: edit.minimum,
            maximum: edit.maximum,
            minimumDate: edit.minimumDate,
            maximumDate: edit.maximumDate,
            isRequired: edit.isRequired,
            isNumeric: isNumeric,
            labels: labels,
            format: edit.format,
            validate: { edit.validate?(row, $0) },
            onCommit: { try await edit.onCommit(row, $0) }
        )
    }
}
