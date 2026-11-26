import Foundation
import SwiftUI

/// Plans work into buckets (iterations, sprints, releases and the backlog): a `List` with one
/// section per bucket, totals and a capacity meter in each header, and three ways to move an item:
/// drag it, use its Move menu, or use the VoiceOver actions.
///
/// ```swift
/// @State private var buckets = [
///     MonarchBacklogBucket(id: "s14", title: "Sprint 14", badge: "Active", capacity: 20, items: sprint),
///     MonarchBacklogBucket(id: "backlog", title: "Backlog", items: backlog),
/// ]
///
/// MonarchBacklogPlanner(
///     buckets: $buckets,
///     itemLabel: { $0.key },
///     points: { $0.estimate },
///     onMove: { move in api.move(move) },
///     row: { issue in Text(issue.title) }
/// )
/// ```
///
/// Rows are `draggable` and each bucket's rows are a `dropDestination`, so an item can be dragged
/// within a bucket and across buckets; `onMove` also supports the system reorder handles in edit
/// mode. The Move menu on every row is the path that always works: it lists the other buckets
/// and Move up and Move down. The binding is updated at once and `onMove` reports each finished
/// move; to undo a move that failed to save, set the buckets back.
///
/// Accessibility: each bucket heading is a header button that shows or hides the bucket, with
/// "Expanded" or "Collapsed" as its value. The Move menu is named "Move PROJ-1 to…" and carries
/// Move up, Move down and "Move to Sprint 14" as VoiceOver actions. Each move is announced
/// ("PROJ-1 moved to Sprint 14, position 2 of 5."). Over capacity is a note with an icon, not only
/// a colour. Touch targets are at least 44 points.
@_spi(Experimental) public struct MonarchBacklogPlanner<Item: Identifiable, Row: View>: View {
    @Binding private var buckets: [MonarchBacklogBucket<Item>]
    private let itemLabel: (Item) -> String
    private let points: ((Item) -> Int?)?
    private let canMove: ((Item, String, String) -> Bool)?
    private let externalCollapsed: Binding<Set<String>>?
    private let isReadOnly: Bool
    private let text: MonarchBacklogPlannerText
    private let onMove: (@MainActor (MonarchBacklogMove<Item.ID>) -> Void)?
    private let onBucketAction: (@MainActor (_ bucketID: String) -> Void)?
    private let row: (Item) -> Row

    @Environment(\.monarchTheme) private var theme
    @State private var ownCollapsed: Set<String> = []

    /// A backlog planner.
    ///
    /// - Parameters:
    ///   - buckets: The buckets, top to bottom: usually the active and planned iterations, then
    ///     the backlog.
    ///   - itemLabel: A short plain name of an item, such as its key, for the menu and announcements.
    ///   - points: The points of an item, `nil` when it has no estimate. Turns on totals and capacity.
    ///   - canMove: Whether an item may go from one bucket to another (item, to, from). Refused
    ///     buckets are disabled in the menu and skipped by Move up and Move down.
    ///   - collapsed: The ids of closed buckets, when the app keeps them. Kept inside otherwise.
    ///   - isReadOnly: Turns off every way of moving.
    ///   - labels: Replacements for the built-in strings.
    ///   - onMove: Runs after each finished move.
    ///   - onBucketAction: Runs when the header button of a bucket (its `actionTitle`) is pressed, with the bucket id.
    ///   - row: The content of an item's row, such as a title and pickers.
    public init(
        buckets: Binding<[MonarchBacklogBucket<Item>]>,
        itemLabel: @escaping (Item) -> String,
        points: ((Item) -> Int?)? = nil,
        canMove: ((_ item: Item, _ toBucketID: String, _ fromBucketID: String) -> Bool)? = nil,
        collapsed: Binding<Set<String>>? = nil,
        isReadOnly: Bool = false,
        labels: MonarchBacklogPlannerLabels = MonarchBacklogPlannerLabels(),
        onMove: (@MainActor (MonarchBacklogMove<Item.ID>) -> Void)? = nil,
        onBucketAction: (@MainActor (_ bucketID: String) -> Void)? = nil,
        @ViewBuilder row: @escaping (Item) -> Row
    ) {
        _buckets = buckets
        self.itemLabel = itemLabel
        self.points = points
        self.canMove = canMove
        self.externalCollapsed = collapsed
        self.isReadOnly = isReadOnly
        self.text = MonarchBacklogPlannerText(labels: labels)
        self.onMove = onMove
        self.onBucketAction = onBucketAction
        self.row = row
    }

    private var collapsed: Binding<Set<String>> {
        externalCollapsed ?? $ownCollapsed
    }

    /// The content of the view.
    public var body: some View {
        List {
            Group {
                ForEach(buckets) { bucket in
                    Section(isExpanded: expandedBinding(bucket.id)) {
                        if bucket.items.isEmpty {
                            Text(bucket.emptyMessage ?? text.empty)
                                .monarchTextStyle(.body)
                                .foregroundStyle(theme.colors[role: MonarchBacklogPlannerTokens.muted])
                        } else {
                            bucketRows(bucket)
                        }
                    } header: {
                        header(bucket)
                    }
                }

            }
            .monarchRowsOnCanvas()
        }
        .listStyle(.plain)
        .monarchCanvas()
    }

