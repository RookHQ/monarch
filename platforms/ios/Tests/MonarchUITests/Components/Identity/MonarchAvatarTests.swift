import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private struct Person: Identifiable {
    let id: Int
    let name: String
}

private let people = [
    Person(id: 1, name: "Ana Silva"),
    Person(id: 2, name: "Ben Okafor"),
    Person(id: 3, name: "Chen Wei"),
    Person(id: 4, name: "Dara Kim"),
    Person(id: 5, name: "Eli Roth"),
]

enum MonarchAvatarFixtures: ComponentFixtures {
    static let id = "avatar"
    static let fixtures = [
        Fixture("sizes with initials") {
            HStack {
                ForEach(MonarchAvatarSize.allCases, id: \.self) { size in
                    MonarchAvatar(image: nil, name: "Ana Silva", size: size)
                }
            }
        },
        Fixture("every presence") {
            HStack {
                ForEach(MonarchPresenceStatus.allCases, id: \.self) { status in
                    MonarchAvatar(image: nil, name: "Ben Okafor", size: .lg, status: status)
                }
            }
        },
        Fixture("square entity with a symbol") {
            HStack {
                MonarchAvatar(image: nil, name: "Rook Labs", systemImage: "building.2", shape: .square, size: .lg)
                MonarchAvatar(image: nil, name: "Rook Labs", shape: .square, size: .sm, status: .away)
            }
        },
        Fixture("image, URL and no letters") {
            HStack {
                MonarchAvatar(image: Image(systemName: "photo"), name: "Chen Wei")
                MonarchAvatar(url: nil, name: "Dara Kim")
                MonarchAvatar(image: nil, name: "", status: .offline)
                MonarchAvatar(image: nil, name: "😀 🎉")
            }
        },
        Fixture("group with overflow") {
            MonarchAvatarGroup("Assignees", people, maxVisible: 3) { person in
                MonarchAvatar(image: nil, name: person.name)
            }
        },
        Fixture("square group, small, all shown") {
            MonarchAvatarGroup("Workspaces", people.prefix(4), size: .sm, shape: .square) { person in
                MonarchAvatar(image: nil, name: person.name, shape: .square, size: .sm)
            }
        },
    ]
}

@Suite("MonarchAvatar")
struct MonarchAvatarTests {
    @Test("Initials take the first and last words, whole characters, upper-cased")
    func initials() {
        #expect(MonarchAvatarTokens.initials("Ana Silva") == "AS")
        #expect(MonarchAvatarTokens.initials("ana maria da silva") == "AS")
        #expect(MonarchAvatarTokens.initials("  Chen  ") == "C")
        #expect(MonarchAvatarTokens.initials("") == "")
        #expect(MonarchAvatarTokens.initials("élodie Ørsted") == "ÉØ")
        #expect(MonarchAvatarTokens.initials("👩🏽‍💻 Dev") == "👩🏽‍💻D")
    }

    @Test("Initials without a letter or digit fall back to the symbol")
    func readableInitials() {
        #expect(MonarchAvatarTokens.hasReadableInitials("AS"))
        #expect(MonarchAvatarTokens.hasReadableInitials("7"))
        #expect(!MonarchAvatarTokens.hasReadableInitials(""))
        #expect(!MonarchAvatarTokens.hasReadableInitials("😀🎉"))
    }

