import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let memberRowRoles = [
    MonarchRoleOption("owner", title: "Owner"),
    MonarchRoleOption("admin", title: "Admin"),
    MonarchRoleOption("member", title: "Member"),
]

enum MonarchMemberRowFixtures: ComponentFixtures {
    static let id = "member-row"
    static let fixtures = [
        Fixture("editable role and remove") {
            MonarchMemberRow(
                "Jonas Weber",
                image: nil,
                email: "jonas@example.com",
                role: "admin",
                roles: memberRowRoles,
                onRoleChange: { _ in },
                onRemove: {}
            )
        },
        Fixture("self with presence") {
            MonarchMemberRow(
                "Priya Raman",
                image: nil,
                email: "priya@example.com",
                presence: .online,
                role: "owner",
                roles: memberRowRoles,
                isSelf: true,
                onRoleChange: { _ in },
                onRemove: {}
            )
        },
        Fixture("invited with resend, revoke and meta") {
            MonarchMemberRow(
                "lee@example.com",
                image: nil,
                meta: "Invite expires in 6 days",
                status: .invited,
                role: "member",
                roles: memberRowRoles,
                onResend: {},
                onRemove: {}
            )
        },
        Fixture("suspended, read-only role, guest badge") {
            MonarchMemberRow(
                "Sam Carter",
                url: nil,
                email: "sam@example.com",
                status: .suspended,
                role: "member",
                roles: memberRowRoles,
                badges: ["Guest"]
            )
        },
        Fixture("no role set yet") {
            MonarchMemberRow("Ana Lima", image: nil, roles: memberRowRoles, onRoleChange: { _ in })
        },
        Fixture("extra actions") {
            MonarchMemberRow(
                "Amara Okafor",
                image: nil,
                email: "amara@example.com",
                role: "member",
                roles: memberRowRoles,
                onRemove: {},
                actions: {
                    Menu("More for Amara Okafor") { Button("Transfer ownership") {} }
                }
            )
        },
        Fixture("long name and email") {
            MonarchMemberRow(
                "Maximiliane Alexandra von Hohenberg-Lichtenstein",
                image: nil,
                email: "maximiliane.alexandra.von.hohenberg@a-very-long-company-domain.example.com",
                presence: .away,
                role: "a-custom-role-not-in-the-list",
                roles: memberRowRoles
            )
        },
        Fixture("disabled") {
            MonarchMemberRow(
                "Jonas Weber",
                image: nil,
                email: "jonas@example.com",
                role: "admin",
                roles: memberRowRoles,
                onRoleChange: { _ in },
                onRemove: {}
            )
            .disabled(true)
        },
        Fixture("in a list") {
            List {
                MonarchMemberRow("Jonas Weber", image: nil, role: "admin", roles: memberRowRoles, onRemove: {})
                MonarchMemberRow("lee@example.com", image: nil, status: .invited, onResend: {}, onRemove: {})
            }
            .frame(minHeight: MonarchAccessibility.minimumHitTarget * 4)
        },
    ]
}

private struct MemberRowDescribedError: LocalizedError {
    var errorDescription: String? { "Only owners can change roles." }
}

private struct MemberRowPlainError: Error {}

@Suite("MonarchMemberRow")
struct MonarchMemberRowTests {
    private typealias Tokens = MonarchMemberRowTokens
    private let labels = MonarchMemberRowLabels()

    @Test("The role is a picker only with a handler, roles, and permission; not on your own row by default")
    func roleEditable() {
        #expect(Tokens.isRoleEditable(canChangeRole: nil, hasHandler: true, isSelf: false, roleCount: 3))
        #expect(!Tokens.isRoleEditable(canChangeRole: nil, hasHandler: true, isSelf: true, roleCount: 3))
        #expect(Tokens.isRoleEditable(canChangeRole: true, hasHandler: true, isSelf: true, roleCount: 3))
        #expect(!Tokens.isRoleEditable(canChangeRole: false, hasHandler: true, isSelf: false, roleCount: 3))
        #expect(!Tokens.isRoleEditable(canChangeRole: true, hasHandler: false, isSelf: false, roleCount: 3))
        #expect(!Tokens.isRoleEditable(canChangeRole: nil, hasHandler: true, isSelf: false, roleCount: 0))
    }

