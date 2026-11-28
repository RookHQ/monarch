import SwiftUI

/// A post in a community feed or search result: author, time, markers,
/// title, a short excerpt, and reaction and comment counts.
///
/// ```swift
/// MonarchPostCard(
///     post.title,
///     author: post.author.name,
///     excerpt: post.body,
///     publishedAt: post.date,
///     reactionCount: post.reactions,
///     commentCount: post.comments
/// ) {
///     path.append(post)
/// }
/// ```
///
/// The card is drawn by ``MonarchCard``. Tapping the author line, title or
/// excerpt runs `action`, which opens the post. Controls passed in
/// `actions`, such as a bookmark button, sit at the end of the footer and
/// are separate buttons. The time is relative ("3 hours ago") and stays
/// current while it is on screen; pass `now` to fix it.
///
/// Accessibility: the author line, title and excerpt are one button that
/// reads them in order, author first, so VoiceOver users hear who and when
/// before the title. Counts are one element read as "12 reactions, 4
/// comments". The avatar and icons are decorative. Everything wraps at
/// large text sizes.
public struct MonarchPostCard<Actions: View>: View {
    private let title: String
    private let author: String
    private let authorAvatarURL: URL?
    private let excerpt: String?
    private let excerptLength: Int
    private let publishedAt: Date?
    private let now: Date?
    private let tags: [MonarchPostCardTag]
    private let isPinned: Bool
    private let isAnswered: Bool
    private let reactionCount: Int?
    private let commentCount: Int?
    private let labels: MonarchPostCardLabels
    private let action: @MainActor () -> Void
    private let actions: Actions

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// A post card with controls in the footer.
    ///
    /// - Parameters:
    ///   - title: The post's title, already localised.
    ///   - author: Who wrote it, already localised.
    ///   - authorAvatarURL: The author's picture, loaded with `AsyncImage`.
    ///   - excerpt: Plain text of the post, shortened to `excerptLength` characters.
    ///   - excerptLength: The longest excerpt, in characters.
    ///   - publishedAt: When it was published, shown relative to now.
    ///   - now: Fixes "now" for the relative time, for tests and previews. The clock by default.
    ///   - tags: Markers after the time, such as the category or "In review".
    ///   - isPinned: Shows the pinned marker.
    ///   - isAnswered: Shows the answered badge, for a question with an accepted answer.
    ///   - reactionCount: The number of reactions. Hidden when `nil`.
    ///   - commentCount: The number of comments. Hidden when `nil`.
    ///   - labels: Replacements for the built-in strings.
    ///   - action: Opens the post.
    ///   - actions: Controls at the end of the footer, such as a bookmark button.
    public init(
        _ title: String,
        author: String,
        authorAvatarURL: URL? = nil,
        excerpt: String? = nil,
        excerptLength: Int = 220,
        publishedAt: Date? = nil,
        now: Date? = nil,
        tags: [MonarchPostCardTag] = [],
        isPinned: Bool = false,
        isAnswered: Bool = false,
        reactionCount: Int? = nil,
        commentCount: Int? = nil,
        labels: MonarchPostCardLabels = MonarchPostCardLabels(),
        action: @escaping @MainActor () -> Void,
        @ViewBuilder actions: () -> Actions
    ) {
        self.title = title
        self.author = author
        self.authorAvatarURL = authorAvatarURL
        self.excerpt = excerpt
        self.excerptLength = excerptLength
        self.publishedAt = publishedAt
        self.now = now
        self.tags = tags
        self.isPinned = isPinned
        self.isAnswered = isAnswered
        self.reactionCount = reactionCount
        self.commentCount = commentCount
        self.labels = labels
        self.action = action
        self.actions = actions()
    }

