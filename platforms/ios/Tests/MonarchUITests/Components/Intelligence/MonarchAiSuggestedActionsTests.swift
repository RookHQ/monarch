import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchAiSuggestedActionsFixtures: ComponentFixtures {
    static let id = "ai-suggested-actions"

    static let remind = MonarchAiSuggestedAction(
        id: "remind",
        label: "Send the renewal reminder",
        description: "The contract ends in 14 days."
    )
    static let close = MonarchAiSuggestedAction(
        id: "close",
        label: "Close 4 stale tickets",
        risk: .destructive,
        consequences: ["4 tickets will be closed", "Requesters are notified"],
        confirmLabel: "Close tickets"
    )
    static let invite = MonarchAiSuggestedAction(
        id: "invite",
        label: "Invite the finance team",
        requiresApproval: true,
        risk: .medium
    )

    static let fixtures: [Fixture] = [
        Fixture("AI suggestions with approval, risk and dismiss") {
            MonarchAiSuggestedActions([remind, close, invite], onDismiss: { _ in }, onRun: { _ in })
        },
        Fixture("every status") {
            MonarchAiSuggestedActions(
                [
                    MonarchAiSuggestedAction(id: "a", label: "Ready action"),
                    MonarchAiSuggestedAction(id: "b", label: "Running action", status: .running),
                    MonarchAiSuggestedAction(id: "c", label: "Done action", status: .done),
                    MonarchAiSuggestedAction(id: "d", label: "Failed action", status: .failed),
                ],
                onRun: { _ in }
            )
        },
        Fixture("every risk") {
            MonarchAiSuggestedActions(
                MonarchAiToolCallRisk.allCases.map {
                    MonarchAiSuggestedAction(id: $0.rawValue, label: "Risk \($0.rawValue)", risk: $0)
                },
                onRun: { _ in }
            )
        },
        Fixture("rule-based with a title") {
            MonarchAiSuggestedActions([remind], title: "Next steps", origin: .rule) { _ in }
        },
        Fixture("empty") {
            MonarchAiSuggestedActions([]) { _ in }
        },
        Fixture("long label, disabled") {
            MonarchAiSuggestedActions(
                [
                    MonarchAiSuggestedAction(
                        id: "long",
                        label: String(repeating: "Follow up with the customer about the renewal ", count: 4),
                        description: String(repeating: "They asked for a discount. ", count: 6)
                    )
                ],
                onDismiss: { _ in },
                onRun: { _ in }
            )
            .disabled(true)
        },
    ]
}

@Suite("MonarchAiSuggestedActions")
struct MonarchAiSuggestedActionsTests {
    private typealias Tokens = MonarchAiSuggestedActionsTokens
    private typealias F = MonarchAiSuggestedActionsFixtures

    @Test("Approval is needed when required or destructive")
    func needsApproval() {
        #expect(!F.remind.needsApproval)
        #expect(F.close.needsApproval)
        #expect(F.invite.needsApproval)
        #expect(!MonarchAiSuggestedAction(id: "x", label: "x", risk: .high).needsApproval)
    }

    @Test("The action's own status wins over the tracked one, which wins over ready")
    func status() {
        let tracked: [String: MonarchAiSuggestedActionStatus] = ["remind": .failed]
        #expect(Tokens.status(F.remind, tracked: tracked) == .failed)
        #expect(Tokens.status(F.close, tracked: tracked) == .ready)
        var controlled = F.remind
        controlled.status = .done
        #expect(Tokens.status(controlled, tracked: tracked) == .done)
        #expect(Tokens.canRun(.ready))
        #expect(Tokens.canRun(.failed))
        #expect(!Tokens.canRun(.running))
        #expect(!Tokens.canRun(.done))
    }

    @Test("Low risk has no tag; medium and high warn; destructive is danger")
    func risk() {
        let labels = MonarchAiSuggestedActionsLabels()
        #expect(Tokens.riskTag(.low, labels: labels) == nil)
        #expect(Tokens.riskTag(.medium, labels: labels)?.text == "Medium risk")
        #expect(Tokens.riskTag(.medium, labels: labels)?.tone == .warning)
        #expect(Tokens.riskTag(.high, labels: labels)?.tone == .warning)
        #expect(Tokens.riskTag(.destructive, labels: labels)?.text == "Destructive")
        #expect(Tokens.riskTag(.destructive, labels: labels)?.tone == .danger)
        let custom = MonarchAiSuggestedActionsLabels(risk: { $0 == .high ? "Hoch" : nil })
        #expect(Tokens.riskTag(.high, labels: custom)?.text == "Hoch")
        #expect(Tokens.riskTag(.medium, labels: custom)?.text == "Medium risk")
    }

