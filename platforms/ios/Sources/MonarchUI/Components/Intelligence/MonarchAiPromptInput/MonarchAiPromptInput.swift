import SwiftUI
import UniformTypeIdentifiers

/// The prompt box of an AI chat: a growing text field with attachments, a
/// slot for the slash command or mention menu, a toolbar, a character and
/// token count, and Send, which becomes Stop while a response runs.
///
/// ```swift
/// ScrollView { messages }
///     .safeAreaInset(edge: .bottom) {
///         MonarchAiPromptInput(
///             "Message the assistant",
///             text: $prompt,
///             attachments: files,
///             maxLength: 4000,
///             isBusy: conversation.isBusy,
///             onStop: { conversation.stop() },
///             onAttach: { showsPhotoPicker = true },
///             onReceiveFiles: { providers in upload(providers) },
///             onRemoveAttachment: { id in files.removeAll { $0.id == id } },
///             triggerMenu: {
///                 MonarchAiSlashCommands(text: $prompt, commands: commands) { "/\($0.label) " }
///             },
///             toolbar: { MonarchAiModelPicker(selection: $model, models: models) },
///             footer: { MonarchAiDisclaimer() },
///             onSubmit: { text in conversation.send(text) }
///         )
///         .padding()
///     }
/// ```
///
/// The app owns `text`, so it can keep a draft, and the attachments, because
/// it picks and uploads them itself: `onAttach` opens its picker,
/// `onReceiveFiles` receives files pasted with the system Paste button or
/// dropped on the box, filtered by `acceptedContentTypes`. Send clears the
/// text at once and calls `onSubmit`; if it throws, the text comes back
/// unless something new was typed. Disable the input with `.disabled(true)`
/// and say why with `disabledReason`.
///
/// Put it in `.safeAreaInset(edge: .bottom)` of the conversation: SwiftUI
/// keeps it above the on-screen keyboard. The on-screen keyboard's Return
/// adds a line. On a hardware keyboard Return sends and Shift-Return adds a
/// line (or only Command-Return sends, with `.commandReturn`), and Escape
/// stops a running response.
///
/// Accessibility: the field is named by `label`. Send, Stop, Attach and
/// Paste are standard buttons of at least 44 points; Send is named "Send"
/// with an icon. The counter is read as "120 of 4000 characters", or by how
/// much the text is over; the characters left are announced once when the
/// warning share is reached, and going over the limit is announced. While
/// files are dragged over the box it shows "Drop files to attach" as text
/// as well as the accent colours. Dropping has an equivalent that VoiceOver
/// can reach: the Paste and Attach buttons.
public struct MonarchAiPromptInput<TriggerMenu: View, Toolbar: View, Footer: View>: View {
    private let label: String
    @Binding private var text: String
    private let placeholder: String?
    private let attachments: [MonarchAiAttachmentTrayFile]
    private let acceptedContentTypes: [UTType]
    private let submitOn: MonarchAiPromptInputSubmitOn
    private let minLines: Int
    private let maxLines: Int
    private let maxLength: Int?
    private let warnAt: Double
    private let isBusy: Bool
    private let showsLabel: Bool
    private let disabledReason: String?
    private let labels: MonarchAiPromptInputLabels
    private let countTokens: (@MainActor (String) -> Int)?
    private let onStop: (@MainActor () -> Void)?
    private let onAttach: (@MainActor () -> Void)?
    private let onReceiveFiles: (@MainActor ([NSItemProvider]) -> Void)?
    private let onRemoveAttachment: (@MainActor (String) -> Void)?
    private let onRetryAttachment: (@MainActor (String) -> Void)?
    private let triggerMenu: TriggerMenu
    private let toolbar: Toolbar
    private let footer: Footer
    private let onSubmit: @MainActor (String) async throws -> Void

