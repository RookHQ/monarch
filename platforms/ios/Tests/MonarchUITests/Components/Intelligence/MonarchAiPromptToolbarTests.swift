import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiPromptToolbarFixtures: ComponentFixtures {
    static let id = "ai-prompt-toolbar"
    static let fixtures = [
        Fixture("toggles on and off, buttons, icon only") {
            MonarchAiPromptToolbar {
                Toggle("Search the web", systemImage: "globe", isOn: .constant(true))
                Toggle("Deep research", systemImage: "books.vertical", isOn: .constant(false))
                Button("Templates", systemImage: "list.bullet") {}
                Button("Attach a file", systemImage: "paperclip") {}
                    .labelStyle(.iconOnly)
            }
        },
        Fixture("with the mode switcher, own label") {
            MonarchAiPromptToolbar(label: "Message options") {
                MonarchAiModeSwitcher(selection: .constant("ask"), showsDescription: false)
                Toggle("Search the web", isOn: .constant(false))
            }
        },
        Fixture("many controls wrap") {
            MonarchAiPromptToolbar {
                ForEach(0..<8) { index in
                    Button("Option number \(index + 1)") {}
                }
            }
        },
        Fixture("disabled") {
            MonarchAiPromptToolbar {
                Toggle("Search the web", isOn: .constant(true))
                Toggle("Deep research", isOn: .constant(false))
                Button("Templates") {}
            }
            .disabled(true)
        },
    ]
}

@Suite("MonarchAiPromptToolbar")
struct MonarchAiPromptToolbarTests {
    typealias Tokens = MonarchAiPromptToolbarTokens

    @Test("A toggle that is on has the assistant fill, border and text")
    func on() {
        let on = Tokens.toggleAppearance(isOn: true, isPressed: false, isEnabled: true)
        #expect(on.background == \MonarchColorTokens.aiSubtle)
        #expect(on.border == \MonarchColorTokens.aiBorder)
        #expect(on.foreground == \MonarchColorTokens.aiStrong)
        let pressed = Tokens.toggleAppearance(isOn: true, isPressed: true, isEnabled: true)
        #expect(pressed.background == \MonarchColorTokens.aiSubtleHover)
    }

    @Test("A toggle that is off is a quiet ghost with no border, so on differs by more than colour")
    func off() {
        let off = Tokens.toggleAppearance(isOn: false, isPressed: false, isEnabled: true)
        #expect(off.background == nil)
        #expect(off.border == nil)
        #expect(off.foreground == \MonarchColorTokens.textMuted)
        #expect(
            Tokens.toggleAppearance(isOn: false, isPressed: true, isEnabled: true).background
                == \MonarchColorTokens.bgEmphasis
        )
    }

    @Test("A disabled toggle uses the disabled text role and keeps a border when on", arguments: [true, false])
    func disabled(isOn: Bool) {
        let appearance = Tokens.toggleAppearance(isOn: isOn, isPressed: false, isEnabled: false)
        #expect(appearance.foreground == \MonarchColorTokens.textDisabled)
        #expect((appearance.border != nil) == isOn)
    }

    @Test("The check mark shows only for an on toggle with differentiation without colour")
    func checkmark() {
        #expect(Tokens.showsCheckmark(isOn: true, differentiateWithoutColor: true))
        #expect(!Tokens.showsCheckmark(isOn: true, differentiateWithoutColor: false))
        #expect(!Tokens.showsCheckmark(isOn: false, differentiateWithoutColor: true))
    }

    @Test("Controls are small ghost buttons")
    func controls() {
        #expect(Tokens.controlSize == .sm)
        #expect(Tokens.buttonVariant == .ghost)
    }

    @Test("The built-in label comes from the strings table")
    func strings() {
        #expect(MonarchAiPromptToolbarStrings.label == "Prompt options")
    }

    @Test("A row that does not fit wraps onto more lines")
    @MainActor
    func wraps() {
        let one = Host.layout(MonarchAiPromptToolbar { Button("Templates") {} })
        let many = Host.layout(
            MonarchAiPromptToolbar {
                ForEach(0..<10) { index in
                    Button("Option number \(index + 1)") {}
                }
            }
        )
        #expect(one.height >= MonarchAccessibility.minimumHitTarget)
        #expect(many.height > one.height)
    }
}
