import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchGoalTreeFixtures: ComponentFixtures {
    static let id = "goal-tree"

    static let goals = [
        MonarchGoal(
            id: "o1",
            title: "Grow repeat customers across every region we sell in",
            progress: 0.45,
            status: .onTrack,
            ownerName: "Ana Ruiz",
            meta: "Q2, weight 40%"
        ),
        MonarchGoal(id: "k1", parentID: "o1", title: "Raise repeat rate to 35%", progress: 0.6, status: .onTrack),
        MonarchGoal(
            id: "k2",
            parentID: "o1",
            title: "Cut churn to 4%",
            progress: 0.2,
            status: .atRisk,
            ownerName: "Ben Okafor",
            alignedTo: "Retention plan (Sam Lee)"
        ),
        MonarchGoal(id: "k3", parentID: "o1", title: "Launch loyalty pilot", progress: 0, status: .notStarted),
        MonarchGoal(id: "k4", parentID: "k2", title: "Call every lapsed account", progress: 1.4, status: .done),
        MonarchGoal(id: "o2", title: "Ship the mobile app", progress: .nan, status: .offTrack),
    ]

    static let fixtures = [
        Fixture("open tree, every status, owner, meta, alignment, add child") {
            MonarchGoalTree(goals, onAddChild: { _ in })
        },
        Fixture("closed goals with child counts, not quiet, actions") {
            MonarchGoalTree(
                goals,
                expandedIDs: .constant([]),
                isQuietChildren: false,
                actions: { _ in
                    Menu {
                        Button("Check in") {}
                    } label: {
                        Label("More", systemImage: "ellipsis")
                    }
                    .labelStyle(.iconOnly)
                    .buttonStyle(.monarch(.ghost, size: .sm))
                }
            )
        },
        Fixture("empty, and own labels") {
            VStack {
                MonarchGoalTree([])
                MonarchGoalTree(
                    Array(goals.prefix(2)),
                    labels: MonarchGoalTreeLabels(status: { _ in "State" }, addChild: "Add target"),
                    canAddChild: { _, depth in depth < 2 },
                    onAddChild: { _ in }
                )
            }
        },
    ]
}

@Suite("MonarchGoalTree")
struct MonarchGoalTreeTests {
    private let goals = MonarchGoalTreeFixtures.goals
    private let labels = MonarchGoalTreeLabels()

    @Test("The tree keeps sibling order and puts orphans and self-parents at the top")
    func structure() {
        let extra = [
            MonarchGoal(id: "x", parentID: "missing", title: "Orphan", progress: 0, status: .onTrack),
            MonarchGoal(id: "y", parentID: "y", title: "Self", progress: 0, status: .onTrack),
        ]
        let tree = MonarchGoalTreeTokens.structure(goals + extra)
        #expect(tree.roots.map(\.id) == ["o1", "o2", "x", "y"])
        #expect(tree.children["o1"]?.map(\.id) == ["k1", "k2", "k3"])
        #expect(tree.children["k2"]?.map(\.id) == ["k4"])
        #expect(MonarchGoalTreeTokens.defaultExpanded(goals) == ["o1", "k2"])
    }

    @Test("A cycle stops at a goal already on the path")
    func cycle() {
        let looped = [
            MonarchGoal(id: "a", parentID: "b", title: "A", progress: 0, status: .onTrack),
            MonarchGoal(id: "b", parentID: "a", title: "B", progress: 0, status: .onTrack),
        ]
        let tree = MonarchGoalTreeTokens.structure(looped)
        let a = looped[0]
        #expect(MonarchGoalTreeTokens.children(of: a, in: tree.children, trail: []).map(\.id) == ["b"])
        #expect(MonarchGoalTreeTokens.children(of: a, in: tree.children, trail: ["a"]).isEmpty)
    }

    @Test("Progress is clamped and rounded; not-a-number reads as 0")
    func percent() {
        #expect(MonarchGoalTreeTokens.percent(0.456) == 46)
        #expect(MonarchGoalTreeTokens.percent(1.4) == 100)
        #expect(MonarchGoalTreeTokens.percent(-0.2) == 0)
        #expect(MonarchGoalTreeTokens.percent(.nan) == 0)
        #expect(MonarchGoalTreeTokens.percent(.infinity) == 0)
        #expect(MonarchGoalTreeTokens.fraction(0.456) == 0.46)
        #expect(MonarchGoalTreeTokens.accessibilityValue(0.456, locale: Locale(identifier: "en_US")) == "46%")
    }

