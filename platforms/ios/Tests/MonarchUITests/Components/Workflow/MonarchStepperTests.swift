import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchStepperFixtures: ComponentFixtures {
    static let id = "stepper"

    static let steps = [
        MonarchStepperStep(id: "details", label: "Details"),
        MonarchStepperStep(id: "address", label: "Address", description: "Where we send invoices"),
        MonarchStepperStep(id: "team", label: "Team", isOptional: true),
        MonarchStepperStep(id: "review", label: "Review"),
    ]

    static let fixtures = [
        Fixture("busy and finished") {
            VStack(alignment: .leading) {
                MonarchStepper(steps, current: 1, compact: .never, isBusy: true)
                MonarchStepper(steps, current: 1, compact: .always, isBusy: true)
                MonarchStepper(steps, current: 1, compact: .never, isFinished: true)
                MonarchStepper(steps, current: 1, compact: .always, isFinished: true)
            }
        },
        Fixture("horizontal, automatic: the row or the compact form") {
            MonarchStepper(steps, current: 1)
        },
        Fixture("horizontal, never compact") {
            MonarchStepper(steps, current: 2, compact: .never)
        },
        Fixture("always compact, at the first, a middle and the last step") {
            VStack(alignment: .leading) {
                MonarchStepper(steps, current: 0, compact: .always)
                MonarchStepper(steps, currentID: "team", compact: .always)
                MonarchStepper(steps, current: 3, orientation: .vertical, compact: .always)
            }
        },
        Fixture("vertical with every state") {
            MonarchStepper(
                [
                    MonarchStepperStep(id: "a", label: "Complete"),
                    MonarchStepperStep(id: "b", label: "Error", hasError: true),
                    MonarchStepperStep(id: "c", label: "Current", description: "The step the user is on"),
                    MonarchStepperStep(id: "d", label: "Complete out of order", isComplete: true),
                    MonarchStepperStep(id: "e", label: "Upcoming and optional", isOptional: true),
                ],
                current: 2,
                orientation: .vertical
            )
        },
        Fixture("vertical with buttons, one disabled") {
            MonarchStepper(
                [
                    MonarchStepperStep(id: "a", label: "Plan"),
                    MonarchStepperStep(id: "b", label: "Locked", isDisabled: true),
                    MonarchStepperStep(id: "c", label: "Payment"),
                    MonarchStepperStep(id: "d", label: "Reached before going back"),
                    MonarchStepperStep(id: "e", label: "Not reached"),
                ],
                current: 2,
                furthest: 3,
                orientation: .vertical,
                onSelect: { _, _ in }
            )
        },
        Fixture("long names, many steps and custom strings") {
            MonarchStepper(
                (1...12).map { number in
                    MonarchStepperStep(
                        id: "step-\(number)",
                        label: "A step with a name that is long enough to wrap, number \(number)",
                        description: number == 11
                            ? "A description that is also long enough to need a second line" : nil,
                        isOptional: number == 12
                    )
                },
                current: 10,
                orientation: .vertical,
                labels: MonarchStepperLabels(stepOf: { "Schritt \($0) von \($1)" }, optional: "Freiwillig")
            )
        },
        Fixture("one step, and no steps beside a title") {
            VStack(alignment: .leading) {
                MonarchStepper([MonarchStepperStep(id: "only", label: "Only step")], current: 0, compact: .never)
                Text("Setup")
                MonarchStepper([], current: 0)
            }
        },
    ]
}

@Suite("MonarchStepper")
struct MonarchStepperTests {
    typealias Tokens = MonarchStepperTokens

    private let labels = MonarchStepperLabels()
    private let steps = MonarchStepperFixtures.steps

    private func step(
        isComplete: Bool = false,
        hasError: Bool = false,
        isDisabled: Bool = false
    ) -> MonarchStepperStep {
        MonarchStepperStep(id: "x", label: "X", isComplete: isComplete, hasError: hasError, isDisabled: isDisabled)
    }

    // MARK: Position and state

    @Test("A position is kept inside the steps")
    func clamped() {
        #expect(Tokens.clamped(-3, count: 4) == 0)
        #expect(Tokens.clamped(2, count: 4) == 2)
        #expect(Tokens.clamped(9, count: 4) == 3)
        #expect(Tokens.clamped(5, count: 0) == 0)
    }

