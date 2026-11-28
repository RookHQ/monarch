import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private let workspaces = [
    MonarchWorkspaceOption(id: "acme", name: "Acme", handle: "acme"),
    MonarchWorkspaceOption(id: "rook", name: "Rook Labs", handle: "@rooklabs", keywords: "Rook Research"),
    MonarchWorkspaceOption(id: "bare", name: "Café Nord"),
]

private let many = (1...8).map { MonarchWorkspaceOption(id: "w\($0)", name: "Workspace \($0)", handle: "ws\($0)") }

enum MonarchWorkspaceSwitcherFixtures: ComponentFixtures {
    static let id = "workspace-switcher"
    static let fixtures = [
        Fixture("short list, a menu") {
            MonarchWorkspaceSwitcher(selection: .constant("acme"), workspaces: workspaces)
        },
        Fixture("long list, a sheet, every size") {
            VStack(alignment: .leading) {
                MonarchWorkspaceSwitcher(selection: .constant("w2"), workspaces: many, size: .sm)
                MonarchWorkspaceSwitcher(selection: .constant("w3"), workspaces: many)
                MonarchWorkspaceSwitcher(selection: .constant("w4"), workspaces: many, size: .lg)
            }
        },
        Fixture("nothing chosen") {
            MonarchWorkspaceSwitcher(selection: .constant(nil), workspaces: workspaces)
        },
        Fixture("icon only") {
            MonarchWorkspaceSwitcher(selection: .constant("rook"), workspaces: workspaces, isIconOnly: true)
        },
        Fixture("loading") {
            MonarchWorkspaceSwitcher(selection: .constant(nil), workspaces: [], isLoading: true)
        },
        Fixture("disabled") {
            MonarchWorkspaceSwitcher(selection: .constant("acme"), workspaces: workspaces).disabled(true)
        },
        Fixture("sheet list with actions") {
            MonarchWorkspaceSwitcherList(
                workspaces: many,
                selection: "w1",
                isLoading: false,
                canCreate: true,
                manageURL: URL(string: "https://example.com/workspaces"),
                labels: MonarchWorkspaceSwitcherLabels(),
                onChoose: { _ in },
                onCreate: {},
                onCancel: {}
            )
        },
        Fixture("sheet list loading") {
            MonarchWorkspaceSwitcherList(
                workspaces: [],
                selection: nil,
                isLoading: true,
                canCreate: false,
                manageURL: nil,
                labels: MonarchWorkspaceSwitcherLabels(),
                onChoose: { _ in },
                onCreate: {},
                onCancel: {}
            )
        },
    ]
}

@Suite("MonarchWorkspaceSwitcher")
struct MonarchWorkspaceSwitcherTests {
    private typealias Tokens = MonarchWorkspaceSwitcherTokens
    private let labels = MonarchWorkspaceSwitcherLabels()

    @Test("Up to five loaded workspaces are a menu; more or loading is a sheet")
    func presentation() {
        #expect(Tokens.presentation(count: 0, isLoading: false) == .menu)
        #expect(Tokens.presentation(count: Tokens.menuLimit, isLoading: false) == .menu)
        #expect(Tokens.presentation(count: Tokens.menuLimit + 1, isLoading: false) == .sheet)
        #expect(Tokens.presentation(count: 2, isLoading: true) == .sheet)
    }

    @Test("A selected id that is not in the list is no workspace")
    func current() {
        #expect(Tokens.current("rook", in: workspaces)?.name == "Rook Labs")
        #expect(Tokens.current("gone", in: workspaces) == nil)
        #expect(Tokens.current(nil, in: workspaces) == nil)
    }

    @Test("The button names the workspace or the placeholder")
    func triggerName() {
        #expect(Tokens.triggerName(workspaces[0], labels: labels) == "Acme")
        #expect(Tokens.triggerName(nil, labels: labels) == "Choose a workspace")
        #expect(Tokens.triggerName(nil, labels: MonarchWorkspaceSwitcherLabels(placeholder: "Pick")) == "Pick")
    }

    @Test("A handle shows with one @, and not at all when blank")
    func handleText() {
        #expect(Tokens.handleText(workspaces[0]) == "@acme")
        #expect(Tokens.handleText(workspaces[1]) == "@rooklabs")
        #expect(Tokens.handleText(workspaces[2]) == nil)
        #expect(Tokens.handleText(MonarchWorkspaceOption(id: "x", name: "X", handle: " ")) == nil)
        #expect(Tokens.handleText(nil) == nil)
    }

    @Test("Choosing the current workspace again changes nothing")
    func accepts() {
        #expect(!Tokens.accepts("acme", current: "acme"))
        #expect(Tokens.accepts("rook", current: "acme"))
        #expect(Tokens.accepts("rook", current: nil))
    }

    @Test("Search matches the name, handle and keywords, ignoring case and accents")
    func filter() {
        #expect(Tokens.filter(workspaces, query: "").count == workspaces.count)
        #expect(Tokens.filter(workspaces, query: "ACME").map(\.id) == ["acme"])
        #expect(Tokens.filter(workspaces, query: "research").map(\.id) == ["rook"])
        #expect(Tokens.filter(workspaces, query: "cafe").map(\.id) == ["bare"])
        #expect(Tokens.filter(workspaces, query: "zzz").isEmpty)
    }

    @Test("A workspace is read as its name and handle")
    func spokenName() {
        #expect(Tokens.spokenName(workspaces[0]) == "Acme, @acme")
        #expect(Tokens.spokenName(workspaces[2]) == "Café Nord")
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchWorkspaceSwitcherStrings.label == "Workspace")
        #expect(MonarchWorkspaceSwitcherStrings.create == "Create workspace")
        for value in [
            MonarchWorkspaceSwitcherStrings.placeholder, MonarchWorkspaceSwitcherStrings.search,
            MonarchWorkspaceSwitcherStrings.empty, MonarchWorkspaceSwitcherStrings.loading,
            MonarchWorkspaceSwitcherStrings.manage, MonarchWorkspaceSwitcherStrings.cancel,
        ] {
            #expect(!value.hasPrefix("monarch."))
        }
    }
}
