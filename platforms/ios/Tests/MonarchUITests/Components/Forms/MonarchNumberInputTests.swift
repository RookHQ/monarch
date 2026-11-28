import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchNumberInputFixtures: ComponentFixtures {
    static let id = "number-input"
    static let fixtures = [
        Fixture("value with bounds and helper") {
            MonarchNumberInput("Quantity", value: .constant(3), minimum: 0, maximum: 99, helper: "Up to 99.")
        },
        Fixture("empty with prompt, no stepper") {
            MonarchNumberInput("Weight", value: .constant(nil), prompt: "0", showsStepper: false)
        },
        Fixture("percent format with a fractional step") {
            MonarchNumberInput("Discount", value: .constant(0.15), format: .percent, minimum: 0, maximum: 1, step: 0.05)
        },
        Fixture("out of range") {
            MonarchNumberInput("Quantity", value: .constant(120), minimum: 0, maximum: 99)
        },
        Fixture("error from the app, long label") {
            MonarchNumberInput(
                "Number of seats included in the plan before overage applies",
                value: .constant(-4),
                helper: "Hidden while the error shows.",
                error: "Seats cannot be negative."
            )
        },
        Fixture("large number in a German locale") {
            MonarchNumberInput("Betrag", value: .constant(1234.5)).environment(\.locale, Locale(identifier: "de_DE"))
        },
        Fixture("at the maximum, custom range message") {
            MonarchNumberInput("Level", value: .constant(11), maximum: 10, rangeMessage: "Ten is the limit.")
        },
        Fixture("disabled") {
            MonarchNumberInput("Quantity", value: .constant(7), helper: "Locked.").disabled(true)
        },
        Fixture("required, without a value") {
            MonarchNumberInput("Seats", value: .constant(nil), prompt: "0", isRequired: true)
        },
    ]
}

@Suite("MonarchNumberInput")
struct MonarchNumberInputTests {
    private typealias Math = MonarchNumberInputMath
    private typealias Tokens = MonarchNumberInputTokens

    @Test("Steps do not drift")
    func noDrift() {
        #expect(Math.stepped(0.1, by: 0.1, direction: 1) == 0.2)
        #expect(Math.stepped(0.2, by: 0.1, direction: 1) == 0.3)
        #expect(Math.stepped(0.3, by: 0.1, direction: 1) == 0.4)
        #expect(Math.stepped(0.3, by: 0.1, direction: -1) == 0.2)
        #expect(Math.stepped(1.1, by: 0.01, direction: 1) == 1.11)
        var value = 0.0
        for _ in 0..<10 { value = Math.stepped(value, by: 0.1, direction: 1) }
        #expect(value == 1)
        for _ in 0..<10 { value = Math.stepped(value, by: 0.1, direction: -1) }
        #expect(value == 0)
    }

    @Test("A value between grid points moves to the next grid point in that direction")
    func snapsToGrid() {
        #expect(Math.stepped(5, by: 10, direction: 1) == 10)
        #expect(Math.stepped(5, by: 10, direction: -1) == 0)
        #expect(Math.stepped(-5, by: 10, direction: 1) == 0)
        #expect(Math.stepped(-5, by: 10, direction: -1) == -10)
        #expect(Math.stepped(10, by: 10, direction: 1) == 20)
        #expect(Math.stepped(0.25, by: 0.1, direction: 1) == 0.3)
        #expect(Math.stepped(0.25, by: 0.1, direction: -1) == 0.2)
        #expect(Math.stepped(4, by: 3, direction: 1, base: 1) == 7)
        #expect(Math.stepped(5, by: 3, direction: 1, base: 1) == 7)
        #expect(Math.stepped(5, by: 3, direction: -1, base: 1) == 4)
    }

    @Test("A step that is not positive, or a value that is not finite, is left alone")
    func degenerate() {
        #expect(Math.stepped(5, by: 0, direction: 1) == 5)
        #expect(Math.stepped(5, by: -1, direction: 1) == 5)
        #expect(Math.stepped(.infinity, by: 1, direction: 1) == .infinity)
        #expect(Math.stepped(.nan, by: 1, direction: 1).isNaN)
    }

