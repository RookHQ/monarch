import SwiftUI

/// A password field with a toggle that shows the password as text.
///
/// It is a standard `SecureField` that is swapped for a `TextField` while the
/// password is revealed. `purpose` sets the autofill content type, so the
/// system offers a saved password or proposes a strong new one.
///
/// ```swift
/// MonarchPasswordInput("Password", text: $password, purpose: .new)
/// ```
///
/// Accessibility: the field is one element whose label is `label` and whose
/// hint is the error (spoken as "Error: …") or else the helper text. A new
/// error is announced. The reveal control is a separate toggle named "Show
/// password" in both states; its value says whether the password is shown.
/// Both have a 44-point touch target. Autocorrection and capitalisation are
/// off while the password is shown as text.
public struct MonarchPasswordInput: View {
    private let label: String
    @Binding private var text: String
    private let prompt: String?
    private let helper: String?
    private let error: String?
    private let purpose: MonarchPasswordInputPurpose
    private let externalIsRevealed: Binding<Bool>?
    private let revealLabel: String?
    private let isRequired: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @FocusState private var focus: Field?
    @State private var internalIsRevealed = false

    private enum Field: Hashable {
        case secure
        case plain
    }

    /// A password input.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - text: The password.
    ///   - prompt: Placeholder text shown while the field is empty.
    ///   - helper: Supporting text under the field.
    ///   - error: An error message. Replaces the helper text while present.
    ///   - purpose: Whether the password exists already or is being created.
    ///   - isRevealed: Whether the password is shown as text. Without it the component keeps the state itself.
    ///   - revealLabel: Replaces the built-in "Show password" spoken name of the toggle.
    public init(
        _ label: String,
        text: Binding<String>,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        purpose: MonarchPasswordInputPurpose = .current,
        isRevealed: Binding<Bool>? = nil,
        revealLabel: String? = nil
    ) {
        self.label = label
        _text = text
        self.prompt = prompt
        self.helper = helper
        self.error = error
        self.purpose = purpose
        self.externalIsRevealed = isRevealed
        self.revealLabel = revealLabel
        self.isRequired = false
    }

    /// A password input for a required field: a red mark follows the label and the hint says "Required".
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - text: The password.
    ///   - prompt: Placeholder text shown while the field is empty.
    ///   - helper: Supporting text under the field.
    ///   - error: An error message. Replaces the helper text while present.
    ///   - purpose: Whether the password exists already or is being created.
    ///   - isRequired: Shows a mark after the label.
    ///   - isRevealed: Whether the password is shown as text. Without it the component keeps the state itself.
    ///   - revealLabel: Replaces the built-in "Show password" spoken name of the toggle.
    public init(
        _ label: String,
        text: Binding<String>,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        purpose: MonarchPasswordInputPurpose = .current,
        isRequired: Bool,
        isRevealed: Binding<Bool>? = nil,
        revealLabel: String? = nil
    ) {
        self.label = label
        _text = text
        self.prompt = prompt
        self.helper = helper
        self.error = error
        self.purpose = purpose
        self.externalIsRevealed = isRevealed
        self.revealLabel = revealLabel
        self.isRequired = isRequired
    }

    private var activeError: String? {
        guard let error, !error.isEmpty else { return nil }
        return error
    }

    private var isRevealed: Binding<Bool> {
        externalIsRevealed ?? $internalIsRevealed
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchPasswordInputTokens.stackSpacing) {
            MonarchFieldLabelRow(label: label, isRequired: isRequired)
            field
            supportingText
        }
        .onChange(of: isRevealed.wrappedValue) { _, nowRevealed in
            // The other field replaces the focused one, so focus moves with it.
            if focus != nil { focus = nowRevealed ? .plain : .secure }
        }
        .onChange(of: activeError) { _, newError in
            if let newError {
                AccessibilityNotification.Announcement(MonarchPasswordInputStrings.error(newError)).post()
            }
        }
    }

    private var field: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchPasswordInputTokens.cornerRadius, style: .continuous)
        let border = MonarchPasswordInputTokens.border(hasError: activeError != nil, isEnabled: isEnabled)
        let background = MonarchPasswordInputTokens.background(isEnabled: isEnabled)
        return HStack(spacing: 0) {
            input
                .textFieldStyle(.plain)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors[role: MonarchPasswordInputTokens.foreground(isEnabled: isEnabled)])
                .autocorrectionDisabled()
                #if os(iOS)
                    .textContentType(MonarchPasswordInputTokens.contentType(purpose))
                    .textInputAutocapitalization(.never)
                #endif
                .accessibilityLabel(label)
                .accessibilityHint(MonarchPasswordInputTokens.accessibilityHint(helper: helper, error: activeError))
                .padding(.leading, MonarchPasswordInputTokens.leadingPadding)
                .padding(.vertical, MonarchPasswordInputTokens.verticalPadding)
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
                .onTapGesture { focus = isRevealed.wrappedValue ? .plain : .secure }
            revealToggle
        }
        .frame(minHeight: MonarchPasswordInputTokens.minHeight)
        .background(shape.fill(theme.colors[role: background]))
        .overlay(
            shape.strokeBorder(theme.colors[role: border], lineWidth: MonarchPasswordInputTokens.borderWidth)
        )
        .monarchFocusRing(focus != nil, in: shape)
    }

    @ViewBuilder private var input: some View {
        let promptText = prompt.map { Text($0).foregroundStyle(theme.colors.textSubtle) }
        if isRevealed.wrappedValue {
            TextField(label, text: $text, prompt: promptText ?? Text(verbatim: ""))
                .focused($focus, equals: .plain)
        } else {
            SecureField(label, text: $text, prompt: promptText ?? Text(verbatim: ""))
                .focused($focus, equals: .secure)
        }
    }

    /// Drawn as an icon button; VoiceOver is given a standard `Toggle`.
    private var revealToggle: some View {
        let name = revealLabel ?? MonarchPasswordInputStrings.show
        return Button {
            isRevealed.wrappedValue.toggle()
        } label: {
            MonarchIcon(systemName: MonarchPasswordInputTokens.revealSymbol(isRevealed: isRevealed.wrappedValue))
                .foregroundStyle(
                    theme.colors[role: MonarchPasswordInputTokens.revealForeground(isEnabled: isEnabled)]
                )
                .monarchMinimumHitTarget()
        }
        .buttonStyle(.plain)
        .accessibilityRepresentation {
            Toggle(name, isOn: isRevealed)
        }
    }

    @ViewBuilder private var supportingText: some View {
        if let activeError {
            HStack(alignment: .firstTextBaseline, spacing: MonarchPasswordInputTokens.messageIconSpacing) {
                MonarchIcon(systemName: MonarchPasswordInputTokens.errorSymbol, size: .sm)
                Text(activeError).monarchTextStyle(.caption)
            }
            .foregroundStyle(theme.colors.dangerStrong)
            .accessibilityHidden(true)
        } else if let helper, !helper.isEmpty {
            Text(helper)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
                .accessibilityHidden(true)
        }
    }
}

#if DEBUG
    #Preview("MonarchPasswordInput") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchPasswordInput("Password", text: .constant("secret"), helper: "At least 12 characters.")
                MonarchPasswordInput("Shown", text: .constant("secret"), isRevealed: .constant(true))
                MonarchPasswordInput("New password", text: .constant("abc"), error: "Too short.", purpose: .new)
                MonarchPasswordInput("Disabled", text: .constant("secret")).disabled(true)
            }
        }
    }
#endif
