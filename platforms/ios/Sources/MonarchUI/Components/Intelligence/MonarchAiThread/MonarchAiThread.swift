import SwiftUI

/// The scrolling list of an AI conversation: messages oldest first with day
/// labels, pinned to the newest, with Jump to latest, Load earlier, a
/// thinking indicator, connection notices and one announcer for the whole
/// conversation.
///
/// ```swift
/// MonarchAiThread(
///     conversation.messages,
///     status: conversation.status,
///     error: conversation.error,
///     isOffline: !conversation.isOnline,
///     queuedCount: conversation.queued.count,
///     isSlow: conversation.isSlow,
///     onStop: { conversation.stop() }
/// )
/// .safeAreaInset(edge: .bottom) {
///     MonarchAiPromptInput("Message the assistant", text: $prompt, isBusy: conversation.isBusy) { text in
///         conversation.send(text)
///     }
///     .padding()
/// }
/// ```
///
/// It is a `ScrollView` with a `LazyVStack`, so only rows near the screen
/// are built, and `defaultScrollAnchor(.bottom)`, so it opens on the newest
/// message. While the newest row is on screen, a new message, the thinking
/// indicator and a growing reply keep it in view; scrolled up, nothing moves
/// and a Jump button counts the new messages. Each message is a
/// ``MonarchAiMessage`` unless you draw it with `message`. Day labels are
/// ``MonarchAiContextDivider``s in the environment's calendar, time zone and
/// locale; pass `now` and set `.environment(\.timeZone, …)` for stable
/// labels in tests. Older history loads with ``MonarchLoadMore``. The prompt
/// input is not part of the thread: put it under it, in
/// `.safeAreaInset(edge: .bottom)`.
///
/// Accessibility: the list is a container named "Conversation". With
/// `announces`, the thread is the only announcer: "Thinking…" when a reply
/// is submitted, then the reply in whole sentences at most once per
/// `announceInterval`, then "Response complete" or "Response stopped";
/// messages get `announces == false` in their context, and replies that were
/// already finished are not read. A conversation error that no message shows
/// is announced with high priority. Notices are announced when they change.
/// The Magic Tap (two-finger double tap) and the Escape key call `onStop`
/// while a reply runs. Jump to latest reads its count of new messages and
/// scrolls without animation under Reduce Motion.
@_spi(Experimental) public struct MonarchAiThread<Message: View, Empty: View>: View {
    private let messages: [MonarchAiConversationMessage]
    private let status: MonarchAiConversationStatus
    private let error: MonarchAiConversationError?
    private let isOffline: Bool
    private let queuedCount: Int
    private let isSlow: Bool
    private let hasEarlier: Bool
    private let isLoadingEarlier: Bool
    private let showsDaySeparators: Bool
    private let now: Date?
    private let announces: Bool
    private let announceInterval: Duration
    private let labels: MonarchAiThreadLabels
    private let onStop: (@MainActor () -> Void)?
    private let onLoadEarlier: (@MainActor () -> Void)?
    private let empty: Empty
    private let message: (MonarchAiConversationMessage, MonarchAiThreadMessageContext) -> Message

    @State private var isAtBottom = true
    @State private var lastSeenID: String?
    @State private var announcer = MonarchAiThreadAnnouncer()
    @State private var speech = MonarchAiThreadSpeech()
    @State private var retryAt: Date?

    @Environment(\.monarchNow) private var clockNow
    @Environment(\.monarchTheme) private var theme
    @Environment(\.calendar) private var calendar
    @Environment(\.timeZone) private var timeZone
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private typealias Tokens = MonarchAiThreadTokens
    private typealias Strings = MonarchAiThreadStrings

