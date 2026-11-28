import SwiftUI

/// The footer of a product's public or about screens: columns of links, then the legal line with its links.
///
/// ```swift
/// MonarchSiteFooter(
///     columns: [MonarchSiteFooterColumn("Company", links: [MonarchSiteLink("About Rook", destination: aboutURL)])],
///     legal: "© 2026 Rook",
///     legalLinks: [MonarchSiteLink("Privacy", destination: privacyURL)]
/// )
/// ```
///
/// Accessibility: column titles have the header trait. Each link is a link that opens its destination with the
/// environment's `openURL`, at least 44 points tall.
public struct MonarchSiteFooter: View {
    private let columns: [MonarchSiteFooterColumn]
    private let legal: String?
    private let legalLinks: [MonarchSiteLink]

    @Environment(\.monarchTheme) private var theme
    @Environment(\.openURL) private var openURL

    /// A site footer.
    ///
    /// - Parameters:
    ///   - columns: Columns of links, in order.
    ///   - legal: The legal line, such as "© 2026 Rook".
    ///   - legalLinks: Links on the legal line, such as Privacy and Terms.
    public init(columns: [MonarchSiteFooterColumn], legal: String? = nil, legalLinks: [MonarchSiteLink] = []) {
        self.columns = columns
        self.legal = legal
        self.legalLinks = legalLinks
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchSiteShellTokens.columnSpacing) {
            LazyVGrid(
                columns: [
                    GridItem(.adaptive(minimum: MonarchSiteShellTokens.columnMinimumWidth), alignment: .topLeading)
                ],
                alignment: .leading,
                spacing: MonarchSiteShellTokens.columnSpacing
            ) {
                ForEach(columns) { column in
                    VStack(alignment: .leading, spacing: MonarchSiteShellTokens.linkSpacing) {
                        Text(column.title)
                            .monarchTextStyle(.label, weight: .semibold)
                            .foregroundStyle(theme.colors[role: MonarchSiteShellTokens.titleColor])
                            .accessibilityAddTraits(.isHeader)
                        ForEach(column.links) { link in
                            linkButton(link, style: .bodySmall)
                        }
                    }
                }
            }
            if legal != nil || !legalLinks.isEmpty {
                MonarchDivider()
                MonarchFlowLayout(spacing: MonarchSiteShellTokens.legalSpacing) {
                    if let legal {
                        Text(legal)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors[role: MonarchSiteShellTokens.mutedColor])
                            .monarchMinimumHitTarget()
                    }
                    ForEach(legalLinks) { link in linkButton(link, style: .caption) }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func linkButton(_ link: MonarchSiteLink, style: MonarchTextStyle) -> some View {
        Button {
            if let destination = link.destination { openURL(destination) }
        } label: {
            Text(link.label)
                .monarchTextStyle(style)
                .foregroundStyle(theme.colors[role: MonarchSiteShellTokens.mutedColor])
                .frame(minHeight: MonarchAccessibility.minimumHitTarget)
        }
        .buttonStyle(.plain)
        .accessibilityRemoveTraits(.isButton)
        .accessibilityAddTraits(.isLink)
    }
}
