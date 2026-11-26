import MonarchTokens
import SwiftUI

/// A board of columns of cards, such as To do, Doing and Done. People move a card by dragging it
/// within and across columns, with its Move menu, or with the VoiceOver actions; each column
/// shows its count against an optional work-in-progress limit.
///
/// ```swift
/// @State private var columns = [
///     MonarchKanbanColumn(id: "todo", title: "To do", items: todo),
///     MonarchKanbanColumn(id: "doing", title: "Doing", items: doing, wipLimit: 2),
///     MonarchKanbanColumn(id: "done", title: "Done", items: done),
/// ]
///
/// MonarchKanbanBoard(
///     columns: $columns,
///     itemLabel: { $0.key },
///     onMove: { move, _ in try await api.move(move) },
///     card: { issue, _ in MonarchIssueCard(issue.title, key: issue.key) }
/// )
/// .frame(height: 560)
/// ```
///
/// The board is a horizontal `ScrollView` of columns with view-aligned scrolling: on a phone one
/// column fills the page and a swipe shows the next; wider screens show as many columns as fit
/// `minimumColumnWidth`. When not every column is in view, the columns keep their width (288 points,
/// or 85 % of the board) and the board scrolls sideways, the next column showing at the edge, as on the web.
///
/// Every movable card is `draggable` and a `dropDestination`: touch and hold a card, drag it over
/// another card and drop, and it takes that card's place; drop it on the space after the last
/// card to put it last. The binding is updated at once and `onMove` is called with the move and
/// the columns after it. While `onMove` runs, the card's state has `isPending`; if it throws, the
/// card goes back and the failure is announced.
///
/// Accessibility: each column title is a header, its count is read in words ("2 cards, limit 2,
/// at the limit") and the limit is also a symbol, not only a colour. Every movable card has a
/// Move menu named "Move Fix login to…" with the other columns, Move up and Move down; the same
/// moves are its VoiceOver actions. Moves, drops, refused drops and failed saves are announced.
/// Pick-up, a change of target and the drop play system haptics. Touch targets are at least
/// 44 points.
public struct MonarchKanbanBoard<Item: Identifiable, Card: View>: View {
    @Binding private var columns: [MonarchKanbanColumn<Item>]
    private let itemLabel: (Item) -> String
    private let canMove: ((Item, String, String) -> Bool)?
    private let isWipLimitEnforced: Bool
    private let isItemEnabled: ((Item) -> Bool)?
    private let isReadOnly: Bool
    private let externalPosition: Binding<String?>?
    private let text: MonarchKanbanBoardText
    private let onMove:
        (@MainActor (_ move: MonarchKanbanMove<Item.ID>, _ columns: [MonarchKanbanColumn<Item>]) async throws -> Void)?
    private let card: (Item, MonarchKanbanCardState) -> Card

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ScaledMetric private var minimumColumnWidth: CGFloat
    @ScaledMetric(relativeTo: .body) private var endZoneHeight = MonarchKanbanBoardTokens.endZoneHeight
    @State private var width: CGFloat = 0
    @State private var ownPosition: String?
    @State private var target: MonarchKanbanBoardDropTarget?
    @State private var pending: Set<Item.ID> = []
    @State private var dropFeedback = MonarchKanbanBoardDropFeedback()
    @State private var namespace = UUID().uuidString

    private typealias Tokens = MonarchKanbanBoardTokens
    private typealias Model = MonarchKanbanBoardModel

