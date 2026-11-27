import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchInfoTooltipFixtures: ComponentFixtures {
    static let id = "info-tooltip"
    static let fixtures = [
        Fixture("both icons beside a label") {
            VStack(alignment: .leading) {
                ForEach(MonarchInfoTooltipIcon.allCases, id: \.self) { icon in
                    HStack {
                        Text("Visibility score").monarchTextStyle(.label)
                        MonarchInfoTooltip(
                            "How often your listing appears in search, from 0 to 100.",
                            label: "About visibility score",
                            icon: icon
                        )
                    }
                }
            }
        },
        Fixture("built-in label, every arrow edge") {
            HStack {
                ForEach([Edge.top, .bottom, .leading, .trailing], id: \.self) { edge in
                    MonarchInfoTooltip("Explanation", arrowEdge: edge)
                }
            }
        },
        Fixture("controlled, closed") {
            MonarchInfoTooltip("Explanation", isPresented: .constant(false))
        },
        Fixture("disabled") {
            MonarchInfoTooltip("Explanation", icon: .help).disabled(true)
        },
    ]
}

@Suite("MonarchInfoTooltip")
struct MonarchInfoTooltipTests {
    @Test("Each icon has its own symbol")
    func symbols() {
        #expect(MonarchInfoTooltipTokens.symbol(.info) == "info.circle")
        #expect(MonarchInfoTooltipTokens.symbol(.help) == "questionmark.circle")
    }

    @Test("The glyph is muted at rest and takes the default text colour while pressed or open")
    func iconColor() {
        #expect(
            MonarchInfoTooltipTokens.iconColor(isPressed: false, isOpen: false, isEnabled: true)
                == \MonarchColorTokens.textMuted
        )
        #expect(
            MonarchInfoTooltipTokens.iconColor(isPressed: true, isOpen: false, isEnabled: true)
                == \MonarchColorTokens.textDefault
        )
        #expect(
            MonarchInfoTooltipTokens.iconColor(isPressed: false, isOpen: true, isEnabled: true)
                == \MonarchColorTokens.textDefault
        )
    }

    @Test("A disabled button uses the disabled text role in every state", arguments: [true, false])
    func disabled(isPressed: Bool) {
        for isOpen in [true, false] {
            #expect(
                MonarchInfoTooltipTokens.iconColor(isPressed: isPressed, isOpen: isOpen, isEnabled: false)
                    == \MonarchColorTokens.textDisabled
            )
        }
    }

    @Test("The bubble is the raised surface with the default text colour")
    func bubble() {
        #expect(MonarchInfoTooltipTokens.bubbleColor == \MonarchColorTokens.bgRaised)
        #expect(MonarchInfoTooltipTokens.textColor == \MonarchColorTokens.textDefault)
        #expect(MonarchInfoTooltipTokens.idealWidth == 288)
    }

    @Test("The spoken name comes from the table and can be replaced")
    func label() {
        #expect(MonarchInfoTooltipTokens.label(nil) == "More information")
        #expect(MonarchInfoTooltipTokens.label("About visibility score") == "About visibility score")
    }

    #if os(iOS)
        @Test("The button keeps a 44-point touch target")
        @MainActor
        func hitTarget() {
            let size = Host.layout(MonarchInfoTooltip("Explanation").fixedSize())
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
            #expect(size.width >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
