import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

/// Shared sample data for the conversation list fixtures and tests.
private enum Sample {
    static let now = Date(timeIntervalSince1970: 1_763_100_000)
    static let day: TimeInterval = 86_400
    static var calendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC") ?? .current
        return calendar
    }

    static let history = [
        MonarchAiConversationSummary(id: "p", title: "Quarterly plan", updatedAt: now - day * 40, isPinned: true),
        MonarchAiConversationSummary(
            id: "t1",
            title: "Summarise the support tickets",
            updatedAt: now - 60,
            preview: "Here are the five themes from this week."
        ),
        MonarchAiConversationSummary(id: "t2", title: "", updatedAt: now - 3_600),
        MonarchAiConversationSummary(id: "w", title: "Draft the launch email", updatedAt: now - day * 3),
        MonarchAiConversationSummary(id: "o", title: "Trip ideas for Porto", updatedAt: now - day * 30),
    ]

    @MainActor static func list(
        _ conversations: [MonarchAiConversationSummary] = history,
        isSearchable: Bool = true,
        visibleCount: Int = 200
    ) -> some View {
        NavigationStack {
            MonarchAiConversationList(
                conversations,
                activeID: "t1",
                isSearchable: isSearchable,
                visibleCount: visibleCount,
                now: now,
                calendar: calendar,
                onNew: {},
                onRename: { _, _ in },
                onPin: { _, _ in },
                onDelete: { _ in },
                onSelect: { _ in }
            )
        }
        .frame(height: 560)
    }
}

enum MonarchAiConversationListFixtures: ComponentFixtures {
    static let id = "ai-conversation-list"
    static let fixtures = [
        Fixture("every group, active row, untitled, all commands") { Sample.list() },
        Fixture("empty history") { Sample.list([]) },
        Fixture("long history with show more, no search") {
            Sample.list(
                (0..<12).map {
                    MonarchAiConversationSummary(
                        id: "c\($0)",
                        title: "Conversation \($0) with a long title that wraps onto a second line",
                        updatedAt: Sample.now - Sample.day * Double($0)
                    )
                },
                isSearchable: false,
                visibleCount: 5
            )
        },
        Fixture("select only, own labels") {
            NavigationStack {
                MonarchAiConversationList(
                    Sample.history,
                    now: Sample.now,
                    calendar: Sample.calendar,
                    labels: MonarchAiConversationListLabels(group: { $0 == .pinned ? "Starred" : "Recent" })
                ) { _ in }
            }
            .frame(height: 480)
        },
    ]
}

@Suite("MonarchAiConversationList")
struct MonarchAiConversationListTests {
    typealias Tokens = MonarchAiConversationListTokens

    @Test("Conversations group into pinned, today, the previous 7 days and older")
    func groups() {
        let sections = Tokens.sections(Sample.history, now: Sample.now, calendar: Sample.calendar)
        #expect(sections.map(\.group) == [.pinned, .today, .previous7Days, .older])
        #expect(sections[1].items.map(\.id) == ["t1", "t2"])
    }

    @Test("Seven days before today is still recent; eight is older; the future is today")
    func boundaries() {
        func group(_ offset: TimeInterval) -> MonarchAiConversationGroup {
            Tokens.group(
                of: MonarchAiConversationSummary(id: "x", title: "x", updatedAt: Sample.now + offset),
                now: Sample.now,
                calendar: Sample.calendar
            )
        }
        #expect(group(-Sample.day * 7) == .previous7Days)
        #expect(group(-Sample.day * 8) == .older)
        #expect(group(Sample.day) == .today)
    }

    @Test("Empty groups are left out and each group is newest first")
    func ordering() {
        let sections = Tokens.sections(
            [
                MonarchAiConversationSummary(id: "a", title: "a", updatedAt: Sample.now - 100),
                MonarchAiConversationSummary(id: "b", title: "b", updatedAt: Sample.now - 10),
            ],
            now: Sample.now,
            calendar: Sample.calendar
        )
        #expect(sections.count == 1)
        #expect(sections[0].items.map(\.id) == ["b", "a"])
    }

    @Test("Search matches the title or the preview, ignoring case and accents")
    func search() {
        let item = MonarchAiConversationSummary(id: "a", title: "Café plans", updatedAt: Sample.now, preview: "Budget")
        #expect(Tokens.matches(item, query: "cafe"))
        #expect(Tokens.matches(item, query: " BUDGET "))
        #expect(Tokens.matches(item, query: ""))
        #expect(!Tokens.matches(item, query: "travel"))
    }

