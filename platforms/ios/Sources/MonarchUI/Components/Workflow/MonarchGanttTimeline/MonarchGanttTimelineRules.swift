import CoreGraphics
import Foundation

/// A span of whole days, first day before or equal to the last.
struct MonarchGanttTimelineSpan: Hashable, Sendable {
    let start: Int
    let end: Int

    /// The number of days, counting both ends.
    var days: Int { end - start + 1 }
}

/// One row of the chart and the list.
enum MonarchGanttTimelineLine: Hashable, Sendable {
    /// A group heading.
    case group(MonarchGanttTimelineGroup)
    /// An item at a nesting depth, with its number of direct children and,
    /// when it has no dates of its own, the span of its children.
    case item(MonarchGanttTimelineItem, depth: Int, children: Int, rollup: MonarchGanttTimelineSpan?)

    /// A key unique among the rows.
    var key: String {
        switch self {
        case .group(let group): "g:\(group.id)"
        case .item(let item, _, _, _): "i:\(item.id)"
        }
    }
}

/// The rows and the top-level items without dates that are listed apart.
struct MonarchGanttTimelineLines: Hashable, Sendable {
    let lines: [MonarchGanttTimelineLine]
    let unscheduled: [MonarchGanttTimelineItem]
}

/// A month label of the header: its day and whether it carries the year.
struct MonarchGanttTimelineTick: Hashable, Sendable {
    let day: Int
    let withYear: Bool
}

/// The month labels and the day or week labels of the header.
struct MonarchGanttTimelineTicks: Hashable, Sendable {
    let major: [MonarchGanttTimelineTick]
    let minor: [Int]
}

/// The pure rules of ``MonarchGanttTimeline``: rows, the window, ticks,
/// dependency paths and gestures.
enum MonarchGanttTimelineRules {
    /// How many days before today the automatic window starts, before going back to the start of the week.
    static let leadDays = 7
    /// Months between month labels at the year zoom.
    static let quarterMonths = 3
    /// A bar narrower than this many edge zones moves as a whole.
    static let edgeZones: CGFloat = 3

    /// The span of two optional dates, or `nil` without either. Reversed dates are swapped.
    static func span(_ start: Date?, _ end: Date?, days: MonarchGanttTimelineDays) -> MonarchGanttTimelineSpan? {
        guard let first = (start ?? end).map(days.day), let last = (end ?? start).map(days.day) else { return nil }
        return MonarchGanttTimelineSpan(start: min(first, last), end: max(first, last))
    }

    /// Whether only one of the two dates is set: a one-day marker.
    static func isPoint(_ dates: MonarchGanttTimelineDates) -> Bool {
        (dates.start == nil) != (dates.end == nil)
    }

    /// Groups first with their items, then the items of no known group;
    /// children under their parents unless the parent is collapsed. Top-level
    /// items without dates, children or a rollup go to the unscheduled list
    /// with `.list`. Cycles and unknown parents are ignored.
    static func lines(
        _ items: [MonarchGanttTimelineItem],
        groups: [MonarchGanttTimelineGroup],
        collapsed: Set<String>,
        undated: MonarchGanttTimelineUndated,
        days: MonarchGanttTimelineDays
    ) -> MonarchGanttTimelineLines {
        var builder = MonarchGanttTimelineLineBuilder(
            items: items,
            collapsed: collapsed,
            listsUndated: undated == .list,
            days: days
        )
        let top = items.filter(builder.isTopLevel)
        let groupIDs = Set(groups.map(\.id))
        for group in groups {
            builder.out.append(.group(group))
            for item in top where item.groupID == group.id {
                builder.visit(item, depth: 0, seen: [item.id])
            }
        }
        for item in top where item.groupID.map({ !groupIDs.contains($0) }) ?? true {
            builder.visit(item, depth: 0, seen: [item.id])
        }
        return MonarchGanttTimelineLines(lines: builder.out, unscheduled: builder.unscheduled)
    }

