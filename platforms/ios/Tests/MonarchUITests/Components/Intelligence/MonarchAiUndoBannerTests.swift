import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private let undoChanges = [
    MonarchAiChange("Status", before: "Open", after: "Closed"),
    MonarchAiChange("Owner", before: "Ana Flores", after: "Ben Okafor"),
    MonarchAiChange("Priority", before: "Low", after: "High"),
    MonarchAiChange("Due date", before: "14 Oct", after: "21 Oct"),
]

enum MonarchAiUndoBannerFixtures: ComponentFixtures {
    static let id = "ai-undo-banner"
    static let fixtures = [
        Fixture("changes, audit link and dismiss") {
            MonarchAiUndoBanner(
                "AI changed 2 fields",
                description: "On the Acme Ltd account",
                changes: Array(undoChanges.prefix(2)),
                auditURL: URL(string: "https://app.example.com/audit/812"),
                onUndo: {},
                onDismiss: {}
            )
        },
        Fixture("more than three changes, with a time limit") {
            MonarchAiUndoBanner(
                "AI changed 4 fields",
                changes: undoChanges,
                duration: .seconds(30),
                onUndo: {}
            )
        },
        Fixture("title only, custom labels") {
            MonarchAiUndoBanner(
                "AI archived 12 tickets that had no activity in the last ninety days and were marked as resolved",
                labels: MonarchAiUndoBannerLabels(aiPrefix: "Assistant change:", undo: "Restore"),
                onUndo: {}
            )
        },
        Fixture("unsafe audit link") {
            MonarchAiUndoBanner("AI renamed the project", auditURL: URL(string: "javascript:alert(1)"), onUndo: {})
        },
    ]
}

@Suite("MonarchAiUndoBanner")
struct MonarchAiUndoBannerTests {
    typealias Tokens = MonarchAiUndoBannerTokens

    @Test("The state follows the undo, and a finished undo wins over the timer")
    func states() {
        #expect(Tokens.state(undo: .idle, isExpired: false) == .active)
        #expect(Tokens.state(undo: .undoing, isExpired: false) == .undoing)
        #expect(Tokens.state(undo: .failed, isExpired: false) == .failed)
        #expect(Tokens.state(undo: .undone, isExpired: false) == .undone)
        #expect(Tokens.state(undo: .idle, isExpired: true) == .expired)
        #expect(Tokens.state(undo: .failed, isExpired: true) == .expired)
        #expect(Tokens.state(undo: .undone, isExpired: true) == .undone)
    }

    @Test("Undo shows until it worked or expired, and starts only when idle or failed")
    func undo() {
        #expect(Tokens.showsUndo(.active))
        #expect(Tokens.showsUndo(.undoing))
        #expect(Tokens.showsUndo(.failed))
        #expect(!Tokens.showsUndo(.undone))
        #expect(!Tokens.showsUndo(.expired))
        #expect(Tokens.canStartUndo(.active))
        #expect(Tokens.canStartUndo(.failed))
        #expect(!Tokens.canStartUndo(.undoing))
        #expect(!Tokens.canStartUndo(.expired))
    }

    @Test("The time limit pauses in the background, while reading changes, and with VoiceOver or Switch Control")
    func countdown() {
        func counts(
            hasDuration: Bool = true,
            state: Tokens.State = .active,
            active: Bool = true,
            reading: Bool = false,
            assistive: Bool = false
        ) -> Bool {
            Tokens.isCountingDown(
                hasDuration: hasDuration,
                state: state,
                isSceneActive: active,
                isReadingChanges: reading,
                isAssistiveTechnologyRunning: assistive
            )
        }
        #expect(counts())
        #expect(counts(state: .failed))
        #expect(!counts(hasDuration: false))
        #expect(!counts(state: .undoing))
        #expect(!counts(state: .undone))
        #expect(!counts(state: .expired))
        #expect(!counts(active: false))
        #expect(!counts(reading: true))
        #expect(!counts(assistive: true))
    }

    @Test("The surface uses the AI roles while the change can be undone, neutral afterwards")
    func surface() {
        #expect(Tokens.surface(.active).background == \.aiSubtle)
        #expect(Tokens.surface(.active).border == \.aiBorder)
        #expect(Tokens.surface(.failed).background == \.aiSubtle)
        #expect(Tokens.surface(.undone).background == \.bgSubtle)
        #expect(Tokens.surface(.expired).border == \.borderSubtle)
        #expect(Tokens.statusColor(.failed) == \.dangerStrong)
        #expect(Tokens.statusColor(.undone) == \.textMuted)
    }

    @Test("More than three changes collapse into a disclosure")
    func collapsing() {
        #expect(!Tokens.collapsesChanges(0))
        #expect(!Tokens.collapsesChanges(3))
        #expect(Tokens.collapsesChanges(4))
    }

    @Test("Only http and https audit links are shown")
    func auditLinks() {
        #expect(Tokens.safeURL(URL(string: "https://app.example.com/audit/812")) != nil)
        #expect(Tokens.safeURL(URL(string: "javascript:alert(1)")) == nil)
        #expect(Tokens.safeURL(URL(string: "mailto:audit@example.com")) == nil)
        #expect(Tokens.safeURL(nil) == nil)
    }

    @Test("VoiceOver hears that it is an AI change and how each field changed")
    func accessibility() {
        #expect(
            Tokens.titleLabel(prefix: MonarchAiUndoBannerStrings.aiPrefix, title: "AI changed 2 fields")
                == "AI change: AI changed 2 fields"
        )
        #expect(Tokens.titleLabel(prefix: "", title: "Changed") == "Changed")
        #expect(MonarchAiUndoBannerStrings.change("Status", "Open", "Closed") == "Status changed from Open to Closed")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchAiUndoBannerStrings.undo == "Undo")
        #expect(MonarchAiUndoBannerStrings.audit == "View in audit log")
        #expect(MonarchAiUndoBannerStrings.dismiss == "Dismiss")
        #expect(MonarchAiUndoBannerStrings.showChanges(5) == "Show 5 changes")
        #expect(MonarchAiUndoBannerStrings.undone == "Changes undone")
        #expect(MonarchAiUndoBannerStrings.undoFailed == "Couldn’t undo. Try again.")
        #expect(MonarchAiUndoBannerStrings.expired == "Undo is no longer available.")
    }
}
