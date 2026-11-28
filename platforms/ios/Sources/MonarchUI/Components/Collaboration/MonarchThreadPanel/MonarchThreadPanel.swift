import SwiftUI

/// The replies to one message of a conversation: the message, a reply count
/// divider, the replies pinned to the newest one, a Follow button, and the
/// app's reply box pinned to the bottom.
///
/// ```swift
/// .navigationDestination(item: $openThread) { message in
///     MonarchThreadPanel(
///         store.replies(to: message),
///         isFollowing: store.follows(message),
///         onFollowingChange: { try await store.setFollowing($0, message) },
///         onRepliesSeen: { _ in store.markThreadRead(message) },
///         root: { MessageRowView(message) },
///         composer: {
///             MonarchMessageComposer("Reply in thread", text: $draft) { sent in
///                 try await store.reply(sent.text, to: message)
///             }
///         },
///         reply: { reply, context in
///             MonarchMessageRow(MonarchMessageRowAuthor(reply.author), time: reply.time, isGrouped: context.isGrouped) {
///                 Text(reply.text)
///             }
///         }
///     )
/// }
/// ```
///
/// Push it onto the conversation's `NavigationStack` on a phone, where the
/// back button and the back swipe close it, or show it in a
/// `NavigationStack` inside `.inspector` or a `.sheet` with `onClose`. The
/// panel sets the navigation title and puts Follow, `headerActions` and
/// Close in the toolbar. The replies are a ``MonarchVirtualMessageList``, so
/// only rows near the screen are built, day separators are added and
/// consecutive replies group; `root` and the reply count divider are its
/// header. The typing indicator and the reply box sit in the bottom safe
/// area inset, above the keyboard. `onRepliesSeen` is called once the
/// replies have been on screen for `seenDelay` while the scene is active.
///
/// Accessibility: the navigation title is the screen's heading. The
/// replies are a container named "Replies"; arriving replies are announced
/// by the list. The reply count divider is a header. Follow is a toggle
/// button with the system selected state; following, unfollowing and a
/// failure are announced. The deleted notice is announced when it appears.
/// Loading is one element read as "Loading thread".
@_spi(Experimental)
public struct MonarchThreadPanel<
    Item: MonarchVirtualMessageListItem,
    Reply: View,
    Root: View,
    Typing: View,
    Composer: View,
    Actions: View
