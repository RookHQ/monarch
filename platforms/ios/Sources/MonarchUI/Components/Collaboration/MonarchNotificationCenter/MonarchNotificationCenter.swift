import SwiftUI

/// The in-app list of notifications: All and Unread tabs, day groups, mark
/// one or all as read, load more, loading, error and empty states.
///
/// ```swift
/// MonarchNotificationsBell(unreadCount: unread) { showsNotifications = true }
///     .sheet(isPresented: $showsNotifications) {
///         NavigationStack {
///             MonarchNotificationCenter(
///                 store.notifications,
///                 hasMore: store.hasMore,
///                 onMarkRead: { store.markRead($0) },
///                 onMarkAllRead: { store.markAllRead() },
///                 onLoadMore: { store.loadMore() },
///                 onSelect: { open($0) }
///             )
///         }
///     }
/// ```
///
/// Present it in a `.sheet` inside a `NavigationStack`, which supplies the
/// title bar with "Mark all as read" and "Done", or push it onto an existing
/// navigation stack with `showsDoneButton: false`. Rows are
/// ``MonarchInboxRow`` views: a swipe from the leading edge marks one item
/// read, and activating an unread item marks it read too. Times are
/// relative ("5 minutes ago") and stay current; pass `now` to fix them and
/// the day headings. The app supplies the data and the callbacks; system
/// notifications are a separate concern.
///
/// Accessibility: the tabs are a tab bar named "Show", with the unread count
/// in the Unread tab's name. Day headings are headers. Each item is one
/// button read as the title, "Unread" when unread, the full date and time
/// and the message; marking it read is also a VoiceOver action. The loading
/// placeholder is one element named "Loading notifications".
public struct MonarchNotificationCenter<Footer: View>: View {
    private let items: [MonarchNotificationCenterItem]
    private let unreadCount: Int?
    private let now: Date?
    private let isLoading: Bool
    private let errorMessage: String?
    private let hasMore: Bool
    private let isLoadingMore: Bool
    private let isLoadMoreFailed: Bool
    private let groupsByDay: Bool
    private let marksReadOnSelect: Bool
    private let showsDoneButton: Bool
    private let labels: MonarchNotificationCenterLabels
    private let onFilterChange: (@MainActor (MonarchNotificationCenterFilter) -> Void)?
    private let onMarkRead: (@MainActor (String) -> Void)?
    private let onMarkAllRead: (@MainActor () -> Void)?
    private let onLoadMore: (@MainActor () -> Void)?
    private let onRetry: (@MainActor () -> Void)?
    private let onSelect: (@MainActor (MonarchNotificationCenterItem) -> Void)?
    private let footer: Footer

