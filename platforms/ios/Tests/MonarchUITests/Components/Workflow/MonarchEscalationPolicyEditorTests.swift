import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchEscalationPolicyEditorFixtures: ComponentFixtures {
    static let id = "escalation-policy-editor"

    static let targets = [
        MonarchEscalationTarget(id: "oncall", label: "On-call rota"),
        MonarchEscalationTarget(id: "ops", label: "#ops", description: "Slack channel"),
        MonarchEscalationTarget(id: "lead", label: "Ada Moss", description: "Team lead"),
    ]

    static let levels = [
        MonarchEscalationLevel(id: "l1", afterMinutes: 5, targets: ["oncall"]),
        MonarchEscalationLevel(id: "l2", afterMinutes: 30, targets: ["oncall", "ops"]),
        MonarchEscalationLevel(id: "l3", afterMinutes: 1_590, targets: ["lead"]),
    ]

    static let fixtures: [Fixture] = [
        Fixture("three levels") {
            MonarchEscalationPolicyEditor(
                levels: .constant(levels),
                targets: targets,
                description: "Each level fires only if nobody has acknowledged yet.",
                makeID: { "new" }
            )
        },
        Fixture("problems shown") {
            MonarchEscalationPolicyEditor(
                levels: .constant([
                    MonarchEscalationLevel(id: "a", afterMinutes: 10, targets: ["ops"]),
                    MonarchEscalationLevel(id: "b", afterMinutes: 10, targets: ["oncall"]),
                    MonarchEscalationLevel(id: "c", afterMinutes: 60),
                ]),
                targets: targets,
                showsErrors: true,
                makeID: { "new" }
            )
        },
        Fixture("no targets") {
            MonarchEscalationPolicyEditor(
                levels: .constant([MonarchEscalationLevel(id: "a", afterMinutes: 15)]),
                targets: [],
                makeID: { "new" }
            )
        },
        Fixture("interval timing, full") {
            MonarchEscalationPolicyEditor(
                levels: .constant(levels),
                targets: targets,
                timing: .interval,
                maxLevels: 3,
                makeID: { "new" }
            )
        },
        Fixture("disabled") {
            MonarchEscalationPolicyEditor(levels: .constant([levels[0]]), targets: targets, makeID: { "new" })
                .disabled(true)
        },
    ]
}

@Suite("MonarchEscalationPolicyEditor")
struct MonarchEscalationPolicyEditorTests {
    private typealias Tokens = MonarchEscalationPolicyEditorTokens
    private let targets = MonarchEscalationPolicyEditorFixtures.targets
    private let none = MonarchEscalationPolicyEditorLabels()

    @Test("Levels need targets, and with cumulative timing must get later")
    func validate() {
        let levels = [
            MonarchEscalationLevel(id: "a", afterMinutes: 10, targets: ["ops"]),
            MonarchEscalationLevel(id: "b", afterMinutes: 10, targets: ["oncall"]),
            MonarchEscalationLevel(id: "c", afterMinutes: 5),
            MonarchEscalationLevel(id: "d", afterMinutes: 3, targets: ["ops"]),
        ]
        #expect(MonarchEscalationPolicyEditor.validate(levels) == ["b": .order, "c": .targets, "d": .order])
        #expect(MonarchEscalationPolicyEditor.validate(levels, timing: .interval) == ["c": .targets])
        #expect(MonarchEscalationPolicyEditor.validate(MonarchEscalationPolicyEditorFixtures.levels).isEmpty)
    }

    @Test("A new level waits the step after the last, or just the step with interval timing")
    func newLevel() {
        let levels = MonarchEscalationPolicyEditorFixtures.levels
        #expect(Tokens.newLevelMinutes(after: levels, timing: .cumulative, step: 15) == 1_605)
        #expect(Tokens.newLevelMinutes(after: [], timing: .cumulative, step: 15) == 15)
        #expect(Tokens.newLevelMinutes(after: levels, timing: .interval, step: 15) == 15)
    }

