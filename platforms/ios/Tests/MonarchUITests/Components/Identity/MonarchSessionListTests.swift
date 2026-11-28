import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let now = Date(timeIntervalSinceReferenceDate: 800_000_000)

private let sessions = [
    MonarchSession(
        id: "phone",
        label: "Safari on iPhone",
        kind: .mobile,
        detail: "Berlin, Germany",
        signedIn: now.addingTimeInterval(-86_400 * 12),
        lastActive: now.addingTimeInterval(-60),
        isCurrent: true
    ),
    MonarchSession(
        id: "windows",
        label: "Chrome on Windows",
        detail: "203.0.113.7",
        signedIn: now.addingTimeInterval(-86_400 * 40),
        lastActive: now.addingTimeInterval(-3_600)
    ),
    MonarchSession(
        id: "ipad",
        label: "Rook for iPad with a very long name that wraps onto a second line",
        kind: .tablet
    ),
    MonarchSession(id: "unknown", label: "Unknown browser", kind: .unknown, signedIn: now.addingTimeInterval(-120)),
]

enum MonarchSessionListFixtures: ComponentFixtures {
    static let id = "session-list"
    static let fixtures = [
        Fixture("every kind, with sign out") {
            MonarchSessionList(sessions, now: now, onSignOut: { _ in }, onSignOutOthers: { _ in })
        },
        Fixture("read only") {
            MonarchSessionList(Array(sessions.prefix(2)), now: now)
        },
        Fixture("only the current session") {
            MonarchSessionList([sessions[0]], now: now, onSignOut: { _ in }, onSignOutOthers: { _ in })
        },
        Fixture("footnote") {
            MonarchSessionList(
                Array(sessions.prefix(2)),
                now: now,
                footnote: "Apps that are open stay signed in for up to an hour.",
                onSignOut: { _ in }
            )
        },
        Fixture("loading") {
            MonarchSessionList([], state: .loading)
        },
        Fixture("empty") {
            MonarchSessionList([])
        },
        Fixture("failed, retrying") {
            MonarchSessionList([], state: .failed, isRetrying: true, onRetry: {})
        },
        Fixture("custom title and labels") {
            MonarchSessionList(
                [sessions[1]],
                title: "Sessions",
                description: "Browsers signed in to Rook.",
                now: now,
                labels: MonarchSessionListLabels(signOut: "End"),
                onSignOut: { _ in }
            )
        },
    ]
}

@Suite("MonarchSessionList")
struct MonarchSessionListTests {
    private let locale = Locale(identifier: "en_US")

    @Test("A loaded empty list is the empty state, and each phase has its card state")
    func phase() {
        #expect(MonarchSessionListTokens.phase(.ready, sessions: sessions) == .list)
        #expect(MonarchSessionListTokens.phase(.ready, sessions: []) == .empty)
        #expect(MonarchSessionListTokens.phase(.loading, sessions: sessions) == .loading)
        #expect(MonarchSessionListTokens.phase(.failed, sessions: []) == .failed)
        #expect(MonarchSessionListTokens.cardState(.list) == .ready)
        #expect(MonarchSessionListTokens.cardState(.loading) == .loading)
        #expect(MonarchSessionListTokens.cardState(.empty) == .empty)
        #expect(MonarchSessionListTokens.cardState(.failed) == .error)
    }

    @Test("Sign out other devices targets every session but the current one")
    func others() {
        #expect(MonarchSessionListTokens.otherIDs(sessions) == ["windows", "ipad", "unknown"])
        #expect(MonarchSessionListTokens.showsSignOutOthers(sessions, phase: .list, hasHandler: true))
        #expect(!MonarchSessionListTokens.showsSignOutOthers(sessions, phase: .list, hasHandler: false))
        #expect(!MonarchSessionListTokens.showsSignOutOthers(sessions, phase: .loading, hasHandler: true))
        #expect(!MonarchSessionListTokens.showsSignOutOthers([sessions[0]], phase: .list, hasHandler: true))
    }

    @Test("Each kind of device has its own symbol")
    func symbols() {
        let symbols = MonarchDeviceKind.allCases.map(MonarchSessionListTokens.symbol)
        #expect(Set(symbols).count == MonarchDeviceKind.allCases.count)
    }

    @Test("The current session has no last-active line")
    func times() {
        let current = MonarchSessionListTokens.times(
            sessions[0],
            now: now,
            locale: locale,
            signedIn: "Signed in",
            lastActive: "Last active"
        )
        #expect(current.count == 1)
        #expect(current[0].hasPrefix("Signed in "))
        // The web rule picks the unit: 12 days is 1.7 weeks, which rounds to 2 weeks (the formatter alone said "last week").
        #expect(current[0] == "Signed in 2 weeks ago")
        let other = MonarchSessionListTokens.times(
            sessions[1],
            now: now,
            locale: locale,
            signedIn: "Signed in",
            lastActive: "Last active"
        )
        #expect(other.count == 2)
        #expect(other[1].hasPrefix("Last active "))
        #expect(other[1].contains("hour"))
        #expect(
            MonarchSessionListTokens.times(
                sessions[2],
                now: now,
                locale: locale,
                signedIn: "Signed in",
                lastActive: "Last active"
            ).isEmpty
        )
    }

    @Test("VoiceOver reads the badge, detail and times after the label")
    func accessibilityValue() {
        let value = MonarchSessionListTokens.accessibilityValue(
            current: "This device",
            detail: "Berlin, Germany",
            times: ["Signed in 12 days ago"]
        )
        #expect(value == "This device, Berlin, Germany, Signed in 12 days ago")
        #expect(MonarchSessionListTokens.accessibilityValue(current: nil, detail: nil, times: []).isEmpty)
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchSessionListStrings.title == "Where you’re signed in")
        #expect(MonarchSessionListStrings.signOutLabel("Chrome on Windows") == "Sign out Chrome on Windows")
        #expect(MonarchSessionListStrings.confirmTitle("Chrome", isCurrent: false) == "Sign out Chrome?")
        #expect(MonarchSessionListStrings.confirmTitle("Chrome", isCurrent: true) == "Sign out of this device?")
        #expect(
            MonarchSessionListStrings.confirmMessage(isCurrent: true)
                != MonarchSessionListStrings.confirmMessage(isCurrent: false)
        )
        #expect(MonarchSessionListStrings.othersMessage(1).hasPrefix("1 other device will"))
        #expect(MonarchSessionListStrings.othersMessage(3).hasPrefix("3 other devices will"))
        #expect(MonarchSessionListStrings.signedOutOthers(1) == "Signed out 1 other device")
        #expect(MonarchSessionListStrings.signedOutOthers(2) == "Signed out 2 other devices")
        #expect(MonarchSessionListStrings.signedOut("Chrome") == "Signed out Chrome")
        #expect(MonarchSessionListStrings.signOutFailed("Chrome") == "Couldn’t sign out Chrome. Try again.")
        for value in [
            MonarchSessionListStrings.description, MonarchSessionListStrings.listLabel,
            MonarchSessionListStrings.current, MonarchSessionListStrings.signedIn,
            MonarchSessionListStrings.lastActive, MonarchSessionListStrings.signOut,
            MonarchSessionListStrings.signOutOthers, MonarchSessionListStrings.othersTitle,
            MonarchSessionListStrings.cancel, MonarchSessionListStrings.signOutOthersFailed,
            MonarchSessionListStrings.loading, MonarchSessionListStrings.emptyTitle,
            MonarchSessionListStrings.errorTitle,
        ] {
            #expect(!value.hasPrefix("monarch."))
        }
    }
}
