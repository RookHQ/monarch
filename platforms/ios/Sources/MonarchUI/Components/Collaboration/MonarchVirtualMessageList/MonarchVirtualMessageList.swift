import SwiftUI

/// A conversation's messages, pinned to the newest at the bottom, with day
/// separators, a "New messages" divider, grouping of consecutive messages,
/// Load older at the top and Jump to latest.
///
/// ```swift
/// MonarchVirtualMessageList(
///     conversation.messages,
///     firstUnreadID: conversation.firstUnreadID,
///     hasOlder: conversation.hasOlder,
///     isLoadingOlder: conversation.isLoadingOlder,
///     onLoadOlder: { conversation.loadOlder() },
///     onAtBottomChange: { if $0 { conversation.markRead() } },
///     row: { message, context in
///         MonarchMessageRow(
///             MonarchMessageRowAuthor(message.author),
///             time: message.time,
///             isGrouped: context.isGrouped,
///             isHighlighted: context.isHighlighted
///         ) {
///             Text(message.text)
///         }
///     }
/// )
/// ```
///
/// It is a `ScrollView` with a `LazyVStack`, so only rows near the screen
/// are built, and `defaultScrollAnchor(.bottom)`, so it opens on the newest
/// message and stays still when older messages are added above. Messages
/// are oldest first and conform to ``MonarchVirtualMessageListItem``. The
/// app renders each row, usually a ``MonarchMessageRow``, and receives
/// whether it continues a group. When new messages arrive while the reader
/// is at the bottom, the list follows them; otherwise a Jump button counts
/// them. Older history loads with ``MonarchLoadMore`` when the top comes
/// into view. Pass `now`, a fixed `Locale` and `Calendar` for stable day
/// separators in tests.
///
/// Accessibility: the list is a container named "Messages". Day separators
/// and the unread divider are headers, so the VoiceOver rotor moves between
/// days. Arriving messages are announced ("2 new messages"). Placeholder
/// rows read "Loading messages". The Jump button is a standard button of at
/// least 44 points and scrolls without animation under Reduce Motion.
public struct MonarchVirtualMessageList<
    Item: MonarchVirtualMessageListItem,
    Row: View,
    Header: View,
    Footer: View
