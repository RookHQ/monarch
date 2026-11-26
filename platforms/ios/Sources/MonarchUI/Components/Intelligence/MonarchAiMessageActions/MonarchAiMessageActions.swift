import SwiftUI

/// The row of actions under an assistant message: copy, regenerate, edit,
/// thumbs up and down, share, read aloud, and a More menu.
///
/// ```swift
/// MonarchAiMessageActions(
///     message.text,
///     feedback: $message.feedback,
///     isReadingAloud: $isSpeaking,
///     isShareable: true,
///     onRegenerate: { regenerate(message) },
///     more: {
///         Button("Report", systemImage: "flag") { report(message) }
///     }
/// )
/// ```
///
/// An action appears only when it is turned on: Copy by `isCopyable`, Share
/// by `isShareable`, the thumbs by `feedback`, Read aloud by
/// `isReadingAloud`, Regenerate and Edit by their callbacks. Copy is
/// ``MonarchCopyButton`` (it writes to the pasteboard, shows "Copied" and
/// plays the success haptic); Share is the system `ShareLink` with the
/// message text. Read aloud only flips `isReadingAloud`: the app speaks the
/// text, for example with `AVSpeechSynthesizer`, and sets it back to `false`
/// when it finishes. Pressing the pressed thumb clears the rating; for
/// reasons and a comment after a rating, show ``MonarchAiFeedback`` instead
/// of the thumbs here.
///
/// The row is always visible; phones have no hover. When the full row does
/// not fit the width, Copy and the thumbs stay and the other actions move
/// into the More menu, with the `more` items after them. Put the same
/// actions in the message's `.contextMenu` for touch and hold.
///
/// Accessibility: the row is a container named "Message actions". Each
/// action is a standard button named by its title; the thumbs are toggles
/// (selected when pressed) and a pressed thumb is filled, so the state is a
/// shape as well as a colour. Read aloud is named "Stop reading" while
/// reading. Every button is at least 44 points.
public struct MonarchAiMessageActions<More: View>: View {
    private let messageText: String
    private let feedback: Binding<MonarchAiMessageFeedback?>?
    private let isReadingAloud: Binding<Bool>?
    private let isCopyable: Bool
    private let isShareable: Bool
    private let labels: MonarchAiMessageActionsLabels
    private let onCopy: (@MainActor (String) -> Void)?
    private let onRegenerate: (@MainActor () -> Void)?
    private let onEdit: (@MainActor () -> Void)?
    private let hasMore: Bool
    private let more: More

    /// Message actions with extra items in the More menu.
    ///
    /// - Parameters:
    ///   - messageText: The message's plain text: what Copy copies and Share shares.
    ///   - feedback: The rating. Shows the thumbs when set.
    ///   - isReadingAloud: Whether the app is reading the message aloud. Shows Read aloud when set.
    ///   - isCopyable: Shows Copy.
    ///   - isShareable: Shows Share, the system share sheet with the message text.
    ///   - labels: Replacements for the built-in strings.
    ///   - onCopy: Runs with the copied text after each copy.
    ///   - onRegenerate: Shows Regenerate and runs when it is pressed.
    ///   - onEdit: Shows Edit and runs when it is pressed.
    ///   - more: Extra menu items, such as `Button`s, at the end of the More menu.
    public init(
        _ messageText: String,
        feedback: Binding<MonarchAiMessageFeedback?>? = nil,
        isReadingAloud: Binding<Bool>? = nil,
        isCopyable: Bool = true,
        isShareable: Bool = false,
        labels: MonarchAiMessageActionsLabels = MonarchAiMessageActionsLabels(),
        onCopy: (@MainActor (String) -> Void)? = nil,
        onRegenerate: (@MainActor () -> Void)? = nil,
        onEdit: (@MainActor () -> Void)? = nil,
        @ViewBuilder more: () -> More
    ) {
        self.messageText = messageText
        self.feedback = feedback
        self.isReadingAloud = isReadingAloud
        self.isCopyable = isCopyable
        self.isShareable = isShareable
        self.labels = labels
        self.onCopy = onCopy
        self.onRegenerate = onRegenerate
        self.onEdit = onEdit
        self.hasMore = true
        self.more = more()
    }

    private typealias Tokens = MonarchAiMessageActionsTokens

    private var actions: [Tokens.Action] {
        Tokens.actions(
            isCopyable: isCopyable,
            hasRegenerate: onRegenerate != nil,
            hasEdit: onEdit != nil,
            hasFeedback: feedback != nil,
            isShareable: isShareable,
            hasReadAloud: isReadingAloud != nil
        )
    }

    private var isReading: Bool { isReadingAloud?.wrappedValue ?? false }

    /// The content of the view.
    public var body: some View {
        let all = actions
        ViewThatFits(in: .horizontal) {
            row(all, menu: [])
            row(Tokens.compactRow(all), menu: Tokens.compactMenu(all))
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.toolbar ?? MonarchAiMessageActionsStrings.toolbar)
        .sensoryFeedback(.selection, trigger: feedback?.wrappedValue)
    }