    // MARK: Rows

    @ViewBuilder private func bucketRows(_ bucket: MonarchBacklogBucket<Item>) -> some View {
        let rows = ForEach(bucket.items) { item in
            itemRow(item, in: bucket)
        }
        if isReadOnly {
            rows
        } else {
            rows
                .onMove { source, destination in
                    if let result = MonarchBacklogPlannerTokens.reordering(
                        buckets,
                        bucketID: bucket.id,
                        from: source,
                        to: destination
                    ) {
                        commit(result)
                    }
                }
                .dropDestination(for: String.self) { payloads, index in
                    for payload in payloads {
                        drop(payload, into: bucket.id, at: index)
                    }
                }
        }
    }

    @ViewBuilder private func itemRow(_ item: Item, in bucket: MonarchBacklogBucket<Item>) -> some View {
        let content = HStack(alignment: .center, spacing: MonarchBacklogPlannerTokens.spacing) {
            if !isReadOnly {
                // The grip of the web row; the row is dragged by it and the menu at the end moves it too.
                MonarchDragHandle(text.moveMenu(itemLabel(item)), size: .sm)
                    .accessibilityHidden(true)
            }
            row(item).frame(maxWidth: .infinity, alignment: .leading)
            if let points {
                let estimate = points(item)
                Text(text.rowPoints(estimate))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchBacklogPlannerTokens.muted])
                    .accessibilityLabel(text.spokenPoints(estimate))
            }
            if !isReadOnly { moveMenu(item, in: bucket) }
        }
        if isReadOnly {
            content
        } else {
            content.draggable(Self.payload(item))
        }
    }

    private func moveMenu(_ item: Item, in bucket: MonarchBacklogBucket<Item>) -> some View {
        let label = itemLabel(item)
        let up = MonarchBacklogPlannerTokens.step(buckets, itemID: item.id, up: true, allows: allows)
        let down = MonarchBacklogPlannerTokens.step(buckets, itemID: item.id, up: false, allows: allows)
        let others = buckets.filter { $0.id != bucket.id }
        return Menu {
            Section(text.moveTo) {
                ForEach(others) { target in
                    Button(target.title) { move(item, to: target.id, at: target.items.count) }
                        .disabled(!allows(item, target.id, bucket.id))
                }
            }
            Button(text.moveUp, systemImage: "arrow.up") {
                if let up { move(item, to: up.bucketID, at: up.index) }
            }
            .disabled(up == nil)
            Button(text.moveDown, systemImage: "arrow.down") {
                if let down { move(item, to: down.bucketID, at: down.index) }
            }
            .disabled(down == nil)
        } label: {
            Label(text.moveMenu(label), systemImage: MonarchBacklogPlannerTokens.moveSymbol)
                .labelStyle(.iconOnly)
        }
        .buttonStyle(.monarch(.ghost, size: .sm))
        .accessibilityActions {
            if let up {
                Button(text.moveUp) { move(item, to: up.bucketID, at: up.index) }
            }
            if let down {
                Button(text.moveDown) { move(item, to: down.bucketID, at: down.index) }
            }
            ForEach(others.filter { allows(item, $0.id, bucket.id) }) { target in
                Button(text.moveToBucket(target.title)) { move(item, to: target.id, at: target.items.count) }
            }
        }
    }

    // MARK: Header

    private func header(_ bucket: MonarchBacklogBucket<Item>) -> some View {
        let isExpanded = !collapsed.wrappedValue.contains(bucket.id)
        let totals = MonarchBacklogPlannerTokens.totals(bucket.items, points: points)
        let over = MonarchBacklogPlannerTokens.overCapacity(points: totals.points, capacity: bucket.capacity)
        return VStack(alignment: .leading, spacing: MonarchBacklogPlannerTokens.headerSpacing) {
            HStack(alignment: .center, spacing: MonarchBacklogPlannerTokens.spacing) {
                Button {
                    expandedBinding(bucket.id).wrappedValue.toggle()
                } label: {
                    HStack(alignment: .firstTextBaseline, spacing: MonarchBacklogPlannerTokens.spacing) {
                        MonarchIcon(
                            systemName: isExpanded
                                ? MonarchBacklogPlannerTokens.expandedSymbol
                                : MonarchBacklogPlannerTokens.collapsedSymbol,
                            size: .sm
                        )
                        Text(bucket.title)
                            .monarchTextStyle(.bodyStrong)
                            .foregroundStyle(theme.colors.textDefault)
                        if let badge = bucket.badge {
                            MonarchBadge(badge, tone: bucket.badgeTone)
                        }
                        Spacer(minLength: 0)
                    }
                    .monarchMinimumHitTarget()
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(.isHeader)
                .accessibilityValue(isExpanded ? text.expanded : text.collapsed)
                .accessibilityHint(text.toggleHint)
                if let actionTitle = bucket.actionTitle {
                    MonarchButton(actionTitle, variant: .secondary, size: .xs) { onBucketAction?(bucket.id) }
                }
            }
            // The dates, the totals and the capacity share a wrapping line, as on the web.
            MonarchFlowLayout(spacing: MonarchBacklogPlannerTokens.spacing * 2) {
                if let subtitle = bucket.subtitle { muted(subtitle) }
                muted(text.summary(count: totals.count, points: totals.points, unestimated: totals.unestimated))
                if let capacity = bucket.capacity, let planned = totals.points {
                    HStack(spacing: MonarchBacklogPlannerTokens.spacing) {
                        ProgressView(
                            value: MonarchBacklogPlannerTokens.capacityFraction(points: planned, capacity: capacity)
                        )
                        .progressViewStyle(.monarchLinear(tone: .neutral))
                        .frame(width: 96)
                        .accessibilityLabel(text.capacityLabel)
                        .accessibilityValue(text.capacity(points: planned, capacity: capacity))
                        muted(text.capacity(points: planned, capacity: capacity))
                    }
                }
            }
            if let over {
                HStack(alignment: .firstTextBaseline, spacing: MonarchBacklogPlannerTokens.spacing) {
                    MonarchIcon(systemName: MonarchBacklogPlannerTokens.overCapacitySymbol, size: .sm)
                    Text(text.overCapacity(over)).monarchTextStyle(.caption)
                }
                .foregroundStyle(theme.colors[role: MonarchBacklogPlannerTokens.overCapacityRole])
                .accessibilityElement(children: .combine)
            }
        }
        .textCase(nil)
    }

    private func muted(_ string: String) -> some View {
        Text(string)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors[role: MonarchBacklogPlannerTokens.muted])
    }

    // MARK: Moving

    private func allows(_ item: Item, _ toBucketID: String, _ fromBucketID: String) -> Bool {
        toBucketID == fromBucketID || canMove?(item, toBucketID, fromBucketID) ?? true
    }

    private func expandedBinding(_ id: String) -> Binding<Bool> {
        Binding(
            get: { !collapsed.wrappedValue.contains(id) },
            set: { isExpanded in
                if isExpanded { collapsed.wrappedValue.remove(id) } else { collapsed.wrappedValue.insert(id) }
            }
        )
    }

    /// The drag payload of an item: its id as text.
    static func payload(_ item: Item) -> String {
        String(describing: item.id)
    }

    private func drop(_ payload: String, into bucketID: String, at index: Int) {
        guard let from = buckets.flatMap(\.items).first(where: { Self.payload($0) == payload }),
            let location = MonarchBacklogPlannerTokens.location(of: from.id, in: buckets),
            allows(from, bucketID, buckets[location.bucket].id)
        else { return }
        let sameBucket = buckets[location.bucket].id == bucketID
        let target = sameBucket && index > location.index ? index - 1 : index
        move(from, to: bucketID, at: target)
    }

    private func move(_ item: Item, to bucketID: String, at index: Int) {
        guard let location = MonarchBacklogPlannerTokens.location(of: item.id, in: buckets),
            allows(item, bucketID, buckets[location.bucket].id),
            let result = MonarchBacklogPlannerTokens.moving(
                buckets,
                itemID: item.id,
                toBucketID: bucketID,
                toIndex: index
            )
        else { return }
        commit(result)
    }

    private func commit(_ result: (buckets: [MonarchBacklogBucket<Item>], move: MonarchBacklogMove<Item.ID>)) {
        buckets = result.buckets
        if let bucket = result.buckets.first(where: { $0.id == result.move.toBucketID }),
            let item = bucket.items.first(where: { $0.id == result.move.itemID })
        {
            let message = text.moved(
                itemLabel(item),
                bucket: bucket.title,
                position: result.move.toIndex + 1,
                count: bucket.items.count
            )
            AccessibilityNotification.Announcement(message).post()
        }
        onMove?(result.move)
    }
}

