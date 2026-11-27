import MonarchTokens
import SwiftUI

/// The responsive grid a dashboard's widgets sit in, which people can
/// rearrange in an edit mode.
///
/// ```swift
/// @AppStorage("homeLayout") private var stored = "[]"
/// @State private var layout: [String] = []
/// @State private var isEditing = false
///
/// MonarchDashboardGrid(
///     widgets,
///     layout: $layout,
///     itemLabel: \.title,
///     span: { $0.isWide ? .two : .one },
///     isEditing: isEditing,
///     content: { widget in WidgetView(widget) }
/// )
/// ```
///
/// The grid has one column on phones and at the accessibility text sizes,
/// two at medium widths (from 640 points of grid width) and `columns` from
/// 1024 points, so an iPad in landscape shows the full grid. Widgets span
/// columns with `span`; a widget that does not fit in what is left of a row
/// starts the next one. The grid does not scroll: put it in a `ScrollView`.
///
/// `layout` is the order of ids as stored for this person. Ids that no
/// longer exist are skipped and new items follow in their own order, so a
/// stored layout survives widgets being added or removed. Pass an empty
/// array for the default order.
///
/// With `isEditing`, each widget gets an arrange bar with its name and Move
/// earlier and Move later buttons; touch and hold the bar and drag it onto
/// another widget to take its place. Every finished move writes `layout` and
/// calls `onLayoutChange`.
///
/// Accessibility: the grid is a container named "Dashboard". In edit mode
/// the buttons are named "Move Team load earlier" and "Move Team load
/// later", are disabled at the ends, and every move or drop is announced
/// ("Team load moved to position 2 of 6."). The bar's grip is hidden; the
/// bar reads the widget's name and how to drag.
public struct MonarchDashboardGrid<Item: Identifiable, Content: View>: View {
    private let items: [Item]
    @Binding private var layout: [Item.ID]
    private let itemLabel: KeyPath<Item, String>
    private let span: ((Item) -> MonarchDashboardGridSpan)?
    private let columns: Int
    private let isEditing: Bool
    private let labels: MonarchDashboardGridLabels
    private let onLayoutChange: (@MainActor (_ from: Int, _ to: Int) -> Void)?
    private let content: (Item) -> Content

    @State private var width: CGFloat = 0
    @State private var dropTarget: Int?
    @State private var namespace = UUID().uuidString
    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// A dashboard grid.
    ///
    /// - Parameters:
    ///   - items: The widgets, in their default order.
    ///   - layout: The order of ids as stored for this person. The grid writes the new order after each move.
    ///   - itemLabel: The plain-text name of a widget, used in the arrange bar, labels and announcements.
    ///   - span: The columns a widget spans on wide screens. One by default.
    ///   - columns: The columns on wide screens, from 1 to 4.
    ///   - isEditing: Whether widgets can be rearranged.
    ///   - labels: Replacements for the built-in strings.
    ///   - onLayoutChange: Called after each finished move with the old and new index.
    ///   - content: Draws a widget, usually a ``MonarchWidget``.
    public init(
        _ items: [Item],
        layout: Binding<[Item.ID]>,
        itemLabel: KeyPath<Item, String>,
        span: ((Item) -> MonarchDashboardGridSpan)? = nil,
        columns: Int = 3,
        isEditing: Bool = false,
        labels: MonarchDashboardGridLabels = MonarchDashboardGridLabels(),
        onLayoutChange: (@MainActor (_ from: Int, _ to: Int) -> Void)? = nil,
        @ViewBuilder content: @escaping (Item) -> Content
    ) {
        self.items = items
        _layout = layout
        self.itemLabel = itemLabel
        self.span = span
        self.columns = columns
        self.isEditing = isEditing
        self.labels = labels
        self.onLayoutChange = onLayoutChange
        self.content = content
    }

    private typealias Tokens = MonarchDashboardGridTokens