    /// A kanban board.
    ///
    /// - Parameters:
    ///   - columns: The columns, leading to trailing, and their cards. The board writes the new
    ///     columns after each move.
    ///   - itemLabel: The plain-text name of a card, such as its key, for the menu and announcements.
    ///   - canMove: Whether a card may go to another column (card, to, from). Its own column always
    ///     takes it. Refused columns are disabled in the menu and refuse drops.
    ///   - isWipLimitEnforced: A column at or over its `wipLimit` refuses cards from other columns.
    ///   - isItemEnabled: Whether a card can be moved, such as one the person may not edit.
    ///   - isReadOnly: Turns off every way of moving.
    ///   - minimumColumnWidth: The narrowest a column gets, before Dynamic Type scaling. 288 points
    ///     by default: one column per page on a phone.
    ///   - scrollPosition: The id of the first column in view, to choose the column shown or
    ///     follow it. Kept inside when `nil`.
    ///   - labels: Replacements for the built-in strings.
    ///   - onMove: Saves a finished move with the columns after it. Throwing puts the card back
    ///     and announces it.
    ///   - card: The content of a card, such as an issue card (`MonarchIssueCard`, experimental).
    public init(
        columns: Binding<[MonarchKanbanColumn<Item>]>,
        itemLabel: @escaping (Item) -> String,
        canMove: ((_ item: Item, _ toColumnID: String, _ fromColumnID: String) -> Bool)? = nil,
        isWipLimitEnforced: Bool = false,
        isItemEnabled: ((Item) -> Bool)? = nil,
        isReadOnly: Bool = false,
        minimumColumnWidth: CGFloat? = nil,
        scrollPosition: Binding<String?>? = nil,
        labels: MonarchKanbanBoardLabels = MonarchKanbanBoardLabels(),
        onMove: (
            @MainActor (_ move: MonarchKanbanMove<Item.ID>, _ columns: [MonarchKanbanColumn<Item>]) async throws
                -> Void
        )? = nil,
        @ViewBuilder card: @escaping (Item, MonarchKanbanCardState) -> Card
    ) {
        _columns = columns
        self.itemLabel = itemLabel
        self.canMove = canMove
        self.isWipLimitEnforced = isWipLimitEnforced
        self.isItemEnabled = isItemEnabled
        self.isReadOnly = isReadOnly
        self.externalPosition = scrollPosition
        self.text = MonarchKanbanBoardText(labels: labels)
        self.onMove = onMove
        self.card = card
        _minimumColumnWidth = ScaledMetric(
            wrappedValue: minimumColumnWidth ?? MonarchKanbanBoardTokens.minimumColumnWidth,
            relativeTo: .body
        )
    }

    private var position: Binding<String?> {
        externalPosition ?? $ownPosition
    }

    /// The content of the view.
    public var body: some View {
        let spacing = Tokens.columnSpacing
        let perPage = Model.columnsPerPage(
            width: width - spacing * 2,
            minimumWidth: minimumColumnWidth,
            spacing: spacing,
            count: columns.count
        )
        VStack(alignment: .leading, spacing: Tokens.tabsSpacing) {
            // The columns sit side by side and scroll sideways, as on the web: a column is 288 points
            // wide, or 85 % of the board when that is narrower, so the next column shows at its edge.
            // No tab strip stands in for them.
            ScrollView(.horizontal) {
                HStack(alignment: .top, spacing: spacing) {
                    ForEach(columns) { column in
                        if perPage < columns.count {
                            columnView(column)
                                .frame(width: min(minimumColumnWidth, max(0, (width - spacing * 2) * 0.85)))
                                .id(column.id)
                        } else {
                            columnView(column)
                                .containerRelativeFrame(.horizontal, count: perPage, span: 1, spacing: spacing)
                                .id(column.id)
                        }
                    }
                }
                .scrollTargetLayout()
            }
            .scrollTargetBehavior(.viewAligned)
            .scrollPosition(id: position)
            .scrollIndicators(.hidden)
            .contentMargins(.horizontal, spacing, for: .scrollContent)
        }
        .onGeometryChange(for: CGFloat.self, of: { $0.size.width }, action: { width = $0 })
        .sensoryFeedback(trigger: target) { old, new in
            guard new != nil else { return nil }
            return old == nil ? .impact : .selection
        }
        .sensoryFeedback(trigger: dropFeedback) { _, new in
            new.isAccepted ? .success : .error
        }
        .accessibilityElement(children: .contain)
    }

    // MARK: Tabs

    private var tabs: some View {
        let selected = position.wrappedValue ?? columns.first?.id
        return ScrollView(.horizontal) {
            HStack(spacing: 0) {
                ForEach(columns) { column in
                    tab(column, isSelected: column.id == selected)
                }
            }
            .padding(.horizontal, Tokens.columnSpacing)
        }
        .scrollIndicators(.hidden)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(text.columns)
    }

