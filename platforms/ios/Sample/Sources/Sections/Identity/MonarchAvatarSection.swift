import MonarchUI
import SwiftUI

private struct Person: Identifiable {
    let id: Int
    let name: String
}

struct MonarchAvatarSection: CataloguePage {
    static let title = "Avatar"

    @State private var status = MonarchPresenceStatus.online
    @State private var maxVisible = 3

    private let people = [
        Person(id: 1, name: "Ana Silva"),
        Person(id: 2, name: "Ben Okafor"),
        Person(id: 3, name: "Chen Wei"),
        Person(id: 4, name: "Dara Kim"),
        Person(id: 5, name: "Eli Roth"),
        Person(id: 6, name: "Fay Moreau"),
    ]

    var body: some View {
        CatalogueCaption("Sizes, initials on a colour stable per name")
        HStack {
            ForEach(MonarchAvatarSize.allCases, id: \.self) { size in
                MonarchAvatar(image: nil, name: "Ana Silva", size: size)
            }
        }

        CatalogueCaption("Presence")
        Picker("Presence", selection: $status) {
            ForEach(MonarchPresenceStatus.allCases, id: \.self) { status in
                Text(MonarchPresenceDot.label(for: status)).tag(status)
            }
        }
        MonarchAvatar(image: nil, name: "Ben Okafor", size: .lg, status: status)

        CatalogueCaption("Entities: a square with a symbol")
        HStack {
            MonarchAvatar(image: nil, name: "Rook Labs", systemImage: "building.2", shape: .square, size: .lg)
            MonarchAvatar(image: nil, name: "Rook Labs", shape: .square, size: .lg)
        }

        CatalogueCaption("A name written beside it: decorative")
        HStack {
            MonarchAvatar(image: nil, name: "Chen Wei", isDecorative: true)
            Text("Chen Wei").monarchTextStyle(.body)
        }

        CatalogueCaption("Group, at most \(maxVisible)")
        Stepper("Show at most \(maxVisible)", value: $maxVisible, in: 0...6)
        MonarchAvatarGroup("Assignees", people, maxVisible: maxVisible) { person in
            MonarchAvatar(image: nil, name: person.name)
        }

        CatalogueCaption("Extra small and extra large; a group read as its number of people")
        HStack {
            MonarchAvatar(image: nil, name: "Ana Silva", size: .xs)
            MonarchAvatar(image: nil, name: "Ana Silva", size: .xl)
        }
        MonarchAvatarGroup(people, size: .xs) { person in
            MonarchAvatar(image: nil, name: person.name, size: .xs)
        }
    }
}
