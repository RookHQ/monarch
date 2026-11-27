import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private struct ResizablePanelsFixture: View {
    var fraction = 0.4
    var axis: MonarchResizablePanelsAxis = .horizontal
    var showsGrip = false
    var stacksWhenCompact = false

    var body: some View {
        MonarchResizablePanels(
            fraction: .constant(fraction),
            axis: axis,
            showsGrip: showsGrip,
            stacksWhenCompact: stacksWhenCompact,
            first: { Text("Message list").frame(maxWidth: .infinity, maxHeight: .infinity) },
            second: { Text("The selected message, which is longer").frame(maxWidth: .infinity, maxHeight: .infinity) }
        )
        .frame(height: MonarchTokens.Space.space24 * 2)
    }
}

enum MonarchResizablePanelsFixtures: ComponentFixtures {
    static let id = "resizable-panels"
    static let fixtures = [
        Fixture("horizontal") {
            ResizablePanelsFixture()
        },
        Fixture("vertical with a grip") {
            ResizablePanelsFixture(axis: .vertical, showsGrip: true)
        },
        Fixture("horizontal with a grip, at the ends of the range") {
            VStack {
                ResizablePanelsFixture(fraction: 0, showsGrip: true)
                ResizablePanelsFixture(fraction: 4, showsGrip: true)
            }
        },
        Fixture("disabled") {
            ResizablePanelsFixture(showsGrip: true).disabled(true)
        },
        Fixture("stacked when compact") {
            MonarchResizablePanels(
                fraction: .constant(0.5),
                first: { Text("Message list") },
                second: { Text("The selected message") }
            )
            .environment(\.horizontalSizeClass, .compact)
        },
    ]
}

@Suite("MonarchResizablePanels")
struct MonarchResizablePanelsTests {
    private typealias Tokens = MonarchResizablePanelsTokens
    private let range = 0.2...0.8
    private let tolerance = 1e-9

    @Test("A fraction is kept inside the range")
    func clamping() {
        #expect(Tokens.clamped(0.5, to: range) == 0.5)
        #expect(Tokens.clamped(0, to: range) == 0.2)
        #expect(Tokens.clamped(3, to: range) == 0.8)
        #expect(Tokens.clamped(.nan, to: range) == 0.2)
    }

    @Test("A range is kept inside 0 to 1")
    func normalizedRange() {
        #expect(Tokens.normalized(0.1...0.9) == 0.1...0.9)
        #expect(Tokens.normalized(-1...2) == 0...1)
        #expect(Tokens.normalized(1.5...3) == 1...1)
    }

    @Test("A drag moves the separator by the share of the length it covers")
    func drag() {
        let moved = Tokens.fraction(start: 0.4, translation: 100, total: 1000, isMirrored: false, range: range)
        #expect(abs(moved - 0.5) < tolerance)
        let back = Tokens.fraction(start: 0.4, translation: -100, total: 1000, isMirrored: false, range: range)
        #expect(abs(back - 0.3) < tolerance)
    }

    @Test("In a right-to-left layout a drag to the right makes the first panel smaller")
    func dragMirrored() {
        let moved = Tokens.fraction(start: 0.4, translation: 100, total: 1000, isMirrored: true, range: range)
        #expect(abs(moved - 0.3) < tolerance)
    }

    @Test("A drag stops at the ends of the range and survives a zero length")
    func dragLimits() {
        #expect(Tokens.fraction(start: 0.4, translation: 900, total: 1000, isMirrored: false, range: range) == 0.8)
        #expect(Tokens.fraction(start: 0.4, translation: -900, total: 1000, isMirrored: false, range: range) == 0.2)
        #expect(Tokens.fraction(start: 0.4, translation: 50, total: 0, isMirrored: false, range: range) == 0.4)
    }

