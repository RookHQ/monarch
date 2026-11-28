import SwiftUI
import Testing

@testable import MonarchUI

private let tourSteps = [
    MonarchAiOnboardingTourStep(
        "What the assistant can see",
        body: "The page you have open, the records you select and any files you attach. "
            + "It does not see other workspaces.",
        systemImage: "eye"
    ),
    MonarchAiOnboardingTourStep(
        "What it can do",
        body: "Summarise pages, draft text and answer questions. It asks before it changes anything."
    ),
    MonarchAiOnboardingTourStep(
        "How to turn it off",
        body: "Use Turn off AI below at any time, or switch it off in workspace settings."
    ),
]

enum MonarchAiOnboardingTourFixtures: ComponentFixtures {
    static let id = "ai-onboarding-tour"
    static let fixtures = [
        Fixture("first step with close") {
            MonarchAiOnboardingTour(tourSteps, onSkip: {}, onDisable: {}, onFinish: {})
        },
        Fixture("middle and last step, controlled") {
            VStack {
                MonarchAiOnboardingTour(tourSteps, step: .constant(1), onDisable: {}, onFinish: {})
                MonarchAiOnboardingTour(tourSteps, step: .constant(9), onDisable: {}, onFinish: {})
            }
        },
        Fixture("one step, in a card, own labels") {
            MonarchCard {
                MonarchAiOnboardingTour(
                    [tourSteps[2]],
                    labels: MonarchAiOnboardingTourLabels(eyebrow: "Assistant", finish: "Done", disable: "No thanks"),
                    onDisable: {},
                    onFinish: {}
                )
            }
        },
        Fixture("no steps") {
            MonarchAiOnboardingTour([], onDisable: {}, onFinish: {})
        },
    ]
}

@Suite("MonarchAiOnboardingTour")
struct MonarchAiOnboardingTourTests {
    typealias Tokens = MonarchAiOnboardingTourTokens

    @Test("The step is kept inside the steps")
    func clamped() {
        #expect(Tokens.clamped(-1, count: 3) == 0)
        #expect(Tokens.clamped(1, count: 3) == 1)
        #expect(Tokens.clamped(7, count: 3) == 2)
        #expect(Tokens.clamped(4, count: 0) == 0)
    }

    @Test("Back appears after the first step; the last step finishes")
    func navigation() {
        let labels = MonarchAiOnboardingTourLabels()
        #expect(!Tokens.showsBack(0, count: 3))
        #expect(Tokens.showsBack(2, count: 3))
        #expect(Tokens.primaryTitle(0, count: 3, labels: labels) == "Next")
        #expect(Tokens.primaryTitle(2, count: 3, labels: labels) == "Got it")
        #expect(Tokens.primaryTitle(0, count: 1, labels: labels) == "Got it")
        #expect(Tokens.isLast(5, count: 3))
        #expect(Tokens.isLast(0, count: 0))
    }

    @Test("The stepper gets one step per page with unique ids and the tour's position text")
    func stepper() {
        let duplicated = [tourSteps[0], tourSteps[0]]
        let steps = Tokens.stepperSteps(duplicated)
        #expect(steps.map(\.label) == ["", ""])
        #expect(Set(steps.map(\.id)).count == 2)
        let labels = Tokens.stepperLabels(MonarchAiOnboardingTourLabels(stepOf: { "\($0)/\($1)" }))
        #expect(labels.stepOf?(2, 3) == "2/3")
    }

    @Test("A step's id defaults to its title")
    func stepID() {
        #expect(MonarchAiOnboardingTourStep("Title", body: "Body").id == "Title")
        #expect(MonarchAiOnboardingTourStep("Title", id: "a", body: "Body").id == "a")
    }

    @Test("The eyebrow uses the AI role")
    func appearance() {
        #expect(Tokens.eyebrowRole == \.aiStrong)
    }

    @Test("Own labels replace the built-in strings")
    func overrides() {
        let labels = MonarchAiOnboardingTourLabels(
            eyebrow: "E",
            stepOf: { "\($0)-\($1)" },
            back: "B",
            next: "N",
            finish: "F",
            skip: "S",
            disable: "D"
        )
        #expect(
            [labels.eyebrow, labels.back, labels.next, labels.finish, labels.skip, labels.disable] == [
                "E", "B", "N", "F", "S", "D",
            ]
        )
        #expect(labels.stepOf(1, 2) == "1-2")
    }

    @Test("The built-in strings come from the strings table")
    func strings() {
        #expect(MonarchAiOnboardingTourStrings.eyebrow == "AI")
        #expect(MonarchAiOnboardingTourStrings.stepOf(2, 3) == "Step 2 of 3")
        #expect(MonarchAiOnboardingTourStrings.back == "Back")
        #expect(MonarchAiOnboardingTourStrings.next == "Next")
        #expect(MonarchAiOnboardingTourStrings.finish == "Got it")
        #expect(MonarchAiOnboardingTourStrings.skip == "Skip introduction")
        #expect(MonarchAiOnboardingTourStrings.disable == "Turn off AI")
    }
}
