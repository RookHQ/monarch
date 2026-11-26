import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchToggleButtonStyleFixtures: ComponentFixtures {
    static let id = "toggle-button"
    static let fixtures = [
        Fixture("variants, sizes and states") {
            VStack(alignment: .leading) {
                ForEach(MonarchToggleButtonVariant.allCases, id: \.self) { variant in
                    ForEach(MonarchButtonSize.allCases, id: \.self) { size in
                        HStack {
                            Toggle("Off", isOn: .constant(false))
                            Toggle("On", isOn: .constant(true))
                        }
                        .toggleStyle(.monarchButton(variant, size: size))
                    }
                }
            }
        },
        Fixture("disabled, icon only and long label") {
            VStack(alignment: .leading) {
                HStack {
                    Toggle("Off", isOn: .constant(false))
                    Toggle("On", isOn: .constant(true))
                }
                .disabled(true)
                Toggle("Bold", systemImage: "bold", isOn: .constant(true))
                    .labelStyle(.iconOnly)
                Toggle("Show archived conversations from every workspace", isOn: .constant(true))
            }
            .toggleStyle(.monarchButton(.outline))
        },
    ]
}

@Suite("MonarchToggleButtonStyle")
struct MonarchToggleButtonStyleTests {
    private func appearance(
        _ variant: MonarchToggleButtonVariant,
        isOn: Bool,
        isPressed: Bool = false,
        isEnabled: Bool = true
    ) -> MonarchSurfaceAppearance {
        MonarchToggleButtonTokens.appearance(variant: variant, isOn: isOn, isPressed: isPressed, isEnabled: isEnabled)
    }

    @Test("Off, the ghost variant has no fill or border and the outline variant is a bordered surface")
    func off() {
        let ghost = appearance(.ghost, isOn: false)
        #expect(ghost.background == nil)
        #expect(ghost.border == nil)
        #expect(ghost.foreground == \MonarchColorTokens.textMuted)

        let outline = appearance(.outline, isOn: false)
        #expect(outline.background == \MonarchColorTokens.bgSurface)
        #expect(outline.border == \MonarchColorTokens.borderDefault)
        #expect(outline.foreground == \MonarchColorTokens.textMuted)
    }

    @Test("On, both variants gain an accent fill and no border", arguments: MonarchToggleButtonVariant.allCases)
    func on(variant: MonarchToggleButtonVariant) {
        let on = appearance(variant, isOn: true)
        #expect(on.background == \MonarchColorTokens.accentSubtle)
        #expect(on.foreground == \MonarchColorTokens.accentStrong)
        #expect(on.border == nil)
        // On differs from off by more than the text colour: a fill.
        let off = appearance(variant, isOn: false)
        #expect(on.background != off.background)
    }

    @Test(
        "A press changes the fill and nothing else",
        arguments: MonarchToggleButtonVariant.allCases,
        [false, true]
    )
    func pressed(variant: MonarchToggleButtonVariant, isOn: Bool) {
        let resting = appearance(variant, isOn: isOn)
        let pressed = appearance(variant, isOn: isOn, isPressed: true)
        #expect(pressed.background != nil)
        #expect(pressed.background != resting.background)
        #expect(pressed.foreground == resting.foreground)
        #expect(pressed.border == resting.border)
    }

    @Test(
        "Disabled uses the disabled text role, ignores presses and still shows on and off",
        arguments: MonarchToggleButtonVariant.allCases
    )
    func disabled(variant: MonarchToggleButtonVariant) {
        let on = appearance(variant, isOn: true, isEnabled: false)
        let off = appearance(variant, isOn: false, isEnabled: false)
        #expect(on.foreground == \MonarchColorTokens.textDisabled)
        #expect(off.foreground == \MonarchColorTokens.textDisabled)
        #expect(on == appearance(variant, isOn: true, isPressed: true, isEnabled: false))
        #expect(on.background == \MonarchColorTokens.bgMuted)
        #expect(on.background != off.background)
    }

    @Test("The checkmark appears only while on and only with Differentiate Without Colour")
    func checkmark() {
        #expect(MonarchToggleButtonTokens.showsCheckmark(isOn: true, differentiateWithoutColor: true))
        #expect(!MonarchToggleButtonTokens.showsCheckmark(isOn: true, differentiateWithoutColor: false))
        #expect(!MonarchToggleButtonTokens.showsCheckmark(isOn: false, differentiateWithoutColor: true))
        #expect(!MonarchToggleButtonTokens.showsCheckmark(isOn: false, differentiateWithoutColor: false))
    }

    #if os(iOS)
        @Test("Every size keeps a 44-point touch target", arguments: MonarchButtonSize.allCases)
        @MainActor
        func hitTarget(size: MonarchButtonSize) {
            let toggle = Toggle("B", isOn: .constant(true)).toggleStyle(.monarchButton(size: size)).fixedSize()
            let layout = Host.layout(toggle)
            #expect(layout.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}

@Suite("MonarchToggleButtonStyle tone")
struct MonarchToggleButtonToneTests {
    private typealias Tokens = MonarchToggleButtonTokens

    @Test("While on, the button takes the colours of its tone", arguments: MonarchTone.allCases)
    func tone(tone: MonarchTone) {
        let palette = MonarchTonePalette.palette(for: tone)
        let on = Tokens.appearance(variant: .ghost, tone: tone, isOn: true, isPressed: false, isEnabled: true)
        #expect(on.background == palette.subtle)
        #expect(on.foreground == palette.strong)
        #expect(on.border == nil)
        let pressed = Tokens.appearance(variant: .ghost, tone: tone, isOn: true, isPressed: true, isEnabled: true)
        #expect(pressed.background != on.background)
    }

    @Test("The AI tone uses the AI roles; the default stays the accent")
    func ai() {
        let ai = Tokens.appearance(variant: .outline, tone: .ai, isOn: true, isPressed: false, isEnabled: true)
        #expect(ai.background == \MonarchColorTokens.aiSubtle)
        let accent = Tokens.appearance(variant: .outline, isOn: true, isPressed: false, isEnabled: true)
        #expect(accent.background == \MonarchColorTokens.accentSubtle)
        #expect(accent.foreground == \MonarchColorTokens.accentStrong)
    }
}
