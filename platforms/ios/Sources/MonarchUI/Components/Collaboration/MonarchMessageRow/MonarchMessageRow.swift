import SwiftUI

/// One message in a conversation: avatar, author, time and body, with
/// grouped, pending, failed, edited and deleted states, reactions, a thread
/// reply count and per-message actions in a context menu.
///
/// ```swift
/// MonarchMessageRow(
///     MonarchMessageRowAuthor(message.author, url: message.avatarURL),
///     time: message.sentAt,
///     status: message.status,
///     reactions: message.reactions,
///     replyCount: message.replies,
///     onRetry: { resend(message) },
///     onReactionToggle: { emoji, isOn in react(emoji, isOn, message) },
///     onOpenThread: { open(message) }
/// ) {
///     Text(message.text)
/// } actions: {
///     Button("Reply", systemImage: "arrowshape.turn.up.left") { reply(message) }
///     Button("Delete", systemImage: "trash", role: .destructive) { delete(message) }
/// }
/// ```
///
/// The body is the app's: Monarch lays out the row and never parses message
/// content. Consecutive messages from one author are `isGrouped`: the
/// avatar and header are hidden and the body lines up under the first
/// message. Per-message actions are the `actions` menu items, shown as the
/// row's context menu (touch and hold, or a secondary click), which is how
/// iOS offers actions on a message. The time is relative and updates every
/// minute; pass `now` to fix it.
///
/// Accessibility: the author, the tags, the full date and time, the body,
/// the edited marker and "Sending…" are read as one element; a grouped
/// message still starts with the author and time. The edit history is a
/// VoiceOver action. A failed message is announced, and its error is an
/// icon and text, with Retry and Discard as separate buttons. Reactions and
/// the thread button are separate elements. The avatar is decorative.
public struct MonarchMessageRow<Content: View, Actions: View>: View {
    private let author: MonarchMessageRowAuthor
    private let time: Date
    private let now: Date?
    private let variant: MonarchMessageRowVariant
    private let status: MonarchMessageRowStatus
    private let timeStyle: MonarchMessageRowTimeStyle
    private let errorMessage: String?
    private let tags: [String]
    private let reactions: [MonarchReactionBarItem]
    private let replyCount: Int
    private let isGrouped: Bool
    private let isEdited: Bool
    private let isDeleted: Bool
    private let isHighlighted: Bool
    private let labels: MonarchMessageRowLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onDiscard: (@MainActor () -> Void)?
    private let onShowEdits: (@MainActor () -> Void)?
    private let onReactionToggle: (@MainActor (String, Bool) -> Void)?
    private let onOpenThread: (@MainActor () -> Void)?
    private let content: Content
    private let actions: Actions

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.calendar) private var calendar
    @ScaledMetric(relativeTo: .body) private var gutterWidth = MonarchMessageRowTokens.gutterWidth

    private typealias Tokens = MonarchMessageRowTokens

    /// A message row with per-message actions.
    ///
    /// - Parameters:
    ///   - author: Who wrote the message.
    ///   - time: When it was sent.
    ///   - now: Fixes "now" for the relative time, for tests and previews. The clock by default.
    ///   - variant: A message, an internal note or a system line.
    ///   - status: Sent, pending or failed.
    ///   - timeStyle: Relative ("5 minutes ago") or the clock time.
    ///   - errorMessage: Why sending failed. "Not sent" by default.
    ///   - tags: Small tags after the author: audience ("Internal"), role ("Guest", "App").
    ///   - reactions: The emoji reactions, shown with ``MonarchReactionBar``.
    ///   - replyCount: The number of thread replies. Shown when above zero.
    ///   - isGrouped: A follow-up from the same author: no avatar and no header.
    ///   - isEdited: Shows the edited marker.
    ///   - isDeleted: Replaces the body with the deleted text and hides reactions and actions.
    ///   - isHighlighted: Tints the row, for example after opening a link to it.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Sends a failed message again. Adds the Retry button.
    ///   - onDiscard: Removes a failed message. Adds the Discard button.
    ///   - onShowEdits: Opens the edit history. Makes the edited marker a button.
    ///   - onReactionToggle: Receives the emoji and the new state. Without it reactions are read-only.
    ///   - onOpenThread: Opens the thread. Makes the reply count a button.
    ///   - content: The message body.
    ///   - actions: Menu items shown as the row's context menu: reply, edit, copy, delete.
    public init(
        _ author: MonarchMessageRowAuthor,
        time: Date,
        now: Date? = nil,
        variant: MonarchMessageRowVariant = .standard,
        status: MonarchMessageRowStatus = .sent,
        timeStyle: MonarchMessageRowTimeStyle = .relative,
        errorMessage: String? = nil,
        tags: [String] = [],
        reactions: [MonarchReactionBarItem] = [],
        replyCount: Int = 0,
        isGrouped: Bool = false,
        isEdited: Bool = false,
        isDeleted: Bool = false,
        isHighlighted: Bool = false,
        labels: MonarchMessageRowLabels = MonarchMessageRowLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onDiscard: (@MainActor () -> Void)? = nil,
        onShowEdits: (@MainActor () -> Void)? = nil,
        onReactionToggle: (@MainActor (String, Bool) -> Void)? = nil,
        onOpenThread: (@MainActor () -> Void)? = nil,
        @ViewBuilder content: () -> Content,
        @ViewBuilder actions: () -> Actions
    ) {
        self.author = author
        self.time = time
        self.now = now
        self.variant = variant
        self.status = status
        self.timeStyle = timeStyle
        self.errorMessage = errorMessage
        self.tags = tags
        self.reactions = reactions
        self.replyCount = replyCount
        self.isGrouped = isGrouped
        self.isEdited = isEdited
        self.isDeleted = isDeleted
        self.isHighlighted = isHighlighted
        self.labels = labels
        self.onRetry = onRetry
        self.onDiscard = onDiscard
        self.onShowEdits = onShowEdits
        self.onReactionToggle = onReactionToggle
        self.onOpenThread = onOpenThread
        self.content = content()
        self.actions = actions()
    }

    /// The content of the view.
    public var body: some View {
        Group {
            if variant == .system {
                systemLine
            } else {
                message
            }
        }
        .padding(.horizontal, Tokens.horizontalPadding)
        .padding(.vertical, isGrouped && variant != .system ? Tokens.groupedVerticalPadding : Tokens.verticalPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            if let role = Tokens.background(variant: variant, isHighlighted: isHighlighted) {
                Rectangle().fill(theme.colors[role: role])
            }
        }
        .overlay(alignment: .leading) {
            if let role = Tokens.leadingBar(variant) {
                Rectangle().fill(theme.colors[role: role]).frame(width: Tokens.barWidth)
            }
        }
        .monarchAnimation(.fast, value: isHighlighted)
        .onChange(of: status) { _, new in
            if new == .failed {
                AccessibilityNotification.Announcement(Tokens.errorText(errorMessage, labels: labels)).post()
            }
        }
    }

    // MARK: - Standard and internal

    @ViewBuilder private var message: some View {
        let row = HStack(alignment: .top, spacing: Tokens.gutterSpacing) {
            gutter
            VStack(alignment: .leading, spacing: Tokens.stackSpacing) {
                readable
                if status == .failed { failedLine }
                if Tokens.showsReactions(reactions, isDeleted: isDeleted) {
                    MonarchReactionBar(reactions, labels: labels.reactions, onToggle: onReactionToggle)
                }
                if !isDeleted, let replies = Tokens.repliesText(replyCount, labels: labels) {
                    threadButton(replies)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        if Tokens.showsActions(status: status, isDeleted: isDeleted, variant: variant), Actions.self != EmptyView.self {
            row
                .contentShape(Rectangle())
                .contextMenu { actions }
        } else {
            row
        }
    }

    @ViewBuilder private var gutter: some View {
        if isGrouped {
            Color.clear.frame(width: gutterWidth, height: 1)
        } else if let image = author.image {
            MonarchAvatar(
                image: image,
                name: author.name,
                seed: author.seed,
                size: Tokens.avatarSize,
                isDecorative: true
            )
        } else {
            MonarchAvatar(
                url: author.url,
                name: author.name,
                seed: author.seed,
                size: Tokens.avatarSize,
                isDecorative: true
            )
        }
    }

    /// Everything read as the message: header or byline, body, edited marker, pending line.
    private var readable: some View {
        VStack(alignment: .leading, spacing: Tokens.stackSpacing) {
            if isGrouped {
                timeView { _, spoken in
                    // Not drawn: a grouped message still starts with its author and time for VoiceOver.
                    Color.clear
                        .frame(width: 0, height: 0)
                        .accessibilityElement()
                        .accessibilityLabel(Tokens.byline(author: author.name, time: spoken))
                }
            } else {
                header
            }
            Group {
                if isDeleted {
                    Text(labels.deleted ?? MonarchMessageRowStrings.deleted).italic()
                } else {
                    content
                }
            }
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors[role: Tokens.bodyForeground(status: status, isDeleted: isDeleted)])
            .frame(maxWidth: .infinity, alignment: .leading)
            if isEdited, !isDeleted {
                editedMarker
            }
            if status == .pending {
                Text(labels.sending ?? MonarchMessageRowStrings.sending)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .accessibilityElement(children: .combine)
        .modifier(EditHistoryAction(isOn: isEdited && !isDeleted, name: labels.editHistory, perform: onShowEdits))
    }

    private var header: some View {
        MonarchFlowLayout(spacing: Tokens.headerSpacing, rowSpacing: Tokens.stackSpacing) {
            Text(author.name)
                .monarchTextStyle(.bodyStrong)
                .foregroundStyle(theme.colors.textDefault)
            ForEach(tags, id: \.self) { tag in
                MonarchBadge(tag)
            }
            timeView { text, spoken in
                Text(text)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .accessibilityLabel(spoken)
            }
        }
    }

    @ViewBuilder private var editedMarker: some View {
        let marker = Text(labels.edited ?? MonarchMessageRowStrings.edited)
            .monarchTextStyle(.caption)
        if let onShowEdits {
            Button(action: onShowEdits) { marker }
                .buttonStyle(.monarchLink(.subtle))
        } else {
            marker.foregroundStyle(theme.colors.textMuted)
        }
    }

    private var failedLine: some View {
        MonarchFlowLayout(spacing: Tokens.statusSpacing, rowSpacing: 0) {
            HStack(alignment: .firstTextBaseline, spacing: Tokens.stackSpacing) {
                MonarchIcon(systemName: Tokens.failedSymbol, size: .sm)
                Text(Tokens.errorText(errorMessage, labels: labels)).monarchTextStyle(.caption)
            }
            .foregroundStyle(theme.colors.dangerStrong)
            .accessibilityElement(children: .combine)
            if let onRetry {
                MonarchButton(
                    labels.retry ?? MonarchMessageRowStrings.retry,
                    systemImage: Tokens.retrySymbol,
                    variant: .ghost,
                    size: .sm,
                    action: onRetry
                )
            }
            if let onDiscard {
                MonarchButton(
                    labels.discard ?? MonarchMessageRowStrings.discard,
                    variant: .ghost,
                    size: .sm,
                    action: onDiscard
                )
            }
        }
    }

    @ViewBuilder private func threadButton(_ text: String) -> some View {
        if let onOpenThread {
            Button(text, action: onOpenThread)
                .buttonStyle(.monarchLink(.standalone))
                .monarchTextStyle(.label, weight: .semibold)
                .monarchMinimumHitTarget()
        } else {
            Text(text)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textMuted)
        }
    }

    // MARK: - System

    private var systemLine: some View {
        HStack(alignment: .firstTextBaseline, spacing: Tokens.headerSpacing) {
            content
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
            timeView { text, spoken in
                Text(text)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textSubtle)
                    .accessibilityLabel(spoken)
            }
        }
        .multilineTextAlignment(.center)
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .combine)
    }

    // MARK: - Time

    /// Builds the time with the visible text and the spoken full date, fixed
    /// to `now` when given and redrawn on the relative-time schedule otherwise.
    @ViewBuilder private func timeView<V: View>(@ViewBuilder _ make: @escaping (String, String) -> V) -> some View {
        let spoken = Tokens.absoluteTime(time, locale: locale, calendar: calendar)
        if let now {
            make(Tokens.timeText(time, style: timeStyle, now: now, locale: locale, calendar: calendar), spoken)
        } else if timeStyle == .clock {
            make(Tokens.clockTime(time, locale: locale, calendar: calendar), spoken)
        } else {
            TimelineView(MonarchRelativeTimeSchedule(dates: [time])) { context in
                make(
                    Tokens.timeText(time, style: timeStyle, now: context.date, locale: locale, calendar: calendar),
                    spoken
                )
            }
        }
    }
}

