import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchOtpPinInputFixtures: ComponentFixtures {
    static let id = "otp-pin-input"
    static let fixtures = [
        Fixture("empty with helper") {
            MonarchOtpPinInput("Verification code", text: .constant(""), helper: "Sent to your phone.")
        },
        Fixture("partly typed") {
            MonarchOtpPinInput("Verification code", text: .constant("123"))
        },
        Fixture("complete, grouped, with error") {
            MonarchOtpPinInput("Code", text: .constant("482913"), error: "That code has expired.", groupSize: 3)
        },
        Fixture("masked, large, four cells") {
            MonarchOtpPinInput("PIN", text: .constant("12"), length: 4, size: .lg, isMasked: true)
        },
        Fixture("alphanumeric, eight cells") {
            MonarchOtpPinInput("Invite code", text: .constant("ab12cd"), length: 8, mode: .alphanumeric, groupSize: 4)
        },
        Fixture("disabled") {
            MonarchOtpPinInput("Code", text: .constant("1234"), helper: "Ask your admin for a code.").disabled(true)
        },
    ]
}

@Suite("MonarchOtpPinInput")
struct MonarchOtpPinInputTests {
    @Test("Numeric codes keep digits only, from any script, as ASCII")
    func cleanNumeric() {
        #expect(MonarchOtpPinInputTokens.clean("12 34-56", mode: .numeric, length: 6) == "123456")
        #expect(MonarchOtpPinInputTokens.clean("12ab", mode: .numeric, length: 6) == "12")
        #expect(MonarchOtpPinInputTokens.clean("١٢٣", mode: .numeric, length: 6) == "123")
        #expect(MonarchOtpPinInputTokens.clean("４５６", mode: .numeric, length: 6) == "456")
        #expect(MonarchOtpPinInputTokens.clean("½④", mode: .numeric, length: 6).isEmpty)
    }

    @Test("Alphanumeric codes keep A to Z and digits, in upper case")
    func cleanAlphanumeric() {
        #expect(MonarchOtpPinInputTokens.clean("ab-12 cd", mode: .alphanumeric, length: 8) == "AB12CD")
        #expect(MonarchOtpPinInputTokens.clean("é1ß2", mode: .alphanumeric, length: 8) == "12")
    }

    @Test("A code is never longer than its length")
    func cleanTruncates() {
        #expect(MonarchOtpPinInputTokens.clean("1234567890", mode: .numeric, length: 6) == "123456")
        #expect(MonarchOtpPinInputTokens.clean("1234567890", mode: .numeric, length: 1) == "1")
        #expect(MonarchOtpPinInputTokens.length(0) == 1)
        #expect(MonarchOtpPinInputTokens.length(-3) == 1)
        #expect(MonarchOtpPinInputTokens.length(8) == 8)
    }

