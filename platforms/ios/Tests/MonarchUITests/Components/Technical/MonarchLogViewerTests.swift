import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchLogViewerFixtures: ComponentFixtures {
    static let id = "log-viewer"

    /// 2026-03-04 15:00:00.250 UTC, fixed so nothing depends on the clock.
    static let start = Date(timeIntervalSince1970: 1_772_636_400.25)
    static let utc = TimeZone(identifier: "UTC") ?? .gmt

    static let lines: [MonarchLogLine] = [
        MonarchLogLine(id: "1", message: "Starting build 4812", level: .info, time: start, source: "runner"),
        MonarchLogLine(id: "2", message: "Resolved 214 packages", level: .debug, time: start.addingTimeInterval(1.5)),
        MonarchLogLine(
            id: "3",
            message:
                "Cache miss for key node-modules-7f3a; downloading 312 MB from the artifact store, which can take a while",
            level: .warn,
            time: start.addingTimeInterval(2),
            source: "cache"
        ),
        MonarchLogLine(id: "4", message: "Test suite api failed: 3 of 120 tests", level: .error, time: start + 61),
        MonarchLogLine(id: "5", message: "Uploading artifacts"),
    ]

    static let fixtures = [
        Fixture("every level, timestamps, sources, following") {
            MonarchLogViewer(lines).frame(height: 360).environment(\.timeZone, utc)
        },
        Fixture("wrapped with line numbers and highlighted search") {
            MonarchLogViewer(
                lines,
                query: .constant("build"),
                isWrapped: .constant(true),
                searchMode: .highlight,
                showsLineNumbers: true
            )
            .frame(height: 360)
            .environment(\.timeZone, utc)
        },
        Fixture("paused with a filter, no timestamps, an extra action") {
            MonarchLogViewer(
                lines,
                levels: .constant([.warn, .error]),
                isFollowing: .constant(false),
                showsTimestamps: false
            ) {
                Button("Clear") {}.buttonStyle(.monarch(.ghost, size: .sm))
            }
            .frame(height: 360)
        },
        Fixture("no matches") {
            MonarchLogViewer(lines, query: .constant("no such text")).frame(height: 360)
        },
        Fixture("loading, empty and own labels") {
            VStack {
                MonarchLogViewer([], isLoading: true).frame(height: 240)
                MonarchLogViewer([], labels: MonarchLogViewerLabels(empty: "Waiting for the first line")).frame(
                    height: 240
                )
            }
        },
    ]
}

@Suite("MonarchLogViewer")
struct MonarchLogViewerTests {
    private let lines = MonarchLogViewerFixtures.lines
    private let all = Set(MonarchLogLevel.allCases)

    @Test("Level filters hide lines and keep positions in the full list")
    func levelFilter() {
        let visible = MonarchLogViewerTokens.visible(lines, levels: [.warn, .error], query: "", mode: .filter)
        #expect(visible.map(\.line.id) == ["3", "4"])
        #expect(visible.map(\.number) == [3, 4])
        #expect(MonarchLogViewerTokens.visible(lines, levels: [], query: "", mode: .filter).isEmpty)
    }

    @Test("Search in filter mode matches message or source, ignoring case and outer spaces")
    func searchFilter() {
        let byMessage = MonarchLogViewerTokens.visible(lines, levels: all, query: "  BUILD ", mode: .filter)
        #expect(byMessage.map(\.line.id) == ["1"])
        let bySource = MonarchLogViewerTokens.visible(lines, levels: all, query: "cache", mode: .filter)
        #expect(bySource.map(\.line.id) == ["3"])
    }

    @Test("Search in highlight mode keeps every line")
    func searchHighlight() {
        let visible = MonarchLogViewerTokens.visible(lines, levels: all, query: "build", mode: .highlight)
        #expect(visible.count == lines.count)
    }

    @Test("Counts per level include every level, even at zero")
    func counts() {
        let counts = MonarchLogViewerTokens.counts(lines)
        #expect(counts == [.debug: 1, .info: 2, .warn: 1, .error: 1])
        #expect(MonarchLogViewerTokens.counts([]) == [.debug: 0, .info: 0, .warn: 0, .error: 0])
    }

    @Test("Match ranges find every occurrence, ignoring case, without overlap")
    func matchRanges() {
        let text = "Aa aaA"
        let ranges = MonarchLogViewerTokens.matchRanges(of: "aa", in: text)
        #expect(ranges.map { String(text[$0]) } == ["Aa", "aa"])
        #expect(MonarchLogViewerTokens.matchRanges(of: "", in: text).isEmpty)
        #expect(MonarchLogViewerTokens.matchRanges(of: "z", in: text).isEmpty)
    }

    @Test("The time column is 24-hour with milliseconds, in the given time zone")
    func timeText() {
        let start = MonarchLogViewerFixtures.start
        #expect(MonarchLogViewerTokens.timeText(start, timeZone: MonarchLogViewerFixtures.utc) == "15:00:00.250")
        let tokyo = TimeZone(identifier: "Asia/Tokyo") ?? .gmt
        #expect(MonarchLogViewerTokens.timeText(start + 61, timeZone: tokyo) == "00:01:01.250")
    }

