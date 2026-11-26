import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private struct UndoFailure: Error {}

enum MonarchAiToolResultCardFixtures: ComponentFixtures {
    static let id = "ai-tool-result-card"
    static let fixtures = [
        Fixture("record created, every part") {
            MonarchAiToolResultCard(
                "Acme Ltd",
                kind: .recordCreated,
                description: "Added to Accounts",
                url: URL(string: "https://crm.example.com/accounts/42"),
                meta: [
                    MonarchKeyValueItem("Owner", value: "Ana Flores"),
                    MonarchKeyValueItem("Stage", value: "Lead"),
                ],
                onUndo: {}
            )
        },
        Fixture("every kind") {
            VStack {
                ForEach(MonarchAiToolResultKind.allCases, id: \.self) { kind in
                    MonarchAiToolResultCard("Quarterly review", kind: kind)
                }
            }
        },
        Fixture("long text, undo that fails, custom labels and icon") {
            MonarchAiToolResultCard(
                "Re: The renewal terms for the enterprise plan and the three new workspaces in the EU region",
                kind: .emailSent,
                description: "To Ana Flores, Ben Okafor and 3 others",
                url: URL(string: "mailto:ana@example.com"),
                systemImage: "paperplane",
                labels: MonarchAiToolResultCardLabels(kind: { _ in "Sent" }, open: "View", undo: "Unsend"),
                onUndo: { throw UndoFailure() }
            )
        },
        Fixture("unsafe address, no actions") {
            MonarchAiToolResultCard("Ticket 812", kind: .ticketFiled, url: URL(string: "javascript:alert(1)"))
        },
    ]
}

@Suite("MonarchAiToolResultCard")
struct MonarchAiToolResultCardTests {
    typealias Tokens = MonarchAiToolResultCardTokens

    @Test("Every kind has its own icon and label")
    func kinds() {
        let symbols = Set(MonarchAiToolResultKind.allCases.map(Tokens.symbol))
        #expect(symbols.count == MonarchAiToolResultKind.allCases.count)
        let names = Set(MonarchAiToolResultKind.allCases.map(MonarchAiToolResultCardStrings.kind))
        #expect(names.count == MonarchAiToolResultKind.allCases.count)
        #expect(MonarchAiToolResultCardStrings.kind(.recordCreated) == "Record created")
        #expect(MonarchAiToolResultCardStrings.kind(.generic) == "Done")
    }

    @Test("The tile is success while the result stands and muted once undone")
    func tile() {
        #expect(Tokens.tile(isUndone: false).background == \.successSubtle)
        #expect(Tokens.tile(isUndone: false).foreground == \.successStrong)
        #expect(Tokens.tile(isUndone: true).background == \.bgMuted)
        #expect(Tokens.tile(isUndone: true).foreground == \.textMuted)
        #expect(Tokens.statusColor(.failed) == \.dangerStrong)
        #expect(Tokens.statusColor(.done) == \.textMuted)
    }

    @Test("Undo and Open go away once the undo worked, and Undo stays after a failure")
    func undoStates() {
        #expect(Tokens.showsUndo(hasHandler: true, state: .idle))
        #expect(Tokens.showsUndo(hasHandler: true, state: .pending))
        #expect(Tokens.showsUndo(hasHandler: true, state: .failed))
        #expect(!Tokens.showsUndo(hasHandler: true, state: .done))
        #expect(!Tokens.showsUndo(hasHandler: false, state: .idle))
        let url = URL(string: "https://example.com")
        #expect(Tokens.showsOpen(url: url, state: .idle))
        #expect(!Tokens.showsOpen(url: url, state: .done))
        #expect(!Tokens.showsOpen(url: nil, state: .idle))
        #expect(Tokens.canStartUndo(.idle))
        #expect(Tokens.canStartUndo(.failed))
        #expect(!Tokens.canStartUndo(.pending))
        #expect(!Tokens.canStartUndo(.done))
    }

    @Test("Only http, https and mailto results are linked")
    func links() {
        #expect(Tokens.safeURL(URL(string: "https://crm.example.com/a")) != nil)
        #expect(Tokens.safeURL(URL(string: "mailto:ana@example.com")) != nil)
        #expect(Tokens.safeURL(URL(string: "javascript:alert(1)")) == nil)
        #expect(Tokens.safeURL(URL(string: "crm://accounts/42")) == nil)
        #expect(Tokens.safeURL(URL(string: "https://")) == nil)
        #expect(Tokens.safeURL(nil) == nil)
    }

    @Test("VoiceOver reads the kind, the title and the description")
    func accessibilityLabel() {
        #expect(
            Tokens.accessibilityLabel(kind: "Record created", title: "Acme Ltd", description: "Added to Accounts")
                == "Record created, Acme Ltd, Added to Accounts"
        )
        #expect(Tokens.accessibilityLabel(kind: "Done", title: "Synced", description: nil) == "Done, Synced")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchAiToolResultCardStrings.open == "Open")
        #expect(MonarchAiToolResultCardStrings.undo == "Undo")
        #expect(MonarchAiToolResultCardStrings.undone == "Undone")
        #expect(MonarchAiToolResultCardStrings.undoFailed == "Couldn’t undo. Try again.")
    }
}
