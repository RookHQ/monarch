import SwiftUI

/// A labelled number field with − and + buttons, helper text and an error state.
///
/// The number is read and written in the person's locale ("1.234,5" in
/// German) with `format`, and the − and + buttons move it by `step`
/// without floating-point drift: from 0.3 with a step of 0.1, one step up
/// is exactly 0.4.
///
/// ```swift
/// MonarchNumberInput("Quantity", value: $quantity, minimum: 0, maximum: 99, isRequired: true)
/// MonarchNumberInput("Discount", value: $discount, format: .percent, minimum: 0, maximum: 1, step: 0.05)
/// ```
///
/// The value is `nil` while the field is empty. Every edit that reads as a
/// number is written to `value` at once, so a Save button tapped straight
/// after typing reads what is shown. Text that is not a number yet ("-",
/// "1,2,") leaves `value` alone, and is put back to the value when the
/// person leaves the field. A number outside the bounds is kept and flagged
/// with a message once the person leaves the field; the buttons stop at
/// the bounds.
///
/// Accessibility: the field is one element whose label is `label`, whose
/// value is the text, and whose hint is the error (spoken as "Error: …"),
/// the helper text and "Required"; a new error is announced. The − and +
/// buttons are named
/// "Decrease <label>" and "Increase <label>", are dimmed at the bounds, and
/// each has a 44-point touch target. The error is shown with an icon and
/// text, not colour alone. Text scales with Dynamic Type.
public struct MonarchNumberInput<Format: ParseableFormatStyle>: View
where Format.FormatInput == Double, Format.FormatOutput == String {
    private let label: String
    @Binding private var value: Double?
    private let format: Format
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

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.locale) private var locale
    @FocusState private var isFocused: Bool
    @State private var draft: String?

    /// A number input with a format, for example `.percent` or `.number.precision(.fractionLength(2))`.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - value: The number, or `nil` while the field is empty.
    ///   - format: How the number is shown and read. It is given the locale of the environment.
    ///   - minimum: The smallest allowed value. Typed values below it are flagged; steps stop at it.
    ///   - maximum: The largest allowed value. Typed values above it are flagged; steps stop at it.
    ///   - step: The amount one press of − or + adds or removes.
    ///   - prompt: Placeholder text shown while the field is empty.
    ///   - helper: Supporting text under the field.
    ///   - error: An error message. Replaces the helper text and the range message while present.
    ///   - showsStepper: Whether the − and + buttons are shown beside the field.
    ///   - rangeMessage: Replaces the built-in message for a value outside the bounds.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - labels: Replacements for the spoken "Required" and the form of an error.
    public init(
        _ label: String,
        value: Binding<Double?>,
        format: Format,
        minimum: Double? = nil,
        maximum: Double? = nil,
        step: Double = 1,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        showsStepper: Bool = true,
        rangeMessage: String? = nil,
        isRequired: Bool = false,
        labels: MonarchFormFieldLabels = MonarchFormFieldLabels()
    ) {
        self.label = label
        _value = value
        self.format = format
        self.minimum = minimum
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

    private var localizedFormat: Format { format.locale(locale) }

    private func text(_ number: Double) -> String { localizedFormat.format(number) }

    private func parse(_ text: String) -> Double? {
        try? localizedFormat.parseStrategy.parse(text)
    }

    private var activeError: String? {
        MonarchNumberInputTokens.shownError(
            error: error,
            rangeMessage: MonarchNumberInputTokens.rangeMessage(
                value: value,
                minimum: minimum,
                maximum: maximum,
                override: rangeMessage,
                format: text
            ),
            isEditing: isFocused
        )
    }

    /// The text in the field: the draft while the person types, else the value.
    private var fieldText: Binding<String> {
        Binding(
            get: { draft ?? value.map(text) ?? "" },
            set: { newText in
                draft = newText
                if case .value(let committed) = MonarchNumberInputTokens.commit(newText, parse: parse) {
                    if committed != value { value = committed }
                }
            }
        )
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchNumberInputTokens.stackSpacing) {
            MonarchFieldLabelRow(label: label, isRequired: isRequired)
            field
            MonarchFieldSupportingText(helper: helper, error: activeError)
        }
        .monarchAnnouncesError(activeError, labels: labels)
        .onChange(of: isFocused) { _, focused in
            if !focused { endEditing() }
        }
    }

    private var field: some View {
        TextField(
            label,
            text: fieldText,
            prompt: prompt.map { Text($0).foregroundStyle(theme.colors.textSubtle) }
        )
        .textFieldStyle(.plain)
        .monarchNumberKeyboard(MonarchNumberInputTokens.keyboard(minimum: minimum))
        .monarchFieldText()
        .monospacedDigit()
        .multilineTextAlignment(showsStepper ? .center : .leading)
        .foregroundStyle(theme.colors[role: MonarchNumberInputTokens.foreground(isEnabled: isEnabled)])
        .focused($isFocused)
        .onSubmit { endEditing() }
        .accessibilityLabel(label)
        .accessibilityHint(
            MonarchFieldChrome.accessibilityHint(
                helper: helper,
                error: activeError,
                isRequired: isRequired,
                labels: labels
            )
        )
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(Rectangle())
        .onTapGesture { isFocused = true }
        // The − button sits inside the box at the start and the + button at the end, as on the web, each a
        // 44 point square beside a hairline, with the value centred between them.
        .modifier(
            MonarchNumberStepperSlot(isShown: showsStepper, leading: { decrementButton }, trailing: { incrementButton })
        )
        .monarchFieldBox(hasError: activeError != nil, isFocused: isFocused)
    }

    private var decrementButton: some View {
        stepButton(
            systemImage: MonarchNumberInputTokens.decrementSymbol,
            title: MonarchNumberInputStrings.decrement(label),
            isEnabled: MonarchNumberInputMath.canDecrement(value, step: step, minimum: minimum),
            dividerLeading: false
        ) { move(-1) }
    }

    private var incrementButton: some View {
        stepButton(
            systemImage: MonarchNumberInputTokens.incrementSymbol,
            title: MonarchNumberInputStrings.increment(label),
            isEnabled: MonarchNumberInputMath.canIncrement(value, step: step, maximum: maximum),
            dividerLeading: true
        ) { move(1) }
    }

    private func stepButton(
        systemImage: String,
        title: String,
        isEnabled: Bool,
        dividerLeading: Bool,
        action: @escaping @MainActor () -> Void
    ) -> some View {
        let divider = Rectangle()
            .fill(theme.colors[role: MonarchNumberInputTokens.stepDividerColor])
            .frame(width: MonarchShapes.borderWidth, height: MonarchAccessibility.minimumHitTarget)
        return HStack(spacing: 0) {
            if dividerLeading { divider }
            Button(action: action) {
                MonarchIcon(systemName: systemImage, size: .sm)
                    .foregroundStyle(
                        theme.colors[
                            role: MonarchNumberInputTokens.stepIconColor(isEnabled: isEnabled && self.isEnabled)
                        ]
                    )
                    .frame(width: MonarchAccessibility.minimumHitTarget, height: MonarchAccessibility.minimumHitTarget)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .disabled(!isEnabled)
            .accessibilityLabel(title)
            .accessibilityValue(MonarchNumberInputTokens.accessibilityValue(value, format: text))
            if !dividerLeading { divider }
        }
    }

    /// Places the − button at the start of the box and the + button at the end, reaching its top, bottom and side
    /// edges: the box's own padding is taken back.
    private struct MonarchNumberStepperSlot<Leading: View, Trailing: View>: ViewModifier {
        let isShown: Bool
        @ViewBuilder let leading: () -> Leading
        @ViewBuilder let trailing: () -> Trailing
        @Environment(\.monarchFieldSize) private var size

        func body(content: Content) -> some View {
            HStack(spacing: MonarchNumberInputTokens.stepperSpacing) {
                if isShown {
                    leading()
                        .padding(.vertical, -MonarchNumberInputTokens.verticalPadding)
                        .padding(.leading, -MonarchControlMetrics.fieldHorizontalPadding(size))
                }
                content
                if isShown {
                    trailing()
                        .padding(.vertical, -MonarchNumberInputTokens.verticalPadding)
                        .padding(.trailing, -MonarchControlMetrics.fieldHorizontalPadding(size))
                }
            }
        }
    }

    /// Commits the draft when the person leaves or submits the field, and
    /// shows the value formatted again.
    private func endEditing() {
        guard let current = draft else { return }
        if case .value(let committed) = MonarchNumberInputTokens.commit(current, parse: parse) {
            if committed != value { value = committed }
        }
        draft = nil
    }

    private func move(_ direction: Int) {
        draft = nil
        value = MonarchNumberInputMath.stepping(
            value,
            by: step,
            direction: direction,
            minimum: minimum,
            maximum: maximum
        )
    }
}

