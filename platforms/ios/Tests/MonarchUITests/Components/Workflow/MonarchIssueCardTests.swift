import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchIssueCardFixtures: ComponentFixtures {
    static let id = "issue-card"

    /// 2026-03-10 12:00 UTC, fixed so nothing depends on the clock.
    static let today = Date(timeIntervalSince1970: 1_773_144_000)
    static let utc = TimeZone(identifier: "UTC") ?? .gmt

    static let statuses = [
        MonarchIssueCardStatus(id: "backlog", name: "Backlog", category: .backlog),
        MonarchIssueCardStatus(id: "todo", name: "Todo", category: .unstarted),
        MonarchIssueCardStatus(id: "doing", name: "In progress", category: .started),
        MonarchIssueCardStatus(id: "done", name: "Done", category: .completed),
        MonarchIssueCardStatus(id: "wontfix", name: "Won't fix", category: .cancelled),
    ]

    static let fixtures = [
        Fixture("editable status and priority, tags, overdue, assignee, link") {
            MonarchIssueCard(
                "Sign-in fails on slow networks when the token refresh races the first request",
                key: "STR-142",
                url: URL(string: "https://example.com/STR-142"),
                status: statuses[2],
                statuses: statuses,
                priority: .urgent,
                assignee: .person("Ana Ruiz"),
                tags: ["Bug", "Auth", "Mobile"],
                dueDate: today.addingTimeInterval(-86_400 * 2),
                today: today,
                onStatusChange: { _ in },
                onPriorityChange: { _ in }
            )
            .environment(\.timeZone, utc)
        },
        Fixture("every status and priority, read-only") {
            VStack {
                ForEach(0..<statuses.count, id: \.self) { index in
                    let status = statuses[index]
                    let priority = MonarchIssueCardPriority.allCases[index]
                    MonarchIssueCard(
                        "Issue in \(status.name)",
                        key: "STR-1",
                        status: status,
                        priority: priority,
                        dueDate: today.addingTimeInterval(-86_400),
                        today: today
                    )
                    .environment(\.timeZone, utc)
                }
            }
        },
        Fixture("selected, dragging, unassigned, button title, own labels") {
            VStack {
                MonarchIssueCard("Write the release notes", key: "STR-150", assignee: .unassigned, isSelected: true)
                MonarchIssueCard("Move the docs", key: "STR-151", isDragging: true, onOpen: {})
                MonarchIssueCard(
                    "Translate the card",
                    key: "STR-152",
                    priority: .low,
                    dueDate: today,
                    today: today,
                    labels: MonarchIssueCardLabels(due: { "By \($0)" }, priorityLevel: { _ in "P3" })
                )
            }
        },
    ]
}

@Suite("MonarchIssueCard")
struct MonarchIssueCardTests {
    private let labels = MonarchIssueCardLabels()
    private let statuses = MonarchIssueCardFixtures.statuses
    private let today = MonarchIssueCardFixtures.today
    private let english = Locale(identifier: "en_US")

