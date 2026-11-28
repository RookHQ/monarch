import SwiftUI

/// Tiles that people reorder in two dimensions, by dragging or with
/// accessibility actions.
///
/// ```swift
/// MonarchSortableGrid($widgets, itemLabel: \.title) { widget, state in
///     WidgetTile(widget)
/// }
/// ```
///
/// The grid is a `LazyVGrid`. Each tile is `draggable` and a
/// `dropDestination`: touch and hold a tile, drag it over another and drop,
/// and it takes that tile's place. Each tile also shows a
/// ``MonarchDragHandle`` in its top trailing corner, whose accessibility
/// actions move the tile one place backward or forward, or one row up or
/// down. Every move updates `items` and calls `onReorder`.
///
/// Without `columns` the grid fits as many columns of `minimumItemWidth` as
/// the width allows; the width scales with Dynamic Type, so larger text gives
/// fewer, wider tiles.
///
/// Accessibility: each handle is named "Reorder" and the item's name, and
/// offers the move actions; after a move or a drop the new position is
/// announced ("Design review moved to position 3 of 8."). The tile a drag is
/// over gets an accent outline. Disabled items have no handle actions and
/// cannot be dragged, but other items can move past them.
public struct MonarchSortableGrid<Item: Identifiable, Content: View>: View {
    @Binding private var items: [Item]
    private let itemLabel: KeyPath<Item, String>
    private let columns: Int?
    private let isItemDisabled: ((Item) -> Bool)?
    private let labels: MonarchSortableGridLabels
    private let onReorder: (@MainActor (_ from: Int, _ to: Int) -> Void)?
    private let content: (Item, MonarchSortableGridItemState) -> Content

    @State private var width: CGFloat = 0
    @State private var dropTarget: Int?
    @State private var namespace = UUID().uuidString
    @ScaledMetric private var minimumItemWidth: CGFloat
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.monarchTheme) private var theme
    @Environment(\.monarchSortableGridHandlePlacement) private var handlePlacement

    /// A sortable grid.
    ///
    /// - Parameters:
    ///   - items: The items in order. The grid writes the new order after each move.
    ///   - itemLabel: The plain-text name of an item, used in spoken labels and announcements.
    ///   - columns: A fixed number of columns. Without it, as many as fit `minimumItemWidth`.
    ///   - minimumItemWidth: The smallest tile width when `columns` is not set,
    ///     before Dynamic Type scaling. 160 points by default.
    ///   - isItemDisabled: Whether an item cannot be moved.
    ///   - labels: Replacements for the built-in strings.
    ///   - onReorder: Called after each finished move with the old and new index.
    ///   - content: Draws the tile of an item; the handle is added in its top trailing corner.
    public init(
        _ items: Binding<[Item]>,
        itemLabel: KeyPath<Item, String>,
        columns: Int? = nil,
        minimumItemWidth: CGFloat? = nil,
        isItemDisabled: ((Item) -> Bool)? = nil,
        labels: MonarchSortableGridLabels = MonarchSortableGridLabels(),
        onReorder: (@MainActor (_ from: Int, _ to: Int) -> Void)? = nil,
        @ViewBuilder content: @escaping (Item, MonarchSortableGridItemState) -> Content
    ) {
        _items = items
        self.itemLabel = itemLabel
        self.columns = columns
        self.isItemDisabled = isItemDisabled
        self.labels = labels
        self.onReorder = onReorder
        self.content = content
        _minimumItemWidth = ScaledMetric(
            wrappedValue: minimumItemWidth ?? MonarchSortableGridTokens.minimumItemWidth,
            relativeTo: .body
        )
    }

    /// The content of the view.
    public var body: some View {
        let spacing = MonarchSortableGridTokens.spacing
        let count = MonarchSortableGridTokens.columnCount(
            width: width,
            columns: columns,
            minimumItemWidth: minimumItemWidth,
            spacing: spacing
        )
        LazyVGrid(
            columns: Array(repeating: GridItem(.flexible(), spacing: spacing, alignment: .top), count: count),
            alignment: .leading,
            spacing: spacing
        ) {
            ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                tile(item, at: index, columns: count)
            }
        }
        .onGeometryChange(for: CGFloat.self, of: { $0.size.width }, action: { width = $0 })
    }

    private func tile(_ item: Item, at index: Int, columns: Int) -> some View {
        let isDisabled = !isEnabled || (isItemDisabled?(item) ?? false)
        let state = MonarchSortableGridItemState(
            index: index,
            isDropTarget: dropTarget == index,
            isDisabled: isDisabled
        )
        let name = item[keyPath: itemLabel]
        return content(item, state)
            .frame(maxWidth: .infinity, alignment: .topLeading)
            .overlay(alignment: handlePlacement == .leading ? .topLeading : .topTrailing) {
                MonarchDragHandle(
                    MonarchSortableGridTokens.handleLabel(name, labels: labels),
                    axis: MonarchSortableGridTokens.axis(columns: columns),
                    size: .sm,
                    actionLabels: labels.actions
                ) { direction in
                    moveByAction(from: index, direction: direction, columns: columns)
                }
                .disabled(isDisabled)
                .padding(MonarchSortableGridTokens.handleInset)
            }
            .overlay {
                if dropTarget == index {
                    MonarchShapes.container
                        .strokeBorder(
                            theme.colors[role: MonarchSortableGridTokens.dropTargetRole],
                            lineWidth: MonarchSortableGridTokens.dropTargetWidth
                        )
                        .allowsHitTesting(false)
                        .accessibilityHidden(true)
                }
            }
            .modifier(
                MonarchSortableGridDragSource(
                    payload: MonarchSortableGridTokens.payload(namespace: namespace, index: index),
                    isEnabled: !isDisabled
                )
            )
            .dropDestination(
                for: String.self,
                action: { payloads, _ in drop(payloads, onto: index) },
                isTargeted: { isTargeted in
                    if isTargeted {
                        dropTarget = index
                    } else if dropTarget == index {
                        dropTarget = nil
                    }
                }
            )
    }

    private func moveByAction(from index: Int, direction: MonarchDragHandleDirection, columns: Int) {
        guard
            let target = MonarchSortableGridTokens.target(
                from: index,
                direction: direction,
                columns: columns,
                count: items.count
            )
        else { return }
        let name = items[index][keyPath: itemLabel]
        commit(from: index, to: target)
        AccessibilityNotification.Announcement(
            MonarchSortableGridTokens.movedAnnouncement(name, to: target, count: items.count, labels: labels)
        )
        .post()
    }

    private func drop(_ payloads: [String], onto index: Int) -> Bool {
        dropTarget = nil
        guard isEnabled,
            let payload = payloads.first,
            let from = MonarchSortableGridTokens.index(payload: payload, namespace: namespace, count: items.count),
            !(isItemDisabled?(items[from]) ?? false)
        else { return false }
        let name = items[from][keyPath: itemLabel]
        commit(from: from, to: index)
        AccessibilityNotification.Announcement(
            MonarchSortableGridTokens.droppedAnnouncement(name, to: index, count: items.count, labels: labels)
        )
        .post()
        return true
    }

    private func commit(from: Int, to: Int) {
        guard from != to else { return }
        items = MonarchSortableGridTokens.move(items, from: from, to: to)
        onReorder?(from, to)
    }
}

