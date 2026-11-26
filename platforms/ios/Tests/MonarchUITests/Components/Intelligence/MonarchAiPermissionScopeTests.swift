import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let sample = [
    MonarchAiPermissionScopeItem("Calendar", access: .read),
    MonarchAiPermissionScopeItem("Drive", resources: "Shared projects only", access: .write),
    MonarchAiPermissionScopeItem("Mail", access: .none),
]

enum MonarchAiPermissionScopeFixtures: ComponentFixtures {
    static let id = "ai-permission-scope"
    static let fixtures = [
        Fixture("read only, every level, manage") {
            MonarchAiPermissionScope(sample, onManage: {})
        },
        Fixture("editable") {
            MonarchAiPermissionScope(scopes: .constant(sample))
        },
        Fixture("empty") {
            MonarchAiPermissionScope([])
        },
        Fixture("long names, own labels, disabled") {
            MonarchAiPermissionScope(
                scopes: .constant([
                    MonarchAiPermissionScopeItem(
                        "Customer relationship management workspace",
                        resources: "Accounts and opportunities in the north-east region only",
                        access: .read
                    )
                ]),
                labels: MonarchAiPermissionScopeLabels(option: { level in
                    switch level {
                    case .none: "Off"
                    case .read: "View"
                    case .write: "Edit"
                    }
                })
            )
            .disabled(true)
        },
    ]
}

@Suite("MonarchAiPermissionScope")
struct MonarchAiPermissionScopeTests {
    typealias Tokens = MonarchAiPermissionScopeTokens

    @Test("Levels are ordered and write includes read")
    func levels() {
        #expect(MonarchAiAccessLevel.none < .read && MonarchAiAccessLevel.read < .write)
        #expect(MonarchAiAccessLevel.write.includes(.read))
        #expect(MonarchAiAccessLevel.write.includes(.write))
        #expect(MonarchAiAccessLevel.read.includes(.read))
        #expect(!MonarchAiAccessLevel.read.includes(.write))
        #expect(!MonarchAiAccessLevel.none.includes(.read))
        #expect(MonarchAiAccessLevel.none.includes(.none))
    }

    @Test("Updating a row changes only that row")
    func updating() {
        let updated = Tokens.updating(sample, at: 2, to: .write)
        #expect(updated.map(\.access) == [.read, .write, .write])
        #expect(updated[2].app == "Mail")
        #expect(Tokens.updating(sample, at: 9, to: .none) == sample)
        #expect(Tokens.updating(sample, at: -1, to: .none) == sample)
    }

    @Test("Each level has its own symbol; no access is muted")
    func symbols() {
        let symbols = MonarchAiAccessLevel.allCases.map(Tokens.symbol)
        #expect(Set(symbols).count == 3)
        #expect(Tokens.accessRole(.none) == \MonarchColorTokens.textMuted)
        #expect(Tokens.accessRole(.read) == \MonarchColorTokens.textDefault)
        #expect(Tokens.accessRole(.write) == \MonarchColorTokens.textDefault)
    }

    @Test("A read-only row reads the app and resources, with the access as its value")
    func spoken() {
        let labels = MonarchAiPermissionScopeLabels()
        let drive = Tokens.spoken(sample[1], labels: labels)
        #expect(drive.label == "Drive, Shared projects only")
        #expect(drive.value == "Can read and edit")
        let calendar = Tokens.spoken(sample[0], labels: labels)
        #expect(calendar.label == "Calendar")
        #expect(calendar.value == "Can read")
        let empty = Tokens.spoken(MonarchAiPermissionScopeItem("Mail", resources: "", access: .none), labels: labels)
        #expect(empty.label == "Mail")
        #expect(empty.value == "No access")
    }

    @Test("The built-in strings come from the strings table")
    func strings() {
        let labels = MonarchAiPermissionScopeLabels()
        #expect(labels.label == "What the AI can access")
        #expect(MonarchAiAccessLevel.allCases.map(labels.option) == ["None", "Read", "Write"])
        #expect(labels.control("Calendar") == "Access to Calendar")
        #expect(labels.manage == "Manage permissions")
        #expect(labels.empty == "The AI has no access to any app.")
    }

    @Test("Own labels replace the built-in ones")
    func overrides() {
        let labels = MonarchAiPermissionScopeLabels(
            label: "Agent access",
            access: { _ in "x" },
            control: { "Level for \($0)" },
            manage: "Settings",
            empty: "Nothing"
        )
        #expect(labels.label == "Agent access")
        #expect(labels.access(.read) == "x")
        #expect(labels.control("Mail") == "Level for Mail")
        #expect(labels.option(.write) == "Write")
        #expect(labels.manage == "Settings")
        #expect(labels.empty == "Nothing")
    }

    @Test("More rows take more room")
    @MainActor
    func rowHeight() {
        let one = Host.layout(MonarchAiPermissionScope(scopes: .constant(Array(sample.prefix(1)))))
        let three = Host.layout(MonarchAiPermissionScope(scopes: .constant(sample)))
        #expect(three.height > one.height)
    }
}
