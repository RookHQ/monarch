import SwiftUI

/// One conversation in an inbox: avatar, name, age, a preview line and
/// markers, with unread state and Mail's swipe actions.
///
/// ```swift
/// List(conversations, selection: $selection) { conversation in
///     NavigationLink(value: conversation) {
///         MonarchInboxRow(
///             conversation.title,
///             preview: conversation.lastMessage,
///             timestamp: conversation.updatedAt,
///             unreadCount: conversation.unread,
///             swipeActions: [.toggleRead { toggleRead(conversation) }, .archive { archive(conversation) }]
///         ) {
///             MonarchStatusBadge(conversation.status, size: .sm)
///         }
///     }
/// }
/// ```
///
/// The row is the content of a `List` row: put it in a `NavigationLink`, or
/// in a `List` with a selection, so opening, highlighting the open
/// conversation and selecting rows in edit mode come from the system. Swipe
/// actions are placed as in Mail: read and unread on the leading edge,
/// archive and delete on the trailing edge, and a full swipe runs the first
/// one on each edge. The age is compact ("5m", "3h") and updates every
/// minute; pass `now` to fix it.
///
/// Accessibility: the row is one element read as the title, "3 unread",
/// the channel, the full date and time, the preview, then the badges and the
/// trailing content. Swipe actions are also VoiceOver actions. The unread
/// state is a dot or a count and a bold title, not colour alone. The avatar
/// is decorative.
public struct MonarchInboxRow<Badges: View, Trailing: View>: View {
    private let title: String
    private let preview: String?
    private let timestamp: Date?
    private let now: Date?
    private let avatarName: String?
    private let avatarURL: URL?
    private let showsAvatar: Bool
    private let systemImage: String?
    private let systemImageLabel: String?
    private let isUnread: Bool
    private let isCurrent: Bool
    private let unreadCount: Int?
    private let swipeActions: [MonarchInboxRowSwipeAction]
    private let timeStyle: MonarchInboxRowTimeStyle
    private let lineLimit: Int?
    private let labels: MonarchInboxRowLabels
    private let badges: Badges
    private let trailing: Trailing

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.calendar) private var calendar

    /// An inbox row with badges and trailing content on its last line.
    ///
    /// - Parameters:
    ///   - title: Who or what the conversation is with, already localised.
    ///   - preview: The latest message or a summary, on one line.
    ///   - timestamp: When the conversation last changed, shown as a compact age.
    ///   - now: Fixes "now" for the age, for tests and previews. The clock by default.
    ///   - avatarName: The name for the avatar's initials. The title by default.
    ///   - avatarURL: The avatar's picture, loaded with `AsyncImage`.
    ///   - showsAvatar: Whether the avatar is shown.
    ///   - systemImage: A small SF Symbol after the title, such as the channel.
    ///   - systemImageLabel: The spoken description of the symbol, such as "via email".
    ///   - isUnread: Shows the unread dot and a bold title.
    ///   - isCurrent: Marks the conversation that is open beside the list: an accent-tinted background and an accent bar on the leading edge.
    ///   - unreadCount: Shows a count instead of the dot, "99+" above 99. A count above zero makes the row unread.
    ///   - swipeActions: The swipe actions, placed as in Mail.
    ///   - timeStyle: How the time is written: a compact age ("5m"), or the full relative time ("5 minutes ago").
    ///   - lineLimit: The lines the title and the preview may take. One by default; `nil` shows them in full.
    ///   - labels: Replacements for the built-in strings.
    ///   - badges: Markers on the last line: status, SLA, priority.
    ///   - trailing: The end of the last line: the reference number, an assignee.
    public init(
        _ title: String,
        preview: String? = nil,
        timestamp: Date? = nil,
        now: Date? = nil,
        avatarName: String? = nil,
        avatarURL: URL? = nil,
        showsAvatar: Bool = true,
        systemImage: String? = nil,
        systemImageLabel: String? = nil,
        isUnread: Bool = false,
        isCurrent: Bool = false,
        unreadCount: Int? = nil,
        swipeActions: [MonarchInboxRowSwipeAction] = [],
        timeStyle: MonarchInboxRowTimeStyle = .compact,
        lineLimit: Int? = 1,
        labels: MonarchInboxRowLabels = MonarchInboxRowLabels(),
        @ViewBuilder badges: () -> Badges,
        @ViewBuilder trailing: () -> Trailing
    ) {
        self.title = title
        self.preview = preview
        self.timestamp = timestamp
        self.now = now
        self.avatarName = avatarName
        self.avatarURL = avatarURL
        self.showsAvatar = showsAvatar
        self.systemImage = systemImage
        self.systemImageLabel = systemImageLabel
        self.isUnread = isUnread
        self.isCurrent = isCurrent
        self.unreadCount = unreadCount
        self.swipeActions = swipeActions
        self.timeStyle = timeStyle
        self.lineLimit = lineLimit
        self.labels = labels
        self.badges = badges()
        self.trailing = trailing()
    }

    private var unread: Bool { MonarchInboxRowTokens.isUnread(isUnread, count: unreadCount) }
    private var hasMetaLine: Bool { Badges.self != EmptyView.self || Trailing.self != EmptyView.self }

    /// The content of the view.
    public var body: some View {
        HStack(alignment: .top, spacing: MonarchInboxRowTokens.leadingSpacing) {
            if showsAvatar {
                MonarchAvatar(
                    url: avatarURL,
                    name: avatarName ?? title,
                    size: MonarchInboxRowTokens.avatarSize,
                    isDecorative: true
                )
            }
            VStack(alignment: .leading, spacing: MonarchInboxRowTokens.lineSpacing) {
                firstLine
                if let preview, !preview.isEmpty {
                    Text(preview)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                        .lineLimit(lineLimit)
                }
                if hasMetaLine {
                    HStack(spacing: MonarchInboxRowTokens.itemSpacing) {
                        badges
                        Spacer(minLength: 0)
                        trailing
                    }
                }
            }
        }
        .frame(minHeight: MonarchAccessibility.minimumHitTarget)
        .background {
            if isCurrent {
                ZStack(alignment: .leading) {
                    Rectangle().fill(theme.colors[role: \.accentSubtle])
                    Rectangle()
                        .fill(theme.colors[role: \.accentStrong])
                        .frame(width: MonarchInboxRowTokens.currentBarWidth)
                }
                .padding(.horizontal, -MonarchInboxRowTokens.currentBleed)
                .padding(.vertical, -MonarchInboxRowTokens.currentBleed / 2)
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(isCurrent ? .isSelected : [])
        .swipeActions(edge: .leading, allowsFullSwipe: true) {
            ForEach(Array(leadingActions.enumerated()), id: \.offset) { _, action in
                swipeButton(action)
            }
        }
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            ForEach(Array(trailingActions.enumerated()), id: \.offset) { _, action in
                swipeButton(action)
            }
        }
    }

    private var leadingActions: [MonarchInboxRowSwipeAction] { swipeActions.filter(MonarchInboxRowTokens.isLeading) }
    private var trailingActions: [MonarchInboxRowSwipeAction] {
        swipeActions.filter { !MonarchInboxRowTokens.isLeading($0) }
    }

    private var firstLine: some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchInboxRowTokens.itemSpacing) {
            if unread, MonarchInboxRowTokens.countText(unreadCount) == nil {
                Circle()
                    .fill(theme.colors[role: MonarchInboxRowTokens.unreadFill])
                    .frame(width: MonarchInboxRowTokens.dotDiameter, height: MonarchInboxRowTokens.dotDiameter)
                    .accessibilityHidden(true)
            }
            Text(title)
                .monarchTextStyle(unread ? .bodyStrong : .body)
                .foregroundStyle(theme.colors.textDefault)
                .lineLimit(lineLimit)
                .accessibilityLabel(
                    MonarchInboxRowTokens.titleLabel(
                        title,
                        unread: MonarchInboxRowTokens.unreadLabel(
                            isUnread: isUnread,
                            count: unreadCount,
                            labels: labels
                        )
                    )
                )
            if let systemImage {
                if let systemImageLabel {
                    MonarchIcon(systemName: systemImage, label: systemImageLabel, size: .sm)
                        .foregroundStyle(theme.colors.textMuted)
                } else {
                    MonarchIcon(systemName: systemImage, size: .sm).foregroundStyle(theme.colors.textMuted)
                }
            }
            Spacer(minLength: 0)
            if let timestamp {
                if let now {
                    age(timestamp, now: now)
                } else {
                    TimelineView(MonarchRelativeTimeSchedule(dates: [timestamp])) { context in
                        age(timestamp, now: context.date)
                    }
                }
            }
            if let count = MonarchInboxRowTokens.countText(unreadCount) {
                Text(count)
                    .monarchTextStyle(.caption, weight: .semibold)
                    .monospacedDigit()
                    .foregroundStyle(theme.colors[role: MonarchInboxRowTokens.unreadForeground])
                    .padding(.horizontal, MonarchInboxRowTokens.countPadding)
                    .frame(minWidth: MonarchInboxRowTokens.countMinWidth)
                    .background(Capsule().fill(theme.colors[role: MonarchInboxRowTokens.unreadFill]))
                    .accessibilityHidden(true)
            }
        }
    }

    private func age(_ date: Date, now: Date) -> some View {
        Text(
            MonarchInboxRowTokens.timeText(
                date,
                style: timeStyle,
                now: now,
                labels: labels,
                locale: locale,
                calendar: calendar
            )
        )
        .monarchTextStyle(.caption, weight: unread ? .semibold : .regular)
        .foregroundStyle(theme.colors[role: unread ? \.textDefault : \.textMuted])
        .lineLimit(1)
        .fixedSize()
        .accessibilityLabel(MonarchInboxRowTokens.absoluteTime(date, locale: locale, calendar: calendar))
    }

    private func swipeButton(_ action: MonarchInboxRowSwipeAction) -> some View {
        let title = MonarchInboxRowTokens.title(action, isUnread: unread, labels: labels)
        let symbol = MonarchInboxRowTokens.symbol(action, isUnread: unread)
        let run: @MainActor () -> Void
        let role: ButtonRole?
        switch action {
        case .toggleRead(let perform), .archive(let perform):
            run = perform
            role = nil
        case .delete(let perform):
            run = perform
            role = .destructive
        }
        return Button(role: role, action: run) {
            Label(title, systemImage: symbol)
        }
        .tint(theme.colors[role: MonarchInboxRowTokens.tint(action)])
    }
}

