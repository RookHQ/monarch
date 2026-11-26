import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let twoChanges: [MonarchAiDiffSegment] = [
    MonarchAiDiffSegment(.equal, text: "The "),
    MonarchAiDiffSegment(.insert, text: "planning "),
    MonarchAiDiffSegment(.equal, text: "meeting is on "),
    MonarchAiDiffSegment(.delete, text: "Monday"),
    MonarchAiDiffSegment(.insert, text: "Tuesday"),
    MonarchAiDiffSegment(.equal, text: "."),
]

private let longText = MonarchAiDiffSegment.words(
    from:
        "Our quarterly planning review covers revenue, hiring, the product roadmap and the support backlog in one meeting.",
    to:
        "Our quarterly planning review covers revenue, hiring plans, the product roadmap and the customer support backlog in a single meeting."
)

enum MonarchAiInlineDiffFixtures: ComponentFixtures {
    static let id = "ai-inline-diff"
    static let fixtures = [
        Fixture("decidable, two changes") {
            MonarchAiInlineDiff(twoChanges, onAccept: { _ in }, onReject: { _ in })
        },
        Fixture("accept only") {
            MonarchAiInlineDiff(twoChanges, onAccept: { _ in })
        },
        Fixture("read only") {
            MonarchAiInlineDiff(twoChanges)
        },
        Fixture("without navigation") {
            MonarchAiInlineDiff(twoChanges, showsNavigation: false, onAccept: { _ in }, onReject: { _ in })
        },
        Fixture("long text") {
            MonarchAiInlineDiff(longText, onAccept: { _ in }, onReject: { _ in })
        },
        Fixture("no changes") {
            MonarchAiInlineDiff([MonarchAiDiffSegment(.equal, text: "Nothing to change.")], onAccept: { _ in })
        },
        Fixture("empty") {
            MonarchAiInlineDiff([])
        },
        Fixture("custom labels") {
            MonarchAiInlineDiff(
                twoChanges,
                labels: MonarchAiInlineDiffLabels(acceptTitle: "Keep", rejectTitle: "Discard"),
                onAccept: { _ in },
                onReject: { _ in }
            )
        },
    ]
}

@Suite("MonarchAiInlineDiff")
struct MonarchAiInlineDiffTests {
    private typealias Tokens = MonarchAiInlineDiffTokens
    private let labels = MonarchAiInlineDiffLabels()

