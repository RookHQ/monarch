import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchRatingPickerFixtures: ComponentFixtures {
    static let id = "rating-picker"

    static let labelled = [
        MonarchRatingOption(1, label: "Below expectations"),
        MonarchRatingOption(2, label: "Meets expectations"),
        MonarchRatingOption(3, label: "Exceeds expectations"),
    ]

    static let fixtures = [
        Fixture("scale: no rating, rated, small") {
            VStack(alignment: .leading) {
                MonarchRatingPicker("Overall", value: .constant(nil))
                MonarchRatingPicker("Overall", value: .constant(4), isClearable: true)
                MonarchRatingPicker("Effort", value: .constant(7), in: 0...10, size: .sm)
            }
        },
        Fixture("scale: labelled options") {
            VStack(alignment: .leading) {
                MonarchRatingPicker("Performance", value: .constant(2), options: labelled)
                MonarchRatingPicker("Performance", value: .constant(3), options: labelled, size: .sm)
            }
        },
        Fixture("stars: no rating, rated, small, with caption") {
            VStack(alignment: .leading) {
                MonarchRatingPicker("Stars", value: .constant(nil), variant: .stars)
                MonarchRatingPicker("Stars", value: .constant(3), variant: .stars)
                MonarchRatingPicker("Stars", value: .constant(5), variant: .stars, size: .sm)
                MonarchRatingPicker("Performance", value: .constant(2), options: labelled, variant: .stars)
            }
        },
        Fixture("stars: ten stars wrap") {
            MonarchRatingPicker("Score", value: .constant(6), in: 1...10, variant: .stars)
        },
        Fixture("read-only and disabled") {
            VStack(alignment: .leading) {
                MonarchRatingPicker("Read-only", value: .constant(4), isReadOnly: true)
                MonarchRatingPicker("Read-only", value: .constant(4), variant: .stars, isReadOnly: true)
                MonarchRatingPicker("Disabled", value: .constant(2)).disabled(true)
                MonarchRatingPicker("Disabled", value: .constant(2), variant: .stars).disabled(true)
            }
        },
        Fixture("empty scale") {
            MonarchRatingPicker("Nothing to rate", value: .constant(nil), options: [])
        },
        Fixture("required with an error") {
            MonarchRatingPicker(
                "Overall",
                value: .constant(nil),
                variant: .stars,
                isRequired: true,
                error: "Choose a rating."
            )
        },
    ]
}

@Suite("MonarchRatingPicker")
struct MonarchRatingPickerTests {
    typealias Tokens = MonarchRatingPickerTokens

    private let five = MonarchRatingPickerTokens.options(in: 1...5)

    @Test("A range becomes its whole numbers, without labels")
    func rangeOptions() {
        #expect(five.map(\.value) == [1, 2, 3, 4, 5])
        #expect(five.allSatisfy { $0.label == nil })
        #expect(Tokens.options(in: 0...10).count == 11)
    }

    @Test("Activating a value chooses it; activating the chosen one clears only a clearable rating")
    func activated() {
        #expect(Tokens.activated(3, current: nil, isClearable: false) == 3)
        #expect(Tokens.activated(3, current: 5, isClearable: false) == 3)
        #expect(Tokens.activated(3, current: 3, isClearable: false) == 3)
        #expect(Tokens.activated(3, current: 3, isClearable: true) == nil)
        #expect(Tokens.activated(4, current: 3, isClearable: true) == 4)
    }

    @Test("Adjusting moves one step and stops at the ends")
    func adjusting() {
        #expect(Tokens.incremented(nil, options: five) == 1)
        #expect(Tokens.incremented(3, options: five) == 4)
        #expect(Tokens.incremented(5, options: five) == 5)
        #expect(Tokens.decremented(nil, options: five) == nil)
        #expect(Tokens.decremented(3, options: five) == 2)
        #expect(Tokens.decremented(1, options: five) == 1)
        #expect(Tokens.incremented(nil, options: []) == nil)
        #expect(Tokens.incremented(9, options: five) == 1)
    }

    @Test("Adjusting follows the order of the options, not their numbers")
    func adjustingCustomScale() {
        let scale = [MonarchRatingOption(10), MonarchRatingOption(20), MonarchRatingOption(50)]
        #expect(Tokens.incremented(20, options: scale) == 50)
        #expect(Tokens.decremented(20, options: scale) == 10)
    }

    @Test("Stars fill up to the chosen value")
    func filled() {
        #expect(!Tokens.isFilled(1, current: nil))
        #expect(Tokens.isFilled(1, current: 3))
        #expect(Tokens.isFilled(3, current: 3))
        #expect(!Tokens.isFilled(4, current: 3))
    }