extension MonarchInboxRow where Badges == EmptyView, Trailing == EmptyView {
    /// An inbox row without badges or trailing content.
    ///
    /// - Parameters:
    ///   - title: Who or what the conversation is with, already localised.
    ///   - preview: The latest message or a summary, on one line.
    ///   - timestamp: When the conversation last changed, shown as a compact age.
    ///   - now: Fixes "now" for the age, for tests and previews. The clock by default.
    ///   - avatarName: The name for the avatar's initials. The title by default.
    ///   - avatarURL: The avatar's picture, loaded with `AsyncImage`.
    ///   - showsAvatar: Whether the avatar is shown.
    ///   - systemImage: A small SF Symbol after the title, such as the channel.
    ///   - systemImageLabel: The spoken description of the symbol, such as "via email".
    ///   - isUnread: Shows the unread dot and a bold title.
    ///   - isCurrent: Marks the conversation that is open beside the list: an accent-tinted background and an accent bar on the leading edge.
    ///   - unreadCount: Shows a count instead of the dot, "99+" above 99. A count above zero makes the row unread.
    ///   - swipeActions: The swipe actions, placed as in Mail.
    ///   - timeStyle: How the time is written: a compact age ("5m"), or the full relative time ("5 minutes ago").
    ///   - lineLimit: The lines the title and the preview may take. One by default; `nil` shows them in full.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ title: String,
        preview: String? = nil,
        timestamp: Date? = nil,
        now: Date? = nil,
        avatarName: String? = nil,
        avatarURL: URL? = nil,
        showsAvatar: Bool = true,
        systemImage: String? = nil,
        systemImageLabel: String? = nil,
        isUnread: Bool = false,
        isCurrent: Bool = false,
        unreadCount: Int? = nil,
        swipeActions: [MonarchInboxRowSwipeAction] = [],
        timeStyle: MonarchInboxRowTimeStyle = .compact,
        lineLimit: Int? = 1,
        labels: MonarchInboxRowLabels = MonarchInboxRowLabels()
    ) {
        self.init(
            title,
            preview: preview,
            timestamp: timestamp,
            now: now,
            avatarName: avatarName,
            avatarURL: avatarURL,
            showsAvatar: showsAvatar,
            systemImage: systemImage,
            systemImageLabel: systemImageLabel,
            isUnread: isUnread,
            isCurrent: isCurrent,
            unreadCount: unreadCount,
            swipeActions: swipeActions,
            timeStyle: timeStyle,
            lineLimit: lineLimit,
            labels: labels,
            badges: { EmptyView() },
            trailing: { EmptyView() }
        )
    }
}