    /// A thread with your own message rows and empty content.
    ///
    /// - Parameters:
    ///   - messages: The visible conversation, oldest first: ``MonarchAiConversation/messages``.
    ///   - status: `submitted` shows the thinking indicator; `submitted` and `streaming` let Escape and the Magic Tap stop.
    ///   - error: Announced with high priority when the newest message has no error part of its own.
    ///   - isOffline: Shows the offline notice under the messages.
    ///   - queuedCount: How many messages wait for the connection, named in the notices.
    ///   - isSlow: Shows "taking longer than usual" while a reply runs.
    ///   - hasEarlier: Older messages exist. With `onLoadEarlier`, "Load earlier messages" is the top row.
    ///   - isLoadingEarlier: The load row shows progress.
    ///   - showsDaySeparators: A day label above the first message of each calendar day.
    ///   - now: The reference time for "Today" and "Yesterday". The clock by default.
    ///   - announces: Whether the thread announces for the whole conversation. Off, each message announces itself.
    ///   - announceInterval: The least time between two announcements while a reply streams.
    ///   - labels: Replacements for the built-in strings.
    ///   - onStop: Stops the running reply, from Escape or the Magic Tap.
    ///   - onLoadEarlier: Loads older messages. Prepend them to `messages`.
    ///   - empty: Shown instead of the list when there are no messages, nothing to load and no submitted reply.
    ///   - message: Draws one message, given its index, whether it is the newest and whether it announces itself.
    public init(
        _ messages: [MonarchAiConversationMessage],
        status: MonarchAiConversationStatus = .idle,
        error: MonarchAiConversationError? = nil,
        isOffline: Bool = false,
        queuedCount: Int = 0,
        isSlow: Bool = false,
        hasEarlier: Bool = false,
        isLoadingEarlier: Bool = false,
        showsDaySeparators: Bool = true,
        now: Date? = nil,
        announces: Bool = true,
        announceInterval: Duration = .seconds(1),
        labels: MonarchAiThreadLabels = MonarchAiThreadLabels(),
        onStop: (@MainActor () -> Void)? = nil,
        onLoadEarlier: (@MainActor () -> Void)? = nil,
        @ViewBuilder empty: () -> Empty,
        @ViewBuilder message: @escaping (MonarchAiConversationMessage, MonarchAiThreadMessageContext) -> Message
    ) {
        self.messages = messages
        self.status = status
        self.error = error
        self.isOffline = isOffline
        self.queuedCount = queuedCount
        self.isSlow = isSlow
        self.hasEarlier = hasEarlier
        self.isLoadingEarlier = isLoadingEarlier
        self.showsDaySeparators = showsDaySeparators
        self.now = now
        self.announces = announces
        self.announceInterval = announceInterval
        self.labels = labels
        self.onStop = onStop
        self.onLoadEarlier = onLoadEarlier
        self.empty = empty()
        self.message = message
    }

    /// The content of the view.
    public var body: some View {
        let notice = Tokens.notice(isOffline: isOffline, queuedCount: queuedCount, isSlow: isSlow, status: status)
        let noticeText = notice.map { Tokens.noticeText($0, labels: labels) }
        let errorText = Tokens.errorAnnouncement(error, last: messages.last, labels: labels)
        return VStack(spacing: 0) {
            if Tokens.showsEmpty(
                count: messages.count,
                hasEarlier: hasEarlier,
                canLoadEarlier: onLoadEarlier != nil,
                status: status
            ) {
                empty.frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                list
            }
            if let notice, let noticeText {
                MonarchInlineNotice(noticeText, systemImage: notice.systemImage, tone: notice.tone)
                    .padding(.horizontal, Tokens.contentPadding)
                    .padding(.vertical, Tokens.noticePadding)
            }
        }
        .onChange(of: noticeText) { _, text in
            if let text { AccessibilityNotification.Announcement(text).post() }
        }
        .onChange(of: errorText) { _, text in
            guard announces, let text else { return }
            var urgent = AttributedString(text)
            urgent.accessibilitySpeechAnnouncementPriority = .high
            AccessibilityNotification.Announcement(urgent).post()
        }
        .onChange(of: announceKey, initial: true) { announce() }
        .task(id: retryAt) {
            guard let retryAt else { return }
            try? await Task.sleep(for: .seconds(max(0, retryAt.timeIntervalSinceNow)))
            guard !Task.isCancelled else { return }
            self.retryAt = nil
            announce()
        }
    }