    private func tab(_ column: MonarchKanbanColumn<Item>, isSelected: Bool) -> some View {
        let role = Tokens.tab(isSelected: isSelected)
        return Button {
            withAnimation(theme.motion.animation(reduceMotion: reduceMotion)) {
                position.wrappedValue = column.id
            }
        } label: {
            HStack(alignment: .firstTextBaseline, spacing: Tokens.spacing) {
                Text(column.title).monarchTextStyle(.label)
                Text(text.countText(column.items.count, limit: column.wipLimit)).monarchTextStyle(.caption)
            }
            .foregroundStyle(theme.colors[role: role])
            .padding(.horizontal, Tokens.tabPadding)
            .monarchMinimumHitTarget()
            .overlay(alignment: .bottom) {
                if isSelected {
                    Rectangle()
                        .fill(theme.colors[role: role])
                        .frame(height: Tokens.targetWidth)
                        .accessibilityHidden(true)
                }
            }
        }
        .buttonStyle(.plain)
        .springLoadingBehavior(.enabled)
        .accessibilityLabel(column.title)
        .accessibilityValue(text.count(column.items.count, limit: column.wipLimit))
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .dropDestination(
            for: String.self,
            action: { payloads, _ in drop(payloads, into: column.id, onto: nil) },
            isTargeted: { setTarget(MonarchKanbanBoardDropTarget(columnID: column.id, index: nil), $0) }
        )
    }

    // MARK: Columns

    private func columnView(_ column: MonarchKanbanColumn<Item>) -> some View {
        let isTargeted = target?.columnID == column.id
        let shape = RoundedRectangle(cornerRadius: Tokens.columnRadius, style: .continuous)
        return VStack(alignment: .leading, spacing: 0) {
            header(column)
                .padding([.horizontal, .top], Tokens.columnPadding)
            ScrollView(.vertical) {
                LazyVStack(alignment: .leading, spacing: Tokens.cardSpacing) {
                    ForEach(Array(column.items.enumerated()), id: \.element.id) { index, item in
                        cardRow(item, at: index, in: column)
                    }
                    endZone(column)
                }
                .padding(Tokens.columnPadding)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(theme.colors[role: Tokens.columnFill], in: shape)
        .overlay(
            shape.strokeBorder(
                theme.colors[role: Tokens.columnBorder(isTargeted: isTargeted)],
                lineWidth: Tokens.columnBorderWidth(isTargeted: isTargeted)
            )
            .allowsHitTesting(false)
        )
        .monarchAnimation(.fast, value: isTargeted)
        .accessibilityElement(children: .contain)
    }

    private func header(_ column: MonarchKanbanColumn<Item>) -> some View {
        let count = column.items.count
        let state = Tokens.limitState(count: count, limit: column.wipLimit)
        return HStack(alignment: .firstTextBaseline, spacing: Tokens.spacing) {
            if let systemImage = column.systemImage {
                MonarchIcon(systemName: systemImage, size: .sm)
                    .foregroundStyle(theme.colors[role: Tokens.muted])
            }
            Text(column.title)
                .monarchTextStyle(.subheading)
                .foregroundStyle(theme.colors.textDefault)
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityAddTraits(.isHeader)
            HStack(alignment: .firstTextBaseline, spacing: Tokens.spacing) {
                if Tokens.showsLimitSymbol(state) {
                    MonarchIcon(systemName: Tokens.limitSymbol, size: .sm)
                }
                Text(text.countText(count, limit: column.wipLimit)).monarchTextStyle(.caption)
            }
            .foregroundStyle(theme.colors[role: Tokens.countRole(state)])
            .padding(.horizontal, Tokens.spacing)
            .frame(minWidth: Tokens.spacing * 3)
            .background(theme.colors[role: Tokens.countFill(state)], in: Capsule())
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(text.count(count, limit: column.wipLimit))
        }
    }

    private func endZone(_ column: MonarchKanbanColumn<Item>) -> some View {
        let zone = MonarchKanbanBoardDropTarget(columnID: column.id, index: nil)
        return VStack(alignment: .leading, spacing: Tokens.cardSpacing) {
            if target == zone { insertionLine }
            if column.items.isEmpty {
                Text(column.emptyText ?? text.empty)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: Tokens.muted])
            }
        }
        .frame(maxWidth: .infinity, minHeight: endZoneHeight, alignment: .topLeading)
        .contentShape(Rectangle())
        .dropDestination(
            for: String.self,
            action: { payloads, _ in drop(payloads, into: column.id, onto: nil) },
            isTargeted: { setTarget(zone, $0) }
        )
    }

