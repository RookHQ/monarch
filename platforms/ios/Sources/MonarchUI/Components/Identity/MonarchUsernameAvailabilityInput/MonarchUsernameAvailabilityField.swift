import SwiftUI

/// The drawn part of ``MonarchUsernameAvailabilityInput``: the label, the
/// field with its prefix and status icon, and the message. It holds no check
/// state, so every status can be rendered directly.
struct MonarchUsernameAvailabilityField: View {
    let label: String
    @Binding var text: String
    let prompt: String?
    let helper: String?
    let prefix: String?
    var isRequired = false
    let status: MonarchUsernameAvailabilityStatus
    let statusText: String
    let retryLabel: String
    let retry: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @FocusState private var isFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchUsernameAvailabilityInputTokens.stackSpacing) {
            MonarchFieldLabelRow(label: label, isRequired: isRequired)
            field
            supportingText
        }
    }

    private var field: some View {
        let shape = RoundedRectangle(
            cornerRadius: MonarchUsernameAvailabilityInputTokens.cornerRadius,
            style: .continuous
        )
        let border = MonarchUsernameAvailabilityInputTokens.border(status: status, isEnabled: isEnabled)
        let background = MonarchUsernameAvailabilityInputTokens.background(isEnabled: isEnabled)
        return HStack(spacing: MonarchUsernameAvailabilityInputTokens.contentSpacing) {
            if let prefix, !prefix.isEmpty {
                Text(prefix)
                    .monarchTextStyle(.body)
                    .foregroundStyle(isEnabled ? theme.colors.textMuted : theme.colors.textDisabled)
                    .accessibilityHidden(true)
            }
            TextField(label, text: $text, prompt: prompt.map { Text($0).foregroundStyle(theme.colors.textSubtle) })
                .textFieldStyle(.plain)
                .monarchTextStyle(.body)
                .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                .focused($isFocused)
                .autocorrectionDisabled()
                #if os(iOS)
                    .textInputAutocapitalization(.never)
                #endif
                .accessibilityLabel(label)
                .accessibilityHint(
                    MonarchUsernameAvailabilityInputTokens.accessibilityHint(
                        prefix: prefix,
                        helper: helper,
                        statusText: statusText
                    )
                )
            statusIcon
        }
        .padding(.horizontal, MonarchUsernameAvailabilityInputTokens.horizontalPadding)
        .padding(.vertical, MonarchUsernameAvailabilityInputTokens.verticalPadding)
        .frame(
            maxWidth: .infinity,
            minHeight: MonarchUsernameAvailabilityInputTokens.minHeight,
            alignment: .leading
        )
        .background(shape.fill(theme.colors[role: background]))
        .overlay(
            shape.strokeBorder(
                theme.colors[role: border],
                lineWidth: MonarchUsernameAvailabilityInputTokens.borderWidth
            )
        )
        .monarchFocusRing(isFocused, in: shape)
        .contentShape(Rectangle())
        .onTapGesture { isFocused = true }
    }

    @ViewBuilder private var statusIcon: some View {
        if status == .checking {
            ProgressView().controlSize(.small).accessibilityHidden(true)
        } else if let symbol = MonarchUsernameAvailabilityInputTokens.symbol(status) {
            MonarchIcon(systemName: symbol, size: .sm)
                .foregroundStyle(theme.colors[role: MonarchUsernameAvailabilityInputTokens.iconForeground(status)])
        }
    }

    @ViewBuilder private var supportingText: some View {
        if !statusText.isEmpty {
            HStack(alignment: .firstTextBaseline, spacing: MonarchUsernameAvailabilityInputTokens.messageSpacing) {
                Text(statusText)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(
                        theme.colors[role: MonarchUsernameAvailabilityInputTokens.messageForeground(status)]
                    )
                    .accessibilityHidden(true)
                if status == .error {
                    Button(retryLabel, action: retry)
                        .buttonStyle(MonarchUsernameAvailabilityRetryStyle())
                }
            }
        } else if let helper, !helper.isEmpty {
            Text(helper)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
                .accessibilityHidden(true)
        }
    }
}

/// The retry button: link-coloured caption text with a full touch target.
private struct MonarchUsernameAvailabilityRetryStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        MonarchUsernameAvailabilityRetryBody(configuration: configuration)
    }
}

/// Reads the environment, which a `ButtonStyle` value cannot do itself.
private struct MonarchUsernameAvailabilityRetryBody: View {
    let configuration: ButtonStyleConfiguration

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let role: MonarchColorRole =
            !isEnabled ? \.textDisabled : (configuration.isPressed ? \.textDefault : \.textLink)
        configuration.label
            .monarchTextStyle(.caption)
            .underline()
            .foregroundStyle(theme.colors[role: role])
            .monarchMinimumHitTarget()
    }
}
