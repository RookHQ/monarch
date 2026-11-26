import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let leaderboardAgents = [
    MonarchLeaderboardEntry("Ana Lima", value: 42, change: 0.12, badge: "3 breached", badgeTone: .danger),
    MonarchLeaderboardEntry("Sam Ortiz", value: 42, secondary: "Tier 2 support", isCurrent: true),
    MonarchLeaderboardEntry("Kai Chen", value: 17, change: -0.05),
    MonarchLeaderboardEntry("Noor Haddad", value: 9, avatarShape: .square),
]

enum MonarchLeaderboardFixtures: ComponentFixtures {
    static let id = "leaderboard"
    static let fixtures = [
        Fixture("ranked, ties, current row, badge, change, more") {
            MonarchLeaderboard(
                leaderboardAgents,
                label: "Tickets solved",
                valueLabel: "tickets solved",
                sort: .descending,
                changeComparison: "vs last month",
                highlightsTop: true,
                total: 12
            )
        },
        Fixture("percent, no avatars or bars, ascending, tappable, more button") {
            MonarchLeaderboard(
                [
                    MonarchLeaderboardEntry("Step 2 of Spring launch", value: 0.31),
                    MonarchLeaderboardEntry("Intro email", value: 0.12, display: "12 percent"),
                ],
                sort: .ascending,
                format: .percent,
                showsAvatars: false,
                showsBars: false,
                limit: 1,
                onShowMore: {},
                onSelect: { _ in }
            )
        },
        Fixture("compact values and a long name") {
            MonarchLeaderboard(
                [
                    MonarchLeaderboardEntry(
                        "The northern region enterprise accounts team with a very long name",
                        value: 1_250_000
                    ),
                    MonarchLeaderboardEntry("Retail", value: 0),
                ],
                isCompact: true
            )
        },
        Fixture("loading") {
            MonarchLeaderboard([], isLoading: true, loadingRows: 3)
        },
        Fixture("empty") {
            MonarchLeaderboard([], labels: MonarchLeaderboardLabels(empty: "No agents yet"))
        },
        Fixture("failed, with and without retry") {
            VStack {
                MonarchLeaderboard([], errorMessage: "Couldn’t load the ranking.", onRetry: {})
                MonarchLeaderboard([], errorMessage: "Couldn’t load the ranking.", isRetrying: true, onRetry: {})
                MonarchLeaderboard([], errorMessage: "Couldn’t load the ranking.")
            }
        },
    ]
}

@Suite("MonarchLeaderboard")
struct MonarchLeaderboardTests {
    private typealias Tokens = MonarchLeaderboardTokens
    private let english = Locale(identifier: "en_US")

    private func entry(_ name: String, _ value: Double) -> MonarchLeaderboardEntry {
        MonarchLeaderboardEntry(name, value: value)
    }

    @Test("Equal values share a rank and the next rank skips")
    func ties() {
        let ranked = Tokens.rank([entry("a", 5), entry("b", 9), entry("c", 9), entry("d", 1)], sort: .descending)
        #expect(ranked.map(\.entry.name) == ["b", "c", "a", "d"])
        #expect(ranked.map(\.rank) == [1, 1, 3, 4])
    }

    @Test("Ascending ranks the smallest first; none keeps the order")
    func sorting() {
        let entries = [entry("a", 5), entry("b", 2), entry("c", 2)]
        let ascending = Tokens.rank(entries, sort: .ascending)
        #expect(ascending.map(\.entry.name) == ["b", "c", "a"])
        #expect(ascending.map(\.rank) == [1, 1, 3])
        let given = Tokens.rank(entries, sort: .none)
        #expect(given.map(\.entry.name) == ["a", "b", "c"])
        #expect(given.map(\.rank) == [1, 2, 2])
        #expect(Tokens.rank([], sort: .descending).isEmpty)
    }

    @Test("A limit shows the first rows; hidden counts from the total")
    func limitAndHidden() {
        let ranked = Tokens.rank([entry("a", 3), entry("b", 2), entry("c", 1)], sort: .none)
        #expect(Tokens.shown(ranked, limit: 2).map(\.entry.name) == ["a", "b"])
        #expect(Tokens.shown(ranked, limit: nil).count == 3)
        #expect(Tokens.shown(ranked, limit: 0).count == 3)
        #expect(Tokens.hidden(shown: 2, entries: 3, total: nil) == 1)
        #expect(Tokens.hidden(shown: 3, entries: 3, total: 12) == 9)
        #expect(Tokens.hidden(shown: 3, entries: 3, total: 2) == 0)
    }

