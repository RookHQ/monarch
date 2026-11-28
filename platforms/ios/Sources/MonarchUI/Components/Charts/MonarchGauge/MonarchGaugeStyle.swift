import SwiftUI

/// The Monarch look of a `Gauge`: a half circle with bands, a target tick,
/// the value as text, the range and the label. Internal, because the bands
/// and the target are not part of a gauge's configuration; use ``MonarchGauge``.
struct MonarchGaugeStyle: GaugeStyle {
    let reading: MonarchGaugeTokens.Reading
    let bands: [MonarchGaugeTokens.Span]
    let target: Double?
    let indicator: MonarchGaugeIndicator
    let size: MonarchGaugeSize
    let description: String?
    let showsRange: Bool

    func makeBody(configuration: Configuration) -> some View {
        MonarchGaugeBody(configuration: configuration, style: self)
    }
}

/// Reads the environment, which a `GaugeStyle` value cannot do itself.
private struct MonarchGaugeBody: View {
    let configuration: GaugeStyleConfiguration
    let style: MonarchGaugeStyle

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// The value sits inside the arc, unless the text is too large for it or
    /// a needle is in the way.
    private var isValueInside: Bool {
        style.indicator == .arc && !dynamicTypeSize.isAccessibilitySize
    }

    var body: some View {
        let width = MonarchGaugeTokens.width(style.size)
        VStack(spacing: MonarchGaugeTokens.textSpacing) {
            ZStack(alignment: .bottom) {
                MonarchGaugeDial(style: style, fraction: configuration.value)
                if isValueInside {
                    valueText
                }
            }
            .aspectRatio(MonarchGaugeTokens.aspectRatio, contentMode: .fit)
            .frame(maxWidth: width)
            if style.showsRange {
                // SwiftUI mirrors the dial in right-to-left layouts, and this row with it.
                HStack {
                    configuration.minimumValueLabel
                    Spacer(minLength: 0)
                    configuration.maximumValueLabel
                }
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
                .frame(maxWidth: width)
            }
            if !isValueInside {
                valueText
            }
            if let band = style.reading.bandLabel {
                Text(band)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: style.reading.text])
            }
            configuration.label
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textDefault)
            if let description = style.description {
                Text(description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .multilineTextAlignment(.center)
    }

    private var valueText: some View {
        configuration.currentValueLabel
            .monarchTextStyle(MonarchGaugeTokens.valueTextStyle(style.size))
            .monospacedDigit()
            .foregroundStyle(theme.colors[role: style.reading.text])
    }
}

/// The drawn part of the gauge: track, bands, value arc, target and needle.
private struct MonarchGaugeDial: View {
    let style: MonarchGaugeStyle
    let fraction: Double

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        GeometryReader { proxy in
            let metrics = MonarchGaugeTokens.metrics(
                width: proxy.size.width,
                hasOuterBands: style.indicator == .arc && !style.bands.isEmpty
            )
            ZStack {
                MonarchGaugeArc(from: 0, to: 1, inset: metrics.trackInset)
                    .stroke(theme.colors[role: MonarchGaugeTokens.track], lineWidth: metrics.thickness)
                ForEach(Array(style.bands.enumerated()), id: \.offset) { _, band in
                    MonarchGaugeArc(from: band.from, to: band.to, inset: metrics.bandInset(style.indicator))
                        .stroke(
                            theme.colors[role: MonarchGaugeTokens.bandColor(band.tone)],
                            lineWidth: metrics.bandThickness(style.indicator)
                        )
                }
                if style.indicator == .arc, style.reading.hasValue {
                    MonarchGaugeArc(from: 0, to: fraction, inset: metrics.trackInset)
                        .stroke(
                            theme.colors.soft(style.reading.fill, by: MonarchBarChartTokens.softening),
                            lineWidth: metrics.thickness
                        )
                        .monarchAnimation(.base, value: fraction)
                }
                if let target = style.target {
                    MonarchGaugeTick(
                        fraction: target,
                        innerInset: metrics.trackInset + metrics.thickness / 2 + MonarchGaugeTokens.tickOverhang,
                        outerInset: metrics.trackInset - metrics.thickness / 2 - MonarchGaugeTokens.tickOverhang
                    )
                    .stroke(theme.colors[role: MonarchGaugeTokens.mark], lineWidth: MonarchGaugeTokens.markWidth)
                }
                if style.indicator == .needle, style.reading.hasValue {
                    MonarchGaugeNeedle(
                        fraction: fraction,
                        inset: metrics.trackInset + metrics.thickness / 2 + MonarchGaugeTokens.needleGap
                    )
                    .stroke(
                        theme.colors[role: MonarchGaugeTokens.mark],
                        style: StrokeStyle(lineWidth: MonarchGaugeTokens.markWidth, lineCap: .round)
                    )
                    .monarchAnimation(.base, value: fraction)
                }
            }
        }
    }
}
