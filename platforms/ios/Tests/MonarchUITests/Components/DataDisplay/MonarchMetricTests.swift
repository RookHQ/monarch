import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchMetricFixtures: ComponentFixtures {
    static let id = "metric"
    static let fixtures = [
        Fixture("every size, stacked, with a change") {
            VStack(alignment: .leading) {
                ForEach(MonarchMetricSize.allCases, id: \.self) { size in
                    MonarchMetric(
                        "Open tickets",
                        value: 128,
                        change: MonarchMetricChange(0.12, comparison: "vs last week"),
                        size: size
                    )
                }
            }
        },
        Fixture("inline in every size") {
            VStack(alignment: .leading) {
                ForEach(MonarchMetricSize.allCases, id: \.self) { size in
                    MonarchMetric(
                        "Response time",
                        value: 840,
                        unit: "ms",
                        change: MonarchMetricChange(-0.08, goodDirection: .down),
                        size: size,
                        layout: .inline
                    )
                }
            }
        },
        Fixture("formats: percent, currency, compact") {
            VStack(alignment: .leading) {
                MonarchMetric("Conversion", value: 0.034, format: .percent)
                MonarchMetric("Revenue", value: 1_284_000, format: .currency(code: "EUR"), isCompact: true)
                MonarchMetric("Events", value: 12_345, size: .lg, isCompact: true)
            }
        },
        Fixture("empty values and trailing alignment") {
            VStack(alignment: .trailing) {
                MonarchMetric("Churn", value: nil, unit: "%", alignment: .trailing)
                MonarchMetric("Rating", text: nil, emptyLabel: "Not rated yet")
                MonarchMetric("Rating", text: "4.8 / 5", spokenText: "4.8 out of 5", alignment: .trailing)
            }
        },
        Fixture("long label, unit and comparison") {
            MonarchMetric(
                "Median time to first response across every support queue",
                value: 123_456_789,
                unit: "requests per month",
                change: MonarchMetricChange(
                    current: 120,
                    previous: 100,
                    comparison: "compared with the same week of last year",
                    variant: .subtle
                ),
                size: .lg
            )
        },
    ]
}

@Suite("MonarchMetric")
struct MonarchMetricTests {
    private let english = Locale(identifier: "en_US")

