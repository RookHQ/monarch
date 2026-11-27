import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchApprovalQueueFixtures: ComponentFixtures {
    static let id = "approval-queue"

    /// 2026-03-10 12:00 UTC, fixed so nothing depends on the clock.
    static let now = Date(timeIntervalSince1970: 1_773_144_000)
    static let utc = TimeZone(identifier: "UTC") ?? .gmt

    static let items = [
        MonarchApprovalQueueItem(
            id: "1",
            requester: MonarchApprovalPerson("Alex Morgan"),
            requesterDetail: "Design",
            title: "Expense claim",
            summary: "3 receipts, travel to the client workshop in Lisbon, including the hotel and two taxis",
            submittedAt: now.addingTimeInterval(-86_400 * 2),
            isOverdue: true
        ),
        MonarchApprovalQueueItem(
            id: "2",
            requester: MonarchApprovalPerson("Sam Lee", seed: "u-2"),
            requesterDetail: "Engineering",
            title: "Leave, 2 days",
            summary: "14 to 15 April",
            submittedAt: now.addingTimeInterval(-3_600)
        ),
        MonarchApprovalQueueItem(
            id: "3",
            requester: MonarchApprovalPerson("Jo Park"),
            title: "Timesheet",
            submittedAt: now.addingTimeInterval(-600),
            badges: [MonarchStatusDefinition("Resubmitted", tone: .warning)],
            canDecide: false
        ),
    ]

    static let fixtures = [
        Fixture("every action, selection, overdue, badges, read-only row") {
            MonarchApprovalQueue(
                items,
                now: now,
                onApprove: { _ in },
                onReject: { _, _ in },
                onReview: { _ in },
                onApproveMany: { _ in }
            )
            .frame(height: 640)
            .environment(\.timeZone, utc)
        },
        Fixture("dashboard card with a limit and a footer") {
            MonarchApprovalQueue(
                items,
                limit: 1,
                now: now,
                onApprove: { _ in },
                footer: { Button("See all requests") {} }
            )
            .frame(height: 320)
        },
        Fixture("searchable in a navigation stack") {
            NavigationStack {
                MonarchApprovalQueue(items, isSearchable: true, now: now, onReview: { _ in })
            }
            .frame(height: 560)
        },
        Fixture("empty, with own labels") {
            MonarchApprovalQueue([], labels: MonarchApprovalQueueLabels(emptyTitle: "All done"))
                .frame(height: 320)
        },
    ]
}

@Suite("MonarchApprovalQueue")
struct MonarchApprovalQueueTests {
    private let items = MonarchApprovalQueueFixtures.items
    private let labels = MonarchApprovalQueueLabels()
    private let english = Locale(identifier: "en_US")

