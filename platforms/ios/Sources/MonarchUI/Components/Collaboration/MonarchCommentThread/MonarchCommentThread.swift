import SwiftUI

/// One threaded discussion: the starting comment, its replies with older
/// ones folded, an optional quote of the passage it is about, Resolve and
/// Reopen, and a reply field.
///
/// ```swift
/// MonarchCommentThread(
///     thread.comments,
///     quote: thread.quote,
///     isResolved: thread.isResolved,
///     onResolvedChange: { try await store.setResolved($0, thread) },
///     onDelete: { try await store.delete(commentID: $0) },
///     onReply: { try await store.reply($0, to: thread) }
/// )
/// ```
///
/// The app saves: every action is an `async throws` closure. While it runs
/// the control is busy; when it throws, "That didn't work. Try again." is
/// shown and announced, and a reply keeps its text. Each comment is a
/// ``MonarchMessageRow``; a comment the person may delete has Delete in its
/// context menu (touch and hold), confirmed with a system confirmation
/// dialog. The reply field is a ``MonarchMessageComposer``. The first
/// comment and the latest `visibleReplies` replies stay visible; the ones in
/// between fold behind "Show N earlier replies". Pass `now` to fix the
/// relative times.
///
/// Without `onResolvedChange` there is no Resolve button; without `onReply`
/// there is no reply field. A resolved thread shows a Resolved badge and,
/// in place of the reply field, a hint to reopen it.
///
/// Accessibility: the thread is a container named "Thread started by …"
/// whose value is "Resolved" when resolved. The quote is read as "Quoted
/// text" with its text. Each comment is read as its author, time and text.
/// Revealing folded replies, resolving, reopening, posting and failures are
/// announced. Buttons are at least 44 points.
@_spi(Experimental) public struct MonarchCommentThread: View {
    private let comments: [MonarchCommentThreadComment]
    private let quote: String?
    private let isResolved: Bool
    private let visibleReplies: Int
    private let now: Date?
    private let labels: MonarchCommentThreadLabels
    private let onResolvedChange: (@MainActor (Bool) async throws -> Void)?
    private let onDelete: (@MainActor (String) async throws -> Void)?
    private let onReply: (@MainActor (String) async throws -> Void)?

    @State private var draft = ""
    @State private var isExpanded = false
    @State private var busy: Tokens.Busy?
    @State private var isFailed = false
    @State private var pendingDelete: MonarchCommentThreadComment?

    @Environment(\.monarchTheme) private var theme

    private typealias Tokens = MonarchCommentThreadTokens
    private typealias Strings = MonarchCommentThreadStrings

    /// A comment thread.
    ///
    /// - Parameters:
    ///   - comments: The comments, oldest first. The first starts the thread; the rest are replies.
    ///   - quote: The passage the thread is about, shown above it.
    ///   - isResolved: Whether the thread is resolved: a Resolved badge, and a hint in place of the reply field.
    ///   - visibleReplies: The latest replies kept visible before older ones fold.
    ///   - now: Fixes "now" for the relative times, for tests and previews. The clock by default.
    ///   - labels: Replacements for the built-in strings.
    ///   - onResolvedChange: Resolves (`true`) or reopens the thread. Adds the Resolve button.
    ///   - onDelete: Deletes the comment with this id, after confirmation. Adds Delete to comments that allow it.
    ///   - onReply: Posts a reply. Adds the reply field. The text clears when it returns and stays when it throws.
    public init(
        _ comments: [MonarchCommentThreadComment],
        quote: String? = nil,
        isResolved: Bool = false,
        visibleReplies: Int = 3,
        now: Date? = nil,
        labels: MonarchCommentThreadLabels = MonarchCommentThreadLabels(),
        onResolvedChange: (@MainActor (Bool) async throws -> Void)? = nil,
        onDelete: (@MainActor (String) async throws -> Void)? = nil,
        onReply: (@MainActor (String) async throws -> Void)? = nil
    ) {
        self.comments = comments
        self.quote = quote
        self.isResolved = isResolved
        self.visibleReplies = visibleReplies
        self.now = now
        self.labels = labels
        self.onResolvedChange = onResolvedChange
        self.onDelete = onDelete
        self.onReply = onReply
    }

