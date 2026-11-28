import SwiftUI

/// A labelled field for a length of time, typed the way people say it and
/// kept as whole minutes: estimates, time logged, meeting lengths, response
/// targets.
///
/// ```swift
/// @State private var estimate: Int? = 90
///
/// MonarchDurationInput("Estimate", minutes: $estimate, helper: "Type 1h 30m, 90m or 1.5h.")
/// ```
///
/// The field reads `90`, `90m`, `1h 30m`, `1h30`, `1.5h`, `1,5h`, `1:30`,
/// `2 hours 15 minutes` and `1d 4h`, and writes the value back one way,
/// "1h 30m". Typing never changes the binding: the text is read when the
/// person submits or leaves the field. Empty text saves `nil`. Text that
/// cannot be read, or a duration outside the bounds, is kept on screen,
/// flagged and explained, and never saved. The standard `Stepper` moves the
/// value by `step` minutes, snapped to the step grid (50 minutes plus 15 is
/// 1 hour) and kept inside the bounds. With a hardware keyboard, the up and
/// down arrows step, Page Up and Page Down move by an hour, and Escape puts
/// back the saved value.
///
/// Accessibility: the field is one element whose label is `label` and whose
/// hint is the error (spoken as "Error: …") or else the helper text; a new
/// error is announced. The stepper is the system control with the same label,
/// the duration spelled out by the system in the person's language ("1 hour,
/// 30 minutes") as its value, and the increment and decrement actions. The
/// error is shown with an icon and text, not colour alone. The field is at
/// least 44 points high and text scales with Dynamic Type.
public struct MonarchDurationInput: View {
    private let label: String
    @Binding private var minutes: Int?
    private let minimum: Int
    private let maximum: Int?
    private let step: Int
    private let largestUnit: MonarchDurationInputUnit
    private let minutesPerDay: Int
    private let prompt: String?
    private let helper: String?
    private let error: String?
    private let showsStepper: Bool
    private let labels: MonarchDurationInputLabels
    private let customFormat: (@Sendable (Int) -> String)?
    private let customParse: (@Sendable (String) -> Int?)?

