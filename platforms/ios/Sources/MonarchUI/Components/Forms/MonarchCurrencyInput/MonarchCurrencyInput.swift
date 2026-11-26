import SwiftUI

/// An amount of money in one currency, typed and shown in the person's
/// locale ("1.234,50 €" in German) with the currency's own decimals.
///
/// ```swift
/// MonarchCurrencyInput("Amount", value: $amount, currencyCode: "USD")
/// MonarchCurrencyInput("Price", minorUnits: $priceInCents, currencyCode: "EUR", helper: "Including VAT.")
/// ```
///
/// It is ``MonarchNumberInput`` with the system currency format,
/// `FloatingPointFormatStyle<Double>.Currency`: the number keyboard, the
/// label, help and error text, and the range message come from there. A
/// typed amount is written to the binding on every edit, rounded to the
/// currency's decimals (JPY 0, USD 2, KWD 3), so the stored amount is
/// exactly what is shown once the person leaves the field, and a Save
/// button tapped straight after typing reads the typed amount.
/// Amounts can be major units (19.99) or whole minor units (1999), for APIs
/// that store integers. Negative amounts are refused unless
/// `allowsNegative` is set or a negative `minimum` is given.
///
/// Accessibility: as ``MonarchNumberInput``. The field's label is `label`,
/// its value is the formatted amount with the currency, and its hint is the
/// error or the help text. The field is at least 44 points high.
public struct MonarchCurrencyInput: View {
    private let label: String
    private let value: Binding<Double?>
    private let currencyCode: String
    private let currencyDisplay: MonarchMoneyCurrencyDisplay
    private let minimum: Double?
    private let maximum: Double?
    private let step: Double
    private let prompt: String?
    private let helper: String?
    private let error: String?
    private let showsStepper: Bool
    private let rangeMessage: String?
    private let isRequired: Bool
    private let labels: MonarchFormFieldLabels

    /// A currency input whose amount is in major units, such as 19.99.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - value: The amount in major units, or `nil` while the field is empty.
    ///   - currencyCode: The ISO 4217 code, such as `USD`, `EUR` or `JPY`. Sets the symbol and the decimals.
    ///   - currencyDisplay: How the currency is written: symbol, narrow symbol, code or name.
    ///   - minimum: The smallest amount. Without it the smallest is 0, unless `allowsNegative`.
    ///   - maximum: The largest amount.
    ///   - step: The amount one press of the stepper adds or removes.
    ///   - allowsNegative: Whether amounts below zero are allowed (refunds, adjustments).
    ///   - prompt: Placeholder text shown while the field is empty.
    ///   - helper: Supporting text under the field.
    ///   - error: An error message. Replaces the helper text and the range message while present.
    ///   - showsStepper: Whether the stepper is shown. Amounts are usually typed, so it is off.
    ///   - rangeMessage: Replaces the built-in message for an amount outside the bounds.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - labels: Replacements for the spoken "Required" and the form of an error.
    public init(
        _ label: String,
        value: Binding<Double?>,
        currencyCode: String,
        currencyDisplay: MonarchMoneyCurrencyDisplay = .symbol,
        minimum: Double? = nil,
        maximum: Double? = nil,
        step: Double = 1,
        allowsNegative: Bool = false,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        showsStepper: Bool = false,
        rangeMessage: String? = nil,
        isRequired: Bool = false,
        labels: MonarchFormFieldLabels = MonarchFormFieldLabels()
    ) {
        let digits = MonarchCurrencyInputMath.fractionDigits(currencyCode: currencyCode)
        self.label = label
        self.value = Binding(
            get: { value.wrappedValue },
            set: { newValue in
                value.wrappedValue = newValue.map { MonarchCurrencyInputMath.rounded($0, digits: digits) }
            }
        )
        self.currencyCode = currencyCode
        self.currencyDisplay = currencyDisplay
        self.minimum = MonarchCurrencyInputMath.lowest(minimum: minimum, allowsNegative: allowsNegative)
        self.maximum = maximum
        self.step = step
        self.prompt = prompt
        self.helper = helper
        self.error = error
        self.showsStepper = showsStepper
        self.rangeMessage = rangeMessage
        self.isRequired = isRequired
        self.labels = labels
    }