    private func row(_ shown: [Tokens.Action], menu: [Tokens.Action]) -> some View {
        HStack(spacing: Tokens.spacing) {
            ForEach(shown, id: \.self) { action in
                button(action)
            }
            if Tokens.showsMenu(menuActions: menu, hasMore: hasMore) {
                Menu {
                    ForEach(menu, id: \.self) { action in
                        menuItem(action)
                    }
                    if hasMore && !menu.isEmpty { Divider() }
                    more
                } label: {
                    Label(labels.more ?? MonarchAiMessageActionsStrings.more, systemImage: Tokens.moreSymbol)
                        .labelStyle(.iconOnly)
                }
                .buttonStyle(.monarch(Tokens.buttonVariant, size: Tokens.buttonSize))
            }
        }
        .fixedSize(horizontal: true, vertical: false)
    }

    @ViewBuilder private func button(_ action: Tokens.Action) -> some View {
        let title = Tokens.title(action, isReading: isReading, labels: labels)
        let symbol = Tokens.symbol(action, feedback: feedback?.wrappedValue, isReading: isReading)
        switch action {
        case .copy:
            MonarchCopyButton(
                title,
                value: messageText,
                variant: Tokens.buttonVariant,
                size: Tokens.buttonSize,
                isIconOnly: true,
                copiedTitle: labels.copied,
                onCopy: onCopy
            )
        case .good, .bad:
            if let feedback {
                Toggle(
                    isOn: Binding(
                        get: { feedback.wrappedValue == (action == .good ? .up : .down) },
                        set: { _ in feedback.wrappedValue = Tokens.rating(after: action, current: feedback.wrappedValue)
                        }
                    )
                ) {
                    Label(title, systemImage: symbol).labelStyle(.iconOnly)
                }
                .toggleStyle(.monarchButton(.ghost, size: Tokens.buttonSize))
            }
        case .share:
            ShareLink(item: messageText) {
                Label(title, systemImage: symbol).labelStyle(.iconOnly)
            }
            .buttonStyle(.monarch(Tokens.buttonVariant, size: Tokens.buttonSize))
        default:
            Button {
                perform(action)
            } label: {
                Label(title, systemImage: symbol).labelStyle(.iconOnly)
            }
            .buttonStyle(.monarch(Tokens.buttonVariant, size: Tokens.buttonSize))
        }
    }

    @ViewBuilder private func menuItem(_ action: Tokens.Action) -> some View {
        let title = Tokens.title(action, isReading: isReading, labels: labels)
        let symbol = Tokens.symbol(action, feedback: feedback?.wrappedValue, isReading: isReading)
        if action == .share {
            ShareLink(item: messageText) { Label(title, systemImage: symbol) }
        } else {
            Button(title, systemImage: symbol) { perform(action) }
        }
    }

    private func perform(_ action: Tokens.Action) {
        switch action {
        case .regenerate: onRegenerate?()
        case .edit: onEdit?()
        case .readAloud: isReadingAloud?.wrappedValue.toggle()
        case .good, .bad:
            if let feedback {
                feedback.wrappedValue = Tokens.rating(after: action, current: feedback.wrappedValue)
            }
        case .copy, .share: break
        }
    }
}

extension MonarchAiMessageActions where More == EmptyView {
    /// Message actions without extra menu items.
    ///
    /// - Parameters:
    ///   - messageText: The message's plain text: what Copy copies and Share shares.
    ///   - feedback: The rating. Shows the thumbs when set.
    ///   - isReadingAloud: Whether the app is reading the message aloud. Shows Read aloud when set.
    ///   - isCopyable: Shows Copy.
    ///   - isShareable: Shows Share, the system share sheet with the message text.
    ///   - labels: Replacements for the built-in strings.
    ///   - onCopy: Runs with the copied text after each copy.
    ///   - onRegenerate: Shows Regenerate and runs when it is pressed.
    ///   - onEdit: Shows Edit and runs when it is pressed.
    public init(
        _ messageText: String,
        feedback: Binding<MonarchAiMessageFeedback?>? = nil,
        isReadingAloud: Binding<Bool>? = nil,
        isCopyable: Bool = true,
        isShareable: Bool = false,
        labels: MonarchAiMessageActionsLabels = MonarchAiMessageActionsLabels(),
        onCopy: (@MainActor (String) -> Void)? = nil,
        onRegenerate: (@MainActor () -> Void)? = nil,
        onEdit: (@MainActor () -> Void)? = nil
    ) {
        self.messageText = messageText
        self.feedback = feedback
        self.isReadingAloud = isReadingAloud
        self.isCopyable = isCopyable
        self.isShareable = isShareable
        self.labels = labels
        self.onCopy = onCopy
        self.onRegenerate = onRegenerate
        self.onEdit = onEdit
        self.hasMore = false
        self.more = EmptyView()
    }
}

#if DEBUG
    #Preview("MonarchAiMessageActions") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiMessageActions(
                    "The answer.",
                    feedback: .constant(.up),
                    isReadingAloud: .constant(false),
                    isShareable: true,
                    onRegenerate: {},
                    onEdit: {},
                    more: { Button("Report", systemImage: "flag") {} }
                )
                MonarchAiMessageActions("The answer.", feedback: .constant(nil), isReadingAloud: .constant(true))
                MonarchAiMessageActions("Copy only.")
            }
        }
    }
#endif
