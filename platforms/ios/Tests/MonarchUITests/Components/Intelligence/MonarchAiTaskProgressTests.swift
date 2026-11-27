import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiTaskProgressFixtures: ComponentFixtures {
    static let id = "ai-task-progress"
    static let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
    static let started = now.addingTimeInterval(-300)
    static let steps = [
        MonarchAiPlanStep(id: "read", label: "Read the documents", state: .done),
        MonarchAiPlanStep(id: "group", label: "Group them by topic", state: .active),
        MonarchAiPlanStep(id: "write", label: "Write the summary"),
    ]

    static let fixtures = [
        Fixture("running, steps, every control") {
            MonarchAiTaskProgress(
                "Summarising 40 documents",
                status: .running,
                startedAt: started,
                steps: steps,
                eta: 180,
                isNotifying: .constant(true),
                now: now,
                onMinimize: {},
                onCancel: {}
            )
        },
        Fixture("running, indeterminate") {
            MonarchAiTaskProgress("Indexing the workspace", status: .running, startedAt: started, now: now)
        },
        Fixture("paused, explicit progress") {
            MonarchAiTaskProgress(
                "Translating the handbook",
                status: .paused,
                startedAt: started,
                progress: 0.4,
                isNotifying: .constant(false),
                now: now,
                onCancel: {}
            )
        },
        Fixture("done") {
            MonarchAiTaskProgress("Summarising 40 documents", status: .done, startedAt: started, now: now)
        },
        Fixture("failed") {
            MonarchAiTaskProgress(
                "Export the report",
                status: .failed,
                startedAt: started,
                steps: [
                    MonarchAiPlanStep(id: "a", label: "Collect the rows", state: .done),
                    MonarchAiPlanStep(id: "b", label: "Write the file", state: .failed),
                ],
                now: now,
                onCancel: {}
            )
        },
        Fixture("cancelled") {
            MonarchAiTaskProgress("Clean up duplicates", status: .cancelled, startedAt: started, now: now)
        },
        Fixture("long title") {
            MonarchAiTaskProgress(
                "Compare every supplier contract signed this year with the new standard terms",
                status: .running,
                startedAt: started,
                progress: 0.75,
                eta: 5_400,
                isNotifying: .constant(false),
                now: now,
                onMinimize: {},
                onCancel: {}
            )
        },
    ]
}

@Suite("MonarchAiTaskProgress")
struct MonarchAiTaskProgressTests {
    private let english = Locale(identifier: "en_US")
    private let labels = MonarchAiTaskProgressLabels()
    private let steps = MonarchAiTaskProgressFixtures.steps

    @Test("Progress comes from the value, else the finished steps, else done")
    func fraction() {
        #expect(MonarchAiTaskProgressTokens.fraction(progress: 0.4, steps: steps, status: .running) == 0.4)
        #expect(MonarchAiTaskProgressTokens.fraction(progress: 1.7, steps: [], status: .running) == 1)
        #expect(MonarchAiTaskProgressTokens.fraction(progress: nil, steps: steps, status: .running) == 1.0 / 3.0)
        let failedAndSkipped = [
            MonarchAiPlanStep(id: "a", label: "A", state: .failed),
            MonarchAiPlanStep(id: "b", label: "B", state: .skipped),
        ]
        #expect(MonarchAiTaskProgressTokens.fraction(progress: nil, steps: failedAndSkipped, status: .failed) == 1)
        #expect(MonarchAiTaskProgressTokens.fraction(progress: nil, steps: [], status: .done) == 1)
        #expect(MonarchAiTaskProgressTokens.fraction(progress: nil, steps: [], status: .running) == nil)
        #expect(MonarchAiTaskProgressTokens.fraction(progress: .nan, steps: [], status: .paused) == nil)
    }

    @Test("The bar is indeterminate only while running without progress")
    func bar() {
        #expect(MonarchAiTaskProgressTokens.barValue(fraction: nil, status: .running) == nil)
        #expect(MonarchAiTaskProgressTokens.barValue(fraction: nil, status: .paused) == 0)
        #expect(MonarchAiTaskProgressTokens.barValue(fraction: 0.5, status: .cancelled) == 0.5)
        #expect(MonarchAiTaskProgressTokens.percent(1.0 / 3.0) == 33)
        #expect(MonarchAiTaskProgressTokens.progressText(40, labels: labels) == "40% complete")
    }

