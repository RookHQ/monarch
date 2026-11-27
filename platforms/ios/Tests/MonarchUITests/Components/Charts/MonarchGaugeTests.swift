import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let gaugeBands = [
    MonarchGaugeBand(0...70, tone: .success, label: "Healthy"),
    MonarchGaugeBand(70...90, tone: .warning, label: "At risk"),
    MonarchGaugeBand(90...100, tone: .danger, label: "Critical"),
]

enum MonarchGaugeFixtures: ComponentFixtures {
    static let id = "gauge"
    static let fixtures = [
        Fixture("arc: sizes, bands and target") {
            VStack {
                ForEach(MonarchGaugeSize.allCases, id: \.self) { size in
                    MonarchGauge("CPU", value: 72, description: "Last hour", bands: gaugeBands, target: 80, size: size)
                }
            }
        },
        Fixture("needle") {
            VStack {
                MonarchGauge("Score", value: 40, bands: gaugeBands, target: 60, indicator: .needle)
                MonarchGauge("Score", value: 95, indicator: .needle, size: .sm, showsRange: false)
            }
        },
        Fixture("no bands, every tone") {
            VStack {
                MonarchGauge("Quota", value: 55)
                ForEach(MonarchTone.allCases, id: \.self) { tone in
                    MonarchGauge(value: 55, tone: tone, size: .sm, showsRange: false)
                }
            }
        },
        Fixture("no value, out of range, empty range and custom format") {
            VStack {
                MonarchGauge("No reading", value: nil, bands: gaugeBands, target: 50, noValueLabel: "Offline")
                MonarchGauge("Over", value: 140, bands: gaugeBands)
                MonarchGauge("Under", value: -20)
                MonarchGauge("Flat scale", value: 5, in: 5...5)
                MonarchGauge(
                    "A long label that names the reading in full, for a large text size",
                    value: 0.72,
                    in: 0...1,
                    description: "Measured over the last twenty-four hours",
                    valueLabel: "72 percent",
                    formatValue: { $0.formatted(.percent) }
                )
            }
        },
    ]
}

@Suite("MonarchGauge")
struct MonarchGaugeTests {
    private let english = Locale(identifier: "en_US")

    private func reading(
        _ value: Double?,
        in range: ClosedRange<Double> = 0...100,
        bands: [MonarchGaugeBand] = [],
        tone: MonarchTone? = nil
    ) -> MonarchGaugeTokens.Reading {
        MonarchGaugeTokens.reading(value: value, range: range, bands: bands, tone: tone)
    }

    private func text(_ reading: MonarchGaugeTokens.Reading, target: Double? = nil) -> MonarchGaugeTokens.ReadingText {
        let locale = english
        return MonarchGaugeTokens.text(reading: reading, target: target, noValueLabel: "No data") {
            MonarchGaugeTokens.number($0, locale: locale)
        }
    }

    @Test("A value belongs to one band; a boundary belongs to the next band, except at the end of the scale")
    func bandAt() {
        func label(_ value: Double) -> String? {
            MonarchGaugeTokens.band(at: value, in: gaugeBands, upperBound: 100)?.label
        }
        #expect(label(0) == "Healthy")
        #expect(label(69.9) == "Healthy")
        #expect(label(70) == "At risk")
        #expect(label(90) == "Critical")
        #expect(label(100) == "Critical")
        #expect(label(101) == nil)
        #expect(label(-1) == nil)
    }

    @Test("The value arc takes the tone of the band, the accent without bands, or the given tone")
    func fill() {
        #expect(reading(55).fill == \MonarchColorTokens.accentSolid)
        #expect(reading(72, bands: gaugeBands).fill == \MonarchColorTokens.warningSolid)
        #expect(reading(95, bands: gaugeBands).fill == \MonarchColorTokens.dangerSolid)
        // Outside every band there is no status to show.
        #expect(reading(140, bands: gaugeBands).fill == \MonarchColorTokens.textMuted)
        #expect(reading(72, bands: gaugeBands, tone: .info).fill == \MonarchColorTokens.infoSolid)
        #expect(reading(nil, bands: gaugeBands).fill == \MonarchColorTokens.textMuted)
    }

    @Test("Values outside the scale are drawn at its ends and keep their real value")
    func clamping() {
        let over = reading(140)
        #expect(over.clamped == 100)
        #expect(over.value == 140)
        #expect(reading(-20).clamped == 0)
        #expect(text(over).value == "140")
    }

    @Test("A missing or non-finite value is no value")
    func noValue() {
        for value in [nil, Double.nan, Double.infinity] {
            let empty = reading(value, bands: gaugeBands)
            #expect(!empty.hasValue)
            #expect(empty.clamped == 0)
            #expect(empty.bandLabel == nil)
            #expect(text(empty).value == "–")
            #expect(text(empty).accessibilityValue == "No data")
        }
    }

    @Test("An empty scale is widened, so nothing divides by zero")
    func emptyRange() {
        let flat = reading(5, in: 5...5)
        #expect(flat.range == 5...6)
        #expect(MonarchGaugeTokens.fraction(of: 5, in: flat.range) == 0)
        #expect(MonarchGaugeTokens.fraction(of: 5, in: 5...5) == 0)
    }

