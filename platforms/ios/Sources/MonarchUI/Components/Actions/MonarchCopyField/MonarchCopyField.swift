import SwiftUI

/// A read-only value with a copy button beside it: a webhook URL, a DNS
/// record, an API key prefix, an embed snippet.
///
/// ```swift
/// MonarchCopyField("Webhook URL", value: "https://example.com/hooks/42")
/// MonarchCopyField("Embed code", value: snippet, isMultiline: true)
/// ```
///
/// The value is shown in a field-shaped box and can also be selected by
/// hand (touch and hold). A single-line field keeps one line and shortens
/// the middle of a long value; a multi-line field shows the whole value.
/// The copy button is ``MonarchCopyButton``, so it confirms with "Copied",
/// the success haptic and an announcement.
///
/// Accessibility: the box is one element read as the label and then the
/// whole value, even when the visible text is shortened. The copy button is
/// a separate element named "Copy" followed by the label. The field and the
/// button scale with Dynamic Type, and the button keeps a 44-point target.
public struct MonarchCopyField: View {
    private let label: String
    private let value: String
    private let size: MonarchButtonSize
    private let isMultiline: Bool
    private let isMonospaced: Bool
    private let isIconOnly: Bool
    private let isLabelHidden: Bool
    private let copyTitle: String?
    private let copiedTitle: String?
    private let resetAfter: Duration
    private let buttonVariant: MonarchButtonVariant
    private let onCopy: (@MainActor (String) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    /// A copy field.
    ///
    /// - Parameters:
    ///   - label: What the value is, such as "Webhook URL". Already localised.
    ///     It is shown above the box and completes the button's spoken name.
    ///   - value: The read-only text shown and copied.
    ///   - size: The height of the box and the size of the button.
    ///   - isMultiline: Whether the whole value wraps over several lines,
    ///     for certificates, JSON and snippets.
    ///   - isMonospaced: Whether the value uses the code type role, for keys,
    ///     hosts and URLs.
    ///   - isIconOnly: Whether the button shows only the copy icon.
    ///   - isLabelHidden: Whether the label is hidden from view. It is still spoken.
    ///   - copyTitle: Replaces the button's built-in title "Copy".
    ///   - copiedTitle: Replaces the button's built-in title "Copied".
    ///   - resetAfter: How long the copied state lasts. Two seconds by default.
    ///   - onCopy: Runs with the copied text after each copy.
    public init(
        _ label: String,
        value: String,
        size: MonarchButtonSize = .md,
        isMultiline: Bool = false,
        isMonospaced: Bool = true,
        isIconOnly: Bool = false,
        isLabelHidden: Bool = false,
        copyTitle: String? = nil,
        copiedTitle: String? = nil,
        resetAfter: Duration = .seconds(2),
        onCopy: (@MainActor (String) -> Void)? = nil
    ) {
        self.label = label
        self.value = value
        self.size = size
        self.isMultiline = isMultiline
        self.isMonospaced = isMonospaced
        self.isIconOnly = isIconOnly
        self.isLabelHidden = isLabelHidden
        self.copyTitle = copyTitle
        self.copiedTitle = copiedTitle
        self.resetAfter = resetAfter
        self.buttonVariant = .secondary
        self.onCopy = onCopy
    }

    /// A copy field whose button has the given weight, such as `.primary` where copying is the one thing to do.
    ///
    /// - Parameters:
    ///   - label: What the value is, such as "Webhook URL". Already localised.
    ///   - value: The read-only text shown and copied.
    ///   - buttonVariant: The visual weight of the copy button.
    ///   - size: The height of the box and the size of the button.
    ///   - isMultiline: Whether the whole value wraps over several lines.
    ///   - isMonospaced: Whether the value uses the code type role.
    ///   - isIconOnly: Whether the button shows only the copy icon.
    ///   - isLabelHidden: Whether the label is hidden from view. It is still spoken.
    ///   - copyTitle: Replaces the button's built-in title "Copy".
    ///   - copiedTitle: Replaces the button's built-in title "Copied".
    ///   - resetAfter: How long the copied state lasts. Two seconds by default.
    ///   - onCopy: Runs with the copied text after each copy.
    public init(
        _ label: String,
        value: String,
        buttonVariant: MonarchButtonVariant,
        size: MonarchButtonSize = .md,
        isMultiline: Bool = false,
        isMonospaced: Bool = true,
        isIconOnly: Bool = false,
        isLabelHidden: Bool = false,
        copyTitle: String? = nil,
        copiedTitle: String? = nil,
        resetAfter: Duration = .seconds(2),
        onCopy: (@MainActor (String) -> Void)? = nil
    ) {
        self.label = label
        self.value = value
        self.buttonVariant = buttonVariant
        self.size = size
        self.isMultiline = isMultiline
        self.isMonospaced = isMonospaced
        self.isIconOnly = isIconOnly
        self.isLabelHidden = isLabelHidden
        self.copyTitle = copyTitle
        self.copiedTitle = copiedTitle
        self.resetAfter = resetAfter
        self.onCopy = onCopy
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchCopyFieldTokens.stackSpacing) {
            if !isLabelHidden {
                Text(label)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors[role: MonarchCopyFieldTokens.label(isEnabled: isEnabled)])
                    .accessibilityHidden(true)
            }
            HStack(
                alignment: MonarchCopyFieldTokens.rowAlignment(isMultiline: isMultiline),
                spacing: MonarchCopyFieldTokens.buttonSpacing
            ) {
                box
                MonarchCopyButton(
                    copyTitle,
                    value: value,
                    target: label,
                    variant: buttonVariant,
                    size: size,
                    isIconOnly: isIconOnly,
                    copiedTitle: copiedTitle,
                    resetAfter: resetAfter,
                    onCopy: onCopy
                )
                .fixedSize()
            }
        }
    }

    private var box: some View {
        let shape = MonarchShapes.control
        return Text(MonarchCopyFieldTokens.displayText(value))
            .monarchTextStyle(MonarchCopyFieldTokens.textStyle(isMonospaced: isMonospaced))
            .foregroundStyle(theme.colors[role: MonarchCopyFieldTokens.text(isEnabled: isEnabled)])
            .lineLimit(MonarchCopyFieldTokens.lineLimit(isMultiline: isMultiline))
            .truncationMode(.middle)
            .multilineTextAlignment(.leading)
            .textSelection(.enabled)
            .padding(.horizontal, MonarchCopyFieldTokens.horizontalPadding)
            .padding(.vertical, MonarchCopyFieldTokens.verticalPadding)
            .frame(maxWidth: .infinity, minHeight: MonarchCopyFieldTokens.minHeight(size), alignment: .leading)
            .background(shape.fill(theme.colors[role: MonarchCopyFieldTokens.background(isEnabled: isEnabled)]))
            .overlay(
                shape.strokeBorder(
                    theme.colors[role: MonarchCopyFieldTokens.border(isEnabled: isEnabled)],
                    lineWidth: MonarchShapes.borderWidth
                )
            )
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(label)
            .accessibilityValue(MonarchCopyFieldTokens.accessibilityValue(value))
            .accessibilityAddTraits(.isStaticText)
    }
}

#if DEBUG
    #Preview("MonarchCopyField") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchCopyField("Webhook URL", value: "https://example.com/hooks/workspace/42/events")
                MonarchCopyField("Record", value: "v=spf1 include:_spf.example.com ~all", size: .sm, isIconOnly: true)
                MonarchCopyField(
                    "Embed code",
                    value: "<script src=\"https://example.com/widget.js\" async></script>",
                    isMultiline: true
                )
                MonarchCopyField("Disabled", value: "ws_01HZX4").disabled(true)
            }
        }
    }
#endif
