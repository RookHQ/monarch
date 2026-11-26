import SwiftUI

/// The text and number input of a question, drawn like ``MonarchTextField`` (it uses the
/// shared field box) but without its own label, so ``MonarchFormField`` can give the question its
/// required mark. It adds what `MonarchTextField` does not offer: several lines, a keyboard per type and a
/// read-only form that stays readable and selectable.
struct MonarchFieldRendererTextInput: View {
    let field: MonarchFormFieldDefinition
    @Binding var value: MonarchFormFieldValue?
    let hasError: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.locale) private var locale
    @FocusState private var isFocused: Bool

    private var text: Binding<String> {
        Binding(
            get: {
                if case .text(let text)? = value { return text }
                return ""
            },
            set: { value = .text(MonarchFieldRendererTokens.typed($0, maxLength: field.maxLength)) }
        )
    }

    private var number: Binding<Double?> {
        Binding(
            get: {
                if case .number(let number)? = value, !number.isNaN { return number }
                return nil
            },
            set: { value = $0.map(MonarchFormFieldValue.number) }
        )
    }

    var body: some View {
        input
            .monarchFieldText()
            .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
            .monarchFieldBox(hasError: hasError, isFocused: isFocused, isReadOnly: field.isReadOnly)
            .contentShape(Rectangle())
            .onTapGesture { if !field.isReadOnly { isFocused = true } }
            // A number is short, as on the web.
            .frame(
                maxWidth: field.type == .number ? MonarchFieldRendererTokens.numberWidth : .infinity,
                alignment: .leading
            )
    }

    @ViewBuilder private var input: some View {
        // Without the question's own placeholder the field is empty, as on the web: a nil prompt would show the label.
        let prompt = Text(MonarchFieldRendererTokens.prompt(field) ?? "").foregroundStyle(theme.colors.textSubtle)
        if field.isReadOnly {
            Text(readOnlyText)
                .textSelection(.enabled)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityElement(children: .ignore)
                .accessibilityValue(readOnlyText)
                .accessibilityAddTraits(.isStaticText)
        } else if field.type == .number {
            TextField(field.label, value: number, format: .number.locale(locale), prompt: prompt)
                .textFieldStyle(.plain)
                .monospacedDigit()
                .focused($isFocused)
                .monarchFieldKeyboard(.number)
        } else if field.type == .longText {
            TextField(field.label, text: text, prompt: prompt, axis: .vertical)
                .textFieldStyle(.plain)
                .lineLimit(max(field.rows, 1), reservesSpace: true)
                .focused($isFocused)
        } else {
            TextField(field.label, text: text, prompt: prompt)
                .textFieldStyle(.plain)
                .focused($isFocused)
                .monarchFieldKeyboard(field.type)
        }
    }

    private var readOnlyText: String {
        switch value {
        case .text(let text)?: text
        case .number(let number)? where !number.isNaN: number.formatted(.number.locale(locale))
        default: ""
        }
    }
}

extension View {
    /// The keyboard, content type and capitalisation of a text question. Only iOS has software keyboards;
    /// on the macOS host build this does nothing.
    @ViewBuilder fileprivate func monarchFieldKeyboard(_ type: MonarchFormFieldType) -> some View {
        #if os(iOS)
            switch type {
            case .email:
                keyboardType(.emailAddress).textContentType(.emailAddress)
                    .textInputAutocapitalization(.never).autocorrectionDisabled()
            case .phone:
                keyboardType(.phonePad).textContentType(.telephoneNumber)
            case .url:
                keyboardType(.URL).textContentType(.URL)
                    .textInputAutocapitalization(.never).autocorrectionDisabled()
            case .number:
                keyboardType(.numbersAndPunctuation)
            default:
                self
            }
        #else
            self
        #endif
    }
}

/// The options of a radio question: one row per option with a check mark on the chosen one, as in the
/// system's settings lists. iOS has no radio button.
struct MonarchFieldRendererChoiceList: View {
    let options: [MonarchFormFieldOption]
    @Binding var selection: String?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchTextFieldTokens.cornerRadius, style: .continuous)
        VStack(spacing: 0) {
            ForEach(Array(options.enumerated()), id: \.element.id) { index, option in
                if index > 0 { Divider() }
                row(option)
            }
        }
        .background(shape.fill(theme.colors[role: MonarchTextFieldTokens.background(isEnabled: isEnabled)]))
        .overlay(
            shape.strokeBorder(
                theme.colors[role: MonarchTextFieldTokens.border(hasError: false, isEnabled: isEnabled)],
                lineWidth: MonarchTextFieldTokens.borderWidth
            )
        )
        .clipShape(shape)
    }

    private func row(_ option: MonarchFormFieldOption) -> some View {
        let isSelected = selection == option.value
        return Button {
            selection = option.value
        } label: {
            HStack(spacing: MonarchFieldRendererTokens.controlGap) {
                Text(option.label)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: MonarchFieldRendererTokens.textColor(isEnabled: isEnabled)])
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if isSelected {
                    MonarchIcon(systemName: MonarchFieldRendererTokens.selectedSymbol)
                        .foregroundStyle(
                            theme.colors[role: MonarchFieldRendererTokens.selectedColor(isEnabled: isEnabled)]
                        )
                }
            }
            .padding(.horizontal, MonarchFieldRendererTokens.rowPadding)
            .frame(minHeight: MonarchFieldRendererTokens.rowMinHeight)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

