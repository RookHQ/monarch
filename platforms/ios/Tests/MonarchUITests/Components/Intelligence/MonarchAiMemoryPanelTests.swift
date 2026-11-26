import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

/// Shared sample data for the memory panel fixtures and tests.
private enum MemorySample {
    static let now = Date(timeIntervalSince1970: 1_763_100_000)
    static let day: TimeInterval = 86_400
    static let ada = MonarchAiMemoryEntity(kind: .person, name: "Ada Lovelace")
    static let grace = MonarchAiMemoryEntity(kind: .person, name: "Grace Hopper")
    static let rook = MonarchAiMemoryEntity(kind: .organization, name: "Rook")
    static let launch = MonarchAiMemoryEntity(kind: .project, name: "Launch")
    static let general = MonarchAiMemoryEntity(kind: .other, name: "General")

    static let memories = [
        MonarchAiMemoryItem(id: "1", text: "Prefers summaries under 200 words", entity: ada, updatedAt: now - day),
        MonarchAiMemoryItem(id: "2", text: "Works in Lisbon", entity: ada, updatedAt: now),
        MonarchAiMemoryItem(id: "3", text: "Fiscal year starts in April", entity: rook, updatedAt: now - 3 * day),
        MonarchAiMemoryItem(id: "4", text: "Ships on Mondays", entity: launch, updatedAt: now - 9 * day),
        MonarchAiMemoryItem(id: "5", text: "Writes in British English", entity: general, updatedAt: now - 30 * day),
        MonarchAiMemoryItem(id: "6", text: "Reviews code on Fridays", entity: grace, updatedAt: now - 2 * day),
    ]

    @MainActor static func panel(_ memories: [MonarchAiMemoryItem] = memories) -> some View {
        NavigationStack {
            MonarchAiMemoryPanel(memories, onEdit: { _, _ in }, onForget: { _ in }, onForgetMany: { _ in })
                .navigationTitle("Memories")
        }
        .environment(\.locale, Locale(identifier: "en_US"))
        .environment(\.timeZone, TimeZone(identifier: "UTC") ?? .current)
        .frame(height: 560)
    }
}

enum MonarchAiMemoryPanelFixtures: ComponentFixtures {
    static let id = "ai-memory-panel"
    static let fixtures = [
        Fixture("every kind of group, newest first") { MemorySample.panel() },
        Fixture("empty: nothing remembered") { MemorySample.panel([]) },
        Fixture("long memory text and own labels") {
            NavigationStack {
                MonarchAiMemoryPanel(
                    [
                        MonarchAiMemoryItem(
                            id: "l",
                            text: "Prefers that every weekly report starts with the open risks, then the decisions "
                                + "taken, then a short list of next steps with owners and dates",
                            entity: MemorySample.ada,
                            updatedAt: MemorySample.now
                        )
                    ],
                    labels: MonarchAiMemoryPanelLabels(select: "Choose", kind: { _ in "Someone" }),
                    onEdit: { _, _ in },
                    onForget: { _ in },
                    onForgetMany: { _ in }
                )
            }
            .frame(height: 400)
        },
    ]
}

@Suite("MonarchAiMemoryPanel")
struct MonarchAiMemoryPanelTests {
    typealias Tokens = MonarchAiMemoryPanelTokens
    let locale = Locale(identifier: "en_US")

    @Test("Groups: people, organizations, projects, other; names in order; newest memory first")
    func groups() {
        let groups = Tokens.groups(MemorySample.memories, query: "", locale: locale)
        #expect(groups.map(\.entity.name) == ["Ada Lovelace", "Grace Hopper", "Rook", "Launch", "General"])
        #expect(groups[0].items.map(\.id) == ["2", "1"])
        #expect(Tokens.shownIDs(groups) == ["2", "1", "6", "3", "4", "5"])
    }

    @Test("Search matches the text or the entity name, ignoring case; empty matches everything")
    func search() {
        let memory = MemorySample.memories[0]
        #expect(Tokens.matches(memory, query: "SUMMARIES", locale: locale))
        #expect(Tokens.matches(memory, query: "lovelace", locale: locale))
        #expect(!Tokens.matches(memory, query: "Lisbon", locale: locale))
        #expect(Tokens.matches(memory, query: "  ", locale: locale))
        let found = Tokens.groups(MemorySample.memories, query: "on", locale: locale)
        #expect(Tokens.shownIDs(found) == ["2", "6", "4"])
        #expect(Tokens.groups(MemorySample.memories, query: "zebra", locale: locale).isEmpty)
    }

    @Test("Selection keeps only shown memories, in the order shown; select all toggles")
    func selection() {
        let groups = Tokens.groups(MemorySample.memories, query: "Ada", locale: locale)
        #expect(Tokens.selected(["1", "3", "2"], groups: groups) == ["2", "1"])
        #expect(Tokens.isAllSelected(["1", "2"], groups: groups))
        #expect(!Tokens.isAllSelected(["1"], groups: groups))
        #expect(!Tokens.isAllSelected([], groups: []))
        let labels = MonarchAiMemoryPanelLabels()
        #expect(Tokens.selectAllTitle(isAllSelected: false, shown: 2, labels: labels) == "Select all (2)")
        #expect(Tokens.selectAllTitle(isAllSelected: true, shown: 2, labels: labels) == "Deselect all")
    }

