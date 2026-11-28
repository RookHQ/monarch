import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let now = Date(timeIntervalSinceReferenceDate: 800_000_000)

private let invites = [
    MonarchPendingInvite(
        id: "ana",
        email: "ana@example.com",
        role: "Member",
        invitedBy: "Sam Rivera",
        sentAt: now.addingTimeInterval(-86_400 * 2),
        expiresAt: now.addingTimeInterval(86_400 * 5),
        link: "https://example.com/join/ana"
    ),
    MonarchPendingInvite(
        id: "bo",
        email: "bo@example.com",
        role: "Admin",
        invitedBy: "Sam Rivera",
        sentAt: now.addingTimeInterval(-86_400 * 9),
        expiresAt: now.addingTimeInterval(-86_400 * 2),
        link: "https://example.com/join/bo"
    ),
    MonarchPendingInvite(
        id: "long",
        email: "a.very.long.address.that.has.to.wrap.or.truncate@subdomain.example.com"
    ),
]

enum MonarchPendingInvitesListFixtures: ComponentFixtures {
    static let id = "pending-invites-list"
    static let fixtures = [
        Fixture("active, expired and bare, every action") {
            MonarchPendingInvitesList(invites, now: now, onResend: { _ in }, onRevoke: { _ in })
        },
        Fixture("read only") {
            MonarchPendingInvitesList(invites, now: now)
        },
        Fixture("resend only, with a description") {
            MonarchPendingInvitesList(
                Array(invites.prefix(2)),
                title: "Invitations",
                description: "People who have not joined yet.",
                now: now,
                onResend: { _ in }
            )
        },
        Fixture("loading") {
            MonarchPendingInvitesList([], state: .loading)
        },
        Fixture("empty") {
            MonarchPendingInvitesList([])
        },
        Fixture("error, retrying") {
            MonarchPendingInvitesList([], state: .error, isRetrying: true, onRetry: {})
        },
        Fixture("custom labels") {
            MonarchPendingInvitesList(
                [invites[1]],
                now: now,
                labels: MonarchPendingInvitesListLabels(expired: "Lapsed"),
                onRevoke: { _ in }
            )
        },
    ]
}

@Suite("MonarchPendingInvitesList")
struct MonarchPendingInvitesListTests {
    private typealias Tokens = MonarchPendingInvitesListTokens
    private let labels = MonarchPendingInvitesListLabels()
    private let locale = Locale(identifier: "en_US")

    @Test("A loaded empty list is the empty state; other states pass through")
    func phase() {
        #expect(Tokens.phase(.ready, invites: invites) == .ready)
        #expect(Tokens.phase(.ready, invites: []) == .empty)
        #expect(Tokens.phase(.loading, invites: []) == .loading)
        #expect(Tokens.phase(.error, invites: invites) == .error)
    }

    @Test("Expiry comes from the flag, else from expiresAt against now")
    func expiry() {
        #expect(!Tokens.isExpired(invites[0], now: now))
        #expect(Tokens.isExpired(invites[1], now: now))
        #expect(!Tokens.isExpired(invites[2], now: now))
        let exact = MonarchPendingInvite(id: "x", email: "x@example.com", expiresAt: now)
        #expect(Tokens.isExpired(exact, now: now))
        let flagged = MonarchPendingInvite(id: "y", email: "y@example.com", expiresAt: now, isExpired: false)
        #expect(!Tokens.isExpired(flagged, now: now))
    }

    @Test("The detail line names the sender, the sent time and the expiry")
    func detail() {
        let line = Tokens.detail(invites[0], isExpired: false, now: now, locale: locale, labels: labels)
        #expect(line == "Invited by Sam Rivera · Sent 2 days ago · Expires in 5 days")
        let expired = Tokens.detail(invites[1], isExpired: true, now: now, locale: locale, labels: labels)
        #expect(expired == "Invited by Sam Rivera · Sent last week")
        #expect(Tokens.detail(invites[2], isExpired: false, now: now, locale: locale, labels: labels) == nil)
        let custom = MonarchPendingInvitesListLabels(invitedBy: "From", sent: "Mailed")
        let customLine = Tokens.detail(invites[1], isExpired: true, now: now, locale: locale, labels: custom)
        #expect(customLine == "From Sam Rivera · Mailed last week")
    }

    @Test("Copy link needs a link and an invitation that has not expired")
    func copyableLink() {
        #expect(Tokens.copyableLink(invites[0], isExpired: false) == "https://example.com/join/ana")
        #expect(Tokens.copyableLink(invites[1], isExpired: true) == nil)
        #expect(Tokens.copyableLink(invites[2], isExpired: false) == nil)
        let blank = MonarchPendingInvite(id: "b", email: "b@example.com", link: "  ")
        #expect(Tokens.copyableLink(blank, isExpired: false) == nil)
    }

    @Test("An expired row's status badge reads Expired; other row strings pass through")
    func rowLabels() {
        #expect(Tokens.rowLabels(isExpired: false, labels: labels).invited == nil)
        #expect(Tokens.rowLabels(isExpired: true, labels: labels).invited == "Expired")
        let custom = MonarchPendingInvitesListLabels(
            expired: "Lapsed",
            row: MonarchMemberRowLabels(invited: "Waiting", resend: "Send again")
        )
        #expect(Tokens.rowLabels(isExpired: true, labels: custom).invited == "Lapsed")
        #expect(Tokens.rowLabels(isExpired: false, labels: custom).invited == "Waiting")
        #expect(Tokens.rowLabels(isExpired: true, labels: custom).resend == "Send again")
    }

    @Test("Spoken copy target and revoke announcement name the email")
    func spokenText() {
        #expect(Tokens.copyTarget("ana@example.com", labels: labels) == "for ana@example.com")
        let revoked = Tokens.revokedAnnouncement("ana@example.com", labels: labels)
        #expect(revoked == "Invitation to ana@example.com revoked")
        let custom = MonarchPendingInvitesListLabels(
            copyTarget: { "invite link of \($0)" },
            revoked: { "Cancelled \($0)" }
        )
        #expect(Tokens.copyTarget("bo@example.com", labels: custom) == "invite link of bo@example.com")
        #expect(Tokens.revokedAnnouncement("bo@example.com", labels: custom) == "Cancelled bo@example.com")
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchPendingInvitesListStrings.title == "Pending invitations")
        #expect(MonarchPendingInvitesListStrings.expired == "Expired")
        for value in [
            MonarchPendingInvitesListStrings.listLabel, MonarchPendingInvitesListStrings.invitedBy,
            MonarchPendingInvitesListStrings.sent, MonarchPendingInvitesListStrings.expires,
            MonarchPendingInvitesListStrings.copyLink, MonarchPendingInvitesListStrings.loading,
            MonarchPendingInvitesListStrings.emptyTitle, MonarchPendingInvitesListStrings.errorTitle,
        ] {
            #expect(!value.hasPrefix("monarch."))
        }
    }
}
