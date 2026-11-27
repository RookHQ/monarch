import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchAiToolApprovalFixtures: ComponentFixtures {
    static let id = "ai-tool-approval"

    static let email = MonarchAiToolCallPart(
        id: "call-1",
        name: "send_email",
        input: ["to": ["ada@example.com", "sam@example.com"], "subject": "Q3 plan", "bodyText": "See attached."],
        state: .pendingApproval
    )

    static let fixtures: [Fixture] =
        [
            Fixture("pending, medium, always allow") {
                MonarchAiToolApproval(
                    email,
                    summary: "Send email to 2 people",
                    argumentLabels: ["to": "Recipients"],
                    onAlwaysAllow: { _ in },
                    onDeny: { _ in },
                    onApprove: { _ in }
                )
            },
            Fixture("destructive with consequence") {
                MonarchAiToolApproval(
                    MonarchAiToolCallPart(
                        id: "call-2",
                        name: "delete_records",
                        input: ["count": 12, "table": "invoices"],
                        state: .pendingApproval,
                        risk: .destructive
                    ),
                    summary: "Delete 12 records",
                    onAlwaysAllow: { _ in },
                    onDeny: { _ in },
                    onApprove: { _ in }
                )
            },
            Fixture("no arguments, not editable") {
                MonarchAiToolApproval(
                    MonarchAiToolCallPart(id: "call-3", name: "refresh_cache", state: .pendingApproval),
                    risk: .low,
                    isEditable: false,
                    onDeny: { _ in },
                    onApprove: { _ in }
                )
            },
            Fixture("raw string input, high risk, custom consequence") {
                MonarchAiToolApproval(
                    MonarchAiToolCallPart(
                        id: "call-4",
                        name: "run_sql",
                        input: "DROP TABLE drafts;",
                        state: .pendingApproval
                    ),
                    consequence: "Every draft is removed for all users.",
                    risk: .high,
                    onDeny: { _ in },
                    onApprove: { _ in }
                )
            },
            Fixture("long title and values") {
                MonarchAiToolApproval(
                    MonarchAiToolCallPart(
                        id: "call-5",
                        name: "post_message",
                        input: ["text": .string(String(repeating: "A long message body that wraps. ", count: 6))],
                        state: .pendingApproval
                    ),
                    summary: String(repeating: "Post a message to the whole company channel ", count: 2),
                    onDeny: { _ in },
                    onApprove: { _ in }
                )
            },
        ]
        + [MonarchAiToolCallState.succeeded, .denied, .cancelled].map { state in
            Fixture("resolved \(state.rawValue)") {
                MonarchAiToolApproval(
                    MonarchAiToolCallPart(
                        id: state.rawValue,
                        name: "send_email",
                        input: ["to": "ada@example.com"],
                        state: state
                    ),
                    onDeny: { _ in },
                    onApprove: { _ in }
                )
            }
        }
}

@Suite("MonarchAiToolApproval")
struct MonarchAiToolApprovalTests {
    private typealias Tokens = MonarchAiToolApprovalTokens

    @Test("The risk is the card's, else the call's, else medium")
    func risk() {
        let plain = MonarchAiToolCallPart(id: "1", name: "x", state: .pendingApproval)
        let risky = MonarchAiToolCallPart(id: "2", name: "x", state: .pendingApproval, risk: .high)
        #expect(Tokens.risk(nil, part: plain) == .medium)
        #expect(Tokens.risk(nil, part: risky) == .high)
        #expect(Tokens.risk(.low, part: risky) == .low)
    }

    @Test("Each risk has a tone and colour; destructive is danger")
    func riskLook() {
        #expect(Tokens.tone(.low) == .neutral)
        #expect(Tokens.tone(.medium) == .warning)
        #expect(Tokens.tone(.high) == .warning)
        #expect(Tokens.tone(.destructive) == .danger)
        #expect(Tokens.accent(.destructive) == \.dangerStrong)
        #expect(Tokens.accent(.high) == \.warningStrong)
        #expect(Tokens.accent(.low) == \.textMuted)
        #expect(Tokens.approveVariant(.destructive) == .danger)
        #expect(Tokens.approveVariant(.high) == .primary)
    }

    @Test("Risk names and consequences: built in, overridden, and only destructive by default")
    func riskText() {
        let none = MonarchAiToolApprovalLabels()
        #expect(Tokens.riskName(.low, labels: none) == "Low risk")
        #expect(Tokens.riskName(.destructive, labels: none) == "Can’t be undone")
        #expect(Tokens.consequence(nil, risk: .destructive, labels: none) == "This can’t be undone once it runs.")
        #expect(Tokens.consequence(nil, risk: .high, labels: none) == nil)
        #expect(Tokens.consequence("Mail goes out now.", risk: .low, labels: none) == "Mail goes out now.")
        let custom = MonarchAiToolApprovalLabels(risk: { _ in "Risk" }, consequence: { $0 == .high ? "Careful" : "" })
        #expect(Tokens.riskName(.medium, labels: custom) == "Risk")
        #expect(Tokens.consequence(nil, risk: .high, labels: custom) == "Careful")
        #expect(Tokens.consequence(nil, risk: .destructive, labels: custom) == nil)
    }

