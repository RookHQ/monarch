import MonarchTokens
import SwiftUI

/// Renders GitHub-flavoured Markdown written by a model as SwiftUI text and
/// layout, never as HTML: headings, paragraphs with emphasis, strikethrough,
/// code spans and links, ordered, unordered, nested and task lists,
/// quotations, fenced code, tables, rules and footnotes.
///
/// ```swift
/// MonarchAiMarkdown(answer.text, isStreaming: answer.isStreaming)
/// ```
///
/// Built by Monarch: SwiftUI's `AttributedString(markdown:)` reads inline
/// Markdown only and does not lay out blocks. The parser is a port of the
/// web's, so every platform reads the same answer the same way, and it is
/// incremental: while `text` grows during streaming, only the last block is
/// parsed again. The source is untrusted: raw HTML stays text, a link is a
/// link only for `http`, `https` or `mailto` (or a relative link when
/// `onOpenLink` handles it) and keeps its text otherwise, links show their
/// host when the text does not name it, and images show their alt text
/// unless the app renders them with ``image(_:)``. Fenced code renders in
/// ``MonarchCodeBlock``, or in ``codeBlock(_:)``.
///
/// Links open through the environment's `openURL` action, or through
/// `onOpenLink`. The column does not scroll: put it in the message list.
///
/// Accessibility: headings have the header trait and their level, so the
/// rotor moves between them; links are underlined, not told apart by colour
/// alone, and reachable through the rotor; ordered markers are read and
/// bullets are not; a task's check mark is read as a dimmed checkbox named
/// "Task" with its state; a table is named "Table" with its size, and its
/// header cells have the header trait. Paragraphs are selectable.
public struct MonarchAiMarkdown: View {
    private let text: String
    private let isStreaming: Bool
    private let showsLinkHosts: Bool
    private let labels: MonarchAiMarkdownLabels
    private let onOpenLink: (@MainActor (URL) -> Void)?
    private var imageContent: (@MainActor (URL, String) -> AnyView)?
    private var codeBlockContent: (@MainActor (String, String, Bool) -> AnyView)?

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var bodySize = MonarchAiMarkdownTokens.bodySize
    @State private var cache = MonarchAiMarkdownCache()

    /// Markdown from a model.
    ///
    /// - Parameters:
    ///   - text: The Markdown source, treated as untrusted.
    ///   - isStreaming: Whether the text is still arriving. An unclosed code
    ///     block is then incomplete: ``MonarchCodeBlock`` hides its copy
    ///     button and ``codeBlock(_:)`` receives `false`.
    ///   - showsLinkHosts: Whether the host follows a link whose text does not
    ///     name it, such as "Read more (example.com)".
    ///   - labels: Replaces the built-in strings.
    ///   - onOpenLink: Opens links in the app instead of the environment's
    ///     `openURL` action. With it, relative links are links too.
    public init(
        _ text: String,
        isStreaming: Bool = false,
        showsLinkHosts: Bool = true,
        labels: MonarchAiMarkdownLabels = MonarchAiMarkdownLabels(),
        onOpenLink: (@MainActor (URL) -> Void)? = nil
    ) {
        self.text = text
        self.isStreaming = isStreaming
        self.showsLinkHosts = showsLinkHosts
        self.labels = labels
        self.onOpenLink = onOpenLink
    }

    /// Renders every paragraph that is a single image with an `http` or
    /// `https` source and alt text. Monarch loads no images; without this the
    /// alt text is shown.
    ///
    /// - Parameter content: The view for an image, given its URL and alt text.
    /// - Returns: The Markdown view with the image slot.
    public func image<Content: View>(
        @ViewBuilder _ content: @escaping @MainActor (_ url: URL, _ alt: String) -> Content
    ) -> MonarchAiMarkdown {
        var copy = self
        copy.imageContent = { url, alt in AnyView(content(url, alt)) }
        return copy
    }

    /// Renders every code block instead of ``MonarchCodeBlock``, for example
    /// a diagram for `mermaid` or typeset maths for `math` (display maths
    /// written as `$$` or `\[` on their own line).
    ///
    /// - Parameter content: The view for a code block, given its language
    ///   (lower-cased, empty when none), its code and whether it is complete.
    /// - Returns: The Markdown view with the code block slot.
    public func codeBlock<Content: View>(
        @ViewBuilder _ content:
            @escaping @MainActor (_ language: String, _ code: String, _ isComplete: Bool) -> Content
    ) -> MonarchAiMarkdown {
        var copy = self
        copy.codeBlockContent = { language, code, isComplete in AnyView(content(language, code, isComplete)) }
        return copy
    }

    /// The content of the view.
    public var body: some View {
        let state = cache.parse(text)
        let footnotes = MonarchAiMarkdownFootnotes.of(state.blocks)
        let renderer = MonarchAiMarkdownInlineRenderer(
            theme: theme,
            footnoteNumbers: footnotes.numbers,
            showsLinkHosts: showsLinkHosts,
            allowsRelativeLinks: onOpenLink != nil,
            linkHost: labels.linkHost,
            footnoteFont: MonarchAiMarkdownTokens.footnoteFont(
                family: theme.typography.fontFamily,
                bodySize: bodySize
            ),
            footnoteRaise: bodySize * MonarchAiMarkdownTokens.footnoteRaise,
            strongFont: MonarchAiMarkdownTokens.inlineFont(theme: theme, bodySize: bodySize, strong: true),
            emphasisFont: MonarchAiMarkdownTokens.inlineFont(theme: theme, bodySize: bodySize, strong: false)
        )
        let context = MonarchAiMarkdownContext(
            renderer: renderer,
            labels: labels,
            isStreaming: isStreaming,
            image: imageContent,
            codeBlock: codeBlockContent
        )
        let content = VStack(alignment: .leading, spacing: 0) {
            MonarchAiMarkdownBlocks(blocks: state.blocks, context: context, depth: 0, isTight: false, isMuted: false)
            if !footnotes.ordered.isEmpty {
                MonarchAiMarkdownFootnotesView(footnotes: footnotes, context: context)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
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
}

/// Keeps the last parse of ``MonarchAiMarkdown`` so a growing text is parsed
/// incrementally. A reference, so the view can update it while rendering.
@MainActor
final class MonarchAiMarkdownCache {
    private var state: MonarchAiMarkdownParseState?

    /// The blocks of `text`, reusing the previous parse where the text only grew.
    func parse(_ text: String) -> MonarchAiMarkdownParseState {
        let next = MonarchAiMarkdownParser.parseIncremental(text, previous: state)
        state = next
        return next
    }
}

#if DEBUG
    #Preview("MonarchAiMarkdown") {
        MonarchPreviewMatrix {
            MonarchAiMarkdown(
                """
                ## Plan
                Ship the **beta** on *Monday*, then read the [notes](https://example.com/notes)[^1].

                1. Freeze the branch
                2. Run `make release`

                - [x] Write the changelog
                - [ ] Tell support

                > Small steps every day.

                | Plan | Seats | Price |
                | :--- | :---: | ----: |
                | Team | 10 | $90 |

                ```swift
                let total = items.reduce(0, +)
                ```

                [^1]: Updated every Friday.
                """
            )
        }
    }
#endif