    // MARK: - List

    private var list: some View {
        var zoned = calendar
        zoned.timeZone = timeZone
        let days = showsDaySeparators ? Tokens.dayStarts(messages, calendar: zoned) : []
        let reference = now ?? clockNow ?? Date()
        let lastIndex = messages.count - 1
        return ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: Tokens.rowSpacing) {
                    if Tokens.showsLoadEarlier(
                        hasEarlier: hasEarlier,
                        isLoadingEarlier: isLoadingEarlier,
                        canLoadEarlier: onLoadEarlier != nil
                    ) {
                        loadEarlier
                    }
                    ForEach(Array(messages.enumerated()), id: \.element.id) { index, item in
                        VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
                            if index < days.count, let day = days[index] {
                                MonarchAiContextDivider(
                                    .day,
                                    date: day,
                                    now: reference,
                                    label: Tokens.dayLabel(day, now: reference, calendar: zoned, labels: labels)
                                )
                            }
                            message(
                                item,
                                MonarchAiThreadMessageContext(
                                    index: index,
                                    isLast: index == lastIndex,
                                    announces: !announces
                                )
                            )
                        }
                        .id(item.id)
                    }
                    if status == .submitted {
                        MonarchAiThinkingIndicator(label: labels.thinking, isAnnounced: !announces)
                    }
                    Color.clear
                        .frame(height: 1)
                        .id(Tokens.bottomID)
                        .onAppear { isAtBottom = true }
                        .onDisappear { isAtBottom = false }
                        .accessibilityHidden(true)
                }
                .padding(Tokens.contentPadding)
            }
            .defaultScrollAnchor(.bottom)
            .overlay(alignment: .bottomTrailing) {
                jumpButton(proxy: proxy)
            }
            .onChange(of: messages.last?.id) { _, newLast in
                guard isAtBottom else { return }
                lastSeenID = newLast
                withAnimation(theme.motion.animation(.base, reduceMotion: reduceMotion)) {
                    proxy.scrollTo(Tokens.bottomID, anchor: .bottom)
                }
            }
            .onChange(of: growthKey) {
                // A growing reply or a new indicator stays in view, without animation.
                guard isAtBottom else { return }
                proxy.scrollTo(Tokens.bottomID, anchor: .bottom)
            }
            .onChange(of: isAtBottom) { _, atBottom in
                if atBottom { lastSeenID = messages.last?.id }
            }
            .onAppear { lastSeenID = messages.last?.id }
            .onKeyPress(.escape) {
                guard Tokens.isRunning(status), let onStop else { return .ignored }
                onStop()
                return .handled
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.label ?? Strings.label)
        .magicTapToStop {
            if Tokens.isRunning(status) { onStop?() }
        }
    }

    private var loadEarlier: some View {
        MonarchLoadMore(
            hasMore: hasEarlier,
            isLoading: isLoadingEarlier,
            showsEnd: false,
            labels: MonarchLoadMoreLabels(
                loadMore: labels.loadEarlier ?? Strings.loadEarlier,
                loading: labels.loadingEarlier ?? Strings.loadingEarlier
            ),
            onLoadMore: { onLoadEarlier?() }
        )
        .frame(maxWidth: .infinity)
    }

    @ViewBuilder private func jumpButton(proxy: ScrollViewProxy) -> some View {
        if Tokens.showsJump(isAtBottom: isAtBottom, count: messages.count) {
            let count = Tokens.newCount(ids: messages.map(\.id), lastSeenID: lastSeenID)
            MonarchButton(
                Tokens.jumpTitle(newCount: count, labels: labels),
                systemImage: Tokens.jumpSymbol,
                variant: .secondary,
                size: .sm
            ) {
                withAnimation(theme.motion.animation(.base, reduceMotion: reduceMotion)) {
                    proxy.scrollTo(Tokens.bottomID, anchor: .bottom)
                }
            }
            .accessibilityLabel(Tokens.jumpAccessibilityLabel(newCount: count, labels: labels))
            .padding(Tokens.jumpPadding)
        }
    }

    // MARK: - Announcer

    /// What the announcer and the pinning follow.
    private struct AnnounceKey: Equatable {
        let lastID: String?
        let lastText: String
        let isStreaming: Bool
        let status: MonarchAiConversationStatus
    }

    private var announceKey: AnnounceKey {
        let last = messages.last
        return AnnounceKey(
            lastID: last?.id,
            lastText: last?.text ?? "",
            isStreaming: last?.isStreaming ?? false,
            status: status
        )
    }

    /// What makes the newest row taller.
    private struct GrowthKey: Equatable {
        let parts: Int
        let textLength: Int
        let status: MonarchAiConversationStatus
        let hasNotice: Bool
    }

    private var growthKey: GrowthKey {
        let last = messages.last
        return GrowthKey(
            parts: last?.parts.count ?? 0,
            textLength: last?.text.utf16.count ?? 0,
            status: status,
            hasNotice: Tokens.notice(isOffline: isOffline, queuedCount: queuedCount, isSlow: isSlow, status: status)
                != nil
        )
    }

    private func announce() {
        guard announces else { return }
        let snapshot = messages.last.map { last in
            MonarchAiThreadAnnouncer.Snapshot(
                id: last.id,
                isAssistant: last.role == .assistant,
                spoken: last.role == .assistant
                    ? speech.spoken(
                        last,
                        codeBlock: labels.codeBlock ?? Strings.codeBlock,
                        table: labels.table ?? Strings.table
                    )
                    : "",
                isStreaming: last.isStreaming
            )
        }
        let outcome = announcer.update(
            message: snapshot,
            status: status,
            words: Tokens.words(labels),
            interval: MonarchAiThreadAnnouncer.interval(announceInterval),
            now: Date()
        )
        switch outcome {
        case .silent:
            break
        case .announce(let text):
            AccessibilityNotification.Announcement(text).post()
        case .wait(let until):
            if retryAt == nil { retryAt = until }
        }
    }
}

