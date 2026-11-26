import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchButtonFixtures: ComponentFixtures {
    static let id = "button"
    static let fixtures = [
        Fixture("style: variants and sizes") {
            VStack {
                ForEach(MonarchButtonVariant.allCases, id: \.self) { variant in
                    ForEach(MonarchButtonSize.allCases, id: \.self) { size in
                        Button("Continue") {}.buttonStyle(.monarch(variant, size: size))
                    }
                }
            }
        },
        Fixture("style: full width and disabled") {
            VStack {
                Button("Full width") {}.buttonStyle(.monarch(.primary, fullWidth: true))
                ForEach(MonarchButtonVariant.allCases, id: \.self) { variant in
                    Button("Disabled") {}.buttonStyle(.monarch(variant)).disabled(true)
                }
            }
        },
        Fixture("button: icons and loading") {
            VStack {
                MonarchButton("Save", systemImage: "checkmark", variant: .primary) {}
                MonarchButton("Next", trailingSystemImage: "chevron.forward", size: .lg) {}
                MonarchButton("Saving", variant: .primary, isLoading: true, loadingLabel: "Saving") {}
                MonarchButton("Delete", variant: .danger, size: .sm, fullWidth: true) {}.disabled(true)
            }
        },
        Fixture("button: icon only and the status variants") {
            HStack {
                MonarchButton("Dismiss", systemImage: "xmark", variant: .ghost, isIconOnly: true) {}
                MonarchButton("Approve", systemImage: "checkmark", variant: .success) {}
                MonarchButton("Override", variant: .warning, size: .sm) {}
                MonarchButton("Remove", variant: .dangerSubtle, size: .sm) {}
            }
        },
    ]
}

enum MonarchIconButtonFixtures: ComponentFixtures {
    static let id = "icon-button"
    static let fixtures = [
        Fixture("variants") {
            HStack {
                MonarchButton("More actions", systemImage: "ellipsis", variant: .ghost, isIconOnly: true) {}
                MonarchButton("Refresh", systemImage: "arrow.clockwise", variant: .secondary, isIconOnly: true) {}
                MonarchButton("Delete", systemImage: "trash", variant: .danger, isIconOnly: true) {}
            }
        }
    ]
}

@Suite("MonarchButton")
struct MonarchButtonTests {
    @Test("Colours at rest")
    func resting() {
        let primary = MonarchButtonAppearance.resolve(variant: .primary, isPressed: false, isEnabled: true)
        #expect(primary.background == \MonarchColorTokens.accentSolid)
        #expect(primary.foreground == \MonarchColorTokens.accentOnSolid)
        #expect(primary.border == nil)

        let secondary = MonarchButtonAppearance.resolve(variant: .secondary, isPressed: false, isEnabled: true)
        #expect(secondary.background == \MonarchColorTokens.bgSurface)
        #expect(secondary.foreground == \MonarchColorTokens.textDefault)
        #expect(secondary.border == \MonarchColorTokens.borderDefault)

        let ghost = MonarchButtonAppearance.resolve(variant: .ghost, isPressed: false, isEnabled: true)
        #expect(ghost.background == nil)
        #expect(ghost.border == nil)

        let danger = MonarchButtonAppearance.resolve(variant: .danger, isPressed: false, isEnabled: true)
        #expect(danger.background == \MonarchColorTokens.dangerSolid)
        #expect(danger.foreground == \MonarchColorTokens.dangerOnSolid)
    }

    @Test("A pressed button changes its fill", arguments: MonarchButtonVariant.allCases)
    func pressed(variant: MonarchButtonVariant) {
        let resting = MonarchButtonAppearance.resolve(variant: variant, isPressed: false, isEnabled: true)
        let pressed = MonarchButtonAppearance.resolve(variant: variant, isPressed: true, isEnabled: true)
        #expect(pressed.background != nil)
        #expect(pressed.background != resting.background)
        #expect(pressed.foreground == resting.foreground)
    }

    @Test(
        "A disabled button keeps its enabled roles (the style dims it, like the web) and ignores presses",
        arguments: MonarchButtonVariant.allCases
    )
    func disabled(variant: MonarchButtonVariant) {
        let disabled = MonarchButtonAppearance.resolve(variant: variant, isPressed: false, isEnabled: false)
        let pressed = MonarchButtonAppearance.resolve(variant: variant, isPressed: true, isEnabled: false)
        #expect(disabled == MonarchButtonAppearance.resolve(variant: variant, isPressed: false, isEnabled: true))
        #expect(disabled == pressed)
    }

