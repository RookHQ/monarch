import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

#if canImport(UIKit)
    import UIKit
#endif

enum MonarchPasswordInputFixtures: ComponentFixtures {
    static let id = "password-input"
    static let fixtures = [
        Fixture("empty with prompt and helper") {
            MonarchPasswordInput(
                "Password",
                text: .constant(""),
                prompt: "Your password",
                helper: "At least 12 characters."
            )
        },
        Fixture("hidden") {
            MonarchPasswordInput("Password", text: .constant("correct horse"))
        },
        Fixture("revealed") {
            MonarchPasswordInput("Password", text: .constant("correct horse"), isRevealed: .constant(true))
        },
        Fixture("new password with error") {
            MonarchPasswordInput("New password", text: .constant("abc"), error: "Too short.", purpose: .new)
        },
        Fixture("disabled") {
            MonarchPasswordInput("Password", text: .constant("secret"), revealLabel: "Show").disabled(true)
        },
    ]
}

@Suite("MonarchPasswordInput")
struct MonarchPasswordInputTests {
    @Test("The reveal symbol shows what activating the toggle will do")
    func revealSymbol() {
        #expect(MonarchPasswordInputTokens.revealSymbol(isRevealed: false) == "eye")
        #expect(MonarchPasswordInputTokens.revealSymbol(isRevealed: true) == "eye.slash")
    }

    @Test("The border shows the error unless the field is disabled")
    func colours() {
        typealias Tokens = MonarchPasswordInputTokens
        #expect(Tokens.border(hasError: false, isEnabled: true) == \MonarchColorTokens.borderInput)
        #expect(Tokens.border(hasError: true, isEnabled: true) == \MonarchColorTokens.dangerStrong)
        #expect(Tokens.border(hasError: true, isEnabled: false) == \MonarchColorTokens.borderSubtle)
        #expect(Tokens.background(isEnabled: true) == \MonarchColorTokens.bgSurface)
        #expect(Tokens.background(isEnabled: false) == \MonarchColorTokens.bgSubtle)
        #expect(Tokens.foreground(isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(Tokens.foreground(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(Tokens.revealForeground(isEnabled: true) == \MonarchColorTokens.textMuted)
        #expect(Tokens.revealForeground(isEnabled: false) == \MonarchColorTokens.textDisabled)
    }

    @Test("The field matches the text field, so the two line up in a form")
    func matchesTextField() {
        #expect(MonarchPasswordInputTokens.minHeight == MonarchTextFieldTokens.minHeight)
        #expect(MonarchPasswordInputTokens.minHeight >= MonarchAccessibility.minimumHitTarget)
        #expect(MonarchPasswordInputTokens.cornerRadius == MonarchTextFieldTokens.cornerRadius)
        #expect(MonarchPasswordInputTokens.leadingPadding == MonarchTextFieldTokens.horizontalPadding)
        #expect(MonarchPasswordInputTokens.verticalPadding == MonarchTextFieldTokens.verticalPadding)
        #expect(
            MonarchPasswordInputTokens.border(hasError: true, isEnabled: true)
                == MonarchTextFieldTokens.border(hasError: true, isEnabled: true)
        )
    }

    @Test("VoiceOver reads the error before the helper text")
    func accessibilityHint() {
        typealias Tokens = MonarchPasswordInputTokens
        #expect(Tokens.accessibilityHint(helper: nil, error: nil).isEmpty)
        #expect(Tokens.accessibilityHint(helper: "At least 12.", error: nil) == "At least 12.")
        #expect(Tokens.accessibilityHint(helper: "At least 12.", error: "") == "At least 12.")
        #expect(Tokens.accessibilityHint(helper: "At least 12.", error: "Too short.") == "Error: Too short.")
    }

    @Test("Built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchPasswordInputStrings.show == "Show password")
        #expect(MonarchPasswordInputStrings.error("Too short.") == "Error: Too short.")
    }

    #if canImport(UIKit)
        @Test("The purpose selects the autofill content type")
        func contentType() {
            #expect(MonarchPasswordInputTokens.contentType(.current) == UITextContentType.password)
            #expect(MonarchPasswordInputTokens.contentType(.new) == UITextContentType.newPassword)
        }
    #endif

    #if os(iOS)
        @Test("The field keeps a 44-point touch target")
        @MainActor
        func hitTarget() {
            let input = MonarchPasswordInput("Password", text: .constant(""))
            #expect(Host.layout(input).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