    @Test("Remove needs a handler and is hidden on your own row by default; resend is for invitations")
    func removeAndResend() {
        #expect(Tokens.isRemovable(canRemove: nil, hasHandler: true, isSelf: false))
        #expect(!Tokens.isRemovable(canRemove: nil, hasHandler: true, isSelf: true))
        #expect(Tokens.isRemovable(canRemove: true, hasHandler: true, isSelf: true))
        #expect(!Tokens.isRemovable(canRemove: false, hasHandler: true, isSelf: false))
        #expect(!Tokens.isRemovable(canRemove: true, hasHandler: false, isSelf: false))
        #expect(Tokens.isResendable(status: .invited, hasHandler: true))
        #expect(!Tokens.isResendable(status: .invited, hasHandler: false))
        #expect(!Tokens.isResendable(status: .active, hasHandler: true))
        #expect(!Tokens.isResendable(status: .suspended, hasHandler: true))
    }

    @Test("The role is shown by its title, or its value when it is not in the list")
    func roleTitle() {
        #expect(Tokens.roleTitle("admin", roles: memberRowRoles) == "Admin")
        #expect(Tokens.roleTitle("billing", roles: memberRowRoles) == "billing")
        #expect(Tokens.roleTitle(nil, roles: memberRowRoles) == nil)
        #expect(Tokens.roleTitle("", roles: memberRowRoles) == nil)
    }

    @Test("Only invited and suspended members get a badge, each with its own tone and words")
    func badges() {
        #expect(Tokens.badge(.active, labels: labels) == nil)
        #expect(Tokens.badge(.invited, labels: labels)?.text == "Invited")
        #expect(Tokens.badge(.invited, labels: labels)?.tone == .info)
        #expect(Tokens.badge(.suspended, labels: labels)?.text == "Suspended")
        #expect(Tokens.badge(.suspended, labels: labels)?.tone == .warning)
        #expect(Tokens.badge(.invited, tone: .accent, labels: labels)?.tone == .accent)
        #expect(Tokens.badge(.suspended, tone: .danger, labels: labels)?.tone == .danger)
        #expect(Tokens.badge(.active, tone: .danger, labels: labels) == nil)
        #expect(Tokens.badge(.invited, labels: MonarchMemberRowLabels(invited: "Eingeladen"))?.text == "Eingeladen")
    }

    @Test("Colours: a suspended name is muted; failures are danger and confirmations success")
    func colours() {
        #expect(Tokens.nameRole(.active) == \MonarchColorTokens.textDefault)
        #expect(Tokens.nameRole(.invited) == \MonarchColorTokens.textDefault)
        #expect(Tokens.nameRole(.suspended) == \MonarchColorTokens.textMuted)
        #expect(Tokens.noticeRole(.error("x")) == \MonarchColorTokens.dangerStrong)
        #expect(Tokens.noticeRole(.done("x")) == \MonarchColorTokens.successStrong)
        #expect(Tokens.Notice.error("x").isError)
        #expect(!Tokens.Notice.done("x").isError)
        #expect(Tokens.Notice.done("Sent").text == "Sent")
    }

