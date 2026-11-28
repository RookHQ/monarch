import AuthenticationServices
import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let now = Date(timeIntervalSinceReferenceDate: 800_000_000)

private let passkeys = [
    MonarchPasskey(
        id: "phone",
        name: "iPhone",
        created: now.addingTimeInterval(-86_400 * 60),
        lastUsed: now.addingTimeInterval(-3_600),
        isSynced: true,
        detail: "iCloud Keychain"
    ),
    MonarchPasskey(
        id: "key",
        name: "YubiKey 5C with a long name that wraps",
        created: now.addingTimeInterval(-86_400 * 200)
    ),
    MonarchPasskey(id: "bare", name: "Old laptop"),
]

/// A failure with its own description.
private struct DescribedError: LocalizedError {
    var errorDescription: String? { "The server refused the passkey." }
}

/// A failure without a description.
private struct BareError: Error {}

enum MonarchPasskeyManagerFixtures: ComponentFixtures {
    static let id = "passkey-manager"
    static let fixtures = [
        Fixture("every action") {
            MonarchPasskeyManager(passkeys, now: now, onAdd: {}, onRename: { _, _ in }, onRemove: { _ in })
        },
        Fixture("read only") {
            MonarchPasskeyManager(Array(passkeys.prefix(2)), now: now)
        },
        Fixture("unsupported") {
            MonarchPasskeyManager([passkeys[0]], isSupported: false, now: now, onAdd: {})
        },
        Fixture("last sign-in method") {
            MonarchPasskeyManager([passkeys[0]], isLastSignInMethod: true, now: now, onRemove: { _ in })
        },
        Fixture("loading") {
            MonarchPasskeyManager([], state: .loading)
        },
        Fixture("empty with add") {
            MonarchPasskeyManager([], onAdd: {})
        },
        Fixture("failed, retrying") {
            MonarchPasskeyManager([], state: .failed, isRetrying: true, onRetry: {})
        },
        Fixture("custom title and labels") {
            MonarchPasskeyManager(
                [passkeys[2]],
                title: "Sign-in keys",
                description: "Keys that can sign in to this account.",
                now: now,
                labels: MonarchPasskeyManagerLabels(neverUsed: "Not used yet")
            )
        },
    ]
}

@Suite("MonarchPasskeyManager")
struct MonarchPasskeyManagerTests {
    private let labels = MonarchPasskeyManagerLabels()
    private let locale = Locale(identifier: "en_US")

    @Test("A loaded empty list is the empty state")
    func phase() {
        #expect(MonarchPasskeyManagerTokens.phase(.ready, passkeys: passkeys) == .list)
        #expect(MonarchPasskeyManagerTokens.phase(.ready, passkeys: []) == .empty)
        #expect(MonarchPasskeyManagerTokens.phase(.loading, passkeys: passkeys) == .loading)
        #expect(MonarchPasskeyManagerTokens.phase(.failed, passkeys: passkeys) == .failed)
    }

    @Test("Cancelling the system sheet is a neutral note, not a failure")
    func cancellation() {
        let cancelled = ASAuthorizationError(.canceled)
        #expect(MonarchPasskeyManagerTokens.isCancellation(cancelled))
        #expect(MonarchPasskeyManagerTokens.isCancellation(CancellationError()))
        #expect(!MonarchPasskeyManagerTokens.isCancellation(ASAuthorizationError(.failed)))
        #expect(!MonarchPasskeyManagerTokens.isCancellation(BareError()))
        let notice = MonarchPasskeyManagerTokens.addFailure(cancelled, labels: labels)
        #expect(notice == .init(text: "Passkey setup was cancelled. Nothing was changed.", tone: .neutral))
    }

