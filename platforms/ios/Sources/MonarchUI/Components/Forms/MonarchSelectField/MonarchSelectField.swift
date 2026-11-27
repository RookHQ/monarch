import SwiftUI

/// A select: the Monarch field box with the chosen value and a chevron, that
/// opens the system menu of options.
///
/// The `Picker` with `.pickerStyle(.menu)` draws bare text with no box. This
/// view gives the same control the Monarch anatomy: the label above, a 1 point
/// box of the field height, the value and a chevron inside it, the help or
/// error line below. The menu is the system `Menu` with an inline `Picker`,
/// so the options, the check mark, scrolling, dismissal and the keyboard
/// behaviour are the system's.
///
/// ```swift
/// MonarchSelectField(
///     "Status",
///     selection: $status,
///     options: [
///         MonarchSelectOption("Open", value: "open"),
///         MonarchSelectOption("Closed", value: "closed"),
///     ],
///     prompt: "Choose a status",
///     helper: "Shown to the customer."
/// )
/// ```
///
/// Accessibility: the field is one button named by `label`, with the chosen
/// text as its value and the help, error and "Required" as its hint. The
/// visible label and the chevron are hidden from VoiceOver. The box is at
/// least 44 points high, scales with Dynamic Type and shows the error with an
/// icon as well as colour. A new error is announced.
public struct MonarchSelectField<Value: Hashable, Content: View>: View {
    private let label: String
    @Binding private var selection: Value
    private let shownText: String
    private let hasValue: Bool
    private let helper: String?
    private let error: String?
    private let isRequired: Bool
    private let labelVisibility: Visibility
    private let labels: MonarchFormFieldLabels
    private let style: MonarchSelectStyle
    private let content: Content

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// A select with its options given as views.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised. Always required: it is the spoken name.
    ///   - selection: The chosen value.
    ///   - valueText: The text of the chosen option, shown in the field. `nil` shows `prompt`.
    ///   - prompt: Text shown while nothing is chosen.
    ///   - helper: Supporting text under the field.
    ///   - error: An error message. Replaces the helper text while present, and is announced.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - labelVisibility: `.hidden` draws no label, for a row. The label is still spoken.
    ///   - labels: Replacements for the spoken "Required", "Read only" and the form of an error.
    ///   - style: `.field` for a form, `.compact` for a toolbar, filter or table-row picker.
    ///   - content: The options, as in a `Picker`: views with a `tag(_:)`.
    public init(
        _ label: String,
        selection: Binding<Value>,
        valueText: String?,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        isRequired: Bool = false,
        labelVisibility: Visibility = .visible,
        labels: MonarchFormFieldLabels = MonarchFormFieldLabels(),
        style: MonarchSelectStyle = .field,
        @ViewBuilder content: () -> Content
    ) {
        self.label = label
        _selection = selection
        let hasValue = !(valueText ?? "").isEmpty
        self.hasValue = hasValue
        self.shownText = hasValue ? (valueText ?? "") : (prompt ?? "")
        self.helper = helper
        self.error = error
        self.isRequired = isRequired
        self.labelVisibility = labelVisibility
        self.labels = labels
        self.style = style
        self.content = content()
    }

    private var activeError: String? { MonarchFieldChrome.activeError(error) }

    /// The content of the view.
    public var body: some View {
        switch style {
        case .field:
            VStack(alignment: .leading, spacing: MonarchSelectFieldTokens.stackSpacing) {
                if labelVisibility != .hidden {
                    MonarchFieldLabelRow(label: label, isRequired: isRequired)
                }
                menu
                MonarchFieldSupportingText(helper: helper, error: activeError)
            }
            .monarchAnnouncesError(activeError, labels: labels)
        case .compact:
            menu
        }
    }

    private var menu: some View {
        Menu {
            Picker(label, selection: $selection) { content }
                .pickerStyle(.inline)
                .labelsHidden()
        } label: {
            if style == .compact { compactFace } else { face }
        }
        .buttonStyle(.plain)
        .sensoryFeedback(.selection, trigger: selection)
        .accessibilityLabel(label)
        .accessibilityValue(shownText)
        .accessibilityHint(
            MonarchFieldChrome.accessibilityHint(
                helper: helper,
                error: activeError,
                isRequired: isRequired,
                labels: labels
            )
        )
    }

