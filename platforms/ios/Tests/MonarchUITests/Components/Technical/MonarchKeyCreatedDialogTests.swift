import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchKeyCreatedDialogFixtures: ComponentFixtures {
    static let id = "key-created-dialog"
    static let fixtures = [
        Fixture("secret with a name") {
            MonarchKeyCreatedDialog("sk_live_51HxQ2JkL0mNpQr", name: "Production server")
        },
        Fixture("public, nothing to confirm") {
            MonarchKeyCreatedDialog(
                "pk_live_9d2f7a61",
                label: "Publishable key",
                description: "Use it in your web page.",
                kind: .public
            )
        },
        Fixture("masked signing secret with a note") {
            MonarchKeyCreatedDialog(
                "whsec_9d2f7a61c0b84e3f",
                label: "Signing secret",
                title: "Copy your signing secret",
                isMasked: true,
                note: "The previous secret keeps working for 24 hours."
            )
        },
        Fixture("multiline private key, custom labels") {
            MonarchKeyCreatedDialog(
                "-----BEGIN PRIVATE KEY-----\n" + String(repeating: "MIIEvQIBADANBgkqhkiG9w0BAQEFAASC", count: 6)
                    + "\n-----END PRIVATE KEY-----",
                label: "Private key",
                isMultiline: true,
                labels: MonarchKeyCreatedDialogLabels(done: "Fertig", copy: "Kopieren")
            )
        },
        Fixture("secret without acknowledgement") {
            MonarchKeyCreatedDialog("tok_123", requiresAcknowledgement: false)
        },
    ]
}

@Suite("MonarchKeyCreatedDialog")
struct MonarchKeyCreatedDialogTests {
    typealias Tokens = MonarchKeyCreatedDialogTokens

    @Test("A secret needs the acknowledgement by default; a public value does not")
    func acknowledgement() {
        #expect(Tokens.needsAcknowledgement(kind: .secret, requires: nil))
        #expect(!Tokens.needsAcknowledgement(kind: .public, requires: nil))
        #expect(!Tokens.needsAcknowledgement(kind: .secret, requires: false))
        #expect(Tokens.needsAcknowledgement(kind: .public, requires: true))
    }

    @Test("The sheet closes only once confirmed; Done explains otherwise")
    func done() {
        #expect(!Tokens.isConfirmed(needsAcknowledgement: true, isAcknowledged: false))
        #expect(Tokens.isConfirmed(needsAcknowledgement: true, isAcknowledged: true))
        #expect(Tokens.isConfirmed(needsAcknowledgement: false, isAcknowledged: false))
        #expect(Tokens.doneOutcome(isConfirmed: false) == .nudge)
        #expect(Tokens.doneOutcome(isConfirmed: true) == .close)
        #expect(Tokens.showsNudge(isNudged: true, isAcknowledged: false))
        #expect(!Tokens.showsNudge(isNudged: true, isAcknowledged: true))
        #expect(!Tokens.showsNudge(isNudged: false, isAcknowledged: false))
    }

    @Test("A secret gets a warning, a public value an information notice")
    func notice() {
        let labels = MonarchKeyCreatedDialogLabels()
        let secret = Tokens.notice(kind: .secret, labels: labels)
        #expect(secret.tone == .warning)
        #expect(secret.title == "Copy it now. It will not be shown again.")
        #expect(secret.message.hasPrefix("Keep it in a secret manager"))
        let open = Tokens.notice(kind: .public, labels: labels)
        #expect(open.tone == .info)
        #expect(open.title == "You can view this again later.")
        #expect(open.message.hasPrefix("It is meant to be public"))
    }

    @Test("The field is labelled with the name, else with what the value is")
    func fieldLabel() {
        #expect(Tokens.fieldLabel(name: "Production", label: "API key") == "Production")
        #expect(Tokens.fieldLabel(name: nil, label: "API key") == "API key")
        #expect(Tokens.fieldLabel(name: "", label: "API key") == "API key")
    }

    @Test("Built-in strings resolve and every one can be replaced")
    func strings() {
        let labels = MonarchKeyCreatedDialogLabels()
        #expect(labels.resolvedTitle == "Copy your new key")
        #expect(labels.resolvedLabel == "API key")
        #expect(labels.acknowledgeText("API key") == "I have saved this API key somewhere safe.")
        #expect(labels.resolvedAcknowledgeRequired == "Confirm that you have saved it before you close this dialog.")
        #expect(labels.resolvedDone == "Done")
        #expect(labels.resolvedCopy == "Copy")
        #expect(labels.resolvedCopied == "Copied")

        let custom = MonarchKeyCreatedDialogLabels(
            title: "T",
            label: "L",
            secretNoticeTitle: "ST",
            secretNotice: "S",
            publicNoticeTitle: "PT",
            publicNotice: "P",
            acknowledge: { "OK \($0)" },
            acknowledgeRequired: "R",
            done: "D",
            copy: "C",
            copied: "CD"
        )
        #expect(custom.resolvedTitle == "T")
        #expect(custom.resolvedLabel == "L")
        #expect(
            Tokens.notice(kind: .secret, labels: custom)
                == MonarchKeyCreatedDialogNotice(title: "ST", message: "S", tone: .warning)
        )
        #expect(
            Tokens.notice(kind: .public, labels: custom)
                == MonarchKeyCreatedDialogNotice(title: "PT", message: "P", tone: .info)
        )
        #expect(custom.acknowledgeText("x") == "OK x")
        #expect(custom.resolvedAcknowledgeRequired == "R")
        #expect(custom.resolvedDone == "D")
        #expect(custom.resolvedCopy == "C")
        #expect(custom.resolvedCopied == "CD")
    }
}