extension MonarchNumberInput where Format == FloatingPointFormatStyle<Double> {
    /// A number input with the standard number format of the locale, with
    /// up to three fraction digits, or as many as `step` has.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - value: The number, or `nil` while the field is empty.
    ///   - minimum: The smallest allowed value. Typed values below it are flagged; steps stop at it.
    ///   - maximum: The largest allowed value. Typed values above it are flagged; steps stop at it.
    ///   - step: The amount one press of − or + adds or removes.
    ///   - prompt: Placeholder text shown while the field is empty.
    ///   - helper: Supporting text under the field.
    ///   - error: An error message. Replaces the helper text and the range message while present.
    ///   - showsStepper: Whether the − and + buttons are shown beside the field.
    ///   - rangeMessage: Replaces the built-in message for a value outside the bounds.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - labels: Replacements for the spoken "Required" and the form of an error.
    public init(
        _ label: String,
        value: Binding<Double?>,
        minimum: Double? = nil,
        maximum: Double? = nil,
        step: Double = 1,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        showsStepper: Bool = true,
        rangeMessage: String? = nil,
        isRequired: Bool = false,
        labels: MonarchFormFieldLabels = MonarchFormFieldLabels()
    ) {
        self.init(
            label,
            value: value,
            format: .number.precision(.fractionLength(0...MonarchNumberInputTokens.fractionDigits(step: step))),
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

extension View {
    /// Asks for a number keyboard on iOS; other platforms have none to choose.
    @ViewBuilder
    fileprivate func monarchNumberKeyboard(_ keyboard: MonarchNumberInputTokens.Keyboard) -> some View {
        #if os(iOS)
            switch keyboard {
            case .decimal: keyboardType(.decimalPad)
            case .signedDecimal: keyboardType(.numbersAndPunctuation)
            }
        #else
            self
        #endif
    }
}

#if DEBUG
    #Preview("MonarchNumberInput") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchNumberInput("Quantity", value: .constant(3), minimum: 0, maximum: 99, helper: "Up to 99.")
                MonarchNumberInput(
                    "Discount",
                    value: .constant(0.15),
                    format: .percent,
                    minimum: 0,
                    maximum: 1,
                    step: 0.05
                )
                MonarchNumberInput("Out of range", value: .constant(120), minimum: 0, maximum: 99)
                MonarchNumberInput("Seats", value: .constant(nil), prompt: "0", showsStepper: false, isRequired: true)
                MonarchNumberInput("Disabled", value: .constant(7)).disabled(true)
            }
        }
    }
#endif