    @Test("Floor and ceiling work below zero")
    func rounding() throws {
        let negative = try #require(Math.decimal(-0.5))
        #expect(Math.floor(negative) == -1)
        #expect(Math.ceiling(negative) == 0)
        let positive = try #require(Math.decimal(2.25))
        #expect(Math.floor(positive) == 2)
        #expect(Math.ceiling(positive) == 3)
        #expect(Math.floor(3) == 3)
        #expect(Math.ceiling(-3) == -3)
    }

    @Test("Decimal places are counted from the shortest text of the number")
    func decimalPlaces() {
        #expect(Math.decimalPlaces(1) == 0)
        #expect(Math.decimalPlaces(10) == 0)
        #expect(Math.decimalPlaces(0.1) == 1)
        #expect(Math.decimalPlaces(0.05) == 2)
        #expect(Math.decimalPlaces(1e-7) == 7)
        #expect(Math.decimalPlaces(.nan) == 0)
        #expect(Tokens.fractionDigits(step: 1) == 3)
        #expect(Tokens.fractionDigits(step: 0.05) == 3)
        #expect(Tokens.fractionDigits(step: 0.000_1) == 4)
    }

    @Test("The stepper starts an empty value from the minimum and stops at the bounds")
    func stepping() {
        #expect(Math.stepping(nil, by: 1, direction: 1, minimum: nil, maximum: nil) == 0)
        #expect(Math.stepping(nil, by: 1, direction: 1, minimum: 5, maximum: 10) == 5)
        #expect(Math.stepping(nil, by: 1, direction: -1, minimum: nil, maximum: -3) == -3)
        #expect(Math.stepping(9, by: 2, direction: 1, minimum: 0, maximum: 10) == 10)
        #expect(Math.stepping(1, by: 2, direction: -1, minimum: 0, maximum: 10) == 0)
        #expect(Math.stepping(0, by: 2, direction: -1, minimum: 0, maximum: 10) == 0)
        #expect(Math.stepping(50, by: 1, direction: -1, minimum: 0, maximum: 10) == 10)
        #expect(Math.stepping(6, by: 2, direction: 1, minimum: 1, maximum: nil) == 7, "the grid starts at the minimum")
        #expect(Math.clamped(5, minimum: nil, maximum: nil) == 5)
    }

    @Test("The stepper's halves are disabled at the bounds and for a step that is not positive")
    func stepAvailability() {
        #expect(Math.canIncrement(nil, step: 1, maximum: 10))
        #expect(Math.canIncrement(9, step: 1, maximum: 10))
        #expect(!Math.canIncrement(10, step: 1, maximum: 10))
        #expect(!Math.canIncrement(11, step: 1, maximum: 10))
        #expect(Math.canIncrement(11, step: 1, maximum: nil))
        #expect(!Math.canIncrement(1, step: 0, maximum: nil))
        #expect(Math.canDecrement(nil, step: 1, minimum: 0))
        #expect(Math.canDecrement(1, step: 1, minimum: 0))
        #expect(!Math.canDecrement(0, step: 1, minimum: 0))
        #expect(Math.canDecrement(-5, step: 1, minimum: nil))
        #expect(!Math.canDecrement(1, step: -1, minimum: nil))
    }

    @Test("A value outside the bounds is flagged with the bounds formatted")
    func rangeMessage() {
        let format: (Double) -> String = { "<\(Int($0))>" }
        func message(_ value: Double?, _ minimum: Double?, _ maximum: Double?, override: String? = nil) -> String? {
            Tokens.rangeMessage(value: value, minimum: minimum, maximum: maximum, override: override, format: format)
        }
        #expect(message(120, 0, 99) == "Enter a number from <0> to <99>.")
        #expect(message(-1, 0, 99) == "Enter a number from <0> to <99>.")
        #expect(message(-1, 0, nil) == "Enter <0> or more.")
        #expect(message(11, nil, 10) == "Enter <10> or less.")
        #expect(message(11, nil, 10, override: "Ten is the limit.") == "Ten is the limit.")
        #expect(message(50, 0, 99) == nil)
        #expect(message(0, 0, 99) == nil)
        #expect(message(99, 0, 99) == nil)
        #expect(message(nil, 0, 99) == nil)
        #expect(message(1_000, nil, nil) == nil)
        #expect(message(50, 0, 99, override: "Unused") == nil)
    }

