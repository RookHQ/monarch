/// The roadmap's own strings, resolved with the app's replacements. Pure, so
/// tests read every string.
struct MonarchRoadmapText: Sendable {
    let labels: MonarchRoadmapLabels

    func health(_ health: MonarchRoadmapHealth) -> String {
        labels.health?(health) ?? MonarchRoadmapStrings.health(health)
    }

    func count(_ count: Int) -> String {
        labels.count?(count) ?? MonarchRoadmapStrings.count(count)
    }

    func meta(_ first: String, _ second: String) -> String {
        labels.meta?(first, second) ?? MonarchRoadmapStrings.meta(first, second)
    }
}

/// The roadmap as Gantt timeline rows, and the projects by id for the callbacks.
struct MonarchRoadmapRows {
    let items: [MonarchGanttTimelineItem]
    let groups: [MonarchGanttTimelineGroup]
    let byID: [String: MonarchRoadmapItem]
}

/// Pure mapping from roadmap projects and initiatives to Gantt timeline rows.
enum MonarchRoadmapRules {
    /// The second line of a project heading: the progress text and the health
    /// in words, or `nil` for neither.
    static func itemMeta(_ item: MonarchRoadmapItem, text: MonarchRoadmapText) -> String? {
        let parts = [item.progressText, item.health.map(text.health)].compactMap { $0 }
        guard let first = parts.first else { return nil }
        return parts.dropFirst().reduce(first) { text.meta($0, $1) }
    }

    /// The second line of a group heading: the number of projects, then the
    /// health in words when given.
    static func groupMeta(_ group: MonarchRoadmapGroup, count: Int, text: MonarchRoadmapText) -> String {
        let counted = text.count(count)
        guard let health = group.health else { return counted }
        return text.meta(counted, text.health(health))
    }

    /// Maps projects and groups to the timeline's rows. Every project keeps
    /// its own dates and order; the first project with an id wins for the callbacks.
    static func rows(
        _ items: [MonarchRoadmapItem],
        groups: [MonarchRoadmapGroup],
        text: MonarchRoadmapText
    ) -> MonarchRoadmapRows {
        var perGroup: [String: Int] = [:]
        for groupID in items.compactMap(\.groupID) {
            perGroup[groupID, default: 0] += 1
        }
        return MonarchRoadmapRows(
            items: items.map { item in
                MonarchGanttTimelineItem(
                    id: item.id,
                    label: item.label,
                    start: item.start,
                    end: item.end,
                    groupID: item.groupID,
                    progress: item.progress,
                    tone: MonarchRoadmapTokens.barTone(item.tone, health: item.health),
                    meta: itemMeta(item, text: text)
                )
            },
            groups: groups.map { group in
                MonarchGanttTimelineGroup(
                    id: group.id,
                    label: group.label,
                    start: group.start,
                    end: group.end,
                    meta: groupMeta(group, count: perGroup[group.id] ?? 0, text: text)
                )
            },
            byID: Dictionary(items.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        )
    }

    /// The timeline's labels with the roadmap's chart name and row header
    /// where the app gave none.
    static func timelineLabels(_ labels: MonarchGanttTimelineLabels) -> MonarchGanttTimelineLabels {
        var merged = labels
        merged.timeline = labels.timeline ?? MonarchRoadmapStrings.timeline
        merged.rowHeader = labels.rowHeader ?? MonarchRoadmapStrings.rowHeader
        return merged
    }

    /// The timeline's change callback, which reports the roadmap project the
    /// app passed instead of the timeline row.
    static func change(
        _ onChange: (@MainActor (MonarchRoadmapItem, MonarchGanttTimelineDates) -> Void)?,
        rows: MonarchRoadmapRows
    ) -> (@MainActor (MonarchGanttTimelineItem, MonarchGanttTimelineDates) -> Void)? {
        guard let onChange else { return nil }
        let byID = rows.byID
        return { row, dates in
            if let item = byID[row.id] { onChange(item, dates) }
        }
    }

    /// The timeline's open callback, which reports the roadmap project.
    static func open(
        _ onOpen: (@MainActor (MonarchRoadmapItem) -> Void)?,
        rows: MonarchRoadmapRows
    ) -> (@MainActor (MonarchGanttTimelineItem) -> Void)? {
        guard let onOpen else { return nil }
        let byID = rows.byID
        return { row in
            if let item = byID[row.id] { onOpen(item) }
        }
    }
}