    @Test("VoiceOver reads the value with its band, or with the range, then the target")
    func accessibilityValue() {
        #expect(text(reading(72, bands: gaugeBands)).accessibilityValue == "72, At risk")
        #expect(text(reading(72)).accessibilityValue == "72 (from 0 to 100)")
        #expect(text(reading(72, bands: gaugeBands), target: 80).accessibilityValue == "72, At risk, Target 80")
        #expect(text(reading(nil), target: 80).accessibilityValue == "No data, Target 80")
        // A band without a name falls back to the range.
        let unnamed = [MonarchGaugeBand(0...100, tone: .success)]
        #expect(text(reading(1_500.3, in: 0...2_000, bands: unnamed)).accessibilityValue == "1,500.3 (from 0 to 2,000)")
    }

    @Test("Fractions and band spans are limited to the scale")
    func fractions() {
        #expect(MonarchGaugeTokens.fraction(of: 25, in: 0...100) == 0.25)
        #expect(MonarchGaugeTokens.fraction(of: 150, in: 0...100) == 1)
        #expect(MonarchGaugeTokens.fraction(of: -5, in: 0...100) == 0)
        #expect(MonarchGaugeTokens.fraction(of: 0, in: -50...50) == 0.5)
        let span = MonarchGaugeTokens.span(of: MonarchGaugeBand(70...120, tone: .warning), in: 0...100)
        #expect(span == MonarchGaugeTokens.Span(from: 0.7, to: 1, tone: .warning))
    }

    @Test("The scale runs over the top of a half circle, from the leading end to the trailing end")
    func geometry() {
        let rect = CGRect(x: 0, y: 0, width: 200, height: 100)
        let hub = MonarchGaugeTokens.hubRadius
        let centre = MonarchGaugeTokens.centre(of: rect)
        #expect(centre == CGPoint(x: 100, y: 100 - hub))
        let radius = MonarchGaugeTokens.outerRadius(of: rect)
        #expect(radius == 100 - hub)

        let start = MonarchGaugeTokens.point(at: 0, inset: 0, in: rect)
        let top = MonarchGaugeTokens.point(at: 0.5, inset: 0, in: rect)
        let end = MonarchGaugeTokens.point(at: 1, inset: 0, in: rect)
        #expect(abs(start.x - (100 - radius)) < 0.001 && abs(start.y - centre.y) < 0.001)
        #expect(abs(top.x - 100) < 0.001 && abs(top.y - (centre.y - radius)) < 0.001)
        #expect(abs(end.x - (100 + radius)) < 0.001 && abs(end.y - centre.y) < 0.001)
        // Every point stays inside the dial, also with an inset larger than the radius.
        let inner = MonarchGaugeTokens.point(at: 0.25, inset: 500, in: rect)
        #expect(inner == centre)
        #expect(MonarchGaugeTokens.point(at: 7, inset: 0, in: rect) == end)
    }

    @Test("Outer bands take room around the track, and a needle gauge draws bands on the track")
    func metrics() {
        let plain = MonarchGaugeTokens.metrics(width: 200, hasOuterBands: false)
        #expect(abs(plain.thickness - 14) < 0.001)
        #expect(abs(plain.trackInset - 7) < 0.001)
        let banded = MonarchGaugeTokens.metrics(width: 200, hasOuterBands: true)
        #expect(banded.trackInset > plain.trackInset)
        #expect(banded.bandInset(.arc) + banded.bandThickness(.arc) / 2 < banded.trackInset - banded.thickness / 2)
        #expect(plain.bandInset(.needle) == plain.trackInset)
        #expect(plain.bandThickness(.needle) == plain.thickness)
    }

    @Test("An arc is drawn only between its ends")
    func arcShape() {
        let rect = CGRect(x: 0, y: 0, width: 200, height: 100)
        #expect(MonarchGaugeArc(from: 0.5, to: 0.5, inset: 10).path(in: rect).isEmpty)
        #expect(MonarchGaugeArc(from: 0.8, to: 0.2, inset: 10).path(in: rect).isEmpty)
        let half = MonarchGaugeArc(from: 0, to: 0.5, inset: 0).path(in: rect).boundingRect
        // The first half of the scale stays in the left half of the dial.
        #expect(half.maxX <= 100.001)
        #expect(half.minX >= 0)
    }

    @Test("Sizes grow, and neutral bands and tones have no status colour")
    func tokens() {
        #expect(MonarchGaugeTokens.width(.sm) == 120)
        #expect(MonarchGaugeTokens.width(.md) == 160)
        #expect(MonarchGaugeTokens.width(.lg) == 200)
        #expect(MonarchGaugeTokens.bandColor(.neutral) == \MonarchColorTokens.borderStrong)
        #expect(MonarchGaugeTokens.bandColor(.success) == \MonarchColorTokens.successSolid)
        #expect(MonarchGaugeTokens.fill(tone: .neutral) == \MonarchColorTokens.textMuted)
        #expect(MonarchGaugeTokens.fill(tone: nil) == \MonarchColorTokens.accentSolid)
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchGaugeStrings.name == "Gauge")
        #expect(MonarchGaugeStrings.noValue == "No data")
        #expect(MonarchGaugeStrings.target("80") == "Target 80")
        #expect(MonarchGaugeStrings.valueInBand(value: "72", band: "At risk") == "72, At risk")
        #expect(MonarchGaugeStrings.valueInRange(value: "72", minimum: "0", maximum: "100") == "72 (from 0 to 100)")
    }
}
