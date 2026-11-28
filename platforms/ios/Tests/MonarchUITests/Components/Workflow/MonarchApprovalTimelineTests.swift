import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchApprovalTimelineFixtures: ComponentFixtures {
    static let id = "approval-timeline"

    /// 2026-03-10 12:00 UTC, fixed so nothing depends on the clock.
    static let now = Date(timeIntervalSince1970: 1_773_144_000)
    static let utc = TimeZone(identifier: "UTC") ?? .gmt

    static let sam = MonarchApprovalPerson("Sam Lee")
    static let ana = MonarchApprovalPerson("Ana Ruiz", seed: "u-2")
    static let jo = MonarchApprovalPerson("Jo Park")
    static let kim = MonarchApprovalPerson("Kim Tran")

    static let steps = [
        MonarchApprovalTimelineStep(
            id: "0",
            label: "Submitted",
            status: .submitted,
            actor: sam,
            date: now.addingTimeInterval(-86_400 * 9)
        ),
        MonarchApprovalTimelineStep(
            id: "1",
            label: "Reporting manager",
            status: .approved,
            actor: ana,
            delegatedFrom: kim,
            date: now.addingTimeInterval(-3_600),
            comment: "Fine by me. The receipts match the travel dates and the hotel is within the policy."
        ),
        MonarchApprovalTimelineStep(
            id: "2",
            label: "Finance check",
            status: .pending,
            approvers: [jo, kim],
            delegatedFrom: ana
        ),
        MonarchApprovalTimelineStep(id: "3", label: "Director", status: .skipped, note: "Below the 1,000 limit"),
        MonarchApprovalTimelineStep(id: "4", label: "Payout", status: .waiting, approvers: [jo]),
    ]

    static let outcomes = [
        MonarchApprovalTimelineStep(id: "a", label: "Submitted", status: .submitted, actor: sam, date: now - 600),
        MonarchApprovalTimelineStep(
            id: "b",
            label: "Reporting manager",
            status: .rejected,
            actor: ana,
            date: now - 60,
            comment: "Add the hotel receipt."
        ),
        MonarchApprovalTimelineStep(id: "c", label: "Finance check", status: .cancelled, note: "Withdrawn by Sam"),
    ]

    static let fixtures = [
        Fixture("submitted, approved with delegation, pending, skipped, waiting") {
            MonarchApprovalTimeline(steps, now: now).environment(\.timeZone, utc)
        },
        Fixture("rejected and cancelled, no avatars") {
            MonarchApprovalTimeline(outcomes, now: now, showsAvatars: false).environment(\.timeZone, utc)
        },
        Fixture("empty, and own labels") {
            VStack(alignment: .leading) {
                MonarchApprovalTimeline([])
                MonarchApprovalTimeline(
                    Array(steps.prefix(3)),
                    now: now,
                    labels: MonarchApprovalTimelineLabels(status: { _ in "State" }, empty: "Draft")
                )
            }
        },
    ]
}

@Suite("MonarchApprovalTimeline")
struct MonarchApprovalTimelineTests {
    private let labels = MonarchApprovalTimelineLabels()
    private let english = Locale(identifier: "en_US")
    private let now = MonarchApprovalTimelineFixtures.now
    private let steps = MonarchApprovalTimelineFixtures.steps