    private var utcCalendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = MonarchIssueCardFixtures.utc
        return calendar
    }

    @Test("Selected cards are tinted with the accent and dragged cards are lifted")
    func surface() {
        let plain = MonarchIssueCardTokens.surface(isSelected: false, isDragging: false)
        #expect(plain.fill == \MonarchColorTokens.bgSurface)
        #expect(plain.border == \MonarchColorTokens.borderDefault)
        #expect(plain.elevation == .xs)
        let selected = MonarchIssueCardTokens.surface(isSelected: true, isDragging: false)
        #expect(selected.fill == \MonarchColorTokens.accentSubtle)
        #expect(selected.border == \MonarchColorTokens.accentBorder)
        #expect(selected.borderWidth > plain.borderWidth)
        #expect(MonarchIssueCardTokens.surface(isSelected: false, isDragging: true).elevation == .lg)
    }

    @Test("Every priority and status category has its own symbol")
    func symbols() {
        let priorities = MonarchIssueCardPriority.allCases.map(MonarchIssueCardTokens.prioritySymbol)
        #expect(Set(priorities).count == MonarchIssueCardPriority.allCases.count)
        let categories = MonarchIssueCardStatusCategory.allCases.map(MonarchIssueCardTokens.statusSymbol)
        #expect(Set(categories).count == MonarchIssueCardStatusCategory.allCases.count)
        #expect(MonarchIssueCardTokens.priorityRole(.urgent) == \MonarchColorTokens.dangerStrong)
        #expect(MonarchIssueCardTokens.priorityRole(.none) == \MonarchColorTokens.textMuted)
        #expect(MonarchIssueCardTokens.statusRole(.started) == \MonarchColorTokens.warningStrong)
        #expect(MonarchIssueCardTokens.statusRole(.completed) == \MonarchColorTokens.successStrong)
    }

    @Test("A due day before today is overdue, unless the issue is closed")
    func overdue() {
        let calendar = utcCalendar
        let yesterday = today.addingTimeInterval(-86_400)
        let laterToday = today.addingTimeInterval(3_600 * 11)
        #expect(MonarchIssueCardTokens.isOverdue(due: yesterday, today: today, status: statuses[2], calendar: calendar))
        #expect(MonarchIssueCardTokens.isOverdue(due: yesterday, today: today, status: nil, calendar: calendar))
        #expect(!MonarchIssueCardTokens.isOverdue(due: laterToday, today: today, status: nil, calendar: calendar))
        #expect(!MonarchIssueCardTokens.isOverdue(due: today - 3_600, today: today, status: nil, calendar: calendar))
        #expect(
            !MonarchIssueCardTokens.isOverdue(due: yesterday, today: today, status: statuses[3], calendar: calendar)
        )
        #expect(
            !MonarchIssueCardTokens.isOverdue(due: yesterday, today: today, status: statuses[4], calendar: calendar)
        )
    }

    @Test("The due day is read in the calendar's time zone")
    func dueText() {
        let lateNight = Date(timeIntervalSince1970: 1_773_183_600)  // 2026-03-10 23:00 UTC
        #expect(MonarchIssueCardTokens.dueDateText(lateNight, calendar: utcCalendar, locale: english) == "Mar 10")
        var tokyo = utcCalendar
        tokyo.timeZone = TimeZone(identifier: "Asia/Tokyo") ?? .gmt
        #expect(MonarchIssueCardTokens.dueDateText(lateNight, calendar: tokyo, locale: english) == "Mar 11")
    }

    @Test("Only http and https addresses open")
    func safeURL() {
        #expect(MonarchIssueCardTokens.safeURL(URL(string: "https://example.com/a")) != nil)
        #expect(MonarchIssueCardTokens.safeURL(URL(string: "HTTP://example.com")) != nil)
        #expect(MonarchIssueCardTokens.safeURL(URL(string: "javascript:alert(1)")) == nil)
        #expect(MonarchIssueCardTokens.safeURL(URL(string: "mailto:a@example.com")) == nil)
        #expect(MonarchIssueCardTokens.safeURL(nil) == nil)
    }

    @Test("Spoken text for the title, due date and assignee")
    func accessibility() {
        #expect(MonarchIssueCardTokens.titleAccessibilityLabel(key: "STR-1", title: "Fix") == "STR-1, Fix")
        #expect(MonarchIssueCardTokens.titleAccessibilityLabel(key: "", title: "Fix") == "Fix")
        #expect(
            MonarchIssueCardTokens.dueAccessibilityLabel(dateText: "Mar 8", isOverdue: true, labels: labels)
                == "Due Mar 8, Overdue"
        )
        #expect(
            MonarchIssueCardTokens.dueAccessibilityLabel(dateText: "Mar 8", isOverdue: false, labels: labels)
                == "Due Mar 8"
        )
        #expect(
            MonarchIssueCardTokens.assigneeAccessibilityLabel(.person("Ana Ruiz"), labels: labels)
                == "Assigned to Ana Ruiz"
        )
        #expect(MonarchIssueCardTokens.assigneeAccessibilityLabel(.unassigned, labels: labels) == "Unassigned")
    }

    @Test("Built-in strings resolve, and labels can replace them")
    func strings() {
        #expect(
            MonarchIssueCardPriority.allCases.map(labels.priorityLevel)
                == ["Urgent", "High", "Medium", "Low", "No priority"]
        )
        #expect(labels.priority("High") == "Priority: High")
        #expect(labels.status("Todo") == "Status: Todo")
        #expect(labels.due("Mar 8") == "Due Mar 8")
        for text in [labels.overdue, labels.tags, labels.unassigned, labels.selected] {
            #expect(!text.hasPrefix("monarch."))
        }
        let custom = MonarchIssueCardLabels(overdue: "Late", assignee: { "Owner \($0)" })
        #expect(custom.overdue == "Late")
        #expect(custom.assignee("Jo") == "Owner Jo")
    }
}
