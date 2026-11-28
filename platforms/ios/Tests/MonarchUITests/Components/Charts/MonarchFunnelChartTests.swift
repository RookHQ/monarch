import Accessibility
import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private let signup = [
    MonarchFunnelChartStage("Visited pricing", value: 800),
    MonarchFunnelChartStage("Started sign-up", value: 500),
    MonarchFunnelChartStage("Verified email", value: 200),
    MonarchFunnelChartStage("Paid", value: 150),
]

private let english = Locale(identifier: "en_US")

enum MonarchFunnelChartFixtures: ComponentFixtures {
    static let id = "funnel-chart"
    static let fixtures = [
        Fixture("centred with steps") {
            MonarchFunnelChart("Sign-up funnel", stages: signup, description: "Last 30 days")
        },
        Fixture("leading, no steps, selectable, colour slot") {
            MonarchFunnelChart(
                "Hiring pipeline",
                stages: [
                    MonarchFunnelChartStage("Applied", value: 120, color: .data3),
                    MonarchFunnelChartStage("Interviewed", value: 30, color: .data3),
                    MonarchFunnelChartStage("Hired", value: 0, color: .data3),
                ],
                alignment: .leading,
                showsSteps: false,
                onSelect: { _ in }
            )
        },
        Fixture("one stage, long labels, custom formats") {
            MonarchFunnelChart(
                "Visitors who opened the pricing page from a campaign email this quarter",
                stages: [
                    MonarchFunnelChartStage("Opened the pricing page from the spring campaign email", value: 12_400)
                ],
                footer: "Source: analytics.",
                formatValue: { "\(Int($0)) people" },
                formatRate: { "\(Int($0 * 100)) per cent" }
            )
        },
        Fixture("empty, loading, error") {
            VStack {
                MonarchFunnelChart("Sign-up funnel", stages: [], emptyMessage: "Stages appear after the first visit.")
                MonarchFunnelChart("Sign-up funnel", stages: signup, status: .loading)
                MonarchFunnelChart("Sign-up funnel", stages: signup, status: .error, onRetry: {})
            }
        },
        Fixture("data table") {
            let model = MonarchFunnelChartTokens.model(signup)
            MonarchChartFrameTableView(
                table: MonarchFunnelChartTokens.table(
                    signup,
                    model: model,
                    value: { $0.formatted() },
                    rate: { $0.formatted(.percent) },
                    labels: MonarchFunnelChartLabels()
                ),
                noValue: "No value"
            )
        },
    ]
}

@Suite("MonarchFunnelChart")
struct MonarchFunnelChartTests {
    @Test("Shares of the previous and first stage, and the largest drop")
    func model() {
        let model = MonarchFunnelChartTokens.model(signup)
        #expect(model.values == [800, 500, 200, 150])
        #expect(model.widest == 800)
        #expect(model.fromPrevious == [nil, 0.625, 0.4, 0.75])
        #expect(model.ofFirst == [1, 0.625, 0.25, 0.1875])
        #expect(model.largestDrop == MonarchFunnelChartTokens.Drop(from: 1, lost: 0.6))
    }

    @Test("Negative, non-finite and zero values count as zero without dividing by zero")
    func badValues() {
        let model = MonarchFunnelChartTokens.model([
            MonarchFunnelChartStage("A", value: 0),
            MonarchFunnelChartStage("B", value: -5),
            MonarchFunnelChartStage("C", value: .nan),
        ])
        #expect(model.values == [0, 0, 0])
        #expect(model.fromPrevious == [nil, 0, 0])
        #expect(model.ofFirst == [0, 0, 0])
        #expect(model.largestDrop == nil)
        #expect(MonarchFunnelChartTokens.scale(widest: 0) == 0...1)
    }

    @Test("A growing stage is not a drop")
    func growth() {
        let model = MonarchFunnelChartTokens.model([
            MonarchFunnelChartStage("A", value: 10),
            MonarchFunnelChartStage("B", value: 20),
        ])
        #expect(model.fromPrevious == [nil, 2])
        #expect(model.largestDrop == nil)
    }

