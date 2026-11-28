import SwiftUI

/// The bar of ``MonarchAiContextWindowMeter``: coloured segments for a
/// breakdown, or one fill in the colour of the state. Segments are laid out
/// in an `HStack`, so they start at the leading edge in both directions.
struct MonarchAiContextWindowMeterBar: View {
    let segments: [MonarchAiContextWindowMeterTokens.Segment]
    let used: Int
    let limit: Int
    let state: MonarchAiContextWindowState

    @Environment(\.monarchTheme) private var theme

    private typealias Tokens = MonarchAiContextWindowMeterTokens

    var body: some View {
        GeometryReader { proxy in
            let width = proxy.size.width
            HStack(spacing: 0) {
                if segments.isEmpty {
                    Rectangle()
                        .fill(theme.colors.soft(Tokens.fill(state), by: MonarchBarChartTokens.softening))
                        .frame(width: width * Tokens.fraction(used: used, limit: limit))
                } else {
                    let fractions = Tokens.fractions(segments, limit: limit)
                    ForEach(Array(segments.enumerated()), id: \.element.id) { index, segment in
                        Rectangle()
                            .fill(
                                theme.colors.soft(
                                    MonarchChartLegendTokens.role(segment.color),
                                    by: MonarchBarChartTokens.softening
                                )
                            )
                            .frame(width: width * fractions[index])
                    }
                }
                Spacer(minLength: 0)
            }
            .monarchAnimation(.base, value: used)
        }
        .frame(height: Tokens.barHeight)
        .background(theme.colors[role: Tokens.track])
        .clipShape(Capsule())
    }
}
