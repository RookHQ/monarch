import SwiftUI

/// A masked secret, such as an API key, a bot token or a signing secret,
/// with a toggle that reveals it and an optional copy button.
///
/// ```swift
/// MonarchSecretField("API key", text: $apiKey, isCopyable: true)
/// MonarchSecretField("Bot token", text: $newToken, isSet: true)
/// ```
///
/// It is a standard `SecureField` that is swapped for a `TextField` while
/// the secret is revealed. The secret is masked until the user asks, and
/// copying does not need it revealed. With `isSet` the field shows that a
/// value is saved on the server but not sent to the device: it stays empty,
/// says so, and offers nothing to reveal or copy until the user types a
/// replacement. With `isReadOnly` it shows a secret the app holds, such as a
/// key shown once after it was created.
///
/// The value is marked with `privacySensitive()`, so it is redacted where
/// the system hides private data. A copied secret is put on the pasteboard
/// as local only, so it is not sent to the user's other devices.
///
/// Accessibility: the field is one element whose label is `label` and whose
/// hint is the error (spoken as "Error: …") or else the helper text, after
/// the "saved" description when `isSet`. A new error is announced. The
/// reveal control is a standard toggle named "Show <label>"; the copy button
/// is named "Copy <label>" and announces the copy. Both have a 44-point touch
/// target and are disabled while there is nothing to reveal or copy.
public struct MonarchSecretField: View {
    private let label: String
    @Binding private var text: String
    private let prompt: String?
    private let helper: String?
    private let error: String?
    private let isSet: Bool
    private let isRevealable: Bool
    private let isCopyable: Bool
    private let isReadOnly: Bool
    private let externalIsRevealed: Binding<Bool>?
    private let labels: MonarchSecretFieldLabels
    private let onCopy: (@MainActor (String) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @FocusState private var focus: Field?
    @State private var internalIsRevealed = false
    @State private var copyCount = 0
    @State private var isCopied = false

    private enum Field: Hashable {
        case secure
        case plain
    }

    /// A secret field.
    ///
    /// - Parameters:
    ///   - label: What the secret is, such as "API key". Already localised.
    ///     It is shown above the field and names the reveal and copy buttons.
    ///   - text: The secret. Bind a constant for a read-only secret.
    ///   - prompt: Placeholder text shown while the field is empty.
    ///   - helper: Supporting text under the field.
    ///   - error: An error message. Replaces the helper text while present.
    ///   - isSet: Whether a value is saved on the server but not shown.
    ///     The field stays empty and says so; leaving it empty keeps the
    ///     saved value, typing one replaces it. The app decides what to send.
    ///   - isRevealable: Whether the reveal toggle is shown.
    ///   - isCopyable: Whether a copy button is shown. It copies the current
    ///     text without revealing it.
    ///   - isReadOnly: Whether the secret is shown but cannot be edited.
    ///   - isRevealed: Whether the secret is shown as text. Without it the
    ///     component keeps the state itself, starting masked.
    ///   - labels: Replacements for the built-in strings.
    ///   - onCopy: Runs with the copied text after each copy.
    public init(
        _ label: String,
        text: Binding<String>,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        isSet: Bool = false,
        isRevealable: Bool = true,
        isCopyable: Bool = false,
        isReadOnly: Bool = false,
        isRevealed: Binding<Bool>? = nil,
        labels: MonarchSecretFieldLabels = MonarchSecretFieldLabels(),
        onCopy: (@MainActor (String) -> Void)? = nil
    ) {
        self.label = label
        _text = text
        self.prompt = prompt
        self.helper = helper
        self.error = error
        self.isSet = isSet
        self.isRevealable = isRevealable
        self.isCopyable = isCopyable
        self.isReadOnly = isReadOnly
        self.externalIsRevealed = isRevealed
        self.labels = labels
        self.onCopy = onCopy
    }

    private var activeError: String? {
        guard let error, !error.isEmpty else { return nil }
        return error
    }

    private var isRevealed: Binding<Bool> {
        externalIsRevealed ?? $internalIsRevealed
    }

    private var hasPlainText: Bool {
        MonarchSecretFieldTokens.hasPlainText(text)
    }

    private var hint: String {
        MonarchSecretFieldTokens.accessibilityHint(
            helper: helper,
            error: activeError,
            isSet: isSet,
            text: text,
            labels: labels
        )
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchSecretFieldTokens.stackSpacing) {
            Text(label)
                .monarchTextStyle(.label)
                .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                .accessibilityHidden(true)
            field
            supportingText
        }
        .onChange(of: isRevealed.wrappedValue) { _, nowRevealed in
            // The other field replaces the focused one, so focus moves with it.
            if focus != nil { focus = nowRevealed ? .plain : .secure }
        }
        .onChange(of: activeError) { _, newError in
            if let newError {
                AccessibilityNotification.Announcement(labels.errorText(newError)).post()
            }
        }
        .sensoryFeedback(.success, trigger: copyCount)
        .task(id: copyCount) { await resetCopiedWhenDue() }
    }

