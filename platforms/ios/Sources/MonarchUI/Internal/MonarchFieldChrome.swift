import MonarchTokens
import SwiftUI

// The shared parts of every Monarch form control: the label row with the
// required mark, the bordered field with its focus ring, and the help or
// error line with its announcement. ``MonarchTextField``,
// ``MonarchNumberInput``, ``MonarchFormField``, ``MonarchCombobox`` and
// ``MonarchRatingPicker`` are built from them, and so are the fields of
// other components, so every field in an app has the same chrome.
//
// The visible label and supporting text are hidden from VoiceOver: the
// control carries the label, and the hint built by
// ``MonarchFieldChrome/accessibilityHint(helper:error:isRequired:labels:)``.

/// Pure decisions of the field chrome.
enum MonarchFieldChrome {
    /// The error to show: `nil` for a missing or empty message.
    static func activeError(_ error: String?) -> String? {
        guard let error, !error.isEmpty else { return nil }
        return error
    }

    /// What VoiceOver reads after the control's label and value: the error,
    /// the help text, "Read only" and "Required", each when present.
    static func accessibilityHint(
        helper: String?,
        error: String?,
        isRequired: Bool,
        isReadOnly: Bool = false,
        labels: MonarchFormFieldLabels = MonarchFormFieldLabels()
    ) -> String {
        var parts = [
            MonarchFormFieldTokens.accessibilityHint(helper: helper, error: error, isRequired: false, labels: labels)
        ]
        if isReadOnly { parts.append(labels.readOnly ?? MonarchFormFieldStrings.readOnly) }
        if isRequired { parts.append(labels.required ?? MonarchFormFieldStrings.required) }
        return MonarchSpokenLabel.join(parts)
    }

    /// The border role of a field box.
    static func border(hasError: Bool, isEnabled: Bool, isReadOnly: Bool) -> MonarchColorRole {
        if !isEnabled || (isReadOnly && !hasError) { return \.borderSubtle }
        return hasError ? \.dangerStrong : \.borderInput
    }

    /// The fill role of a field box. A read-only field sits on the subtle
    /// background, as a disabled one does, but keeps the default text colour.
    static func background(isEnabled: Bool, isReadOnly: Bool) -> MonarchColorRole {
        isEnabled && !isReadOnly ? \.bgSurface : \.bgSubtle
    }
}

/// The label of a field with its required mark or optional text. Hidden from
/// VoiceOver, which hears the label on the control and "Required" in the hint.
struct MonarchFieldLabelRow: View {
    let label: String
    var isRequired = false
    var optionalText: String?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.monarchFieldLabelCode) private var code

    /// The label cut around the phrase that is drawn as code, with the quotes that wrap it left out.
    private var codeParts: (before: String, code: String, after: String)? {
        guard let code, !code.isEmpty else { return nil }
        for needle in ["\u{201C}\(code)\u{201D}", "\"\(code)\"", code] {
            if let range = label.range(of: needle) {
                return (String(label[..<range.lowerBound]), code, String(label[range.upperBound...]))
            }
        }
        return nil
    }

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchFormFieldTokens.markSpacing) {
            labelText
            if isRequired {
                Text(MonarchFormFieldTokens.requiredMark)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors[role: MonarchFormFieldTokens.requiredColor(isEnabled: isEnabled)])
            } else if let optionalText, !optionalText.isEmpty {
                Text(optionalText)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchFormFieldTokens.helperColor(isEnabled: isEnabled)])
            }
        }
        .accessibilityHidden(true)
    }

    @ViewBuilder private var labelText: some View {
        let color = theme.colors[role: MonarchFormFieldTokens.labelColor(isEnabled: isEnabled)]
        if let parts = codeParts {
            HStack(alignment: .firstTextBaseline, spacing: MonarchFormFieldTokens.markSpacing) {
                if !parts.before.isEmpty {
                    Text(parts.before.trimmingCharacters(in: .whitespaces)).monarchTextStyle(.label)
                }
                Text(parts.code)
                    .monarchTextStyle(.code)
                    .padding(.horizontal, MonarchTokens.Space.space1)
                    .background(
                        theme.colors.bgSubtle,
                        in: RoundedRectangle(cornerRadius: MonarchTokens.Radius.sm, style: .continuous)
                    )
                if !parts.after.isEmpty {
                    Text(parts.after.trimmingCharacters(in: .whitespaces)).monarchTextStyle(.label)
                }
            }
            .foregroundStyle(color)
        } else {
            Text(label)
                .monarchTextStyle(.label)
                .foregroundStyle(color)
        }
    }
}

private struct MonarchFieldLabelCodeKey: EnvironmentKey {
    static let defaultValue: String? = nil
}