    @Test("Numbers are formatted by format and notation")
    func formatting() {
        #expect(MonarchMetric.formatted(12_345, locale: english) == "12,345")
        #expect(MonarchMetric.formatted(12_345, isCompact: true, locale: english) == "12K")
        #expect(MonarchMetric.formatted(0.12, format: .percent, locale: english) == "12%")
        #expect(MonarchMetric.formatted(1_250, format: .currency(code: "USD"), locale: english) == "$1,250.00")
        if #available(iOS 18, macOS 15, *) {
            #expect(
                MonarchMetric.formatted(3_400_000, format: .currency(code: "USD"), isCompact: true, locale: english)
                    == "$3.4M"
            )
        }
    }

    @Test("Compact numbers and currency follow the locale: de_DE and ja_JP (C-092)")
    func compactInOtherLocales() {
        let german = Locale(identifier: "de_DE")
        let japanese = Locale(identifier: "ja_JP")
        #expect(MonarchMetric.formatted(12_345, isCompact: true, locale: german).contains("12"))
        #expect(MonarchMetric.formatted(3_400_000, isCompact: true, locale: german).contains("Mio"))
        #expect(MonarchMetric.formatted(3_400_000, isCompact: true, locale: japanese).contains("万"))
        #expect(MonarchMetric.formatted(1_234, locale: german) == "1.234")
        if #available(iOS 18, macOS 15, *) {
            let euro = MonarchMetric.formatted(
                3_400_000,
                format: .currency(code: "EUR"),
                isCompact: true,
                locale: german
            )
            #expect(euro.contains("Mio") && euro.contains("€"))
            let yen = MonarchMetric.formatted(
                3_400_000,
                format: .currency(code: "JPY"),
                isCompact: true,
                locale: japanese
            )
            #expect(yen.contains("万") && (yen.contains("¥") || yen.contains("￥")))
        }
        // Before iOS 18 a compact currency is written in full.
        #expect(
            MonarchMetric.formatted(1_250, format: .currency(code: "EUR"), locale: german).contains("1.250,00")
        )
        let figure = MonarchMetricTokens.figure(
            12_345,
            format: .number,
            isCompact: true,
            emptyLabel: nil,
            locale: german
        )
        #expect(figure.spoken == "12.345")
    }

    @Test("A compact figure speaks the full value")
    func compactFigure() {
        let figure = MonarchMetricTokens.figure(
            12_345,
            format: .number,
            isCompact: true,
            emptyLabel: nil,
            locale: english
        )
        #expect(figure.text == "12K")
        #expect(figure.spoken == "12,345")
        #expect(!figure.isEmpty)
    }

    @Test("A missing value is a dash read as No data, or the override")
    func emptyFigure() {
        let number = MonarchMetricTokens.figure(
            nil,
            format: .number,
            isCompact: false,
            emptyLabel: nil,
            locale: english
        )
        #expect(number.text == "—")
        #expect(number.spoken == "No data")
        #expect(number.isEmpty)
        let infinite = MonarchMetricTokens.figure(
            .infinity,
            format: .number,
            isCompact: false,
            emptyLabel: nil,
            locale: english
        )
        #expect(infinite.isEmpty)
        let text = MonarchMetricTokens.figure(text: "", spoken: nil, emptyLabel: "Not rated")
        #expect(text.isEmpty)
        #expect(text.spoken == "Not rated")
        let given = MonarchMetricTokens.figure(text: "4.8 / 5", spoken: "4.8 out of 5", emptyLabel: nil)
        #expect(given.text == "4.8 / 5")
        #expect(given.spoken == "4.8 out of 5")
    }

    @Test("VoiceOver reads value with unit, then the change; no unit for an empty value")
    func accessibilityValue() {
        let figure = MonarchMetricTokens.Figure(text: "840", spoken: "840", isEmpty: false)
        let change = MonarchMetricTokens.changeSentence(
            MonarchMetricChange(-0.08, comparison: "vs last week", goodDirection: .down),
            locale: english
        )
        #expect(change == "down 8% vs last week, better")
        #expect(
            MonarchMetricTokens.accessibilityValue(figure: figure, unit: "ms", changeSentence: change)
                == "840 ms, down 8% vs last week, better"
        )
        #expect(MonarchMetricTokens.accessibilityValue(figure: figure, unit: nil, changeSentence: nil) == "840")
        let empty = MonarchMetricTokens.Figure(text: "—", spoken: "No data", isEmpty: true)
        #expect(MonarchMetricTokens.accessibilityValue(figure: empty, unit: "ms", changeSentence: nil) == "No data")
    }

    @Test("A change from two periods is a ratio of the previous one")
    func changeFromPeriods() {
        let change = MonarchMetricChange(current: 120, previous: 100)
        #expect(change.value == 0.2)
        #expect(MonarchMetricChange(current: 5, previous: 0).value == nil)
        #expect(MonarchMetricTokens.changeSentence(MonarchMetricChange(nil), locale: english) == "No earlier data")
    }

    @Test("Type roles grow with size; inline is one size")
    func typeRoles() {
        #expect(MonarchMetricTokens.valueStyle(.sm, layout: .stacked) == .body)
        #expect(MonarchMetricTokens.valueStyle(.md, layout: .stacked) == .heading)
        #expect(MonarchMetricTokens.valueStyle(.lg, layout: .stacked) == .display)
        #expect(MonarchMetricTokens.labelStyle(.lg, layout: .stacked) == .label)
        #expect(MonarchMetricTokens.labelStyle(.md, layout: .stacked) == .caption)
        for size in MonarchMetricSize.allCases {
            #expect(MonarchMetricTokens.valueStyle(size, layout: .inline) == .label)
            #expect(MonarchMetricTokens.unitStyle(size, layout: .inline) == .caption)
        }
        #expect(MonarchMetricTokens.changeSize(.lg) == .md)
        #expect(MonarchMetricTokens.changeSize(.md) == .sm)
    }

    @Test("Colours: the value is default, the dash subtle, label and unit muted")
    func colours() {
        #expect(MonarchMetricTokens.valueColor(isEmpty: false) == \.textDefault)
        #expect(MonarchMetricTokens.valueColor(isEmpty: true) == \.textSubtle)
        #expect(MonarchMetricTokens.muted == \.textMuted)
    }

    @Test("Alignment maps to text alignment")
    func alignment() {
        #expect(MonarchMetricTokens.textAlignment(.leading) == .leading)
        #expect(MonarchMetricTokens.textAlignment(.trailing) == .trailing)
        #expect(MonarchMetricTokens.textAlignment(.center) == .center)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchMetricStrings.empty == "No data")
    }
}
