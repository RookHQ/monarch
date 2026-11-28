import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let inProgress = MonarchStatusDefinition("In progress", tone: .info, systemImage: "circle.lefthalf.filled")
private let dependencyItems = [
    MonarchDependencyItem(
        id: "1",
        key: "STR-12",
        title: "Fix login",
        status: inProgress,
        url: URL(string: "https://example.com/issues/12")
    ),
    MonarchDependencyItem(id: "2", key: "STR-14", title: "Update the pricing page"),
    MonarchDependencyItem(id: "3", title: "Write the launch post", url: URL(string: "javascript:alert(1)")),
    MonarchDependencyItem(id: "self", key: "STR-10", title: "This issue"),
]
private let dependencyLinks = [
    MonarchDependencyLink(id: "l1", relation: .blockedBy, item: dependencyItems[0]),
    MonarchDependencyLink(id: "l2", relation: .relates, item: dependencyItems[2]),
    MonarchDependencyLink(id: "l3", relation: .blockedBy, item: dependencyItems[1]),
]

enum MonarchDependencyPickerFixtures: ComponentFixtures {
    static let id = "dependency-picker"
    static let fixtures = [
        Fixture("links and add controls") {
            MonarchDependencyPicker(
                links: dependencyLinks,
                items: dependencyItems,
                excludedIDs: ["self"],
                onAdd: { _, _ in },
                onRemove: { _ in }
            )
        },
        Fixture("empty, searching, two relations") {
            MonarchDependencyPicker(
                links: [],
                items: [],
                relations: [.blocks, .relates],
                defaultRelation: .relates,
                isSearching: true,
                onAdd: { _, _ in }
            )
        },
        Fixture("read only, long title") {
            MonarchDependencyPicker(
                links: [
                    MonarchDependencyLink(
                        id: "x",
                        relation: .duplicatedBy,
                        item: MonarchDependencyItem(
                            id: "x",
                            key: "PLATFORM-12345",
                            title: "Investigate the intermittent timeout in the nightly export of large workspaces",
                            status: inProgress
                        )
                    )
                ],
                isReadOnly: true
            )
        },
        Fixture("disabled") {
            MonarchDependencyPicker(
                links: dependencyLinks,
                items: dependencyItems,
                onAdd: { _, _ in },
                onRemove: { _ in }
            )
            .disabled(true)
        },
    ]
}

@Suite("MonarchDependencyPicker")
struct MonarchDependencyPickerTests {
    private let text = MonarchDependencyPickerText(labels: MonarchDependencyPickerLabels())

    @Test("Links are grouped by relation in the offered order, empty groups left out")
    func groups() {
        let groups = MonarchDependencyPickerTokens.groups(
            dependencyLinks,
            relations: MonarchDependencyRelation.allCases
        )
        #expect(groups.map(\.relation) == [.blockedBy, .relates])
        #expect(groups[0].links.map(\.id) == ["l1", "l3"])
        let narrowed = MonarchDependencyPickerTokens.groups(dependencyLinks, relations: [.relates])
        #expect(narrowed.map(\.relation) == [.relates])
    }

    @Test("Search leaves out excluded and already linked items")
    func candidates() {
        let candidates = MonarchDependencyPickerTokens.candidates(
            dependencyItems,
            links: [dependencyLinks[0]],
            excluding: ["self"]
        )
        #expect(candidates.map(\.id) == ["2", "3"])
    }

    @Test("An item becomes a search option with its key as hint and its status as description")
    func option() {
        let option = MonarchDependencyPickerTokens.option(dependencyItems[0])
        #expect(option.id == "1")
        #expect(option.label == "Fix login")
        #expect(option.hint == "STR-12")
        #expect(option.description == "In progress")
    }

    @Test("Only http and https addresses become links")
    func safeURL() {
        #expect(MonarchDependencyPickerTokens.safeURL(dependencyItems[0]) != nil)
        #expect(MonarchDependencyPickerTokens.safeURL(dependencyItems[1]) == nil)
        #expect(MonarchDependencyPickerTokens.safeURL(dependencyItems[2]) == nil)
    }

    @Test("The first relation is the preferred one when offered")
    func startRelation() {
        #expect(
            MonarchDependencyPickerTokens.startRelation(MonarchDependencyRelation.allCases, preferred: nil)
                == .blockedBy
        )
        #expect(MonarchDependencyPickerTokens.startRelation([.blocks, .relates], preferred: .relates) == .relates)
        #expect(MonarchDependencyPickerTokens.startRelation([.blocks], preferred: .relates) == .blocks)
        #expect(MonarchDependencyPickerTokens.startRelation([], preferred: nil) == .blockedBy)
    }

    @Test("Rows read the key, title and status; buttons and announcements name relation and item")
    func accessibility() {
        #expect(
            MonarchDependencyPickerTokens.rowLabel(dependencyItems[0], text: text)
                == "STR-12 Fix login, Status: In progress"
        )
        #expect(MonarchDependencyPickerTokens.rowLabel(dependencyItems[2], text: text) == "Write the launch post")
        #expect(text.remove("Blocked by", "STR-12 Fix login") == "Remove “Blocked by” link to STR-12 Fix login")
        #expect(text.added("Blocks", "STR-14 Update") == "Blocks STR-14 Update added.")
        #expect(text.removed("Blocks", "STR-14 Update") == "Blocks STR-14 Update removed.")
    }

    @Test("Colours: link role for linked titles, muted keys, disabled roles")
    func colors() {
        #expect(MonarchDependencyPickerTokens.titleColor(isLink: true, isEnabled: true) == \.textLink)
        #expect(MonarchDependencyPickerTokens.titleColor(isLink: false, isEnabled: true) == \.textDefault)
        #expect(MonarchDependencyPickerTokens.titleColor(isLink: true, isEnabled: false) == \.textDisabled)
        #expect(MonarchDependencyPickerTokens.keyColor(isEnabled: true) == \.textMuted)
    }

    @Test("Built-in strings come from the table and can be replaced")
    func strings() {
        #expect(
            MonarchDependencyRelation.allCases.map(text.relation) == [
                "Blocked by", "Blocks", "Related to", "Duplicates", "Duplicated by",
            ]
        )
        #expect(text.empty == "No linked items.")
        #expect(text.searchLabel == "Find an item to link")
        #expect(text.searchPrompt == "Search by key or title")
        #expect(text.noResults == "No matching items.")
        #expect(text.relationLabel == "Relation")
        let custom = MonarchDependencyPickerText(
            labels: MonarchDependencyPickerLabels(
                relation: { $0 == .blocks ? "Bloque" : "Autre" },
                status: { "État \($0)" }
            )
        )
        #expect(custom.relation(.blocks) == "Bloque")
        #expect(custom.status("Ouvert") == "État Ouvert")
    }
}
