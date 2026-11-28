import SwiftUI

/// The chat and ticket reply box: a growing text field with a send button,
/// mentions, emoji, an attachment row, a draft status, reply and edit
/// contexts, typing callbacks and optional modes such as public reply and
/// internal note.
///
/// ```swift
/// MonarchMessageComposer(
///     "Message #design",
///     text: $draft,
///     placeholder: "Message #design",
///     replyTo: replying.map { MonarchMessageComposerReply(author: $0.author, excerpt: $0.text) },
///     mentions: MonarchMessageComposerMentions(people, status: status) { query = $0 },
///     draftStatus: draftStatus,
///     onAttach: { showsImporter = true },
///     onDismissReply: { replying = nil }
/// ) {
///     ForEach(uploads) { MonarchAttachmentChip($0.name, status: $0.status, onRemove: { remove($0) }) }
/// } onSend: { message in
///     try await store.send(message)
/// }
/// ```
///
/// The text is plain: mentions are written as "@Label" and reported in
/// ``MonarchMessageComposerMessage/mentions``. Rich formatting is not
/// offered. The app owns `text`, so it saves and restores drafts itself and
/// shows the outcome with `draftStatus`. `onSend` is `async`: the button
/// shows "Sending…" until it returns, then the text is cleared; if it
/// throws, the text stays so nothing is lost, and the error (or "Message not
/// sent. Try again.") shows under the composer and is announced.
///
/// The send button sends. With a hardware keyboard Return sends and
/// Shift-Return adds a line (or only Command-Return sends, with
/// `.commandReturn`), Escape cancels an edit and Up Arrow in an empty box
/// calls `onEditLast`. The on-screen keyboard's Return adds a line.
/// Disable the composer with `.disabled(true)` and say why with
/// `disabledReason`.
///
/// Accessibility: the text field is named by `label`. The reply or edit
/// context is read as one element with a separate dismiss button; the
/// emoji, attach and send buttons are named, with touch targets of at least
/// 44 points. The character count is read as the characters left. The
/// draft status uses ``MonarchSaveStatusIndicator``, which announces it.
/// Mention suggestions use ``MonarchMentionPicker``, which announces results
/// while focus stays in the text.
public struct MonarchMessageComposer<Attachments: View, Actions: View>: View {
    private let label: String
    @Binding private var text: String
    private let placeholder: String?
    private let replyTo: MonarchMessageComposerReply?
    private let modes: [MonarchMessageComposerMode]
    private let selectedMode: Binding<String>?
    private let mentions: MonarchMessageComposerMentions?
    private let draftStatus: MonarchSaveStatus
    private let sendShortcut: MonarchMessageComposerSendShortcut
    private let maxLength: Int?
    private let typingInterval: TimeInterval
    private let isEditing: Bool
    private let isSending: Bool
    private let hasAttachments: Bool
    private let showsEmojiButton: Bool
    private let showsFormatting: Bool
    private let disabledReason: String?
    private let labels: MonarchMessageComposerLabels
    private let onAttach: (@MainActor () -> Void)?
    private let onDismissReply: (@MainActor () -> Void)?
    private let onCancelEdit: (@MainActor () -> Void)?
    private let onEditLast: (@MainActor () -> Void)?
    private let onTyping: (@MainActor () -> Void)?
    private let onTypingStop: (@MainActor () -> Void)?
    private let attachments: Attachments
    private let actions: Actions
    private let onSend: @MainActor (MonarchMessageComposerMessage) async throws -> Void