    @Test("Bars are relative to the largest magnitude shown")
    func share() {
        let ranked = Tokens.rank([entry("a", 40), entry("b", -10), entry("c", 0)], sort: .none)
        #expect(Tokens.share(40, among: ranked) == 1)
        #expect(Tokens.share(-10, among: ranked) == 0.25)
        #expect(Tokens.share(0, among: ranked) == 0)
        #expect(Tokens.share(5, among: Tokens.rank([entry("z", 0)], sort: .none)) == 0)
    }

    @Test("Values use the entry's text or the board's format")
    func values() {
        #expect(Tokens.valueText(entry("a", 1_234), format: .number, isCompact: false, locale: english) == "1,234")
        #expect(Tokens.valueText(entry("a", 1_200), format: .number, isCompact: true, locale: english) == "1.2K")
        #expect(Tokens.valueText(entry("a", 0.38), format: .percent, isCompact: false, locale: english) == "38%")
        let shown = MonarchLeaderboardEntry("a", value: 4.2, display: "4.2 h")
        #expect(Tokens.valueText(shown, format: .number, isCompact: false, locale: english) == "4.2 h")
    }

    @Test("Only the top three get the filled marker, and only when asked")
    func rankMarker() {
        for rank in 1...3 {
            let marker = Tokens.rankMarker(rank: rank, highlightsTop: true)
            #expect(marker.fill == \MonarchColorTokens.bgMuted)
            #expect(marker.text == \MonarchColorTokens.textDefault)
        }
        #expect(Tokens.rankMarker(rank: 4, highlightsTop: true).fill == nil)
        #expect(Tokens.rankMarker(rank: 1, highlightsTop: false).fill == nil)
        #expect(Tokens.rankMarker(rank: 1, highlightsTop: false).text == \MonarchColorTokens.textMuted)
    }

    @Test("A row reads rank, name, you, badge, secondary, value with its unit, and change")
    func rowLabel() {
        let labels = MonarchLeaderboardLabels()
        let ranked = Tokens.Ranked(
            entry: MonarchLeaderboardEntry(
                "Ana Lima",
                value: 42,
                secondary: "Tier 2",
                change: 0.12,
                badge: "3 breached",
                isCurrent: true
            ),
            rank: 1
        )
        let change = Tokens.changeLabel(ranked.entry, comparison: nil, goodDirection: .up, locale: english)
        #expect(change == "up 12%, better")
        #expect(
            Tokens.rowLabel(
                ranked,
                valueText: "42",
                valueLabel: "tickets solved",
                changeLabel: change,
                labels: labels
            ) == "Rank 1: Ana Lima, (you), 3 breached, Tier 2, 42 tickets solved, up 12%, better"
        )
        let plain = Tokens.Ranked(entry: entry("Kai", 3), rank: 4)
        #expect(
            Tokens.rowLabel(plain, valueText: "3", valueLabel: nil, changeLabel: nil, labels: labels)
                == "Rank 4: Kai, 3"
        )
        let custom = MonarchLeaderboardLabels(you: "(moi)", rank: { "#\($0)" })
        let mine = Tokens.Ranked(entry: MonarchLeaderboardEntry("Moi", value: 1, isCurrent: true), rank: 2)
        #expect(
            Tokens.rowLabel(mine, valueText: "1", valueLabel: nil, changeLabel: nil, labels: custom)
                == "#2 Moi, (moi), 1"
        )
    }

    @Test("A worse change is spoken as worse when lower is better")
    func changeDirection() {
        let slower = MonarchLeaderboardEntry("a", value: 1, change: 0.2)
        #expect(
            Tokens.changeLabel(slower, comparison: "vs last week", goodDirection: .down, locale: english)
                == "up 20% vs last week, worse"
        )
        #expect(Tokens.changeLabel(entry("a", 1), comparison: nil, goodDirection: .up, locale: english) == nil)
    }

    @Test("Loading wins over a failure, a failure over empty")
    func phase() {
        #expect(Tokens.phase(isLoading: true, errorMessage: "x", shownCount: 3) == .loading)
        #expect(Tokens.phase(isLoading: false, errorMessage: "x", shownCount: 3) == .failed)
        #expect(Tokens.phase(isLoading: false, errorMessage: "", shownCount: 0) == .empty)
        #expect(Tokens.phase(isLoading: false, errorMessage: nil, shownCount: 2) == .ready)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchLeaderboardStrings.you == "(you)")
        #expect(MonarchLeaderboardStrings.empty == "Nothing to rank yet")
        #expect(MonarchLeaderboardStrings.loading == "Loading ranking")
        #expect(MonarchLeaderboardStrings.more(4) == "4 more")
        #expect(MonarchLeaderboardStrings.rank(1) == "Rank 1:")
        #expect(Tokens.moreText(4, labels: MonarchLeaderboardLabels(more: { "+\($0)" })) == "+4")
    }
}
