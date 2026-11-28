import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let sparklineValues: [Double?] = [3, 5, 4, nil, 8, 7, 9, 6, 11]

enum MonarchSparklineFixtures: ComponentFixtures {
    static let id = "sparkline"
    static let fixtures = [
        Fixture("every variant and tone") {
            VStack(alignment: .leading) {
                ForEach(MonarchSparklineVariant.allCases, id: \.self) { variant in
                    MonarchSparkline(sparklineValues, label: "Sign-ups", variant: variant)
                    ForEach(MonarchTone.allCases, id: \.self) { tone in
                        MonarchSparkline(sparklineValues, tone: tone, variant: variant)
                    }
                }
            }
        },
        Fixture("gaps, lone points, inverted, fixed range and full width") {
            VStack(alignment: .leading) {
                MonarchSparkline([1, nil, 4, nil, nil, 2, 3], label: "Rank", isInverted: true)
                MonarchSparkline([4, 6, 5], range: 0...100, showsLastPoint: false)
                MonarchSparkline([-3, 2, -1, 4], tone: .info, variant: .bar, fullWidth: true)
                MonarchSparkline([5, 5, 5, 5], variant: .area, fullWidth: true)
                MonarchSparkline([-2, -4, -1], variant: .bar, isInverted: true)
            }
        },
        Fixture("not enough data") {
            VStack(alignment: .leading) {
                MonarchSparkline([], label: "Sign-ups")
                MonarchSparkline([4])
                MonarchSparkline([nil, .nan], variant: .bar, notEnoughDataLabel: "Waiting for the first value")
                MonarchSparkline([4], variant: .bar)
            }
        },
    ]
}

@Suite("MonarchSparkline")
struct MonarchSparklineTests {
    private let size = CGSize(width: 80, height: 24)

    private func plot(
        _ values: [Double?],
        _ variant: MonarchSparklineVariant = .line,
        range: ClosedRange<Double>? = nil,
        isInverted: Bool = false
    ) -> MonarchSparklineTokens.Plot {
        MonarchSparklineTokens.Plot(values: values, variant: variant, range: range, isInverted: isInverted)
    }

    @Test("The latest, lowest and highest values ignore gaps and values that are not finite")
    func stats() {
        let stats = plot([3, nil, 9, .nan, 5, .infinity]).stats
        #expect(stats == MonarchSparklineTokens.Stats(last: 5, minimum: 3, maximum: 9, count: 3))
        #expect(plot([nil, .nan]).stats == nil)
        #expect(plot([]).stats == nil)
    }

    @Test("A line needs two values and bars need one")
    func enoughData() {
        #expect(!plot([4]).hasEnoughData)
        #expect(!plot([4, nil], .area).hasEnoughData)
        #expect(plot([4, 5]).hasEnoughData)
        #expect(plot([4], .bar).hasEnoughData)
        #expect(!plot([nil], .bar).hasEnoughData)
    }

    @Test("Larger values are drawn higher, and inverted they are drawn lower")
    func vertical() {
        let padding = MonarchSparklineTokens.padding
        let normal = plot([0, 10])
        #expect(normal.y(0, in: size) == size.height - padding)
        #expect(normal.y(10, in: size) == padding)
        #expect(normal.y(5, in: size) == size.height / 2)
        let inverted = plot([0, 10], isInverted: true)
        #expect(inverted.y(0, in: size) == padding)
        #expect(inverted.y(10, in: size) == size.height - padding)
        // A value outside a fixed range stays inside the plot.
        #expect(plot([0, 500], range: 0...10).y(500, in: size) == padding)
    }

    @Test("A flat series is drawn in the middle, not on an edge")
    func flatSeries() {
        let flat = plot([5, 5, 5])
        #expect(flat.domain == 4...6)
        #expect(flat.y(5, in: size) == size.height / 2)
    }

    @Test("Points are evenly spaced from left to right, with a gap where a value is missing")
    func points() {
        let points = plot([1, nil, 3]).points(in: size)
        let padding = MonarchSparklineTokens.padding
        #expect(points.count == 3)
        #expect(points[0]?.x == padding)
        #expect(points[1] == nil)
        #expect(points[2]?.x == size.width - padding)
    }

    @Test("The line breaks at every gap")
    func runs() {
        let runs = plot([1, 2, nil, 3, nil, nil, 4, 5, 6]).runs(in: size)
        #expect(runs.map(\.count) == [2, 1, 3])
        #expect(plot([nil, nil, 1, 2]).runs(in: size).map(\.count) == [2])
    }

    @Test("Lone points always get a dot, and the latest point gets one when asked")
    func dots() {
        let series = plot([1, 2, nil, 3, nil, 4, 5])
        let lone = series.points(in: size)[3]
        let last = series.points(in: size)[6]
        #expect(series.dots(in: size, includesLast: false) == [lone].compactMap { $0 })
        #expect(series.dots(in: size, includesLast: true) == [lone, last].compactMap { $0 })
        // A lone latest point is not drawn twice.
        #expect(plot([1, 2, nil, 3]).dots(in: size, includesLast: true).count == 1)
    }