    @Test("A step is found by its identifier, or the fallback is used")
    func indexOfIdentifier() {
        #expect(Tokens.index(of: "team", in: steps, fallback: 0) == 2)
        #expect(Tokens.index(of: "missing", in: steps, fallback: 1) == 1)
        #expect(Tokens.index(of: nil, in: steps, fallback: 3) == 3)
    }

    @Test("The furthest step is never before the current step and never outside the steps")
    func furthest() {
        #expect(Tokens.furthestIndex(nil, currentIndex: 1, count: 4) == 1)
        #expect(Tokens.furthestIndex(3, currentIndex: 1, count: 4) == 3)
        #expect(Tokens.furthestIndex(0, currentIndex: 2, count: 4) == 2)
        #expect(Tokens.furthestIndex(99, currentIndex: 1, count: 4) == 3)
    }

    @Test("The state of a step comes from its position and its flags")
    func status() {
        #expect(Tokens.status(index: 0, currentIndex: 2, step: step()) == .complete)
        #expect(Tokens.status(index: 2, currentIndex: 2, step: step()) == .current)
        #expect(Tokens.status(index: 3, currentIndex: 2, step: step()) == .upcoming)
        #expect(Tokens.status(index: 3, currentIndex: 2, step: step(isComplete: true)) == .complete)
        #expect(Tokens.status(index: 2, currentIndex: 2, step: step(isComplete: true)) == .current)
        #expect(Tokens.status(index: 0, currentIndex: 2, step: step(hasError: true)) == .error)
        #expect(Tokens.status(index: 2, currentIndex: 2, step: step(hasError: true)) == .error)
        #expect(Tokens.status(index: 3, currentIndex: 2, step: step(isComplete: true, hasError: true)) == .error)
    }

    @Test("Reached and finished steps can be selected; the current, disabled and unreached ones cannot")
    func selectable() {
        func selectable(_ index: Int, _ step: MonarchStepperStep, hasAction: Bool = true) -> Bool {
            Tokens.isSelectable(index: index, currentIndex: 2, furthestIndex: 3, step: step, hasAction: hasAction)
        }
        #expect(selectable(0, step()))
        #expect(selectable(1, step(hasError: true)))
        #expect(!selectable(2, step()))
        #expect(selectable(3, step()))
        #expect(!selectable(4, step()))
        #expect(selectable(4, step(isComplete: true)))
        #expect(!selectable(4, step(isComplete: true, hasError: true)))
        #expect(!selectable(0, step(isDisabled: true)))
        #expect(!selectable(0, step(), hasAction: false))
    }

    @Test("The form follows the orientation and the compact mode")
    func form() {
        #expect(Tokens.form(orientation: .horizontal, compact: .automatic) == .rowOrCompact)
        #expect(Tokens.form(orientation: .horizontal, compact: .never) == .row)
        #expect(Tokens.form(orientation: .horizontal, compact: .always) == .compact)
        #expect(Tokens.form(orientation: .vertical, compact: .automatic) == .column)
        #expect(Tokens.form(orientation: .vertical, compact: .never) == .column)
        #expect(Tokens.form(orientation: .vertical, compact: .always) == .compact)
    }

    @Test("An initialiser keeps the current step inside the steps and finds it by identifier")
    @MainActor
    func initialisers() {
        // Out of range and unknown identifiers must not crash when drawn.
        #expect(Host.layout(MonarchStepper(steps, current: 99, compact: .never)).height > 0)
        #expect(Host.layout(MonarchStepper(steps, current: -1, compact: .always)).height > 0)
        #expect(Host.layout(MonarchStepper(steps, currentID: "missing", furthestID: "nope")).height > 0)
    }

    // MARK: Appearance

    @Test("The marker colours follow the state")
    func marker() {
        let upcoming = Tokens.marker(for: .upcoming)
        #expect(upcoming.background == \MonarchColorTokens.bgSurface)
        #expect(upcoming.foreground == \MonarchColorTokens.textMuted)
        #expect(upcoming.border == \MonarchColorTokens.borderStrong)

        let current = Tokens.marker(for: .current)
        #expect(current.background == \MonarchColorTokens.bgSurface)
        #expect(current.foreground == \MonarchColorTokens.accentStrong)
        #expect(current.border == \MonarchColorTokens.accentSolid)

        let complete = Tokens.marker(for: .complete)
        #expect(complete.background == \MonarchColorTokens.accentSolid)
        #expect(complete.foreground == \MonarchColorTokens.accentOnSolid)

        let error = Tokens.marker(for: .error)
        #expect(error.background == \MonarchColorTokens.dangerSubtle)
        #expect(error.foreground == \MonarchColorTokens.dangerStrong)
        #expect(error.border == \MonarchColorTokens.dangerBorder)
    }