    private var folded: Int {
        Tokens.foldedCount(
            replies: max(0, comments.count - 1),
            visibleReplies: visibleReplies,
            isExpanded: isExpanded
        )
    }

    /// The content of the view.
    public var body: some View {
        let shown = Tokens.shown(comments, folded: folded)
        VStack(alignment: .leading, spacing: Tokens.stackSpacing) {
            if quote != nil || onResolvedChange != nil || isResolved {
                topRow
                    .padding(.horizontal, Tokens.horizontalPadding)
            }
            VStack(alignment: .leading, spacing: 0) {
                if let root = shown.root {
                    row(root)
                }
                if folded > 0 {
                    MonarchButton(
                        Tokens.showEarlierTitle(folded, labels: labels),
                        variant: .ghost,
                        size: .sm
                    ) {
                        reveal()
                    }
                    .padding(.horizontal, Tokens.horizontalPadding)
                }
                ForEach(shown.replies) { comment in
                    row(comment)
                }
            }
            footer
            if isFailed {
                MonarchInlineNotice(labels.actionFailed ?? Strings.actionFailed, tone: .danger)
                    .padding(.horizontal, Tokens.horizontalPadding)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(Tokens.threadLabel(comments, labels: labels) ?? "")
        .accessibilityValue(Tokens.stateValue(isResolved: isResolved, labels: labels) ?? "")
        .confirmationDialog(
            labels.deleteTitle ?? Strings.deleteTitle,
            isPresented: Binding(
                get: { pendingDelete != nil },
                set: { if !$0 { pendingDelete = nil } }
            ),
            titleVisibility: .visible,
            presenting: pendingDelete,
            actions: { comment in
                Button(labels.delete ?? Strings.delete, role: .destructive) { delete(comment) }
                Button(labels.cancel ?? Strings.cancel, role: .cancel) { pendingDelete = nil }
            },
            message: { _ in
                Text(labels.deleteMessage ?? Strings.deleteMessage)
            }
        )
    }

    // MARK: - Parts

    private var topRow: some View {
        HStack(alignment: .top, spacing: Tokens.topSpacing) {
            if let quote {
                HStack(spacing: Tokens.quoteSpacing) {
                    Rectangle()
                        .fill(theme.colors[role: Tokens.quoteBar])
                        .frame(width: Tokens.quoteBarWidth)
                    Text(quote)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors[role: Tokens.muted])
                        .fixedSize(horizontal: false, vertical: true)
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(labels.quote ?? Strings.quote)
                .accessibilityValue(quote)
            }
            Spacer(minLength: 0)
            if isResolved {
                MonarchBadge(
                    labels.resolved ?? Strings.resolved,
                    tone: .success,
                    systemImage: Tokens.resolveSymbol
                )
            }
            if onResolvedChange != nil {
                let button = Tokens.resolveButton(isResolved: isResolved, labels: labels)
                MonarchButton(
                    button.title,
                    systemImage: button.symbol,
                    variant: .ghost,
                    size: .sm,
                    isLoading: busy == .resolve
                ) {
                    setResolved(!isResolved)
                }
                .disabled(busy != nil && busy != .resolve)
            }
        }
    }

    private func row(_ comment: MonarchCommentThreadComment) -> some View {
        let canDelete = Tokens.canDelete(comment, hasDeleteAction: onDelete != nil)
        return MonarchMessageRow(
            MonarchMessageRowAuthor(comment.author, url: comment.avatarURL, seed: comment.seed),
            time: comment.createdAt,
            now: now
        ) {
            Text(comment.body)
        } actions: {
            if canDelete {
                Button(labels.delete ?? Strings.delete, systemImage: Tokens.deleteSymbol, role: .destructive) {
                    pendingDelete = comment
                }
            }
        }
        // The delete button on the row itself, as on the web; the long-press menu has it too.
        .overlay(alignment: .topTrailing) {
            if canDelete {
                Button {
                    pendingDelete = comment
                } label: {
                    MonarchIcon(systemName: Tokens.deleteSymbol, size: .sm)
                        .foregroundStyle(theme.colors[role: Tokens.muted])
                        .monarchMinimumHitTarget()
                }
                .buttonStyle(.plain)
                .accessibilityLabel(labels.delete ?? Strings.delete)
            }
        }
    }

    @ViewBuilder private var footer: some View {
        switch Tokens.footer(canReply: onReply != nil, isResolved: isResolved) {
        case .composer:
            MonarchMessageComposer(
                labels.reply ?? Strings.reply,
                text: $draft,
                placeholder: labels.replyPlaceholder ?? Strings.replyPlaceholder,
                showsEmojiButton: false,
                showsFormatting: false,
                labels: MonarchMessageComposerLabels(send: labels.send ?? Strings.send)
            ) { message in
                try await reply(message.text)
            }
            .padding(.horizontal, Tokens.horizontalPadding)
        case .resolvedHint:
            Text(labels.resolvedHint ?? Strings.resolvedHint)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: Tokens.muted])
                .padding(.horizontal, Tokens.horizontalPadding)
        case .none:
            EmptyView()
        }
    }

