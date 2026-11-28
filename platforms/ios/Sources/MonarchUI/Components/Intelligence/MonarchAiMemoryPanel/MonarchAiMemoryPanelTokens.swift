import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiMemoryPanel``.
enum MonarchAiMemoryPanelTokens {
    /// The gap between a memory and its updated line.
    static let lineGap: CGFloat = MonarchTokens.Space.space1
    /// The gap between a group's name and its kind and count.
    static let headerGap: CGFloat = MonarchTokens.Space.space0_5
    /// How long the search waits before announcing the number of matches, as on the web.
    static let searchDebounce: Duration = .milliseconds(300)
    /// The SF Symbol of the forget commands.
    static let forgetSymbol = "trash"
    /// The colour role of a group's name.
    static var headingRole: MonarchColorRole { \.textDefault }
    /// The colour role of secondary text.
    static var mutedRole: MonarchColorRole { \.textMuted }

    /// One group of the panel: an entity and its memories, most recently updated first.
    struct Group: Identifiable, Equatable {
        let entity: MonarchAiMemoryEntity
        let items: [MonarchAiMemoryItem]

        var id: String { entity.kind.rawValue + ":" + entity.name }
    }

    /// Whether a memory matches the search by its text or its entity's name,
    /// ignoring case. An empty search matches.
    static func matches(_ memory: MonarchAiMemoryItem, query: String, locale: Locale) -> Bool {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return true }
        return [memory.text, memory.entity.name].contains { field in
            field.range(of: trimmed, options: .caseInsensitive, locale: locale) != nil
        }
    }

    /// The memories that match the search, grouped by entity: people, then
    /// organizations, projects and other, each by name in the locale's
    /// order, with the most recently updated memory first in each group.
    static func groups(_ memories: [MonarchAiMemoryItem], query: String, locale: Locale) -> [Group] {
        let order = MonarchAiMemoryEntityKind.allCases
        var byEntity: [MonarchAiMemoryEntity: [MonarchAiMemoryItem]] = [:]
        var entities: [MonarchAiMemoryEntity] = []
        for memory in memories where matches(memory, query: query, locale: locale) {
            if byEntity[memory.entity] == nil { entities.append(memory.entity) }
            byEntity[memory.entity, default: []].append(memory)
        }
        let sorted = entities.sorted { lhs, rhs in
            let left = order.firstIndex(of: lhs.kind) ?? order.count
            let right = order.firstIndex(of: rhs.kind) ?? order.count
            if left != right { return left < right }
            return lhs.name.compare(rhs.name, options: [.caseInsensitive], range: nil, locale: locale)
                == .orderedAscending
        }
        return sorted.map { entity in
            Group(entity: entity, items: (byEntity[entity] ?? []).sorted { $0.updatedAt > $1.updatedAt })
        }
    }

    /// The ids of every memory shown, in the order shown.
    static func shownIDs(_ groups: [Group]) -> [String] {
        groups.flatMap { $0.items.map(\.id) }
    }

    /// The selected ids that are still shown, in the order shown.
    static func selected(_ selection: Set<String>, groups: [Group]) -> [String] {
        shownIDs(groups).filter(selection.contains)
    }

    /// Whether every shown memory is selected, so Select all becomes Deselect all.
    static func isAllSelected(_ selection: Set<String>, groups: [Group]) -> Bool {
        let shown = shownIDs(groups)
        return !shown.isEmpty && shown.allSatisfy(selection.contains)
    }

    /// The title of the select-all button.
    static func selectAllTitle(isAllSelected: Bool, shown: Int, labels: MonarchAiMemoryPanelLabels) -> String {
        isAllSelected ? labels.deselectAll : labels.selectAll(shown)
    }

    /// The kind and count under a group's name, such as "Person · 3 memories".
    static func groupDetail(_ group: Group, labels: MonarchAiMemoryPanelLabels) -> String {
        labels.kind(group.entity.kind) + " \u{00B7} " + labels.count(group.items.count)
    }

    /// The line under a memory: "Updated Mar 10, 2026" in the locale and time zone.
    static func updatedLine(
        _ date: Date,
        locale: Locale,
        timeZone: TimeZone,
        labels: MonarchAiMemoryPanelLabels
    ) -> String {
        let style = Date.FormatStyle(date: .abbreviated, time: .omitted, locale: locale, timeZone: timeZone)
        return labels.updated(date.formatted(style))
    }

    /// What to announce after the search settles: the number of matches, or
    /// nothing when the search is empty.
    static func searchAnnouncement(query: String, count: Int, labels: MonarchAiMemoryPanelLabels) -> String? {
        query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : labels.matches(count)
    }

    /// The memory as the ``MonarchAiMemoryChip`` row shows it.
    static func part(_ memory: MonarchAiMemoryItem) -> MonarchAiMemoryPart {
        MonarchAiMemoryPart(id: memory.id, action: .write, text: memory.text, scope: memory.entity.name)
    }
}