    @Test("Only the current step has a ring, and finished and failed steps have a glyph in place of the number")
    func ringAndSymbol() {
        #expect(Tokens.ring(for: .current) == \MonarchColorTokens.accentSubtle)
        for status in [MonarchStepperStatus.complete, .upcoming, .error] {
            #expect(Tokens.ring(for: status) == nil)
        }
        #expect(Tokens.symbol(for: .complete) == "checkmark")
        #expect(Tokens.symbol(for: .error) == "exclamationmark")
        #expect(Tokens.symbol(for: .current) == nil)
        #expect(Tokens.symbol(for: .upcoming) == nil)
    }

    @Test("The name of a step is coloured and weighted by its state")
    func label() {
        #expect(Tokens.labelColor(for: .current, isUnavailable: false) == \MonarchColorTokens.textDefault)
        #expect(Tokens.labelColor(for: .complete, isUnavailable: false) == \MonarchColorTokens.textDefault)
        #expect(Tokens.labelColor(for: .upcoming, isUnavailable: false) == \MonarchColorTokens.textMuted)
        #expect(Tokens.labelColor(for: .error, isUnavailable: false) == \MonarchColorTokens.dangerStrong)
        for status in MonarchStepperStatus.allCases {
            #expect(Tokens.labelColor(for: status, isUnavailable: true) == \MonarchColorTokens.textDisabled)
        }
        #expect(Tokens.labelWeight(for: .current) == .semibold)
        #expect(Tokens.labelWeight(for: .complete) == .medium)
        #expect(Tokens.labelWeight(for: .upcoming) == .medium)
    }

    @Test("The line after a finished step is in the accent colour")
    func connector() {
        #expect(Tokens.connector(after: .complete) == \MonarchColorTokens.accentSolid)
        for status in [MonarchStepperStatus.current, .upcoming, .error] {
            #expect(Tokens.connector(after: status) == \MonarchColorTokens.borderDefault)
        }
    }

    // MARK: Text

    @Test("The compact bar counts the current step as reached")
    func fraction() {
        #expect(Tokens.fraction(currentIndex: 0, count: 4) == 0.25)
        #expect(Tokens.fraction(currentIndex: 1, count: 4) == 0.5)
        #expect(Tokens.fraction(currentIndex: 3, count: 4) == 1)
        #expect(Tokens.fraction(currentIndex: 9, count: 4) == 1)
        #expect(Tokens.fraction(currentIndex: 0, count: 0) == 0)
    }

    @Test("The number in the marker uses the digits of the locale")
    func markerText() {
        #expect(Tokens.markerText(position: 3, locale: Locale(identifier: "en_US")) == "3")
        #expect(Tokens.markerText(position: 12, locale: Locale(identifier: "en_US")) == "12")
        #expect(Tokens.markerText(position: 3, locale: Locale(identifier: "ar_EG@numbers=arab")) == "٣")
    }

