import SwiftUI

/// A consent question: one checkbox whose label is the statement, the help under it and the error under
/// that. The statement is the checkbox's name; the error, help and "Required" are its hint.
struct MonarchFieldRendererConsent: View {
    let field: MonarchFormFieldDefinition
    @Binding var value: MonarchFormFieldValue?
    let error: String?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.monarchHidesConsentMark) private var hidesMark

    private var statement: String { field.consentText ?? field.label }

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchFormFieldTokens.stackSpacing) {
            Toggle(
                isOn: Binding(get: { value == .flag(true) }, set: { value = .flag($0) })
            ) {
                HStack(alignment: .firstTextBaseline, spacing: MonarchFormFieldTokens.markSpacing) {
                    Text(statement).fixedSize(horizontal: false, vertical: true)
                    if field.isRequired && !hidesMark {
                        Text(MonarchFormFieldTokens.requiredMark)
                            .foregroundStyle(
                                theme.colors[role: MonarchFormFieldTokens.requiredColor(isEnabled: isEnabled)]
                            )
                            .accessibilityHidden(true)
                    }
                }
            }
            .toggleStyle(.monarchCheckbox)
            .accessibilityLabel(statement)
            .accessibilityHint(
                MonarchFormFieldTokens.accessibilityHint(
                    helper: field.description,
                    error: error,
                    isRequired: field.isRequired,
                    labels: MonarchFormFieldLabels()
                )
            )
            if let description = field.description, !description.isEmpty {
                MonarchFieldHelperText(text: description)
            }
            MonarchFieldRendererError(error: error)
        }
        .monarchAnnouncesError(error)
    }
}

/// A yes-or-no question: the question at the leading edge and a switch at the trailing edge, as the web draws it,
/// with the help under the question and the error under that. Off is a saved answer of no.
struct MonarchFieldRendererSwitch: View {
    let field: MonarchFormFieldDefinition
    @Binding var value: MonarchFormFieldValue?
    let error: String?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchFormFieldTokens.stackSpacing) {
            Toggle(
                isOn: Binding(get: { value == .flag(true) }, set: { value = .flag($0) })
            ) {
                VStack(alignment: .leading, spacing: MonarchFormFieldTokens.stackSpacing) {
                    HStack(alignment: .firstTextBaseline, spacing: MonarchFormFieldTokens.markSpacing) {
                        Text(field.label)
                            .monarchTextStyle(.label)
                            .fixedSize(horizontal: false, vertical: true)
                        if field.isRequired {
                            Text(MonarchFormFieldTokens.requiredMark)
                                .foregroundStyle(
                                    theme.colors[role: MonarchFormFieldTokens.requiredColor(isEnabled: isEnabled)]
                                )
                                .accessibilityHidden(true)
                        }
                    }
                    // The help sits under the label inside the row, as on the web.
                    if let description = field.description, !description.isEmpty {
                        MonarchFieldHelperText(text: description)
                    }
                }
            }
            .toggleStyle(.monarchSwitch)
            .accessibilityLabel(field.label)
            .accessibilityHint(
                MonarchFormFieldTokens.accessibilityHint(
                    helper: field.description,
                    error: error,
                    isRequired: field.isRequired,
                    labels: MonarchFormFieldLabels()
                )
            )
            MonarchFieldRendererError(error: error)
        }
        .monarchAnnouncesError(error)
    }
}

/// A rating question: ``MonarchRatingPicker``, which draws its own label and is one adjustable element,
/// with the error, help and "Required" added to its hint and the help and error drawn under it.
struct MonarchFieldRendererRating: View {
    let field: MonarchFormFieldDefinition
    @Binding var value: MonarchFormFieldValue?
    let error: String?

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchFormFieldTokens.stackSpacing) {
            MonarchRatingPicker(
                field.label,
                value: Binding(
                    get: {
                        if case .number(let number)? = value, number.isFinite { return Int(number) }
                        return nil
                    },
                    set: { value = $0.map { .number(Double($0)) } }
                ),
                in: MonarchFieldRendererTokens.ratingRange(field),
                variant: .stars,
                isClearable: !field.isRequired,
                isReadOnly: field.isReadOnly
            )
            .accessibilityHint(
                MonarchFormFieldTokens.accessibilityHint(
                    helper: field.description,
                    error: error,
                    isRequired: field.isRequired,
                    labels: MonarchFormFieldLabels()
                )
            )
            if let description = field.description, !description.isEmpty, error == nil {
                MonarchFieldHelperText(text: description)
            }
            MonarchFieldRendererError(error: error)
        }
        .monarchAnnouncesError(error)
    }
}

/// The error under a question that draws its own label: the shared error row, which is hidden from VoiceOver
/// because the control's hint carries it.
struct MonarchFieldRendererError: View {
    let error: String?

    var body: some View {
        if let activeError = MonarchFieldChrome.activeError(error) {
            MonarchFieldErrorRow(message: activeError)
        }
    }
}

private struct MonarchHidesConsentMarkKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    /// Whether a consent question leaves out the mark after its statement. A schema-driven form does, as the web
    /// checkbox shows none; the required answer is still checked.
    var monarchHidesConsentMark: Bool {
        get { self[MonarchHidesConsentMarkKey.self] }
        set { self[MonarchHidesConsentMarkKey.self] = newValue }
    }
}