/// Makes a tile draggable only when it can move.
private struct MonarchSortableGridDragSource: ViewModifier {
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

/// Which side of a tile the drag handle of a ``MonarchSortableGrid`` sits on.
public enum MonarchSortableGridHandlePlacement: Sendable, Hashable {
    /// The top leading corner, as the web draws it: the tile's text is indented past the handle.
    case leading
    /// The top trailing corner. The default.
    case trailing
}

private struct MonarchSortableGridHandlePlacementKey: EnvironmentKey {
    static let defaultValue = MonarchSortableGridHandlePlacement.trailing
}

extension EnvironmentValues {
    var monarchSortableGridHandlePlacement: MonarchSortableGridHandlePlacement {
        get { self[MonarchSortableGridHandlePlacementKey.self] }
        set { self[MonarchSortableGridHandlePlacementKey.self] = newValue }
    }
}

extension View {
    /// Puts the drag handles of the sortable grids inside this view on the leading or the trailing side of
    /// their tiles. Leave room for a leading handle in the tile's content.
    public func monarchSortableGridHandle(_ placement: MonarchSortableGridHandlePlacement) -> some View {
        environment(\.monarchSortableGridHandlePlacement, placement)
    }
}

#if DEBUG
    private struct MonarchSortableGridPreviewItem: Identifiable {
        let id: Int
        let title: String
    }

    #Preview("MonarchSortableGrid") {
        MonarchPreviewMatrix {
            MonarchSortableGrid(
                .constant((1...5).map { MonarchSortableGridPreviewItem(id: $0, title: "Widget \($0)") }),
                itemLabel: \.title,
                columns: 2
            ) { item, _ in
                Text(item.title)
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(.quaternary, in: MonarchShapes.container)
            }
        }
    }
#endif