    @State private var isDragging = false
    @FocusState private var ownFocus: Bool
    private let focus: FocusState<Bool>.Binding?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.monarchPromptShowsGlyph) private var showsFieldGlyph
    @Environment(\.isEnabled) private var isEnabled

    private typealias Tokens = MonarchAiPromptInputTokens

    /// A prompt input with a trigger menu, a toolbar and a footer.
    ///
    /// - Parameters:
    ///   - label: The accessible name of the field, such as "Message the assistant".
    ///   - text: The prompt. The app owns it, so it can keep a draft.
    ///   - placeholder: Shown while the text is empty. Built in: "Send a message…".
    ///   - attachments: The attached files, shown by ``MonarchAiAttachmentTray`` inside the box. They allow sending without text.
    ///   - acceptedContentTypes: The types `onReceiveFiles` takes from Paste and drops.
    ///   - submitOn: Which hardware-keyboard key sends.
    ///   - minLines: The fewest visible lines.
    ///   - maxLines: The lines the text grows to before it scrolls.
    ///   - maxLength: The most characters. Shows the counter; over it sending is off. The text is never cut.
    ///   - warnAt: The share of `maxLength` at which the counter turns to the warning colour and the characters left are announced.
    ///   - isBusy: A response is running: Send becomes Stop and Escape stops.
    ///   - showsLabel: Shows the label above the box.
    ///   - disabledReason: Why the input is disabled, shown under it while disabled.
    ///   - labels: Replacements for the built-in strings, including the attachment tray's.
    ///   - countTokens: Estimates tokens for the "≈ N tokens" readout. Called on every change.
    ///   - onStop: Stops the running response. Without it Stop is disabled.
    ///   - onAttach: Adds the attach button. Present the app's photo or file picker.
    ///   - onReceiveFiles: Adds the system Paste button and accepts dropped files of the accepted types.
    ///   - onRemoveAttachment: Removes an attachment by id. Without it the tray is disabled.
    ///   - onRetryAttachment: Retries a failed upload by id.
    ///   - focus: The app's focus state for the field, to focus it when a screen opens or to read whether it has focus. Without it the input keeps its own.
    ///   - triggerMenu: A menu above the box, such as ``MonarchAiSlashCommands`` or ``MonarchAiMentionContext``.
    ///   - toolbar: Controls in the bar under the text, such as ``MonarchAiPromptToolbar`` or a model picker.
    ///   - footer: Under the box, such as ``MonarchAiDisclaimer``.
    ///   - onSubmit: Receives the text. The field clears at once; if it throws, the text comes back.
    public init(
        _ label: String,
        text: Binding<String>,
        placeholder: String? = nil,
        attachments: [MonarchAiAttachmentTrayFile] = [],
        acceptedContentTypes: [UTType] = [.image],
        submitOn: MonarchAiPromptInputSubmitOn = .returnKey,
        minLines: Int = 1,
        maxLines: Int = 8,
        maxLength: Int? = nil,
        warnAt: Double = 0.9,
        isBusy: Bool = false,
        showsLabel: Bool = false,
        disabledReason: String? = nil,
        labels: MonarchAiPromptInputLabels = MonarchAiPromptInputLabels(),
        countTokens: (@MainActor (String) -> Int)? = nil,
        onStop: (@MainActor () -> Void)? = nil,
        onAttach: (@MainActor () -> Void)? = nil,
        onReceiveFiles: (@MainActor ([NSItemProvider]) -> Void)? = nil,
        onRemoveAttachment: (@MainActor (String) -> Void)? = nil,
        onRetryAttachment: (@MainActor (String) -> Void)? = nil,
        focus: FocusState<Bool>.Binding? = nil,
        @ViewBuilder triggerMenu: () -> TriggerMenu,
        @ViewBuilder toolbar: () -> Toolbar,
        @ViewBuilder footer: () -> Footer,
        onSubmit: @escaping @MainActor (String) async throws -> Void
    ) {
        self.label = label
        self._text = text
        self.placeholder = placeholder
        self.attachments = attachments
        self.acceptedContentTypes = acceptedContentTypes
        self.submitOn = submitOn
        self.minLines = minLines
        self.maxLines = maxLines
        self.maxLength = maxLength
        self.warnAt = warnAt
        self.isBusy = isBusy
        self.showsLabel = showsLabel
        self.disabledReason = disabledReason
        self.labels = labels
        self.countTokens = countTokens
        self.onStop = onStop
        self.onAttach = onAttach
        self.onReceiveFiles = onReceiveFiles
        self.onRemoveAttachment = onRemoveAttachment
        self.onRetryAttachment = onRetryAttachment
        self.focus = focus
        self.triggerMenu = triggerMenu()
        self.toolbar = toolbar()
        self.footer = footer()
        self.onSubmit = onSubmit
    }

    private var focusBinding: FocusState<Bool>.Binding { focus ?? $ownFocus }
    private var isFocused: Bool { focusBinding.wrappedValue }
    private var limit: Tokens.Limit { Tokens.limit(count: text.count, maxLength: maxLength, warnAt: warnAt) }
    private var canSubmit: Bool {
        Tokens.canSubmit(
            text: text,
            hasAttachments: !attachments.isEmpty,
            maxLength: maxLength,
            isEnabled: isEnabled,
            isBusy: isBusy
        )
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: Tokens.stackSpacing) {
            if showsLabel {
                Text(label)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors[role: Tokens.foreground(isEnabled: isEnabled)])
                    .accessibilityHidden(true)
            }
            triggerMenu
            box
            if !isEnabled, let disabledReason, !disabledReason.isEmpty {
                Text(disabledReason)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            footer
        }
        .onChange(of: limit) { previous, current in
            if let message = Tokens.announcement(
                from: previous,
                to: current,
                count: text.count,
                maxLength: maxLength,
                labels: labels
            ) {
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }

    // MARK: - Parts

    private var box: some View {
        let shape = MonarchShapes.container
        return VStack(alignment: .leading, spacing: Tokens.boxSpacing) {
            if !attachments.isEmpty {
                MonarchAiAttachmentTray(
                    attachments,
                    labels: labels.attachments,
                    onRetry: onRetryAttachment,
                    onRemove: { id in onRemoveAttachment?(id) }
                )
                .disabled(onRemoveAttachment == nil)
            }
            field
            if isDragging {
                Label {
                    Text(labels.dropHint ?? MonarchAiPromptInputStrings.dropHint)
                        .monarchTextStyle(.caption)
                } icon: {
                    MonarchIcon(systemName: Tokens.dropSymbol, size: .sm)
                }
                .foregroundStyle(theme.colors.accentStrong)
                .padding(.horizontal, Tokens.textPadding)
            }
            bar
        }
        .padding(Tokens.padding)
        .background(shape.fill(theme.colors[role: Tokens.background(isDragging: isDragging, isEnabled: isEnabled)]))
        .overlay(
            shape.strokeBorder(
                theme.colors[role: Tokens.border(limit: limit, isDragging: isDragging, isEnabled: isEnabled)],
                lineWidth: MonarchShapes.borderWidth
            )
        )
        .monarchFocusRing(isFocused, in: shape)
        .onDrop(of: onReceiveFiles == nil ? [] : acceptedContentTypes, isTargeted: $isDragging) { providers in
            receive(providers)
        }
    }

    private var field: some View {
        HStack(alignment: .top, spacing: Tokens.fieldGlyphSpacing) {
            if showsFieldGlyph {
                // The assistant mark at the start of the field, as the web draws it in the inline prompt.
                MonarchAiBotGlyph(size: .sm)
                    .foregroundStyle(theme.colors.aiStrong)
                    .frame(minHeight: MonarchAccessibility.minimumHitTarget)
                    .padding(.leading, Tokens.textPadding)
            }
            textField
        }
    }

    private var textField: some View {
        TextField(
            label,
            text: $text,
            prompt: Text(placeholder ?? MonarchAiPromptInputStrings.placeholder)
                .foregroundStyle(theme.colors.textSubtle),
            axis: .vertical
        )
        .lineLimit(Tokens.lines(min: minLines, max: maxLines))
        .textFieldStyle(.plain)
        .monarchTextStyle(.body)
        .foregroundStyle(theme.colors[role: Tokens.foreground(isEnabled: isEnabled)])
        .focused(focusBinding)
        .accessibilityLabel(label)
        .padding(.horizontal, Tokens.textPadding)
        .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget, alignment: .leading)
        .contentShape(Rectangle())
        .onTapGesture { focusBinding.wrappedValue = true }
        .onKeyPress(.return, phases: .down) { press in
            // Hardware keyboards only: the on-screen Return key adds a line.
            guard Tokens.sends(submitOn: submitOn, modifiers: press.modifiers) else { return .ignored }
            submit()
            return .handled
        }
        .onKeyPress(.escape) {
            guard isBusy, let onStop else { return .ignored }
            onStop()
            return .handled
        }
    }

    private var bar: some View {
        HStack(spacing: Tokens.barSpacing) {
            if let onAttach {
                Button(action: onAttach) {
                    Label(labels.attach ?? MonarchAiPromptInputStrings.attach, systemImage: Tokens.attachSymbol)
                        .labelStyle(.iconOnly)
                }
                .buttonStyle(.monarch(.ghost, size: .sm))
            }
            if onReceiveFiles != nil {
                PasteButton(supportedContentTypes: acceptedContentTypes) { providers in
                    _ = receive(providers)
                }
                .labelStyle(.iconOnly)
                .buttonBorderShape(.capsule)
            }
            toolbar
            Spacer(minLength: 0)
            if let countTokens {
                Text(Tokens.tokensText(countTokens(text), labels: labels))
                    .monarchTextStyle(.caption)
                    .monospacedDigit()
                    .foregroundStyle(theme.colors.textMuted)
            }
            if let maxLength {
                Text(Tokens.counterText(count: text.count, maxLength: maxLength, labels: labels))
                    .monarchTextStyle(.caption)
                    .monospacedDigit()
                    .foregroundStyle(theme.colors[role: Tokens.counterForeground(limit)])
                    .accessibilityLabel(Tokens.counterLabel(count: text.count, maxLength: maxLength, labels: labels))
            }
            if isBusy {
                MonarchAiStopButton(stopLabel: labels.stop, showsLabel: false) {
                    onStop?()
                }
                .disabled(onStop == nil)
            } else {
                Button {
                    submit()
                } label: {
                    Label(labels.send ?? MonarchAiPromptInputStrings.send, systemImage: Tokens.sendSymbol)
                        .labelStyle(.iconOnly)
                }
                .buttonStyle(.monarch(.primary, size: .sm))
                .keyboardShortcut(.return, modifiers: .command)
                .disabled(!canSubmit)
            }
        }
    }

    // MARK: - Behaviour

    private func receive(_ providers: [NSItemProvider]) -> Bool {
        guard let onReceiveFiles, isEnabled else { return false }
        let accepted = providers.filter { provider in
            acceptedContentTypes.contains { type in provider.hasItemConformingToTypeIdentifier(type.identifier) }
        }
        guard !accepted.isEmpty else { return false }
        onReceiveFiles(accepted)
        return true
    }

    private func submit() {
        guard canSubmit else { return }
        let sent = text
        text = ""
        Task { @MainActor in
            do {
                try await onSubmit(sent)
            } catch {
                text = Tokens.restored(current: text, sent: sent)
            }
        }
    }
}