extension EnvironmentValues {
    /// A phrase in the label of the fields inside that is drawn as inline code, as the web's type-to-confirm label does.
    var monarchFieldLabelCode: String? {
        get { self[MonarchFieldLabelCodeKey.self] }
        set { self[MonarchFieldLabelCodeKey.self] = newValue }
    }
}

/// The error line of a field: an icon and the message, in the danger colour,
/// so the error never depends on colour alone. Hidden from VoiceOver, which
/// hears the error in the hint and in the announcement, unless `isSpoken`
/// (an error that stands alone, such as a list that failed to load).
struct MonarchFieldErrorRow: View {
    let message: String
    var isSpoken = false

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchFormFieldTokens.messageIconSpacing) {
            MonarchIcon(systemName: MonarchFormFieldTokens.errorSymbol, size: .sm)
            Text(message)
                .monarchTextStyle(.caption)
                .fixedSize(horizontal: false, vertical: true)
        }
        .foregroundStyle(theme.colors[role: MonarchFormFieldTokens.errorColor])
        .accessibilityElement(children: .combine)
        .accessibilityHidden(!isSpoken)
    }
}

/// The help text of a field. Hidden from VoiceOver, which hears it in the hint.
struct MonarchFieldHelperText: View {
    let text: String

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors[role: MonarchFormFieldTokens.helperColor(isEnabled: isEnabled)])
            .fixedSize(horizontal: false, vertical: true)
            .accessibilityHidden(true)
    }
}

/// The line under a field: the error when there is one, else the help text.
struct MonarchFieldSupportingText: View {
    let helper: String?
    let error: String?

    var body: some View {
        if let error = MonarchFieldChrome.activeError(error) {
            MonarchFieldErrorRow(message: error)
        } else if let helper, !helper.isEmpty {
            MonarchFieldHelperText(text: helper)
        }
    }
}

extension View {
    /// The text of a field: 14 points, regular, as the web field. It scales
    /// with Dynamic Type.
    func monarchFieldText() -> some View {
        monarchTextStyle(.label, weight: .regular)
    }

    /// Announces a new error message, spoken as "Error: …" (or the app's
    /// override), each time the message changes to a new one.
    func monarchAnnouncesError(
        _ error: String?,
        labels: MonarchFormFieldLabels = MonarchFormFieldLabels()
    ) -> some View {
        onChange(of: MonarchFieldChrome.activeError(error)) { _, newError in
            if let newError {
                MonarchAnnouncer.post(MonarchFormFieldTokens.spokenError(newError, labels: labels))
            }
        }
    }

    /// The bordered box of a text-like field: padding, the least height of a
    /// touch target, the fill and border for the state, and the focus ring.
    func monarchFieldBox(
        hasError: Bool,
        isFocused: Bool,
        isReadOnly: Bool = false,
        minHeight: CGFloat? = nil
    ) -> some View {
        modifier(
            MonarchFieldBoxModifier(
                hasError: hasError,
                isFocused: isFocused,
                isReadOnly: isReadOnly,
                minHeight: minHeight
            )
        )
    }
}

/// Draws the field box, reading the theme and the enabled state.
private struct MonarchFieldBoxModifier: ViewModifier {
    let hasError: Bool
    let isFocused: Bool
    let isReadOnly: Bool
    let minHeight: CGFloat?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.monarchFieldSize) private var size
    @Environment(\.monarchAiTone) private var aiTone

    func body(content: Content) -> some View {
        let shape = MonarchShapes.control
        let tinted = aiTone && !hasError && isEnabled && !isReadOnly
        let border: MonarchColorRole =
            tinted
            ? \.aiBorder : MonarchFieldChrome.border(hasError: hasError, isEnabled: isEnabled, isReadOnly: isReadOnly)
        let background: MonarchColorRole =
            tinted ? \.aiSubtle : MonarchFieldChrome.background(isEnabled: isEnabled, isReadOnly: isReadOnly)
        content
            .padding(.horizontal, MonarchControlMetrics.fieldHorizontalPadding(size))
            .padding(.vertical, MonarchTextFieldTokens.verticalPadding)
            .frame(
                maxWidth: .infinity,
                minHeight: minHeight ?? MonarchControlMetrics.fieldMinHeight(size),
                alignment: .leading
            )
            .background(shape.fill(theme.colors[role: background]))
            .overlay(shape.strokeBorder(theme.colors[role: border], lineWidth: MonarchTextFieldTokens.borderWidth))
            .monarchFocusRing(isFocused, in: shape)
    }
}

private struct MonarchAiToneKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    /// Draws the field boxes inside in the AI tint and border: a value an AI filled in and the person has not yet
    /// accepted.
    var monarchAiTone: Bool {
        get { self[MonarchAiToneKey.self] }
        set { self[MonarchAiToneKey.self] = newValue }
    }
}