    @Test("VoiceOver reads the name, then status, presence, badges, role, email and meta")
    func identity() {
        #expect(Tokens.accessibilityLabel(name: "Jonas Weber", isSelf: false, labels: labels) == "Jonas Weber")
        #expect(Tokens.accessibilityLabel(name: "Priya Raman", isSelf: true, labels: labels) == "Priya Raman, You")
        #expect(Tokens.selfMarker(labels) == "(You)")
        #expect(
            Tokens.accessibilityValue(
                status: .invited,
                presence: .online,
                badges: ["Guest"],
                readOnlyRole: "Member",
                email: "lee@example.com",
                meta: "Invite expires in 6 days",
                labels: labels
            ) == "Invited, Online, Guest, Member, lee@example.com, Invite expires in 6 days"
        )
        #expect(
            Tokens.accessibilityValue(
                status: .active,
                presence: nil,
                badges: [],
                readOnlyRole: nil,
                email: "jonas@example.com",
                meta: "",
                labels: labels
            ) == "jonas@example.com"
        )
        let custom = MonarchMemberRowLabels(presence: { _ in "Da" })
        #expect(
            Tokens.accessibilityValue(
                status: .active,
                presence: .online,
                badges: [],
                readOnlyRole: nil,
                email: nil,
                meta: nil,
                labels: custom
            ) == "Da"
        )
    }

    @Test("Every control is named after the person")
    func controlNames() {
        #expect(Tokens.removeLabel(name: "Jonas Weber", status: .active, labels: labels) == "Remove Jonas Weber")
        #expect(Tokens.removeLabel(name: "Lee", status: .invited, labels: labels) == "Revoke invite for Lee")
        #expect(MonarchMemberRowStrings.roleLabel("Jonas Weber") == "Role for Jonas Weber")
        #expect(MonarchMemberRowStrings.resendLabel("Lee") == "Resend invite, Lee")
        let custom = MonarchMemberRowLabels(remove: { "Entfernen: \($0)" })
        #expect(Tokens.removeLabel(name: "Jonas", status: .active, labels: custom) == "Entfernen: Jonas")
    }

    @Test("The confirmation differs for members and invitations and lists the consequences")
    func confirmation() {
        let member = Tokens.confirmation(
            name: "Jonas Weber",
            status: .active,
            consequences: ["3 assigned tickets go back to the queue", ""],
            labels: labels
        )
        #expect(member.title == "Remove Jonas Weber?")
        #expect(member.confirm == "Remove")
        #expect(
            member.message
                == "They lose access straight away. You can invite them again later.\n3 assigned tickets go back to the queue"
        )
        let invite = Tokens.confirmation(name: "Lee", status: .invited, consequences: [], labels: labels)
        #expect(invite.title == "Revoke the invite for Lee?")
        #expect(invite.confirm == "Revoke invite")
        #expect(invite.message == "The invite link stops working. You can invite them again later.")
        let suspended = Tokens.confirmation(name: "Sam", status: .suspended, consequences: [], labels: labels)
        #expect(suspended.title == "Remove Sam?")
    }

    @Test("A failure is shown in the error's own words when it has them")
    func failures() {
        #expect(Tokens.failureMessage(MemberRowDescribedError(), labels: labels) == "Only owners can change roles.")
        #expect(Tokens.failureMessage(MemberRowPlainError(), labels: labels) == "That did not work. Try again.")
        let custom = MonarchMemberRowLabels(failed: "Fehlgeschlagen")
        #expect(Tokens.failureMessage(MemberRowPlainError(), labels: custom) == "Fehlgeschlagen")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchMemberRowStrings.you == "You")
        #expect(MonarchMemberRowStrings.invited == "Invited")
        #expect(MonarchMemberRowStrings.suspended == "Suspended")
        #expect(MonarchMemberRowStrings.chooseRole == "Choose a role")
        #expect(MonarchMemberRowStrings.resend == "Resend invite")
        #expect(MonarchMemberRowStrings.resent == "Invite sent again.")
        #expect(MonarchMemberRowStrings.remove("A") == "Remove A")
        #expect(MonarchMemberRowStrings.revoke("A") == "Revoke invite for A")
        #expect(MonarchMemberRowStrings.removeTitle("A") == "Remove A?")
        #expect(MonarchMemberRowStrings.revokeTitle("A") == "Revoke the invite for A?")
        #expect(MonarchMemberRowStrings.removeConfirm == "Remove")
        #expect(MonarchMemberRowStrings.revokeConfirm == "Revoke invite")
        #expect(MonarchMemberRowStrings.cancel == "Cancel")
        #expect(MonarchMemberRowStrings.failed == "That did not work. Try again.")
        #expect(!MonarchMemberRowStrings.removeDescription.hasPrefix("monarch."))
        #expect(!MonarchMemberRowStrings.revokeDescription.hasPrefix("monarch."))
    }

    #if os(iOS)
        @Test("A row with controls is at least as tall as the touch target")
        @MainActor
        func hitTarget() {
            let row = MonarchMemberRow("Jonas Weber", image: nil, role: "admin", roles: memberRowRoles, onRemove: {})
            #expect(Host.layout(row).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
