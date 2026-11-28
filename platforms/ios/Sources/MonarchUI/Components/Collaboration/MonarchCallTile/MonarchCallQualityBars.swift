import SwiftUI

/// Three signal bars of increasing height; the number lit is the quality.
struct MonarchCallQualityBars: View {
    let quality: MonarchCallQuality
    let label: String

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .caption) private var height = MonarchCallTileTokens.qualityHeight
    @ScaledMetric(relativeTo: .caption) private var barWidth = MonarchCallTileTokens.qualityBarWidth

    var body: some View {
        let lit = MonarchCallTileTokens.bars(quality)
        HStack(alignment: .bottom, spacing: barWidth) {
            ForEach(1...3, id: \.self) { index in
                Capsule()
                    .fill(
                        theme.colors[
                            role: index <= lit ? MonarchCallTileTokens.qualityOn : MonarchCallTileTokens.qualityOff
                        ]
                    )
                    .frame(width: barWidth, height: height * CGFloat(index) / 3)
            }
        }
        .frame(height: height, alignment: .bottom)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityAddTraits(.isImage)
    }
}
