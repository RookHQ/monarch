import Foundation
import MonarchTokens

/// Component tokens, geometry and text rules for ``MonarchGauge``.
enum MonarchGaugeTokens {
    /// The gap between the dial and the lines of text under it.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1
    /// The width of the dial as a multiple of its height: a half circle.
    static let aspectRatio: CGFloat = 2
    /// The thickness of the track as a share of the dial's width.
    static let thicknessRatio: CGFloat = 0.07
    /// The thickness of a band drawn outside the track.
    static let outerBandThickness: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between an outer band and the track.
    static let bandGap: CGFloat = MonarchTokens.Space.space1
    /// How far the target tick reaches beyond the track on both sides.
    static let tickOverhang: CGFloat = MonarchTokens.Space.space0_5
    /// The width of the target tick and of the needle.
    static let markWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the tip of the needle and the track.
    static let needleGap: CGFloat = MonarchTokens.Space.space1_5
    /// The radius of the needle's hub, which is also the room kept under the dial's centre.
    static let hubRadius: CGFloat = MonarchTokens.Space.space1
    /// The number of line segments an arc is drawn with.
    static let arcSteps = 64
    /// Shown in place of the value when there is none.
    static let emptySymbol = "–"
    /// The track role.
    static var track: MonarchColorRole { \.bgMuted }
    /// The role of the target tick and the needle.
    static var mark: MonarchColorRole { \.textDefault }

