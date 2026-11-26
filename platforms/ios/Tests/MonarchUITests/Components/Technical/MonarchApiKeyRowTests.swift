import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

/// A fixed reference time, so relative times do not depend on the clock.
private let apiKeyRowNow = Date(timeIntervalSince1970: 1_794_666_600)

private struct ApiKeyRowServerError: LocalizedError {
    var errorDescription: String? { "The server is offline." }
}

private struct ApiKeyRowSilentError: Error {}

enum MonarchApiKeyRowFixtures: ComponentFixtures {
    static let id = "api-key-row"
    static let fixtures = [
        Fixture("active with actions and hidden scopes") {
            MonarchApiKeyRow(
                "Reporting job",
                prefix: "sk_live_4f2a",
                createdAt: apiKeyRowNow.addingTimeInterval(-90 * 86_400),
                lastUsedAt: apiKeyRowNow.addingTimeInterval(-7_200),
                expiresAt: apiKeyRowNow.addingTimeInterval(30 * 86_400),
                scopes: ["items:read", "items:write", "reports:read", "users:read", "billing:read"],
                now: apiKeyRowNow,
                onRotate: {},
                onRevoke: {},
                actions: {
                    Button("Edit") {}
                }
            )
        },
        Fixture("every status") {
            VStack(alignment: .leading) {
                ForEach(MonarchApiKeyStatus.allCases, id: \.self) { status in
                    MonarchApiKeyRow(
                        "Key \(status.rawValue)",
                        prefix: "sk_test_91bc",
                        createdAt: apiKeyRowNow.addingTimeInterval(-86_400),
                        status: status,
                        now: apiKeyRowNow,
                        onRevoke: {}
                    )
                }
            }
        },
        Fixture("no scopes, never used, read only") {
            MonarchApiKeyRow("Webhook signer", prefix: "whsec_77", createdAt: apiKeyRowNow, now: apiKeyRowNow)
        },
        Fixture("long name and prefix") {
            MonarchApiKeyRow(
                "Nightly export of every workspace's billing and usage data to the warehouse",
                prefix: "sk_live_0123456789abcdef0123456789abcdef",
                createdAt: apiKeyRowNow.addingTimeInterval(-400 * 86_400),
                scopes: ["exports:write"],
                now: apiKeyRowNow,
                onRotate: {}
            )
        },
        Fixture("following the clock") {
            MonarchApiKeyRow("Live clock", prefix: "sk_live_1", createdAt: apiKeyRowNow, onRotate: {})
        },
    ]
}

@Suite("MonarchApiKeyRow")
struct MonarchApiKeyRowTests {
    typealias Tokens = MonarchApiKeyRowTokens
    private let labels = MonarchApiKeyRowLabels()
    private let locale = Locale(identifier: "en_US")

    @Test("Statuses take the web's tones and names")
    func statuses() {
        #expect(Tokens.tone(.active) == .success)
        #expect(Tokens.tone(.expiring) == .warning)
        #expect(Tokens.tone(.expired) == .neutral)
        #expect(Tokens.tone(.revoked) == .danger)
        let names = MonarchApiKeyStatus.allCases.map { Tokens.statusDefinition($0, labels: labels).label }
        #expect(names == ["Active", "Expiring soon", "Expired", "Revoked"])
        #expect(Tokens.statusDefinition(.active, labels: MonarchApiKeyRowLabels(status: "Aktiv")).label == "Aktiv")
    }

    @Test("Only active and expiring keys have actions")
    func menu() {
        #expect(Tokens.showsMenu(status: .active, canRotate: true, canRevoke: false, hasActions: false))
        #expect(Tokens.showsMenu(status: .expiring, canRotate: false, canRevoke: false, hasActions: true))
        #expect(Tokens.showsMenu(status: .active, canRotate: false, canRevoke: false, hasActions: false) == false)
        #expect(Tokens.showsMenu(status: .expired, canRotate: true, canRevoke: true, hasActions: true) == false)
        #expect(Tokens.showsMenu(status: .revoked, canRotate: true, canRevoke: true, hasActions: true) == false)
    }

    @Test("Scopes beyond the limit fold into +N and are all spoken")
    func scopes() {
        let all = ["a:read", "b:read", "c:read", "d:read", "e:read"]
        let split = Tokens.scopes(all, max: 3)
        #expect(split.shown == ["a:read", "b:read", "c:read"])
        #expect(split.hidden == ["d:read", "e:read"])
        #expect(MonarchApiKeyRowStrings.moreScopes(split.hidden.count) == "+2")
        #expect(Tokens.scopes(all, max: -1).shown.isEmpty)
        #expect(Tokens.scopes(["a"], max: 3).hidden.isEmpty)
        let spoken = Tokens.scopesAccessibility(all, labels: labels)
        #expect(spoken.label == "Scopes")
        #expect(spoken.value == "a:read, b:read, c:read, d:read, e:read")
        #expect(Tokens.scopesAccessibility([], labels: labels).value == "No scopes")
    }

