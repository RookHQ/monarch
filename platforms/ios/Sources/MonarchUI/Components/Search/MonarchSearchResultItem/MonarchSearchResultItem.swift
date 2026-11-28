import MonarchTokens
import SwiftUI

/// One search result: a title, a breadcrumb path, a snippet with the matched
/// words highlighted, an optional icon and optional trailing details.
///
/// ```swift
/// MonarchSearchResultItem(
///     "Travel policy",
///     path: ["Handbook", "Finance"],
///     snippet: "Book travel at least two weeks ahead.",
///     terms: MonarchSearchResultItem.terms(from: query),
///     fileName: "travel-policy.pdf"
/// ) {
///     open(result)
/// }
/// ```
///
/// Text is always shown as text: `<mark>` markers from a search service are
/// read only when `snippetHasMarkTags` is true, and nothing else is parsed.
/// Matches are drawn from an `AttributedString` with the subtle warning fill
/// and a semibold weight, as the web's `mark`.
///
/// With an `action` the whole row is one `Button` and the title takes the
/// link colour; the `.neutral` tone keeps it in the default text colour, as a
/// command palette wants. Without one the row is plain content, for example inside a
/// `NavigationLink` or a `List` row with its own selection.
///
/// Accessibility: the row is one element that reads the title, then
/// "Location:" and the path segments, then the snippet without markers, then
/// the meta. With an action it has the button trait. The icon is decorative.
/// Text wraps and scales with Dynamic Type; at accessibility sizes the meta
/// moves under the text.
public struct MonarchSearchResultItem<Meta: View>: View {
    private let title: String
    private let path: [String]
    private let snippet: String?
    private let terms: [String]
    private let titleRanges: [Range<Int>]?
    private let snippetRanges: [Range<Int>]?
    private let snippetHasMarkTags: Bool
    private let systemImage: String?
    private let fileName: String?
    private let pathLabel: String?
    private let tone: MonarchTone?
    private let action: (@MainActor () -> Void)?
    private let meta: Meta

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo: .body) private var iconTile = MonarchSearchResultItemTokens.iconTile

    /// A search result with trailing details.
    ///
    /// - Parameters:
    ///   - title: The title, as plain text.
    ///   - path: Breadcrumb segments, such as `["Handbook", "Travel"]`.
    ///   - snippet: A short excerpt of the matching content, as plain text.
    ///   - terms: Words to highlight in the title and the snippet. Use
    ///     ``terms(from:)`` to split what the person typed.
    ///   - titleRanges: Exact matches in the title, as UTF-16 offsets. Replaces `terms` for the title.
    ///   - snippetRanges: Exact matches in the snippet, as UTF-16 offsets. Replaces `terms` for the snippet.
    ///   - snippetHasMarkTags: Whether the snippet carries `<mark>` and `</mark>`
    ///     markers from the search service. Replaces `terms` and `snippetRanges`.
    ///   - systemImage: An SF Symbol in the leading tile.
    ///   - fileName: A file name whose type icon fills the leading tile. Takes
    ///     precedence over `systemImage`.
    ///   - pathLabel: Replaces the built-in "Location" read before the path.
    ///   - tone: A status tone for the title and the icon, such as ``MonarchTone/danger`` for a destructive command.
    ///   - action: Runs when the row is activated. Without it the row is not a button.
    ///   - meta: Trailing details, such as a badge or a date.
    public init(
        _ title: String,
        path: [String] = [],
        snippet: String? = nil,
        terms: [String] = [],
        titleRanges: [Range<Int>]? = nil,
        snippetRanges: [Range<Int>]? = nil,
        snippetHasMarkTags: Bool = false,
        systemImage: String? = nil,
        fileName: String? = nil,
        pathLabel: String? = nil,
        tone: MonarchTone? = nil,
        action: (@MainActor () -> Void)? = nil,
        @ViewBuilder meta: () -> Meta
    ) {
        self.title = title
        self.path = path
        self.snippet = snippet
        self.terms = terms
        self.titleRanges = titleRanges
        self.snippetRanges = snippetRanges
        self.snippetHasMarkTags = snippetHasMarkTags
        self.systemImage = systemImage
        self.fileName = fileName
        self.pathLabel = pathLabel
        self.tone = tone
        self.action = action
        self.meta = meta()
    }

    /// The content of the view.
    public var body: some View {
        if let action {
            Button(action: action) { row(isInteractive: true) }
                .buttonStyle(MonarchSearchResultItemRowStyle())
        } else {
            row(isInteractive: false)
                .accessibilityElement(children: .combine)
        }
    }

    private func row(isInteractive: Bool) -> some View {
        let isStacked = dynamicTypeSize.isAccessibilitySize
        return HStack(alignment: .top, spacing: MonarchSearchResultItemTokens.spacing) {
            icon
            VStack(alignment: .leading, spacing: MonarchSearchResultItemTokens.lineSpacing) {
                text(isInteractive: isInteractive)
                if isStacked { metaView }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if !isStacked { metaView }
        }
        .padding(MonarchSearchResultItemTokens.padding)
        .frame(minHeight: MonarchAccessibility.minimumHitTarget)
        .contentShape(RoundedRectangle(cornerRadius: MonarchSearchResultItemTokens.rowRadius, style: .continuous))
    }

    @ViewBuilder
    private var icon: some View {
        if fileName != nil || systemImage != nil {
            Group {
                if let fileName {
                    MonarchFileTypeIcon(fileName: fileName, size: .sm, isDecorative: true)
                } else if let systemImage {
                    MonarchIcon(systemName: systemImage, size: .sm)
                }
            }
            .foregroundStyle(
                tone.flatMap(MonarchSearchResultItemTokens.toneRole).map { theme.colors[role: $0] }
                    ?? theme.colors.textMuted
            )
            .frame(width: iconTile, height: iconTile)
            .background(
                theme.colors.bgMuted,
                in: RoundedRectangle(cornerRadius: MonarchSearchResultItemTokens.iconRadius, style: .continuous)
            )
            .accessibilityHidden(true)
        }
    }

    @ViewBuilder
    private func text(isInteractive: Bool) -> some View {
        Text(highlighted(title, ranges: titleRanges, markTags: false, style: MonarchSearchResultItemTokens.titleStyle))
            .monarchTextStyle(
                MonarchSearchResultItemTokens.titleStyle,
                weight: MonarchSearchResultItemTokens.titleWeight
            )
            .foregroundStyle(
                theme.colors[
                    role: tone == .neutral
                        ? \.textDefault
                        : tone.flatMap(MonarchSearchResultItemTokens.toneRole)
                            ?? MonarchSearchResultItemTokens.titleRole(isInteractive: isInteractive)
                ]
            )
            .multilineTextAlignment(.leading)
            .fixedSize(horizontal: false, vertical: true)
        let pathLabel = pathLabel ?? MonarchSearchResultItemStrings.path
        if let spoken = MonarchSearchResultItemTokens.pathAccessibilityLabel(path, pathLabel: pathLabel) {
            MonarchFlowLayout(spacing: MonarchSearchResultItemTokens.pathSpacing) {
                ForEach(Array(path.filter { !$0.isEmpty }.enumerated()), id: \.offset) { index, segment in
                    if index > 0 {
                        Text(MonarchSearchResultItemTokens.pathSeparator)
                            .foregroundStyle(theme.colors.textSubtle)
                    }
                    Text(segment)
                }
            }
            .monarchTextStyle(MonarchSearchResultItemTokens.detailStyle)
            .foregroundStyle(theme.colors.textMuted)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(spoken)
        }
        if let snippet, !snippet.isEmpty {
            Text(
                highlighted(
                    snippet,
                    ranges: snippetRanges,
                    markTags: snippetHasMarkTags,
                    style: MonarchSearchResultItemTokens.snippetStyle
                )
            )
            .monarchTextStyle(MonarchSearchResultItemTokens.snippetStyle)
            .foregroundStyle(theme.colors.textMuted)
            .lineLimit(MonarchSearchResultItemTokens.snippetLineLimit)
            .multilineTextAlignment(.leading)
            .accessibilityLabel(MonarchSearchResultItemTokens.plainText(snippet, markTags: snippetHasMarkTags))
        }
    }

    private var metaView: some View {
        meta
            .monarchTextStyle(MonarchSearchResultItemTokens.detailStyle)
            .foregroundStyle(theme.colors.textMuted)
    }

    private func highlighted(_ text: String, ranges: [Range<Int>]?, markTags: Bool, style: MonarchTextStyle)
        -> AttributedString
    {
        let segments = MonarchSearchResultItemTokens.segments(text, terms: terms, ranges: ranges, markTags: markTags)
        var result = AttributedString()
        for segment in segments {
            var run = AttributedString(segment.text)
            if segment.isMatch {
                run.backgroundColor = theme.colors[role: MonarchSearchResultItemTokens.highlightBackground]
                run.foregroundColor = theme.colors[role: MonarchSearchResultItemTokens.highlightForeground]
                run.inlinePresentationIntent = .stronglyEmphasized
            }
            result += run
        }
        return result.monarchStrongFaces(theme: theme, style: style)
    }
}