    @Test("The app's error is shown before the range message, and VoiceOver reads it before the helper")
    func errors() {
        #expect(Tokens.shownError(error: "App", rangeMessage: "Range") == "App")
        #expect(Tokens.shownError(error: nil, rangeMessage: "Range") == "Range")
        #expect(Tokens.shownError(error: "", rangeMessage: nil) == nil)
        #expect(Tokens.accessibilityHint(helper: "Up to 99.", error: nil) == "Up to 99.")
        #expect(Tokens.accessibilityHint(helper: "Up to 99.", error: "Too many") == "Error: Too many")
        #expect(Tokens.accessibilityHint(helper: nil, error: nil).isEmpty)
        #expect(Tokens.accessibilityValue(3, format: { "\(Int($0)) items" }) == "3 items")
        #expect(Tokens.accessibilityValue(nil, format: { "\($0)" }).isEmpty)
    }

    @Test("The default format writes and reads numbers in the locale")
    func localeFormat() throws {
        let digits = Tokens.fractionDigits(step: 1)
        let base = FloatingPointFormatStyle<Double>.number.precision(.fractionLength(0...digits))
        let german = base.locale(Locale(identifier: "de_DE"))
        #expect(german.format(1234.5) == "1.234,5")
        #expect(try german.parseStrategy.parse("1.234,5") == 1234.5)
        let english = base.locale(Locale(identifier: "en_US"))
        #expect(english.format(1234.5) == "1,234.5")
        #expect(english.format(3) == "3")
        #expect(english.format(0.123_456) == "0.123")
        #expect(try english.parseStrategy.parse("1,234.5") == 1234.5)
    }

    @Test("The keyboard has a minus sign unless negative numbers are ruled out")
    func keyboard() {
        #expect(Tokens.keyboard(minimum: 0) == .decimal)
        #expect(Tokens.keyboard(minimum: 5) == .decimal)
        #expect(Tokens.keyboard(minimum: -1) == .signedDecimal)
        #expect(Tokens.keyboard(minimum: nil) == .signedDecimal)
    }

    @Test("The field takes its colours and its height from the text field")
    func appearance() {
        #expect(Tokens.border(hasError: false, isEnabled: true) == \MonarchColorTokens.borderInput)
        #expect(Tokens.border(hasError: true, isEnabled: true) == \MonarchColorTokens.dangerStrong)
        #expect(Tokens.border(hasError: true, isEnabled: false) == \MonarchColorTokens.borderSubtle)
        #expect(Tokens.background(isEnabled: true) == \MonarchColorTokens.bgSurface)
        #expect(Tokens.background(isEnabled: false) == \MonarchColorTokens.bgSubtle)
        #expect(Tokens.foreground(isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(Tokens.foreground(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(Tokens.minHeight >= MonarchAccessibility.minimumHitTarget)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchNumberInputStrings.range(minimum: "0", maximum: "99") == "Enter a number from 0 to 99.")
        #expect(MonarchNumberInputStrings.atLeast("0") == "Enter 0 or more.")
        #expect(MonarchNumberInputStrings.atMost("10") == "Enter 10 or less.")
        #expect(MonarchNumberInputStrings.error("Too many") == "Error: Too many")
    }

