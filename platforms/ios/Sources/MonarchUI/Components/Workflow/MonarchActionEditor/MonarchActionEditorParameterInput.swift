import SwiftUI

/// The labelled input of one parameter, chosen by its kind from Monarch components and system controls.
struct MonarchActionEditorParameterInput: View {
    let parameter: MonarchActionParameter
    @Binding var value: MonarchActionValue
    let error: String?
    let text: MonarchActionEditorText

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        switch parameter.kind {
        case .text:
            MonarchTextField(
                parameter.label,
                text: textBinding,
                prompt: parameter.prompt,
                helper: parameter.description,
                error: error
            )
        case .multilineText:
            field {
                MonarchActionEditorMultilineField(
                    label: parameter.label,
                    text: textBinding,
                    prompt: parameter.prompt,
                    hasError: error != nil
                )
            }
        case .number(let minimum, let maximum):
            MonarchNumberInput(
                parameter.label,
                value: Binding(
                    get: { if case .number(let number) = value { number } else { nil } },
                    set: { value = .number($0) }
                ),
                minimum: minimum,
                maximum: maximum,
                prompt: parameter.prompt,
                helper: parameter.description,
                error: error
            )
        case .select(let options):
            let chosen = Binding<String?>(
                get: { if case .option(let id) = value { id } else { nil } },
                set: { value = .option($0) }
            )
            MonarchSelectField(
                parameter.label,
                selection: chosen,
                valueText: options.first { $0.id == chosen.wrappedValue }?.label,
                prompt: parameter.emptyLabel ?? text.choose,
                helper: parameter.description,
                error: error,
                isRequired: parameter.isRequired
            ) {
                Text(parameter.emptyLabel ?? text.choose).tag(String?.none)
                ForEach(options) { option in
                    Text(option.label).tag(String?.some(option.id)).disabled(option.isDisabled)
                }
            }
        case .multiSelect(let options):
            field { multiSelect(options) }
        case .tags(let suggestions):
            MonarchTagInput(
                parameter.label,
                tags: listBinding,
                prompt: parameter.prompt,
                helper: parameter.description,
                error: error,
                suggestions: suggestions
            )
        case .boolean:
            VStack(alignment: .leading, spacing: MonarchActionEditorTokens.messageIconSpacing) {
                Toggle(
                    parameter.label,
                    isOn: Binding(
                        get: { value == .boolean(true) },
                        set: { value = .boolean($0) }
                    )
                )
                .toggleStyle(.monarchCheckbox)
                .accessibilityHint(parameter.description ?? "")
                if let description = parameter.description {
                    Text(description)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(isEnabled ? theme.colors.textMuted : theme.colors.textDisabled)
                        .accessibilityHidden(true)
                }
            }
        }
    }

    private func field(@ViewBuilder _ control: () -> some View) -> some View {
        MonarchFormField(
            parameter.label,
            helper: parameter.description,
            error: error,
            isRequired: parameter.isRequired,
            control: control
        )
    }

    private func multiSelect(_ options: [MonarchComboboxOption]) -> some View {
        let chosen = listBinding.wrappedValue
        return Menu {
            ForEach(options) { option in
                Toggle(
                    option.label,
                    isOn: Binding(
                        get: { chosen.contains(option.id) },
                        set: { isOn in
                            listBinding.wrappedValue = MonarchActionEditorTokens.toggling(
                                option.id,
                                isOn: isOn,
                                in: listBinding.wrappedValue,
                                options: options
                            )
                        }
                    )
                )
                .disabled(option.isDisabled)
            }
        } label: {
            Text(
                MonarchActionEditorTokens.multiSelectSummary(
                    chosen,
                    options: options,
                    prompt: parameter.prompt,
                    text: text
                )
            )
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        #if os(iOS)
            // Keeps the menu open while several choices are checked. iOS only; the macOS build is a convenience.
            .menuActionDismissBehavior(.disabled)
        #endif
        .buttonStyle(.monarch(.secondary, fullWidth: true))
        .accessibilityValue(chosen.compactMap { id in options.first { $0.id == id }?.label }.joined(separator: ", "))
    }

    private var textBinding: Binding<String> {
        Binding(
            get: { if case .text(let string) = value { string } else { "" } },
            set: { value = .text($0) }
        )
    }

    private var listBinding: Binding<[String]> {
        Binding(
            get: { if case .list(let values) = value { values } else { [] } },
            set: { value = .list($0) }
        )
    }
}

/// A bordered multi-line text input for a message. ``MonarchTextField`` is single-line, so this
/// reads `MonarchTextFieldTokens` to match it.
struct MonarchActionEditorMultilineField: View {
    let label: String
    @Binding var text: String
    let prompt: String?
    var hasError = false

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @FocusState private var isFocused: Bool

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchTextFieldTokens.cornerRadius, style: .continuous)
        TextField(
            label,
            text: $text,
            prompt: Text(prompt ?? "").foregroundStyle(theme.colors.textSubtle),
            axis: .vertical
        )
        .textFieldStyle(.plain)
        .monarchTextStyle(.body)
        .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
        .focused($isFocused)
        .padding(.horizontal, MonarchTextFieldTokens.horizontalPadding)
        .padding(.vertical, MonarchTextFieldTokens.verticalPadding)
        .frame(maxWidth: .infinity, minHeight: MonarchActionEditorTokens.multilineMinHeight, alignment: .topLeading)
        .background(shape.fill(theme.colors[role: MonarchTextFieldTokens.background(isEnabled: isEnabled)]))
        .overlay(
            shape.strokeBorder(
                theme.colors[role: MonarchTextFieldTokens.border(hasError: hasError, isEnabled: isEnabled)],
                lineWidth: MonarchTextFieldTokens.borderWidth
            )
        )
        .monarchFocusRing(isFocused, in: shape)
        .contentShape(Rectangle())
        .onTapGesture { isFocused = true }
    }
}