    @Test("Typing reports a change only when the cleaned code differs, and completion at full length")
    func change() {
        typealias Tokens = MonarchOtpPinInputTokens
        #expect(Tokens.change(from: "12", typed: "12a", mode: .numeric, length: 6) == nil)
        #expect(Tokens.change(from: "123456", typed: "1234567", mode: .numeric, length: 6) == nil)
        #expect(
            Tokens.change(from: "12", typed: "123", mode: .numeric, length: 6)
                == Tokens.Change(value: "123", isComplete: false)
        )
        #expect(
            Tokens.change(from: "", typed: "482 913", mode: .numeric, length: 6)
                == Tokens.Change(value: "482913", isComplete: true)
        )
        #expect(
            Tokens.change(from: "123456", typed: "12345", mode: .numeric, length: 6)
                == Tokens.Change(value: "12345", isComplete: false)
        )
    }

    @Test("The active cell is the next empty one, or the last when the code is full")
    func activeCell() {
        #expect(MonarchOtpPinInputTokens.activeIndex(count: 0, length: 6, isFocused: false) == nil)
        #expect(MonarchOtpPinInputTokens.activeIndex(count: 0, length: 6, isFocused: true) == 0)
        #expect(MonarchOtpPinInputTokens.activeIndex(count: 3, length: 6, isFocused: true) == 3)
        #expect(MonarchOtpPinInputTokens.activeIndex(count: 6, length: 6, isFocused: true) == 5)
    }

    @Test("The caret shows only in the active cell while it is empty")
    func caret() {
        #expect(MonarchOtpPinInputTokens.showsCaret(at: 3, count: 3, length: 6, isFocused: true))
        #expect(!MonarchOtpPinInputTokens.showsCaret(at: 2, count: 3, length: 6, isFocused: true))
        #expect(!MonarchOtpPinInputTokens.showsCaret(at: 3, count: 3, length: 6, isFocused: false))
        #expect(!MonarchOtpPinInputTokens.showsCaret(at: 5, count: 6, length: 6, isFocused: true))
    }

    @Test("Groups put a wider gap before every group but the first")
    func separators() {
        let withGroups = (0..<6).filter { MonarchOtpPinInputTokens.hasSeparator(before: $0, groupSize: 3) }
        #expect(withGroups == [3])
        let pairs = (0..<6).filter { MonarchOtpPinInputTokens.hasSeparator(before: $0, groupSize: 2) }
        #expect(pairs == [2, 4])
        #expect((0..<6).allSatisfy { !MonarchOtpPinInputTokens.hasSeparator(before: $0, groupSize: nil) })
        #expect((0..<6).allSatisfy { !MonarchOtpPinInputTokens.hasSeparator(before: $0, groupSize: 0) })
    }

    @Test("Cell colours follow the error and disabled states")
    func colours() {
        #expect(MonarchOtpPinInputTokens.border(hasError: false, isEnabled: true) == \MonarchColorTokens.borderInput)
        #expect(MonarchOtpPinInputTokens.border(hasError: true, isEnabled: true) == \MonarchColorTokens.dangerStrong)
        #expect(MonarchOtpPinInputTokens.border(hasError: true, isEnabled: false) == \MonarchColorTokens.borderSubtle)
        #expect(MonarchOtpPinInputTokens.background(isEnabled: true) == \MonarchColorTokens.bgSurface)
        #expect(MonarchOtpPinInputTokens.background(isEnabled: false) == \MonarchColorTokens.bgSubtle)
        #expect(MonarchOtpPinInputTokens.foreground(isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(MonarchOtpPinInputTokens.foreground(isEnabled: false) == \MonarchColorTokens.textDisabled)
    }

    @Test("VoiceOver hears the code length, then the error or else the helper text")
    func accessibilityHint() {
        typealias Tokens = MonarchOtpPinInputTokens
        #expect(Tokens.accessibilityHint(length: 6, lengthHint: nil, helper: nil, error: nil) == "Code length: 6")
        #expect(
            Tokens.accessibilityHint(length: 4, lengthHint: nil, helper: "Sent by text.", error: nil)
                == "Code length: 4. Sent by text."
        )
        #expect(
            Tokens.accessibilityHint(length: 6, lengthHint: nil, helper: "Sent by text.", error: "Expired")
                == "Code length: 6. Error: Expired"
        )
        #expect(
            Tokens.accessibilityHint(length: 6, lengthHint: "Six digits", helper: nil, error: "")
                == "Six digits"
        )
    }

    @Test("Built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchOtpPinInputStrings.lengthHint(6) == "Code length: 6")
        #expect(MonarchOtpPinInputStrings.error("Expired") == "Error: Expired")
    }

    @Test("Cells are at least as tall as the touch target", arguments: MonarchOtpPinInputSize.allCases)
    func hitTarget(size: MonarchOtpPinInputSize) {
        #expect(MonarchOtpPinInputTokens.cellHeight(size) >= MonarchAccessibility.minimumHitTarget)
        #expect(MonarchOtpPinInputTokens.cellWidth(size) >= MonarchAccessibility.minimumHitTarget)
    }

    @Test("The large size has larger cells")
    func sizes() {
        #expect(MonarchOtpPinInputTokens.cellWidth(.md) == 44)
        #expect(MonarchOtpPinInputTokens.cellWidth(.lg) == 52)
        #expect(MonarchOtpPinInputTokens.cellHeight(.lg) == 48)
    }

    #if os(iOS)
        @Test("Six cells fit the canvas at the largest text size")
        @MainActor
        func fitsAtLargestText() {
            let input = MonarchOtpPinInput("Code", text: .constant("123456"))
                .environment(\.dynamicTypeSize, .accessibility5)
            #expect(Host.layout(input).width <= Host.width)
        }
    #endif
}
