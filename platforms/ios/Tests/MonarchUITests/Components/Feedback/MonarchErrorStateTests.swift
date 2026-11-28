import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchErrorStateFixtures: ComponentFixtures {
    static let id = "error-state"
    static let fixtures = [
        Fixture("every kind") {
            VStack {
                ForEach(MonarchErrorStateKind.allCases, id: \.self) { kind in
                    MonarchErrorState("Could not load", message: "Kind \(kind)", kind: kind, size: .sm, onRetry: {})
                }
            }
        },
        Fixture("server error with request id and second action") {
            MonarchErrorState(
                "Invoices didn't load",
                message: "Something went wrong on our side. Try again in a minute.",
                kind: .server,
                requestID: "req_8f2k1c9d-4a7b-41e2-9c3f-0d6a5e7b1f20",
                requestIDLabel: "Reference",
                actionTitle: "Contact support",
                onRetry: {},
                action: {}
            )
        },
        Fixture("retrying, custom strings and icon") {
            MonarchErrorState(
                "You're offline",
                systemImage: "antenna.radiowaves.left.and.right.slash",
                kind: .offline,
                size: .lg,
                isRetrying: true,
                retryLabel: "Reconnect",
                retryingLabel: "Reconnecting",
                onRetry: {}
            )
        },
        Fixture("title only") {
            MonarchErrorState("Something went wrong")
        },
    ]
}

@Suite("MonarchErrorState")
struct MonarchErrorStateTests {
    @Test("Each kind has its own default icon, and the app's icon wins")
    func symbol() {
        let symbols = MonarchErrorStateKind.allCases.map { MonarchErrorStateTokens.symbol(kind: $0, override: nil) }
        #expect(Set(symbols).count == MonarchErrorStateKind.allCases.count)
        #expect(MonarchErrorStateTokens.symbol(kind: .server, override: "bolt.slash") == "bolt.slash")
        #expect(MonarchErrorStateTokens.symbol(kind: .forbidden, override: "") == "lock")
    }

    @Test("Failures use the danger colour; no access, not found and maintenance are muted")
    func iconRole() {
        for kind in [MonarchErrorStateKind.error, .offline, .server] {
            #expect(MonarchErrorStateTokens.iconRole(kind: kind) == \MonarchColorTokens.dangerStrong)
        }
        for kind in [MonarchErrorStateKind.forbidden, .notFound, .maintenance] {
            #expect(MonarchErrorStateTokens.iconRole(kind: kind) == \MonarchColorTokens.textMuted)
        }
    }

    @Test("Retry is not offered where it cannot help, or without a handler")
    func retry() {
        #expect(!MonarchErrorStateTokens.showsRetry(kind: .forbidden, hasHandler: true))
        #expect(!MonarchErrorStateTokens.showsRetry(kind: .notFound, hasHandler: true))
        for kind in [MonarchErrorStateKind.error, .offline, .server, .maintenance] {
            #expect(MonarchErrorStateTokens.showsRetry(kind: kind, hasHandler: true))
            #expect(!MonarchErrorStateTokens.showsRetry(kind: kind, hasHandler: false))
        }
    }

    @Test("The request id is read with its label")
    func requestID() {
        #expect(MonarchErrorStateTokens.requestIDLabel(nil, label: nil) == nil)
        #expect(MonarchErrorStateTokens.requestIDLabel("", label: nil) == nil)
        #expect(MonarchErrorStateTokens.requestIDLabel("req_1", label: nil) == "Request ID: req_1")
        #expect(MonarchErrorStateTokens.requestIDLabel("req_1", label: "Reference") == "Reference: req_1")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchErrorStateStrings.retry == "Try again")
        #expect(MonarchErrorStateStrings.retrying == "Retrying")
        #expect(MonarchErrorStateStrings.requestID == "Request ID")
    }

    @Test("The retry button adds height only for kinds that can be retried")
    @MainActor
    func retryButtonLayout() {
        let plain = Host.layout(MonarchErrorState("Could not load", kind: .server))
        let withRetry = Host.layout(MonarchErrorState("Could not load", kind: .server, onRetry: {}))
        let forbidden = Host.layout(MonarchErrorState("Could not load", kind: .forbidden))
        let forbiddenWithRetry = Host.layout(MonarchErrorState("Could not load", kind: .forbidden, onRetry: {}))
        #expect(withRetry.height > plain.height)
        #expect(forbiddenWithRetry.height == forbidden.height)
    }
}
