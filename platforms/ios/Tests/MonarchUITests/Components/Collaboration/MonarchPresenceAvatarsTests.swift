import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private let people = [
    MonarchPresencePerson(id: "1", name: "Ana Silva", activity: "Editing"),
    MonarchPresencePerson(id: "2", name: "Ben Okafor", status: .away),
    MonarchPresencePerson(id: "3", name: "Chen Wei", status: .busy, customStatus: "In a meeting"),
    MonarchPresencePerson(id: "4", name: "Dara Kim", status: .doNotDisturb),
    MonarchPresencePerson(id: "5", name: "Eli Roth", status: .offline),
    MonarchPresencePerson(id: "6", name: "Fay Ng", activity: "Viewing slide 4"),
]

enum MonarchPresenceAvatarsFixtures: ComponentFixtures {
    static let id = "presence-avatars"
    static let fixtures = [
        Fixture("all shown, not a button") {
            MonarchPresenceAvatars(Array(people.prefix(3)))
        },
        Fixture("overflow, every presence") {
            MonarchPresenceAvatars(people, maxVisible: 3)
        },
        Fixture("sizes, tappable") {
            VStack(alignment: .leading) {
                ForEach(MonarchAvatarSize.allCases, id: \.self) { size in
                    MonarchPresenceAvatars(people, size: size) { _ in }
                }
            }
        },
        Fixture("one person") {
            MonarchPresenceAvatars(Array(people.prefix(1)))
        },
        Fixture("nobody") {
            MonarchPresenceAvatars([])
        },
        Fixture("the list in the sheet") {
            MonarchPresenceAvatarsList(people: people, labels: MonarchPresenceAvatarsLabels(), onPersonTap: { _ in }) {}
        },
        Fixture("the list, long names, not tappable") {
            MonarchPresenceAvatarsList(
                people: [
                    MonarchPresencePerson(
                        id: "x",
                        name: "Maximilian Alexander von Hohenberg-Lichtenstein",
                        customStatus: "Working from the Lisbon office this week",
                        activity: "Editing the quarterly planning document"
                    )
                ],
                labels: MonarchPresenceAvatarsLabels(),
                onPersonTap: nil
            ) {}
        },
    ]
}

@Suite("MonarchPresenceAvatars")
@MainActor
struct MonarchPresenceAvatarsTests {
    typealias Tokens = MonarchPresenceAvatarsTokens
    let labels = MonarchPresenceAvatarsLabels()

    @Test("A person is read with their presence, custom status and activity")
    func personText() {
        #expect(Tokens.personText(people[0], labels: labels) == "Ana Silva, Online, Editing")
        #expect(Tokens.personText(people[1], labels: labels) == "Ben Okafor, Away")
        #expect(Tokens.personText(people[2], labels: labels).hasPrefix("Chen Wei, "))
        #expect(Tokens.personText(people[2], labels: labels).contains("In a meeting"))
    }

    @Test("Every presence is spoken differently, so the dot colour is never needed")
    func presencesDiffer() {
        let spoken = MonarchPresenceStatus.allCases.map {
            Tokens.spokenStatus(MonarchPresencePerson(id: "a", name: "A", status: $0), labels: labels)
        }
        #expect(Set(spoken).count == MonarchPresenceStatus.allCases.count)
    }

    @Test("The second line in the list joins presence and activity")
    func detail() {
        #expect(Tokens.detail(people[0], labels: labels) == "Online · Editing")
        #expect(Tokens.detail(people[1], labels: labels) == "Away")
        let blank = MonarchPresencePerson(id: "b", name: "B", activity: "  ")
        #expect(Tokens.detail(blank, labels: labels) == "Online")
    }

    @Test("The stack is a button when people are hidden or a person can be tapped")
    func opensList() {
        #expect(!Tokens.opensList(total: 3, maxVisible: 4, hasPersonAction: false))
        #expect(!Tokens.opensList(total: 5, maxVisible: 4, hasPersonAction: false))
        #expect(Tokens.opensList(total: 6, maxVisible: 4, hasPersonAction: false))
        #expect(Tokens.opensList(total: 2, maxVisible: 4, hasPersonAction: true))
    }

    @Test("The stack's name counts people and its value names those shown, then the rest")
    func labelAndValue() {
        #expect(Tokens.label(count: 1, labels: labels) == "1 person here")
        #expect(Tokens.label(count: 6, labels: labels) == "6 people here")
        let value = Tokens.value(people, maxVisible: 2, labels: labels)
        #expect(value == "Ana Silva, Online, Editing; Ben Okafor, Away; 4 more")
    }

    @Test("Voice Control accepts the initials shown when there is no picture")
    func inputLabels() {
        #expect(Tokens.inputLabels(people[0]) == ["Ana Silva", "AS"])
        let pictured = MonarchPresencePerson(
            id: "p",
            name: "Ana Silva",
            avatarURL: URL(string: "https://example.com/a.png")
        )
        #expect(Tokens.inputLabels(pictured) == ["Ana Silva"])
    }

    @Test("Overrides replace the built-in strings")
    func overrides() {
        let custom = MonarchPresenceAvatarsLabels(
            label: { "\($0) hier" },
            status: { presence, activity in [presence, activity].compactMap { $0 }.joined(separator: " / ") },
            detail: { presence, _ in presence.uppercased() }
        )
        #expect(Tokens.label(count: 3, labels: custom) == "3 hier")
        #expect(Tokens.personText(people[0], labels: custom) == "Ana Silva, Online / Editing")
        #expect(Tokens.detail(people[1], labels: custom) == "AWAY")
    }

    @Test("A person's colour seed defaults to the id")
    func seed() {
        #expect(MonarchPresencePerson(id: "u1", name: "Ana").seed == "u1")
        #expect(MonarchPresencePerson(id: "u1", name: "Ana", seed: "s").seed == "s")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchPresenceAvatarsStrings.showAll == "Shows everyone here")
        #expect(MonarchPresenceAvatarsStrings.list == "Everyone here")
        #expect(MonarchPresenceAvatarsStrings.done == "Done")
        #expect(MonarchPresenceAvatarsStrings.status("Online", "Editing") == "Online, Editing")
    }

    #if os(iOS)
        @Test("A stack that opens the list is at least 44 points high")
        func touchTarget() {
            let size = Host.layout(MonarchPresenceAvatars(people, maxVisible: 2))
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
