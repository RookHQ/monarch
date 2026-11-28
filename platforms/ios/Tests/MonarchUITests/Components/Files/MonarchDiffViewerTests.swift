import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let diffBefore = "Ship on Friday.\nTell support.\nKeep the old banner."
private let diffAfter = "Ship on Monday.\nTell support.\nUpdate the status page.\nPost in the channel."

enum MonarchDiffViewerFixtures: ComponentFixtures {
    static let id = "diff-viewer"
    static let fixtures = [
        Fixture("unified, word changes") {
            MonarchDiffViewer(before: diffBefore, after: diffAfter, beforeLabel: "Version 3", afterLabel: "Version 4")
        },
        Fixture("side by side, no line numbers, actions") {
            MonarchDiffViewer(
                before: diffBefore,
                after: diffAfter,
                mode: .constant(.split),
                showsLineNumbers: false
            ) {
                MonarchButton("Restore", size: .sm) {}
            }
        },
        Fixture("folded context, line granularity") {
            MonarchDiffViewer(
                before: (1...20).map { "Line \($0)" },
                after: (1...20).map { $0 == 10 ? "Line ten" : "Line \($0)" },
                granularity: .line,
                context: 1
            )
        },
        Fixture("no changes") {
            MonarchDiffViewer(before: "Same text", after: "Same text")
        },
        Fixture("only added, long lines, blank lines") {
            MonarchDiffViewer(
                before: "",
                after: "A very long paragraph that has to wrap over several lines on a phone screen.\n\nEnd"
            )
        },
        Fixture("custom labels, mode switch hidden") {
            MonarchDiffViewer(
                before: ["Paragraph one", "Paragraph two"],
                after: ["Paragraph one", "Paragraph 2"],
                showsModeSwitch: false,
                labels: MonarchDiffViewerLabels(stats: { "\($0) new, \($1) gone" }, noChanges: "Identical")
            )
        },
    ]
}

@Suite("MonarchDiffViewer")
@MainActor
struct MonarchDiffViewerTests {
    private let labels = MonarchDiffViewerLabels()

    @Test("The edit script follows a longest common subsequence, deletions first")
    func sequences() {
        let operations = MonarchTextDiff.sequences(Array("abc"), Array("axc"))
        #expect(operations == [.equal("a"), .delete("b"), .insert("x"), .equal("c")])
        #expect(MonarchTextDiff.sequences([Int](), [1, 2]) == [.insert(1), .insert(2)])
        #expect(MonarchTextDiff.sequences([1, 2], [Int]()) == [.delete(1), .delete(2)])
        let capped = MonarchTextDiff.sequences([1, 2], [1, 2], maxPairs: 3)
        #expect(capped == [.delete(1), .delete(2), .insert(1), .insert(2)])
    }

    @Test("Text splits on every kind of line break; empty text has no lines")
    func split() {
        #expect(MonarchDiffViewerLine.lines(of: "") == [])
        #expect(MonarchDiffViewerLine.lines(of: "a\r\nb\rc\nd") == ["a", "b", "c", "d"])
        #expect(MonarchDiffViewerLine.lines(of: "a\n") == ["a", ""])
    }

