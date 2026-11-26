import SwiftUI

/// A table or list cell that edits one value in place: text, number, select or date.
///
/// Text and number cells show the value; touching it opens a field. Return or
/// the save button saves, the cancel button (or a hardware Escape) leaves,
/// and leaving the field saves. Select cells are a system menu and date cells
/// the system compact date picker: choosing saves at once. Numbers are read
/// in the locale; required values, bounds and the app's own rule are checked
/// before `onCommit` runs. While `onCommit` runs the cell shows progress; if
/// it throws, the message is shown, a typed draft is kept and a chosen value
/// goes back to the saved one. The app owns persistence: on success the new
/// value is written to `value`.
///
/// ```swift
/// MonarchEditableCell("Quantity", value: $quantity, kind: .number, minimum: 1, maximum: 99, isRequired: true) { next in
///     try await api.setQuantity(next)
/// }
/// ```
///
/// Accessibility: in view mode a text or number cell is one button whose
/// label is `label`, whose value is the shown text and whose hint is "Edit".
/// Fields, menus and date pickers carry `label`. An error is shown with an
/// icon and text, spoken as the editor's hint ("Error: …") and announced when
/// it appears. Every control has a 44-point touch target and scales with
/// Dynamic Type.
public struct MonarchEditableCell: View {
    private let label: String
    @Binding private var value: MonarchEditableCellValue
    private let kind: MonarchEditableCellKind
    private let options: [MonarchEditableCellOption]
    private let placeholder: String?
    private let minimum: Double?
    private let maximum: Double?
    private let minimumDate: Date?
    private let maximumDate: Date?
    private let isRequired: Bool
    private let isNumeric: Bool?
    private let savesOnFocusLoss: Bool
    private let labels: MonarchEditableCellLabels
    private let format: (@MainActor (MonarchEditableCellValue) -> String)?
    private let validate: (@MainActor (MonarchEditableCellValue) -> String?)?
    private let onCancel: (@MainActor () -> Void)?
    private let onCommit: @MainActor (MonarchEditableCellValue) async throws -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.calendar) private var calendar
    @Environment(\.locale) private var locale
    @State private var session: MonarchEditableCellSession
    @FocusState private var isFocused: Bool

    /// A cell that edits in place.
    ///
    /// - Parameters:
    ///   - label: What the cell holds, for example "Status for Acme", already localised. Read
    ///     by VoiceOver; not drawn.
    ///   - value: The saved value. Written after a successful save.
    ///   - kind: The editor: text, number, select or date.
    ///   - options: The choices of a select cell.
    ///   - placeholder: The text of an empty cell and the prompt of text and number fields.
    ///   - minimum: The lowest value of a number cell.
    ///   - maximum: The highest value of a number cell.
    ///   - minimumDate: The earliest day of a date cell.
    ///   - maximumDate: The latest day of a date cell.
    ///   - isRequired: Whether an empty value is refused. A required select cell offers no "None".
    ///   - isNumeric: Whether the value is trailing-aligned with tabular figures. `nil` means
    ///     number cells only.
    ///   - startsEditing: Whether a text or number field is open when the cell first appears.
    ///   - savesOnFocusLoss: Whether leaving a text or number field saves the draft.
    ///   - labels: Replacements for the built-in strings.
    ///   - format: Returns the text of a value in view mode. The default is the option label,
    ///     the number or day in the locale, or the text.
    ///   - validate: Returns a message to refuse a value before saving, or `nil` to allow it.
    ///   - onCancel: Runs when the person leaves a field without saving.
    ///   - onCommit: Persists a new value. Not called when the value is unchanged. Throw to
    ///     refuse it with the error's description.
    public init(
        _ label: String,
        value: Binding<MonarchEditableCellValue>,
        kind: MonarchEditableCellKind = .text,
        options: [MonarchEditableCellOption] = [],
        placeholder: String? = nil,
        minimum: Double? = nil,
        maximum: Double? = nil,
        minimumDate: Date? = nil,
        maximumDate: Date? = nil,
        isRequired: Bool = false,
        isNumeric: Bool? = nil,
        startsEditing: Bool = false,
        savesOnFocusLoss: Bool = true,
        labels: MonarchEditableCellLabels = MonarchEditableCellLabels(),
        format: (@MainActor (MonarchEditableCellValue) -> String)? = nil,
        validate: (@MainActor (MonarchEditableCellValue) -> String?)? = nil,
        onCancel: (@MainActor () -> Void)? = nil,
        onCommit: @escaping @MainActor (MonarchEditableCellValue) async throws -> Void
    ) {
        self.label = label
        _value = value
        self.kind = kind
        self.options = options
        self.placeholder = placeholder
        self.minimum = minimum
        self.maximum = maximum
        self.minimumDate = minimumDate
        self.maximumDate = maximumDate
        self.isRequired = isRequired
        self.isNumeric = isNumeric
        self.savesOnFocusLoss = savesOnFocusLoss
        self.labels = labels
        self.format = format
        self.validate = validate
        self.onCancel = onCancel
        self.onCommit = onCommit
        let session = MonarchEditableCellSession()
        if startsEditing, kind == .text || kind == .number {
            session.begin(draft: MonarchEditableCellTokens.draft(from: value.wrappedValue, locale: .current))
        }
        _session = State(initialValue: session)
    }

    private var numeric: Bool { MonarchEditableCellTokens.isNumeric(isNumeric, kind: kind) }

    private var rules: MonarchEditableCellRules {
        MonarchEditableCellRules(
            kind: kind,
            isRequired: isRequired,
            minimum: minimum,
            maximum: maximum,
            minimumDate: minimumDate,
            maximumDate: maximumDate,
            calendar: calendar,
            locale: locale
        )
    }

    private var display: String? {
        MonarchEditableCellTokens.display(
            value,
            options: options,
            calendar: calendar,
            locale: locale,
            format: format
        )
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: numeric ? .trailing : .leading, spacing: MonarchEditableCellTokens.stackSpacing) {
            content
            if let message = session.errorMessage, isEnabled {
                MonarchFieldErrorRow(message: message)
            }
        }
        .frame(maxWidth: .infinity, alignment: numeric ? .trailing : .leading)
        .monarchAnnouncesError(
            session.errorMessage,
            labels: MonarchFormFieldLabels(error: { [error = labels.error] in
                error?($0) ?? MonarchEditableCellStrings.error($0)
            })
        )
    }

    @ViewBuilder private var content: some View {
        if !isEnabled {
            valueText(isEmpty: display == nil)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
                .accessibilityValue(MonarchEditableCellTokens.shownText(display: display, placeholder: placeholder))
        } else {
            switch kind {
            case .text, .number:
                if session.isEditing { textEditor } else { viewButton }
            case .select:
                selectEditor
            case .date:
                if session.isEditing {
                    emptyDateEditor
                } else if case .date = session.pending ?? value {
                    dateEditor
                } else {
                    viewButton
                }
            }
        }
    }

    // MARK: View mode

    private func valueText(isEmpty: Bool) -> some View {
        Text(MonarchEditableCellTokens.shownText(display: display, placeholder: emptyText))
            .monarchTextStyle(.body)
            .monospacedDigit()
            .foregroundStyle(
                theme.colors[role: MonarchEditableCellTokens.viewForeground(isEmpty: isEmpty, isEnabled: isEnabled)]
            )
            .multilineTextAlignment(numeric ? .trailing : .leading)
            .padding(.horizontal, MonarchEditableCellTokens.horizontalPadding)
            .frame(
                maxWidth: .infinity,
                minHeight: MonarchEditableCellTokens.minHeight,
                alignment: numeric ? .trailing : .leading
            )
    }

    /// An empty date cell reads "None" when it has no placeholder, so it has something to touch.
    private var emptyText: String? {
        kind == .date ? (placeholder ?? labels.noValue ?? MonarchEditableCellStrings.noValue) : placeholder
    }

    private var viewButton: some View {
        Button {
            begin()
        } label: {
            valueText(isEmpty: display == nil)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        .accessibilityValue(MonarchEditableCellTokens.shownText(display: display, placeholder: emptyText))
        .accessibilityHint(MonarchEditableCellTokens.editHint(labels))
    }

    private func begin() {
        let day = MonarchEditableCellTokens.initialDay(
            today: .now,
            minimumDate: minimumDate,
            maximumDate: maximumDate,
            calendar: calendar
        )
        session.begin(draft: MonarchEditableCellTokens.draft(from: value, locale: locale), day: day)
    }

    // MARK: Text and number

    private var textEditor: some View {
        @Bindable var session = session
        let shape = MonarchShapes.control
        let border = MonarchEditableCellTokens.border(hasError: session.errorMessage != nil)
        return HStack(spacing: MonarchEditableCellTokens.accessorySpacing) {
            TextField(
                label,
                text: $session.draft,
                prompt: placeholder.map { Text($0).foregroundStyle(theme.colors.textSubtle) }
            )
            .textFieldStyle(.plain)
            .monarchTextStyle(.body)
            .monospacedDigit()
            .multilineTextAlignment(numeric ? .trailing : .leading)
            .foregroundStyle(theme.colors.textDefault)
            .focused($isFocused)
            .disabled(session.isSaving)
            .accessibilityLabel(label)
            .accessibilityValue(
                session.isSaving
                    ? MonarchEditableCellTokens.accessibilityValue(shown: "", isSaving: true, labels: labels)
                    : session.draft
            )
            .accessibilityHint(MonarchEditableCellTokens.errorHint(session.errorMessage, labels: labels))
            #if os(iOS)
                .keyboardType(kind == .number ? .numbersAndPunctuation : .default)
            #endif
            .onSubmit { commitDraft() }
            .onAppear { isFocused = true }
            .onKeyPress(.escape) {
                cancel()
                return .handled
            }
            .onChange(of: isFocused) { _, focused in
                if !focused, savesOnFocusLoss, session.savesOnFocusLoss { commitDraft() }
            }
            accessories(onSave: commitDraft)
        }
        .padding(.leading, MonarchEditableCellTokens.horizontalPadding)
        .padding(.trailing, session.isSaving ? 0 : MonarchEditableCellTokens.horizontalPadding)
        .accessibilityAction(named: Text(labels.save ?? MonarchEditableCellStrings.save)) { commitDraft() }
        .accessibilityAction(named: Text(labels.cancel ?? MonarchEditableCellStrings.cancel)) { cancel() }
        .frame(maxWidth: .infinity, minHeight: MonarchEditableCellTokens.minHeight)
        .background(shape.fill(theme.colors[role: MonarchEditableCellTokens.fieldBackground]))
        .overlay(shape.strokeBorder(theme.colors[role: border], lineWidth: MonarchShapes.borderWidth))
        .monarchFocusRing(isFocused, in: shape)
    }

    /// Save and cancel, or progress while saving.
    @ViewBuilder
    private func accessories(onSave: @escaping @MainActor () -> Void) -> some View {
        if session.isSaving {
            savingIndicator
        }
        // No save and cancel buttons, as on the web: Return saves, Escape cancels, and VoiceOver has both as actions.
    }

    private var savingIndicator: some View {
        ProgressView()
            .controlSize(.small)
            .monarchMinimumHitTarget()
            .accessibilityLabel(labels.saving ?? MonarchEditableCellStrings.saving)
    }

    // MARK: Select

    private var selectEditor: some View {
        let selection = Binding<MonarchEditableCellValue>(
            get: { session.pending ?? value },
            set: { submit(.success($0)) }
        )
        let current = session.pending ?? value
        return HStack(spacing: MonarchEditableCellTokens.accessorySpacing) {
            MonarchSelectField(
                label,
                selection: selection,
                valueText: options.first { MonarchEditableCellValue.text($0.value) == current }?.label
                    ?? (current == .empty ? labels.noValue ?? MonarchEditableCellStrings.noValue : nil),
                style: .compact
            ) {
                if MonarchEditableCellTokens.showsNoValueChoice(isRequired: isRequired, value: current) {
                    Text(labels.noValue ?? MonarchEditableCellStrings.noValue).tag(MonarchEditableCellValue.empty)
                }
                ForEach(options) { option in
                    Text(option.label)
                        .tag(MonarchEditableCellValue.text(option.value))
                        .disabled(option.isDisabled)
                }
            }
            .disabled(session.isSaving)
            .accessibilityLabel(label)
            .accessibilityHint(MonarchEditableCellTokens.errorHint(session.errorMessage, labels: labels))
            if session.isSaving { savingIndicator }
        }
        .frame(
            maxWidth: .infinity,
            minHeight: MonarchEditableCellTokens.minHeight,
            alignment: numeric ? .trailing : .leading
        )
    }

    // MARK: Date

    private var dateRange: ClosedRange<Date> {
        let lower = minimumDate.map { calendar.startOfDay(for: $0) } ?? .distantPast
        let upper = maximumDate.map { calendar.startOfDay(for: $0) } ?? .distantFuture
        return lower...max(lower, upper)
    }

    private var dateEditor: some View {
        let selection = Binding<Date>(
            get: {
                if case .date(let date) = session.pending ?? value { return date }
                return calendar.startOfDay(for: .now)
            },
            set: { submit(.success(.date(calendar.startOfDay(for: $0)))) }
        )
        return HStack(spacing: MonarchEditableCellTokens.accessorySpacing) {
            DatePicker(label, selection: selection, in: dateRange, displayedComponents: .date)
                .labelsHidden()
                .disabled(session.isSaving)
                .accessibilityHint(MonarchEditableCellTokens.errorHint(session.errorMessage, labels: labels))
            if session.isSaving { savingIndicator }
        }
        .frame(
            maxWidth: .infinity,
            minHeight: MonarchEditableCellTokens.minHeight,
            alignment: numeric ? .trailing : .leading
        )
        .contextMenu {
            if !isRequired {
                Button(labels.noValue ?? MonarchEditableCellStrings.noValue, systemImage: "xmark") {
                    submit(.success(.empty))
                }
            }
        }
    }

    /// An empty date cell that was touched: a picker on a proposed day, with save and cancel.
    private var emptyDateEditor: some View {
        @Bindable var session = session
        return HStack(spacing: MonarchEditableCellTokens.accessorySpacing) {
            DatePicker(label, selection: $session.dayDraft, in: dateRange, displayedComponents: .date)
                .labelsHidden()
                .disabled(session.isSaving)
                .accessibilityHint(MonarchEditableCellTokens.errorHint(session.errorMessage, labels: labels))
            accessories {
                submit(.success(.date(calendar.startOfDay(for: session.dayDraft))))
            }
        }
        .frame(
            maxWidth: .infinity,
            minHeight: MonarchEditableCellTokens.minHeight,
            alignment: numeric ? .trailing : .leading
        )
    }

    // MARK: Actions

    private func commitDraft() {
        submit(
            MonarchEditableCellTokens.parse(
                session.draft,
                kind: kind,
                locale: locale,
                invalidNumber: labels.invalidNumber ?? MonarchEditableCellStrings.invalidNumber
            )
        )
    }

    private func submit(_ parsed: Result<MonarchEditableCellValue, MonarchEditableCellParseError>) {
        let session = session
        let saved = value
        let rules = rules
        let labels = labels
        let validate = validate
        Task {
            let outcome = await session.commit(
                parsed,
                check: {
                    MonarchEditableCellTokens.check(
                        $0,
                        saved: saved,
                        rules: rules,
                        labels: labels,
                        validate: validate
                    )
                },
                message: { MonarchEditableCellTokens.failureMessage(for: $0, labels: labels) },
                onCommit: onCommit
            )
            switch outcome {
            case .saved(let next):
                value = next
            case .invalid, .failed:
                if kind == .text || kind == .number { isFocused = true }
            case .ignored, .unchanged:
                break
            }
        }
    }

    private func cancel() {
        if session.cancel() { onCancel?() }
    }
}

#if DEBUG
    #Preview("MonarchEditableCell") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchEditableCell("Title", value: .constant(.text("Quarterly review"))) { _ in }
                MonarchEditableCell("Quantity", value: .constant(.number(12)), kind: .number, minimum: 1) { _ in }
                MonarchEditableCell(
                    "Status",
                    value: .constant(.text("open")),
                    kind: .select,
                    options: [
                        MonarchEditableCellOption("Open", value: "open"),
                        MonarchEditableCellOption("Done", value: "done"),
                    ]
                ) { _ in }
                MonarchEditableCell("Due", value: .constant(.date(.now)), kind: .date) { _ in }
                MonarchEditableCell("Notes", value: .constant(.empty), placeholder: "Add a note", startsEditing: true) {
                    _ in
                }
                MonarchEditableCell("Owner", value: .constant(.text("Read only"))) { _ in }.disabled(true)
            }
        }
    }
#endif
