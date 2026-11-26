import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchSecurityAlertCardFixtures: ComponentFixtures {
    static let id = "security-alert-card"
    static let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
    static let alerts = [
        MonarchSecurityAlert(
            id: "1",
            title: "Sign-in blocked from an unknown device",
            description: "Someone entered your password from Lagos, Nigeria. We blocked the sign-in.",
            severity: .critical,
            date: now.addingTimeInterval(-120),
            actionTitle: "Change password",
            actionURL: URL(string: "https://example.com/security/password")
        ),
        MonarchSecurityAlert(
            id: "2",
            title: "New sign-in from Firefox on Linux",
            description: "Berlin, Germany",
            severity: .high,
            date: now.addingTimeInterval(-3_600),
            actionTitle: "Review sessions"
        ),
        MonarchSecurityAlert(
            id: "3",
            title: "Two-step verification turned off",
            severity: .medium,
            date: now.addingTimeInterval(-86_400)
        ),
        MonarchSecurityAlert(id: "4", title: "Recovery email changed", severity: .low, isRead: true),
        MonarchSecurityAlert(
            id: "5",
            title: "Password changed",
            severity: .info,
            date: now.addingTimeInterval(-604_800),
            isRead: true
        ),
    ]

    static let fixtures = [
        Fixture("every severity, every action") {
            MonarchSecurityAlertCard(
                alerts,
                now: now,
                onAction: { _ in },
                onMarkRead: { _ in },
                onMarkAllRead: {},
                onDismiss: { _ in }
            )
        },
        Fixture("read only") {
            MonarchSecurityAlertCard(Array(alerts.prefix(3)), now: now)
        },
        Fixture("loading") {
            MonarchSecurityAlertCard([], state: .loading)
        },
        Fixture("empty") {
            MonarchSecurityAlertCard([])
        },
        Fixture("failed, retrying") {
            MonarchSecurityAlertCard([], state: .failed, isRetrying: true, onRetry: {})
        },
        Fixture("custom title and labels") {
            MonarchSecurityAlertCard(
                [alerts[1]],
                title: "Account activity",
                description: "The last 30 days.",
                now: now,
                labels: MonarchSecurityAlertCardLabels(unreadCount: { "\($0) new" }, markRead: "Seen"),
                onMarkRead: { _ in }
            )
        },
    ]
}

@Suite("MonarchSecurityAlertCard")
struct MonarchSecurityAlertCardTests {
    private let english = Locale(identifier: "en_US")
    private let now = MonarchSecurityAlertCardFixtures.now

    @Test("Severities map to tones, icons and badge variants")
    func severities() {
        #expect(MonarchSecurityAlertCardTokens.tone(.info) == .info)
        #expect(MonarchSecurityAlertCardTokens.tone(.low) == .neutral)
        #expect(MonarchSecurityAlertCardTokens.tone(.medium) == .warning)
        #expect(MonarchSecurityAlertCardTokens.tone(.high) == .danger)
        #expect(MonarchSecurityAlertCardTokens.tone(.critical) == .danger)
        #expect(MonarchSecurityAlertCardTokens.badgeVariant(.critical) == .solid)
        #expect(MonarchSecurityAlertCardTokens.badgeVariant(.high) == .subtle)
        #expect(MonarchSecurityAlertCardTokens.symbol(.medium) == "exclamationmark.triangle")
        #expect(MonarchSecurityAlertCardTokens.symbol(.critical) == "xmark.octagon")
        #expect(MonarchSecurityAlertCardTokens.iconRole(.low) == \MonarchColorTokens.textMuted)
        #expect(MonarchSecurityAlertCardTokens.iconRole(.high) == \MonarchColorTokens.dangerStrong)
        let names = MonarchSecurityAlertSeverity.allCases.map { MonarchSecurityAlertCardStrings.severity($0) }
        #expect(names == ["Info", "Low", "Medium", "High", "Critical"])
    }

    @Test("The phase follows the state, and a loaded empty list is the empty state")
    func phases() {
        let alerts = MonarchSecurityAlertCardFixtures.alerts
        #expect(MonarchSecurityAlertCardTokens.phase(.ready, alerts: alerts) == .list)
        #expect(MonarchSecurityAlertCardTokens.phase(.ready, alerts: []) == .empty)
        #expect(MonarchSecurityAlertCardTokens.phase(.loading, alerts: alerts) == .loading)
        #expect(MonarchSecurityAlertCardTokens.phase(.failed, alerts: []) == .failed)
        #expect(MonarchSecurityAlertCardTokens.unreadCount(alerts) == 3)
    }