    @Test("Always allow is never offered for destructive calls; focus goes to Deny when cautious")
    func caution() {
        #expect(Tokens.offersAlwaysAllow(hasCallback: true, risk: .high))
        #expect(!Tokens.offersAlwaysAllow(hasCallback: true, risk: .destructive))
        #expect(!Tokens.offersAlwaysAllow(hasCallback: false, risk: .low))
        #expect(Tokens.focusesDeny(.destructive))
        #expect(Tokens.focusesDeny(.high))
        #expect(!Tokens.focusesDeny(.medium))
    }

    @Test("The title is the summary, else the tool's label")
    func title() {
        let part = MonarchAiToolCallPart(id: "1", name: "send_email", state: .pendingApproval)
        #expect(
            Tokens.title(summary: "Send email to 3 people", part: part, toolLabels: [:]) == "Send email to 3 people"
        )
        #expect(Tokens.title(summary: nil, part: part, toolLabels: [:]) == "Send email")
        #expect(Tokens.title(summary: "", part: part, toolLabels: ["send_email": "Email"]) == "Email")
    }

    @Test("Resolved and outcome follow the decision first, then the call's state")
    func outcome() {
        #expect(!Tokens.isResolved(.pendingApproval, decision: nil))
        #expect(Tokens.isResolved(.pendingApproval, decision: .denied))
        #expect(Tokens.isResolved(.running, decision: nil))
        #expect(Tokens.outcome(.pendingApproval, decision: .approved) == .approved)
        #expect(Tokens.outcome(.denied, decision: nil) == .denied)
        #expect(Tokens.outcome(.cancelled, decision: nil) == .cancelled)
        #expect(Tokens.outcome(.succeeded, decision: nil) == .approved)
        let none = MonarchAiToolApprovalLabels()
        #expect(Tokens.outcomeText(.approved, labels: none) == "Approved")
        #expect(Tokens.outcomeText(.denied, labels: none) == "Denied")
        #expect(Tokens.outcomeText(.cancelled, labels: MonarchAiToolApprovalLabels(cancelled: "Stopped")) == "Stopped")
        #expect(
            Set([Tokens.outcomeSymbol(.approved), Tokens.outcomeSymbol(.denied), Tokens.outcomeSymbol(.cancelled)])
                .count == 3
        )
    }

    @Test("Arguments are readable pairs with humanized or given labels")
    func arguments() throws {
        let input: MonarchAiJSON = [
            "to": ["a@x.com", "b@x.com"], "bodyText": "Hi", "count": 3, "urgent": true, "meta": ["k": 1],
        ]
        let rows = try #require(Tokens.arguments(input, argumentLabels: ["to": "Recipients"]))
        #expect(rows.map(\.key) == ["bodyText", "count", "meta", "to", "urgent"])
        #expect(rows.first { $0.key == "to" }?.label == "Recipients")
        #expect(rows.first { $0.key == "to" }?.value == "a@x.com, b@x.com")
        #expect(rows.first { $0.key == "bodyText" }?.label == "Body text")
        #expect(rows.first { $0.key == "count" }?.value == "3")
        #expect(rows.first { $0.key == "urgent" }?.value == "true")
        #expect(rows.first { $0.key == "meta" }?.value.contains("\"k\": 1") == true)
        #expect(Tokens.arguments("text", argumentLabels: [:]) == nil)
        #expect(Tokens.readable(.null) == "—")
        #expect(Tokens.readable(0.5) == "0.5")
        #expect(Tokens.hasNoArguments(.null))
        #expect(Tokens.hasNoArguments([:]))
        #expect(!Tokens.hasNoArguments("x"))
    }

    @Test("Edited arguments: unchanged, changed into JSON, or invalid")
    func edited() {
        let original = Tokens.editorText(["to": "a@x.com"])
        #expect(Tokens.edited(original + "\n", original: original) == .unchanged)
        #expect(Tokens.edited("{\"to\": \"b@x.com\"}", original: original) == .changed(["to": "b@x.com"]))
        #expect(Tokens.edited("{to: b}", original: original) == .invalid)
    }

    @Test("Announcement and header label")
    func spoken() {
        #expect(
            Tokens.announcement("Send email", labels: MonarchAiToolApprovalLabels()) == "Approval needed: Send email"
        )
        #expect(Tokens.announcement("X", labels: MonarchAiToolApprovalLabels(announce: { "Check \($0)" })) == "Check X")
        #expect(
            Tokens.headerLabel(eyebrow: "Approval needed", title: "Send email", risk: "Low risk")
                == "Approval needed, Send email, Low risk"
        )
    }

    @Test("Built-in strings resolve")
    func strings() {
        let all =
            [
                MonarchAiToolApprovalStrings.eyebrow, MonarchAiToolApprovalStrings.arguments,
                MonarchAiToolApprovalStrings.noArguments, MonarchAiToolApprovalStrings.approve,
                MonarchAiToolApprovalStrings.deny, MonarchAiToolApprovalStrings.editArguments,
                MonarchAiToolApprovalStrings.discardEdits, MonarchAiToolApprovalStrings.argumentsJSON,
                MonarchAiToolApprovalStrings.invalidJSON, MonarchAiToolApprovalStrings.addReason,
                MonarchAiToolApprovalStrings.reason, MonarchAiToolApprovalStrings.alwaysAllow,
                MonarchAiToolApprovalStrings.approved, MonarchAiToolApprovalStrings.denied,
                MonarchAiToolApprovalStrings.cancelled, MonarchAiToolApprovalStrings.announce("x"),
            ] + MonarchAiToolCallRisk.allCases.map(MonarchAiToolApprovalStrings.risk)
        for string in all {
            #expect(!string.hasPrefix("monarch."))
        }
    }
}