    @Test("A failure shows its own description, else the built-in one")
    func failure() {
        #expect(
            MonarchPasskeyManagerTokens.addFailure(DescribedError(), labels: labels)
                == .init(text: "The server refused the passkey.", tone: .danger)
        )
        #expect(
            MonarchPasskeyManagerTokens.addFailure(BareError(), labels: labels)
                == .init(text: "Couldn’t add the passkey. Try again.", tone: .danger)
        )
    }

    @Test("Times read Added and Last used, or Never used")
    func times() {
        let lines = MonarchPasskeyManagerTokens.times(passkeys[0], now: now, locale: locale, labels: labels)
        #expect(lines.count == 2)
        #expect(lines[0].hasPrefix("Added "))
        #expect(lines[1].hasPrefix("Last used "))
        #expect(
            MonarchPasskeyManagerTokens.times(passkeys[2], now: now, locale: locale, labels: labels) == ["Never used"]
        )
    }

    @Test("VoiceOver reads synced, detail and times after the name")
    func accessibilityValue() {
        #expect(
            MonarchPasskeyManagerTokens.accessibilityValue(
                synced: "Synced",
                detail: "iCloud Keychain",
                times: ["Last used 1 hour ago"]
            ) == "Synced, iCloud Keychain, Last used 1 hour ago"
        )
        #expect(
            MonarchPasskeyManagerTokens.accessibilityValue(synced: nil, detail: "", times: ["Never used"])
                == "Never used"
        )
    }

    @Test("Removing the only way to sign in warns")
    func lastMethod() {
        #expect(MonarchPasskeyManagerTokens.isLastMethod(passkeyCount: 1, isLastSignInMethod: true))
        #expect(!MonarchPasskeyManagerTokens.isLastMethod(passkeyCount: 2, isLastSignInMethod: true))
        #expect(!MonarchPasskeyManagerTokens.isLastMethod(passkeyCount: 1, isLastSignInMethod: false))
        let plain = MonarchPasskeyManagerTokens.confirmMessage(isLastMethod: false, labels: labels)
        let warned = MonarchPasskeyManagerTokens.confirmMessage(isLastMethod: true, labels: labels)
        #expect(plain == "You won’t be able to sign in with this passkey any more.")
        #expect(warned.hasPrefix(plain))
        #expect(warned.contains("only way to sign in"))
    }

    @Test("A rename saves a trimmed, changed, non-empty name")
    func renamed() {
        #expect(MonarchPasskeyManagerTokens.renamed("  Work phone ", from: "iPhone") == "Work phone")
        #expect(MonarchPasskeyManagerTokens.renamed("   ", from: "iPhone") == nil)
        #expect(MonarchPasskeyManagerTokens.renamed("iPhone", from: "iPhone") == nil)
    }

    @Test("Add is unavailable when unsupported or busy")
    func canAdd() {
        #expect(MonarchPasskeyManagerTokens.canAdd(isSupported: true, isAdding: false))
        #expect(!MonarchPasskeyManagerTokens.canAdd(isSupported: false, isAdding: false))
        #expect(!MonarchPasskeyManagerTokens.canAdd(isSupported: true, isAdding: true))
    }

    @Test("Overrides replace the built-in text")
    func overrides() {
        let custom = MonarchPasskeyManagerLabels(
            confirmMessage: "Sure?",
            lastMethodWarning: "Last one.",
            cancelled: "Stopped."
        )
        #expect(MonarchPasskeyManagerTokens.confirmMessage(isLastMethod: true, labels: custom) == "Sure?\n\nLast one.")
        #expect(MonarchPasskeyManagerTokens.addFailure(CancellationError(), labels: custom).text == "Stopped.")
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchPasskeyManagerStrings.title == "Passkeys")
        #expect(MonarchPasskeyManagerStrings.add == "Add a passkey")
        #expect(MonarchPasskeyManagerStrings.renameLabel("iPhone") == "Rename iPhone")
        #expect(MonarchPasskeyManagerStrings.removeLabel("iPhone") == "Remove iPhone")
        #expect(MonarchPasskeyManagerStrings.confirmTitle("iPhone") == "Remove iPhone?")
        #expect(MonarchPasskeyManagerStrings.removed("iPhone") == "Removed iPhone")
        for value in [
            MonarchPasskeyManagerStrings.description, MonarchPasskeyManagerStrings.listLabel,
            MonarchPasskeyManagerStrings.rename, MonarchPasskeyManagerStrings.renameTitle,
            MonarchPasskeyManagerStrings.nameField, MonarchPasskeyManagerStrings.save,
            MonarchPasskeyManagerStrings.remove, MonarchPasskeyManagerStrings.synced,
            MonarchPasskeyManagerStrings.confirmRemove, MonarchPasskeyManagerStrings.cancel,
            MonarchPasskeyManagerStrings.unsupported, MonarchPasskeyManagerStrings.added,
            MonarchPasskeyManagerStrings.renameFailed("a"), MonarchPasskeyManagerStrings.removeFailed("a"),
            MonarchPasskeyManagerStrings.loading, MonarchPasskeyManagerStrings.emptyTitle,
            MonarchPasskeyManagerStrings.emptyMessage, MonarchPasskeyManagerStrings.errorTitle,
        ] {
            #expect(!value.hasPrefix("monarch."))
        }
    }
}