>: View {
    private let items: [Item]
    private let firstUnreadID: Item.ID?
    private let highlightedID: Item.ID?
    private let scrollToID: Item.ID?
    private let hasOlder: Bool
    private let isLoadingOlder: Bool
    private let isOlderFailed: Bool
    private let isLoading: Bool
    private let errorMessage: String?
    private let groupWindow: TimeInterval
    private let now: Date?
    private let stickToBottom: Bool
    private let showsJumpToLatest: Bool
    private let labels: MonarchVirtualMessageListLabels
    private let onLoadOlder: (@MainActor () -> Void)?
    private let onRetry: (@MainActor () -> Void)?
    private let onAtBottomChange: (@MainActor (Bool) -> Void)?
    private let header: Header
    private let footer: Footer
    private let row: (Item, MonarchVirtualMessageListContext<Item>) -> Row

    @State private var isAtBottom = true
    @State private var unseen = 0

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.calendar) private var calendar
    @Environment(\.monarchHidesDayDividers) private var hidesDayDividers
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private typealias Tokens = MonarchVirtualMessageListTokens
    private typealias Strings = MonarchVirtualMessageListStrings

    /// A message list with a header above the first message and a footer under the last.
    ///
    /// - Parameters:
    ///   - items: The messages, oldest first.
    ///   - firstUnreadID: The first unread message: a "New messages" divider goes above it.
    ///   - highlightedID: A message to highlight, given to its row as `context.isHighlighted`.
    ///   - scrollToID: A message to scroll to once it is loaded, for example from a link.
    ///   - hasOlder: Whether there is older history to load.
    ///   - isLoadingOlder: Whether older history is loading.
    ///   - isOlderFailed: Whether loading older history failed. Shows a retry.
    ///   - isLoading: Whether the first messages are loading. Shows placeholder rows while empty.
    ///   - errorMessage: Why the first load failed. Shown with a retry while empty.
    ///   - groupWindow: Consecutive messages by one author within this many seconds are grouped.
    ///   - now: Fixes "now" for Today and Yesterday. The clock by default.
    ///   - stickToBottom: Whether new messages are followed while the reader is at the bottom.
    ///   - showsJumpToLatest: Whether the Jump button shows when scrolled up.
    ///   - labels: Replacements for the built-in strings.
    ///   - onLoadOlder: Loads the previous page. Prepend the result to `items`. Adds the load row at the top.
    ///   - onRetry: Retries the first load after `errorMessage`.
    ///   - onAtBottomChange: Called when the reader reaches or leaves the newest message, for example to mark it read.
    ///   - header: Above the first message once all history is loaded, and shown alone while empty.
    ///   - footer: Under the last message, such as a typing indicator or a read receipt.
    ///   - row: Builds the row of one message, usually a ``MonarchMessageRow``.
    public init(
        _ items: [Item],
        firstUnreadID: Item.ID? = nil,
        highlightedID: Item.ID? = nil,
        scrollToID: Item.ID? = nil,
        hasOlder: Bool = false,
        isLoadingOlder: Bool = false,
        isOlderFailed: Bool = false,
        isLoading: Bool = false,
        errorMessage: String? = nil,
        groupWindow: TimeInterval = 300,
        now: Date? = nil,
        stickToBottom: Bool = true,
        showsJumpToLatest: Bool = true,
        labels: MonarchVirtualMessageListLabels = MonarchVirtualMessageListLabels(),
        onLoadOlder: (@MainActor () -> Void)? = nil,
        onRetry: (@MainActor () -> Void)? = nil,
        onAtBottomChange: (@MainActor (Bool) -> Void)? = nil,
        @ViewBuilder header: () -> Header,
        @ViewBuilder footer: () -> Footer,
        @ViewBuilder row: @escaping (Item, MonarchVirtualMessageListContext<Item>) -> Row
    ) {
        self.items = items
        self.firstUnreadID = firstUnreadID
        self.highlightedID = highlightedID
        self.scrollToID = scrollToID
        self.hasOlder = hasOlder
        self.isLoadingOlder = isLoadingOlder
        self.isOlderFailed = isOlderFailed
        self.isLoading = isLoading
        self.errorMessage = errorMessage
        self.groupWindow = groupWindow
        self.now = now
        self.stickToBottom = stickToBottom
        self.showsJumpToLatest = showsJumpToLatest
        self.labels = labels
        self.onLoadOlder = onLoadOlder
        self.onRetry = onRetry
        self.onAtBottomChange = onAtBottomChange
        self.header = header()
        self.footer = footer()
        self.row = row
    }

    /// The content of the view.
    public var body: some View {
        Group {
            switch Tokens.content(count: items.count, isLoading: isLoading, isFailed: errorMessage != nil) {
            case .loading:
                placeholder
            case .failed:
                MonarchErrorState(errorMessage ?? "", size: .sm, onRetry: onRetry)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .empty:
                ScrollView {
                    header.frame(maxWidth: .infinity, alignment: .leading)
                }
            case .messages:
                messages
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.list ?? Strings.list)
    }

    // MARK: - Messages

    private var messages: some View {
        let entries = Tokens.entries(
            items,
            firstUnreadID: firstUnreadID,
            groupWindow: groupWindow,
            now: now ?? Date(),
            calendar: calendar,
            locale: locale,
            labels: labels
        )
        return ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 0) {
                    top
                    ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                        let entry = entries[index]
                        VStack(alignment: .leading, spacing: 0) {
                            if let day = entry.day, !hidesDayDividers { dayDivider(day) }
                            if entry.isFirstUnread { unreadDivider }
                            row(
                                item,
                                MonarchVirtualMessageListContext(
                                    isGrouped: entry.isGrouped,
                                    index: index,
                                    previous: index > 0 ? items[index - 1] : nil,
                                    isHighlighted: highlightedID.map { $0 == item.id } ?? false
                                )
                            )
                        }
                        .id(item.id)
                    }
                    footer
                    Color.clear
                        .frame(height: 1)
                        .id(Tokens.bottomID)
                        .onAppear { isAtBottom = true }
                        .onDisappear { isAtBottom = false }
                        .accessibilityHidden(true)
                }
            }
            .defaultScrollAnchor(.bottom)
            .overlay(alignment: .bottomTrailing) {
                if Tokens.showsJump(isAtBottom: isAtBottom, showsJumpToLatest: showsJumpToLatest, count: items.count) {
                    MonarchButton(
                        Tokens.jumpTitle(unseen: unseen, labels: labels),
                        systemImage: Tokens.jumpSymbol,
                        variant: .secondary,
                        size: .sm
                    ) {
                        unseen = 0
                        withAnimation(theme.motion.animation(.base, reduceMotion: reduceMotion)) {
                            proxy.scrollTo(Tokens.bottomID, anchor: .bottom)
                        }
                    }
                    .padding(Tokens.jumpPadding)
                }
            }
            .onChange(of: items.last?.id) { oldLast, _ in
                arrived(Tokens.arrivedCount(oldLastID: oldLast, newIDs: items.map(\.id)), proxy: proxy)
            }
            .onChange(of: isAtBottom) { _, atBottom in
                if atBottom { unseen = 0 }
                onAtBottomChange?(atBottom)
            }
            .onChange(of: scrollToID) { _, id in
                scroll(to: id, proxy: proxy)
            }
            .onAppear {
                scroll(to: scrollToID, proxy: proxy)
            }
            .task {
                // A long history is laid out lazily, so the first scroll anchor lands short of the newest message.
                // Settle on the bottom while the rows around it are measured.
                guard stickToBottom, scrollToID == nil else { return }
                for delay in [0, 120, 350] {
                    try? await Task.sleep(for: .milliseconds(delay))
                    guard !Task.isCancelled, isAtBottom || delay == 0 || unseen == 0 else { return }
                    proxy.scrollTo(Tokens.bottomID, anchor: .bottom)
                }
            }
        }
    }

    @ViewBuilder private var top: some View {
        if Tokens.showsLoadOlder(
            hasOlder: hasOlder,
            isLoadingOlder: isLoadingOlder,
            isOlderFailed: isOlderFailed,
            hasAction: onLoadOlder != nil
        ) {
            MonarchLoadMore(
                hasMore: hasOlder,
                isLoading: isLoadingOlder,
                isFailed: isOlderFailed,
                showsEnd: false,
                autoLoads: true,
                labels: MonarchLoadMoreLabels(
                    loadMore: labels.loadOlder ?? Strings.loadOlder,
                    loading: labels.loadingOlder ?? Strings.loadingOlder,
                    failed: labels.olderFailed ?? Strings.olderFailed
                ),
                onLoadMore: { onLoadOlder?() }
            )
            .frame(maxWidth: .infinity)
        } else if !hasOlder {
            header
        }
    }

    /// The day separator: a hairline with the day in a bordered pill at its centre, as on the web.
    private func dayDivider(_ text: String) -> some View {
        HStack(spacing: 0) {
            Rectangle().fill(theme.colors[role: \.borderSubtle]).frame(height: MonarchShapes.borderWidth)
            Text(text)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: Tokens.dayText])
                .multilineTextAlignment(.center)
                .padding(.horizontal, theme.spacing.space3)
                .padding(.vertical, theme.spacing.space1)
                .background(Capsule().fill(theme.colors.bgSurface))
                .overlay(
                    Capsule().strokeBorder(theme.colors[role: \.borderSubtle], lineWidth: MonarchShapes.borderWidth)
                )
                .layoutPriority(1)
            Rectangle().fill(theme.colors[role: \.borderSubtle]).frame(height: MonarchShapes.borderWidth)
        }
        .padding(.horizontal, Tokens.horizontalPadding)
        .padding(.vertical, Tokens.dividerPadding)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }

    /// The unread divider: a hairline running to the title, set at the trailing edge in the accent tone.
    private var unreadDivider: some View {
        HStack(spacing: Tokens.dividerSpacing) {
            Rectangle().fill(theme.colors[role: \.borderStrong]).frame(height: MonarchShapes.borderWidth)
            Text(labels.newMessages ?? Strings.newMessages)
                .monarchTextStyle(.caption, weight: .semibold)
                .foregroundStyle(theme.colors[role: Tokens.unreadText])
                .layoutPriority(1)
        }
        .padding(.horizontal, Tokens.horizontalPadding)
        .padding(.vertical, Tokens.dividerPadding)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }

    private var placeholder: some View {
        // The text of these rows is never shown or read: the rows are
        // redacted and hidden behind one "Loading messages" element.
        let date = Date(timeIntervalSinceReferenceDate: 0)
        return VStack(alignment: .leading, spacing: 0) {
            ForEach(0..<Tokens.placeholderRows, id: \.self) { _ in
                MonarchMessageRow(MonarchMessageRowAuthor("Placeholder"), time: date, now: date) {
                    Text("Placeholder message text on one line")
                }
            }
        }
        .redacted(reason: .placeholder)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(labels.loading ?? Strings.loading)
    }

    // MARK: - Behaviour

    private func arrived(_ count: Int, proxy: ScrollViewProxy) {
        guard count > 0 else { return }
        if Tokens.follows(isAtBottom: isAtBottom, stickToBottom: stickToBottom) {
            withAnimation(theme.motion.animation(.base, reduceMotion: reduceMotion)) {
                proxy.scrollTo(Tokens.bottomID, anchor: .bottom)
            }
        } else {
            unseen += count
        }
        AccessibilityNotification.Announcement(Tokens.arrivedText(count, labels: labels)).post()
    }

    private func scroll(to id: Item.ID?, proxy: ScrollViewProxy) {
        guard let id, items.contains(where: { $0.id == id }) else { return }
        proxy.scrollTo(id, anchor: .center)
    }
}