    @Test("VoiceOver reads the value with the scale and the option's label")
    func accessibilityValue() {
        #expect(Tokens.accessibilityValue(nil, options: five) == "No rating")
        #expect(Tokens.accessibilityValue(3, options: five) == "3 of 5")
        #expect(Tokens.accessibilityValue(9, options: five) == "No rating")
        #expect(
            Tokens.accessibilityValue(2, options: MonarchRatingPickerFixtures.labelled) == "2 of 3, Meets expectations"
        )
        #expect(Tokens.accessibilityValue(nil, options: []) == "No rating")
    }

    @Test("The chosen tile uses the accent and a heavier border")
    func tileColours() {
        let chosen = Tokens.tileAppearance(isSelected: true, isPressed: false, isEnabled: true)
        #expect(chosen.background == \MonarchColorTokens.accentSubtle)
        #expect(chosen.border == \MonarchColorTokens.accentBorder)
        #expect(chosen.foreground == \MonarchColorTokens.textDefault)
        let resting = Tokens.tileAppearance(isSelected: false, isPressed: false, isEnabled: true)
        #expect(resting.background == \MonarchColorTokens.bgSurface)
        #expect(resting.border == \MonarchColorTokens.borderDefault)
        #expect(resting.foreground == \MonarchColorTokens.textMuted)
        #expect(Tokens.tileBorderWidth(isSelected: true) > Tokens.tileBorderWidth(isSelected: false))
    }

    @Test("Pressing a tile changes its fill; a disabled tile ignores presses", arguments: [true, false])
    func tileStates(isSelected: Bool) {
        let resting = Tokens.tileAppearance(isSelected: isSelected, isPressed: false, isEnabled: true)
        let pressed = Tokens.tileAppearance(isSelected: isSelected, isPressed: true, isEnabled: true)
        #expect(pressed.background != resting.background)
        let disabled = Tokens.tileAppearance(isSelected: isSelected, isPressed: false, isEnabled: false)
        #expect(disabled.foreground == \MonarchColorTokens.textDisabled)
        #expect(disabled == Tokens.tileAppearance(isSelected: isSelected, isPressed: true, isEnabled: false))
    }

    @Test("Star colours")
    func starColours() {
        #expect(Tokens.starForeground(isFilled: true, isEnabled: true) == \MonarchColorTokens.warningStrong)
        #expect(Tokens.starForeground(isFilled: false, isEnabled: true) == \MonarchColorTokens.borderStrong)
        #expect(Tokens.starForeground(isFilled: true, isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(Tokens.filledStarSymbol != Tokens.emptyStarSymbol)
    }

    @Test("Tiles are at least as wide as the touch target, and wider with labels")
    func tileWidth() {
        #expect(Tokens.tileMinWidth(hasLabels: false) == MonarchAccessibility.minimumHitTarget)
        #expect(Tokens.tileMinWidth(hasLabels: true) > Tokens.tileMinWidth(hasLabels: false))
        #expect(Tokens.starSize(.sm) < Tokens.starSize(.md))
    }

    @Test("Built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchRatingPickerStrings.value(3, of: 5) == "3 of 5")
        #expect(MonarchRatingPickerStrings.value(3, of: 5, label: "Good") == "3 of 5, Good")
        #expect(MonarchRatingPickerStrings.none == "No rating")
        #expect(MonarchRatingPickerStrings.clear == "Clear rating")
    }

    #if os(iOS)
        @Test("Stars keep a 44-point touch target", arguments: MonarchRatingPickerSize.allCases)
        @MainActor
        func hitTarget(size: MonarchRatingPickerSize) {
            let picker = MonarchRatingPicker("Stars", value: .constant(nil), variant: .stars, size: size)
            #expect(Host.layout(picker).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}

@Suite("MonarchRatingPicker required and error")
@MainActor
struct MonarchRatingPickerRequiredTests {
    @Test("The error adds a line under the scale")
    func errorLine() {
        let plain = Host.layout(MonarchRatingPicker("Overall", value: .constant(nil)))
        let flagged = Host.layout(
            MonarchRatingPicker("Overall", value: .constant(nil), isRequired: true, error: "Choose one.")
        )
        #expect(flagged.height > plain.height)
    }

    @Test("The hint reads the error and Required")
    func hint() {
        #expect(
            MonarchFieldChrome.accessibilityHint(helper: nil, error: "Choose one.", isRequired: true)
                == "Error: Choose one., Required"
        )
    }
}