    @Test("The fallback colour is stable per seed and matches the web's hash")
    func palette() {
        // The web: h = (h * 31 + code) % 360 over the UTF-16 units, then % 5.
        #expect(MonarchAvatarTokens.paletteIndex(seed: "") == 0)
        #expect(MonarchAvatarTokens.paletteIndex(seed: "A") == 65 % 5)
        #expect(MonarchAvatarTokens.paletteIndex(seed: "Ab") == ((65 * 31 + 98) % 360) % 5)
        #expect(
            MonarchAvatarTokens.paletteIndex(seed: "Ana Silva") == MonarchAvatarTokens.paletteIndex(seed: "Ana Silva")
        )
        let indices = Set(["Ana", "Ben", "Chen", "Dara", "Eli", "Fay", "Gus"].map(MonarchAvatarTokens.paletteIndex))
        #expect(indices.count > 1)
        #expect(indices.allSatisfy { (0..<MonarchAvatarTokens.paletteCount).contains($0) })
    }

    @Test("Fallback colours are subtle fills with the strong text of the same family")
    func fallbackColors() {
        #expect(MonarchAvatarTokens.fallback(index: 0) == .init(background: \.accentSubtle, foreground: \.accentStrong))
        #expect(
            MonarchAvatarTokens.fallback(index: 3) == .init(background: \.warningSubtle, foreground: \.warningStrong)
        )
        #expect(MonarchAvatarTokens.fallback(index: 4) == .init(background: \.aiSubtle, foreground: \.aiStrong))
        let all = (0..<MonarchAvatarTokens.paletteCount).map { MonarchAvatarTokens.fallback(index: $0) }
        #expect(Set(all.map(\.background)).count == MonarchAvatarTokens.paletteCount)
    }

    @Test("VoiceOver reads the name and the presence")
    func accessibilityLabel() {
        #expect(MonarchAvatarTokens.accessibilityLabel(name: "Ana Silva", status: nil, statusLabel: nil) == "Ana Silva")
        let online = MonarchAvatarTokens.accessibilityLabel(name: "Ana Silva", status: .online, statusLabel: nil)
        #expect(online == "Ana Silva, \(MonarchPresenceDot.label(for: .online))")
        #expect(
            MonarchAvatarTokens.accessibilityLabel(name: "Ana", status: .busy, statusLabel: "Occupée") == "Ana, Occupée"
        )
    }

    @Test("A group never shows a +1 tile")
    func visibleCount() {
        #expect(MonarchAvatarTokens.visibleCount(total: 5, maxVisible: nil) == 5)
        #expect(MonarchAvatarTokens.visibleCount(total: 5, maxVisible: 3) == 3)
        #expect(MonarchAvatarTokens.visibleCount(total: 4, maxVisible: 3) == 4)
        #expect(MonarchAvatarTokens.visibleCount(total: 2, maxVisible: 3) == 2)
        #expect(MonarchAvatarTokens.visibleCount(total: 3, maxVisible: -1) == 0)
    }

    @Test("Sizes grow and the presence dot follows")
    func sizes() {
        #expect(MonarchAvatarTokens.side(.sm) < MonarchAvatarTokens.side(.md))
        #expect(MonarchAvatarTokens.side(.md) < MonarchAvatarTokens.side(.lg))
        #expect(MonarchAvatarTokens.dotSize(.sm) == .sm)
        #expect(MonarchAvatarTokens.dotSize(.lg) == .lg)
    }

    @Test("A 20 or 24 pt avatar overlaps an eighth and moves its dot out; larger ones keep the quarter")
    func smallAvatarsKeepTheirInitials() {
        #expect(MonarchAvatarTokens.overlapRatio(.xs) == 0.125)
        #expect(MonarchAvatarTokens.overlapRatio(.sm) == 0.125)
        #expect(MonarchAvatarTokens.overlapRatio(.md) == MonarchAvatarTokens.overlapRatio)
        #expect(MonarchAvatarTokens.dotOutset(.sm) > 0)
        #expect(MonarchAvatarTokens.dotOutset(.lg) == 0)
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchAvatarStrings.more(3) == "3 more")
        #expect(MonarchAvatarStrings.moreShort(3) == "+3")
        #expect(MonarchAvatarStrings.withStatus(name: "A", status: "B") == "A, B")
    }
}

@Suite("MonarchAvatar sizes and group label")
@MainActor
struct MonarchAvatarSizeTests {
    @Test("Five sizes, from 20 to 56 points, each larger than the last")
    func sizes() {
        let sides = MonarchAvatarSize.allCases.map(MonarchAvatarTokens.side)
        #expect(sides == [20, 24, 32, 40, 56])
    }

    @Test("A group without a label is named by its number of people")
    func groupLabel() {
        #expect(MonarchAvatarTokens.groupLabel(nil, count: 1) == "1 person")
        #expect(MonarchAvatarTokens.groupLabel(nil, count: 4) == "4 people")
        #expect(MonarchAvatarTokens.groupLabel("", count: 2) == "2 people")
        #expect(MonarchAvatarTokens.groupLabel("Assignees", count: 2) == "Assignees")
        let names = ["Ana Silva", "Ben Ode"]
        let group = MonarchAvatarGroup(names, id: \.self, size: .xs) { MonarchAvatar(image: nil, name: $0, size: .xs) }
        #expect(Host.layout(group).height > 0)
    }
}