    /// The content of the view.
    public var body: some View {
        let byID = Dictionary(items.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let order = Tokens.resolve(ids: items.map(\.id), layout: layout).compactMap { byID[$0] }
        let count = Tokens.columnCount(
            width: width,
            columns: columns,
            isAccessibilitySize: dynamicTypeSize.isAccessibilitySize
        )
        let spans = order.map { Tokens.span(span?($0) ?? .one, columns: count) }
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            ForEach(Tokens.rows(spans: spans, columns: count), id: \.self) { row in
                HStack(alignment: .top, spacing: Tokens.spacing) {
                    ForEach(row, id: \.self) { index in
                        cell(order[index], at: index, of: order, columns: count)
                            .frame(width: cellWidth(spans[index], columns: count), alignment: .topLeading)
                            .frame(maxWidth: width > 0 ? nil : .infinity, alignment: .topLeading)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .onGeometryChange(for: CGFloat.self, of: { $0.size.width }, action: { width = $0 })
        .monarchAnimation(value: order.map(\.id))
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.label ?? MonarchDashboardGridStrings.label)
    }

    private func cellWidth(_ span: Int, columns: Int) -> CGFloat? {
        guard width > 0 else { return nil }
        return Tokens.width(span: span, columns: columns, total: width, spacing: Tokens.spacing)
    }

    // MARK: Cell

    @ViewBuilder private func cell(_ item: Item, at index: Int, of order: [Item], columns: Int) -> some View {
        if isEditing {
            VStack(alignment: .leading, spacing: Tokens.barSpacing) {
                arrangeBar(item, at: index, count: order.count, columns: columns)
                    .draggable(MonarchSortableGridTokens.payload(namespace: namespace, index: index))
                content(item)
            }
            .overlay {
                if dropTarget == index {
                    MonarchShapes.container
                        .strokeBorder(theme.colors[role: Tokens.dropTargetRole], lineWidth: Tokens.dropTargetWidth)
                        .allowsHitTesting(false)
                        .accessibilityHidden(true)
                }
            }
            .dropDestination(
                for: String.self,
                action: { payloads, _ in drop(payloads, onto: index, order: order) },
                isTargeted: { isTargeted in
                    if isTargeted {
                        dropTarget = index
                    } else if dropTarget == index {
                        dropTarget = nil
                    }
                }
            )
        } else {
            content(item)
        }
    }

    private func arrangeBar(_ item: Item, at index: Int, count: Int, columns: Int) -> some View {
        let name = item[keyPath: itemLabel]
        let symbols = Tokens.moveSymbols(columns: columns)
        return HStack(spacing: Tokens.barSpacing) {
            HStack(spacing: Tokens.barSpacing) {
                MonarchIcon(systemName: Tokens.gripSymbol, size: .sm)
                Text(name)
                    .monarchTextStyle(.label)
                    .lineLimit(1)
            }
            .foregroundStyle(theme.colors[role: Tokens.barText])
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(name)
            .accessibilityHint(labels.dragHint ?? MonarchDashboardGridStrings.dragHint)
            Spacer(minLength: 0)
            moveButton(
                Tokens.moveEarlierLabel(name, labels: labels),
                systemImage: symbols.earlier,
                target: Tokens.target(from: index, step: -1, count: count),
                from: index
            )
            moveButton(
                Tokens.moveLaterLabel(name, labels: labels),
                systemImage: symbols.later,
                target: Tokens.target(from: index, step: 1, count: count),
                from: index
            )
        }
        .padding(Tokens.barPadding)
        .background(theme.colors[role: Tokens.barFill], in: MonarchShapes.control)
    }

    private func moveButton(_ label: String, systemImage: String, target: Int?, from index: Int) -> some View {
        Button {
            guard let target else { return }
            move(from: index, to: target, announcement: Tokens.movedAnnouncement)
        } label: {
            MonarchIcon(systemName: systemImage, label: label, size: .sm)
        }
        .buttonStyle(.monarch(.ghost, size: .sm))
        .disabled(target == nil)
    }

    // MARK: Moves

    private func drop(_ payloads: [String], onto index: Int, order: [Item]) -> Bool {
        dropTarget = nil
        guard let payload = payloads.first,
            let from = MonarchSortableGridTokens.index(payload: payload, namespace: namespace, count: order.count)
        else { return false }
        move(from: from, to: index, announcement: Tokens.droppedAnnouncement)
        return true
    }

    private func move(
        from: Int,
        to: Int,
        announcement: (String, Int, Int, MonarchDashboardGridLabels) -> String
    ) {
        let ids = Tokens.resolve(ids: items.map(\.id), layout: layout)
        guard from != to, ids.indices.contains(from), ids.indices.contains(to),
            let item = items.first(where: { $0.id == ids[from] })
        else { return }
        layout = MonarchSortableGridTokens.move(ids, from: from, to: to)
        onLayoutChange?(from, to)
        AccessibilityNotification.Announcement(
            announcement(item[keyPath: itemLabel], to, ids.count, labels)
        )
        .post()
    }
}

#if DEBUG
    private struct MonarchDashboardGridPreviewItem: Identifiable {
        let id: String
        var title: String { id }
    }

    #Preview("MonarchDashboardGrid") {
        MonarchPreviewMatrix {
            MonarchDashboardGrid(
                ["Team load", "Response time", "Backlog"].map(MonarchDashboardGridPreviewItem.init),
                layout: .constant([]),
                itemLabel: \.title,
                isEditing: true
            ) { item in
                MonarchWidget(item.title) {
                    Text("42").monarchTextStyle(.title)
                }
            }
        }
    }
#endif
