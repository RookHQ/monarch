import SwiftUI

/// The block of sources under an AI answer: each with its favicon or kind
/// icon, `[n]` number, title, host and snippet, grouped into "Used" and
/// "Considered" when the assistant reports both, and collapsed to the first
/// few when there are more.
///
/// ```swift
/// MonarchAiSourceList(MonarchAiSourceListItem.sources(of: message)) { item in
///     previewed = item.source
/// }
/// ```
///
/// With `onOpen` each source is a button, for example to show
/// ``MonarchAiSourcePreview`` in a sheet. Without it, a source with a safe
/// `http`, `https` or `mailto` address is a link opened through the
/// environment's `openURL`, and any other source is plain text.
///
/// Accessibility: the heading and the group headings have the header trait.
/// Each source is one element read as "Source 1: title", then the host,
/// product and kind, then the snippet; a source that opens is a button or a
/// link, at least 44 points high. The favicon and the kind icon are
/// decorative. The toggle is a separate button whose title says what it does.
public struct MonarchAiSourceList: View {
    private let items: [MonarchAiSourceListItem]
    private let collapseAfter: Int?
    private let isNumbered: Bool
    private let labels: MonarchAiSourceListLabels
    private let onOpen: (@MainActor (MonarchAiSourceListItem) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @State private var isExpanded: Bool

    /// A list of sources.
    ///
    /// - Parameters:
    ///   - items: The sources in citation order: the first is `[1]`. Use
    ///     ``MonarchAiSourceListItem/sources(of:)`` for the sources of a message.
    ///   - collapseAfter: How many sources show while the list is collapsed. `nil` never collapses.
    ///   - isInitiallyExpanded: Whether a collapsible list starts expanded.
    ///   - isNumbered: Whether each source shows the `[n]` number its citations use.
    ///   - labels: Replacements for the built-in strings.
    ///   - onOpen: Makes each source a button that calls this, for example to show a preview.
    public init(
        _ items: [MonarchAiSourceListItem],
        collapseAfter: Int? = 3,
        isInitiallyExpanded: Bool = false,
        isNumbered: Bool = true,
        labels: MonarchAiSourceListLabels = MonarchAiSourceListLabels(),
        onOpen: (@MainActor (MonarchAiSourceListItem) -> Void)? = nil
    ) {
        self.items = items
        self.collapseAfter = collapseAfter
        self.isNumbered = isNumbered
        self.labels = labels
        self.onOpen = onOpen
        _isExpanded = State(initialValue: isInitiallyExpanded)
    }

    /// The content of the view.
    public var body: some View {
        let layout = MonarchAiSourceListTokens.layout(items, collapseAfter: collapseAfter, isExpanded: isExpanded)
        VStack(alignment: .leading, spacing: MonarchAiSourceListTokens.sectionSpacing) {
            heading(labels.heading ?? MonarchAiSourceListStrings.heading)
            if layout.isGrouped {
                group(
                    labels.used?(layout.usedCount) ?? MonarchAiSourceListStrings.used(layout.usedCount),
                    entries: layout.used
                )
                group(
                    labels.considered?(layout.consideredCount)
                        ?? MonarchAiSourceListStrings.considered(layout.consideredCount),
                    entries: layout.considered
                )
            } else {
                rows(layout.used)
            }
            if layout.isCollapsible {
                MonarchButton(
                    toggleTitle,
                    systemImage: isExpanded ? "chevron.up" : "chevron.down",
                    variant: .ghost,
                    size: .sm
                ) {
                    isExpanded.toggle()
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .monarchAnimation(.fast, value: isExpanded)
        .accessibilityElement(children: .contain)
    }

    private var toggleTitle: String {
        isExpanded
            ? labels.showFewer ?? MonarchAiSourceListStrings.showFewer
            : labels.showAll?(items.count) ?? MonarchAiSourceListStrings.showAll(items.count)
    }

    private func heading(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.label)
            .foregroundStyle(theme.colors[role: MonarchAiSourceListTokens.headingColor])
            .accessibilityAddTraits(.isHeader)
    }

    @ViewBuilder private func group(_ title: String, entries: [MonarchAiSourceListTokens.Entry]) -> some View {
        if !entries.isEmpty {
            VStack(alignment: .leading, spacing: MonarchAiSourceListTokens.groupSpacing) {
                Text(title)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchAiSourceListTokens.headingColor])
                    .accessibilityAddTraits(.isHeader)
                rows(entries)
            }
        }
    }

    private func rows(_ entries: [MonarchAiSourceListTokens.Entry]) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(entries) { entry in
                row(entry)
            }
        }
    }

    @ViewBuilder private func row(_ entry: MonarchAiSourceListTokens.Entry) -> some View {
        let source = entry.item.source
        let meta = MonarchAiSourceListTokens.meta(source, kindName: kindName(source.kind))
        let label = MonarchAiSourceListTokens.accessibilityLabel(
            number: isNumbered ? entry.number : nil,
            title: source.title,
            meta: meta,
            snippet: entry.item.snippet,
            sourceLabel: { labels.source?($0, $1) ?? MonarchAiSourceListStrings.source($0, $1) }
        )
        if let onOpen {
            Button {
                onOpen(entry.item)
            } label: {
                rowContent(entry, meta: meta, isActionable: true)
            }
            .buttonStyle(MonarchAiSourceListRowStyle())
            .accessibilityLabel(label)
        } else if let url = MonarchAiSourceFormat.safeURL(source.url) {
            Link(destination: url) {
                rowContent(entry, meta: meta, isActionable: true)
            }
            .buttonStyle(MonarchAiSourceListRowStyle())
            .accessibilityLabel(label)
            .accessibilityHint(labels.opensSource ?? MonarchAiSourceListStrings.opensSource)
        } else {
            rowContent(entry, meta: meta, isActionable: false)
                .padding(.horizontal, MonarchAiSourceListTokens.rowHorizontalPadding)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
        }
    }

    private func rowContent(
        _ entry: MonarchAiSourceListTokens.Entry,
        meta: String,
        isActionable: Bool
    ) -> some View {
        let source = entry.item.source
        return HStack(alignment: .firstTextBaseline, spacing: MonarchAiSourceListTokens.glyphSpacing) {
            glyph(source)
            VStack(alignment: .leading, spacing: MonarchAiSourceListTokens.textSpacing) {
                HStack(alignment: .firstTextBaseline, spacing: MonarchAiSourceListTokens.numberSpacing) {
                    if isNumbered {
                        Text(verbatim: "[\(entry.number)]")
                            .monarchTextStyle(.caption)
                            .monospacedDigit()
                            .foregroundStyle(theme.colors[role: MonarchAiSourceListTokens.numberColor])
                    }
                    Text(source.title)
                        .monarchTextStyle(.bodyStrong)
                        .foregroundStyle(
                            theme.colors[role: MonarchAiSourceListTokens.titleColor(isActionable: isActionable)]
                        )
                }
                if !meta.isEmpty {
                    Text(meta)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: MonarchAiSourceListTokens.supportingColor])
                }
                if let snippet = entry.item.snippet, !snippet.isEmpty {
                    Text(snippet)
                        .monarchTextStyle(.caption)
                        .lineLimit(MonarchAiSourceListTokens.snippetLineLimit)
                        .foregroundStyle(theme.colors[role: MonarchAiSourceListTokens.supportingColor])
                }
            }
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, MonarchAiSourceListTokens.rowVerticalPadding)
        .frame(minHeight: MonarchAccessibility.minimumHitTarget)
    }

    @ViewBuilder private func glyph(_ source: MonarchAiSourcePart) -> some View {
        if let favicon = source.favicon, let url = URL(string: favicon) {
            MonarchFavicon(url: url, name: MonarchAiSourceFormat.host(source.url) ?? source.title)
        } else {
            MonarchIcon(systemName: MonarchAiSourceFormat.symbol(source.kind), size: .sm)
                .foregroundStyle(theme.colors[role: MonarchAiSourceListTokens.supportingColor])
        }
    }

    private func kindName(_ kind: MonarchAiSourceKind) -> String {
        labels.kind?(kind) ?? MonarchAiSourceListStrings.kind(kind)
    }
}