    @Test("Lines are numbered on both sides")
    func numbering() {
        let lines = MonarchDiffViewerLine.diff(from: ["a", "b", "c"], to: ["a", "x", "c", "d"])
        #expect(
            lines == [
                MonarchDiffViewerLine(.same, text: "a", oldNumber: 1, newNumber: 1),
                MonarchDiffViewerLine(.removed, text: "b", oldNumber: 2),
                MonarchDiffViewerLine(.added, text: "x", newNumber: 2),
                MonarchDiffViewerLine(.same, text: "c", oldNumber: 3, newNumber: 3),
                MonarchDiffViewerLine(.added, text: "d", newNumber: 4),
            ]
        )
        let stats = MonarchTextDiff.stats(lines)
        #expect(stats.added == 2 && stats.removed == 1)
    }

    @Test("Words, spaces and punctuation are the units of a word diff")
    func words() {
        #expect(MonarchTextDiff.tokens("Ship on Friday.") == ["Ship", " ", "on", " ", "Friday", "."])
        #expect(MonarchTextDiff.tokens("a_b  c!!") == ["a_b", "  ", "c", "!", "!"])
        let segments = MonarchTextDiff.words("Ship on Friday.", "Ship on Monday.")
        #expect(
            segments == [
                MonarchDiffSegment(kind: .same, text: "Ship on "),
                MonarchDiffSegment(kind: .removed, text: "Friday"),
                MonarchDiffSegment(kind: .added, text: "Monday"),
                MonarchDiffSegment(kind: .same, text: "."),
            ]
        )
        #expect(
            MonarchDiffViewerTokens.visibleSegments(segments, forKind: .removed).map(\.text) == [
                "Ship on ", "Friday", ".",
            ]
        )
        #expect(
            MonarchDiffViewerTokens.visibleSegments(segments, forKind: .added).map(\.text) == [
                "Ship on ", "Monday", ".",
            ]
        )
    }

    @Test("Removed and added lines of a change pair up for the word diff")
    func pairing() {
        let lines = MonarchDiffViewerLine.diff(from: ["a b", "c d", "same"], to: ["a x", "same"])
        let map = MonarchTextDiff.wordSegments(lines)
        // Lines: removed "a b", removed "c d", added "a x", same.
        #expect(map.keys.sorted() == [0, 2])
        #expect(map[0] == map[2])
    }

    @Test("Unchanged runs fold beyond the context, but not at a single extra line")
    func folding() {
        let before = (1...20).map { "Line \($0)" }
        let after = before.enumerated().map { $0.offset == 9 ? "Changed" : $0.element }
        let lines = MonarchDiffViewerLine.diff(from: before, to: after)
        let folded = MonarchTextDiff.rows(lines, isSplit: false, context: 3, expanded: [])
        #expect(folded.hunks == 1)
        #expect(folded.rows.first == .gap(id: 0, lines: Array(0..<6)))
        #expect(folded.rows[1] == .line(6, hunk: nil))
        #expect(folded.rows[4] == .line(9, hunk: 0))
        #expect(folded.rows[5] == .line(10, hunk: nil))
        #expect(folded.rows.last == .gap(id: 1, lines: Array(14..<21)))
        let open = MonarchTextDiff.rows(lines, isSplit: false, context: 3, expanded: [0, 1])
        #expect(open.rows.count == lines.count)
        let never = MonarchTextDiff.rows(
            lines,
            isSplit: false,
            context: MonarchDiffViewerTokens.context(nil),
            expanded: []
        )
        #expect(never.rows.count == lines.count)
        // A run one longer than the context is shown, not folded.
        let short = MonarchDiffViewerLine.diff(from: ["a", "b", "c", "d", "e"], to: ["a", "b", "c", "d", "x"])
        #expect(MonarchTextDiff.rows(short, isSplit: false, context: 3, expanded: []).rows.count == short.count)
    }

    @Test("Side by side pairs removed with added lines; the first row of a change has the hunk")
    func sideBySide() {
        let lines = MonarchDiffViewerLine.diff(from: ["a", "b", "c"], to: ["x", "c"])
        let built = MonarchTextDiff.rows(lines, isSplit: true, context: 3, expanded: [])
        #expect(
            built.rows == [
                .pair(old: 0, new: 2, hunk: 0),
                .pair(old: 1, new: nil, hunk: nil),
                .pair(old: 3, new: 3, hunk: nil),
            ]
        )
    }

    @Test("The layout follows the app, then the reader, then the width")
    func mode() {
        #expect(MonarchDiffViewerTokens.mode(bound: nil, chosen: nil, isRegularWidth: false) == .unified)
        #expect(MonarchDiffViewerTokens.mode(bound: nil, chosen: nil, isRegularWidth: true) == .split)
        #expect(MonarchDiffViewerTokens.mode(bound: nil, chosen: .unified, isRegularWidth: true) == .unified)
        #expect(MonarchDiffViewerTokens.mode(bound: .split, chosen: .unified, isRegularWidth: false) == .split)
        #expect(MonarchDiffViewerTokens.context(-2) == 0)
    }

    @Test("Previous and next wrap around the changes")
    func navigation() {
        #expect(MonarchDiffViewerTokens.next(from: nil, hunks: 3) == 0)
        #expect(MonarchDiffViewerTokens.next(from: 2, hunks: 3) == 0)
        #expect(MonarchDiffViewerTokens.previous(from: nil, hunks: 3) == 2)
        #expect(MonarchDiffViewerTokens.previous(from: 0, hunks: 3) == 2)
        #expect(MonarchDiffViewerTokens.previous(from: 2, hunks: 3) == 1)
        #expect(MonarchDiffViewerTokens.next(from: nil, hunks: 0) == nil)
        #expect(MonarchDiffViewerTokens.positionText(1, of: 5, labels: labels) == "Change 2 of 5")
    }

    @Test("Colours mark added and removed lines and words, with a visible marker")
    func colours() {
        #expect(MonarchDiffViewerTokens.lineBackground(.added) == \MonarchColorTokens.successSubtle)
        #expect(MonarchDiffViewerTokens.lineBackground(.removed) == \MonarchColorTokens.dangerSubtle)
        #expect(MonarchDiffViewerTokens.lineBackground(.same) == nil)
        #expect(MonarchDiffViewerTokens.wordBackground(.added) == \MonarchColorTokens.successBorder)
        #expect(MonarchDiffViewerTokens.wordBackground(.removed) == \MonarchColorTokens.dangerBorder)
        #expect(MonarchDiffViewerTokens.markerRole(.added) == \MonarchColorTokens.successStrong)
        #expect(MonarchDiffViewerTokens.markerRole(.removed) == \MonarchColorTokens.dangerStrong)
        #expect(MonarchDiffViewerTokens.marker(.added) == "+")
        #expect(MonarchDiffViewerTokens.marker(.removed) == "\u{2212}")
        #expect(MonarchDiffViewerTokens.statsText(added: 3) == "+3")
        #expect(MonarchDiffViewerTokens.statsText(removed: 1) == "\u{2212}1")
    }

    @Test("Line numbers are padded to the widest")
    func numbers() {
        let lines = MonarchDiffViewerLine.diff(from: (1...120).map(String.init), to: (1...120).map(String.init))
        #expect(MonarchDiffViewerTokens.numberDigits(lines) == 3)
        #expect(MonarchDiffViewerTokens.numberText(7, digits: 3) == "  7")
        #expect(MonarchDiffViewerTokens.numberText(nil, digits: 2) == "  ")
    }

    @Test("Lines and rows read their kind, side and text")
    func spoken() {
        let added = MonarchDiffViewerLine(.added, text: "Monday", newNumber: 1)
        let removed = MonarchDiffViewerLine(.removed, text: "Friday", oldNumber: 1)
        let blank = MonarchDiffViewerLine(.same, text: "  ", oldNumber: 2, newNumber: 2)
        #expect(MonarchDiffViewerTokens.spokenLine(added, labels: labels) == "Added: Monday")
        #expect(MonarchDiffViewerTokens.spokenLine(removed, labels: labels) == "Removed: Friday")
        #expect(MonarchDiffViewerTokens.spokenLine(blank, labels: labels) == "Blank line")
        #expect(
            MonarchDiffViewerTokens.spokenPair(
                old: removed,
                new: added,
                beforeName: "V3",
                afterName: "V4",
                labels: labels
            )
                == "V3: Removed: Friday. V4: Added: Monday"
        )
        #expect(
            MonarchDiffViewerTokens.spokenPair(
                old: blank,
                new: blank,
                beforeName: "V3",
                afterName: "V4",
                labels: labels
            )
                == "Blank line"
        )
        #expect(MonarchDiffViewerTokens.statsLabel(added: 3, removed: 1, labels: labels) == "3 added, 1 removed")
        #expect(MonarchDiffViewerTokens.caption(before: "V3", after: "V4", labels: labels) == "Changes from V3 to V4")
        #expect(MonarchDiffViewerTokens.expandTitle(1, labels: labels) == "Show 1 unchanged line")
        #expect(MonarchDiffViewerTokens.expandTitle(12, labels: labels) == "Show 12 unchanged lines")
        let custom = MonarchDiffViewerLabels(added: "New", expand: { "+\($0)" })
        #expect(MonarchDiffViewerTokens.spokenLine(added, labels: custom) == "New: Monday")
        #expect(MonarchDiffViewerTokens.expandTitle(4, labels: custom) == "+4")
    }

    @Test("Built-in strings resolve")
    func strings() {
        let all = [
            MonarchDiffViewerStrings.before, MonarchDiffViewerStrings.after, MonarchDiffViewerStrings.mode,
            MonarchDiffViewerStrings.unified, MonarchDiffViewerStrings.split, MonarchDiffViewerStrings.added,
            MonarchDiffViewerStrings.removed, MonarchDiffViewerStrings.blankLine, MonarchDiffViewerStrings.previous,
            MonarchDiffViewerStrings.next, MonarchDiffViewerStrings.noChanges,
        ]
        for string in all { #expect(!string.hasPrefix("monarch.")) }
    }
}