    @State private var filter: MonarchNotificationCenterFilter
    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.calendar) private var calendar
    @Environment(\.dismiss) private var dismiss

    private typealias Tokens = MonarchNotificationCenterTokens

    /// A notification center with a footer after the list.
    ///
    /// - Parameters:
    ///   - items: The loaded notifications, newest first.
    ///   - unreadCount: The unread total from the server, which may exceed the loaded items. The unread loaded items by default.
    ///   - filter: The tab shown first.
    ///   - now: Fixes "now" for the times and day headings, for tests and previews. The clock by default.
    ///   - isLoading: The first load: shows placeholder rows.
    ///   - errorMessage: The first load failed: shows the error instead of the list.
    ///   - hasMore: Another page exists: shows the load-more row at the end.
    ///   - isLoadingMore: A page is loading.
    ///   - isLoadMoreFailed: The last page failed to load.
    ///   - groupsByDay: Groups items under Today, Yesterday and dates.
    ///   - marksReadOnSelect: Calls `onMarkRead` when an unread item is activated.
    ///   - showsDoneButton: Shows a Done button that dismisses the presentation.
    ///   - labels: Replacements for the built-in strings.
    ///   - onFilterChange: Receives the tab, for apps that fetch each tab from the server.
    ///   - onMarkRead: Marks one item read. Adds the swipe action.
    ///   - onMarkAllRead: Marks everything read. Adds the toolbar button.
    ///   - onLoadMore: Loads the next page; also retries a failed page.
    ///   - onRetry: Retries a failed first load. Adds a retry button to the error.
    ///   - onSelect: An item was activated. Navigate to what it is about.
    ///   - footer: Content after the list, such as a link to notification settings.
    public init(
        _ items: [MonarchNotificationCenterItem],
        unreadCount: Int? = nil,
        filter: MonarchNotificationCenterFilter = .all,
        now: Date? = nil,
        isLoading: Bool = false,
        errorMessage: String? = nil,
        hasMore: Bool = false,
        isLoadingMore: Bool = false,
        isLoadMoreFailed: Bool = false,
        groupsByDay: Bool = true,
        marksReadOnSelect: Bool = true,
        showsDoneButton: Bool = true,
        labels: MonarchNotificationCenterLabels = MonarchNotificationCenterLabels(),
        onFilterChange: (@MainActor (MonarchNotificationCenterFilter) -> Void)? = nil,
        onMarkRead: (@MainActor (String) -> Void)? = nil,
        onMarkAllRead: (@MainActor () -> Void)? = nil,
        onLoadMore: (@MainActor () -> Void)? = nil,
        onRetry: (@MainActor () -> Void)? = nil,
        onSelect: (@MainActor (MonarchNotificationCenterItem) -> Void)? = nil,
        @ViewBuilder footer: () -> Footer
    ) {
        self.items = items
        self.unreadCount = unreadCount
        self.now = now
        self.isLoading = isLoading
        self.errorMessage = errorMessage
        self.hasMore = hasMore
        self.isLoadingMore = isLoadingMore
        self.isLoadMoreFailed = isLoadMoreFailed
        self.groupsByDay = groupsByDay
        self.marksReadOnSelect = marksReadOnSelect
        self.showsDoneButton = showsDoneButton
        self.labels = labels
        self.onFilterChange = onFilterChange
        self.onMarkRead = onMarkRead
        self.onMarkAllRead = onMarkAllRead
        self.onLoadMore = onLoadMore
        self.onRetry = onRetry
        self.onSelect = onSelect
        self.footer = footer()
        _filter = State(initialValue: filter)
    }

    private var unread: Int { Tokens.unreadCount(items, override: unreadCount) }
    private var visible: [MonarchNotificationCenterItem] { Tokens.visible(items, filter: filter) }
    private var content: Tokens.Content {
        Tokens.content(
            isLoading: isLoading,
            errorMessage: errorMessage,
            visibleCount: visible.count,
            groupsByDay: groupsByDay
        )
    }

    /// The content of the view.
    public var body: some View {
        VStack(spacing: 0) {
            if showsDoneButton {
                sheetHeader
            }
            MonarchTabs(
                labels.filter ?? MonarchNotificationCenterStrings.filter,
                selection: $filter,
                items: [
                    MonarchTabsItem(labels.all ?? MonarchNotificationCenterStrings.all, value: .all),
                    MonarchTabsItem(
                        labels.unread ?? MonarchNotificationCenterStrings.unread,
                        value: .unread,
                        count: Tokens.badgeText(unread, locale: locale),
                        countTone: Tokens.countTone
                    ),
                ]
            )
            list
        }
        .background(theme.colors.bgSurface)
        .navigationTitle(showsDoneButton ? "" : labels.title ?? MonarchNotificationCenterStrings.title)
        #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            if showsDoneButton {
                ToolbarItem(placement: .cancellationAction) {
                    Button(labels.done ?? MonarchNotificationCenterStrings.done) { dismiss() }
                }
                .monarchFlat()
            }
            if let onMarkAllRead, !showsDoneButton {
                ToolbarItem(placement: .primaryAction) {
                    Button(action: onMarkAllRead) {
                        HStack(spacing: 4) {
                            Image(systemName: Tokens.markReadSymbol)
                            Text(labels.markAllRead ?? MonarchNotificationCenterStrings.markAllRead)
                        }
                    }
                    .disabled(unread == 0)
                }
                .monarchFlat()
            }
        }
        .onChange(of: filter) { _, new in onFilterChange?(new) }
    }

    /// In a sheet the title and Mark all as read sit in a row of the content, as the web's popover has them, so the
    /// title bar holds only Done and the title is never cut short.
    private var sheetHeader: some View {
        HStack(alignment: .firstTextBaseline, spacing: theme.spacing.space2) {
            Text(labels.title ?? MonarchNotificationCenterStrings.title)
                .monarchTextStyle(.heading)
                .accessibilityAddTraits(.isHeader)
            Spacer(minLength: 0)
            if let onMarkAllRead {
                Button(action: onMarkAllRead) {
                    HStack(spacing: theme.spacing.space1) {
                        MonarchIcon(systemName: Tokens.markReadSymbol, size: .sm)
                        Text(labels.markAllRead ?? MonarchNotificationCenterStrings.markAllRead)
                            .monarchTextStyle(.label)
                    }
                    .monarchMinimumHitTarget()
                }
                .buttonStyle(.plain)
                .foregroundStyle(theme.colors.textDefault)
                .disabled(unread == 0)
            }
        }
        .padding(.horizontal, theme.spacing.space4)
    }

    private var list: some View {
        List {
            Group {
                switch content {
                case .loading:
                    placeholder
                case .failed(let message):
                    MonarchErrorState(message, size: .sm, onRetry: onRetry)
                        .frame(maxWidth: .infinity)
                        .listRowSeparator(.hidden)
                case .empty:
                    MonarchEmptyState(
                        Tokens.emptyTitle(filter, labels: labels),
                        message: labels.emptyMessage ?? MonarchNotificationCenterStrings.emptyMessage,
                        systemImage: Tokens.emptySymbol,
                        kind: Tokens.emptyKind(filter),
                        size: .sm
                    )
                    .frame(maxWidth: .infinity)
                    .listRowSeparator(.hidden)
                case .grouped:
                    let reference = now ?? Date()
                    ForEach(Tokens.groups(visible, calendar: calendar)) { group in
                        Section {
                            ForEach(group.items) { row($0) }
                        } header: {
                            Text(
                                Tokens.dayLabel(
                                    group.day,
                                    now: reference,
                                    calendar: calendar,
                                    locale: locale,
                                    labels: labels
                                )
                            )
                            .monarchTextStyle(.label)
                            .foregroundStyle(theme.colors.textMuted)
                            .accessibilityAddTraits(.isHeader)
                        }
                    }
                case .flat:
                    ForEach(visible) { row($0) }
                }
                if Tokens.showsLoadMore(hasMore: hasMore, canLoad: onLoadMore != nil, content: content), let onLoadMore
                {
                    MonarchLoadMore(
                        hasMore: true,
                        isLoading: isLoadingMore,
                        isFailed: isLoadMoreFailed,
                        showsCount: false,
                        showsEnd: false,
                        labels: labels.loadMore,
                        onRetry: onLoadMore,
                        onLoadMore: onLoadMore
                    )
                    .listRowSeparator(.hidden)
                }
                if Footer.self != EmptyView.self {
                    Section { footer }
                }
            }
            .monarchRowsOnCanvas()
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
    }

    private var placeholder: some View {
        VStack(spacing: 0) {
            // Redacted shapes only: the text is never shown or read.
            ForEach(0..<Tokens.placeholderRows, id: \.self) { _ in
                MonarchInboxRow(Tokens.placeholderTitle, preview: Tokens.placeholderMessage)
                    .padding(.vertical, Tokens.placeholderSpacing)
            }
        }
        .redacted(reason: .placeholder)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(labels.loading ?? MonarchNotificationCenterStrings.loading)
        .listRowSeparator(.hidden)
        .monarchRowsOnCanvas()
    }

    private func row(_ item: MonarchNotificationCenterItem) -> some View {
        let select = Button {
            if Tokens.marksRead(item, marksReadOnSelect: marksReadOnSelect) { onMarkRead?(item.id) }
            onSelect?(item)
        } label: {
            rowContent(item)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        return HStack(alignment: .top, spacing: theme.spacing.space1) {
            select
            if !item.isRead {
                Circle()
                    .fill(theme.colors[role: \.accentSolid])
                    .frame(width: theme.spacing.space2, height: theme.spacing.space2)
                    .padding(.top, theme.spacing.space2)
                    .accessibilityHidden(true)
            }
            if !item.isRead, let onMarkRead {
                Button {
                    onMarkRead(item.id)
                } label: {
                    MonarchIcon(systemName: Tokens.markReadSymbol, size: .sm)
                        .foregroundStyle(theme.colors.textDefault)
                        .monarchMinimumHitTarget()
                }
                .buttonStyle(.plain)
                .accessibilityLabel(labels.markRead ?? MonarchNotificationCenterStrings.markRead)
            }
        }
        .monarchRowsOnCanvas()
        .swipeActions(edge: .leading, allowsFullSwipe: true) {
            if !item.isRead, let onMarkRead {
                Button {
                    onMarkRead(item.id)
                } label: {
                    Label(
                        labels.markRead ?? MonarchNotificationCenterStrings.markRead,
                        systemImage: Tokens.markReadSymbol
                    )
                }
                .tint(theme.colors[role: Tokens.markReadTint])
            }
        }
    }

    /// One notification as on the web: the actor's avatar or a bell, the title (bold while unread), the
    /// excerpt and the time under it.
    private func rowContent(_ item: MonarchNotificationCenterItem) -> some View {
        HStack(alignment: .top, spacing: theme.spacing.space3) {
            if let name = item.actorName {
                MonarchAvatar(url: item.actorAvatarURL, name: name, size: .sm, isDecorative: true)
            } else {
                MonarchIcon(systemName: item.systemImage ?? Tokens.defaultSymbol, size: .sm)
                    .foregroundStyle(theme.colors.textMuted)
                    .frame(width: theme.spacing.space8, height: theme.spacing.space8)
                    .background(Circle().fill(theme.colors.bgMuted))
                    .accessibilityHidden(true)
            }
            VStack(alignment: .leading, spacing: theme.spacing.space0_5) {
                Text(item.title)
                    .monarchTextStyle(item.isRead ? .body : .bodyStrong)
                    .foregroundStyle(theme.colors[role: item.isRead ? \.textMuted : \.textDefault])
                if let message = item.message, !message.isEmpty {
                    Text(message)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                if let now {
                    timeText(item.time, now: now)
                } else {
                    TimelineView(MonarchRelativeTimeSchedule(dates: [item.time])) { context in
                        timeText(item.time, now: context.date)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, theme.spacing.space1)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(rowLabel(item))
    }

    private func timeText(_ date: Date, now: Date) -> some View {
        Text(MonarchRelativeTime.string(date, now: now, locale: locale, calendar: calendar))
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }

    private func rowLabel(_ item: MonarchNotificationCenterItem) -> String {
        [
            item.isRead ? nil : (labels.unreadItem ?? MonarchNotificationCenterStrings.unreadItem), item.title,
            item.message,
        ]
        .compactMap { $0 }.joined(separator: ", ")
    }
}

extension MonarchNotificationCenter where Footer == EmptyView {
    /// A notification center without a footer.
    ///
    /// - Parameters:
    ///   - items: The loaded notifications, newest first.
    ///   - unreadCount: The unread total from the server, which may exceed the loaded items. The unread loaded items by default.
    ///   - filter: The tab shown first.
    ///   - now: Fixes "now" for the times and day headings, for tests and previews. The clock by default.
    ///   - isLoading: The first load: shows placeholder rows.
    ///   - errorMessage: The first load failed: shows the error instead of the list.
    ///   - hasMore: Another page exists: shows the load-more row at the end.
    ///   - isLoadingMore: A page is loading.
    ///   - isLoadMoreFailed: The last page failed to load.
    ///   - groupsByDay: Groups items under Today, Yesterday and dates.
    ///   - marksReadOnSelect: Calls `onMarkRead` when an unread item is activated.
    ///   - showsDoneButton: Shows a Done button that dismisses the presentation.
    ///   - labels: Replacements for the built-in strings.
    ///   - onFilterChange: Receives the tab, for apps that fetch each tab from the server.
    ///   - onMarkRead: Marks one item read. Adds the swipe action.
    ///   - onMarkAllRead: Marks everything read. Adds the toolbar button.
    ///   - onLoadMore: Loads the next page; also retries a failed page.
    ///   - onRetry: Retries a failed first load. Adds a retry button to the error.
    ///   - onSelect: An item was activated. Navigate to what it is about.
    public init(
        _ items: [MonarchNotificationCenterItem],
        unreadCount: Int? = nil,
        filter: MonarchNotificationCenterFilter = .all,
        now: Date? = nil,
        isLoading: Bool = false,
        errorMessage: String? = nil,
        hasMore: Bool = false,
        isLoadingMore: Bool = false,
        isLoadMoreFailed: Bool = false,
        groupsByDay: Bool = true,
        marksReadOnSelect: Bool = true,
        showsDoneButton: Bool = true,
        labels: MonarchNotificationCenterLabels = MonarchNotificationCenterLabels(),
        onFilterChange: (@MainActor (MonarchNotificationCenterFilter) -> Void)? = nil,
        onMarkRead: (@MainActor (String) -> Void)? = nil,
        onMarkAllRead: (@MainActor () -> Void)? = nil,
        onLoadMore: (@MainActor () -> Void)? = nil,
        onRetry: (@MainActor () -> Void)? = nil,
        onSelect: (@MainActor (MonarchNotificationCenterItem) -> Void)? = nil
    ) {
        self.init(
            items,
            unreadCount: unreadCount,
            filter: filter,
            now: now,
            isLoading: isLoading,
            errorMessage: errorMessage,
            hasMore: hasMore,
            isLoadingMore: isLoadingMore,
            isLoadMoreFailed: isLoadMoreFailed,
            groupsByDay: groupsByDay,
            marksReadOnSelect: marksReadOnSelect,
            showsDoneButton: showsDoneButton,
            labels: labels,
            onFilterChange: onFilterChange,
            onMarkRead: onMarkRead,
            onMarkAllRead: onMarkAllRead,
            onLoadMore: onLoadMore,
            onRetry: onRetry,
            onSelect: onSelect,
            footer: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchNotificationCenter") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        let items = [
            MonarchNotificationCenterItem(
                id: "n1",
                title: "Ana Lopez mentioned you in Launch checklist",
                message: "Can you confirm the legal sign-off date?",
                time: now.addingTimeInterval(-1_080),
                actorName: "Ana Lopez"
            ),
            MonarchNotificationCenterItem(
                id: "n2",
                title: "Your weekly export is ready",
                time: now.addingTimeInterval(-86_400),
                isRead: true,
                systemImage: "arrow.down.doc"
            ),
        ]
        MonarchPreviewMatrix {
            NavigationStack {
                MonarchNotificationCenter(
                    items,
                    now: now,
                    hasMore: true,
                    onMarkRead: { _ in },
                    onMarkAllRead: {},
                    onLoadMore: {}
                )
            }
            .frame(minHeight: 480)
        }
    }
#endif
