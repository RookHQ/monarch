import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchKeyValueFixtures: ComponentFixtures {
    static let id = "key-value"
    static let items = [
        MonarchKeyValueItem("Region", value: "eu-west"),
        MonarchKeyValueItem("Plan", value: "Team"),
        MonarchKeyValueItem("ID", value: "4f2a91c0", isCopyable: true, isMonospaced: true),
        MonarchKeyValueItem("Owner", value: nil),
    ]
    static let fixtures = [
        Fixture("every separator, wrap") {
            VStack(alignment: .leading) {
                ForEach(MonarchKeyValueSeparator.allCases, id: \.self) { separator in
                    MonarchKeyValue(items, separator: separator)
                }
            }
        },
        Fixture("stack, both sizes") {
            VStack(alignment: .leading) {
                ForEach(MonarchKeyValueSize.allCases, id: \.self) { size in
                    MonarchKeyValue(items, size: size, layout: .stack)
                }
            }
        },
        Fixture("long values") {
            MonarchKeyValue(
                [
                    MonarchKeyValueItem("Description", value: "A long value that does not fit on one line at all"),
                    MonarchKeyValueItem(
                        "Key",
                        value: "sk_live_0123456789abcdefghijklmnopqrstuvwxyz",
                        isMonospaced: true
                    ),
                ],
                separator: .divider,
                size: .md,
                emptyLabel: "Empty"
            )
        },
        Fixture("empty list") {
            MonarchKeyValue([])
        },
    ]
}

@Suite("MonarchKeyValue")
struct MonarchKeyValueTests {
    @Test("Separators have their colours, and none draws nothing")
    func separatorColours() {
        #expect(MonarchKeyValueTokens.separator(.dot) == \MonarchColorTokens.textSubtle)
        #expect(MonarchKeyValueTokens.separator(.divider) == \MonarchColorTokens.borderDefault)
        #expect(MonarchKeyValueTokens.separator(.none) == nil)
        #expect(MonarchKeyValueTokens.label == \MonarchColorTokens.textMuted)
        #expect(MonarchKeyValueTokens.value == \MonarchColorTokens.textDefault)
    }

    @Test("A separator sits before every wrapped pair but the first")
    func separatorPlacement() {
        #expect(!MonarchKeyValueTokens.showsSeparator(at: 0, separator: .dot, layout: .wrap))
        #expect(MonarchKeyValueTokens.showsSeparator(at: 1, separator: .dot, layout: .wrap))
        #expect(MonarchKeyValueTokens.showsSeparator(at: 2, separator: .divider, layout: .wrap))
        #expect(!MonarchKeyValueTokens.showsSeparator(at: 1, separator: .none, layout: .wrap))
        #expect(!MonarchKeyValueTokens.showsSeparator(at: 1, separator: .dot, layout: .stack))
    }

    @Test("Sizes pick the type role and monospaced values use code")
    func textStyles() {
        #expect(MonarchKeyValueTokens.textStyle(.sm) == .caption)
        #expect(MonarchKeyValueTokens.textStyle(.md) == .body)
        #expect(MonarchKeyValueTokens.valueStyle(.sm, isMonospaced: true) == .code)
        #expect(MonarchKeyValueTokens.valueStyle(.md, isMonospaced: false) == .body)
    }

    @Test("The copy button copies the value, another text, or nothing")
    func copyText() {
        #expect(MonarchKeyValueTokens.copyText(MonarchKeyValueItem("ID", value: "4f2a")) == nil)
        #expect(MonarchKeyValueTokens.copyText(MonarchKeyValueItem("ID", value: "4f2a", isCopyable: true)) == "4f2a")
        #expect(
            MonarchKeyValueTokens.copyText(MonarchKeyValueItem("ID", value: "4f2a…", copyValue: "4f2a91c0"))
                == "4f2a91c0"
        )
        #expect(MonarchKeyValueTokens.copyText(MonarchKeyValueItem("ID", value: nil, isCopyable: true)) == nil)
        #expect(MonarchKeyValueTokens.copyText(MonarchKeyValueItem("ID", value: "", copyValue: "x")) == nil)
    }

    @Test("An empty value is read as Not set, or the app's words")
    func accessibilityValue() {
        #expect(MonarchKeyValueTokens.accessibilityValue("eu-west", emptyLabel: nil) == "eu-west")
        #expect(MonarchKeyValueTokens.accessibilityValue(nil, emptyLabel: nil) == "Not set")
        #expect(MonarchKeyValueTokens.accessibilityValue("", emptyLabel: "None") == "None")
        #expect(MonarchKeyValueTokens.isEmpty(""))
        #expect(!MonarchKeyValueTokens.isEmpty("0"))
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchKeyValueStrings.empty == "Not set")
    }

    @Test("Pairs that do not fit wrap onto more lines")
    @MainActor
    func wraps() {
        let one = Host.layout(MonarchKeyValue([MonarchKeyValueItem("Region", value: "eu-west")]))
        let many = Host.layout(
            MonarchKeyValue(
                Array(repeating: MonarchKeyValueItem("Region", value: "eu-west-1 primary"), count: 12)
            )
        )
        #expect(many.height > one.height)
    }
}