    @Test("A step is read as its position and name, with its state as the value")
    func stepAccessibility() {
        #expect(Tokens.stepLabel(position: 2, total: 5, label: "Address", labels: labels) == "Step 2 of 5, Address")
        #expect(Tokens.stepValue(status: .complete, step: steps[0], labels: labels) == "completed")
        #expect(Tokens.stepValue(status: .error, step: steps[0], labels: labels) == "needs attention")
        #expect(
            Tokens.stepValue(status: .current, step: steps[1], labels: labels)
                == "current step, Where we send invoices"
        )
        #expect(Tokens.stepValue(status: .upcoming, step: steps[2], labels: labels) == "Optional")
        #expect(Tokens.stepValue(status: .upcoming, step: steps[3], labels: labels).isEmpty)
    }

    @Test("The compact form is read as the position and name of the current step")
    func compactAccessibility() {
        #expect(Tokens.compactValue(currentIndex: 1, steps: steps, labels: labels) == "Step 2 of 4, Address")
        #expect(Tokens.compactValue(currentIndex: 0, steps: [], labels: labels).isEmpty)
    }

    @Test("A busy current step adds in progress, and a finished stepper reads the last step as completed")
    func busyAndFinished() {
        #expect(
            Tokens.stepValue(status: .current, step: steps[1], isBusy: true, labels: labels)
                == "current step, in progress, Where we send invoices"
        )
        #expect(
            Tokens.stepValue(status: .current, step: steps[1], isBusy: false, labels: labels)
                == "current step, Where we send invoices"
        )
        #expect(
            Tokens.compactValue(currentIndex: 1, steps: steps, isBusy: true, labels: labels)
                == "Step 2 of 4, Address, in progress"
        )
        #expect(
            Tokens.compactValue(currentIndex: steps.count, steps: steps, labels: labels)
                == "Step 4 of 4, Review, completed"
        )
        #expect(Tokens.fraction(currentIndex: steps.count, count: steps.count) == 1)
        let custom = MonarchStepperLabels(busy: "wird gespeichert")
        #expect(
            Tokens.stepValue(status: .current, step: steps[0], isBusy: true, labels: custom)
                .contains("wird gespeichert")
        )
    }

    @Test("Finished marks every step done, with no current step, and busy is ignored")
    func finishedStatuses() {
        let finished = steps.indices.map { Tokens.status(index: $0, currentIndex: steps.count, step: steps[$0]) }
        #expect(finished.filter { $0 == .current }.isEmpty)
        #expect(finished.filter { $0 == .complete }.count == steps.filter { !$0.hasError }.count)
    }

    @Test("Every string can be replaced")
    func overrides() {
        let custom = MonarchStepperLabels(
            progress: "Fortschritt",
            stepOf: { "Schritt \($0) von \($1)" },
            complete: "abgeschlossen",
            error: "Eingabe prüfen",
            current: "aktueller Schritt",
            optional: "Freiwillig"
        )
        #expect(Tokens.stepOf(2, total: 5, labels: custom) == "Schritt 2 von 5")
        #expect(Tokens.stepLabel(position: 1, total: 4, label: "Team", labels: custom) == "Schritt 1 von 4, Team")
        #expect(Tokens.stepValue(status: .complete, step: steps[0], labels: custom) == "abgeschlossen")
        #expect(Tokens.stepValue(status: .error, step: steps[0], labels: custom) == "Eingabe prüfen")
        #expect(Tokens.stepValue(status: .current, step: steps[2], labels: custom) == "aktueller Schritt, Freiwillig")
        #expect(Tokens.compactValue(currentIndex: 3, steps: steps, labels: custom) == "Schritt 4 von 4, Review")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchStepperStrings.stepOf(2, 5) == "Step 2 of 5")
        let all = [
            MonarchStepperStrings.progress, MonarchStepperStrings.complete, MonarchStepperStrings.error,
            MonarchStepperStrings.current, MonarchStepperStrings.optional,
        ]
        for string in all {
            #expect(!string.hasPrefix("monarch."))
        }
        #expect(Set(all).count == all.count)
    }

    // MARK: Layout

    @MainActor
    @Test("The compact form is shorter than the column, and no steps take no room")
    func layout() {
        let compact = Host.layout(MonarchStepper(steps, current: 1, compact: .always))
        let column = Host.layout(MonarchStepper(steps, current: 1, orientation: .vertical))
        #expect(compact.height > 0)
        #expect(column.height > compact.height)
        #expect(Host.layout(MonarchStepper([], current: 0)).height == 0)
    }

    @MainActor
    @Test("A horizontal stepper that does not fit the width falls back to the compact form")
    func automaticCompact() {
        let long = (1...8).map { MonarchStepperStep(id: "\($0)", label: "A long step name \($0)") }
        let automatic = Host.layout(MonarchStepper(long, current: 2))
        let compact = Host.layout(MonarchStepper(long, current: 2, compact: .always))
        let row = Host.layout(MonarchStepper(long, current: 2, compact: .never))
        #expect(abs(automatic.height - compact.height) < 0.5)
        #expect(row.height > compact.height)

        let short = [MonarchStepperStep(id: "a", label: "A"), MonarchStepperStep(id: "b", label: "B")]
        let fits = Host.layout(MonarchStepper(short, current: 0))
        let never = Host.layout(MonarchStepper(short, current: 0, compact: .never))
        #expect(abs(fits.height - never.height) < 0.5)
    }

    #if os(iOS)
        @MainActor
        @Test("Every step is at least as tall as a touch target")
        func touchTarget() {
            let one = [MonarchStepperStep(id: "a", label: "A")]
            let size = Host.layout(MonarchStepper(one, current: 0, orientation: .vertical))
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
