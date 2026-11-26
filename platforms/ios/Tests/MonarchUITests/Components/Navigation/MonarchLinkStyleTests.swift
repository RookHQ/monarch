import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchLinkStyleFixtures: ComponentFixtures {
    static let id = "link"
    static let fixtures = [
        Fixture("variants on a Button") {
            VStack(alignment: .leading) {
                ForEach(MonarchLinkVariant.allCases, id: \.self) { variant in
                    Button("Billing settings") {}.buttonStyle(.monarchLink(variant))
                }
            }
            .monarchTextStyle(.body)
        },
        Fixture("a SwiftUI Link, external") {
            if let url = URL(string: "https://example.com/terms") {
                VStack(alignment: .leading) {
                    Link("Terms of service", destination: url)
                        .buttonStyle(.monarchLink(isExternal: true))
                    Link("Privacy policy", destination: url)
                        .buttonStyle(.monarchLink(.standalone, isExternal: true, externalLabel: "opens in Safari"))
                }
                .monarchTextStyle(.body)
            }
        },
        Fixture("disabled") {
            VStack(alignment: .leading) {
                ForEach(MonarchLinkVariant.allCases, id: \.self) { variant in
                    Button("Billing settings") {}.buttonStyle(.monarchLink(variant, isExternal: true)).disabled(true)
                }
            }
        },
        Fixture("long title at caption size") {
            Button("Read how Monarch stores the invoices of an organisation that has been closed") {}
                .buttonStyle(.monarchLink(.subtle, isExternal: true))
                .monarchTextStyle(.caption)
        },
    ]
}

@Suite("MonarchLinkStyle")
struct MonarchLinkStyleTests {
    private func appearance(
        _ variant: MonarchLinkVariant,
        isPressed: Bool = false,
        isEnabled: Bool = true,
        showsButtonShapes: Bool = false
    ) -> MonarchLinkAppearance {
        MonarchLinkTokens.appearance(
            variant: variant,
            isPressed: isPressed,
            isEnabled: isEnabled,
            showsButtonShapes: showsButtonShapes
        )
    }

    @Test("Colours at rest")
    func resting() {
        #expect(appearance(.inline).foreground == \MonarchColorTokens.textLink)
        #expect(appearance(.standalone).foreground == \MonarchColorTokens.textLink)
        #expect(appearance(.subtle).foreground == \MonarchColorTokens.textMuted)
    }

    @Test("Only the inline link is underlined at rest")
    func restingUnderline() {
        #expect(appearance(.inline).isUnderlined)
        #expect(!appearance(.standalone).isUnderlined)
        #expect(!appearance(.subtle).isUnderlined)
    }

    @Test("A pressed link is underlined, and a subtle one takes the default text colour")
    func pressed() {
        for variant in MonarchLinkVariant.allCases {
            #expect(appearance(variant, isPressed: true).isUnderlined)
        }
        #expect(appearance(.subtle, isPressed: true).foreground == \MonarchColorTokens.textDefault)
        #expect(appearance(.inline, isPressed: true).foreground == \MonarchColorTokens.textLink)
    }

    @Test("Button Shapes underlines every variant, enabled or not", arguments: MonarchLinkVariant.allCases)
    func buttonShapes(variant: MonarchLinkVariant) {
        #expect(appearance(variant, showsButtonShapes: true).isUnderlined)
        #expect(appearance(variant, isEnabled: false, showsButtonShapes: true).isUnderlined)
    }

    @Test("A disabled link uses the disabled text role and ignores presses", arguments: MonarchLinkVariant.allCases)
    func disabled(variant: MonarchLinkVariant) {
        let disabled = appearance(variant, isEnabled: false)
        #expect(disabled.foreground == \MonarchColorTokens.textDisabled)
        #expect(disabled == appearance(variant, isPressed: true, isEnabled: false))
        #expect(disabled.isUnderlined == (variant == .inline))
    }

    @Test("Only the standalone link sets a weight")
    func weight() {
        #expect(MonarchLinkTokens.fontWeight(.inline) == nil)
        #expect(MonarchLinkTokens.fontWeight(.subtle) == nil)
        #expect(MonarchLinkTokens.fontWeight(.standalone) == .medium)
    }

    @Test("The external note comes from the component's strings table")
    func strings() {
        #expect(MonarchLinkStrings.external == "opens in the browser")
    }

    #if os(iOS)
        @Test("Every variant keeps a 44-point touch target", arguments: MonarchLinkVariant.allCases)
        @MainActor
        func hitTarget(variant: MonarchLinkVariant) {
            let link = Button("OK") {}.buttonStyle(.monarchLink(variant)).fixedSize()
            #expect(Host.layout(link).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
