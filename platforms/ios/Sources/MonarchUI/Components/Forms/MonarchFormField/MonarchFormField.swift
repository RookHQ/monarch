import SwiftUI

/// A label, one control, help text and an error message, wired together for
/// VoiceOver.
///
/// Use it for controls that have no label of their own: a `Picker`, a
/// `Toggle` with a description, a `Stepper`, ``MonarchTagInput`` or a
/// custom control. ``MonarchTextField`` and ``MonarchPasswordInput`` already
/// have a label, help and error; they do not need a field around them.
///
/// ```swift
/// MonarchFormField("Time zone", helper: "Used for reminders.", isRequired: true) {
///     Picker("Time zone", selection: $zone) {
///         ForEach(zones, id: \.self) { Text($0) }
///     }
/// }
///
/// MonarchFormField("Weekly digest", helper: "Sent on Monday mornings.", layout: .row) {
///     Toggle("Weekly digest", isOn: $digest).labelsHidden()
/// }
/// ```
///
/// Disable the whole field with `disabled(_:)`: the control follows and the
/// label takes the disabled text colour.
///
/// For several controls that give one value, use `mode: .group`: each
/// control keeps its own label and the field names the group.
///
/// ```swift
/// MonarchFormField("Starts", mode: .group) {
///     HStack {
///         DatePicker("Date", selection: $start, displayedComponents: .date).labelsHidden()
///         DatePicker("Time", selection: $start, displayedComponents: .hourAndMinute).labelsHidden()
///     }
/// }
/// ```
///
/// Accessibility: the visible label and supporting text are hidden from
/// VoiceOver and given to the control instead. The control's label is
/// `label`, and its hint is the error (read as "Error: …"), the help text and
/// "Required", each when present. A new error is announced. The error has an
/// icon and text, and the required mark is backed by the spoken "Required",
/// so neither depends on colour. Put one control in the field: the label and
/// hint are applied to it.
public struct MonarchFormField<Control: View>: View {
    private let label: String
    private let helper: String?
    private let error: String?
    private let layout: MonarchFormFieldLayout
    private let isRequired: Bool
    private let mode: MonarchFormFieldMode
    private let optionalText: String?
    private let labels: MonarchFormFieldLabels
    private let control: Control

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.monarchHelperAbove) private var helperAbove
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// A form field.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised. It also names the control.
    ///   - helper: Help text under the label or the control.
    ///   - error: An error message. Shown with an icon under the control and announced.
    ///   - layout: A stack, or a row with the control after the text.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - mode: One control that takes the label, or a group of controls with labels of their own.
    ///   - optionalText: Text after the label of an optional field, such as "Optional". Not shown when required.
    ///   - labels: Replacements for the built-in spoken strings.
    ///   - control: The control. One control without its own visible label, or a group with `mode: .group`.
    public init(
        _ label: String,
        helper: String? = nil,
        error: String? = nil,
        layout: MonarchFormFieldLayout = .stack,
        isRequired: Bool = false,
        mode: MonarchFormFieldMode = .single,
        optionalText: String? = nil,
        labels: MonarchFormFieldLabels = MonarchFormFieldLabels(),
        @ViewBuilder control: () -> Control
    ) {
        self.label = label
        self.helper = helper
        self.error = error
        self.layout = layout
        self.isRequired = isRequired
        self.mode = mode
        self.optionalText = optionalText
        self.labels = labels
        self.control = control()
    }

    private var activeError: String? { MonarchFormFieldTokens.activeError(error) }

    /// The content of the view.
    public var body: some View {
        Group {
            if MonarchFormFieldTokens.stacks(layout: layout, isAccessibilitySize: dynamicTypeSize.isAccessibilitySize) {
                VStack(alignment: .leading, spacing: MonarchFormFieldTokens.stackSpacing) {
                    labelRow
                    if helperAbove { helperText }
                    wiredControl
                    if !helperAbove { helperText }
                    if activeError != nil {
                        errorText
                    }
                }
            } else {
                VStack(alignment: .leading, spacing: MonarchFormFieldTokens.stackSpacing) {
                    HStack(alignment: .center, spacing: MonarchFormFieldTokens.rowSpacing) {
                        VStack(alignment: .leading, spacing: MonarchFormFieldTokens.textSpacing) {
                            labelRow
                            helperText
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        // The control keeps its natural width: a switch is not squeezed by the text column.
                        wiredControl
                            .fixedSize(horizontal: true, vertical: false)
                            .layoutPriority(1)
                    }
                    errorText
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .modifier(MonarchFormFieldGroup(isActive: mode == .group, label: label, hint: hint))
        .monarchAnnouncesError(activeError, labels: labels)
    }

    private var hint: String {
        MonarchFormFieldTokens.accessibilityHint(
            helper: helper,
            error: activeError,
            isRequired: isRequired,
            labels: labels
        )
    }

    @ViewBuilder private var wiredControl: some View {
        switch mode {
        case .single:
            control
                .accessibilityLabel(label)
                .accessibilityHint(hint)
        case .group:
            control
        }
    }

    private var labelRow: some View {
        MonarchFieldLabelRow(label: label, isRequired: isRequired, optionalText: optionalText)
    }

    @ViewBuilder private var helperText: some View {
        if let helper, !helper.isEmpty {
            MonarchFieldHelperText(text: helper)
        }
    }

    @ViewBuilder private var errorText: some View {
        if let activeError {
            MonarchFieldErrorRow(message: activeError)
        }
    }
}

/// Makes a group field one VoiceOver container named by the label, with the
/// hint, so each control inside keeps its own label.
private struct MonarchFormFieldGroup: ViewModifier {
    let isActive: Bool
    let label: String
    let hint: String

    func body(content: Content) -> some View {
        if isActive {
            content
                .accessibilityElement(children: .contain)
                .accessibilityLabel(label)
                .accessibilityHint(hint)
        } else {
            content
        }
    }
}

#if DEBUG
    #Preview("MonarchFormField") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchFormField("Time zone", helper: "Used for reminders.", isRequired: true) {
                    Picker("Time zone", selection: .constant("UTC")) {
                        Text("UTC").tag("UTC")
                        Text("London").tag("London")
                    }
                }
                MonarchFormField("Seats", error: "Choose at least 1 seat.", optionalText: "Optional") {
                    Stepper("Seats", value: .constant(0), in: 0...10)
                }
                MonarchFormField("Weekly digest", helper: "Sent on Monday mornings.", layout: .row) {
                    Toggle("Weekly digest", isOn: .constant(true)).labelsHidden()
                }
                MonarchFormField("Starts", helper: "In your time zone.", mode: .group) {
                    HStack {
                        Stepper("Hour", value: .constant(9), in: 0...23)
                        Stepper("Minute", value: .constant(30), in: 0...59)
                    }
                }
                MonarchFormField("Disabled", helper: "Cannot change.", layout: .row) {
                    Toggle("Disabled", isOn: .constant(false)).labelsHidden()
                }
                .disabled(true)
            }
        }
    }
#endif
