import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchTagFixtures: ComponentFixtures {
    static let id = "tag"
    static let fixtures = [
        Fixture("tones and sizes") {
            VStack {
                ForEach(MonarchTagSize.allCases, id: \.self) { size in
                    ForEach(MonarchTone.allCases, id: \.self) { tone in
                        MonarchTag("Design", tone: tone, size: size)
                    }
                }
            }
        },
        Fixture("removable with icon") {
            VStack {
                MonarchTag("Priority: high", systemImage: "flag", tone: .warning) {}
                MonarchTag("Small", size: .sm, removeLabel: "Remove the small tag") {}
            }
        },
        Fixture("selectable") {
            VStack {
                MonarchTag("Unread", isSelected: .constant(true))
                MonarchTag("Assigned to me", isSelected: .constant(false), tone: .info)
                MonarchTag("Selected and removable", isSelected: .constant(true), size: .sm) {}
            }
        },
        Fixture("disabled") {
            VStack {
                MonarchTag("Disabled", isSelected: .constant(true)) {}
                MonarchTag("Disabled label", tone: .success)
            }
            .disabled(true)
        },
        Fixture("long text") {
            MonarchTag("A tag whose text is long enough to wrap onto more than one line at the largest size") {}
        },
    ]
}

@Suite("MonarchTag")
struct MonarchTagTests {
    @Test("Colours follow the tone while the tag is off")
    func toneAppearance() {
        let warning = MonarchTagTokens.appearance(tone: .warning, isSelected: false, isEnabled: true)
        #expect(warning.background == \MonarchColorTokens.warningSubtle)
        #expect(warning.foreground == \MonarchColorTokens.warningStrong)
        #expect(warning.border == \MonarchColorTokens.warningBorder)

        let neutral = MonarchTagTokens.appearance(tone: .neutral, isSelected: false, isEnabled: true)
        #expect(neutral.background == \MonarchColorTokens.bgMuted)
        #expect(neutral.foreground == \MonarchColorTokens.textDefault)
        #expect(neutral.border == \MonarchColorTokens.borderDefault)
    }

    @Test("A selected tag takes the accent whatever its tone", arguments: MonarchTone.allCases)
    func selectedAppearance(tone: MonarchTone) {
        let selected = MonarchTagTokens.appearance(tone: tone, isSelected: true, isEnabled: true)
        #expect(selected.background == \MonarchColorTokens.accentSubtle)
        #expect(selected.foreground == \MonarchColorTokens.accentStrong)
        #expect(selected.border == \MonarchColorTokens.accentBorder)
    }

    @Test("A disabled tag is muted, selected or not", arguments: [true, false])
    func disabledAppearance(isSelected: Bool) {
        let disabled = MonarchTagTokens.appearance(tone: .danger, isSelected: isSelected, isEnabled: false)
        #expect(disabled.background == \MonarchColorTokens.bgMuted)
        #expect(disabled.foreground == \MonarchColorTokens.textDisabled)
        #expect(disabled.border == \MonarchColorTokens.borderSubtle)
    }

    @Test("An interactive tag has a 44-point touch target at every size", arguments: MonarchTagSize.allCases)
    func hitTarget(size: MonarchTagSize) {
        let inset = MonarchTagTokens.hitInset(size, isInteractive: true)
        #expect(MonarchTagTokens.minHeight(size) + inset * 2 == MonarchAccessibility.minimumHitTarget)
        #expect(MonarchTagTokens.hitInset(size, isInteractive: false) == 0)
    }

    @Test("The small tag is lower than the medium tag")
    func sizes() {
        #expect(MonarchTagTokens.minHeight(.sm) < MonarchTagTokens.minHeight(.md))
        #expect(MonarchTagTokens.horizontalPadding(.sm) < MonarchTagTokens.horizontalPadding(.md))
    }

    @Test("The remove button is named after the tag unless the app names it")
    func removeLabel() {
        #expect(MonarchTagTokens.removeLabel(text: "Design", override: nil) == "Remove Design")
        #expect(MonarchTagTokens.removeLabel(text: "Design", override: "") == "Remove Design")
        #expect(MonarchTagTokens.removeLabel(text: "Design", override: "Delete label") == "Delete label")
    }

    #if os(iOS)
        @Test("A removable tag is at least 44 points high and a plain tag is not")
        @MainActor
        func layoutHeight() {
            #expect(Host.layout(MonarchTag("Design") {}).height >= MonarchAccessibility.minimumHitTarget)
            #expect(Host.layout(MonarchTag("Design")).height < MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