#if DEBUG
    private struct MonarchBacklogPlannerPreviewItem: Identifiable {
        let id: String
        let title: String
        let points: Int?
    }

    #Preview("MonarchBacklogPlanner") {
        MonarchPreviewMatrix {
            MonarchBacklogPlanner(
                buckets: .constant([
                    MonarchBacklogBucket(
                        id: "s14",
                        title: "Sprint 14",
                        subtitle: "Mar 3 – Mar 14",
                        badge: "Active",
                        capacity: 5,
                        items: [
                            MonarchBacklogPlannerPreviewItem(id: "P-1", title: "Sign-in with passkeys", points: 5),
                            MonarchBacklogPlannerPreviewItem(id: "P-2", title: "Fix export", points: 2),
                        ]
                    ),
                    MonarchBacklogBucket(
                        id: "backlog",
                        title: "Backlog",
                        items: [MonarchBacklogPlannerPreviewItem(id: "P-3", title: "Audit log", points: nil)]
                    ),
                    MonarchBacklogBucket(id: "s15", title: "Sprint 15", items: []),
                ]),
                itemLabel: { $0.id },
                points: { $0.points },
                row: { item in Text(item.title).monarchTextStyle(.body) }
            )
            .frame(minHeight: 500)
        }
    }
#endif