extension MonarchAiThread where Empty == EmptyView {
    /// A thread with your own message rows and nothing shown while empty.
    ///
    /// - Parameters:
    ///   - messages: The visible conversation, oldest first: ``MonarchAiConversation/messages``.
    ///   - status: `submitted` shows the thinking indicator; `submitted` and `streaming` let Escape and the Magic Tap stop.
    ///   - error: Announced with high priority when the newest message has no error part of its own.
    ///   - isOffline: Shows the offline notice under the messages.
    ///   - queuedCount: How many messages wait for the connection, named in the notices.
    ///   - isSlow: Shows "taking longer than usual" while a reply runs.
    ///   - hasEarlier: Older messages exist. With `onLoadEarlier`, "Load earlier messages" is the top row.
    ///   - isLoadingEarlier: The load row shows progress.
    ///   - showsDaySeparators: A day label above the first message of each calendar day.
    ///   - now: The reference time for "Today" and "Yesterday". The clock by default.
    ///   - announces: Whether the thread announces for the whole conversation. Off, each message announces itself.
    ///   - announceInterval: The least time between two announcements while a reply streams.
    ///   - labels: Replacements for the built-in strings.
    ///   - onStop: Stops the running reply, from Escape or the Magic Tap.
    ///   - onLoadEarlier: Loads older messages. Prepend them to `messages`.
    ///   - message: Draws one message, given its index, whether it is the newest and whether it announces itself.
    public init(
        _ messages: [MonarchAiConversationMessage],
        status: MonarchAiConversationStatus = .idle,
        error: MonarchAiConversationError? = nil,
        isOffline: Bool = false,
        queuedCount: Int = 0,
        isSlow: Bool = false,
        hasEarlier: Bool = false,
        isLoadingEarlier: Bool = false,
        showsDaySeparators: Bool = true,
        now: Date? = nil,
        announces: Bool = true,
        announceInterval: Duration = .seconds(1),
        labels: MonarchAiThreadLabels = MonarchAiThreadLabels(),
        onStop: (@MainActor () -> Void)? = nil,
        onLoadEarlier: (@MainActor () -> Void)? = nil,
        @ViewBuilder message: @escaping (MonarchAiConversationMessage, MonarchAiThreadMessageContext) -> Message
    ) {
        self.init(
            messages,
            status: status,
            error: error,
            isOffline: isOffline,
            queuedCount: queuedCount,
            isSlow: isSlow,
            hasEarlier: hasEarlier,
            isLoadingEarlier: isLoadingEarlier,
            showsDaySeparators: showsDaySeparators,
            now: now,
            announces: announces,
            announceInterval: announceInterval,
            labels: labels,
            onStop: onStop,
            onLoadEarlier: onLoadEarlier,
            empty: { EmptyView() },
            message: message
        )
    }
}

