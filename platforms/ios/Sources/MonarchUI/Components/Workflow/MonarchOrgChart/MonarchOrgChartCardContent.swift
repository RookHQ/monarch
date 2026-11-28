import SwiftUI

/// The inside of a card or a list row: the avatar (or the vacancy tile), the
/// name, the title and the meta line. Read by VoiceOver through the node's
/// combined label, so nothing here is read separately.
struct MonarchOrgChartCardContent: View {
    let node: MonarchOrgChartNode
    let appearance: MonarchOrgChartTokens.CardAppearance

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var tile = MonarchOrgChartTokens.vacancyTile

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchOrgChartTokens.cardSpacing) {
            HStack(alignment: .top, spacing: MonarchOrgChartTokens.cardSpacing) {
                picture
                VStack(alignment: .leading, spacing: MonarchOrgChartTokens.textSpacing) {
                    Text(node.name)
                        .monarchTextStyle(.bodyStrong)
                        .foregroundStyle(theme.colors[role: appearance.name])
                    if let title = MonarchOrgChartTokens.nonBlank(node.title) {
                        Text(title)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors[role: appearance.secondary])
                    }
                }
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)
            }
            if let meta = MonarchOrgChartTokens.nonBlank(node.meta) {
                if node.kind == .person {
                    MonarchBadge(meta, tone: node.metaTone ?? .neutral, size: .sm)
                } else {
                    Text(meta)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: appearance.secondary])
                }
            }
        }
    }

    @ViewBuilder private var picture: some View {
        switch node.kind {
        case .person:
            MonarchAvatar(url: node.avatarURL, name: node.name, seed: node.name, size: .md, isDecorative: true)
        case .vacancy:
            let shape = RoundedRectangle(cornerRadius: MonarchOrgChartTokens.vacancyRadius, style: .continuous)
            MonarchIcon(systemName: MonarchOrgChartTokens.vacancySymbol, size: .sm)
                .foregroundStyle(theme.colors[role: MonarchOrgChartTokens.vacancyTileForeground])
                .frame(width: tile, height: tile)
                .background(theme.colors[role: MonarchOrgChartTokens.vacancyTileFill], in: shape)
                .overlay(
                    shape.strokeBorder(
                        theme.colors[role: appearance.border],
                        style: StrokeStyle(
                            lineWidth: MonarchOrgChartTokens.lineWidth,
                            dash: MonarchOrgChartTokens.borderDash
                        )
                    )
                )
                .accessibilityHidden(true)
        }
    }
}
