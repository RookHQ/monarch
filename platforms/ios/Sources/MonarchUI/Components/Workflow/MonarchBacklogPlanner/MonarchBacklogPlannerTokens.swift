import Foundation
import MonarchTokens
import SwiftUI

/// The resolved strings of ``MonarchBacklogPlanner``: the app's replacements, else the built-in ones.
struct MonarchBacklogPlannerText {
    let labels: MonarchBacklogPlannerLabels

    private func pick(_ value: String?, _ key: String) -> String {
        value ?? MonarchBacklogPlannerStrings.string(key)
    }

    var capacityLabel: String { pick(labels.capacityLabel, "capacityLabel") }
    var rowUnestimated: String { pick(labels.rowUnestimated, "rowUnestimated") }
    var notEstimated: String { pick(labels.notEstimated, "notEstimated") }
    var moveTo: String { pick(labels.moveTo, "moveTo") }
    var moveUp: String { pick(labels.moveUp, "moveUp") }
    var moveDown: String { pick(labels.moveDown, "moveDown") }
    var empty: String { pick(labels.empty, "empty") }
    var expanded: String { pick(labels.expanded, "expanded") }
    var collapsed: String { pick(labels.collapsed, "collapsed") }
    var toggleHint: String { MonarchBacklogPlannerStrings.string("toggleHint") }

    /// "5 items · 13 points · 1 unestimated"; points are left out when `nil`, unestimated when 0.
    func summary(count: Int, points: Int?, unestimated: Int) -> String {
        if let custom = labels.summary { return custom(count, points, unestimated) }
        var parts = [MonarchBacklogPlannerStrings.plural("items", count)]
        if let points { parts.append(MonarchBacklogPlannerStrings.plural("points", points)) }
        if unestimated > 0 { parts.append(MonarchBacklogPlannerStrings.string("unestimated", unestimated)) }
        return parts.joined(separator: " · ")
    }

    func capacity(points: Int, capacity: Int) -> String {
        labels.capacity?(points, capacity) ?? MonarchBacklogPlannerStrings.string("capacity", points, capacity)
    }

    func overCapacity(_ over: Int) -> String {
        labels.overCapacity?(over) ?? MonarchBacklogPlannerStrings.plural("overCapacity", over)
    }

    func rowPoints(_ points: Int?) -> String {
        guard let points else { return rowUnestimated }
        return labels.rowPoints?(points) ?? MonarchBacklogPlannerStrings.string("rowPoints", points)
    }

    func spokenPoints(_ points: Int?) -> String {
        guard let points else { return notEstimated }
        return MonarchBacklogPlannerStrings.string("rowPointsSpoken", points)
    }

    func moveMenu(_ item: String) -> String {
        labels.moveMenu?(item) ?? MonarchBacklogPlannerStrings.string("moveMenu", item)
    }

    func moveToBucket(_ bucket: String) -> String {
        labels.moveToBucket?(bucket) ?? MonarchBacklogPlannerStrings.string("moveToBucket", bucket)
    }

    func moved(_ item: String, bucket: String, position: Int, count: Int) -> String {
        labels.moved?(item, bucket, position, count)
            ?? MonarchBacklogPlannerStrings.string("moved", item, bucket, position, count)
    }
}

/// Component tokens and move logic for ``MonarchBacklogPlanner``.
enum MonarchBacklogPlannerTokens {
    /// The gap between the lines of a bucket header.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the parts of a header line or a row.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The symbol of the move menu.
    static let moveSymbol = "ellipsis"
    /// The symbol of an open bucket.
    static let expandedSymbol = "chevron.down"
    /// The symbol of a closed bucket. Mirrors in right-to-left layouts.
    static let collapsedSymbol = "chevron.forward"
    /// The symbol of the over-capacity note.
    static let overCapacitySymbol = "exclamationmark.triangle.fill"
    /// The role of quiet header text and row points.
    static var muted: MonarchColorRole { \.textMuted }
    /// The role of the over-capacity note.
    static var overCapacityRole: MonarchColorRole { \.warningStrong }