extension MonarchAiPromptInput where TriggerMenu == EmptyView, Footer == EmptyView {
    /// A prompt input with a toolbar.
    ///
    /// - Parameters:
    ///   - label: The accessible name of the field, such as "Message the assistant".
    ///   - text: The prompt. The app owns it, so it can keep a draft.
    ///   - placeholder: Shown while the text is empty. Built in: "Send a message…".
    ///   - attachments: The attached files, shown inside the box. They allow sending without text.
    ///   - acceptedContentTypes: The types `onReceiveFiles` takes from Paste and drops.
    ///   - submitOn: Which hardware-keyboard key sends.
    ///   - minLines: The fewest visible lines.
    ///   - maxLines: The lines the text grows to before it scrolls.
    ///   - maxLength: The most characters. Shows the counter; over it sending is off.
    ///   - warnAt: The share of `maxLength` at which the counter warns.
    ///   - isBusy: A response is running: Send becomes Stop and Escape stops.
    ///   - showsLabel: Shows the label above the box.
    ///   - disabledReason: Why the input is disabled, shown under it while disabled.
    ///   - labels: Replacements for the built-in strings.
    ///   - countTokens: Estimates tokens for the "≈ N tokens" readout.
    ///   - onStop: Stops the running response. Without it Stop is disabled.
    ///   - onAttach: Adds the attach button.
    ///   - onReceiveFiles: Adds the system Paste button and accepts dropped files.
    ///   - onRemoveAttachment: Removes an attachment by id.
    ///   - onRetryAttachment: Retries a failed upload by id.
    ///   - focus: The app's focus state for the field, to focus it when a screen opens or to read whether it has focus. Without it the input keeps its own.
    ///   - toolbar: Controls in the bar under the text.
    ///   - onSubmit: Receives the text. The field clears at once; if it throws, the text comes back.
    public init(
        _ label: String,
        text: Binding<String>,
        placeholder: String? = nil,
        attachments: [MonarchAiAttachmentTrayFile] = [],
        acceptedContentTypes: [UTType] = [.image],
        submitOn: MonarchAiPromptInputSubmitOn = .returnKey,
        minLines: Int = 1,
        maxLines: Int = 8,
        maxLength: Int? = nil,
        warnAt: Double = 0.9,
        isBusy: Bool = false,
        showsLabel: Bool = false,
        disabledReason: String? = nil,
        labels: MonarchAiPromptInputLabels = MonarchAiPromptInputLabels(),
        countTokens: (@MainActor (String) -> Int)? = nil,
        onStop: (@MainActor () -> Void)? = nil,
        onAttach: (@MainActor () -> Void)? = nil,
        onReceiveFiles: (@MainActor ([NSItemProvider]) -> Void)? = nil,
        onRemoveAttachment: (@MainActor (String) -> Void)? = nil,
        onRetryAttachment: (@MainActor (String) -> Void)? = nil,
        focus: FocusState<Bool>.Binding? = nil,
        @ViewBuilder toolbar: () -> Toolbar,
        onSubmit: @escaping @MainActor (String) async throws -> Void
    ) {
        self.init(
            label,
            text: text,
            placeholder: placeholder,
            attachments: attachments,
            acceptedContentTypes: acceptedContentTypes,
            submitOn: submitOn,
            minLines: minLines,
            maxLines: maxLines,
            maxLength: maxLength,
            warnAt: warnAt,
            isBusy: isBusy,
            showsLabel: showsLabel,
            disabledReason: disabledReason,
            labels: labels,
            countTokens: countTokens,
            onStop: onStop,
            onAttach: onAttach,
            onReceiveFiles: onReceiveFiles,
            onRemoveAttachment: onRemoveAttachment,
            onRetryAttachment: onRetryAttachment,
            focus: focus,
            triggerMenu: { EmptyView() },
            toolbar: toolbar,
            footer: { EmptyView() },
            onSubmit: onSubmit
        )
    }
}

