import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchOnboardingChecklistFixtures: ComponentFixtures {
    static let id = "onboarding-checklist"

    static let steps = [
        MonarchOnboardingStep(id: "profile", title: "Complete your profile", isDone: true),
        MonarchOnboardingStep(
            id: "invite",
            title: "Invite your team",
            description: "Work together on shared projects and approvals."
        ),
        MonarchOnboardingStep(id: "calendar", title: "Connect a calendar", description: "See bookings in one place."),
        MonarchOnboardingStep(id: "billing", title: "Add a payment method"),
    ]

    static let fixtures = [
        Fixture("in progress, with actions and dismiss") {
            MonarchOnboardingChecklist(
                "Get started",
                steps: steps,
                description: "A few steps to set up your workspace.",
                onDismiss: {},
                action: { step in
                    Button(step.id == "invite" ? "Invite" : "Set up") {}
                        .buttonStyle(.monarch(.secondary, size: .sm))
                }
            )
        },
        Fixture("collapsed") {
            MonarchOnboardingChecklist("Get started", steps: steps, isExpanded: .constant(false))
        },
        Fixture("complete") {
            MonarchOnboardingChecklist(
                "Get started",
                steps: steps.map { MonarchOnboardingStep(id: $0.id, title: $0.title, isDone: true) },
                onDismiss: {}
            )
        },
        Fixture("not collapsible, custom complete message") {
            MonarchOnboardingChecklist(
                "Set up payments",
                steps: [MonarchOnboardingStep(id: "a", title: "Verify your business", isDone: true)],
                completeMessage: "Payments are ready.",
                isCollapsible: false
            )
        },
        Fixture("no steps") {
            MonarchOnboardingChecklist("Get started", steps: [])
        },
        Fixture("long text, twelve steps") {
            MonarchOnboardingChecklist(
                "Finish setting up the shared workspace for your whole organisation",
                steps: (1...12).map {
                    MonarchOnboardingStep(
                        id: "s\($0)",
                        title: "A long step title that wraps onto a second line at large text sizes \($0)",
                        description: "And a description that also wraps.",
                        isDone: $0 < 4
                    )
                },
                labels: MonarchOnboardingChecklistLabels(progress: { "\($0)/\($1)" })
            )
        },
    ]
}

@Suite("MonarchOnboardingChecklist")
struct MonarchOnboardingChecklistTests {
    typealias Tokens = MonarchOnboardingChecklistTokens
    let steps = MonarchOnboardingChecklistFixtures.steps

    @Test("Progress counts done steps; the first open step is current")
    func progress() {
        #expect(Tokens.doneCount(steps) == 1)
        #expect(Tokens.currentIndex(steps) == 1)
        #expect(!Tokens.isComplete(steps))
        #expect(Tokens.progressTotal(steps) == 4)
        let done = steps.map { MonarchOnboardingStep(id: $0.id, title: $0.title, isDone: true) }
        #expect(Tokens.isComplete(done))
        #expect(Tokens.currentIndex(done) == nil)
        #expect(!Tokens.isComplete([]))
        #expect(Tokens.progressTotal([]) == 1)
        #expect(Tokens.doneCount([]) == 0)
    }

    @Test("A step's spoken value says done or not done, and marks the current step")
    func stepValue() {
        let labels = MonarchOnboardingChecklistLabels()
        #expect(Tokens.stepAccessibilityValue(steps[0], isCurrent: false, labels: labels) == "Done")
        #expect(Tokens.stepAccessibilityValue(steps[1], isCurrent: true, labels: labels) == "Not done, Current step")
        #expect(Tokens.stepAccessibilityValue(steps[2], isCurrent: false, labels: labels) == "Not done")
        let custom = MonarchOnboardingChecklistLabels(done: "Fait", notDone: "À faire", current: "Étape en cours")
        #expect(Tokens.stepAccessibilityValue(steps[0], isCurrent: false, labels: custom) == "Fait")
        #expect(Tokens.stepAccessibilityValue(steps[1], isCurrent: true, labels: custom) == "À faire, Étape en cours")
    }

    @Test("The progress row is read as its name and its text")
    func progressLabel() {
        #expect(
            Tokens.progressAccessibilityLabel(name: "Setup progress", text: "1 of 4 done")
                == "Setup progress, 1 of 4 done"
        )
    }

    @Test("Markers: a check on done steps, a number on the others, the current one in the accent")
    func markers() {
        #expect(Tokens.markerNumber(0) == "1")
        #expect(Tokens.markerNumber(11) == "12")
        #expect(Tokens.markerFill(isDone: true) == \MonarchColorTokens.successSolid)
        #expect(Tokens.markerFill(isDone: false) == nil)
        #expect(Tokens.markerBorder(isDone: true, isCurrent: false) == \MonarchColorTokens.successSolid)
        #expect(Tokens.markerBorder(isDone: false, isCurrent: true) == \MonarchColorTokens.accentStrong)
        #expect(Tokens.markerBorder(isDone: false, isCurrent: false) == \MonarchColorTokens.borderStrong)
        #expect(Tokens.markerForeground(isDone: true, isCurrent: false) == \MonarchColorTokens.successOnSolid)
        #expect(Tokens.markerForeground(isDone: false, isCurrent: true) == \MonarchColorTokens.accentStrong)
        #expect(Tokens.markerForeground(isDone: false, isCurrent: false) == \MonarchColorTokens.textMuted)
        #expect(Tokens.doneSymbol == "checkmark")
    }

    @Test("Done titles are muted; the current step and the complete message have fills")
    func colours() {
        #expect(Tokens.titleForeground(isDone: true) == \MonarchColorTokens.textMuted)
        #expect(Tokens.titleForeground(isDone: false) == \MonarchColorTokens.textDefault)
        #expect(Tokens.stepBackground(isCurrent: true) == \MonarchColorTokens.bgSubtle)
        #expect(Tokens.stepBackground(isCurrent: false) == nil)
        #expect(Tokens.completeBackground == \MonarchColorTokens.successSubtle)
        #expect(Tokens.completeForeground == \MonarchColorTokens.successStrong)
    }

    @Test("Built-in strings come from the component's strings table, and each can be replaced")
    func strings() {
        let builtIn = MonarchOnboardingChecklistLabels()
        #expect(builtIn.progressText(done: 2, total: 5) == "2 of 5 done")
        #expect(builtIn.resolvedProgressLabel == "Setup progress")
        #expect(builtIn.resolvedDismiss == "Dismiss checklist")
        #expect(builtIn.resolvedDone == "Done")
        #expect(builtIn.resolvedNotDone == "Not done")
        #expect(builtIn.resolvedCurrent == "Current step")
        #expect(builtIn.resolvedComplete == "All done. You are set up.")

        let custom = MonarchOnboardingChecklistLabels(
            progress: { "\($0)/\($1)" },
            progressLabel: "Avancement",
            dismiss: "Fermer",
            complete: "Terminé."
        )
        #expect(custom.progressText(done: 2, total: 5) == "2/5")
        #expect(custom.resolvedProgressLabel == "Avancement")
        #expect(custom.resolvedDismiss == "Fermer")
        #expect(custom.resolvedComplete == "Terminé.")
    }

    #if os(iOS)
        @Test("The checklist lays out with a 44-point collapse row")
        @MainActor
        func hitTarget() {
            let checklist = MonarchOnboardingChecklist("Get started", steps: [], isExpanded: .constant(false))
            #expect(Host.layout(checklist).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