extension MonarchAiThread where Message == MonarchAiMessage<EmptyView, EmptyView> {
    /// A thread of ``MonarchAiMessage`` rows with empty content, such as a ``MonarchAiWelcome``.
    ///
    /// - Parameters:
    ///   - messages: The visible conversation, oldest first: ``MonarchAiConversation/messages``.
    ///   - status: `submitted` shows the thinking indicator; `submitted` and `streaming` let Escape and the Magic Tap stop.
    ///   - error: Announced with high priority when the newest message has no error part of its own.
    ///   - isOffline: Shows the offline notice under the messages.
    ///   - queuedCount: How many messages wait for the connection, named in the notices.
    ///   - isSlow: Shows "taking longer than usual" while a reply runs.
    ///   - hasEarlier: Older messages exist. With `onLoadEarlier`, "Load earlier messages" is the top row.
    ///   - isLoadingEarlier: The load row shows progress.
    ///   - showsDaySeparators: A day label above the first message of each calendar day.
    ///   - now: The reference time for "Today" and "Yesterday". The clock by default.
    ///   - announces: Whether the thread announces for the whole conversation. Off, each message announces itself.
    ///   - announceInterval: The least time between two announcements while a reply streams.
    ///   - labels: Replacements for the built-in strings.
    ///   - onStop: Stops the running reply, from Escape or the Magic Tap.
    ///   - onLoadEarlier: Loads older messages. Prepend them to `messages`.
    ///   - empty: Shown instead of the list when there are no messages, nothing to load and no submitted reply.
    public init(
        _ messages: [MonarchAiConversationMessage],
        status: MonarchAiConversationStatus = .idle,
        error: MonarchAiConversationError? = nil,
        isOffline: Bool = false,
        queuedCount: Int = 0,
        isSlow: Bool = false,
        hasEarlier: Bool = false,
        isLoadingEarlier: Bool = false,
        showsDaySeparators: Bool = true,
        now: Date? = nil,
        announces: Bool = true,
        announceInterval: Duration = .seconds(1),
        labels: MonarchAiThreadLabels = MonarchAiThreadLabels(),
        onStop: (@MainActor () -> Void)? = nil,
        onLoadEarlier: (@MainActor () -> Void)? = nil,
        @ViewBuilder empty: () -> Empty
    ) {
        self.init(
            messages,
            status: status,
            error: error,
            isOffline: isOffline,
            queuedCount: queuedCount,
            isSlow: isSlow,
            hasEarlier: hasEarlier,
            isLoadingEarlier: isLoadingEarlier,
            showsDaySeparators: showsDaySeparators,
            now: now,
            announces: announces,
            announceInterval: announceInterval,
            labels: labels,
            onStop: onStop,
            onLoadEarlier: onLoadEarlier,
            empty: empty,
            message: { item, context in MonarchAiMessage(item, announces: context.announces) }
        )
    }
}

