import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchAiBackgroundTasksFixtures: ComponentFixtures {
    static let id = "ai-background-tasks"
    private static let now = Date(timeIntervalSince1970: 1_794_650_000)
    private static let height = MonarchTokens.Space.space24 * 6

    private static let all = [
        MonarchAiBackgroundTask(
            "Summarize 40 contracts",
            id: "1",
            status: .running,
            agent: "Research agent",
            progress: 0.3,
            step: "Reading contract 12 of 40",
            startedAt: now.addingTimeInterval(-192)
        ),
        MonarchAiBackgroundTask(
            "Find duplicate contacts",
            id: "2",
            status: .running,
            startedAt: now.addingTimeInterval(-42)
        ),
        MonarchAiBackgroundTask("Translate the help center", id: "3", status: .queued),
        MonarchAiBackgroundTask(
            "Draft replies to 12 tickets",
            id: "4",
            status: .needsInput,
            progress: 0.5,
            step: "Which tone should the replies use?",
            startedAt: now.addingTimeInterval(-900)
        ),
        MonarchAiBackgroundTask(
            "Clean up the CRM",
            id: "5",
            status: .failed,
            endedAt: now.addingTimeInterval(-300),
            error: "The CRM did not answer in time."
        ),
        MonarchAiBackgroundTask(
            "Weekly report",
            id: "6",
            status: .succeeded,
            endedAt: now.addingTimeInterval(-3_600),
            result: "Report saved to Documents."
        ),
        MonarchAiBackgroundTask(
            "Import invoices",
            id: "7",
            status: .cancelled,
            endedAt: now.addingTimeInterval(-7_200)
        ),
    ]

    static let fixtures = [
        Fixture("every status with every action") {
            NavigationStack {
                MonarchAiBackgroundTasks(
                    tasks: all,
                    now: now,
                    onOpen: { _ in },
                    onCancel: { _ in },
                    onRetry: { _ in },
                    onRespond: { _ in },
                    onDismiss: { _ in },
                    onClearFinished: { _ in }
                )
            }
            .frame(height: height)
        },
        Fixture("no actions") {
            MonarchAiBackgroundTasks(tasks: Array(all.prefix(3)), now: now)
                .frame(height: height)
        },
        Fixture("empty") {
            MonarchAiBackgroundTasks(tasks: [], now: now)
                .frame(height: height)
        },
        Fixture("button with running and needs you") {
            MonarchAiBackgroundTasksButton(tasks: all) {}
        },
        Fixture("button with queued only") {
            MonarchAiBackgroundTasksButton(tasks: [all[2]]) {}
        },
        Fixture("button with nothing running") {
            MonarchAiBackgroundTasksButton(tasks: []) {}
        },
    ]
}

@Suite("MonarchAiBackgroundTasks")
@MainActor
struct MonarchAiBackgroundTasksTests {
    private let us = Locale(identifier: "en_US")
    private let now = Date(timeIntervalSince1970: 1_794_650_000)
    private let none = MonarchAiBackgroundTasksLabels()

    private func task(
        _ id: String,
        _ status: MonarchAiBackgroundTaskStatus,
        started: TimeInterval? = nil,
        ended: TimeInterval? = nil
    ) -> MonarchAiBackgroundTask {
        MonarchAiBackgroundTask(
            id,
            id: id,
            status: status,
            startedAt: started.map { now.addingTimeInterval($0) },
            endedAt: ended.map { now.addingTimeInterval($0) }
        )
    }

    @Test("Tasks are grouped and sorted; missing times go last and ties keep their order")
    func groups() {
        let groups = MonarchAiBackgroundTasksTokens.groups([
            task("q1", .queued, started: -10),
            task("r-late", .running, started: -10),
            task("n-none", .needsInput),
            task("r-early", .running, started: -100),
            task("f-old", .failed, ended: -500),
            task("n-old", .needsInput, started: -300),
            task("s-new", .succeeded, ended: -5),
            task("c-none", .cancelled),
            task("r-none", .running),
            task("r-none-2", .running),
        ])
        #expect(groups.needsInput.map(\.id) == ["n-old", "n-none"])
        #expect(groups.running.map(\.id) == ["r-early", "r-late", "r-none", "r-none-2", "q1"])
        #expect(groups.finished.map(\.id) == ["s-new", "f-old", "c-none"])
        #expect(MonarchAiBackgroundTasksTokens.groups([]) == MonarchAiBackgroundTaskGroups())
    }