extension MonarchVirtualMessageList where Header == EmptyView, Footer == EmptyView {
    /// A message list without a header or a footer.
    ///
    /// - Parameters:
    ///   - items: The messages, oldest first.
    ///   - firstUnreadID: The first unread message: a "New messages" divider goes above it.
    ///   - highlightedID: A message to highlight, given to its row as `context.isHighlighted`.
    ///   - scrollToID: A message to scroll to once it is loaded, for example from a link.
    ///   - hasOlder: Whether there is older history to load.
    ///   - isLoadingOlder: Whether older history is loading.
    ///   - isOlderFailed: Whether loading older history failed. Shows a retry.
    ///   - isLoading: Whether the first messages are loading. Shows placeholder rows while empty.
    ///   - errorMessage: Why the first load failed. Shown with a retry while empty.
    ///   - groupWindow: Consecutive messages by one author within this many seconds are grouped.
    ///   - now: Fixes "now" for Today and Yesterday. The clock by default.
    ///   - stickToBottom: Whether new messages are followed while the reader is at the bottom.
    ///   - showsJumpToLatest: Whether the Jump button shows when scrolled up.
    ///   - labels: Replacements for the built-in strings.
    ///   - onLoadOlder: Loads the previous page. Prepend the result to `items`. Adds the load row at the top.
    ///   - onRetry: Retries the first load after `errorMessage`.
    ///   - onAtBottomChange: Called when the reader reaches or leaves the newest message.
    ///   - row: Builds the row of one message, usually a ``MonarchMessageRow``.
    public init(
        _ items: [Item],
        firstUnreadID: Item.ID? = nil,
        highlightedID: Item.ID? = nil,
        scrollToID: Item.ID? = nil,
        hasOlder: Bool = false,
        isLoadingOlder: Bool = false,
        isOlderFailed: Bool = false,
        isLoading: Bool = false,
        errorMessage: String? = nil,
        groupWindow: TimeInterval = 300,
        now: Date? = nil,
        stickToBottom: Bool = true,
        showsJumpToLatest: Bool = true,
        labels: MonarchVirtualMessageListLabels = MonarchVirtualMessageListLabels(),
        onLoadOlder: (@MainActor () -> Void)? = nil,
        onRetry: (@MainActor () -> Void)? = nil,
        onAtBottomChange: (@MainActor (Bool) -> Void)? = nil,
        @ViewBuilder row: @escaping (Item, MonarchVirtualMessageListContext<Item>) -> Row
    ) {
        self.init(
            items,
            firstUnreadID: firstUnreadID,
            highlightedID: highlightedID,
            scrollToID: scrollToID,
            hasOlder: hasOlder,
            isLoadingOlder: isLoadingOlder,
            isOlderFailed: isOlderFailed,
            isLoading: isLoading,
            errorMessage: errorMessage,
            groupWindow: groupWindow,
            now: now,
            stickToBottom: stickToBottom,
            showsJumpToLatest: showsJumpToLatest,
            labels: labels,
            onLoadOlder: onLoadOlder,
            onRetry: onRetry,
            onAtBottomChange: onAtBottomChange,
            header: { EmptyView() },
            footer: { EmptyView() },
            row: row
        )
    }
}

#if DEBUG
    private struct MonarchVirtualMessageListPreviewMessage: MonarchVirtualMessageListItem {
        let id: Int
        let time: Date
        let authorID: String?
        let text: String

        static func sample(_ index: Int, now: Date) -> Self {
            let day: TimeInterval = index > 6 ? 86_400 : 0
            let offset = TimeInterval(index * 120 - 90_000) + day
            let author: String = index % 3 == 0 ? "ana" : "ben"
            return Self(id: index, time: now.addingTimeInterval(offset), authorID: author, text: "Message \(index)")
        }
    }

    #Preview("MonarchVirtualMessageList") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        let messages: [MonarchVirtualMessageListPreviewMessage] = (0..<12).map {
            MonarchVirtualMessageListPreviewMessage.sample($0, now: now)
        }
        MonarchPreviewMatrix {
            MonarchVirtualMessageList(messages, firstUnreadID: 9, now: now) { message, context in
                MonarchMessageRow(
                    MonarchMessageRowAuthor(message.authorID == "ana" ? "Ana Lopez" : "Ben Okafor"),
                    time: message.time,
                    now: now,
                    isGrouped: context.isGrouped
                ) {
                    Text(message.text)
                }
            }
            .frame(height: 360)
        }
    }
#endif