    @Test("Bars are centred or start at the leading edge, and a small value stays visible")
    func spans() {
        #expect(MonarchFunnelChartTokens.barSpan(value: 400, widest: 800, alignment: .center) == 200...600)
        #expect(MonarchFunnelChartTokens.barSpan(value: 400, widest: 800, alignment: .leading) == 0...400)
        #expect(MonarchFunnelChartTokens.barSpan(value: 800, widest: 800, alignment: .center) == 0...800)
        #expect(MonarchFunnelChartTokens.barSpan(value: 1, widest: 10_000, alignment: .leading) == 0...50)
        #expect(MonarchFunnelChartTokens.barSpan(value: 0, widest: 800, alignment: .center) == 0...0)
    }

    @Test("A ready chart without stages is empty; other statuses are kept")
    func status() {
        #expect(MonarchFunnelChartTokens.status(.ready, stageCount: 0) == .empty)
        #expect(MonarchFunnelChartTokens.status(.ready, stageCount: 2) == .ready)
        #expect(MonarchFunnelChartTokens.status(.loading, stageCount: 0) == .loading)
        #expect(MonarchFunnelChartTokens.status(.error, stageCount: 0) == .error)
    }

    @Test("The kept height grows with the stages and is never under 120 points")
    func height() {
        #expect(MonarchFunnelChartTokens.height(stageCount: 1, showsSteps: true) == 120)
        #expect(MonarchFunnelChartTokens.height(stageCount: 4, showsSteps: true) == 304)
        #expect(MonarchFunnelChartTokens.height(stageCount: 4, showsSteps: false) == 224)
    }

