import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchCircularProgressViewStyleFixtures: ComponentFixtures {
    static let id = "progress-circle"
    static let fixtures = [
        Fixture("determinate, every size, with value") {
            VStack {
                ForEach(MonarchCircularProgressSize.allCases, id: \.self) { size in
                    ProgressView("Uploading report.pdf", value: 0.42)
                        .progressViewStyle(.monarchCircular(size: size, showsValue: true))
                }
            }
        },
        Fixture("tones, complete, empty and over the total") {
            VStack {
                ForEach(MonarchTone.allCases, id: \.self) { tone in
                    ProgressView(value: 0.6).progressViewStyle(.monarchCircular(tone: tone))
                }
                ProgressView(value: 1).progressViewStyle(.monarchCircular)
                ProgressView(value: 0).progressViewStyle(.monarchCircular)
                ProgressView(value: 3, total: 2) {
                    Text("Over")
                } currentValueLabel: {
                    Text("3 of 2")
                }
                .progressViewStyle(.monarchCircular(size: .lg, showsValue: true))
            }
        },
        Fixture("indeterminate") {
            VStack {
                ProgressView("Working").progressViewStyle(.monarchCircular)
                ProgressView().progressViewStyle(.monarchCircular(size: .lg, showsValue: true))
            }
        },
    ]
}

@Suite("MonarchCircularProgressViewStyle")
struct MonarchCircularProgressViewStyleTests {
    private let english = Locale(identifier: "en_US")

    @Test("The arc is the accent without a tone and the solid colour of a tone with one")
    func fill() {
        #expect(MonarchCircularProgressTokens.fill(tone: nil) == \MonarchColorTokens.accentSolid)
        #expect(MonarchCircularProgressTokens.fill(tone: .success) == \MonarchColorTokens.successSolid)
        #expect(MonarchCircularProgressTokens.fill(tone: .warning) == \MonarchColorTokens.warningSolid)
        #expect(MonarchCircularProgressTokens.fill(tone: .danger) == \MonarchColorTokens.dangerSolid)
        #expect(MonarchCircularProgressTokens.fill(tone: .neutral) == \MonarchColorTokens.bgInverse)
        #expect(MonarchCircularProgressTokens.track == \MonarchColorTokens.bgMuted)
    }

    @Test("Fractions are limited to 0 through 1")
    func clamped() {
        #expect(MonarchCircularProgressTokens.clamped(-1) == 0)
        #expect(MonarchCircularProgressTokens.clamped(0.42) == 0.42)
        #expect(MonarchCircularProgressTokens.clamped(1.5) == 1)
        #expect(MonarchCircularProgressTokens.clamped(.nan) == 0)
    }

    @Test("The value is a whole percentage, and empty for indeterminate progress")
    func valueText() {
        #expect(MonarchCircularProgressTokens.valueText(0.42, locale: english) == "42%")
        #expect(MonarchCircularProgressTokens.valueText(1.5, locale: english) == "100%")
        #expect(MonarchCircularProgressTokens.valueText(0, locale: english) == "0%")
        #expect(MonarchCircularProgressTokens.valueText(nil, locale: english).isEmpty)
    }

    @Test("The value is drawn only on the large ring, when asked for, with a known fraction")
    func showsValue() {
        #expect(MonarchCircularProgressTokens.showsValue(true, size: .lg, fraction: 0.4))
        #expect(!MonarchCircularProgressTokens.showsValue(false, size: .lg, fraction: 0.4))
        #expect(!MonarchCircularProgressTokens.showsValue(true, size: .md, fraction: 0.4))
        #expect(!MonarchCircularProgressTokens.showsValue(true, size: .sm, fraction: 0.4))
        #expect(!MonarchCircularProgressTokens.showsValue(true, size: .lg, fraction: nil))
    }

    @Test("Rings and their strokes grow from sm to lg")
    func sizes() {
        #expect(MonarchCircularProgressTokens.diameter(.sm) < MonarchCircularProgressTokens.diameter(.md))
        #expect(MonarchCircularProgressTokens.diameter(.md) < MonarchCircularProgressTokens.diameter(.lg))
        #expect(MonarchCircularProgressTokens.lineWidth(.sm) < MonarchCircularProgressTokens.lineWidth(.lg))
    }

    @Test("The indeterminate arc is a quarter of the ring and uses the shimmer duration token")
    func indeterminate() {
        #expect(MonarchCircularProgressTokens.indeterminateArc == 0.25)
        #expect(MonarchCircularProgressTokens.indeterminateDuration == 1.6)
    }

    @Test("A ring without a label is as high as its diameter", arguments: MonarchCircularProgressSize.allCases)
    @MainActor
    func ringSize(size: MonarchCircularProgressSize) {
        let ring = ProgressView(value: 0.5)
            .progressViewStyle(.monarchCircular(size: size))
            .environment(\.dynamicTypeSize, .large)
        let measured = Host.layout(ring.fixedSize())
        #expect(measured.height == MonarchCircularProgressTokens.diameter(size))
    }
}