    private var utcCalendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = MonarchApprovalTimelineFixtures.utc
        return calendar
    }

    @Test("Every status has its own marker shape, and completed steps are filled with the success colour")
    func markers() {
        let approved = MonarchApprovalTimelineTokens.marker(.approved)
        #expect(approved.fill == \MonarchColorTokens.successSolid)
        #expect(approved.symbol == \MonarchColorTokens.successOnSolid)
        #expect(approved.systemImage == "checkmark")
        #expect(MonarchApprovalTimelineTokens.marker(.submitted) == approved)
        #expect(MonarchApprovalTimelineTokens.marker(.rejected).fill == \MonarchColorTokens.dangerSolid)
        #expect(MonarchApprovalTimelineTokens.marker(.pending).border == \MonarchColorTokens.warningSolid)
        #expect(MonarchApprovalTimelineTokens.marker(.waiting).systemImage == nil)
        #expect(MonarchApprovalTimelineTokens.marker(.skipped).isDashed)
        #expect(MonarchApprovalTimelineTokens.marker(.cancelled).isDashed)
        #expect(!MonarchApprovalTimelineTokens.marker(.rejected).isDashed)
        let shapes = Set(
            [MonarchApprovalStepStatus.approved, .rejected, .pending, .waiting, .skipped].map {
                MonarchApprovalTimelineTokens.marker($0).systemImage
            }
        )
        #expect(shapes.count == 5)
    }

    @Test("The rail is the success border after a completed step")
    func rail() {
        #expect(MonarchApprovalTimelineTokens.railRole(.approved) == \MonarchColorTokens.successBorder)
        #expect(MonarchApprovalTimelineTokens.railRole(.submitted) == \MonarchColorTokens.successBorder)
        #expect(MonarchApprovalTimelineTokens.railRole(.pending) == \MonarchColorTokens.borderSubtle)
    }

    @Test("Status and label colours follow the status")
    func textColours() {
        #expect(MonarchApprovalTimelineTokens.statusRole(.rejected) == \MonarchColorTokens.dangerStrong)
        #expect(MonarchApprovalTimelineTokens.statusRole(.pending) == \MonarchColorTokens.warningStrong)
        #expect(MonarchApprovalTimelineTokens.statusRole(.approved) == \MonarchColorTokens.textMuted)
        #expect(MonarchApprovalTimelineTokens.isStatusEmphasised(.rejected))
        #expect(!MonarchApprovalTimelineTokens.isStatusEmphasised(.waiting))
        #expect(MonarchApprovalTimelineTokens.labelRole(.waiting) == \MonarchColorTokens.textMuted)
        #expect(MonarchApprovalTimelineTokens.labelRole(.skipped) == \MonarchColorTokens.textMuted)
        #expect(MonarchApprovalTimelineTokens.labelRole(.approved) == \MonarchColorTokens.textDefault)
    }

    @Test("Approvers show on undecided steps, with the delegation in brackets")
    func approvers() {
        #expect(
            MonarchApprovalTimelineTokens.approversLine(steps[2], labels: labels)
                == "Decided by Jo Park, Kim Tran (standing in for Ana Ruiz)"
        )
        #expect(MonarchApprovalTimelineTokens.approversLine(steps[4], labels: labels) == "Decided by Jo Park")
        #expect(MonarchApprovalTimelineTokens.approversLine(steps[1], labels: labels) == nil)
        #expect(MonarchApprovalTimelineTokens.delegationLine(steps[1], labels: labels) == "standing in for Kim Tran")
        #expect(MonarchApprovalTimelineTokens.delegationLine(steps[2], labels: labels) == nil)
    }

    @Test("Times are relative within the limit and absolute beyond it")
    func times() {
        let recent = MonarchApprovalTimelineTokens.timeText(
            now - 3_600,
            now: now,
            absoluteAfter: 7 * 86_400,
            calendar: utcCalendar,
            locale: english
        )
        #expect(recent == "1 hour ago")
        let old = MonarchApprovalTimelineTokens.timeText(
            now - 86_400 * 9,
            now: now,
            absoluteAfter: 7 * 86_400,
            calendar: utcCalendar,
            locale: english
        )
        #expect(
            old == MonarchApprovalTimelineTokens.absoluteTime(now - 86_400 * 9, calendar: utcCalendar, locale: english)
        )
        #expect(old.contains("Mar 1"))
    }

    @Test("A step is read with its status, actor, time, people, comment and note")
    func accessibilityLabel() {
        let approved = MonarchApprovalTimelineTokens.accessibilityLabel(steps[1], time: "Mar 10", labels: labels)
        #expect(
            approved.hasPrefix("Reporting manager, Approved, Ana Ruiz, Mar 10, standing in for Kim Tran, Fine by me.")
        )
        let pending = MonarchApprovalTimelineTokens.accessibilityLabel(steps[2], time: nil, labels: labels)
        #expect(
            pending
                == "Finance check, Waiting for a decision, Current step, Decided by Jo Park, Kim Tran (standing in for Ana Ruiz)"
        )
        let skipped = MonarchApprovalTimelineTokens.accessibilityLabel(steps[3], time: nil, labels: labels)
        #expect(skipped == "Director, Skipped, Below the 1,000 limit")
    }

    @Test("Decided and undecided statuses")
    func decided() {
        let decided = MonarchApprovalStepStatus.allCases.filter(MonarchApprovalTimelineTokens.isDecided)
        #expect(decided == [.submitted, .approved, .rejected, .cancelled])
        let undecided = MonarchApprovalStepStatus.allCases.filter(MonarchApprovalTimelineTokens.isUndecided)
        #expect(undecided == [.pending, .waiting])
    }

    @Test("Built-in strings resolve, and labels can replace them")
    func strings() {
        let names = MonarchApprovalStepStatus.allCases.map(labels.status)
        #expect(
            names == ["Submitted", "Approved", "Rejected", "Waiting for a decision", "Up next", "Skipped", "Cancelled"]
        )
        for text in [labels.label, labels.current, labels.empty] {
            #expect(!text.hasPrefix("monarch."))
        }
        let custom = MonarchApprovalTimelineLabels(approvers: { "By \($0)" }, current: "Now")
        #expect(MonarchApprovalTimelineTokens.approversLine(steps[4], labels: custom) == "By Jo Park")
        #expect(custom.current == "Now")
    }
}
