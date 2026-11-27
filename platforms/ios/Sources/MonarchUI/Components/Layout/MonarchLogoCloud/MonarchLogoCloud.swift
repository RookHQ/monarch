import SwiftUI

/// The organisations a product can name (customers, partners, integrations) as a titled row or grid of logos, with
/// the name as a wordmark when there is no image.
///
/// ```swift
/// MonarchLogoCloud(
///     title: "Placeholder organisations",
///     logos: ["Company A", "Company B", "Company C"].map { MonarchLogoCloudLogo($0) }
/// )
/// ```
///
/// Only show organisations you have permission to name. Logos are drawn in one muted colour by default so the
/// strip supports the screen rather than competing with it; images are drawn as templates in that colour.
///
/// Accessibility: the cloud is a container named by its title (or "Organisations"). Each logo is one element read
/// as the organisation's name; a logo with a destination is a link. Linked logos are at least 44 points tall.
public struct MonarchLogoCloud: View {
    private let title: String?
    private let logos: [MonarchLogoCloudLogo]
    private let layout: MonarchLogoCloudLayout
    private let size: MonarchLogoCloudSize
    private let isMonochrome: Bool
    private let listLabel: String?

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var scale: CGFloat = 1

    /// A logo cloud.
    ///
    /// - Parameters:
    ///   - title: A short line above the logos, shown in small capitals.
    ///   - logos: The organisations, in display order.
    ///   - layout: A centred wrapping row, or a grid of bordered cells.
    ///   - size: The height of the logos.
    ///   - isMonochrome: Whether images are drawn in the muted text colour. On by default.
    ///   - listLabel: Replaces the built-in spoken name "Organisations" used when there is no title.
    public init(
        title: String? = nil,
        logos: [MonarchLogoCloudLogo],
        layout: MonarchLogoCloudLayout = .row,
        size: MonarchLogoCloudSize = .md,
        isMonochrome: Bool = true,
        listLabel: String? = nil
    ) {
        self.title = title
        self.logos = logos
        self.layout = layout
        self.size = size
        self.isMonochrome = isMonochrome
        self.listLabel = listLabel
    }

    /// The content of the view.
    public var body: some View {
        VStack(spacing: MonarchLogoCloudTokens.titleSpacing) {
            if let title {
                Text(title)
                    .textCase(.uppercase)
                    .monarchTextStyle(.caption, weight: MonarchLogoCloudTokens.titleWeight)
                    .foregroundStyle(theme.colors[role: MonarchLogoCloudTokens.titleColor])
                    .multilineTextAlignment(.center)
                    .accessibilityAddTraits(.isHeader)
            }
            switch layout {
            case .row:
                MonarchFlowLayout(
                    spacing: MonarchLogoCloudTokens.rowSpacing,
                    rowSpacing: MonarchLogoCloudTokens.rowLineSpacing,
                    justification: .center
                ) {
                    ForEach(logos) { logo in
                        item(logo)
                    }
                }
            case .grid:
                LazyVGrid(
                    columns: [
                        GridItem(
                            .adaptive(minimum: MonarchLogoCloudTokens.cellMinimumWidth),
                            spacing: MonarchLogoCloudTokens.gridSpacing
                        )
                    ],
                    spacing: MonarchLogoCloudTokens.gridSpacing
                ) {
                    ForEach(logos) { logo in
                        item(logo)
                            .padding(MonarchLogoCloudTokens.cellPadding)
                            .frame(maxWidth: .infinity)
                            .overlay(
                                MonarchShapes.container.strokeBorder(
                                    theme.colors[role: MonarchLogoCloudTokens.cellBorder],
                                    lineWidth: MonarchShapes.borderWidth
                                )
                            )
                    }
                }
            }
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(MonarchLogoCloudTokens.listLabel(title: title, override: listLabel))
    }

    @ViewBuilder private func item(_ logo: MonarchLogoCloudLogo) -> some View {
        if let destination = logo.destination {
            Link(destination: destination) {
                mark(logo).monarchMinimumHitTarget()
            }
            .buttonStyle(.plain)
            .accessibilityLabel(logo.name)
        } else {
            mark(logo)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(logo.name)
        }
    }

    @ViewBuilder private func mark(_ logo: MonarchLogoCloudLogo) -> some View {
        if let image = logo.image {
            if isMonochrome {
                image
                    .resizable()
                    .renderingMode(.template)
                    .scaledToFit()
                    .frame(height: MonarchLogoCloudTokens.logoHeight(size) * scale)
                    .foregroundStyle(theme.colors[role: MonarchLogoCloudTokens.wordmarkColor])
            } else {
                image
                    .resizable()
                    .scaledToFit()
                    .frame(height: MonarchLogoCloudTokens.logoHeight(size) * scale)
            }
        } else {
            Text(logo.name)
                .monarchTextStyle(
                    MonarchLogoCloudTokens.wordmarkStyle(size),
                    weight: MonarchLogoCloudTokens.wordmarkWeight
                )
                .foregroundStyle(theme.colors[role: MonarchLogoCloudTokens.wordmarkColor])
                .multilineTextAlignment(.center)
        }
    }
}

#if DEBUG
    #Preview("MonarchLogoCloud") {
        MonarchPreviewMatrix {
            VStack {
                MonarchLogoCloud(
                    title: "Placeholder organisations",
                    logos: ["Company A", "Company B", "Company C", "Company D", "Company E", "Company F"].map {
                        MonarchLogoCloudLogo($0)
                    }
                )
                MonarchLogoCloud(
                    title: "Integrations",
                    logos: ["Company A", "Company B", "Company C", "Company D"].map { MonarchLogoCloudLogo($0) },
                    layout: .grid,
                    size: .sm
                )
            }
        }
    }
#endif