    @Test("The prefix is spoken without the mask")
    func prefix() {
        #expect(Tokens.prefixAccessibility("sk_live_4f2a", labels: labels) == "Key starting with sk_live_4f2a")
        #expect(Tokens.mask.count == 8)
    }

    @Test("Meta reads created, last used or never used, and expires, relative to now")
    func meta() {
        let parts = Tokens.meta(
            createdAt: apiKeyRowNow.addingTimeInterval(-2 * 86_400),
            lastUsedAt: apiKeyRowNow.addingTimeInterval(-3_600),
            expiresAt: apiKeyRowNow.addingTimeInterval(5 * 86_400),
            now: apiKeyRowNow,
            locale: locale,
            labels: labels
        )
        #expect(parts.count == 3)
        #expect(parts[0] == "Created: 2 days ago")
        #expect(parts[1] == "Last used: 1 hour ago")
        #expect(parts[2] == "Expires: in 5 days")
        let unused = Tokens.meta(
            createdAt: apiKeyRowNow,
            lastUsedAt: nil,
            expiresAt: nil,
            now: apiKeyRowNow,
            locale: locale,
            labels: labels
        )
        #expect(unused.count == 2)
        #expect(unused[1] == "Never used")
    }

    @Test("Confirmations name the key and use the web's text")
    func confirmations() {
        #expect(Tokens.confirmTitle(.rotate, name: "Reporting job", labels: labels) == "Rotate Reporting job?")
        #expect(Tokens.confirmTitle(.revoke, name: "Reporting job", labels: labels) == "Revoke Reporting job?")
        #expect(Tokens.confirmButton(.rotate, labels: labels) == "Rotate key")
        #expect(Tokens.confirmButton(.revoke, labels: labels) == "Revoke key")
        #expect(Tokens.confirmMessage(.revoke, labels: labels).contains("cannot be undone"))
        #expect(Tokens.confirmMessage(.rotate, labels: labels).hasPrefix("A new key is issued"))
        #expect(Tokens.menuTitle(.rotate, labels: labels) == "Rotate")
        #expect(Tokens.menuTitle(.revoke, labels: labels) == "Revoke")
        #expect(Tokens.cancel(labels: labels) == "Cancel")
        #expect(Tokens.actionsLabel(name: "Reporting job", labels: labels) == "Actions for Reporting job")
        #expect(Tokens.phrasePrompt("Reporting job", labels: labels) == "Type Reporting job to confirm")
    }

    @Test("Revoking waits for the phrase when one is asked for")
    func phrase() {
        #expect(Tokens.canConfirmRevoke(typed: "", phrase: nil))
        #expect(Tokens.canConfirmRevoke(typed: "", phrase: "Reporting job") == false)
        #expect(Tokens.canConfirmRevoke(typed: "reporting job", phrase: "Reporting job") == false)
        #expect(Tokens.canConfirmRevoke(typed: " Reporting job ", phrase: "Reporting job"))
    }

    @Test("One action runs at a time; a failure keeps the action to retry")
    func operations() {
        var state = Tokens.OperationState()
        let started = state.begin(.rotate)
        #expect(started)
        #expect(state.running == .rotate)
        let startedTwice = state.begin(.revoke)
        #expect(startedTwice == false)
        state.fail(message: "Offline")
        #expect(state.running == nil)
        #expect(state.failed == .rotate)
        #expect(state.message == "Offline")
        let retried = state.begin(.rotate)
        #expect(retried)
        #expect(state.failed == nil)
        #expect(state.message == nil)
        state.succeed()
        #expect(state == Tokens.OperationState())
    }

    @Test("Progress and failure messages")
    func messages() {
        #expect(Tokens.progress(.rotate, labels: labels) == "Rotating…")
        #expect(Tokens.progress(.revoke, labels: labels) == "Revoking…")
        #expect(
            Tokens.failureMessage(.rotate, error: ApiKeyRowServerError(), labels: labels) == "The server is offline."
        )
        #expect(
            Tokens.failureMessage(.rotate, error: ApiKeyRowSilentError(), labels: labels) == "Couldn’t rotate the key."
        )
        #expect(
            Tokens.failureMessage(.revoke, error: ApiKeyRowSilentError(), labels: labels) == "Couldn’t revoke the key."
        )
    }

    @Test("Overrides replace only the strings given")
    func overrides() {
        let custom = MonarchApiKeyRowLabels(prefix: "Schlüssel %@", neverUsed: "Nie benutzt", rotate: "Erneuern")
        #expect(Tokens.prefixAccessibility("sk_1", labels: custom) == "Schlüssel sk_1")
        #expect(Tokens.menuTitle(.rotate, labels: custom) == "Erneuern")
        #expect(Tokens.menuTitle(.revoke, labels: custom) == "Revoke")
        let parts = Tokens.meta(
            createdAt: apiKeyRowNow,
            lastUsedAt: nil,
            expiresAt: nil,
            now: apiKeyRowNow,
            locale: locale,
            labels: custom
        )
        #expect(parts.last == "Nie benutzt")
    }
}
