import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchStackedProportionBarFixtures: ComponentFixtures {
    static let id = "stacked-proportion-bar"

    static let pipeline = [
        MonarchStackedProportionBarSegment("new", label: "New", value: 1_240),
        MonarchStackedProportionBarSegment("campaign", label: "In a campaign", value: 610),
        MonarchStackedProportionBarSegment("replied", label: "Replied", value: 182),
        MonarchStackedProportionBarSegment("converted", label: "Converted", value: 41),
        MonarchStackedProportionBarSegment("unsubscribed", label: "Unsubscribed", value: 6),
        MonarchStackedProportionBarSegment("bounced", label: "Bounced, a sixth part with a long name", value: 2),
    ]

    static let fixtures = [
        Fixture("palette, every size and a tiny share") {
            VStack(alignment: .leading) {
                ForEach(MonarchStackedProportionBarSize.allCases, id: \.self) { size in
                    MonarchStackedProportionBar("Lead pipeline", segments: pipeline, size: size)
                }
            }
        },
        Fixture("every legend content") {
            VStack(alignment: .leading) {
                ForEach(MonarchStackedProportionBarLegendContent.allCases, id: \.self) { content in
                    MonarchStackedProportionBar(
                        "Lead pipeline",
                        segments: Array(pipeline.prefix(3)),
                        legendContent: content,
                        percentDigits: 1
                    )
                }
            }
        },
        Fixture("capacity with a remainder and a custom format") {
            MonarchStackedProportionBar(
                "Storage",
                segments: [
                    MonarchStackedProportionBarSegment("media", label: "Media", value: 42, color: .data3),
                    MonarchStackedProportionBarSegment("docs", label: "Documents", value: 18),
                ],
                total: 100,
                remainderLabel: "Free",
                legendContent: .value,
                size: .lg,
                formatValue: { "\(Int($0)) GB" }
            )
        },
        Fixture("status tones") {
            MonarchStackedProportionBar(
                "Monitors",
                segments: [
                    MonarchStackedProportionBarSegment("up", label: "Operational", value: 41, tone: .success),
                    MonarchStackedProportionBarSegment("slow", label: "Degraded", value: 3, tone: .warning),
                    MonarchStackedProportionBarSegment("down", label: "Down", value: 1, tone: .danger),
                    MonarchStackedProportionBarSegment("info", label: "Maintenance", value: 1, tone: .info),
                    MonarchStackedProportionBarSegment("paused", label: "Paused", value: 2, tone: .neutral),
                ]
            )
        },
        Fixture("hidden legend, zeros kept, empty and custom empty") {
            VStack(alignment: .leading) {
                MonarchStackedProportionBar("Lead pipeline", segments: pipeline, showsLegend: false)
                MonarchStackedProportionBar(
                    "Votes",
                    segments: [
                        MonarchStackedProportionBarSegment("yes", label: "Yes", value: 3),
                        MonarchStackedProportionBarSegment("no", label: "No", value: 0),
                    ],
                    hidesZero: false
                )
                MonarchStackedProportionBar("Empty", segments: [])
                MonarchStackedProportionBar(
                    "All zero",
                    segments: [MonarchStackedProportionBarSegment("a", label: "A", value: 0)],
                    emptyLabel: "Nothing stored"
                )
            }
        },
    ]
}

@Suite("MonarchStackedProportionBar")
struct MonarchStackedProportionBarTests {
    private let english = Locale(identifier: "en_US")

    @Test("Palette slots follow the position; zeros, negatives and non-finite values count as 0")
    func parts() {
        let segments = [
            MonarchStackedProportionBarSegment("a", label: "A", value: 5),
            MonarchStackedProportionBarSegment("zero", label: "Zero", value: 0),
            MonarchStackedProportionBarSegment("negative", label: "Negative", value: -3),
            MonarchStackedProportionBarSegment("nan", label: "NaN", value: .nan),
            MonarchStackedProportionBarSegment("pinned", label: "Pinned", value: 2, color: .data5),
        ]
        let shown = MonarchStackedProportionBarTokens.parts(segments, hidesZero: true)
        #expect(shown.map(\.key) == ["a", "pinned"])
        #expect(shown.map(\.color) == [.data1, .data5])
        #expect(shown.map(\.role) == [\MonarchColorTokens.data1, \MonarchColorTokens.data5])
        let all = MonarchStackedProportionBarTokens.parts(segments, hidesZero: false)
        #expect(all.map(\.value) == [5, 0, 0, 0, 2])
        // The sixth part and later are neutral, never a cycled colour.
        let many = (0..<7).map { MonarchStackedProportionBarSegment("\($0)", label: "\($0)", value: 1) }
        #expect(MonarchStackedProportionBarTokens.parts(many, hidesZero: true).last?.color == .neutral)
    }