    @Test("Chosen targets keep the order of the targets list")
    func ordered() {
        #expect(Tokens.ordered(["lead", "oncall"], targets: targets) == ["oncall", "lead"])
        #expect(Tokens.ordered(["gone"], targets: targets).isEmpty)
    }

    @Test("Summaries name the delay and the targets, or nobody yet")
    func summary() {
        let level = MonarchEscalationLevel(id: "x", afterMinutes: 90, targets: ["ops", "missing"])
        #expect(Tokens.names(level, targets: targets, labels: none) == "#ops, missing")
        #expect(
            Tokens.names(MonarchEscalationLevel(id: "y", afterMinutes: 5), targets: targets, labels: none)
                == "nobody yet"
        )
        #expect(Tokens.shortDuration(90) == "1h 30m")
        #expect(Tokens.shortDuration(1_590) == "1d 2h 30m")
        #expect(Tokens.summary(duration: "15m", names: "#ops", labels: none) == "After 15m, notify #ops")
        let custom = MonarchEscalationPolicyEditorLabels(summary: { "\($1) nach \($0)" }, nobody: "niemand")
        #expect(Tokens.summary(duration: "5m", names: "A", labels: custom) == "A nach 5m")
        #expect(
            Tokens.names(MonarchEscalationLevel(id: "z", afterMinutes: 5), targets: targets, labels: custom)
                == "niemand"
        )
    }

    @Test("The spoken delay is spelled out in the given locale")
    func spokenDuration() {
        let english = Locale(identifier: "en_US")
        #expect(Tokens.spokenDuration(90, locale: english).contains("hour"))
        #expect(Tokens.spokenDuration(90, locale: english).contains("30 minutes"))
        #expect(Tokens.spokenDuration(5, locale: english).contains("5 minute"))
        #expect(!Tokens.spokenDuration(5, locale: english).contains("hour"))
    }

    @Test("Remove and add follow the fewest and most levels")
    func limits() {
        #expect(!Tokens.canRemove(count: 1, minLevels: 1))
        #expect(Tokens.canRemove(count: 2, minLevels: 1))
        #expect(Tokens.canAdd(count: 9, maxLevels: 10))
        #expect(!Tokens.canAdd(count: 10, maxLevels: 10))
    }

    @Test("Titles and problems use the built-in strings unless replaced")
    func text() {
        #expect(Tokens.levelTitle(2, labels: none) == "Level 2")
        #expect(Tokens.problemText(.order, labels: none) == "This level must come later than the one before it.")
        #expect(Tokens.problemText(.targets, labels: none).hasPrefix("Choose at least one target."))
        let custom = MonarchEscalationPolicyEditorLabels(level: { "Stufe \($0)" }, problem: { _ in "Fehler" })
        #expect(Tokens.levelTitle(1, labels: custom) == "Stufe 1")
        #expect(Tokens.problemText(.order, labels: custom) == "Fehler")
    }

    @Test("Built-in strings resolve")
    func strings() {
        let all =
            [
                MonarchEscalationPolicyEditorStrings.label, MonarchEscalationPolicyEditorStrings.after,
                MonarchEscalationPolicyEditorStrings.notify, MonarchEscalationPolicyEditorStrings.nobody,
                MonarchEscalationPolicyEditorStrings.add, MonarchEscalationPolicyEditorStrings.noTargets,
                MonarchEscalationPolicyEditorStrings.level(1), MonarchEscalationPolicyEditorStrings.remove(1),
                MonarchEscalationPolicyEditorStrings.maxReached(10),
                MonarchEscalationPolicyEditorStrings.summary("a", "b"),
            ] + MonarchEscalationLevelProblem.allCases.map(MonarchEscalationPolicyEditorStrings.problem)
        for string in all {
            #expect(!string.hasPrefix("monarch."))
        }
        #expect(MonarchEscalationPolicyEditorStrings.remove(2) == "Remove level 2")
    }
}
