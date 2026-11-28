import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchFormFieldFixtures: ComponentFixtures {
    static let id = "form-field"
    static let fixtures = [
        Fixture("stack with help, required") {
            MonarchFormField("Time zone", helper: "Used for reminders.", isRequired: true) {
                Picker("Time zone", selection: .constant("UTC")) {
                    Text("UTC").tag("UTC")
                    Text("London").tag("London")
                }
            }
        },
        Fixture("stack with error and optional text") {
            MonarchFormField("Seats", error: "Choose at least 1 seat.", optionalText: "Optional") {
                Stepper("Seats", value: .constant(0), in: 0...10)
            }
        },
        Fixture("row with a switch") {
            MonarchFormField("Weekly digest", helper: "A summary of activity, sent on Monday mornings.", layout: .row) {
                Toggle("Weekly digest", isOn: .constant(true)).labelsHidden()
            }
        },
        Fixture("row with an error") {
            MonarchFormField("Share usage data", error: "Turn this on to join the beta.", layout: .row) {
                Toggle("Share usage data", isOn: .constant(false)).labelsHidden()
            }
        },
        Fixture("disabled") {
            MonarchFormField("Plan", helper: "Managed by your administrator.", isRequired: true) {
                Picker("Plan", selection: .constant(1)) { Text("Team").tag(1) }
            }
            .disabled(true)
        },
        Fixture("long text") {
            MonarchFormField(
                "A label that is long enough to wrap onto a second line on a phone",
                helper: "Help text that is also long, so it wraps under the label at every text size.",
                layout: .row
            ) {
                Toggle("Long", isOn: .constant(true)).labelsHidden()
            }
        },
        Fixture("group of controls") {
            MonarchFormField("Starts", helper: "In your time zone.", isRequired: true, mode: .group) {
                HStack {
                    Stepper("Hour", value: .constant(9), in: 0...23)
                    Stepper("Minute", value: .constant(30), in: 0...59)
                }
            }
        },
    ]
}

@Suite("MonarchFormField")
struct MonarchFormFieldTests {
    private let labels = MonarchFormFieldLabels()

    @Test("The hint reads the error, the help text, then Required")
    func accessibilityHint() {
        #expect(
            MonarchFormFieldTokens.accessibilityHint(helper: nil, error: nil, isRequired: false, labels: labels).isEmpty
        )
        #expect(
            MonarchFormFieldTokens.accessibilityHint(
                helper: "Used for reminders.",
                error: nil,
                isRequired: false,
                labels: labels
            )
                == "Used for reminders."
        )
        #expect(
            MonarchFormFieldTokens.accessibilityHint(
                helper: "Help",
                error: "Too short",
                isRequired: true,
                labels: labels
            )
                == "Error: Too short, Help, Required"
        )
        #expect(
            MonarchFormFieldTokens.accessibilityHint(helper: "", error: "", isRequired: true, labels: labels)
                == "Required"
        )
    }

    @Test("Overrides replace the built-in spoken strings")
    func overrides() {
        let custom = MonarchFormFieldLabels(required: "Obligatoire", error: { "Erreur : \($0)" })
        #expect(
            MonarchFormFieldTokens.accessibilityHint(helper: nil, error: "Trop court", isRequired: true, labels: custom)
                == "Erreur : Trop court, Obligatoire"
        )
    }

    @Test("An empty error is no error")
    func activeError() {
        #expect(MonarchFormFieldTokens.activeError(nil) == nil)
        #expect(MonarchFormFieldTokens.activeError("") == nil)
        #expect(MonarchFormFieldTokens.activeError("Bad") == "Bad")
    }

    @Test("The row layout becomes a stack at the accessibility text sizes")
    func layout() {
        #expect(MonarchFormFieldTokens.stacks(layout: .stack, isAccessibilitySize: false))
        #expect(!MonarchFormFieldTokens.stacks(layout: .row, isAccessibilitySize: false))
        #expect(MonarchFormFieldTokens.stacks(layout: .row, isAccessibilitySize: true))
    }

    @Test("Colours follow the enabled state")
    func colors() {
        #expect(MonarchFormFieldTokens.labelColor(isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(MonarchFormFieldTokens.labelColor(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(MonarchFormFieldTokens.helperColor(isEnabled: true) == \MonarchColorTokens.textMuted)
        #expect(MonarchFormFieldTokens.helperColor(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(MonarchFormFieldTokens.requiredColor(isEnabled: true) == \MonarchColorTokens.dangerStrong)
        #expect(MonarchFormFieldTokens.requiredColor(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(MonarchFormFieldTokens.errorColor == \MonarchColorTokens.dangerStrong)
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchFormFieldStrings.required == "Required")
        #expect(MonarchFormFieldStrings.error("Bad") == "Error: Bad")
    }
}

@Suite("MonarchFormField group mode")
@MainActor
struct MonarchFormFieldGroupTests {
    @Test("Both modes lay out the same chrome")
    func modes() {
        #expect(MonarchFormFieldMode.allCases == [.single, .group])
        let single = Host.layout(MonarchFormField("Starts", helper: "Help") { Text("Control") })
        let group = Host.layout(MonarchFormField("Starts", helper: "Help", mode: .group) { Text("Control") })
        #expect(single.height == group.height)
    }

    @Test("Read only has a built-in spoken string and an override")
    func readOnly() {
        #expect(MonarchFormFieldStrings.readOnly == "Read only")
        #expect(MonarchFormFieldLabels(readOnly: "Locked").readOnly == "Locked")
    }
}
