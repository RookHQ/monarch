import SwiftUI

/// Renders one form question from data: the right Monarch input for its type, with the label, help,
/// required mark, error and an optional "filled in automatically" note. Use it for forms that admins author
/// and store as data, and for the preview of a form builder.
///
/// ```swift
/// ForEach(form.filter { $0.isVisible(in: answers) }) { question in
///     MonarchFieldRenderer(
///         question,
///         value: $answers[question.id],
///         problem: submitted ? question.problem(for: answers[question.id]) : nil
///     )
/// }
/// ```
///
/// Each type uses an existing input: text, email, phone, web address and number are a text field with the
/// matching keyboard; long text grows to `rows` lines; a date or time is a button that sets it, then the
/// system compact `DatePicker`; one option is a menu `Picker` (select) or a list of radio rows (radio);
/// several are ``MonarchMultiSelect`` or ``MonarchCheckboxGroup``; yes or no is a Yes and a No radio, as on the
/// web; a consent is a `Toggle` with the ``MonarchCheckboxToggleStyle``; a rating is
/// ``MonarchRatingPicker``; files are a compact ``MonarchFileUpload`` with the chosen files listed under it;
/// a section is a heading. Inputs without a label of their own sit in a ``MonarchFormField``.
///
/// The renderer holds no answer: `value` is the app's. `problem` (from
/// ``MonarchFormFieldDefinition/problem(for:)``) shows its built-in message; `error`, such as a server error,
/// wins over it. Use ``MonarchFormFieldDefinition/isVisible(in:calendar:)`` to leave out questions whose
/// conditions do not hold. Disable every input with `disabled(_:)`.
///
/// Accessibility: each input is named by the question's label; the error, help and "Required" are its
/// hint, and a new error is announced. Radio and checkbox options are separate buttons and toggles in a
/// group named by the label; the chosen radio option has the selected trait and a check mark. A section is
/// a heading. The automatic-fill note is text with a decorative icon, never colour alone. Each remove
/// button is named "Remove" and the file name.
public struct MonarchFieldRenderer: View {
    private let field: MonarchFormFieldDefinition
    @Binding private var value: MonarchFormFieldValue?
    private let problem: MonarchFormFieldProblem?
    private let error: String?
    private let confidence: Double?
    private let confidenceThreshold: Double
    private let today: () -> Date
    private let labels: MonarchFieldRendererLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.locale) private var locale

    /// A question rendered from data.
    ///
    /// - Parameters:
    ///   - field: The question.
    ///   - value: The answer, or `nil` for none. Set to `nil` when the answer is cleared.
    ///   - problem: A problem to show as the error, with its built-in message.
    ///   - error: An error message to show, such as a server error; replaces the message of `problem`.
    ///   - confidence: When the answer was filled in automatically (for example from an uploaded document),
    ///     the confidence from 0 to 1. Shows a note under the question; below `confidenceThreshold` it asks
    ///     for a check.
    ///   - confidenceThreshold: The confidence under which the note asks people to check the answer.
    ///   - today: The current day, where "Choose a date" and "Choose a time" start. Inject it in tests.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ field: MonarchFormFieldDefinition,
        value: Binding<MonarchFormFieldValue?>,
        problem: MonarchFormFieldProblem? = nil,
        error: String? = nil,
        confidence: Double? = nil,
        confidenceThreshold: Double = 0.6,
        today: @escaping () -> Date = { Date() },
        labels: MonarchFieldRendererLabels = MonarchFieldRendererLabels()
    ) {
        self.field = field
        _value = value
        self.problem = problem
        self.error = error
        self.confidence = confidence
        self.confidenceThreshold = confidenceThreshold
        self.today = today
        self.labels = labels
    }

    private var text: MonarchFieldRendererText { MonarchFieldRendererText(labels: labels, locale: locale) }
    private var activeError: String? {
        MonarchFieldRendererTokens.resolvedError(error: error, problem: problem, text: text)
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchFieldRendererTokens.gap) {
            question
                .disabled(MonarchFieldRendererTokens.isDisabled(field, isEnabled: isEnabled))
            if field.type != .section, !showsNoteInField,
                let note = MonarchFieldRendererTokens.aiNote(confidence: confidence, threshold: confidenceThreshold)
            {
                aiNote(note)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        // The web puts a field's description between its label and its control.
        .monarchHelperAbove()
    }

    /// Whether the field is a plain labelled control, whose automatic-fill note sits between the label and the input.
    private var showsNoteInField: Bool {
        switch field.type {
        case .section, .consent, .yesNo, .rating, .checkboxes, .multiSelect, .select: false
        default: true
        }
    }

    @ViewBuilder private var question: some View {
        switch field.type {
        case .section: section
        case .consent: MonarchFieldRendererConsent(field: field, value: $value, error: activeError)
        case .yesNo where field.showsAsSwitch:
            MonarchFieldRendererSwitch(field: field, value: $value, error: activeError)
        case .yesNo:
            MonarchFormField(field.label, helper: field.description, error: activeError, isRequired: field.isRequired) {
                HStack(spacing: MonarchFieldRendererTokens.rowPadding * 2) {
                    ForEach([true, false], id: \.self) { answer in
                        Toggle(
                            answer ? text.yes : text.no,
                            isOn: Binding(get: { value == .flag(answer) }, set: { if $0 { value = .flag(answer) } })
                        )
                        .toggleStyle(.monarchRadio)
                        .fixedSize()
                    }
                    Spacer(minLength: 0)
                }
                .accessibilityElement(children: .contain)
            }
        case .rating: MonarchFieldRendererRating(field: field, value: $value, error: activeError)
        case .checkboxes:
            MonarchCheckboxGroup(
                field.label,
                selection: Binding(
                    get: {
                        if case .values(let values)? = value { return Set(values) }
                        return []
                    },
                    set: { value = .values(MonarchFieldRendererTokens.ordered($0, options: field.options)) }
                ),
                options: field.options.map { MonarchCheckboxGroupOption(id: $0.value, label: $0.label) },
                helper: field.description,
                error: activeError,
                isRequired: field.isRequired
            )
        case .multiSelect:
            MonarchMultiSelect(
                field.label,
                selection: Binding(
                    get: {
                        if case .values(let values)? = value { return values }
                        return []
                    },
                    set: { value = .values($0) }
                ),
                options: field.options.map { MonarchComboboxOption(id: $0.value, label: $0.label) },
                prompt: field.placeholder ?? text.choose,
                helper: field.description,
                error: activeError,
                maxSelections: field.maxSelections,
                isRequired: field.isRequired
            )
        case .select:
            MonarchSelectField(
                field.label,
                selection: optionSelection,
                valueText: field.options.first { $0.value == optionSelection.wrappedValue }?.label,
                prompt: field.placeholder ?? text.choose,
                helper: field.description,
                error: activeError,
                isRequired: field.isRequired
            ) {
                Text(field.placeholder ?? text.choose).tag(String?.none)
                ForEach(field.options) { Text($0.label).tag(String?.some($0.value)) }
            }
        default:
            MonarchFormField(field.label, helper: field.description, error: activeError, isRequired: field.isRequired) {
                VStack(alignment: .leading, spacing: MonarchFieldRendererTokens.gap) {
                    if let note = MonarchFieldRendererTokens.aiNote(
                        confidence: confidence,
                        threshold: confidenceThreshold
                    ) {
                        aiNote(note)
                    }
                    control
                }
            }
        }
    }

    @ViewBuilder private var control: some View {
        switch field.type {
        case .radio:
            VStack(alignment: .leading, spacing: 0) {
                ForEach(field.options) { option in
                    Toggle(
                        option.label,
                        isOn: Binding(
                            get: { optionSelection.wrappedValue == option.value },
                            set: { if $0 { optionSelection.wrappedValue = option.value } }
                        )
                    )
                    .toggleStyle(.monarchRadio)
                }
            }
            .accessibilityElement(children: .contain)
        case .date, .time:
            MonarchFieldRendererDateInput(field: field, value: $value, text: text, today: today)
                .accessibilityElement(children: .contain)
        case .file:
            MonarchFieldRendererFiles(field: field, value: $value, hasError: activeError != nil, text: text)
                .accessibilityElement(children: .contain)
        default:
            MonarchFieldRendererTextInput(field: field, value: $value, hasError: activeError != nil)
        }
    }

    private var optionSelection: Binding<String?> {
        Binding(
            get: { MonarchFieldRendererTokens.chosenOption(value, options: field.options) },
            set: { value = $0.map(MonarchFormFieldValue.text) }
        )
    }

    private var section: some View {
        VStack(alignment: .leading, spacing: MonarchFieldRendererTokens.gap) {
            Text(field.label)
                .monarchTextStyle(.subheading)
                .foregroundStyle(theme.colors[role: MonarchFieldRendererTokens.textColor(isEnabled: isEnabled)])
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            if let description = field.description, !description.isEmpty {
                Text(description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchFieldRendererTokens.noteColor(isEnabled: isEnabled)])
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private func aiNote(_ note: MonarchFieldRendererTokens.AiNote) -> some View {
        // A pill, as on the web.
        HStack(alignment: .firstTextBaseline, spacing: MonarchFieldRendererTokens.iconGap) {
            MonarchIcon(systemName: MonarchFieldRendererTokens.aiSymbol, size: .sm)
            Text(note == .check ? text.aiCheck : text.aiFilled)
                .monarchTextStyle(.caption)
                .fixedSize(horizontal: false, vertical: true)
        }
        .foregroundStyle(theme.colors[role: MonarchFieldRendererTokens.aiNoteColor(isUnsure: note == .check)])
        .padding(.horizontal, MonarchFieldRendererTokens.iconGap * 2)
        .padding(.vertical, MonarchFieldRendererTokens.iconGap)
        .background(Capsule().fill(theme.colors.aiSubtle))
        .overlay(Capsule().strokeBorder(theme.colors.aiBorder, lineWidth: MonarchShapes.borderWidth))
        .accessibilityElement(children: .combine)
    }
}

#if DEBUG
    #Preview("MonarchFieldRenderer") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchFieldRenderer(
                    MonarchFormFieldDefinition(
                        id: "about",
                        type: .section,
                        label: "About you",
                        description: "We reply within a day."
                    ),
                    value: .constant(nil)
                )
                MonarchFieldRenderer(
                    MonarchFormFieldDefinition(id: "name", type: .shortText, label: "Full name", isRequired: true),
                    value: .constant(.text("Alex Morgan")),
                    confidence: 0.92
                )
                MonarchFieldRenderer(
                    MonarchFormFieldDefinition(id: "email", type: .email, label: "Email", isRequired: true),
                    value: .constant(.text("alex@")),
                    problem: .email
                )
                MonarchFieldRenderer(
                    MonarchFormFieldDefinition(id: "remote", type: .yesNo, label: "Can you relocate?"),
                    value: .constant(.flag(true))
                )
                MonarchFieldRenderer(
                    MonarchFormFieldDefinition(id: "rating", type: .rating, label: "How was the process?"),
                    value: .constant(.number(4))
                )
                MonarchFieldRenderer(
                    MonarchFormFieldDefinition(
                        id: "consent",
                        type: .consent,
                        label: "Consent",
                        isRequired: true,
                        consentText: "I agree to be contacted about this request."
                    ),
                    value: .constant(nil),
                    problem: .mustAgree
                )
            }
        }
    }
#endif
