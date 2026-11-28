import MonarchTokens
import SwiftUI

/// A Markdown answer that grows while a model writes it. Finished blocks
/// stay put; only the last block updates as tokens arrive, with half-written
/// syntax closed for display so nothing flashes; VoiceOver hears whole
/// sentences, never single tokens.
///
/// ```swift
/// MonarchAiStreamingText(message.text, isStreaming: message.isStreaming)
/// ```
///
/// Built on ``MonarchAiMarkdown``'s parser and block views, in a `VStack`
/// with stable identity: the text is parsed incrementally, every block
/// before the last keeps its parse and its view (a finished block is not
/// drawn again while later ones arrive), and the last block's source is
/// completed (open emphasis and code spans closed, a link whose target is
/// still arriving shown as its text, table rows held back until the
/// delimiter row arrives) before it is parsed on its own. While text
/// arrives a caret follows the last character.
///
/// Use ``MonarchAiMarkdown`` for an answer that is already complete, such as
/// history.
///
/// Accessibility: while `isStreaming`, complete sentences and finished
/// blocks are announced politely at most once per `announceInterval`
/// (never below one second), with a code block or a table announced by
/// name rather than read out, then "Response complete". Text that never
/// streamed here is not announced. The caret is not drawn while VoiceOver
/// is running, so it is never read. Headings, links, lists and tables keep
/// the accessibility of ``MonarchAiMarkdown``.
public struct MonarchAiStreamingText: View {
    private let text: String
    private let isStreaming: Bool
    private let announces: Bool
    private let announceInterval: Duration
    private let announcesCompletion: Bool
    private let showsCaret: Bool
    private let showsLinkHosts: Bool
    private let labels: MonarchAiStreamingTextLabels
    private let markdownLabels: MonarchAiMarkdownLabels
    private let onOpenLink: (@MainActor (URL) -> Void)?
    private var imageContent: (@MainActor (URL, String) -> AnyView)?
    private var codeBlockContent: (@MainActor (String, String, Bool) -> AnyView)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityVoiceOverEnabled) private var isVoiceOverOn
    @ScaledMetric(relativeTo: .body) private var bodySize = MonarchAiMarkdownTokens.bodySize
    @State private var model = MonarchAiStreamingTextModel()
    @State private var retryAt: Date?

    /// A streaming Markdown answer.
    ///
    /// - Parameters:
    ///   - text: The Markdown received so far, treated as untrusted. It
    ///     normally only grows while `isStreaming`.
    ///   - isStreaming: Whether more text is arriving: shows the caret and
    ///     announces sentences.
    ///   - announces: Whether new sentences, then "Response complete", are
    ///     announced to VoiceOver. Turn it off when the surrounding thread
    ///     announces messages itself.
    ///   - announceInterval: The minimum time between announcements. Values
    ///     below one second are raised to one second.
    ///   - announcesCompletion: Whether "Response complete" may be announced
    ///     when this text stops streaming. Pass `false` while the reply goes
    ///     on after this text (a tool runs), then `true` once it finishes.
    ///   - showsCaret: Whether the caret follows the last character while streaming.
    ///   - showsLinkHosts: Whether the host follows a link whose text does not name it.
    ///   - labels: Replaces the spoken strings this view adds.
    ///   - markdownLabels: Replaces the strings of the Markdown rendering.
    ///   - onOpenLink: Opens links in the app instead of the environment's
    ///     `openURL` action. With it, relative links are links too.
    public init(
        _ text: String,
        isStreaming: Bool = false,
        announces: Bool = true,
        announceInterval: Duration = .seconds(1),
        announcesCompletion: Bool = true,
        showsCaret: Bool = true,
        showsLinkHosts: Bool = true,
        labels: MonarchAiStreamingTextLabels = MonarchAiStreamingTextLabels(),
        markdownLabels: MonarchAiMarkdownLabels = MonarchAiMarkdownLabels(),
        onOpenLink: (@MainActor (URL) -> Void)? = nil
    ) {
        self.text = text
        self.isStreaming = isStreaming
        self.announces = announces
        self.announceInterval = announceInterval
        self.announcesCompletion = announcesCompletion
        self.showsCaret = showsCaret
        self.showsLinkHosts = showsLinkHosts
        self.labels = labels
        self.markdownLabels = markdownLabels
        self.onOpenLink = onOpenLink
    }

    /// Renders every paragraph that is a single image with an `http` or
    /// `https` source and alt text, as ``MonarchAiMarkdown/image(_:)`` does.
    ///
    /// - Parameter content: The view for an image, given its URL and alt text.
    /// - Returns: The streaming text with the image slot.
    public func image<Content: View>(
        @ViewBuilder _ content: @escaping @MainActor (_ url: URL, _ alt: String) -> Content
    ) -> MonarchAiStreamingText {
        var copy = self
        copy.imageContent = { url, alt in AnyView(content(url, alt)) }
        return copy
    }

    /// Renders every code block instead of ``MonarchCodeBlock``, as
    /// ``MonarchAiMarkdown/codeBlock(_:)`` does. `isComplete` is `false`
    /// while the closing fence has not arrived.
    ///
    /// - Parameter content: The view for a code block, given its language,
    ///   its code and whether it is complete.
    /// - Returns: The streaming text with the code block slot.
    public func codeBlock<Content: View>(
        @ViewBuilder _ content:
            @escaping @MainActor (_ language: String, _ code: String, _ isComplete: Bool) -> Content
    ) -> MonarchAiStreamingText {
        var copy = self
        copy.codeBlockContent = { language, code, isComplete in AnyView(content(language, code, isComplete)) }
        return copy
    }

    /// The content of the view.
    public var body: some View {
        let display = model.display(text, isStreaming: isStreaming)
        let footnotes = MonarchAiMarkdownFootnotes.of(display.all)
        let renderer = MonarchAiMarkdownInlineRenderer(
            theme: theme,
            footnoteNumbers: footnotes.numbers,
            showsLinkHosts: showsLinkHosts,
            allowsRelativeLinks: onOpenLink != nil,
            linkHost: markdownLabels.linkHost,
            footnoteFont: MonarchAiMarkdownTokens.footnoteFont(family: theme.typography.fontFamily, bodySize: bodySize),
            footnoteRaise: bodySize * MonarchAiMarkdownTokens.footnoteRaise,
            strongFont: MonarchAiMarkdownTokens.inlineFont(theme: theme, bodySize: bodySize, strong: true),
            emphasisFont: MonarchAiMarkdownTokens.inlineFont(theme: theme, bodySize: bodySize, strong: false)
        )
        let context = MonarchAiMarkdownContext(
            renderer: renderer,
            labels: markdownLabels,
            isStreaming: isStreaming,
            image: imageContent,
            codeBlock: codeBlockContent
        )
        let key = MonarchAiStreamingTextRenderKey(
            theme: theme,
            footnoteNumbers: footnotes.numbers,
            showsLinkHosts: showsLinkHosts,
            allowsRelativeLinks: onOpenLink != nil,
            bodySize: bodySize,
            isStreaming: isStreaming
        )
        let tail =
            isStreaming && showsCaret && !isVoiceOverOn
            ? MonarchAiStreamingTextTokens.withCaret(display.tail) ?? display.tail
            : display.tail
        let rows = MonarchAiStreamingTextRow.rows(finished: display.finished, tail: tail)
        let content = VStack(alignment: .leading, spacing: 0) {
            ForEach(rows) { row in
                if row.isFinished {
                    MonarchAiStreamingTextFinishedBlock(block: row.block, gap: row.gap, key: key, context: context)
                        .equatable()
                } else {
                    MonarchAiMarkdownBlockView(block: row.block, context: context, depth: 0, isMuted: false)
                        .padding(.top, row.gap)
                }
            }
            if !footnotes.ordered.isEmpty {
                MonarchAiMarkdownFootnotesView(footnotes: footnotes, context: context)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .onChange(of: text, initial: true) { announce(display) }
        .onChange(of: isStreaming) { announce(display) }
        .onChange(of: announcesCompletion) { announce(display) }
        .task(id: retryAt) {
            guard let retryAt else { return }
            try? await Task.sleep(for: .seconds(max(0, retryAt.timeIntervalSinceNow)))
            guard !Task.isCancelled else { return }
            self.retryAt = nil
            announce(model.display(text, isStreaming: isStreaming))
        }
        if let onOpenLink {
            content.environment(
                \.openURL,
                OpenURLAction { url in
                    onOpenLink(url)
                    return .handled
                }
            )
        } else {
            content
        }
    }

    private func announce(_ display: MonarchAiStreamingTextTokens.Display) {
        guard announces else { return }
        let spoken = model.spoken(
            display,
            isStreaming: isStreaming,
            codeBlock: labels.codeBlock ?? MonarchAiStreamingTextStrings.codeBlock,
            table: labels.table ?? MonarchAiStreamingTextStrings.tableName
        )
        let outcome = model.announcer.update(
            spoken: spoken.text,
            done: spoken.done,
            isStreaming: isStreaming,
            announcesCompletion: announcesCompletion,
            complete: labels.complete ?? MonarchAiStreamingTextStrings.complete,
            interval: MonarchAiStreamingTextAnnouncer.interval(announceInterval),
            now: Date()
        )
        switch outcome {
        case .silent:
            break
        case .announce(let message):
            AccessibilityNotification.Announcement(message).post()
        case .wait(let until):
            if retryAt == nil { retryAt = until }
        }
    }
}

/// What a finished block's drawing depends on besides the block itself.
struct MonarchAiStreamingTextRenderKey: Equatable, Sendable {
    let theme: MonarchTheme
    let footnoteNumbers: [String: Int]
    let showsLinkHosts: Bool
    let allowsRelativeLinks: Bool
    let bodySize: CGFloat
    let isStreaming: Bool
}

/// One shown block with its position, its gap above and whether it is finished.
struct MonarchAiStreamingTextRow: Identifiable, Equatable {
    let id: Int
    let block: MonarchAiMarkdownBlock
    let gap: CGFloat
    let isFinished: Bool

    /// The rows to draw, footnote definitions left out (they are listed at
    /// the end). The id is the block's position, so a finished block keeps
    /// its view while the tail changes.
    static func rows(finished: [MonarchAiMarkdownBlock], tail: [MonarchAiMarkdownBlock]) -> [Self] {
        var rows: [Self] = []
        var previous: MonarchAiMarkdownBlock?
        for (index, block) in (finished + tail).enumerated() where !block.isFootnoteDefinition {
            rows.append(
                Self(
                    id: index,
                    block: block,
                    gap: MonarchAiMarkdownTokens.gap(before: block, after: previous, isTight: false),
                    isFinished: index < finished.count
                )
            )
            previous = block
        }
        return rows
    }
}

/// A finished block. Equatable on what it draws, so SwiftUI skips it while
/// later blocks stream in.
private struct MonarchAiStreamingTextFinishedBlock: View, Equatable {
    let block: MonarchAiMarkdownBlock
    let gap: CGFloat
    let key: MonarchAiStreamingTextRenderKey
    let context: MonarchAiMarkdownContext

    nonisolated static func == (lhs: Self, rhs: Self) -> Bool {
        lhs.block == rhs.block && lhs.gap == rhs.gap && lhs.key == rhs.key
    }

    var body: some View {
        MonarchAiMarkdownBlockView(block: block, context: context, depth: 0, isMuted: false)
            .padding(.top, gap)
    }
}

#if DEBUG
    #Preview("MonarchAiStreamingText") {
        MonarchPreviewMatrix {
            MonarchAiStreamingText(
                """
                ## Plan
                Ship the **beta** on Monday.

                1. Freeze the branch
                2. Run the *release
                """,
                isStreaming: true
            )
            MonarchAiStreamingText("Done. Everything shipped.")
        }
    }
#endif
