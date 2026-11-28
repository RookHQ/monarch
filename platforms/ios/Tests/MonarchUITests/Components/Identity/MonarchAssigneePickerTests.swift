import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAssigneePickerFixtures: ComponentFixtures {
    static let id = "assignee-picker"

    private static let people = [
        MonarchAssigneePerson(id: "ana", name: "Ana Silva", email: "ana@example.com", hint: "3 open"),
        MonarchAssigneePerson(id: "sam", name: "Sam Lee", email: "sam@example.com"),
        MonarchAssigneePerson(id: "kim", name: "Kim Park", image: Image(systemName: "person.fill"), isDisabled: true),
    ]

    static let fixtures = [
        Fixture("unassigned, Assign to me shown") {
            MonarchAssigneePicker("Assignee", selection: .constant(nil), people: people, currentUserID: "sam")
        },
        Fixture("assigned to the current user, shortcut hidden") {
            MonarchAssigneePicker("Assignee", selection: .constant("sam"), people: people, currentUserID: "sam")
        },
        Fixture("several assignees") {
            MonarchAssigneePicker(
                "Reviewers",
                selection: .constant(["ana", "kim"]),
                people: people,
                currentUserID: "sam",
                helper: "Reviewers get a notification."
            )
        },
        Fixture("required, error, no Unassigned row, no current user") {
            MonarchAssigneePicker(
                "Owner of the renewal for the enterprise account in the northern region",
                selection: .constant(nil),
                people: people,
                error: "Choose who owns this.",
                isRequired: true,
                allowsUnassigned: false
            )
        },
        Fixture("loading, assigned person outside the list") {
            MonarchAssigneePicker(
                "Assignee",
                selection: .constant("lee"),
                people: [],
                selectedPeople: [MonarchAssigneePerson(id: "lee", name: "Lee Moreau")],
                isLoading: true
            )
        },
        Fixture("disabled") {
            MonarchAssigneePicker("Assignee", selection: .constant(nil), people: people, currentUserID: "sam")
                .disabled(true)
        },
    ]
}

@Suite("MonarchAssigneePicker")
struct MonarchAssigneePickerTests {
    private typealias Tokens = MonarchAssigneePickerTokens

    private let people = [
        MonarchAssigneePerson(id: "ana", name: "Ana Silva", email: "ana@example.com", hint: "3 open"),
        MonarchAssigneePerson(id: "sam", name: "Sam Lee"),
        MonarchAssigneePerson(
            id: "kim",
            name: "Kim Park",
            url: URL(string: "https://example.com/kim.png"),
            isDisabled: true
        ),
    ]

    @Test("The current user is listed first; the others keep their order")
    func ordered() {
        #expect(Tokens.ordered(people, currentUserID: "kim").map(\.id) == ["kim", "ana", "sam"])
        #expect(Tokens.ordered(people, currentUserID: "ana").map(\.id) == ["ana", "sam", "kim"])
        #expect(Tokens.ordered(people, currentUserID: nil).map(\.id) == ["ana", "sam", "kim"])
        #expect(Tokens.ordered(people, currentUserID: "nobody").map(\.id) == ["ana", "sam", "kim"])
    }

    @Test("The current user's name says (you), and the text can be replaced")
    func displayName() {
        let none = MonarchAssigneePickerLabels()
        #expect(Tokens.displayName(people[1], currentUserID: "sam", labels: none) == "Sam Lee (you)")
        #expect(Tokens.displayName(people[0], currentUserID: "sam", labels: none) == "Ana Silva")
        let custom = MonarchAssigneePickerLabels(you: { "\($0) – me" })
        #expect(Tokens.displayName(people[1], currentUserID: "sam", labels: custom) == "Sam Lee – me")
    }

    @Test("A person becomes an entity with the email, hint, search words, avatar and state")
    func entity() {
        let none = MonarchAssigneePickerLabels()
        let ana = Tokens.entity(people[0], currentUserID: nil, labels: none)
        #expect(ana.id == "ana")
        #expect(ana.name == "Ana Silva")
        #expect(ana.description == "ana@example.com")
        #expect(ana.hint == "3 open")
        #expect(ana.keywords == "Ana Silva ana@example.com")
        #expect(ana.isDisabled == false)
        let kim = Tokens.entity(people[2], currentUserID: "kim", labels: none)
        #expect(kim.name == "Kim Park (you)")
        #expect(kim.keywords == "Kim Park")
        #expect(kim.url == URL(string: "https://example.com/kim.png"))
        #expect(kim.isDisabled)
        let withImage = Tokens.entity(
            MonarchAssigneePerson(id: "x", name: "X", image: Image(systemName: "person")),
            currentUserID: nil,
            labels: none
        )
        #expect(withImage.image != nil)
        #expect(withImage.url == nil)
        #expect(
            Tokens.entities(people, currentUserID: "sam", labels: none).map(\.name) == [
                "Sam Lee (you)", "Ana Silva", "Kim Park",
            ]
        )
    }

    @Test("Assign to me shows only for a current user who is not assigned, while enabled")
    func showsAssignToMe() {
        #expect(Tokens.showsAssignToMe(currentUserID: "sam", selection: [], isEnabled: true))
        #expect(Tokens.showsAssignToMe(currentUserID: "sam", selection: ["ana"], isEnabled: true))
        #expect(!Tokens.showsAssignToMe(currentUserID: "sam", selection: ["ana", "sam"], isEnabled: true))
        #expect(!Tokens.showsAssignToMe(currentUserID: nil, selection: [], isEnabled: true))
        #expect(!Tokens.showsAssignToMe(currentUserID: "sam", selection: [], isEnabled: false))
    }

    @Test("Assign to me adds the current user once, at the end")
    func assigningMe() {
        #expect(Tokens.assigningMe(["ana"], currentUserID: "sam") == ["ana", "sam"])
        #expect(Tokens.assigningMe(["sam", "ana"], currentUserID: "sam") == ["sam", "ana"])
        #expect(Tokens.assigningMe([], currentUserID: "sam") == ["sam"])
    }

    @Test("The search placeholder and Unassigned are built in and can be replaced")
    func labels() {
        let none = MonarchAssigneePickerLabels()
        #expect(Tokens.pickerLabels(none).search == "Assign to…")
        #expect(Tokens.pickerLabels(MonarchAssigneePickerLabels(search: "Find")).search == "Find")
        #expect(
            Tokens.pickerLabels(
                MonarchAssigneePickerLabels(search: "Find", picker: MonarchEntityPickerLabels(search: "Look"))
            )
            .search == "Look"
        )
        #expect(Tokens.unassigned(none) == "Unassigned")
        #expect(Tokens.unassigned(MonarchAssigneePickerLabels(unassigned: "Nobody")) == "Nobody")
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchAssigneePickerStrings.unassigned == "Unassigned")
        #expect(MonarchAssigneePickerStrings.assignToMe == "Assign to me")
        #expect(MonarchAssigneePickerStrings.assigned == "Assigned to you")
        #expect(MonarchAssigneePickerStrings.search == "Assign to…")
        #expect(MonarchAssigneePickerStrings.you("Ana") == "Ana (you)")
    }
}