    #if os(iOS)
        // The touch target applies on the simulator.
        @Test("The field is at least as tall as the touch target")
        @MainActor
        func hitTarget() {
            let input = MonarchNumberInput("Quantity", value: .constant(3), showsStepper: false)
            #expect(Host.layout(input).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}

/// C-021: a typed number reaches the binding on every edit, so an action
/// that reads the value straight after typing (a Save button tapped while
/// the field still has focus) sees what the person typed.
@Suite("MonarchNumberInput commit on edit")
@MainActor
struct MonarchNumberInputCommitTests {
    private typealias Tokens = MonarchNumberInputTokens

    private static let english = Locale(identifier: "en_US")

    private static func parse(_ text: String) -> Double? {
        try? FloatingPointFormatStyle<Double>.number.locale(english).parseStrategy.parse(text)
    }

    /// Stands in for the app: a value and a Save action that reads it.
    @MainActor
    private final class Form {
        var quantity: Double? = 3
        var saved: [Double?] = []
        func save() { saved.append(quantity) }
    }

    /// Types `text` one character at a time, as the field's binding receives it.
    private static func type(_ text: String, into form: Form) {
        var draft = ""
        for character in text {
            draft.append(character)
            if case .value(let committed) = Tokens.commit(draft, parse: parse) { form.quantity = committed }
        }
    }

    @Test("Type 12, tap Save without leaving the field: the value is 12")
    func saveRightAfterTyping() {
        let form = Form()
        Self.type("12", into: form)
        form.save()
        #expect(form.saved == [12])
    }

    @Test("Every edit that reads as a number commits; others leave the value")
    func commits() {
        #expect(Tokens.commit("12", parse: Self.parse) == .value(12))
        #expect(Tokens.commit(" 1.5 ", parse: Self.parse) == .value(1.5))
        #expect(Tokens.commit("", parse: Self.parse) == .value(nil))
        #expect(Tokens.commit("   ", parse: Self.parse) == .value(nil))
        #expect(Tokens.commit("-", parse: Self.parse) == .none)
        #expect(Tokens.commit("abc", parse: Self.parse) == .none)
    }

    @Test("A typed number outside the bounds is committed, so the app reads what is shown")
    func outOfRangeCommits() {
        let form = Form()
        Self.type("120", into: form)
        #expect(form.quantity == 120)
    }

    @Test("The range message waits until the person leaves the field; the app's error does not")
    func rangeMessageOnBlur() {
        #expect(Tokens.shownError(error: nil, rangeMessage: "At most 99", isEditing: true) == nil)
        #expect(Tokens.shownError(error: nil, rangeMessage: "At most 99", isEditing: false) == "At most 99")
        #expect(Tokens.shownError(error: "Taken", rangeMessage: "At most 99", isEditing: true) == "Taken")
    }

    @Test("A currency amount commits on edit too, rounded to its decimals")
    func currency() {
        let format = MonarchCurrencyInputTokens.format(currencyCode: "USD", display: .symbol, locale: Self.english)
        let parse = { (text: String) in try? format.parseStrategy.parse(text) }
        var stored: Double?
        if case .value(let committed) = Tokens.commit("$19.99", parse: parse) {
            stored = committed.map { MonarchCurrencyInputMath.rounded($0, digits: 2) }
        }
        #expect(stored == 19.99)
    }

    @Test("The − and + buttons are named after the field and keep a 44-point target")
    func stepButtons() {
        #expect(MonarchNumberInputStrings.increment("Quantity") == "Increase Quantity")
        #expect(MonarchNumberInputStrings.decrement("Quantity") == "Decrease Quantity")
        let button = MonarchButton("Increase Quantity", systemImage: "plus", size: .lg, isIconOnly: true) {}
        let size = Host.layout(button.fixedSize())
        #expect(size.height >= MonarchAccessibility.minimumHitTarget)
    }

    @Test("A required number input says so in its hint")
    func required() {
        let hint = MonarchFieldChrome.accessibilityHint(helper: "Up to 99.", error: nil, isRequired: true)
        #expect(hint == "Up to 99., Required")
        #expect(Host.layout(MonarchNumberInput("Seats", value: .constant(nil), isRequired: true)).height > 0)
    }
}