    @Test("Every status has its own name, icon and tone")
    func statuses() {
        let names = MonarchAiTaskStatus.allCases.map { MonarchAiTaskProgressTokens.statusName($0, labels: labels) }
        #expect(names == ["Running", "Paused", "Done", "Failed", "Cancelled"])
        let symbols = Set(MonarchAiTaskStatus.allCases.map(MonarchAiTaskProgressTokens.symbol))
        #expect(symbols.count == MonarchAiTaskStatus.allCases.count)
        #expect(MonarchAiTaskProgressTokens.tone(.running) == .ai)
        #expect(MonarchAiTaskProgressTokens.tone(.paused) == .warning)
        #expect(MonarchAiTaskProgressTokens.tone(.done) == .success)
        #expect(MonarchAiTaskProgressTokens.tone(.failed) == .danger)
        #expect(MonarchAiTaskProgressTokens.tone(.cancelled) == .neutral)
        #expect(MonarchAiTaskProgressTokens.isActive(.running) && MonarchAiTaskProgressTokens.isActive(.paused))
        #expect(!MonarchAiTaskProgressTokens.isActive(.done))
    }

    @Test("The start time is relative to an injected now")
    func started() {
        let now = MonarchAiTaskProgressFixtures.now
        #expect(
            MonarchAiTaskProgressTokens.startedText(
                startedAt: now.addingTimeInterval(-300),
                now: now,
                locale: english,
                labels: labels
            ) == "Started 5 minutes ago"
        )
        #expect(
            MonarchAiTaskProgressTokens.startedText(
                startedAt: now.addingTimeInterval(-7_200),
                now: now,
                locale: english,
                labels: labels
            ) == "Started 2 hours ago"
        )
        let custom = MonarchAiTaskProgressLabels(started: { "Depuis \($0)" })
        #expect(
            MonarchAiTaskProgressTokens.startedText(startedAt: now, now: now, locale: english, labels: custom)
                .hasPrefix("Depuis ")
        )
    }

    @Test("The time left rounds to minutes, then hours")
    func eta() {
        #expect(MonarchAiTaskProgressTokens.etaText(30, locale: english, labels: labels) == "Less than a minute left")
        #expect(MonarchAiTaskProgressTokens.etaText(80, locale: english, labels: labels) == "About 1 minute left")
        #expect(MonarchAiTaskProgressTokens.etaText(180, locale: english, labels: labels) == "About 3 minutes left")
        #expect(MonarchAiTaskProgressTokens.etaText(5_400, locale: english, labels: labels) == "About 1.5 hours left")
        #expect(MonarchAiTaskProgressTokens.etaText(7_200, locale: english, labels: labels) == "About 2 hours left")
        #expect(MonarchAiTaskProgressTokens.showsEta(60, status: .running))
        #expect(!MonarchAiTaskProgressTokens.showsEta(60, status: .paused))
        #expect(!MonarchAiTaskProgressTokens.showsEta(-1, status: .running))
        #expect(!MonarchAiTaskProgressTokens.showsEta(nil, status: .running))
    }

    @Test("Only a status change is announced; a failure is urgent")
    func announcements() {
        let title = "Summarising 40 documents"
        #expect(
            MonarchAiTaskProgressTokens.announcement(title: title, previous: nil, status: .running, labels: labels)
                == nil
        )
        #expect(
            MonarchAiTaskProgressTokens.announcement(title: title, previous: .running, status: .running, labels: labels)
                == nil
        )
        let done = MonarchAiTaskProgressTokens.announcement(
            title: title,
            previous: .running,
            status: .done,
            labels: labels
        )
        #expect(done?.text == "Summarising 40 documents: Done")
        #expect(done?.isUrgent == false)
        let failed = MonarchAiTaskProgressTokens.announcement(
            title: title,
            previous: .running,
            status: .failed,
            labels: labels
        )
        #expect(failed?.isUrgent == true)
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchAiTaskProgressStrings.progressLabel("Export") == "Export progress")
        #expect(MonarchAiTaskProgressStrings.cancel == "Cancel")
        #expect(MonarchAiTaskProgressStrings.minimize == "Minimize")
        #expect(MonarchAiTaskProgressStrings.notify == "Notify me when done")
    }

    @Test("Colours are text roles")
    func appearance() {
        #expect(MonarchAiTaskProgressTokens.title == \MonarchColorTokens.textDefault)
        #expect(MonarchAiTaskProgressTokens.muted == \MonarchColorTokens.textMuted)
    }
}
