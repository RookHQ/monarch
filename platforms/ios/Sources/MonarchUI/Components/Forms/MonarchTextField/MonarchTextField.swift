import SwiftUI

/// A labelled text field with helper text, an error state and the options a
/// form needs: required, read-only, multiline, accessories and focus.
///
/// It lays out a visible label, a standard `TextField` (or `SecureField`)
/// and one line of supporting text. Keyboard, content type, capitalisation,
/// autocorrection and submit label are the usual SwiftUI modifiers on the
/// component; they reach the field inside:
///
/// ```swift
/// MonarchTextField("Email", text: $email, prompt: "name@example.com", isRequired: true)
///     .textContentType(.emailAddress)
///     .keyboardType(.emailAddress)
///     .submitLabel(.next)
///
/// MonarchTextField("Notes", text: $notes, axis: .vertical, lineLimit: 3...8)
///
/// MonarchTextField("Amount", text: $amount) {
///     Text("$")
/// } trailing: {
///     MonarchButton("Clear", systemImage: "xmark.circle.fill", variant: .ghost, isIconOnly: true) { amount = "" }
/// }
/// ```
///
/// - `axis: .vertical` grows the field with its text, within `lineLimit`.
/// - `isReadOnly` shows the text in the field box, selectable but not
///   editable, for values the person may copy but not change.
/// - `labelVisibility: .hidden` draws no label, for a row of a `Form` whose
///   own label names the field. The label is still the spoken name.
/// - `focus` binds the focus to the app's `@FocusState`, to move focus
///   from field to field or to focus a field when a screen opens.
///
/// Accessibility: the field is one element whose label is `label`, whose
/// value is the text, and whose hint is the error (spoken as "Error: …"),
/// the helper text, "Read only" and "Required", each when present. When
/// `error` changes to a new message it is announced. The error is shown
/// with an icon and text and the required mark is backed by the spoken
/// "Required", so neither depends on colour. Accessories are separate
/// elements. The touch target is at least 44 points high.
public struct MonarchTextField: View {
    private let label: String
    @Binding private var text: String
    private let prompt: String?
    private let helper: String?
    private let error: String?
    private let isSecure: Bool
    private let isRequired: Bool
    private let optionalText: String?
    private let isReadOnly: Bool
    private let axis: Axis
    private let lineLimit: ClosedRange<Int>?
    private let labelVisibility: Visibility
    private let focus: FocusState<Bool>.Binding?
    private let labels: MonarchFormFieldLabels
    private let leading: AnyView?
    private let trailing: AnyView?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.monarchHelperAbove) private var helperAbove
    @FocusState private var ownFocus: Bool

    /// A text field.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised. Always required: it is the spoken name.
    ///   - text: The text to edit.
    ///   - prompt: Placeholder text shown while the field is empty.
    ///   - helper: Supporting text under the field.
    ///   - error: An error message. Replaces the helper text while present, and is announced.
    ///   - isSecure: Whether to hide the text, for passwords. Single line only.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - isReadOnly: Shows the text, selectable, without letting it be edited.
    ///   - axis: `.vertical` for a multiline field that grows with its text.
    ///   - lineLimit: The fewest and most lines of a multiline field.
    ///   - labelVisibility: `.hidden` draws no label, for a `Form` row. The label is still spoken.
    ///   - focus: The app's focus binding for the field.
    ///   - labels: Replacements for the spoken "Required", "Read only" and the form of an error.
    public init(
        _ label: String,
        text: Binding<String>,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        isSecure: Bool = false,
        isRequired: Bool = false,
        isReadOnly: Bool = false,
        axis: Axis = .horizontal,
        lineLimit: ClosedRange<Int>? = nil,
        labelVisibility: Visibility = .visible,
        focus: FocusState<Bool>.Binding? = nil,
        labels: MonarchFormFieldLabels = MonarchFormFieldLabels()
    ) {
        self.init(
            label,
            text: text,
            prompt: prompt,
            helper: helper,
            error: error,
            isSecure: isSecure,
            isRequired: isRequired,
            isReadOnly: isReadOnly,
            axis: axis,
            lineLimit: lineLimit,
            labelVisibility: labelVisibility,
            focus: focus,
            labels: labels,
            leading: nil,
            trailing: nil
        )
    }

    /// A text field with views before and after the text inside the field
    /// box, such as a currency symbol, a search icon or a clear button.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised. Always required: it is the spoken name.
    ///   - text: The text to edit.
    ///   - prompt: Placeholder text shown while the field is empty.
    ///   - helper: Supporting text under the field.
    ///   - error: An error message. Replaces the helper text while present, and is announced.
    ///   - isSecure: Whether to hide the text, for passwords. Single line only.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - isReadOnly: Shows the text, selectable, without letting it be edited.
    ///   - axis: `.vertical` for a multiline field that grows with its text.
    ///   - lineLimit: The fewest and most lines of a multiline field.
    ///   - labelVisibility: `.hidden` draws no label, for a `Form` row. The label is still spoken.
    ///   - focus: The app's focus binding for the field.
    ///   - labels: Replacements for the spoken "Required", "Read only" and the form of an error.
    ///   - leading: A view before the text. Decorative text and icons are hidden from VoiceOver by the caller.
    ///   - trailing: A view after the text, such as a button. Give buttons an accessibility label.
    public init<Leading: View, Trailing: View>(
        _ label: String,
        text: Binding<String>,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        isSecure: Bool = false,
        isRequired: Bool = false,
        isReadOnly: Bool = false,
        axis: Axis = .horizontal,
        lineLimit: ClosedRange<Int>? = nil,
        labelVisibility: Visibility = .visible,
        focus: FocusState<Bool>.Binding? = nil,
        labels: MonarchFormFieldLabels = MonarchFormFieldLabels(),
        @ViewBuilder leading: () -> Leading,
        @ViewBuilder trailing: () -> Trailing
    ) {
        self.init(
            label,
            text: text,
            prompt: prompt,
            helper: helper,
            error: error,
            isSecure: isSecure,
            isRequired: isRequired,
            isReadOnly: isReadOnly,
            axis: axis,
            lineLimit: lineLimit,
            labelVisibility: labelVisibility,
            focus: focus,
            labels: labels,
            leading: AnyView(leading()),
            trailing: AnyView(trailing())
        )
    }

    /// A text field with "Optional" (or any text) after its label, as the web field shows it.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised. Always required: it is the spoken name.
    ///   - text: The text to edit.
    ///   - prompt: Placeholder text shown while the field is empty.
    ///   - helper: Supporting text under the field.
    ///   - error: An error message. Replaces the helper text while present, and is announced.
    ///   - isSecure: Whether to hide the text, for passwords. Single line only.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - optionalText: Text after the label of an optional field, such as "Optional". Not shown when required.
    ///   - isReadOnly: Shows the text, selectable, without letting it be edited.
    ///   - axis: `.vertical` for a multiline field that grows with its text.
    ///   - lineLimit: The fewest and most lines of a multiline field. A multiline field is at least as tall as the fewest.
    ///   - labelVisibility: `.hidden` draws no label, for a `Form` row. The label is still spoken.
    ///   - focus: The app's focus binding for the field.
    ///   - labels: Replacements for the spoken "Required", "Read only" and the form of an error.
    public init(
        _ label: String,
        text: Binding<String>,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        isSecure: Bool = false,
        isRequired: Bool = false,
        optionalText: String?,
        isReadOnly: Bool = false,
        axis: Axis = .horizontal,
        lineLimit: ClosedRange<Int>? = nil,
        labelVisibility: Visibility = .visible,
        focus: FocusState<Bool>.Binding? = nil,
        labels: MonarchFormFieldLabels = MonarchFormFieldLabels()
    ) {
        self.init(
            label,
            text: text,
            prompt: prompt,
            helper: helper,
            error: error,
            isSecure: isSecure,
            isRequired: isRequired,
            optionalText: optionalText,
            isReadOnly: isReadOnly,
            axis: axis,
            lineLimit: lineLimit,
            labelVisibility: labelVisibility,
            focus: focus,
            labels: labels,
            leading: nil,
            trailing: nil
        )
    }

    private init(
        _ label: String,
        text: Binding<String>,
        prompt: String?,
        helper: String?,
        error: String?,
        isSecure: Bool,
        isRequired: Bool,
        optionalText: String? = nil,
        isReadOnly: Bool,
        axis: Axis,
        lineLimit: ClosedRange<Int>?,
        labelVisibility: Visibility,
        focus: FocusState<Bool>.Binding?,
        labels: MonarchFormFieldLabels,
        leading: AnyView?,
        trailing: AnyView?
    ) {
        self.label = label
        _text = text
        self.prompt = prompt
        self.helper = helper
        self.error = error
        self.isSecure = isSecure
        self.isRequired = isRequired
        self.optionalText = optionalText
        self.isReadOnly = isReadOnly
        self.axis = axis
        self.lineLimit = lineLimit
        self.labelVisibility = labelVisibility
        self.focus = focus
        self.labels = labels
        self.leading = leading
        self.trailing = trailing
    }

    private var activeError: String? { MonarchFieldChrome.activeError(error) }

    private var focusBinding: FocusState<Bool>.Binding { focus ?? $ownFocus }

    private var isFocused: Bool { focusBinding.wrappedValue }

    private var accessoryColor: MonarchColorRole { MonarchTextFieldTokens.accessoryColor(isEnabled: isEnabled) }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchTextFieldTokens.stackSpacing) {
            if MonarchTextFieldTokens.showsLabel(labelVisibility) {
                MonarchFieldLabelRow(label: label, isRequired: isRequired, optionalText: optionalText)
            }
            if helperAbove, activeError == nil, let helper, !helper.isEmpty {
                MonarchFieldHelperText(text: helper)
            }
            field
            if !helperAbove || activeError != nil {
                MonarchFieldSupportingText(helper: helper, error: activeError)
            }
        }
        .monarchAnnouncesError(activeError, labels: labels)
    }

    private var field: some View {
        HStack(
            alignment: axis == .vertical ? .firstTextBaseline : .center,
            spacing: MonarchTextFieldTokens.accessorySpacing
        ) {
            if let leading {
                leading.foregroundStyle(theme.colors[role: accessoryColor])
            }
            input
                .monarchFieldText()
                .foregroundStyle(
                    theme.colors[role: MonarchTextFieldTokens.textColor(isEnabled: isEnabled)]
                )
                .accessibilityLabel(label)
                .accessibilityHint(
                    MonarchFieldChrome.accessibilityHint(
                        helper: helper,
                        error: activeError,
                        isRequired: isRequired,
                        isReadOnly: isReadOnly,
                        labels: labels
                    )
                )
                .frame(maxWidth: .infinity, alignment: .leading)
            if let trailing {
                trailing.foregroundStyle(theme.colors[role: accessoryColor])
            }
        }
        .monarchFieldBox(hasError: activeError != nil, isFocused: isFocused && !isReadOnly, isReadOnly: isReadOnly)
        .contentShape(Rectangle())
        .onTapGesture { if !isReadOnly { focusBinding.wrappedValue = true } }
    }

    @ViewBuilder private var input: some View {
        let promptText = prompt.map { Text($0).foregroundStyle(theme.colors.textSubtle) }
        if isReadOnly {
            Text(text.isEmpty ? (prompt ?? "") : text)
                .foregroundStyle(theme.colors[role: MonarchTextFieldTokens.readOnlyColor(isEmpty: text.isEmpty)])
                .lineLimit(axis == .vertical ? lineLimit?.upperBound : 1)
                .textSelection(.enabled)
                .accessibilityValue(text)
        } else if isSecure {
            SecureField(label, text: $text, prompt: promptText ?? Text(verbatim: ""))
                .textFieldStyle(.plain)
                .focused(focusBinding)
        } else {
            TextField(label, text: $text, prompt: promptText ?? Text(verbatim: ""), axis: axis)
                .textFieldStyle(.plain)
                .monarchFieldLineLimit(MonarchTextFieldTokens.lineLimit(axis: axis, range: lineLimit))
                .focused(focusBinding)
        }
    }
}

extension View {
    /// Applies a field's line limit: `nil` leaves a multiline field unbounded.
    @ViewBuilder
    fileprivate func monarchFieldLineLimit(_ range: ClosedRange<Int>?) -> some View {
        if let range {
            lineLimit(range)
        } else {
            self
        }
    }
}

#if DEBUG
    #Preview("MonarchTextField") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchTextField("Email", text: .constant(""), prompt: "name@example.com", helper: "Work address")
                MonarchTextField("Password", text: .constant("secret"), error: "Too short", isSecure: true)
                MonarchTextField("Name", text: .constant("Ana"), isRequired: true)
                MonarchTextField("Notes", text: .constant("One\nTwo\nThree"), axis: .vertical, lineLimit: 2...4)
                MonarchTextField("Account ID", text: .constant("acct_1042"), isReadOnly: true)
                MonarchTextField("Amount", text: .constant("19.99")) {
                    Text(verbatim: "$").accessibilityHidden(true)
                } trailing: {
                    EmptyView()
                }
                MonarchTextField("Disabled", text: .constant("Read only")).disabled(true)
            }
        }
    }
#endif