extension MonarchInboxRow where Trailing == EmptyView {
    /// An inbox row with badges on its last line.
    ///
    /// - Parameters:
    ///   - title: Who or what the conversation is with, already localised.
    ///   - preview: The latest message or a summary, on one line.
    ///   - timestamp: When the conversation last changed, shown as a compact age.
    ///   - now: Fixes "now" for the age, for tests and previews. The clock by default.
    ///   - avatarName: The name for the avatar's initials. The title by default.
    ///   - avatarURL: The avatar's picture, loaded with `AsyncImage`.
    ///   - showsAvatar: Whether the avatar is shown.
    ///   - systemImage: A small SF Symbol after the title, such as the channel.
    ///   - systemImageLabel: The spoken description of the symbol, such as "via email".
    ///   - isUnread: Shows the unread dot and a bold title.
    ///   - isCurrent: Marks the conversation that is open beside the list: an accent-tinted background and an accent bar on the leading edge.
    ///   - unreadCount: Shows a count instead of the dot, "99+" above 99. A count above zero makes the row unread.
    ///   - swipeActions: The swipe actions, placed as in Mail.
    ///   - timeStyle: How the time is written: a compact age ("5m"), or the full relative time ("5 minutes ago").
    ///   - lineLimit: The lines the title and the preview may take. One by default; `nil` shows them in full.
    ///   - labels: Replacements for the built-in strings.
    ///   - badges: Markers on the last line: status, SLA, priority.
    public init(
        _ title: String,
        preview: String? = nil,
        timestamp: Date? = nil,
        now: Date? = nil,
        avatarName: String? = nil,
        avatarURL: URL? = nil,
        showsAvatar: Bool = true,
        systemImage: String? = nil,
        systemImageLabel: String? = nil,
        isUnread: Bool = false,
        isCurrent: Bool = false,
        unreadCount: Int? = nil,
        swipeActions: [MonarchInboxRowSwipeAction] = [],
        timeStyle: MonarchInboxRowTimeStyle = .compact,
        lineLimit: Int? = 1,
        labels: MonarchInboxRowLabels = MonarchInboxRowLabels(),
        @ViewBuilder badges: () -> Badges
    ) {
        self.init(
            title,
            preview: preview,
            timestamp: timestamp,
            now: now,
            avatarName: avatarName,
            avatarURL: avatarURL,
            showsAvatar: showsAvatar,
            systemImage: systemImage,
            systemImageLabel: systemImageLabel,
            isUnread: isUnread,
            isCurrent: isCurrent,
            unreadCount: unreadCount,
            swipeActions: swipeActions,
            timeStyle: timeStyle,
            lineLimit: lineLimit,
            labels: labels,
            badges: badges,
            trailing: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchInboxRow") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        MonarchPreviewMatrix {
            List {
                Group {
                    MonarchInboxRow(
                        "Ana Silva",
                        preview: "Can you look at the invoice before Friday?",
                        timestamp: now.addingTimeInterval(-300),
                        now: now,
                        systemImage: "envelope",
                        systemImageLabel: "via email",
                        unreadCount: 3,
                        swipeActions: [.toggleRead {}, .archive {}]
                    ) {
                        MonarchBadge("Open", tone: .info)
                    } trailing: {
                        Text("#1042").monarchTextStyle(.caption)
                    }
                    MonarchInboxRow(
                        "Ben Okafor",
                        preview: "Thanks!",
                        timestamp: now.addingTimeInterval(-7_200),
                        now: now,
                        isUnread: true
                    )
                    MonarchInboxRow(
                        "Design team",
                        preview: "New file shared",
                        timestamp: now.addingTimeInterval(-86_400 * 3),
                        now: now
                    )

                }
                .monarchRowsOnCanvas()
            }
            .listStyle(.plain)
            .monarchCanvas()
        }
    }
#endif