    /// The largest width of the dial.
    static func width(_ size: MonarchGaugeSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space24 + MonarchTokens.Space.space6
        case .md: MonarchTokens.Space.space24 + MonarchTokens.Space.space16
        case .lg: MonarchTokens.Space.space24 * 2 + MonarchTokens.Space.space2
        }
    }

    /// The type role of the value.
    static func valueTextStyle(_ size: MonarchGaugeSize) -> MonarchTextStyle {
        switch size {
        case .sm: .subheading
        case .md: .heading
        case .lg: .title
        }
    }

    // MARK: Colour

    /// The colour of a band. Neutral has no status colour.
    static func bandColor(_ tone: MonarchTone) -> MonarchColorRole {
        tone == .neutral ? \.borderStrong : MonarchTonePalette.palette(for: tone).solid
    }

    /// The colour of the value arc for a tone. `nil` is the accent colour.
    static func fill(tone: MonarchTone?) -> MonarchColorRole {
        guard let tone else { return \.accentSolid }
        return tone == .neutral ? \.textMuted : MonarchTonePalette.palette(for: tone).solid
    }

    // MARK: Geometry

    /// The sizes of the drawn parts for a dial width.
    struct Metrics: Equatable {
        /// The thickness of the track.
        let thickness: CGFloat
        /// From the outer edge of the dial to the centre line of the track.
        let trackInset: CGFloat

        /// From the outer edge of the dial to the centre line of a band.
        func bandInset(_ indicator: MonarchGaugeIndicator) -> CGFloat {
            indicator == .arc ? MonarchGaugeTokens.outerBandThickness / 2 : trackInset
        }

        /// The thickness of a band.
        func bandThickness(_ indicator: MonarchGaugeIndicator) -> CGFloat {
            indicator == .arc ? MonarchGaugeTokens.outerBandThickness : thickness
        }
    }

    /// The metrics for a dial width. Outer bands take room around the track.
    static func metrics(width: CGFloat, hasOuterBands: Bool) -> Metrics {
        let thickness = width * thicknessRatio
        let outer = hasOuterBands ? outerBandThickness + bandGap : 0
        return Metrics(thickness: thickness, trackInset: outer + thickness / 2)
    }

    /// The centre of the half circle: the middle of the bottom edge, raised by the hub.
    static func centre(of rect: CGRect) -> CGPoint {
        CGPoint(x: rect.midX, y: rect.maxY - hubRadius)
    }

    /// The radius of the outer edge of the dial.
    static func outerRadius(of rect: CGRect) -> CGFloat {
        max(min(rect.width / 2, rect.height - hubRadius), 0)
    }

    /// The point at a fraction of the scale: 0 is the leading end, 0.5 the
    /// top, 1 the trailing end. The coordinates are those of a left-to-right
    /// layout; SwiftUI mirrors the drawn shape in right-to-left layouts.
    static func point(at fraction: Double, inset: CGFloat, in rect: CGRect) -> CGPoint {
        let centre = centre(of: rect)
        let radius = max(outerRadius(of: rect) - inset, 0)
        let angle = Double.pi * (1 - min(max(fraction, 0), 1))
        return CGPoint(x: centre.x + radius * cos(angle), y: centre.y - radius * sin(angle))
    }

    // MARK: Reading

    /// What the gauge shows for a value.
    struct Reading: Equatable {
        /// The scale. Its upper bound is above its lower bound.
        let range: ClosedRange<Double>
        /// The value, or `nil` when there is none.
        let value: Double?
        /// The value limited to the scale, for drawing. The lower bound without a value.
        let clamped: Double
        /// The name of the band the value is in.
        let bandLabel: String?
        /// The colour of the value arc.
        let fill: MonarchColorRole
        /// The colour of the value and the band name: the tone's strong role, default text for neutral.
        let text: MonarchColorRole

        /// Whether there is a value.
        var hasValue: Bool { value != nil }
    }

    /// A band as fractions of the scale.
    struct Span: Equatable {
        let from: Double
        let to: Double
        let tone: MonarchTone
    }

    /// The band a value is in. A value at the upper bound of a band belongs
    /// to the next one, except at the end of the scale.
    static func band(at value: Double, in bands: [MonarchGaugeBand], upperBound: Double) -> MonarchGaugeBand? {
        bands.first { band in
            value >= band.range.lowerBound
                && (value < band.range.upperBound
                    || (band.range.upperBound >= upperBound && value <= band.range.upperBound))
        }
    }

    /// Resolves a value against the scale and the bands. Pure, so it is
    /// tested without a view.
    ///
    /// An empty scale is widened by one, so nothing divides by zero. A value
    /// that is not finite counts as no value. The fill is the given tone,
    /// else the tone of the value's band, else the accent colour without
    /// bands and the neutral colour outside every band or without a value.
    static func reading(
        value: Double?,
        range: ClosedRange<Double>,
        bands: [MonarchGaugeBand],
        tone: MonarchTone?
    ) -> Reading {
        let scale = range.upperBound > range.lowerBound ? range : range.lowerBound...(range.lowerBound + 1)
        guard let value, value.isFinite else {
            return Reading(
                range: scale,
                value: nil,
                clamped: scale.lowerBound,
                bandLabel: nil,
                fill: fill(tone: tone ?? .neutral),
                text: \.textDefault
            )
        }
        let band = band(at: value, in: bands, upperBound: scale.upperBound)
        let resolved: MonarchColorRole =
            if let tone {
                fill(tone: tone)
            } else if let band {
                fill(tone: band.tone)
            } else {
                fill(tone: bands.isEmpty ? nil : .neutral)
            }
        let textTone: MonarchTone? = tone ?? band?.tone
        return Reading(
            range: scale,
            value: value,
            clamped: min(max(value, scale.lowerBound), scale.upperBound),
            bandLabel: band?.label,
            fill: resolved,
            text: textTone.map { MonarchToneChrome.titleColor($0) } ?? \.textDefault
        )
    }

    /// A value as a fraction of the scale, limited to 0 through 1.
    static func fraction(of value: Double, in range: ClosedRange<Double>) -> Double {
        let span = range.upperBound - range.lowerBound
        guard span > 0, value.isFinite else { return 0 }
        return min(max((value - range.lowerBound) / span, 0), 1)
    }

    /// A band as fractions of the scale.
    static func span(of band: MonarchGaugeBand, in range: ClosedRange<Double>) -> Span {
        Span(
            from: fraction(of: band.range.lowerBound, in: range),
            to: fraction(of: band.range.upperBound, in: range),
            tone: band.tone
        )
    }

    // MARK: Text

    /// A number in a locale, with at most one fraction digit.
    static func number(_ value: Double, locale: Locale) -> String {
        value.formatted(.number.precision(.fractionLength(0...1)).locale(locale))
    }

    /// The visible value and what VoiceOver reads.
    struct ReadingText: Equatable {
        /// The value as written in the dial, or a dash.
        let value: String
        /// The value with its band or range, then the target.
        let accessibilityValue: String
    }

    /// The texts of a reading. Pure, so it is tested without a view.
    static func text(
        reading: Reading,
        target: Double?,
        noValueLabel: String,
        format: (Double) -> String
    ) -> ReadingText {
        let targetText = target.map { MonarchGaugeStrings.target(format($0)) }
        guard let value = reading.value else {
            return ReadingText(
                value: emptySymbol,
                accessibilityValue: [noValueLabel, targetText].compactMap { $0 }.joined(separator: ", ")
            )
        }
        let written = format(value)
        let spoken =
            if let band = reading.bandLabel, !band.isEmpty {
                MonarchGaugeStrings.valueInBand(value: written, band: band)
            } else {
                MonarchGaugeStrings.valueInRange(
                    value: written,
                    minimum: format(reading.range.lowerBound),
                    maximum: format(reading.range.upperBound)
                )
            }
        return ReadingText(
            value: written,
            accessibilityValue: [spoken, targetText].compactMap { $0 }.joined(separator: ", ")
        )
    }
}