    // MARK: - Actions

    private func reveal() {
        let count = folded
        isExpanded = true
        announce(Tokens.earlierShownText(count, labels: labels))
    }

    private func reply(_ text: String) async throws {
        guard let onReply, let body = Tokens.replyBody(text) else { return }
        isFailed = false
        busy = .reply
        defer { busy = nil }
        do {
            try await onReply(body)
            announce(labels.sentNotice ?? Strings.sentNotice)
        } catch {
            fail()
            throw error
        }
    }

    private func setResolved(_ value: Bool) {
        guard let onResolvedChange, busy == nil else { return }
        isFailed = false
        busy = .resolve
        Task { @MainActor in
            do {
                try await onResolvedChange(value)
                announce(Tokens.resolvedNotice(value, labels: labels))
            } catch {
                fail()
            }
            busy = nil
        }
    }

    private func delete(_ comment: MonarchCommentThreadComment) {
        pendingDelete = nil
        guard let onDelete else { return }
        isFailed = false
        busy = .delete
        Task { @MainActor in
            do {
                try await onDelete(comment.id)
            } catch {
                fail()
            }
            busy = nil
        }
    }

    private func fail() {
        isFailed = true
        announce(labels.actionFailed ?? Strings.actionFailed)
    }

    private func announce(_ text: String) {
        AccessibilityNotification.Announcement(text).post()
    }
}

#if DEBUG
    #Preview("MonarchCommentThread") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        let comments = [
            MonarchCommentThreadComment(
                id: "c1",
                author: "Priya Raman",
                body: "Should this say \"billed every quarter\" instead?",
                createdAt: now.addingTimeInterval(-10_000)
            ),
            MonarchCommentThreadComment(
                id: "c2",
                author: "Tom Becker",
                body: "Yes. Monthly billing was retired last year.",
                createdAt: now.addingTimeInterval(-8_000),
                canDelete: true
            ),
        ]
        MonarchPreviewMatrix {
            MonarchCommentThread(
                comments,
                quote: "Billed monthly, cancel any time.",
                now: now,
                onResolvedChange: { _ in },
                onDelete: { _ in },
                onReply: { _ in }
            )
            MonarchCommentThread(comments, isResolved: true, now: now, onResolvedChange: { _ in }, onReply: { _ in })
        }
    }
#endif