    @Test("Status mode needs a tone on every segment; a mix falls back to the palette")
    func statusMode() {
        let toned = [
            MonarchStackedProportionBarSegment("up", label: "Up", value: 1, tone: .success),
            MonarchStackedProportionBarSegment("down", label: "Down", value: 1, tone: .danger),
        ]
        #expect(MonarchStackedProportionBarTokens.isStatusMode(toned))
        #expect(
            MonarchStackedProportionBarTokens.parts(toned, hidesZero: true).map(\.role)
                == [\MonarchColorTokens.successSolid, \MonarchColorTokens.dangerSolid]
        )
        let mixed = toned + [MonarchStackedProportionBarSegment("x", label: "X", value: 1)]
        #expect(!MonarchStackedProportionBarTokens.isStatusMode(mixed))
        #expect(
            MonarchStackedProportionBarTokens.parts(mixed, hidesZero: true).map(\.role)
                == [\MonarchColorTokens.data1, \MonarchColorTokens.data2, \MonarchColorTokens.data3]
        )
        #expect(!MonarchStackedProportionBarTokens.isStatusMode([]))
        #expect(MonarchStackedProportionBarTokens.toneRole(.neutral) == \MonarchColorTokens.textSubtle)
        #expect(MonarchStackedProportionBarTokens.toneRole(.info) == \MonarchColorTokens.infoSolid)
        #expect(MonarchStackedProportionBarTokens.toneRole(.warning) == \MonarchColorTokens.warningSolid)
    }

    @Test("The whole is the sum, or a larger total")
    func total() {
        #expect(MonarchStackedProportionBarTokens.total(sum: 60, given: nil) == 60)
        #expect(MonarchStackedProportionBarTokens.total(sum: 60, given: 100) == 100)
        #expect(MonarchStackedProportionBarTokens.total(sum: 60, given: 10) == 60)
        #expect(MonarchStackedProportionBarTokens.total(sum: 60, given: .infinity) == 60)
    }

    @Test("Shares are written in the locale, with <1% and >99% at the ends")
    func share() {
        #expect(MonarchStackedProportionBarTokens.share(34, of: 100, digits: 0, locale: english) == "34%")
        #expect(MonarchStackedProportionBarTokens.share(1, of: 300, digits: 0, locale: english) == "<1%")
        #expect(MonarchStackedProportionBarTokens.share(299.5, of: 300, digits: 0, locale: english) == ">99%")
        #expect(MonarchStackedProportionBarTokens.share(1, of: 300, digits: 1, locale: english) == "0.3%")
        #expect(MonarchStackedProportionBarTokens.share(0, of: 300, digits: 0, locale: english) == "0%")
        #expect(MonarchStackedProportionBarTokens.share(5, of: 0, digits: 0, locale: english) == "0%")
        #expect(MonarchStackedProportionBarTokens.share(100, of: 100, digits: 0, locale: english) == "100%")
        let german = MonarchStackedProportionBarTokens.share(1, of: 300, digits: 1, locale: Locale(identifier: "de_DE"))
        #expect(german.contains("0,3"))
    }

    @Test("The legend text follows the legend content")
    func legendValue() {
        func text(_ content: MonarchStackedProportionBarLegendContent, format: ((Double) -> String)? = nil) -> String? {
            MonarchStackedProportionBarTokens.legendValue(
                1_204,
                total: 3_541,
                content: content,
                digits: 0,
                locale: english,
                formatValue: format
            )
        }
        #expect(text(.both) == "1,204 · 34%")
        #expect(text(.value) == "1,204")
        #expect(text(.percent) == "34%")
        #expect(text(.none) == nil)
        #expect(text(.value, format: { "\(Int($0)) GB" }) == "1204 GB")
    }

    @Test("Segment widths are proportional, keep a minimum and leave room for the gaps")
    func widths() {
        let even = MonarchStackedProportionBarTokens.widths([1, 1], remainder: 2, available: 404, gap: 2, minimum: 4)
        #expect(even.segments == [100, 100])
        #expect(even.remainder == 200)

        let tiny = MonarchStackedProportionBarTokens.widths([999, 1], remainder: 0, available: 102, gap: 2, minimum: 4)
        #expect(tiny.segments[1] == 4)
        #expect(abs(tiny.segments[0] - 96) < 0.0001)

        let empty = MonarchStackedProportionBarTokens.widths([0, 0], remainder: 0, available: 100, gap: 2, minimum: 4)
        #expect(empty.segments == [0, 0])
        #expect(
            MonarchStackedProportionBarTokens.widths([], remainder: 0, available: 100, gap: 2, minimum: 4).segments
                .isEmpty
        )
        #expect(
            MonarchStackedProportionBarTokens.widths([1], remainder: 0, available: 0, gap: 2, minimum: 4).segments == [
                0
            ]
        )
    }

    @Test("Thickness grows with the size")
    func thickness() {
        let sizes: [CGFloat] = MonarchStackedProportionBarSize.allCases.map(MonarchStackedProportionBarTokens.thickness)
        let expected: [CGFloat] = [MonarchTokens.Space.space1, MonarchTokens.Space.space2, MonarchTokens.Space.space3]
        #expect(sizes == expected)
    }

    @Test("Without a visible legend, VoiceOver reads every entry")
    func accessibility() {
        #expect(
            MonarchStackedProportionBarTokens.accessibilityValue([("New", "1,240 · 60%"), ("Free", nil)])
                == "New, 1,240 · 60%; Free"
        )
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchStackedProportionBarStrings.empty == "No data yet")
        #expect(MonarchStackedProportionBarStrings.valueAndShare("1", "2%") == "1 · 2%")
        #expect(MonarchStackedProportionBarStrings.lessThan("1%") == "<1%")
        #expect(MonarchStackedProportionBarStrings.moreThan("99%") == ">99%")
    }
}