    @Test("Only http, https and mailto links are opened")
    func safeLinks() {
        let safe = ["https://example.com/a", "http://example.com", "mailto:security@example.com"]
        for link in safe {
            #expect(MonarchSecurityAlertCardTokens.safeURL(URL(string: link)) != nil)
        }
        let unsafe = [
            "javascript:alert(1)", "data:text/html,hi", "file:///etc/passwd", "myapp://reset", "/settings", "https://",
            "mailto:",
        ]
        for link in unsafe {
            #expect(MonarchSecurityAlertCardTokens.safeURL(URL(string: link)) == nil)
        }
        #expect(MonarchSecurityAlertCardTokens.safeURL(nil) == nil)
    }

    @Test("A follow-up is a safe link, else a button with a handler, else nothing")
    func followUps() {
        let link = MonarchSecurityAlert(
            id: "a",
            title: "A",
            actionTitle: "Review",
            actionURL: URL(string: "https://example.com")
        )
        #expect(
            MonarchSecurityAlertCardTokens.followUp(link, hasHandler: false)
                == .link("Review", URL(string: "https://example.com")!)
        )
        let unsafe = MonarchSecurityAlert(
            id: "b",
            title: "B",
            actionTitle: "Review",
            actionURL: URL(string: "javascript:alert(1)")
        )
        #expect(MonarchSecurityAlertCardTokens.followUp(unsafe, hasHandler: true) == .button("Review"))
        #expect(MonarchSecurityAlertCardTokens.followUp(unsafe, hasHandler: false) == nil)
        let none = MonarchSecurityAlert(id: "c", title: "C")
        #expect(MonarchSecurityAlertCardTokens.followUp(none, hasHandler: true) == nil)
    }

    @Test("Times are relative to the reference time")
    func times() {
        #expect(
            MonarchSecurityAlertCardTokens.relativeTime(now.addingTimeInterval(-300), now: now, locale: english)
                == "5 minutes ago"
        )
        #expect(
            MonarchSecurityAlertCardTokens.relativeTime(now.addingTimeInterval(-86_400), now: now, locale: english)
                == "yesterday"
        )
    }

    @Test("An alert is read as its title with unread, severity, time and description as the value")
    func accessibility() {
        #expect(
            MonarchSecurityAlertCardTokens.accessibilityValue(
                unread: "Unread",
                severity: "High",
                time: "5 minutes ago",
                description: "Berlin, Germany"
            ) == "Unread, High, 5 minutes ago, Berlin, Germany"
        )
        #expect(
            MonarchSecurityAlertCardTokens.accessibilityValue(unread: nil, severity: "Info", time: nil, description: "")
                == "Info"
        )
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchSecurityAlertCardStrings.title == "Security alerts")
        #expect(MonarchSecurityAlertCardStrings.unreadCount(3) == "3 unread")
        #expect(MonarchSecurityAlertCardStrings.dismiss("Password changed") == "Dismiss Password changed")
        #expect(MonarchSecurityAlertCardStrings.dismissed("Password changed") == "Dismissed Password changed")
        for text in [
            MonarchSecurityAlertCardStrings.description, MonarchSecurityAlertCardStrings.listLabel,
            MonarchSecurityAlertCardStrings.unread, MonarchSecurityAlertCardStrings.markRead,
            MonarchSecurityAlertCardStrings.markAllRead, MonarchSecurityAlertCardStrings.loading,
            MonarchSecurityAlertCardStrings.emptyTitle, MonarchSecurityAlertCardStrings.emptyDescription,
            MonarchSecurityAlertCardStrings.errorTitle,
        ] {
            #expect(!text.hasPrefix("monarch."))
        }
    }

    @Test("Focus moves to the next alert after a dismissal, else the previous")
    func focus() {
        #expect(MonarchFocusAfterRemoval.target(removedIndex: 0, remaining: ["b", "c"]) == "b")
        #expect(MonarchFocusAfterRemoval.target(removedIndex: 2, remaining: ["a", "b"]) == "b")
        #expect(MonarchFocusAfterRemoval.target(removedIndex: 0, remaining: [String]()) == nil)
    }
}
