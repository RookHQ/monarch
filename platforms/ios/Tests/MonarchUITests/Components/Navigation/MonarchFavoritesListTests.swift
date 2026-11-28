import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private let favoritesListItems = [
    MonarchFavoriteItem("Roadmap", id: "doc:1", systemImage: "doc.text", isCurrent: true),
    MonarchFavoriteItem("Q3 launch", id: "project:2", systemImage: "folder"),
    MonarchFavoriteItem("My open tickets", id: "view:3"),
]

enum MonarchFavoritesListFixtures: ComponentFixtures {
    static let id = "favorites-list"
    static let fixtures = [
        Fixture("in a list, editable, with a current row") {
            List {
                MonarchFavoritesList(
                    items: favoritesListItems,
                    onRemove: { _ in },
                    onReorder: { _ in },
                    onSelect: { _ in }
                )
            }
            .frame(minHeight: MonarchAccessibility.minimumHitTarget * 6)
        },
        Fixture("in a list, editing") {
            List {
                MonarchFavoritesList(
                    items: favoritesListItems,
                    isEditing: .constant(true),
                    onRemove: { _ in },
                    onReorder: { _ in },
                    onSelect: { _ in }
                )
            }
            .frame(minHeight: MonarchAccessibility.minimumHitTarget * 6)
        },
        Fixture("read only, no star, long names") {
            List {
                MonarchFavoritesList(
                    items: [
                        MonarchFavoriteItem(
                            "Quarterly planning for the platform and infrastructure teams, second half",
                            id: "doc:9",
                            systemImage: "doc.text"
                        )
                    ],
                    showsIcon: false,
                    labels: MonarchFavoritesListLabels(title: "Angeheftete Seiten und Ansichten")
                ) { _ in }
            }
            .frame(minHeight: MonarchAccessibility.minimumHitTarget * 4)
        },
        Fixture("empty with a message") {
            List {
                MonarchFavoritesList(
                    items: [],
                    emptyMessage: "Star a page to pin it here.",
                    onRemove: { _ in },
                    onSelect: { _ in }
                )
            }
            .frame(minHeight: MonarchAccessibility.minimumHitTarget * 3)
        },
        Fixture("outside a list, in a stack") {
            VStack(alignment: .leading) {
                MonarchFavoritesList(items: favoritesListItems) { _ in }
                MonarchFavoritesList(items: []) { _ in }
            }
        },
    ]
}

@Suite("MonarchFavoritesList")
struct MonarchFavoritesListTests {
    typealias Tokens = MonarchFavoritesListTokens
    private let labels = MonarchFavoritesListLabels()

    @Test("The current row takes the accent fill and strong text; others the default text")
    func colors() {
        let current = Tokens.colors(isCurrent: true)
        #expect(current.text == \.textDefault)
        #expect(current.fill == \.accentSubtle)
        let other = Tokens.colors(isCurrent: false)
        #expect(other.text == \.textDefault)
        #expect(other.icon == \.textMuted)
        #expect(other.fill == nil)
    }

    @Test("Editing needs a remove or reorder handler and at least one item")
    func editing() {
        #expect(Tokens.isEditable(canRemove: false, canReorder: false) == false)
        #expect(Tokens.isEditable(canRemove: true, canReorder: false))
        #expect(Tokens.isEditable(canRemove: false, canReorder: true))
        #expect(Tokens.isEditing(requested: true, isEditable: true, count: 2))
        #expect(Tokens.isEditing(requested: true, isEditable: false, count: 2) == false)
        #expect(Tokens.isEditing(requested: true, isEditable: true, count: 0) == false)
        #expect(Tokens.isEditing(requested: false, isEditable: true, count: 2) == false)
    }

    @Test("An empty list disappears unless it has a message")
    func hidden() {
        #expect(Tokens.isHidden(count: 0, hasEmptyMessage: false))
        #expect(Tokens.isHidden(count: 0, hasEmptyMessage: true) == false)
        #expect(Tokens.isHidden(count: 1, hasEmptyMessage: false) == false)
    }

    @Test("onMove offsets give the new order")
    func movedByOffsets() {
        let moved = Tokens.moved(favoritesListItems, fromOffsets: IndexSet(integer: 0), toOffset: 3)
        #expect(moved.map(\.id) == ["project:2", "view:3", "doc:1"])
        let back = Tokens.moved(favoritesListItems, fromOffsets: IndexSet(integer: 2), toOffset: 0)
        #expect(back.map(\.id) == ["view:3", "doc:1", "project:2"])
    }

    @Test("Move up and down stop at the ends")
    func movedByStep() {
        #expect(
            Tokens.moved(favoritesListItems, id: "project:2", by: -1)?.map(\.id) == ["project:2", "doc:1", "view:3"]
        )
        #expect(
            Tokens.moved(favoritesListItems, id: "project:2", by: 1)?.map(\.id) == ["doc:1", "view:3", "project:2"]
        )
        #expect(Tokens.moved(favoritesListItems, id: "doc:1", by: -1) == nil)
        #expect(Tokens.moved(favoritesListItems, id: "view:3", by: 1) == nil)
        #expect(Tokens.moved(favoritesListItems, id: "missing", by: 1) == nil)
        #expect(Tokens.moved(favoritesListItems, id: "doc:1", by: 0) == nil)
    }

    @Test("onDelete offsets name the removed items and ignore stale offsets")
    func removed() {
        #expect(Tokens.removed(favoritesListItems, atOffsets: IndexSet([0, 2])).map(\.id) == ["doc:1", "view:3"])
        #expect(Tokens.removed(favoritesListItems, atOffsets: IndexSet(integer: 7)).isEmpty)
    }

    @Test("The built-in strings resolve")
    func builtInStrings() {
        #expect(Tokens.title(labels: labels) == "Favorites")
        #expect(Tokens.editTitle(isEditing: false, labels: labels) == "Edit")
        #expect(Tokens.editTitle(isEditing: true, labels: labels) == "Done")
        #expect(Tokens.editAccessibilityLabel(isEditing: false, labels: labels) == "Edit favorites")
        #expect(Tokens.editAccessibilityLabel(isEditing: true, labels: labels) == "Done")
        #expect(Tokens.removedAnnouncement("Roadmap", labels: labels) == "Roadmap removed from favorites.")
        let actions = Tokens.moveActions(labels: labels)
        #expect(actions.up == "Move up")
        #expect(actions.down == "Move down")
    }

    @Test("Overrides replace only the strings given")
    func overrides() {
        let custom = MonarchFavoritesListLabels(title: "Favoriten", done: "Fertig", removed: "%@ entfernt.")
        #expect(Tokens.title(labels: custom) == "Favoriten")
        #expect(Tokens.editTitle(isEditing: true, labels: custom) == "Fertig")
        #expect(Tokens.editTitle(isEditing: false, labels: custom) == "Edit")
        #expect(Tokens.removedAnnouncement("Roadmap", labels: custom) == "Roadmap entfernt.")
    }

    @Test("Items are identified by their key")
    func identity() {
        let item = MonarchFavoriteItem("Roadmap", id: "doc:1")
        #expect(item.id == "doc:1")
        #expect(item.systemImage == nil)
        #expect(item.isCurrent == false)
    }
}
