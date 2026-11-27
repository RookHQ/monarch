import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchAiEditProposalFixtures: ComponentFixtures {
    static let id = "ai-edit-proposal"
    static let hunks = [
        MonarchAiEditHunk(id: "1", before: "The quick brown fox", after: "The slow brown fox", label: "Title"),
        MonarchAiEditHunk(id: "2", before: "Ships on Friday.", after: "Ships on Monday after the review."),
        MonarchAiEditHunk(id: "3", before: "", after: "A new closing paragraph."),
        MonarchAiEditHunk(id: "4", before: "Unchanged text.", after: "Unchanged text."),
    ]

    static func part(_ state: MonarchAiEditProposalState) -> MonarchAiEditProposalPart {
        MonarchAiEditProposalPart(id: "proposal-\(state.rawValue)", target: "doc-1", diff: .null, state: state)
    }

    static let fixtures = [
        Fixture("proposed with every action") {
            MonarchAiEditProposal(
                part(.proposed),
                hunks: hunks,
                onAcceptAll: {},
                onRejectAll: {},
                onAcceptHunk: { _ in },
                onRejectHunk: { _ in }
            )
        },
        Fixture("proposed, decisions kept by the app") {
            MonarchAiEditProposal(
                part(.proposed),
                hunks: [
                    MonarchAiEditHunk(
                        id: "a",
                        before: "Net 30",
                        after: "Net 45",
                        label: "Payment terms",
                        status: .accepted
                    ),
                    MonarchAiEditHunk(id: "b", before: "USD", after: "EUR", label: "Currency", status: .rejected),
                    MonarchAiEditHunk(id: "c", before: "Draft", after: "Final", label: "Status"),
                ],
                title: "Contract fields",
                onAcceptHunk: { _ in },
                onRejectHunk: { _ in }
            )
        },
        Fixture("applied with revert") {
            MonarchAiEditProposal(part(.applied), hunks: hunks, onRevert: {})
        },
        Fixture("rejected and reverted") {
            VStack {
                MonarchAiEditProposal(part(.rejected), hunks: Array(hunks.prefix(1)))
                MonarchAiEditProposal(part(.reverted), hunks: Array(hunks.prefix(1)))
            }
        },
        Fixture("long text") {
            MonarchAiEditProposal(
                part(.proposed),
                hunks: [
                    MonarchAiEditHunk(
                        id: "long",
                        before:
                            "Our quarterly revenue grew modestly compared with the previous quarter, driven mostly by renewals in the enterprise segment.",
                        after:
                            "Our quarterly revenue grew 12 percent compared with the previous quarter, driven by renewals and two new enterprise customers.",
                        label: "Executive summary, paragraph 1"
                    )
                ],
                onAcceptHunk: { _ in },
                onRejectHunk: { _ in }
            )
        },
    ]
}

@Suite("MonarchAiEditProposal")
struct MonarchAiEditProposalTests {
    private let labels = MonarchAiEditProposalLabels()
    private typealias Segment = MonarchAiEditProposalSegment