    private var utcCalendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = MonarchApprovalQueueFixtures.utc
        return calendar
    }

    @Test("Search matches the requester and title, ignoring case and outer spaces, then the limit applies")
    func visible() {
        #expect(MonarchApprovalQueueTokens.visible(items, query: "", limit: nil).count == 3)
        #expect(MonarchApprovalQueueTokens.visible(items, query: "  sam ", limit: nil).map(\.id) == ["2"])
        #expect(MonarchApprovalQueueTokens.visible(items, query: "TIMESHEET", limit: nil).map(\.id) == ["3"])
        #expect(MonarchApprovalQueueTokens.visible(items, query: "", limit: 2).map(\.id) == ["1", "2"])
        #expect(MonarchApprovalQueueTokens.visible(items, query: "", limit: -1).isEmpty)
        let custom = MonarchApprovalQueueItem(
            id: "x",
            requester: MonarchApprovalPerson("Kim"),
            title: "Purchase",
            searchText: "PO-778"
        )
        #expect(MonarchApprovalQueueTokens.visible([custom], query: "po-778", limit: nil).count == 1)
        #expect(MonarchApprovalQueueTokens.visible([custom], query: "Kim", limit: nil).isEmpty)
    }

    @Test("Only requests that can be decided are chosen and selected by Select all")
    func selection() {
        #expect(MonarchApprovalQueueTokens.chosen(items, selection: ["1", "3", "gone"]).map(\.id) == ["1"])
        let all = MonarchApprovalQueueTokens.selectingAll(true, visible: items, selection: [])
        #expect(all == ["1", "2"])
        let kept = MonarchApprovalQueueTokens.selectingAll(false, visible: Array(items.prefix(1)), selection: all)
        #expect(kept == ["2"])
        #expect(MonarchApprovalQueueTokens.toggled(["1"], id: "2", isOn: true) == ["1", "2"])
        #expect(MonarchApprovalQueueTokens.toggled(["1"], id: "1", isOn: false).isEmpty)
    }

    @Test("A required reason must have text; the reason sent is trimmed or nil")
    func reason() {
        #expect(!MonarchApprovalQueueTokens.canConfirmReject(reason: "  ", requirement: .required))
        #expect(MonarchApprovalQueueTokens.canConfirmReject(reason: "Add receipt", requirement: .required))
        #expect(MonarchApprovalQueueTokens.canConfirmReject(reason: "", requirement: .optional))
        #expect(MonarchApprovalQueueTokens.canConfirmReject(reason: "", requirement: .none))
        #expect(MonarchApprovalQueueTokens.submittedReason("  Add receipt ", requirement: .required) == "Add receipt")
        #expect(MonarchApprovalQueueTokens.submittedReason(" ", requirement: .optional) == nil)
        #expect(MonarchApprovalQueueTokens.submittedReason("Text", requirement: .none) == nil)
    }

    @Test("Submission times are relative to now")
    func times() {
        let now = MonarchApprovalQueueFixtures.now
        #expect(
            MonarchApprovalQueueTokens.relativeTime(now - 3_600, now: now, calendar: utcCalendar, locale: english)
                == "1 hour ago"
        )
        #expect(
            MonarchApprovalQueueTokens.absoluteTime(now, calendar: utcCalendar, locale: english).contains(
                "Mar 10, 2026"
            )
        )
    }

    @Test("A request is read with its requester, title, time, overdue, badges, summary and failure")
    func accessibilityLabel() {
        let overdue = MonarchApprovalQueueTokens.accessibilityLabel(
            items[0],
            time: "Mar 8",
            isFailed: false,
            labels: labels
        )
        #expect(overdue.hasPrefix("Alex Morgan, Design, Expense claim, Mar 8, Past due, 3 receipts"))
        let badged = MonarchApprovalQueueTokens.accessibilityLabel(items[2], time: nil, isFailed: true, labels: labels)
        #expect(badged == "Jo Park, Timesheet, Resubmitted, That did not work. Try again.")
    }

    @Test("Swipe tints are distinct roles")
    func tints() {
        let tints = [
            MonarchApprovalQueueTokens.approveTint, MonarchApprovalQueueTokens.rejectTint,
            MonarchApprovalQueueTokens.reviewTint,
        ]
        #expect(Set(tints).count == 3)
        #expect(MonarchApprovalQueueTokens.approveTint == \MonarchColorTokens.successSolid)
    }

    @Test("Built-in strings resolve, name the request, count requests, and can be replaced")
    func strings() {
        #expect(labels.approveItem("Sam Lee") == "Approve request from Sam Lee")
        #expect(labels.rejectItem("Sam Lee") == "Send back request from Sam Lee")
        #expect(labels.reviewItem("Sam Lee") == "Review request from Sam Lee")
        #expect(labels.selectItem("Sam Lee") == "Select request from Sam Lee")
        #expect(labels.rejectTitle("Sam Lee") == "Send back the request from Sam Lee?")
        #expect(labels.approved("Sam Lee") == "Approved the request from Sam Lee.")
        #expect(labels.rejected("Sam Lee") == "Sent back the request from Sam Lee.")
        #expect(labels.bulkTitle(1) == "Approve 1 request?")
        #expect(labels.bulkTitle(3) == "Approve 3 requests?")
        #expect(labels.bulkApproved(2) == "Approved 2 requests.")
        for text in [
            labels.label, labels.approve, labels.reject, labels.review, labels.selectAll, labels.approveSelected,
            labels.overdue, labels.search, labels.emptyTitle, labels.emptyDescription, labels.noMatchesTitle,
            labels.rejectDescription, labels.rejectReason, labels.rejectReasonPrompt, labels.bulkDescription,
            labels.bulkConfirm, labels.cancel, labels.failed,
        ] {
            #expect(!text.hasPrefix("monarch."))
        }
        let custom = MonarchApprovalQueueLabels(reject: "Decline", bulkTitle: { "Approve \($0)?" })
        #expect(custom.reject == "Decline")
        #expect(custom.bulkTitle(4) == "Approve 4?")
    }
}
