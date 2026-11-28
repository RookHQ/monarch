import SwiftUI

/// An inline editor for a prompt the person already sent.
///
/// Resending an edited prompt starts a new branch of the conversation, so a
/// note under the field says the original stays available. The field is a
/// standard multi-line `TextField` that grows with its text up to `maxLines`
/// and then scrolls.
///
/// ```swift
/// MonarchAiUserMessageEdit(
///     message.text,
///     onCancel: { isEditing = false },
///     onSubmit: { text in resend(text) }
/// )
/// ```
///
/// Send is enabled only when the trimmed text is not empty and differs from
/// the original; `onSubmit` receives the trimmed text. With a hardware
/// keyboard, Command-Return sends and Escape cancels. Disable the editor
/// with the standard `disabled(_:)` modifier, for example while a reply
/// streams.
///
/// Accessibility: the field is labelled "Edit message" and the branch note
/// is its hint; the visible note and its icon are not read twice. The field
/// and both buttons keep a 44-point touch target, and text scales with
/// Dynamic Type.
public struct MonarchAiUserMessageEdit: View {
    private let original: String
    private let maxLines: Int
    private let label: String?
    private let branchHint: String?
    private let sendLabel: String?
    private let cancelLabel: String?
    private let focusesOnAppear: Bool
    private let onCancel: @MainActor () -> Void
    private let onSubmit: @MainActor (String) -> Void

    @State private var text: String
    @FocusState private var isFocused: Bool
    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    /// A message editor.
    ///
    /// - Parameters:
    ///   - original: The prompt as it was sent.
    ///   - maxLines: The number of lines the field grows to before it scrolls.
    ///   - label: Replaces the built-in spoken label "Edit message".
    ///   - branchHint: Replaces the built-in note that resending keeps the original.
    ///   - sendLabel: Replaces the built-in "Send" title.
    ///   - cancelLabel: Replaces the built-in "Cancel" title.
    ///   - focusesOnAppear: Whether the field takes focus when it appears.
    ///   - onCancel: Runs when Cancel is activated. Leave edit mode here.
    ///   - onSubmit: Runs with the trimmed text when Send is activated.
    public init(
        _ original: String,
        maxLines: Int = 10,
        label: String? = nil,
        branchHint: String? = nil,
        sendLabel: String? = nil,
        cancelLabel: String? = nil,
        focusesOnAppear: Bool = true,
        onCancel: @escaping @MainActor () -> Void,
        onSubmit: @escaping @MainActor (String) -> Void
    ) {
        self.original = original
        self.maxLines = maxLines
        self.label = label
        self.branchHint = branchHint
        self.sendLabel = sendLabel
        self.cancelLabel = cancelLabel
        self.focusesOnAppear = focusesOnAppear
        self.onCancel = onCancel
        self.onSubmit = onSubmit
        _text = State(initialValue: original)
    }

    private var fieldLabel: String { label ?? MonarchAiUserMessageEditStrings.label }
    private var hint: String { branchHint ?? MonarchAiUserMessageEditStrings.branchHint }
    private var submission: String? {
        MonarchAiUserMessageEditTokens.submission(text: text, original: original)
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchAiUserMessageEditTokens.stackSpacing) {
            field
            HStack(alignment: .firstTextBaseline, spacing: MonarchAiUserMessageEditTokens.hintIconSpacing) {
                MonarchIcon(systemName: MonarchAiUserMessageEditTokens.hintSymbol, size: .sm)
                Text(hint).monarchTextStyle(.caption)
            }
            .foregroundStyle(theme.colors[role: MonarchAiUserMessageEditTokens.hintColor])
            .accessibilityHidden(true)
            ViewThatFits(in: .horizontal) {
                HStack(spacing: MonarchAiUserMessageEditTokens.actionSpacing) { actions }
                VStack(alignment: .trailing, spacing: MonarchAiUserMessageEditTokens.actionSpacing) { actions }
            }
            .frame(maxWidth: .infinity, alignment: .trailing)
        }
        .onAppear {
            if focusesOnAppear {
                isFocused = true
            }
        }
    }

    private var field: some View {
        let shape = MonarchShapes.control
        let lines = MonarchAiUserMessageEditTokens.lineLimit(maxLines: maxLines)
        return TextField(fieldLabel, text: $text, axis: .vertical)
            .lineLimit(lines)
            .textFieldStyle(.plain)
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors[role: MonarchAiUserMessageEditTokens.text(isEnabled: isEnabled)])
            .focused($isFocused)
            .accessibilityLabel(fieldLabel)
            .accessibilityHint(hint)
            .padding(.horizontal, MonarchAiUserMessageEditTokens.horizontalPadding)
            .padding(.vertical, MonarchAiUserMessageEditTokens.verticalPadding)
            .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget, alignment: .topLeading)
            .background(
                shape.fill(theme.colors[role: MonarchAiUserMessageEditTokens.background(isEnabled: isEnabled)])
            )
            .overlay(
                shape.strokeBorder(
                    theme.colors[role: MonarchAiUserMessageEditTokens.border(isEnabled: isEnabled)],
                    lineWidth: MonarchShapes.borderWidth
                )
            )
            .monarchFocusRing(isFocused, in: shape)
            .contentShape(Rectangle())
            .onTapGesture { isFocused = true }
    }

    @ViewBuilder private var actions: some View {
        MonarchButton(cancelLabel ?? MonarchAiUserMessageEditStrings.cancel, variant: .ghost, size: .sm) {
            onCancel()
        }
        .keyboardShortcut(.cancelAction)
        MonarchButton(sendLabel ?? MonarchAiUserMessageEditStrings.send, variant: .primary, size: .sm) {
            if let submission {
                onSubmit(submission)
            }
        }
        .keyboardShortcut(.return, modifiers: .command)
        .disabled(submission == nil)
    }
}

#if DEBUG
    #Preview("MonarchAiUserMessageEdit") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiUserMessageEdit(
                    "Summarise the open risks from the review.",
                    focusesOnAppear: false,
                    onCancel: {},
                    onSubmit: { _ in }
                )
                MonarchAiUserMessageEdit("Disabled", focusesOnAppear: false, onCancel: {}, onSubmit: { _ in })
                    .disabled(true)
            }
        }
    }
#endif