    /// The first visible day: `start` when set, otherwise the start of the
    /// week a week before `today`, otherwise before the earliest item,
    /// otherwise the week of 1 January 1970.
    static func anchor(
        start: Date?,
        today: Int?,
        items: [MonarchGanttTimelineItem],
        days: MonarchGanttTimelineDays
    ) -> Int {
        if let start { return days.day(start) }
        let base = today ?? items.compactMap { span($0.start, $0.end, days: days)?.start }.min()
        return days.startOfWeek((base ?? leadDays) - leadDays)
    }

    /// The window that shows today: the start of the week a week before it.
    static func todayAnchor(_ today: Int, days: MonarchGanttTimelineDays) -> Int {
        days.startOfWeek(today - leadDays)
    }

    /// The month labels (every month, every third month at the year zoom) and
    /// the day (week zoom) or week (month zoom) labels.
    static func ticks(anchor: Int, zoom: MonarchGanttTimelineZoom, days: MonarchGanttTimelineDays)
        -> MonarchGanttTimelineTicks
    {
        var major: [MonarchGanttTimelineTick] = []
        var minor: [Int] = []
        let yearly = zoom == .quarter || zoom == .year
        for offset in 0..<MonarchGanttTimelineTokens.span(zoom) {
            let day = anchor + offset
            let parts = days.dayAndMonth(day)
            let first = offset == 0
            let shown = first || (parts.day == 1 && (zoom != .year || (parts.month - 1) % quarterMonths == 0))
            if shown {
                major.append(MonarchGanttTimelineTick(day: day, withYear: first || parts.month == 1 || yearly))
            }
            switch zoom {
            case .week: minor.append(day)
            case .month: if days.isStartOfWeek(day) { minor.append(day) }
            case .quarter, .year: break
            }
        }
        return MonarchGanttTimelineTicks(major: major, minor: minor)
    }

    /// The polyline of a dependency arrow, left to right, ending at the
    /// arrow's tip. `gap` is how far the line leaves a bar before turning,
    /// `tip` the space before the target bar.
    static func dependencyPath(
        kind: MonarchGanttTimelineDependencyKind,
        from: MonarchGanttTimelineSpan,
        to: MonarchGanttTimelineSpan,
        fromY: CGFloat,
        toY: CGFloat,
        scale: MonarchGanttTimelineScale,
        gap: CGFloat,
        tip: CGFloat
    ) -> [CGPoint] {
        let fromX = kind == .startToStart ? scale.x(from.start) : scale.x(from.end) + scale.dayWidth
        let toX = kind == .finishToFinish ? scale.x(to.end) + scale.dayWidth : scale.x(to.start)
        if kind == .finishToFinish {
            let middle = max(fromX, toX) + gap
            return [
                CGPoint(x: fromX, y: fromY), CGPoint(x: middle, y: fromY),
                CGPoint(x: middle, y: toY), CGPoint(x: toX + tip, y: toY),
            ]
        }
        let middle = min(fromX + gap, toX - gap)
        if middle > fromX {
            return [
                CGPoint(x: fromX, y: fromY), CGPoint(x: middle, y: fromY),
                CGPoint(x: middle, y: toY), CGPoint(x: toX - tip, y: toY),
            ]
        }
        let half = (fromY + toY) / 2
        let back = toX - gap - tip
        return [
            CGPoint(x: fromX, y: fromY), CGPoint(x: fromX + gap, y: fromY),
            CGPoint(x: fromX + gap, y: half), CGPoint(x: back, y: half),
            CGPoint(x: back, y: toY), CGPoint(x: toX - tip, y: toY),
        ]
    }

    /// Which dates a drag that starts `x` points into a bar `width` wide
    /// changes: an end, or the whole bar.
    static func dragShift(x: CGFloat, width: CGFloat, edge: CGFloat, isPoint: Bool) -> MonarchGanttTimelineShift {
        if isPoint || width < edge * edgeZones { return .move }
        if x < edge { return .start }
        if x > width - edge { return .end }
        return .move
    }

    /// Whole days a drag of `distance` points covers.
    static func dragDays(_ distance: CGFloat, dayWidth: CGFloat) -> Int {
        guard dayWidth > 0 else { return 0 }
        return Int((distance / dayWidth).rounded())
    }