>: View {
    private let replies: [Item]
    private let replyCount: Int?
    private let status: MonarchThreadPanelStatus
    private let isFollowing: Bool
    private let notice: String?
    private let seenDelay: Duration
    private let now: Date?
    private let labels: MonarchThreadPanelLabels
    private let onClose: (@MainActor () -> Void)?
    private let onRetry: (@MainActor () -> Void)?
    private let onFollowingChange: (@MainActor (Bool) async throws -> Void)?
    private let onRepliesSeen: (@MainActor (Int) -> Void)?
    private let root: Root
    private let typing: Typing
    private let composer: Composer
    private let headerActions: Actions
    private let reply: (Item, MonarchVirtualMessageListContext<Item>) -> Reply

    @State private var isFollowBusy = false

    @Environment(\.monarchTheme) private var theme
    @Environment(\.scenePhase) private var scenePhase

    private typealias Tokens = MonarchThreadPanelTokens
    private typealias Strings = MonarchThreadPanelStrings

    /// A thread panel with a typing indicator and extra toolbar buttons.
    ///
    /// - Parameters:
    ///   - replies: The loaded replies, oldest first.
    ///   - replyCount: The number of replies for the divider; can exceed the loaded replies. The loaded count by default.
    ///   - status: Ready shows the replies; loading placeholders; error a failure with Try again; deleted a notice with Close.
    ///   - isFollowing: Whether the person follows the thread: the Follow button is selected and reads Following.
    ///   - notice: A note after the replies, such as "Showing the first 500 replies."
    ///   - seenDelay: How long the replies stay on screen before `onRepliesSeen`.
    ///   - now: Fixes "now" for Today and Yesterday. The clock by default.
    ///   - labels: Replacements for the built-in strings, and optionally the list's.
    ///   - onClose: Adds Close to the toolbar and to a deleted thread, for a sheet or an inspector.
    ///   - onRetry: Adds Try again when the thread could not be loaded.
    ///   - onFollowingChange: Follows (`true`) or unfollows. Adds the Follow button while ready. Throw to announce a failure.
    ///   - onRepliesSeen: Receives the reply count once the replies have been on screen for `seenDelay`, and again when it changes: mark the thread read here.
    ///   - root: The message the thread answers, usually a ``MonarchMessageRow``.
    ///   - typing: Above the reply box, such as a ``MonarchTypingIndicator``.
    ///   - composer: The reply box, usually a ``MonarchMessageComposer``.
    ///   - headerActions: Further toolbar buttons, after Follow.
    ///   - reply: Builds one reply, usually a ``MonarchMessageRow`` with `isGrouped: context.isGrouped`.
    public init(
        _ replies: [Item],
        replyCount: Int? = nil,
        status: MonarchThreadPanelStatus = .ready,
        isFollowing: Bool = false,
        notice: String? = nil,
        seenDelay: Duration = .milliseconds(400),
        now: Date? = nil,
        labels: MonarchThreadPanelLabels = MonarchThreadPanelLabels(),
        onClose: (@MainActor () -> Void)? = nil,
        onRetry: (@MainActor () -> Void)? = nil,
        onFollowingChange: (@MainActor (Bool) async throws -> Void)? = nil,
        onRepliesSeen: (@MainActor (Int) -> Void)? = nil,
        @ViewBuilder root: () -> Root,
        @ViewBuilder typing: () -> Typing,
        @ViewBuilder composer: () -> Composer,
        @ViewBuilder headerActions: () -> Actions,
        @ViewBuilder reply: @escaping (Item, MonarchVirtualMessageListContext<Item>) -> Reply
    ) {
        self.replies = replies
        self.replyCount = replyCount
        self.status = status
        self.isFollowing = isFollowing
        self.notice = notice
        self.seenDelay = seenDelay
        self.now = now
        self.labels = labels
        self.onClose = onClose
        self.onRetry = onRetry
        self.onFollowingChange = onFollowingChange
        self.onRepliesSeen = onRepliesSeen
        self.root = root()
        self.typing = typing()
        self.composer = composer()
        self.headerActions = headerActions()
        self.reply = reply
    }

    private var count: Int { Tokens.count(replyCount: replyCount, loaded: replies.count) }

    /// The content of the view.
    public var body: some View {
        content
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(theme.colors.bgSurface)
            .safeAreaInset(edge: .bottom, spacing: 0) {
                bottomBar
            }
            .navigationTitle(labels.title ?? Strings.title)
            #if os(iOS)
                .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar { toolbar }
            .task(
                id: Tokens.seen(
                    count: count,
                    status: status,
                    isActive: scenePhase == .active,
                    hasAction: onRepliesSeen != nil
                )
            ) {
                await reportSeen()
            }
    }

    // MARK: - Parts

    @ViewBuilder private var content: some View {
        switch status {
        case .ready:
            MonarchVirtualMessageList(
                replies,
                now: now,
                labels: Tokens.listLabels(labels),
                header: {
                    VStack(alignment: .leading, spacing: 0) {
                        root
                        divider
                    }
                },
                footer: {
                    if let notice {
                        Text(notice)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors[role: Tokens.muted])
                            .padding(.horizontal, Tokens.horizontalPadding)
                            .padding(.vertical, Tokens.statePadding)
                    }
                },
                row: reply
            )
            .environment(\.monarchHidesDayDividers, true)
        case .loading:
            MonarchLoadingState(labels.loading ?? Strings.loading, rows: Tokens.placeholderRows)
                .padding(Tokens.statePadding)
        case .error:
            failure
                .padding(Tokens.statePadding)
        case .deleted:
            deleted
                .padding(Tokens.statePadding)
        }
    }

    private var divider: some View {
        HStack(spacing: Tokens.dividerSpacing) {
            Rectangle().fill(theme.colors[role: Tokens.line]).frame(height: MonarchShapes.borderWidth)
            Text(Tokens.replyCountText(count, labels: labels))
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: Tokens.muted])
                .multilineTextAlignment(.center)
                .layoutPriority(1)
            Rectangle().fill(theme.colors[role: Tokens.line]).frame(height: MonarchShapes.borderWidth)
        }
        .padding(.horizontal, Tokens.horizontalPadding)
        .padding(.vertical, Tokens.dividerPadding)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }

    @ViewBuilder private var failure: some View {
        let message = labels.error ?? Strings.error
        if let onRetry {
            MonarchInlineErrorRetry(message, onRetry: onRetry)
        } else {
            Text(message)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors[role: Tokens.failure])
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var deleted: some View {
        let message = labels.deleted ?? Strings.deleted
        return VStack(alignment: .leading, spacing: Tokens.statePadding) {
            Text(message)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors[role: Tokens.muted])
                .fixedSize(horizontal: false, vertical: true)
            if let onClose {
                MonarchButton(labels.close ?? Strings.close, variant: .secondary, size: .sm, action: onClose)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .onAppear { announce(message) }
    }

    private var bottomBar: some View {
        VStack(alignment: .leading, spacing: Tokens.bottomSpacing) {
            typing
            composer
        }
        .padding(.horizontal, Tokens.horizontalPadding)
        .padding(.vertical, Tokens.bottomPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.colors.bgSurface)
        .overlay(alignment: .top) {
            Rectangle().fill(theme.colors[role: Tokens.line]).frame(height: MonarchShapes.borderWidth)
        }
    }

    @ToolbarContentBuilder private var toolbar: some ToolbarContent {
        ToolbarItemGroup(placement: .primaryAction) {
            if Tokens.showsFollow(status: status, hasAction: onFollowingChange != nil) {
                let button = Tokens.followButton(isFollowing: isFollowing, labels: labels)
                // A small outlined bell with its word, as the web draws it, not a filled selected disc.
                Button {
                    setFollowing(!isFollowing)
                } label: {
                    Label(button.title, systemImage: button.symbol)
                        .labelStyle(.titleAndIcon)
                        .foregroundStyle(isFollowing ? theme.colors.accentStrong : theme.colors.textDefault)
                }
                .buttonStyle(.monarch(.ghost, size: .sm))
                .accessibilityAddTraits(isFollowing ? .isSelected : [])
                .disabled(isFollowBusy)
            }
            headerActions
        }
        .monarchFlat()
        if let onClose {
            // An X at the far end of the header, as the web has, named Close for VoiceOver.
            ToolbarItem(placement: .primaryAction) {
                Button(action: onClose) {
                    MonarchIcon(systemName: Tokens.closeSymbol, size: .sm)
                        .foregroundStyle(theme.colors.textDefault)
                        .monarchMinimumHitTarget()
                }
                .accessibilityLabel(labels.close ?? Strings.close)
            }
            .monarchFlat()
        }
    }

    // MARK: - Behaviour

    private func setFollowing(_ value: Bool) {
        guard let onFollowingChange, !isFollowBusy else { return }
        isFollowBusy = true
        Task { @MainActor in
            do {
                try await onFollowingChange(value)
                announce(Tokens.followNotice(isFollowing: value, didFail: false, labels: labels))
            } catch {
                announce(Tokens.followNotice(isFollowing: value, didFail: true, labels: labels))
            }
            isFollowBusy = false
        }
    }

    private func reportSeen() async {
        let seen = Tokens.seen(
            count: count,
            status: status,
            isActive: scenePhase == .active,
            hasAction: onRepliesSeen != nil
        )
        guard seen.isArmed else { return }
        do {
            try await Task.sleep(for: seenDelay)
        } catch {
            return
        }
        onRepliesSeen?(seen.count)
    }

    private func announce(_ text: String) {
        AccessibilityNotification.Announcement(text).post()
    }
}

extension MonarchThreadPanel where Typing == EmptyView, Actions == EmptyView {
    /// A thread panel without a typing indicator or extra toolbar buttons.
    ///
    /// - Parameters:
    ///   - replies: The loaded replies, oldest first.
    ///   - replyCount: The number of replies for the divider; can exceed the loaded replies. The loaded count by default.
    ///   - status: Ready shows the replies; loading placeholders; error a failure with Try again; deleted a notice with Close.
    ///   - isFollowing: Whether the person follows the thread.
    ///   - notice: A note after the replies.
    ///   - seenDelay: How long the replies stay on screen before `onRepliesSeen`.
    ///   - now: Fixes "now" for Today and Yesterday. The clock by default.
    ///   - labels: Replacements for the built-in strings.
    ///   - onClose: Adds Close to the toolbar and to a deleted thread.
    ///   - onRetry: Adds Try again when the thread could not be loaded.
    ///   - onFollowingChange: Follows or unfollows. Adds the Follow button while ready.
    ///   - onRepliesSeen: Receives the reply count once the replies have been seen.
    ///   - root: The message the thread answers.
    ///   - composer: The reply box.
    ///   - reply: Builds one reply.
    public init(
        _ replies: [Item],
        replyCount: Int? = nil,
        status: MonarchThreadPanelStatus = .ready,
        isFollowing: Bool = false,
        notice: String? = nil,
        seenDelay: Duration = .milliseconds(400),
        now: Date? = nil,
        labels: MonarchThreadPanelLabels = MonarchThreadPanelLabels(),
        onClose: (@MainActor () -> Void)? = nil,
        onRetry: (@MainActor () -> Void)? = nil,
        onFollowingChange: (@MainActor (Bool) async throws -> Void)? = nil,
        onRepliesSeen: (@MainActor (Int) -> Void)? = nil,
        @ViewBuilder root: () -> Root,
        @ViewBuilder composer: () -> Composer,
        @ViewBuilder reply: @escaping (Item, MonarchVirtualMessageListContext<Item>) -> Reply
    ) {
        self.init(
            replies,
            replyCount: replyCount,
            status: status,
            isFollowing: isFollowing,
            notice: notice,
            seenDelay: seenDelay,
            now: now,
            labels: labels,
            onClose: onClose,
            onRetry: onRetry,
            onFollowingChange: onFollowingChange,
            onRepliesSeen: onRepliesSeen,
            root: root,
            typing: { EmptyView() },
            composer: composer,
            headerActions: { EmptyView() },
            reply: reply
        )
    }
}

#if DEBUG
    private struct MonarchThreadPanelPreviewReply: MonarchVirtualMessageListItem {
        let id: Int
        let time: Date
        let authorID: String?
        let text: String
    }

    #Preview("MonarchThreadPanel") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        let replies = (0..<4).map {
            MonarchThreadPanelPreviewReply(
                id: $0,
                time: now.addingTimeInterval(TimeInterval($0 * 60 - 600)),
                authorID: $0 < 2 ? "ben" : "ana",
                text: "Reply \($0)"
            )
        }
        MonarchPreviewMatrix {
            NavigationStack {
                MonarchThreadPanel(
                    replies,
                    isFollowing: true,
                    now: now,
                    onClose: {},
                    onFollowingChange: { _ in },
                    root: {
                        MonarchMessageRow(
                            MonarchMessageRowAuthor("Lee Park"),
                            time: now.addingTimeInterval(-900),
                            now: now
                        ) {
                            Text("Who can review the release notes?")
                        }
                    },
                    composer: { Text("Reply box") },
                    reply: { reply, context in
                        MonarchMessageRow(
                            MonarchMessageRowAuthor(reply.authorID == "ana" ? "Ana Lopez" : "Ben Okafor"),
                            time: reply.time,
                            now: now,
                            isGrouped: context.isGrouped
                        ) {
                            Text(reply.text)
                        }
                    }
                )
            }
            .frame(height: 480)
        }
    }
#endif