extension MonarchSearchResultItem where Meta == EmptyView {
    /// A search result without trailing details.
    ///
    /// - Parameters:
    ///   - title: The title, as plain text.
    ///   - path: Breadcrumb segments, such as `["Handbook", "Travel"]`.
    ///   - snippet: A short excerpt of the matching content, as plain text.
    ///   - terms: Words to highlight in the title and the snippet.
    ///   - titleRanges: Exact matches in the title, as UTF-16 offsets.
    ///   - snippetRanges: Exact matches in the snippet, as UTF-16 offsets.
    ///   - snippetHasMarkTags: Whether the snippet carries `<mark>` markers.
    ///   - systemImage: An SF Symbol in the leading tile.
    ///   - fileName: A file name whose type icon fills the leading tile.
    ///   - pathLabel: Replaces the built-in "Location" read before the path.
    ///   - tone: A status tone for the title and the icon, such as ``MonarchTone/danger`` for a destructive command.
    ///   - action: Runs when the row is activated. Without it the row is not a button.
    public init(
        _ title: String,
        path: [String] = [],
        snippet: String? = nil,
        terms: [String] = [],
        titleRanges: [Range<Int>]? = nil,
        snippetRanges: [Range<Int>]? = nil,
        snippetHasMarkTags: Bool = false,
        systemImage: String? = nil,
        fileName: String? = nil,
        pathLabel: String? = nil,
        tone: MonarchTone? = nil,
        action: (@MainActor () -> Void)? = nil
    ) {
        self.init(
            title,
            path: path,
            snippet: snippet,
            terms: terms,
            titleRanges: titleRanges,
            snippetRanges: snippetRanges,
            snippetHasMarkTags: snippetHasMarkTags,
            systemImage: systemImage,
            fileName: fileName,
            pathLabel: pathLabel,
            tone: tone,
            action: action,
            meta: { EmptyView() }
        )
    }

