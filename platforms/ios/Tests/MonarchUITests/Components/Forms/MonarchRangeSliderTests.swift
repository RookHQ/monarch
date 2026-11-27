import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchRangeSliderFixtures: ComponentFixtures {
    static let id = "range-slider"
    static let fixtures = [
        Fixture("md with the shown range") {
            MonarchRangeSlider("Price", value: .constant(20...80), showsValue: true)
        },
        Fixture("sm") {
            MonarchRangeSlider("Hours", value: .constant(9...17), in: 0...24, size: .sm)
        },
        Fixture("thumbs together") {
            MonarchRangeSlider("Budget", value: .constant(50...50), showsValue: true)
        },
        Fixture("at the bounds") {
            MonarchRangeSlider("Age", value: .constant(18...99), in: 18...99)
        },
        Fixture("invalid") {
            MonarchRangeSlider("Budget", value: .constant(10...90), isInvalid: true)
        },
        Fixture("disabled") {
            MonarchRangeSlider("Price", value: .constant(10...40), showsValue: true).disabled(true)
        },
        Fixture("formatted values") {
            MonarchRangeSlider(
                "Salary",
                value: .constant(40_000...90_000),
                in: 0...200_000,
                step: 5_000,
                valueText: { $0.formatted(.currency(code: "EUR").precision(.fractionLength(0))) },
                showsValue: true
            )
        },
    ]
}

@Suite("MonarchRangeSlider")
struct MonarchRangeSliderTests {
    private typealias Model = MonarchRangeSliderModel
    private let rules = MonarchRangeSliderModel.Rules(bounds: 0...100, step: 5, minimumStepsBetween: 0)

    @Test("Values snap to the step and stay in the bounds")
    func snap() {
        #expect(Model.snap(12, rules: rules) == 10)
        #expect(Model.snap(13, rules: rules) == 15)
        #expect(Model.snap(-20, rules: rules) == 0)
        #expect(Model.snap(140, rules: rules) == 100)
        let decimal = Model.Rules(bounds: 0...1, step: 0.1, minimumStepsBetween: 0)
        #expect(Model.snap(0.3, rules: decimal) == 0.3)
        let continuous = Model.Rules(bounds: 0...1, step: 0, minimumStepsBetween: 0)
        #expect(Model.snap(0.123, rules: continuous) == 0.123)
    }

    @Test("A thumb stops at the other thumb, plus the gap")
    func neverCross() {
        #expect(Model.moving(.lower, to: 90, in: 20...60, rules: rules) == 60...60)
        #expect(Model.moving(.upper, to: 5, in: 20...60, rules: rules) == 20...20)
        let gapped = Model.Rules(bounds: 0...100, step: 5, minimumStepsBetween: 2)
        #expect(gapped.gap == 10)
        #expect(Model.moving(.lower, to: 90, in: 20...60, rules: gapped) == 50...60)
        #expect(Model.moving(.upper, to: 0, in: 20...60, rules: gapped) == 20...30)
        #expect(Model.moving(.lower, to: 31, in: 20...60, rules: rules) == 30...60)
    }

    @Test("Each thumb's limits reflect the other thumb")
    func limits() {
        #expect(Model.limits(of: .lower, in: 20...60, rules: rules) == 0...60)
        #expect(Model.limits(of: .upper, in: 20...60, rules: rules) == 20...100)
    }

    @Test("VoiceOver moves a thumb one step and stops at the other thumb")
    func adjusting() {
        #expect(Model.adjusting(.lower, increment: true, in: 20...60, rules: rules) == 25...60)
        #expect(Model.adjusting(.upper, increment: false, in: 20...60, rules: rules) == 20...55)
        #expect(Model.adjusting(.lower, increment: true, in: 60...60, rules: rules) == 60...60)
        #expect(Model.adjusting(.upper, increment: true, in: 20...100, rules: rules) == 20...100)
    }

    @Test("A touch moves the nearer thumb; at the top it picks the lower one")
    func nearestThumb() {
        #expect(Model.nearestThumb(to: 10, in: 20...60, rules: rules) == .lower)
        #expect(Model.nearestThumb(to: 70, in: 20...60, rules: rules) == .upper)
        #expect(Model.nearestThumb(to: 35, in: 20...60, rules: rules) == .lower)
        #expect(Model.nearestThumb(to: 45, in: 20...60, rules: rules) == .upper)
        #expect(Model.nearestThumb(to: 100, in: 100...100, rules: rules) == .lower)
    }