    @State private var isSendingState = false
    @State private var sendError: String?
    @State private var modeState: String?
    @State private var query: MonarchMentionQuery?
    @State private var picked: [MonarchMessageComposerPicked] = []
    @State private var lastTypingSignal: Date?
    @State private var showsEmojiPicker = false
    @FocusState private var isFocused: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.locale) private var locale

    private typealias Tokens = MonarchMessageComposerTokens

    /// A message composer with an attachment row and extra actions.
    ///
    /// - Parameters:
    ///   - label: The accessible name of the text field, such as "Message #design".
    ///   - text: The text. The app owns it, so it can save and restore drafts.
    ///   - placeholder: Shown while the text is empty.
    ///   - replyTo: The message being answered, shown above the text.
    ///   - modes: Message types as a segmented control, such as reply and internal note.
    ///   - selectedMode: The selected mode's value. The composer keeps it when `nil`.
    ///   - mentions: Mention suggestions. Without them no picker is shown.
    ///   - draftStatus: The app's draft save state, shown under the composer.
    ///   - sendShortcut: Which hardware-keyboard Return sends.
    ///   - maxLength: The most characters. Shows a count; over it, sending is off.
    ///   - typingInterval: The least time between two `onTyping` calls, in seconds.
    ///   - isEditing: Edits an existing message: an edit context, a Cancel button and "Save".
    ///   - isSending: Shows the sending state, for apps that track sending themselves.
    ///   - hasAttachments: Allows sending without text, because the attachment row has files.
    ///   - showsEmojiButton: Shows a button that opens ``MonarchEmojiPicker``.
    ///   - showsFormatting: Shows the formatting row (bold, italic, strikethrough, code, lists, link, quote, code
    ///     block). The message stays plain text: each tool writes its Markdown marker at the end of the text.
    ///   - disabledReason: Why the composer is disabled, shown under it while disabled.
    ///   - labels: Replacements for the built-in strings.
    ///   - onAttach: Adds the attach button. Present the app's file or photo picker.
    ///   - onDismissReply: Adds a button that removes the reply context.
    ///   - onCancelEdit: Adds Cancel while editing; Escape calls it too.
    ///   - onEditLast: Up Arrow in an empty box, for example to edit your last message.
    ///   - onTyping: Called at most once per `typingInterval` while the text changes.
    ///   - onTypingStop: Called when the text is emptied or sent, focus leaves, or the composer goes away.
    ///   - attachments: The attachment row above the text, such as ``MonarchAttachmentChip`` views.
    ///   - actions: Extra buttons before the send button, such as schedule or templates.
    ///   - onSend: Receives the message. The text clears when it returns; when it throws the text stays and the error shows under the composer.
    public init(
        _ label: String,
        text: Binding<String>,
        placeholder: String? = nil,
        replyTo: MonarchMessageComposerReply? = nil,
        modes: [MonarchMessageComposerMode] = [],
        selectedMode: Binding<String>? = nil,
        mentions: MonarchMessageComposerMentions? = nil,
        draftStatus: MonarchSaveStatus = .idle,
        sendShortcut: MonarchMessageComposerSendShortcut = .returnKey,
        maxLength: Int? = nil,
        typingInterval: TimeInterval = 3,
        isEditing: Bool = false,
        isSending: Bool = false,
        hasAttachments: Bool = false,
        showsEmojiButton: Bool = true,
        showsFormatting: Bool = true,
        disabledReason: String? = nil,
        labels: MonarchMessageComposerLabels = MonarchMessageComposerLabels(),
        onAttach: (@MainActor () -> Void)? = nil,
        onDismissReply: (@MainActor () -> Void)? = nil,
        onCancelEdit: (@MainActor () -> Void)? = nil,
        onEditLast: (@MainActor () -> Void)? = nil,
        onTyping: (@MainActor () -> Void)? = nil,
        onTypingStop: (@MainActor () -> Void)? = nil,
        @ViewBuilder attachments: () -> Attachments,
        @ViewBuilder actions: () -> Actions,
        onSend: @escaping @MainActor (MonarchMessageComposerMessage) async throws -> Void
    ) {
        self.label = label
        self._text = text
        self.placeholder = placeholder
        self.replyTo = replyTo
        self.modes = modes
        self.selectedMode = selectedMode
        self.mentions = mentions
        self.draftStatus = draftStatus
        self.sendShortcut = sendShortcut
        self.maxLength = maxLength
        self.typingInterval = typingInterval
        self.isEditing = isEditing
        self.isSending = isSending
        self.hasAttachments = hasAttachments
        self.showsEmojiButton = showsEmojiButton
        self.showsFormatting = showsFormatting
        self.disabledReason = disabledReason
        self.labels = labels
        self.onAttach = onAttach
        self.onDismissReply = onDismissReply
        self.onCancelEdit = onCancelEdit
        self.onEditLast = onEditLast
        self.onTyping = onTyping
        self.onTypingStop = onTypingStop
        self.attachments = attachments()
        self.actions = actions()
        self.onSend = onSend
    }

    private var pending: Bool { isSending || isSendingState }
    private var mode: MonarchMessageComposerMode? { Tokens.mode(selectedMode?.wrappedValue ?? modeState, in: modes) }
    private var isNote: Bool { mode?.isNote ?? false }
    private var canSend: Bool {
        Tokens.canSend(
            text: text,
            hasAttachments: hasAttachments,
            maxLength: maxLength,
            isEnabled: isEnabled,
            isSending: pending
        )
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: Tokens.stackSpacing) {
            if !modes.isEmpty {
                modePicker
            }
            if let mentions {
                MonarchMentionPicker(
                    query,
                    items: mentions.suggestions,
                    status: mentions.status,
                    labels: labels.mentionPicker,
                    onRetry: mentions.onRetry,
                    onSelect: pick
                )
            }
            context
            box
            footer
        }
        .onChange(of: text, initial: true) { _, new in
            textChanged(new)
            if !new.isEmpty { sendError = nil }
        }
        .onChange(of: isFocused) { _, focused in
            if !focused { stopTyping() }
        }
        .onChange(of: isEditing, initial: true) { _, editing in
            if editing { isFocused = true }
        }
        .onDisappear { stopTyping() }
        .sheet(isPresented: $showsEmojiPicker) {
            MonarchEmojiPicker(labels: labels.emojiPicker) { emoji, _ in
                showsEmojiPicker = false
                text = Tokens.inserting(emoji, into: text)
                isFocused = true
            }
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
        }
    }

    // MARK: - Parts

    private var modePicker: some View {
        MonarchSegmentedControl(
            labels.modes ?? MonarchMessageComposerStrings.modes,
            selection: Binding(
                get: { mode?.value ?? "" },
                set: { value in
                    if let selectedMode { selectedMode.wrappedValue = value } else { modeState = value }
                }
            ),
            items: modes.map { MonarchSegmentedItem($0.label, value: $0.value) },
            size: .sm
        )
    }

    private var box: some View {
        let shape = MonarchShapes.container
        return VStack(alignment: .leading, spacing: Tokens.stackSpacing) {
            if Attachments.self != EmptyView.self {
                MonarchFlowLayout(spacing: Tokens.actionSpacing, rowSpacing: Tokens.actionSpacing) {
                    attachments
                }
            }
            field
            if showsFormatting {
                Rectangle()
                    .fill(theme.colors[role: \.borderSubtle])
                    .frame(height: MonarchShapes.borderWidth)
                    .padding(.horizontal, -Tokens.padding)
                    .accessibilityHidden(true)
            }
            actionRow
        }
        .padding(Tokens.padding)
        .background(shape.fill(theme.colors[role: Tokens.background(isNote: isNote, isEnabled: isEnabled)]))
        .overlay(
            shape.strokeBorder(
                theme.colors[role: Tokens.border(isNote: isNote, isEnabled: isEnabled)],
                lineWidth: MonarchShapes.borderWidth
            )
        )
        .monarchFocusRing(isFocused, in: shape)
    }

    @ViewBuilder private var context: some View {
        if let title = Tokens.contextTitle(isEditing: isEditing, replyTo: replyTo, labels: labels) {
            HStack(alignment: .center, spacing: Tokens.contextSpacing) {
                Rectangle()
                    .fill(theme.colors[role: \.accentBorder])
                    .frame(width: Tokens.contextRuleWidth)
                HStack(alignment: .firstTextBaseline, spacing: Tokens.contextSpacing) {
                    MonarchIcon(systemName: isEditing ? Tokens.editSymbol : Tokens.replySymbol, size: .sm)
                        .foregroundStyle(theme.colors.textMuted)
                    VStack(alignment: .leading, spacing: 0) {
                        Text(title)
                            .monarchTextStyle(.label)
                            .foregroundStyle(theme.colors.textDefault)
                        if !isEditing, let excerpt = replyTo?.excerpt, !excerpt.isEmpty {
                            Text(excerpt)
                                .monarchTextStyle(.caption)
                                .foregroundStyle(theme.colors.textMuted)
                                .lineLimit(1)
                        }
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityElement(children: .combine)
                if !isEditing, let onDismissReply {
                    iconButton(
                        Tokens.dismissSymbol,
                        label: labels.dismissReply ?? MonarchMessageComposerStrings.dismissReply
                    ) {
                        onDismissReply()
                        isFocused = true
                    }
                }
            }
            .fixedSize(horizontal: false, vertical: true)
            .padding(.trailing, Tokens.contextSpacing)
            .background(
                theme.colors[role: \.bgSubtle],
                in: MonarchShapes.control
            )
            .clipShape(MonarchShapes.control)
        }
    }

    private var field: some View {
        TextField(
            label,
            text: $text,
            prompt: Text(mode?.placeholder ?? placeholder ?? "").foregroundStyle(theme.colors.textSubtle),
            axis: .vertical
        )
        .lineLimit(1...Tokens.maxLines)
        .textFieldStyle(.plain)
        .monarchTextStyle(.body)
        .foregroundStyle(theme.colors[role: Tokens.foreground(isEnabled: isEnabled)])
        .focused($isFocused)
        .accessibilityLabel(label)
        .padding(.horizontal, Tokens.textPadding)
        .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget, alignment: .leading)
        .contentShape(Rectangle())
        .onTapGesture { isFocused = true }
        .onKeyPress(.return, phases: .down) { press in
            // Hardware keyboards only: the on-screen Return key inserts a line.
            guard sendShortcut == .returnKey, press.modifiers.isEmpty else { return .ignored }
            send()
            return .handled
        }
        .onKeyPress(.upArrow) {
            guard text.isEmpty, let onEditLast else { return .ignored }
            onEditLast()
            return .handled
        }
    }

    @ViewBuilder private var actionRow: some View {
        if showsFormatting {
            // The tools beside the send button when all of them fit; else a row of their own above it, so none is
            // out of sight.
            ViewThatFits(in: .horizontal) {
                HStack(spacing: Tokens.actionSpacing) {
                    formatRow([0, 1, 2]).fixedSize(horizontal: true, vertical: false)
                    trailingActions
                }
                VStack(alignment: .leading, spacing: 0) {
                    formattingTools
                    HStack(spacing: Tokens.actionSpacing) { trailingActions }
                }
            }
        } else {
            HStack(spacing: Tokens.actionSpacing) { trailingActions }
        }
    }

    @ViewBuilder private var trailingActions: some View {
        Group {
            if showsEmojiButton, isEnabled {
                iconButton(Tokens.emojiSymbol, label: labels.emoji ?? MonarchMessageComposerStrings.emoji) {
                    showsEmojiPicker = true
                }
            }
            if let onAttach {
                iconButton(
                    Tokens.attachSymbol,
                    label: labels.attach ?? MonarchMessageComposerStrings.attach,
                    action: onAttach
                )
            }
            actions
            Spacer(minLength: 0)
            if let count = Tokens.countText(text, maxLength: maxLength, locale: locale), let maxLength {
                Text(count)
                    .monarchTextStyle(.caption)
                    .monospacedDigit()
                    .foregroundStyle(theme.colors[role: Tokens.countForeground(text, maxLength: maxLength)])
                    .accessibilityLabel(Tokens.countLabel(text, maxLength: maxLength, labels: labels))
            }
            if isEditing, let onCancelEdit {
                MonarchButton(labels.cancel ?? MonarchMessageComposerStrings.cancel, variant: .ghost, size: .sm) {
                    onCancelEdit()
                }
                .keyboardShortcut(.cancelAction)
            }
            MonarchButton(
                Tokens.sendTitle(isSending: false, isEditing: isEditing, mode: mode, labels: labels),
                systemImage: isEditing ? nil : Tokens.sendSymbol,
                variant: .primary,
                size: .sm,
                isLoading: pending,
                loadingLabel: Tokens.sendTitle(isSending: true, isEditing: isEditing, mode: mode, labels: labels)
            ) {
                send()
            }
            .keyboardShortcut(.return, modifiers: .command)
            .disabled(!canSend && !pending)
        }
    }

    @ViewBuilder private var footer: some View {
        if let sendError {
            MonarchFieldErrorRow(message: sendError, isSpoken: true)
        } else if !isEnabled, let disabledReason, !disabledReason.isEmpty {
            Text(disabledReason)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
        } else if draftStatus != .idle {
            MonarchSaveStatusIndicator(
                draftStatus,
                labels: MonarchSaveStatusIndicatorLabels(
                    saving: labels.draftSaving ?? MonarchMessageComposerStrings.draftSaving,
                    saved: labels.draftSaved ?? MonarchMessageComposerStrings.draftSaved,
                    error: labels.draftError ?? MonarchMessageComposerStrings.draftError
                )
            )
        }
    }

    /// The formatting tools: one row when it fits the box, two rows when it does not, so that no tool is out of
    /// sight; a row still scrolls sideways when even that is too wide.
    private var formattingTools: some View {
        ViewThatFits(in: .horizontal) {
            formatRow([0, 1, 2]).fixedSize(horizontal: true, vertical: false)
            VStack(alignment: .leading, spacing: 0) {
                formatRow([0, 1]).fixedSize(horizontal: true, vertical: false)
                formatRow([2]).fixedSize(horizontal: true, vertical: false)
            }
            ScrollView(.horizontal, showsIndicators: false) { formatRow([0, 1, 2]) }
        }
        .disabled(!isEnabled)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func formatRow(_ groups: [Int]) -> some View {
        HStack(spacing: 0) {
            ForEach(Array(groups.enumerated()), id: \.element) { position, index in
                if position > 0 {
                    Rectangle()
                        .fill(theme.colors[role: \.borderSubtle])
                        .frame(width: MonarchShapes.borderWidth, height: Tokens.formatDividerHeight)
                        .accessibilityHidden(true)
                }
                ForEach(Tokens.formatGroups[index]) { tool in
                    iconButton(
                        tool.symbol,
                        label: Tokens.formatLabel(tool, labels: labels)
                    ) {
                        text = Tokens.applying(tool, to: text)
                        isFocused = true
                    }
                }
            }
        }
    }

    private func iconButton(_ symbol: String, label: String, action: @escaping @MainActor () -> Void) -> some View {
        Button(action: action) {
            MonarchIcon(systemName: symbol, size: .md)
                .foregroundStyle(theme.colors[role: isEnabled ? \.textMuted : \.textDisabled])
                .monarchMinimumHitTarget()
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }

    // MARK: - Behaviour

    private func textChanged(_ new: String) {
        if let mentions {
            let found = MonarchMentionQuery.find(in: new, triggers: mentions.triggers)
            if found != query {
                query = found
                mentions.onQueryChange(found)
            }
        }
        if new.isEmpty {
            stopTyping()
        } else if isFocused, let onTyping {
            let now = Date()
            if Tokens.shouldSignalTyping(now: now, lastSignal: lastTypingSignal, interval: typingInterval) {
                lastTypingSignal = now
                onTyping()
            }
        }
    }

    private func stopTyping() {
        guard lastTypingSignal != nil else { return }
        lastTypingSignal = nil
        onTypingStop?()
    }

    private func pick(_ item: MonarchMentionItem) {
        guard let query else { return }
        picked.append(MonarchMessageComposerPicked(trigger: query.trigger, item: item))
        text = query.replacing(in: text, with: Tokens.mentionText(item, trigger: query.trigger))
        isFocused = true
    }

    private func send() {
        guard canSend else { return }
        let message = Tokens.message(text: text, picked: picked, mode: mode)
        stopTyping()
        isSendingState = true
        sendError = nil
        Task { @MainActor in
            do {
                try await onSend(message)
                text = ""
                picked = []
            } catch {
                // The text stays, so the person can try again, and the failure is shown and announced.
                let failure = MonarchFailureMessage.message(
                    for: error,
                    override: labels.sendFailed,
                    fallback: MonarchMessageComposerStrings.sendFailed
                )
                sendError = failure
                MonarchAnnouncer.post(MonarchFailureMessage.spoken(failure), politeness: .assertive)
            }
            isSendingState = false
        }
    }
}

extension MonarchMessageComposer where Actions == EmptyView {
    /// A message composer with an attachment row.
    ///
    /// - Parameters:
    ///   - label: The accessible name of the text field, such as "Message #design".
    ///   - text: The text. The app owns it, so it can save and restore drafts.
    ///   - placeholder: Shown while the text is empty.
    ///   - replyTo: The message being answered, shown above the text.
    ///   - modes: Message types as a segmented control, such as reply and internal note.
    ///   - selectedMode: The selected mode's value. The composer keeps it when `nil`.
    ///   - mentions: Mention suggestions. Without them no picker is shown.
    ///   - draftStatus: The app's draft save state, shown under the composer.
    ///   - sendShortcut: Which hardware-keyboard Return sends.
    ///   - maxLength: The most characters. Shows a count; over it, sending is off.
    ///   - typingInterval: The least time between two `onTyping` calls, in seconds.
    ///   - isEditing: Edits an existing message: an edit context, a Cancel button and "Save".
    ///   - isSending: Shows the sending state, for apps that track sending themselves.
    ///   - hasAttachments: Allows sending without text, because the attachment row has files.
    ///   - showsEmojiButton: Shows a button that opens ``MonarchEmojiPicker``.
    ///   - disabledReason: Why the composer is disabled, shown under it while disabled.
    ///   - labels: Replacements for the built-in strings.
    ///   - onAttach: Adds the attach button. Present the app's file or photo picker.
    ///   - onDismissReply: Adds a button that removes the reply context.
    ///   - onCancelEdit: Adds Cancel while editing; Escape calls it too.
    ///   - onEditLast: Up Arrow in an empty box, for example to edit your last message.
    ///   - onTyping: Called at most once per `typingInterval` while the text changes.
    ///   - onTypingStop: Called when the text is emptied or sent, focus leaves, or the composer goes away.
    ///   - attachments: The attachment row above the text, such as ``MonarchAttachmentChip`` views.
    ///   - onSend: Receives the message. The text clears when it returns; when it throws the text stays and the error shows under the composer.
    public init(
        _ label: String,
        text: Binding<String>,
        placeholder: String? = nil,
        replyTo: MonarchMessageComposerReply? = nil,
        modes: [MonarchMessageComposerMode] = [],
        selectedMode: Binding<String>? = nil,
        mentions: MonarchMessageComposerMentions? = nil,
        draftStatus: MonarchSaveStatus = .idle,
        sendShortcut: MonarchMessageComposerSendShortcut = .returnKey,
        maxLength: Int? = nil,
        typingInterval: TimeInterval = 3,
        isEditing: Bool = false,
        isSending: Bool = false,
        hasAttachments: Bool = false,
        showsEmojiButton: Bool = true,
        disabledReason: String? = nil,
        labels: MonarchMessageComposerLabels = MonarchMessageComposerLabels(),
        onAttach: (@MainActor () -> Void)? = nil,
        onDismissReply: (@MainActor () -> Void)? = nil,
        onCancelEdit: (@MainActor () -> Void)? = nil,
        onEditLast: (@MainActor () -> Void)? = nil,
        onTyping: (@MainActor () -> Void)? = nil,
        onTypingStop: (@MainActor () -> Void)? = nil,
        @ViewBuilder attachments: () -> Attachments,
        onSend: @escaping @MainActor (MonarchMessageComposerMessage) async throws -> Void
    ) {
        self.init(
            label,
            text: text,
            placeholder: placeholder,
            replyTo: replyTo,
            modes: modes,
            selectedMode: selectedMode,
            mentions: mentions,
            draftStatus: draftStatus,
            sendShortcut: sendShortcut,
            maxLength: maxLength,
            typingInterval: typingInterval,
            isEditing: isEditing,
            isSending: isSending,
            hasAttachments: hasAttachments,
            showsEmojiButton: showsEmojiButton,
            disabledReason: disabledReason,
            labels: labels,
            onAttach: onAttach,
            onDismissReply: onDismissReply,
            onCancelEdit: onCancelEdit,
            onEditLast: onEditLast,
            onTyping: onTyping,
            onTypingStop: onTypingStop,
            attachments: attachments,
            actions: { EmptyView() },
            onSend: onSend
        )
    }
}

extension MonarchMessageComposer where Attachments == EmptyView, Actions == EmptyView {
    /// A message composer without an attachment row or extra actions.
    ///
    /// - Parameters:
    ///   - label: The accessible name of the text field, such as "Message #design".
    ///   - text: The text. The app owns it, so it can save and restore drafts.
    ///   - placeholder: Shown while the text is empty.
    ///   - replyTo: The message being answered, shown above the text.
    ///   - modes: Message types as a segmented control, such as reply and internal note.
    ///   - selectedMode: The selected mode's value. The composer keeps it when `nil`.
    ///   - mentions: Mention suggestions. Without them no picker is shown.
    ///   - draftStatus: The app's draft save state, shown under the composer.
    ///   - sendShortcut: Which hardware-keyboard Return sends.
    ///   - maxLength: The most characters. Shows a count; over it, sending is off.
    ///   - typingInterval: The least time between two `onTyping` calls, in seconds.
    ///   - isEditing: Edits an existing message: an edit context, a Cancel button and "Save".
    ///   - isSending: Shows the sending state, for apps that track sending themselves.
    ///   - hasAttachments: Allows sending without text, because the app shows files elsewhere.
    ///   - showsEmojiButton: Shows a button that opens ``MonarchEmojiPicker``.
    ///   - disabledReason: Why the composer is disabled, shown under it while disabled.
    ///   - labels: Replacements for the built-in strings.
    ///   - onAttach: Adds the attach button. Present the app's file or photo picker.
    ///   - onDismissReply: Adds a button that removes the reply context.
    ///   - onCancelEdit: Adds Cancel while editing; Escape calls it too.
    ///   - onEditLast: Up Arrow in an empty box, for example to edit your last message.
    ///   - onTyping: Called at most once per `typingInterval` while the text changes.
    ///   - onTypingStop: Called when the text is emptied or sent, focus leaves, or the composer goes away.
    ///   - onSend: Receives the message. The text clears when it returns; when it throws the text stays and the error shows under the composer.
    public init(
        _ label: String,
        text: Binding<String>,
        placeholder: String? = nil,
        replyTo: MonarchMessageComposerReply? = nil,
        modes: [MonarchMessageComposerMode] = [],
        selectedMode: Binding<String>? = nil,
        mentions: MonarchMessageComposerMentions? = nil,
        draftStatus: MonarchSaveStatus = .idle,
        sendShortcut: MonarchMessageComposerSendShortcut = .returnKey,
        maxLength: Int? = nil,
        typingInterval: TimeInterval = 3,
        isEditing: Bool = false,
        isSending: Bool = false,
        hasAttachments: Bool = false,
        showsEmojiButton: Bool = true,
        disabledReason: String? = nil,
        labels: MonarchMessageComposerLabels = MonarchMessageComposerLabels(),
        onAttach: (@MainActor () -> Void)? = nil,
        onDismissReply: (@MainActor () -> Void)? = nil,
        onCancelEdit: (@MainActor () -> Void)? = nil,
        onEditLast: (@MainActor () -> Void)? = nil,
        onTyping: (@MainActor () -> Void)? = nil,
        onTypingStop: (@MainActor () -> Void)? = nil,
        onSend: @escaping @MainActor (MonarchMessageComposerMessage) async throws -> Void
    ) {
        self.init(
            label,
            text: text,
            placeholder: placeholder,
            replyTo: replyTo,
            modes: modes,
            selectedMode: selectedMode,
            mentions: mentions,
            draftStatus: draftStatus,
            sendShortcut: sendShortcut,
            maxLength: maxLength,
            typingInterval: typingInterval,
            isEditing: isEditing,
            isSending: isSending,
            hasAttachments: hasAttachments,
            showsEmojiButton: showsEmojiButton,
            disabledReason: disabledReason,
            labels: labels,
            onAttach: onAttach,
            onDismissReply: onDismissReply,
            onCancelEdit: onCancelEdit,
            onEditLast: onEditLast,
            onTyping: onTyping,
            onTypingStop: onTypingStop,
            attachments: { EmptyView() },
            actions: { EmptyView() },
            onSend: onSend
        )
    }

    /// A plain message composer: a text box with a send button and, when asked, no formatting row.
    ///
    /// - Parameters:
    ///   - label: The accessible name of the text field.
    ///   - text: The text. The app owns it, so it can save and restore drafts.
    ///   - placeholder: Shown while the text is empty.
    ///   - showsEmojiButton: Shows a button that opens ``MonarchEmojiPicker``.
    ///   - showsFormatting: Shows the formatting row. Off for a plain reply box.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSend: Receives the message. The text clears when it returns; when it throws the text stays and the error shows under the composer.
    public init(
        _ label: String,
        text: Binding<String>,
        placeholder: String? = nil,
        showsEmojiButton: Bool = true,
        showsFormatting: Bool,
        labels: MonarchMessageComposerLabels = MonarchMessageComposerLabels(),
        onSend: @escaping @MainActor (MonarchMessageComposerMessage) async throws -> Void
    ) {
        self.init(
            label,
            text: text,
            placeholder: placeholder,
            showsEmojiButton: showsEmojiButton,
            showsFormatting: showsFormatting,
            labels: labels,
            attachments: { EmptyView() },
            actions: { EmptyView() },
            onSend: onSend
        )
    }
}

#if DEBUG
    #Preview("MonarchMessageComposer") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchMessageComposer("Message #design", text: .constant(""), placeholder: "Message #design") { _ in }
                MonarchMessageComposer(
                    "Reply",
                    text: .constant("Sounds good"),
                    replyTo: MonarchMessageComposerReply(author: "Ana Lopez", excerpt: "Can we ship on Friday?"),
                    draftStatus: .saved,
                    maxLength: 500,
                    onAttach: {},
                    onDismissReply: {}
                ) {
                    MonarchAttachmentChip("brief.pdf", size: 240_000)
                } onSend: { _ in
                }
                MonarchMessageComposer(
                    "Edit message",
                    text: .constant("Shipping on Friday."),
                    isEditing: true,
                    onCancelEdit: {}
                ) { _ in }
                MonarchMessageComposer(
                    "Ticket reply",
                    text: .constant(""),
                    modes: [
                        MonarchMessageComposerMode("reply", label: "Reply"),
                        MonarchMessageComposerMode("note", label: "Internal note", isNote: true, sendLabel: "Add note"),
                    ],
                    selectedMode: .constant("note")
                ) { _ in }
                MonarchMessageComposer(
                    "Message",
                    text: .constant(""),
                    disabledReason: "You can't post in this channel."
                ) { _ in }
                .disabled(true)
            }
        }
    }
#endif