    @State private var draft: String
    @State private var problem: MonarchDurationInputTokens.Problem?
    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.locale) private var locale
    @FocusState private var isFocused: Bool

    /// A duration input.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - minutes: The duration in whole minutes, or `nil` when empty. Changed
    ///     only when the text is read (on submit or when focus leaves) or stepped.
    ///   - minimum: The shortest allowed duration in minutes. Shorter text is flagged; steps stop at it.
    ///   - maximum: The longest allowed duration in minutes. Longer text is flagged; steps stop at it.
    ///   - step: The minutes one press of the stepper adds or removes.
    ///   - largestUnit: Writes 26 hours as "26h" (`.hours`) or "1d 2h" (`.days`).
    ///   - minutesPerDay: The minutes in a "d" when reading and writing: 1440 for
    ///     calendar days, 480 for 8-hour work days.
    ///   - prompt: Replaces the built-in placeholder "e.g. 1h 30m".
    ///   - helper: Supporting text under the field.
    ///   - error: An error message. Replaces the helper text and the field's own message while present.
    ///   - showsStepper: Whether the stepper is shown beside the field.
    ///   - labels: Replacements for the built-in strings.
    ///   - format: Writes minutes as text. The default writes "1h 30m".
    ///   - parse: Reads trimmed, non-empty text as minutes, or `nil` when it cannot be read.
    ///     The default reads the forms listed above.
    public init(
        _ label: String,
        minutes: Binding<Int?>,
        minimum: Int = 0,
        maximum: Int? = nil,
        step: Int = 15,
        largestUnit: MonarchDurationInputUnit = .hours,
        minutesPerDay: Int = 1440,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        showsStepper: Bool = true,
        labels: MonarchDurationInputLabels = MonarchDurationInputLabels(),
        format: (@Sendable (Int) -> String)? = nil,
        parse: (@Sendable (String) -> Int?)? = nil
    ) {
        self.label = label
        _minutes = minutes
        self.minimum = minimum
        self.maximum = maximum
        self.step = step
        self.largestUnit = largestUnit
        self.minutesPerDay = minutesPerDay
        self.prompt = prompt
        self.helper = helper
        self.error = error
        self.showsStepper = showsStepper
        self.labels = labels
        self.customFormat = format
        self.customParse = parse
        let initial = minutes.wrappedValue.map {
            format?($0) ?? MonarchDurationMath.format($0, largestUnit: largestUnit, minutesPerDay: minutesPerDay)
        }
        _draft = State(initialValue: initial ?? "")
    }

    private func text(_ value: Int) -> String {
        customFormat?(value)
            ?? MonarchDurationMath.format(value, largestUnit: largestUnit, minutesPerDay: minutesPerDay)
    }

    private func savedText(_ value: Int?) -> String {
        value.map(text) ?? ""
    }

    private func read(_ text: String) -> MonarchDurationMath.Parsed {
        guard let customParse else { return MonarchDurationMath.parse(text, minutesPerDay: minutesPerDay) }
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return .empty }
        return customParse(trimmed).map { .minutes($0) } ?? .invalid
    }

    private var activeError: String? {
        MonarchDurationInputTokens.shownError(
            error: error,
            problem: problem.map {
                MonarchDurationInputTokens.message($0, minimum: minimum, maximum: maximum, labels: labels, format: text)
            }
        )
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchDurationInputTokens.stackSpacing) {
            Text(label)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors[role: MonarchDurationInputTokens.foreground(isEnabled: isEnabled)])
                .accessibilityHidden(true)
            HStack(spacing: MonarchDurationInputTokens.stepperSpacing) {
                field
                if showsStepper { stepper }
            }
            supportingText
        }
        .onChange(of: minutes) { _, newValue in
            draft = savedText(newValue)
            problem = nil
        }
        .onChange(of: isFocused) { _, focused in
            if !focused { commit() }
        }
        .onChange(of: activeError) { _, newError in
            if let newError {
                AccessibilityNotification.Announcement(
                    MonarchDurationInputTokens.accessibilityHint(helper: nil, error: newError, labels: labels)
                ).post()
            }
        }
    }

    private var field: some View {
        let shape = MonarchShapes.control
        let border = MonarchDurationInputTokens.border(hasError: activeError != nil, isEnabled: isEnabled)
        return TextField(
            label,
            text: $draft,
            prompt: Text(prompt ?? MonarchDurationInputStrings.prompt).foregroundStyle(theme.colors.textSubtle)
        )
        .textFieldStyle(.plain)
        .autocorrectionDisabled()
        .monarchTextStyle(.body)
        .monospacedDigit()
        .foregroundStyle(theme.colors[role: MonarchDurationInputTokens.foreground(isEnabled: isEnabled)])
        .focused($isFocused)
        .onSubmit(commit)
        .onChange(of: draft) { _, _ in
            if problem != nil, isFocused { problem = nil }
        }
        .onKeyPress(.upArrow) { keyStep(step) }
        .onKeyPress(.downArrow) { keyStep(-step) }
        .onKeyPress(.pageUp) { keyStep(MonarchDurationInputTokens.pageStep) }
        .onKeyPress(.pageDown) { keyStep(-MonarchDurationInputTokens.pageStep) }
        .onKeyPress(.escape) { restore() }
        .accessibilityLabel(label)
        .accessibilityHint(
            MonarchDurationInputTokens.accessibilityHint(helper: helper, error: activeError, labels: labels)
        )
        .padding(.horizontal, MonarchDurationInputTokens.horizontalPadding)
        .padding(.vertical, MonarchDurationInputTokens.verticalPadding)
        .frame(maxWidth: .infinity, minHeight: MonarchDurationInputTokens.minHeight, alignment: .leading)
        .background(shape.fill(theme.colors[role: MonarchDurationInputTokens.background(isEnabled: isEnabled)]))
        .overlay(shape.strokeBorder(theme.colors[role: border], lineWidth: MonarchShapes.borderWidth))
        .monarchFocusRing(isFocused, in: shape)
        .contentShape(Rectangle())
        .onTapGesture { isFocused = true }
    }

    private var stepper: some View {
        Stepper(
            label,
            onIncrement: MonarchDurationInputTokens.canIncrement(minutes, maximum: maximum) ? { move(step) } : nil,
            onDecrement: MonarchDurationInputTokens.canDecrement(minutes, minimum: minimum) ? { move(-step) } : nil
        )
        .labelsHidden()
        .accessibilityValue(MonarchDurationInputTokens.valueText(minutes, locale: locale, labels: labels))
    }

    @ViewBuilder private var supportingText: some View {
        if let activeError {
            HStack(alignment: .firstTextBaseline, spacing: MonarchDurationInputTokens.messageIconSpacing) {
                MonarchIcon(systemName: MonarchDurationInputTokens.errorSymbol, size: .sm)
                Text(activeError).monarchTextStyle(.caption)
            }
            .foregroundStyle(theme.colors.dangerStrong)
            .accessibilityHidden(true)
        } else if let helper, !helper.isEmpty {
            Text(helper)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
                .accessibilityHidden(true)
        }
    }

    private func commit() {
        switch MonarchDurationInputTokens.commit(read(draft), minimum: minimum, maximum: maximum) {
        case .save(let value): save(value)
        case .reject(let reason): problem = reason
        }
    }

    private func save(_ value: Int?) {
        draft = savedText(value)
        problem = nil
        if value != minutes { minutes = value }
    }

    private func move(_ delta: Int) {
        let base = MonarchDurationMath.stepBase(parsed: read(draft), saved: minutes, minimum: minimum)
        save(
            MonarchDurationMath.stepped(
                base,
                by: delta,
                direction: delta > 0 ? 1 : -1,
                minimum: minimum,
                maximum: maximum
            )
        )
    }

    private func keyStep(_ delta: Int) -> KeyPress.Result {
        guard isEnabled else { return .ignored }
        move(delta)
        return .handled
    }

    private func restore() -> KeyPress.Result {
        guard problem != nil || draft != savedText(minutes) else { return .ignored }
        draft = savedText(minutes)
        problem = nil
        return .handled
    }
}

#if DEBUG
    #Preview("MonarchDurationInput") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchDurationInput("Estimate", minutes: .constant(90), helper: "Type 1h 30m, 90m or 1.5h.")
                MonarchDurationInput("Buffer", minutes: .constant(10), minimum: 5, maximum: 120, step: 5)
                MonarchDurationInput("Response target", minutes: .constant(1680), largestUnit: .days)
                MonarchDurationInput("Empty", minutes: .constant(nil), showsStepper: false)
                MonarchDurationInput("With an error", minutes: .constant(30), error: "Longer than the sprint.")
                MonarchDurationInput("Disabled", minutes: .constant(45)).disabled(true)
            }
        }
    }
#endif