/// A row that opens its source: the row's content with a subtle background while pressed.
private struct MonarchAiSourceListRowStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        MonarchAiSourceListRowBody(configuration: configuration)
    }
}

/// Reads the theme for ``MonarchAiSourceListRowStyle``, which a style value cannot.
private struct MonarchAiSourceListRowBody: View {
    let configuration: ButtonStyleConfiguration

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchAiSourceListTokens.rowCornerRadius, style: .continuous)
        configuration.label
            .padding(.horizontal, MonarchAiSourceListTokens.rowHorizontalPadding)
            .background(
                shape.fill(
                    configuration.isPressed
                        ? theme.colors[role: MonarchAiSourceListTokens.pressedBackground] : Color.clear
                )
            )
            .contentShape(shape)
    }
}

#if DEBUG
    #Preview("MonarchAiSourceList") {
        MonarchPreviewMatrix {
            MonarchAiSourceList(
                [
                    MonarchAiSourceListItem(
                        MonarchAiSourcePart(
                            id: "s1",
                            title: "Quarterly planning notes",
                            url: "https://www.example.com/notes/q3",
                            kind: .doc,
                            product: "Docs"
                        ),
                        snippet: "Revenue grew 12% in the third quarter."
                    ),
                    MonarchAiSourceListItem(
                        MonarchAiSourcePart(id: "s2", title: "Acme Ltd", kind: .record, product: "CRM")
                    ),
                    MonarchAiSourceListItem(
                        MonarchAiSourcePart(id: "s3", title: "Old forecast.xlsx", kind: .file),
                        isUsed: false
                    ),
                    MonarchAiSourceListItem(
                        MonarchAiSourcePart(id: "s4", title: "Industry report", url: "https://example.org", kind: .web),
                        isUsed: false
                    ),
                ],
                onOpen: { _ in }
            )
        }
    }
#endif
