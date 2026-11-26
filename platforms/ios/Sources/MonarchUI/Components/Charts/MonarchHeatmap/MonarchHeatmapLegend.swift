import MonarchTokens
import SwiftUI

/// The "Less … More" legend of a heatmap's colour steps.
struct MonarchHeatmapLegend: View {
    let color: MonarchChartColor
    let showsNoData: Bool
    let steps: [String]
    let labels: MonarchHeatmapLabels

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .caption) private var swatch = MonarchHeatmapTokens.swatchSize

    var body: some View {
        // The top step is the data colour at full strength, as on the web.
        let base = theme.colors.tokens[keyPath: MonarchChartLegendTokens.role(color)]
        let muted = theme.colors.tokens.bgMuted
        HStack(spacing: MonarchHeatmapTokens.legendSpacing) {
            Text(labels.less ?? MonarchHeatmapStrings.less)
            HStack(spacing: MonarchHeatmapTokens.swatchSpacing) {
                if showsNoData {
                    MonarchShapes.mark
                        .strokeBorder(theme.colors.borderDefault, lineWidth: MonarchShapes.borderWidth)
                        .frame(width: swatch, height: swatch)
                }
                ForEach(0..<MonarchHeatmapTokens.levelCount, id: \.self) { level in
                    if let fill = MonarchHeatmapTokens.fill(level: level, base: base, muted: muted) {
                        MonarchShapes.mark
                            .fill(Color(monarch: fill))
                            .frame(width: swatch, height: swatch)
                    }
                }
            }
            Text(labels.more ?? MonarchHeatmapStrings.more)
        }
        .monarchTextStyle(.caption)
        .foregroundStyle(theme.colors.textMuted)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(labels.frame.legend ?? MonarchChartLegendStrings.legend)
        .accessibilityValue(steps.joined(separator: ", "))
    }
}