    /// A currency input whose amount is in whole minor units, such as 1999
    /// cents for 19.99 dollars.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - minorUnits: The amount in minor units (cents, paise), or `nil` while the field is empty.
    ///   - currencyCode: The ISO 4217 code, such as `USD`, `EUR` or `JPY`. Sets the symbol and the decimals.
    ///   - currencyDisplay: How the currency is written: symbol, narrow symbol, code or name.
    ///   - minimum: The smallest amount, in minor units. Without it the smallest is 0, unless `allowsNegative`.
    ///   - maximum: The largest amount, in minor units.
    ///   - step: The amount one press of the stepper adds or removes, in minor units.
    ///   - allowsNegative: Whether amounts below zero are allowed (refunds, adjustments).
    ///   - prompt: Placeholder text shown while the field is empty.
    ///   - helper: Supporting text under the field.
    ///   - error: An error message. Replaces the helper text and the range message while present.
    ///   - showsStepper: Whether the stepper is shown. Amounts are usually typed, so it is off.
    ///   - rangeMessage: Replaces the built-in message for an amount outside the bounds.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - labels: Replacements for the spoken "Required" and the form of an error.
    public init(
        _ label: String,
        minorUnits: Binding<Int?>,
        currencyCode: String,
        currencyDisplay: MonarchMoneyCurrencyDisplay = .symbol,
        minimum: Int? = nil,
        maximum: Int? = nil,
        step: Int? = nil,
        allowsNegative: Bool = false,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        showsStepper: Bool = false,
        rangeMessage: String? = nil,
        isRequired: Bool = false,
        labels: MonarchFormFieldLabels = MonarchFormFieldLabels()
    ) {
        let digits = MonarchCurrencyInputMath.fractionDigits(currencyCode: currencyCode)
        let major = { (minor: Int) in MonarchCurrencyInputMath.majorUnits(minor, digits: digits) }
        self.init(
            label,
            value: Binding(
                get: { minorUnits.wrappedValue.map(major) },
                set: { newValue in
                    minorUnits.wrappedValue = newValue.flatMap {
                        MonarchCurrencyInputMath.minorUnits($0, digits: digits)
                    }
                }
            ),
            currencyCode: currencyCode,
            currencyDisplay: currencyDisplay,
            minimum: minimum.map(major),
            maximum: maximum.map(major),
            step: step.map(major) ?? 1,
            allowsNegative: allowsNegative,
            prompt: prompt,
            helper: helper,
            error: error,
            showsStepper: showsStepper,
            rangeMessage: rangeMessage,
            isRequired: isRequired,
            labels: labels
        )
    }

    /// The content of the view.
    public var body: some View {
        MonarchNumberInput(
            label,
            value: value,
            format: MonarchCurrencyInputTokens.format(currencyCode: currencyCode, display: currencyDisplay),
            minimum: minimum,
            maximum: maximum,
            step: step,
            prompt: prompt,
            helper: helper,
            error: error,
            showsStepper: showsStepper,
            rangeMessage: rangeMessage,
            isRequired: isRequired,
            labels: labels
        )
    }
}

#if DEBUG
    #Preview("MonarchCurrencyInput") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchCurrencyInput("Amount", value: .constant(19.99), currencyCode: "USD", helper: "Charged today.")
                MonarchCurrencyInput("Price", minorUnits: .constant(1500), currencyCode: "JPY", currencyDisplay: .code)
                MonarchCurrencyInput("Refund", value: .constant(-5), currencyCode: "EUR", allowsNegative: true)
                MonarchCurrencyInput("Budget", value: .constant(-1), currencyCode: "USD")
                MonarchCurrencyInput("Disabled", value: .constant(nil), currencyCode: "KWD", prompt: "0.000")
                    .disabled(true)
            }
        }
    }
#endif
