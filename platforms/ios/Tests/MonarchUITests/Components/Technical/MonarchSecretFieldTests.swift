import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchSecretFieldFixtures: ComponentFixtures {
    static let id = "secret-field"
    static let fixtures = [
        Fixture("empty with prompt and helper") {
            MonarchSecretField(
                "API key",
                text: .constant(""),
                prompt: "sk_live_…",
                helper: "Find it in the provider's dashboard.",
                isCopyable: true
            )
        },
        Fixture("masked with copy") {
            MonarchSecretField("API key", text: .constant("sk_live_51HxQ2Jk"), isCopyable: true)
        },
        Fixture("revealed") {
            MonarchSecretField(
                "API key",
                text: .constant("sk_live_51HxQ2Jk"),
                isCopyable: true,
                isRevealed: .constant(true)
            )
        },
        Fixture("set, not shown") {
            MonarchSecretField("Bot token", text: .constant(""), helper: "Stored encrypted.", isSet: true)
        },
        Fixture("set, with a replacement typed") {
            MonarchSecretField("Bot token", text: .constant("xoxb-new"), isSet: true, isCopyable: true)
        },
        Fixture("read-only masked") {
            MonarchSecretField(
                "Signing secret",
                text: .constant("whsec_9d2f7a61c0b84e3f"),
                helper: "Shown once. Store it somewhere safe.",
                isCopyable: true,
                isReadOnly: true
            )
        },
        Fixture("read-only revealed, long") {
            MonarchSecretField(
                "Signing secret",
                text: .constant(String(repeating: "whsec_9d2f7a61c0b84e3f", count: 6)),
                isCopyable: true,
                isReadOnly: true,
                isRevealed: .constant(true)
            )
        },
        Fixture("without buttons") {
            MonarchSecretField("Webhook secret", text: .constant("abc123"), isRevealable: false)
        },
        Fixture("error") {
            MonarchSecretField("Token", text: .constant("abc"), error: "This token has expired.")
        },
        Fixture("disabled") {
            MonarchSecretField(
                "API key",
                text: .constant("secret"),
                isCopyable: true,
                labels: MonarchSecretFieldLabels(show: { "Reveal \($0)" })
            )
            .disabled(true)
        },
    ]
}

@Suite("MonarchSecretField")
struct MonarchSecretFieldTests {
    typealias Tokens = MonarchSecretFieldTokens

    @Test("The symbols show what activating each button will do")
    func symbols() {
        #expect(Tokens.revealSymbol(isRevealed: false) == "eye")
        #expect(Tokens.revealSymbol(isRevealed: true) == "eye.slash")
        #expect(Tokens.copySymbol(isCopied: false) == "doc.on.doc")
        #expect(Tokens.copySymbol(isCopied: true) == "checkmark")
    }

    @Test("Reveal and copy act only when there is text on the device")
    func plainText() {
        #expect(!Tokens.hasPlainText(""))
        #expect(Tokens.hasPlainText("x"))
    }

    @Test("A saved secret shows the saved prompt until a replacement is typed")
    func prompt() {
        #expect(Tokens.prompt("Key", isSet: false, text: "", setPrompt: "Saved") == "Key")
        #expect(Tokens.prompt(nil, isSet: false, text: "", setPrompt: "Saved") == nil)
        #expect(Tokens.prompt("Key", isSet: true, text: "", setPrompt: "Saved") == "Saved")
        #expect(Tokens.prompt("Key", isSet: true, text: "new", setPrompt: "Saved") == "Key")
    }

    @Test("A read-only secret is masked character for character and spoken only when revealed")
    func readOnly() {
        #expect(Tokens.readOnlyText("abc", isRevealed: false) == "\u{2022}\u{2022}\u{2022}")
        #expect(Tokens.readOnlyText("abc", isRevealed: true) == "abc")
        #expect(Tokens.readOnlyText("", isRevealed: false).isEmpty)
        #expect(Tokens.readOnlyAccessibilityValue("abc", isRevealed: false, hidden: "Hidden") == "Hidden")
        #expect(Tokens.readOnlyAccessibilityValue("abc", isRevealed: true, hidden: "Hidden") == "abc")
        #expect(Tokens.readOnlyAccessibilityValue("", isRevealed: false, hidden: "Hidden").isEmpty)
    }

