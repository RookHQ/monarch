import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private let spaces = [
    MonarchSpaceOption(id: "eng", name: "Engineering", description: "18 members", systemImage: "hammer"),
    MonarchSpaceOption(id: "mkt", name: "Marketing", description: "9 members"),
    MonarchSpaceOption(id: "ops", name: "Opérations"),
    MonarchSpaceOption(id: "long", name: "Customer success and support for enterprise accounts in Europe"),
]

enum MonarchSpaceSwitcherFixtures: ComponentFixtures {
    static let id = "space-switcher"
    static let fixtures = [
        Fixture("all spaces") {
            MonarchSpaceSwitcher(selection: .constant(nil), spaces: spaces)
        },
        Fixture("one space, every size") {
            VStack(alignment: .leading) {
                MonarchSpaceSwitcher(selection: .constant("eng"), spaces: spaces, size: .sm)
                MonarchSpaceSwitcher(selection: .constant("mkt"), spaces: spaces)
                MonarchSpaceSwitcher(selection: .constant("long"), spaces: spaces, size: .lg)
            }
        },
        Fixture("icon only") {
            MonarchSpaceSwitcher(selection: .constant("eng"), spaces: spaces, isIconOnly: true)
        },
        Fixture("all not offered, nothing chosen") {
            MonarchSpaceSwitcher(selection: .constant(nil), spaces: spaces, allowsAll: false)
        },
        Fixture("empty, shown") {
            MonarchSpaceSwitcher(selection: .constant(nil), spaces: [], hidesWhenEmpty: false)
        },
        Fixture("disabled") {
            MonarchSpaceSwitcher(selection: .constant("eng"), spaces: spaces).disabled(true)
        },
        Fixture("list with actions") {
            MonarchSpaceSwitcherList(
                spaces: spaces,
                selection: "eng",
                allowsAll: true,
                isLoading: false,
                canCreate: true,
                manageURL: URL(string: "https://example.com/spaces"),
                labels: MonarchSpaceSwitcherLabels(),
                onChoose: { _ in },
                onCreate: {},
                onCancel: {}
            )
        },
        Fixture("list loading, without all") {
            MonarchSpaceSwitcherList(
                spaces: [],
                selection: nil,
                allowsAll: false,
                isLoading: true,
                canCreate: false,
                manageURL: nil,
                labels: MonarchSpaceSwitcherLabels(),
                onChoose: { _ in },
                onCreate: {},
                onCancel: {}
            )
        },
    ]
}

@Suite("MonarchSpaceSwitcher")
struct MonarchSpaceSwitcherTests {
    private typealias Tokens = MonarchSpaceSwitcherTokens
    private let labels = MonarchSpaceSwitcherLabels()

    @Test("A selected id that is not in the list is no Space")
    func current() {
        #expect(Tokens.current("mkt", in: spaces)?.name == "Marketing")
        #expect(Tokens.current("gone", in: spaces) == nil)
        #expect(Tokens.current(nil, in: spaces) == nil)
    }

    @Test("The button names the Space, All spaces or the placeholder")
    func triggerName() {
        #expect(Tokens.triggerName(spaces[0], allowsAll: true, labels: labels) == "Engineering")
        #expect(Tokens.triggerName(nil, allowsAll: true, labels: labels) == "All spaces")
        #expect(Tokens.triggerName(nil, allowsAll: false, labels: labels) == "Choose a Space")
        let custom = MonarchSpaceSwitcherLabels(all: "Everything", placeholder: "Pick one")
        #expect(Tokens.triggerName(nil, allowsAll: true, labels: custom) == "Everything")
        #expect(Tokens.triggerName(nil, allowsAll: false, labels: custom) == "Pick one")
    }

    @Test("The button shows the Space's symbol or the layers symbol")
    func triggerSymbol() {
        #expect(Tokens.triggerSymbol(spaces[0]) == "hammer")
        #expect(Tokens.triggerSymbol(spaces[1]) == Tokens.allSymbol)
        #expect(Tokens.triggerSymbol(nil) == Tokens.allSymbol)
    }

    @Test("It hides with no Spaces unless loading or asked to stay")
    func visibility() {
        #expect(!Tokens.isVisible(count: 0, isLoading: false, hidesWhenEmpty: true))
        #expect(Tokens.isVisible(count: 0, isLoading: true, hidesWhenEmpty: true))
        #expect(Tokens.isVisible(count: 0, isLoading: false, hidesWhenEmpty: false))
        #expect(Tokens.isVisible(count: 2, isLoading: false, hidesWhenEmpty: true))
    }

    @Test("All spaces can be chosen only when offered")
    func accepts() {
        #expect(Tokens.accepts(nil, allowsAll: true))
        #expect(!Tokens.accepts(nil, allowsAll: false))
        #expect(Tokens.accepts("eng", allowsAll: false))
    }

    @Test("Search matches the name and description, ignoring case and accents")
    func filter() {
        #expect(Tokens.filter(spaces, query: "").count == spaces.count)
        #expect(Tokens.filter(spaces, query: "  market ").map(\.id) == ["mkt"])
        #expect(Tokens.filter(spaces, query: "operations").map(\.id) == ["ops"])
        #expect(Tokens.filter(spaces, query: "18 MEMBERS").map(\.id) == ["eng"])
        #expect(Tokens.filter(spaces, query: "finance").isEmpty)
    }

    @Test("All spaces is listed only without a search")
    func showsAll() {
        #expect(Tokens.showsAll(allowsAll: true, query: ""))
        #expect(Tokens.showsAll(allowsAll: true, query: "  "))
        #expect(!Tokens.showsAll(allowsAll: true, query: "eng"))
        #expect(!Tokens.showsAll(allowsAll: false, query: ""))
    }

    @Test("A row is read as its name and description")
    func rowLabel() {
        #expect(Tokens.rowLabel(title: "Engineering", description: "18 members") == "Engineering, 18 members")
        #expect(Tokens.rowLabel(title: "Opérations", description: nil) == "Opérations")
        #expect(Tokens.rowLabel(title: "All spaces", description: "") == "All spaces")
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchSpaceSwitcherStrings.label == "Space")
        #expect(MonarchSpaceSwitcherStrings.all == "All spaces")
        for value in [
            MonarchSpaceSwitcherStrings.placeholder, MonarchSpaceSwitcherStrings.search,
            MonarchSpaceSwitcherStrings.empty, MonarchSpaceSwitcherStrings.loading,
            MonarchSpaceSwitcherStrings.create, MonarchSpaceSwitcherStrings.manage,
            MonarchSpaceSwitcherStrings.cancel,
        ] {
            #expect(!value.hasPrefix("monarch."))
        }
    }
}