    private var field: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchSecretFieldTokens.cornerRadius, style: .continuous)
        let border = MonarchSecretFieldTokens.border(hasError: activeError != nil, isEnabled: isEnabled)
        let background = MonarchSecretFieldTokens.background(isEnabled: isEnabled, isReadOnly: isReadOnly)
        return HStack(spacing: 0) {
            Group {
                if isReadOnly {
                    readOnlyValue
                } else {
                    input
                }
            }
            .privacySensitive()
            .padding(.leading, MonarchSecretFieldTokens.leadingPadding)
            .padding(.vertical, MonarchSecretFieldTokens.verticalPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
            if isRevealable {
                revealToggle
            }
            if isCopyable {
                copyButton
            }
        }
        .frame(minHeight: MonarchSecretFieldTokens.minHeight)
        .background(shape.fill(theme.colors[role: background]))
        .overlay(
            shape.strokeBorder(theme.colors[role: border], lineWidth: MonarchSecretFieldTokens.borderWidth)
        )
        .monarchFocusRing(focus != nil, in: shape)
    }

    private var input: some View {
        let promptString = MonarchSecretFieldTokens.prompt(
            prompt,
            isSet: isSet,
            text: text,
            setPrompt: labels.resolvedSetPrompt
        )
        let promptText = promptString.map { Text($0).foregroundStyle(theme.colors.textSubtle) }
        return Group {
            if isRevealed.wrappedValue {
                TextField(label, text: $text, prompt: promptText)
                    .focused($focus, equals: .plain)
            } else {
                SecureField(label, text: $text, prompt: promptText)
                    .focused($focus, equals: .secure)
            }
        }
        .textFieldStyle(.plain)
        .monarchTextStyle(.code)
        .foregroundStyle(theme.colors[role: MonarchSecretFieldTokens.foreground(isEnabled: isEnabled)])
        .autocorrectionDisabled()
        #if os(iOS)
            .textInputAutocapitalization(.never)
        #endif
        .accessibilityLabel(label)
        .accessibilityHint(hint)
        .contentShape(Rectangle())
        .onTapGesture { focus = isRevealed.wrappedValue ? .plain : .secure }
    }

    @ViewBuilder private var readOnlyValue: some View {
        let shown = MonarchSecretFieldTokens.readOnlyText(text, isRevealed: isRevealed.wrappedValue)
        let value = Text(shown)
            .monarchTextStyle(.code)
            .foregroundStyle(theme.colors[role: MonarchSecretFieldTokens.foreground(isEnabled: isEnabled)])
            .lineLimit(1)
            .truncationMode(.middle)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(label)
            .accessibilityValue(
                MonarchSecretFieldTokens.readOnlyAccessibilityValue(
                    text,
                    isRevealed: isRevealed.wrappedValue,
                    hidden: labels.resolvedHidden
                )
            )
            .accessibilityHint(hint)
            .accessibilityAddTraits(.isStaticText)
        if isRevealed.wrappedValue {
            value.textSelection(.enabled)
        } else {
            value
        }
    }

    /// Drawn as an icon button; VoiceOver is given a standard `Toggle`.
    private var revealToggle: some View {
        let name = labels.showName(label)
        return Button {
            isRevealed.wrappedValue.toggle()
        } label: {
            MonarchIcon(systemName: MonarchSecretFieldTokens.revealSymbol(isRevealed: isRevealed.wrappedValue))
                .foregroundStyle(
                    theme.colors[
                        role: MonarchSecretFieldTokens.actionForeground(isEnabled: isEnabled, canAct: hasPlainText)
                    ]
                )
                .monarchMinimumHitTarget()
        }
        .buttonStyle(.plain)
        .disabled(!hasPlainText)
        .accessibilityRepresentation {
            Toggle(name, isOn: isRevealed)
                .disabled(!hasPlainText)
        }
    }

    private var copyButton: some View {
        Button {
            copy()
        } label: {
            MonarchIcon(systemName: MonarchSecretFieldTokens.copySymbol(isCopied: isCopied))
                .foregroundStyle(
                    theme.colors[
                        role: MonarchSecretFieldTokens.actionForeground(isEnabled: isEnabled, canAct: hasPlainText)
                    ]
                )
                .monarchMinimumHitTarget()
        }
        .buttonStyle(.plain)
        .disabled(!hasPlainText)
        .accessibilityLabel(labels.copyName(label, isCopied: isCopied))
    }

    @ViewBuilder private var supportingText: some View {
        if let activeError {
            HStack(alignment: .firstTextBaseline, spacing: MonarchSecretFieldTokens.messageIconSpacing) {
                MonarchIcon(systemName: MonarchSecretFieldTokens.errorSymbol, size: .sm)
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

    private func copy() {
        let value = text
        guard MonarchSecretFieldTokens.hasPlainText(value) else { return }
        MonarchSecretFieldPasteboard.write(value)
        isCopied = true
        copyCount += 1
        AccessibilityNotification.Announcement(labels.resolvedCopiedAnnouncement).post()
        onCopy?(value)
    }

    /// Returns the copy button to its idle look. A newer copy cancels this
    /// task and starts another, so only the last copy ends the state.
    private func resetCopiedWhenDue() async {
        guard MonarchSecretFieldTokens.resets(copyCount: copyCount) else { return }
        try? await Task.sleep(for: MonarchSecretFieldTokens.copiedDuration)
        if !Task.isCancelled {
            isCopied = false
        }
    }
}

#if DEBUG
    #Preview("MonarchSecretField") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchSecretField("API key", text: .constant("sk_live_51HxQ2"), isCopyable: true)
                MonarchSecretField(
                    "API key",
                    text: .constant("sk_live_51HxQ2"),
                    isCopyable: true,
                    isRevealed: .constant(true)
                )
                MonarchSecretField("Bot token", text: .constant(""), helper: "Stored encrypted.", isSet: true)
                MonarchSecretField(
                    "Signing secret",
                    text: .constant("whsec_9d2f7a61c0b84e3f"),
                    isCopyable: true,
                    isReadOnly: true
                )
                MonarchSecretField("Token", text: .constant("abc"), error: "This token has expired.")
                MonarchSecretField("Disabled", text: .constant("secret"), isCopyable: true).disabled(true)
            }
        }
    }
#endif