    @Test("The word diff marks one removal and one insertion per change")
    func diff() {
        #expect(
            MonarchAiEditProposalDiff.words("The quick fox", "The slow fox") == [
                Segment(kind: .equal, text: "The "),
                Segment(kind: .delete, text: "quick"),
                Segment(kind: .insert, text: "slow"),
                Segment(kind: .equal, text: " fox"),
            ]
        )
        // Whitespace alone between two changes joins them into one.
        #expect(
            MonarchAiEditProposalDiff.words("a big red dog", "a small blue dog") == [
                Segment(kind: .equal, text: "a "),
                Segment(kind: .delete, text: "big red"),
                Segment(kind: .insert, text: "small blue"),
                Segment(kind: .equal, text: " dog"),
            ]
        )
        #expect(MonarchAiEditProposalDiff.words("", "New text") == [Segment(kind: .insert, text: "New text")])
        #expect(MonarchAiEditProposalDiff.words("Old", "") == [Segment(kind: .delete, text: "Old")])
        #expect(MonarchAiEditProposalDiff.words("", "").isEmpty)
        #expect(MonarchAiEditProposalDiff.words("Same.", "Same.") == [Segment(kind: .equal, text: "Same.")])
    }

    @Test("The proposal's state overrides the decisions")
    func status() {
        let hunk = MonarchAiEditHunk(id: "1", before: "a", after: "b")
        let own = MonarchAiEditHunk(id: "2", before: "a", after: "b", status: .rejected)
        let decided: [String: MonarchAiEditHunkStatus] = ["1": .accepted]
        #expect(MonarchAiEditProposalTokens.status(of: hunk, decided: [:], state: .proposed) == .proposed)
        #expect(MonarchAiEditProposalTokens.status(of: hunk, decided: decided, state: .proposed) == .accepted)
        #expect(MonarchAiEditProposalTokens.status(of: own, decided: ["2": .accepted], state: .proposed) == .rejected)
        #expect(MonarchAiEditProposalTokens.status(of: hunk, decided: [:], state: .applied) == .accepted)
        #expect(MonarchAiEditProposalTokens.status(of: own, decided: [:], state: .applied) == .rejected)
        #expect(MonarchAiEditProposalTokens.status(of: hunk, decided: decided, state: .rejected) == .rejected)
        #expect(MonarchAiEditProposalTokens.status(of: hunk, decided: decided, state: .reverted) == .proposed)
    }

    @Test("Deciding all only decides the undecided changes")
    func decideAll() {
        let hunks = MonarchAiEditProposalFixtures.hunks
        let result = MonarchAiEditProposalTokens.decideAll(
            hunks,
            statuses: [.rejected, .proposed, .proposed, .accepted],
            as: .accepted,
            decided: ["1": .rejected, "4": .accepted]
        )
        #expect(result == ["1": .rejected, "2": .accepted, "3": .accepted, "4": .accepted])
    }

    @Test("Buttons show only while the proposal is proposed")
    func visibility() {
        #expect(MonarchAiEditProposalTokens.showsAll(state: .proposed, pending: 2, hasAllActions: true))
        #expect(!MonarchAiEditProposalTokens.showsAll(state: .proposed, pending: 0, hasAllActions: true))
        #expect(!MonarchAiEditProposalTokens.showsAll(state: .applied, pending: 2, hasAllActions: true))
        #expect(!MonarchAiEditProposalTokens.showsAll(state: .proposed, pending: 2, hasAllActions: false))
        #expect(MonarchAiEditProposalTokens.showsHunkActions(state: .proposed, status: .proposed, hasHunkActions: true))
        #expect(
            !MonarchAiEditProposalTokens.showsHunkActions(state: .proposed, status: .accepted, hasHunkActions: true)
        )
        #expect(
            !MonarchAiEditProposalTokens.showsHunkActions(state: .reverted, status: .proposed, hasHunkActions: true)
        )
    }

    @Test("Focus moves to the next undecided change, else the previous, else the title")
    func focus() {
        #expect(MonarchAiEditProposalTokens.nextFocus(after: 0, statuses: [.accepted, .proposed, .proposed]) == 1)
        #expect(MonarchAiEditProposalTokens.nextFocus(after: 2, statuses: [.proposed, .accepted, .rejected]) == 0)
        #expect(MonarchAiEditProposalTokens.nextFocus(after: 1, statuses: [.accepted, .accepted]) == nil)
    }

    @Test("Removed text is struck and added text underlined; a decision mutes the losing side")
    func styles() {
        let removed = MonarchAiEditProposalTokens.style(.delete, status: .proposed)
        let added = MonarchAiEditProposalTokens.style(.insert, status: .proposed)
        #expect(removed.background == \MonarchColorTokens.aiDiffRemove)
        #expect(removed.foreground == \MonarchColorTokens.aiDiffRemoveStrong)
        #expect(removed.isStruck && !removed.isUnderlined)
        #expect(added.background == \MonarchColorTokens.aiDiffAdd)
        #expect(added.foreground == \MonarchColorTokens.aiDiffAddStrong)
        #expect(added.isUnderlined && !added.isStruck)
        #expect(
            MonarchAiEditProposalTokens.style(.delete, status: .accepted).foreground == \MonarchColorTokens.textSubtle
        )
        #expect(MonarchAiEditProposalTokens.style(.insert, status: .accepted).background == nil)
        #expect(
            MonarchAiEditProposalTokens.style(.insert, status: .rejected).foreground == \MonarchColorTokens.textSubtle
        )
        #expect(!MonarchAiEditProposalTokens.style(.delete, status: .rejected).isStruck)
        #expect(MonarchAiEditProposalTokens.style(.equal, status: .proposed).background == nil)
    }

    @Test("Each state has a tone, and each decision a colour")
    func tones() {
        #expect(MonarchAiEditProposalTokens.tone(.proposed) == .info)
        #expect(MonarchAiEditProposalTokens.tone(.applied) == .success)
        #expect(MonarchAiEditProposalTokens.tone(.rejected) == .neutral)
        #expect(MonarchAiEditProposalTokens.tone(.reverted) == .warning)
        #expect(MonarchAiEditProposalTokens.statusRole(.accepted) == \MonarchColorTokens.successStrong)
        #expect(MonarchAiEditProposalTokens.statusRole(.rejected) == \MonarchColorTokens.textMuted)
    }

    @Test("VoiceOver reads the diff with Deleted and Inserted")
    func spoken() {
        let segments = MonarchAiEditProposalDiff.words("The quick fox", "The slow fox")
        let diff = MonarchAiEditProposalTokens.spokenDiff(segments, deleted: "Deleted:", inserted: "Inserted:")
        #expect(diff == "The Deleted: quick Inserted: slow fox")
        #expect(
            MonarchAiEditProposalTokens.hunkLabel(name: "Title", status: "Accepted", diff: diff)
                == "Title. Accepted. The Deleted: quick Inserted: slow fox"
        )
        #expect(MonarchAiEditProposalTokens.hunkLabel(name: "Title", status: nil, diff: "x") == "Title. x")
        #expect(MonarchAiEditProposalStrings.action("Accept", "Change 2 of 3") == "Accept, Change 2 of 3")
    }

    @Test("Counts, names, notes and announcements")
    func texts() {
        #expect(MonarchAiEditProposalTokens.countText(total: 3, pending: 3, labels: labels) == "3 changes")
        #expect(MonarchAiEditProposalTokens.countText(total: 1, pending: 1, labels: labels) == "1 change")
        #expect(MonarchAiEditProposalTokens.countText(total: 3, pending: 1, labels: labels) == "1 of 3 changes left")
        #expect(
            MonarchAiEditProposalTokens.summary(state: .applied, total: 3, pending: 0, labels: labels)
                == "These edits were applied."
        )
        let unnamed = MonarchAiEditHunk(id: "x", before: "a", after: "b")
        #expect(MonarchAiEditProposalTokens.hunkName(unnamed, index: 1, total: 3, labels: labels) == "Change 2 of 3")
        #expect(
            MonarchAiEditProposalTokens.hunkName(
                MonarchAiEditProposalFixtures.hunks[0],
                index: 0,
                total: 3,
                labels: labels
            )
                == "Title"
        )
        #expect(
            MonarchAiEditProposalTokens.announcement(.accepted, left: 2, labels: labels) == "Change accepted. 2 left."
        )
        #expect(
            MonarchAiEditProposalTokens.announcement(.rejected, left: 0, labels: labels)
                == "Change rejected. No changes left."
        )
        #expect(
            MonarchAiEditProposalTokens.announcement(.acceptedAll, left: 0, labels: labels) == "All changes accepted."
        )
        #expect(
            MonarchAiEditProposalTokens.announcement(.rejectedAll, left: 0, labels: labels) == "All changes rejected."
        )
        let custom = MonarchAiEditProposalLabels(count: { "\($1)/\($0)" }, announcement: { _, left in "\(left)" })
        #expect(MonarchAiEditProposalTokens.countText(total: 3, pending: 2, labels: custom) == "2/3")
        #expect(MonarchAiEditProposalTokens.announcement(.accepted, left: 4, labels: custom) == "4")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAiEditProposalStrings.title == "Suggested edits")
        #expect(MonarchAiEditProposalStrings.badge == "AI generated")
        #expect(MonarchAiEditProposalStrings.state(.reverted) == "Reverted")
        #expect(MonarchAiEditProposalStrings.hunkState(.accepted) == "Accepted")
        #expect(MonarchAiEditProposalStrings.acceptAll == "Accept all")
        #expect(MonarchAiEditProposalStrings.revert == "Revert")
        #expect(MonarchAiEditProposalStrings.unchanged == "No change")
        #expect(MonarchAiEditProposalStrings.readOnly(.rejected) == "These edits were rejected.")
    }
}