    /// Where an item is: its bucket's position and its own.
    static func location<Item: Identifiable>(
        of itemID: Item.ID,
        in buckets: [MonarchBacklogBucket<Item>]
    ) -> (bucket: Int, index: Int)? {
        for (b, bucket) in buckets.enumerated() {
            if let index = bucket.items.firstIndex(where: { $0.id == itemID }) { return (b, index) }
        }
        return nil
    }

    /// The buckets with an item at a new place: taken out, then put at `toIndex` (clamped) of the
    /// target bucket. `nil` when the item or bucket is unknown or nothing changes.
    static func moving<Item: Identifiable>(
        _ buckets: [MonarchBacklogBucket<Item>],
        itemID: Item.ID,
        toBucketID: String,
        toIndex: Int
    ) -> (buckets: [MonarchBacklogBucket<Item>], move: MonarchBacklogMove<Item.ID>)? {
        guard let from = location(of: itemID, in: buckets),
            let target = buckets.firstIndex(where: { $0.id == toBucketID })
        else { return nil }
        var next = buckets
        let item = next[from.bucket].items.remove(at: from.index)
        let index = max(0, min(toIndex, next[target].items.count))
        if target == from.bucket && index == from.index { return nil }
        next[target].items.insert(item, at: index)
        let move = MonarchBacklogMove(
            itemID: itemID,
            fromBucketID: buckets[from.bucket].id,
            fromIndex: from.index,
            toBucketID: toBucketID,
            toIndex: index
        )
        return (next, move)
    }

    /// The move for `List`'s `onMove`: `destination` counts places before the rows are taken out.
    static func reordering<Item: Identifiable>(
        _ buckets: [MonarchBacklogBucket<Item>],
        bucketID: String,
        from source: IndexSet,
        to destination: Int
    ) -> (buckets: [MonarchBacklogBucket<Item>], move: MonarchBacklogMove<Item.ID>)? {
        guard let b = buckets.firstIndex(where: { $0.id == bucketID }), let first = source.first,
            source.count == 1, first < buckets[b].items.count
        else { return nil }
        let toIndex = destination > first ? destination - 1 : destination
        return moving(buckets, itemID: buckets[b].items[first].id, toBucketID: bucketID, toIndex: toIndex)
    }

    /// One place up or down. At the end of a bucket the item goes to the end of the previous
    /// bucket, or the start of the next, skipping buckets that refuse it. `nil` at the very ends.
    static func step<Item: Identifiable>(
        _ buckets: [MonarchBacklogBucket<Item>],
        itemID: Item.ID,
        up: Bool,
        allows: (_ item: Item, _ toBucketID: String, _ fromBucketID: String) -> Bool
    ) -> (bucketID: String, index: Int)? {
        guard let from = location(of: itemID, in: buckets) else { return nil }
        let bucket = buckets[from.bucket]
        if up, from.index > 0 { return (bucket.id, from.index - 1) }
        if !up, from.index < bucket.items.count - 1 { return (bucket.id, from.index + 1) }
        let item = bucket.items[from.index]
        let order = up ? Array((0..<from.bucket).reversed()) : Array((from.bucket + 1)..<buckets.count)
        for b in order where allows(item, buckets[b].id, bucket.id) {
            return (buckets[b].id, up ? buckets[b].items.count : 0)
        }
        return nil
    }

    /// The count, the points (`nil` without a points function) and how many have no estimate.
    static func totals<Item>(_ items: [Item], points: ((Item) -> Int?)?) -> (count: Int, points: Int?, unestimated: Int)
    {
        guard let points else { return (items.count, nil, 0) }
        let estimates = items.map(points)
        return (items.count, estimates.compactMap { $0 }.reduce(0, +), estimates.filter { $0 == nil }.count)
    }

    /// The points over capacity, or `nil` when within it or with no capacity.
    static func overCapacity(points: Int?, capacity: Int?) -> Int? {
        guard let points, let capacity, points > capacity else { return nil }
        return points - capacity
    }

    /// The share of the capacity meter that is filled, 0 to 1.
    static func capacityFraction(points: Int, capacity: Int) -> Double {
        guard capacity > 0 else { return points > 0 ? 1 : 0 }
        return min(Double(points) / Double(capacity), 1)
    }
}