    /// The compact trigger: a hairline box as high as a button (36 points)
    /// and as wide as its text, inside a 44 point touch area.
    private var compactFace: some View {
        let shape = MonarchShapes.control
        return HStack(spacing: MonarchSelectFieldTokens.compactSpacing) {
            Text(shownText.isEmpty ? " " : shownText)
                .monarchFieldText()
                .foregroundStyle(
                    theme.colors[role: MonarchSelectFieldTokens.textColor(hasValue: hasValue, isEnabled: isEnabled)]
                )
                .lineLimit(MonarchSelectFieldTokens.lineLimit(isAccessibilitySize: dynamicTypeSize.isAccessibilitySize))
                .truncationMode(.tail)
            MonarchIcon(systemName: MonarchSelectFieldTokens.chevronSymbol, size: .sm)
                .imageScale(.small)
                .foregroundStyle(theme.colors[role: MonarchSelectFieldTokens.chevronColor(isEnabled: isEnabled)])
        }
        .padding(.horizontal, MonarchSelectFieldTokens.compactHorizontalPadding)
        .frame(minHeight: MonarchSelectFieldTokens.compactHeight)
        .background(
            shape.fill(theme.colors[role: MonarchFieldChrome.background(isEnabled: isEnabled, isReadOnly: false)])
        )
        .overlay(
            shape.strokeBorder(
                theme.colors[role: MonarchFieldChrome.border(hasError: false, isEnabled: isEnabled, isReadOnly: false)],
                lineWidth: MonarchTextFieldTokens.borderWidth
            )
        )
        .frame(minHeight: MonarchAccessibility.minimumHitTarget)
        .contentShape(Rectangle())
    }

    private var face: some View {
        HStack(spacing: MonarchSelectFieldTokens.chevronSpacing) {
            Text(shownText.isEmpty ? " " : shownText)
                .monarchFieldText()
                .foregroundStyle(
                    theme.colors[role: MonarchSelectFieldTokens.textColor(hasValue: hasValue, isEnabled: isEnabled)]
                )
                .lineLimit(MonarchSelectFieldTokens.lineLimit(isAccessibilitySize: dynamicTypeSize.isAccessibilitySize))
                .truncationMode(.tail)
                .frame(maxWidth: .infinity, alignment: .leading)
            MonarchIcon(systemName: MonarchSelectFieldTokens.chevronSymbol, size: .sm)
                .foregroundStyle(theme.colors[role: MonarchSelectFieldTokens.chevronColor(isEnabled: isEnabled)])
        }
        .monarchFieldBox(hasError: activeError != nil, isFocused: false)
        .contentShape(Rectangle())
    }
}

extension MonarchSelectField where Content == MonarchSelectOptions<Value> {
    /// A select with its options given as values.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised. Always required: it is the spoken name.
    ///   - selection: The chosen value.
    ///   - options: The options. The field shows the title of the chosen one.
    ///   - prompt: Text shown while no option is chosen.
    ///   - helper: Supporting text under the field.
    ///   - error: An error message. Replaces the helper text while present, and is announced.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - labelVisibility: `.hidden` draws no label, for a row. The label is still spoken.
    ///   - labels: Replacements for the spoken "Required", "Read only" and the form of an error.
    ///   - style: `.field` for a form, `.compact` for a toolbar, filter or table-row picker.
    public init(
        _ label: String,
        selection: Binding<Value>,
        options: [MonarchSelectOption<Value>],
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        isRequired: Bool = false,
        labelVisibility: Visibility = .visible,
        labels: MonarchFormFieldLabels = MonarchFormFieldLabels(),
        style: MonarchSelectStyle = .field
    ) {
        let shown = MonarchSelectFieldTokens.shownText(
            selection: selection.wrappedValue,
            options: options,
            prompt: prompt
        )
        self.init(
            label,
            selection: selection,
            valueText: shown.hasValue ? shown.text : nil,
            prompt: prompt,
            helper: helper,
            error: error,
            isRequired: isRequired,
            labelVisibility: labelVisibility,
            labels: labels,
            style: style
        ) {
            MonarchSelectOptions(options: options)
        }
    }
}

/// The option rows of a ``MonarchSelectField`` made from values.
public struct MonarchSelectOptions<Value: Hashable>: View {
    let options: [MonarchSelectOption<Value>]

    /// The content of the view.
    public var body: some View {
        ForEach(options) { option in
            if let image = option.systemImage {
                Label(option.title, systemImage: image).tag(option.value).disabled(option.isDisabled)
            } else {
                Text(option.title).tag(option.value).disabled(option.isDisabled)
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchSelectField") {
        let options = [
            MonarchSelectOption("Open", value: "open"),
            MonarchSelectOption("In progress", value: "progress"),
            MonarchSelectOption("Closed", value: "closed"),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchSelectField("Status", selection: .constant("open"), options: options)
                MonarchSelectField(
                    "Status",
                    selection: .constant(""),
                    options: options,
                    prompt: "Choose a status",
                    helper: "Shown to the customer."
                )
                MonarchSelectField(
                    "Status",
                    selection: .constant(""),
                    options: options,
                    prompt: "Choose a status",
                    error: "Choose a status.",
                    isRequired: true
                )
                MonarchSelectField("Status", selection: .constant("closed"), options: options).disabled(true)
            }
        }
    }
#endif