/// A date or time question: a button that sets it, then the system compact picker with a Clear button when
/// the question is optional.
struct MonarchFieldRendererDateInput: View {
    let field: MonarchFormFieldDefinition
    @Binding var value: MonarchFormFieldValue?
    let text: MonarchFieldRendererText
    let today: () -> Date

    @Environment(\.calendar) private var calendar
    @Environment(\.locale) private var locale
    @Environment(\.monarchTheme) private var theme

    private var isTime: Bool { field.type == .time }

    private var current: Date? {
        switch value {
        case .date(let date)? where !isTime: date
        case .text(let answer)? where isTime:
            MonarchFieldRendererTokens.timeDate(answer, on: today(), calendar: calendar)
        default: nil
        }
    }

    var body: some View {
        HStack(spacing: MonarchFieldRendererTokens.controlGap) {
            if let date = current {
                picker(Binding(get: { date }, set: { set($0) }))
                    .labelsHidden()
                if !field.isRequired {
                    MonarchButton(text.clear, variant: .ghost, size: .sm) { value = nil }
                }
            } else if isTime {
                MonarchButton(text.chooseTime, systemImage: MonarchFieldRendererTokens.timeSymbol, size: .sm) {
                    set(today())
                }
            } else {
                emptyDate
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    /// An empty date, drawn as the web's empty date field: the format as the placeholder and a calendar at the end.
    /// A tap sets the first day that can be picked, which opens the system picker.
    private var emptyDate: some View {
        Button {
            set(
                MonarchFieldRendererTokens.initialDate(
                    today: today(),
                    minDate: field.minDate,
                    maxDate: field.maxDate
                )
            )
        } label: {
            HStack(spacing: MonarchFieldRendererTokens.controlGap) {
                Text(MonarchFieldRendererTokens.datePlaceholder(locale: locale))
                    .foregroundStyle(theme.colors.textSubtle)
                Spacer(minLength: 0)
                MonarchIcon(systemName: MonarchFieldRendererTokens.dateSymbol, size: .sm)
                    .foregroundStyle(theme.colors.textMuted)
            }
            .monarchFieldText()
            .monarchFieldBox(hasError: false, isFocused: false, isReadOnly: false)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(text.chooseDate)
    }

    private func set(_ date: Date) {
        value = isTime ? .text(MonarchFieldRendererTokens.timeAnswer(from: date, calendar: calendar)) : .date(date)
    }

    @ViewBuilder private func picker(_ selection: Binding<Date>) -> some View {
        if isTime {
            DatePicker(field.label, selection: selection, displayedComponents: .hourAndMinute)
        } else if let min = field.minDate, let max = field.maxDate, min <= max {
            DatePicker(field.label, selection: selection, in: min...max, displayedComponents: .date)
        } else if let min = field.minDate {
            DatePicker(field.label, selection: selection, in: min..., displayedComponents: .date)
        } else if let max = field.maxDate {
            DatePicker(field.label, selection: selection, in: ...max, displayedComponents: .date)
        } else {
            DatePicker(field.label, selection: selection, displayedComponents: .date)
        }
    }
}

/// A file question: the compact upload row while more files fit, then the chosen files with remove buttons.
struct MonarchFieldRendererFiles: View {
    let field: MonarchFormFieldDefinition
    @Binding var value: MonarchFormFieldValue?
    let hasError: Bool
    let text: MonarchFieldRendererText

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    private var files: [MonarchFormFieldFile] {
        if case .files(let files)? = value { return files }
        return []
    }

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchFieldRendererTokens.controlGap) {
            if files.count < field.maxFiles && isEnabled {
                MonarchFileUpload(
                    allowedContentTypes: field.allowedContentTypes,
                    maxSize: field.maxFileSize,
                    maxFiles: field.maxFiles,
                    currentCount: files.count,
                    variant: .compact,
                    isInvalid: hasError
                ) { accepted, _ in
                    let added = accepted.map { MonarchFormFieldFile(name: $0.name, size: $0.size, file: $0) }
                    value = .files(files + added)
                }
            }
            ForEach(Array(files.enumerated()), id: \.offset) { index, file in
                HStack(spacing: MonarchFieldRendererTokens.controlGap) {
                    MonarchIcon(systemName: MonarchFieldRendererTokens.fileSymbol, size: .sm)
                        .foregroundStyle(theme.colors[role: MonarchFieldRendererTokens.noteColor(isEnabled: isEnabled)])
                    Text(file.name)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors[role: MonarchFieldRendererTokens.textColor(isEnabled: isEnabled)])
                        .frame(maxWidth: .infinity, alignment: .leading)
                    if isEnabled {
                        MonarchButton(text.remove, variant: .ghost, size: .sm) {
                            var remaining = files
                            remaining.remove(at: index)
                            value = .files(remaining)
                        }
                        .accessibilityLabel(text.removeFile(file.name))
                    }
                }
                .frame(minHeight: MonarchFieldRendererTokens.rowMinHeight)
            }
        }
    }
}