    @Test("Steps move by the step, stop at the ends, and jump to the ends")
    func steps() {
        #expect(abs(Tokens.fraction(0.4, applying: .increase, step: 0.05, range: range) - 0.45) < tolerance)
        #expect(abs(Tokens.fraction(0.4, applying: .decrease, step: 0.05, range: range) - 0.35) < tolerance)
        #expect(abs(Tokens.fraction(0.4, applying: .increase, step: -0.05, range: range) - 0.45) < tolerance)
        #expect(Tokens.fraction(0.78, applying: .increase, step: 0.05, range: range) == 0.8)
        #expect(Tokens.fraction(0.21, applying: .decrease, step: 0.05, range: range) == 0.2)
        #expect(Tokens.fraction(0.4, applying: .minimum, step: 0.05, range: range) == 0.2)
        #expect(Tokens.fraction(0.4, applying: .maximum, step: 0.05, range: range) == 0.8)
    }

    @Test("Arrow keys along the axis move the separator the way they point")
    func keys() {
        #expect(Tokens.move(for: .leftArrow, axis: .horizontal, isRightToLeft: false) == .decrease)
        #expect(Tokens.move(for: .rightArrow, axis: .horizontal, isRightToLeft: false) == .increase)
        #expect(Tokens.move(for: .leftArrow, axis: .horizontal, isRightToLeft: true) == .increase)
        #expect(Tokens.move(for: .rightArrow, axis: .horizontal, isRightToLeft: true) == .decrease)
        #expect(Tokens.move(for: .upArrow, axis: .vertical, isRightToLeft: true) == .decrease)
        #expect(Tokens.move(for: .downArrow, axis: .vertical, isRightToLeft: false) == .increase)
    }

    @Test("Arrow keys across the axis and other keys are left alone; Home and End go to the ends")
    func otherKeys() {
        #expect(Tokens.move(for: .upArrow, axis: .horizontal, isRightToLeft: false) == nil)
        #expect(Tokens.move(for: .leftArrow, axis: .vertical, isRightToLeft: false) == nil)
        #expect(Tokens.move(for: .space, axis: .horizontal, isRightToLeft: false) == nil)
        for axis in MonarchResizablePanelsAxis.allCases {
            #expect(Tokens.move(for: .home, axis: axis, isRightToLeft: false) == .minimum)
            #expect(Tokens.move(for: .end, axis: axis, isRightToLeft: true) == .maximum)
        }
    }

    @Test("Only side-by-side panels stack, and only when asked to at a compact width")
    func stacking() {
        #expect(Tokens.isStacked(axis: .horizontal, stacksWhenCompact: true, isCompact: true))
        #expect(!Tokens.isStacked(axis: .horizontal, stacksWhenCompact: true, isCompact: false))
        #expect(!Tokens.isStacked(axis: .horizontal, stacksWhenCompact: false, isCompact: true))
        #expect(!Tokens.isStacked(axis: .vertical, stacksWhenCompact: true, isCompact: true))
    }

    @Test("The first panel gives up half the separator and is never negative")
    func firstLength() {
        #expect(Tokens.firstLength(fraction: 0.5, total: 400) == 200 - Tokens.lineThickness / 2)
        #expect(Tokens.firstLength(fraction: 0, total: 400) == 0)
    }

    @Test("The separator is accented while active and subdued while disabled")
    func handleColor() {
        #expect(Tokens.handleColor(isActive: false, isEnabled: true) == \MonarchColorTokens.borderDefault)
        #expect(Tokens.handleColor(isActive: true, isEnabled: true) == \MonarchColorTokens.accentStrong)
        #expect(Tokens.handleColor(isActive: true, isEnabled: false) == \MonarchColorTokens.borderSubtle)
        #expect(Tokens.activeLineThickness > Tokens.lineThickness)
    }

    @Test("The spoken value is the share of the first panel as a whole percentage")
    func accessibilityValue() {
        #expect(Tokens.accessibilityValue(fraction: 0.4) == 0.4.formatted(.percent.precision(.fractionLength(0))))
        #expect(Tokens.accessibilityValue(fraction: 0.4).contains("40"))
        #expect(Tokens.accessibilityValue(fraction: 0.4) != Tokens.accessibilityValue(fraction: 0.45))
    }

    @Test("The separator's name comes from the component's strings table")
    func strings() {
        #expect(MonarchResizablePanelsStrings.handle == "Resize panels")
    }
}
