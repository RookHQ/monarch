import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchSignInWithRookButtonFixtures: ComponentFixtures {
    static let id = "sign-in-with-rook-button"
    static let fixtures = [
        Fixture("variants and sizes") {
            VStack(alignment: .leading) {
                ForEach(MonarchSignInWithRookButtonVariant.allCases, id: \.self) { variant in
                    ForEach(MonarchSignInWithRookButtonSize.allCases, id: \.self) { size in
                        MonarchSignInWithRookButton(variant: variant, size: size) {}
                    }
                }
            }
        },
        Fixture("with a mark") {
            VStack(alignment: .leading) {
                MonarchSignInWithRookButton(mark: Image(systemName: "seal.fill")) {}
                MonarchSignInWithRookButton(mark: Image(systemName: "seal.fill"), variant: .dark, size: .lg) {}
                MonarchSignInWithRookButton(markURL: nil, variant: .neutral, size: .sm) {}
            }
        },
        Fixture("loading") {
            VStack(alignment: .leading) {
                ForEach(MonarchSignInWithRookButtonVariant.allCases, id: \.self) { variant in
                    MonarchSignInWithRookButton(variant: variant, isLoading: true, loadingLabel: "Starting") {}
                }
            }
        },
        Fixture("full width, custom title and disabled") {
            VStack(alignment: .leading) {
                MonarchSignInWithRookButton("Continue with Rook ID", fullWidth: true) {}
                ForEach(MonarchSignInWithRookButtonVariant.allCases, id: \.self) { variant in
                    MonarchSignInWithRookButton(variant: variant) {}.disabled(true)
                }
            }
        },
    ]
}

@Suite("MonarchSignInWithRookButton")
struct MonarchSignInWithRookButtonTests {
    typealias Tokens = MonarchSignInWithRookButtonTokens

    private let themes: [MonarchTheme] = [.light, .dark, .highContrastLight, .highContrastDark]

    @Test("Light, dark and neutral keep their theme on every screen")
    func pinnedThemes() {
        for current in [MonarchTheme.light, .dark] {
            #expect(Tokens.theme(for: .light, in: current) == .light)
            #expect(Tokens.theme(for: .neutral, in: current) == .light)
            #expect(Tokens.theme(for: .dark, in: current) == .dark)
        }
    }

    @Test("Pinned variants keep the screen's contrast setting")
    func pinnedThemesKeepContrast() {
        for current in [MonarchTheme.highContrastLight, .highContrastDark] {
            #expect(Tokens.theme(for: .light, in: current) == .highContrastLight)
            #expect(Tokens.theme(for: .neutral, in: current) == .highContrastLight)
            #expect(Tokens.theme(for: .dark, in: current) == .highContrastDark)
        }
    }

    @Test("The auto variant follows the screen")
    func autoTheme() {
        for current in themes {
            #expect(Tokens.theme(for: .auto, in: current) == current)
        }
    }

    @Test("Colours at rest")
    func resting() {
        for variant in [MonarchSignInWithRookButtonVariant.light, .dark, .auto] {
            let appearance = Tokens.appearance(variant: variant, isPressed: false, isEnabled: true)
            #expect(appearance.background == \MonarchColorTokens.bgSurface)
            #expect(appearance.foreground == \MonarchColorTokens.textDefault)
            #expect(appearance.border == \MonarchColorTokens.borderStrong)
        }
        let neutral = Tokens.appearance(variant: .neutral, isPressed: false, isEnabled: true)
        #expect(neutral.background == \MonarchColorTokens.bgMuted)
        #expect(neutral.border == nil)
    }

    @Test("A pressed button changes its fill", arguments: MonarchSignInWithRookButtonVariant.allCases)
    func pressed(variant: MonarchSignInWithRookButtonVariant) {
        let resting = Tokens.appearance(variant: variant, isPressed: false, isEnabled: true)
        let pressed = Tokens.appearance(variant: variant, isPressed: true, isEnabled: true)
        #expect(pressed.background != resting.background)
        #expect(pressed.foreground == resting.foreground)
        #expect(pressed.border == resting.border)
    }

    @Test("A disabled button uses the disabled text role", arguments: MonarchSignInWithRookButtonVariant.allCases)
    func disabled(variant: MonarchSignInWithRookButtonVariant) {
        let disabled = Tokens.appearance(variant: variant, isPressed: false, isEnabled: false)
        #expect(disabled.foreground == \MonarchColorTokens.textDisabled)
        #expect(disabled == Tokens.appearance(variant: variant, isPressed: true, isEnabled: false))
    }

    @Test("Sizes are 32, 40 and 48 points high, with a mark of 16, 20 and 24 points")
    func sizes() {
        #expect(Tokens.minHeight(.sm) == 32)
        #expect(Tokens.minHeight(.md) == 40)
        #expect(Tokens.minHeight(.lg) == 48)
        #expect(Tokens.markSize(.sm) == 16)
        #expect(Tokens.markSize(.md) == 20)
        #expect(Tokens.markSize(.lg) == 24)
        #expect(Tokens.trailingPadding(.sm) < Tokens.trailingPadding(.lg))
    }

    @Test("Built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchSignInWithRookButtonStrings.title == "Sign in with Rook ID")
        #expect(MonarchSignInWithRookButtonStrings.loading == "Loading")
    }

    #if os(iOS)
        @Test("Every size keeps a 44-point touch target", arguments: MonarchSignInWithRookButtonSize.allCases)
        @MainActor
        func hitTarget(size: MonarchSignInWithRookButtonSize) {
            let button = MonarchSignInWithRookButton(size: size) {}.fixedSize()
            #expect(Host.layout(button).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
