import MonarchUI
import SwiftUI

struct MonarchPresenceAvatarsSection: CataloguePage {
    static let title = "Presence avatars"

    @State private var count = 6
    @State private var tapped = ""

    private let people = [
        MonarchPresencePerson(id: "1", name: "Ana Silva", activity: "Editing"),
        MonarchPresencePerson(id: "2", name: "Ben Okafor", status: .away),
        MonarchPresencePerson(id: "3", name: "Chen Wei", status: .busy, customStatus: "In a meeting"),
        MonarchPresencePerson(id: "4", name: "Dara Kim", status: .doNotDisturb),
        MonarchPresencePerson(id: "5", name: "Eli Roth", status: .offline),
        MonarchPresencePerson(id: "6", name: "Fay Ng", activity: "Viewing slide 4"),
    ]

    var body: some View {
        Stepper("People here: \(count)", value: $count, in: 0...people.count)
        CatalogueCaption("Tap the stack to list everyone; tap a person")
        MonarchPresenceAvatars(Array(people.prefix(count)), maxVisible: 3) { person in
            tapped = "Jumped to \(person.name)"
        }
        if !tapped.isEmpty {
            Text(tapped).monarchTextStyle(.caption)
        }
        CatalogueCaption("Everyone shown, not a button")
        MonarchPresenceAvatars(Array(people.prefix(3)), size: .md)
    }
}
