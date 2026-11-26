import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchCopyFieldFixtures: ComponentFixtures {
    static let id = "copy-field"
    static let fixtures = [
        Fixture("sizes") {
            VStack(alignment: .leading) {
                ForEach(MonarchButtonSize.allCases, id: \.self) { size in
                    MonarchCopyField("Webhook URL", value: "https://example.com/hooks/42", size: size)
                }
            }
        },
        Fixture("long single-line value, icon only, proportional type") {
            VStack(alignment: .leading) {
                MonarchCopyField(
                    "Calendar feed",
                    value: "https://calendar.example.com/feeds/workspace-01HZX4/team/ical/subscribe.ics",
                    isIconOnly: true
                )
                MonarchCopyField("Reference", value: "Invoice 1042, March", isMonospaced: false)
            }
        },
        Fixture("multi-line") {
            MonarchCopyField(
                "Embed code",
                value: "<script src=\"https://example.com/widget.js\"\n  data-workspace=\"ws_01HZX4\" async></script>",
                isMultiline: true
            )
        },
        Fixture("hidden label, custom titles, empty value, disabled") {
            VStack(alignment: .leading) {
                MonarchCopyField("Key", value: "pk_live_51H", isLabelHidden: true, copyTitle: "Copy key")
                MonarchCopyField("Empty", value: "")
                MonarchCopyField("Disabled", value: "ws_01HZX4").disabled(true)
            }
        },
    ]
}

@Suite("MonarchCopyField")
struct MonarchCopyFieldTests {
    @Test("A single-line field keeps one line; a multi-line field shows the whole value")
    func lines() {
        #expect(MonarchCopyFieldTokens.lineLimit(isMultiline: false) == 1)
        #expect(MonarchCopyFieldTokens.lineLimit(isMultiline: true) == nil)
        #expect(MonarchCopyFieldTokens.rowAlignment(isMultiline: false) == .center)
        #expect(MonarchCopyFieldTokens.rowAlignment(isMultiline: true) == .top)
    }

    @Test("Codes use the code type role unless proportional type is asked for")
    func textStyle() {
        #expect(MonarchCopyFieldTokens.textStyle(isMonospaced: true) == .code)
        #expect(MonarchCopyFieldTokens.textStyle(isMonospaced: false) == .body)
    }

    @Test("VoiceOver reads the whole value, also when the box shortens it")
    func accessibilityValue() {
        let long = "https://calendar.example.com/feeds/workspace-01HZX4/team/ical/subscribe.ics"
        #expect(MonarchCopyFieldTokens.accessibilityValue(long) == long)
        #expect(MonarchCopyButtonTokens.accessibilityLabel(title: "Copy", target: "Webhook URL") == "Copy Webhook URL")
    }

    @Test("An empty value keeps the box one line high")
    func emptyValue() {
        #expect(MonarchCopyFieldTokens.displayText("") == " ")
        #expect(MonarchCopyFieldTokens.displayText("abc") == "abc")
    }

    @Test("Disabled fields use the disabled roles instead of dimming")
    func colours() {
        #expect(MonarchCopyFieldTokens.text(isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(MonarchCopyFieldTokens.text(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(MonarchCopyFieldTokens.label(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(MonarchCopyFieldTokens.background(isEnabled: true) == \MonarchColorTokens.bgSubtle)
        #expect(MonarchCopyFieldTokens.background(isEnabled: false) == \MonarchColorTokens.bgMuted)
        #expect(MonarchCopyFieldTokens.border(isEnabled: true) == \MonarchColorTokens.borderInput)
        #expect(MonarchCopyFieldTokens.border(isEnabled: false) == \MonarchColorTokens.borderSubtle)
    }

    @Test("The box is as high as the button of the same size")
    func heights() {
        for size in MonarchButtonSize.allCases {
            #expect(MonarchCopyFieldTokens.minHeight(size) == MonarchButtonTokens.minHeight(size))
        }
    }

    @Test("A multi-line field grows with its value")
    @MainActor
    func multilineGrows() {
        let value = Array(repeating: "line", count: 8).joined(separator: "\n")
        let single = Host.layout(MonarchCopyField("Snippet", value: value))
        let multi = Host.layout(MonarchCopyField("Snippet", value: value, isMultiline: true))
        #expect(multi.height > single.height)
    }
}