    private var insertionLine: some View {
        Capsule()
            .fill(theme.colors[role: Tokens.accent])
            .frame(height: Tokens.targetWidth)
            .accessibilityHidden(true)
    }

    // MARK: Cards

    private func isMovable(_ item: Item) -> Bool {
        !isReadOnly && isEnabled && (isItemEnabled?(item) ?? true)
    }

    private func cardRow(_ item: Item, at index: Int, in column: MonarchKanbanColumn<Item>) -> some View {
        let movable = isMovable(item)
        let place = MonarchKanbanBoardDropTarget(columnID: column.id, index: index)
        let state = MonarchKanbanCardState(
            columnID: column.id,
            index: index,
            isDropTarget: target == place,
            isPending: pending.contains(item.id),
            isEnabled: movable
        )
        return VStack(alignment: .leading, spacing: Tokens.cardSpacing) {
            if state.isDropTarget { insertionLine }
            HStack(alignment: .top, spacing: Tokens.spacing) {
                // The grip is on the leading edge of the card, as on the web.
                if movable { moveMenu(item, in: column) }
                card(item, state).frame(maxWidth: .infinity, alignment: .leading)
            }
            // The board draws the card's surface, as on the web.
            .padding(.vertical, Tokens.spacing)
            .padding(.leading, movable ? 0 : Tokens.spacing)
            .padding(.trailing, Tokens.spacing)
            .monarchElevation(.xs, in: RoundedRectangle(cornerRadius: MonarchTokens.Radius.md, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: MonarchTokens.Radius.md, style: .continuous)
                    .strokeBorder(theme.colors.borderSubtle, lineWidth: Tokens.borderWidth)
            )
        }
        .modifier(
            MonarchKanbanBoardDragSource(
                payload: Model.payload(namespace: namespace, itemID: item.id),
                isEnabled: movable
            )
        )
        .dropDestination(
            for: String.self,
            action: { payloads, _ in drop(payloads, into: column.id, onto: index) },
            isTargeted: { setTarget(place, $0) }
        )
    }

    private func moveMenu(_ item: Item, in column: MonarchKanbanColumn<Item>) -> some View {
        let label = itemLabel(item)
        let up = Model.step(columns, itemID: item.id, up: true)
        let down = Model.step(columns, itemID: item.id, up: false)
        let others = columns.filter { $0.id != column.id }
        return Menu {
            Section(text.moveToSection) {
                ForEach(others) { other in
                    Button(other.title) { move(item, to: other.id, at: other.items.count) }
                        .disabled(!allows(item, to: other.id, from: column.id))
                }
            }
            Button(text.moveUp, systemImage: "arrow.up") {
                if let up { move(item, to: column.id, at: up) }
            }
            .disabled(up == nil)
            Button(text.moveDown, systemImage: "arrow.down") {
                if let down { move(item, to: column.id, at: down) }
            }
            .disabled(down == nil)
        } label: {
            Label {
                Text(text.moveMenu(label))
            } icon: {
                MonarchGripGlyph()
            }
            .labelStyle(.iconOnly)
        }
        .buttonStyle(.monarch(.ghost, size: .sm))
        .accessibilityActions {
            if let up {
                Button(text.moveUp) { move(item, to: column.id, at: up) }
            }
            if let down {
                Button(text.moveDown) { move(item, to: column.id, at: down) }
            }
            ForEach(others.filter { allows(item, to: $0.id, from: column.id) }) { other in
                Button(text.moveTo(other.title)) { move(item, to: other.id, at: other.items.count) }
            }
        }
    }

    // MARK: Moving

    private func allows(_ item: Item, to columnID: String, from fromColumnID: String) -> Bool {
        Model.allows(
            item,
            toColumnID: columnID,
            fromColumnID: fromColumnID,
            columns: columns,
            canMove: canMove,
            isWipLimitEnforced: isWipLimitEnforced
        )
    }

    private func setTarget(_ place: MonarchKanbanBoardDropTarget, _ isTargeted: Bool) {
        if isTargeted {
            target = place
        } else if target == place {
            target = nil
        }
    }

