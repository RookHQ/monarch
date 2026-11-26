import Foundation
import SwiftUI

/// A VoiceOver action of a bar: its name and the move it makes.
struct MonarchGanttTimelineAction: Hashable, Sendable {
    let name: String
    let shift: MonarchGanttTimelineShift
    let days: Int
}

/// Everything the chart, the list and the toolbar share: the data, the
/// window, the strings and the callbacks. Everything but `commit` is pure.
struct MonarchGanttTimelineModel {
    let items: [MonarchGanttTimelineItem]
    let groups: [MonarchGanttTimelineGroup]
    let dependencies: [MonarchGanttTimelineDependency]
    let milestones: [MonarchGanttTimelineMilestone]
    let lines: MonarchGanttTimelineLines
    let state: MonarchGanttTimelineState
    let anchor: Int
    let today: Int?
    let days: MonarchGanttTimelineDays
    let formats: MonarchGanttTimelineFormats
    let text: MonarchGanttTimelineText
    let onChange: (@MainActor (MonarchGanttTimelineItem, MonarchGanttTimelineDates) -> Void)?
    let onOpen: (@MainActor (MonarchGanttTimelineItem) -> Void)?
    let setState: @MainActor (MonarchGanttTimelineState) -> Void
    let itemMeta: (@MainActor (MonarchGanttTimelineItem) -> AnyView)?
    let groupMeta: (@MainActor (MonarchGanttTimelineGroup) -> AnyView)?

    /// Builds the model from the timeline's inputs.
    static func make(
        items: [MonarchGanttTimelineItem],
        groups: [MonarchGanttTimelineGroup] = [],
        dependencies: [MonarchGanttTimelineDependency] = [],
        milestones: [MonarchGanttTimelineMilestone] = [],
        state: MonarchGanttTimelineState = MonarchGanttTimelineState(),
        undated: MonarchGanttTimelineUndated = .list,
        today: Date?,
        calendar: Calendar,
        locale: Locale,
        labels: MonarchGanttTimelineLabels = MonarchGanttTimelineLabels(),
        onChange: (@MainActor (MonarchGanttTimelineItem, MonarchGanttTimelineDates) -> Void)? = nil,
        onOpen: (@MainActor (MonarchGanttTimelineItem) -> Void)? = nil,
        setState: @escaping @MainActor (MonarchGanttTimelineState) -> Void = { _ in },
        itemMeta: (@MainActor (MonarchGanttTimelineItem) -> AnyView)? = nil,
        groupMeta: (@MainActor (MonarchGanttTimelineGroup) -> AnyView)? = nil
    ) -> MonarchGanttTimelineModel {
        let days = MonarchGanttTimelineDays(calendar: calendar)
        let todayDay = today.map(days.day)
        return MonarchGanttTimelineModel(
            items: items,
            groups: groups,
            dependencies: dependencies,
            milestones: milestones,
            lines: MonarchGanttTimelineRules.lines(
                items,
                groups: groups,
                collapsed: state.collapsed,
                undated: undated,
                days: days
            ),
            state: state,
            anchor: MonarchGanttTimelineRules.anchor(start: state.start, today: todayDay, items: items, days: days),
            today: todayDay,
            days: days,
            formats: MonarchGanttTimelineFormats(days: days, locale: locale),
            text: MonarchGanttTimelineText(labels: labels),
            onChange: onChange,
            onOpen: onOpen,
            setState: setState,
            itemMeta: itemMeta,
            groupMeta: groupMeta
        )
    }

    var zoom: MonarchGanttTimelineZoom { state.zoom }

    /// The last visible day.
    var rangeEnd: Int { anchor + MonarchGanttTimelineTokens.span(zoom) - 1 }

    var scale: MonarchGanttTimelineScale {
        MonarchGanttTimelineScale(anchor: anchor, dayWidth: MonarchGanttTimelineTokens.dayWidth(zoom))
    }

