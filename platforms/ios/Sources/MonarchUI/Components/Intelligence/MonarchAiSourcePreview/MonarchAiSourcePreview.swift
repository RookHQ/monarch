import SwiftUI

/// The content of a sheet that shows the document, record or page an AI
/// answer cited, with the quoted passage highlighted and scrolled into view,
/// and a button that opens the original.
///
/// ```swift
/// MonarchAiSourceList(items) { item in previewed = item.source }
///     .sheet(item: $previewed) { source in
///         MonarchAiSourcePreview(source, quote: quotes[source.id], content: texts[source.id])
///     }
/// ```
///
/// Present it with SwiftUI's `sheet(item:)`: the view brings its own
/// navigation bar with a Close button, medium and large detents and the drag
/// indicator. Content is plain text, split into paragraphs on blank lines;
/// the first passage that matches `quote`, ignoring case and spacing, is
/// highlighted. When the quote is not in the content, or there is no content,
/// the quote is shown on its own above it.
///
/// Accessibility: the title has the header trait. The highlighted passage
/// has a fill and an underline, and its paragraph reads "Quoted passage" as
/// its value, so the highlight does not depend on colour. The passage is
/// scrolled into view when the sheet opens, without animation when Reduce
/// Motion is on. "Open original" is a link with a 44-point touch target, shown
/// only for a safe `http`, `https` or `mailto` address.
public struct MonarchAiSourcePreview: View {
    private let source: MonarchAiSourcePart
    private let quote: String?
    private let content: String?
    private let originalURL: URL?
    private let labels: MonarchAiSourcePreviewLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dismiss) private var dismiss

    /// A source preview.
    ///
    /// - Parameters:
    ///   - source: The cited source.
    ///   - quote: The cited passage, highlighted in `content` and scrolled into view.
    ///   - content: The text of the document, record or page. Paragraphs are separated by blank lines.
    ///   - originalURL: Where "Open original" goes. Defaults to the source's address. Shown only when safe.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ source: MonarchAiSourcePart,
        quote: String? = nil,
        content: String? = nil,
        originalURL: URL? = nil,
        labels: MonarchAiSourcePreviewLabels = MonarchAiSourcePreviewLabels()
    ) {
        self.source = source
        self.quote = quote
        self.content = content
        self.originalURL = originalURL
        self.labels = labels
    }

    private var safeOriginal: URL? {
        MonarchAiSourceFormat.safeURL(originalURL?.absoluteString ?? source.url)
    }

    /// The content of the view.
    public var body: some View {
        let paragraphs = MonarchAiSourcePreviewTokens.paragraphs(content ?? "", quote: quote)
        NavigationStack {
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: MonarchAiSourcePreviewTokens.sectionSpacing) {
                        header
                        if MonarchAiSourcePreviewTokens.showsQuoteBlock(quote: quote, content: content), let quote {
                            quoteBlock(quote)
                        }
                        VStack(alignment: .leading, spacing: MonarchAiSourcePreviewTokens.paragraphSpacing) {
                            ForEach(paragraphs) { paragraph in
                                paragraphText(paragraph).id(paragraph.id)
                            }
                        }
                        if MonarchAiSourcePreviewTokens.isEmpty(quote: quote, content: content) {
                            Text(labels.noPreview ?? MonarchAiSourcePreviewStrings.noPreview)
                                .monarchTextStyle(.body)
                                .foregroundStyle(theme.colors[role: MonarchAiSourcePreviewTokens.supportingColor])
                        }
                    }
                    .padding(MonarchAiSourcePreviewTokens.padding)
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .task(id: quote) {
                    guard let marked = paragraphs.first(where: \.isMarked) else { return }
                    // Wait for the sheet's first layout before scrolling.
                    await Task.yield()
                    withAnimation(theme.motion.animation(.base, reduceMotion: reduceMotion)) {
                        proxy.scrollTo(marked.id, anchor: .center)
                    }
                }
            }
            .safeAreaInset(edge: .bottom) { footer }
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(labels.close ?? MonarchAiSourcePreviewStrings.close) { dismiss() }
                }
            }
        }
        .foregroundStyle(theme.colors.textDefault)
        .tint(theme.colors.accentStrong)
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
        .presentationBackground(theme.colors[role: MonarchAiSourcePreviewTokens.background])
    }

    private var header: some View {
        let kindName = labels.kind?(source.kind) ?? MonarchAiSourceListStrings.kind(source.kind)
        let meta = MonarchAiSourceListTokens.meta(source, kindName: kindName)
        return VStack(alignment: .leading, spacing: MonarchAiSourcePreviewTokens.textSpacing) {
            Text(source.title)
                .monarchTextStyle(.heading)
                .accessibilityAddTraits(.isHeader)
            HStack(alignment: .firstTextBaseline, spacing: MonarchAiSourcePreviewTokens.glyphSpacing) {
                if let favicon = source.favicon, let url = URL(string: favicon) {
                    MonarchFavicon(url: url, name: MonarchAiSourceFormat.host(source.url) ?? source.title)
                } else {
                    MonarchIcon(systemName: MonarchAiSourceFormat.symbol(source.kind), size: .sm)
                }
                Text(meta.isEmpty ? kindName : meta)
                    .monarchTextStyle(.caption)
            }
            .foregroundStyle(theme.colors[role: MonarchAiSourcePreviewTokens.supportingColor])
        }
    }

    private func quoteBlock(_ quote: String) -> some View {
        let quoted = labels.quoted ?? MonarchAiSourcePreviewStrings.quoted
        return HStack(alignment: .top, spacing: MonarchAiSourcePreviewTokens.quoteBarSpacing) {
            Rectangle()
                .fill(theme.colors[role: MonarchAiSourcePreviewTokens.quoteBar])
                .frame(width: MonarchAiSourcePreviewTokens.quoteBarWidth)
            VStack(alignment: .leading, spacing: MonarchAiSourcePreviewTokens.textSpacing) {
                Text(quoted)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchAiSourcePreviewTokens.supportingColor])
                Text(marked(quote))
                    .monarchTextStyle(.body)
            }
        }
        .fixedSize(horizontal: false, vertical: true)
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder private func paragraphText(_ paragraph: MonarchAiSourcePreviewTokens.Paragraph) -> some View {
        let text = Text(AttributedString(paragraph.before) + marked(paragraph.mark) + AttributedString(paragraph.after))
            .monarchTextStyle(.body)
            .textSelection(.enabled)
            .frame(maxWidth: .infinity, alignment: .leading)
        if paragraph.isMarked {
            text.accessibilityValue(labels.quoted ?? MonarchAiSourcePreviewStrings.quoted)
        } else {
            text
        }
    }

    private func marked(_ text: String) -> AttributedString {
        var marked = AttributedString(text)
        guard !text.isEmpty else { return marked }
        marked.backgroundColor = theme.colors[role: MonarchAiSourcePreviewTokens.markBackground]
        marked.underlineStyle = Text.LineStyle(
            pattern: .solid,
            color: theme.colors[role: MonarchAiSourcePreviewTokens.markUnderline]
        )
        return marked
    }

    @ViewBuilder private var footer: some View {
        if let url = safeOriginal {
            Link(destination: url) {
                Label(
                    labels.openOriginal ?? MonarchAiSourcePreviewStrings.openOriginal,
                    systemImage: "arrow.up.forward.square"
                )
            }
            .buttonStyle(.monarch(.secondary))
            .padding(MonarchAiSourcePreviewTokens.padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.colors[role: MonarchAiSourcePreviewTokens.background])
        }
    }
}

#if DEBUG
    #Preview("MonarchAiSourcePreview") {
        MonarchPreviewMatrix {
            MonarchAiSourcePreview(
                MonarchAiSourcePart(
                    id: "s1",
                    title: "Quarterly planning notes",
                    url: "https://www.example.com/notes/q3",
                    kind: .doc,
                    product: "Docs"
                ),
                quote: "revenue grew 12%",
                content:
                    "Planning for the third quarter.\n\nRevenue grew 12% against the same quarter last year.\n\nHiring stays flat."
            )
        }
    }
#endif