    @Test("A long history is capped across groups in display order")
    func capped() {
        let sections = Tokens.sections(Sample.history, now: Sample.now, calendar: Sample.calendar)
        let capped = Tokens.capped(sections, limit: 2)
        #expect(capped.flatMap(\.items).map(\.id) == ["p", "t1"])
        #expect(Tokens.capped(sections, limit: 0).isEmpty)
        #expect(Tokens.step(0) == 1)
    }

    @Test("Titles fall back to New conversation; renames are trimmed and never empty or unchanged")
    func titles() {
        let untitled = MonarchAiConversationSummary(id: "a", title: "  ", updatedAt: Sample.now)
        #expect(Tokens.title(untitled, untitled: "New conversation") == "New conversation")
        #expect(Tokens.renamed(" Plan ", original: "Old") == "Plan")
        #expect(Tokens.renamed("Old", original: "Old") == nil)
        #expect(Tokens.renamed("  ", original: "Old") == nil)
    }

    @Test("A summary made from messages takes the last time and the last text")
    func fromMessages() {
        let first = Date(timeIntervalSince1970: 1_000)
        let last = Date(timeIntervalSince1970: 2_000)
        let summary = MonarchAiConversationSummary(
            id: "c",
            title: "Plan",
            messages: [
                MonarchAiConversationMessage(
                    id: "1",
                    role: .user,
                    parts: [.text(MonarchAiTextPart(text: "Plan the\nlaunch"))],
                    createdAt: MonarchAiConversationMessage.timestamp(first)
                ),
                MonarchAiConversationMessage(
                    id: "2",
                    role: .assistant,
                    parts: [],
                    createdAt: MonarchAiConversationMessage.timestamp(last)
                ),
            ]
        )
        #expect(summary.preview == "Plan the launch")
        #expect(summary.updatedAt == last)
        #expect(MonarchAiConversationSummary(id: "e", title: "", messages: []).updatedAt == .distantPast)
    }

    @Test("Rows read the title and the preview; searches announce the number of matches")
    func accessibility() {
        #expect(Tokens.accessibilityLabel(title: "Plan", preview: "Next steps") == "Plan, Next steps")
        #expect(Tokens.accessibilityLabel(title: "Plan", preview: "") == "Plan")
        let labels = MonarchAiConversationListLabels()
        #expect(Tokens.announcement(query: "a", count: 1, searchesLocally: true, labels: labels) == "1 conversation")
        #expect(Tokens.announcement(query: "a", count: 3, searchesLocally: true, labels: labels) == "3 conversations")
        #expect(Tokens.announcement(query: " ", count: 3, searchesLocally: true, labels: labels) == nil)
        #expect(Tokens.announcement(query: "a", count: 3, searchesLocally: false, labels: labels) == nil)
    }

    @Test("Own labels replace the built-in strings")
    func overrides() {
        let labels = MonarchAiConversationListLabels(
            label: "History",
            group: { _ in "G" },
            noResults: { "none " + $0 },
            results: { "\($0) found" },
            deleteTitle: { "Remove " + $0 },
            showMore: { "+\($0)" }
        )
        #expect(labels.label == "History")
        #expect(labels.group(.today) == "G")
        #expect(labels.noResults("x") == "none x")
        #expect(labels.results(2) == "2 found")
        #expect(labels.deleteTitle("Plan") == "Remove Plan")
        #expect(labels.showMore(4) == "+4")
    }

    @Test("The built-in strings come from the strings table")
    func strings() {
        typealias Strings = MonarchAiConversationListStrings
        #expect(Strings.label == "Conversation history")
        #expect(
            MonarchAiConversationGroup.allCases.map(Strings.group) == ["Pinned", "Today", "Previous 7 days", "Older"]
        )
        #expect(Strings.untitled == "New conversation")
        #expect(Strings.search == "Search")
        #expect(Strings.noResults("tax") == "No conversations match “tax”")
        #expect(Strings.deleteTitle("Plan") == "Delete “Plan”?")
        #expect(Strings.showMore(1) == "Show 1 more conversation")
        #expect(Strings.showMore(5) == "Show 5 more conversations")
        for value in [
            Strings.newConversation, Strings.empty, Strings.rename, Strings.pin, Strings.unpin, Strings.delete,
            Strings.renameTitle, Strings.renameField, Strings.save, Strings.cancel, Strings.deleteMessage,
            Strings.deleteConfirm,
        ] {
            #expect(!value.hasPrefix("monarch."))
        }
    }
}
