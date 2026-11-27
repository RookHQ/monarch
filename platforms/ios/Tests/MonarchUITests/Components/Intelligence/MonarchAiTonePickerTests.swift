import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiTonePickerFixtures: ComponentFixtures {
    static let id = "ai-tone-picker"
    static let fixtures = [
        Fixture("cards: default tones") {
            MonarchAiTonePicker(selection: .constant("technical"))
        },
        Fixture("cards: custom selected, with its field") {
            MonarchAiTonePicker(
                selection: .constant(MonarchAiTone.customID),
                customText: .constant("Warm but brief")
            )
        },
        Fixture("cards: nothing selected, disabled") {
            VStack {
                MonarchAiTonePicker(selection: .constant(""))
                MonarchAiTonePicker(selection: .constant("casual")).disabled(true)
            }
        },
        Fixture("cards: own tones, long text, overridden strings") {
            MonarchAiTonePicker(
                selection: .constant("legal"),
                customText: .constant(""),
                tones: [
                    MonarchAiTone(
                        id: "legal",
                        label: "Legal and compliance review",
                        detail: "Careful, exact wording that names every condition and leaves nothing implied."
                    ),
                    MonarchAiTone(id: "empty", label: "No description", detail: ""),
                ],
                label: "Voice",
                customLabel: "Your own",
                customDetail: "Write it yourself."
            )
        },
        Fixture("compact") {
            VStack(alignment: .leading) {
                MonarchAiTonePicker(selection: .constant("casual"), variant: .compact)
                MonarchAiTonePicker(
                    selection: .constant(MonarchAiTone.customID),
                    customText: .constant(""),
                    variant: .compact,
                    customFieldLabel: "Tone in your words",
                    customPrompt: "Short and direct"
                )
                MonarchAiTonePicker(selection: .constant("casual"), variant: .compact).disabled(true)
            }
        },
    ]
}

@Suite("MonarchAiTonePicker")
struct MonarchAiTonePickerTests {
    typealias Tokens = MonarchAiTonePickerTokens

    @Test("The default tones are the four built-in ones, in order, with text from the strings table")
    func defaults() {
        let tones = MonarchAiTone.defaults
        #expect(tones.map(\.id) == ["professional", "technical", "casual", "executive"])
        #expect(tones.map(\.label) == ["Professional", "Technical", "Casual", "Executive"])
        #expect(tones[0].detail == "Clear, polite and to the point.")
        #expect(tones[3].detail == "Short, decision-first, no detail.")
        #expect(!tones.map(\.id).contains(MonarchAiTone.customID))
    }

    @Test("The Custom option comes last and only when it is allowed")
    func options() {
        let tones = MonarchAiTone.defaults
        let custom = MonarchAiTone(id: MonarchAiTone.customID, label: "Custom", detail: "Yours")
        #expect(Tokens.options(tones: tones, custom: nil) == tones)
        #expect(Tokens.options(tones: tones, custom: custom) == tones + [custom])
    }

    @Test("A tone that reuses the Custom id is replaced by the Custom option, so ids stay unique")
    func uniqueIDs() {
        let clash = MonarchAiTone(id: MonarchAiTone.customID, label: "Clash", detail: "")
        let custom = MonarchAiTone(id: MonarchAiTone.customID, label: "Custom", detail: "Yours")
        let options = Tokens.options(tones: [clash] + MonarchAiTone.defaults, custom: custom)
        #expect(options.count == 5)
        #expect(Set(options.map(\.id)).count == 5)
        #expect(options.last == custom)
    }

    @Test("The custom field shows only while Custom is allowed and selected")
    func customField() {
        #expect(Tokens.showsCustomField(selection: MonarchAiTone.customID, allowsCustom: true))
        #expect(!Tokens.showsCustomField(selection: MonarchAiTone.customID, allowsCustom: false))
        #expect(!Tokens.showsCustomField(selection: "casual", allowsCustom: true))
    }

    @Test("The selected row uses the assistant roles, a filled mark and the same hairline border")
    func selected() {
        let selected = Tokens.appearance(isSelected: true, isEnabled: true)
        #expect(selected.background == \MonarchColorTokens.aiSubtle)
        #expect(selected.border == \MonarchColorTokens.aiBorder)
        #expect(selected.mark == \MonarchColorTokens.aiStrong)
        #expect(selected.label == \MonarchColorTokens.textDefault)

        let resting = Tokens.appearance(isSelected: false, isEnabled: true)
        #expect(resting.background == \MonarchColorTokens.bgSurface)
        #expect(resting.border == \MonarchColorTokens.borderDefault)
        #expect(resting.mark == \MonarchColorTokens.borderInput)
        #expect(resting.detail == \MonarchColorTokens.textMuted)

        #expect(Tokens.symbol(isSelected: true) != Tokens.symbol(isSelected: false))
        #expect(Tokens.borderWidth(isSelected: true) == Tokens.borderWidth(isSelected: false))
    }

    @Test("A disabled row uses the disabled text role and still marks the selection", arguments: [true, false])
    func disabled(isSelected: Bool) {
        let disabled = Tokens.appearance(isSelected: isSelected, isEnabled: false)
        #expect(disabled.label == \MonarchColorTokens.textDisabled)
        #expect(disabled.detail == \MonarchColorTokens.textDisabled)
        #expect(disabled.mark == \MonarchColorTokens.textDisabled)
        #expect(disabled.background == \MonarchColorTokens.bgSurface)
        #expect(disabled.border == (isSelected ? \MonarchColorTokens.borderStrong : \MonarchColorTokens.borderSubtle))
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAiTonePickerStrings.label == "Tone")
        #expect(MonarchAiTonePickerStrings.custom == "Custom")
        #expect(MonarchAiTonePickerStrings.customDetail == "Describe the tone in your own words.")
        #expect(MonarchAiTonePickerStrings.customField == "Describe the tone")
        #expect(MonarchAiTonePickerStrings.customPrompt == "e.g. Warm but brief, for a first-time customer")
        #expect(MonarchAiTonePickerStrings.technicalDetail == "Precise terms, detail and structure.")
        #expect(MonarchAiTonePickerStrings.casualDetail == "Friendly and conversational.")
    }

    @Test("Selecting Custom adds the text field below the rows")
    @MainActor
    func customFieldTakesRoom() {
        let without = Host.layout(
            MonarchAiTonePicker(selection: .constant("casual"), customText: .constant(""))
        )
        let with = Host.layout(
            MonarchAiTonePicker(selection: .constant(MonarchAiTone.customID), customText: .constant(""))
        )
        #expect(with.height > without.height + MonarchAccessibility.minimumHitTarget)
    }

    @Test("Every row is at least as tall as the touch target")
    @MainActor
    func rowHeight() {
        let tone = MonarchAiTone(id: "a", label: "A", detail: "")
        let one = Host.layout(MonarchAiTonePicker(selection: .constant("a"), tones: [tone]))
        let two = Host.layout(
            MonarchAiTonePicker(
                selection: .constant("a"),
                tones: [tone, MonarchAiTone(id: "b", label: "B", detail: "")]
            )
        )
        #expect(two.height - one.height >= MonarchAccessibility.minimumHitTarget)
    }
}