    @Test("Every status has its own tone and symbol")
    func tones() {
        let tones = MonarchGoalStatus.allCases.map(MonarchGoalTreeTokens.tone)
        #expect(tones == [.neutral, .info, .warning, .danger, .success])
        #expect(
            Set(MonarchGoalStatus.allCases.map(MonarchGoalTreeTokens.symbol)).count == MonarchGoalStatus.allCases.count
        )
    }

    @Test("Quiet children hide the badge only when on track or not started")
    func badges() {
        #expect(MonarchGoalTreeTokens.showsBadge(.onTrack, depth: 0, isQuietChildren: true))
        #expect(!MonarchGoalTreeTokens.showsBadge(.onTrack, depth: 1, isQuietChildren: true))
        #expect(!MonarchGoalTreeTokens.showsBadge(.notStarted, depth: 2, isQuietChildren: true))
        #expect(MonarchGoalTreeTokens.showsBadge(.atRisk, depth: 1, isQuietChildren: true))
        #expect(MonarchGoalTreeTokens.showsBadge(.done, depth: 1, isQuietChildren: true))
        #expect(MonarchGoalTreeTokens.showsBadge(.onTrack, depth: 1, isQuietChildren: false))
    }

    @Test("Add child is offered on top-level goals by default, and only with a handler")
    func addChild() {
        let goal = goals[0]
        #expect(MonarchGoalTreeTokens.addsChild(goal, depth: 0, hasHandler: true, canAddChild: nil))
        #expect(!MonarchGoalTreeTokens.addsChild(goal, depth: 1, hasHandler: true, canAddChild: nil))
        #expect(!MonarchGoalTreeTokens.addsChild(goal, depth: 0, hasHandler: false, canAddChild: nil))
        #expect(
            MonarchGoalTreeTokens.addsChild(goal, depth: 1, hasHandler: true, canAddChild: { _, depth in depth < 2 })
        )
    }

    @Test("Opening and closing change the expanded set")
    func toggled() {
        #expect(MonarchGoalTreeTokens.toggled(["a"], id: "b", isExpanded: true) == ["a", "b"])
        #expect(MonarchGoalTreeTokens.toggled(["a", "b"], id: "a", isExpanded: false) == ["b"])
    }

    @Test("The chevron turns toward the children in both directions")
    func chevron() {
        #expect(MonarchGoalTreeTokens.chevronRotation(isExpanded: false, layoutDirection: .leftToRight) == .zero)
        #expect(MonarchGoalTreeTokens.chevronRotation(isExpanded: true, layoutDirection: .leftToRight) == .degrees(90))
        #expect(MonarchGoalTreeTokens.chevronRotation(isExpanded: true, layoutDirection: .rightToLeft) == .degrees(-90))
    }

    @Test("A goal is read with its status, owner, facts, alignment and closed child count")
    func accessibilityLabel() {
        let open = MonarchGoalTreeTokens.accessibilityLabel(goals[0], childCount: 3, isExpanded: true, labels: labels)
        #expect(open == "Grow repeat customers across every region we sell in, On track, Ana Ruiz, Q2, weight 40%")
        let closed = MonarchGoalTreeTokens.accessibilityLabel(
            goals[0],
            childCount: 3,
            isExpanded: false,
            labels: labels
        )
        #expect(closed.hasSuffix("Q2, weight 40%, 3 key results"))
        let aligned = MonarchGoalTreeTokens.accessibilityLabel(
            goals[2],
            childCount: 1,
            isExpanded: true,
            labels: labels
        )
        #expect(aligned == "Cut churn to 4%, At risk, Ben Okafor, Aligned to Retention plan (Sam Lee)")
    }

    @Test("Built-in strings resolve, and labels can replace them")
    func strings() {
        #expect(
            MonarchGoalStatus.allCases.map(labels.status) == [
                "Not started", "On track", "At risk", "Off track", "Done",
            ]
        )
        #expect(labels.expand("Q2") == "Expand Q2")
        #expect(labels.collapse("Q2") == "Collapse Q2")
        #expect(labels.addChildTo("Q2") == "Add key result to Q2")
        #expect(labels.childCount(1) == "1 key result")
        #expect(labels.childCount(3) == "3 key results")
        for text in [labels.alignedTo, labels.addChild, labels.empty] {
            #expect(!text.hasPrefix("monarch."))
        }
        let custom = MonarchGoalTreeLabels(childCount: { "\($0) targets" })
        #expect(custom.childCount(2) == "2 targets")
    }
}