    @Test("The summary names the stages, the overall conversion and the largest drop")
    func summary() {
        let model = MonarchFunnelChartTokens.model(signup)
        let text = MonarchFunnelChartTokens.summary(
            signup,
            model: model,
            count: "4",
            rate: { MonarchFunnelChartTokens.rateText($0, locale: english, format: nil) },
            labels: MonarchFunnelChartLabels()
        )
        #expect(
            text
                == "4 stages from Visited pricing to Paid. Overall conversion 18.8%. "
                + "Largest drop: Started sign-up to Verified email, 60% lost"
        )
        let one = MonarchFunnelChartTokens.summary(
            [signup[0]],
            model: MonarchFunnelChartTokens.model([signup[0]]),
            count: "1",
            rate: { "\($0)" },
            labels: MonarchFunnelChartLabels()
        )
        #expect(one == "1 stage, Visited pricing")
    }

    @Test("Each stage is read as its label, value and shares")
    func describe() {
        let labels = MonarchFunnelChartLabels()
        #expect(
            MonarchFunnelChartTokens.describe(
                signup[0],
                value: "800",
                fromPrevious: nil,
                ofFirst: "100%",
                labels: labels
            )
                == "Visited pricing: 800"
        )
        #expect(
            MonarchFunnelChartTokens.describe(
                signup[1],
                value: "500",
                fromPrevious: "62.5%",
                ofFirst: "62.5%",
                labels: labels
            )
                == "Started sign-up: 500, 62.5% of previous, 62.5% of first"
        )
        let german = MonarchFunnelChartLabels(describeFirst: "%1$@ – %2$@")
        #expect(
            MonarchFunnelChartTokens.describe(signup[0], value: "800", fromPrevious: nil, ofFirst: "", labels: german)
                == "Visited pricing – 800"
        )
    }

    @Test("The step line and number formats use the locale, or the app's closures")
    func formats() {
        #expect(
            MonarchFunnelChartTokens.stepText(rate: "62.5%", dropped: "300", labels: MonarchFunnelChartLabels())
                == "62.5% continued · 300 dropped"
        )
        #expect(MonarchFunnelChartTokens.valueText(4_800, locale: english, format: nil) == "4,800")
        #expect(
            MonarchFunnelChartTokens.valueText(4_800, locale: english, format: { "\(Int($0)) people" }) == "4800 people"
        )
        #expect(MonarchFunnelChartTokens.rateText(0.625, locale: english, format: nil) == "62.5%")
        #expect(MonarchFunnelChartTokens.rateText(1.0 / 3, locale: english, format: nil) == "33.3%")
    }

    @Test("The table has counts and both shares; the audio graph has counts only")
    func table() {
        let model = MonarchFunnelChartTokens.model(signup)
        let table = MonarchFunnelChartTokens.table(
            signup,
            model: model,
            value: { "\(Int($0))" },
            rate: { "\(Int($0 * 100))%" },
            labels: MonarchFunnelChartLabels(stage: "Step")
        )
        #expect(table.categoryTitle == "Step")
        #expect(table.categories == ["Visited pricing", "Started sign-up", "Verified email", "Paid"])
        #expect(table.series.map(\.name) == ["Count", "From previous", "Of first"])
        #expect(table.series[1].values.first == .some(nil))
        #expect(table.series[2].formattedValues == ["100%", "62%", "25%", "18%"])
        let audio = MonarchFunnelChartTokens.audioGraphTable(table)
        #expect(audio.series.count == 1)
        #expect(audio.valueTitle == "Count")
    }

    @Test("VoiceOver's chart descriptor has one point per stage with the formatted count")
    @MainActor
    func descriptor() {
        let model = MonarchFunnelChartTokens.model(signup)
        let table = MonarchFunnelChartTokens.table(
            signup,
            model: model,
            value: { "\(Int($0)) people" },
            rate: { "\($0)" },
            labels: MonarchFunnelChartLabels()
        )
        let chart = MonarchChartFrameDescriptor(
            title: "Sign-up",
            summary: "4 stages",
            table: MonarchFunnelChartTokens.audioGraphTable(table)
        )
        .makeChartDescriptor()
        #expect(chart.series.count == 1)
        #expect(chart.series[0].dataPoints.count == 4)
        #expect(chart.series[0].dataPoints.first?.label == "800 people")
        #expect(chart.yAxis?.range == 0...800)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchFunnelChartStrings.stage == "Stage")
        #expect(MonarchFunnelChartStrings.value == "Count")
        #expect(MonarchFunnelChartStrings.fromPrevious == "From previous")
        #expect(MonarchFunnelChartStrings.ofFirst == "Of first")
        for format in [
            MonarchFunnelChartStrings.step, MonarchFunnelChartStrings.oneStage, MonarchFunnelChartStrings.stages,
            MonarchFunnelChartStrings.overall, MonarchFunnelChartStrings.largestDrop,
            MonarchFunnelChartStrings.describeFirst, MonarchFunnelChartStrings.describe,
        ] {
            #expect(!format.hasPrefix("monarch."))
            #expect(format.contains("%1$@"))
        }
    }

    @Test("A leading bar starts at the leading edge in both layout directions")
    @MainActor
    func barDirection() {
        for direction in [LayoutDirection.leftToRight, .rightToLeft] {
            let bar = MonarchFunnelChartBar(span: 0...0.5, scale: 0...1, role: \.data1)
                .frame(width: 200, height: 20)
                .environment(\.layoutDirection, direction)
            let leftHalf = funnelInk(bar, atX: 0.25)
            let rightHalf = funnelInk(bar, atX: 0.75)
            if direction == .leftToRight {
                #expect(leftHalf && !rightHalf)
            } else {
                #expect(rightHalf && !leftHalf)
            }
        }
    }

    @Test("The chart keeps its planned height")
    @MainActor
    func keepsHeight() {
        let size = Host.layout(MonarchFunnelChart("Sign-up", stages: signup))
        #expect(size.height >= MonarchFunnelChartTokens.height(stageCount: 4, showsSteps: true))
    }
}

/// Whether the rendered view has paint at a fraction of its width, on its middle row.
@MainActor
private func funnelInk(_ view: some View, atX fraction: CGFloat) -> Bool {
    let renderer = ImageRenderer(content: view)
    renderer.scale = 1
    guard let image = renderer.cgImage else { return false }
    let width = image.width
    let height = image.height
    var pixels = [UInt8](repeating: 0, count: width * height * 4)
    let drawn = pixels.withUnsafeMutableBytes { buffer -> Bool in
        guard
            let context = CGContext(
                data: buffer.baseAddress,
                width: width,
                height: height,
                bitsPerComponent: 8,
                bytesPerRow: width * 4,
                space: CGColorSpaceCreateDeviceRGB(),
                bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
            )
        else { return false }
        context.draw(image, in: CGRect(x: 0, y: 0, width: width, height: height))
        return true
    }
    guard drawn else { return false }
    let x = min(width - 1, Int(CGFloat(width) * fraction))
    let y = height / 2
    return pixels[(y * width + x) * 4 + 3] > 0
}
