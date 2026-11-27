import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchConnectedAppsListFixtures: ComponentFixtures {
    static let id = "connected-apps-list"
    static let now = Date(timeIntervalSince1970: 1_800_000_000)
    static let apps = [
        MonarchConnectedApp(
            id: "notes",
            name: "Notes",
            domain: "notes.example.com",
            isVerified: true,
            workspace: "Acme",
            scopes: ["See your email address", "See your name and profile picture"],
            connectedAt: now.addingTimeInterval(-3 * 86_400),
            lastUsedAt: now.addingTimeInterval(-86_400)
        ),
        MonarchConnectedApp(
            id: "old",
            name: "A very old reporting tool with a long name",
            logoURL: URL(filePath: "/missing.png"),
            isSuspended: true,
            connectedAt: now.addingTimeInterval(-400 * 86_400)
        ),
    ]

    static let fixtures = [
        Fixture("rows with remove") {
            MonarchConnectedAppsList(apps, now: now, onRevoke: { _ in })
        },
        Fixture("rows, read only, custom title") {
            MonarchConnectedAppsList(apps, title: "Apps with access", description: "Signed in with Rook.", now: now)
        },
        Fixture("no header") {
            MonarchConnectedAppsList(apps, title: "", now: now)
        },
        Fixture("loading") {
            MonarchConnectedAppsList([], state: .loading)
        },
        Fixture("error with retry") {
            MonarchConnectedAppsList([], state: .error, onRetry: {})
        },
        Fixture("error retrying") {
            MonarchConnectedAppsList([], state: .error, isRetrying: true, onRetry: {})
        },
        Fixture("empty") {
            MonarchConnectedAppsList([])
        },
    ]
}

@Suite("MonarchConnectedAppsList")
struct MonarchConnectedAppsListTests {
    private let english = Locale(identifier: "en_US")
    private let now = MonarchConnectedAppsListFixtures.now
    private let apps = MonarchConnectedAppsListFixtures.apps

    @Test("The state and the number of apps decide what is shown")
    func content() {
        #expect(MonarchConnectedAppsListTokens.content(state: .loading, count: 3) == .loading)
        #expect(MonarchConnectedAppsListTokens.content(state: .error, count: 3) == .error)
        #expect(MonarchConnectedAppsListTokens.content(state: .ready, count: 0) == .empty)
        #expect(MonarchConnectedAppsListTokens.content(state: .ready, count: 2) == .rows)
    }

    @Test("Relative times use the reference time and the locale")
    func relative() {
        #expect(
            MonarchConnectedAppsListTokens.relative(now.addingTimeInterval(-3 * 86_400), now: now, locale: english)
                == "3 days ago"
        )
        #expect(
            MonarchConnectedAppsListTokens.relative(now.addingTimeInterval(-86_400), now: now, locale: english)
                == "yesterday"
        )
    }

    @Test("The meta line has the connected and last-used times and the workspace")
    func meta() {
        let parts = MonarchConnectedAppsListTokens.metaParts(
            apps[0],
            now: now,
            locale: english,
            labels: MonarchConnectedAppsListLabels()
        )
        #expect(parts == ["Connected 3 days ago", "Last used yesterday", "Workspace: Acme"])
        let custom = MonarchConnectedAppsListTokens.metaParts(
            apps[0],
            now: now,
            locale: english,
            labels: MonarchConnectedAppsListLabels(connected: { "Added \($0)" }, workspace: { "In \($0)" })
        )
        #expect(custom == ["Added 3 days ago", "Last used yesterday", "In Acme"])
        #expect(
            MonarchConnectedAppsListTokens.metaParts(
                MonarchConnectedApp(id: "x", name: "X"),
                now: now,
                locale: english,
                labels: MonarchConnectedAppsListLabels()
            ).isEmpty
        )
    }

    @Test("Badges are text: verified, then suspended")
    func badges() {
        let both = MonarchConnectedApp(id: "x", name: "X", isVerified: true, isSuspended: true)
        #expect(
            MonarchConnectedAppsListTokens.badges(both, labels: MonarchConnectedAppsListLabels()) == [
                "Verified", "Suspended",
            ]
        )
        #expect(
            MonarchConnectedAppsListTokens.badges(
                MonarchConnectedApp(id: "y", name: "Y"),
                labels: MonarchConnectedAppsListLabels()
            ).isEmpty
        )
    }

    @Test("VoiceOver reads the badges, the domain and the meta after the name")
    func value() {
        #expect(
            MonarchConnectedAppsListTokens.accessibilityValue(
                badges: ["Verified"],
                domain: "notes.example.com",
                metaParts: ["Connected 3 days ago"]
            ) == "Verified, notes.example.com, Connected 3 days ago"
        )
        #expect(MonarchConnectedAppsListTokens.accessibilityValue(badges: [], domain: nil, metaParts: []) == "")
    }

    @Test("A suspended app's name is muted")
    func nameRole() {
        #expect(MonarchConnectedAppsListTokens.nameRole(isSuspended: true) == \MonarchColorTokens.textMuted)
        #expect(MonarchConnectedAppsListTokens.nameRole(isSuspended: false) == \MonarchColorTokens.textDefault)
    }

    @Test("Built-in strings resolve")
    func strings() {
        let plain = [
            MonarchConnectedAppsListStrings.title, MonarchConnectedAppsListStrings.description,
            MonarchConnectedAppsListStrings.verified, MonarchConnectedAppsListStrings.suspended,
            MonarchConnectedAppsListStrings.revoke, MonarchConnectedAppsListStrings.confirmMessage,
            MonarchConnectedAppsListStrings.cancel, MonarchConnectedAppsListStrings.loading,
            MonarchConnectedAppsListStrings.emptyTitle, MonarchConnectedAppsListStrings.emptyMessage,
            MonarchConnectedAppsListStrings.errorTitle,
        ]
        #expect(plain.allSatisfy { !$0.hasPrefix("monarch.") })
        #expect(MonarchConnectedAppsListStrings.revokeLabel("Notes") == "Remove access for Notes")
        #expect(MonarchConnectedAppsListStrings.confirmTitle("Notes") == "Remove access for Notes?")
        #expect(MonarchConnectedAppsListStrings.revoked("Notes") == "Removed access for Notes")
        #expect(MonarchConnectedAppsListStrings.scopes("Notes") == "What Notes can access")
    }
}