    var itemsByID: [String: MonarchGanttTimelineItem] {
        Dictionary(items.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
    }

    /// Milestones that share the lane under the header.
    var laneMilestones: [MonarchGanttTimelineMilestone] { milestones.filter { $0.itemID == nil } }

    /// Whether the timeline can change dates at all.
    var isEditable: Bool { onChange != nil }

    /// Whether `item`'s dates can be changed.
    func isEditable(_ item: MonarchGanttTimelineItem) -> Bool { onChange != nil && item.isEditable }

    /// Whether the window contains today.
    var showsToday: Bool { today.map { $0 >= anchor && $0 <= rangeEnd } ?? false }

    func date(_ day: Int) -> String { formats.date(day) }

    /// The visible window: "Jan 5, 2026 – Apr 5, 2026".
    var range: String { text.range(date(anchor), date(rangeEnd)) }

    /// "60%", or `nil` without progress.
    func percent(_ item: MonarchGanttTimelineItem) -> String? {
        item.progress.map { formats.percent(min(max($0, 0), 1)) }
    }

    func span(_ dates: MonarchGanttTimelineDates) -> MonarchGanttTimelineSpan? {
        MonarchGanttTimelineRules.span(dates.start, dates.end, days: days)
    }

    /// What VoiceOver reads for a bar: the dates, the progress and the conflict.
    func describe(_ item: MonarchGanttTimelineItem, span: MonarchGanttTimelineSpan) -> String {
        let bar = text.bar(item.label, start: date(span.start), end: date(span.end), progress: percent(item))
        guard let conflict = item.conflict else { return bar }
        return "\(bar). \(text.conflict(conflict))"
    }

    /// The labels of the items `item` depends on, in the order of the dependencies.
    func dependsOn(_ item: MonarchGanttTimelineItem) -> [String] {
        let byID = itemsByID
        return dependencies.filter { $0.to == item.id }.compactMap { byID[$0.from]?.label }
    }

    /// What VoiceOver reads for a milestone.
    func describe(_ milestone: MonarchGanttTimelineMilestone) -> String {
        text.milestone(milestone.label, date: date(days.day(milestone.date)), isDone: milestone.isDone)
    }

    /// The written dates of an item: one day, a range, or "No dates".
    func writtenDates(_ item: MonarchGanttTimelineItem) -> String {
        guard let span = span(item.dates) else { return text.noDates }
        return span.start == span.end ? date(span.start) : text.range(date(span.start), date(span.end))
    }

    /// The written facts of an item in the list: dates, then length, progress
    /// and meta, then dependencies. The conflict is written apart.
    func facts(_ item: MonarchGanttTimelineItem) -> [String] {
        let span = span(item.dates)
        let details = [span.map { text.duration($0.days) }, percent(item).map(text.progress), item.meta]
            .compactMap { $0 }
        let names = dependsOn(item)
        return [
            writtenDates(item),
            details.isEmpty ? nil : details.joined(separator: " · "),
            names.isEmpty ? nil : text.dependsOn(names.joined(separator: ", ")),
        ].compactMap { $0 }
    }

    /// The new dates to report, or `nil` when nothing changes or the item is locked.
    func change(_ item: MonarchGanttTimelineItem, to dates: MonarchGanttTimelineDates) -> MonarchGanttTimelineDates? {
        guard isEditable(item) else { return nil }
        let normalized = MonarchGanttTimelineDates(
            start: dates.start.map(days.calendar.startOfDay(for:)),
            end: dates.end.map(days.calendar.startOfDay(for:))
        )
        let before = item.dates.shifted(.move, by: 0, in: days.calendar)
        return normalized == before ? nil : normalized
    }

    /// What is announced after `item` gets `dates`.
    func announcement(_ item: MonarchGanttTimelineItem, dates: MonarchGanttTimelineDates) -> String? {
        span(dates).map { text.changed(item.label, start: date($0.start), end: date($0.end)) }
    }

    /// Reports new dates for `item` and announces them. Does nothing when they did not change or it is locked.
    @MainActor func commit(_ item: MonarchGanttTimelineItem, dates: MonarchGanttTimelineDates) {
        guard let onChange, let next = change(item, to: dates) else { return }
        onChange(item, next)
        if let message = announcement(item, dates: next) {
            AccessibilityNotification.Announcement(message).post()
        }
    }

    /// Moves `item` by `days` with `shift`.
    @MainActor func shift(_ item: MonarchGanttTimelineItem, _ shift: MonarchGanttTimelineShift, by count: Int) {
        commit(item, dates: item.dates.shifted(shift, by: count, in: days.calendar))
    }

    /// The VoiceOver actions of an editable bar: move it, and for a bar of two dates, move either end.
    func actions(_ item: MonarchGanttTimelineItem, isPoint: Bool) -> [MonarchGanttTimelineAction] {
        guard isEditable(item) else { return [] }
        let move = [
            MonarchGanttTimelineAction(name: text.moveEarlier, shift: .move, days: -1),
            MonarchGanttTimelineAction(name: text.moveLater, shift: .move, days: 1),
        ]
        if isPoint { return move }
        return move + [
            MonarchGanttTimelineAction(name: text.startEarlier, shift: .start, days: -1),
            MonarchGanttTimelineAction(name: text.startLater, shift: .start, days: 1),
            MonarchGanttTimelineAction(name: text.endEarlier, shift: .end, days: -1),
            MonarchGanttTimelineAction(name: text.endLater, shift: .end, days: 1),
        ]
    }

    /// The state with the window moved one step of the zoom later, or earlier.
    func stepped(forward: Bool) -> MonarchGanttTimelineState {
        let step = MonarchGanttTimelineTokens.step(zoom)
        var next = state
        next.start = days.date(anchor + (forward ? step : -step))
        return next
    }

    /// The state with the window on the week before today, or `nil` without today.
    var todayState: MonarchGanttTimelineState? {
        guard let today else { return nil }
        var next = state
        next.start = days.date(MonarchGanttTimelineRules.todayAnchor(today, days: days))
        return next
    }

    /// Sets the state through `transform`.
    @MainActor func update(_ transform: (inout MonarchGanttTimelineState) -> Void) {
        var next = state
        transform(&next)
        setState(next)
    }
}