    @Test("The origin is text, a symbol and a colour")
    func origin() {
        let labels = MonarchAiSuggestedActionsLabels()
        #expect(Tokens.originText(.ai, labels: labels) == "Suggested by AI")
        #expect(Tokens.originText(.rule, labels: labels) == "Rule-based")
        #expect(Tokens.originText(.rule, labels: MonarchAiSuggestedActionsLabels(originRule: "Regel")) == "Regel")
        #expect(Tokens.originSymbol(.ai) != Tokens.originSymbol(.rule))
        #expect(Tokens.originColor(.ai) == \.aiStrong)
        #expect(Tokens.originColor(.rule) == \.textMuted)
    }

    @Test("Buttons and announcements name the action")
    func spoken() {
        let labels = MonarchAiSuggestedActionsLabels()
        #expect(Tokens.runLabel(F.remind, labels: labels) == "Run “Send the renewal reminder”")
        #expect(Tokens.dismissLabel(F.remind, labels: labels) == "Dismiss “Send the renewal reminder”")
        #expect(Tokens.doneAnnouncement(F.remind, labels: labels) == "“Send the renewal reminder” done.")
        let custom = MonarchAiSuggestedActionsLabels(runAction: { "Go: \($0)" }, doneAnnouncement: { "\($0)!" })
        #expect(Tokens.runLabel(F.remind, labels: custom) == "Go: Send the renewal reminder")
        #expect(Tokens.doneAnnouncement(F.remind, labels: custom) == "Send the renewal reminder!")
    }

    @Test("The approval dialog shows the description or the built-in text, then the consequences")
    func approval() {
        let labels = MonarchAiSuggestedActionsLabels()
        #expect(Tokens.approvalTitle(F.close, labels: labels) == "Run “Close 4 stale tickets”?")
        #expect(
            Tokens.approvalMessage(F.close, labels: labels)
                == "Check what this action will do before it runs.\n• 4 tickets will be closed\n• Requesters are notified"
        )
        #expect(Tokens.approvalMessage(F.remind, labels: labels) == "The contract ends in 14 days.")
        #expect(Tokens.confirmTitle(F.close, labels: labels) == "Close tickets")
        #expect(Tokens.confirmTitle(F.invite, labels: labels) == "Run action")
        #expect(Tokens.confirmTitle(F.invite, labels: MonarchAiSuggestedActionsLabels(approve: "Los")) == "Los")
        #expect(Tokens.confirmRole(F.close) == .destructive)
        #expect(Tokens.confirmRole(F.invite) == nil)
    }

    @Test("A done action's label is muted")
    func colours() {
        #expect(Tokens.labelColor(.done) == \.textMuted)
        #expect(Tokens.labelColor(.ready) == \.textDefault)
        #expect(Tokens.labelColor(.failed) == \.textDefault)
    }

    @Test("Built-in strings resolve")
    func strings() {
        let all = [
            MonarchAiSuggestedActionsStrings.title, MonarchAiSuggestedActionsStrings.originAi,
            MonarchAiSuggestedActionsStrings.originRule, MonarchAiSuggestedActionsStrings.run,
            MonarchAiSuggestedActionsStrings.runAction("x"), MonarchAiSuggestedActionsStrings.running,
            MonarchAiSuggestedActionsStrings.done, MonarchAiSuggestedActionsStrings.failed,
            MonarchAiSuggestedActionsStrings.dismiss("x"), MonarchAiSuggestedActionsStrings.needsApproval,
            MonarchAiSuggestedActionsStrings.approvalTitle("x"), MonarchAiSuggestedActionsStrings.approvalDescription,
            MonarchAiSuggestedActionsStrings.approve, MonarchAiSuggestedActionsStrings.cancel,
            MonarchAiSuggestedActionsStrings.doneAnnouncement("x"), MonarchAiSuggestedActionsStrings.empty,
        ]
        for string in all {
            #expect(!string.hasPrefix("monarch."))
        }
        #expect(MonarchAiSuggestedActionsStrings.risk(.low) == nil)
        #expect(MonarchAiSuggestedActionsStrings.risk(.high) == "High risk")
        #expect(MonarchAiSuggestedActionsStrings.failed == "That didn’t work. Try again.")
    }
}