extension MonarchAiThread where Message == MonarchAiMessage<EmptyView, EmptyView>, Empty == EmptyView {
    /// A thread of ``MonarchAiMessage`` rows.
    ///
    /// - Parameters:
    ///   - messages: The visible conversation, oldest first: ``MonarchAiConversation/messages``.
    ///   - status: `submitted` shows the thinking indicator; `submitted` and `streaming` let Escape and the Magic Tap stop.
    ///   - error: Announced with high priority when the newest message has no error part of its own.
    ///   - isOffline: Shows the offline notice under the messages.
    ///   - queuedCount: How many messages wait for the connection, named in the notices.
    ///   - isSlow: Shows "taking longer than usual" while a reply runs.
    ///   - hasEarlier: Older messages exist. With `onLoadEarlier`, "Load earlier messages" is the top row.
    ///   - isLoadingEarlier: The load row shows progress.
    ///   - showsDaySeparators: A day label above the first message of each calendar day.
    ///   - now: The reference time for "Today" and "Yesterday". The clock by default.
    ///   - announces: Whether the thread announces for the whole conversation. Off, each message announces itself.
    ///   - announceInterval: The least time between two announcements while a reply streams.
    ///   - labels: Replacements for the built-in strings.
    ///   - onStop: Stops the running reply, from Escape or the Magic Tap.
    ///   - onLoadEarlier: Loads older messages. Prepend them to `messages`.
    public init(
        _ messages: [MonarchAiConversationMessage],
        status: MonarchAiConversationStatus = .idle,
        error: MonarchAiConversationError? = nil,
        isOffline: Bool = false,
        queuedCount: Int = 0,
        isSlow: Bool = false,
        hasEarlier: Bool = false,
        isLoadingEarlier: Bool = false,
        showsDaySeparators: Bool = true,
        now: Date? = nil,
        announces: Bool = true,
        announceInterval: Duration = .seconds(1),
        labels: MonarchAiThreadLabels = MonarchAiThreadLabels(),
        onStop: (@MainActor () -> Void)? = nil,
        onLoadEarlier: (@MainActor () -> Void)? = nil
    ) {
        self.init(
            messages,
            status: status,
            error: error,
            isOffline: isOffline,
            queuedCount: queuedCount,
            isSlow: isSlow,
            hasEarlier: hasEarlier,
            isLoadingEarlier: isLoadingEarlier,
            showsDaySeparators: showsDaySeparators,
            now: now,
            announces: announces,
            announceInterval: announceInterval,
            labels: labels,
            onStop: onStop,
            onLoadEarlier: onLoadEarlier,
            empty: { EmptyView() }
        )
    }
}

extension View {
    /// The VoiceOver Magic Tap, which exists on iOS only; the macOS host build skips it.
    @ViewBuilder fileprivate func magicTapToStop(_ action: @escaping () -> Void) -> some View {
        #if os(iOS)
            accessibilityAction(.magicTap, action)
        #else
            self
        #endif
    }
}

#if DEBUG
    #Preview("MonarchAiThread") {
        let now = Date(timeIntervalSince1970: 1_773_144_000)
        let messages = [
            MonarchAiConversationMessage(
                id: "u1",
                role: .user,
                parts: [.text(MonarchAiTextPart(text: "When is invoice 42 due?"))],
                createdAt: MonarchAiConversationMessage.timestamp(now.addingTimeInterval(-86_400))
            ),
            MonarchAiConversationMessage(
                id: "a1",
                role: .assistant,
                parts: [.text(MonarchAiTextPart(text: "Invoice 42 is due on **4 December**."))],
                createdAt: MonarchAiConversationMessage.timestamp(now.addingTimeInterval(-86_390))
            ),
            MonarchAiConversationMessage(
                id: "u2",
                role: .user,
                parts: [.text(MonarchAiTextPart(text: "Remind the client today."))],
                createdAt: MonarchAiConversationMessage.timestamp(now.addingTimeInterval(-60))
            ),
        ]
        MonarchPreviewMatrix {
            MonarchAiThread(messages, status: .submitted, isSlow: true, hasEarlier: true, now: now, onLoadEarlier: {})
                .frame(height: 420)
            MonarchAiThread(messages, isOffline: true, queuedCount: 1, now: now)
                .frame(height: 320)
        }
    }
#endif
