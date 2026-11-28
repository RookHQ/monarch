import SwiftUI

/// One row of a bar list: rank, label on its bar, and value.
struct MonarchBarListRow: View {
    let row: MonarchBarListTokens.Row
    let valueText: String?
    let accessibilityValue: String
    let isRanked: Bool
    let onSelect: (@MainActor (MonarchBarListItem) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    var body: some View {
        if let onSelect {
            Button {
                onSelect(row.item)
            } label: {
                content.monarchMinimumHitTarget()
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(MonarchBarListTokens.accessibilityLabel(for: row.item))
            .accessibilityValue(accessibilityValue)
            .accessibilityAddTraits(.isButton)
        } else {
            content
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(MonarchBarListTokens.accessibilityLabel(for: row.item))
                .accessibilityValue(accessibilityValue)
        }
    }

    private var content: some View {
        // As on the web: rank, label and value on one line, the bar as a thin track under the label.
        VStack(alignment: .leading, spacing: MonarchBarListTokens.trackSpacing) {
            HStack(alignment: .center, spacing: MonarchBarListTokens.columnSpacing) {
                if isRanked {
                    Text(row.rank.formatted(.number.locale(locale)))
                        .monarchTextStyle(.caption)
                        .monospacedDigit()
                        .foregroundStyle(theme.colors.textMuted)
                        .frame(width: MonarchBarListTokens.rankWidth, alignment: .leading)
                }
                label
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(valueText ?? MonarchBarListTokens.missingSymbol)
                    .monarchTextStyle(
                        .label,
                        weight: MonarchBarListTokens.labelWeight(isHighlighted: row.item.isHighlighted)
                    )
                    .monospacedDigit()
                    .foregroundStyle(theme.colors[role: valueText == nil ? \.textMuted : \.textDefault])
            }
            bar(fill: MonarchBarListTokens.bar(isHighlighted: row.item.isHighlighted))
                .frame(height: MonarchBarListTokens.trackHeight)
                .padding(.leading, isRanked ? MonarchBarListTokens.rankWidth + MonarchBarListTokens.columnSpacing : 0)
        }
        .frame(minHeight: MonarchBarListTokens.minRowHeight)
    }

    private var label: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .firstTextBaseline, spacing: MonarchBarListTokens.iconSpacing) {
                if let systemImage = row.item.systemImage {
                    MonarchIcon(systemName: systemImage, size: .sm)
                }
                Text(row.item.label)
                    .monarchTextStyle(
                        .label,
                        weight: MonarchBarListTokens.labelWeight(isHighlighted: row.item.isHighlighted)
                    )
            }
            .foregroundStyle(theme.colors.textDefault)
            if let description = row.item.description {
                Text(description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .multilineTextAlignment(.leading)
    }

    /// The bar grows from the leading edge, so it mirrors in right-to-left layouts.
    private func bar(fill: MonarchColorRole) -> some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule().fill(theme.colors.bgMuted)
                Capsule()
                    .fill(theme.colors[role: fill])
                    .frame(width: proxy.size.width * row.fraction)
                    .monarchAnimation(.base, value: row.fraction)
            }
        }
    }
}