    @Test("Elapsed times use short units in the locale")
    func elapsed() {
        #expect(MonarchAiBackgroundTasksTokens.elapsed(42, locale: us) == "42 sec")
        #expect(MonarchAiBackgroundTasksTokens.elapsed(192, locale: us) == "3 min 12 sec")
        #expect(MonarchAiBackgroundTasksTokens.elapsed(3_840, locale: us) == "1 hr 4 min")
        #expect(MonarchAiBackgroundTasksTokens.elapsed(-5, locale: us) == "0 sec")
    }

    @Test("The status line shows status, agent and elapsed or end time")
    func meta() {
        var running = task("Run", .running, started: -192)
        running.agent = "Research agent"
        #expect(
            MonarchAiBackgroundTasksTokens.meta(running, now: now, locale: us, labels: none)
                == ["Running", "Research agent", "Running for 3 min 12 sec"]
        )
        let finished = task("Done", .succeeded, ended: -300)
        #expect(
            MonarchAiBackgroundTasksTokens.meta(finished, now: now, locale: us, labels: none) == [
                "Done", "Ended 5 minutes ago",
            ]
        )
        #expect(
            MonarchAiBackgroundTasksTokens.meta(task("Q", .queued), now: now, locale: us, labels: none) == ["Queued"]
        )
        let custom = MonarchAiBackgroundTasksLabels(status: { _ in "S" }, elapsed: { "for \($0)" })
        #expect(
            MonarchAiBackgroundTasksTokens.meta(task("R", .running, started: -42), now: now, locale: us, labels: custom)
                == ["S", "for 42 sec"]
        )
    }

    @Test("Detail, bar and fraction follow the status")
    func detailAndBar() {
        var item = MonarchAiBackgroundTask(
            "T",
            id: "t",
            status: .running,
            progress: 1.4,
            step: "Step",
            result: "R",
            error: "E"
        )
        #expect(MonarchAiBackgroundTasksTokens.detail(item) == "Step")
        #expect(MonarchAiBackgroundTasksTokens.fraction(item) == 1)
        #expect(MonarchAiBackgroundTasksTokens.showsBar(item))
        item.isIndeterminate = true
        #expect(MonarchAiBackgroundTasksTokens.fraction(item) == nil)
        #expect(MonarchAiBackgroundTasksTokens.showsBar(item), "a running task always has a bar")
        item.status = .queued
        #expect(!MonarchAiBackgroundTasksTokens.showsBar(item), "a waiting task needs a known amount")
        item.isIndeterminate = false
        #expect(MonarchAiBackgroundTasksTokens.showsBar(item))
        item.status = .failed
        #expect(MonarchAiBackgroundTasksTokens.detail(item) == "E")
        #expect(!MonarchAiBackgroundTasksTokens.showsBar(item))
        item.status = .succeeded
        #expect(MonarchAiBackgroundTasksTokens.detail(item) == "R")
        item.status = .cancelled
        #expect(MonarchAiBackgroundTasksTokens.detail(item) == nil)
    }

    @Test("Actions depend on the status and on the callbacks given")
    func actions() {
        let all = Set(MonarchAiBackgroundTaskAction.allCases)
        #expect(MonarchAiBackgroundTasksTokens.actions(for: .needsInput, available: all) == [.respond, .open, .cancel])
        #expect(MonarchAiBackgroundTasksTokens.actions(for: .running, available: all) == [.open, .cancel])
        #expect(MonarchAiBackgroundTasksTokens.actions(for: .failed, available: all) == [.retry, .open, .dismiss])
        #expect(MonarchAiBackgroundTasksTokens.actions(for: .succeeded, available: all) == [.open, .dismiss])
        #expect(MonarchAiBackgroundTasksTokens.actions(for: .failed, available: [.open]) == [.open])
        #expect(MonarchAiBackgroundTasksTokens.actions(for: .running, available: []).isEmpty)
        #expect(MonarchAiBackgroundTasksTokens.actionLabel(.open, title: "Report", labels: none) == "Open: Report")
        #expect(
            MonarchAiBackgroundTasksTokens.actionLabel(
                .retry,
                title: "Report",
                labels: MonarchAiBackgroundTasksLabels(retry: "Again")
            )
                == "Again: Report"
        )
    }

    @Test("Button counts: running first, queued only when nothing runs, needs you in words")
    func counts() {
        let mixed = [task("a", .running), task("b", .running), task("c", .queued), task("d", .needsInput)]
        let counts = MonarchAiBackgroundTasksTokens.buttonCounts(mixed, locale: us, labels: none)
        #expect(counts.activity == "2 running")
        #expect(counts.needsInput == "1 needs you")
        let queued = MonarchAiBackgroundTasksTokens.buttonCounts([task("c", .queued)], locale: us, labels: none)
        #expect(queued.activity == "1 queued")
        #expect(queued.needsInput == nil)
        let needs = MonarchAiBackgroundTasksTokens.buttonCounts(
            [task("d", .needsInput), task("e", .needsInput)],
            locale: us,
            labels: none
        )
        #expect(needs.needsInput == "2 need you")
        #expect(
            MonarchAiBackgroundTasksTokens.buttonLabel(mixed, locale: us, labels: none)
                == "Background tasks, 2 running, 1 needs you"
        )
        #expect(MonarchAiBackgroundTasksTokens.buttonLabel([], locale: us, labels: none) == "Background tasks")
        #expect(
            MonarchAiBackgroundTasksTokens.groupHeading("Running", count: 2, locale: us, labels: none) == "Running (2)"
        )
    }

    @Test("Only a change into succeeded, failed or needs input is news")
    func news() {
        let before: [String: MonarchAiBackgroundTaskStatus] = [
            "a": .running, "b": .running, "c": .running, "d": .needsInput,
        ]
        let after = [
            task("a", .succeeded), task("b", .running), task("c", .cancelled), task("d", .needsInput),
            task("new", .failed),
        ]
        let news = MonarchAiBackgroundTasksTokens.news(before: before, after: after)
        #expect(news.map(\.id) == ["a"])
        let changed = [task("Report", .succeeded), task("Import", .needsInput)]
        #expect(
            MonarchAiBackgroundTasksTokens.announcement(changed, labels: none)
                == "Report: Done. Import: Needs your input"
        )
        #expect(MonarchAiBackgroundTasksTokens.statuses(after)["a"] == .succeeded)
        #expect(MonarchAiBackgroundTasksTokens.ticks([task("a", .running, started: -1)]))
        #expect(!MonarchAiBackgroundTasksTokens.ticks([task("a", .running)]))
        #expect(!MonarchAiBackgroundTasksTokens.ticks([task("a", .queued, started: -1)]))
    }

    @Test("Status icons and colours are distinct by meaning")
    func statusAppearance() {
        let symbols = Set(MonarchAiBackgroundTaskStatus.allCases.map(MonarchAiBackgroundTasksTokens.symbol))
        #expect(symbols.count == MonarchAiBackgroundTaskStatus.allCases.count)
        #expect(MonarchAiBackgroundTasksTokens.symbolColor(.failed) == \.dangerStrong)
        #expect(MonarchAiBackgroundTasksTokens.symbolColor(.succeeded) == \.successStrong)
        #expect(MonarchAiBackgroundTasksTokens.symbolColor(.needsInput) == \.warningStrong)
        #expect(MonarchAiBackgroundTasksTokens.symbolColor(.running) == \.accentStrong)
        #expect(MonarchAiBackgroundTasksTokens.symbolColor(.queued) == \.textMuted)
        #expect(MonarchAiBackgroundTasksTokens.detailColor(.failed) == \.dangerStrong)
        #expect(MonarchAiBackgroundTasksTokens.detailColor(.running) == \.textMuted)
        #expect(MonarchAiBackgroundTaskStatus.cancelled.isFinished)
        #expect(!MonarchAiBackgroundTaskStatus.needsInput.isFinished)
    }

    @Test("Built-in strings resolve")
    func strings() {
        var all = [
            MonarchAiBackgroundTasksStrings.title, MonarchAiBackgroundTasksStrings.needsInputGroup,
            MonarchAiBackgroundTasksStrings.runningGroup, MonarchAiBackgroundTasksStrings.finishedGroup,
            MonarchAiBackgroundTasksStrings.open, MonarchAiBackgroundTasksStrings.cancel,
            MonarchAiBackgroundTasksStrings.retry, MonarchAiBackgroundTasksStrings.respond,
            MonarchAiBackgroundTasksStrings.dismiss, MonarchAiBackgroundTasksStrings.clearFinished,
            MonarchAiBackgroundTasksStrings.empty, MonarchAiBackgroundTasksStrings.needsInputCountOne,
            MonarchAiBackgroundTasksStrings.runningCount("1"), MonarchAiBackgroundTasksStrings.queuedCount("1"),
            MonarchAiBackgroundTasksStrings.needsInputCountOther("2"),
            MonarchAiBackgroundTasksStrings.groupHeading("a", "1"),
            MonarchAiBackgroundTasksStrings.elapsed("1"), MonarchAiBackgroundTasksStrings.ended("1"),
            MonarchAiBackgroundTasksStrings.progressLabel("t"), MonarchAiBackgroundTasksStrings.actionLabel("a", "t"),
            MonarchAiBackgroundTasksStrings.announce("t", "s"),
        ]
        all += MonarchAiBackgroundTaskStatus.allCases.map(MonarchAiBackgroundTasksStrings.status)
        for string in all { #expect(!string.contains("monarch.")) }
    }
}