    /// The text a row shows instead of a bar: no dates, ended before the
    /// window or starting after it. `nil` when the bar is in the window.
    static func rowNote(
        span: MonarchGanttTimelineSpan?,
        hasRollup: Bool,
        anchor: Int,
        rangeEnd: Int,
        formats: MonarchGanttTimelineFormats,
        text: MonarchGanttTimelineText
    ) -> String? {
        guard let span else { return hasRollup ? nil : text.noDates }
        if span.end < anchor { return text.ended(formats.date(span.end)) }
        if span.start > rangeEnd { return text.starts(formats.date(span.start)) }
        return nil
    }
}

/// The horizontal scale of the grid: the first visible day and the width of a day.
struct MonarchGanttTimelineScale: Hashable, Sendable {
    let anchor: Int
    let dayWidth: CGFloat

    /// The left edge of `day`.
    func x(_ day: Int) -> CGFloat { CGFloat(day - anchor) * dayWidth }
}

/// A bar being dragged: which item, which dates move and by how many days so far.
struct MonarchGanttTimelineDrag: Hashable, Sendable {
    let id: String
    let shift: MonarchGanttTimelineShift
    let days: Int

    /// The dates of `item` as shown: moved while it is being dragged.
    static func shown(
        _ item: MonarchGanttTimelineItem,
        drag: MonarchGanttTimelineDrag?,
        calendar: Calendar
    ) -> MonarchGanttTimelineDates {
        guard let drag, drag.id == item.id else { return item.dates }
        return item.dates.shifted(drag.shift, by: drag.days, in: calendar)
    }
}

/// Walks the items into rows for ``MonarchGanttTimelineRules/lines(_:groups:collapsed:undated:days:)``.
private struct MonarchGanttTimelineLineBuilder {
    let collapsed: Set<String>
    let listsUndated: Bool
    let days: MonarchGanttTimelineDays
    let ids: Set<String>
    var children: [String: [MonarchGanttTimelineItem]] = [:]
    var out: [MonarchGanttTimelineLine] = []
    var unscheduled: [MonarchGanttTimelineItem] = []

    init(items: [MonarchGanttTimelineItem], collapsed: Set<String>, listsUndated: Bool, days: MonarchGanttTimelineDays)
    {
        self.collapsed = collapsed
        self.listsUndated = listsUndated
        self.days = days
        ids = Set(items.map(\.id))
        for item in items where !Self.isTopLevel(item, ids: ids) {
            if let parent = item.parentID { children[parent, default: []].append(item) }
        }
    }

    private static func isTopLevel(_ item: MonarchGanttTimelineItem, ids: Set<String>) -> Bool {
        guard let parent = item.parentID else { return true }
        return !ids.contains(parent) || parent == item.id
    }

    /// An item without a known parent other than itself.
    func isTopLevel(_ item: MonarchGanttTimelineItem) -> Bool {
        Self.isTopLevel(item, ids: ids)
    }

    /// The span of the children of `id` (and theirs, for children without dates), skipping `seen` items.
    func rollup(_ id: String, seen: Set<String>) -> MonarchGanttTimelineSpan? {
        let spans = (children[id] ?? []).filter { !seen.contains($0.id) }.compactMap { child in
            MonarchGanttTimelineRules.span(child.start, child.end, days: days)
                ?? rollup(child.id, seen: seen.union([child.id]))
        }
        guard let first = spans.map(\.start).min(), let last = spans.map(\.end).max() else { return nil }
        return MonarchGanttTimelineSpan(start: first, end: last)
    }

    /// Adds `item` at `depth` and, unless it is collapsed, its children; `seen` guards against cycles.
    mutating func visit(_ item: MonarchGanttTimelineItem, depth: Int, seen: Set<String>) {
        let kids = children[item.id] ?? []
        let own = MonarchGanttTimelineRules.span(item.start, item.end, days: days)
        let roll = own == nil ? rollup(item.id, seen: [item.id]) : nil
        if own == nil && roll == nil && kids.isEmpty && depth == 0 && listsUndated {
            unscheduled.append(item)
            return
        }
        out.append(.item(item, depth: depth, children: kids.count, rollup: roll))
        guard !collapsed.contains(item.id) else { return }
        for kid in kids where !seen.contains(kid.id) {
            visit(kid, depth: depth + 1, seen: seen.union([kid.id]))
        }
    }
}