    @Test("Bars start at zero: positive ones go up and negative ones go down")
    func bars() {
        let series = plot([4, -4, nil, 0], .bar)
        let bars = series.bars(in: size)
        let zero = series.y(0, in: size)
        #expect(bars.count == 3)
        #expect(abs(bars[0].maxY - zero) < 0.001 && bars[0].minY < zero)
        #expect(abs(bars[1].minY - zero) < 0.001 && bars[1].maxY > zero)
        // A zero is still visible.
        #expect(bars[2].height == MonarchSparklineTokens.minBarHeight)
        // Every bar stays inside its slot and inside the plot.
        #expect(bars.allSatisfy { $0.minX >= 0 && $0.maxX <= size.width && $0.minY >= 0 && $0.maxY <= size.height })
        // The gap leaves the slot of the missing value empty.
        #expect(bars[2].minX >= size.width * 3 / 4)
    }

    @Test("Bars always include zero in their range")
    func barDomain() {
        #expect(plot([5, 8], .bar).domain == 0...8)
        #expect(plot([-5, -8], .bar).domain == -8...0)
        #expect(plot([5, 8], .line).domain == 5...8)
    }

    @Test("The drawn paths stay inside the frame they are given")
    func shapes() {
        let rect = CGRect(x: 10, y: 20, width: 80, height: 24)
        let series = plot(sparklineValues, .area)
        for part in [MonarchSparklineMark.Part.line, .area, .dots(includesLast: true, radius: 2)] {
            let bounds = MonarchSparklineMark(plot: series, part: part).path(in: rect).boundingRect
            #expect(!bounds.isEmpty)
            #expect(rect.insetBy(dx: -0.001, dy: -0.001).contains(bounds), "\(part) leaves its frame")
        }
        let bars = MonarchSparklineMark(plot: plot(sparklineValues, .bar), part: .bars).path(in: rect).boundingRect
        #expect(rect.insetBy(dx: -0.001, dy: -0.001).contains(bars))
    }

    @Test("The area is filled from the line down to the baseline, over the whole run")
    func areaFill() {
        let rect = CGRect(x: 0, y: 0, width: 80, height: 24)
        let flat = plot([5, 5, 5, 5], .area)
        let area = MonarchSparklineMark(plot: flat, part: .area).path(in: rect)
        let line = flat.y(5, in: rect.size)
        let baseline = flat.baseline(in: rect.size)
        // Under the start of the line as well as under its end: a rectangle, not a wedge.
        #expect(area.contains(CGPoint(x: 4, y: baseline - 1)))
        #expect(area.contains(CGPoint(x: 76, y: baseline - 1)))
        #expect(area.contains(CGPoint(x: 4, y: line + 1)))
        #expect(!area.contains(CGPoint(x: 40, y: line - 1)))
        #expect(abs(area.boundingRect.height - (baseline - line)) < 0.001)

        // Inverted, the baseline is the top edge.
        let inverted = plot([5, 5, 5, 5], .area, isInverted: true)
        let top = MonarchSparklineMark(plot: inverted, part: .area).path(in: rect)
        #expect(top.contains(CGPoint(x: 4, y: inverted.baseline(in: rect.size) + 1)))

        // A gap splits the fill: nothing is drawn under the missing value.
        let gapped = MonarchSparklineMark(plot: plot([5, 6, nil, nil, 6, 5], .area), part: .area).path(in: rect)
        #expect(!gapped.contains(CGPoint(x: 40, y: baseline - 1)))
    }

    @Test("VoiceOver reads the latest, lowest and highest values, then the tone")
    func summary() throws {
        let stats = try #require(plot(sparklineValues).stats)
        let locale = Locale(identifier: "en_US")
        func summary(_ tone: MonarchTone?) -> String {
            MonarchSparklineTokens.summary(stats: stats, tone: tone) {
                MonarchSparklineTokens.number($0, locale: locale)
            }
        }
        #expect(summary(nil) == "latest 11, low 3, high 11")
        #expect(summary(.neutral) == "latest 11, low 3, high 11")
        #expect(summary(.warning) == "latest 11, low 3, high 11. Warning")
        #expect(summary(.danger).hasSuffix(". \(MonarchToneStrings.name(.danger) ?? "")"))
    }

    @Test("Without a tone the sparkline takes the surrounding foreground style")
    func styles() {
        let plain = MonarchSparklineTokens.styles(tone: nil)
        #expect(plain.line == nil && plain.area == nil)
        let success = MonarchSparklineTokens.styles(tone: .success)
        #expect(success.line == \MonarchColorTokens.successStrong)
        #expect(success.area == \MonarchColorTokens.successSubtle)
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchSparklineStrings.notEnoughData == "Not enough data")
        #expect(MonarchSparklineStrings.summary(last: "1", minimum: "2", maximum: "3") == "latest 1, low 2, high 3")
    }
}