    /// The content of the view.
    public var body: some View {
        MonarchCard {
            Button(action: action) {
                VStack(alignment: .leading, spacing: theme.spacing.space2) {
                    meta
                    Text(title)
                        .monarchTextStyle(MonarchPostCardTokens.titleStyle)
                        .foregroundStyle(theme.colors[role: MonarchPostCardTokens.titleColor])
                        .multilineTextAlignment(.leading)
                    if let text = excerptText {
                        Text(text)
                            .monarchTextStyle(MonarchPostCardTokens.excerptStyle)
                            .foregroundStyle(theme.colors[role: MonarchPostCardTokens.excerptColor])
                            .multilineTextAlignment(.leading)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            footer
        }
    }

    private var excerptText: String? {
        guard let excerpt else { return nil }
        let text = MonarchPostCardTokens.excerpt(excerpt, max: excerptLength)
        return text.isEmpty ? nil : text
    }

    private var meta: some View {
        MonarchFlowLayout(spacing: MonarchPostCardTokens.metaSpacing) {
            MonarchAvatar(
                url: authorAvatarURL,
                name: author,
                size: MonarchPostCardTokens.avatarSize,
                isDecorative: true
            )
            Text(author).monarchTextStyle(MonarchPostCardTokens.metaStyle, weight: .semibold)
            if let publishedAt {
                if let now {
                    timeText(publishedAt, now: now)
                } else {
                    TimelineView(MonarchRelativeTimeSchedule(dates: [publishedAt])) { context in
                        timeText(publishedAt, now: context.date)
                    }
                }
            }
            if isPinned {
                HStack(spacing: MonarchPostCardTokens.iconSpacing) {
                    MonarchIcon(systemName: MonarchPostCardTokens.pinnedSymbol, size: .sm)
                    Text(labels.pinned ?? MonarchPostCardStrings.pinned)
                        .monarchTextStyle(MonarchPostCardTokens.metaStyle)
                }
            }
            if isAnswered {
                MonarchBadge(
                    labels.answered ?? MonarchPostCardStrings.answered,
                    tone: .success
                )
            }
            ForEach(tags) { tag in
                MonarchBadge(tag.label, tone: tag.tone)
            }
        }
        .foregroundStyle(theme.colors[role: MonarchPostCardTokens.metaColor])
    }

    private func timeText(_ date: Date, now: Date) -> some View {
        Text(MonarchPostCardTokens.relativeTime(date, now: now, locale: locale))
            .monarchTextStyle(MonarchPostCardTokens.metaStyle)
    }

    @ViewBuilder private var footer: some View {
        if reactionCount != nil || commentCount != nil || Actions.self != EmptyView.self {
            HStack(spacing: MonarchPostCardTokens.metaSpacing) {
                counts
                Spacer(minLength: 0)
                actions
            }
        }
    }

    @ViewBuilder private var counts: some View {
        if reactionCount != nil || commentCount != nil {
            HStack(spacing: theme.spacing.space4) {
                if let reactionCount {
                    count(reactionCount, symbol: MonarchPostCardTokens.reactionsSymbol)
                        .accessibilityLabel(MonarchPostCardTokens.reactions(reactionCount, labels: labels))
                }
                if let commentCount {
                    count(commentCount, symbol: MonarchPostCardTokens.commentsSymbol)
                        .accessibilityLabel(MonarchPostCardTokens.comments(commentCount, labels: labels))
                }
            }
            .foregroundStyle(theme.colors[role: MonarchPostCardTokens.metaColor])
            .accessibilityElement(children: .combine)
        }
    }

    private func count(_ value: Int, symbol: String) -> some View {
        HStack(spacing: MonarchPostCardTokens.iconSpacing) {
            MonarchIcon(systemName: symbol, size: .sm)
            Text(value.formatted()).monarchTextStyle(MonarchPostCardTokens.metaStyle).monospacedDigit()
        }
        .accessibilityElement(children: .ignore)
    }
}

extension MonarchPostCard where Actions == EmptyView {
    /// A post card without controls in the footer.
    ///
    /// - Parameters:
    ///   - title: The post's title, already localised.
    ///   - author: Who wrote it, already localised.
    ///   - authorAvatarURL: The author's picture, loaded with `AsyncImage`.
    ///   - excerpt: Plain text of the post, shortened to `excerptLength` characters.
    ///   - excerptLength: The longest excerpt, in characters.
    ///   - publishedAt: When it was published, shown relative to now.
    ///   - now: Fixes "now" for the relative time, for tests and previews. The clock by default.
    ///   - tags: Markers after the time, such as the category or "In review".
    ///   - isPinned: Shows the pinned marker.
    ///   - isAnswered: Shows the answered badge, for a question with an accepted answer.
    ///   - reactionCount: The number of reactions. Hidden when `nil`.
    ///   - commentCount: The number of comments. Hidden when `nil`.
    ///   - labels: Replacements for the built-in strings.
    ///   - action: Opens the post.
    public init(
        _ title: String,
        author: String,
        authorAvatarURL: URL? = nil,
        excerpt: String? = nil,
        excerptLength: Int = 220,
        publishedAt: Date? = nil,
        now: Date? = nil,
        tags: [MonarchPostCardTag] = [],
        isPinned: Bool = false,
        isAnswered: Bool = false,
        reactionCount: Int? = nil,
        commentCount: Int? = nil,
        labels: MonarchPostCardLabels = MonarchPostCardLabels(),
        action: @escaping @MainActor () -> Void
    ) {
        self.init(
            title,
            author: author,
            authorAvatarURL: authorAvatarURL,
            excerpt: excerpt,
            excerptLength: excerptLength,
            publishedAt: publishedAt,
            now: now,
            tags: tags,
            isPinned: isPinned,
            isAnswered: isAnswered,
            reactionCount: reactionCount,
            commentCount: commentCount,
            labels: labels,
            action: action,
            actions: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchPostCard") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        MonarchPreviewMatrix {
            VStack {
                MonarchPostCard(
                    "How do you run design critiques remotely?",
                    author: "Ana Silva",
                    excerpt:
                        "We moved our weekly critique online and attendance dropped. What has worked for your team?",
                    publishedAt: now.addingTimeInterval(-3 * 3_600),
                    now: now,
                    tags: [MonarchPostCardTag("Design"), MonarchPostCardTag("In review", tone: .warning)],
                    isPinned: true,
                    isAnswered: true,
                    reactionCount: 12,
                    commentCount: 4
                ) {
                } actions: {
                    Button("Save", systemImage: "bookmark") {}.labelStyle(.iconOnly)
                }
                MonarchPostCard("A post with only a title", author: "Ben Okafor") {}
            }
        }
    }
#endif