    @Test("Adjacent removals and insertions form one change")
    func changes() {
        let changes = MonarchAiInlineDiff.changes(in: twoChanges)
        #expect(changes.count == 2)
        #expect(
            changes[0] == MonarchAiInlineDiffChange(id: "1", segmentIndexes: [1], deleted: "", inserted: "planning ")
        )
        #expect(
            changes[1]
                == MonarchAiInlineDiffChange(id: "3", segmentIndexes: [3, 4], deleted: "Monday", inserted: "Tuesday")
        )
        #expect(MonarchAiInlineDiff.changes(in: []).isEmpty)
        #expect(MonarchAiInlineDiff.changes(in: [MonarchAiDiffSegment(.equal, text: "a")]).isEmpty)
    }

    @Test("Different ids split adjacent runs into separate changes")
    func ids() {
        let segments = [
            MonarchAiDiffSegment(.delete, text: "a", id: "x"),
            MonarchAiDiffSegment(.insert, text: "b", id: "x"),
            MonarchAiDiffSegment(.insert, text: "c", id: "y"),
        ]
        let changes = Tokens.changes(segments)
        #expect(changes.map(\.id) == ["x", "y"])
        #expect(changes[0].segmentIndexes == [0, 1])
        #expect(changes[1].inserted == "c")
    }

    @Test("Accepting keeps the insertion, rejecting keeps the deletion, and plain text merges")
    func resolve() {
        let accepted = MonarchAiInlineDiff.resolve(twoChanges, change: "3", decision: .accept)
        #expect(Tokens.changes(accepted).count == 1)
        #expect(accepted.last == MonarchAiDiffSegment(.equal, text: "meeting is on Tuesday."))
        let rejected = MonarchAiInlineDiff.resolve(twoChanges, change: "1", decision: .reject)
        #expect(rejected.first == MonarchAiDiffSegment(.equal, text: "The meeting is on "))
        let all = MonarchAiInlineDiff.resolve(
            MonarchAiInlineDiff.resolve(twoChanges, change: "3", decision: .accept),
            change: "1",
            decision: .accept
        )
        #expect(all == [MonarchAiDiffSegment(.equal, text: "The planning meeting is on Tuesday.")])
        #expect(MonarchAiInlineDiff.resolve(twoChanges, change: "missing", decision: .accept) == twoChanges)
    }

    @Test("The word diff marks the changed words")
    func words() {
        let segments = MonarchAiDiffSegment.words(from: "on Monday now", to: "on Tuesday now")
        let changes = Tokens.changes(segments)
        #expect(changes.count == 1)
        #expect(changes.first?.deleted == "Monday")
        #expect(changes.first?.inserted == "Tuesday")
        #expect(MonarchAiDiffSegment.words(from: "", to: "").isEmpty)
    }

    @Test("The active change stays inside the changes")
    func clamp() {
        #expect(Tokens.clamp(5, total: 2) == 1)
        #expect(Tokens.clamp(-1, total: 2) == 0)
        #expect(Tokens.clamp(3, total: 0) == 0)
        #expect(Tokens.clamp(1, total: 3) == 1)
    }

    @Test("Change links round-trip and other URLs are ignored")
    func links() throws {
        let url = try #require(Tokens.url(forChange: 4))
        #expect(Tokens.change(from: url) == 4)
        #expect(Tokens.change(from: URL(string: "https://example.com/change/4")!) == nil)
        #expect(Tokens.change(from: URL(string: "monarch-ai-inline-diff://other/4")!) == nil)
    }

    @Test("Deletions and insertions use their own roles, stronger when active")
    func appearance() {
        #expect(Tokens.appearance(.equal, isActive: true) == .init(foreground: \.textDefault, background: nil))
        #expect(
            Tokens.appearance(.delete, isActive: false) == .init(foreground: \.dangerStrong, background: \.dangerSubtle)
        )
        #expect(
            Tokens.appearance(.delete, isActive: true)
                == .init(foreground: \.dangerStrong, background: \.dangerSubtleHover)
        )
        #expect(
            Tokens.appearance(.insert, isActive: false)
                == .init(foreground: \.successStrong, background: \.successSubtle)
        )
        #expect(
            Tokens.appearance(.insert, isActive: true)
                == .init(foreground: \.successStrong, background: \.successSubtleHover)
        )
    }

    @Test("The text marks deletions, insertions and the active change without colour alone")
    func attributed() {
        let changes = Tokens.changes(twoChanges)
        let text = Tokens.attributed(twoChanges, changes: changes, active: 1, colors: MonarchTheme.light.colors)
        #expect(String(text.characters) == "The planning meeting is on MondayTuesday.")
        var struck: [String] = []
        var underlined: [String] = []
        var bold: [String] = []
        var linked: [Int] = []
        for run in text.runs {
            let piece = String(text[run.range].characters)
            if run[Tokens.SwiftUIAttributes.StrikethroughStyleAttribute.self] != nil { struck.append(piece) }
            if run[Tokens.SwiftUIAttributes.UnderlineStyleAttribute.self] != nil { underlined.append(piece) }
            if run.inlinePresentationIntent == .stronglyEmphasized { bold.append(piece) }
            if let link = run.link, let index = Tokens.change(from: link) { linked.append(index) }
        }
        #expect(struck == ["Monday"])
        #expect(underlined == ["planning ", "Tuesday"])
        #expect(bold == ["Monday", "Tuesday"])
        #expect(linked == [0, 1, 1])
    }

    @Test("VoiceOver reads removals and additions with their prefixes")
    func spoken() {
        #expect(
            Tokens.spokenText(twoChanges, labels: labels)
                == "The Inserted: planning meeting is on Deleted: Monday Inserted: Tuesday ."
        )
        let custom = MonarchAiInlineDiffLabels(deleted: "Removed:", inserted: "Added:")
        #expect(Tokens.spokenText(twoChanges, labels: custom).contains("Removed: Monday Added: Tuesday"))
    }

    @Test("Position, button names and announcements")
    func strings() {
        #expect(Tokens.position(1, total: 5, labels: labels) == "Change 2 of 5")
        #expect(Tokens.position(0, total: 0, labels: labels) == "No suggested changes")
        #expect(Tokens.buttonLabel(.accept, index: 1, total: 5, labels: labels) == "Accept change 2 of 5")
        #expect(Tokens.buttonLabel(.reject, index: 0, total: 1, labels: labels) == "Reject change 1 of 1")
        #expect(Tokens.announcement(.accept, left: 3, labels: labels) == "Change accepted. 3 left.")
        #expect(Tokens.announcement(.reject, left: 0, labels: labels) == "Change rejected. No changes left.")
        #expect(Tokens.announcement(.accept, left: -1, labels: labels) == "Change accepted. No changes left.")
        let custom = MonarchAiInlineDiffLabels(
            position: { "\($0)/\($1)" },
            accept: { _, _ in "Keep" },
            announce: { decision, left in "\(decision.rawValue) \(left)" }
        )
        #expect(Tokens.position(1, total: 5, labels: custom) == "2/5")
        #expect(Tokens.buttonLabel(.accept, index: 0, total: 1, labels: custom) == "Keep")
        #expect(Tokens.announcement(.reject, left: 2, labels: custom) == "reject 2")
    }

    @Test("Built-in strings resolve")
    func builtIn() {
        let all = [
            MonarchAiInlineDiffStrings.label, MonarchAiInlineDiffStrings.deleted,
            MonarchAiInlineDiffStrings.inserted, MonarchAiInlineDiffStrings.none,
            MonarchAiInlineDiffStrings.previous, MonarchAiInlineDiffStrings.next,
            MonarchAiInlineDiffStrings.acceptTitle, MonarchAiInlineDiffStrings.rejectTitle,
            MonarchAiInlineDiffStrings.position(1, 2), MonarchAiInlineDiffStrings.accept(1, 2),
            MonarchAiInlineDiffStrings.reject(1, 2), MonarchAiInlineDiffStrings.announce(.accept, left: 1),
            MonarchAiInlineDiffStrings.announce(.reject, left: 0),
        ]
        for string in all {
            #expect(!string.hasPrefix("monarch."))
        }
    }
}
