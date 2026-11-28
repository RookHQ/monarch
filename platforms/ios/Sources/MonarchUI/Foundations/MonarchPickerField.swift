import SwiftUI

/// A bordered field that shows a chosen date, time or range as text, with an icon at each end, as the web's date and
/// time inputs do. It draws only the field: the choosing is done by the `control` laid invisibly over the text (a
/// system date picker, which opens its own popover) or by the `onPress` action (for example to open a sheet).
///
/// ```swift
/// MonarchPickerField("10/11/2026") {
///     MonarchIcon(systemName: "clock")
/// } control: {
///     DatePicker("Interview", selection: $start, displayedComponents: .date)
/// } trailing: {
///     MonarchIcon(systemName: "calendar")
/// }
/// ```
///
/// Accessibility: the text and icons are hidden from VoiceOver, which reads the `control`; with `onPress` the field
/// is a button named by `label` and read with its value.
public struct MonarchPickerField<Leading: View, Control: View, Trailing: View>: View {
    private let text: String
    private let placeholder: String?
    private let label: String?
    private let onPress: (@MainActor () -> Void)?
    private let leading: Leading
    private let control: Control
    private let trailing: Trailing

    @Environment(\.monarchTheme) private var theme

    /// A picker field.
    ///
    /// - Parameters:
    ///   - text: The chosen value as text. Empty shows `placeholder`.
    ///   - placeholder: What shows while there is no value.
    ///   - label: The accessible name of the field when `onPress` is given.
    ///   - onPress: Makes the whole field a button that runs this action.
    ///   - leading: A view before the text, such as a clock.
    ///   - control: The control that does the choosing, drawn invisibly over the text.
    ///   - trailing: A view after the text, such as a clear button and a calendar icon.
    public init(
        _ text: String,
        placeholder: String? = nil,
        label: String? = nil,
        onPress: (@MainActor () -> Void)? = nil,
        @ViewBuilder leading: () -> Leading,
        @ViewBuilder control: () -> Control,
        @ViewBuilder trailing: () -> Trailing
    ) {
        self.text = text
        self.placeholder = placeholder
        self.label = label
        self.onPress = onPress
        self.leading = leading()
        self.control = control()
        self.trailing = trailing()
    }

    /// The content of the view.
    public var body: some View {
        if let onPress {
            Button(action: onPress) { field }
                .buttonStyle(.plain)
                .accessibilityLabel(label ?? text)
                .accessibilityValue(text)
        } else {
            field
        }
    }

    private var field: some View {
        HStack(spacing: theme.spacing.space2) {
            leading
                .foregroundStyle(theme.colors[role: \.textMuted])
                .accessibilityHidden(true)
            Text(text.isEmpty ? placeholder ?? "" : text)
                .monarchFieldText()
                .lineLimit(1)
                .minimumScaleFactor(0.75)
                .foregroundStyle(theme.colors[role: text.isEmpty ? \.textSubtle : \.textDefault])
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityHidden(onPress == nil)
                .overlay(alignment: .leading) {
                    control
                        .labelsHidden()
                        .opacity(0.003)
                }
            trailing
                .foregroundStyle(theme.colors[role: \.textMuted])
        }
        .monarchFieldBox(hasError: false, isFocused: false)
    }
}

extension MonarchPickerField where Leading == EmptyView, Control == EmptyView {
    /// A picker field that opens something when pressed, with an icon after the text.
    public init(
        _ text: String,
        placeholder: String? = nil,
        label: String,
        onPress: @escaping @MainActor () -> Void,
        @ViewBuilder trailing: () -> Trailing
    ) {
        self.init(
            text,
            placeholder: placeholder,
            label: label,
            onPress: onPress,
            leading: { EmptyView() },
            control: { EmptyView() },
            trailing: trailing
        )
    }
}
