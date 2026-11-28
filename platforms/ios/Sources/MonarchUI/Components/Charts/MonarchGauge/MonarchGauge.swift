import SwiftUI

/// A half-circle meter for one reading against a scale: utilisation, a health
/// score, a quota used.
///
/// ```swift
/// MonarchGauge(
///     "CPU",
///     value: 72,
///     bands: [
///         MonarchGaugeBand(0...70, tone: .success, label: "Healthy"),
///         MonarchGaugeBand(70...90, tone: .warning, label: "At risk"),
///         MonarchGaugeBand(90...100, tone: .danger, label: "Critical"),
///     ],
///     target: 80
/// )
/// ```
///
/// It is a SwiftUI `Gauge` with a Monarch style. Bands give the scale a
/// meaning, a target is marked with a tick, and the value is written as text,
/// so the reading never depends on the arc or on its colour. A value outside
/// the range is drawn at the nearest end; the text shows the real value. The
/// scale starts at the leading end, so the dial is mirrored in right-to-left
/// layouts.
///
/// Accessibility: the gauge is one element. Its label is the label you give
/// it, or "Gauge". Its value is the reading with the name of its band
/// ("72, At risk"), or with the range when there are no bands
/// ("72 (from 0 to 100)"), then the target. Without a value it reads "No data".
public struct MonarchGauge: View {
    private let label: String?
    private let value: Double?
    private let range: ClosedRange<Double>
    private let description: String?
    private let bands: [MonarchGaugeBand]
    private let target: Double?
    private let tone: MonarchTone?
    private let indicator: MonarchGaugeIndicator
    private let size: MonarchGaugeSize
    private let noValueLabel: String?
    private let valueLabel: String?
    private let showsRange: Bool
    private let formatValue: ((Double) -> String)?

    @Environment(\.locale) private var locale

    /// A gauge.
    ///
    /// - Parameters:
    ///   - label: The visible label under the gauge and its spoken name. Already localised.
    ///   - value: The reading. `nil` shows an empty track and a dash.
    ///   - range: The scale.
    ///   - description: A second line under the label, such as the period.
    ///   - bands: Stretches of the scale with a meaning, drawn along the arc.
    ///   - target: A target or threshold on the scale, marked with a tick.
    ///   - tone: The colour of the value arc. `nil` takes the tone of the band
    ///     the value is in, or the accent colour when there are no bands.
    ///   - indicator: A filled arc, or a needle.
    ///   - size: The largest width. The gauge shrinks with its container.
    ///   - noValueLabel: Replaces the built-in spoken text "No data".
    ///   - valueLabel: Replaces the value VoiceOver reads.
    ///   - showsRange: Whether the ends of the scale are written under the arc.
    ///   - formatValue: Writes every number. The default is a number in the current locale.
    public init(
        _ label: String? = nil,
        value: Double?,
        in range: ClosedRange<Double> = 0...100,
        description: String? = nil,
        bands: [MonarchGaugeBand] = [],
        target: Double? = nil,
        tone: MonarchTone? = nil,
        indicator: MonarchGaugeIndicator = .arc,
        size: MonarchGaugeSize = .md,
        noValueLabel: String? = nil,
        valueLabel: String? = nil,
        showsRange: Bool = true,
        formatValue: ((Double) -> String)? = nil
    ) {
        self.label = label
        self.value = value
        self.range = range
        self.description = description
        self.bands = bands
        self.target = target
        self.tone = tone
        self.indicator = indicator
        self.size = size
        self.noValueLabel = noValueLabel
        self.valueLabel = valueLabel
        self.showsRange = showsRange
        self.formatValue = formatValue
    }

    /// The content of the view.
    public var body: some View {
        let format = formatValue ?? { MonarchGaugeTokens.number($0, locale: locale) }
        let reading = MonarchGaugeTokens.reading(value: value, range: range, bands: bands, tone: tone)
        let text = MonarchGaugeTokens.text(
            reading: reading,
            target: target,
            noValueLabel: noValueLabel ?? MonarchGaugeStrings.noValue,
            format: format
        )
        Gauge(value: reading.clamped, in: reading.range) {
            if let label {
                Text(label)
            }
        } currentValueLabel: {
            Text(text.value)
        } minimumValueLabel: {
            Text(format(reading.range.lowerBound))
        } maximumValueLabel: {
            Text(format(reading.range.upperBound))
        }
        .gaugeStyle(
            MonarchGaugeStyle(
                reading: reading,
                bands: bands.map { MonarchGaugeTokens.span(of: $0, in: reading.range) },
                target: target.map { MonarchGaugeTokens.fraction(of: $0, in: reading.range) },
                indicator: indicator,
                size: size,
                description: description,
                showsRange: showsRange
            )
        )
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label ?? MonarchGaugeStrings.name)
        .accessibilityValue(valueLabel ?? text.accessibilityValue)
    }
}

#if DEBUG
    #Preview("MonarchGauge") {
        let bands = [
            MonarchGaugeBand(0...70, tone: .success, label: "Healthy"),
            MonarchGaugeBand(70...90, tone: .warning, label: "At risk"),
            MonarchGaugeBand(90...100, tone: .danger, label: "Critical"),
        ]
        return MonarchPreviewMatrix {
            VStack {
                MonarchGauge("CPU", value: 72, description: "Last hour", bands: bands, target: 80)
                MonarchGauge("Score", value: 40, bands: bands, indicator: .needle, size: .lg)
                MonarchGauge("Quota", value: 55, size: .sm)
                MonarchGauge("No reading", value: nil)
            }
        }
    }
#endif
