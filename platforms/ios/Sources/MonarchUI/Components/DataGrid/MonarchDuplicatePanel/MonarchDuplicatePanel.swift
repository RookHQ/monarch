import SwiftUI

/// Likely duplicates of a record, why each one matched, and the actions on
/// them.
///
/// ```swift
/// MonarchDuplicatePanel(
///     matches,
///     footer: "You can still create it.",
///     onOpen: { open($0) },
///     onMerge: { merge($0) },
///     onDismiss: { notADuplicate($0) }
/// )
/// ```
///
/// Each match is a card with its name, its kind and confidence as badges,
/// the reasons and one button for each action that was given. Nothing is
/// drawn when there are no matches.
///
/// Accessibility: the heading has the header trait. The text of a match is
/// one element. Each button is read as its title and the name of the record,
/// so the buttons of different matches differ. The hint variant posts an
/// announcement when it appears and when the matches change.
public struct MonarchDuplicatePanel: View {
    private let matches: [MonarchDuplicateMatch]
    private let footer: String?
    private let variant: MonarchDuplicatePanelVariant
    private let title: String?
    private let openLabel: String?
    private let mergeLabel: String?
    private let dismissLabel: String?
    private let onOpen: (@MainActor (MonarchDuplicateMatch) -> Void)?
    private let onMerge: (@MainActor (MonarchDuplicateMatch) -> Void)?
    private let onDismiss: (@MainActor (MonarchDuplicateMatch) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// A duplicate panel.
    ///
    /// - Parameters:
    ///   - matches: The likely duplicates. Nothing is drawn when it is empty.
    ///   - footer: A closing line, for example "You can still create it."
    ///   - variant: A warning box under a form, or a plain section.
    ///   - title: Replaces the built-in heading, "Possible duplicate" or "Possible duplicates".
    ///   - openLabel: Replaces the built-in "Open" title.
    ///   - mergeLabel: Replaces the built-in "Merge" title.
    ///   - dismissLabel: Replaces the built-in "Not a duplicate" title.
    ///   - onOpen: Opens a match. No button without it.
    ///   - onMerge: Merges a match. No button without it.
    ///   - onDismiss: Says a match is not a duplicate. No button without it.
    public init(
        _ matches: [MonarchDuplicateMatch],
        footer: String? = nil,
        variant: MonarchDuplicatePanelVariant = .hint,
        title: String? = nil,
        openLabel: String? = nil,
        mergeLabel: String? = nil,
        dismissLabel: String? = nil,
        onOpen: (@MainActor (MonarchDuplicateMatch) -> Void)? = nil,
        onMerge: (@MainActor (MonarchDuplicateMatch) -> Void)? = nil,
        onDismiss: (@MainActor (MonarchDuplicateMatch) -> Void)? = nil
    ) {
        self.matches = matches
        self.footer = footer
        self.variant = variant
        self.title = title
        self.openLabel = openLabel
        self.mergeLabel = mergeLabel
        self.dismissLabel = dismissLabel
        self.onOpen = onOpen
        self.onMerge = onMerge
        self.onDismiss = onDismiss
    }

    private var heading: String {
        MonarchDuplicatePanelTokens.heading(count: matches.count, override: title)
    }

    /// The content of the view.
    public var body: some View {
        if !matches.isEmpty {
            let appearance = MonarchDuplicatePanelTokens.appearance(variant: variant)
            VStack(alignment: .leading, spacing: MonarchDuplicatePanelTokens.sectionSpacing) {
                header(appearance)
                ForEach(Array(matches.enumerated()), id: \.element.id) { index, match in
                    if index > 0 { MonarchDivider() }
                    row(match)
                }
                if let footer {
                    Text(footer)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .padding(appearance.background == nil ? 0 : MonarchDuplicatePanelTokens.padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background {
                if let background = appearance.background {
                    MonarchShapes.container.fill(theme.colors[role: background])
                }
            }
            .overlay {
                if let border = appearance.border {
                    MonarchShapes.container
                        .strokeBorder(theme.colors[role: border], lineWidth: MonarchDuplicatePanelTokens.borderWidth)
                        .allowsHitTesting(false)
                }
            }
            .accessibilityElement(children: .contain)
            .onAppear(perform: announce)
            .onChange(of: matches.map(\.id)) { announce() }
        }
    }

    private func header(_ appearance: MonarchDuplicatePanelTokens.Appearance) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchDuplicatePanelTokens.iconSpacing) {
            if let icon = appearance.icon {
                MonarchIcon(systemName: MonarchDuplicatePanelTokens.symbol, size: .sm)
                    .foregroundStyle(theme.colors[role: icon])
            }
            Text(heading)
                .monarchTextStyle(.label, weight: .semibold)
                .foregroundStyle(
                    appearance.icon.map { theme.colors[role: $0] } ?? theme.colors.textDefault
                )
        }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }

    private func row(_ match: MonarchDuplicateMatch) -> some View {
        // A plain row, as on the web: the name over "Client · Same email", the actions as links at the right.
        let score = MonarchDuplicatePanelTokens.scoreText(match.score, locale: locale)
        let summary = [match.kind, MonarchDuplicatePanelTokens.reasonsText(match.reasons, locale: locale), score]
            .compactMap { $0 }
            .joined(separator: " \u{00B7} ")
        return HStack(alignment: .center, spacing: MonarchDuplicatePanelTokens.itemSpacing) {
            VStack(alignment: .leading, spacing: MonarchDuplicatePanelTokens.lineSpacing) {
                Text(match.title)
                    .monarchTextStyle(.label, weight: .medium)
                    .foregroundStyle(theme.colors.textDefault)
                if !summary.isEmpty {
                    Text(summary)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                if let detail = match.detail {
                    Text(detail)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .combine)
            linkActions(match)
        }
    }

    @ViewBuilder
    private func linkActions(_ match: MonarchDuplicateMatch) -> some View {
        if let onOpen {
            linkAction(openLabel ?? MonarchDuplicatePanelStrings.open, theme.colors.textLink, match, onOpen)
        }
        if let onMerge {
            linkAction(mergeLabel ?? MonarchDuplicatePanelStrings.merge, theme.colors.textLink, match, onMerge)
        }
        if let onDismiss {
            linkAction(dismissLabel ?? MonarchDuplicatePanelStrings.dismiss, theme.colors.textMuted, match, onDismiss)
        }
    }

    private func linkAction(
        _ title: String,
        _ color: Color,
        _ match: MonarchDuplicateMatch,
        _ action: @escaping @MainActor (MonarchDuplicateMatch) -> Void
    ) -> some View {
        Button {
            action(match)
        } label: {
            Text(title)
                .monarchTextStyle(.label)
                .underline()
                .foregroundStyle(color)
                .monarchMinimumHitTarget()
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(MonarchDuplicatePanelTokens.actionLabel(action: title, record: match.title))
    }

    @ViewBuilder
    private func badges(_ match: MonarchDuplicateMatch) -> some View {
        let score = MonarchDuplicatePanelTokens.scoreText(match.score, locale: locale)
        if match.kind != nil || score != nil {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: MonarchDuplicatePanelTokens.itemSpacing) {
                    badgeViews(kind: match.kind, score: score)
                }
                VStack(alignment: .leading, spacing: MonarchDuplicatePanelTokens.itemSpacing) {
                    badgeViews(kind: match.kind, score: score)
                }
            }
        }
    }

    @ViewBuilder
    private func badgeViews(kind: String?, score: String?) -> some View {
        if let kind {
            MonarchBadge(kind)
        }
        if let score {
            MonarchBadge(score, tone: .warning, variant: .outline)
        }
    }

    @ViewBuilder
    private func actions(_ match: MonarchDuplicateMatch) -> some View {
        if onOpen != nil || onMerge != nil || onDismiss != nil {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: MonarchDuplicatePanelTokens.itemSpacing) {
                    actionButtons(match)
                }
                VStack(alignment: .leading, spacing: MonarchDuplicatePanelTokens.itemSpacing) {
                    actionButtons(match)
                }
            }
        }
    }

    @ViewBuilder
    private func actionButtons(_ match: MonarchDuplicateMatch) -> some View {
        if let onOpen {
            actionButton(openLabel ?? MonarchDuplicatePanelStrings.open, .secondary, match, onOpen)
        }
        if let onMerge {
            actionButton(mergeLabel ?? MonarchDuplicatePanelStrings.merge, .secondary, match, onMerge)
        }
        if let onDismiss {
            actionButton(dismissLabel ?? MonarchDuplicatePanelStrings.dismiss, .ghost, match, onDismiss)
        }
    }

    private func actionButton(
        _ title: String,
        _ variant: MonarchButtonVariant,
        _ match: MonarchDuplicateMatch,
        _ action: @escaping @MainActor (MonarchDuplicateMatch) -> Void
    ) -> some View {
        Button(title) {
            action(match)
        }
        .buttonStyle(.monarch(variant, size: .sm))
        .accessibilityLabel(MonarchDuplicatePanelTokens.actionLabel(action: title, record: match.title))
    }

    private func announce() {
        guard variant == .hint, !matches.isEmpty else { return }
        let text = MonarchDuplicatePanelTokens.announcement(heading: heading, matches: matches, locale: locale)
        AccessibilityNotification.Announcement(text).post()
    }
}

#if DEBUG
    #Preview("MonarchDuplicatePanel") {
        let matches = [
            MonarchDuplicateMatch(
                id: "1",
                title: "Example Ltd",
                kind: "Client",
                reasons: ["Same email", "Same company domain"],
                detail: "Created 3 Mar",
                score: 0.92
            ),
            MonarchDuplicateMatch(id: "2", title: "Example Limited", reasons: ["Same name"]),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchDuplicatePanel(
                    matches,
                    footer: "You can still create it.",
                    onOpen: { _ in },
                    onMerge: { _ in },
                    onDismiss: { _ in }
                )
                MonarchDuplicatePanel(matches, variant: .panel, onOpen: { _ in })
            }
        }
    }
#endif