    @Test("Sizes come from the control tokens")
    func tokens() {
        #expect(MonarchButtonTokens.textStyle(.sm, variant: .ghost) == .label)
        #expect(MonarchButtonTokens.textStyle(.sm, variant: .secondary) == .caption)
        #expect(MonarchButtonTokens.minHeight(.sm) == 32)
        #expect(MonarchButtonTokens.minHeight(.md) == 36)
        #expect(MonarchButtonTokens.minHeight(.lg) == 40)
        #expect(MonarchButtonTokens.horizontalPadding(.sm) < MonarchButtonTokens.horizontalPadding(.lg))
    }

    @Test("The loading value comes from the component's strings table")
    func strings() {
        #expect(MonarchButtonStrings.loading == "Loading")
    }

    #if os(iOS)
        // Layout sizes are checked on the simulator, where Dynamic Type and
        // the touch target apply.
        @Test("Every size keeps a 44-point touch target", arguments: MonarchButtonSize.allCases)
        @MainActor
        func hitTarget(size: MonarchButtonSize) {
            let button = Button("OK") {}.buttonStyle(.monarch(.primary, size: size)).fixedSize()
            #expect(Host.layout(button).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}

@Suite("MonarchButton icon only, status variants, group style")
@MainActor
struct MonarchButtonOptionTests {
    @Test("The status variants take their tone's colours")
    func statusVariants() {
        let success = MonarchButtonAppearance.resolve(variant: .success, isPressed: false, isEnabled: true)
        #expect(success.background == \MonarchColorTokens.successSolid)
        #expect(success.foreground == \MonarchColorTokens.successOnSolid)
        let warning = MonarchButtonAppearance.resolve(variant: .warning, isPressed: true, isEnabled: true)
        #expect(warning.background == \MonarchColorTokens.warningSolidActive)
        let subtle = MonarchButtonAppearance.resolve(variant: .dangerSubtle, isPressed: false, isEnabled: true)
        #expect(subtle.background == \MonarchColorTokens.dangerSubtle)
        #expect(subtle.foreground == \MonarchColorTokens.dangerStrong)
        #expect(subtle.border == \MonarchColorTokens.dangerBorder)
        let disabled = MonarchButtonAppearance.resolve(variant: .dangerSubtle, isPressed: false, isEnabled: false)
        #expect(disabled == subtle)
    }

    @Test("Icon only needs a symbol; without one the title shows")
    func iconOnlyRule() {
        #expect(MonarchButtonTokens.showsIconOnly(isIconOnly: true, systemImage: "xmark"))
        #expect(!MonarchButtonTokens.showsIconOnly(isIconOnly: true, systemImage: nil))
        #expect(!MonarchButtonTokens.showsIconOnly(isIconOnly: true, systemImage: ""))
        #expect(!MonarchButtonTokens.showsIconOnly(isIconOnly: false, systemImage: "xmark"))
        #expect(MonarchButtonTokens.horizontalPadding(.md, isIconOnly: true) == MonarchButtonTokens.verticalPadding)
    }

    @Test(
        "An icon-only button is narrower than its titled form and keeps a 44-point target",
        arguments: MonarchButtonSize.allCases
    )
    func iconOnlyTarget(size: MonarchButtonSize) {
        let icon = MonarchButton("Dismiss notification", systemImage: "xmark", size: size, isIconOnly: true) {}
        let titled = MonarchButton("Dismiss notification", systemImage: "xmark", size: size) {}
        let iconSize = Host.layout(icon.fixedSize())
        #expect(iconSize.height >= MonarchAccessibility.minimumHitTarget)
        #expect(Host.idealWidth(icon) >= MonarchAccessibility.minimumHitTarget)
        #expect(Host.idealWidth(icon) < Host.idealWidth(titled))
    }

    @Test("Inside a container that styles buttons, the container's style is used")
    func defersStyle() {
        struct Tall: ButtonStyle {
            func makeBody(configuration: Configuration) -> some View {
                configuration.label.frame(height: 120)
            }
        }
        let own = Host.layout(MonarchButton("Save") {})
        let deferred = Host.layout(MonarchButton("Save") {}.buttonStyle(Tall()).monarchButtonDefersStyle())
        #expect(own.height < 120)
        #expect(deferred.height == 120)
    }
}