    @Test("A range is normalised: snapped, ordered and gapped")
    func normalized() {
        #expect(Model.normalized(12...88, rules: rules) == 10...90)
        let gapped = Model.Rules(bounds: 0...100, step: 5, minimumStepsBetween: 2)
        #expect(Model.normalized(50...50, rules: gapped) == 50...60)
        #expect(Model.normalized(100...100, rules: gapped) == 100...100)
    }

    @Test("Positions follow the reading direction and invert to the same fraction")
    func positions() {
        #expect(Model.position(ofFraction: 0, width: 220, thumbDiameter: 20, isRightToLeft: false) == 10)
        #expect(Model.position(ofFraction: 1, width: 220, thumbDiameter: 20, isRightToLeft: false) == 210)
        #expect(Model.position(ofFraction: 0, width: 220, thumbDiameter: 20, isRightToLeft: true) == 210)
        #expect(Model.position(ofFraction: 0.25, width: 220, thumbDiameter: 20, isRightToLeft: true) == 160)
        for isRightToLeft in [false, true] {
            let x = Model.position(ofFraction: 0.4, width: 220, thumbDiameter: 20, isRightToLeft: isRightToLeft)
            let back = Model.fraction(atX: x, width: 220, thumbDiameter: 20, isRightToLeft: isRightToLeft)
            #expect(abs(back - 0.4) < 1e-9)
        }
        #expect(Model.fraction(atX: -50, width: 220, thumbDiameter: 20, isRightToLeft: false) == 0)
        #expect(Model.fraction(of: 50, rules: Model.Rules(bounds: 5...5, step: 1, minimumStepsBetween: 0)) == 0)
    }

    @Test("Colours for each state")
    func appearance() {
        let normal = MonarchRangeSliderTokens.appearance(isInvalid: false, isEnabled: true)
        #expect(normal.range == \MonarchColorTokens.accentSolid)
        #expect(normal.track == \MonarchColorTokens.bgEmphasis)
        #expect(normal.thumbFill == \MonarchColorTokens.bgSurface)
        let invalid = MonarchRangeSliderTokens.appearance(isInvalid: true, isEnabled: true)
        #expect(invalid.range == \MonarchColorTokens.dangerSolid)
        #expect(invalid.thumbBorder == \MonarchColorTokens.dangerSolid)
        let disabled = MonarchRangeSliderTokens.appearance(isInvalid: true, isEnabled: false)
        #expect(disabled.range == \MonarchColorTokens.textDisabled)
        #expect(disabled.value == \MonarchColorTokens.textDisabled)
    }

    @Test("Thumbs are named by the label and Minimum or Maximum")
    func accessibility() {
        let labels = MonarchRangeSliderLabels()
        #expect(MonarchRangeSliderTokens.thumbLabel(.lower, label: "Price", labels: labels) == "Price, Minimum")
        #expect(MonarchRangeSliderTokens.thumbLabel(.upper, label: "Price", labels: labels) == "Price, Maximum")
        let custom = MonarchRangeSliderLabels(minimum: "From", maximum: "To", range: { "\($0) to \($1)" })
        #expect(MonarchRangeSliderTokens.thumbLabel(.lower, label: "Price", labels: custom) == "Price, From")
        #expect(MonarchRangeSliderTokens.rangeText(lower: "1", upper: "2", labels: custom) == "1 to 2")
        #expect(MonarchRangeSliderTokens.rangeText(lower: "1", upper: "2", labels: labels) == "1 – 2")
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchRangeSliderStrings.minimum == "Minimum")
        #expect(MonarchRangeSliderStrings.maximum == "Maximum")
    }

    @Test("The touch area is at least the minimum target")
    func hitTarget() {
        #expect(MonarchRangeSliderTokens.hitTarget >= MonarchAccessibility.minimumHitTarget)
        for size in MonarchRangeSliderSize.allCases {
            #expect(MonarchRangeSliderTokens.thumbDiameter(size) < MonarchRangeSliderTokens.hitTarget)
        }
    }
}