extension MonarchAiPromptInput where TriggerMenu == EmptyView, Toolbar == EmptyView, Footer == EmptyView {
    /// A prompt input without a trigger menu, a toolbar or a footer.
    ///
    /// - Parameters:
    ///   - label: The accessible name of the field, such as "Message the assistant".
    ///   - text: The prompt. The app owns it, so it can keep a draft.
    ///   - placeholder: Shown while the text is empty. Built in: "Send a message…".
    ///   - attachments: The attached files, shown inside the box. They allow sending without text.
    ///   - acceptedContentTypes: The types `onReceiveFiles` takes from Paste and drops.
    ///   - submitOn: Which hardware-keyboard key sends.
    ///   - minLines: The fewest visible lines.
    ///   - maxLines: The lines the text grows to before it scrolls.
    ///   - maxLength: The most characters. Shows the counter; over it sending is off.
    ///   - warnAt: The share of `maxLength` at which the counter warns.
    ///   - isBusy: A response is running: Send becomes Stop and Escape stops.
    ///   - showsLabel: Shows the label above the box.
    ///   - disabledReason: Why the input is disabled, shown under it while disabled.
    ///   - labels: Replacements for the built-in strings.
    ///   - countTokens: Estimates tokens for the "≈ N tokens" readout.
    ///   - onStop: Stops the running response. Without it Stop is disabled.
    ///   - onAttach: Adds the attach button.
    ///   - onReceiveFiles: Adds the system Paste button and accepts dropped files.
    ///   - onRemoveAttachment: Removes an attachment by id.
    ///   - onRetryAttachment: Retries a failed upload by id.
    ///   - focus: The app's focus state for the field, to focus it when a screen opens or to read whether it has focus. Without it the input keeps its own.
    ///   - onSubmit: Receives the text. The field clears at once; if it throws, the text comes back.
    public init(
        _ label: String,
        text: Binding<String>,
        placeholder: String? = nil,
        attachments: [MonarchAiAttachmentTrayFile] = [],
        acceptedContentTypes: [UTType] = [.image],
        submitOn: MonarchAiPromptInputSubmitOn = .returnKey,
        minLines: Int = 1,
        maxLines: Int = 8,
        maxLength: Int? = nil,
        warnAt: Double = 0.9,
        isBusy: Bool = false,
        showsLabel: Bool = false,
        disabledReason: String? = nil,
        labels: MonarchAiPromptInputLabels = MonarchAiPromptInputLabels(),
        countTokens: (@MainActor (String) -> Int)? = nil,
        onStop: (@MainActor () -> Void)? = nil,
        onAttach: (@MainActor () -> Void)? = nil,
        onReceiveFiles: (@MainActor ([NSItemProvider]) -> Void)? = nil,
        onRemoveAttachment: (@MainActor (String) -> Void)? = nil,
        onRetryAttachment: (@MainActor (String) -> Void)? = nil,
        focus: FocusState<Bool>.Binding? = nil,
        onSubmit: @escaping @MainActor (String) async throws -> Void
    ) {
        self.init(
            label,
            text: text,
            placeholder: placeholder,
            attachments: attachments,
            acceptedContentTypes: acceptedContentTypes,
            submitOn: submitOn,
            minLines: minLines,
            maxLines: maxLines,
            maxLength: maxLength,
            warnAt: warnAt,
            isBusy: isBusy,
            showsLabel: showsLabel,
            disabledReason: disabledReason,
            labels: labels,
            countTokens: countTokens,
            onStop: onStop,
            onAttach: onAttach,
            onReceiveFiles: onReceiveFiles,
            onRemoveAttachment: onRemoveAttachment,
            onRetryAttachment: onRetryAttachment,
            focus: focus,
            triggerMenu: { EmptyView() },
            toolbar: { EmptyView() },
            footer: { EmptyView() },
            onSubmit: onSubmit
        )
    }
}

#if DEBUG
    #Preview("MonarchAiPromptInput") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiPromptInput("Message the assistant", text: .constant("")) { _ in }
                MonarchAiPromptInput(
                    "Message the assistant",
                    text: .constant("Summarise the overdue invoices"),
                    attachments: [MonarchAiAttachmentTrayFile(id: "1", name: "ledger.pdf", size: 182_000)],
                    maxLength: 40,
                    isBusy: true,
                    countTokens: { $0.count / 4 },
                    onStop: {},
                    onAttach: {},
                    onRemoveAttachment: { _ in },
                    onSubmit: { _ in }
                )
                MonarchAiPromptInput(
                    "Message the assistant",
                    text: .constant(""),
                    disabledReason: "Connect a provider to start."
                ) { _ in }
                .disabled(true)
            }
        }
    }
#endif

private struct MonarchPromptShowsGlyphKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    /// Draws the assistant mark at the start of the prompt field.
    var monarchPromptShowsGlyph: Bool {
        get { self[MonarchPromptShowsGlyphKey.self] }
        set { self[MonarchPromptShowsGlyphKey.self] = newValue }
    }
}
