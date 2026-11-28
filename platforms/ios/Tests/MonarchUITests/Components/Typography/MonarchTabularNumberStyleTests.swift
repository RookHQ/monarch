import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

#if canImport(AppKit)
    import AppKit
#elseif canImport(UIKit)
    import UIKit
#endif

enum MonarchTabularNumberStyleFixtures: ComponentFixtures {
    static let id = "tabular-number"
    static let fixtures = [
        Fixture("a column of figures") {
            VStack(alignment: .trailing) {
                Text(monarchTabular: 1_111.11, format: .number.precision(.fractionLength(2)))
                Text(monarchTabular: 9_048.5, format: .number.precision(.fractionLength(2)))
                Text(monarchTabular: 20, format: .number.precision(.fractionLength(2)))
            }
            .monarchTextStyle(.body)
        },
        Fixture("percent, currency and whole numbers") {
            VStack(alignment: .trailing) {
                Text(monarchTabular: 0.125, format: .percent)
                Text(monarchTabular: Decimal(1_299.5), format: .currency(code: "EUR"))
                Text(monarchTabular: 1_234_567, format: .number)
            }
            .monarchTextStyle(.body)
        },
        Fixture("missing values") {
            VStack(alignment: .trailing) {
                Text(monarchTabular: Double?.none, format: .number)
                Text(monarchTabular: Double.nan, format: .number)
                Text(monarchTabular: Int?.none, format: .number, emptyText: "n/a", emptyLabel: "Not available")
            }
            .monarchTextStyle(.body)
        },
        Fixture("the modifier on formatted text and on a container") {
            VStack(alignment: .leading) {
                Text("11:11").monarchTextStyle(.title).monarchTabularNumber()
                HStack {
                    Text("Ref. 0042")
                    Text("x 12")
                }
                .monarchTextStyle(.caption)
                .monarchTabularNumber()
            }
        },
        Fixture("a long number in a large role") {
            Text(monarchTabular: 123_456_789_012.345, format: .number.precision(.fractionLength(3)))
                .monarchTextStyle(.display)
        },
    ]
}

@Suite("MonarchTabularNumberStyle")
struct MonarchTabularNumberStyleTests {
    typealias Tokens = MonarchTabularNumberTokens

    private let english = Locale(identifier: "en_US")
    private let german = Locale(identifier: "de_DE")

    @Test("A value is missing when it is nil or not finite")
    func missing() {
        #expect(Tokens.isMissing(Double?.none))
        #expect(Tokens.isMissing(Int?.none))
        #expect(Tokens.isMissing(Double.nan))
        #expect(Tokens.isMissing(Double.infinity))
        #expect(Tokens.isMissing(-Double.infinity))
        #expect(Tokens.isMissing(Float.nan))
        #expect(Tokens.isMissing(Decimal.nan))

        #expect(!Tokens.isMissing(0.0))
        #expect(!Tokens.isMissing(-12.5))
        #expect(!Tokens.isMissing(0))
        #expect(!Tokens.isMissing(Int.max))
        #expect(!Tokens.isMissing(Decimal(0)))
    }

    @Test("A number is written by the format style, in its locale")
    func formatted() {
        let twoPlaces = FloatingPointFormatStyle<Double>.number.precision(.fractionLength(2))
        #expect(Tokens.formatted(1234.5, format: twoPlaces.locale(english)) == "1,234.50")
        #expect(Tokens.formatted(1234.5, format: twoPlaces.locale(german)) == "1.234,50")
        #expect(Tokens.formatted(0, format: twoPlaces.locale(english)) == "0.00")
        #expect(Tokens.formatted(-7, format: IntegerFormatStyle<Int>.number.locale(english)) == "-7")
        #expect(
            Tokens.formatted(0.125, format: FloatingPointFormatStyle<Double>.Percent.percent.locale(english)) == "12.5%"
        )
    }

    @Test("A missing value has no formatted text")
    func formattedMissing() {
        let number = FloatingPointFormatStyle<Double>.number.locale(english)
        #expect(Tokens.formatted(nil, format: number) == nil)
        #expect(Tokens.formatted(.nan, format: number) == nil)
        #expect(Tokens.formatted(.infinity, format: number) == nil)
        #expect(Tokens.formatted(nil, format: IntegerFormatStyle<Int>.number) == nil)
    }

    @Test("The missing-value strings resolve and can be replaced")
    func strings() {
        #expect(Tokens.emptyText(nil) == "—")
        #expect(Tokens.emptyLabel(nil) == "No value")
        #expect(!MonarchTabularNumberStrings.empty.hasPrefix("monarch."))
        #expect(!MonarchTabularNumberStrings.emptyLabel.hasPrefix("monarch."))
        #expect(Tokens.emptyText("n/a") == "n/a")
        #expect(Tokens.emptyLabel("Nicht verfügbar") == "Nicht verfügbar")
    }

    @MainActor
    @Test("Digits have one width with the style and different widths without it")
    func digitsHaveOneWidth() {
        let ones = String(repeating: "1", count: 12)
        let zeros = String(repeating: "0", count: 12)

        let plainOnes = TabularNumberMeasure.idealWidth(Text(ones).monarchTextStyle(.body))
        let plainZeros = TabularNumberMeasure.idealWidth(Text(zeros).monarchTextStyle(.body))
        #expect(plainOnes > 0)
        #expect(plainZeros - plainOnes > 1)

        let styledOnes = TabularNumberMeasure.idealWidth(Text(ones).monarchTextStyle(.body).monarchTabularNumber())
        let styledZeros = TabularNumberMeasure.idealWidth(Text(zeros).monarchTextStyle(.body).monarchTabularNumber())
        #expect(abs(styledOnes - styledZeros) < 0.5)
    }

    @MainActor
    @Test("A formatted number from the Text initialiser has fixed-width digits")
    func initialiserUsesFixedWidthDigits() {
        let whole = IntegerFormatStyle<Int>.number.grouping(.never)
        let ones = TabularNumberMeasure.idealWidth(
            Text(monarchTabular: 111_111_111_111, format: whole).monarchTextStyle(.body)
        )
        let zeros = TabularNumberMeasure.idealWidth(
            Text(monarchTabular: 100_000_000_000, format: whole).monarchTextStyle(.body)
        )
        #expect(ones > 0)
        #expect(abs(ones - zeros) < 0.5)
    }

    @MainActor
    @Test("A missing value draws the dash, which takes room")
    func missingValueIsDrawn() {
        let size = Host.layout(Text(monarchTabular: Double?.none, format: .number).monarchTextStyle(.body))
        #expect(size.height > 0)
    }
}

/// Measures the width a view asks for when nothing constrains it.
@MainActor
private enum TabularNumberMeasure {
    static func idealWidth(_ view: some View) -> CGFloat {
        let content = view.fixedSize()
        #if canImport(AppKit)
            return NSHostingView(rootView: content).fittingSize.width
        #elseif canImport(UIKit)
            let unbounded = CGSize(width: CGFloat.greatestFiniteMagnitude, height: .greatestFiniteMagnitude)
            return UIHostingController(rootView: content).sizeThatFits(in: unbounded).width
        #endif
    }
}