    @Test("Each level has its own name colour, and only warnings and errors are tinted")
    func colours() {
        let roles = MonarchLogLevel.allCases.map(MonarchLogViewerTokens.levelRole)
        #expect(Set(roles).count == MonarchLogLevel.allCases.count)
        #expect(MonarchLogViewerTokens.levelRole(.error) == \MonarchColorTokens.dangerStrong)
        #expect(MonarchLogViewerTokens.lineBackground(.debug) == nil)
        #expect(MonarchLogViewerTokens.lineBackground(.info) == nil)
        #expect(MonarchLogViewerTokens.lineBackground(.warn) == \MonarchColorTokens.warningSubtle)
        #expect(MonarchLogViewerTokens.lineBackground(.error) == \MonarchColorTokens.dangerSubtle)
    }

    @Test("A line is read as level, time, source and message")
    func accessibilityLabel() {
        let label = MonarchLogViewerTokens.accessibilityLabel(lines[2], levelName: "Warn", time: "15:00:02.250")
        #expect(label.hasPrefix("Warn, 15:00:02.250, cache, Cache miss"))
        let plain = MonarchLogViewerTokens.accessibilityLabel(lines[4], levelName: "Info", time: nil)
        #expect(plain == "Info, Uploading artifacts")
    }

    @MainActor
    @Test("The line text has the columns, and matches are marked in the message")
    func lineText() {
        let entry = MonarchLogViewerTokens.Entry(line: lines[0], number: 1)
        let text = MonarchLogViewerTokens.lineText(
            entry,
            needle: "build",
            showsTimestamps: true,
            showsLineNumbers: true,
            numberWidth: 3,
            timeZone: MonarchLogViewerFixtures.utc,
            levelName: "Info",
            colors: MonarchTheme.light.colors
        )
        #expect(String(text.characters) == "  1  15:00:00.250  Info  runner  Starting build 4812")
        let underlined = text.runs
            .filter { $0[AttributeScopes.SwiftUIAttributes.UnderlineStyleAttribute.self] != nil }
            .map { String(text[$0.range].characters) }
        #expect(underlined == ["build"])
    }

    @Test("Filters toggle levels on and off")
    func toggled() {
        let off = MonarchLogViewerTokens.toggled(all, .debug, isOn: false)
        #expect(off == [.info, .warn, .error])
        #expect(MonarchLogViewerTokens.toggled(off, .debug, isOn: true) == all)
    }

    @Test("Lines that arrived while paused are counted, and the jump title says so")
    func unseen() {
        let labels = MonarchLogViewerLabels()
        #expect(MonarchLogViewerTokens.unseen(total: 10, pausedAt: nil) == 0)
        #expect(MonarchLogViewerTokens.unseen(total: 10, pausedAt: 7) == 3)
        #expect(MonarchLogViewerTokens.unseen(total: 5, pausedAt: 7) == 0)
        #expect(MonarchLogViewerTokens.jumpTitle(unseen: 0, labels: labels) == "Jump to latest")
        #expect(MonarchLogViewerTokens.jumpTitle(unseen: 1, labels: labels) == "1 new line")
        #expect(MonarchLogViewerTokens.jumpTitle(unseen: 3, labels: labels) == "3 new lines")
    }

    @Test("Only a mostly vertical drag toward older lines pauses following")
    func drag() {
        #expect(MonarchLogViewerTokens.isDragTowardOlder(CGSize(width: 0, height: 40)))
        #expect(!MonarchLogViewerTokens.isDragTowardOlder(CGSize(width: 0, height: -40)))
        #expect(!MonarchLogViewerTokens.isDragTowardOlder(CGSize(width: 80, height: 20)))
        #expect(!MonarchLogViewerTokens.isDragTowardOlder(CGSize(width: 0, height: 2)))
    }

    @Test("The scrolling area shows loading, empty, no matches or lines")
    func content() {
        #expect(MonarchLogViewerTokens.content(isLoading: true, total: 0, visible: 0) == .loading)
        #expect(MonarchLogViewerTokens.content(isLoading: false, total: 0, visible: 0) == .empty)
        #expect(MonarchLogViewerTokens.content(isLoading: true, total: 4, visible: 0) == .noMatches)
        #expect(MonarchLogViewerTokens.content(isLoading: false, total: 4, visible: 2) == .lines)
    }

    @Test("Built-in strings resolve, and labels can replace them")
    func strings() {
        let labels = MonarchLogViewerLabels()
        for text in [
            labels.log, labels.levels, labels.search, labels.searchPrompt, labels.clearSearch, labels.follow,
            labels.wrap, labels.jumpToLatest, labels.followPaused, labels.followResumed, labels.empty,
            labels.noMatches, labels.loading,
        ] {
            #expect(!text.hasPrefix("monarch."))
        }
        #expect(MonarchLogLevel.allCases.map(labels.levelName) == ["Debug", "Info", "Warn", "Error"])
        #expect(labels.count(5, 5) == "5 lines")
        #expect(labels.count(1, 1) == "1 line")
        #expect(labels.count(2, 5) == "2 of 5 lines")
        #expect(MonarchLogViewerTokens.followAnnouncement(isFollowing: false, labels: labels) == "Following paused")
        #expect(MonarchLogViewerTokens.followAnnouncement(isFollowing: true, labels: labels) == "Following new lines")
        let custom = MonarchLogViewerLabels(levelName: { _ in "L" }, count: { "\($0)/\($1)" })
        #expect(custom.levelName(.error) == "L")
        #expect(custom.count(2, 5) == "2/5")
    }
}