    @Test("The hint reads the error, the saved description and the helper in that order")
    func accessibilityHint() {
        let labels = MonarchSecretFieldLabels()
        let saved = "A value is saved and hidden. Leave blank to keep it."
        #expect(Tokens.accessibilityHint(helper: nil, error: nil, isSet: false, text: "", labels: labels).isEmpty)
        #expect(
            Tokens.accessibilityHint(helper: "Stored.", error: nil, isSet: false, text: "", labels: labels)
                == "Stored."
        )
        #expect(
            Tokens.accessibilityHint(helper: "Stored.", error: "", isSet: false, text: "", labels: labels)
                == "Stored."
        )
        #expect(
            Tokens.accessibilityHint(helper: "Stored.", error: "Expired.", isSet: false, text: "x", labels: labels)
                == "Error: Expired."
        )
        #expect(
            Tokens.accessibilityHint(helper: "Stored.", error: nil, isSet: true, text: "", labels: labels)
                == "\(saved) Stored."
        )
        #expect(
            Tokens.accessibilityHint(helper: "Stored.", error: nil, isSet: true, text: "new", labels: labels)
                == "Stored."
        )
        #expect(
            Tokens.accessibilityHint(helper: nil, error: "Expired.", isSet: true, text: "", labels: labels)
                == "Error: Expired. \(saved)"
        )
    }

    @Test("The buttons are named after the secret, and every string can be replaced")
    func labels() {
        let builtIn = MonarchSecretFieldLabels()
        #expect(builtIn.showName("API key") == "Show API key")
        #expect(builtIn.copyName("API key", isCopied: false) == "Copy API key")
        #expect(builtIn.copyName("API key", isCopied: true) == "Copied")
        #expect(builtIn.resolvedCopiedAnnouncement == "Copied to clipboard")
        #expect(builtIn.resolvedSetPrompt == "Saved. Enter a new value to replace it")
        #expect(builtIn.resolvedSetDescription == "A value is saved and hidden. Leave blank to keep it.")
        #expect(builtIn.resolvedHidden == "Hidden")
        #expect(builtIn.errorText("Expired.") == "Error: Expired.")

        let custom = MonarchSecretFieldLabels(
            show: { "Afficher \($0)" },
            copy: { "Copier \($0)" },
            copied: "Copié",
            copiedAnnouncement: "Copié dans le presse-papiers",
            setPrompt: "Enregistré",
            setDescription: "Une valeur est enregistrée.",
            hidden: "Masqué",
            error: { "Erreur : \($0)" }
        )
        #expect(custom.showName("clé") == "Afficher clé")
        #expect(custom.copyName("clé", isCopied: false) == "Copier clé")
        #expect(custom.copyName("clé", isCopied: true) == "Copié")
        #expect(custom.resolvedCopiedAnnouncement == "Copié dans le presse-papiers")
        #expect(custom.resolvedSetPrompt == "Enregistré")
        #expect(custom.resolvedSetDescription == "Une valeur est enregistrée.")
        #expect(custom.resolvedHidden == "Masqué")
        #expect(custom.errorText("x") == "Erreur : x")
    }

    @Test("Built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchSecretFieldStrings.show("key") == "Show key")
        #expect(MonarchSecretFieldStrings.copy("key") == "Copy key")
        for value in [
            MonarchSecretFieldStrings.copied, MonarchSecretFieldStrings.announcement,
            MonarchSecretFieldStrings.setPrompt, MonarchSecretFieldStrings.setDescription,
            MonarchSecretFieldStrings.hidden,
        ] {
            #expect(!value.hasPrefix("monarch."))
        }
    }

    @Test("Colours follow the state; buttons dim when there is nothing to act on")
    func colours() {
        #expect(Tokens.border(hasError: false, isEnabled: true) == \MonarchColorTokens.borderInput)
        #expect(Tokens.border(hasError: true, isEnabled: true) == \MonarchColorTokens.dangerStrong)
        #expect(Tokens.border(hasError: true, isEnabled: false) == \MonarchColorTokens.borderSubtle)
        #expect(Tokens.background(isEnabled: true, isReadOnly: false) == \MonarchColorTokens.bgSurface)
        #expect(Tokens.background(isEnabled: false, isReadOnly: false) == \MonarchColorTokens.bgSubtle)
        #expect(Tokens.background(isEnabled: true, isReadOnly: true) == \MonarchColorTokens.bgSubtle)
        #expect(Tokens.foreground(isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(Tokens.foreground(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(Tokens.actionForeground(isEnabled: true, canAct: true) == \MonarchColorTokens.textMuted)
        #expect(Tokens.actionForeground(isEnabled: true, canAct: false) == \MonarchColorTokens.textDisabled)
        #expect(Tokens.actionForeground(isEnabled: false, canAct: true) == \MonarchColorTokens.textDisabled)
    }

    @Test("The field matches the text field and keeps a 44-point height")
    func metrics() {
        #expect(Tokens.minHeight == MonarchTextFieldTokens.minHeight)
        #expect(Tokens.minHeight >= MonarchAccessibility.minimumHitTarget)
        #expect(Tokens.cornerRadius == MonarchTextFieldTokens.cornerRadius)
        #expect(Tokens.leadingPadding == MonarchTextFieldTokens.horizontalPadding)
    }

    @Test("The copied state ends only after a copy")
    func resets() {
        #expect(!Tokens.resets(copyCount: 0))
        #expect(Tokens.resets(copyCount: 1))
        #expect(Tokens.copiedDuration > .zero)
    }

    #if os(iOS)
        @Test("The field keeps a 44-point touch target")
        @MainActor
        func hitTarget() {
            let field = MonarchSecretField("API key", text: .constant(""), isCopyable: true)
            #expect(Host.layout(field).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