    @Test("Group detail, the updated line and the search announcement")
    func texts() {
        let labels = MonarchAiMemoryPanelLabels()
        let groups = Tokens.groups(MemorySample.memories, query: "", locale: locale)
        #expect(Tokens.groupDetail(groups[0], labels: labels) == "Person \u{00B7} 2 memories")
        #expect(Tokens.groupDetail(groups[2], labels: labels) == "Organization \u{00B7} 1 memory")
        let utc = TimeZone(identifier: "UTC") ?? .current
        #expect(
            Tokens.updatedLine(MemorySample.now, locale: locale, timeZone: utc, labels: labels)
                == "Updated Nov 14, 2025"
        )
        #expect(Tokens.searchAnnouncement(query: "", count: 3, labels: labels) == nil)
        #expect(Tokens.searchAnnouncement(query: "a", count: 1, labels: labels) == "1 memory matches")
        #expect(Tokens.searchAnnouncement(query: "a", count: 0, labels: labels) == "0 memories match")
    }

    @Test("Rows show the memory without its scope prefix, as a saved memory")
    func part() {
        let part = Tokens.part(MemorySample.memories[0])
        #expect(part.id == "1")
        #expect(part.action == .write)
        #expect(part.scope == "Ada Lovelace")
    }

    @Test("Memory events: write adds or updates, forget removes, read changes nothing")
    func apply() {
        let start = [MemorySample.memories[0]]
        let later = MemorySample.now + 60
        let read = MonarchAiMemoryPart(id: "1", action: .read, text: "x")
        #expect(MonarchAiMemoryItem.apply(start, event: read, now: later) == start)
        let same = MonarchAiMemoryPart(id: "1", action: .write, text: start[0].text)
        #expect(MonarchAiMemoryItem.apply(start, event: same, now: later) == start)
        let update = MonarchAiMemoryPart(id: "1", action: .write, text: "Prefers bullet points")
        let updated = MonarchAiMemoryItem.apply(start, event: update, now: later)
        #expect(updated[0].text == "Prefers bullet points")
        #expect(updated[0].updatedAt == later)
        let added = MonarchAiMemoryItem.apply(
            start,
            event: MonarchAiMemoryPart(id: "9", action: .write, text: "Likes tea", scope: "Ada"),
            now: later
        )
        #expect(added.last?.entity == MonarchAiMemoryEntity(kind: .other, name: "Ada"))
        let unscoped = MonarchAiMemoryItem.apply(
            start,
            event: MonarchAiMemoryPart(id: "9", action: .write, text: "Likes tea"),
            now: later,
            defaultEntityName: "Misc"
        )
        #expect(unscoped.last?.entity.name == "Misc")
        let custom = MonarchAiMemoryItem.apply(
            start,
            event: MonarchAiMemoryPart(id: "9", action: .write, text: "Likes tea"),
            now: later,
            entity: { _ in MemorySample.ada }
        )
        #expect(custom.last?.entity == MemorySample.ada)
        let forgotten = MonarchAiMemoryItem.apply(
            start,
            event: MonarchAiMemoryPart(id: "1", action: .forget, text: ""),
            now: later
        )
        #expect(forgotten.isEmpty)
        #expect(
            MonarchAiMemoryItem.apply(start, event: MonarchAiMemoryPart(id: "x", action: .forget, text: ""), now: later)
                == start
        )
    }

    @Test("Colour roles: default group names, muted details")
    func roles() {
        #expect(Tokens.headingRole == \.textDefault)
        #expect(Tokens.mutedRole == \.textMuted)
    }

    @Test("The built-in strings come from the strings table, with singular and plural forms")
    func strings() {
        let labels = MonarchAiMemoryPanelLabels()
        #expect(labels.label == "Memories")
        #expect(labels.search == "Search memories")
        #expect(labels.select == "Select")
        #expect(labels.done == "Done")
        #expect(labels.kind(.organization) == "Organization")
        #expect(labels.kind(.other) == "Other")
        #expect(labels.count(1) == "1 memory")
        #expect(labels.empty == "Nothing is remembered yet.")
        #expect(labels.noMatches("tea") == "No memories match “tea”.")
        #expect(labels.confirmTitle(1) == "Forget 1 memory?")
        #expect(labels.confirmTitle(3) == "Forget 3 memories?")
        #expect(labels.confirmMessage.hasPrefix("The assistant will stop"))
        #expect(labels.forgotten(1) == "Memory forgotten")
        #expect(labels.forgotten(2) == "2 memories forgotten")
        #expect(labels.edited == "Memory updated")
        #expect(labels.forget == "Forget")
        #expect(labels.cancel == "Cancel")
    }

    @Test("Own labels replace the built-in strings")
    func overrides() {
        let labels = MonarchAiMemoryPanelLabels(
            label: "L",
            search: "S",
            selectAll: { "A\($0)" },
            matches: { "M\($0)" },
            forgotten: { "F\($0)" }
        )
        #expect(
            [labels.label, labels.search, labels.selectAll(2), labels.matches(3), labels.forgotten(4)] == [
                "L", "S", "A2", "M3", "F4",
            ]
        )
    }
}
