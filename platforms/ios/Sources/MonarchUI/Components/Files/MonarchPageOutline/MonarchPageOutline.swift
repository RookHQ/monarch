import SwiftUI

/// A document's outline: its headings as a nested list, with the section
/// being read marked as current. Picking a heading calls `onNavigate`, where
/// the app scrolls to it.
///
/// ```swift
/// ScrollViewReader { proxy in
///     ScrollView { document }
///         .sheet(isPresented: $showsOutline) {
///             MonarchPageOutline(headings, currentID: current) { heading in
///                 showsOutline = false
///                 withAnimation { proxy.scrollTo(heading.id, anchor: .top) }
///             }
///             .padding()
///             .presentationDetents([.medium, .large])
///         }
/// }
/// ```
///
/// On a phone, show it in a sheet; on a wide screen, beside the document.
/// The app knows which section is on screen (for example with
/// `onScrollGeometryChange` or `scrollPosition(id:)`) and passes its id as
/// `currentID`.
///
/// Accessibility: the outline is a container named by its title ("On this
/// page"), which has the header trait. Each heading is a button with a
/// 44-point touch target; the current one has the selected trait and the
/// value "Current section", and is drawn in a stronger weight with an accent
/// bar, so it is not shown by colour alone. The indent of nested headings
/// scales with Dynamic Type and follows the layout direction.
public struct MonarchPageOutline: View {
    private let headings: [MonarchPageOutlineHeading]
    private let currentID: String?
    private let maxLevel: Int
    private let stats: MonarchPageOutlineStats?
    private let isTitleHidden: Bool
    private let labels: MonarchPageOutlineLabels
    private let onNavigate: @MainActor (MonarchPageOutlineHeading) -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.locale) private var locale
    @ScaledMetric(relativeTo: .body) private var indent = MonarchPageOutlineTokens.indent

    /// A page outline.
    ///
    /// - Parameters:
    ///   - headings: The document's headings, in order.
    ///   - currentID: The id of the heading of the section being read.
    ///   - maxLevel: The deepest heading level listed.
    ///   - stats: The word count and reading time, shown under the list.
    ///   - isTitleHidden: Hides the visible title; the outline keeps it as its name.
    ///   - labels: Replacements for the built-in strings.
    ///   - onNavigate: Runs when a heading is picked. Scroll to it here.
    public init(
        _ headings: [MonarchPageOutlineHeading],
        currentID: String? = nil,
        maxLevel: Int = 3,
        stats: MonarchPageOutlineStats? = nil,
        isTitleHidden: Bool = false,
        labels: MonarchPageOutlineLabels = MonarchPageOutlineLabels(),
        onNavigate: @escaping @MainActor (MonarchPageOutlineHeading) -> Void
    ) {
        self.headings = headings
        self.currentID = currentID
        self.maxLevel = maxLevel
        self.stats = stats
        self.isTitleHidden = isTitleHidden
        self.labels = labels
        self.onNavigate = onNavigate
    }

    private var title: String { labels.title ?? MonarchPageOutlineStrings.title }

    /// The content of the view.
    public var body: some View {
        let visible = MonarchPageOutlineTokens.visible(headings, maxLevel: maxLevel)
        let depths = MonarchPageOutlineTokens.depths(visible)
        VStack(alignment: .leading, spacing: MonarchPageOutlineTokens.sectionSpacing) {
            if !isTitleHidden {
                Text(title)
                    .monarchTextStyle(.label)
                    .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                    .accessibilityAddTraits(.isHeader)
            }
            if visible.isEmpty {
                Text(labels.empty ?? MonarchPageOutlineStrings.empty)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            } else {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(zip(visible, depths)), id: \.0.id) { heading, depth in
                        row(heading, depth: depth)
                    }
                }
                .overlay(alignment: .leading) {
                    // One rail down the whole list, as on the web; the current row draws its own bar over it.
                    Rectangle()
                        .fill(theme.colors[role: MonarchPageOutlineTokens.barRole(isCurrent: false)])
                        .frame(width: MonarchPageOutlineTokens.railWidth)
                        .accessibilityHidden(true)
                }
            }
            if let stats {
                Text(labels.stats?(stats) ?? MonarchPageOutlineStrings.stats(stats, locale: locale))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(title)
    }

    private func row(_ heading: MonarchPageOutlineHeading, depth: Int) -> some View {
        let isCurrent = heading.id == currentID
        return Button {
            onNavigate(heading)
        } label: {
            HStack(spacing: MonarchPageOutlineTokens.barSpacing) {
                Rectangle()
                    .fill(isCurrent ? theme.colors[role: MonarchPageOutlineTokens.barRole(isCurrent: true)] : .clear)
                    .frame(width: MonarchPageOutlineTokens.barWidth)
                Text(heading.text)
                    .monarchTextStyle(
                        MonarchPageOutlineTokens.textStyle(isCurrent: isCurrent),
                        weight: isCurrent ? .medium : .regular
                    )
                    .foregroundStyle(
                        theme.colors[
                            role: MonarchPageOutlineTokens.textRole(isCurrent: isCurrent, isEnabled: isEnabled)
                        ]
                    )
                    .multilineTextAlignment(.leading)
                    .padding(.vertical, MonarchPageOutlineTokens.rowPadding)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .fixedSize(horizontal: false, vertical: true)
            .padding(.leading, MonarchPageOutlineTokens.railInset + indent * CGFloat(depth))
            .frame(minHeight: MonarchAccessibility.minimumHitTarget)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(heading.text)
        .accessibilityValue(
            MonarchPageOutlineTokens.accessibilityValue(
                isCurrent: isCurrent,
                currentLabel: labels.current ?? MonarchPageOutlineStrings.current
            )
        )
        .accessibilityAddTraits(isCurrent ? .isSelected : [])
    }
}

#if DEBUG
    #Preview("MonarchPageOutline") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchPageOutline(
                    [
                        MonarchPageOutlineHeading("Overview", level: 1, id: "overview"),
                        MonarchPageOutlineHeading("Goals", level: 2, id: "goals"),
                        MonarchPageOutlineHeading("Risks", level: 3, id: "risks"),
                        MonarchPageOutlineHeading("Timeline", level: 2, id: "timeline"),
                    ],
                    currentID: "goals",
                    stats: MonarchPageOutlineStats(words: 1_234, minutes: 6)
                ) { _ in }
                MonarchPageOutline([]) { _ in }
            }
        }
    }
#endif
