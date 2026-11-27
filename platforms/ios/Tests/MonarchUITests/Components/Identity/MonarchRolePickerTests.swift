import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchRolePickerFixtures: ComponentFixtures {
    static let id = "role-picker"
    static let roles = [
        MonarchRoleOption("viewer", title: "Viewer", description: "Can see everything, change nothing."),
        MonarchRoleOption(
            "editor",
            title: "Editor",
            description: "Can edit content and settings of projects they are in.",
            capabilities: ["Edit pages", "Invite guests", "Export data"]
        ),
        MonarchRoleOption(
            "owner",
            title: "Owner",
            description: "Full control, including billing.",
            isDisabled: true,
            disabledReason: "Only owners can make someone an owner."
        ),
    ]

    static let fixtures = [
        Fixture("cards, chosen, with helper") {
            MonarchRolePicker(selection: .constant("editor"), roles: roles, helper: "You can change this later.")
        },
        Fixture("cards, nothing chosen, error") {
            MonarchRolePicker("Access", selection: .constant(nil), roles: roles, error: "Choose a role.")
        },
        Fixture("compact, chosen") {
            MonarchRolePicker(selection: .constant("viewer"), roles: roles, layout: .compact)
        },
        Fixture("compact, nothing chosen") {
            MonarchRolePicker(
                selection: .constant(nil),
                roles: roles,
                layout: .compact,
                placeholder: "Pick one"
            )
        },
        Fixture("disabled") {
            MonarchRolePicker(selection: .constant("viewer"), roles: roles).disabled(true)
        },
    ]
}

@Suite("MonarchRolePicker")
struct MonarchRolePickerTests {
    @Test("A card shows the description, each capability and the reason, one per line")
    func cardDescription() {
        #expect(
            MonarchRolePickerTokens.cardDescription(
                description: "Can edit.",
                capabilities: ["Edit pages", "", "Invite guests"],
                isDisabled: false,
                disabledReason: "Ignored while enabled"
            ) == "Can edit.\n\u{2022}  Edit pages\n\u{2022}  Invite guests"
        )
        #expect(
            MonarchRolePickerTokens.cardDescription(
                description: nil,
                capabilities: [],
                isDisabled: true,
                disabledReason: "Only owners can do this."
            ) == "Only owners can do this."
        )
        #expect(
            MonarchRolePickerTokens.cardDescription(
                description: "",
                capabilities: [],
                isDisabled: false,
                disabledReason: nil
            ) == nil
        )
    }

    @Test("Roles become radio cards in order, keeping disabled ones")
    func options() {
        let options = MonarchRolePickerTokens.cardOptions(MonarchRolePickerFixtures.roles)
        #expect(options.map(\.value) == ["viewer", "editor", "owner"])
        #expect(options.map(\.title) == ["Viewer", "Editor", "Owner"])
        #expect(options.map(\.isDisabled) == [false, false, true])
        #expect(options[2].description == "Full control, including billing.\nOnly owners can make someone an owner.")
    }

    @Test("The compact picker describes the chosen role, or why it is disabled")
    func compact() {
        let roles = MonarchRolePickerFixtures.roles
        let viewer = MonarchRolePickerTokens.selectedRole(roles, selection: "viewer")
        #expect(viewer?.title == "Viewer")
        #expect(MonarchRolePickerTokens.compactDescription(viewer) == "Can see everything, change nothing.")
        let owner = MonarchRolePickerTokens.selectedRole(roles, selection: "owner")
        #expect(MonarchRolePickerTokens.compactDescription(owner) == "Only owners can make someone an owner.")
        #expect(MonarchRolePickerTokens.selectedRole(roles, selection: nil) == nil)
        #expect(MonarchRolePickerTokens.selectedRole(roles, selection: "unknown") == nil)
        #expect(MonarchRolePickerTokens.compactDescription(nil as MonarchRoleOption<String>?) == nil)
    }

    @Test("An error replaces the helper text")
    func message() {
        #expect(MonarchRolePickerTokens.message(helper: "Help", error: nil)! == ("Help", false))
        #expect(MonarchRolePickerTokens.message(helper: "Help", error: "Wrong")! == ("Wrong", true))
        #expect(MonarchRolePickerTokens.message(helper: "", error: "") == nil)
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchRolePickerStrings.label == "Role")
        #expect(MonarchRolePickerStrings.placeholder == "Choose a role")
    }
}