    private func drop(_ payloads: [String], into columnID: String, onto index: Int?) -> Bool {
        target = nil
        guard let payload = payloads.first,
            let itemID = Model.itemID(payload: payload, namespace: namespace, in: columns),
            let from = Model.location(of: itemID, in: columns),
            let column = columns.first(where: { $0.id == columnID })
        else { return false }
        let item = columns[from.column].items[from.index]
        guard isMovable(item) else { return false }
        let fromColumnID = columns[from.column].id
        guard allows(item, to: columnID, from: fromColumnID) else {
            announce(text.notAllowed(itemLabel(item), column: column.title))
            dropFeedback.record(isAccepted: false)
            return false
        }
        let place = Model.dropIndex(
            isSameColumn: fromColumnID == columnID,
            onto: index,
            count: column.items.count
        )
        if commit(item, to: columnID, at: place, isDrop: true) {
            dropFeedback.record(isAccepted: true)
        }
        return true
    }

    private func move(_ item: Item, to columnID: String, at index: Int) {
        guard isMovable(item), let from = Model.location(of: item.id, in: columns),
            allows(item, to: columnID, from: columns[from.column].id)
        else { return }
        commit(item, to: columnID, at: index, isDrop: false)
    }

    @discardableResult
    private func commit(_ item: Item, to columnID: String, at index: Int, isDrop: Bool) -> Bool {
        guard let result = Model.moving(columns, itemID: item.id, toColumnID: columnID, toIndex: index) else {
            return false
        }
        columns = result.columns
        let name = itemLabel(item)
        if let column = result.columns.first(where: { $0.id == columnID }) {
            let position = result.move.toIndex + 1
            let total = column.items.count
            announce(
                isDrop
                    ? text.dropped(name, column: column.title, position: position, total: total)
                    : text.moved(name, column: column.title, position: position, total: total)
            )
        }
        save(result.move, columns: result.columns, name: name)
        return true
    }

    private func save(_ move: MonarchKanbanMove<Item.ID>, columns after: [MonarchKanbanColumn<Item>], name: String) {
        guard let onMove else { return }
        pending.insert(move.itemID)
        Task { @MainActor in
            do {
                try await onMove(move, after)
            } catch {
                columns = Model.reverting(columns, move)
                let title = columns.first(where: { $0.id == move.fromColumnID })?.title ?? move.fromColumnID
                announce(text.failed(name, column: title))
            }
            pending.remove(move.itemID)
        }
    }

    private func announce(_ message: String) {
        AccessibilityNotification.Announcement(message).post()
    }
}

/// A place a dragged card is over: a card (`index`) or the end of a column (`index` is `nil`).
struct MonarchKanbanBoardDropTarget: Hashable {
    let columnID: String
    let index: Int?
}

/// Changes on every finished drop, so the drop haptic plays once per drop.
struct MonarchKanbanBoardDropFeedback: Equatable {
    private(set) var count = 0
    private(set) var isAccepted = true

    mutating func record(isAccepted: Bool) {
        count += 1
        self.isAccepted = isAccepted
    }
}

/// Makes a card draggable only when it can move.
private struct MonarchKanbanBoardDragSource: ViewModifier {
    let payload: String
    let isEnabled: Bool

    func body(content: Content) -> some View {
        if isEnabled {
            content.draggable(payload)
        } else {
            content
        }
    }
}

#if DEBUG
    private struct MonarchKanbanBoardPreviewItem: Identifiable {
        let id: String
        let title: String
    }

    #Preview("MonarchKanbanBoard") {
        MonarchPreviewMatrix {
            MonarchKanbanBoard(
                columns: .constant([
                    MonarchKanbanColumn(
                        id: "todo",
                        title: "To do",
                        items: [
                            MonarchKanbanBoardPreviewItem(id: "ENG-1", title: "Fix login"),
                            MonarchKanbanBoardPreviewItem(id: "ENG-2", title: "Export CSV"),
                        ]
                    ),
                    MonarchKanbanColumn(
                        id: "doing",
                        title: "Doing",
                        items: [MonarchKanbanBoardPreviewItem(id: "ENG-3", title: "Billing page")],
                        wipLimit: 1
                    ),
                    MonarchKanbanColumn(id: "done", title: "Done", items: [MonarchKanbanBoardPreviewItem]()),
                ]),
                itemLabel: { $0.id },
                isItemEnabled: { $0.id != "ENG-2" },
                card: { item, _ in MonarchIssueCard(item.title, key: item.id) }
            )
            .frame(height: 480)
        }
    }
#endif