    /// Splits what the person typed into the words to highlight: letters and
    /// digits only, lower-cased, without repeats, at most eight.
    nonisolated public static func terms(from query: String) -> [String] {
        MonarchSearchResultItemTokens.terms(from: query)
    }
}

/// Draws the pressed fill of an interactive row.
private struct MonarchSearchResultItemRowStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        MonarchSearchResultItemRowSurface(configuration: configuration)
    }
}

/// Reads the theme, which a style value cannot do itself.
private struct MonarchSearchResultItemRowSurface: View {
    let configuration: ButtonStyleConfiguration

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchSearchResultItemTokens.rowRadius, style: .continuous)
        configuration.label
            .background {
                if let fill = MonarchSearchResultItemTokens.rowFill(isPressed: configuration.isPressed) {
                    shape.fill(theme.colors[role: fill])
                }
            }
            .monarchAnimation(.fast, value: configuration.isPressed)
    }
}

#if DEBUG
    #Preview("MonarchSearchResultItem") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchSearchResultItem(
                    "Travel policy",
                    path: ["Handbook", "Finance"],
                    snippet: "Book travel at least two weeks ahead and keep receipts.",
                    terms: ["travel"],
                    fileName: "travel-policy.pdf",
                    action: {},
                    meta: { MonarchBadge("PDF") }
                )
                MonarchSearchResultItem(
                    "Quarterly planning",
                    snippet: "The <mark>roadmap</mark> for the next quarter.",
                    snippetHasMarkTags: true,
                    systemImage: "doc.text"
                )
            }
        }
    }
#endif
