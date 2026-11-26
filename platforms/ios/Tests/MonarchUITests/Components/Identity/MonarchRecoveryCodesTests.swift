import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchRecoveryCodesFixtures: ComponentFixtures {
    static let id = "recovery-codes"
    static let codes: [MonarchRecoveryCode] = [
        "4f7k-2m9q", "8d3r-6t1w", MonarchRecoveryCode("1x5c-9v2b", isUsed: true), "7n4h-3j8k",
        "2p6s-5l0z", MonarchRecoveryCode("9a1e-4u7y", isUsed: true), "3g8b-1q6r", "6m2t-7c4x",
    ]

    static let fixtures = [
        Fixture("setup, with every action") {
            MonarchRecoveryCodes(
                ["4f7k-2m9q", "8d3r-6t1w", "7n4h-3j8k", "2p6s-5l0z"],
                heading: "Rook account recovery codes",
                onRegenerate: {},
                onAcknowledge: {}
            )
        },
        Fixture("some used") {
            MonarchRecoveryCodes(codes, onRegenerate: {})
        },
        Fixture("hidden at first") {
            MonarchRecoveryCodes(codes, startsRevealed: false)
        },
        Fixture("all used") {
            MonarchRecoveryCodes([
                MonarchRecoveryCode("4f7k-2m9q", isUsed: true), MonarchRecoveryCode("8d3r-6t1w", isUsed: true),
            ])
        },
        Fixture("custom labels, toggle forced on") {
            MonarchRecoveryCodes(
                ["4f7k-2m9q", "8d3r-6t1w"],
                showsRevealToggle: true,
                labels: MonarchRecoveryCodesLabels(copy: "Copy", save: "Export", continueTitle: "Done"),
                onAcknowledge: {}
            )
        },
    ]
}

@Suite("MonarchRecoveryCodes")
struct MonarchRecoveryCodesTests {
    @Test("The file exporter writes the unused codes as plain text under the given name")
    func fileDocument() throws {
        let text = MonarchRecoveryCodesTokens.text(
            [MonarchRecoveryCode("aaaa-1111"), MonarchRecoveryCode("bbbb-2222", isUsed: true)],
            heading: "Acme account"
        )
        let document = MonarchRecoveryCodesDocument(text: text)
        let written = try #require(String(data: document.data, encoding: .utf8))
        #expect(written == text)
        #expect(written.contains("aaaa-1111") && !written.contains("bbbb-2222"))
        #expect(MonarchRecoveryCodesDocument.readableContentTypes == [.plainText])
    }

    private let codes = MonarchRecoveryCodesFixtures.codes

    @Test("Only unused codes are copied and saved, one per line, after an optional heading")
    func text() {
        #expect(MonarchRecoveryCodesTokens.unused(codes).count == 6)
        #expect(!MonarchRecoveryCodesTokens.unused(codes).contains("1x5c-9v2b"))
        let short: [MonarchRecoveryCode] = ["aaaa", MonarchRecoveryCode("bbbb", isUsed: true), "cccc"]
        #expect(MonarchRecoveryCodesTokens.text(short, heading: nil) == "aaaa\ncccc")
        #expect(MonarchRecoveryCodesTokens.text(short, heading: "Rook") == "Rook\n\naaaa\ncccc")
        #expect(MonarchRecoveryCodesTokens.text(short, heading: "") == "aaaa\ncccc")
    }

    @Test("The count is shown once a code is used")
    func count() {
        #expect(MonarchRecoveryCodesTokens.showsCount(codes))
        #expect(!MonarchRecoveryCodesTokens.showsCount(["aaaa", "bbbb"]))
        #expect(MonarchRecoveryCodesStrings.remaining(6, of: 8) == "6 of 8 codes left")
    }

    @Test("The show and hide button appears when the codes start hidden, unless overridden")
    func toggle() {
        #expect(MonarchRecoveryCodesTokens.showsRevealToggle(startsRevealed: false, override: nil))
        #expect(!MonarchRecoveryCodesTokens.showsRevealToggle(startsRevealed: true, override: nil))
        #expect(MonarchRecoveryCodesTokens.showsRevealToggle(startsRevealed: true, override: true))
        #expect(!MonarchRecoveryCodesTokens.showsRevealToggle(startsRevealed: false, override: false))
    }

    @Test("Continue needs the acknowledgement")
    func acknowledgement() {
        #expect(MonarchRecoveryCodesTokens.continueResult(isAcknowledged: true) == .acknowledge)
        #expect(MonarchRecoveryCodesTokens.continueResult(isAcknowledged: false) == .showError)
    }

    @Test("A new set of codes has a new signature")
    func signature() {
        #expect(MonarchRecoveryCodesTokens.signature(["a", "b"]) != MonarchRecoveryCodesTokens.signature(["a", "c"]))
        // Marking a code used is not a new set.
        #expect(
            MonarchRecoveryCodesTokens.signature(["a", "b"])
                == MonarchRecoveryCodesTokens.signature(["a", MonarchRecoveryCode("b", isUsed: true)])
        )
    }

    @Test("Used codes are subtle, struck through and read as used")
    func usedCodes() {
        #expect(MonarchRecoveryCodesTokens.codeRole(isUsed: true) == \MonarchColorTokens.textSubtle)
        #expect(MonarchRecoveryCodesTokens.codeRole(isUsed: false) == \MonarchColorTokens.textDefault)
        #expect(MonarchRecoveryCodesTokens.codeValue(isUsed: true, usedLabel: "Used") == "Used")
        #expect(MonarchRecoveryCodesTokens.codeValue(isUsed: false, usedLabel: "Used") == "")
    }

    @Test("A string literal is an unused code")
    func literal() {
        let code: MonarchRecoveryCode = "4f7k-2m9q"
        #expect(code == MonarchRecoveryCode("4f7k-2m9q", isUsed: false))
    }

    @Test("Built-in strings resolve")
    func strings() {
        let all = [
            MonarchRecoveryCodesStrings.listLabel, MonarchRecoveryCodesStrings.used, MonarchRecoveryCodesStrings.copy,
            MonarchRecoveryCodesStrings.copied, MonarchRecoveryCodesStrings.save, MonarchRecoveryCodesStrings.reveal,
            MonarchRecoveryCodesStrings.conceal, MonarchRecoveryCodesStrings.concealedHint,
            MonarchRecoveryCodesStrings.regenerate, MonarchRecoveryCodesStrings.regenerateTitle,
            MonarchRecoveryCodesStrings.regenerateDescription, MonarchRecoveryCodesStrings.regenerateConfirm,
            MonarchRecoveryCodesStrings.cancel, MonarchRecoveryCodesStrings.regenerated,
            MonarchRecoveryCodesStrings.acknowledge, MonarchRecoveryCodesStrings.acknowledgeMissing,
            MonarchRecoveryCodesStrings.continueTitle,
        ]
        #expect(all.allSatisfy { !$0.hasPrefix("monarch.") })
        #expect(MonarchRecoveryCodesStrings.acknowledge == "I have saved my recovery codes")
    }
}