/// Adds the "Show edit history" VoiceOver action to an edited message.
private struct EditHistoryAction: ViewModifier {
    let isOn: Bool
    let name: String?
    let perform: (@MainActor () -> Void)?

    func body(content: Content) -> some View {
        if isOn, let perform {
            content.accessibilityAction(named: Text(name ?? MonarchMessageRowStrings.editHistory)) { perform() }
        } else {
            content
        }
    }
}

extension MonarchMessageRow where Actions == EmptyView {
    /// A message row without per-message actions.
    ///
    /// - Parameters:
    ///   - author: Who wrote the message.
    ///   - time: When it was sent.
    ///   - now: Fixes "now" for the relative time, for tests and previews. The clock by default.
    ///   - variant: A message, an internal note or a system line.
    ///   - status: Sent, pending or failed.
    ///   - timeStyle: Relative ("5 minutes ago") or the clock time.
    ///   - errorMessage: Why sending failed. "Not sent" by default.
    ///   - tags: Small tags after the author: audience ("Internal"), role ("Guest", "App").
    ///   - reactions: The emoji reactions, shown with ``MonarchReactionBar``.
    ///   - replyCount: The number of thread replies. Shown when above zero.
    ///   - isGrouped: A follow-up from the same author: no avatar and no header.
    ///   - isEdited: Shows the edited marker.
    ///   - isDeleted: Replaces the body with the deleted text and hides reactions.
    ///   - isHighlighted: Tints the row, for example after opening a link to it.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Sends a failed message again. Adds the Retry button.
    ///   - onDiscard: Removes a failed message. Adds the Discard button.
    ///   - onShowEdits: Opens the edit history. Makes the edited marker a button.
    ///   - onReactionToggle: Receives the emoji and the new state. Without it reactions are read-only.
    ///   - onOpenThread: Opens the thread. Makes the reply count a button.
    ///   - content: The message body.
    public init(
        _ author: MonarchMessageRowAuthor,
        time: Date,
        now: Date? = nil,
        variant: MonarchMessageRowVariant = .standard,
        status: MonarchMessageRowStatus = .sent,
        timeStyle: MonarchMessageRowTimeStyle = .relative,
        errorMessage: String? = nil,
        tags: [String] = [],
        reactions: [MonarchReactionBarItem] = [],
        replyCount: Int = 0,
        isGrouped: Bool = false,
        isEdited: Bool = false,
        isDeleted: Bool = false,
        isHighlighted: Bool = false,
        labels: MonarchMessageRowLabels = MonarchMessageRowLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onDiscard: (@MainActor () -> Void)? = nil,
        onShowEdits: (@MainActor () -> Void)? = nil,
        onReactionToggle: (@MainActor (String, Bool) -> Void)? = nil,
        onOpenThread: (@MainActor () -> Void)? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.init(
            author,
            time: time,
            now: now,
            variant: variant,
            status: status,
            timeStyle: timeStyle,
            errorMessage: errorMessage,
            tags: tags,
            reactions: reactions,
            replyCount: replyCount,
            isGrouped: isGrouped,
            isEdited: isEdited,
            isDeleted: isDeleted,
            isHighlighted: isHighlighted,
            labels: labels,
            onRetry: onRetry,
            onDiscard: onDiscard,
            onShowEdits: onShowEdits,
            onReactionToggle: onReactionToggle,
            onOpenThread: onOpenThread,
            content: content,
            actions: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchMessageRow") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        let ana = MonarchMessageRowAuthor("Ana Lopez")
        MonarchPreviewMatrix {
            VStack(alignment: .leading, spacing: 0) {
                MonarchMessageRow(
                    ana,
                    time: now.addingTimeInterval(-480),
                    now: now,
                    reactions: [MonarchReactionBarItem("👍", count: 3, isReacted: true)],
                    replyCount: 4,
                    onReactionToggle: { _, _ in },
                    onOpenThread: {}
                ) {
                    Text("The release notes are ready for review.")
                } actions: {
                    Button("Reply", systemImage: "arrowshape.turn.up.left") {}
                }
                MonarchMessageRow(ana, time: now.addingTimeInterval(-420), now: now, isGrouped: true, isEdited: true) {
                    Text("Comments by Thursday, please.")
                }
                MonarchMessageRow(MonarchMessageRowAuthor("Ben Okafor"), time: now, now: now, status: .pending) {
                    Text("On it.")
                }
                MonarchMessageRow(
                    MonarchMessageRowAuthor("Ben Okafor"),
                    time: now,
                    now: now,
                    status: .failed,
                    isGrouped: true,
                    onRetry: {},
                    onDiscard: {}
                ) {
                    Text("Shipping it now.")
                }
                MonarchMessageRow(
                    MonarchMessageRowAuthor("Chloé Martin"),
                    time: now,
                    now: now,
                    variant: .internal,
                    tags: ["Internal"]
                ) {
                    Text("Customer is on the enterprise plan.")
                }
                MonarchMessageRow(ana, time: now, now: now, isDeleted: true) { Text("Removed") }
                MonarchMessageRow(ana, time: now, now: now, variant: .system) { Text("Ana Lopez joined the channel") }
            }
        }
    }
#endif
